package com.example.TWBnkRate.Service;

import com.example.TWBnkRate.domain.Rate;
import com.example.TWBnkRate.domain.UserRate;
import com.example.TWBnkRate.mapper.RateMapper;
import com.example.TWBnkRate.mapper.UserRateMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.messaging.model.FlexBox;
import com.linecorp.bot.messaging.model.FlexBubble;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;
import com.linecorp.bot.model.response.BotApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.linecorp.bot.client.LineMessagingClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@Service
public class LineBotService {
    @Autowired
    private UserRateMapper userRateMapper;
    @Autowired
    private LineMessagingClient lineMessagingClient;
    @Autowired
    private RateService rateService;

    //查詢所有用戶設定的匯率標準
    public List<UserRate> findALLUserRate()throws Exception {
        return userRateMapper.findAllUserRate();
    }
    //查詢指定用戶的匯率標準
    public List<UserRate> findUserRateByUserId(String userId)throws Exception {
        return userRateMapper.findUserRateByUserId(userId);
    }

    //解析webhook回傳的json並做出相對應的功能邏輯
    public CompletableFuture<String> callbackMessage(String payload)throws Exception {
        // 解析 JSON
        String receivedText = parseReceivedText(payload);

        //如果型態是message才跑邏輯
        String type = this.parseReceivedText_type(payload);
        if (type.equals("message")){
            // 提取 replyToken
            String replyToken = parseReceivedText_replyToken(payload);

            String userId = parseReceivedText_userId(payload);

            if (receivedText.equals(("設定匯率標準"))) {
                // 使用 Quick Reply 回應用戶的訊息
                Message quickReplyMessage = createQuickReplyMessage();
                ReplyMessage replyMessage = new ReplyMessage(replyToken, quickReplyMessage);
                lineMessagingClient.replyMessage(replyMessage);
                /*
                lineMessagingClient.replyMessage(new ReplyMessage(replyToken, quickReplyMessage))
                        .thenApply(reply -> {
                            // 處理成功的情況
                            return reply;
                        })
                        .exceptionally(ex -> {
                            // 處理異常情況
                            ex.printStackTrace();
                            return null;  // 或者返回一個默認值
                        });*/
            } else {
                if (receivedText.equals("我要設定JPY匯率達標標準") || receivedText.equals("我要設定CNY匯率達標標準") || receivedText.equals("我要設定USD匯率達標標準")){
                    switch (receivedText) {
                        case "我要設定JPY匯率達標標準":
                            //this.sendMessage(userId, "JPY", "0", replyToken);
                            this.settingRateFlexMsg("JPY", replyToken);
                            break;
                        case "我要設定CNY匯率達標標準":
                            //this.sendMessage(userId, "CNY", "0", replyToken);
                            this.settingRateFlexMsg("CNY", replyToken);
                            break;
                        case "我要設定USD匯率達標標準":
                            //this.sendMessage(userId, "USD", "0", replyToken);
                            this.settingRateFlexMsg("USD", replyToken);
                            break;
                        default:
                    }
                } else if (receivedText.contains("JPY:") || receivedText.contains("CNY:") || receivedText.contains("USD:")){
                    this.checkInputNum(receivedText, userId, replyToken);

                } else if (receivedText.equals(("查看我設定的匯率標準"))){
                    //this.selectUserRateByUserId(userId, replyToken);
                    this.settingAndSearchRateByFlexMsg(replyToken, userId);
                } else if (receivedText.equals(("查看目前最新匯率"))){
                    // 使用 Quick Reply 回應用戶的訊息
                    Message quickReplyMessage = createQuickReplyMessage_selectLastRate();
                    ReplyMessage replyMessage = new ReplyMessage(replyToken, quickReplyMessage);
                    lineMessagingClient.replyMessage(replyMessage);
                    /*
                    lineMessagingClient.replyMessage(new ReplyMessage(replyToken, quickReplyMessage))
                            .thenApply(reply -> {
                                // 處理成功的情況
                                return reply;
                            })
                            .exceptionally(ex -> {
                                // 處理異常情況
                                ex.printStackTrace();
                                return null;  // 或者返回一個默認值
                            });*/
                } else if (receivedText.equals("我要查看JPY目前最新匯率") || receivedText.equals("我要查看CNY目前最新匯率") || receivedText.equals("我要查看USD目前最新匯率")){
                    switch (receivedText) {
                        case "我要查看JPY目前最新匯率":
                            //this.selectLastRate("JPY", userId, replyToken);
                            this.getLastRateByFlexMsg("JPY", userId, replyToken);
                            break;
                        case "我要查看CNY目前最新匯率":
                            //this.selectLastRate("CNY", userId, replyToken);
                            this.getLastRateByFlexMsg("CNY", userId, replyToken);
                            break;
                        case "我要查看USD目前最新匯率":
                            //this.selectLastRate("USD", userId, replyToken);
                            this.getLastRateByFlexMsg("USD", userId, replyToken);
                            break;
                        default:
                    }
                } else if (receivedText.equals(("設定匯率達標通知頻率"))){
                    this.settingNotifyTimesFlexMsg(replyToken, userId);
                } else if (receivedText.equals(("一天一次"))){
                    userRateMapper.UpdateNotifyTimesByUserId(0, userId);
                    this.sendMessage(userId, "設定通知頻率完成", "1", replyToken);
                    rateService.deleteLastNotifyTime(userId);
                } else if (receivedText.equals(("半天一次"))){
                    userRateMapper.UpdateNotifyTimesByUserId(1, userId);
                    this.sendMessage(userId, "設定通知頻率完成", "1", replyToken);
                    rateService.deleteLastNotifyTime(userId);
                }
                else {
                    this.sendMessage(userId, "很抱歉,我不知道りしれ供さ小,請依照指定的格式輸入\n(可點選圖文選單依據指示操作)", "1", replyToken);
                }
            }

            //this.sendMessage(userId);
            //this.sendSticker(userId);

        }
        return CompletableFuture.completedFuture("Callback processed successfully!");
    }

