package group.aelysium.rustyconnector.modules.static_family;

import group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.declarative_yaml.annotations.*;
import group.aelysium.declarative_yaml.lib.Printer;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.Gson;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

@Namespace("rustyconnector")
@Config("/static_families/{id}.yml")
@Comment({
        "###########################################################################################################",
        "#|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "###########################################################################################################",
        "#                                                                                                         #",
        "#                    ______   ______     ______     ______   __     ______     ______                     #",
        "#                   /\\  == \\ /\\  __ \\   /\\  == \\   /\\__  _\\ /\\ \\   /\\  ___\\   /\\  ___\\                    #",
        "#                   \\ \\  _-/ \\ \\  __ \\  \\ \\  __<   \\/_/\\ \\/ \\ \\ \\  \\ \\  __\\   \\ \\___  \\                   #",
        "#                    \\ \\_\\    \\ \\_\\ \\_\\  \\ \\_\\ \\_\\    \\ \\_\\  \\ \\_\\  \\ \\_____\\  \\/\\_____\\                  #",
        "#                     \\/_/     \\/_/\\/_/   \\/_/ /_/     \\/_/   \\/_/   \\/_____/   \\/_____/                  #",
        "#                                                                                                         #",
        "#                                                                                                         #",
        "#                                            Welcome to Parties!                                          #",
        "#                                                                                                         #",
        "#                            -------------------------------------------------                            #",
        "#                                                                                                         #",
        "#                        | Allow your users to teleport around your network together                      #",
        "#                        | in parties!                                                                    #",
        "#                                                                                                         #",
        "#                            -------------------------------------------------                            #",
        "#                                                                                                         #",
        "###########################################################################################################",
        "#|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "###########################################################################################################"
})
public class PartyConfig {
    private int maxMember = 5;
    private boolean friendsOnly = false;
    private boolean localOnly = true;

    private boolean partyLeader_onlyLeaderCanInvite = true;
    private boolean partyLeader_onlyLeaderCanKick = true;
    private boolean partyLeader_onlyLeaderCanSwitch = true;
    private boolean partyLeader_disbandOnLeaderQuit = true;

    private Player.Connection.Power switchingServers_switchPower = Player.Connection.Power.MODERATE;

    public StaticFamily.Tinder tinder() throws IOException, ParseException {
        StaticFamily.Tinder tinder = new StaticFamily.Tinder(
                id,
                displayName.isEmpty() ? null : displayName,
                parentFamily.isEmpty() ? null : parentFamily,
                LoadBalancerConfig.New(loadBalancer).tinder(),
                this.database
        );
        tinder.storageProtocol(this.storageProtocol);
        tinder.unavailableProtocol(this.unavailableProtocol);
        tinder.residenceExpiration(LiquidTimestamp.from(this.residenceExpiration));

        Gson gson = new Gson();
        JsonObject metadataJson = gson.fromJson(this.metadata, JsonObject.class);
        metadataJson.entrySet().forEach(e->tinder.metadata(e.getKey(), Packet.Parameter.fromJSON(e.getValue()).getOriginalValue()));

        return tinder;
    }

    public static PartyConfig New(String familyID) throws IOException {
        Printer printer = new Printer()
                .pathReplacements(Map.of("id", familyID))
                .commentReplacements(Map.of("id", familyID));
        return DeclarativeYAML.From(PartyConfig.class, printer);
    }
}