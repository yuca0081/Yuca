package org.yuca.yuca.note.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 笔记本树形结构响应DTO
 */
@Data
public class NoteTreeResponse {

    /**
     * 笔记本ID
     */
    private Long id;

    /**
     * 笔记本名称
     */
    private String name;

    /**
     * 节点树（根节点列表）
     */
    private List<NoteItemTreeNode> nodes;

    /**
     * 树节点
     */
    @Data
    public static class NoteItemTreeNode {

        /**
         * 节点ID
         */
        private Long id;

        /**
         * 父节点ID
         */
        private Long parentId;

        /**
         * 节点类型：FOLDER, DOCUMENT
         */
        private String type;

        /**
         * 标题
         */
        private String title;

        /**
         * 图标
         */
        private String icon;

        /**
         * 排序序号
         */
        private Integer sortOrder;

        /**
         * 是否置顶
         */
        private Boolean isPinned;

        /**
         * 子项数量（仅文件夹）
         */
        private Integer childCount;

        /**
         * 子节点列表（仅文件夹）
         */
        private List<NoteItemTreeNode> children;
    }
}
