package me.voidxwalker.autoreset.mixin;

import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(OptionsScreen.class)
public class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void addAutoResetButton(CallbackInfo ci) {
        if (Atum.running) {
            this.addButton(new ButtonWidget(0, this.height - 20, 100, 20, new TranslatableText("menu.stop_resets"), (buttonWidget) -> {
                Atum.running = false;
                if (client.world != null) {
                    client.world.disconnect();
                    client.disconnect(new SaveLevelScreen(new TranslatableText("menu.savingLevel")));
                    client.openScreen(new TitleScreen());
                }
            }));
        }
    }
}
