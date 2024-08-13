package me.voidxwalker.autoreset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.voidxwalker.autoreset.mixin.access.CreateWorldScreen$ModeAccessor;
import me.voidxwalker.autoreset.mixin.access.GeneratorTypeAccessor;
import me.voidxwalker.autoreset.mixin.access.RuleAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
import org.mcsr.speedrunapi.config.SpeedrunConfigContainer;
import org.mcsr.speedrunapi.config.api.SpeedrunConfig;
import org.mcsr.speedrunapi.config.api.SpeedrunOption;
import org.mcsr.speedrunapi.config.api.annotations.Config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AtumConfig implements SpeedrunConfig {
    @Config.Ignored
    private SpeedrunConfigContainer<?> container;

    public CreateWorldScreen.Mode gameMode = CreateWorldScreen.Mode.SURVIVAL;
    public boolean structures = true;
    // renamed from difficulty to worldDifficulty in 2.1
    // 2.0 set the default to NORMAL, causing people to play on normal instead of easy because they weren't used to it
    public Difficulty worldDifficulty = Difficulty.EASY;
    @Config.Strings.MaxChars(32)
    public String seed = "";
    public boolean bonusChest = false;
    public boolean cheatsEnabled;
    public AtumGeneratorType generatorType = AtumGeneratorType.DEFAULT;
    public String generatorDetails = "";
    @Config.Access(setter = "setGameRules")
    public GameRules gameRules = new GameRules();
    @Config.Access(setter = "setDataPackSettings")
    public DataPackSettings dataPackSettings = DataPackSettings.SAFE_MODE;

    @SuppressWarnings({"unused", "FieldCanBeLocal"}) // saved to config for PaceMan
    private boolean hasLegalSettings;

    @Config.Ignored
    private boolean modifiedGameRules;

    @Config.Ignored
    public boolean dataPackMismatch;
    @Config.Ignored
    public final Path dataPackDirectory = SpeedrunConfigAPI.getConfigDir().resolve("atum").resolve("datapacks");

    @Config.Ignored
    public AttemptTracker attemptTracker = new AttemptTracker();

    {
        Atum.config = this;
    }

    public AtumConfig() throws IOException {
    }

    public boolean isSetSeed() {
        return !this.seed.isEmpty();
    }

    public void setGameRules(GameRules gameRules) {
        this.gameRules = gameRules;
        this.modifiedGameRules = this.areGameRulesModified(gameRules);
    }

    public boolean hasModifiedGameRules() {
        return this.modifiedGameRules;
    }

    private boolean areGameRulesModified(GameRules gameRules) {
        GameRules defaultGameRules = new GameRules();
        MutableBoolean modified = new MutableBoolean();
        GameRules.forEachType(new GameRules.TypeConsumer() {
            @Override
            public <T extends GameRules.Rule<T>> void accept(GameRules.Key<T> key, GameRules.Type<T> type) {
                if (gameRules.get(key).getCommandResult() != defaultGameRules.get(key).getCommandResult()) {
                    modified.setTrue();
                }
            }
        });
        return modified.booleanValue();
    }

    private JsonElement serializeGameRules(GameRules gameRules) {
        JsonObject jsonObject = new JsonObject();
        GameRules.forEachType(new GameRules.TypeConsumer() {
            @Override
            public <T extends GameRules.Rule<T>> void accept(GameRules.Key<T> key, GameRules.Type<T> type) {
                jsonObject.add(key.getName(), new JsonPrimitive(gameRules.get(key).serialize()));
            }
        });
        return jsonObject;
    }

    private GameRules deserializeGameRules(JsonElement jsonElement) {
        GameRules gameRules = new GameRules();
        GameRules.forEachType(new GameRules.TypeConsumer() {
            @Override
            public <T extends GameRules.Rule<T>> void accept(GameRules.Key<T> key, GameRules.Type<T> type) {
                ((RuleAccessor) gameRules.get(key)).callDeserialize(jsonElement.getAsJsonObject().get(key.getName()).getAsString());
            }
        });
        return gameRules;
    }

    public void setDataPackSettings(DataPackSettings dataPackSettings) {
        this.dataPackSettings = this.validateDataPackSettings(dataPackSettings);
    }

    private DataPackSettings validateDataPackSettings(DataPackSettings dataPackSettings) {
        List<String> enabled = new ArrayList<>(dataPackSettings.getEnabled());
        List<String> disabled = new ArrayList<>(dataPackSettings.getDisabled());

        // remove all fabric mod data packs, then re-add them right behind the vanilla pack
        int vanillaIndex = enabled.indexOf("vanilla");
        vanillaIndex = vanillaIndex == -1 ? 0 : vanillaIndex;
        enabled.removeAll(DataPackSettings.SAFE_MODE.getEnabled());
        enabled.removeIf(dataPack -> dataPack.startsWith("fabric/"));
        enabled.addAll(vanillaIndex, DataPackSettings.SAFE_MODE.getEnabled());

        // remove duplicates
        enabled = enabled.stream().distinct().collect(Collectors.toList());
        disabled = disabled.stream().distinct().collect(Collectors.toList());

        disabled.removeAll(enabled);

        dataPackSettings = new DataPackSettings(enabled, disabled);

        if (this.isDefaultDataPackSettings(dataPackSettings)) {
            return DataPackSettings.SAFE_MODE;
        }
        return dataPackSettings;
    }

    // When porting to versions with experimental data packs included, this might need some changes
    public boolean isDefaultDataPackSettings(DataPackSettings dataPackSettings) {
        if (DataPackSettings.SAFE_MODE == dataPackSettings) {
            return true;
        }
        return DataPackSettings.SAFE_MODE.getEnabled().equals(dataPackSettings.getEnabled()) && DataPackSettings.SAFE_MODE.getDisabled().equals(dataPackSettings.getDisabled());
    }

    public Set<String> getExpectedDataPacks() {
        Set<String> expectedDataPacks = new HashSet<>();
        expectedDataPacks.addAll(this.filterOnlyFileDataPacks(this.dataPackSettings.getEnabled()));
        expectedDataPacks.addAll(this.filterOnlyFileDataPacks(this.dataPackSettings.getDisabled()));
        return expectedDataPacks;
    }

    private Set<String> filterOnlyFileDataPacks(List<String> dataPacks) {
        Set<String> fileDataPacks = new HashSet<>(dataPacks);
        fileDataPacks.removeIf(dataPack -> !dataPack.startsWith("file/"));
        return fileDataPacks;
    }

    private JsonElement serializeDataPackSettings(DataPackSettings dataPackSettings) {
        JsonObject jsonObject = new JsonObject();
        JsonArray enabled = new JsonArray();
        for (String dataPack : dataPackSettings.getEnabled()) {
            enabled.add(dataPack);
        }
        jsonObject.add("enabled", enabled);
        JsonArray disabled = new JsonArray();
        for (String dataPack : dataPackSettings.getDisabled()) {
            disabled.add(dataPack);
        }
        jsonObject.add("disabled", disabled);
        return jsonObject;
    }

    private DataPackSettings deserializeDataPackSettings(JsonElement jsonElement) {
        List<String> enabled = this.deserializeDataPacks(jsonElement.getAsJsonObject().getAsJsonArray("enabled"));
        List<String> disabled = this.deserializeDataPacks(jsonElement.getAsJsonObject().getAsJsonArray("disabled"));
        return new DataPackSettings(enabled, disabled);
    }

    private List<String> deserializeDataPacks(JsonArray jsonArray) {
        List<String> dataPacks = new ArrayList<>();
        for (JsonElement dataPack : jsonArray) {
            String dataPackName = dataPack.getAsString();
            if (dataPacks.contains(dataPackName)) {
                continue;
            }
            dataPacks.add(dataPackName);
        }
        return dataPacks;
    }

    public void save() {
        try {
            this.container.save();
        } catch (IOException e) {
            Atum.LOGGER.warn("Failed to save Atum config.");
        }
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        Class<?> type = field.getType();
        if (GameRules.class.equals(type)) {
            return new SpeedrunConfigAPI.CustomOption.Builder<GameRules>(config, this, field, idPrefix)
                    .fromJson(((option, config_, configStorage, optionField, jsonElement) -> option.set(this.deserializeGameRules(jsonElement))))
                    .toJson(((option, config_, configStorage, optionField) -> this.serializeGameRules(option.get())))
                    .build();
        }
        if (DataPackSettings.class.equals(type)) {
            return new SpeedrunConfigAPI.CustomOption.Builder<DataPackSettings>(config, this, field, idPrefix)
                    .fromJson(((option, config_, configStorage, optionField, jsonElement) -> option.set(this.deserializeDataPackSettings(jsonElement))))
                    .toJson(((option, config_, configStorage, optionField) -> this.serializeDataPackSettings(option.get())))
                    .build();
        }
        return SpeedrunConfig.super.parseField(field, config, idPrefix);
    }

    public boolean updateHasLegalSettings() {
        return this.hasLegalSettings = (this.gameMode == CreateWorldScreen.Mode.SURVIVAL || this.gameMode == CreateWorldScreen.Mode.HARDCORE) &&
                this.structures &&
                !this.bonusChest &&
                !this.cheatsEnabled &&
                this.generatorType == AtumGeneratorType.DEFAULT &&
                !this.areGameRulesModified(this.gameRules) &&
                this.isDefaultDataPackSettings(this.dataPackSettings);
    }

    public Text getIllegalSettingsWarning() {
        List<Text> warnings = this.getIllegalSettingsTexts();
        if (warnings.isEmpty()) {
            return new TranslatableText("gui.none");
        }
        MutableText warning = warnings.remove(0).shallowCopy();
        for (Text w : warnings) {
            warning.append(", ").append(w);
        }
        return warning;
    }

    private List<Text> getIllegalSettingsTexts() {
        List<Text> texts = new ArrayList<>();
        if (this.gameMode != CreateWorldScreen.Mode.SURVIVAL && this.gameMode != CreateWorldScreen.Mode.HARDCORE) {
            texts.add(new TranslatableText("selectWorld.gameMode").append(": ").append(new TranslatableText("selectWorld.gameMode." + ((CreateWorldScreen$ModeAccessor) (Object) this.gameMode).getTranslationSuffix())));
        }
        if (this.cheatsEnabled) {
            texts.add(new TranslatableText("selectWorld.allowCommands").append(" ").append(ScreenTexts.ON));
        }
        if (!this.structures) {
            texts.add(new TranslatableText("selectWorld.mapFeatures").append(" ").append(ScreenTexts.OFF));
        }
        if (this.bonusChest) {
            texts.add(new TranslatableText("selectWorld.bonusItems").append(" ").append(ScreenTexts.ON));
        }
        if (this.generatorType != AtumGeneratorType.DEFAULT) {
            texts.add(new TranslatableText("selectWorld.mapType").append(" ").append(this.generatorType.get().getTranslationKey()));
        }
        if (this.modifiedGameRules) {
            texts.add(new TranslatableText("selectWorld.gameRules").append(": Modified"));
        }
        if (!this.isDefaultDataPackSettings(this.dataPackSettings)) {
            String dataPackInformation;
            if (this.dataPackMismatch) {
                dataPackInformation = "? | ?";
            } else {
                dataPackInformation = this.filterOnlyFileDataPacks(this.dataPackSettings.getEnabled()).size() + " | " + this.filterOnlyFileDataPacks(this.dataPackSettings.getDisabled()).size();
            }
            texts.add(new TranslatableText("selectWorld.dataPacks").append(": " + dataPackInformation));
        }
        return texts;
    }

    public void resetToLegalSettings() {
        if (this.gameMode != CreateWorldScreen.Mode.HARDCORE) {
            this.gameMode = CreateWorldScreen.Mode.SURVIVAL;
        }
        this.structures = true;
        this.bonusChest = false;
        this.cheatsEnabled = false;
        this.generatorType = AtumGeneratorType.DEFAULT;
        this.generatorDetails = "";
        this.setGameRules(new GameRules());
        if (Files.exists(this.dataPackDirectory)) {
            try {
                FileUtils.cleanDirectory(this.dataPackDirectory.toFile());
            } catch (IOException e) {
                Atum.LOGGER.error("Failed to clear datapack directory!", e);
            }
        }
        this.setDataPackSettings(DataPackSettings.SAFE_MODE);
    }

    public List<String> getDebugText() {
        List<String> debugText = new ArrayList<>();

        debugText.add("");
        if (this.isSetSeed()) {
            debugText.add("Resetting the seed \"" + this.seed + "\"");
        } else {
            debugText.add("Resetting a random seed");
        }

        for (Text text : this.getIllegalSettingsTexts()) {
            debugText.add(text.getString());
        }

        return debugText;
    }

    @Override
    public void finishInitialization(SpeedrunConfigContainer<?> container) {
        this.container = container;
    }

    @Override
    public void finishLoading() {
        this.updateHasLegalSettings();
    }

    @Override
    public String modID() {
        return "atum";
    }

    @Override
    public @NotNull Screen createConfigScreen(Screen parent) {
        // isAvailable() already takes care of this, but because it's so important we do another check just to be completely sure Atum is not running when the player opens the Atum config
        if (Atum.isRunning()) {
            throw new IllegalStateException("Cannot configure Atum while it's running.");
        }
        return new AtumCreateWorldScreen(parent);
    }

    @Override
    public boolean isAvailable() {
        return !Atum.isRunning();
    }

    @SuppressWarnings("unused")
    public enum AtumGeneratorType {
        DEFAULT(GeneratorTypeAccessor.getDEFAULT()),
        FLAT(GeneratorTypeAccessor.getFLAT()),
        LARGE_BIOMES(GeneratorTypeAccessor.getLARGE_BIOMES()),
        AMPLIFIED(GeneratorTypeAccessor.getAMPLIFIED()),
        SINGLE_BIOME_SURFACE(GeneratorTypeAccessor.getSINGLE_BIOME_SURFACE()),
        SINGLE_BIOME_CAVES(GeneratorTypeAccessor.getSINGLE_BIOME_CAVES()),
        SINGLE_BIOME_FLOATING_ISLANDS(GeneratorTypeAccessor.getSINGLE_BIOME_FLOATING_ISLANDS()),
        DEBUG(GeneratorTypeAccessor.getDEBUG_ALL_BLOCK_STATES());

        private final GeneratorType generatorType;

        AtumGeneratorType(GeneratorType generatorType) {
            this.generatorType = generatorType;
        }

        public GeneratorType get() {
            return this.generatorType;
        }

        public static @Nullable AtumGeneratorType from(GeneratorType generatorType) {
            for (AtumGeneratorType atumGeneratorType : values()) {
                if (atumGeneratorType.get() == generatorType) {
                    return atumGeneratorType;
                }
            }
            return null;
        }
    }
}