    // 使用 Quick Reply 回應用戶的訊息--->設定匯率標準
    private Message createQuickReplyMessage()throws Exception {
        QuickReply quickReply = QuickReply.builder()
                .items(Arrays.asList(
                        QuickReplyItem.builder()
                                .action(new MessageAction("JPY", "我要設定JPY匯率達標標準"))
                                .build(),
                        QuickReplyItem.builder()
                                .action(new MessageAction("CNY", "我要設定CNY匯率達標標準"))
                                .build(),
                        QuickReplyItem.builder()
                                .action(new MessageAction("USD", "我要設定USD匯率達標標準"))
                                .build()
                ))
                .build();

        return TextMessage.builder()
                .text("請選擇你要設定的匯率")
                .quickReply(quickReply)
                .build();
    }

    // 使用 Quick Reply 回應用戶的訊息--->查看目前最新匯率
    private Message createQuickReplyMessage_selectLastRate()throws Exception {
        QuickReply quickReply = QuickReply.builder()
                .items(Arrays.asList(
                        QuickReplyItem.builder()
                                .action(new MessageAction("查看JPY目前最新匯率", "我要查看JPY目前最新匯率"))
                                .build(),
                        QuickReplyItem.builder()
                                .action(new MessageAction("查看CNY目前最新匯率", "我要查看CNY目前最新匯率"))
                                .build(),
                        QuickReplyItem.builder()
                                .action(new MessageAction("查看USD目前最新匯率", "我要查看USD目前最新匯率"))
                                .build()
                ))
                .build();

        return TextMessage.builder()
                .text("請選擇你查看的匯率")
                .quickReply(quickReply)
                .build();
    }

    //檢查輸入的匯率標準是否為"數值"
    private void checkInputNum(String input, String userId, String replyToken)throws Exception
    {
        //使用冒號分隔字串
        String[] parts = input.split(":");
        // 判斷是否有足夠的部分
        if (parts.length >= 2) {
            String firstPart = parts[0].trim();
            // 取得第二部分
            String secondPart = parts[1].trim();

            try {
                // 嘗試將第二部分轉換為Double
                //double number = Double.parseDouble(secondPart);
                BigDecimal rate = new BigDecimal(secondPart);

                //將用戶設定的匯率寫入DB
                int DataCountByUserId = userRateMapper.findCountUserRateByUserId(userId);
                if (DataCountByUserId == 0){
                    //代表需要Insert這個用戶的"匯率標準"資料到DB
                    userRateMapper.InsertUserRate(firstPart, rate, userId);
                } else {
                    //代表此用戶已經設定過"匯率標準",直接幫他做更新就可以
                    userRateMapper.UpdateUserRateByUserId(firstPart, rate, userId);
                }

                this.sendMessage(userId, String.format("%s匯率標準設定完成", firstPart), "1", replyToken);
                //System.out.println("第二部分是數字: " + number);
            } catch (NumberFormatException e) {
                this.sendMessage(userId, String.format("匯率標準請輸入數值 ex:%s:0.032", firstPart), "1", replyToken);
            }
        } else {
            this.sendMessage(userId, "很抱歉,我不知道りしれ供さ小,請依照指定的格式輸入\n(可點選圖文選單依據指示操作)", "1", replyToken);
        }
    }

    //查看我設定的匯率標準
    private void selectUserRateByUserId(String userId, String replyToken)throws Exception
    {
        List<UserRate> userRateList = userRateMapper.findUserRateByUserId(userId);
        //String ResultMsg = String.format("您設定的匯率標準如下: \n JPY:%s \n CNY:%s \n USD:%s", );

        if (!userRateList.isEmpty()) {
            UserRate firstUserRate = userRateList.stream().findFirst().orElse(null);

            if (firstUserRate != null) {
                String resultMsg = String.join("\n",
                        "您設定的匯率標準如下:",
                        "JPY:" + firstUserRate.getJpy(),
                        "CNY:" + firstUserRate.getCny(),
                        "USD:" + firstUserRate.getUsd());

                this.sendMessage(userId, resultMsg, "1", replyToken);
            }
        } else {
            // 如果 userRateList 為空，表示沒有找到匯率數據
            this.sendMessage(userId, "您尚未設定匯率標準喔! \n可以點選圖文選單來設定:)", "1", replyToken);
        }
    }

