package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.common.PageResult;
import com.blog.entity.Comment;

import java.util.List;
import java.util.Map;

public interface CommentService extends IService<Comment> {

    /** 获取文章评论 */
    List<Map<String, Object>> getPostComments(String slug);

    /** 发表评论 */
    void addComment(String slug, String nickname, String content, Long parentId, Long userId);

    /** 点赞评论 */
    void likeComment(Long id);

    /** 踩评论 */
    void dislikeComment(Long id);

    /** 评论统计 */
    Map<String, Object> getStats();

    /** 分页查询所有评论（管理员） */
    PageResult<Map<String, Object>> getCommentList(int page, int size, String keyword, String nickname);

    /** 懒加载某条评论的子回复 */
    PageResult<Map<String, Object>> getCommentReplies(Long commentId, int page, int size);
}
