package org.yuca.yuca.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.yuca.yuca.common.exception.BusinessException;
import org.yuca.yuca.common.response.ErrorCode;
import org.yuca.yuca.note.dto.request.CreateItemRequest;
import org.yuca.yuca.note.dto.request.MoveItemRequest;
import org.yuca.yuca.note.dto.request.UpdateItemRequest;
import org.yuca.yuca.note.dto.request.AddTagsRequest;
import org.yuca.yuca.note.dto.response.NoteItemResponse;
import org.yuca.yuca.note.dto.response.NoteTreeResponse;
import org.yuca.yuca.note.dto.response.NoteTagResponse;
import org.yuca.yuca.note.entity.NoteBook;
import org.yuca.yuca.note.entity.NoteItem;
import org.yuca.yuca.note.entity.NoteItemTag;
import org.yuca.yuca.note.entity.NoteTag;
import org.yuca.yuca.note.enums.ItemType;
import org.yuca.yuca.note.mapper.NoteBookMapper;
import org.yuca.yuca.note.mapper.NoteItemMapper;
import org.yuca.yuca.note.mapper.NoteItemTagMapper;
import org.yuca.yuca.note.mapper.NoteTagMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点服务实现（单表设计：文件夹和文档统一管理）
 */
@Slf4j
@Service
public class NoteItemService extends ServiceImpl<NoteItemMapper, NoteItem> {

    @Autowired
    private NoteItemMapper noteItemMapper;

    @Autowired
    private NoteBookMapper noteBookMapper;

    @Autowired
    private NoteItemTagMapper noteItemTagMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    @Autowired
    private NoteTagService noteTagService;

    @Autowired
    private org.yuca.yuca.note.util.NoteDataIntegrityUtils dataIntegrityUtils;

    /**
     * 创建节点（文件夹或文档）
     *
     * @param request 创建请求
     * @param userId  用户ID
     * @return 节点ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createItem(CreateItemRequest request, Long userId) {
        // 验证笔记本存在且属于该用户
        NoteBook noteBook = dataIntegrityUtils.validateNoteBook(request.getBookId(), userId);

        // 验证节点类型
        try {
            ItemType.fromCode(request.getType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的节点类型");
        }

        // 验证父节点
        dataIntegrityUtils.validateParentNode(request.getParentId(), request.getBookId(), userId, null);

        // 构建节点
        NoteItem.NoteItemBuilder builder = NoteItem.builder()
                .userId(userId)
                .bookId(request.getBookId())
                .parentId(request.getParentId())
                .type(request.getType())
                .title(request.getTitle())
                .icon(request.getIcon())
                .sortOrder(0)
                .isPinned(false);

        // 文档专用字段
        if (ItemType.DOCUMENT.getCode().equals(request.getType())) {
            builder.content(request.getContent())
                    .contentType(request.getContentType())
                    .summary(request.getSummary())
                    .status("DRAFT")
                    .viewCount(0)
                    .wordCount(calculateWordCount(request.getContent()));
        } else {
            // 文件夹专用字段
            builder.childCount(0);
        }

        NoteItem item = builder.build();

        noteItemMapper.insert(item);

        // 更新父节点的子项计数
        dataIntegrityUtils.updateParentChildCount(request.getParentId());

        // 更新笔记本的节点计数
        dataIntegrityUtils.updateBookItemCount(request.getBookId());

        log.info("创建节点成功: userId={}, itemId={}, type={}, title={}",
                userId, item.getId(), request.getType(), request.getTitle());

        return item.getId();
    }

    /**
     * 更新节点
     *
     * @param itemId  节点ID
     * @param request 更新请求
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long itemId, UpdateItemRequest request, Long userId) {
        // 验证节点存在且属于该用户
        NoteItem item = dataIntegrityUtils.validateNoteItem(itemId, userId);

        // 更新通用字段
        if (request.getTitle() != null) {
            item.setTitle(request.getTitle());
        }

        if (request.getIcon() != null) {
            item.setIcon(request.getIcon());
        }

        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }

        if (request.getIsPinned() != null) {
            item.setIsPinned(request.getIsPinned());
        }

        // 更新文档专用字段
        if (item.isDocument()) {
            if (request.getContent() != null) {
                item.setContent(request.getContent());
                item.setWordCount(calculateWordCount(request.getContent()));
            }

            if (request.getContentType() != null) {
                item.setContentType(request.getContentType());
            }

            if (request.getSummary() != null) {
                item.setSummary(request.getSummary());
            }

            if (request.getStatus() != null) {
                item.setStatus(request.getStatus());
            }

            if (request.getWordCount() != null) {
                item.setWordCount(request.getWordCount());
            }
        }

        noteItemMapper.updateById(item);

        log.info("更新节点成功: itemId={}", itemId);
    }

    /**
     * 删除节点（逻辑删除）
     *
     * @param itemId 节点ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId, Long userId) {
        // 验证节点存在且属于该用户
        NoteItem item = dataIntegrityUtils.validateNoteItem(itemId, userId);

        // 如果是文件夹且有子节点，使用级联删除
        if (item.isFolder() && item.getChildCount() != null && item.getChildCount() > 0) {
            // 使用级联删除
            dataIntegrityUtils.cascadeDeleteNoteItem(itemId, userId);
        } else {
            // 逻辑删除单个节点
            noteItemMapper.deleteById(itemId);

            // 删除标签关联
            LambdaQueryWrapper<NoteItemTag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteItemTag::getItemId, itemId);
            noteItemTagMapper.delete(wrapper);
        }

        // 更新父节点的子项计数
        dataIntegrityUtils.updateParentChildCount(item.getParentId());

        // 更新笔记本的节点计数
        dataIntegrityUtils.updateBookItemCount(item.getBookId());

        log.info("删除节点成功: itemId={}", itemId);
    }

    /**
     * 移动节点
     *
     * @param itemId  节点ID
     * @param request 移动请求
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveItem(Long itemId, MoveItemRequest request, Long userId) {
        // 验证节点存在且属于该用户
        NoteItem item = dataIntegrityUtils.validateNoteItem(itemId, userId);

        // 验证新父节点
        dataIntegrityUtils.validateParentNode(request.getParentId(), item.getBookId(), userId, itemId);

        // 更新旧父节点的子项计数
        Long oldParentId = item.getParentId();
        if (oldParentId != null) {
            dataIntegrityUtils.updateParentChildCount(oldParentId);
        }

        // 更新节点
        item.setParentId(request.getParentId());
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }
        noteItemMapper.updateById(item);

        // 更新新父节点的子项计数
        dataIntegrityUtils.updateParentChildCount(request.getParentId());

        log.info("移动节点成功: itemId={}, newParentId={}", itemId, request.getParentId());
    }

    /**
     * 获取节点详情
     *
     * @param itemId 节点ID
     * @param userId 用户ID
     * @return 节点响应
     */
    public NoteItemResponse getItem(Long itemId, Long userId) {
        NoteItem item = dataIntegrityUtils.validateNoteItem(itemId, userId);
        return convertToResponse(item);
    }

