package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.common.PageResult;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.entity.Post;
import com.blog.exception.BusinessException;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.NotificationMapper;
import com.blog.entity.User;
import com.blog.service.BadgeService;
import com.blog.service.CommentService;
import com.blog.service.ContentModerationService;
import com.blog.service.PostService;
import com.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final PostService postService;

    private final NotificationMapper notificationMapper;

    private final UserService userService;

    private final BadgeService badgeService;

    private final ContentModerationService contentModerationService;

    @Override
    public List<Map<String, Object>> getPostComments(String slug) {
        Post post = postService.getBySlug(slug);
        List<Comment> list = baseMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, post.getId())
                        .orderByAsc(Comment::getCreatedAt));

        // 缁熻姣忔潯璇勮鐨勭洿鎺ュ瓙鍥炲鏁?        Map<Long, Long> replyCountMap = list.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId, Collectors.counting()));

        return list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("postId", c.getPostId());
            m.put("parentId", c.getParentId());
            m.put("userId", c.getUserId());
            m.put("nickname", c.getNickname());
            m.put("content", c.getContent());
            m.put("likes", c.getLikes() != null ? c.getLikes() : 0);
            m.put("dislikes", c.getDislikes() != null ? c.getDislikes() : 0);
            m.put("createdAt", c.getCreatedAt());
            m.put("replyCount", replyCountMap.getOrDefault(c.getId(), 0L).intValue());
            // 璁剧疆澶村儚
            if (c.getUserId() != null) {
                com.blog.entity.User u = userService.getUserById(c.getUserId());
                if (u != null) m.put("avatar", u.getAvatar());
            }
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(String slug, String nickname, String content, Long parentId, Long userId) {
        Post post = postService.getBySlug(slug);

        ContentModerationService.ModerationResult nickCheck = contentModerationService.moderateText(nickname);
        ContentModerationService.ModerationResult contentCheck = contentModerationService.moderateText(content);

        log.info("鏁忔劅璇嶆鏌? nickname={}, content={}, nickPassed={}, contentPassed={}",
                nickname, content, nickCheck.isPassed(), contentCheck.isPassed());

        if (!nickCheck.isPassed()) {
            log.warn("鏄电О琚嫆缁? {}", nickCheck.getReason());
            throw new BusinessException(400, nickCheck.getReason());
        }
        if (!contentCheck.isPassed()) {
            log.warn("鍐呭琚嫆缁? {}", contentCheck.getReason());
            throw new BusinessException(400, contentCheck.getReason());
        }

        Comment comment = new Comment();
        comment.setPostId(post.getId());
        comment.setNickname(nickCheck.getFilteredText() != null ? nickCheck.getFilteredText() : "鍖垮悕");
        comment.setContent(contentCheck.getFilteredText() != null ? contentCheck.getFilteredText() : "");
        comment.setParentId(parentId);
        comment.setUserId(userId);
        baseMapper.insert(comment);

        // 鍒涘缓璇勮閫氱煡锛堥€氱煡鏂囩珷浣滆€咃紝鑷繁璇勮鑷繁闄ゅ锛?        if (userId == null || !userId.equals(post.getAuthorId())) {
            try {
                Notification n = new Notification();
                n.setUserId(post.getAuthorId());
                n.setType("comment");
                n.setRelatedId(post.getId());
                n.setIsRead(0);
                n.setCreatedAt(java.time.LocalDateTime.now());
                if (parentId != null) {
                    n.setMessage(comment.getNickname() + " 鍥炲浜嗕綘鐨勮瘎璁?);
                } else {
                    n.setMessage(comment.getNickname() + " 璇勮浜嗕綘鐨勬枃绔犮€? + post.getTitle() + "銆?);
                }
                notificationMapper.insert(n);
            } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        }

        // @鎻愬強閫氱煡锛氳В鏋?@username 骞堕€氱煡琚彁鍙婄殑鐢ㄦ埛
        if (content != null && content.contains("@")) {
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("@(\\w{2,20})").matcher(content);
                while (m.find()) {
                    String username = m.group(1);
                    User mentioned = userService.findByUsername(username);
                    if (mentioned != null && !mentioned.getId().equals(userId)) {
                        Notification n = new Notification();
                        n.setUserId(mentioned.getId());
                        n.setType("mention");
                        n.setMessage(comment.getNickname() + " 鍦ㄨ瘎璁轰腑鎻愬埌浜嗕綘");
                        n.setRelatedId(post.getId());
                        n.setIsRead(0);
                        n.setCreatedAt(java.time.LocalDateTime.now());
                        notificationMapper.insert(n);
                    }
                }
            } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        }

        log.info("璇勮鍙戣〃鎴愬姛: postId={}, nickname={}", post.getId(), nickname);

        // 妫€鏌ュ窘绔?        if (userId != null) {
            badgeService.checkAndGrant(userId, "COMMENT_CREATED");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long id) {
        Comment c = baseMapper.selectById(id);
        if (c == null) throw new RuntimeException("璇勮涓嶅瓨鍦?);
        c.setLikes(c.getLikes() == null ? 1 : c.getLikes() + 1);
        baseMapper.updateById(c);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dislikeComment(Long id) {
        Comment c = baseMapper.selectById(id);
        if (c == null) throw new RuntimeException("璇勮涓嶅瓨鍦?);
        c.setDislikes(c.getDislikes() == null ? 1 : c.getDislikes() + 1);
        baseMapper.updateById(c);
    }

    @Override
    public Map<String, Object> getStats() {
        Long total = baseMapper.selectCount(new LambdaQueryWrapper<>());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalComments", total != null ? total : 0);
        return m;
    }

    @Override
    public PageResult<Map<String, Object>> getCommentList(int page, int size, String keyword, String nickname) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .orderByDesc(Comment::getCreatedAt);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Comment::getContent, keyword);
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            wrapper.eq(Comment::getNickname, nickname);
        }

        IPage<Comment> ipage = baseMapper.selectPage(new Page<>(page, size), wrapper);

        // 鏀堕泦鏂囩珷淇℃伅
        Set<Long> postIds = ipage.getRecords().stream()
                .map(Comment::getPostId).collect(Collectors.toSet());
        Map<Long, String> postTitles = new HashMap<>();
        Map<Long, String> postSlugs = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<Post> posts = postService.listByIds(postIds);
            for (Post p : posts) {
                postTitles.put(p.getId(), p.getTitle());
                postSlugs.put(p.getId(), p.getSlug());
            }
        }

        List<Map<String, Object>> list = ipage.getRecords().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("postId", c.getPostId());
            m.put("postTitle", postTitles.getOrDefault(c.getPostId(), "宸插垹闄?));
            m.put("postSlug", postSlugs.get(c.getPostId()));
            m.put("nickname", c.getNickname());
            m.put("content", c.getContent());
            m.put("createdAt", c.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(ipage.getTotal());
        result.setPage(ipage.getCurrent());
        result.setSize(ipage.getSize());
        result.setPages(ipage.getPages());
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getCommentReplies(Long commentId, int page, int size) {
        IPage<Comment> ipage = baseMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, commentId)
                        .orderByAsc(Comment::getCreatedAt));

        List<Map<String, Object>> list = ipage.getRecords().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("postId", c.getPostId());
            m.put("parentId", c.getParentId());
            m.put("userId", c.getUserId());
            m.put("nickname", c.getNickname());
            m.put("content", c.getContent());
            m.put("likes", c.getLikes() != null ? c.getLikes() : 0);
            m.put("dislikes", c.getDislikes() != null ? c.getDislikes() : 0);
            m.put("createdAt", c.getCreatedAt());
            // 缁熻璇ュ洖澶嶈嚜宸辩殑瀛愬洖澶嶆暟
            Long childCount = baseMapper.selectCount(
                    new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, c.getId()));
            m.put("replyCount", childCount != null ? childCount.intValue() : 0);
            if (c.getUserId() != null) {
                User u = userService.getUserById(c.getUserId());
                if (u != null) m.put("avatar", u.getAvatar());
            }
            return m;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(ipage.getTotal());
        result.setPage(ipage.getCurrent());
        result.setSize(ipage.getSize());
        result.setPages(ipage.getPages());
        return result;
    }
}
