package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ReactionResponse;
import com.blog.entity.Comment;
import com.blog.entity.CommentReaction;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.CommentReactionMapper;
import com.blog.service.BadgeService;
import com.blog.service.CommentReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentReactionServiceImpl implements CommentReactionService {

    private final CommentReactionMapper reactionMapper;

    private final CommentMapper commentMapper;

    private final BadgeService badgeService;

    @Override
    @Transactional
    public void react(Long commentId, Long userId, String ip, Integer reactionType) {
        if (reactionType == null || (reactionType != 0 && reactionType != 1)) {
            throw new com.blog.exception.BusinessException(400, "反应类型非法，只能是 1=赞 / 0=踩");
        }
        // 登录用户按 user_id 去重，ip 必须留空：
        // uk_ip_comment(comment_id, ip) 对所有行生效，若登录用户也写 ip，
        // 同一出口 IP（NAT/公司网）下第二个用户点同一条评论会撞唯一键直接 500。
        String dedupIp = (userId != null) ? null : ip;

        LambdaQueryWrapper<CommentReaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentReaction::getCommentId, commentId);
        if (userId != null) {
            wrapper.eq(CommentReaction::getUserId, userId);
        } else {
            wrapper.isNull(CommentReaction::getUserId)
                   .eq(CommentReaction::getIp, dedupIp);
        }

        CommentReaction existing = reactionMapper.selectOne(wrapper);

        if (existing != null) {
            if (existing.getReactionType().equals(reactionType)) {
                // 同类型，取消反应
                reactionMapper.deleteById(existing.getId());
            } else {
                // 不同类型，切换
                existing.setReactionType(reactionType);
                reactionMapper.updateById(existing);
            }
        } else {
            // 新增反应
            CommentReaction reaction = new CommentReaction();
            reaction.setCommentId(commentId);
            reaction.setUserId(userId);
            reaction.setIp(dedupIp);
            reaction.setReactionType(reactionType);
            try {
                reactionMapper.insert(reaction);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发双击/重复提交：另一请求已插入，视为已完成
                return;
            }
        }

        // 检查徽章（评论被点赞）
        if (reactionType == 1) {
            Comment comment = commentMapper.selectById(commentId);
            if (comment != null && comment.getUserId() != null) {
                badgeService.checkAndGrant(comment.getUserId(), "COMMENT_LIKED");
            }
        }
    }

    @Override
    @Transactional
    public void removeReaction(Long commentId, Long userId, String ip) {
        LambdaQueryWrapper<CommentReaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentReaction::getCommentId, commentId);
        if (userId != null) {
            wrapper.eq(CommentReaction::getUserId, userId);
        } else {
            wrapper.isNull(CommentReaction::getUserId)
                   .eq(CommentReaction::getIp, ip);
        }
        reactionMapper.delete(wrapper);
    }

    @Override
    public ReactionResponse getReactions(Long commentId, Long userId, String ip) {
        ReactionResponse response = new ReactionResponse();

        // 统计点赞数
        LambdaQueryWrapper<CommentReaction> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(CommentReaction::getCommentId, commentId)
                   .eq(CommentReaction::getReactionType, 1);
        Long likeCount = reactionMapper.selectCount(likeWrapper);
        response.setLikeCount(likeCount != null ? likeCount.intValue() : 0);

        // 统计点踩数
        LambdaQueryWrapper<CommentReaction> dislikeWrapper = new LambdaQueryWrapper<>();
        dislikeWrapper.eq(CommentReaction::getCommentId, commentId)
                      .eq(CommentReaction::getReactionType, 0);
        Long dislikeCount = reactionMapper.selectCount(dislikeWrapper);
        response.setDislikeCount(dislikeCount != null ? dislikeCount.intValue() : 0);

        // 查询当前用户的反应
        LambdaQueryWrapper<CommentReaction> myWrapper = new LambdaQueryWrapper<>();
        myWrapper.eq(CommentReaction::getCommentId, commentId);
        if (userId != null) {
            myWrapper.eq(CommentReaction::getUserId, userId);
        } else {
            myWrapper.isNull(CommentReaction::getUserId)
                     .eq(CommentReaction::getIp, ip);
        }
        CommentReaction myReaction = reactionMapper.selectOne(myWrapper);
        response.setMyReaction(myReaction != null ? myReaction.getReactionType() : null);

        return response;
    }
}
