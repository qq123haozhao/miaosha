package com.miaosha.service;

import com.miaosha.error.BusinessException;
import com.miaosha.service.model.OrderModel;

public interface OrderService {
    OrderModel createOrder(Integer itemId, Integer userId, Integer amount, Integer promoId, String stockLogId) throws BusinessException;

    String generateOrderNo();
}
