package io.ryoung.heatmap;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HeatmapPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HeatmapPlugin.class);
		RuneLite.main(args);
	}
}