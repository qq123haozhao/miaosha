package com.miaosha.service;

import com.miaosha.error.BusinessException;
import com.miaosha.service.model.UserModel;

public interface UserService {
    UserModel getUser(int id);

    UserModel getUserFromCache(int id);

    void register(UserModel userModel) throws BusinessException;

    UserModel login(String telPhone, String password) throws BusinessException;
}
