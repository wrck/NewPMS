/**
 * IndexedDB 封装：用于弱网场景下的照片本地缓存与断点续传
 * 数据库：vibe_mobile_db
 * 对象仓库：upload_files（按业务ID索引）
 */

const DB_NAME = 'vibe_mobile_db'
const DB_VERSION = 1
const STORE_FILES = 'upload_files'

/** 离线缓存文件记录 */
export interface CachedFile {
  /** 本地唯一ID（与 UploadFileMeta.localId 对应） */
  localId: string
  /** 业务关联ID */
  bizId: string
  /** 业务类型 */
  bizType: string
  /** 原始 Blob */
  blob: Blob
  /** 缩略图 base64 */
  thumbnail?: string
  /** 文件元信息（JSON 字符串） */
  meta: string
  /** 创建时间戳 */
  createdAt: number
}

let dbInstance: IDBDatabase | null = null

/** 打开数据库 */
function openDB(): Promise<IDBDatabase> {
  if (dbInstance) return Promise.resolve(dbInstance)
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result
      if (!db.objectStoreNames.contains(STORE_FILES)) {
        const store = db.createObjectStore(STORE_FILES, { keyPath: 'localId' })
        store.createIndex('bizId', 'bizId', { unique: false })
        store.createIndex('bizType', 'bizType', { unique: false })
        store.createIndex('createdAt', 'createdAt', { unique: false })
      }
    }
    request.onsuccess = (event) => {
      dbInstance = (event.target as IDBOpenDBRequest).result
      resolve(dbInstance)
    }
    request.onerror = () => reject(request.error)
  })
}

/** 事务包装 */
async function withStore<T>(
  mode: IDBTransactionMode,
  fn: (store: IDBObjectStore) => IDBRequest<T> | Promise<T>
): Promise<T> {
  const db = await openDB()
  return new Promise<T>((resolve, reject) => {
    const tx = db.transaction(STORE_FILES, mode)
    const store = tx.objectStore(STORE_FILES)
    let result: T
    Promise.resolve(fn(store))
      .then((req) => {
        if (req instanceof IDBRequest) {
          req.onsuccess = () => {
            result = req.result
          }
          req.onerror = () => reject(req.error)
        } else {
          result = req as T
        }
      })
      .catch(reject)
    tx.oncomplete = () => resolve(result)
    tx.onerror = () => reject(tx.error)
    tx.onabort = () => reject(tx.error)
  })
}

/** 新增/更新缓存文件 */
export async function putCachedFile(file: CachedFile): Promise<void> {
  await withStore('readwrite', (store) => store.put(file))
}

/** 批量新增 */
export async function putCachedFiles(files: CachedFile[]): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_FILES, 'readwrite')
    const store = tx.objectStore(STORE_FILES)
    files.forEach((f) => store.put(f))
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

/** 根据 localId 获取 */
export async function getCachedFile(localId: string): Promise<CachedFile | undefined> {
  return withStore('readonly', (store) => store.get(localId) as IDBRequest<CachedFile | undefined>)
}

/** 按业务ID获取所有缓存文件 */
export async function getCachedFilesByBiz(bizId: string): Promise<CachedFile[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_FILES, 'readonly')
    const store = tx.objectStore(STORE_FILES)
    const index = store.index('bizId')
    const result: CachedFile[] = []
    const req = index.openCursor(IDBKeyRange.only(bizId))
    req.onsuccess = (event) => {
      const cursor = (event.target as IDBRequest<IDBCursorWithValue>).result
      if (cursor) {
        result.push(cursor.value as CachedFile)
        cursor.continue()
      }
    }
    req.onerror = () => reject(req.error)
    tx.oncomplete = () => resolve(result)
  })
}

/** 获取所有待上传文件（按创建时间排序） */
export async function getAllCachedFiles(): Promise<CachedFile[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_FILES, 'readonly')
    const store = tx.objectStore(STORE_FILES)
    const index = store.index('createdAt')
    const result: CachedFile[] = []
    const req = index.openCursor()
    req.onsuccess = (event) => {
      const cursor = (event.target as IDBRequest<IDBCursorWithValue>).result
      if (cursor) {
        result.push(cursor.value as CachedFile)
        cursor.continue()
      }
    }
    req.onerror = () => reject(req.error)
    tx.oncomplete = () => resolve(result)
  })
}

/** 删除缓存文件 */
export async function deleteCachedFile(localId: string): Promise<void> {
  await withStore('readwrite', (store) => store.delete(localId))
}

/** 清空所有缓存文件 */
export async function clearAllCachedFiles(): Promise<void> {
  await withStore('readwrite', (store) => store.clear())
}

/** 缓存文件总数 */
export async function getCachedFileCount(): Promise<number> {
  return withStore('readonly', (store) => store.count() as IDBRequest<number>)
}
