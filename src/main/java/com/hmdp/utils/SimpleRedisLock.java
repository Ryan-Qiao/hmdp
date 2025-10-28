package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate,String name){
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }
    @Override
    public boolean tryLock(long timeoutSec){
        //ID_PREFIX用来区分不同jvm，线程id用来区分同一jvm的不同线程
        String id = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, id, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    @Override
    public void unLock(){
        String id = ID_PREFIX + Thread.currentThread().getId();
        String lockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if(id.equals(lockId)){//判断锁是否是自己的必须和删除锁同时执行，保持原子性，否则可能会线程不安全
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }

}
