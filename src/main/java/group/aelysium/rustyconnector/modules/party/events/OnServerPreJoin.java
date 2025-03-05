package group.aelysium.rustyconnector.modules.party.events;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.common.events.EventPriority;
import group.aelysium.rustyconnector.modules.party.Party;
import group.aelysium.rustyconnector.modules.party.PartyConfig;
import group.aelysium.rustyconnector.modules.party.PartyRegistry;
import group.aelysium.rustyconnector.proxy.events.ServerPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class OnServerPreJoin {
    @EventListener(order = EventPriority.HIGH)
    public void handle(ServerPreJoinEvent event) {
        try {
            Server targetServer = event.server;
            PartyRegistry parties = (PartyRegistry) Objects.requireNonNull(RC.Kernel().fetchModule("PartyRegistry")).orElseThrow();

            Party party = parties.fetch(event.player.uuid()).orElse(null);
            if(party == null) return;

            PartyConfig config = parties.config();

            if(config.partyLeader_onlyLeaderCanSwitch && !party.leader().equals(event.player.uuid())) {
                event.canceled(true, "Only the party leader can switch servers.");
                return;
            }

            long availableSlots = 100;
            if(config.switchingServers_switchPower.equals(Player.Connection.Power.MINIMAL))
                availableSlots = (targetServer.softPlayerCap() - targetServer.players());
            if(config.switchingServers_switchPower.equals(Player.Connection.Power.MODERATE))
                availableSlots = (targetServer.hardPlayerCap() - targetServer.players());
            // AGGRESSIVE doesn't matter because it would just set "willFail" to "false".

            if(availableSlots < 1) {
                event.canceled(true, "The target server doesn't have enough space for your party. Try again later.");
                return;
            }

            Set<UUID> leftBehind = new HashSet<>();
            List<UUID> preparedForConnection = new ArrayList<>();
            if(availableSlots < party.size()) {
                if(config.switchingServers_overflowHandler.equals(PartyRegistry.OverflowHandler.BLOCK_SWITCH)) {
                    event.canceled(true, "The target server doesn't have enough space for your party. Try again later.");
                    return;
                }

                preparedForConnection.addAll(party.players());
                preparedForConnection.remove(party.leader());
                int i = 0;
                while (i < availableSlots) {
                    UUID item = preparedForConnection.get(i);
                    if(item == null) break;

                    leftBehind.add(preparedForConnection.get(i));

                    i++;
                }
                preparedForConnection.removeAll(leftBehind);

                if(config.switchingServers_overflowHandler.equals(PartyRegistry.OverflowHandler.KICK)) {
                    leftBehind.forEach(u -> party.kick(u, "The party moved to a server that didn't have enough room for you."));
                }
                if(config.switchingServers_overflowHandler.equals(PartyRegistry.OverflowHandler.ABANDON)) {
                    leftBehind.forEach(u -> {
                        try {
                            RC.P.Player(u).orElseThrow().message(Component.text("The party moved to a server that didn't have enough room for you."));
                        } catch (Exception ignore) {}
                    });
                }
            }

            preparedForConnection.forEach(u -> {
                try {
                    Player.Connection.Request request = RC.P.Adapter().connectServer(event.server, RC.P.Player(u).orElseThrow());
                    Player.Connection.Result result = request.result().get(7, TimeUnit.SECONDS);
                    if(result.connected()) return;

                    event.player.message(result.message());
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To help a party member follow their party leader.")
                            .detail("Server", event.server.id())
                            .detail("Party Players", party.size())
                            .detail("Party Leader", party.leader())
                    );
                }
            });

            party.server(event.server.id());
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To check if the player is a member of a party. This exception won't affect the player's ability to connect to this or any other server."));
        }
    }
}
