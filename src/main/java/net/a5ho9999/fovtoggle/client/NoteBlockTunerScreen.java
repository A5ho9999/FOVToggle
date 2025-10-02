package net.a5ho9999.fovtoggle.client;

import net.a5ho9999.fovtoggle.NoteBlockTuner;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class NoteBlockTunerScreen extends Screen {
    private final BlockPos noteBlockPos;
    private int currentNote;
    private int selectedNote;

    // Note names for display
    private static final String[] NOTE_NAMES = {
            "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B", "C", "C#/Db",
            "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A",
            "A#/Bb", "B", "C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb"
    };

    // Short note names for buttons
    private static final String[] SHORT_NOTE_NAMES = {
            "F# (0)", "G (1)", "G# (2)", "A (3)", "A# (4)", "B (5)", "C (6)", "C# (7)",
            "D (8)", "D# (9)", "E (10)", "F (11)", "F# (12)", "G (13)", "G# (14)", "A (15)",
            "A# (16)", "B (17)", "C (18)", "C# (19)", "D (20)", "D# (21)", "E (22)", "F (23)", "F# (24)"
    };

    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 200;
    private static final int NOTE_BUTTON_SIZE = 32;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;

    private ButtonWidget cancelButton;
    private ButtonWidget[] noteButtons;

    public NoteBlockTunerScreen(BlockPos pos, int currentNote) {
        super(Text.literal("NoteBlock Tuner"));
        this.noteBlockPos = pos;
        this.currentNote = currentNote;
        this.selectedNote = currentNote;
    }

    @Override
    protected void init() {
        super.init();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            BlockState state = client.world.getBlockState(noteBlockPos);
            if (state.getBlock() == Blocks.NOTE_BLOCK) {
                currentNote = state.get(NoteBlock.NOTE);
                selectedNote = currentNote;
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        noteButtons = new ButtonWidget[25];
        int gridStartX = centerX - (5 * NOTE_BUTTON_SIZE) / 2;
        int gridStartY = centerY - (GUI_HEIGHT / 2) + 10;

        for (int i = 0; i < 25; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = gridStartX + col * (NOTE_BUTTON_SIZE + 14) - (14 * 2);
            int y = gridStartY + row * NOTE_BUTTON_SIZE;

            final int noteIndex = i;

            ButtonWidget noteButton = ButtonWidget.builder(
                            Text.literal(SHORT_NOTE_NAMES[i]),
                            button -> {
                                try {
                                    NoteBlockTuner.setAutoTuning(noteBlockPos, noteIndex);
                                    this.close();
                                } catch (Exception e) {
                                    this.close();
                                }
                            }
                    )
                    .dimensions(x, y, NOTE_BUTTON_SIZE + 14 - 1, NOTE_BUTTON_SIZE - 2)
                    .build();

            noteButtons[i] = noteButton;
            this.addDrawableChild(noteButton);
        }

        cancelButton = ButtonWidget.builder(Text.literal("Cancel"), button -> this.close()).dimensions(centerX - BUTTON_WIDTH / 2, centerY + 70, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        this.addDrawableChild(cancelButton);

        updateButtonStates();
    }

    private void updateButtonStates() {
        for (int i = 0; i < noteButtons.length && noteButtons[i] != null; i++) {
            noteButtons[i].active = (i != currentNote);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.fill(centerX - GUI_WIDTH/2, centerY - GUI_HEIGHT/2, centerX + GUI_WIDTH/2, centerY + GUI_HEIGHT/2, 0xC0000000);
        context.drawBorder(centerX - GUI_WIDTH/2, centerY - GUI_HEIGHT/2, GUI_WIDTH, GUI_HEIGHT, 0xFFAAAAAA);

        Text title = this.title;
        int titleX = centerX - this.textRenderer.getWidth(title) / 2;
        context.drawTextWithShadow(this.textRenderer, title, titleX, centerY - 80, 0xFFFFFF);

        Text currentText = Text.literal("Current: " + NOTE_NAMES[currentNote] + " (Note " + currentNote + ")");
        int currentX = centerX - this.textRenderer.getWidth(currentText) / 2;
        context.drawTextWithBackground(this.textRenderer, currentText, currentX, centerY - 65, currentText.getString().length(), 0xCCCCCC);

        Text instructionText = Text.literal("Click a note to tune the noteblock");
        int instructionX = centerX - this.textRenderer.getWidth(instructionText) / 2;
        context.drawTextWithShadow(this.textRenderer, instructionText, instructionX, centerY + 45, 0xCCCCCC);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < noteButtons.length && noteButtons[i] != null; i++) {
            ButtonWidget noteButton = noteButtons[i];
            if (noteButton.isMouseOver(mouseX, mouseY) && noteButton.active) {
                System.out.println("Manual click detected on note button: " + i);
                try {
                    NoteBlockTuner.setAutoTuning(noteBlockPos, i);
                    this.close();
                } catch (Exception e) {
                    System.err.println("Error during manual auto-tuning: " + e.getMessage());
                    this.close();
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            this.close();
            return true;
        }

        if (keyCode == 257 && selectedNote != currentNote) { // Enter
            NoteBlockTuner.setAutoTuning(noteBlockPos, selectedNote);
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    protected void applyBlur(DrawContext context) {

    }

    @Override
    public void blur() {

    }
}
