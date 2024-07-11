package de.sebampuerom.handler;

import com.google.gson.JsonObject;
import de.sebampuerom.configuration.ApiConfig;
import de.sebampuerom.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private final WebClient webClient;
    private final String apiUrl;

    @Autowired
    private QueueService queueService;

    @Autowired
    public ChatWebSocketHandler(WebClient webClient, ApiConfig apiConfig) {
        this.webClient = webClient;
        this.apiUrl = apiConfig.getApiUrl();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String currUserID = queueService.getCurrentUserID();
        if(currUserID == null){
            return session.close(CloseStatus.NOT_ACCEPTABLE);
        }
        String reqUserID = getUserIdFromSession(session);
        if(!reqUserID.equals(currUserID)){
            return session.close(CloseStatus.NOT_ACCEPTABLE);
        }
        return session.receive()
                .flatMap(message -> {
                    String payload = message.getPayloadAsText();
                    JsonObject jsonBody = new JsonObject();
                    jsonBody.addProperty("input", payload);
                    jsonBody.addProperty("session_id", currUserID);
                    return webClient.post()
                            .uri(this.apiUrl)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(BodyInserters.fromValue(jsonBody.toString()))
                            .retrieve()
                            .bodyToFlux(String.class)
                            .flatMap(chunk -> session.send(Mono.just(session.textMessage(chunk))));
                })
                .then();
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // Extract user ID from session, e.g., from a query parameter
        return session.getHandshakeInfo().getUri().getQuery().split("=")[1];
    }
}