    //查看目前最新匯率(從redis獲取)
    private void selectLastRate(String xCurrency, String userId, String replyToken)throws Exception
    {
        List<Rate> getLatestData = rateService.findLatestDataRateByCurrency(xCurrency);
        if (!getLatestData.isEmpty()) {
            Rate firstRate = getLatestData.stream().findFirst().orElse(null);

            if (firstRate != null) {
                String resultMsg = String.join("\n",
                        String.format("目前%s最新匯率", xCurrency),
                        "本行買入 現金 : " + firstRate.getbuyCash(),
                        "本行買入 即期 : " + firstRate.getbuyRate(),
                        "本行賣出 現金 : " + firstRate.getsaleCash(),
                        "本行賣出 即期 : " + firstRate.getsaleRate());

                this.sendMessage(userId, resultMsg, "1", replyToken);
            }
        } else {
            // 如果 userRateList 為空，表示沒有找到匯率數據
            this.sendMessage(userId, "查不到目前最新匯率", "1", replyToken);
        }
    }

    //回復訊息給用戶
    public String sendMessage(@RequestBody String userId, String pMsg, String pType, String pReplyToken)throws Exception {
        // 替换为你自己的消息内容
        String messageText = null;
        if (pType.equals(("0"))) {
            messageText = String.format("請輸入「%s:您要設定的匯率標準」\n ex:%s:0.032", pMsg, pMsg);
        } else {
            messageText = pMsg;
        }

        // 创建 TextMessage 对象
        TextMessage textMessage = new TextMessage(messageText);

        //if (!pReplyToken.equals(Optional.empty())){
        if (pReplyToken != ""){
            //(被動回覆)
            replyTextMessage(pReplyToken, messageText);
            return "Message sent successfully!";
        } else {
            // (主動回覆)--->匯率達標通知用戶時使用
            PushMessage pushMessage = new PushMessage(userId, textMessage);
            lineMessagingClient.pushMessage(pushMessage);
            return "Message sent successfully!";
        }
    }

