package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.entity.Like;

public interface LikeService extends IService<Like> {

    /** 点赞/取消点赞，返回当前是否已赞 */
    boolean toggleLike(Long postId, String visitorId);

    /** 查询访客是否已赞 */
    boolean isLiked(Long postId, String visitorId);

    /** 获取文章点赞数 */
    int getLikeCount(Long postId);
}
