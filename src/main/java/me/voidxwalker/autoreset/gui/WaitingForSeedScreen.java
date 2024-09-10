package me.voidxwalker.autoreset.gui;

import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

public class WaitingForSeedScreen extends Screen {
    public WaitingForSeedScreen() {
        super(new TranslatableText("atum.menu.waiting_for_seed"));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(0);
        this.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 70, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        new Thread(() -> {
            try {
                Atum.getSeedProvider().waitForSeed();
            } catch (Throwable t) {
                Atum.LOGGER.error("Error while waiting for seed!", t);
                Atum.stopRunning();
            } finally {
                client.execute(() -> client.openScreen(new AtumCreateWorldScreen(this)));
            }
        }).start();
    }
}
