package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.dto.AlbumDTO;
import com.blog.dto.AlbumResponse;
import com.blog.entity.Album;

public interface AlbumService extends IService<Album> {

    IPage<AlbumResponse> listAlbums(int page, int size, Long authorId);

    AlbumResponse getAlbumDetail(Long id);

    Album createAlbum(AlbumDTO dto, Long authorId);

    Album updateAlbum(Long id, AlbumDTO dto, Long userId, String role);

    void deleteAlbum(Long id, Long userId, String role);
}
