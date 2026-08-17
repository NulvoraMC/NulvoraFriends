package com.nulvora.friends.velocity.friends;

import com.nulvora.friends.velocity.storage.Database;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FriendService {

    private final Database db;

    public FriendService(Database db) {
        this.db = db;
    }

    public CompletableFuture<Boolean> canAddMore(UUID uuid, int maxFriends) {
        return db.getFriendCount(uuid).thenApply(count -> count < maxFriends);
    }

    public CompletableFuture<Boolean> sendRequest(UUID sender, UUID receiver) {
        return db.areFriends(sender, receiver).thenCompose(alreadyFriends -> {
            if (alreadyFriends) {
                return CompletableFuture.completedFuture(false);
            }
            return db.hasPendingRequest(sender, receiver).thenCompose(hasPending -> {
                if (hasPending) {
                    return CompletableFuture.completedFuture(false);
                }
                return db.sendFriendRequest(sender, receiver).thenApply(v -> true);
            });
        });
    }

    public CompletableFuture<Boolean> acceptRequest(UUID sender, UUID receiver) {
        return db.acceptFriendRequest(sender, receiver);
    }

    public CompletableFuture<Boolean> denyRequest(UUID sender, UUID receiver) {
        return db.denyFriendRequest(sender, receiver);
    }

    public CompletableFuture<Boolean> removeFriend(UUID a, UUID b) {
        return db.removeFriendship(a, b);
    }

    public CompletableFuture<List<UUID>> getFriendIds(UUID uuid) {
        return db.getFriendIds(uuid);
    }

    public CompletableFuture<List<UUID>> getPendingRequests(UUID uuid) {
        return db.getPendingRequests(uuid);
    }
}
