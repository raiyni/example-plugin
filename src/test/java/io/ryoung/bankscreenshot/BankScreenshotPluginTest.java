package io.ryoung.bankscreenshot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankScreenshotPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankScreenshotPlugin.class);
		RuneLite.main(args);
	}
}