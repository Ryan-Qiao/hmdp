package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public Result seckillVoucher(Long voucherId) {
        /**
         * 先根据优惠券id获取到秒杀券
         * 判断下单时间是否在有效期内
         */
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("抢购未开始！");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("抢购已结束！");
        }
        /**
         * 判断库存是否充足
         */
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }


        Long userId = UserHolder.getUser().getId();
        /**
         * Long userId 是一个包装类型对象，而 不同的线程即使 userId 数值相同，也可能是不同的对象实例。
         * 1️⃣ userId.toString()
         * 把 Long 转成字符串，比如 1001L → "1001"。
         * 字符串内容相同，但对象可能还不一样（不同的 String 实例）。
         * 2️⃣ .intern()
         * 是关键点 🔑。
         * String.intern() 的作用是：
         * 把字符串放入 JVM 的字符串常量池中（String Pool），
         * 并返回池中唯一的那个引用。
         * 这种做法虽然巧妙，但有两个隐患：
         * 	1.	intern() 使用字符串常量池，可能造成内存压力（用户数很大时）。
         * 	2.	适用于单机部署，在分布式环境中，锁只在当前 JVM 内有效。
         */
//        synchronized (userId.toString().intern()) {
//            /**
//             * Spring 的 @Transactional 是靠 动态代理 实现的。
//             * 当外部调用一个 @Transactional 方法时：
//             * 	1.	Spring 通过代理对象拦截调用；
//             * 	2.	代理在调用前开启事务；
//             * 	3.	调用结束后提交或回滚事务。
//             *
//             * 	内部调用这个函数不会走代理，Transactional就失效了，只有外部调用走代理，Transactional才会有效
//             * 	Spring 默认使用 JDK 动态代理（实现了接口），所以这里转为IVoucherOrderService
//             */
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();//获取当前代理对象
//            /**
//             * AopContext.currentProxy() 返回的是 当前 Bean 的代理对象，而 Spring 默认使用 JDK 动态代理（基于接口），所以返回类型是 IVoucherOrderService 接口
//             */
//            return proxy.createVoucherOrder(voucherId);
//        }

        //这里是用自己实现的锁
        //SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate,"order:"+userId);
        //这里是用redisson实现的锁
        RLock lock = redissonClient.getLock("order:" + userId);
        boolean isLock = lock.tryLock();
        if(!isLock){
            return Result.fail("一人仅可下一单！");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }

    //Transactional要想有效果，需要Spring利用他的代理对象进行执行，而直接在类中调用这个函数不会触发Transactional
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        /**
         * 限制一人一单
         */
        Long userId = UserHolder.getUser().getId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            //用户已经购买过
            return Result.fail("用户已经购买！");
        }

        /**
         * 前置条件没问题后，开始秒杀的逻辑
         * 扣库存，这里是mp写法
         */
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1")//set条件
                .eq("voucher_id", voucherId).gt("stock", 0)//where条件 采用cas乐观锁解决超卖问题
                .update();
        if (!success) {
            return Result.fail("库存不足！");
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);

        voucherOrder.setUserId(userId);

        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);

    }


}
