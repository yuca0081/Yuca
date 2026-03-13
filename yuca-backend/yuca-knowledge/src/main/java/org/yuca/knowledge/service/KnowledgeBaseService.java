package org.yuca.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.yuca.knowledge.dto.request.CreateKnowledgeBaseRequest;
import org.yuca.knowledge.dto.request.UpdateKnowledgeBaseRequest;
import org.yuca.knowledge.dto.response.KnowledgeBaseResponse;
import org.yuca.knowledge.mapper.KnowledgeBaseMapper;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.knowledge.entity.KnowledgeBase;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Service
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {

    /**
     * 创建知识库
     *
     * @param request    创建请求
     * @param userId     用户ID
     * @return 知识库ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(CreateKnowledgeBaseRequest request, Long userId) {
        // 检查同名知识库
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getName, request.getName());

        Long count = this.baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库名称已存在");
        }

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());

        this.baseMapper.insert(knowledgeBase);

        log.info("创建知识库成功: userId={}, kbId={}, name={}", userId, knowledgeBase.getId(), request.getName());
        return knowledgeBase.getId();
    }

    /**
     * 更新知识库
     *
     * @param request 更新请求
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(UpdateKnowledgeBaseRequest request, Long userId) {
        KnowledgeBase knowledgeBase = this.baseMapper.selectById(request.getId());

        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不存在");
        }

        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此知识库");
        }

        // 检查名称冲突（排除自己）
        if (request.getName() != null && !request.getName().equals(knowledgeBase.getName())) {
            LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeBase::getUserId, userId)
                    .eq(KnowledgeBase::getName, request.getName())
                    .ne(KnowledgeBase::getId, request.getId());

            Long count = this.baseMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库名称已存在");
            }

            knowledgeBase.setName(request.getName());
        }

        if (request.getDescription() != null) {
            knowledgeBase.setDescription(request.getDescription());
        }

        this.baseMapper.updateById(knowledgeBase);

        log.info("更新知识库成功: kbId={}", request.getId());
    }

    /**
     * 删除知识库（逻辑删除）
     *
     * @param kbId   知识库ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long kbId, Long userId) {
        KnowledgeBase knowledgeBase = this.baseMapper.selectById(kbId);

        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不存在");
        }

        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此知识库");
        }

        // 逻辑删除
        this.baseMapper.deleteById(kbId);

        log.info("删除知识库成功: kbId={}", kbId);
    }

    /**
     * 获取知识库详情
     *
     * @param kbId   知识库ID
     * @param userId 用户ID
     * @return 知识库响应
     */
    public KnowledgeBaseResponse getKnowledgeBase(Long kbId, Long userId) {
        KnowledgeBase knowledgeBase = this.baseMapper.selectById(kbId);

        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不存在");
        }

        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此知识库");
        }

        return convertToResponse(knowledgeBase);
    }

    /**
     * 获取用户的知识库列表
     *
     * @param userId 用户ID
     * @return 知识库列表
     */
    public List<KnowledgeBaseResponse> listKnowledgeBases(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId)
                .orderByDesc(KnowledgeBase::getUpdatedAt);

        List<KnowledgeBase> knowledgeBases = this.baseMapper.selectList(wrapper);

        return knowledgeBases.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应DTO
     */
    private KnowledgeBaseResponse convertToResponse(KnowledgeBase knowledgeBase) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        BeanUtils.copyProperties(knowledgeBase, response);
        return response;
    }
}
