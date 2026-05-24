package com.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.entity.Like;
import com.blog.mapper.LikeMapper;
import com.blog.service.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

    @Override
    @Transactional
    public boolean toggleLike(Long postId, String visitorId) {
        Like record = baseMapper.findByPostIdAndVisitorId(postId, visitorId);

        if (record == null) {
            log.info("首次点赞: postId={}, visitorId={}", postId, visitorId);
            record = new Like();
            record.setPostId(postId);
            record.setVisitorId(visitorId);
            record.setLiked(1);
            baseMapper.insert(record);
            return true;
        }

        int newStatus = record.getLiked() == 1 ? 0 : 1;
        String action = newStatus == 1 ? "点赞" : "取消点赞";
        log.info("{}: postId={}, visitorId={}", action, postId, visitorId);
        record.setLiked(newStatus);
        baseMapper.updateById(record);
        return newStatus == 1;
    }

    @Override
    public boolean isLiked(Long postId, String visitorId) {
        Like record = baseMapper.findByPostIdAndVisitorId(postId, visitorId);
        return record != null && record.getLiked() == 1;
    }

    @Override
    public int getLikeCount(Long postId) {
        return baseMapper.countByPostId(postId);
    }
}
