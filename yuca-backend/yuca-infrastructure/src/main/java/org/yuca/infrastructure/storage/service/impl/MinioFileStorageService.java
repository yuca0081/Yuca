package org.yuca.infrastructure.storage.service.impl;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.infrastructure.minio.config.MinioProperties;
import org.yuca.infrastructure.storage.dto.FileInfo;
import org.yuca.infrastructure.storage.dto.UploadResult;
import org.yuca.infrastructure.storage.service.FileStorageService;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MinIO文件存储服务实现
 *
 * @author Yuca
 * @since 2025-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public UploadResult upload(MultipartFile file) {
        String objectName = generateObjectName(file.getOriginalFilename());
        return upload(file, objectName);
    }

    @Override
    public UploadResult upload(MultipartFile file, String objectName) {
        try {
            InputStream inputStream = file.getInputStream();
            String contentType = file.getContentType();
            long fileSize = file.getSize();

            return upload(inputStream, objectName, contentType, fileSize);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public UploadResult upload(InputStream inputStream, String objectName, String contentType, long fileSize) {
        try {
            // 确保bucket存在
            ensureBucketExists();

            // 上传文件
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(objectName)
                .stream(inputStream, fileSize, -1)
                .contentType(contentType)
                .build();

            ObjectWriteResponse response = minioClient.putObject(putObjectArgs);

            // 构建返回结果
            String url = getFileUrl(objectName);

            return UploadResult.builder()
                .objectName(objectName)
                .originalFileName(extractFileName(objectName))
                .fileSize(fileSize)
                .contentType(contentType)
                .url(url)
                .etag(response.etag())
                .build();

        } catch (Exception e) {
            log.error("文件上传失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(objectName)
                .build();

            minioClient.removeObject(args);
            log.info("文件删除成功: {}", objectName);

        } catch (Exception e) {
            log.error("文件删除失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBatch(List<String> objectNames) {
        try {
            List<DeleteObject> objects = new ArrayList<>();
            for (String objectName : objectNames) {
                objects.add(new DeleteObject(objectName));
            }

            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                    .bucket(properties.getBucketName())
                    .objects(objects)
                    .build()
            );

            // 检查删除结果
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("删除文件失败: object={}, message={}",
                    error.objectName(), error.message());
            }

            log.info("批量删除文件成功: count={}", objectNames.size());

        } catch (Exception e) {
            log.error("批量删除文件失败: error={}", e.getMessage(), e);
            throw new RuntimeException("批量删除文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String objectName) {
        return properties.getEndpoint() + "/" +
               properties.getBucketName() + "/" +
               objectName;
    }

    @Override
    public String getPresignedUrl(String objectName, int expires) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .expiry(expires, TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            log.error("获取临时URL失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("获取临时URL失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            log.error("文件下载失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public FileInfo getFileInfo(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .build()
            );

            return FileInfo.builder()
                .objectName(objectName)
                .fileName(extractFileName(objectName))
                .fileSize(stat.size())
                .contentType(stat.contentType())
                .etag(stat.etag())
                .lastModified(LocalDateTime.ofInstant(
                    stat.lastModified().toInstant(),
                    ZoneId.systemDefault()
                ))
                .build();

        } catch (Exception e) {
            log.error("获取文件信息失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("获取文件信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String objectName) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            if (!minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build())) {

                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .region(properties.getRegion())
                        .build()
                );

                log.info("创建bucket成功: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("创建bucket失败: bucketName={}, error={}", bucketName, e.getMessage(), e);
            throw new RuntimeException("创建bucket失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
        } catch (Exception e) {
            log.error("检查bucket存在失败: bucketName={}, error={}", bucketName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 确保bucket存在
     */
    private void ensureBucketExists() {
        String bucketName = properties.getBucketName();
        if (!bucketExists(bucketName)) {
            createBucket(bucketName);
        }
    }

    /**
     * 生成对象名称（存储路径）
     * 格式: upload/{year}/{month}/{day}/{uuid}{extension}
     */
    private String generateObjectName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        // 按日期分目录: upload/2025/01/27/uuid.jpg
        LocalDateTime now = LocalDateTime.now();
        String datePath = String.format("%d/%02d/%02d",
            now.getYear(),
            now.getMonthValue(),
            now.getDayOfMonth());

        return String.format("%s/%s/%s%s",
            "upload",
            datePath,
            uuid,
            extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * 从对象名称中提取文件名
     */
    private String extractFileName(String objectName) {
        int lastSlashIndex = objectName.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            return objectName;
        }
        return objectName.substring(lastSlashIndex + 1);
    }
}
