-- 库存原子预扣:KEYS[1]=库存key ARGV[1]=扣减数量
-- 返回:1 成功;-1 库存key不存在(需预热);-2 库存不足
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return -1
end
if stock < tonumber(ARGV[1]) then
    return -2
end
redis.call('DECRBY', KEYS[1], ARGV[1])
return 1
