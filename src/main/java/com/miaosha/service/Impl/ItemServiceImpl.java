package com.miaosha.service.Impl;

import com.miaosha.dao.ItemDOMapper;
import com.miaosha.dao.ItemStockDOMapper;
import com.miaosha.dao.StockLogDOMapper;
import com.miaosha.dataobject.ItemDO;
import com.miaosha.dataobject.ItemStockDO;
import com.miaosha.dataobject.StockLogDO;
import com.miaosha.error.BusinessException;
import com.miaosha.error.EmBusinessError;
import com.miaosha.mq.MqConsumer;
import com.miaosha.mq.MqProducer;
import com.miaosha.service.ItemService;
import com.miaosha.service.PromoService;
import com.miaosha.service.model.ItemModel;
import com.miaosha.service.model.PromoModel;
import com.miaosha.validator.ValidationResult;
import com.miaosha.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer producer;

    @Autowired
    private MqConsumer consumer;

    /**
     * 根据商品id获取商品
     * @param id
     * @return
     */
    @Override
    public ItemModel getItemById(Integer id) {
        //1、查询商品
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null){
            return null;
        }
        //2、查询商品销量
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        //3、从do模型转换为model模型
        ItemModel itemModel = convertItemModelFromDataObject(itemDO, itemStockDO);

        //4、查询商品是否有活动
        PromoModel promoModel = promoService.selectPromoByItemId(itemModel.getId());
        if (promoModel!=null && promoModel.getPromoStatus()!=3){
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    /**
     * 从redis中获取商品
     * @param id
     * @return
     */
    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_"+id);
        if (itemModel == null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_"+id, itemModel);
            redisTemplate.expire("item_validate_"+id, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    /**
     * 商品销量增加
     * @param itemId
     * @param amount
     */
    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemDOMapper.increaseSales(itemId, amount);
    }


    /**
     * 创建商品
     * @param itemModel
     * @return
     * @throws BusinessException
     */
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //入参校验
        ValidationResult result = validator.validate(itemModel);
        if (result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }

        //从model转化为do
        ItemDO itemDO = convertItemDOFromItemModel(itemModel);

        //存入数据库
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = convertItemStockDOFromItemModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);


        //返回创建完成的对象
        return getItemById(itemModel.getId());
    }

    /**
     * 获取所有商品
     * @return
     */
    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = convertItemModelFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }




    /**
     * 将ItemModel转换为ItemDO，用于插入数据库
     * @param itemModel
     * @return
     */
    private ItemDO convertItemDOFromItemModel(ItemModel itemModel){
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        return itemDO;
    }

    /**
     * 将ItemModel转换为ItemStockDO，用于插入数据库
     * @param itemModel
     * @return
     */
    private ItemStockDO convertItemStockDOFromItemModel(ItemModel itemModel){
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setStock(itemModel.getStock());
        itemStockDO.setItemId(itemModel.getId());
        return itemStockDO;
    }

    /**
     * 从ItemDO和ItemStockDO转换出ItemModel，用于返回给前端
     * @param itemDO
     * @param itemStockDO
     * @return
     */
    private ItemModel convertItemModelFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO){
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }

    /**
     * 减商品库存函数
     * @param itemId
     * @param amount
     * @return
     */
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
        long result = redisTemplate.opsForValue().increment("promo_item_stock_"+itemId, amount.intValue() * -1);
//        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
        if (result > 0){
            return true;
        }else if (result == 0){
            //打上售罄标志
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");
            return true;
        } else {
            //更新失败
//            redisTemplate.opsForValue().increment("promo_item_stock_"+itemId, amount.intValue());
            increaseStock(itemId, amount);
            return false;
        }
    }

    /**
     * 异步扣减库存
     * @param itemId
     * @param amount
     * @return
     */
    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean sendResult = producer.asyncReduceStock(itemId, amount);
        return sendResult;
    }

    /**
     * 回滚商品库存
     * @param itemId
     * @param amount
     * @return
     */
    @Override
    @Transactional
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_"+itemId, amount.intValue());
        return true;
    }

    //初始化对应的库存流水，在下单前调用，成功返回流水单号
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDO.setStatus(1);
        stockLogDOMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }
}
