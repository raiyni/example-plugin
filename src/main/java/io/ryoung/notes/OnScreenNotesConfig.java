package io.ryoung.notes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OnScreenNotesPlugin.CONFIG_GROUP)
public interface OnScreenNotesConfig extends Config
{
	@ConfigItem(
		keyName = "useKeybinds",
		name = "Enable management menu",
		description = "Enables notes management options while holding shift key",
		position = -2
	)
	default boolean useKeybinds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "toggleMenu",
		name = "Enable toggle menu",
		description = "Enables toggle options, requires management menu"
	)
	default boolean toggleMenu()
	{
		return true;
	}

	@ConfigItem(
		keyName = "deleteMenu",
		name = "Enable delete menu",
		description = "Enables menu delete options, requires management menu"
	)
	default boolean deleteMenu()
	{
		return true;
	}

	@ConfigItem(
		keyName = "editMenu",
		name = "Enable edit menu",
		description = "Enables edit options, requires management menu"
	)
	default boolean editMenu()
	{
		return false;
	}
}
