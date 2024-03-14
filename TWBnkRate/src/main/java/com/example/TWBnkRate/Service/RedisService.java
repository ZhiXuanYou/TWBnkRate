/*package com.example.TWBnkRate.Service;

import com.example.TWBnkRate.mapper.RateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    @Autowired
    private static LineNotifyService lineNotifyService;

    private final RateMapper rateMapper;

    private static RedisTemplate<String, String> redisTemplate;

    private static HashOperations<String, String, Double> hashOperations;

    public RedisService(RateMapper rateMapper) {
        this.rateMapper = rateMapper;
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }


    //---------------------------------------------------------Redis---------------------------------------------------------
    // 更新最新匯率到Redis
    public static void updateCurrencyRate(String currencyCode, Double rate) {
        // 使用貨幣代碼和當前日期的特定格式作為 Redis 鍵
        /*SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String key = currencyCode + "_" + dateFormat.format(new Date());*/
/*        String key = currencyCode;
        // 存儲最新匯率到 Redis
        hashOperations.put("currency_rates", key, rate);

        // 設置鍵的生存時間為 24 小時
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    // 從Redis中獲取匯率
    public static Double getCurrencyRate(String currencyCode) {
        // 使用提供的日期鍵作為 Redis 鍵
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String key = currencyCode + "_" + dateFormat.format(new Date());

        // 從 Redis 中獲取匯率
        return hashOperations.get("currency_rates", key);
    }

}
*/