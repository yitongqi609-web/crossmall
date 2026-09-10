package com.crossmall.task;

import com.crossmall.service.FxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 汇率定时拉取任务:模拟调用第三方汇率源(如 openexchangerates),
 * 真实场景替换为 RestClient 调用外部 API + 失败告警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxPullTask {

    private final FxService fxService;

    @Scheduled(cron = "${crossmall.fx.pull-cron}")
    public void pull() {
        try {
            fxService.refreshFromSource();
        } catch (Exception e) {
            // 拉取失败不影响主流程:订单按已有快照汇率结算
            log.error("汇率拉取失败,下轮重试", e);
        }
    }
}
