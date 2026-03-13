package org.yuca.infrastructure.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.infrastructure.storage.dto.FileInfo;
import org.yuca.infrastructure.storage.dto.UploadResult;
import org.yuca.infrastructure.storage.dto.request.FileUploadRequest;
import org.yuca.infrastructure.storage.dto.response.StorageFileResponse;
import org.yuca.infrastructure.storage.entity.StorageFile;
import org.yuca.infrastructure.storage.enums.FileType;
import org.yuca.infrastructure.storage.mapper.StorageFileMapper;
import org.yuca.infrastructure.storage.service.FileStorageService;
import org.yuca.infrastructure.storage.service.StorageFileService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件存储记录服务实现
 *
 * @author Yuca
 * @since 2025-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageFileServiceImpl extends ServiceImpl<StorageFileMapper, StorageFile>
        implements StorageFileService {

    private final FileStorageService fileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse uploadAndRecord(MultipartFile file, Long uploadedBy, FileUploadRequest request) {
        // 1. 上传文件到MinIO
        UploadResult uploadResult = fileStorageService.upload(file);

        // 2. 保存文件记录到数据库
        StorageFile storageFile = new StorageFile();
        storageFile.setUploadedBy(uploadedBy);
        storageFile.setFileName(file.getOriginalFilename());
        storageFile.setFileSize(file.getSize());
        storageFile.setContentType(file.getContentType());
        storageFile.setObjectName(uploadResult.getObjectName());
        storageFile.setFileUrl(uploadResult.getUrl());
        storageFile.setFileType(FileType.fromFileName(file.getOriginalFilename()).getCategory().toLowerCase());
        storageFile.setSourceType(request.getSourceType());
        storageFile.setBusinessType(request.getBusinessType());
        storageFile.setBusinessId(request.getBusinessId());
        storageFile.setMetadata(request.getMetadata());

        this.baseMapper.insert(storageFile);

        log.info("文件上传并记录成功: fileId={}, uploadedBy={}, fileName={}",
                storageFile.getId(), uploadedBy, file.getOriginalFilename());

        // 3. 转换为响应DTO
        return convertToResponse(storageFile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse recordExistingFile(String objectName, Long uploadedBy, FileUploadRequest request) {
        // 1. 获取MinIO文件信息
        FileInfo fileInfo = fileStorageService.getFileInfo(objectName);

        // 2. 保存文件记录到数据库
        StorageFile storageFile = new StorageFile();
        storageFile.setUploadedBy(uploadedBy);
        storageFile.setFileName(fileInfo.getFileName());
        storageFile.setFileSize(fileInfo.getFileSize());
        storageFile.setContentType(fileInfo.getContentType());
        storageFile.setObjectName(objectName);
        storageFile.setFileUrl(fileStorageService.getFileUrl(objectName));
        storageFile.setFileType(FileType.fromFileName(fileInfo.getFileName()).getCategory().toLowerCase());
        storageFile.setSourceType(request.getSourceType());
        storageFile.setBusinessType(request.getBusinessType());
        storageFile.setBusinessId(request.getBusinessId());
        storageFile.setMetadata(request.getMetadata());

        this.baseMapper.insert(storageFile);

        log.info("已存在文件记录成功: fileId={}, objectName={}", storageFile.getId(), objectName);

        return convertToResponse(storageFile);
    }

    @Override
    public List<StorageFileResponse> listByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<StorageFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageFile::getBusinessType, businessType)
                .eq(businessId != null, StorageFile::getBusinessId, businessId)
                .orderByDesc(StorageFile::getCreatedAt);

        List<StorageFile> storageFiles = this.baseMapper.selectList(wrapper);
        return storageFiles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StorageFileResponse> listBySource(String sourceType) {
        LambdaQueryWrapper<StorageFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageFile::getSourceType, sourceType)
                .orderByDesc(StorageFile::getCreatedAt);

        List<StorageFile> storageFiles = this.baseMapper.selectList(wrapper);
        return storageFiles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId, Long userId) {
        StorageFile storageFile = this.baseMapper.selectById(fileId);

        if (storageFile == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件记录不存在");
        }

        // 权限验证：用户只能删除自己上传的文件
        if ("user".equals(storageFile.getSourceType()) &&
            (userId == null || !storageFile.getUploadedBy().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此文件");
        }

        // 逻辑删除数据库记录
        this.baseMapper.deleteById(fileId);

        // 删除MinIO中的文件
        fileStorageService.delete(storageFile.getObjectName());

        log.info("文件删除成功: fileId={}", fileId);
    }

    @Override
    public Long getUserTotalFileSize(Long userId) {
        LambdaQueryWrapper<StorageFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageFile::getUploadedBy, userId)
                .eq(StorageFile::getSourceType, "user");

        List<StorageFile> files = this.baseMapper.selectList(wrapper);
        return files.stream()
                .mapToLong(StorageFile::getFileSize)
                .sum();
    }

    /**
     * 转换为响应DTO
     */
    private StorageFileResponse convertToResponse(StorageFile storageFile) {
        StorageFileResponse response = new StorageFileResponse();
        response.setId(storageFile.getId());
        response.setUploadedBy(storageFile.getUploadedBy());
        response.setFileName(storageFile.getFileName());
        response.setFileSize(storageFile.getFileSize());
        response.setContentType(storageFile.getContentType());
        response.setFileUrl(storageFile.getFileUrl());
        response.setFileType(storageFile.getFileType());
        response.setSourceType(storageFile.getSourceType());
        response.setBusinessType(storageFile.getBusinessType());
        response.setBusinessId(storageFile.getBusinessId());
        response.setMetadata(storageFile.getMetadata());
        response.setCreatedAt(storageFile.getCreatedAt());
        return response;
    }
}