    //被動回復
    private void replyTextMessage(String replyToken, String text) throws Exception{
        TextMessage textMessage = new TextMessage(text);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, textMessage);
        lineMessagingClient.replyMessage(replyMessage);
    }

    //回復貼圖給用戶
    public String sendSticker(@RequestBody String userId)throws Exception {
        // 替换为你自己的贴纸 ID 和包 ID
        String packageId = "789";
        String stickerId = "10855";

        // 创建 StickerMessage 对象
        StickerMessage stickerMessage = new StickerMessage(packageId, stickerId);

        // 创建 ReplyMessage 对象，指定接收者的用户 ID 和消息
        PushMessage replyMessage = new PushMessage(userId, stickerMessage);

        try {
            // 使用 LineMessagingClient 的 replyMessage 方法发送消息
            //lineMessagingClient.replyMessage(replyMessage).get();
            BotApiResponse response = lineMessagingClient.pushMessage(replyMessage).get();
            return "Sticker sent successfully!";
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Error sending sticker: " + e.getMessage();
        }
    }

    //解析Json--->取的用戶傳送過來的問字內容
    private String parseReceivedText(String jsonString)throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode textNode = jsonNode.at("/events/0/message/text");
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing received text";
        }
    }

    //解析Json--->取的用戶的回覆Token
    private String parseReceivedText_replyToken(String jsonString)throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode textNode = jsonNode.at("/events/0/replyToken");
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing received text";
        }
    }

    //解析Json--->取的用戶的userId
    private String parseReceivedText_userId(String jsonString)throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode textNode = jsonNode.at("/events/0/source/userId");
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing received text";
        }
    }

    //解析Json--->取的用戶的動作[unfollow/follow/message]
    private String parseReceivedText_type(String jsonString)throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode textNode = jsonNode.at("/events/0/type");
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing received text";
        }
    }


    //Flex Message

    public void settingAndSearchRateByFlexMsg(String replyToken, String userId) throws Exception {

        List<UserRate> userRateList = userRateMapper.findUserRateByUserId(userId);
        //String ResultMsg = String.format("您設定的匯率標準如下: \n JPY:%s \n CNY:%s \n USD:%s", );
        String strJPY = "0.000"; //JPY匯率標準
        String strCNY = "0.000"; //CNY匯率標準
        String strUSD = "0.000"; //USD匯率標準
        //String JPYIsSet = ""; //是否設置JPY
        //String CNYIsSet = ""; //是否設置CNY
        //String USDIsSet = ""; //是否設置USD
        String JPYStatus = "未完成設置"; //是否設置JPY
        String CNYStatus = "未完成設置"; //是否設置CNY
        String USDStatus = "未完成設置"; //是否設置USD

        if (!userRateList.isEmpty()) {
            UserRate firstUserRate = userRateList.stream().findFirst().orElse(null);

            if (firstUserRate != null) {
                //strJPY = firstUserRate.getJpy().toString();
                strJPY = (firstUserRate.getJpy() != null) ? firstUserRate.getJpy().toString() : "0.000";
                //strCNY = firstUserRate.getCny().toString();
                strCNY = (firstUserRate.getCny() != null) ? firstUserRate.getCny().toString() : "0.000"; // 使用預設值替代 null
                //strUSD = firstUserRate.getUsd().toString();
                strUSD = (firstUserRate.getUsd() != null) ? firstUserRate.getUsd().toString() : "0.000";
            }
        }

        if (!strJPY.equals("0.000")){
            JPYStatus = "已完成設置";
        }
        if (!strCNY.equals("0.000")){
            CNYStatus = "已完成設置";
        }
        if (!strUSD.equals("0.000")){
            USDStatus = "已完成設置";
        }

        // 這裡填入你的 JSON 字串
        String flexJson = "{\n" +
                "  \"type\": \"carousel\",\n" +
                "  \"contents\": [\n" +
                "    {\n" +
                "      \"type\": \"bubble\",\n" +
                "      \"size\": \"nano\",\n" +
                "      \"header\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"JPY\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"align\": \"start\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"gravity\": \"center\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+JPYStatus+"\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"align\": \"start\",\n" +
                "            \"size\": \"xs\",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"margin\": \"lg\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"backgroundColor\": \"#27ACB2\",\n" +
                "        \"paddingTop\": \"19px\",\n" +
                "        \"paddingAll\": \"12px\",\n" +
                "        \"paddingBottom\": \"16px\"\n" +
                "      },\n" +
                "      \"body\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"horizontal\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"text\",\n" +
                "                \"text\": \""+strJPY+"\",\n" +
                "                \"color\": \"#8C8C8C\",\n" +
                "                \"size\": \"sm\",\n" +
                "                \"wrap\": true\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"md\",\n" +
                "        \"paddingAll\": \"12px\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"修改設置\",\n" +
                "              \"text\": \"我要設定JPY匯率達標標準\"\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"styles\": {\n" +
                "        \"footer\": {\n" +
                "          \"separator\": false\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"bubble\",\n" +
                "      \"size\": \"nano\",\n" +
                "      \"header\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"CNY\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"align\": \"start\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"gravity\": \"center\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+CNYStatus+"\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"align\": \"start\",\n" +
                "            \"size\": \"xs\",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"margin\": \"lg\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"backgroundColor\": \"#FF6B6E\",\n" +
                "        \"paddingTop\": \"19px\",\n" +
                "        \"paddingAll\": \"12px\",\n" +
                "        \"paddingBottom\": \"16px\"\n" +
                "      },\n" +
                "      \"body\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"horizontal\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"text\",\n" +
                "                \"text\": \""+strCNY+"\",\n" +
                "                \"color\": \"#8C8C8C\",\n" +
                "                \"size\": \"sm\",\n" +
                "                \"wrap\": true\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"md\",\n" +
                "        \"paddingAll\": \"12px\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"修改設置\",\n" +
                "              \"text\": \"我要設定CNY匯率達標標準\"\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"styles\": {\n" +
                "        \"footer\": {\n" +
                "          \"separator\": false\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"bubble\",\n" +
                "      \"size\": \"nano\",\n" +
                "      \"header\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"USD\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"align\": \"start\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"gravity\": \"center\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+USDStatus+"\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"align\": \"start\",\n" +
                "            \"size\": \"xs\",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"margin\": \"lg\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"backgroundColor\": \"#A17DF5\",\n" +
                "        \"paddingTop\": \"19px\",\n" +
                "        \"paddingAll\": \"12px\",\n" +
                "        \"paddingBottom\": \"16px\"\n" +
                "      },\n" +
                "      \"body\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"horizontal\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"text\",\n" +
                "                \"text\": \""+strUSD+"\",\n" +
                "                \"color\": \"#8C8C8C\",\n" +
                "                \"size\": \"sm\",\n" +
                "                \"wrap\": true\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"md\",\n" +
                "        \"paddingAll\": \"12px\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"text\": \"我要設定USD匯率達標標準\",\n" +
                "              \"label\": \"修改設置\"\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"styles\": {\n" +
                "        \"footer\": {\n" +
                "          \"separator\": false\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // 將 JSON 轉換為 FlexContainer
        FlexContainer flexContainer = convertJsonToFlexContainer(flexJson);

        // 發送 Flex Message
        sendFlexMessage(replyToken, flexContainer);
    }

    //用flexMsg傳送目前最新匯率給用戶
    public void getLastRateByFlexMsg(String xCurrency, String userId, String replyToken) throws Exception {

        List<Rate> getLatestData = rateService.findLatestDataRateByCurrency(xCurrency);
        if (!getLatestData.isEmpty()) {
            Rate firstRate = getLatestData.stream().findFirst().orElse(null);

            if (firstRate != null) {
                //String resultMsg = String.join("\n",
                        //String.format("目前%s最新匯率", xCurrency),
                String buyCash = firstRate.getbuyCash().toString();
                String buyRate = firstRate.getbuyRate().toString();
                String saleCash = firstRate.getsaleCash().toString();
                String saleRate = firstRate.getsaleRate().toString();

                // 這裡填入你的 JSON 字串
                String flexJson = "{\n" +
                        "  \"type\": \"bubble\",\n" +
                        "  \"body\": {\n" +
                        "    \"type\": \"box\",\n" +
                        "    \"layout\": \"vertical\",\n" +
                        "    \"contents\": [\n" +
                        "      {\n" +
                        "        \"type\": \"text\",\n" +
                        "        \"weight\": \"bold\",\n" +
                        "        \"color\": \"#1DB446\",\n" +
                        "        \"size\": \"xxl\",\n" +
                        "        \"contents\": [\n" +
                        "          {\n" +
                        "            \"type\": \"span\",\n" +
                        "            \"text\": \"目前"+xCurrency+"最新匯率\"\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"type\": \"separator\",\n" +
                        "        \"margin\": \"xxl\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"type\": \"box\",\n" +
                        "        \"layout\": \"vertical\",\n" +
                        "        \"margin\": \"xxl\",\n" +
                        "        \"spacing\": \"sm\",\n" +
                        "        \"contents\": [\n" +
                        "          {\n" +
                        "            \"type\": \"box\",\n" +
                        "            \"layout\": \"horizontal\",\n" +
                        "            \"contents\": [\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \"本行買入 現金\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#555555\",\n" +
                        "                \"flex\": 0\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \""+buyCash+"\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#111111\",\n" +
                        "                \"align\": \"end\"\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"type\": \"box\",\n" +
                        "            \"layout\": \"horizontal\",\n" +
                        "            \"contents\": [\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \"本行買入 即期\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#555555\",\n" +
                        "                \"flex\": 0\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \""+buyRate+"\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#111111\",\n" +
                        "                \"align\": \"end\"\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"type\": \"box\",\n" +
                        "            \"layout\": \"horizontal\",\n" +
                        "            \"contents\": [\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \"本行賣出 現金\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#555555\",\n" +
                        "                \"flex\": 0\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \""+saleCash+"\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#111111\",\n" +
                        "                \"align\": \"end\"\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"type\": \"box\",\n" +
                        "            \"layout\": \"horizontal\",\n" +
                        "            \"contents\": [\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \"本行賣出 即期\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#555555\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"type\": \"text\",\n" +
                        "                \"text\": \""+saleRate+"\",\n" +
                        "                \"size\": \"sm\",\n" +
                        "                \"color\": \"#111111\",\n" +
                        "                \"align\": \"end\"\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"type\": \"box\",\n" +
                        "            \"layout\": \"vertical\",\n" +
                        "            \"contents\": []\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"styles\": {\n" +
                        "    \"footer\": {\n" +
                        "      \"separator\": true\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";

                // 將 JSON 轉換為 FlexContainer
                FlexContainer flexContainer = convertJsonToFlexContainer(flexJson);

                // 發送 Flex Message
                sendFlexMessage(replyToken, flexContainer);
            }
        } else {
            // 如果 userRateList 為空，表示沒有找到匯率數據
            this.sendMessage(userId, "查不到目前最新匯率", "1", replyToken);
        }
    }

    //設定匯率標準,用flexMsg提供按鈕給用戶選取想設定的值
    public void settingRateFlexMsg(String xCurrency, String replyToken) throws Exception {
        //region JPY
        // 這裡填入你的 JSON 字串
        String flexJson_JPY = "{\n" +
                "  \"type\": \"bubble\",\n" +
                "  \"size\": \"mega\",\n" +
                "  \"header\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"JPY匯率標準設置\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"size\": \"xxl\",\n" +
                "            \"flex\": 4,\n" +
                "            \"weight\": \"bold\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"提供幾個預設值供選擇\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"size\": \"lg\",\n" +
                "            \"weight\": \"regular\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"若以下沒有想設定的值，\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"可以輸入「JPY:您要設定的匯率標準」\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" ex:JPY:0.20\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"paddingAll\": \"20px\",\n" +
                "    \"backgroundColor\": \"#0367D3\",\n" +
                "    \"spacing\": \"md\",\n" +
                "    \"height\": \"160px\",\n" +
                "    \"paddingTop\": \"22px\"\n" +
                "  },\n" +
                "  \"body\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"text\",\n" +
                "        \"text\": \"請直接點選您想設置的匯率標準\",\n" +
                "        \"color\": \"#b7b7b7\",\n" +
                "        \"size\": \"xs\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"horizontal\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"text\",\n" +
                "                \"text\": \" \",\n" +
                "                \"gravity\": \"center\",\n" +
                "                \"size\": \"sm\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderWidth\": \"2px\",\n" +
                "                \"borderColor\": \"#ffc400\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"0.20\",\n" +
                "              \"text\": \"JPY:0.20\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"80px\",\n" +
                "            \"flex\": 50,\n" +
                "            \"height\": \"md\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"0.21\",\n" +
                "              \"text\": \"JPY:0.21\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"0.22\",\n" +
                "              \"text\": \"JPY:0.22\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"0.23\",\n" +
                "              \"text\": \"JPY:0.23\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"0.24\",\n" +
                "              \"text\": \"JPY:0.24\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"0.25\",\n" +
                "              \"text\": \"JPY:0.25\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        //endregion

        //region CNY
        String flexJson_CNY = "{\n" +
                "  \"type\": \"bubble\",\n" +
                "  \"size\": \"mega\",\n" +
                "  \"header\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"CNY匯率標準設置\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"size\": \"xxl\",\n" +
                "            \"flex\": 4,\n" +
                "            \"weight\": \"bold\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"提供幾個預設值供選擇\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"size\": \"lg\",\n" +
                "            \"weight\": \"regular\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"若以下沒有想設定的值，\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"可以輸入「CNY:您要設定的匯率標準」\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" ex:CNY:4.20\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"paddingAll\": \"20px\",\n" +
                "    \"backgroundColor\": \"#0367D3\",\n" +
                "    \"spacing\": \"md\",\n" +
                "    \"height\": \"160px\",\n" +
                "    \"paddingTop\": \"22px\"\n" +
                "  },\n" +
                "  \"body\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"text\",\n" +
                "        \"text\": \"請直接點選您想設置的匯率標準\",\n" +
                "        \"color\": \"#b7b7b7\",\n" +
                "        \"size\": \"xs\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"horizontal\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"text\",\n" +
                "                \"text\": \" \",\n" +
                "                \"gravity\": \"center\",\n" +
                "                \"size\": \"sm\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderWidth\": \"2px\",\n" +
                "                \"borderColor\": \"#ffc400\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"4.20\",\n" +
                "              \"text\": \"CNY:4.20\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"80px\",\n" +
                "            \"flex\": 50,\n" +
                "            \"height\": \"md\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"4.25\",\n" +
                "              \"text\": \"CNY:4.25\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"4.30\",\n" +
                "              \"text\": \"CNY:4.30\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"4.35\",\n" +
                "              \"text\": \"CNY:4.35\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"4.40\",\n" +
                "              \"text\": \"CNY:4.40\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"4.45\",\n" +
                "              \"text\": \"CNY:4.45\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        //endregion

        //region USD
        String flexJson_USD = "{\n" +
                "  \"type\": \"bubble\",\n" +
                "  \"size\": \"mega\",\n" +
                "  \"header\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"USD匯率標準設置\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"size\": \"xxl\",\n" +
                "            \"flex\": 4,\n" +
                "            \"weight\": \"bold\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"提供幾個預設值供選擇\",\n" +
                "            \"color\": \"#ffffff\",\n" +
                "            \"size\": \"lg\",\n" +
                "            \"weight\": \"regular\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"若以下沒有想設定的值，\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"可以輸入「USD:您要設定的匯率標準」\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" ex:USD:30.50\",\n" +
                "            \"color\": \"#ffffff66\",\n" +
                "            \"size\": \"sm\",\n" +
                "            \"weight\": \"bold\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"paddingAll\": \"20px\",\n" +
                "    \"backgroundColor\": \"#0367D3\",\n" +
                "    \"spacing\": \"md\",\n" +
                "    \"height\": \"160px\",\n" +
                "    \"paddingTop\": \"22px\"\n" +
                "  },\n" +
                "  \"body\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"text\",\n" +
                "        \"text\": \"請直接點選您想設置的匯率標準\",\n" +
                "        \"color\": \"#b7b7b7\",\n" +
                "        \"size\": \"xs\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"horizontal\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"text\",\n" +
                "                \"text\": \" \",\n" +
                "                \"gravity\": \"center\",\n" +
                "                \"size\": \"sm\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderWidth\": \"2px\",\n" +
                "                \"borderColor\": \"#ffc400\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"30.50\",\n" +
                "              \"text\": \"USD:30.50\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"80px\",\n" +
                "            \"flex\": 50,\n" +
                "            \"height\": \"md\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"30.75\",\n" +
                "              \"text\": \"USD:30.75\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"31.00\",\n" +
                "              \"text\": \"USD:31.00\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"31.25\",\n" +
                "              \"text\": \"USD:31.25\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"31.50\",\n" +
                "              \"text\": \"USD:31.50\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"31.75\",\n" +
                "              \"text\": \"USD:31.75\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"baseline\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"horizontal\",\n" +
                "                \"contents\": [\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"box\",\n" +
                "                    \"layout\": \"vertical\",\n" +
                "                    \"contents\": [],\n" +
                "                    \"width\": \"2px\",\n" +
                "                    \"backgroundColor\": \"#ffc400\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"type\": \"filler\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"flex\": 1\n" +
                "              }\n" +
                "            ],\n" +
                "            \"width\": \"12px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"flex\": 50,\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#8c8c8c\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"height\": \"40px\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \" \",\n" +
                "            \"gravity\": \"center\",\n" +
                "            \"size\": \"sm\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"box\",\n" +
                "            \"layout\": \"vertical\",\n" +
                "            \"contents\": [\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"box\",\n" +
                "                \"layout\": \"vertical\",\n" +
                "                \"contents\": [],\n" +
                "                \"cornerRadius\": \"30px\",\n" +
                "                \"width\": \"12px\",\n" +
                "                \"height\": \"12px\",\n" +
                "                \"borderColor\": \"#ffc400\",\n" +
                "                \"borderWidth\": \"2px\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"type\": \"filler\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"flex\": 0,\n" +
                "            \"offsetEnd\": \"108px\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"button\",\n" +
                "            \"action\": {\n" +
                "              \"type\": \"message\",\n" +
                "              \"label\": \"32.00\",\n" +
                "              \"text\": \"USD:32.00\"\n" +
                "            },\n" +
                "            \"offsetEnd\": \"135px\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"spacing\": \"lg\",\n" +
                "        \"cornerRadius\": \"30px\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        //endregion

        String flexJson = flexJson_JPY;
        if (xCurrency.equals("CNY")){
            flexJson = flexJson_CNY;
        } else if (xCurrency.equals("USD")){
            flexJson = flexJson_USD;
        }
        // 將 JSON 轉換為 FlexContainer
        FlexContainer flexContainer = convertJsonToFlexContainer(flexJson);

        // 發送 Flex Message
        sendFlexMessage(replyToken, flexContainer);
    }

    //提供flexMsg按鈕讓用戶設定匯率達標通知頻率(一天一次OR半天一次)
    //一天一次 ---> DB值為0
    //半天一次 ---> DB值為1
    public void settingNotifyTimesFlexMsg(String replyToken, String userId) throws Exception {
        int NotifyTimes = userRateMapper.findNotifyTimesByUserId(userId);
        String NotifyTimesName = "一天一次";
        if (NotifyTimes == 1){
            NotifyTimesName = "半天一次(4小時)";
        }
        //region flexJson
        String flexJson = "{\n" +
                "  \"type\": \"bubble\",\n" +
                "  \"hero\": {\n" +
                "    \"type\": \"image\",\n" +
                "    \"size\": \"full\",\n" +
                "    \"aspectRatio\": \"20:13\",\n" +
                "    \"aspectMode\": \"cover\",\n" +
                "    \"action\": {\n" +
                "      \"type\": \"uri\",\n" +
                "      \"uri\": \"http://linecorp.com/\"\n" +
                "    },\n" +
                "    \"url\": \"https://canamcurrencyexchange.com/wp-content/uploads/Foreign-Currency-Exchange-Risk-Defined.jpg\"\n" +
                "  },\n" +
                "  \"body\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"text\",\n" +
                "        \"text\": \"匯率達標通知頻率\",\n" +
                "        \"weight\": \"bold\",\n" +
                "        \"size\": \"xl\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"vertical\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"◎當天匯率達標時,依據所選的頻率做通知\",\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#b7b7b7\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"◎預設為一天一次,可點選下方按鈕重新設定\",\n" +
                "            \"color\": \"#b7b7b7\",\n" +
                "            \"size\": \"xs\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"◎請注意,設定完匯率,會重新計算通知的時間\",\n" +
                "            \"size\": \"xs\",\n" +
                "            \"color\": \"#b7b7b7\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"footer\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"spacing\": \"sm\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"button\",\n" +
                "        \"style\": \"link\",\n" +
                "        \"height\": \"sm\",\n" +
                "        \"action\": {\n" +
                "          \"type\": \"message\",\n" +
                "          \"label\": \"一天一次\",\n" +
                "          \"text\": \"一天一次\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"button\",\n" +
                "        \"action\": {\n" +
                "          \"type\": \"message\",\n" +
                "          \"label\": \"半天一次(4小時)\",\n" +
                "          \"text\": \"半天一次\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"text\",\n" +
                "        \"text\": \"目前您設定的頻率為:"+NotifyTimesName+"\",\n" +
                "        \"size\": \"md\",\n" +
                "        \"color\": \"#f57d7d\",\n" +
                "        \"style\": \"normal\",\n" +
                "        \"weight\": \"bold\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"flex\": 0\n" +
                "  }\n" +
                "}";
        //endregion

        // 將 JSON 轉換為 FlexContainer
        FlexContainer flexContainer = convertJsonToFlexContainer(flexJson);

        // 發送 Flex Message
        sendFlexMessage(replyToken, flexContainer);
    }

    //匯率達標提醒用戶
    public void NotifyTimesFlexMsg(String userId, Map<String, String> LineMsgMap, String xCurrency, String formattedDate, String stardRate) throws Exception {
        //紀錄匯率是否達標以及匯率值
        String valueForBuyCash = LineMsgMap.get("buyCash");
        String valueForBuyRate = LineMsgMap.get("buyRate");
        String valueForSaleCash = LineMsgMap.get("saleCash");
        String valueForSaleRate = LineMsgMap.get("saleRate");

        String[] BuyCashParts = valueForBuyCash.split(";");
        String[] BuyRateParts = valueForBuyRate.split(";");
        String[] SaleCashParts = valueForSaleCash.split(";");
        String[] SaleRateParts = valueForSaleRate.split(";");

        //達標的匯率的顏色要特別標記
        String BuyCashColor = BuyCashParts[0].equals("已達標") ? "#cc0606" : "#aaaaaa";
        String BuyRateColor = BuyRateParts[0].equals("已達標") ? "#cc0606" : "#aaaaaa";
        String SaleCashColor = SaleCashParts[0].equals("已達標") ? "#cc0606" : "#aaaaaa";
        String SaleRateColor = SaleRateParts[0].equals("已達標") ? "#cc0606" : "#aaaaaa";

        //region flexJson
        String flexJson = "{\n" +
                "  \"type\": \"bubble\",\n" +
                "  \"body\": {\n" +
                "    \"type\": \"box\",\n" +
                "    \"layout\": \"vertical\",\n" +
                "    \"contents\": [\n" +
                "      {\n" +
                "        \"type\": \"text\",\n" +
                "        \"text\": \"幣別"+xCurrency+"匯率達標通知\",\n" +
                "        \"weight\": \"bold\",\n" +
                "        \"color\": \"#1DB446\",\n" +
                "        \"size\": \"xl\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"text\",\n" +
                "        \"text\": \""+formattedDate+"\",\n" +
                "        \"weight\": \"bold\",\n" +
                "        \"size\": \"md\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\",\n" +
                "        \"margin\": \"xxl\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"margin\": \"md\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"您設定的"+xCurrency+"匯率標準\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"flex\": 0,\n" +
                "            \"weight\": \"bold\",\n" +
                "            \"color\": \"#04265c\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+stardRate+"\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"align\": \"end\",\n" +
                "            \"weight\": \"bold\",\n" +
                "            \"color\": \"#04265c\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\",\n" +
                "        \"margin\": \"xxl\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"margin\": \"md\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"本行買入 現金 ["+ BuyCashParts[0] +"]\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"color\": \""+BuyCashColor+"\",\n" +
                "            \"flex\": 0\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+ BuyCashParts[1] +"\",\n" +
                "            \"color\": \""+BuyCashColor+"\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"align\": \"end\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\",\n" +
                "        \"margin\": \"xxl\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"margin\": \"md\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"本行買入 即期 ["+BuyRateParts[0]+"]\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"color\": \""+BuyRateColor+"\",\n" +
                "            \"flex\": 0\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+ BuyRateParts[1] +"\",\n" +
                "            \"color\": \""+BuyRateColor+"\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"align\": \"end\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\",\n" +
                "        \"margin\": \"xxl\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"margin\": \"md\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"本行賣出 現金 ["+SaleCashParts[0]+"]\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"color\": \""+SaleCashColor+"\",\n" +
                "            \"flex\": 0\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+ SaleCashParts[1] +"\",\n" +
                "            \"color\": \""+SaleCashColor+"\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"align\": \"end\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"separator\",\n" +
                "        \"margin\": \"xxl\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"box\",\n" +
                "        \"layout\": \"horizontal\",\n" +
                "        \"margin\": \"md\",\n" +
                "        \"contents\": [\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \"本行賣出 即期 ["+SaleRateParts[0]+"]\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"color\": \""+SaleRateColor+"\",\n" +
                "            \"flex\": 0\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"text\",\n" +
                "            \"text\": \""+ SaleRateParts[1] +"\",\n" +
                "            \"color\": \""+SaleRateColor+"\",\n" +
                "            \"size\": \"md\",\n" +
                "            \"align\": \"end\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"styles\": {\n" +
                "    \"footer\": {\n" +
                "      \"separator\": true\n" +
                "    }\n" +
                "  }\n" +
                "}";
        //endregion



        // 將 JSON 轉換為 FlexContainer
        FlexContainer flexContainer = convertJsonToFlexContainer(flexJson);

        // 發送 Flex Message
        sendFlexMessage_Init(userId, flexContainer);
    }

    private FlexContainer convertJsonToFlexContainer(String flexJson) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(flexJson, FlexContainer.class);
    }

    //被動
    private void sendFlexMessage(String replyToken, FlexContainer flexContainer) {
        FlexMessage flexMessage = new FlexMessage("Flex Message", flexContainer);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, flexMessage);
        lineMessagingClient.replyMessage(replyMessage);
    }

    //主動
    private void sendFlexMessage_Init(String userId, FlexContainer flexContainer) {
        FlexMessage flexMessage = new FlexMessage("Flex Message", flexContainer);
        //ReplyMessage replyMessage = new ReplyMessage(replyToken, flexMessage);
        //lineMessagingClient.replyMessage(replyMessage);

        PushMessage pushMessage = new PushMessage(userId, flexMessage);
        lineMessagingClient.pushMessage(pushMessage);
    }

}
