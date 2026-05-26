package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Photo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {

    @Select("SELECT COUNT(*) FROM photos WHERE album_id = #{albumId}")
    int countByAlbumId(@Param("albumId") Long albumId);
}
