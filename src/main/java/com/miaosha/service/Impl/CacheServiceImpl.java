package com.miaosha.service.Impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.miaosha.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;


/**
 * @auhor: dhz
 * @date: 2020/5/21 15:56
 */
@Service
public class CacheServiceImpl implements CacheService {
    private Cache<String, Object> commonCache = null;

    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                //初始大小为10
                .initialCapacity(10)
                //最大大小为100
                .maximumSize(100)
                //设置写入后一分钟过期
                .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }

    @Override
    public void setCommonCache(String key, Object object) {
        commonCache.put(key, object);
    }

    @Override
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
