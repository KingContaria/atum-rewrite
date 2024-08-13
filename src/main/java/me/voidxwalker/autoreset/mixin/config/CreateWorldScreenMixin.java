package me.voidxwalker.autoreset.mixin.config;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.voidxwalker.autoreset.AttemptTracker;
import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import me.voidxwalker.autoreset.interfaces.IMoreOptionsDialog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {

    @Shadow
    @Final
    private Screen parent;

    @Shadow
    private Difficulty safeDifficulty;
    @Shadow
    private Difficulty difficulty;
    @Shadow
    private CreateWorldScreen.Mode currentMode;
    @Shadow
    private boolean cheatsEnabled;
    @Shadow
    private boolean tweakedCheats;
    @Shadow
    private GameRules gameRules;
    @Shadow
    protected DataPackSettings dataPackSettings;
    @Shadow
    private @Nullable Path dataPackTempDir;

    @Shadow
    @Final
    public MoreOptionsDialog moreOptionsDialog;
    @Shadow
    private TextFieldWidget levelNameField;
    @Shadow
    private ButtonWidget createLevelButton;
    @Shadow
    private ButtonWidget dataPacksButton;

    @Shadow
    protected abstract void createLevel();

    protected CreateWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/gui/screen/world/MoreOptionsDialog;)V", at = @At("TAIL"))
    private void loadAtumConfigurations(CallbackInfo ci) {
        if (!this.isAtum()) {
            return;
        }

        this.currentMode = Atum.config.gameMode;
        this.safeDifficulty = this.difficulty = Atum.config.worldDifficulty;
        this.cheatsEnabled = Atum.config.cheatsEnabled;
        this.tweakedCheats = true;
        if (Atum.config.hasModifiedGameRules()) {
            this.gameRules.setAllValues(Atum.config.gameRules, null);
        }
        this.dataPackSettings = new DataPackSettings(Atum.config.dataPackSettings.getEnabled(), Atum.config.dataPackSettings.getDisabled());

        ((IMoreOptionsDialog) this.moreOptionsDialog).atum$loadAtumConfigurations();

        if (!Atum.isRunning()) {
            this.dataPackTempDir = Atum.config.dataPackDirectory;
            return;
        }

        if (!Atum.config.isDefaultDataPackSettings(this.dataPackSettings)) {
            if (Files.isDirectory(Atum.config.dataPackDirectory)) {
                this.dataPackTempDir = CreateWorldScreen.copyDataPack(Atum.config.dataPackDirectory, this.client);
                if (this.dataPackTempDir == null) {
                    Atum.config.dataPackMismatch = true;
                    Atum.LOGGER.warn("Data pack mismatch, failed to copy data packs!");
                }
            } else {
                Atum.config.dataPackMismatch = true;
                Atum.LOGGER.warn("Data pack mismatch, the Atum data pack directory is missing!");
            }
        }
    }

    @WrapWithCondition(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;addButton(Lnet/minecraft/client/gui/widget/AbstractButtonWidget;)Lnet/minecraft/client/gui/widget/AbstractButtonWidget;", ordinal = 0), slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ScreenTexts;CANCEL:Lnet/minecraft/text/Text;")
    ))
    private boolean captureCancelButton(CreateWorldScreen screen, AbstractButtonWidget button) {
        return !this.isAtum();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void modifyAtumCreateWorldScreen(CallbackInfo info) {
        if (!this.isAtum()) {
            return;
        }

        if (Atum.isRunning()) {
            if (Atum.config.isSetSeed()) {
                this.levelNameField.setText(Atum.config.attemptTracker.incrementAndGetWorldName(AttemptTracker.Type.SSG));
                Atum.LOGGER.info(String.format("Creating \"%s\" with seed \"%s\"...", this.levelNameField.getText(), Atum.config.seed));
            } else {
                this.levelNameField.setText(Atum.config.attemptTracker.incrementAndGetWorldName(AttemptTracker.Type.RSG));
                Atum.LOGGER.info(String.format("Creating \"%s\"...", this.levelNameField.getText()));
            }
            this.createLevel();
        } else {
            if (((IMoreOptionsDialog) this.moreOptionsDialog).atum$isSetSeed()) {
                this.levelNameField.setText(Atum.config.attemptTracker.getWorldName(AttemptTracker.Type.SSG));
            } else {
                this.levelNameField.setText(Atum.config.attemptTracker.getWorldName(AttemptTracker.Type.RSG));
            }
            this.levelNameField.setSelected(false);
            this.levelNameField.setEditable(false);
            this.levelNameField.setFocusUnlocked(false);
            this.levelNameField.active = false;

            this.dataPacksButton.active = this.dataPackTempDir != null;

            this.createLevelButton.setMessage(new TranslatableText("gui.done"));
        }
    }

    @Inject(method = "toggleMoreOptions", at = @At("TAIL"))
    private void updateLevelNameField(CallbackInfo ci) {
        if (this.isAtum()) {
            if (((IMoreOptionsDialog) this.moreOptionsDialog).atum$isSetSeed()) {
                this.levelNameField.setText(Atum.config.attemptTracker.getWorldName(AttemptTracker.Type.SSG));
            } else {
                this.levelNameField.setText(Atum.config.attemptTracker.getWorldName(AttemptTracker.Type.RSG));
            }
        }
    }

    @Inject(method = "createLevel", at = @At("HEAD"), cancellable = true)
    private void saveAtumConfigurations(CallbackInfo ci) {
        assert this.client != null;
        if (!this.isAtum() || Atum.isRunning()) {
            return;
        }

        Atum.config.gameMode = this.currentMode;
        Atum.config.worldDifficulty = this.difficulty;
        Atum.config.cheatsEnabled = this.cheatsEnabled;
        Atum.config.setGameRules(this.gameRules.copy());
        Atum.config.setDataPackSettings(this.dataPackSettings);

        ((IMoreOptionsDialog) this.moreOptionsDialog).atum$saveAtumConfigurations();

        if (Atum.config.updateHasLegalSettings()) {
            Atum.config.save();
            this.client.openScreen(this.parent);
        } else {
            this.client.openScreen(new ConfirmScreen(confirm -> {
                if (!confirm) {
                    Atum.config.resetToLegalSettings();
                }
                Atum.config.save();
                this.client.openScreen(this.parent);
            }, new TranslatableText("atum.menu.legal_settings.warning"), Atum.config.getIllegalSettingsWarning(), new TranslatableText("atum.menu.legal_settings.confirm"), new TranslatableText("atum.menu.legal_settings.reset")));
        }

        ci.cancel();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;drawStringWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0), slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=selectWorld.resultFolder")
    ))
    private boolean doNotShowResultFolderOnConfigScreen(CreateWorldScreen screen, MatrixStack matrices, TextRenderer textRenderer, String string, int x, int y, int color) {
        return !this.isAtum();
    }

    @ModifyExpressionValue(method = "copyDataPack(Ljava/nio/file/Path;Lnet/minecraft/client/MinecraftClient;)Ljava/nio/file/Path;", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private static Stream<Path> filterAtumDataPacks(Stream<Path> dataPacks, Path directory, MinecraftClient minecraftClient) {
        if (directory.equals(Atum.config.dataPackDirectory)) {
            Set<String> expectedDataPacks = Atum.config.getExpectedDataPacks();

            dataPacks = dataPacks.filter(path -> {
                String dataPackName = path.toString().replace("\\", "/").replaceFirst(Atum.config.dataPackDirectory.toString().replace("\\", "/"), "file");
                return expectedDataPacks.remove(dataPackName);
            }).collect(Collectors.toList()).stream();

            if (!expectedDataPacks.isEmpty()) {
                Atum.config.dataPackMismatch = true;
                Atum.LOGGER.warn("Data pack mismatch, some of the configured files are missing!");
            }
        }
        return dataPacks;
    }

    @Unique
    private boolean isAtum() {
        return (Object) this instanceof AtumCreateWorldScreen;
    }
}
