package net.a5ho9999.fovtoggle.mixin;

import net.a5ho9999.fovtoggle.NoteBlockInteractionUtil;
import net.a5ho9999.fovtoggle.client.NoteBlockTunerScreen;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class NoteBlockInteractionMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(net.minecraft.client.network.ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;

        // Ensure not tuning already
        if (world != null && player != null && hand == Hand.MAIN_HAND && !NoteBlockInteractionUtil.isTuning(hitResult)) {
            if (player.getMainHandStack().isEmpty()) {

                BlockPos pos = hitResult.getBlockPos();
                BlockState state = world.getBlockState(pos);

                if (state.getBlock() == Blocks.NOTE_BLOCK && player.isSneaking()) {
                    int currentNote = state.get(NoteBlock.NOTE);

                    client.execute(() -> {
                        client.setScreen(new NoteBlockTunerScreen(pos, currentNote));
                    });

                    cir.setReturnValue(ActionResult.SUCCESS);
                }
            }
        }
    }
}
