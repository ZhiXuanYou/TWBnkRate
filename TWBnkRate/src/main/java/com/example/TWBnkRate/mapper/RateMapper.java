package com.example.TWBnkRate.mapper;

import com.example.TWBnkRate.domain.Rate;

import java.math.BigDecimal;
import java.util.List;

public interface RateMapper {

    //region [依據日期查詢匯率][查詢全部匯率][依據日期及幣別查看匯率]

    //[依據日期查詢匯率]
    List<Rate> findALLRate();

    //[查詢全部匯率]
    List<Rate> findRateByDate(String xDate);

    //[依據日期及幣別查看匯率]
    List<Rate> findRateByDateAndCurrency(String xCurrency, String xDate);

    //endregion

    void downloadFile()throws Exception;

    //buyCash[買入現金];[buyRate]買入即期;[saleCash]賣出現金;[saleRate]賣出即期
    //void insert(String currency, String buyCash, String buyRate, String saleCash, String saleRate);
    void insert(String currency, BigDecimal buyCash, BigDecimal buyRate, BigDecimal saleCash, BigDecimal saleRate);
}
