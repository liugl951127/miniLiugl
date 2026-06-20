package com.minimax.auth.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.*;
import com.minimax.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * unionid 跨应用账号打通服务 (V5.1).
 *
 * 场景: 同一微信开放平台下的公众号 + 小程序 + App 共享 unionid.
 * 需求: 用户在公众号绑定平台账号后, 在小程序扫码应自动识别为同一账号.
 *
 * 流程:
 *   1. 跨应用首次扫码: unionid 不存在 → 新建账号 + binding (app_type=A)
 *   2. 跨应用二次扫码: unionid 存在 → 查 unionid_relations → 复用 user_id + 新增 binding (app_type=B)
 *
 * 表设计:
 *   unionid_relations: (user_id, unionid) 一对多关系 (一个 unionid 跨多应用, 一个 user_id 可能多个 unionid)
 *   wechat_user_binding: (app_type, openid) 唯一, 但 unionid 不唯一
 *
 * @since 2026-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatUnionidService {

    private final SysUserMapper userMapper;
    private final WechatUserBindingMapper bindingMapper;
    private final UnionidRelationsMapper unionidMapper;
    private final com.minimax.auth.mapper.OAuthBindingMapper oauthBindingMapper;

    /**
     * 查 unionid 关联的所有用户 (跨平台: wechat/qq/alipay).
     * V5.2: 同时查 oauth_binding 表.
     */
    public List<Map<String, Object>> findUsersByUnionid(String unionid) {
        List<UnionidRelations> relations = unionidMapper.selectList(
                new LambdaQueryWrapper<UnionidRelations>().eq(UnionidRelations::getUnionid, unionid));
        if (relations.isEmpty()) return List.of();

        List<Map<String, Object>> out = new ArrayList<>();
        for (UnionidRelations r : relations) {
            SysUser u = userMapper.selectById(r.getUserId());
            if (u == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", u.getId());
            row.put("username", u.getUsername());
            row.put("nickname", u.getNickname());
            row.put("avatar", u.getAvatar());
            row.put("unionid", r.getUnionid());
            row.put("platform", r.getPlatform());
            row.put("bindingCount", r.getBindingCount());
            row.put("firstSeenAt", r.getFirstSeenAt());
            row.put("lastSeenAt", r.getLastSeenAt());

            // 合并 wechat_user_binding + oauth_binding
            List<Map<String, Object>> apps = new ArrayList<>();

            // 1) 老的 wechat binding
            List<WechatUserBinding> wxBindings = bindingMapper.selectList(
                    new LambdaQueryWrapper<WechatUserBinding>().eq(WechatUserBinding::getUserId, u.getId()));
            for (WechatUserBinding b : wxBindings) {
                Map<String, Object> app = new LinkedHashMap<>();
                app.put("platform", "wechat");
                app.put("appType", b.getAppType());
                app.put("openid", b.getOpenid());
                app.put("nickname", b.getNickname());
                app.put("avatar", b.getAvatar());
                app.put("boundAt", b.getBoundAt());
                app.put("lastLoginAt", b.getLastLoginAt());
                apps.add(app);
            }

            // 2) 新的 oauth binding (含 qq/alipay/weibo/github)
            List<com.minimax.auth.entity.OAuthBinding> oauthBindings = oauthBindingMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.minimax.auth.entity.OAuthBinding>()
                            .eq(com.minimax.auth.entity.OAuthBinding::getUserId, u.getId()));
            for (com.minimax.auth.entity.OAuthBinding b : oauthBindings) {
                Map<String, Object> app = new LinkedHashMap<>();
                app.put("platform", b.getPlatform());
                app.put("appType", b.getAppType());
                app.put("openid", b.getOpenid());
                app.put("nickname", b.getNickname());
                app.put("avatar", b.getAvatar());
                app.put("boundAt", b.getBoundAt());
                app.put("lastLoginAt", b.getLastLoginAt());
                apps.add(app);
            }
            row.put("bindings", apps);
            out.add(row);
        }
        return out;
    }

    /**
     * 注册/更新 unionid 关联 (扫码成功时由 WechatScanLoginService 调用)
     *
     * @return binding_count (该 unionid 下有多少 app_type)
     */
    @Transactional
    public int recordUnionid(Long userId, String unionid, String platform) {
        if (unionid == null || unionid.isBlank()) return 0;
        if (platform == null) platform = "wechat";

        UnionidRelations exist = unionidMapper.selectOne(
                new LambdaQueryWrapper<UnionidRelations>()
                        .eq(UnionidRelations::getUserId, userId)
                        .eq(UnionidRelations::getUnionid, unionid));
        if (exist == null) {
            UnionidRelations r = new UnionidRelations();
            r.setUserId(userId);
            r.setUnionid(unionid);
            r.setPlatform(platform);
            r.setFirstSeenAt(LocalDateTime.now());
            r.setLastSeenAt(LocalDateTime.now());
            r.setBindingCount(1);
            unionidMapper.insert(r);
            return 1;
        }
        // 更新 binding_count + last_seen
        int currentCount = exist.getBindingCount() != null ? exist.getBindingCount() : 1;
        // 实际 binding 数 = 该 user_id + unionid 下 binding 表记录数
        Long realCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<WechatUserBinding>()
                        .eq(WechatUserBinding::getUserId, userId)
                        .eq(WechatUserBinding::getUnionid, unionid));
        UnionidRelations update = new UnionidRelations();
        update.setId(exist.getId());
        update.setLastSeenAt(LocalDateTime.now());
        update.setBindingCount(realCount.intValue());
        unionidMapper.updateById(update);
        return realCount.intValue();
    }

    /**
     * 手动合并账号 (把 userFrom 的所有 binding 转到 userTo)
     * 管理员用: 用户投诉"我有 2 个账号, 帮我合并"
     */
    @Transactional
    public void mergeAccounts(Long userToId, Long userFromId, String reason) {
        if (userToId == null || userFromId == null || userToId.equals(userFromId)) {
            throw new RuntimeException("user_id 无效");
        }
        SysUser target = userMapper.selectById(userToId);
        SysUser source = userMapper.selectById(userFromId);
        if (target == null || source == null) throw new RuntimeException("用户不存在");

        // 把 source 的所有 binding 改到 target
        List<WechatUserBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<WechatUserBinding>().eq(WechatUserBinding::getUserId, userFromId));
        for (WechatUserBinding b : bindings) {
            // 检查 target 是否已有同 (app_type, openid)
            WechatUserBinding dup = bindingMapper.selectOne(
                    new LambdaQueryWrapper<WechatUserBinding>()
                            .eq(WechatUserBinding::getUserId, userToId)
                            .eq(WechatUserBinding::getAppType, b.getAppType())
                            .eq(WechatUserBinding::getOpenid, b.getOpenid()));
            if (dup != null) {
                // 重复, 删 source 的
                bindingMapper.deleteById(b.getId());
            } else {
                b.setUserId(userToId);
                bindingMapper.updateById(b);
            }
        }

        // unionid_relations 也合并
        List<UnionidRelations> sourceRels = unionidMapper.selectList(
                new LambdaQueryWrapper<UnionidRelations>().eq(UnionidRelations::getUserId, userFromId));
        for (UnionidRelations r : sourceRels) {
            UnionidRelations targetRel = unionidMapper.selectOne(
                    new LambdaQueryWrapper<UnionidRelations>()
                            .eq(UnionidRelations::getUserId, userToId)
                            .eq(UnionidRelations::getUnionid, r.getUnionid()));
            if (targetRel == null) {
                r.setUserId(userToId);
                unionidMapper.updateById(r);
            } else {
                unionidMapper.deleteById(r.getId());
            }
        }

        // 软删 source
        source.setStatus(0);
        source.setNickname("[已合并] " + source.getNickname());
        source.setRemark("合并到 user_id=" + userToId + ": " + reason);
        userMapper.updateById(source);

        log.info("账号合并: from={} to={} reason={}", userFromId, userToId, reason);
    }

    /**
     * 列出所有有 unionid 关联的账号 (admin 端)
     */
    public List<Map<String, Object>> listAllUnionidRelations(int limit) {
        List<UnionidRelations> rels = unionidMapper.selectList(
                new LambdaQueryWrapper<UnionidRelations>()
                        .orderByDesc(UnionidRelations::getLastSeenAt)
                        .last("LIMIT " + Math.min(limit, 200)));
        if (rels.isEmpty()) return List.of();
        Set<Long> userIds = new HashSet<>();
        for (UnionidRelations r : rels) userIds.add(r.getUserId());
        Map<Long, SysUser> users = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> userList = userMapper.selectBatchIds(userIds);
            for (SysUser u : userList) users.put(u.getId(), u);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (UnionidRelations r : rels) {
            SysUser u = users.get(r.getUserId());
            if (u == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", u.getId());
            row.put("username", u.getUsername());
            row.put("nickname", u.getNickname());
            row.put("unionid", r.getUnionid());
            row.put("platform", r.getPlatform());
            row.put("bindingCount", r.getBindingCount());
            row.put("firstSeenAt", r.getFirstSeenAt());
            row.put("lastSeenAt", r.getLastSeenAt());
            out.add(row);
        }
        return out;
    }
}