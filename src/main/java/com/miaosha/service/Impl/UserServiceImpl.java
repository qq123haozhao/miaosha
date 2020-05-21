package com.miaosha.service.Impl;

import com.miaosha.controller.viewobject.UserVO;
import com.miaosha.dao.UserDOMapper;
import com.miaosha.dao.UserPasswordDOMapper;
import com.miaosha.dataobject.UserDO;
import com.miaosha.dataobject.UserPasswordDO;
import com.miaosha.error.BusinessException;
import com.miaosha.error.CommonError;
import com.miaosha.error.EmBusinessError;
import com.miaosha.service.UserService;
import com.miaosha.service.model.UserModel;
import com.miaosha.validator.ValidationResult;
import com.miaosha.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public UserModel getUser(int id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(id);
        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);
        return userModel;
    }

    /**
     * 从缓存中获取用户信息
     * @param id
     * @return
     */
    @Override
    public UserModel getUserFromCache(int id) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_"+id);
        if (userModel==null){
            userModel = this.getUser(id);
            redisTemplate.opsForValue().set("user_validate_"+id, userModel);
            redisTemplate.expire("user_validate_"+id, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    //从dataObject转为model
    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO){
        UserModel userModel = new UserModel();
        if (userDO != null){
            BeanUtils.copyProperties(userDO, userModel);
        }else {
            return null;
        }

        if (userPasswordDO != null){
            userModel.setEncryptPassword(userPasswordDO.getEncryptPassword());
        }
        return userModel;
    }


    /**
     * 注册函数
     * @param userModel
     * @throws BusinessException
     */
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        if (userModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        ValidationResult result = validator.validate(userModel);
        if (result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }


        UserDO userDO = convertFromUserModel(userModel);

        try {
            userDOMapper.insertSelective(userDO);
        }catch (DuplicateKeyException ex){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "该手机号已被注册");
        }


        userModel.setId(userDO.getId());

        UserPasswordDO userPasswordDO = convertPasswordFromUserModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);

        return;

    }

    /**
     * 登陆函数
     * @param telPhone
     * @param password
     * @return
     */
    @Override
    public UserModel login(String telPhone, String password) throws BusinessException {
        //判空处理
        if (StringUtils.isEmpty(telPhone) || StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //从数据库查找用户
        UserDO userDO = userDOMapper.selectByTelPhone(telPhone);
        if (userDO == null){
            throw new BusinessException(EmBusinessError.LOGIN_ERROR);
        }

        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        if (userPasswordDO == null){
            throw new BusinessException(EmBusinessError.LOGIN_ERROR);
        }
        if (!StringUtils.equals(password, userPasswordDO.getEncryptPassword())){
            throw new BusinessException(EmBusinessError.LOGIN_ERROR);
        }
        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);
        return userModel;
    }


    //将user从model转为do
    private UserDO convertFromUserModel(UserModel userModel){
        if (userModel == null){
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }

    //将password从model转为do
    private UserPasswordDO convertPasswordFromUserModel(UserModel userModel){
        if (userModel == null){
            return null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        BeanUtils.copyProperties(userModel, userPasswordDO);
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }
}
