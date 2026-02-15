package com.turtial.turtaiblty.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TurtaibltyConfig {
	public enum ColorSource {
		SECTIONS,
		SINGLE_BASE
	}

	public enum TransitionMode {
		STEP,
		SMOOTH,
		FADE
	}

	public static final class ColorPoint {
		public float percent;
		public int color;

		public ColorPoint() {
		}

		public ColorPoint(float percent, int color) {
			this.percent = percent;
			this.color = color & 0xFFFFFF;
		}
	}

	public static final class DurabilitySection {
		public float minPercent;
		public float maxPercent;
		public List<ColorPoint> points = new ArrayList<>();

		public DurabilitySection() {
		}

		public DurabilitySection(float minPercent, float maxPercent, List<ColorPoint> points) {
			this.minPercent = minPercent;
			this.maxPercent = maxPercent;
			this.points = points;
		}
	}

	private record CacheKey(String itemId, int damage, int maxDamage, int configVersion) {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("turtaiblty");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("turtabilty.json");
	private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("turtaiblty.json");
	private static final int NO_TINT = -1;
	private static volatile int CACHE_VERSION = 0;
	private static TurtaibltyConfig INSTANCE = createDefault();
	private static final Map<CacheKey, Integer> COLOR_CACHE = new ConcurrentHashMap<>();

	public boolean enabled = true;
	public boolean tintWornArmor = true;
	public boolean tintItemIcons = false;
	public boolean affectDyeable = false;
	public boolean showWhenUndamaged = false;
	public TransitionMode transitionMode = TransitionMode.SMOOTH;
	public ColorSource colorSource = ColorSource.SECTIONS;
	public int singleBaseColor = 0x2BFF8F;

	public boolean enableLeather = true;
	public boolean enableChainmail = true;
	public boolean enableIron = true;
	public boolean enableGolden = true;
	public boolean enableDiamond = true;
	public boolean enableNetherite = true;
	public boolean enableTurtle = true;
	public boolean enableOtherVanilla = true;
	public boolean enableModdedArmor = true;

	public List<DurabilitySection> sections = new ArrayList<>();

	public static TurtaibltyConfig get() {
		return INSTANCE;
	}

	public static void load() {
		Path loadPath = Files.exists(CONFIG_PATH) ? CONFIG_PATH : LEGACY_CONFIG_PATH;
		if (!Files.exists(loadPath)) {
			INSTANCE = createDefault();
			save();
			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(loadPath)) {
			JsonElement json = JsonParser.parseReader(reader);
			TurtaibltyConfig loaded = parseWithMigration(json);
			loaded.sanitize();
			INSTANCE = loaded;
			if (!loadPath.equals(CONFIG_PATH)) {
				save();
			}
			bumpCacheVersion();
		} catch (Exception e) {
			LOGGER.warn("Failed to load config, falling back to defaults.", e);
			INSTANCE = createDefault();
			bumpCacheVersion();
		}
	}

	public static void save() {
		INSTANCE.sanitize();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to save config.", e);
		}
		bumpCacheVersion();
	}

	public static int getColorFor(ItemStack stack, int originalColor) {
		int tint = getDurabilityTintOrNone(stack);
		if (tint == NO_TINT) {
			return originalColor;
		}
		int alpha = (originalColor >>> 24) & 0xFF;
		if (alpha == 0) {
			alpha = 0xFF;
		}
		return (alpha << 24) | (tint & 0xFFFFFF);
	}

	public static int getDurabilityTintOrNone(ItemStack stack) {
		TurtaibltyConfig config = INSTANCE;
		if (!config.enabled || stack == null || stack.isEmpty()) {
			return NO_TINT;
		}
		if (!stack.isDamageableItem()) {
			return NO_TINT;
		}
		if (!config.showWhenUndamaged && stack.getDamageValue() <= 0) {
			return NO_TINT;
		}
		if (!config.affectDyeable && DyedItemColor.getOrDefault(stack, NO_TINT) != NO_TINT) {
			return NO_TINT;
		}
		if (!config.isMaterialEnabled(stack)) {
			return NO_TINT;
		}

		int maxDamage = stack.getMaxDamage();
		if (maxDamage <= 0) {
			return NO_TINT;
		}

		ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		CacheKey cacheKey = new CacheKey(key.toString(), stack.getDamageValue(), maxDamage, CACHE_VERSION);
		Integer cached = COLOR_CACHE.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		float percent = 100.0f * (1.0f - (float) stack.getDamageValue() / (float) maxDamage);
		int resolved = config.resolveColor(percent);
		resolved = applyTurtEasterEgg(stack, resolved, percent);
		COLOR_CACHE.put(cacheKey, resolved);
		return resolved;
	}

	private static TurtaibltyConfig parseWithMigration(JsonElement json) {
		if (!(json instanceof JsonObject root)) {
			return createDefault();
		}

		TurtaibltyConfig loaded = GSON.fromJson(root, TurtaibltyConfig.class);
		if (loaded == null) {
			loaded = createDefault();
		}

		if (loaded.sections == null || loaded.sections.isEmpty()) {
			if (hasLegacyHighMidLow(root)) {
				loaded.sections = migrateLegacyHighMidLow(root);
			} else {
				loaded.sections = defaultSections();
			}
		}

		return loaded;
	}

	private static boolean hasLegacyHighMidLow(JsonObject root) {
		return root.has("lowThreshold") || root.has("midThreshold") || root.has("lowColor") || root.has("midColor") || root.has("highColor");
	}

	private static List<DurabilitySection> migrateLegacyHighMidLow(JsonObject root) {
		float low = readFloat(root, "lowThreshold", 0.20f) * 100.0f;
		float mid = readFloat(root, "midThreshold", 0.50f) * 100.0f;
		if (mid < low) {
			float temp = low;
			low = mid;
			mid = temp;
		}

		int lowColor = readColor(root, "lowColor", 0xFF2A2A);
		int midColor = readColor(root, "midColor", 0xFFD83D);
		int highColor = readColor(root, "highColor", 0x2BFF8F);

		List<DurabilitySection> migrated = new ArrayList<>();
		migrated.add(new DurabilitySection(0.0f, low, new ArrayList<>(List.of(
			new ColorPoint(0.0f, lowColor),
			new ColorPoint(low, lowColor)
		))));
		migrated.add(new DurabilitySection(low, mid, new ArrayList<>(List.of(
			new ColorPoint(low, lowColor),
			new ColorPoint(mid, midColor)
		))));
		migrated.add(new DurabilitySection(mid, 100.0f, new ArrayList<>(List.of(
			new ColorPoint(mid, midColor),
			new ColorPoint(100.0f, highColor)
		))));
		return migrated;
	}

	private static float readFloat(JsonObject root, String key, float fallback) {
		try {
			if (!root.has(key)) {
				return fallback;
			}
			return root.get(key).getAsFloat();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static int readColor(JsonObject root, String key, int fallback) {
		try {
			if (!root.has(key)) {
				return fallback;
			}
			return root.get(key).getAsInt() & 0xFFFFFF;
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static TurtaibltyConfig createDefault() {
		TurtaibltyConfig config = new TurtaibltyConfig();
		config.sections = defaultSections();
		config.sanitize();
		return config;
	}

	private static List<DurabilitySection> defaultSections() {
		List<DurabilitySection> result = new ArrayList<>();
		int[] colors = new int[]{0x0C2A7A, 0xFF3A3A, 0xFF9A2F, 0xFFE14A, 0x2BFF8F};
		final float sectionSize = 20.0f;
		for (int i = 0; i < colors.length; i++) {
			float min = i * sectionSize;
			float max = (i + 1) * sectionSize;
			int color = colors[i];
			result.add(new DurabilitySection(min, max, new ArrayList<>(List.of(
				new ColorPoint(min, color),
				new ColorPoint(max, color)
			))));
		}
		return result;
	}

	private void sanitize() {
		if (sections == null) {
			sections = new ArrayList<>();
		}
		for (DurabilitySection section : sections) {
			if (section == null) {
				continue;
			}
			section.minPercent = clampPercent(section.minPercent);
			section.maxPercent = clampPercent(section.maxPercent);
			if (section.maxPercent < section.minPercent) {
				float temp = section.minPercent;
				section.minPercent = section.maxPercent;
				section.maxPercent = temp;
			}
			if (section.points == null) {
				section.points = new ArrayList<>();
			}
			section.points.removeIf(point -> point == null);
			for (ColorPoint point : section.points) {
				point.percent = clampPercent(point.percent);
				point.color = point.color & 0xFFFFFF;
			}
			section.points.sort(Comparator.comparingDouble(point -> point.percent));
			if (section.points.isEmpty()) {
				section.points.add(new ColorPoint(section.minPercent, 0xFF2A2A));
				section.points.add(new ColorPoint(section.maxPercent, 0x2BFF8F));
			}
		}

		sections.removeIf(section -> section == null);
		if (sections.isEmpty()) {
			sections = defaultSections();
		}
		sections.sort(Comparator.comparingDouble(s -> s.minPercent));
		if (transitionMode == null) {
			transitionMode = TransitionMode.SMOOTH;
		}
		if (colorSource == null) {
			colorSource = ColorSource.SECTIONS;
		}
		singleBaseColor = singleBaseColor & 0xFFFFFF;
	}

	private int resolveColor(float durabilityPercent) {
		if (colorSource == ColorSource.SINGLE_BASE) {
			return resolveSingleBaseColor(durabilityPercent);
		}
		DurabilitySection section = findSection(durabilityPercent);
		if (section == null || section.points.isEmpty()) {
			return NO_TINT;
		}
		TransitionMode mode = transitionMode == null ? TransitionMode.SMOOTH : transitionMode;
		if (mode == TransitionMode.STEP) {
			return sectionColor(section);
		}

		int idx = sections.indexOf(section);
		if (idx < 0 || idx >= sections.size() - 1) {
			return sectionColor(section);
		}
		DurabilitySection next = sections.get(idx + 1);
		float span = Math.max(0.0001f, section.maxPercent - section.minPercent);
		float t = (durabilityPercent - section.minPercent) / span;
		t = clampPercent(t * 100.0f) / 100.0f;
		if (mode == TransitionMode.FADE) {
			t = t * t * (3.0f - 2.0f * t);
		}
		return lerpColor(sectionColor(section), sectionColor(next), t);
	}

	private int resolveSingleBaseColor(float durabilityPercent) {
		float t = clampPercent(durabilityPercent) / 100.0f;
		TransitionMode mode = transitionMode == null ? TransitionMode.SMOOTH : transitionMode;
		if (mode == TransitionMode.STEP) {
			float steps = 4.0f;
			t = Math.round(t * steps) / steps;
		} else if (mode == TransitionMode.FADE) {
			t = t * t * (3.0f - 2.0f * t);
		}

		int dark = multiplyColor(singleBaseColor, 0.38f);
		int light = blendTowardWhite(singleBaseColor, 0.28f);
		return lerpColor(dark, light, t);
	}

	private static int sectionColor(DurabilitySection section) {
		if (section == null || section.points == null || section.points.isEmpty()) {
			return 0xFFFFFF;
		}
		return section.points.get(0).color & 0xFFFFFF;
	}

	private DurabilitySection findSection(float durabilityPercent) {
		DurabilitySection best = null;
		float bestWidth = Float.MAX_VALUE;
		for (DurabilitySection section : sections) {
			if (section == null) {
				continue;
			}
			if (durabilityPercent < section.minPercent || durabilityPercent > section.maxPercent) {
				continue;
			}
			float width = section.maxPercent - section.minPercent;
			if (width < bestWidth) {
				best = section;
				bestWidth = width;
			}
		}
		return best;
	}

	private boolean isMaterialEnabled(ItemStack stack) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String namespace = id.getNamespace();
		String path = id.getPath();

		if ("minecraft".equals(namespace)) {
			if (path.contains("leather")) return enableLeather;
			if (path.contains("chainmail")) return enableChainmail;
			if (path.contains("iron")) return enableIron;
			if (path.contains("golden")) return enableGolden;
			if (path.contains("diamond")) return enableDiamond;
			if (path.contains("netherite")) return enableNetherite;
			if (path.contains("turtle")) return enableTurtle;
			return enableOtherVanilla;
		}

		return enableModdedArmor;
	}

	private static int lerpColor(int a, int b, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int ar = (a >> 16) & 0xFF;
		int ag = (a >> 8) & 0xFF;
		int ab = a & 0xFF;
		int br = (b >> 16) & 0xFF;
		int bg = (b >> 8) & 0xFF;
		int bb = b & 0xFF;
		int rr = (int) (ar + (br - ar) * t);
		int rg = (int) (ag + (bg - ag) * t);
		int rb = (int) (ab + (bb - ab) * t);
		return (rr << 16) | (rg << 8) | rb;
	}

	private static int multiplyColor(int color, float factor) {
		int r = (int) (((color >> 16) & 0xFF) * factor);
		int g = (int) (((color >> 8) & 0xFF) * factor);
		int b = (int) ((color & 0xFF) * factor);
		return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static int blendTowardWhite(int color, float amount) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		r = (int) (r + (255 - r) * amount);
		g = (int) (g + (255 - g) * amount);
		b = (int) (b + (255 - b) * amount);
		return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static int applyTurtEasterEgg(ItemStack stack, int baseColor, float durabilityPercent) {
		if (stack == null || stack.isEmpty()) {
			return baseColor;
		}
		String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
		float t = clampPercent(durabilityPercent) / 100.0f;
		if (name.contains("turt")) {
			return lerpColor(0x2F7A4B, 0x82C67C, t);
		}
		if (name.contains("memory") || name.contains("rip")) {
			return lerpColor(0x3C8D74, 0x6BC8FF, t);
		}
		if (name.contains("shell")) {
			return 0x2F6A8A;
		}
		return baseColor;
	}

	private static float clampPercent(float value) {
		if (Float.isNaN(value)) {
			return 0.0f;
		}
		return Math.max(0.0f, Math.min(100.0f, value));
	}

	private static void bumpCacheVersion() {
		CACHE_VERSION++;
		COLOR_CACHE.clear();
	}
}
