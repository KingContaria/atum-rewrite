package me.voidxwalker.autoreset.mixin.hotkey;

import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Keyboard;debugCrashStartTime:J", ordinal = 0), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int j, CallbackInfo ci) {
        // 1 is GLFW for "clicked" (0 -> "released", 2 -> "held down")
        if (action == 1 && Atum.resetKey.matchesKey(key, scancode)) {
            if (this.client.currentScreen instanceof ControlsOptionsScreen && ((ControlsOptionsScreen) this.client.currentScreen).focusedBinding == Atum.resetKey) {
                return;
            }
            Atum.scheduleReset();
            ci.cancel();
        }
    }
}
