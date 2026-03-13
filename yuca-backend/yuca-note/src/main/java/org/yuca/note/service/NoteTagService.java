package org.yuca.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.yuca.note.dto.request.CreateTagRequest;
import org.yuca.note.dto.request.UpdateTagRequest;
import org.yuca.note.dto.response.NoteTagResponse;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.note.entity.NoteTag;
import org.yuca.note.mapper.NoteTagMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yuca.note.util.NoteDataIntegrityUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 */
@Slf4j
@Service
public class NoteTagService extends ServiceImpl<NoteTagMapper, NoteTag> {

    @Autowired
    private NoteDataIntegrityUtils dataIntegrityUtils;

    /**
     * 创建标签
     *
     * @param request 创建请求
     * @param userId  用户ID
     * @return 标签ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(CreateTagRequest request, Long userId) {
        // 检查同名标签
        LambdaQueryWrapper<NoteTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteTag::getUserId, userId)
                .eq(NoteTag::getName, request.getName());

        Long count = this.baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签名称已存在");
        }

        NoteTag tag = NoteTag.builder()
                .userId(userId)
                .name(request.getName())
                .color(request.getColor())
                .useCount(0)
                .build();

        this.baseMapper.insert(tag);

        log.info("创建标签成功: userId={}, tagId={}, name={}", userId, tag.getId(), request.getName());
        return tag.getId();
    }

    /**
     * 更新标签
     *
     * @param request 更新请求
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(UpdateTagRequest request, Long userId) {
        // 验证标签存在且属于该用户
        NoteTag tag = dataIntegrityUtils.validateNoteTag(request.getId(), userId);

        // 检查名称冲突（排除自己）
        if (request.getName() != null && !request.getName().equals(tag.getName())) {
            LambdaQueryWrapper<NoteTag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteTag::getUserId, userId)
                    .eq(NoteTag::getName, request.getName())
                    .ne(NoteTag::getId, request.getId());

            Long count = this.baseMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "标签名称已存在");
            }

            tag.setName(request.getName());
        }

        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }

        this.baseMapper.updateById(tag);

        log.info("更新标签成功: tagId={}", request.getId());
    }

    /**
     * 删除标签（逻辑删除）
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId, Long userId) {
        // 验证标签存在且属于该用户
        dataIntegrityUtils.validateNoteTag(tagId, userId);

        // 逻辑删除
        this.baseMapper.deleteById(tagId);

        log.info("删除标签成功: tagId={}", tagId);
    }

    /**
     * 获取标签详情
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @return 标签响应
     */
    public NoteTagResponse getTag(Long tagId, Long userId) {
        NoteTag tag = dataIntegrityUtils.validateNoteTag(tagId, userId);
        return convertToResponse(tag);
    }

    /**
     * 获取用户的标签列表
     *
     * @param userId 用户ID
     * @return 标签列表
     */
    public List<NoteTagResponse> listTags(Long userId) {
        LambdaQueryWrapper<NoteTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteTag::getUserId, userId)
                .orderByDesc(NoteTag::getUseCount)
                .orderByAsc(NoteTag::getName);

        List<NoteTag> tags = this.baseMapper.selectList(wrapper);

        return tags.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 增加标签使用次数
     *
     * @param tagId 标签ID
     */
    public void incrementUseCount(Long tagId) {
        NoteTag tag = this.baseMapper.selectById(tagId);
        if (tag != null) {
            tag.incrementUseCount();
            this.baseMapper.updateById(tag);
        }
    }

    /**
     * 减少标签使用次数
     *
     * @param tagId 标签ID
     */
    public void decrementUseCount(Long tagId) {
        NoteTag tag = this.baseMapper.selectById(tagId);
        if (tag != null) {
            tag.decrementUseCount();
            this.baseMapper.updateById(tag);
        }
    }

    /**
     * 转换为响应DTO
     */
    private NoteTagResponse convertToResponse(NoteTag tag) {
        NoteTagResponse response = new NoteTagResponse();
        BeanUtils.copyProperties(tag, response);
        return response;
    }
}
