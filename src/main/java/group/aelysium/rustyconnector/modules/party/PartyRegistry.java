package group.aelysium.rustyconnector.modules.party;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.modules.party.events.OnServerPreJoin;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PartyRegistry implements Module {
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    protected PartyConfig config;
    protected final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    protected final Map<UUID, Party> players = new ConcurrentHashMap<>();
    protected final Map<UUID, Party.Invitation> invitations = new ConcurrentHashMap<>();

    public PartyRegistry(
            PartyConfig config
    ) {
        this.config = config;
        if (config.friendsOnly)
            if (RC.Kernel().fetchModule("FriendRegistry") == null)
                throw new IllegalStateException("If you enable 'friends-only' you must also install the 'rcm-friend' module.");

        this.cleaner.schedule(this::clean, 2, TimeUnit.MINUTES);
    }

    public @NotNull PartyConfig config() {
        return this.config;
    }

    public @NotNull Party startParty(@NotNull Player leader, @NotNull Server server) {
        Party party = new Party(this, leader.uuid(), server.id());

        this.parties.put(party.uuid(), party);
        this.players.put(leader.uuid(), party);

        return party;
    }

    public boolean contains(@NotNull UUID player) {
        return this.players.containsKey(player);
    }
    public @NotNull Optional<Party> fetch(@NotNull UUID player) {
        return Optional.ofNullable(this.players.get(player));
    }

    public void disbandParty(@NotNull Party party) {
        this.parties.remove(party.uuid());
        party.players().forEach(this.players::remove);
        party.invitations().forEach(i -> this.invitations.remove(i.target()));
        party.disband();
    }

    private void clean() {
        try {
            Set<UUID> expired = new HashSet<>();
            this.invitations.forEach((u, i) -> {
                if(i.expired()) expired.add(u);
            });
            expired.forEach(this.invitations::remove);
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To clear out expired party invitations."));
        }
        try {
            Set<UUID> closed = new HashSet<>();
            this.parties.forEach((k, v) -> {
                if(v.closed()) closed.add(k);
            });
            closed.forEach(this.parties::remove);
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To clear out closed parties."));
        }

        this.cleaner.schedule(this::clean, 20, TimeUnit.SECONDS);
    }

    @Override
    public @Nullable Component details() {
        return null;
    }

    @Override
    public void close() throws Exception {
        this.parties.forEach((k,v)->v.disband());
        this.invitations.clear();
        this.players.clear();
        this.parties.clear();
        this.cleaner.close();
    }

    public static class Builder extends ExternalModuleBuilder<PartyRegistry> {
        public void bind(@NotNull ProxyKernel kernel, @NotNull PartyRegistry instance) {
            kernel.fetchModule("EventManager").onStart(e->{
                ((EventManager) e).listen(new OnServerPreJoin());
            });
        }

        @NotNull
        @Override
        public PartyRegistry onStart(@NotNull Path dataDirectory) throws Exception {
            return new PartyRegistry(PartyConfig.New());
        }
    }

    public enum OverflowHandler {
        KICK,
        ABANDON,
        BLOCK_SWITCH
    }
}