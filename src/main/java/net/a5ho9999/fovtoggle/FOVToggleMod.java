package net.a5ho9999.fovtoggle;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FOVToggleMod implements ModInitializer {
	public static final String MOD_ID = "fovtoggle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyBinding keyBinding;
	private static ModConfig config;
	private static int originalFOV = 90;
	private static boolean fovSwitched = false;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing FOV Switcher Mod");

		config = ModConfig.load();

		keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.fovtoggle", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_V, "category.fovtoggle"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (keyBinding.wasPressed()) {
				GameOptions options = client.options;

				if (client.player != null) {
					if (!fovSwitched) {
						originalFOV = options.getFov().getValue();
						options.getFov().setValue(config.getFovValue());
						client.player.sendMessage(Text.literal("FOV switched to " + config.getFovValue()), true);
					} else {
						options.getFov().setValue(originalFOV);
						client.player.sendMessage(Text.literal("FOV restored to " + originalFOV), true);
					}
				}

				fovSwitched = !fovSwitched;
			}
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(FOVToggleMod::resetFOVIfNeeded);

		FOVCommand.register();
	}

	public static ModConfig getConfig() {
		return config;
	}

	public static void saveConfig() {
		config.save();
	}

	private static void resetFOVIfNeeded(MinecraftClient client) {
		if (FOVToggleMod.fovSwitched && FOVToggleMod.originalFOV >= 0) {
			LOGGER.info("Resetting FOV to original value: {}", FOVToggleMod.originalFOV);
			if (client != null && client.options != null) {
				client.options.getFov().setValue(FOVToggleMod.originalFOV);
				FOVToggleMod.fovSwitched = false;
			}
		}
	}
}