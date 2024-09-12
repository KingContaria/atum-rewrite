package me.voidxwalker.autoreset.gui;

import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class AtumWaitingScreen extends Screen {
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
        this.client.openScreen(new AtumCreateWorldScreen(null));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
