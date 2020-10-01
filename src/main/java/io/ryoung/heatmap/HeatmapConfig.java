package io.ryoung.heatmap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("heatmap")
public interface HeatmapConfig extends Config
{
	@ConfigItem(
		keyName = "tutorial",
		name = "Show Heatmap Tutorial",
		description = "Show Heatmap Tutorial",
		position = 1
	)
	default boolean showTutorial()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tutorial",
		name = "",
		description = ""
	)
	void setTutorial(boolean tutorial);
}
