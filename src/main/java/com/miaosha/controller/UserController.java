package com.miaosha.controller;

import com.miaosha.controller.viewobject.UserVO;
import com.miaosha.error.BusinessException;
import com.miaosha.error.EmBusinessError;
import com.miaosha.response.CommonReturnType;
import com.miaosha.service.UserService;
import com.miaosha.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Controller("user")
@RequestMapping(value = "/user")

//解决ajax跨域调用问题
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")

public class UserController extends BaseController{

    @Autowired
    private UserService userServices;

    @Autowired
    private HttpServletRequest httpServletRequest;

    //spring 内嵌的redis操作类
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取用户接口
     * @param id
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name="id") Integer id) throws BusinessException{
        UserModel userModel = userServices.getUser(id);
        if (userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        UserVO userVO = convertFromModel(userModel);
        return CommonReturnType.create(userVO, "success");
    }

    private UserVO convertFromModel(UserModel userModel){
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

    /**
     * 获取验证码接口
     * @param telPhone
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/getOtp", method = RequestMethod.POST)
    public CommonReturnType getOtp(@RequestParam(name = "telPhone") String telPhone){
        //随机生成otp验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otp = String.valueOf(randomInt);

        //将验证码存进session并发送给用户，这里用打印代替发送给用户
        this.httpServletRequest.getSession().setAttribute(telPhone, otp);
        System.out.println("手机号:" + telPhone + " 验证码：" + otp);

        return CommonReturnType.create(null);
    }

    /**
     * 用户注册接口
     * @param telPhone
     * @param name
     * @param gender
     * @param age
     * @param password
     * @param otpCode
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws BusinessException
     */
    @ResponseBody
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public CommonReturnType register(@RequestParam(name = "telPhone") String telPhone,
                                        @RequestParam(name = "name") String name,
                                        @RequestParam(name = "gender") Integer gender,
                                        @RequestParam(name = "age") Integer age,
                                        @RequestParam(name = "password") String password,
                                        @RequestParam(name = "otpCode") String otpCode) throws UnsupportedEncodingException, NoSuchAlgorithmException, BusinessException {

        //从session中获取手机号对应的otp验证码
        String optCodeInSession = (String) this.httpServletRequest.getSession().getAttribute(telPhone);

        //用apache的字符串验证工具，该equals会先分别验证两个字符串是否为空
        if (!StringUtils.equals(otpCode, optCodeInSession)){
            return CommonReturnType.create(EmBusinessError.PARAMETER_VALIDATION_ERROR.getErrMsg(), "fail");
        }

        UserModel userModel = new UserModel();
        userModel.setTelphone(telPhone);
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setEncryptPassword(encodeByMd5(password));
        userModel.setRegisterMode("byPhone");

        userServices.register(userModel);

        return CommonReturnType.create(null);
    }


    /**
     * 用户登陆接口
     * @param telPhone
     * @param password
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public CommonReturnType login(@RequestParam(name = "telPhone")String telPhone,
                                  @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        if (StringUtils.isEmpty(telPhone) || StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        String encodePassword = encodeByMd5(password);
        UserModel userModel = userServices.login(telPhone, encodePassword);
        if (userModel == null){
            return CommonReturnType.create(EmBusinessError.LOGIN_ERROR);
        }


        //老版本中用session做登录凭证，现已弃用，改用token做登录验证
//        this.httpServletRequest.getSession().setAttribute("user", userModel);
//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);

        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-", "");
        redisTemplate.opsForValue().set(uuidToken, userModel);
        redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);

        //将token返回给前端
        return CommonReturnType.create(uuidToken);

    }

    //md5加密，java原生的md5加密只能加密16位
    private String encodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (StringUtils.isEmpty(str)){
            return null;
        }
        //设置加密方式
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();

        //加密字符串
        String newStr = base64Encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }


}
