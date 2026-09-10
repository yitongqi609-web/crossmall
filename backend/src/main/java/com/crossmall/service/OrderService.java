package com.crossmall.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossmall.common.exception.BizException;
import com.crossmall.config.CrossMallProperties;
import com.crossmall.dto.AddressSnapshot;
import com.crossmall.dto.OrderCreateDTO;
import com.crossmall.dto.OrderItemDTO;
import com.crossmall.dto.OrderPreviewDTO;
import com.crossmall.entity.Address;
import com.crossmall.entity.FxRate;
import com.crossmall.entity.LogisticsLine;
import com.crossmall.entity.Order;
import com.crossmall.entity.OrderItem;
import com.crossmall.entity.OrderTrack;
import com.crossmall.fulfillment.OrderStatus;
import com.crossmall.mapper.OrderItemMapper;
import com.crossmall.mapper.OrderMapper;
import com.crossmall.mapper.OrderTrackMapper;
import com.crossmall.mq.producer.OrderProducer;
import com.crossmall.mq.model.TrackMessage;
import com.crossmall.vo.OrderPreviewVO;
import com.crossmall.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务:试算 / 下单 / 查询 / 取消
 *
 * <p>下单链路(面试考点):
 * <ol>
 *   <li>Redis Lua 原子预扣 —— 快速失败挡住无效流量</li>
 *   <li>DB WHERE stock &gt;= n 条件扣减 —— 并发兜底,杜绝超卖</li>
 *   <li>金额与汇率写入订单快照 —— 与实时汇率解耦,支付/退款按快照结算</li>
 *   <li>事务提交后(afterCommit)再发延迟关单消息 —— 避免消息先于订单落库</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderTrackMapper orderTrackMapper;
    private final AddressService addressService;
    private final TradeCalcService tradeCalcService;
    private final LogisticsService logisticsService;
    private final StockService stockService;
    private final FxService fxService;
    private final OrderProducer orderProducer;
    private final CrossMallProperties properties;

    // ==================== 结算试算 ====================

    public OrderPreviewVO preview(Long userId, OrderPreviewDTO dto) {
        Address address = addressService.requireOwned(userId, dto.getAddressId());
        List<TradeCalcService.DraftItem> drafts = tradeCalcService.loadDraftItems(dto.getItems());

        List<LogisticsLine> lines = logisticsService.listAvailable(address.getCountryCode());
        if (lines.isEmpty()) {
            throw new BizException(400, "目的国 " + address.getCountryName() + " 暂无可用物流线路");
        }
        LogisticsLine selected;
        if (dto.getLineId() != null) {
            selected = lines.stream()
                    .filter(l -> l.getId().equals(dto.getLineId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(400, "所选线路不支持该目的国"));
        } else {
            selected = lines.get(0);
        }

        String currency = dto.getCurrency() == null || dto.getCurrency().isBlank()
                ? FxService.BASE_CURRENCY : dto.getCurrency();
        FxRate fx = fxService.getFx(currency);
        TradeCalcService.CalcResult calc = tradeCalcService.calc(address.getCountryCode(), selected, drafts);

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setCountryCode(address.getCountryCode());
        vo.setCountryName(address.getCountryName());
        vo.setLineId(selected.getId());
        vo.setFirstWeightGrams((long) selected.getFirstWeightGrams());
        vo.setTotalWeightGrams((long) calc.totalWeightGrams());
        vo.setGoodsFeeCents(calc.goodsUsd());
        vo.setShippingFeeCents(calc.shippingUsd());
        vo.setTaxFeeCents(calc.taxUsd());
        vo.setTotalFeeCents(calc.totalUsd());
        vo.setDeMinimisCents(calc.countryRule().getDeMinimisCents());
        vo.setTaxHits(calc.taxHits().stream().map(h -> {
            OrderPreviewVO.TaxHitVO t = new OrderPreviewVO.TaxHitVO();
            t.setHsCode(h.hsCode());
            t.setHsName(h.hsName());
            t.setRate(h.rate());
            t.setBaseCents(h.baseUsd());
            t.setTaxCents(h.taxUsd());
            return t;
        }).toList());
        vo.setCurrency(currency);
        vo.setDisplaySymbol(fx.getSymbol());
        vo.setFxRate(fx.getRate());
        vo.setTotalLocalCents(fxService.convert(calc.totalUsd(), currency));
        vo.setLineOptions(lines.stream().map(line -> {
            OrderPreviewVO.LineOption option = new OrderPreviewVO.LineOption();
            option.setLineId(line.getId());
            option.setName(line.getName());
            option.setNameEn(line.getNameEn());
            option.setCarrier(line.getCarrier());
            option.setEtaMinDays(line.getEtaMinDays());
            option.setEtaMaxDays(line.getEtaMaxDays());
            long fee = logisticsService.calcShippingFee(line, calc.totalWeightGrams());
            option.setShippingFeeCents(fee);
            option.setShippingDisplayPrice(fxService.convert(fee, currency));
            option.setDisplaySymbol(fx.getSymbol());
            option.setSelected(line.getId().equals(selected.getId()));
            return option;
        }).toList());
        vo.setItems(drafts.stream().map(d -> {
            OrderPreviewVO.ItemLine item = new OrderPreviewVO.ItemLine();
            item.setSkuId(d.sku().getId());
            item.setSpuId(d.spu().getId());
            item.setTitle(d.spu().getTitle());
            item.setTitleEn(d.spu().getTitleEn());
            item.setAttrs(d.sku().getAttrs());
            item.setQuantity(d.quantity());
            item.setUnitPriceCents(d.sku().getPriceCents());
            item.setSubtotalCents(d.subtotalUsd());
            item.setDisplaySymbol(fx.getSymbol());
            item.setUnitDisplayPrice(fxService.convert(d.sku().getPriceCents(), currency));
            item.setSubtotalDisplayPrice(fxService.convert(d.subtotalUsd(), currency));
            return item;
        }).toList());
        return vo;
    }

    // ==================== 下单 ====================

    @Transactional
    public String create(Long userId, OrderCreateDTO dto) {
        Address address = addressService.requireOwned(userId, dto.getAddressId());
        fxService.checkCurrency(dto.getCurrency());
        LogisticsLine line = logisticsService.getById(dto.getLineId());
        tradeCalcService.checkLineSupports(line, address.getCountryCode());
        List<TradeCalcService.DraftItem> drafts = tradeCalcService.loadDraftItems(dto.getItems());

        // 1. Redis Lua 原子预扣:判存+扣减一体,失败快速返回
        List<TradeCalcService.DraftItem> deducted = new java.util.ArrayList<>();
        for (TradeCalcService.DraftItem draft : drafts) {
            if (!stockService.tryDeduct(draft.sku().getId(), draft.quantity())) {
                deducted.forEach(x -> stockService.restore(x.sku().getId(), x.quantity()));
                throw new BizException(400, "「" + draft.spu().getTitle() + "」库存不足");
            }
            deducted.add(draft);
        }

        Order order;
        try {
            TradeCalcService.CalcResult calc = tradeCalcService.calc(address.getCountryCode(), line, drafts);
            FxRate fx = fxService.getFx(dto.getCurrency());

            // 2. DB 条件扣减兜底:WHERE stock >= n,并发下最后一道防线
            for (TradeCalcService.DraftItem draft : drafts) {
                if (!stockService.deductDb(draft.sku().getId(), draft.quantity())) {
                    throw new BizException(400, "「" + draft.spu().getTitle() + "」库存不足(并发冲突)");
                }
            }

            // 3. 订单落库:金额与汇率全部写入快照
            order = buildOrder(userId, address, line, dto.getCurrency(), fx, calc);
            orderMapper.insert(order);
            for (TradeCalcService.DraftItem draft : drafts) {
                orderItemMapper.insert(buildItem(order, draft));
            }
        } catch (Exception e) {
            // DB 阶段任一步失败:回补 Redis 预扣,事务随之回滚
            deducted.forEach(x -> stockService.restore(x.sku().getId(), x.quantity()));
            throw e;
        }

        // 4. 事务提交后再发延迟关单消息,避免消息先于订单落库被消费
        String orderNo = order.getOrderNo();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                orderProducer.sendCloseDelay(orderNo);
                log.info("订单 {} 下单成功,{} 前未支付将自动关单",
                        orderNo, LocalDateTime.now().plus(properties.getOrder().getPayTimeout()));
            }
        });
        return orderNo;
    }

    private Order buildOrder(Long userId, Address address, LogisticsLine line,
                             String currency, FxRate fx, TradeCalcService.CalcResult calc) {
        Order order = new Order();
        order.setOrderNo("CM" + LocalDateTime.now().format(ORDER_NO_FMT)
                + RandomUtil.randomNumbers(6) + String.format("%02d", userId % 100));
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setAddressSnapshot(JSONUtil.toJsonStr(AddressSnapshot.of(address)));
        order.setLogisticsLineId(line.getId());
        order.setLineName(line.getName());
        order.setCurrency(currency);
        order.setFxRate(fx.getRate());
        order.setGoodsFeeCents(calc.goodsUsd());
        order.setShippingFeeCents(calc.shippingUsd());
        order.setTaxFeeCents(calc.taxUsd());
        order.setTotalFeeCents(calc.totalUsd());
        order.setTotalLocalCents(fxService.convert(calc.totalUsd(), currency));
        order.setPayTimeoutAt(LocalDateTime.now().plus(properties.getOrder().getPayTimeout()));
        return order;
    }

    private OrderItem buildItem(Order order, TradeCalcService.DraftItem draft) {
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setSpuId(draft.spu().getId());
        item.setSkuId(draft.sku().getId());
        item.setSpuTitle(draft.spu().getTitle());
        item.setSpuTitleEn(draft.spu().getTitleEn());
        item.setSkuAttrs(draft.sku().getAttrs());
        item.setImage(draft.spu().getMainImage());
        item.setPriceCents(draft.sku().getPriceCents());
        item.setQuantity(draft.quantity());
        item.setSubtotalCents(draft.subtotalUsd());
        item.setWeightGrams(draft.sku().getWeightGrams());
        return item;
    }

    // ==================== 查询 ====================

    public Page<OrderVO> pageMine(Long userId, String status, long page, long size) {
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(status != null && !status.isBlank(), Order::getStatus, status)
                        .orderByDesc(Order::getId));
        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toOrderVOs(result.getRecords(), false));
        return voPage;
    }

    public OrderVO detail(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        List<OrderVO> vos = toOrderVOs(List.of(order), true);
        return vos.get(0);
    }

    /** 管理端订单详情(不限买家) */
    public OrderVO detailForAdmin(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return toOrderVOs(List.of(order), true).get(0);
    }

    /** 管理端订单列表组装(不限买家) */
    public List<OrderVO> pageForAdmin(List<Order> orders) {
        return toOrderVOs(orders, false);
    }

    private List<OrderVO> toOrderVOs(List<Order> orders, boolean withTracks) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<String> orderNos = orders.stream().map(Order::getOrderNo).toList();
        Map<String, List<OrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderNo, orderNos))
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderNo));
        Map<String, List<OrderTrack>> tracksByOrder = withTracks
                ? orderTrackMapper.selectList(new LambdaQueryWrapper<OrderTrack>()
                                .in(OrderTrack::getOrderNo, orderNos)
                                .orderByAsc(OrderTrack::getOccurredAt))
                        .stream()
                        .collect(Collectors.groupingBy(OrderTrack::getOrderNo))
                : Map.of();

        return orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            OrderStatus statusEnum = OrderStatus.of(order.getStatus());
            vo.setStatusLabel(statusEnum.getLabel());
            vo.setStatusLabelEn(statusEnum.getLabelEn());
            vo.setAddress(JSONUtil.toBean(order.getAddressSnapshot(), OrderVO.AddressInfo.class));
            vo.setLineId(order.getLogisticsLineId());
            vo.setLineName(order.getLineName());
            LogisticsLine line = logisticsService.getByIdNullable(order.getLogisticsLineId());
            if (line != null) {
                vo.setEtaText(line.getEtaMinDays() + "-" + line.getEtaMaxDays() + " days");
            }
            vo.setCurrency(order.getCurrency());
            vo.setFxRate(order.getFxRate());
            vo.setGoodsFeeCents(order.getGoodsFeeCents());
            vo.setShippingFeeCents(order.getShippingFeeCents());
            vo.setTaxFeeCents(order.getTaxFeeCents());
            vo.setTotalFeeCents(order.getTotalFeeCents());
            vo.setDisplaySymbol(fxService.symbolOf(order.getCurrency()));
            vo.setTotalDisplayPrice(order.getTotalLocalCents());
            vo.setPayTimeoutAt(order.getPayTimeoutAt());
            vo.setPaidAt(order.getPaidAt());
            vo.setCompletedAt(order.getCompletedAt());
            vo.setCancelReason(order.getCancelReason());
            vo.setCreateTime(order.getCreateTime());

            vo.setItems(itemsByOrder.getOrDefault(order.getOrderNo(), List.of()).stream().map(item -> {
                OrderVO.Item itemVO = new OrderVO.Item();
                itemVO.setSpuId(item.getSpuId());
                itemVO.setSkuId(item.getSkuId());
                itemVO.setTitle(item.getSpuTitle());
                itemVO.setTitleEn(item.getSpuTitleEn());
                itemVO.setAttrs(item.getSkuAttrs());
                itemVO.setImage(item.getImage());
                itemVO.setPriceCents(item.getPriceCents());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setSubtotalCents(item.getSubtotalCents());
                itemVO.setDisplaySymbol(vo.getDisplaySymbol());
                itemVO.setPriceDisplay(fxService.convert(item.getPriceCents(), order.getCurrency()));
                itemVO.setSubtotalDisplayPrice(fxService.convert(item.getSubtotalCents(), order.getCurrency()));
                return itemVO;
            }).toList());

            if (withTracks) {
                vo.setTracks(tracksByOrder.getOrDefault(order.getOrderNo(), List.of()).stream().map(track -> {
                    OrderVO.Track trackVO = new OrderVO.Track();
                    trackVO.setStatus(track.getStatus());
                    trackVO.setLocation(track.getLocation());
                    trackVO.setDescription(track.getDescription());
                    trackVO.setDescriptionEn(track.getDescriptionEn());
                    trackVO.setOccurredAt(track.getOccurredAt());
                    return trackVO;
                }).toList());
            }
            return vo;
        }).toList();
    }

    // ==================== 取消 ====================

    @Transactional
    public void cancel(Long userId, String orderNo, String reason) {
        Order order = requireOwnedOrder(userId, orderNo);
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getStatus, OrderStatus.CANCELLED.name())
                .set(Order::getCancelReason, reason == null ? "用户主动取消" : reason)
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.name()));
        if (rows == 0) {
            throw new BizException(400, "订单当前状态不可取消");
        }
        compensateStock(orderNo);
        sendTrack(orderNo, OrderStatus.CANCELLED, null, "买家取消订单", "Cancelled by buyer");
    }

    /** 双端回补库存:DB 权威值 + Redis 闸门 */
    private void compensateStock(String orderNo) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem item : items) {
            stockService.restoreDb(item.getSkuId(), item.getQuantity());
            stockService.restore(item.getSkuId(), item.getQuantity());
        }
    }

    public Order requireOwnedOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(404, "订单不存在");
        }
        return order;
    }

    void sendTrack(String orderNo, OrderStatus status, String location, String desc, String descEn) {
        TrackMessage message = new TrackMessage();
        message.setOrderNo(orderNo);
        message.setStatus(status.name());
        message.setLocation(location);
        message.setDescription(desc);
        message.setDescriptionEn(descEn);
        message.setOccurredAt(LocalDateTime.now());
        orderProducer.sendTrack(message);
    }
}
