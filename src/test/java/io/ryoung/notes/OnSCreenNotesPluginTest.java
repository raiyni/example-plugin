package io.ryoung.notes;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OnSCreenNotesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OnScreenNotesPlugin.class);
		RuneLite.main(args);
	}
}