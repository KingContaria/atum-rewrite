package me.voidxwalker.autoreset;

import net.minecraft.client.gui.screen.Screen;

import java.util.Optional;

public interface SeedProvider {
    Optional<String> getSeed();

    void waitForSeed();

    boolean shouldShowSeed();

    Screen getWaitingScreen();
}
