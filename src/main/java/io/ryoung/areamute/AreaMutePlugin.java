package io.ryoung.areamute;

import com.google.inject.Provides;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.chatfilter.ChatFilterPlugin;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Area Mute",
	description = "Mute player chat within regions of the game"
)
public class AreaMutePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AreaMuteConfig config;

	@Inject
	private OverlayManager overlayManager;

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

//	private final LinkedHashMap<String, WorldPoint> locCache = new LinkedHashMap<String, WorldPoint>()
//	{
//		private static final int MAX_ENTRIES = 200;
//
//		@Override
//		protected boolean removeEldestEntry(Map.Entry<String, WorldPoint> eldest)
//		{
//			return size() > MAX_ENTRIES;
//		}
//	};

//	@Inject
//	private AreaMuteOverlay overlay;

	@Override
	protected void startUp() throws Exception
	{
//		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
//		overlayManager.remove(overlay);
	}

	public void muteArea(int regionId) {
		log.debug("adding region {}", regionId);
		regions.add(regionId);
	}

	public boolean shouldFilter(Player actor)
	{
		if (actor == null)
		{
			return false;
		}

		if (actor == client.getLocalPlayer() && !config.filterSelf())
		{
			return false;
		}

		if (actor.isFriend() && !config.filterFriends())
		{
			return false;
		}

		if (actor.isClanMember() && !config.filterClanMates())
		{
			return false;
		}

		if (actor.isFriendsChatMember() && !config.filterFriendChat())
		{
			return false;
		}

		return regions.contains(actor.getWorldLocation().getRegionID());
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		switch(event.getCommand()) {
			case "amute":
				this.muteArea(client.getLocalPlayer().getWorldLocation().getRegionID());
		}
	}

	@Subscribe(priority = -999999)
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!(event.getActor() instanceof Player) || !shouldFilter((Player)event.getActor()))
		{
			return;
		}

		event.getActor().setOverheadText(" ");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int regionId = client.getLocalPlayer().getWorldLocation().getRegionID();

		if (!regions.contains(regionId))
		{
			return;
		}

//		for (Player p : client.getCachedPlayers()) {
//			if (p != null) {
//				locCache.put(p.getName(), p.getWorldLocation());
//			}
//		}
	}

	@Subscribe(priority = 999999) // run after ChatMessageManager
	public void onChatMessage(ChatMessage chatMessage)
	{
		String name = Text.toJagexName(Text.removeTags(chatMessage.getName()));
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

		if (shouldFilter(actor)) {
			chatCache.put(messageId, true);
		}

//		if (actor == null) {
//			WorldPoint p = locCache.get(name);
//			if (p != null && regions.contains(p.getRegionID())) {
//				chatCache.put(messageId, true);
//			}
//		}
	}

	@Subscribe
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

	@Provides
	AreaMuteConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AreaMuteConfig.class);
	}
}
