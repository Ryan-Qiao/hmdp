package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
//IShopService 相当于 定制化接口，它基于 IService<Shop> 扩展了一些可能的自定义方法
public interface IShopService extends IService<Shop> {


    Result queryById(Long id);

    Result update(Shop shop);
}
