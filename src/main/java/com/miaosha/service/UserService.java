package com.miaosha.service;

import com.miaosha.controller.viewobject.UserVO;
import com.miaosha.dao.UserDOMapper;
import com.miaosha.dao.UserPasswordDOMapper;
import com.miaosha.error.BusinessException;
import com.miaosha.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public interface UserService {
    public UserModel getUser(int id);

    public void register(UserModel userModel) throws BusinessException;

    public UserModel login(String telPhone, String password) throws BusinessException;
}
