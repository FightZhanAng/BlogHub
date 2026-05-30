package com.blog.service;

import com.blog.dto.ReactionResponse;

public interface CommentReactionService {

    /**
     * 添加或切换评论反应（点赞/点踩）
     * @param commentId 评论 ID
     * @param userId 用户 ID（游客为 null）
     * @param ip 访客 IP
     * @param reactionType 1=赞, 0=踩
     */
    void react(Long commentId, Long userId, String ip, Integer reactionType);

    /**
     * 取消评论反应
     */
    void removeReaction(Long commentId, Long userId, String ip);

    /**
     * 获取评论反应统计
     */
    ReactionResponse getReactions(Long commentId, Long userId, String ip);
}
