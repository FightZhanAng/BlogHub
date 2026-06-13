package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.entity.SensitiveWord;
import java.util.List;

public interface SensitiveWordService {
    IPage<SensitiveWord> list(int page, int size, String keyword);
    SensitiveWord add(SensitiveWord word);
    SensitiveWord update(Long id, SensitiveWord word);
    void delete(Long id);
    int batchAdd(List<SensitiveWord> words);
    void reload();
}
