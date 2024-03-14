package com.example.TWBnkRate.Controller;

import com.example.TWBnkRate.Service.LineBotService;
import com.example.TWBnkRate.Service.RateService;
import com.example.TWBnkRate.domain.Rate;
import com.example.TWBnkRate.domain.UserRate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


import retrofit2.http.Header;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.DoubleStream;

@Component//("myLineBotController")
//@LineMessageHandler
@RestController
@RequestMapping(value = "/api/line")
/*
@RequestMapping("/api/line")*/
public class LineBotController {

    /*@Autowired
    private LineBotService lineBotService;

    @GetMapping("/send/{userId}/{message}")
    public String sendMessage(@PathVariable String userId, @PathVariable String message) {
        try {
            lineBotService.sendMessage(userId, message);
            return "Message sent successfully!";
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return "Error sending message";
        }
    }*/
    private  LineBotService lineBotService;



    public LineBotController(LineBotService lineBotService) {
        this.lineBotService = lineBotService;
    }
    @Autowired
    private LineMessagingClient lineMessagingClient;

    @PostMapping("/callback")
    public CompletableFuture<String> callbackMessage(@RequestBody String payload)throws Exception {
        return lineBotService.callbackMessage(payload);
    }

    @GetMapping("/all")
    public List<UserRate> getALLUserRate()throws Exception {
        return lineBotService.findALLUserRate();
    }

    @GetMapping("/{userId}")
    public List<UserRate> getALLUserRateByUserId(@PathVariable String userId)throws Exception {
        return lineBotService.findUserRateByUserId(userId);
    }







    @PostMapping("/callback2")
    public ResponseEntity<String> callback2(@RequestBody String payload, @RequestHeader("X-Line-Signature") String signature) {
        // 处理 Webhook 回调的逻辑
        // ...

        // 解析 JSON 字符串
        String receivedText = parseReceivedText(payload);
        // 提取 replyToken
        String replyToken = parseReceivedText_replyToken(payload);
        // 回复相同的文字消息
        replyTextMessage(replyToken, receivedText);
        return ResponseEntity.ok("Callback processed successfully!");
    }

    @PostMapping("/callbackX")
    public ResponseEntity<String> callbackMessageX(@RequestBody String payload) {
        // 处理 Webhook 回调的逻辑
        // ...

        // 解析 JSON 字符串
        String receivedText = parseReceivedText(payload);
        // 提取 replyToken
        String replyToken = parseReceivedText_replyToken(payload);

        String userId = parseReceivedText_userId(payload);

        if (receivedText.equals(("設定匯率標準"))) {
            // 使用 Quick Reply 回應用戶的訊息
            Message quickReplyMessage = createQuickReplyMessage();

            lineMessagingClient.replyMessage(new ReplyMessage(replyToken, quickReplyMessage))
                    .thenApply(reply -> {
                        // 處理成功的情況
                        return reply;
                    })
                    .exceptionally(ex -> {
                        // 處理異常情況
                        ex.printStackTrace();
                        return null;  // 或者返回一個默認值
                    });
        } else {
            if (receivedText.equals("我要設定JPY匯率達標標準") || receivedText.equals("我要設定CNY匯率達標標準") || receivedText.equals("我要設定USD匯率達標標準")){
                switch (receivedText) {
                    case "我要設定JPY匯率達標標準":
                        this.sendMessage(userId, "JPY", "0");
                        break;
                    case "我要設定CNY匯率達標標準":
                        this.sendMessage(userId, "CNY", "0");
                        break;
                    case "我要設定USD匯率達標標準":
                        this.sendMessage(userId, "USD", "0");
                        break;
                    // 可以有更多的 case
                    default:
                        // 如果上面的所有 case 都不匹配，则执行 default 代码块
                }
            } else if (receivedText.contains("JPY:") || receivedText.contains("CNY:") || receivedText.contains("USD:")){
                if (receivedText.contains("JPY:")) {
                    this.sendMessage(userId, "JPY匯率設定完成", "1");
                } else if (receivedText.contains("CNY:")) {
                    this.sendMessage(userId, "CNY匯率設定完成", "1");
                } else {
                    this.sendMessage(userId, "USD匯率設定完成", "1");
                }


            } else {
                this.sendMessage(userId, "很抱歉,我不知道りしれ供さ小,請依照指定的格式輸入", "1");
            }
        }


        //this.sendMessage(userId);
        //this.sendSticker(userId);





        return ResponseEntity.ok("Callback processed successfully!");
    }

    private Message createQuickReplyMessage() {
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

    @PostMapping("/send-message")
    public String sendMessage(@RequestBody String userId, String pMsg, String pType) {
        // 替换为你自己的消息内容
        String messageText = null;
        if (pType.equals(("0"))) {
            messageText = String.format("請輸入「%s:您要設定的匯率標準」\n ex:%s:0.032", pMsg, pMsg);
        } else {
            messageText = pMsg;
        }


        // 创建 TextMessage 对象
        TextMessage textMessage = new TextMessage(messageText);

        // 创建 PushMessage 对象，指定接收者的用户 ID 和消息
        PushMessage pushMessage = new PushMessage(userId, textMessage);

        try {
            // 使用 LineMessagingClient 的 pushMessage 方法发送消息
            BotApiResponse response = lineMessagingClient.pushMessage(pushMessage).get();
            return "Message sent successfully!";
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Error sending message: " + e.getMessage();
        }
    }

    @PostMapping("/send-sticker")
    public String sendSticker(@RequestBody String userId) {
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

   /* public static void validateChannel(String channelId, String channelSecret) {
        String validationUrl = "https://api.line.me/v2/bot/channel/check/";

        WebClient client = WebClient.builder().baseUrl(validationUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + channelSecret)
                .build();

        client.post()
                .body(BodyInserters.fromValue("{\"id\":\"" + channelId + "\"}"))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    System.out.println("Validation Response: " + response);
                })
                .block();
    }*/

    private String parseReceivedText(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // 获取 message 节点下的 text 节点的值
            JsonNode textNode = jsonNode.at("/events/0/message/text");
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing received text";
        }
    }

    private String parseReceivedText_replyToken(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // 获取 message 节点下的 text 节点的值
            JsonNode textNode = jsonNode.at("/events/0/replyToken");
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing received text";
        }
    }

    private String parseReceivedText_userId(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // 获取 message 节点下的 text 节点的值
            JsonNode textNode = jsonNode.at("/events/0/source/userId");
            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing received text";
        }
    }

    private void XreplyTextMessage(String replyToken, String text) {
        TextMessage textMessage = new TextMessage(text);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, textMessage);

        // 使用 LineMessagingClient 发送回复消息
        lineMessagingClient.replyMessage(replyMessage);
    }



    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
        System.out.println("Received message: " + event.getMessage().getText());

        // 回覆相同的文字
        String replyToken = event.getReplyToken();
        String replyText = event.getMessage().getText();
        replyTextMessage(replyToken, replyText);
    }

    private void replyTextMessage(String replyToken, String text) {
        TextMessage textMessage = new TextMessage(text);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, textMessage);
        lineMessagingClient.replyMessage(replyMessage);
    }



}
