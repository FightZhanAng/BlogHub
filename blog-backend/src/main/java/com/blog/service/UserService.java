package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.common.PageResult;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /** 登录 */
    LoginResponse login(LoginRequest request);

    /** 注册 */
    LoginResponse register(LoginRequest request);

    /** 分页查询用户（管理员，支持关键词搜索） */
    IPage<User> getUsers(int page, int size, String keyword);

    /** 根据用户名查询 */
    User findByUsername(String username);

    /** 获取用户信息（脱敏） */
    User getUserById(Long userId);

    /** 更新个人信息 */
    User updateProfile(Long userId, String nickname, String avatar);

    /** 修改密码 */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /** 管理员创建用户 */
    User createUser(String username, String password, String nickname, String role);

    /** 管理员更新用户 */
    User updateUser(Long id, Map<String, String> body);

    /** 获取我的评论 */
    PageResult<Map<String, Object>> getMyComments(Long userId, int page, int size);

    /** 搜索用户（@提及用） */
    java.util.List<User> searchByUsername(String q, int limit);
}
