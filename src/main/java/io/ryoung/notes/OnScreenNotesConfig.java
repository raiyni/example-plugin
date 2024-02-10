package io.ryoung.notes;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.components.ComponentConstants;

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

	@Alpha
	@ConfigItem(
		keyName = "defaultBackgroundColor",
		name = "Default Background",
		description = "Default Background Color"
	)
	default Color defaultBackground()
	{
		return ComponentConstants.STANDARD_BACKGROUND_COLOR;
	}

	@ConfigItem(
		keyName = "defaultTextColor",
		name = "Default Text Color",
		description = "Default Text Color"
	)
	default Color defaultTextColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "defaultTitleColor",
		name = "Default Title Color",
		description = "Default Title Color"
	)
	default Color defaultTitleColor()
	{
		return ColorScheme.BRAND_ORANGE;
	}
}
