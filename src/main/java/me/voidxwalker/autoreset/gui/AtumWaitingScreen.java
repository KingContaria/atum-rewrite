package me.voidxwalker.autoreset.gui;

import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class AtumWaitingScreen extends Screen {
    private boolean acwsOpened = false;

    protected AtumWaitingScreen(Text title) {
        super(title);
    }

    protected final void cancelWorldCreation() {
        Atum.stopRunning();
        onClose();
    }

    @SuppressWarnings("unused")
    protected final void continueWorldCreation() {
        assert this.client != null;
        this.acwsOpened = true;
        this.client.openScreen(new AtumCreateWorldScreen(null));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            cancelWorldCreation();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void removed() {
        Atum.ensureState(this.acwsOpened || !Atum.isRunning(), "Improper closing of AtumWaitingScreen. Methods continueWorldCreation or cancelWorldCreation should be used.");
    }
}
