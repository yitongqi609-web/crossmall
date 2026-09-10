package com.crossmall.service;

import com.crossmall.common.exception.BizException;
import com.crossmall.entity.Sku;
import com.crossmall.mapper.SkuMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存服务:Redis Lua 原子预扣(挡量) + DB 条件更新兜底(防超卖最终一致)
 * <p>考点:三层防超卖 —— Lua 判重扣减一体 → DB WHERE stock>=n 条件更新 →
 * 取消/关单/退款双端回补。Redis 库存只是"闸门",DB 库存才是权威值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    public static final String STOCK_KEY_PREFIX = "crossmall:stock:";

    private final SkuMapper skuMapper;
    private final StringRedisTemplate redis;

    private final DefaultRedisScript<Long> deductScript = buildScript("lua/stock_deduct.lua");
    private final DefaultRedisScript<Long> restoreScript = buildScript("lua/stock_restore.lua");

    private static DefaultRedisScript<Long> buildScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    /** 启动预热:在售 SKU 的 DB 库存灌入 Redis(SET NX,不覆盖已有值) */
    @PostConstruct
    public void warmUp() {
        List<Sku> skus = skuMapper.selectList(null);
        for (Sku sku : skus) {
            redis.opsForValue().setIfAbsent(STOCK_KEY_PREFIX + sku.getId(), String.valueOf(sku.getStock()));
        }
        log.info("Redis 库存预热完成,共 {} 个 SKU", skus.size());
    }

    /**
     * Lua 原子预扣。key 不存在(冷启动/被清)时从 DB 加载后重试一次
     *
     * @return 是否预扣成功
     */
    public boolean tryDeduct(Long skuId, int quantity) {
        String key = STOCK_KEY_PREFIX + skuId;
        Long result = redis.execute(deductScript, List.of(key), String.valueOf(quantity));
        if (result != null && result == -1L) {
            Sku sku = skuMapper.selectById(skuId);
            if (sku == null) {
                throw new BizException(404, "商品规格不存在");
            }
            redis.opsForValue().setIfAbsent(key, String.valueOf(sku.getStock()));
            result = redis.execute(deductScript, List.of(key), String.valueOf(quantity));
        }
        return result != null && result == 1L;
    }

    /** Redis 库存回补(订单取消/超时关单/退款) */
    public void restore(Long skuId, int quantity) {
        redis.execute(restoreScript, List.of(STOCK_KEY_PREFIX + skuId), String.valueOf(quantity));
    }

    /** DB 条件扣减(下单事务内),返回是否成功 */
    public boolean deductDb(Long skuId, int quantity) {
        return skuMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Sku>()
                        .setSql("stock = stock - " + quantity)
                        .eq(Sku::getId, skuId)
                        .ge(Sku::getStock, quantity)) > 0;
    }

    /** DB 回补(取消/超时关单/退款) */
    public void restoreDb(Long skuId, int quantity) {
        skuMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Sku>()
                        .setSql("stock = stock + " + quantity)
                        .eq(Sku::getId, skuId));
    }

    /** 管理端改库存后,以 DB 权威值覆盖 Redis 闸门 */
    public void syncFromDb(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku != null) {
            redis.opsForValue().set(STOCK_KEY_PREFIX + skuId, String.valueOf(sku.getStock()));
        }
    }

    /** 当前 Redis 库存(DB 兜底) */
    public int peek(Long skuId) {
        String v = redis.opsForValue().get(STOCK_KEY_PREFIX + skuId);
        if (v != null) {
            return Integer.parseInt(v);
        }
        Sku sku = skuMapper.selectById(skuId);
        return sku == null ? 0 : sku.getStock();
    }
}
