package net.a5ho9999.fovtoggle;

import com.mojang.brigadier.arguments.BoolArgumentType;
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

        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess) -> commandDispatcher.register(ClientCommandManager.literal("nobonks").then(ClientCommandManager.argument("value", BoolArgumentType.bool()).executes(context -> {
            boolean value = BoolArgumentType.getBool(context, "value");
            ModConfig config = FOVToggleMod.getConfig();
            config.setNoBonk(value);
            FOVToggleMod.saveConfig();

            context.getSource().sendFeedback(Text.literal("Bonks set to " + !value));
            return 1;
        }))));

        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess) -> commandDispatcher.register(ClientCommandManager.literal("notedelay").then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 20)).executes(context -> {
            int value = IntegerArgumentType.getInteger(context, "value");
            ModConfig config = FOVToggleMod.getConfig();
            config.setNoteBlockDelay(value);
            FOVToggleMod.saveConfig();

            context.getSource().sendFeedback(Text.literal("Noteblock Tuning delay set to " + value));
            return 1;
        }))));

        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess) -> commandDispatcher.register(ClientCommandManager.literal("notequeue").then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 20)).executes(context -> {
            int value = IntegerArgumentType.getInteger(context, "value");
            ModConfig config = FOVToggleMod.getConfig();
            config.setConcurrentTuning(value);
            FOVToggleMod.saveConfig();

            context.getSource().sendFeedback(Text.literal("Noteblock Concurrent Tuning set to " + value));
            return 1;
        }))));
    }
}
