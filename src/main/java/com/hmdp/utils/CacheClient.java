package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //isNotBlank
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        //解决缓存穿透问题，采用缓存空值的办法
        if (json != null) {
            return null;
        }


        R r = dbFallback.apply(id);
        if (r == null) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return r;
        }
        this.set(key, r, time, unit);
        return r;
    }


    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id,Class<R> type,Function<ID,R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject jsonObject = (JSONObject) redisData.getData();

        //拿到当前redis的数据（新旧都有可能）
        R r = JSONUtil.toBean(jsonObject, type);

        LocalDateTime expireTime = redisData.getExpireTime();
        //逻辑时间未过期，直接返回redis缓存
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
        //逻辑时间过期，查询数据库重建缓存
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        //启动新线程查询数据库，更新缓存
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
//                    this.saveShop2Redis(id, 20L);
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key,r1,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }

            });
        }
        return r;

        //解锁。。

    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }


}
