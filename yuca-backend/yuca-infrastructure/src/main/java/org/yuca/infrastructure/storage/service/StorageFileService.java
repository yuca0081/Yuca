package org.yuca.infrastructure.storage.service;

import org.yuca.infrastructure.storage.dto.request.FileUploadRequest;
import org.yuca.infrastructure.storage.dto.response.StorageFileResponse;
import org.yuca.infrastructure.storage.entity.StorageFile;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 文件存储记录服务接口
 *
 * @author Yuca
 * @since 2025-01-29
 */
public interface StorageFileService extends IService<StorageFile> {

    /**
     * 上传文件并记录
     *
     * @param file       文件
     * @param uploadedBy 上传者ID（可为NULL，系统文件）
     * @param request    上传请求参数
     * @return 文件记录响应
     */
    StorageFileResponse uploadAndRecord(MultipartFile file, Long uploadedBy, FileUploadRequest request);

    /**
     * 记录已上传的文件（用于文件已存在，只需记录的情况）
     *
     * @param objectName MinIO对象名称
     * @param uploadedBy 上传者ID
     * @param request    请求参数
     * @return 文件记录响应
     */
    StorageFileResponse recordExistingFile(String objectName, Long uploadedBy, FileUploadRequest request);

    /**
     * 根据业务查询文件列表
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 文件列表
     */
    List<StorageFileResponse> listByBusiness(String businessType, Long businessId);

    /**
     * 根据来源查询文件列表
     *
     * @param sourceType 来源类型
     * @return 文件列表
     */
    List<StorageFileResponse> listBySource(String sourceType);

    /**
     * 删除文件记录（逻辑删除数据库记录，物理删除MinIO文件）
     *
     * @param fileId 文件ID
     * @param userId 用户ID（权限验证）
     */
    void deleteFile(Long fileId, Long userId);

    /**
     * 获取用户上传的文件总大小
     *
     * @param userId 用户ID
     * @return 文件总大小（字节）
     */
    Long getUserTotalFileSize(Long userId);
}
