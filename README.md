# 整合台銀匯率
- 台銀: 
 0. 匯率 API, Method: GET<https://rate.bot.com.tw/xrt/fltxt/0/day>
 1. 檢查裡面是不是有 **USD, CNY, JPY** 等匯率，缺一不可, 若有異常, 需要記錄.
 2. 若**連續 30 分鐘** , 無法更新匯率，則要發出 **Line Notify**
 3. **每天 16:59,** **備份當天**最後一次的匯率資料 
 4. 該排程**每一分鐘**, 向台銀取一次最新的匯率資料
 5. 監聽匯率功能, 如果達標, 也要發出 **Line Notify.
       EX. 今日元**匯率: 0.22, 監聽目標匯率: 0.23, 則發出通知
 6. 查詢歷史匯率的功能. (跟第三點有關)
 7. 即時匯率查詢功能. (跟第四點有關, 且不能從 DB 查詢, 避開大併發的情況)

專案規劃流程圖:
https://sun-dogsled-ff7.notion.site/Line-461ce6cfbc9c49d2bbabc2da76c1e0a9?pvs=4

成果:
https://sun-dogsled-ff7.notion.site/Line-20240105-24605e1521db4dfaa3942d9d7751b04e?pvs=4
