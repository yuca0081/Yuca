package org.yuca.yuca.infrastructure.storage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.yuca.common.response.Result;
import org.yuca.yuca.infrastructure.security.SecurityUtils;
import org.yuca.yuca.infrastructure.storage.dto.FileInfo;
import org.yuca.yuca.infrastructure.storage.dto.UploadResult;
import org.yuca.yuca.infrastructure.storage.dto.request.BatchDeleteRequest;
import org.yuca.yuca.infrastructure.storage.dto.request.FileUploadRequest;
import org.yuca.yuca.infrastructure.storage.dto.response.StorageFileResponse;
import org.yuca.yuca.infrastructure.storage.enums.SourceType;
import org.yuca.yuca.infrastructure.storage.service.FileStorageService;
import org.yuca.yuca.infrastructure.storage.service.StorageFileService;

import java.util.List;

/**
 * 文件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Validated
@Tag(name = "文件管理", description = "文件上传、下载、删除等接口")
public class FileController {

    private final FileStorageService fileStorageService;
    private final StorageFileService storageFileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件并记录", description = "上传单个文件，返回文件访问URL并保存记录")
    public Result<StorageFileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "businessId", required = false) Long businessId,
            @RequestParam(value = "metadata", required = false) String metadata) {
        log.info("上传文件: fileName={}, size={}, businessType={}",
                file.getOriginalFilename(), file.getSize(), businessType);
        Long userId = SecurityUtils.getCurrentUserId();

        FileUploadRequest request = new FileUploadRequest();
        request.setSourceType(SourceType.USER.getValue());
        request.setBusinessType(businessType);
        request.setBusinessId(businessId);
        request.setMetadata(metadata);

        StorageFileResponse result = storageFileService.uploadAndRecord(file, userId, request);
        return Result.success(result);
    }

    @GetMapping("/my-files")
    @Operation(summary = "查询我的文件", description = "查询当前用户上传的所有文件")
    public Result<List<StorageFileResponse>> getMyFiles() {
        Long userId = SecurityUtils.getCurrentUserId();

        List<StorageFileResponse> files = storageFileService.listBySource("user");
        // TODO: 过滤只返回当前用户的文件
        return Result.success(files);
    }

    @GetMapping("/business/{businessType}/{businessId}")
    @Operation(summary = "查询业务文件", description = "查询指定业务关联的所有文件")
    public Result<List<StorageFileResponse>> getBusinessFiles(
            @PathVariable String businessType,
            @PathVariable Long businessId) {
        List<StorageFileResponse> files = storageFileService.listByBusiness(businessType, businessId);
        return Result.success(files);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件记录", description = "删除文件记录并物理删除MinIO文件")
    public Result<Void> deleteFile(@PathVariable Long fileId) {
        // TODO: 从JWT获取用户ID
        Long userId = 1L; // 暂时硬编码

        storageFileService.deleteFile(fileId, userId);
        return Result.success();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件（通过对象名称）", description = "根据对象名称删除MinIO文件（不操作数据库记录）")
    public Result<Void> deleteByObjectName(@RequestParam("objectName") String objectName) {
        log.info("删除MinIO文件: objectName={}", objectName);
        fileStorageService.delete(objectName);
        return Result.success();
    }

    @DeleteMapping("/batch-delete")
    @Operation(summary = "批量删除文件", description = "批量删除多个MinIO文件（不操作数据库记录）")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        log.info("批量删除MinIO文件: count={}", request.getObjectNames().size());
        fileStorageService.deleteBatch(request.getObjectNames());
        return Result.success();
    }

    @GetMapping("/url")
    @Operation(summary = "获取文件URL", description = "获取文件的访问地址")
    public Result<String> getFileUrl(@RequestParam("objectName") String objectName) {
        String url = fileStorageService.getFileUrl(objectName);
        return Result.success(url);
    }

    @GetMapping("/presigned-url")
    @Operation(summary = "获取临时URL", description = "获取带过期时间的临时访问URL，默认7天有效")
    public Result<String> getPresignedUrl(
        @RequestParam("objectName") String objectName,
        @RequestParam(value = "expires", defaultValue = "604800") int expires) {
        String url = fileStorageService.getPresignedUrl(objectName, expires);
        return Result.success(url);
    }

    @GetMapping("/info")
    @Operation(summary = "获取文件信息", description = "获取文件的详细信息")
    public Result<FileInfo> getFileInfo(
        @RequestParam("objectName") String objectName) {
        var fileInfo = fileStorageService.getFileInfo(objectName);
        return Result.success(fileInfo);
    }

    @GetMapping("/exists")
    @Operation(summary = "检查文件是否存在", description = "检查指定对象名称的文件是否存在")
    public Result<Boolean> exists(@RequestParam("objectName") String objectName) {
        boolean exists = fileStorageService.exists(objectName);
        return Result.success(exists);
    }

    @GetMapping("/download")
    @Operation(summary = "下载文件", description = "下载指定文件")
    public ResponseEntity<byte[]> download(@RequestParam("objectName") String objectName) {
        var fileInfo = fileStorageService.getFileInfo(objectName);

        try (var inputStream = fileStorageService.download(objectName)) {
            byte[] bytes = inputStream.readAllBytes();

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .header("Content-Disposition", "attachment; filename=\"" + fileInfo.getFileName() + "\"")
                .body(bytes);
        } catch (Exception e) {
            log.error("文件下载失败: objectName={}, error={}", objectName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
