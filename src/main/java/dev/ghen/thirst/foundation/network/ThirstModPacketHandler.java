package dev.ghen.thirst.foundation.network;

import dev.ghen.thirst.foundation.network.message.DrinkByHandMessage;
import dev.ghen.thirst.foundation.network.message.PlayerThirstSyncMessage;
import dev.ghen.thirst.Thirst;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public class ThirstModPacketHandler
{
    private static final String PROTOCOL_VERSION = "0.1.2";
    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(Thirst.asResource("main"))
            .networkProtocolVersion(Integer.parseInt(PROTOCOL_VERSION.replace(".", "")))
            .simpleChannel();

    public static void init()
    {
        INSTANCE.messageBuilder(PlayerThirstSyncMessage.class, 0)
                .encoder(PlayerThirstSyncMessage::encode)
                .decoder(PlayerThirstSyncMessage::decode)
                .consumerMainThread(PlayerThirstSyncMessage::handle)
                .add();
        INSTANCE.messageBuilder(DrinkByHandMessage.class, 1)
                .encoder(DrinkByHandMessage::encode)
                .decoder(DrinkByHandMessage::decode)
                .consumerMainThread(DrinkByHandMessage::handle)
                .add();
    }
}
