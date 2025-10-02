package net.a5ho9999.fovtoggle;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.UUID;

public class FOVToggleMod implements ModInitializer {
	public static final String MOD_ID = "fovtoggle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyBinding fovToggleKey;
	private static KeyBinding sneakToggleKey;
	private static KeyBinding zoomiesToggleKey;
	private static KeyBinding speedResetToggleKey;
	private static ModConfig config;
	private static int originalFOV = 90;
	private static boolean fovSwitched = false;
	private static boolean originalSneak = false;
	private static boolean sneakSwitched = false;
	private static boolean zoomiesEnabled = true;

	public static final Identifier BOAT_LANDING_SOUND_ID = Identifier.of(MOD_ID, "boat_landing");
	public static final SoundEvent BOAT_LANDING_SOUND_EVENT = SoundEvent.of(BOAT_LANDING_SOUND_ID);

	public static final Identifier BOAT_COLLISION_SOUND_ID = Identifier.of(MOD_ID, "boat_collision");
	public static final SoundEvent BOAT_COLLISION_SOUND_EVENT = SoundEvent.of(BOAT_COLLISION_SOUND_ID);

	private static final HashMap<UUID, Vec3d> lastBoatPositions = new HashMap<>();
	private static final HashMap<UUID, Boolean> wasOnGround = new HashMap<>();

	private static final double COLLISION_DISTANCE_THRESHOLD  = 0.1;

	private static final HashMap<UUID, Integer> landingCooldowns = new HashMap<>();
	private static final HashMap<UUID, Integer> collisionCooldowns = new HashMap<>();

	private static final int LANDING_COOLDOWN_TICKS = 10;
	private static final int COLLISION_COOLDOWN_TICKS = 10;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing FOV Switcher Mod");

		config = ModConfig.load();

		fovToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.fovtoggle", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_V, "category.fovtoggle"));
		sneakToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sneaktoggle", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_LEFT_ALT, "category.fovtoggle"));
		zoomiesToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.zoomiestoggle", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_N, "category.fovtoggle"));
		speedResetToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.resetspeed", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_L, "category.fovtoggle"));

		Registry.register(Registries.SOUND_EVENT, BOAT_LANDING_SOUND_ID, BOAT_LANDING_SOUND_EVENT);
		Registry.register(Registries.SOUND_EVENT, BOAT_COLLISION_SOUND_ID, BOAT_COLLISION_SOUND_EVENT);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (fovToggleKey.wasPressed()) {
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

			if (sneakToggleKey.wasPressed()) {
				GameOptions options = client.options;

				if (client.player != null) {
					if (!sneakSwitched) {
						originalSneak = options.getSneakToggled().getValue();
						options.getSneakToggled().setValue(!originalSneak);
						client.player.sendMessage(Text.literal("Sneaking Mode switched to " + (!originalSneak ? "Toggle" : "Hold")), true);
					} else {
						options.getSneakToggled().setValue(originalSneak);
						client.player.sendMessage(Text.literal("Sneaking Mode restored to " + (originalSneak ? "Toggle" : "Hold")), true);
					}
				}

				sneakSwitched = !sneakSwitched;
			}

			if (zoomiesToggleKey.wasPressed()) {
				if (client.player != null) {
					if (!zoomiesEnabled) {
						client.player.sendMessage(Text.literal("Nroom Mode switched to super sick"), true);
					} else {
						client.player.sendMessage(Text.literal("Nroom Mode switched to super boring"), true);
					}
				}

				zoomiesEnabled = !zoomiesEnabled;
			}

			if (speedResetToggleKey.wasPressed()) {
				if (client.player != null) {
					client.player.sendMessage(Text.literal("ResET mAx BoAT sPeEd"), true);
				}

				BoatSpeedometerRenderer.resetMaxSpeed();
			}

			if (!config.getNoBonk()) {
				if (client.world != null && client.player != null) {
					var world = client.world;

					int chunkX = client.player.getChunkPos().x;
					int chunkZ = client.player.getChunkPos().z;

					Box chunkBox = new Box(chunkX * 16, client.world.getBottomY(), chunkZ * 16, (chunkX + 1) * 16, client.world.getTopYInclusive(), (chunkZ + 1) * 16);

					world.getEntitiesByType(TypeFilter.instanceOf(BoatEntity.class), chunkBox, boatEntity -> true).forEach(boat -> {
						UUID boatId = boat.getUuid();

						updateCooldowns(boatId);

						boolean isOnGround = boat.isOnGround();
						Vec3d currentPos = boat.getPos();

						if (wasOnGround.containsKey(boatId)) {
							boolean wasGrounded = wasOnGround.get(boatId);

							if (!wasGrounded && isOnGround && !landingCooldowns.containsKey(boatId)) {
								client.player.playSound(BOAT_LANDING_SOUND_EVENT, 1.0f, 1.0f);
								landingCooldowns.put(boatId, LANDING_COOLDOWN_TICKS);
							}
						}

						if (!landingCooldowns.containsKey(boatId) || landingCooldowns.get(boatId) < LANDING_COOLDOWN_TICKS - 5) {
							if (boat.isOnGround()) {
								checkForCollisionClient(world, boat, boatId, currentPos, client.player);
							}
						}

						wasOnGround.put(boatId, isOnGround);
						lastBoatPositions.put(boatId, currentPos);
					});
				}
			}
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(FOVToggleMod::resetFOVIfNeeded);

		FOVCommand.register();

		SpeedEffectRenderer.register();
		BoatSpeedometerRenderer.register();
		NoteBlockTuner.register();
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

		if (FOVToggleMod.sneakSwitched) {
			LOGGER.info("Resetting Sneak to original value: {}", FOVToggleMod.originalSneak);
			if (client != null && client.options != null) {
				client.options.getSneakToggled().setValue(FOVToggleMod.originalSneak);
				FOVToggleMod.sneakSwitched = false;
			}
		}
	}

