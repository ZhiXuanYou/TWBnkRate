package com.example.TWBnkRate.mapper;

import com.example.TWBnkRate.domain.UserRate;

import java.math.BigDecimal;
import java.util.List;

public interface UserRateMapper {

    List<UserRate> findAllUserRate();

    List<UserRate> findUserRateByDate(String xUpdateDate);

    List<UserRate> findUserRateByUserId(String xUserId);

    int findCountUserRateByUserId(String xUserId);

    int findNotifyTimesByUserId(String xUserId);

    void InsertUserRate(String xCurrency, BigDecimal xRate, String xUserId);

    void UpdateUserRateByUserId(String xCurrency, BigDecimal xRate, String xUserId);

    void UpdateNotifyTimesByUserId(Integer notifyTimes, String xUserId);
}
