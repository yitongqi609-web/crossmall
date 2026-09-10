package com.crossmall.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.entity.FxRate;
import com.crossmall.mapper.FxRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * 汇率服务:Redis 缓存读穿透 + DB 兜底 + 模拟第三方汇率源定时拉取
 * <p>考点:多币种金额全链路以 USD 分为基准币,展示/结算时按汇率换算;
 * 下单时把汇率写入订单快照,后续支付/退款均按快照,与实时汇率波动解耦。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FxService {

    public static final String BASE_CURRENCY = "USD";
    private static final String CACHE_KEY = "crossmall:fx:rate:";
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    private final FxRateMapper fxRateMapper;
    private final StringRedisTemplate redis;

    /** 币种表内存缓存(符号等只读信息,60s 失效;写操作主动失效) */
    private volatile List<FxRate> tableCache;
    private volatile long tableCacheAt;

    private List<FxRate> table() {
        List<FxRate> local = tableCache;
        if (local == null || System.currentTimeMillis() - tableCacheAt > 60_000L) {
            local = listAll();
            tableCache = local;
            tableCacheAt = System.currentTimeMillis();
        }
        return local;
    }

    /** 取完整汇率对象 */
    public FxRate getFx(String currency) {
        return table().stream()
                .filter(r -> r.getCurrency().equals(currency))
                .findFirst()
                .orElseThrow(() -> new BizException(400, "不支持的币种: " + currency));
    }

    /** 币种符号,如 $ / € / CN¥ */
    public String symbolOf(String currency) {
        return getFx(currency).getSymbol();
    }

    /** 全部币种(管理端币种表/前端切换器) */
    public List<FxRate> listAll() {
        return fxRateMapper.selectList(new LambdaQueryWrapper<FxRate>().orderByAsc(FxRate::getId));
    }

    /**
     * 取汇率(1 USD = rate),Redis → DB 读穿透
     */
    public BigDecimal getRate(String currency) {
        if (BASE_CURRENCY.equals(currency)) {
            return BigDecimal.ONE;
        }
        String cached = redis.opsForValue().get(CACHE_KEY + currency);
        if (cached != null) {
            return new BigDecimal(cached);
        }
        FxRate rate = fxRateMapper.selectOne(new LambdaQueryWrapper<FxRate>()
                .eq(FxRate::getCurrency, currency));
        if (rate == null) {
            throw new BizException(400, "不支持的币种: " + currency);
        }
        redis.opsForValue().set(CACHE_KEY + currency, rate.getRate().toPlainString(), CACHE_TTL);
        return rate.getRate();
    }

    /** 校验币种合法且存在 */
    public void checkCurrency(String currency) {
        getRate(currency);
    }

    /**
     * USD 分 → 目标币分(四舍五入到整数分)
     */
    public long convert(long usdCents, String currency) {
        if (BASE_CURRENCY.equals(currency)) {
            return usdCents;
        }
        BigDecimal local = BigDecimal.valueOf(usdCents).multiply(getRate(currency));
        return local.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * 模拟第三方汇率源定时拉取:围绕当前汇率做 ±0.5% 随机游走并落库
     * 真实场景替换为调用 openexchangerates 等外部 API
     */
    public int refreshFromSource() {
        List<FxRate> rates = fxRateMapper.selectList(null);
        int updated = 0;
        for (FxRate rate : rates) {
            if (BASE_CURRENCY.equals(rate.getCurrency())) {
                continue;
            }
            BigDecimal fluctuation = BigDecimal.valueOf(1 + (RandomUtil.randomDouble(-0.005, 0.005)));
            BigDecimal newRate = rate.getRate().multiply(fluctuation).setScale(6, RoundingMode.HALF_UP);
            rate.setRate(newRate);
            rate.setSource("AUTO");
            fxRateMapper.updateById(rate);
            redis.delete(CACHE_KEY + rate.getCurrency());
            updated++;
        }
        log.info("汇率定时拉取完成,更新 {} 个币种", updated);
        tableCache = null;
        return updated;
    }

    /** 手动改汇率(管理端),同步失效缓存 */
    public void updateRate(String currency, BigDecimal newRate, String operator) {
        FxRate rate = fxRateMapper.selectOne(new LambdaQueryWrapper<FxRate>()
                .eq(FxRate::getCurrency, currency));
        if (rate == null) {
            throw new BizException(400, "币种不存在: " + currency);
        }
        rate.setRate(newRate.setScale(6, RoundingMode.HALF_UP));
        rate.setSource("MANUAL");
        fxRateMapper.updateById(rate);
        redis.delete(CACHE_KEY + currency);
        tableCache = null;
    }
}
