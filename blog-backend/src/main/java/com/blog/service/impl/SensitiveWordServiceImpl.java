package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.SensitiveWordFilter;
import com.blog.entity.SensitiveWord;
import com.blog.exception.BusinessException;
import com.blog.mapper.SensitiveWordMapper;
import com.blog.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordServiceImpl.class);

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public IPage<SensitiveWord> list(int page, int size, String keyword) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<SensitiveWord>()
                .orderByDesc(SensitiveWord::getCreatedAt);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(SensitiveWord::getWord, keyword.trim());
        }
        return sensitiveWordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public SensitiveWord add(SensitiveWord word) {
        SensitiveWord existing = sensitiveWordMapper.selectOne(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getWord, word.getWord()));
        if (existing != null) {
            throw new BusinessException(400, "敏感词已存在");
        }
        if (word.getLevel() == null) word.setLevel(1);
        if (word.getEnabled() == null) word.setEnabled(1);
        sensitiveWordMapper.insert(word);
        reload();
        return word;
    }

    @Override
    public SensitiveWord update(Long id, SensitiveWord word) {
        SensitiveWord existing = sensitiveWordMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "敏感词不存在");
        if (word.getWord() != null) existing.setWord(word.getWord());
        if (word.getLevel() != null) existing.setLevel(word.getLevel());
        if (word.getEnabled() != null) existing.setEnabled(word.getEnabled());
        sensitiveWordMapper.updateById(existing);
        reload();
        return existing;
    }

    @Override
    public void delete(Long id) {
        sensitiveWordMapper.deleteById(id);
        reload();
    }

    @Override
    public int batchAdd(List<SensitiveWord> words) {
        int count = 0;
        for (SensitiveWord word : words) {
            SensitiveWord existing = sensitiveWordMapper.selectOne(
                    new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getWord, word.getWord()));
            if (existing != null) continue;
            if (word.getLevel() == null) word.setLevel(1);
            if (word.getEnabled() == null) word.setEnabled(1);
            sensitiveWordMapper.insert(word);
            count++;
        }
        if (count > 0) reload();
        return count;
    }

    @Override
    public void reload() {
        List<SensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getEnabled, 1));
        List<SensitiveWordFilter.SensitiveWordEntry> entries = words.stream()
                .map(w -> new SensitiveWordFilter.SensitiveWordEntry(w.getWord(), w.getLevel() != null ? w.getLevel() : 1))
                .collect(Collectors.toList());
        sensitiveWordFilter.init(entries);
        log.info("敏感词库已加载，共 {} 个词", entries.size());
    }
}
