package io.ryoung.areamute;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("areamute")
public interface AreaMuteConfig extends Config
{
	@ConfigItem(
		keyName = "filterSelf",
		name = "Filter yourself",
		description = "Filter your own messages"
	)
	default boolean filterSelf()
	{
		return false;
	}

	@ConfigItem(
		keyName = "filterFriends",
		name = "Filter friends",
		description = "Filter your friends' messages"
	)
	default boolean filterFriends()
	{
		return false;
	}

	@ConfigItem(
		keyName = "filterClanMates",
		name = "Filter clan mates",
		description = "Filter your clan mates' messages"
	)
	default boolean filterClanMates()
	{
		return false;
	}

	@ConfigItem(
		keyName = "filterCC",
		name = "Filter CC members",
		description = "Filter your CC members' messages"
	)
	default boolean filterFriendChat()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "mutedColor",
		name = "Muted Region Color",
		description = "Color of muted regions on the map"
	)
	default Color mutedColor()
	{
		return new Color(255, 0, 0, 180);
	}

	@Alpha
	@ConfigItem(
		keyName = "hoveredColor",
		name = "Hovered Region Color",
		description = "Color of hovered region on the map"
	)
	default Color hoveredColor()
	{
		return new Color(255, 255, 255, 95);
	}

	@ConfigItem(
		keyName = "regions",
		name = "",
		description = "",
		hidden = true
	)
	default String regions()
	{
		return "[]";
	}

	@ConfigItem(
		keyName = "regions",
		name = "",
		description = ""
	)
	void regions(String regions);
}
