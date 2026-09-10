package com.crossmall.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossmall.common.exception.BizException;
import com.crossmall.dto.LineDTO;
import com.crossmall.dto.SpuSaveDTO;
import com.crossmall.entity.Category;
import com.crossmall.entity.LogisticsLine;
import com.crossmall.entity.Sku;
import com.crossmall.entity.Spu;
import com.crossmall.mapper.CategoryMapper;
import com.crossmall.mapper.LogisticsLineMapper;
import com.crossmall.mapper.SkuMapper;
import com.crossmall.mapper.SpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端:商品(SPU/SKU)/分类/物流线路 维护
 */
@Service
@RequiredArgsConstructor
public class AdminGoodsService {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final LogisticsLineMapper lineMapper;
    private final StockService stockService;

    // ==================== 商品 ====================

    public Page<Map<String, Object>> pageSpu(long page, long size, String keyword, Long categoryId) {
        Page<Spu> spuPage = spuMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Spu>()
                        .eq(categoryId != null, Spu::getCategoryId, categoryId)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Spu::getTitle, keyword).or().like(Spu::getTitleEn, keyword))
                        .orderByDesc(Spu::getId));
        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, List<Sku>> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                        .in(Sku::getSpuId, spuPage.getRecords().stream().map(Spu::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(Sku::getSpuId));

        Page<Map<String, Object>> result = new Page<>(spuPage.getCurrent(), spuPage.getSize(), spuPage.getTotal());
        result.setRecords(spuPage.getRecords().stream().map(spu -> {
            List<Sku> skuList = skus.getOrDefault(spu.getId(), List.of());
            Map<String, Object> row = JSONUtil.parseObj(JSONUtil.toJsonStr(spu));
            row.put("categoryName", categoryNames.get(spu.getCategoryId()));
            row.put("skuCount", skuList.size());
            row.put("minPriceCents", skuList.stream().mapToLong(Sku::getPriceCents).min().orElse(0));
            row.put("totalStock", skuList.stream().mapToInt(Sku::getStock).sum());
            row.put("skus", skuList);
            return row;
        }).collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public void saveSpu(SpuSaveDTO dto) {
        Spu spu = new Spu();
        spu.setId(dto.getId());
        spu.setCategoryId(dto.getCategoryId());
        spu.setTitle(dto.getTitle());
        spu.setTitleEn(dto.getTitleEn());
        spu.setDescription(dto.getDescription());
        spu.setDescriptionEn(dto.getDescriptionEn());
        spu.setBrand(dto.getBrand());
        spu.setHsCode(dto.getHsCode());
        spu.setWeightGrams(dto.getWeightGrams());
        spu.setMainImage(dto.getMainImage());
        spu.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) {
            spu.setSales(0);
            spuMapper.insert(spu);
        } else {
            if (spuMapper.selectById(dto.getId()) == null) {
                throw new BizException(404, "商品不存在");
            }
            spuMapper.updateById(spu);
        }

        if (dto.getSkus() != null) {
            for (SpuSaveDTO.SkuSaveDTO skuDTO : dto.getSkus()) {
                Sku sku = new Sku();
                sku.setId(skuDTO.getId());
                sku.setSpuId(spu.getId());
                sku.setAttrs(skuDTO.getAttrs());
                sku.setPriceCents(skuDTO.getPriceCents());
                sku.setStock(skuDTO.getStock());
                sku.setWeightGrams(skuDTO.getWeightGrams());
                sku.setStatus(skuDTO.getStatus() == null ? 1 : skuDTO.getStatus());
                if (skuDTO.getId() == null) {
                    skuMapper.insert(sku);
                } else {
                    skuMapper.updateById(sku);
                }
                // 库存变更同步 Redis 闸门(以 DB 为准覆盖)
                stockService.syncFromDb(sku.getId());
            }
        }
    }

    @Transactional
    public void updateSpuStatus(Long spuId, Integer status) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(404, "商品不存在");
        }
        spu.setStatus(status);
        spuMapper.updateById(spu);
    }

    // ==================== 分类 ====================

    public List<Category> listCategory() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort));
    }

    @Transactional
    public void saveCategory(Category category) {
        if (category.getId() == null) {
            category.setStatus(category.getStatus() == null ? 1 : category.getStatus());
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
    }

    @Transactional
    public void deleteCategory(Long id) {
        Long used = spuMapper.selectCount(new LambdaQueryWrapper<Spu>()
                .eq(Spu::getCategoryId, id));
        if (used > 0) {
            throw new BizException(400, "分类下仍有商品,无法删除");
        }
        categoryMapper.deleteById(id);
    }

    // ==================== 物流线路 ====================

    public List<LogisticsLine> listLines() {
        return lineMapper.selectList(new LambdaQueryWrapper<LogisticsLine>()
                .orderByAsc(LogisticsLine::getId));
    }

    @Transactional
    public void saveLine(LineDTO dto) {
        LogisticsLine line = new LogisticsLine();
        line.setId(dto.getId());
        line.setName(dto.getName());
        line.setNameEn(dto.getNameEn());
        line.setCarrier(dto.getCarrier());
        line.setSupportedCountries(JSONUtil.toJsonStr(dto.getSupportedCountries()));
        line.setFirstWeightGrams(dto.getFirstWeightGrams());
        line.setFirstFeeCents(dto.getFirstFeeCents());
        line.setContinueWeightGrams(dto.getContinueWeightGrams());
        line.setContinueFeeCents(dto.getContinueFeeCents());
        line.setEtaMinDays(dto.getEtaMinDays());
        line.setEtaMaxDays(dto.getEtaMaxDays());
        line.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) {
            lineMapper.insert(line);
        } else {
            lineMapper.updateById(line);
        }
    }

    @Transactional
    public void deleteLine(Long id) {
        lineMapper.deleteById(id);
    }
}
