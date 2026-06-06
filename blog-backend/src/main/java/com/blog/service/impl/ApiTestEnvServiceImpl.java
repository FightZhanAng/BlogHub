package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ApiTestEnvDTO;
import com.blog.entity.ApiTestEnv;
import com.blog.exception.BusinessException;
import com.blog.mapper.ApiTestEnvMapper;
import com.blog.service.ApiTestEnvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTestEnvServiceImpl implements ApiTestEnvService {

    private final ApiTestEnvMapper envMapper;

    @Override
    public List<ApiTestEnv> listByUser(Long userId) {
        return envMapper.selectList(
            new LambdaQueryWrapper<ApiTestEnv>()
                .eq(ApiTestEnv::getUserId, userId)
                .orderByAsc(ApiTestEnv::getName)
        );
    }

    @Override
    public ApiTestEnv create(ApiTestEnvDTO dto, Long userId) {
        ApiTestEnv env = new ApiTestEnv();
        env.setUserId(userId);
        env.setName(dto.getName());
        env.setValue(dto.getValue());
        envMapper.insert(env);
        log.info("创建 API 测试环境变量: id={}, name={}", env.getId(), dto.getName());
        return env;
    }

    @Override
    public ApiTestEnv update(Long id, ApiTestEnvDTO dto, Long userId) {
        ApiTestEnv env = envMapper.selectById(id);
        if (env == null || !env.getUserId().equals(userId)) {
            throw new BusinessException(404, "环境变量不存在");
        }
        env.setName(dto.getName());
        env.setValue(dto.getValue());
        envMapper.updateById(env);
        return env;
    }

    @Override
    public void delete(Long id, Long userId) {
        ApiTestEnv env = envMapper.selectById(id);
        if (env == null || !env.getUserId().equals(userId)) {
            throw new BusinessException(404, "环境变量不存在");
        }
        envMapper.deleteById(id);
        log.info("删除 API 测试环境变量: id={}", id);
    }
}
