package com.example.TWBnkRate.Service;

import com.linecorp.bot.client.LineMessagingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LineBotConfig {

    @Value("${line.bot.channelId}")
    private String channelId;

    @Value("${line.bot.channelSecret}")
    private String channelSecret;

    @Value("${line.bot.channelToken}")
    private String lineChannelToken;

    @Bean(name = "lineMessagingClient")
    public LineMessagingClient lineMessagingClient() {
        return LineMessagingClient.builder(lineChannelToken).build();
    }
}
