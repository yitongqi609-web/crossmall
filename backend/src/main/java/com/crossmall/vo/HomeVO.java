package com.crossmall.vo;

import com.crossmall.entity.Category;
import lombok.Data;

import java.util.List;

@Data
public class HomeVO {

    private List<Category> categories;
    private List<GoodsCardVO> hotGoods;
}
