package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    /** 查询某个访客对某篇文章的点赞记录 */
    @Select("SELECT * FROM likes WHERE post_id = #{postId} AND visitor_id = #{visitorId} LIMIT 1")
    Like findByPostIdAndVisitorId(@Param("postId") Long postId, @Param("visitorId") String visitorId);

    /** 统计某篇文章的点赞数 */
    @Select("SELECT COUNT(*) FROM likes WHERE post_id = #{postId} AND liked = 1")
    int countByPostId(@Param("postId") Long postId);
}
