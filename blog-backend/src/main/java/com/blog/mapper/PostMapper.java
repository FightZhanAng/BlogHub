package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /** 从 likes 表统计点赞数 */
    @Select("SELECT COUNT(*) FROM likes WHERE post_id = #{postId} AND liked = 1")
    int selectCountLikes(@Param("postId") Long postId);

    /** 查询关联某标签的文章（已发布，按创建时间降序） */
    @Select("SELECT p.* FROM posts p INNER JOIN post_tag pt ON p.id = pt.post_id WHERE pt.tag_id = #{tagId} AND p.status = 1 ORDER BY p.created_at DESC")
    IPage<Post> selectTagPosts(IPage<?> page, @Param("tagId") Long tagId);
}
