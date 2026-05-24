package com.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.entity.Bookmark;
import com.blog.mapper.BookmarkMapper;
import com.blog.service.BookmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookmarkServiceImpl extends ServiceImpl<BookmarkMapper, Bookmark> implements BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkServiceImpl.class);

    @Override
    @Transactional
    public boolean toggleBookmark(Long postId, String visitorId) {
        Bookmark record = baseMapper.findByPostIdAndVisitorId(postId, visitorId);

        if (record == null) {
            log.info("添加收藏: postId={}, visitorId={}", postId, visitorId);
            record = new Bookmark();
            record.setPostId(postId);
            record.setVisitorId(visitorId);
            baseMapper.insert(record);
            return true;
        }

        log.info("取消收藏: postId={}, visitorId={}", postId, visitorId);
        baseMapper.deleteById(record.getId());
        return false;
    }

    @Override
    public boolean isBookmarked(Long postId, String visitorId) {
        return baseMapper.findByPostIdAndVisitorId(postId, visitorId) != null;
    }

    @Override
    public List<Long> getBookmarkedPostIds(String visitorId) {
        log.debug("查询访客收藏列表: visitorId={}", visitorId);
        return baseMapper.findPostIdsByVisitorId(visitorId);
    }
}
