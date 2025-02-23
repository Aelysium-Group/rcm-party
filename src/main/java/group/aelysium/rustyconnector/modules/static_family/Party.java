package group.aelysium.rustyconnector.modules.static_family;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Party {
    protected final UUID uuid = UUID.randomUUID();
    protected final AtomicBoolean closed = new AtomicBoolean(false);
    protected final PartyRegistry registry;
    protected final Set<UUID> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected final AtomicReference<UUID> leader;
    protected final AtomicReference<String> server;
    protected final Map<UUID, Invitation> invitations = new ConcurrentHashMap<>();

    protected Party(
        @NotNull PartyRegistry registry,
        @NotNull UUID leader,
        @NotNull String server
    ) {
        this.registry = registry;
        this.leader = new AtomicReference<>(leader);
        this.players.add(leader);
        this.server = new AtomicReference<>(server);
    }

    public @NotNull UUID uuid() {
        return this.uuid;
    }

    public @NotNull UUID leader() {
        return this.leader.get();
    }
    public void leader(@NotNull UUID player) {
        this.players.add(player);
        this.leader.set(player);
    }
    public @NotNull Optional<Player> resolvedLeader() {
        return RC.P.Player(this.leader.get());
    }

    public @NotNull Set<UUID> players() {
        return Collections.unmodifiableSet(this.players);
    }

    public int size() {
        return this.players.size();
    }

    public boolean contains(@NotNull UUID player) {
        return this.players.contains(player);
    }

    public boolean full() {
        return this.players.size() >= this.registry.config().maxMember;
    }

    public void server(@NotNull String id) {
        this.server.set(id);
    }
    public @NotNull String server() {
        return this.server.get();
    }
    public @NotNull Optional<Server> resolvedServer() {
        return RC.P.Server(this.server.get());
    }

    public @NotNull List<Invitation> invitations() {
        return this.invitations.values().stream().toList();
    }
    public @NotNull Optional<Invitation> fetchInvitation(@NotNull UUID player) {
        return Optional.ofNullable(this.invitations.get(player));
    }
    public void closeInvitation(@NotNull UUID player) {
        this.invitations.remove(player);
    }
    public @NotNull Invitation invite(@NotNull UUID sender, @NotNull UUID target) throws IllegalAccessException {
        if(this.registry.config().partyLeader_onlyLeaderCanInvite)
            if(!this.leader.get().equals(sender))
                throw new IllegalAccessException("Only the party leader is allowed to send party invites.");

        Invitation invitation = new Invitation(this, sender, target);
        this.registry.invitations.put(invitation.target(), invitation);
        return invitation;
    }

    public @NotNull JoinAttempt join(@NotNull UUID player) {
        if(this.closed.get()) return new JoinAttempt(false, "That party no-longer exists.");
        if(this.players.size() >= this.registry.config().maxMember) return new JoinAttempt(false, "That party is full.");
        if(this.players.contains(player))  return new JoinAttempt(false, "You're already in the party.");
        if(this.registry.config().friendsOnly) {
            try {
                return false; // Need to properly implement the friends module.
            } catch (TimeoutException e) {
                RC.Error(Error.from(e).detail("Reason", "The PartyRegistry requires that players are friends with the party leader before they can join. However the FriendRegistry module didn't respond in time for us to validate. We did not let the player join the Party."));
                return new JoinAttempt(false, "Only friends of the current party leader can join the party.");
            }
        }
        this.players.add(player);
        this.registry.players.put(player, this);
        return new JoinAttempt(true, "Success!");
    }
    public void leave(@NotNull UUID uuid) {
        if(!this.leader.get().equals(uuid)) {
            this.players.remove(uuid);
            this.registry.players.remove(uuid);
            return;
        }

        if (this.registry.config().partyLeader_disbandOnLeaderQuit) {
            this.registry.disbandParty(this);
            return;
        }

        UUID newLeader = this.players.stream().findAny().orElse(null);
        if(newLeader == null) {
            this.registry.disbandParty(this);
            return;
        }

        this.leader.set(newLeader);
    }
    public void kick(@NotNull UUID uuid, @NotNull String reason) {
        this.leave(uuid);

        Player player = RC.P.Player(uuid).orElse(null);
        if(player == null) return;

        player.message(Component.text(reason));
    }

    public void disband() {
        this.closed.set(true);
        this.leader.set(null);
        this.players.clear();
    }

    public boolean closed() {
        return this.closed.get();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Party party = (Party) o;
        return Objects.equals(uuid, party.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    public static class Invitation {
        private final UUID uuid = UUID.randomUUID();
        private final Party party;
        private final UUID sender;
        private final UUID target;
        private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
        private final Instant issuedAt = Instant.now();

        protected Invitation(
                @NotNull Party party,
                @NotNull UUID sender,
                @NotNull UUID target
        ) {
            this.party = party;
            this.sender = sender;
            this.target = target;
        }

        public Party party() {
            return this.party;
        }
        public UUID sender() {
            return this.sender;
        }
        public UUID target() {
            return this.target;
        }
        public Status status() {
            return this.status.get();
        }
        public boolean expired() {
            return this.issuedAt.plusSeconds(60).isBefore(Instant.now());
        }

        public @NotNull JoinAttempt accept() {
            if(!this.status.get().equals(Status.PENDING)) return new JoinAttempt(false, "Your invitation to that party is expired.");
            if(this.party.closed()) return new JoinAttempt(false, "That party no-longer exists.");
            if(this.party.full()) return new JoinAttempt(false, "That is full.");

            JoinAttempt attempt = this.party.join(this.target);

            if(attempt.successful())
                this.status.set(Status.ACCEPTED);

            return attempt;
        }
        public void ignore() {
            if(!this.status.get().equals(Status.PENDING)) return;
            this.status.set(Status.IGNORED);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Invitation that = (Invitation) o;
            return Objects.equals(uuid, that.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(uuid);
        }

        public enum Status {
            PENDING,
            ACCEPTED,
            IGNORED
        }
    }

    public record JoinAttempt(
            boolean successful,
            String message
    ) {}
}
