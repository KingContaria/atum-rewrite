package me.voidxwalker.autoreset.mixin;

import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.interfaces.ISeedStringHolder;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(GeneratorOptions.class)
public abstract class GeneratorOptionsMixin implements ISeedStringHolder {
    @Unique
    private String seedString;

    @Override
    public void atum$setSeedString(String seedString) {
        Atum.ensureState(this.seedString == null, "Seed string for this GeneratorOptions has already been set!");
        this.seedString = Objects.requireNonNull(seedString);
    }

    @Override
    public String atum$getSeedString() {
        return this.seedString;
    }

    @Inject(method = {"withHardcore", "withDimensions", "withBonusChest", "toggleBonusChest", "toggleGenerateStructures"}, at = @At("RETURN"))
    private void transferSeedString(CallbackInfoReturnable<GeneratorOptions> cir) {
        if (this.seedString != null) ((ISeedStringHolder) cir.getReturnValue()).atum$setSeedString(this.seedString);
    }
}
