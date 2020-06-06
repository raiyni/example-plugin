package io.ryoung.bankscreenshot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("bankscreenshot")
public interface BankScreenshotConfig extends Config
{
	@ConfigItem(
		keyName = "hotkey",
		name = "Hotkey",
		description = "The key to press to screenshot your bank"
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "button",
		name = "Show button",
		description = "Show a button to press to take a screenshot"
	)
	default boolean button()
	{
		return true;
	}

	@ConfigItem(
		keyName = "title",
		name = "Show title in screenshot",
		description = "Show bank title in the screenshot"
	)
	default boolean title()
	{
		return true;
	}
}
