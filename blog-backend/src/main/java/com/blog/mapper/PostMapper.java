package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /** 从 likes 表统计点赞数 */
    @Select("SELECT COUNT(*) FROM likes WHERE post_id = #{postId} AND liked = 1")
    int selectCountLikes(@Param("postId") Long postId);

    /**
     * 查询关联某标签的文章（已发布，按创建时间降序）。
     * 必须带可见性条件：之前只过滤 status，私有/隐藏文章会从这里泄漏。
     * isAdmin 为 1 时不过滤 is_hidden；userId 为空时只看公开文章。
     */
    @Select("<script>"
            + "SELECT p.* FROM posts p INNER JOIN post_tag pt ON p.id = pt.post_id "
            + "WHERE pt.tag_id = #{tagId} AND p.status = 1 "
            + "<if test='isAdmin == null or isAdmin == 0'> AND p.is_hidden = 0 </if>"
            + "<choose>"
            + "  <when test='userId != null'> AND (p.is_private = 0 OR p.author_id = #{userId}) </when>"
            + "  <otherwise> AND p.is_private = 0 </otherwise>"
            + "</choose>"
            + "ORDER BY p.created_at DESC"
            + "</script>")
    IPage<Post> selectTagPosts(IPage<?> page, @Param("tagId") Long tagId,
                               @Param("userId") Long userId, @Param("isAdmin") Integer isAdmin);
}
