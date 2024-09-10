package me.voidxwalker.autoreset.mixin;

import me.voidxwalker.autoreset.interfaces.ISeedStringHolder;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GeneratorOptions.class)
public abstract class GeneratorOptionsMixin implements ISeedStringHolder {
    @Unique
    private String seedString;

    @Override
    public void atum$setSeedString(String seedString) {
        assert this.seedString == null; // No double setting!
        assert seedString != null;
        this.seedString = seedString;
    }

    @Override
    public String atum$getSeedString() {
        return this.seedString;
    }

    @Inject(method = {"withHardcore", "withDimensions", "withBonusChest", "toggleBonusChest", "toggleGenerateStructures"}, at = @At("RETURN"))
    private void transferSeedString(CallbackInfoReturnable<GeneratorOptions> cir) {
        ((ISeedStringHolder) cir.getReturnValue()).atum$setSeedString(this.seedString);
    }
}
