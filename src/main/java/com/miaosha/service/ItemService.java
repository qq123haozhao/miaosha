package com.miaosha.service;

import com.miaosha.error.BusinessException;
import com.miaosha.service.model.ItemModel;

import java.util.List;

public interface ItemService {
    //获取商品信息
    ItemModel getItemById(Integer id);

    //从缓存里获取商品信息
    ItemModel getItemByIdInCache(Integer id);

    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    //返回所有商品列表
    List<ItemModel> listItem();

    //扣减库存
    boolean decreaseStock(Integer itemId, Integer amount);

    //异步扣减库存
    boolean asyncDecreaseStock(Integer itemId, Integer amount);

    //回滚商品库存
    boolean increaseStock(Integer itemId, Integer amount);

    //增加销量
    void increaseSales(Integer itemId, Integer amount);

    //初始化库存流水信息,成功返回流水单号
    String initStockLog(Integer itemId, Integer amount);
}
