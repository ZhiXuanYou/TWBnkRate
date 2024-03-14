package com.example.TWBnkRate.Service;

import com.example.TWBnkRate.domain.Rate;
import com.example.TWBnkRate.domain.UserRate;
import com.example.TWBnkRate.mapper.RateMapper;
import com.example.TWBnkRate.mapper.UserRateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.NStringTypeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.tokens.StreamEndToken;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RateService {
    public static String downloadFailTime = null; //用來記錄下載匯率失敗的時間,連續超過30分鐘需要發送line給user

    SimpleDateFormat downloadFailTime_dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    Map<String, String> currencyRates = new HashMap<>(); //

    @Autowired
    private LineNotifyService lineNotifyService;

    @Autowired
    @Lazy
    private  LineBotService lineBotService;

    @Autowired
    private UserRateMapper userRateMapper;

    @Autowired
    private RestTemplate restTemplate;

    //private static RedisService RedisService;

    @Autowired
    private RateMapper rateMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private HashOperations<String, String, String> hashOperations;

    //Begin ---> [依據日期查詢匯率][查詢全部匯率][依據日期及幣別查看匯率]
    //[依據日期查詢匯率]
    public List<Rate> findRateByDate(String xDate) {
        return rateMapper.findRateByDate(xDate);
    }

    //[查詢全部匯率]
    public List<Rate> findALLRate() {
        return rateMapper.findALLRate();
    }

    //[依據日期及幣別查看匯率]
    public List<Rate> findRateByDateAndCurrency(String xCurrency, String xDate) {
        return rateMapper.findRateByDateAndCurrency(xCurrency, xDate);
    }

    //[即時匯率查詢功能]不能從 DB 查詢
    public List<Rate> findLatestDataRateByCurrency(String xCurrency) throws Exception {
        String redisKey = xCurrency + "_" + dateFormat.format(new Date());
        String dataFromRedis = hashOperations.get("currency_rates", redisKey);

        if (dataFromRedis != null) {
            String[] rateValues = dataFromRedis.split(";");
            Rate rate = new Rate();
            rate.setCurrency(xCurrency);
            if (rateValues.length >= 4) {
                rate.setbuyCash(new BigDecimal(rateValues[0]));
                rate.setbuyRate(new BigDecimal(rateValues[1]));
                rate.setsaleCash(new BigDecimal(rateValues[2]));
                rate.setsaleRate(new BigDecimal(rateValues[3]));
                rate.setDate(new Date());

                return Collections.singletonList(rate);
            }
        }
        return Collections.emptyList();
    }


    public void downloadFile()throws Exception{
        //當前日期
        String currentDateTime = dateFormat.format(new Date());
        String url = "https://rate.bot.com.tw/xrt/fltxt/0/day"; // 要下載的URL
        String userHome = System.getProperty("user.home");
        String desktopPath = userHome + "\\Desktop";
        String destinationPath = desktopPath + "\\RateDoc\\" + currentDateTime + ".txt";

        //try {
            // 獲取目標文件夾路徑
            Path destinationDirectory = new File(destinationPath).toPath().getParent();

            // 如果目標文件夾不存在，創建它
            if (!Files.exists(destinationDirectory)) {
                try {
                    Files.createDirectories(destinationDirectory);
                } catch (FileAlreadyExistsException e) {
                    // 如果文件夾已存在，繼續
                }
            }

            // 送出 HTTP GET 請求至 URL
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

            //使用了response.getStatusCode()来获得响应的HTTP状态码，并使用is2xxSuccessful()方法来检查状态码是否在2xx范围内。2xx范围内的状态码通常表示成功。
            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] fileData = response.getBody();

                // 儲存下載的檔案
                Path destination = new File(destinationPath).toPath();
                Files.write(destination, fileData);
                System.out.println("文件下載成功到：" + destinationPath);
            }

            //解析取得的匯率檔案
            boolean HasAllType = this.CheckRateType(destinationPath); //檢查是否包含USD, CNY, JPY
            if (!HasAllType) {
                handleDownloadFailure(desktopPath, currentDateTime, downloadFailTime);
            } else {
                System.out.println("匯率文件有包含USD, CNY, JPY");
                downloadFailTime = null; //匯率下載成功把失敗時間更新成null

                //更新最新匯率到Redis
                /*for (Map.Entry<String, String> entry : currencyRates.entrySet()) {
                    String currencyCode = entry.getKey(); // 幣別_日期
                    String rate = entry.getValue();
                    //this.(currencyCode, rate);
                    this.updateCurrencyRate_lineBot(currencyCode, rate);
                }*/
                currencyRates.forEach((currencyCode, rate) -> {
                    try {
                        //每分鐘觸發,循環用戶自訂的匯率,判斷是否達標,達標就通知用戶
                        this.updateCurrencyRate_lineBot(currencyCode, rate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
    }

    //下載失敗時--->判斷是否連續 30 分鐘 , 無法更新匯率
    private void handleDownloadFailure(String desktopPath, String currentDateTime, String downloadFailTime) throws Exception {
        createErrorLog(desktopPath, currentDateTime + " 下載匯率失敗");
        downloadFailTime = downloadFailTime_dateFormat.format(new Date());

        if (downloadFailTime.equals(Optional.empty())) {
            Date date1 = downloadFailTime_dateFormat.parse(downloadFailTime_dateFormat.format(new Date()));
            Date date2 = downloadFailTime_dateFormat.parse(downloadFailTime);

            long millisecondsDifference = date2.getTime() - date1.getTime();
            long minutesDifference = millisecondsDifference / (60 * 1000);

            if (minutesDifference >= 30) {
                lineNotifyService.sendLineNotifyMessage("連續 30 分鐘 , 無法更新匯率");
                downloadFailTime = downloadFailTime_dateFormat.format(new Date());
            }
        }
    }

    //匯率下載失敗,紀錄log
    public void createErrorLog(String desktopPath, String errorMessage) throws Exception {
        // 当前日期
        String currentDateTime = downloadFailTime_dateFormat.format(new Date());
        // 日志文件的路径
        String logFolderPath = desktopPath + "\\Log";
        String logFilePath = logFolderPath + "\\LOG_" + currentDateTime + ".txt";

        // 确认日志文件夹是否存在，不存在则创建它
        if (!Files.exists(Paths.get(logFolderPath))) {
            Files.createDirectories(Paths.get(logFolderPath));
        }

        // 创建日志文件，并写入错误消息
        Files.write(Paths.get(logFilePath), errorMessage.getBytes(), StandardOpenOption.CREATE);
        System.out.println("日志文件已创建：" + logFilePath);
    }


    //檢查裡面是不是有 USD, CNY, JPY 等匯率，缺一不可, 若有異常, 需要記錄.
    public boolean CheckRateType(String pFilePath) throws Exception {
        currencyRates = new HashMap<>();
        boolean[] hasCurrency = {false, false, false}; // [HasUSD, HasCNY, HasJPY]

        try (BufferedReader reader = new BufferedReader(new FileReader(pFilePath))) {
            reader.lines()
                    .takeWhile(line -> !(hasCurrency[0] && hasCurrency[1] && hasCurrency[2]))
                    .forEach(line -> {
                        String[] parts = line.split("\\s+");
                        String currency = parts[0];

                        if (line.contains("USD")) {
                            hasCurrency[0] = true;
                        } else if (line.contains("CNY")) {
                            hasCurrency[1] = true;
                        } else if (line.contains("JPY")) {
                            hasCurrency[2] = true;
                        } else {
                            return;
                        }

                        String buyCash = parts[2];
                        String buyRate = parts[3];
                        String saleCash = parts[12];
                        String saleRate = parts[13];
                        String allRate = buyCash + ";" + buyRate + ";" + saleCash + ";" + saleRate;

                        String key = currency + "_" + dateFormat.format(new Date());
                        currencyRates.put(key, allRate);
                    });
        }

        return hasCurrency[0] && hasCurrency[1] && hasCurrency[2];
    }



    //---------------------------------------------------------Redis---------------------------------------------------------
    // 更新最新匯率到Redis
    public void updateCurrencyRate(String currencyCode, String rate){
        // 使用貨幣代碼和當前日期的特定格式作為 Redis 鍵
        /*SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String key = currencyCode + "_" + dateFormat.format(new Date());*/
        String key = currencyCode;
        // 存儲最新匯率到 Redis
        hashOperations.put("currency_rates", key, rate);

        //判斷匯率是否達標,發賴通知user
        String[] currency_rates_parts = key.split("_"); // 使用;分割--->取[0]幣別
        /*String[] parts = rate.split(";"); // 使用;分割--->//買入現金;買入即期;賣出現金;賣出即期
        BigDecimal buyCash = new BigDecimal(parts[0]);
        BigDecimal buyRate = new BigDecimal(parts[1]);
        BigDecimal saleCash = new BigDecimal(parts[2]);
        BigDecimal saleRate = new BigDecimal(parts[3]);*/

        String[] rateParts = rate.split(";");
        /*BigDecimal[] rateValues = Arrays.stream(rateParts)
                .map(BigDecimal::new)
                .toArray(BigDecimal[]::new);*/
        BigDecimal[] rateValues = Arrays.stream(rateParts)
                .map(part -> new BigDecimal(part))
                .toArray(BigDecimal[]::new);

        if (rateValues.length >= 4){
            BigDecimal buyCash = rateValues[0];
            BigDecimal buyRate = rateValues[1];
            BigDecimal saleCash = rateValues[2];
            BigDecimal saleRate = rateValues[3];

            String lineMsg = ""; //達標時要發給user的訊息
            //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 指定日期格式
            String formattedDate = downloadFailTime_dateFormat.format(new Date());

            String stardRate; //匯率達標通知標準

            switch (currency_rates_parts[0]) { //enum20231109
                case "USD":
                    stardRate = "32";
                    break;
                case "CNY":
                    stardRate = "4.4";
                    break;
                default:
                    stardRate = "0.2";
                    break;
            }

            //if (stardRate != null)
            if (stardRate != null) {
                if (buyCash.compareTo(new BigDecimal(stardRate)) > 0) {
                    lineMsg +=  String.format("幣別%s ---> %s [買入現金]匯率達標%s \n",currency_rates_parts[0], formattedDate, buyCash.toString());
                }
                if (buyRate.compareTo(new BigDecimal(stardRate)) > 0) {
                    lineMsg +=  String.format("幣別%s ---> %s [買入即期]匯率達標%s \n",currency_rates_parts[0], formattedDate, buyRate.toString());
                }
                if (saleCash.compareTo(new BigDecimal(stardRate)) > 0) {
                    lineMsg +=  String.format("幣別%s ---> %s [賣出現金]匯率達標%s \n",currency_rates_parts[0], formattedDate, saleCash.toString());
                }
                if (saleRate.compareTo(new BigDecimal(stardRate)) > 0) {
                    lineMsg +=  String.format("幣別%s ---> %s [賣出即期]匯率達標%s \n",currency_rates_parts[0], formattedDate, saleRate.toString());
                }

            /*if (currency_rates_parts[0] == "USD") //USD
            {
                //31.9200	32.2450	32.5900	32.3950
                if (buyCash.compareTo(new BigDecimal("31")) > 0)
                {
                    lineMsg +=  String.format("幣別USD ---> %s [買入現金]匯率達標%s", formattedDate, buyCash.toString());
                }
                if (buyRate.compareTo(new BigDecimal("31")) > 0)
                {
                    lineMsg +=  String.format("幣別USD ---> %s [買入即期]匯率達標%s", formattedDate, buyRate.toString());
                }
                if (saleCash.compareTo(new BigDecimal("31")) > 0)
                {
                    lineMsg +=  String.format("幣別USD ---> %s [賣出現金]匯率達標%s", formattedDate, saleCash.toString());
                }
                if (saleRate.compareTo(new BigDecimal("31")) > 0)
                {
                    lineMsg +=  String.format("幣別USD ---> %s [賣出即期]匯率達標%s", formattedDate, saleRate.toString());
                }
            }
            else if (currency_rates_parts[0] == "CNY") //CNY
            {
                //4.3150	4.3820	4.4770	4.4420
                if (buyCash.compareTo(new BigDecimal("4")) > 0)
                {
                    lineMsg +=  String.format("幣別CNY ---> %s [買入現金]匯率達標%s", formattedDate, buyCash.toString());
                }
                if (buyRate.compareTo(new BigDecimal("4")) > 0)
                {
                    lineMsg +=  String.format("幣別CNY ---> %s [買入即期]匯率達標%s", formattedDate, buyRate.toString());
                }
                if (saleCash.compareTo(new BigDecimal("4")) > 0)
                {
                    lineMsg +=  String.format("幣別CNY ---> %s [賣出現金]匯率達標%s", formattedDate, saleCash.toString());
                }
                if (saleRate.compareTo(new BigDecimal("4")) > 0)
                {
                    lineMsg +=  String.format("幣別CNY ---> %s [賣出即期]匯率達標%s", formattedDate, saleRate.toString());
                }
            }
            else //JPY
            {
                //0.2065	0.2133	0.2193	0.2183
                if (buyCash.compareTo(new BigDecimal("0.2")) > 0)
                {
                    lineMsg +=  String.format("幣別JPY ---> %s [買入現金]匯率達標%s", formattedDate, buyCash.toString());
                }
                if (buyRate.compareTo(new BigDecimal("0.2")) > 0)
                {
                    lineMsg +=  String.format("幣別JPY ---> %s [買入即期]匯率達標%s", formattedDate, buyRate.toString());
                }
                if (saleCash.compareTo(new BigDecimal("0.2")) > 0)
                {
                    lineMsg +=  String.format("幣別JPY ---> %s [賣出現金]匯率達標%s", formattedDate, saleCash.toString());
                }
                if (saleRate.compareTo(new BigDecimal("0.2")) > 0)
                {
                    lineMsg +=  String.format("幣別JPY ---> %s [賣出即期]匯率達標%s", formattedDate, saleRate.toString());
                }
            }*/

                if (lineMsg != null) {
                    lineNotifyService.sendLineNotifyMessage("\n" + lineMsg);
                }
            }

            // 設置鍵的生存時間為 24 小時
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
    }

    //每分鐘觸發,循環用戶自訂的匯率,判斷是否達標,達標就通知用戶
    public void updateCurrencyRate_lineBot(String currencyCode, String rate)throws Exception{
        // 使用貨幣代碼和當前日期的特定格式作為 Redis 鍵
        String key = currencyCode;
        // 存儲最新匯率到 Redis
        hashOperations.put("currency_rates", key, rate);

        //判斷匯率是否達標,發賴通知user
        String[] currency_rates_parts = key.split("_"); // 使用;分割--->取[0]幣別

        String[] rateParts = rate.split(";");

        BigDecimal[] rateValues = Arrays.stream(rateParts)
                .map(part -> new BigDecimal(part))
                .toArray(BigDecimal[]::new);

        if (rateValues.length >= 4){

            String formattedDate = downloadFailTime_dateFormat.format(new Date());

            List<UserRate> allUserRateList = userRateMapper.findAllUserRate();
            if (!allUserRateList.isEmpty()) {
                allUserRateList.forEach(userRate -> {

                    // 檢查上次通知時間
                    long lastNotifyTime = getLastNotifyTime(userRate.getUserId(), currency_rates_parts[0]);

                    //從DB獲取用戶設定的通知頻率--->一天一次 = 8小時, 半天一次 = 4小時
                    int NotifyTimes = userRateMapper.findNotifyTimesByUserId(userRate.getUserId());

                    int Hours = 8;
                    if (NotifyTimes == 1){
                        Hours = 4;
                    }
                    // 如果距離上次通知已經超過4小時，則進行通知
                    if (lastNotifyTime == 0 || (System.currentTimeMillis() - lastNotifyTime >= TimeUnit.HOURS.toMillis(Hours))){
                        String lineMsg = ""; //達標時要發給user的訊息
                        BigDecimal buyCash = rateValues[0];
                        BigDecimal buyRate = rateValues[1];
                        BigDecimal saleCash = rateValues[2];
                        BigDecimal saleRate = rateValues[3];
                        BigDecimal stardRate; //匯率達標通知標準
                        switch (currency_rates_parts[0]) { //enum20231109
                            case "USD":
                                stardRate = userRate.getUsd();
                                break;
                            case "CNY":
                                stardRate = userRate.getCny();
                                break;
                            default:
                                stardRate = userRate.getJpy();
                                break;
                        }

                        Map<String, String> hashMap = new HashMap<>();
                        Boolean IsNotify = false;

                        //if (stardRate != null)
                        if (stardRate != null) {
                            if (buyCash.compareTo(stardRate) > 0) {
                                //lineMsg +=  String.format("%s [買入現金]匯率達標%s \n", formattedDate, buyCash.toString());
                                //lineMsg +=  "---------------------\n";
                                IsNotify = true;
                                hashMap.put("buyCash", "已達標;" + buyCash.toString());
                            } else {hashMap.put("buyCash", "未達標;" + buyCash.toString());}
                            if (buyRate.compareTo(stardRate) > 0) {
                                //lineMsg +=  String.format("%s [買入即期]匯率達標%s \n", formattedDate, buyRate.toString());
                                //lineMsg +=  "---------------------\n";
                                IsNotify = true;
                                hashMap.put("buyRate", "已達標;" + buyRate.toString());
                            }  else {hashMap.put("buyRate", "未達標;" + buyRate.toString());}
                            if (saleCash.compareTo(stardRate) > 0) {
                                //lineMsg +=  String.format("%s [賣出現金]匯率達標%s \n", formattedDate, saleCash.toString());
                                //lineMsg +=  "---------------------\n";
                                IsNotify = true;
                                hashMap.put("saleCash", "已達標;" + saleCash.toString());
                            }  else {hashMap.put("saleCash", "未達標;" + saleCash.toString());}
                            if (saleRate.compareTo(stardRate) > 0) {
                                //lineMsg +=  String.format("%s [賣出即期]匯率達標%s \n", formattedDate, saleRate.toString());
                                IsNotify = true;
                                hashMap.put("saleRate", "已達標;" + saleRate.toString());
                            }  else {hashMap.put("saleRate", "未達標;" + saleRate.toString());}

                            //if (lineMsg != null && !lineMsg.isEmpty()) {
                            if (IsNotify) {
                                //lineNotifyService.sendLineNotifyMessage("\n" + lineMsg);
                                try {
                                    //String lineMsg_result = String.format("☆★幣別%s <達標通知>☆★\n", currency_rates_parts[0]);
                                    //lineMsg_result += String.format("您設定的 %s 匯率標準為 : %s\n", currency_rates_parts[0], stardRate.toString());
                                    //lineBotService.sendMessage(userRate.getUserId(), lineMsg_result + lineMsg, "1", "");
                                    lineBotService.NotifyTimesFlexMsg(userRate.getUserId(), hashMap, currency_rates_parts[0], formattedDate, stardRate.toString());


                                    // 更新通知時間
                                    // 只有在通知成功之後才更新通知時間
                                    updateLastNotifyTime(userRate.getUserId(), System.currentTimeMillis(), currency_rates_parts[0]);

                                } catch (Exception sendMessageException) {
                                    // 處理發送訊息例外，例如紀錄或報警
                                    sendMessageException.printStackTrace();
                                }
                            }
                        }
                    }
                });
            }

            // 設置鍵的生存時間為 24 小時
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
    }

    // 更新 Redis 中上次通知時間
    private void updateLastNotifyTime(String userId, long notifyTime, String currency) {
        hashOperations.put("last_notify_times", userId + currency, String.valueOf(notifyTime));
    }

    // 從 Redis 中獲取上次通知時間，如果為空則返回預設值
    private long getLastNotifyTime(String userId, String currency) {
        String lastNotifyTimeStr = hashOperations.get("last_notify_times", userId + currency);
        return lastNotifyTimeStr != null ? Long.parseLong(lastNotifyTimeStr) : 0;
    }

    // 刪除 Redis 中特定 userId 的 last_notify_times 資料
    public void deleteLastNotifyTime(String userId) {
        hashOperations.delete("last_notify_times", userId + "JPY");
        hashOperations.delete("last_notify_times", userId + "CNY");
        hashOperations.delete("last_notify_times", userId + "USD");
    }

    // 從Redis中獲取匯率
    public String getCurrencyRate(String currencyCode) {
        // 使用提供的日期鍵作為 Redis 鍵
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String key = currencyCode + "_" + dateFormat.format(new Date());

        // 從 Redis 中獲取匯率
        //return hashOperations.get("currency_rates", key);

        // 從 Redis 中獲取匯率
        String rate = hashOperations.get("currency_rates", key);

        if (rate != null) {
            // 成功獲取到了值
            return rate;
        } else {
            // 未找到對應的鍵值
            System.out.println("未找到匯率");
            return "未找到匯率";

        }
    }
}
