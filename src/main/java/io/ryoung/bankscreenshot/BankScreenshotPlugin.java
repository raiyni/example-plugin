package io.ryoung.bankscreenshot;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
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
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.ImageUploadStyle;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

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

		client.getWidgetSpriteCache().reset();


		Widget container = client.getWidget(WidgetInfo.BANK_CONTAINER);
		Widget itemContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		if (container == null || container.isHidden() || itemContainer == null || itemContainer.isHidden())
		{
			return;
		}

		int height = 32;

		int y = 0;
		for (Widget item : itemContainer.getDynamicChildren())
		{
			if (item.isHidden())
			{
				continue;
			}

			if (item.getRelativeY() > y && item.getItemId() != 6512)
			{
				y = item.getRelativeY();
				height = y + 32;
			}
		}

		int width = itemContainer.getWidth();

		if (config.info() == BankScreenshotConfig.DisplayMode.FRAME)
		{
			width = container.getWidth();
			height += 120;

			height = Math.max(height, 335);
		}
		else if (config.info() == BankScreenshotConfig.DisplayMode.TITLE)
		{
			height += 45;
		}
		else
		{
			height += 30;
		}

		BufferedImage screenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = screenshot.getGraphics();

		BufferedImage background = spriteManager.getSprite(297, 0);
		int x = screenshot.getWidth() / background.getWidth() + 1;
		y = screenshot.getHeight() / background.getHeight() + 1;
		for (int i = 0; i < x; i++)
		{
			for (int z = 0; z < y; z++)
			{
				graphics.drawImage(background, i * background.getWidth(), z * background.getHeight(), null);
			}
		}

		Widget content = client.getWidget(WidgetInfo.BANK_CONTENT_CONTAINER);
		Graphics contentGraphics;

		int itemsOffset = 0;
		if (config.info() == BankScreenshotConfig.DisplayMode.FRAME)
		{
			drawFrame(graphics, width, height);

			Widget titleBar = client.getWidget(WidgetInfo.BANK_TITLE_BAR);

			Graphics titleGraphics = graphics.create(content.getOriginalX(), 0, titleBar.getWidth(), titleBar.getHeight());
			titleGraphics.setClip(0, 0, titleBar.getWidth(), titleBar.getHeight());
			drawWidget(titleGraphics, titleBar, 0, 0, 0, 0, false);

			BufferedImage closeBtn = spriteManager.getSprite(535, 0);
			graphics.drawImage(closeBtn, width - closeBtn.getWidth() - 7, 7, null);
			titleGraphics.dispose();

			contentGraphics = graphics.create(content.getRelativeX(), content.getRelativeY(), content.getWidth(), height - titleBar.getHeight() - 16);
			contentGraphics.setClip(0, 0, content.getWidth(), height - titleBar.getHeight() - 15);

			int bottomBarY = 0;
			int scrollbarY = 0;

			for (int i = WidgetInfo.BANK_ITEM_COUNT_TOP.getChildId(); i < WidgetInfo.BANK_ITEM_COUNT_TOP.getChildId() + 3; i++)
			{
				Widget child = client.getWidget(12, i);
				drawChildren(graphics, child, child.getRelativeX(), child.getRelativeY());
			}

			Widget settingsBtn = client.getWidget(WidgetInfo.BANK_SETTINGS_BUTTON);
			drawChildren(graphics, settingsBtn, settingsBtn.getRelativeX(), settingsBtn.getRelativeY(), settingsBtn.getHeight(), false);

			Widget equipBtn = client.getWidget(WidgetInfo.BANK_EQUIPMENT_BUTTON);
			drawChildren(graphics, equipBtn, equipBtn.getRelativeX(), equipBtn.getRelativeY(), equipBtn.getHeight(), false);

			Widget tutorialBtn = client.getWidget(WidgetInfo.BANK_TUTORIAL_BUTTON);
			drawChildren(graphics, tutorialBtn, tutorialBtn.getRelativeX(), tutorialBtn.getRelativeY(), tutorialBtn.getHeight(), false);

			for (Widget child : content.getStaticChildren())
			{
				if (child.getId() == WidgetInfo.BANK_ITEM_CONTAINER.getId())
				{
					itemsOffset = child.getRelativeY();
				}
				else if (child.getId() == WidgetInfo.BANK_TAB_CONTAINER.getId())
				{
					drawChildren(contentGraphics, child, child.getRelativeX(), child.getRelativeY());
				}
				else if (child.getId() == WidgetInfo.BANK_INCINERATOR.getId())
				{
					// do nothing
				}
				else if (child.getId() == WidgetInfo.BANK_SCROLLBAR.getId())
				{
					scrollbarY = child.getRelativeY();
				}
				else if (!child.isHidden())
				{
					bottomBarY = contentGraphics.getClipBounds().height - child.getHeight();
					drawChildren(contentGraphics, child, child.getRelativeX(), contentGraphics.getClipBounds().height - child.getHeight());
				}
			}

			drawScrollbar(contentGraphics, content.getWidth() - 17, scrollbarY - 1, 16, bottomBarY - scrollbarY);
		}
		else if (config.info() == BankScreenshotConfig.DisplayMode.TITLE)
		{
			Widget titleBar = client.getWidget(WidgetInfo.BANK_TITLE_BAR);
			Graphics titleGraphics = graphics.create(0, 0, content.getWidth(), titleBar.getHeight());
			drawWidget(titleGraphics, titleBar, 0, 0, content.getWidth(), 1, false);
			titleGraphics.dispose();
			itemsOffset = 0;
			contentGraphics = graphics.create(0, titleBar.getHeight() + 16, content.getWidth(), height - titleBar.getHeight() - 16);
		}
		else
		{
			itemsOffset = 0;
			contentGraphics = graphics.create(0, 16, content.getWidth(), height - 16);
		}

		Widget items = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		drawChildren(contentGraphics, items, items.getRelativeX(), itemsOffset, contentGraphics.getClipBounds().height, true);

		contentGraphics.dispose();
		imageCapture.takeScreenshot(screenshot, "bankscreenshot", "bank", true, ImageUploadStyle.NEITHER);
	}

	private void drawScrollbar(Graphics graphics, int x, int y, int width, int height)
	{
		Graphics layer = graphics.create(x, y, width, height);
		layer.setClip(0, 0, width, height);

		BufferedImage sprite = spriteManager.getSprite(792, 0);
		layer.drawImage(sprite, 0, 16, width, height - 32, null);

		sprite = spriteManager.getSprite(790, 0);
		layer.drawImage(sprite, 0, 16, width, height - 32, null);

		sprite = spriteManager.getSprite(789, 0);
		layer.drawImage(sprite, 0, 16, width, 5, null);

		sprite = spriteManager.getSprite(791, 0);
		layer.drawImage(sprite, 0, height - 21, width, 5, null);

		sprite = spriteManager.getSprite(773, 0);
		layer.drawImage(sprite, 0, 0, 16, 16, null);

		sprite = spriteManager.getSprite(788, 0);
		layer.drawImage(sprite, 0, height - 16, 16, 16, null);

		layer.dispose();
	}

	private void drawFrame(Graphics graphics, int width, int height)
	{
		BufferedImage sprite = spriteManager.getSprite(310, 0);
		graphics.drawImage(sprite, 0, 0, null);

		sprite = spriteManager.getSprite(314, 0);
		for (int x = 6; x < width - 6; x += sprite.getWidth())
		{
			graphics.drawImage(sprite, x, 0, null);
			graphics.drawImage(sprite, x, 29, null);
		}

		sprite = spriteManager.getSprite(173, 0);
		for (int x = 6; x < width - 6; x += sprite.getWidth())
		{
			graphics.drawImage(sprite, x, height - sprite.getHeight(), null);
		}

		sprite = spriteManager.getSprite(172, 0);
		for (int y = 6; y < height - 6; y += sprite.getHeight())
		{
			graphics.drawImage(sprite, 0, y, null);
		}

		sprite = spriteManager.getSprite(315, 0);
		for (int y = 6; y < height - 6; y += sprite.getHeight())
		{
			graphics.drawImage(sprite, width - sprite.getWidth(), y, null);
		}

		sprite = spriteManager.getSprite(311, 0);
		graphics.drawImage(sprite, width - sprite.getWidth(), 0, null);

		sprite = spriteManager.getSprite(312, 0);
		graphics.drawImage(sprite, 0, height - sprite.getHeight(), null);

		sprite = spriteManager.getSprite(313, 0);
		graphics.drawImage(sprite, width - sprite.getWidth(), height - sprite.getHeight(), null);
	}


	private void drawChildren(Graphics graphics, Widget child, int x, int y, int overHeight, boolean shouldTile)
	{
		if (child == null || child.isHidden())
		{
			return;
		}

		Graphics layer = graphics.create(x, y, child.getWidth(), overHeight > 0 ? overHeight : child.getHeight());
		layer.setClip(0, 0, child.getWidth(), overHeight > 0 ? overHeight : child.getHeight());
		drawWidget(graphics, child, child.getRelativeX(), child.getRelativeY(), 0, 0, shouldTile);

		if (child.getStaticChildren() != null)
		{
			for (Widget children : child.getStaticChildren())
			{
				drawChildren(layer, children, children.getRelativeX(), children.getRelativeY(), children.getHeight(), shouldTile);
			}
		}

		if (child.getDynamicChildren() != null)
		{
			Widget[] children = child.getDynamicChildren();

			for (int i = 0; i < children.length; i++)
			{
				Widget child2 = children[i];
				drawWidget(layer, child2, child2.getRelativeX(), child2.getRelativeY(), 0, 0, shouldTile);
			}
		}

		layer.dispose();
	}

	private void drawChildren(Graphics graphics, Widget child, int x, int y)
	{
		drawChildren(graphics, child, x, y, child.getHeight(), true);
	}

	private void drawWidget(Graphics graphics, Widget child, int x, int y, int overWidth, int overX, boolean shouldTile)
	{
		if (child == null || child.isHidden() || child.getType() == 0)
		{
			return;
		}

		int width = overWidth > 0 ? overWidth : child.getWidth();
		int height = child.getHeight();
		if (child.getSpriteId() > 0)
		{
			BufferedImage childImage = spriteManager.getSprite(child.getSpriteId(), 0);

			if (child.getSpriteTiling() && shouldTile)
			{

				int sw = x, ew = width + x,
					sh = y, eh = height + y,
					iw = childImage.getWidth(), ih = childImage.getHeight();

				Rectangle clips = graphics.getClipBounds();
				graphics.setClip(x, y, child.getWidth(), child.getHeight());

				for (int dx = sw; dx < ew; dx += iw)
				{
					for (int dy = sh; dy < eh; dy += ih)
					{
						drawAt(graphics, childImage, dx, dy);
					}
				}

				graphics.setClip(clips);
			}
			else
			{
				if (width == childImage.getWidth() && height == childImage.getHeight())
				{
					drawAt(graphics, childImage, x, y);
				}
				else
				{
					drawScaled(graphics, childImage, x, y, width, height);
				}
			}
		}
		else if (child.getItemId() > 0)
		{
			BufferedImage image;
			ItemComposition composition = itemManager.getItemComposition(child.getItemId());
			if (child.getId() == WidgetInfo.BANK_TAB_CONTAINER.getId())
			{
				image = itemManager.getImage(itemManager.canonicalize(child.getItemId()), 1, false);
			}
			else if (composition.getPlaceholderTemplateId() > 0)
			{
				image = ImageUtil.alphaOffset(itemManager.getImage(child.getItemId(), 0, true), 0.5f);
			}
			else
			{
				boolean stackable = child.getItemQuantity() > 1 || child.getItemQuantityMode() == ItemQuantityMode.ALWAYS;
				image = itemManager.getImage(child.getItemId(), child.getItemQuantity(), stackable);
			}

			graphics.drawImage(image, child.getRelativeX(), child.getRelativeY(), null);
		}
		else if (child.getType() == WidgetType.TEXT)
		{
			String text = Text.removeTags(child.getText());
			Font font = FontManager.getRunescapeFont();

			Graphics textLayer = graphics.create(overX > 0 ? overX : child.getRelativeX(), child.getRelativeY(), width, height);

			if (child.getFontId() == FontID.PLAIN_11)
			{
				font = FontManager.getRunescapeSmallFont();
			}
			else if (child.getFontId() == FontID.BARBARIAN || child.getFontId() == FontID.QUILL_MEDIUM)
			{
				font = new Font("Times New Roman", Font.PLAIN, 20);
			}
			else if (child.getFontId() == FontID.BOLD_12)
			{
				font = FontManager.getRunescapeBoldFont();
			}

			textLayer.setFont(font);

			int xPos = 0;
			int yPos = 0;

			int textWidth = textLayer.getFontMetrics().stringWidth(child.getText());

			if (child.getXTextAlignment() == 1)
			{
				xPos = (width - textWidth) / 2 + 1;
			}

			if (child.getYTextAlignment() == 0)
			{
				yPos = font.getSize() - 3;
			}
			else if (child.getYTextAlignment() == 1)
			{
				yPos = (height + font.getSize()) / 2 - 1;
			}
			else if (child.getYTextAlignment() == 2)
			{
				yPos = height;
			}

			if (child.getTextShadowed())
			{
				textLayer.setColor(Color.BLACK);
				textLayer.drawString(text, xPos, yPos);
				xPos -= 1;
				yPos -= 1;
			}

			textLayer.setColor(new Color(child.getTextColor()));
			textLayer.drawString(text, xPos, yPos);
			textLayer.dispose();
		}
		else if (child.getType() == WidgetType.LINE)
		{
			graphics.setColor(new Color(child.getTextColor()));
			graphics.drawLine(child.getRelativeX(), child.getRelativeY(), child.getRelativeX() + child.getWidth(), child.getRelativeY());
		}
	}

	private void drawScaled(Graphics graphics, BufferedImage image, int x, int y, int width, int height)
	{
		image = ImageUtil.resizeCanvas(image, width, height);
		graphics.drawImage(image, x, y, null);
	}

	private void drawAt(Graphics graphics, BufferedImage image, int x, int y)
	{
		graphics.drawImage(image, x, y, null);
	}
}
