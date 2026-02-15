package com.turtial.turtaiblty

import net.fabricmc.api.ClientModInitializer
import com.turtial.turtaiblty.config.TurtaibltyConfig

object TurtaibltyClient : ClientModInitializer {
	override fun onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		TurtaibltyConfig.load()
	}
}
