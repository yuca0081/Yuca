package org.yuca.infrastructure.storage.service;

import org.yuca.infrastructure.storage.dto.FileInfo;
import org.yuca.infrastructure.storage.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储服务接口
 * 定义文件存储的通用操作，支持多种存储实现（MinIO、阿里云OSS、腾讯云COS等）
 *
 * @author Yuca
 * @since 2025-01-29
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 上传结果
     */
    UploadResult upload(MultipartFile file);

    /**
     * 上传文件到指定路径
     *
     * @param file       文件
     * @param objectName 对象名称（存储路径）
     * @return 上传结果
     */
    UploadResult upload(MultipartFile file, String objectName);

    /**
     * 上传文件（使用输入流）
     *
     * @param inputStream 文件输入流
     * @param objectName  对象名称
     * @param contentType 内容类型
     * @param fileSize    文件大小
     * @return 上传结果
     */
    UploadResult upload(InputStream inputStream, String objectName, String contentType, long fileSize);

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     */
    void delete(String objectName);

    /**
     * 批量删除文件
     *
     * @param objectNames 对象名称列表
     */
    void deleteBatch(List<String> objectNames);

    /**
     * 获取文件访问URL
     *
     * @param objectName 对象名称
     * @return 文件访问URL
     */
    String getFileUrl(String objectName);

    /**
     * 获取临时访问URL（带过期时间）
     *
     * @param objectName 对象名称
     * @param expires    过期时间（秒）
     * @return 临时访问URL
     */
    String getPresignedUrl(String objectName, int expires);

    /**
     * 下载文件
     *
     * @param objectName 对象名称
     * @return 文件输入流
     */
    InputStream download(String objectName);

    /**
     * 获取文件信息
     *
     * @param objectName 对象名称
     * @return 文件信息
     */
    FileInfo getFileInfo(String objectName);

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    boolean exists(String objectName);

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     */
    void createBucket(String bucketName);

    /**
     * 检查存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    boolean bucketExists(String bucketName);
}
