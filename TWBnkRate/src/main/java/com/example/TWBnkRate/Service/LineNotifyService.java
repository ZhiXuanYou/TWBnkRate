package com.example.TWBnkRate.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class LineNotifyService {

    @Value("${line.notify.token}") // Read Access Token from configuration
    private String lineNotifyToken;

    @Autowired
    private RestTemplate restTemplate;

    public void sendLineNotifyMessage(String message) {
        String url = "https://notify-api.line.me/api/notify";

        //RestTemplate restTemplate = new RestTemplate();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + lineNotifyToken);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("message", message);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, requestHeaders);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        // Handle the response to check if the message was sent successfully
    }
}

