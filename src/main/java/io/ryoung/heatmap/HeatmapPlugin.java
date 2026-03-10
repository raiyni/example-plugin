package io.ryoung.heatmap;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Bank Heatmap"
)
public class HeatmapPlugin extends Plugin
{
	enum HEATMAP_MODE
	{
		NULL,
		HA,
		GE
	}

	private static final List<Integer> TAB_VARBITS = ImmutableList.of(
		VarbitID.BANK_TAB_1,
		VarbitID.BANK_TAB_2,
		VarbitID.BANK_TAB_3,
		VarbitID.BANK_TAB_4,
		VarbitID.BANK_TAB_5,
		VarbitID.BANK_TAB_6,
		VarbitID.BANK_TAB_7,
		VarbitID.BANK_TAB_8,
		VarbitID.BANK_TAB_9
	);

	@Inject
	private Client client;

	@Inject
	private HeatmapCalculation heatmapCalculation;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HeatmapItemOverlay heatmapItemOverlay;

	@Inject
	private HeatmapTutorialOverlay heatmapTutorialOverlay;

	@Inject
	private HeatmapConfig config;

	@Getter
	private HEATMAP_MODE heatmapMode = HEATMAP_MODE.NULL;

	@Override
	protected void startUp()
	{
		overlayManager.add(heatmapItemOverlay);
		overlayManager.add(heatmapTutorialOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(heatmapItemOverlay);
		overlayManager.remove(heatmapTutorialOverlay);
		heatmapMode = HEATMAP_MODE.NULL;
	}

	@Provides
	HeatmapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HeatmapConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"heatmap".equals(event.getGroup()) || !"tutorial".equals(event.getKey()))
		{
			return;
		}

		if (config.showTutorial())
		{
			overlayManager.add(heatmapTutorialOverlay);
		}
		else
		{
			overlayManager.remove(heatmapTutorialOverlay);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_BUILD)
		{
			Item[] items = getBankTabItems();
			heatmapItemOverlay.getHeatmapImages().invalidateAll();
			heatmapCalculation.calculate(items);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getType() != MenuAction.CC_OP.getId() || !event.getOption().equals("Show menu")
			|| (event.getActionParam1() >> 16) != InterfaceID.BANKMAIN)
		{
			return;
		}

		client.getMenu().createMenuEntry(-1)
			.setOption("Toggle")
			.setTarget("GE Heatmap")
			.setType(MenuAction.RUNELITE)
			.onClick(this::onClick)
			.setDeprioritized(true);

		client.getMenu().createMenuEntry(-1)
			.setOption("Toggle")
			.setTarget("HA Heatmap")
			.setType(MenuAction.RUNELITE)
			.onClick(this::onClick)
			.setDeprioritized(true);

		if (config.showTutorial())
		{
			client.getMenu().createMenuEntry(-1)
				.setOption("")
				.setTarget("Disable tutorial")
				.setType(MenuAction.WIDGET_FIFTH_OPTION)
				.setIdentifier(event.getIdentifier())
				.setParam0(event.getActionParam0())
				.setParam1(event.getActionParam1())
				.onClick(e -> config.setTutorial(false))
				.setDeprioritized(true);
		}
	}

	public void onClick(MenuEntry e)
	{
		HEATMAP_MODE mode = e.getTarget().equals("GE Heatmap") ? HEATMAP_MODE.GE : HEATMAP_MODE.HA;
		if (mode == heatmapMode)
		{
			heatmapMode = HEATMAP_MODE.NULL;
		}
		else
		{
			heatmapItemOverlay.getHeatmapImages().invalidateAll();
			heatmapMode = mode;
		}
	}

	private Item[] getBankTabItems()
	{
		final ItemContainer container = client.getItemContainer(InventoryID.BANK);
		if (container == null)
		{
			return null;
		}

		final Item[] items = container.getItems();
		int currentTab = client.getVarbitValue(VarbitID.BANK_TAB_DISPLAY);

		if (currentTab > 0 && currentTab < 14)
		{
			int startIndex = 0;

			for (int i = currentTab - 1; i > 0; i--)
			{
				startIndex += client.getVarbitValue(TAB_VARBITS.get(i - 1));
			}

			int itemCount = client.getVarbitValue(TAB_VARBITS.get(currentTab - 1));
			return Arrays.copyOfRange(items, startIndex, startIndex + itemCount);
		}

		return items;
	}

	HeatmapItem getHeatmapItem(int id)
	{
		return heatmapCalculation.getHeatmapItems().get(id);
	}

	boolean isBankVisible()
	{
		Widget bank = client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
		return config.showTutorial() && bank != null && !bank.isHidden();
	}
}
