package com.miaosha.service.Impl;

import com.miaosha.dao.PromoDOMapper;
import com.miaosha.dataobject.PromoDO;
import com.miaosha.service.PromoService;
import com.miaosha.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

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
