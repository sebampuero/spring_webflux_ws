package de.sebampuerom.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class QueueService {
    private final Queue<String> userQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<String> currentUser = new AtomicReference<>();

    public boolean tryConnect(String userId) {
        if (currentUser.get() == null) {
            return currentUser.compareAndSet(null, userId);
        }
        userQueue.offer(userId);
        return false;
    }

    public void disconnect(String userId) {
        if (currentUser.compareAndSet(userId, null)) {
            String nextUser = userQueue.poll();
            if (nextUser != null) {
                currentUser.set(nextUser);
            }
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
