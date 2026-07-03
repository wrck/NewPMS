package com.vibe.utils;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件工具类
 *
 * <p>提供文件上传/下载/预签名 URL/删除等能力。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class MinioUtils {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public MinioUtils(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    /**
     * 初始化默认 Bucket（启动时调用，幂等）
     */
    public void ensureDefaultBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
                log.info("[MinIO] 默认 Bucket 创建成功: {}", minioConfig.getBucket());
            }
        } catch (Exception e) {
            log.error("[MinIO] Bucket 初始化失败: {}", minioConfig.getBucket(), e);
        }
    }

    /**
     * 上传 MultipartFile
     *
     * @param file     Spring 文件对象
     * @param dir      Bucket 内目录，如 "project/101"
     * @return Bucket 内对象路径（objectName）
     */
    public String upload(MultipartFile file, String dir) {
        String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String suffix = "";
        int dotIdx = original.lastIndexOf('.');
        if (dotIdx >= 0) {
            suffix = original.substring(dotIdx);
        }
        String objectName = (dir == null || dir.isBlank() ? "" : dir + "/")
                + UUID.randomUUID().toString().replace("-", "") + suffix;
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectName;
        } catch (Exception e) {
            log.error("[MinIO] 文件上传失败: {}", original, e);
            throw new BusinessException(ResultCode.MINIO_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传字节数组
     */
    public String upload(byte[] bytes, String objectName, String contentType) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(bis, bytes.length, -1)
                    .contentType(contentType)
                    .build());
            return objectName;
        } catch (Exception e) {
            log.error("[MinIO] 字节数组上传失败: {}", objectName, e);
            throw new BusinessException(ResultCode.MINIO_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件流
     */
    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("[MinIO] 文件下载失败: {}", objectName, e);
            throw new BusinessException(ResultCode.MINIO_ERROR, "文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名下载 URL（默认过期时间）
     */
    public String getPresignedDownloadUrl(String objectName) {
        return getPresignedDownloadUrl(objectName, minioConfig.getDefaultExpiry());
    }

    /**
     * 生成预签名下载 URL
     *
     * @param expirySeconds 过期秒数
     */
    public String getPresignedDownloadUrl(String objectName, long expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .expiry((int) Math.min(expirySeconds, Integer.MAX_VALUE), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("[MinIO] 预签名 URL 生成失败: {}", objectName, e);
            throw new BusinessException(ResultCode.MINIO_ERROR, "预签名 URL 生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名上传 URL（前端直传场景）
     */
    public String getPresignedUploadUrl(String objectName, long expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .expiry((int) Math.min(expirySeconds, Integer.MAX_VALUE), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("[MinIO] 预签名上传 URL 生成失败: {}", objectName, e);
            throw new BusinessException(ResultCode.MINIO_ERROR, "预签名上传 URL 生成失败: " + e.getMessage());
        }
    }

    /**
     * 删除对象
     */
    public boolean delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("[MinIO] 文件删除失败: {}", objectName, e);
            return false;
        }
    }

    /**
     * 列出指定前缀下的对象
     */
    public List<String> listObjects(String prefix) {
        List<String> result = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .prefix(prefix)
                    .recursive(true)
                    .build());
            for (Result<Item> r : results) {
                result.add(r.get().objectName());
            }
        } catch (Exception e) {
            log.error("[MinIO] 列对象失败: prefix={}", prefix, e);
        }
        return result;
    }

    /**
     * 获取默认 Bucket 名称
     */
    public String getDefaultBucket() {
        return minioConfig.getBucket();
    }

    /**
     * Duration 兼容方法（保留给调用方使用）
     */
    public Duration defaultExpiryDuration() {
        return Duration.ofSeconds(minioConfig.getDefaultExpiry());
    }
}
