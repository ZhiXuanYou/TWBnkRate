package com.example.TWBnkRate.domain;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "rates")
public class Rate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    //幣別
    @Column(length = 30, nullable = false)
    private String currency;

    //匯率
    @Column(precision = 12, scale = 4, nullable = false)
    private BigDecimal rate;

    //買入現金
    @Column(precision = 12, scale = 4, nullable = false)
    private BigDecimal buyCash;

    //買入即期
    @Column(precision = 12, scale = 4, nullable = false)
    private BigDecimal buyRate;

    //賣出現金
    @Column(precision = 12, scale = 4, nullable = false)
    private BigDecimal saleCash;

    //賣出即期
    @Column(precision = 12, scale = 4, nullable = false)
    private BigDecimal saleRate;

    //日期
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date date;

    public Rate() {
    }

    public Rate(String currency, BigDecimal buyCash, BigDecimal buyRate, BigDecimal saleCash, BigDecimal saleRate, BigDecimal rate, Date date) {
        this.currency = currency;
        this.rate = rate;
        this.buyCash = buyCash;
        this.buyRate = buyRate;
        this.saleCash = saleCash;
        this.saleRate = saleRate;
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getbuyCash() {
        return buyCash;
    }

    public void setbuyCash(BigDecimal buyCash) {
        this.buyCash = buyCash;
    }

    public BigDecimal getbuyRate() {
        return buyRate;
    }

    public void setbuyRate(BigDecimal buyRate) {
        this.buyRate = buyRate;
    }

    public BigDecimal getsaleCash() {
        return saleCash;
    }

    public void setsaleCash(BigDecimal saleCash) {
        this.saleCash = saleCash;
    }

    public BigDecimal getsaleRate() {
        return saleRate;
    }

    public void setsaleRate(BigDecimal saleRate) {
        this.saleRate = saleRate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
