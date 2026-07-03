#!/bin/sh
# =============================================================================
# MinIO Bucket 初始化脚本
# 由 minio-init 一次性服务调用，创建系统所需的全部 Bucket 并配置过期策略。
# =============================================================================
set -e

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-admin}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-admin12345}"

# 待创建的 Bucket 列表（与设计文档 1.10 节 Bucket 规划一致）
BUCKETS="vibe-photos vibe-documents vibe-attachments vibe-avatars vibe-exports"

echo "[minio-init] Waiting for MinIO at ${MINIO_ENDPOINT} ..."

# 等待 MinIO 就绪并建立 alias
until mc alias set local "${MINIO_ENDPOINT}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" >/dev/null 2>&1; do
  echo "[minio-init] MinIO not ready, retrying in 3s ..."
  sleep 3
done

echo "[minio-init] Connected to MinIO. Starting bucket creation ..."

for bucket in ${BUCKETS}; do
  if mc ls "local/${bucket}" >/dev/null 2>&1; then
    echo "[minio-init] Bucket '${bucket}' already exists, skip."
  else
    mc mb "local/${bucket}" >/dev/null
    echo "[minio-init] Bucket '${bucket}' created."
  fi
done

# 为 vibe-exports 设置 7 天自动过期策略（导出文件临时存储）
echo "[minio-init] Setting 7-day expiration rule on 'vibe-exports' ..."
if mc ilm rule add --expire-days 7 "local/vibe-exports" >/dev/null 2>&1; then
  echo "[minio-init] Expiration rule (7 days) applied to 'vibe-exports'."
else
  echo "[minio-init] WARN: failed to add expiration rule (may already exist), continuing."
fi

# 输出最终 Bucket 列表确认
echo "[minio-init] Current buckets:"
mc ls local

echo "[minio-init] Done."
