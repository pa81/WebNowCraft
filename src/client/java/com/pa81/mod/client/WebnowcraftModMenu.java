package com.pa81.mod.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class WebnowcraftModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            WebnowcraftConfig config = WebnowcraftConfig.get();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Webnowcraft Config")); // Text.of -> Text.literal

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // X Offset
            general.addEntry(entryBuilder.startIntField(Text.literal("X Offset"), config.xOffset)
                    .setDefaultValue(-10)
                    .setTooltip(Text.literal("Horizontal position offset"))
                    .setSaveConsumer(newValue -> config.xOffset = newValue)
                    .build());

            // Y Offset
            general.addEntry(entryBuilder.startIntField(Text.literal("Y Offset"), config.yOffset)
                    .setDefaultValue(10)
                    .setTooltip(Text.literal("Vertical position offset"))
                    .setSaveConsumer(newValue -> config.yOffset = newValue)
                    .build());

            // Scale (修正箇所: IntSliderを使ってパーセントで管理)
            // 0.5f -> 50, 2.0f -> 200 に変換して表示します
            general.addEntry(entryBuilder.startIntSlider(
                            Text.literal("Scale (%)"),
                            (int) (config.scale * 100), // 現在値を100倍してintにする
                            50,  // 最小 50%
                            200  // 最大 200%
                    )
                    .setDefaultValue(100) // デフォルト 100%
                    .setTooltip(Text.literal("HUD Scale size (50% - 200%)"))
                    .setSaveConsumer(newValue -> config.scale = newValue / 100.0f) // 保存時に100で割ってfloatに戻す
                    .build());

            // Anchor Position
            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Anchor Top-Left"), config.anchorLeft)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If true, positions from left. If false, from right."))
                    .setSaveConsumer(newValue -> config.anchorLeft = newValue)
                    .build());

            builder.setSavingRunnable(config::save);

            return builder.build();
        };
    }
}