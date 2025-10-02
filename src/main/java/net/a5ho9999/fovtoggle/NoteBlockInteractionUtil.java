package net.a5ho9999.fovtoggle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class NoteBlockInteractionUtil {
    public static boolean interactWithNoteBlock(MinecraftClient client, BlockPos pos) {
        if (client.player == null || client.interactionManager == null || client.world == null) {
            return false;
        }

        ClientWorld world = client.world;
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() != Blocks.NOTE_BLOCK) {
            return false;
        }

        Vec3d hitPos = pos.toCenterPos().add(0, 0.5, 0);
        BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, pos, false);

        try {
            if (!client.player.isSneaking()) {
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                return true;
            }
            return false;
        } catch (Exception e) {
            FOVToggleMod.LOGGER.info("Failed to interact with noteblock at {}: {}", pos, e.getMessage());
            return false;
        }
    }

    public static boolean isTuning(BlockHitResult hitResult) {
        if (hitResult == null) {
            return false;
        }

        BlockPos pos = hitResult.getBlockPos();
        return NoteBlockTuner.isPositionBeingTuned(pos);
    }
}
