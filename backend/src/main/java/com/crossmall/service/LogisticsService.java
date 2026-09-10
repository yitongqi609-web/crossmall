package com.crossmall.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.entity.LogisticsLine;
import com.crossmall.mapper.LogisticsLineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 国际物流服务:线路按目的国过滤 + 首重/续重计费
 * <p>计费公式:重量 <= 首重 → 首重费;否则 首重费 + ceil(超出重量/续重单位) * 续重费
 * <p>订单计费重 = Σ(明细单件计费重 × 数量),不足首重按首重计。
 */
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsLineMapper lineMapper;

    /** 目的国可用线路 */
    public List<LogisticsLine> listAvailable(String countryCode) {
        return lineMapper.selectList(new LambdaQueryWrapper<LogisticsLine>()
                .eq(LogisticsLine::getStatus, 1)
                .orderByAsc(LogisticsLine::getId))
                .stream()
                .filter(line -> supports(line, countryCode))
                .toList();
    }

    public LogisticsLine getById(Long id) {
        LogisticsLine line = lineMapper.selectById(id);
        if (line == null || line.getStatus() != 1) {
            throw new IllegalArgumentException("物流线路不可用: " + id);
        }
        return line;
    }

    /** 查不到返回 null(展示场景容错) */
    public LogisticsLine getByIdNullable(Long id) {
        return id == null ? null : lineMapper.selectById(id);
    }

    public boolean supports(LogisticsLine line, String countryCode) {
        JSONArray countries = JSONUtil.parseArray(line.getSupportedCountries());
        return countries.contains("*") || countries.contains(countryCode);
    }

    /** 首重/续重计费(USD 分) */
    public long calcShippingFee(LogisticsLine line, int totalWeightGrams) {
        int first = line.getFirstWeightGrams();
        if (totalWeightGrams <= first) {
            return line.getFirstFeeCents();
        }
        int overweight = totalWeightGrams - first;
        int units = (int) Math.ceil((double) overweight / line.getContinueWeightGrams());
        return line.getFirstFeeCents() + (long) units * line.getContinueFeeCents();
    }
}
