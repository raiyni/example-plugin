package io.ryoung.areamute;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Area Mute",
	description = "Mute player chat within regions of the game"
)
public class AreaMutePlugin extends Plugin
{
	public static final int REGION_SIZE = 1 << 6;
	private static final Type TOKEN = new TypeToken<HashSet<Integer>>()
	{
	}.getType();

	@Inject
	private Client client;

	@Inject
	private AreaMuteConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;

	@Getter
	private final Set<Integer> regions = new HashSet<>();

	private final LinkedHashMap<Integer, Boolean> chatCache = new LinkedHashMap<Integer, Boolean>()
	{
		private static final int MAX_ENTRIES = 2000;

		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> eldest)
		{
			return size() > MAX_ENTRIES;
		}
	};

	@Inject
	private AreaMuteOverlay overlay;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		this.loadRegions();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		chatCache.clear();
		regions.clear();
	}

	public void loadRegions()
	{
		this.regions.clear();
		this.regions.addAll(gson.fromJson(config.regions(), TOKEN));
	}

	public void saveRegions()
	{
		config.regions(gson.toJson(this.regions));
	}

	public void muteArea(int regionId)
	{
		log.debug("adding region {}", regionId);
		regions.add(regionId);
		saveRegions();
	}

	public void unmuteArea(int regionId)
	{
		log.debug("unmuting region {}", regionId);
		regions.remove(regionId);
		saveRegions();
	}

	public boolean shouldFilter(Player actor, String name)
	{
		int region = client.getLocalPlayer().getWorldLocation().getRegionID();
		if (actor != null)
		{
			region = actor.getWorldLocation().getRegionID();
		}

		if (actor == client.getLocalPlayer() && !config.filterSelf())
		{
			return false;
		}

		if (!config.filterFriends() && isFriendsChatMember(name))
		{
			return false;
		}

		if (!config.filterClanMates() && isClanChatMember(name))
		{
			return false;
		}

		if (!config.filterFriendChat() && isFriendsChatMember(name))
		{
			return false;
		}

		return regions.contains(region);
	}

	private boolean isFriendsChatMember(String name)
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		return friendsChatManager != null && friendsChatManager.findByName(name) != null;
	}

	private boolean isClanChatMember(String name)
	{
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel != null && clanChannel.findMember(name) != null)
		{
			return true;
		}

		clanChannel = client.getGuestClanChannel();
		if (clanChannel != null && clanChannel.findMember(name) != null)
		{
			return true;
		}

		return false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("areamute".equals(event.getGroup()) && "regions".equals(event.getKey()))
		{
			this.loadRegions();
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		switch (event.getCommand())
		{
			case "mutearea":
				this.muteArea(client.getLocalPlayer().getWorldLocation().getRegionID());
				break;
			case "unmutearea":
				this.unmuteArea(client.getLocalPlayer().getWorldLocation().getRegionID());
				break;
		}
	}

	@Subscribe(priority = -999999)
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!(event.getActor() instanceof Player) || !shouldFilter((Player) event.getActor(), event.getActor().getName()))
		{
			return;
		}

		event.getActor().setOverheadText(" ");
	}

	@Subscribe(priority = 999999)
	public void onChatMessage(ChatMessage chatMessage)
	{
		String name = Text.removeTags(chatMessage.getName());
		int messageId = chatMessage.getMessageNode().getId();

		Player actor = null;

		for (Player p : client.getPlayers())
		{
			if (name.equalsIgnoreCase(p.getName()))
			{
				actor = p;
				break;
			}
		}

		if (actor == null && name.equalsIgnoreCase(client.getLocalPlayer().getName()))
		{
			actor = client.getLocalPlayer();
		}

		if (shouldFilter(actor, name))
		{
			chatCache.put(messageId, true);
		}
	}

	@Subscribe(priority = -1)
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatFilterCheck".equals(event.getEventName()))
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		String[] stringStack = client.getStringStack();
		int stringStackSize = client.getStringStackSize();

		final int messageType = intStack[intStackSize - 2];
		final int messageId = intStack[intStackSize - 1];

		ChatMessageType chatMessageType = ChatMessageType.of(messageType);

		switch (chatMessageType)
		{
			case PUBLICCHAT:
			case AUTOTYPER:
				if (chatCache.containsKey(messageId))
				{
					intStack[intStackSize - 3] = 0;
					stringStack[stringStackSize - 1] = null;
				}
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
		if (map == null)
		{
			return;
		}

		Rectangle worldMapRect = map.getBounds();
		Point p = client.getMouseCanvasPosition();
		if (worldMapRect.contains(p.getX(), p.getY()))
		{
			int regionId = getRegionIdFromCursor();
			boolean muted = regions.contains(regionId);

			String option = muted ? "Unmute" : "Mute";
			client.createMenuEntry(-1)
				.setOption(option)
				.setTarget("Region")
				.onClick(e -> {
					if (muted)
					{
						unmuteArea(regionId);
					}
					else
					{
						muteArea(regionId);
					}
				});
		}
	}

	public int getRegionIdFromCursor()
	{
		Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
		if (map == null)
		{
			return 0;
		}

		WorldMap worldMap = client.getWorldMap();
		float pixelsPerTile = worldMap.getWorldMapZoom();

		Rectangle worldMapRect = map.getBounds();

		int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
		int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

		Point worldMapPosition = worldMap.getWorldMapPosition();

		Point mp = client.getMouseCanvasPosition();
		if (worldMapRect.contains(mp.getX(), mp.getY()))
		{
			int rXO = (mp.getX() - (int) worldMapRect.getX()) / (int) pixelsPerTile;
			int x = rXO + worldMapPosition.getX() - (widthInTiles / 2);

			int rYO = (-mp.getY() + (int) worldMapRect.getY() + worldMapRect.height) / (int) pixelsPerTile;
			int y = rYO - heightInTiles / 2 + worldMapPosition.getY();

			return ((x >>> 6) << 8) | (y >> 6);
		}

		return 0;
	}

	@Provides
	AreaMuteConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AreaMuteConfig.class);
	}
}
