package com.miaosha.service.model;

import java.math.BigDecimal;

public class OrderModel {
    //订单号id 16位 2020040800000100
    private String id;

    //用户id
    private Integer userId;

    //商品id
    private Integer itemId;

    //购买时商品价格,若promoId非口，则是秒杀价格
    private BigDecimal itemPrice;

    //订单总价,若promoId非口，则是秒杀商品价格
    private BigDecimal totalPrice;

    //购买数量
    private Integer amount;

    //若非空，表示是以秒杀活动方式下单
    private Integer promoId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
