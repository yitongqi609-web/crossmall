package com.crossmall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端订单履约操作
 */
@Data
public class OrderActionDTO {

    /** 目标状态:STOCKED/DECLARED/IN_TRANSIT/CUSTOMS_CLEARANCE/DELIVERING/COMPLETED/REFUNDED */
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    /** 轨迹地点(空则用各节点默认文案) */
    private String location;

    private String description;
    private String descriptionEn;
}
