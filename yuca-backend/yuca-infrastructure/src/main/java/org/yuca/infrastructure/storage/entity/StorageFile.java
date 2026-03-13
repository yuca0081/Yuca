package org.yuca.infrastructure.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件存储记录实体
 * 记录所有上传到OSS的文件（包括用户上传和系统生成）
 *
 * @author Yuca
 * @since 2025-01-29
 */
@Data
@TableName("storage_file")
public class StorageFile {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传者ID（NULL表示系统文件）
     */
    private Long uploadedBy;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * MinIO对象名称
     */
    private String objectName;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 文件分类
     */
    private String fileType;

    /**
     * 来源类型（user/system/task/sync/api/export）
     */
    private String sourceType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务关联ID
     */
    private Long businessId;

    /**
     * 扩展元数据
     */
    private String metadata;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
