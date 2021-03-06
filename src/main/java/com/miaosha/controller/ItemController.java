package com.miaosha.controller;

import com.miaosha.controller.viewobject.ItemVO;
import com.miaosha.error.BusinessException;
import com.miaosha.error.EmBusinessError;
import com.miaosha.response.CommonReturnType;
import com.miaosha.service.CacheService;
import com.miaosha.service.ItemService;
import com.miaosha.service.PromoService;
import com.miaosha.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("item")
@RequestMapping(value = "/item")

//解决ajax跨域调用问题
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")

public class ItemController extends BaseController{

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    //本地缓存操作类，用google的guava cache实现
    @Autowired
    private CacheService cacheService;

    /**
     * 获取商品详情接口
     * @param id
     * @return
     */
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) throws BusinessException {
        ItemVO itemVO = new ItemVO();
        ItemModel itemModel = null;

        itemModel = (ItemModel) cacheService.getFromCommonCache("item_" + id);

        if (itemModel == null){
            //本地缓存没有则从redis取，并且加入本地缓存
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);
            if (itemModel == null){
                //redis没有再从数据库获取并存入reids
                itemModel = itemService.getItemById(id);
                redisTemplate.opsForValue().set("item_"+id, itemModel);
                redisTemplate.expire("item_"+id, 10, TimeUnit.MINUTES);
            }

            cacheService.setCommonCache("item_"+id, itemModel);
        }


        if (itemModel == null){
            throw new BusinessException(EmBusinessError.ITEM_NOT_EXIST);
        }
        itemVO = convertItemVOFromItemModel(itemModel);
        return CommonReturnType.create(itemVO);
    }

    /**
     * 获取所有商品接口
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList = itemService.listItem();
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = convertItemVOFromItemModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }

    /**
     * 创建商品接口
     * @param title
     * @param description
     * @param price
     * @param stock
     * @param imgUrl
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "stock") Integer stock,
                                       @RequestParam(name = "imgUrl") String imgUrl) throws BusinessException {

        //创建商品模型
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        //调用service层创建商品函数并返回创建成功的商品
        ItemModel itemModelForReturn = itemService.createItem(itemModel);

        //将返回的商品从model转换为VO并封装成CommonReturnType返回给前端
        ItemVO itemVO = convertItemVOFromItemModel(itemModelForReturn);
        return CommonReturnType.create(itemVO);
    }

    @RequestMapping(value = "/publishPromo", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam("promoId") Integer promoId){
        promoService.publishPromo(promoId);
        return CommonReturnType.create(null);
    }

    /**
     * 将商品从model转换为VO
     * @param itemModel
     * @return
     */
    private ItemVO convertItemVOFromItemModel(ItemModel itemModel){
        if (itemModel == null){
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if (itemModel.getPromoModel()!=null){
            itemVO.setPromoStatus(itemModel.getPromoModel().getPromoStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
            itemVO.setStartTime(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }
}
