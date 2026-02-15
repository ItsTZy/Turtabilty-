package com.turtial.turtaiblty.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.turtial.turtaiblty.config.TurtaibltyConfig;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
	@Unique
	private static final ThreadLocal<ItemStack> TURTAIBLTY$CURRENT_STACK = new ThreadLocal<>();

	@Inject(
		method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V",
		at = @At("HEAD")
	)
	private void turtaiblty$captureCurrentStack(
		EquipmentClientInfo.LayerType layerType,
		ResourceKey<EquipmentAsset> equipmentAssetId,
		Model model,
		ItemStack stack,
		PoseStack poseStack,
		MultiBufferSource buffers,
		int light,
		ResourceLocation textureOverride,
		CallbackInfo ci
	) {
		TURTAIBLTY$CURRENT_STACK.set(stack);
	}

	@Inject(
		method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V",
		at = @At("RETURN")
	)
	private void turtaiblty$clearCurrentStack(
		EquipmentClientInfo.LayerType layerType,
		ResourceKey<EquipmentAsset> equipmentAssetId,
		Model model,
		ItemStack stack,
		PoseStack poseStack,
		MultiBufferSource buffers,
		int light,
		ResourceLocation textureOverride,
		CallbackInfo ci
	) {
		TURTAIBLTY$CURRENT_STACK.remove();
	}

	@ModifyArg(
		method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
		),
		index = 4
	)
	private int turtaiblty$modifyArmorLayerColor(int originalColor) {
		ItemStack stack = TURTAIBLTY$CURRENT_STACK.get();
		if (!TurtaibltyConfig.get().tintWornArmor) {
			return originalColor;
		}
		return TurtaibltyConfig.getColorFor(stack, originalColor);
	}
}
