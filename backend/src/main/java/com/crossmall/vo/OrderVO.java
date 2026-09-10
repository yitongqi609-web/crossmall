package com.crossmall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情(含明细与履约轨迹)
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private String status;
    private String statusLabel;
    private String statusLabelEn;
    private AddressInfo address;
    private Long lineId;
    private String lineName;
    private String etaText;

    private String currency;
    private BigDecimal fxRate;
    private Long goodsFeeCents;
    private Long shippingFeeCents;
    private Long taxFeeCents;
    private Long totalFeeCents;
    private String displaySymbol;
    private Long totalDisplayPrice;

    private LocalDateTime payTimeoutAt;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private String cancelReason;
    private LocalDateTime createTime;

    private List<Item> items;
    private List<Track> tracks;

    @Data
    public static class AddressInfo {
        private String receiverName;
        private String phone;
        private String countryCode;
        private String countryName;
        private String stateProvince;
        private String city;
        private String street;
        private String postcode;
    }

    @Data
    public static class Item {
        private Long spuId;
        private Long skuId;
        private String title;
        private String titleEn;
        private String attrs;
        private String image;
        private Long priceCents;
        private String displaySymbol;
        private Long priceDisplay;
        private Integer quantity;
        private Long subtotalCents;
        private Long subtotalDisplayPrice;
    }

    @Data
    public static class Track {
        private String status;
        private String location;
        private String description;
        private String descriptionEn;
        private LocalDateTime occurredAt;
    }
}
