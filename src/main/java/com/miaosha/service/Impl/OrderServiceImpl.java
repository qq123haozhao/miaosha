package com.miaosha.service.Impl;

import com.miaosha.dao.OrderDOMapper;
import com.miaosha.dao.SequenceDOMapper;
import com.miaosha.dataobject.ItemDO;
import com.miaosha.dataobject.OrderDO;
import com.miaosha.dataobject.PromoDO;
import com.miaosha.dataobject.SequenceDO;
import com.miaosha.error.BusinessException;
import com.miaosha.error.EmBusinessError;
import com.miaosha.service.ItemService;
import com.miaosha.service.OrderService;
import com.miaosha.service.PromoService;
import com.miaosha.service.UserService;
import com.miaosha.service.model.ItemModel;
import com.miaosha.service.model.OrderModel;
import com.miaosha.service.model.PromoModel;
import com.miaosha.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.print.attribute.standard.MediaSize;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private PromoService promoService;


    @Override
    public OrderModel createOrder(Integer itemId, Integer userId, Integer amount, Integer promoId) throws BusinessException {
        //1、校验订单信息，商品是否存在，用户信息是否正常，订单数量是否正常

        //校验商品是否存在
//        ---------弃用，改成从redis中验证--------
//        ItemModel itemModel = itemService.getItemById(itemId);
//        if (itemModel == null){
//            throw new BusinessException(EmBusinessError.ITEM_NOT_EXIST, "商品不存在");
//        }

        //验证商品是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null){
            throw new BusinessException(EmBusinessError.ITEM_NOT_EXIST, "商品不存在");
        }

//        --------------弃用-------------
        //校验用户信息是否正常
//        UserModel userModel = userService.getUser(userId);
//        if (userModel == null){
//            throw new BusinessException(EmBusinessError.USER_NOT_EXIST, "用户不存在");
//        }

        //从redis中验证用户信息
        UserModel userModel = userService.getUserFromCache(userId);
        if (userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST, "用户不存在");
        }

        //校验订单数量是否正常
        if (amount<=0 || amount>99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量信息不正确");
        }

        //校验是否为秒杀活动
        if (promoId!=null){
            //校验活动跟商品是否对应
            if (promoId.intValue()!=itemModel.getPromoModel().getId()){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息异常");
            }else if (itemModel.getPromoModel().getPromoStatus()!=2){
                //校验活动是否进行中
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动尚未开始");
            }
        }


        //2、落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //3、订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setItemId(itemId);
        orderModel.setUserId(userId);
        orderModel.setAmount(amount);

        if (promoId==null){
            //原价商品
            orderModel.setItemPrice(itemModel.getPrice());
        }else {
            //活动商品
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }

        orderModel.setTotalPrice(orderModel.getItemPrice().multiply(BigDecimal.valueOf(amount)));


        //生成订单流水线号
        String id = generateOrderNo();
        orderModel.setId(id);
        OrderDO orderDO = convertOrderDOFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);
        itemService.increaseSales(itemId, amount);

        //4、返回前端
        return orderModel;
    }

    /**
     * 将Order从model转换为dataObject方便存入数据库
     * @param orderModel
     * @return
     */
    private OrderDO convertOrderDOFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setTotalPrice(orderModel.getTotalPrice().doubleValue());
        return orderDO;
    }

    /**
     * 生成订单流水号
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo(){
        StringBuffer stringBuffer = new StringBuffer();

        //日期序列 例：20200408
        LocalDate now = LocalDate.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuffer.append(nowDate);

        //自增序列
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequence + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKey(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for (int i=0; i<6-sequenceStr.length(); i++){
            stringBuffer.append(0);
        }
        stringBuffer.append(sequenceStr);

        //分库分表号，暂时全部写为0
        stringBuffer.append("00");
        return stringBuffer.toString();


    }
}
