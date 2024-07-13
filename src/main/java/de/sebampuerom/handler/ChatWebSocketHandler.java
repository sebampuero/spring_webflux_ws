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
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Flux;
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

        if (shouldCloseSocket(session)) {
            return session.close(CloseStatus.NOT_ACCEPTABLE);
        }

        return sendIntroMessage(session)
            .and(handleWebSocketMessages(session, currUserID));
    }

    private Boolean shouldCloseSocket(WebSocketSession session) {
        String currUserID = queueService.getCurrentUserID();
        String reqUserID = getUserIdFromSession(session);
    
        if (currUserID == null) {
            log.debug("Closing socket because there was no user ID in query");
            return true;
        }
    
        if (!reqUserID.equals(currUserID)) {
            log.debug("Closing socket because input userid {} != curr user id {}", reqUserID, currUserID);
            return true;
        }
    
        return false;
    }

    private Mono<WebSocketMessage> buildIntroMessage(WebSocketSession session) {
        JsonObject firstMsgBody = new JsonObject();
        firstMsgBody.addProperty("type", "chunk");
        firstMsgBody.addProperty("content", """
                Welcome! This is a chat powered by RAG (Retrieval Augmented Generation)
                This does not represent me 100%! Data used is minimal because this runs 
                on a Raspberry Pi. 
                You can start sending messages to the RAG powered LLM now ->
                """);
        return Mono.just(session.textMessage(firstMsgBody.toString()));
    }

    private Flux<Object> handleWebSocketMessages(WebSocketSession session, String currUserID) {
        return session.receive()
            .flatMap(message -> processMessage(session, message, currUserID));
    }

    private Mono<Void> processMessage(WebSocketSession session, WebSocketMessage message, String currUserID) {
        String payload = message.getPayloadAsText();
        JsonObject jsonBody = createRequestBody(payload, currUserID);
        log.debug("Received payload with {} and session_id {}", payload, currUserID);
        return sendApiRequest(session, jsonBody);
    }

    private JsonObject createRequestBody(String payload, String sessionId) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("input", payload);
        jsonBody.addProperty("session_id", sessionId);
        return jsonBody;
    }

    private Mono<Void> sendApiRequest(WebSocketSession session, JsonObject jsonBody) {
        return webClient.post()
            .uri(this.apiConfig.getApiUrl())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(BodyInserters.fromValue(jsonBody.toString()))
            .retrieve()
            .bodyToFlux(String.class)
            .flatMap(chunk -> session.send(Mono.just(session.textMessage(chunk))))
            .then();
    }

    private Mono<Void> sendIntroMessage(WebSocketSession session) {
        return session.send(buildIntroMessage(session));
    }

    private String getUserIdFromSession(WebSocketSession session) {
        return session.getHandshakeInfo().getUri().getQuery().split("=")[1];
    }
}
