-- 库存回补(取消/超时关单):KEYS[1]=库存key ARGV[1]=回补数量
redis.call('INCRBY', KEYS[1], ARGV[1])
return tonumber(redis.call('GET', KEYS[1]))
