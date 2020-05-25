package com.miaosha.controller;

import com.miaosha.error.BusinessException;
import com.miaosha.error.EmBusinessError;
import com.miaosha.mq.MqProducer;
import com.miaosha.response.CommonReturnType;
import com.miaosha.service.ItemService;
import com.miaosha.service.OrderService;
import com.miaosha.service.PromoService;
import com.miaosha.service.model.UserModel;
import javafx.geometry.Pos;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

@Controller("order")
@RequestMapping(value = "/order")

//解决ajax跨域调用问题
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends BaseController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemService itemService;

    //spring内嵌的redis操作类
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private PromoService promoService;

    //使用ExecutorService来创建线程池，实现队列泄洪
    private ExecutorService executorService;

    @PostConstruct
    public void init(){
        //设置线程数为20，超出20的部分请求进入等待队列
        executorService = Executors.newFixedThreadPool(20);
    }

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
                                        @RequestParam(name = "promoId", required = false) Integer promoId,
                                        @RequestParam(name = "token") String token,
                                        @RequestParam(name = "promoToken", required = false) String promoToken) throws BusinessException {
        //判断用户是否登陆
        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
        }

        //从redis中获取用户信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
        }

        //判断是否为活动商品,是的话验证秒杀令牌是否存在
        if (promoId != null){
            //是活动商品，判读秒杀令牌是否存在
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_"+promoId+"_userId_"+userModel.getId()+"_itemId_"+itemId);
            if (inRedisPromoToken == null){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌不存在");
            }
            if (!StringUtils.equals(promoToken, inRedisPromoToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌不存在");
            }
        }


//        旧版本的登录验证
//        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
//        if (isLogin==null || !isLogin.booleanValue()){
//            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
//        }
//        从session中获取用户信息，旧版本弃用，现在用token从redis中获取用户信息
//        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("user");
        //orderService.createOrder(itemId, userModel.getId(), amount, promoId);

        //调用线程池中的线程处理请求，请求处理完后返回future
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入流水单号
                String stockLogId = itemService.initStockLog(itemId, amount);

                if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), promoId, itemId, amount, stockLogId)){
                    throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "异步消息发送失败，请联系网站管理员");
                }
                return null;
            }
        });

        //获取future，返回前端
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOW_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOW_ERROR);
        }


        return CommonReturnType.create(null);
    }

    /**
     * 生成秒杀令牌接口
     * @param itemId
     * @param promoId
     * @param token
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "generateToken", method = RequestMethod.POST)
    public CommonReturnType generateToken(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "promoId") Integer promoId,
                                        @RequestParam(name = "token") String token) throws BusinessException {
        //根据token获取用户信息
        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆");
        }

        //获取秒杀令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if (promoToken == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }
        return CommonReturnType.create(promoToken);
    }

}
