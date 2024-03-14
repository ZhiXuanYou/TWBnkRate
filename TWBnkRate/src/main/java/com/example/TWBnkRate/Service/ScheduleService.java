package com.example.TWBnkRate.Service;

import com.example.TWBnkRate.mapper.RateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ScheduleService {

    @Autowired
    private LineNotifyService lineNotifyService;
    //private static RedisService RedisService;
    @Autowired
    private RateMapper rateMapper;
    @Autowired
    private RateService RateService;

    /*public ScheduleService(RateMapper rateMapper) {
        this.rateMapper = rateMapper;
    }*/
    //---------------------------------------------------------自動觸發---------------------------------------------------------
    //每分鐘更新匯率
    //@Scheduled(cron = "0 * * * * *")
    //@Scheduled(cron = "0 * 0-17 * * *") //從 00:00 到 17:00，* 表示每天都觸發，0 表示每分鐘的第 0 秒觸發。
    /*public void autoTriggerDownload(boolean pAuto) {
        if (pAuto) {
            downloadFile();
        }
    }*/


    //@Scheduled(cron = "0 * 0-17 * * *") //從 00:00 到 17:00，* 表示每天都觸發，0 表示每分鐘的第 0 秒觸發。
    //@Scheduled(cron = "0 * * * * *") // 每分鐘觸發一次
    //@Scheduled(cron = "0 */5 * * * *") // 每5分鐘觸發一次
    @Scheduled(cron = "0 * * * * *") // 每分鐘觸發一次
    //@Scheduled(cron = "0 * 9-17 * * *") // [秒 分 時 日 月 星期]--->每天上午9點到下午5點的每分鐘觸發一次
    public void autoTriggerDownload() {
        try {
            RateService.downloadFile();
        } catch (Exception e) {
            // 處理ParseException或根據需要記錄錯誤。
            e.printStackTrace();
        }
        // 透過Line Notify發送消息
        //lineNotifyService.sendLineNotifyMessage("Scheduled task executed!");
    }
    /*public void autoTriggerDownload() {
        RateService.downloadFile();
        // 透過Line Notify發送消息
        //lineNotifyService.sendLineNotifyMessage("Scheduled task executed!");
    }*/

    @Scheduled(cron = "0 59 16 * * *") // 在每天的16:59觸發
    //@Scheduled(cron = "0 */3 * * * *")
    public void backupRateData() {
        List<String> currencyList = Arrays.asList("USD", "CNY", "JPY");

        currencyList.forEach(currency -> {
            String rate = RateService.getCurrencyRate(currency); //獲取現在redis最新匯率
            if (rate != null) {
                String[] rateParts = rate.split(";");
                BigDecimal[] rateValues = Arrays.stream(rateParts)
                        .map(part -> new BigDecimal(part))
                        .toArray(BigDecimal[]::new);

                if (rateValues.length == 4) {
                    BigDecimal buyCash = rateValues[0];
                    BigDecimal buyRate = rateValues[1];
                    BigDecimal saleCash = rateValues[2];
                    BigDecimal saleRate = rateValues[3];
                    rateMapper.insert(currency, buyCash, buyRate, saleCash, saleRate); //將匯率輩分到DB
                } /*else {
                    // 處理rateValues長度不等於4的情況，例如拋出異常或記錄錯誤訊息
                    // 這里只是一個示例，您可以根據實際需求進行處理
                    System.out.println("無效的rateValues長度: " + rateValues.length);
                    // 或者拋出一個自定義異常
                    throw new IllegalArgumentException("無效的rateValues長度");
                }*/
            }
        });
    }

 /*   public void backupRateData() {
        // 在這裡執行備份操作的程式碼
        // 可以使用定時任務將當天的最後一次匯率資料備份
        /*Rate newRate = new Rate(); // 创建一个新的 User 对象
        newRate.setName(user.getName());
        newRate.setAge(user.getAge());
        rateMapper.insert(newRate);*/
        //USD, CNY, JPY
        /*List<String> currencyList = new ArrayList<>();
        currencyList.add("USD");
        currencyList.add("CNY");
        currencyList.add("JPY");

        BigDecimal buyCash = BigDecimal.ZERO;
        BigDecimal buyRate = BigDecimal.ZERO;
        BigDecimal saleCash = BigDecimal.ZERO;
        BigDecimal saleRate = BigDecimal.ZERO;

        for (String currency : currencyList) {
            //備份當天16:59目前最新redis裡的匯率到DB
            String rate =  RateService.getCurrencyRate(currency);
            if (rate != null)
            {
                /*String[] parts = rate.split(";"); // 使用;分割

                // 假设parts数组中的元素是String类型
                BigDecimal buyCash = new BigDecimal(parts[0]);
                BigDecimal buyRate = new BigDecimal(parts[1]);
                BigDecimal saleCash = new BigDecimal(parts[2]);
                BigDecimal saleRate = new BigDecimal(parts[3]);*/

                /*String[] rateParts = rate.split(";");
                BigDecimal[] rateValues = Arrays.stream(rateParts)
                        .map(BigDecimal::new)
                        .toArray(BigDecimal[]::new);
                buyCash = rateValues[0];
                buyRate = rateValues[1];
                saleCash = rateValues[2];
                saleRate = rateValues[3];

                rateMapper.insert(currency, buyCash, buyRate, saleCash, saleRate);
            }
        }
    }
*/
}
