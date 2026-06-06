package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ApiTestCollectionDTO;
import com.blog.entity.ApiTestCollection;
import com.blog.entity.ApiTestRequest;
import com.blog.exception.BusinessException;
import com.blog.mapper.ApiTestCollectionMapper;
import com.blog.mapper.ApiTestRequestMapper;
import com.blog.service.ApiTestCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTestCollectionServiceImpl implements ApiTestCollectionService {

    private final ApiTestCollectionMapper collectionMapper;
    private final ApiTestRequestMapper requestMapper;

    @Override
    public List<ApiTestCollection> listByUser(Long userId) {
        return collectionMapper.selectList(
            new LambdaQueryWrapper<ApiTestCollection>()
                .eq(ApiTestCollection::getUserId, userId)
                .orderByDesc(ApiTestCollection::getUpdatedAt)
        );
    }

    @Override
    public ApiTestCollection create(ApiTestCollectionDTO dto, Long userId) {
        ApiTestCollection collection = new ApiTestCollection();
        collection.setName(dto.getName());
        collection.setDescription(dto.getDescription());
        collection.setUserId(userId);
        collection.setBaseUrl(dto.getBaseUrl());
        collectionMapper.insert(collection);
        log.info("创建 API 测试集合: id={}, name={}", collection.getId(), dto.getName());
        return collection;
    }

    @Override
    public ApiTestCollection update(Long id, ApiTestCollectionDTO dto, Long userId) {
        ApiTestCollection collection = collectionMapper.selectById(id);
        if (collection == null || !collection.getUserId().equals(userId)) {
            throw new BusinessException(404, "集合不存在");
        }
        collection.setName(dto.getName());
        collection.setDescription(dto.getDescription());
        collection.setBaseUrl(dto.getBaseUrl());
        collectionMapper.updateById(collection);
        return collection;
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        ApiTestCollection collection = collectionMapper.selectById(id);
        if (collection == null || !collection.getUserId().equals(userId)) {
            throw new BusinessException(404, "集合不存在");
        }
        // 删除集合下的所有请求
        requestMapper.delete(
            new LambdaQueryWrapper<ApiTestRequest>()
                .eq(ApiTestRequest::getCollectionId, id)
        );
        collectionMapper.deleteById(id);
        log.info("删除 API 测试集合: id={}", id);
    }
}
