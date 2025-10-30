package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:"+id;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        if(BooleanUtil.isFalse(isMember)){
            boolean isSuccess = update().setSql("liked = liked+1").eq("id", id).update();
            if(BooleanUtil.isTrue(isSuccess)){
                stringRedisTemplate.opsForSet().add(key,userId.toString());
            }

        }
        else{
            boolean isSuccess = update().setSql("liked = liked-1").eq("id", id).update();
            if(BooleanUtil.isTrue(isSuccess)){
                stringRedisTemplate.opsForSet().remove(key,userId.toString());
            }

        }
        return Result.ok();
    }

    @Override
    public boolean isBlogLiked(Blog blog) {
        String key = "blog:liked:"+blog.getId();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, UserHolder.getUser().getId().toString());
        return BooleanUtil.isTrue(isMember);
    }
}