	private void updateCooldowns(UUID boatId) {
		if (landingCooldowns.containsKey(boatId)) {
			int currentCooldown = landingCooldowns.get(boatId);
			if (currentCooldown <= 1) {
				landingCooldowns.remove(boatId);
			} else {
				landingCooldowns.put(boatId, currentCooldown - 1);
			}
		}

		if (collisionCooldowns.containsKey(boatId)) {
			int currentCooldown = collisionCooldowns.get(boatId);
			if (currentCooldown <= 1) {
				collisionCooldowns.remove(boatId);
			} else {
				collisionCooldowns.put(boatId, currentCooldown - 1);
			}
		}
	}

	private void checkForCollision(World world, BoatEntity boat, UUID boatId, Vec3d currentPos) {
		if (lastBoatPositions.containsKey(boatId)) {
			Vec3d lastPos = lastBoatPositions.get(boatId);
			Vec3d movement = currentPos.subtract(lastPos);
			double movementDistance = movement.length();

			if (movementDistance > COLLISION_DISTANCE_THRESHOLD) {
				Vec3d movementDir = movement.normalize();
				Vec3d expectedNextPos = currentPos.add(movementDir.multiply(0.2));

				if (!collisionCooldowns.containsKey(boatId) && isCollidingWithBlocks(world, boat, expectedNextPos)) {
					float volume = (float) Math.min(1.0f, movementDistance * 3.0f);

					boat.playSound(BOAT_COLLISION_SOUND_EVENT, volume, 0.8f + world.random.nextFloat() * 0.4f);

					collisionCooldowns.put(boatId, COLLISION_COOLDOWN_TICKS);
				}
			}
		}
	}

	private void checkForCollisionClient(World world, BoatEntity boat, UUID boatId, Vec3d currentPos, ClientPlayerEntity player) {
		if (lastBoatPositions.containsKey(boatId)) {
			Vec3d lastPos = lastBoatPositions.get(boatId);
			Vec3d movement = currentPos.subtract(lastPos);
			double movementDistance = movement.length();

			if (movementDistance > COLLISION_DISTANCE_THRESHOLD) {
				Vec3d movementDir = movement.normalize();
				Vec3d expectedNextPos = currentPos.add(movementDir.multiply(0.2));

				if (!collisionCooldowns.containsKey(boatId) && isCollidingWithBlocks(world, boat, expectedNextPos)) {
					float volume = (float) Math.min(1.0f, movementDistance * 3.0f);

					player.playSound(BOAT_COLLISION_SOUND_EVENT, volume, 0.8f + world.random.nextFloat() * 0.4f);

					collisionCooldowns.put(boatId, COLLISION_COOLDOWN_TICKS);
				}
			}
		}
	}

	private boolean isCollidingWithBlocks(World world, BoatEntity boat, Vec3d position) {
		Box boatBox = boat.getBoundingBox().offset(position.x - boat.getX(), position.y - boat.getY(), position.z - boat.getZ());

		boatBox = boatBox.shrink(0.1, 0.1, 0.1);

		for (int x = (int) Math.floor(boatBox.minX); x <= Math.ceil(boatBox.maxX); x++) {
			for (int y = (int) Math.floor(boatBox.minY); y <= Math.ceil(boatBox.maxY); y++) {
				for (int z = (int) Math.floor(boatBox.minZ); z <= Math.ceil(boatBox.maxZ); z++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!world.getBlockState(pos).isAir() && world.getBlockState(pos).isSolidBlock(world, pos)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean getZoomies() {
		return zoomiesEnabled;
	}
}