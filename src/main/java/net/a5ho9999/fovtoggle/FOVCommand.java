package net.a5ho9999.fovtoggle;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

public class FOVCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess) -> commandDispatcher.register(ClientCommandManager.literal("fovtoggle").then(ClientCommandManager.argument("value", IntegerArgumentType.integer(30, 110)).executes(context -> {
            int value = IntegerArgumentType.getInteger(context, "value");
            ModConfig config = FOVToggleMod.getConfig();
            config.setFovValue(value);
            FOVToggleMod.saveConfig();

            context.getSource().sendFeedback(Text.literal("FOV toggle value set to " + value));
            return 1;
        }))));
    }
}
