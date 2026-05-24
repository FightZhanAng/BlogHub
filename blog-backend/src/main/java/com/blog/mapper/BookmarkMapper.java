package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Bookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookmarkMapper extends BaseMapper<Bookmark> {

    /** 查询某个访客对某篇文章的收藏记录 */
    @Select("SELECT * FROM bookmarks WHERE post_id = #{postId} AND visitor_id = #{visitorId} LIMIT 1")
    Bookmark findByPostIdAndVisitorId(@Param("postId") Long postId, @Param("visitorId") String visitorId);

    /** 查询某个访客收藏的所有文章ID */
    @Select("SELECT post_id FROM bookmarks WHERE visitor_id = #{visitorId} ORDER BY created_at DESC")
    List<Long> findPostIdsByVisitorId(@Param("visitorId") String visitorId);
}
