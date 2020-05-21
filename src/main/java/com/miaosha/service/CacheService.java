package com.miaosha.service;

/**
 * @auhor: dhz
 * @date: 2020/5/21 15:52
 */

//封装本地缓存操作类
public interface CacheService {
    void setCommonCache(String key, Object object);

    Object getFromCommonCache(String key);

}
