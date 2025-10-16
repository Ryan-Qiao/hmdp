package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result shopTypeList() {
        // 1.从Redis中查找商品列表缓存
        List<String> cacheList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        // 2.判断是否存在
        if (cacheList != null && cacheList.size() != 0) {
            // 3.存在返回商铺信息
            //字符串转换为ShopType List<String> -> List<ShopType>
            ArrayList<ShopType> shopTypeList = new ArrayList<>();
            for (String cache : cacheList) {
                ShopType shopType = JSONUtil.toBean(cache, ShopType.class);
                shopTypeList.add(shopType);
            }
            return Result.ok(shopTypeList);
        }
        // 4.不存在，查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 5.数据库没列表，返回404
        if (shopTypes == null) {
            return Result.fail("商品列表为空！");
        }
        // 6.数据库有列表数据，写入Redis
        /*stringRedisTemplate.opsForValue().set("cache:shop_type:",Convert.toStr(shopTypes));*/
        for (ShopType shopType : shopTypes) {
            stringRedisTemplate.opsForList().rightPush(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopType));
        }
        // 7.返回数据
        return Result.ok(shopTypes);

    }

}

