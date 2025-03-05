package group.aelysium.rustyconnector.modules.party.events;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.modules.party.Party;
import group.aelysium.rustyconnector.modules.party.PartyRegistry;
import group.aelysium.rustyconnector.proxy.events.NetworkLeaveEvent;

import java.util.*;

public class OnDisconnect {
    @EventListener
    public void handle(NetworkLeaveEvent event) {
        try {
            UUID player = event.player.uuid();
            PartyRegistry parties = (PartyRegistry) Objects.requireNonNull(RC.Kernel().fetchModule("PartyRegistry")).orElseThrow();

            Party party = parties.fetch(player).orElse(null);
            if(party == null) return;

            party.leave(player);
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To check if the player is a member of a party. This exception won't affect the player's ability to connect to this or any other server."));
        }
    }
}
