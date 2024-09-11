package me.voidxwalker.autoreset.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.interfaces.ISeedStringHolder;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin implements ISeedStringHolder {
    @Unique
    private String seedString;

    @Override
    public void atum$setSeedString(String seedString) {
        Atum.ensureState(this.seedString == null); // No double setting!
        Atum.ensureState(seedString != null);
        this.seedString = seedString;
    }

    @Override
    public String atum$getSeedString() {
        return this.seedString;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;drawCenteredString(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
    private void drawSeedString(LevelLoadingScreen screen, MatrixStack matrixStack, TextRenderer textRenderer, String s, int x, int y, int color, Operation<Void> original) {
        original.call(screen, matrixStack, textRenderer, s, x, y, color);
        if (!Atum.isRunning()) return;
        if (Atum.inDemoMode()) {
            screen.drawCenteredString(matrixStack, textRenderer, "North Carolina", x, y - 20, color);
        } else if (!seedString.isEmpty()) {
            screen.drawCenteredString(matrixStack, textRenderer, Atum.getSeedProvider().shouldShowSeed() ? seedString : "Set Seed", x, y - 20, color);
        }
    }
}
