-- MiniMax wrk 压测脚本 (Day 21)
-- 用法: wrk -t4 -c100 -d60s -s login.lua http://localhost:7080
--
-- wrk 参数说明:
--   -t4    4 个线程
--   -c100  100 个连接（并发）
--   -d60s  持续 60 秒
--   -s    指定 Lua 脚本

local counter = 0
local threads = {}

-- 生成唯一 sessionId，避免缓存
function request()
  counter = counter + 1
  local session_id = string.format("%d-%d", os.time(), counter)
  local body = string.format('{"username":"admin","password":"admin@123"}')
  local headers = {}
  headers["Content-Type"] = "application/json"
  headers["X-Request-Source"] = "wrk"
  return wrk.format("POST", "/api/v1/auth/login", headers, body)
end

-- 响应回调：检查 code=0
function response(status, headers, body)
  if status ~= 200 then return end
  -- 简单检查响应体包含 "code":0
  local ok, json = pcall(function() return require("cjson").decode(body) end)
  if ok and json and json.code == 0 then
    -- 登录成功，提取 Token（可选）
    -- wrk 不支持在 callback 中共享变量给其他请求，这里仅记录成功
  end
end

-- 线程回调（初始化每个线程）
function setup(thread)
  thread:set("session_id", 0)
end

-- 周期回调（每个请求后调用，可用于动态 token 刷新）
function delay(request)
  return 0
end
