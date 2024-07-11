package de.sebampuerom.handler;

import de.sebampuerom.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class QueueWebsocketHandler implements WebSocketHandler {

    @Autowired
    private QueueService queueService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = getUserIdFromSession(session);

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
        return session.send(Flux.just(session.textMessage("Connected")))
                .then(session.receive()
                        .doFinally(signalType -> queueService.disconnect(userId))
                        .then());
    }

    private Mono<Void> handleQueuedUser(WebSocketSession session, String userId) {
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> {
                    int position = queueService.getQueuePosition(userId);
                    if (position == 0) {
                        return session.send(Mono.just(session.textMessage("Your turn")))
                                .then(Mono.defer(() -> handleConnectedUser(session, userId)));
                    } else {
                        return session.send(Mono.just(session.textMessage("Queue position: " + position)));
                    }
                })
                .then();
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // Extract user ID from session, e.g., from a query parameter
        return session.getHandshakeInfo().getUri().getQuery().split("=")[1];
    }
}
