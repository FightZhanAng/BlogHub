package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.common.PageResult;
import com.blog.config.JwtUtil;
import com.blog.config.SecurityConfig;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.PostService;
import com.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final CommentMapper commentMapper;

    private final PostService postService;

    private String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = baseMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(400, "账号已被禁用");
        }

        // 兼容：先 BCrypt 校验，失败则回退 MD5
        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!matched) {
            // 尝试 MD5 兼容（旧密码格式）
            String md5 = DigestUtils.md5DigestAsHex((request.getPassword() + "BlogHub@2024").getBytes(StandardCharsets.UTF_8));
            if (md5.equals(user.getPassword())) {
                // 命中 MD5 → 自动升级为 BCrypt
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                baseMapper.updateById(user);
                matched = true;
            }
        }
        if (!matched) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功: username={}, role={}", user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole(), user.getId());
    }

    @Override
    public LoginResponse register(LoginRequest request) {
        User exist = baseMapper.findByUsername(request.getUsername());
        if (exist != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encryptPassword(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setRole("user");
        user.setStatus(1);
        baseMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("用户注册成功: username={}", user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole(), user.getId());
    }

    @Override
    public IPage<User> getUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public User findByUsername(String username) {
        return baseMapper.findByUsername(username);
    }

    @Override
    public User getUserById(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(Long userId, String nickname, String avatar) {
        log.info("更新个人信息: userId={}", userId);
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        baseMapper.updateById(user);
        user.setPassword(null);
        log.info("个人信息更新成功: userId={}", userId);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码: userId={}", userId);
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new BusinessException(400, "请输入原密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "新密码至少 6 位");
        }

        String encryptedOld = encryptPassword(oldPassword);
        if (!encryptedOld.equals(user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        user.setPassword(encryptPassword(newPassword));
        baseMapper.updateById(user);
        log.info("密码修改成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(String username, String password, String nickname, String role) {
        log.info("管理员创建用户: username={}", username);
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(role != null ? role : "user");
        user.setStatus(1);
        baseMapper.insert(user);
        user.setPassword(null);
        log.info("用户创建成功: id={}", user.getId());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(Long id, Map<String, String> body) {
        log.info("管理员更新用户: id={}", id);
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (body.containsKey("nickname")) user.setNickname(body.get("nickname"));
        if (body.containsKey("role")) user.setRole(body.get("role"));
        if (body.containsKey("status")) user.setStatus(Integer.parseInt(body.get("status")));
        if (body.containsKey("password")) {
            user.setPassword(encryptPassword(body.get("password")));
        }
        baseMapper.updateById(user);
        user.setPassword(null);
        log.info("用户更新成功: id={}", id);
        return user;
    }

    @Override
    public PageResult<Map<String, Object>> getMyComments(Long userId, int page, int size) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String nickname = user.getNickname();
        IPage<Comment> ipage = commentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getNickname, nickname)
                        .orderByDesc(Comment::getCreatedAt));

        PageResult<Map<String, Object>> result = new PageResult<>();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Comment c : ipage.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("postId", c.getPostId());
            m.put("content", c.getContent());
            m.put("createdAt", c.getCreatedAt());
            Post post = postService.getById(c.getPostId());
            m.put("postTitle", post != null ? post.getTitle() : "已删除");
            m.put("postSlug", post != null ? post.getSlug() : "");
            list.add(m);
        }
        result.setList(list);
        result.setTotal(ipage.getTotal());
        result.setPage(ipage.getCurrent());
        result.setSize(ipage.getSize());
        result.setPages(ipage.getPages());
        return result;
    }

    @Override
    public List<User> searchByUsername(String q, int limit) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .like(User::getUsername, q)
                        .or().like(User::getNickname, q)
                        .last("LIMIT " + limit)
                        .select(User::getId, User::getUsername, User::getNickname));
    }
}
