package org.yuca.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.yuca.note.dto.request.CreateNoteBookRequest;
import org.yuca.note.dto.request.UpdateNoteBookRequest;
import org.yuca.note.dto.response.NoteBookResponse;
import org.yuca.note.entity.NoteItem;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.note.entity.NoteBook;
import org.yuca.note.mapper.NoteBookMapper;
import org.yuca.note.util.NoteDataIntegrityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记本服务实现
 */
@Slf4j
@Service
public class NoteBookService extends ServiceImpl<NoteBookMapper, NoteBook> {

    @Autowired
    private NoteDataIntegrityUtils dataIntegrityUtils;

    /**
     * 创建笔记本
     *
     * @param request 创建请求
     * @param userId  用户ID
     * @return 笔记本ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createNoteBook(CreateNoteBookRequest request, Long userId) {
        // 检查同名笔记本
        LambdaQueryWrapper<NoteBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteBook::getUserId, userId)
                .eq(NoteBook::getName, request.getName());

        Long count = this.baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "笔记本名称已存在");
        }

        NoteBook noteBook = NoteBook.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .sortOrder(0)
                .isDefault(false)
                .itemCount(0)
                .build();

        this.baseMapper.insert(noteBook);

        log.info("创建笔记本成功: userId={}, bookId={}, name={}", userId, noteBook.getId(), request.getName());
        return noteBook.getId();
    }

    /**
     * 更新笔记本
     *
     * @param request 更新请求
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateNoteBook(UpdateNoteBookRequest request, Long userId) {
        NoteBook noteBook = this.baseMapper.selectById(request.getId());

        if (noteBook == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "笔记本不存在");
        }

        if (!noteBook.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此笔记本");
        }

        // 检查名称冲突（排除自己）
        if (request.getName() != null && !request.getName().equals(noteBook.getName())) {
            LambdaQueryWrapper<NoteBook> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteBook::getUserId, userId)
                    .eq(NoteBook::getName, request.getName())
                    .ne(NoteBook::getId, request.getId());

            Long count = this.baseMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "笔记本名称已存在");
            }

            noteBook.setName(request.getName());
        }

        if (request.getDescription() != null) {
            noteBook.setDescription(request.getDescription());
        }

        if (request.getIcon() != null) {
            noteBook.setIcon(request.getIcon());
        }

        if (request.getColor() != null) {
            noteBook.setColor(request.getColor());
        }

        this.baseMapper.updateById(noteBook);

        log.info("更新笔记本成功: bookId={}", request.getId());
    }

    /**
     * 删除笔记本（逻辑删除，级联删除所有节点）
     *
     * @param bookId 笔记本ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNoteBook(Long bookId, Long userId) {
        // 验证笔记本存在且属于该用户
        NoteBook noteBook = dataIntegrityUtils.validateNoteBook(bookId, userId);

        // 检查笔记本是否有节点
        List<NoteItem> items = dataIntegrityUtils.getItemsByBookId(bookId);
        if (!items.isEmpty()) {
            // 使用级联删除
            dataIntegrityUtils.cascadeDeleteNoteBook(bookId, userId);
        } else {
            // 直接删除笔记本
            this.baseMapper.deleteById(bookId);
        }

        log.info("删除笔记本成功: bookId={}, itemCount={}", bookId, items.size());
    }

    /**
     * 获取笔记本详情
     *
     * @param bookId 笔记本ID
     * @param userId 用户ID
     * @return 笔记本响应
     */
    public NoteBookResponse getNoteBook(Long bookId, Long userId) {
        NoteBook noteBook = this.baseMapper.selectById(bookId);

        if (noteBook == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "笔记本不存在");
        }

        if (!noteBook.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此笔记本");
        }

        return convertToResponse(noteBook);
    }

    /**
     * 获取用户的笔记本列表
     *
     * @param userId 用户ID
     * @return 笔记本列表
     */
    public List<NoteBookResponse> listNoteBooks(Long userId) {
        LambdaQueryWrapper<NoteBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteBook::getUserId, userId)
                .orderByDesc(NoteBook::getIsDefault)
                .orderByAsc(NoteBook::getSortOrder)
                .orderByDesc(NoteBook::getUpdatedAt);

        List<NoteBook> noteBooks = this.baseMapper.selectList(wrapper);

        return noteBooks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 设置默认笔记本
     *
     * @param bookId 笔记本ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void setAsDefault(Long bookId, Long userId) {
        NoteBook noteBook = this.baseMapper.selectById(bookId);

        if (noteBook == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "笔记本不存在");
        }

        if (!noteBook.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此笔记本");
        }

        // 取消其他笔记本的默认状态
        LambdaQueryWrapper<NoteBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteBook::getUserId, userId)
                .eq(NoteBook::getIsDefault, true);

        List<NoteBook> defaultBooks = this.baseMapper.selectList(wrapper);
        for (NoteBook book : defaultBooks) {
            book.setIsDefault(false);
            this.baseMapper.updateById(book);
        }

        // 设置新的默认笔记本
        noteBook.setAsDefault();
        this.baseMapper.updateById(noteBook);

        log.info("设置默认笔记本成功: bookId={}", bookId);
    }

    /**
     * 转换为响应DTO
     */
    private NoteBookResponse convertToResponse(NoteBook noteBook) {
        NoteBookResponse response = new NoteBookResponse();
        BeanUtils.copyProperties(noteBook, response);
        return response;
    }
}
