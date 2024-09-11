package me.voidxwalker.autoreset;

import me.voidxwalker.autoreset.gui.AtumWaitingScreen;
import net.minecraft.client.gui.screen.Screen;

import java.util.Optional;

public interface SeedProvider {
    /**
     * Gets a seed if a seed is available, otherwise should return empty immediately.
     * If empty can be returned, waitForSeed and getWaitingScreen should be implemented.
     */
    Optional<String> getSeed();

    /**
     * Determines whether a set seed should be present in logs, LevelLoadingScreen, and DebugHUD.
     */
    default boolean shouldShowSeed() {
        return true;
    }

    /**
     * Waits for a seed to be available. This should only be called by Atum while using SeedQueue.
     */
    default void waitForSeed() {
    }

    /**
     * Gets the waiting screen. The implemented waiting screen should run the provided onSeedFound method once a seed is available, or alternatively cancel.
     */
    default AtumWaitingScreen getWaitingScreen() {
        throw new IllegalStateException("No waiting screen available!");
    }
}
