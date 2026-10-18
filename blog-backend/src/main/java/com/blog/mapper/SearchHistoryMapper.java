package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {

    /**
     * 近 N 天的热门搜索词（按出现次数降序）。
     *
     * <p>只 select keyword 并聚合计数，符合 ONLY_FULL_GROUP_BY，不会再报 ERROR 1055。</p>
     */
    @Select("SELECT keyword FROM search_history "
            + "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) "
            + "AND keyword IS NOT NULL AND keyword <> '' "
            + "GROUP BY keyword ORDER BY COUNT(*) DESC, MAX(created_at) DESC "
            + "LIMIT #{limit}")
    List<String> selectHotKeywords(@Param("days") int days, @Param("limit") int limit);
}
