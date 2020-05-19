package com.miaosha.controller;

import com.miaosha.error.BusinessException;
import com.miaosha.error.EmBusinessError;
import com.miaosha.response.CommonReturnType;
import com.miaosha.service.OrderService;
import com.miaosha.service.model.UserModel;
import javafx.geometry.Pos;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller("order")
@RequestMapping(value = "/order")

//解决ajax跨域调用问题
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends BaseController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private OrderService orderService;

    //spring内嵌的redis操作类
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 下单接口
     * @param itemId
     * @param amount
     * @return
     * @throws BusinessException
     */
    @ResponseBody
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId") Integer promoId,
                                        @RequestParam(name = "token") String token) throws BusinessException {

        //判断用户是否登陆
        //String token = httpServletRequest.getParameterMap().get("token")[0];
        System.out.println(token);
        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
        }

        //从redis中获取用户信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
        }

//        旧版本的登录验证
//        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
//        if (isLogin==null || !isLogin.booleanValue()){
//            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
//        }
//        从session中获取用户信息，旧版本弃用，现在用token从redis中获取用户信息
//        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("user");
        orderService.createOrder(itemId, userModel.getId(), amount, promoId);

        return CommonReturnType.create(null);
    }

}
