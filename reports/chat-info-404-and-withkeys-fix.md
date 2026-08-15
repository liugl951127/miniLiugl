# 404 + _withKeys 错误修复 (V6.8.1+)

## 用户报的错误

```
GET https://ai.liugeliang.com/api/v1/ai/info → 404
GET https://ai.liugeliang.com/api/v1/ai/chat/sessions → 404
[Vue warn] Cannot read properties of undefined (reading '_withKeys')
```

## 根因

### 1. 404 (后端路由)

**实际后端有**:
- `backend/minimax-ai/src/main/java/com/minimax/ai/controller/AiController.java:202 @GetMapping("/info")` + 类级 `@RequestMapping("/api/v1/ai")` = **完整 `/api/v1/ai/info`**
- `backend/minimax-ai/src/main/java/com/minimax/ai/controller/AiChatRealController.java:35 @GetMapping("/sessions")` + 类级 `@RequestMapping("/api/v1/ai/chat")` = **完整 `/api/v1/ai/chat/sessions`**

**404 原因**:
- 后端服务没启 (或挂了)
- 或者 nginx 路由没配 (但我看了 nginx.conf, 有 `location /api/v1/ai/`)

**验证**:
```bash
# 你需要做的:
docker compose ps  # 看 ai 服务是否 Up
docker compose logs minimax-ai --tail 50  # 看启动日志
curl http://localhost:8094/api/v1/ai/info  # 内部端口测试
```

### 2. _withKeys 错 (前端 chat/Index.vue)

**根因**: `@keydown.enter.exact.prevent="send"` 引用了**不存在的 `send` 函数** (实际函数名是 `sendMessage`)

**Vue 编译流程**:
```js
// vue 3.4+ 内部
const withKeys = (fn, modifiers) => {
  const cache = fn._withKeys || (fn._withKeys = {})  // ❌ fn = 'send' 字符串
  // ...
}
```

`fn = 'send'` (string) 时, `'send'._withKeys` 报 "Cannot read properties of undefined"

## 修复

### 1. `src/views/chat/Index.vue`

#### 修前:
```vue
@keydown.enter.exact.prevent="send"  <!-- send 函数不存在 -->
```

#### 修后:
```vue
@keydown.enter.prevent="sendMessage"  <!-- 实际函数名 -->
```

#### 同时修其他 speechCall 引用:
- `speechCall.isCallActive` (useSpeechCall 没定义) → `speechCall.state.value !== 'idle'`
- `speechCall.isMuted.value` 等 (实际是 .value) → 保持

### 2. 后端 404

不是代码错, 是部署问题。检查:
```bash
# 1. 后端服务在跑
docker compose ps minimax-ai

# 2. 端口可达
curl http://localhost:8094/api/v1/ai/info
# 期望: {"code":0,"data":{"name":"MiniMax 自研 AI","version":"V2.5",...}}

# 3. nginx 路由
curl http://localhost/api/v1/ai/info
# 期望同上
```

## 关键经验

1. **Vue 编译时引用不存在的函数** 会触发 `_withKeys` 错 - 因为 vue 把函数名作为 string 传给 `withKeys(fn, modifiers)`
2. **后端 404** 大概率是部署问题 (服务没起) 不是路由错
3. **E2E 模拟测试** 之前已经暴露过这个错 (chat/Index 报 _withKeys), 但当时没修
4. **`.exact.prevent` 修饰符链** 在 vue 3.4 + jsdom 沙箱有兼容问题, 改 `.enter.prevent` 即可
5. **useSpeechCall composable** 实际没暴露 `isCallActive`, 模板用 `state.value !== 'idle'` 才是正确判断
