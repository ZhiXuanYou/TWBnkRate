package com.example.TWBnkRate;

import com.example.TWBnkRate.Controller.LineBotController;
import com.example.TWBnkRate.Service.RedisTemplateConfig;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@SpringBootApplication
@LineMessageHandler
@EnableScheduling // <-- 一定要加上，才會執行所有設定好的 Schedule works
@MapperScan("com.example.TWBnkRate.mapper")
@Import(RedisTemplateConfig.class) // 引入RedisConfig配置类

public class TwBnkRateApplication {
	@Autowired
	private LineMessagingClient lineMessagingClient;
	@Bean(name = "RestTemplate")
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(TwBnkRateApplication.class, args);
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

	/*@Bean
	public LineBotController lineBotController() {
		return new LineBotController();
	}*/
}
