package com.miaosha.service;

import com.miaosha.service.model.PromoModel;

public interface PromoService {

    PromoModel selectPromoByItemId(Integer itemId);

    /**
     * 发布活动
     * @param promoId
     */
    void publishPromo(Integer promoId);
}
