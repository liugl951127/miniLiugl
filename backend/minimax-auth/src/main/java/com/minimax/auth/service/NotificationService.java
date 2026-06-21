package com.minimax.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.auth.entity.Notification;
import com.minimax.auth.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 分页查询通知列表。
     */
    public IPage<Notification> list(Long userId, int page, int size) {
        Page<Notification> p = new Page<>(page, size);
        LambdaQueryWrapper<Notification> q = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectPage(p, q);
    }

    /**
     * 未读数量。
     */
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
        );
    }

    /**
     * 标记单条已读。
     */
    @Transactional
    public boolean markRead(Long id) {
        return notificationMapper.update(null,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, id)
                        .set(Notification::getIsRead, 1)
        ) > 0;
    }

    /**
     * 全部已读。
     */
    @Transactional
    public int markAllRead(Long userId) {
        return notificationMapper.update(null,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .set(Notification::getIsRead, 1)
        );
    }

    /**
     * 清空用户所有通知。
     */
    @Transactional
    public int clear(Long userId) {
        return notificationMapper.delete(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
        );
    }

    /**
     * 发送通知。
     */
    @Transactional
    public Notification sendNotification(Long userId, String type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
        log.info("发送通知 userId={} type={} title={}", userId, type, title);
        return n;
    }
}