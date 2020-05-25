package com.miaosha.service;

import com.miaosha.service.model.PromoModel;

public interface PromoService {

    /**
     * 根据商品id查询活动
     * @param itemId
     * @return
     */
    PromoModel selectPromoByItemId(Integer itemId);

    /**
     * 发布活动
     * @param promoId
     */
    void publishPromo(Integer promoId);

    /**
     * 生成秒杀令牌
     * @param promoId
     * @return
     */
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId);
}