    /**
     * 获取直接子节点列表
     *
     * @param parentId 父节点ID（NULL表示笔记本根目录）
     * @param bookId   笔记本ID
     * @param userId   用户ID
     * @return 子节点列表
     */
    public List<NoteItemResponse> getChildren(Long parentId, Long bookId, Long userId) {
        // 验证笔记本权限
        dataIntegrityUtils.validateNoteBook(bookId, userId);

        List<NoteItem> items = noteItemMapper.getChildren(parentId, bookId);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取笔记本的完整树形结构
     *
     * @param bookId 笔记本ID
     * @param userId 用户ID
     * @return 树形结构
     */
    public NoteTreeResponse getTree(Long bookId, Long userId) {
        // 验证笔记本权限
        NoteBook noteBook = dataIntegrityUtils.validateNoteBook(bookId, userId);

        // 获取所有节点
        List<NoteItem> allItems = noteItemMapper.getTreeByBookId(bookId);

        // 构建树形结构
        NoteTreeResponse response = new NoteTreeResponse();
        response.setId(noteBook.getId());
        response.setName(noteBook.getName());
        response.setNodes(buildTree(allItems, null));

        return response;
    }

    /**
     * 构建树形结构
     */
    private List<NoteTreeResponse.NoteItemTreeNode> buildTree(List<NoteItem> allItems, Long parentId) {
        List<NoteTreeResponse.NoteItemTreeNode> result = new ArrayList<>();

        for (NoteItem item : allItems) {
            if ((parentId == null && item.getParentId() == null) ||
                    (parentId != null && parentId.equals(item.getParentId()))) {
                NoteTreeResponse.NoteItemTreeNode node = new NoteTreeResponse.NoteItemTreeNode();
                node.setId(item.getId());
                node.setParentId(item.getParentId());
                node.setType(item.getType());
                node.setTitle(item.getTitle());
                node.setIcon(item.getIcon());
                node.setSortOrder(item.getSortOrder());
                node.setIsPinned(item.getIsPinned());
                node.setChildCount(item.getChildCount());

                // 递归构建子节点
                if (item.isFolder()) {
                    node.setChildren(buildTree(allItems, item.getId()));
                }

                result.add(node);
            }
        }

        return result;
    }

    /**
     * 获取最近编辑的文档
     *
     * @param userId 用户ID
     * @param limit  数量限制
     * @return 文档列表
     */
    public List<NoteItemResponse> getRecentDocuments(Long userId, int limit) {
        List<NoteItem> items = noteItemMapper.getRecentDocuments(userId, limit);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取置顶文档
     *
     * @param userId 用户ID
     * @return 文档列表
     */
    public List<NoteItemResponse> getPinnedDocuments(Long userId) {
        List<NoteItem> items = noteItemMapper.getPinnedDocuments(userId);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 计算字数
     */
    private Integer calculateWordCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.length();
    }

    // ========== 标签关联功能 ==========

    /**
     * 给文档添加标签
     *
     * @param itemId  节点ID（仅文档）
     * @param request 添加标签请求
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void addTags(Long itemId, AddTagsRequest request, Long userId) {
        // 验证节点存在且属于该用户
        NoteItem item = dataIntegrityUtils.validateNoteItem(itemId, userId);

        if (!item.isDocument()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有文档可以添加标签");
        }

        // 获取已关联的标签ID
        List<Long> existingTagIds = noteItemTagMapper.getTagIdsByItemId(itemId);

        // 添加新标签
        for (Long tagId : request.getTagIds()) {
            if (!existingTagIds.contains(tagId)) {
                // 验证标签存在且属于该用户
                dataIntegrityUtils.validateNoteTag(tagId, userId);

                // 创建关联
                NoteItemTag itemTag = NoteItemTag.builder()
                        .itemId(itemId)
                        .tagId(tagId)
                        .build();
                noteItemTagMapper.insert(itemTag);

                // 增加标签使用次数
                noteTagService.incrementUseCount(tagId);
            }
        }

        log.info("给文档添加标签成功: itemId={}, tagIds={}", itemId, request.getTagIds());
    }

    /**
     * 移除文档标签
     *
     * @param itemId 节点ID
     * @param tagId  标签ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeTag(Long itemId, Long tagId, Long userId) {
        // 验证节点存在且属于该用户
        dataIntegrityUtils.validateNoteItem(itemId, userId);

        // 查询关联记录
        LambdaQueryWrapper<NoteItemTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteItemTag::getItemId, itemId)
                .eq(NoteItemTag::getTagId, tagId);

        NoteItemTag itemTag = noteItemTagMapper.selectOne(wrapper);
        if (itemTag != null) {
            noteItemTagMapper.deleteById(itemTag.getId());

            // 减少标签使用次数
            noteTagService.decrementUseCount(tagId);
        }

        log.info("移除文档标签成功: itemId={}, tagId={}", itemId, tagId);
    }

    /**
     * 获取文档的标签列表
     *
     * @param itemId 节点ID
     * @param userId 用户ID
     * @return 标签列表
     */
    public List<NoteTagResponse> getItemTags(Long itemId, Long userId) {
        // 验证节点存在且属于该用户
        NoteItem item = dataIntegrityUtils.validateNoteItem(itemId, userId);

        if (!item.isDocument()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有文档有标签");
        }

        // 获取标签ID列表
        List<Long> tagIds = noteItemTagMapper.getTagIdsByItemId(itemId);

        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询标签
        LambdaQueryWrapper<NoteTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(NoteTag::getId, tagIds)
                .eq(NoteTag::getUserId, userId)
                .orderByAsc(NoteTag::getName);

        List<NoteTag> tags = noteTagMapper.selectList(wrapper);

        return tags.stream()
                .map(tag -> {
                    NoteTagResponse response = new NoteTagResponse();
                    BeanUtils.copyProperties(tag, response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取标签下的所有文档
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @return 文档列表
     */
    public List<NoteItemResponse> getDocumentsByTag(Long tagId, Long userId) {
        // 验证标签存在且属于该用户
        dataIntegrityUtils.validateNoteTag(tagId, userId);

        // 获取标签下的文档ID列表
        List<Long> itemIds = noteItemTagMapper.getItemIdsByTagId(tagId);

        if (itemIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询文档
        LambdaQueryWrapper<NoteItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(NoteItem::getId, itemIds)
                .eq(NoteItem::getUserId, userId)
                .eq(NoteItem::getType, ItemType.DOCUMENT.getCode())
                .orderByDesc(NoteItem::getUpdatedAt);

        List<NoteItem> items = noteItemMapper.selectList(wrapper);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 批量更新节点排序
     *
     * @param items 节点排序列表（包含id和sortOrder）
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSort(List<NoteItem> items, Long userId) {
        for (NoteItem item : items) {
            NoteItem existingItem = noteItemMapper.selectById(item.getId());

            if (existingItem == null) {
                continue;
            }

            if (!existingItem.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此节点");
            }

            existingItem.setSortOrder(item.getSortOrder());
            noteItemMapper.updateById(existingItem);
        }

        log.info("批量更新节点排序成功: count={}", items.size());
    }

    /**
     * 转换为响应DTO
     */
    private NoteItemResponse convertToResponse(NoteItem item) {
        NoteItemResponse response = new NoteItemResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }
}
