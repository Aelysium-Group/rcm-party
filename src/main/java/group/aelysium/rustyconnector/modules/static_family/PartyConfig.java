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

@Namespace("rustyconnector-modules")
@Config("/rcm-party/config.yml")
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
    public int maxMember = 5;
    public boolean friendsOnly = false;
    public boolean localOnly = true;

    public boolean partyLeader_onlyLeaderCanInvite = true;
    public boolean partyLeader_onlyLeaderCanKick = true;
    public boolean partyLeader_onlyLeaderCanSwitch = true;
    public boolean partyLeader_disbandOnLeaderQuit = true;

    public Player.Connection.Power switchingServers_switchPower = Player.Connection.Power.MODERATE;
    public PartyRegistry.OverflowHandler switchingServers_overflowHandler = PartyRegistry.OverflowHandler.BLOCK_SWITCH;

    public static PartyConfig New() {
        return DeclarativeYAML.From(PartyConfig.class, new Printer());
    }
}