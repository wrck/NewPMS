/**
 * FileUpload 弱网断点续传本地缓存
 *
 * 设计：
 *   - 使用 IndexedDB 存储分片 Blob 和 uploadId
 *   - 不依赖任何第三方库（避免引入 idb-keyval 增加 bundle 体积）
 *   - 一个 fileHash 对应一个 uploadId + 多个 partNumber -> Blob 映射
 *   - 上传完成后由组件主动调用 clearChunks 清除
 *
 * 存储结构：
 *   DB: vibe_file_upload
 *   ObjectStore: chunks       keyPath: [fileHash, partNumber]   值: { fileHash, partNumber, blob }
 *   ObjectStore: upload_ids  keyPath: fileHash                  值: { fileHash, uploadId, accessUrl, objectName, parts: number }
 *
 * 容错：
 *   - IndexedDB 在 SSR / 无痕模式可能不可用，所有 API catch 后回退为 no-op
 *   - 接口均返回 Promise，调用方需 await
 */

const DB_NAME = 'vibe_file_upload'
const DB_VERSION = 1
const STORE_CHUNKS = 'chunks'
const STORE_UPLOAD_IDS = 'upload_ids'

let dbPromise: Promise<IDBDatabase | null> | null = null

/**
 * 打开 / 升级 IndexedDB
 *
 * 失败时返回 null，调用方据此降级（不启用断点续传）。
 */
function openDB(): Promise<IDBDatabase | null> {
  if (dbPromise) return dbPromise
  dbPromise = new Promise((resolve) => {
    if (typeof indexedDB === 'undefined') {
      resolve(null)
      return
    }
    try {
      const req = indexedDB.open(DB_NAME, DB_VERSION)
      req.onupgradeneeded = () => {
        const db = req.result
        if (!db.objectStoreNames.contains(STORE_CHUNKS)) {
          // 复合主键 [fileHash, partNumber]
          db.createObjectStore(STORE_CHUNKS, { keyPath: ['fileHash', 'partNumber'] })
        }
        if (!db.objectStoreNames.contains(STORE_UPLOAD_IDS)) {
          db.createObjectStore(STORE_UPLOAD_IDS, { keyPath: 'fileHash' })
        }
      }
      req.onsuccess = () => resolve(req.result)
      req.onerror = () => {
        console.warn('[FileUpload] IndexedDB open failed, resume disabled:', req.error)
        resolve(null)
      }
    } catch (e) {
      console.warn('[FileUpload] IndexedDB open threw, resume disabled:', e)
      resolve(null)
    }
  })
  return dbPromise
}

/** 通用事务包装：返回 store 用于操作 */
async function withStore<T>(
  storeName: string,
  mode: IDBTransactionMode,
  fn: (store: IDBObjectStore) => IDBRequest<T>
): Promise<T | null> {
  const db = await openDB()
  if (!db) return null
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, mode)
    const store = tx.objectStore(storeName)
    const req = fn(store)
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => resolve(null) // 不抛错，降级处理
    tx.onerror = () => resolve(null)
  })
}

/* ============ 分片缓存 API ============ */

/**
 * 保存一个分片到 IndexedDB
 */
export async function saveChunk(fileHash: string, partNumber: number, blob: Blob): Promise<void> {
  await withStore(STORE_CHUNKS, 'readwrite', (store) =>
    store.put({ fileHash, partNumber, blob })
  )
}

/**
 * 获取某文件已缓存的所有分片
 *
 * @returns Record<partNumber, Blob>；DB 不可用时返回空对象
 */
export async function getChunks(fileHash: string): Promise<Record<number, Blob>> {
  const db = await openDB()
  if (!db) return {}
  return new Promise((resolve) => {
    try {
      const tx = db.transaction(STORE_CHUNKS, 'readonly')
      const store = tx.objectStore(STORE_CHUNKS)
      // 复合主键查询：按 fileHash 前缀匹配（IDBKeyRange 仅支持首列）
      const idx = store.index // 无索引，使用游标
      const result: Record<number, Blob> = {}
      const req = store.openCursor()
      req.onsuccess = () => {
        const cursor = req.result
        if (!cursor) {
          resolve(result)
          return
        }
        const value = cursor.value
        if (value && value.fileHash === fileHash) {
          result[value.partNumber] = value.blob
        }
        cursor.continue()
      }
      req.onerror = () => resolve({})
    } catch (e) {
      console.warn('[FileUpload] getChunks failed:', e)
      resolve({})
    }
  })
}

/**
 * 清除某文件的所有分片缓存
 */
export async function clearChunks(fileHash: string): Promise<void> {
  const chunks = await getChunks(fileHash)
  const db = await openDB()
  if (!db) return
  for (const partNumber of Object.keys(chunks).map(Number)) {
    await withStore(STORE_CHUNKS, 'readwrite', (store) =>
      store.delete([fileHash, partNumber])
    )
  }
}

/* ============ uploadId 缓存 API ============ */

export interface UploadIdRecord {
  fileHash: string
  uploadId: string
  accessUrl?: string
  objectName?: string
  parts: number
}

/**
 * 保存 uploadId（含 accessUrl / objectName / 总分片数）
 */
export async function saveUploadId(record: UploadIdRecord): Promise<void> {
  await withStore(STORE_UPLOAD_IDS, 'readwrite', (store) =>
    store.put(record)
  )
}

/**
 * 获取 uploadId 记录
 */
export async function getUploadId(fileHash: string): Promise<UploadIdRecord | null> {
  const result = await withStore<UploadIdRecord | undefined>(STORE_UPLOAD_IDS, 'readonly', (store) =>
    store.get(fileHash)
  )
  return result || null
}

/**
 * 更新 uploadId 记录的部分字段（如保存 accessUrl）
 */
export async function updateUploadId(fileHash: string, patch: Partial<UploadIdRecord>): Promise<void> {
  const existing = await getUploadId(fileHash)
  if (!existing) return
  await withStore(STORE_UPLOAD_IDS, 'readwrite', (store) =>
    store.put({ ...existing, ...patch, fileHash })
  )
}

/**
 * 删除 uploadId 记录
 */
export async function deleteUploadId(fileHash: string): Promise<void> {
  await withStore(STORE_UPLOAD_IDS, 'readwrite', (store) =>
    store.delete(fileHash)
  )
}

/**
 * 清除某文件的所有断点续传记录（分片 + uploadId）
 *
 * 上传完成或失败时调用。
 */
export async function clearResume(fileHash: string): Promise<void> {
  await clearChunks(fileHash)
  await deleteUploadId(fileHash)
}

/**
 * 检测 IndexedDB 是否可用（部分浏览器 / 无痕模式禁用）
 */
export async function isIndexedDBAvailable(): Promise<boolean> {
  const db = await openDB()
  return !!db
}

export default {
  saveChunk,
  getChunks,
  clearChunks,
  saveUploadId,
  getUploadId,
  updateUploadId,
  deleteUploadId,
  clearResume,
  isIndexedDBAvailable
}
