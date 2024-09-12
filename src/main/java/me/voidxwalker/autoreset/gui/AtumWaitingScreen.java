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

    @SuppressWarnings("unused")
    protected final void cancel(){
        Atum.stopRunning();
        onClose();
    }

    @SuppressWarnings("unused")
    protected final void onSeedFound(){
        assert this.client != null;
        acwsOpened = true;
        this.client.openScreen(new AtumCreateWorldScreen(null));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void removed() {
        Atum.ensureState(acwsOpened || !Atum.isRunning(), "Improper closing of AtumWaitingScreen. Methods onSeedFound or cancel should be used.");
    }
}
