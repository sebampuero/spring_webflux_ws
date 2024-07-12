package de.sebampuerom.handler;

import com.google.gson.JsonObject;
import de.sebampuerom.configuration.ApiConfig;
import de.sebampuerom.service.QueueService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    @Setter
    private WebClient webClient;
    @Setter
    private ApiConfig apiConfig;

    @Autowired
    private QueueService queueService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String currUserID = queueService.getCurrentUserID();
        if(currUserID == null){
            log.debug("Closing socket because there was not user ID in query");
            return session.close(CloseStatus.NOT_ACCEPTABLE);
        }
        String reqUserID = getUserIdFromSession(session);
        if(!reqUserID.equals(currUserID)){
            log.debug("Closing socket because input userid {} != curr user id {}", reqUserID, currUserID);
            return session.close(CloseStatus.NOT_ACCEPTABLE);
        }
        return session.receive()
                .flatMap(message -> {
                    String payload = message.getPayloadAsText();
                    JsonObject jsonBody = new JsonObject();
                    jsonBody.addProperty("input", payload);
                    jsonBody.addProperty("session_id", currUserID);
                    log.debug("Received payload with {} and session_id {}", payload, reqUserID);
                    return webClient.post()
                            .uri(this.apiConfig.getApiUrl())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(BodyInserters.fromValue(jsonBody.toString()))
                            .retrieve()
                            .bodyToFlux(String.class)
                            .flatMap(chunk -> session.send(Mono.just(session.textMessage(chunk))));
                })
                .then();
    }

    private String getUserIdFromSession(WebSocketSession session) {
        return session.getHandshakeInfo().getUri().getQuery().split("=")[1];
    }
}
