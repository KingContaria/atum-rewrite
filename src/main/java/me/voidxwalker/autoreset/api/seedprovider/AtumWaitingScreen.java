package me.voidxwalker.autoreset.api.seedprovider;

import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class AtumWaitingScreen extends Screen {
    private boolean decided = false;

    protected AtumWaitingScreen(Text title) {
        super(title);
    }

    protected final void cancelWorldCreation() {
        this.onDecided();
        Atum.stopRunning();
        this.onClose();
    }

    @SuppressWarnings("unused")
    protected final void continueWorldCreation() {
        this.onDecided();
        assert this.client != null;
        this.client.openScreen(new AtumCreateWorldScreen(null));
    }

    private void onDecided() {
        Atum.ensureState(!this.decided, "AtumWaitingScreen continue method(s) called more than once!");
        this.decided = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            this.cancelWorldCreation();
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
        Atum.ensureState(this.decided, "Improper closing of AtumWaitingScreen. Methods continueWorldCreation or cancelWorldCreation should be used.");
    }
}
