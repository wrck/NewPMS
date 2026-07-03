package com.vibe.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置
 *
 * @author vibe
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vibe.minio")
public class MinioConfig {

    /** MinIO 服务地址，如 http://localhost:9000 */
    private String endpoint;
    /** 访问 Key */
    private String accessKey;
    /** 密钥 */
    private String secretKey;
    /** 默认 Bucket */
    private String bucket;
    /** 是否启用 HTTPS（与 endpoint 一致） */
    private boolean secure = false;
    /** 预签名 URL 默认过期时间（秒），默认 7 天 */
    private long defaultExpiry = 7 * 24 * 60 * 60L;

    @Bean
    public MinioClient minioClient() {
        // MinIO 8.5.x 移除了 secure(boolean) 方法，HTTPS 由 endpoint 的协议（https://）决定
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
