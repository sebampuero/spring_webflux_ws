package de.sebampuerom.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class QueueService {
    private final Queue<String> userQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<String> currentUser = new AtomicReference<>();

    public boolean tryConnect(String userId) {
        log.debug("Trying to set current user {}", userId);
        if (currentUser.get() == null) {
            log.debug("There is currently no user set");
            return currentUser.compareAndSet(null, userId);
        }
        log.debug("{} was put in the queue", userId);
        log.debug("There is one current user set: {}", currentUser.get());
        log.debug("Queue: {}", userQueue.toString());
        userQueue.offer(userId);
        return false;
    }

    public void disconnect(String userId) {
        log.info("Disconnecting user {}", userId);
        log.debug("Next in the queue is: {}", userQueue.peek());
        if (currentUser.compareAndSet(userId, null)) {
            log.info("{} was disconnected", userId);
            setNextUserInQueueAsCurrent();
        } else {
            log.warn("For some reason, {} was not the current user", userId);
            log.warn("Trying to set next user in queue for connection: {}", userQueue.peek());
            setNextUserInQueueAsCurrent();
        }
    }

    private void setNextUserInQueueAsCurrent() {
        String nextUser = userQueue.poll();
        if (nextUser != null) {
            log.debug("Setting {} as current user", nextUser);
            currentUser.set(nextUser);
        }
    }

    public String removeFromQueue() {
        return userQueue.poll();
    }

    public String getCurrentUserID() {
        return currentUser.get();
    }

    public int getQueuePosition(String userId) {
        if (userId.equals(currentUser.get())) {
            return 0;
        }
        return new ArrayList<>(userQueue).indexOf(userId) + 1;
    }

    public boolean isUserConnected(String userId) {
        return userId.equals(currentUser.get());
    }
}
