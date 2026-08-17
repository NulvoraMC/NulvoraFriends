package com.nulvora.friends.velocity.storage;

import com.nulvora.friends.velocity.config.NulvoraConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Database {

    private final HikariDataSource dataSource;

    public Database(NulvoraConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mariadb://" + config.mysql().host() + ":" + config.mysql().port() + "/" + config.mysql().database());
        hikari.setUsername(config.mysql().user());
        hikari.setPassword(config.mysql().password());
        hikari.setMaximumPoolSize(config.mysql().poolSize());
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setConnectionTimeout(10000);
        hikari.setPoolName("nulfriends-hikari");
        this.dataSource = new HikariDataSource(hikari);
    }

    public void initialize() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    discord_id BIGINT UNSIGNED NULL UNIQUE,
                    linked_at TIMESTAMP NULL,
                    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friendships (
                    player_a VARCHAR(36) NOT NULL,
                    player_b VARCHAR(36) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_a, player_b),
                    INDEX idx_friendships_b (player_b)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friend_requests (
                    sender VARCHAR(36) NOT NULL,
                    receiver VARCHAR(36) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (sender, receiver)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS link_codes (
                    code VARCHAR(8) PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    expires_at TIMESTAMP NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public CompletableFuture<Void> upsertPlayer(UUID uuid, String name) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO players (uuid, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = ?")) {
                String id = uuid.toString();
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setString(3, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> updateLastSeen(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE players SET last_seen = CURRENT_TIMESTAMP WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<Long>> getDiscordId(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT discord_id FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long id = rs.getLong("discord_id");
                    return rs.wasNull() ? Optional.empty() : Optional.of(id);
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<UUID>> getUuidByDiscordId(long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid FROM players WHERE discord_id = ?")) {
                ps.setLong(1, discordId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(UUID.fromString(rs.getString("uuid")));
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> linkAccounts(UUID uuid, long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE players SET discord_id = ?, linked_at = CURRENT_TIMESTAMP WHERE uuid = ?")) {
                ps.setLong(1, discordId);
                ps.setString(2, uuid.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> unlinkAccounts(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE players SET discord_id = NULL, linked_at = NULL WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> unlinkByDiscordId(long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE players SET discord_id = NULL, linked_at = NULL WHERE discord_id = ?")) {
                ps.setLong(1, discordId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> areFriends(UUID a, UUID b) {
        return CompletableFuture.supplyAsync(() -> {
            String first = a.toString().compareTo(b.toString()) < 0 ? a.toString() : b.toString();
            String second = first.equals(a.toString()) ? b.toString() : a.toString();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM friendships WHERE player_a = ? AND player_b = ?")) {
                ps.setString(1, first);
                ps.setString(2, second);
                return ps.executeQuery().next();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> addFriendship(UUID a, UUID b) {
        return CompletableFuture.supplyAsync(() -> {
            String first = a.toString().compareTo(b.toString()) < 0 ? a.toString() : b.toString();
            String second = first.equals(a.toString()) ? b.toString() : a.toString();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT IGNORE INTO friendships (player_a, player_b) VALUES (?, ?)")) {
                ps.setString(1, first);
                ps.setString(2, second);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> removeFriendship(UUID a, UUID b) {
        return CompletableFuture.supplyAsync(() -> {
            String first = a.toString().compareTo(b.toString()) < 0 ? a.toString() : b.toString();
            String second = first.equals(a.toString()) ? b.toString() : a.toString();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM friendships WHERE player_a = ? AND player_b = ?")) {
                ps.setString(1, first);
                ps.setString(2, second);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<UUID>> getFriendIds(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String id = uuid.toString();
            List<UUID> friends = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT player_a AS friend FROM friendships WHERE player_b = ? " +
                     "UNION ALL " +
                     "SELECT player_b AS friend FROM friendships WHERE player_a = ?")) {
                ps.setString(1, id);
                ps.setString(2, id);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    friends.add(UUID.fromString(rs.getString("friend")));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return friends;
        });
    }

    public CompletableFuture<Integer> getFriendCount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String id = uuid.toString();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM friendships WHERE player_a = ? OR player_b = ?")) {
                ps.setString(1, id);
                ps.setString(2, id);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> hasPendingRequest(UUID sender, UUID receiver) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM friend_requests WHERE sender = ? AND receiver = ?")) {
                ps.setString(1, sender.toString());
                ps.setString(2, receiver.toString());
                return ps.executeQuery().next();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> sendFriendRequest(UUID sender, UUID receiver) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO friend_requests (sender, receiver) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP")) {
                ps.setString(1, sender.toString());
                ps.setString(2, receiver.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> acceptFriendRequest(UUID sender, UUID receiver) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement del = conn.prepareStatement(
                     "DELETE FROM friend_requests WHERE sender = ? AND receiver = ?")) {
                    del.setString(1, sender.toString());
                    del.setString(2, receiver.toString());
                    if (del.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                String first = sender.toString().compareTo(receiver.toString()) < 0 ? sender.toString() : receiver.toString();
                String second = first.equals(sender.toString()) ? receiver.toString() : sender.toString();
                try (PreparedStatement ins = conn.prepareStatement(
                     "INSERT IGNORE INTO friendships (player_a, player_b) VALUES (?, ?)")) {
                    ins.setString(1, first);
                    ins.setString(2, second);
                    ins.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> denyFriendRequest(UUID sender, UUID receiver) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM friend_requests WHERE sender = ? AND receiver = ?")) {
                ps.setString(1, sender.toString());
                ps.setString(2, receiver.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<UUID>> getPendingRequests(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> requests = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT sender FROM friend_requests WHERE receiver = ?")) {
                ps.setString(1, player.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    requests.add(UUID.fromString(rs.getString("sender")));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return requests;
        });
    }

    public CompletableFuture<Void> storeLinkCode(String code, UUID uuid, long expiryMillis) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO link_codes (code, uuid, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? MICROSECOND)) " +
                     "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), expires_at = VALUES(expires_at)")) {
                ps.setString(1, code);
                ps.setString(2, uuid.toString());
                ps.setLong(3, expiryMillis * 1000);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<UUID>> consumeLinkCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                UUID uuid;
                try (PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid FROM link_codes WHERE code = ? AND expires_at > NOW()")) {
                    ps.setString(1, code);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        conn.rollback();
                        return Optional.empty();
                    }
                    uuid = UUID.fromString(rs.getString("uuid"));
                }
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM link_codes WHERE code = ?")) {
                    del.setString(1, code);
                    del.executeUpdate();
                }
                conn.commit();
                return Optional.of(uuid);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> getName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getString("name") : null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> isLinked(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT discord_id FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next() && !rs.wasNull();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
