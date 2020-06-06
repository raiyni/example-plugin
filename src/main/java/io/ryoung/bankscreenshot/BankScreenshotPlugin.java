package io.ryoung.bankscreenshot;

import com.google.inject.Provides;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.ImageUploadStyle;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Bank Screenshot"
)
public class BankScreenshotPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BankScreenshotConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ImageCapture imageCapture;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ScheduledExecutorService executor;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> screenshot());
		}
	};

	private Widget button = null;

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(hotkeyListener);
		clientThread.invokeLater(this::createButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		keyManager.unregisterKeyListener(hotkeyListener);
		clientThread.invoke(this::hideButton);
	}


	@Provides
	BankScreenshotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankScreenshotConfig.class);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != WidgetID.BANK_GROUP_ID)
		{
			return;
		}

		createButton();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("bankscreenshot".equals(event.getGroup()) && "button".equals(event.getKey()))
		{
			if (config.button())
			{
				clientThread.invoke(this::createButton);
			}
			else
			{
				clientThread.invoke(this::hideButton);
			}
		}
	}

	private void hideButton()
	{
		if (button == null)
		{
			return;
		}

		button.setHidden(true);
		button = null;
	}

	private void createButton()
	{
		if (!config.button())
		{
			return;
		}

		Widget parent = client.getWidget(WidgetInfo.BANK_CONTENT_CONTAINER);
		if (parent == null)
		{
			return;
		}

		hideButton();

		button = parent.createChild(-1, WidgetType.GRAPHIC);
		button.setOriginalHeight(20);
		button.setOriginalWidth(20);
		button.setOriginalX(434);
		button.setOriginalY(48);
		button.setSpriteId(573);
		button.setAction(0, "Screenshot");
		button.setOnOpListener((JavaScriptCallback) (e) -> clientThread.invokeLater(this::screenshot));
		button.setHasListener(true);
		button.revalidate();

		button.setOnMouseOverListener((JavaScriptCallback) (e) -> button.setSpriteId(570));
		button.setOnMouseLeaveListener((JavaScriptCallback) (e) -> button.setSpriteId(573));
	}

	private void screenshot()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Widget container = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		if (container == null || container.isHidden())
		{
			return;
		}

		int height = container.getScrollHeight();
		if (height == 0)
		{
			height = 32;

			int y = 0;
			for (Widget item : container.getDynamicChildren())
			{
				if (item.isHidden())
				{
					continue;
				}

				if (item.getRelativeY() > y)
				{
					y = item.getRelativeY();
					log.debug(y + "");
					height = y + 32;
				}
			}
		}

		int padding = 10;

		if (config.title())
		{
			height += 30;
			padding += 30;
		}

		BufferedImage screenshot = new BufferedImage(container.getWidth(), height + 20, BufferedImage.TYPE_INT_ARGB);

		BufferedImage background = spriteManager.getSprite(297, 0);

		Graphics graphics = screenshot.getGraphics();

		int x = screenshot.getWidth() / background.getWidth() + 1;
		int y = screenshot.getHeight() / background.getHeight() + 1;

		for (int i = 0; i < x; i++)
		{
			for (int z = 0; z < y; z++)
			{
				graphics.drawImage(background, i * background.getWidth(), z * background.getHeight(), null);
			}
		}

		if (config.title())
		{
			Widget titlebar = client.getWidget(WidgetInfo.BANK_TITLE_BAR);
			String text = Text.removeTags(titlebar.getText());
			Font font = FontManager.getRunescapeBoldFont();
			graphics.setFont(font);
			int width = graphics.getFontMetrics().stringWidth(text);
			graphics.setColor(JagexColors.DARK_ORANGE_INTERFACE_TEXT);
			graphics.drawString(text, (screenshot.getWidth() - width) / 2, 20);
		}


		for (Widget item : container.getDynamicChildren())
		{
			if (item.isHidden())
			{
				continue;
			}

			BufferedImage image;
			if (item.getItemId() > 0)
			{
				image = itemManager.getImage(item.getItemId(), item.getItemQuantity(), item.getItemQuantityMode() == ItemQuantityMode.ALWAYS);
				graphics.drawImage(image, item.getRelativeX(), item.getRelativeY() + padding, null);
			}
			else
			{
				image = spriteManager.getSprite(item.getSpriteId(), 0);
				graphics.drawImage(image, item.getRelativeX(), item.getRelativeY() + padding, item.getWidth(), item.getHeight(), null);
			}
		}

		imageCapture.takeScreenshot(screenshot, "bankscreenshot", "bank", true, ImageUploadStyle.NEITHER);
	}
}
