package com.hd.hdbibackend.manager;

import com.hd.hdbibackend.common.ErrorCode;
import com.hd.hdbibackend.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @auther hd
 * @Description  专门提供限流服务的
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     *
     * @param key  区分不同的限流器，比如不同用户id分别统计
     */
    public void doRateLimit(String key) {
            //创建一个名称为user_limiter的限流器，每秒最多访间2次
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
            rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);

            //每当一个操作来了后，请求一个令牌
            boolean canOp = rateLimiter.tryAcquire(1);
            if (!canOp) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
            }
    }
}
