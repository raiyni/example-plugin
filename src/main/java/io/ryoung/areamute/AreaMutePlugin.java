package io.ryoung.areamute;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Example"
)
public class AreaMutePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AreaMuteConfig config;

	@Inject
	private OverlayManager overlayManager;

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

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{

	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
//		if (!(event.getActor() instanceof Player) || !shouldFilterPlayerMessage(event.getActor().getName()))
//		{
//			return;
//		}
//
//		String message = censorMessage(event.getActor().getName(), event.getOverheadText());
//
//		if (message == null)
//		{
//			message = " ";
//		}
//
//		event.getActor().setOverheadText(message);
	}

	@Subscribe(priority = -2) // run after ChatMessageManager
	public void onChatMessage(ChatMessage chatMessage)
	{
//		if (COLLAPSIBLE_MESSAGETYPES.contains(chatMessage.getType()))
//		{
//			final MessageNode messageNode = chatMessage.getMessageNode();
//			// remove and re-insert into map to move to end of list
//			final String key = messageNode.getName() + ":" + messageNode.getValue();
//			ChatFilterPlugin.Duplicate duplicate = duplicateChatCache.remove(key);
//			if (duplicate == null)
//			{
//				duplicate = new ChatFilterPlugin.Duplicate();
//			}
//
//			duplicate.count++;
//			duplicate.messageId = messageNode.getId();
//			duplicateChatCache.put(key, duplicate);
//		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
//		if (!"chatFilterCheck".equals(event.getEventName()))
//		{
//			return;
//		}
//
//		int[] intStack = client.getIntStack();
//		int intStackSize = client.getIntStackSize();
//		String[] stringStack = client.getStringStack();
//		int stringStackSize = client.getStringStackSize();
//
//		final int messageType = intStack[intStackSize - 2];
//		final int messageId = intStack[intStackSize - 1];
//		String message = stringStack[stringStackSize - 1];
//
//		ChatMessageType chatMessageType = ChatMessageType.of(messageType);
//		final MessageNode messageNode = client.getMessages().get(messageId);
//		final String name = messageNode.getName();
//		int duplicateCount = 0;
//		boolean blockMessage = false;
//
//		// Only filter public chat and private messages
//		switch (chatMessageType)
//		{
//			case PUBLICCHAT:
//			case MODCHAT:
//			case AUTOTYPER:
//			case PRIVATECHAT:
//			case MODPRIVATECHAT:
//			case FRIENDSCHAT:
//			case CLAN_CHAT:
//			case CLAN_GUEST_CHAT:
//			case CLAN_GIM_CHAT:
//				if (shouldFilterPlayerMessage(Text.removeTags(name)))
//				{
//					message = censorMessage(name, message);
//					blockMessage = message == null;
//				}
//				break;
//			case GAMEMESSAGE:
//			case ENGINE:
//			case ITEM_EXAMINE:
//			case NPC_EXAMINE:
//			case OBJECT_EXAMINE:
//			case SPAM:
//			case CLAN_MESSAGE:
//			case CLAN_GUEST_MESSAGE:
//			case CLAN_GIM_MESSAGE:
//				if (config.filterGameChat())
//				{
//					message = censorMessage(null, message);
//					blockMessage = message == null;
//				}
//				break;
//		}
//
//		boolean shouldCollapse = chatMessageType == PUBLICCHAT || chatMessageType == MODCHAT
//			? config.collapsePlayerChat()
//			: COLLAPSIBLE_MESSAGETYPES.contains(chatMessageType) && config.collapseGameChat();
//		if (!blockMessage && shouldCollapse)
//		{
//			ChatFilterPlugin.Duplicate duplicateCacheEntry = duplicateChatCache.get(name + ":" + message);
//			// If messageId is -1 then this is a replayed message, which we can't easily collapse since we don't know
//			// the most recent message. This is only for public chat since it is the only thing both replayed and also
//			// collapsed. Just allow uncollapsed playback.
//			if (duplicateCacheEntry != null && duplicateCacheEntry.messageId != -1)
//			{
//				blockMessage = duplicateCacheEntry.messageId != messageId ||
//					((chatMessageType == PUBLICCHAT || chatMessageType == MODCHAT) &&
//						config.maxRepeatedPublicChats() > 0 && duplicateCacheEntry.count > config.maxRepeatedPublicChats());
//				duplicateCount = duplicateCacheEntry.count;
//			}
//		}
//
//		if (blockMessage)
//		{
//			// Block the message
//			intStack[intStackSize - 3] = 0;
//		}
//		else
//		{
//			// Replace the message
//			if (duplicateCount > 1)
//			{
//				message += " (" + duplicateCount + ")";
//			}
//
//			stringStack[stringStackSize - 1] = message;
//		}
	}

	@Provides
	AreaMuteConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AreaMuteConfig.class);
	}
}
