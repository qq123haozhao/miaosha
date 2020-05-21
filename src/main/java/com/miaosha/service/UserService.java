package com.miaosha.service;

import com.miaosha.controller.viewobject.UserVO;
import com.miaosha.dao.UserDOMapper;
import com.miaosha.dao.UserPasswordDOMapper;
import com.miaosha.error.BusinessException;
import com.miaosha.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public interface UserService {
    UserModel getUser(int id);

    UserModel getUserFromCache(int id);

    void register(UserModel userModel) throws BusinessException;

    UserModel login(String telPhone, String password) throws BusinessException;
}
