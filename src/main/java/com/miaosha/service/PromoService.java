package com.miaosha.service;

import com.miaosha.service.model.PromoModel;

public interface PromoService {

    PromoModel selectPromoByItemId(Integer itemId);
}
