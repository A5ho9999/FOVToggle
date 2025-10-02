package net.a5ho9999.fovtoggle;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class NoteBlockTuner {
    private static class TuningOperation {
        final BlockPos pos;
        final int targetNote;
        final int originalNote;
        int currentTicks;

        TuningOperation(BlockPos pos, int targetNote, int originalNote) {
            this.pos = pos;
            this.targetNote = targetNote;
            this.originalNote = originalNote;
            this.currentTicks = 0;
        }
    }

    private static final Map<BlockPos, TuningOperation> activeTuningOperations = new HashMap<>();
    private static final Queue<TuningOperation> tuningQueue = new LinkedList<>();

    // Array for pos and targets

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.interactionManager == null) {
                return;
            }

            processQueue();

            Iterator<Map.Entry<BlockPos, TuningOperation>> iterator = activeTuningOperations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, TuningOperation> entry = iterator.next();
                TuningOperation operation = entry.getValue();

                operation.currentTicks++;

                int requiredDelay = calculateDelay(client, operation);

                if (operation.currentTicks >= requiredDelay) {
                    operation.currentTicks = 0;

                    BlockState state = client.world.getBlockState(operation.pos);
                    if (state.getBlock() == Blocks.NOTE_BLOCK) {
                        int currentNote = state.get(NoteBlock.NOTE);

                        if (currentNote == operation.targetNote) {
                            // Tuning complete
                            iterator.remove();
                            if (client.player != null) {
                                client.player.sendMessage(Text.literal("§aNoteblock at " + posToString(operation.pos) + " tuned to note " + operation.targetNote + "!"), true);
                            }
                        } else {
                            // Continue tuning
                            performNoteBlockTuning(client, operation.pos);
                        }
                    } else {
                        // Noteblock was removed
                        iterator.remove();
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§cNoteblock at " + posToString(operation.pos) + " was removed during tuning!"), true);
                        }
                    }
                }
            }
        });
    }

    private static void processQueue() {
        int maxConcurrent = FOVToggleMod.getConfig().getConcurrentTuning();

        while (activeTuningOperations.size() < maxConcurrent && !tuningQueue.isEmpty()) {
            TuningOperation operation = tuningQueue.poll();
            if (operation != null) {
                activeTuningOperations.put(operation.pos, operation);

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§eStarted auto-tuning noteblock at " + posToString(operation.pos) + " to note " + operation.targetNote + "..."), true);
                }
            }
        }
    }

    private static void performNoteBlockTuning(MinecraftClient client, BlockPos pos) {
        if (client.player != null && client.interactionManager != null) {
            client.execute(() -> {
                boolean success = NoteBlockInteractionUtil.interactWithNoteBlock(client, pos);
                if (!success) {
                    FOVToggleMod.LOGGER.info("Failed to interact with noteblock at {} during auto-tuning", posToString(pos));
                }
            });
        }
    }

    public static void setAutoTuning(BlockPos pos, int note) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (activeTuningOperations.containsKey(pos)) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§cNoteblock at " + posToString(pos) + " is already being tuned!"), true);
            }
            return;
        }

        if (tuningQueue.stream().anyMatch(op -> op.pos.equals(pos))) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§cNoteblock at " + posToString(pos) + " is already queued for tuning!"), true);
            }
            return;
        }

        if (client.world != null) {
            BlockState state = client.world.getBlockState(pos);
            if (state.getBlock() != Blocks.NOTE_BLOCK) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§cBlock at " + posToString(pos) + " is not a noteblock!"), true);
                }
                return;
            }

            int currentNote = state.get(NoteBlock.NOTE);
            if (currentNote == note) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§aNoteblock at " + posToString(pos) + " is already at target note " + note + "!"), true);
                }
                return;
            }

            TuningOperation operation = new TuningOperation(pos, note, currentNote);

            int maxConcurrent = FOVToggleMod.getConfig().getConcurrentTuning();

            if (activeTuningOperations.size() < maxConcurrent) {
                activeTuningOperations.put(pos, operation);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§eStarted auto-tuning noteblock at " + posToString(pos) + " to note " + note + "..."), true);
                }
            } else {
                tuningQueue.offer(operation);
                if (client.player != null) {
                    int queuePosition = tuningQueue.size();
                    client.player.sendMessage(Text.literal("§eQueued noteblock at " + posToString(pos) + " for tuning to note " + note + " (position " + queuePosition + " in queue)"), true);
                }
            }
        }
    }

    public static void stopAutoTuning(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean wasActive = activeTuningOperations.remove(pos) != null;
        boolean wasQueued = tuningQueue.removeIf(op -> op.pos.equals(pos));

        if (wasActive || wasQueued) {
            if (client.player != null) {
                String status = wasActive ? "active tuning" : "queued tuning";
                client.player.sendMessage(Text.literal("§cStopped " + status + " for noteblock at " + posToString(pos)), true);
            }
        } else {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§cNo tuning operation found for noteblock at " + posToString(pos)), true);
            }
        }
    }

    public static void stopAllAutoTuning() {
        int stoppedActive = activeTuningOperations.size();
        int stoppedQueued = tuningQueue.size();

        activeTuningOperations.clear();
        tuningQueue.clear();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            int total = stoppedActive + stoppedQueued;
            if (total > 0) {
                client.player.sendMessage(Text.literal("§cStopped all tuning operations (" + stoppedActive + " active, " + stoppedQueued + " queued)"), true);
            } else {
                client.player.sendMessage(Text.literal("§cNo tuning operations to stop"), true);
            }
        }
    }

    public static void getTuningStatus() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (activeTuningOperations.isEmpty() && tuningQueue.isEmpty()) {
                client.player.sendMessage(Text.literal("§7No tuning operations active or queued"), true);
                return;
            }

            if (!activeTuningOperations.isEmpty()) {
                client.player.sendMessage(Text.literal("§eActive tuning operations (" + activeTuningOperations.size() + "):"), false);
                for (TuningOperation op : activeTuningOperations.values()) {
                    client.player.sendMessage(Text.literal("§7  " + posToString(op.pos) + " -> note " + op.targetNote), false);
                }
            }

            if (!tuningQueue.isEmpty()) {
                client.player.sendMessage(Text.literal("§6Queued tuning operations (" + tuningQueue.size() + "):"), false);
                int position = 1;
                for (TuningOperation op : tuningQueue) {
                    client.player.sendMessage(Text.literal("§7  " + position + ". " + posToString(op.pos) + " -> note " + op.targetNote), false);
                    position++;
                }
            }
        }
    }

    private static int calculateDelay(MinecraftClient client, TuningOperation operation) {
        if (client.world != null) {
            MinecraftServer server = client.world.getServer();

            boolean isSinglePlayer = server != null && server.isSingleplayer();

            if (isSinglePlayer) {
                return FOVToggleMod.getConfig().getNoteBlockDelay();
            } else {
                if (client.world != null) {
                    BlockState state = client.world.getBlockState(operation.pos);
                    if (state.getBlock() == Blocks.NOTE_BLOCK) {
                        int currentNote = state.get(NoteBlock.NOTE);
                        int distance = Math.abs(currentNote - operation.targetNote);

                        if (distance <= 2) {
                            return 5 + (distance == 0 ? 1 : 0); // 5-6 ticks when very close
                        } else if (distance <= 5) {
                            return 3 + (distance <= 3 ? 1 : 0); // 3-4 ticks when moderately close
                        } else {
                            return 1 + (distance > 10 ? 1 : 0); // 1-2 ticks when far
                        }
                    }
                }
            }
        }

        return 4;
    }

    public static boolean isAutoTuning() {
        return !activeTuningOperations.isEmpty() || !tuningQueue.isEmpty();
    }

    public static BlockPos getTuningPos() {
        return activeTuningOperations.isEmpty() ? null : activeTuningOperations.keySet().iterator().next();
    }

    public static boolean isPositionBeingTuned(BlockPos pos) {
        return activeTuningOperations.containsKey(pos) || tuningQueue.stream().anyMatch(op -> op.pos.equals(pos));
    }

    public static int getQueueSize() {
        return tuningQueue.size();
    }

    public static int getActiveOperationsCount() {
        return activeTuningOperations.size();
    }

    private static String posToString(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}
