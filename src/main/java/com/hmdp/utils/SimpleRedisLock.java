package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate,String name){
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }
    private static final String KEY_PREFIX = "lock:";
    @Override
    public boolean tryLock(long timeoutSec){

        long id = Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, id + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    @Override
    public void unLock(){
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }

}
