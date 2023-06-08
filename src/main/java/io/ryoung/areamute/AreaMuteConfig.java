package io.ryoung.areamute;

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
}
