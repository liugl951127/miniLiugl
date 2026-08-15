-- MiniMax wrk 聊天 API 压测脚本
-- 用法: wrk -t4 -c50 -d60s -s chat.lua http://localhost:7080
--
-- 注意: Chat API 需要认证 Token，先运行 login.lua 获取 Token 后填入 WRK_TOKEN
-- wrk 本身不保留 Token，需要用 Pipeline 模式或外部脚本管理

WRK_TOKEN = os.getenv("WRK_TOKEN") or ""
SESSION_ID = os.getenv("WRK_SESSION_ID") or "1"

local counter = 0

function request()
  counter = counter + 1
  local body = string.format('{"role":"user","content":"测试消息 %d"}', counter)
  local headers = {
    ["Content-Type"] = "application/json",
    ["Authorization"] = "Bearer " .. WRK_TOKEN,
    ["X-Request-Source"] = "wrk"
  }
  return wrk.format("POST", "/api/v1/sessions/" .. SESSION_ID .. "/messages", headers, body)
end

function response(status, headers, body)
  -- 检查 HTTP 200
  if status ~= 200 then
    -- 可能是 401 未授权 / 429 限流 / 503 服务降级
  end
end
