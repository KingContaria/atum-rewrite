package me.voidxwalker.autoreset;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class Atum implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static AtumConfig config;
    public static KeyBinding resetKey;
    private static boolean running = false;
    private static boolean shouldReset;

    private static final SeedProvider DEFAULT_SEED_PROVIDER = new SeedProvider() {
        @Override
        public Optional<String> getSeed() {
            return Optional.of(Atum.config.seed);
        }

        @Override
        public void waitForSeed() {
        }

        @Override
        public boolean shouldShowSeed() {
            return true;
        }
    };
    private static SeedProvider seedProvider = DEFAULT_SEED_PROVIDER;

    public static void createNewWorld() {
        running = true;
        shouldReset = false;

        MinecraftClient.getInstance().openScreen(new AtumCreateWorldScreen(null));
    }

    public static boolean isRunning() {
        return running;
    }

    public static void stopRunning() {
        shouldReset = false;
        running = false;
        config.dataPackMismatch = false;
    }

    public static void scheduleReset() {
        shouldReset = true;
    }

    public static boolean isResetScheduled() {
        return shouldReset;
    }

    public static boolean shouldReset() {
        return isResetScheduled() && !isBlocking();
    }

    public static boolean isBlocking() {
        return MinecraftClient.getInstance().getOverlay() != null || isLoadingWorld();
    }

    public static boolean isInWorld() {
        return MinecraftClient.getInstance().world != null;
    }

    public static boolean isLoadingWorld() {
        return MinecraftClient.getInstance().getServer() != null && MinecraftClient.getInstance().world == null;
    }

    public static boolean inDemoMode() {
        return isRunning() && config.demoMode;
    }

    public static boolean isSetSeed() {
        return config.isSetSeed() || config.demoMode;
    }

    public static SeedProvider getSeedProvider() {
        return seedProvider;
    }

    @SuppressWarnings("unused")
    public static void setSeedProvider(SeedProvider seedProvider) {
        Atum.ensureState(seedProvider != null);
        Atum.ensureState(Atum.seedProvider == DEFAULT_SEED_PROVIDER); // Only allow changing once
        Atum.ensureState(!Atum.isRunning());
        Atum.seedProvider = seedProvider;
    }

    public static void ensureState(boolean condition) throws IllegalStateException {
        if (!condition) throw new IllegalStateException();
    }

    @Override
    public void onInitializeClient() {
        resetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Create New World",
                GLFW.GLFW_KEY_F6,
                "key.categories.atum"
        ));
    }
}
