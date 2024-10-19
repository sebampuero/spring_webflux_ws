package de.sebampuerom.handler;

import de.sebampuerom.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Component
@Slf4j
public class QueueWebsocketHandler implements WebSocketHandler {

    @Autowired
    private QueueService queueService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        log.debug("Initiated queue connection with user id {}", userId);

        return Mono.just(queueService.tryConnect(userId))
                .flatMap(connected -> {
                    if (connected) {
                        return handleConnectedUser(session, userId);
                    } else {
                        return handleQueuedUser(session, userId);
                    }
                });
    }

    private Mono<Void> handleConnectedUser(WebSocketSession session, String userId) {
        log.debug("User has connected to chat session {}", userId);
        return session.send(Mono.just(session.textMessage("Connected")));
    }

    private Mono<Void> handleQueuedUser(WebSocketSession session, String userId) {
        log.debug("User has been queued {}", userId);
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> Mono.defer(() -> {
                    int position = queueService.getQueuePosition(userId);
                    if (position == 0) {
                        return Mono.just(true);
                    } else {
                        return session.send(Mono.just(session.textMessage("Queue position: " + position)))
                                .thenReturn(false);
                    }
                }))
                .takeUntil(connected -> connected)
                .then(handleConnectedUser(session, userId))
                .doFinally(signalType ->
                        {
                            log.debug("Promoting user to main websocket chat {}",  userId);
                            queueService.removeFromQueue();
                        }
                );
    }

    private String getUserIdFromSession(WebSocketSession session) {
        return session.getHandshakeInfo().getUri().getQuery().split("=")[1];
    }
}