package com.miaosha.service.Impl;


import com.miaosha.dao.PromoDOMapper;
import com.miaosha.dataobject.PromoDO;
import com.miaosha.error.BusinessException;
import com.miaosha.error.EmBusinessError;
import com.miaosha.service.ItemService;
import com.miaosha.service.PromoService;
import com.miaosha.service.UserService;
import com.miaosha.service.model.ItemModel;
import com.miaosha.service.model.PromoModel;
import com.miaosha.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;


    /**
     * 根据商品id获取活动信息
     * @param itemId
     * @return
     */
    @Override
    public PromoModel selectPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        if (promoDO == null){
            return null;
        }

        PromoModel promoModel = convertPromoModelFromPromoDO(promoDO);

        //设置活动状态
        if (promoModel.getStartTime().isAfterNow()){
            promoModel.setPromoStatus(1);
        }else if (promoModel.getEndTime().isBeforeNow()){
            promoModel.setPromoStatus(3);
        }else {
            promoModel.setPromoStatus(2);
        }

        return promoModel;
    }

    /**
     * 发布活动
     * @param promoId
     */
    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO.getItemId() == null || promoDO.getItemId().intValue()==0){
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());

        //将活动商品库存存入redis
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(), itemModel.getStock());
        //初始化秒杀令牌数量
        redisTemplate.opsForValue().set("promo_door_count_"+promoId, itemModel.getStock().intValue()*5);
        //redisTemplate.expire("promo_item_stock_"+itemModel.getId(), 30, TimeUnit.MINUTES);

    }

    /**
     * 生成秒杀令牌
     * @param promoId
     * @param itemId
     * @param userId
     * @return
     */
    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {

        //判断库存是否已售罄
        if (redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertPromoModelFromPromoDO(promoDO);
        //判断活动是否正在进行
        if (promoModel==null){
            return null;
        }

        //验证商品是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null){
            return null;
        }

        //验证用户是否存在
        UserModel userModel = userService.getUserFromCache(userId);
        if (userModel == null){
            return null;
        }

        //设置活动状态
        if (promoModel.getStartTime().isAfterNow()){
            promoModel.setPromoStatus(1);
        }else if (promoModel.getEndTime().isBeforeNow()){
            promoModel.setPromoStatus(3);
        }else {
            promoModel.setPromoStatus(2);
        }

        if (promoModel.getPromoStatus()!=2){
            return null;
        }

        //令牌数量-1，小于零后全部返回null
        long result = redisTemplate.opsForValue().increment("promo_door_count_"+promoId, -1);
        if (result < 0){
            return null;
        }

        //生成token并存入redis,有效时间为5分钟
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userId_"+userId+"_itemId_"+itemId, token);
        redisTemplate.expire("promo_token_"+promoId+"_userId_"+userId+"_itemId_"+itemId, 5, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 从do转换为model
     * @param promoDO
     * @return
     */
    private PromoModel convertPromoModelFromPromoDO(PromoDO promoDO){
        if (promoDO==null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        //类型不同，手动赋值
        promoModel.setStartTime(new DateTime(promoDO.getStartTime()));
        promoModel.setEndTime(new DateTime(promoDO.getEndTime()));
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        return promoModel;

    }


}
