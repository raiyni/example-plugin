package io.ryoung.heatmap;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
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

	private static final List<Varbits> TAB_VARBITS = ImmutableList.of(
		Varbits.BANK_TAB_ONE_COUNT,
		Varbits.BANK_TAB_TWO_COUNT,
		Varbits.BANK_TAB_THREE_COUNT,
		Varbits.BANK_TAB_FOUR_COUNT,
		Varbits.BANK_TAB_FIVE_COUNT,
		Varbits.BANK_TAB_SIX_COUNT,
		Varbits.BANK_TAB_SEVEN_COUNT,
		Varbits.BANK_TAB_EIGHT_COUNT,
		Varbits.BANK_TAB_NINE_COUNT
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
			|| (event.getActionParam1() >> 16) != WidgetID.BANK_GROUP_ID)
		{
			return;
		}

		client.createMenuEntry(-1)
			.setOption("Toggle GE Heatmap")
			.setTarget("")
			.setType(MenuAction.RUNELITE)
			.onClick(this::onClick)
			.setDeprioritized(true);

		client.createMenuEntry(-1)
			.setOption("Toggle HA Heatmap")
			.setTarget("")
			.setType(MenuAction.RUNELITE)
			.onClick(this::onClick)
			.setDeprioritized(true);


		if (config.showTutorial())
		{
			client.createMenuEntry(-1)
				.setOption("Disable tutorial")
				.setTarget("")
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
		log.info("{}", e.getOption());
		HEATMAP_MODE mode = e.getOption().equals("Toggle GE Heatmap") ? HEATMAP_MODE.GE : HEATMAP_MODE.HA;
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
		int currentTab = client.getVar(Varbits.CURRENT_BANK_TAB);

		if (currentTab > 0)
		{
			int startIndex = 0;

			for (int i = currentTab - 1; i > 0; i--)
			{
				startIndex += client.getVar(TAB_VARBITS.get(i - 1));
			}

			int itemCount = client.getVar(TAB_VARBITS.get(currentTab - 1));
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
		Widget bank = client.getWidget(WidgetInfo.BANK_CONTAINER);
		return config.showTutorial() && bank != null && !bank.isHidden();
	}
}
