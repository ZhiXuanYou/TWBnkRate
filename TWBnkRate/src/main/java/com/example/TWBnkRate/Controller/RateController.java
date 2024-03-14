package com.example.TWBnkRate.Controller;

import com.example.TWBnkRate.Service.RateService;
import com.example.TWBnkRate.domain.Rate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping(value = "/rates")
public class RateController {

    private final RateService rateService;

    public RateController(RateService rateService) {
        this.rateService = rateService;
    }


    //Begin ---> [依據日期查詢匯率][查詢全部匯率][依據日期及幣別查看匯率]
    //[依據日期查詢匯率]
    @GetMapping("/{xDate}")
    public List<Rate> getRateByDate(@PathVariable String xDate) {
        return rateService.findRateByDate(xDate);
    }

    //[查詢全部匯率]
    @GetMapping("/all")
    public List<Rate> getALLRate() {
        return rateService.findALLRate();
    }

    //[依據日期及幣別查看匯率]
    @GetMapping("/{xCurrency}/{xDate}")
    public List<Rate> getRateByDateAndCurrency(@PathVariable String xCurrency, @PathVariable String xDate) {
        return rateService.findRateByDateAndCurrency(xCurrency, xDate);
    }

    //[即時匯率查詢功能]不能從 DB 查詢
    @GetMapping("/getLatestData/{xCurrency}")
    public List<Rate> getLatestData(@PathVariable String xCurrency)throws Exception {
        return rateService.findLatestDataRateByCurrency(xCurrency);
    }


    //end ---> [依據日期查詢匯率][查詢全部匯率][依據日期及幣別查看匯率]

    //訪問台銀API下載匯率
    @GetMapping("/downloadFile")
    public void downloadFile()throws Exception {
        //rateService.autoTriggerDownload(); //開始每分鐘自動觸發更新匯率
        rateService.downloadFile();
    }

    /*@PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestBody String body, @RequestHeader("X-Line-Signature") String signature) {
        // 您的 Webhook 處理邏輯
        // ...
        return ResponseEntity.ok("Callback processed successfully!");
    }*/



    /*@PostMapping("/stopAutoTrigger")
    public void stopAutoTrigger() {
        rateService.autoTriggerDownload(false); // 停止自動觸發
    }*/

}
