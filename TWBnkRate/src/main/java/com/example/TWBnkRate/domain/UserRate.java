package com.example.TWBnkRate.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "userRate")
public class UserRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ListId")
    private Integer listId;

    // 使用者ID
    @Column(name = "userId", length = 32, nullable = false, unique = true)
    private String userId;

    // 日圓
    @Column(name = "JPY", precision = 12, scale = 4)
    private BigDecimal jpy;

    // 人民幣
    @Column(name = "CNY", precision = 12, scale = 4)
    private BigDecimal cny;

    // 美元
    @Column(name = "USD", precision = 12, scale = 4)
    private BigDecimal usd;

    // 更新日期
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updateDate", nullable = false)
    private Date updateDate;

    // 通知次數
    @Column(name = "notifyTimes")
    private Integer notifyTimes;

    public UserRate() {
    }

    public UserRate(String userId, BigDecimal jpy, BigDecimal cny, BigDecimal usd, Date updateDate,Integer notifyTimes) {
        this.userId = userId;
        this.jpy = jpy;
        this.cny = cny;
        this.usd = usd;
        this.updateDate = updateDate;
        this.notifyTimes = notifyTimes;
    }

    public Integer getListId() {
        return listId;
    }

    public void setListId(Integer listId) {
        this.listId = listId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getJpy() {
        return jpy;
    }

    public void setJpy(BigDecimal jpy) {
        this.jpy = jpy;
    }

    public BigDecimal getCny() {
        return cny;
    }

    public void setCny(BigDecimal cny) {
        this.cny = cny;
    }

    public BigDecimal getUsd() {
        return usd;
    }

    public void setUsd(BigDecimal usd) {
        this.usd = usd;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getNotifyTimes() {
        return notifyTimes;
    }

    public void setNotifyTimes(Integer notifyTimes) {
        this.notifyTimes = notifyTimes;
    }
}
