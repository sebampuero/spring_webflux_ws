package de.sebampuerom.configuration;

import de.sebampuerom.handler.ChatWebSocketHandler;
import de.sebampuerom.handler.QueueWebsocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
class WebConfig {

    private final WebClient webClient;
    private final ApiConfig apiConfig;
    private final QueueWebsocketHandler queueWebsocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    public WebConfig(WebClient webClient, ApiConfig apiConfig, QueueWebsocketHandler queueWebsocketHandler, ChatWebSocketHandler chatWebSocketHandler) {
        this.webClient = webClient;
        this.apiConfig = apiConfig;
        this.queueWebsocketHandler = queueWebsocketHandler;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.chatWebSocketHandler.setWebClient(this.webClient);
        this.chatWebSocketHandler.setApiConfig(this.apiConfig);
    }

    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/chat", this.chatWebSocketHandler);
        map.put("/ws/queue",  this.queueWebsocketHandler);
        int order = -1; // before annotated controllers

        return new SimpleUrlHandlerMapping(map, order);
    }
}
