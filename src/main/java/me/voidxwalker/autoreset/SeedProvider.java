package me.voidxwalker.autoreset;

import java.util.Optional;

public interface SeedProvider {
    Optional<String> getSeed();

    void waitForSeed();

    boolean shouldShowSeed();
}
