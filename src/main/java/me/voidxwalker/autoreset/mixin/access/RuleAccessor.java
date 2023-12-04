package me.voidxwalker.autoreset.mixin.access;

import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRules.Rule.class)
public interface RuleAccessor {
    @Invoker
    void callDeserialize(String var1);
}
