package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.entity.Bookmark;

import java.util.List;

public interface BookmarkService extends IService<Bookmark> {

    /** 切换收藏状态 */
    boolean toggleBookmark(Long postId, String visitorId);

    /** 查询是否已收藏 */
    boolean isBookmarked(Long postId, String visitorId);

    /** 获取访客收藏的文章ID列表 */
    List<Long> getBookmarkedPostIds(String visitorId);
}
