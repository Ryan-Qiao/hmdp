package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1760918400L;
    /**
     * 时间戳左移位数
     */
    private static final int COUNT_BITS = 32;
    @Resource
    StringRedisTemplate stringRedisTemplate;


    //使用keyPrefix区分不同业务的id前缀
    public long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

//        System.out.println(date);
        long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);
        return timestamp<<COUNT_BITS | count;

    }

}


