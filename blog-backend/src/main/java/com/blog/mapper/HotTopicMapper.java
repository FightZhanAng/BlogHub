package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.HotTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HotTopicMapper extends BaseMapper<HotTopic> {

    @Select("SELECT DISTINCT platform FROM hot_topics WHERE fetched_at = #{date} ORDER BY platform")
    List<String> findPlatformsByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM hot_topics WHERE platform = #{platform} AND fetched_at = #{date} ORDER BY rank_num ASC")
    List<HotTopic> findByPlatformAndDate(@Param("platform") String platform, @Param("date") LocalDate date);
}
