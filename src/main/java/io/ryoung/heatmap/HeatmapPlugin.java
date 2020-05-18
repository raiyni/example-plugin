package io.ryoung.heatmap;

import com.google.common.collect.ImmutableList;
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
import net.runelite.api.Varbits;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
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

	private static final int BANK_MENU_WIDGET_ID = (WidgetID.BANK_GROUP_ID << 16) + 107;

	@Inject
	private Client client;

	@Inject
	private HeatmapCalculation heatmapCalculation;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HeatmapItemOverlay heatmapItemOverlay;

	@Getter
	private HEATMAP_MODE heatmapMode = HEATMAP_MODE.NULL;

	@Override
	protected void startUp()
	{
		overlayManager.add(heatmapItemOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(heatmapItemOverlay);
		heatmapMode = HEATMAP_MODE.NULL;
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if ("setBankTitle".equals(event.getEventName()))
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
			|| event.getActionParam1() != BANK_MENU_WIDGET_ID)
		{
			return;
		}

		MenuEntry[] entries = client.getMenuEntries();
		entries = Arrays.copyOf(entries, entries.length + 2);

		MenuEntry geHeatmap = new MenuEntry();
		geHeatmap.setOption("Toggle GE Heatmap");
		geHeatmap.setTarget("");
		geHeatmap.setType(MenuAction.WIDGET_FOURTH_OPTION.getId() + 2000);
		geHeatmap.setIdentifier(event.getIdentifier());
		geHeatmap.setParam0(event.getActionParam0());
		geHeatmap.setParam1(event.getActionParam1());

		MenuEntry haHeatmap = new MenuEntry();
		haHeatmap.setOption("Toggle HA Heatmap");
		haHeatmap.setTarget("");
		haHeatmap.setType(MenuAction.WIDGET_FIFTH_OPTION.getId() + 2000);
		haHeatmap.setIdentifier(event.getIdentifier());
		haHeatmap.setParam0(event.getActionParam0());
		haHeatmap.setParam1(event.getActionParam1());

		entries[entries.length - 2] = haHeatmap;
		entries[entries.length - 1] = geHeatmap;

		client.setMenuEntries(entries);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if ((event.getMenuAction() != MenuAction.WIDGET_FOURTH_OPTION && event.getMenuAction() != MenuAction.WIDGET_FIFTH_OPTION)
			|| event.getWidgetId() != BANK_MENU_WIDGET_ID || !event.getMenuOption().startsWith("Toggle"))
		{
			return;
		}

		HEATMAP_MODE mode = event.getMenuOption().equals("Toggle GE Heatmap") ? HEATMAP_MODE.GE : HEATMAP_MODE.HA;
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
}
