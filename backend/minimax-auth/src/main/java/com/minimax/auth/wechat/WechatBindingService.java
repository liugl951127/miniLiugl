package com.minimax.auth.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.minimax.auth.entity.*;
import com.minimax.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 微信绑定管理服务 (V5).
 *
 * 用户主动操作:
 *   - 解绑: 当前用户解绑自己微信
 *   - 我的微信信息: 查当前用户绑定的微信
 *
 * 管理员操作 (adminLiugl):
 *   - 给用户绑定微信 (openid → user_id)
 *   - 强制解绑
 *   - 列出全部绑定
 *
 * @since 2026-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatBindingService {

    private final SysUserMapper userMapper;
    private final WechatUserBindingMapper bindingMapper;

    /**
     * 查询当前用户绑定的微信
     */
    public Map<String, Object> getMyBinding(Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) return Map.of("bound", false);
        if (u.getWechatOpenid() == null) return Map.of("bound", false);

        List<WechatUserBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<WechatUserBinding>().eq(WechatUserBinding::getUserId, userId));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bound", true);
        out.put("openid", u.getWechatOpenid());
        out.put("unionid", u.getWechatUnionid());
        out.put("nickname", u.getWechatNickname());
        out.put("avatar", u.getWechatAvatar());
        out.put("boundAt", u.getWechatBoundAt());
        out.put("bindings", bindings);  // 多应用绑定列表
        return out;
    }

    /**
     * 用户主动解绑
     */
    @Transactional
    public void unbindMyself(Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) throw new RuntimeException("用户不存在");
        if (u.getWechatOpenid() == null) throw new RuntimeException("未绑定微信");

        // 删 sys_user 微信字段 (LambdaUpdateWrapper 确保 null 也会更新)
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getWechatOpenid, null)
                .set(SysUser::getWechatUnionid, null)
                .set(SysUser::getWechatNickname, null)
                .set(SysUser::getWechatAvatar, null)
                .set(SysUser::getWechatBoundAt, null));

        // 删 binding
        bindingMapper.delete(
                new LambdaQueryWrapper<WechatUserBinding>().eq(WechatUserBinding::getUserId, userId));

        log(userId, u.getWechatOpenid(), "unbind", "ok", "用户主动解绑");
        log.info("用户 {} 解绑微信 {}", u.getUsername(), u.getWechatOpenid());
    }

    /**
     * 管理员强制绑定 openid → user_id
     */
    @Transactional
    public void bindByAdmin(Long userId, String openid, String unionid, String nickname,
                            String avatar, String appType, Long operatorId) {
        if (openid == null || openid.isBlank()) throw new RuntimeException("openid 不能为空");
        SysUser u = userMapper.selectById(userId);
        if (u == null) throw new RuntimeException("目标用户不存在");

        // 检查 openid 是否已绑别的用户
        WechatUserBinding exist = bindingMapper.selectOne(
                new LambdaQueryWrapper<WechatUserBinding>()
                        .eq(WechatUserBinding::getOpenid, openid)
                        .ne(WechatUserBinding::getUserId, userId));
        if (exist != null) {
            throw new RuntimeException("openid 已绑定其他用户 (user_id=" + exist.getUserId() + ")");
        }

        // 写 sys_user (LambdaUpdateWrapper 全量 set)
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getWechatOpenid, openid)
                .set(SysUser::getWechatUnionid, unionid)
                .set(SysUser::getWechatNickname, nickname)
                .set(SysUser::getWechatAvatar, avatar)
                .set(SysUser::getWechatBoundAt, LocalDateTime.now()));

        // upsert binding
        WechatUserBinding b = bindingMapper.selectOne(
                new LambdaQueryWrapper<WechatUserBinding>()
                        .eq(WechatUserBinding::getOpenid, openid));
        if (b == null) {
            b = new WechatUserBinding();
            b.setUserId(userId);
            b.setOpenid(openid);
            b.setUnionid(unionid);
            b.setAppType(appType != null ? appType : "mp");
            b.setNickname(nickname);
            b.setAvatar(avatar);
            b.setBoundAt(LocalDateTime.now());
            bindingMapper.insert(b);
        } else {
            b.setUserId(userId);
            b.setUnionid(unionid);
            b.setNickname(nickname);
            b.setAvatar(avatar);
            bindingMapper.updateById(b);
        }

        log(userId, openid, "bind_admin", "ok", "operator=" + operatorId);
    }

    /**
     * 管理员强制解绑 (按 user_id)
     */
    @Transactional
    public void unbindByAdmin(Long userId, Long operatorId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) throw new RuntimeException("用户不存在");
        String openid = u.getWechatOpenid();
        unbindMyself(userId);
        log(userId, openid, "unbind_admin", "ok", "operator=" + operatorId);
    }

    /**
     * 按 openid 找用户 (管理员用)
     */
    public Map<String, Object> findByOpenid(String openid) {
        WechatUserBinding b = bindingMapper.selectOne(
                new LambdaQueryWrapper<WechatUserBinding>().eq(WechatUserBinding::getOpenid, openid));
        if (b == null) return Map.of("found", false);
        SysUser u = userMapper.selectById(b.getUserId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("found", true);
        out.put("userId", b.getUserId());
        out.put("username", u != null ? u.getUsername() : null);
        out.put("nickname", u != null ? u.getNickname() : null);
        out.put("binding", b);
        return out;
    }

    /**
     * 列出全部绑定 (管理员)
     */
    public List<Map<String, Object>> listAll(int limit, String keyword) {
        // 直接查所有 binding + 嵌套 user 查询
        List<WechatUserBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<WechatUserBinding>().orderByDesc(WechatUserBinding::getBoundAt).last("LIMIT " + Math.min(limit, 200)));
        if (bindings.isEmpty()) return List.of();
        // 批量查 user
        Set<Long> userIds = new java.util.HashSet<>();
        for (WechatUserBinding b : bindings) userIds.add(b.getUserId());
        Map<Long, SysUser> users = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> userList = userMapper.selectBatchIds(userIds);
            for (SysUser u : userList) users.put(u.getId(), u);
        }
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (WechatUserBinding b : bindings) {
            SysUser u = users.get(b.getUserId());
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("user_id", b.getUserId());
            row.put("username", u != null ? u.getUsername() : null);
            row.put("user_nickname", u != null ? u.getNickname() : null);
            row.put("openid", b.getOpenid());
            row.put("unionid", b.getUnionid());
            row.put("app_type", b.getAppType());
            row.put("wechat_nickname", b.getNickname());
            row.put("bound_at", b.getBoundAt());
            row.put("last_login_at", b.getLastLoginAt());
            out.add(row);
        }
        return out;
    }

    private void log(Long userId, String openid, String action, String result, String msg) {
        // 写日志 + 后续可同步到 wechat_scan_log 表
        log.info("[WeChat bind] action={} result={} user={} openid={} {}", action, result, userId, openid, msg);
    }
}
