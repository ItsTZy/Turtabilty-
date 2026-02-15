package com.turtial.turtaiblty.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClothConfig {
	private static final class SectionEditor {
		int minPercent;
		int maxPercent;
		int color;
		boolean delete;
	}

	private ClothConfig() {
	}

	public static Screen create(Screen parent) {
		TurtaibltyConfig config = TurtaibltyConfig.get();
		List<SectionEditor> sections = toSectionEditors(config.sections);
		boolean[] addSection = new boolean[]{false};
		boolean[] resetSections = new boolean[]{false};

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("title.turtaiblty.config"));

		ConfigEntryBuilder eb = builder.entryBuilder();
		ConfigCategory basic = builder.getOrCreateCategory(Component.translatable("category.turtaiblty.basic"));
		ConfigCategory materials = builder.getOrCreateCategory(Component.translatable("category.turtaiblty.materials"));
		ConfigCategory sectionsCategory = builder.getOrCreateCategory(Component.translatable("category.turtaiblty.sections"));

		basic.addEntry(eb.startTextDescription(Component.literal("Simple mode: each section has Min %, Max %, and one Color.")).build());
		basic.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.enabled"), config.enabled)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enabled = v)
			.build());
		basic.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.tint_worn_armor"), config.tintWornArmor)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.tintWornArmor = v)
			.build());
		basic.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.affect_dyeable"), config.affectDyeable)
			.setDefaultValue(false)
			.setSaveConsumer(v -> config.affectDyeable = v)
			.build());
		basic.addEntry(eb.startBooleanToggle(Component.literal("Color At 100% Durability"), config.showWhenUndamaged)
			.setDefaultValue(false)
			.setSaveConsumer(v -> config.showWhenUndamaged = v)
			.build());
		basic.addEntry(eb.startEnumSelector(Component.literal("Color Transition"), TurtaibltyConfig.TransitionMode.class,
				config.transitionMode == null ? TurtaibltyConfig.TransitionMode.SMOOTH : config.transitionMode)
			.setDefaultValue(TurtaibltyConfig.TransitionMode.SMOOTH)
			.setSaveConsumer(v -> config.transitionMode = v)
			.build());

		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.leather"), config.enableLeather)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableLeather = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.chainmail"), config.enableChainmail)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableChainmail = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.iron"), config.enableIron)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableIron = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.golden"), config.enableGolden)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableGolden = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.diamond"), config.enableDiamond)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableDiamond = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.netherite"), config.enableNetherite)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableNetherite = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.turtle"), config.enableTurtle)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableTurtle = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.other_vanilla"), config.enableOtherVanilla)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableOtherVanilla = v)
			.build());
		materials.addEntry(eb.startBooleanToggle(Component.translatable("option.turtaiblty.material.modded"), config.enableModdedArmor)
			.setDefaultValue(true)
			.setSaveConsumer(v -> config.enableModdedArmor = v)
			.build());

		sectionsCategory.addEntry(eb.startTextDescription(Component.literal("Add/remove sections and edit Min %, Max %, Color.")).build());
		sectionsCategory.addEntry(eb.startBooleanToggle(Component.literal("Add New Section"), false)
			.setDefaultValue(false)
			.setSaveConsumer(v -> addSection[0] = v)
			.build());
		sectionsCategory.addEntry(eb.startBooleanToggle(Component.literal("Reset Sections To Default"), false)
			.setDefaultValue(false)
			.setSaveConsumer(v -> resetSections[0] = v)
			.build());

		if (sections.isEmpty()) {
			sections.addAll(defaultSectionEditors());
		}

		for (int i = 0; i < sections.size(); i++) {
			SectionEditor section = sections.get(i);
			SubCategoryBuilder sectionUi = eb.startSubCategory(Component.literal("Section " + (i + 1) + " (" + section.minPercent + "%-" + section.maxPercent + "%)"));

			sectionUi.add(eb.startIntField(Component.literal("Min %"), section.minPercent)
				.setDefaultValue(section.minPercent)
				.setMin(0)
				.setMax(100)
				.setSaveConsumer(v -> section.minPercent = v)
				.build());
			sectionUi.add(eb.startIntField(Component.literal("Max %"), section.maxPercent)
				.setDefaultValue(section.maxPercent)
				.setMin(0)
				.setMax(100)
				.setSaveConsumer(v -> section.maxPercent = v)
				.build());
			sectionUi.add(eb.startColorField(Component.literal("Color"), section.color)
				.setAlphaMode(false)
				.setDefaultValue(section.color)
				.setSaveConsumer(v -> section.color = v & 0xFFFFFF)
				.build());
			sectionUi.add(eb.startBooleanToggle(Component.literal("Delete This Section"), false)
				.setDefaultValue(false)
				.setSaveConsumer(v -> section.delete = v)
				.build());

			sectionsCategory.addEntry(sectionUi.build());
		}

		builder.setSavingRunnable(() -> {
			List<SectionEditor> finalEditors = new ArrayList<>();
			for (SectionEditor section : sections) {
				if (section.delete) continue;

				section.minPercent = clamp(section.minPercent, 0, 100);
				section.maxPercent = clamp(section.maxPercent, 0, 100);
				if (section.maxPercent < section.minPercent) {
					int temp = section.minPercent;
					section.minPercent = section.maxPercent;
					section.maxPercent = temp;
				}
				section.color = section.color & 0xFFFFFF;
				finalEditors.add(section);
			}

			if (addSection[0]) {
				finalEditors.add(defaultSectionEditor());
			}
			if (finalEditors.isEmpty()) {
				finalEditors.add(defaultSectionEditor());
			}
			if (resetSections[0]) {
				finalEditors = defaultSectionEditors();
			}

			finalEditors.sort(Comparator.comparingInt(s -> s.minPercent));
			config.sections = toConfigSections(finalEditors);
			config.tintItemIcons = false;
			TurtaibltyConfig.save();
		});

		return builder.build();
	}

	private static List<SectionEditor> toSectionEditors(List<TurtaibltyConfig.DurabilitySection> sections) {
		List<SectionEditor> editors = new ArrayList<>();
		if (sections == null) {
			return editors;
		}
		for (TurtaibltyConfig.DurabilitySection section : sections) {
			if (section == null) {
				continue;
			}
			SectionEditor editor = new SectionEditor();
			editor.minPercent = Math.round(section.minPercent);
			editor.maxPercent = Math.round(section.maxPercent);
			if (section.points != null && !section.points.isEmpty()) {
				editor.color = section.points.get(0).color & 0xFFFFFF;
			} else {
				editor.color = 0x2BFF8F;
			}
			editors.add(editor);
		}
		return editors;
	}

	private static List<TurtaibltyConfig.DurabilitySection> toConfigSections(List<SectionEditor> editors) {
		List<TurtaibltyConfig.DurabilitySection> out = new ArrayList<>();
		for (SectionEditor editor : editors) {
			TurtaibltyConfig.DurabilitySection section = new TurtaibltyConfig.DurabilitySection();
			section.minPercent = editor.minPercent;
			section.maxPercent = editor.maxPercent;
			int color = editor.color & 0xFFFFFF;
			section.points = new ArrayList<>(List.of(
				new TurtaibltyConfig.ColorPoint(editor.minPercent, color),
				new TurtaibltyConfig.ColorPoint(editor.maxPercent, color)
			));
			out.add(section);
		}
		return out;
	}

	private static SectionEditor defaultSectionEditor() {
		SectionEditor editor = new SectionEditor();
		editor.minPercent = 0;
		editor.maxPercent = 100;
		editor.color = 0x2BFF8F;
		return editor;
	}

	private static List<SectionEditor> defaultSectionEditors() {
		int[] colors = new int[]{0x0C2A7A, 0xFF3A3A, 0xFF9A2F, 0xFFE14A, 0x2BFF8F};
		List<SectionEditor> out = new ArrayList<>();
		for (int i = 0; i < colors.length; i++) {
			SectionEditor editor = new SectionEditor();
			editor.minPercent = i * 20;
			editor.maxPercent = (i + 1) * 20;
			editor.color = colors[i];
			out.add(editor);
		}
		return out;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
