package org.yuca.yuca.infrastructure.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO配置属性
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO服务地址
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 区域
     */
    private String region;

    /**
     * 默认存储桶名称
     */
    private String bucketName;

    /**
     * 单个文件最大大小
     */
    private String maxFileSize = "10MB";
}
