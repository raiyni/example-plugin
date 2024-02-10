package io.ryoung.bankscreenshot;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.SpritePixels;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.WidgetUtil;
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

	private Map<Integer, Rectangle> overrideBounds = new HashMap<>();

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
		if (event.getGroupId() != InterfaceID.BANK)
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

		Widget parent = client.getWidget(ComponentID.BANK_CONTENT_CONTAINER);
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

		overrideBounds.clear();
		client.getWidgetSpriteCache().reset();

		Widget container = client.getWidget(ComponentID.BANK_CONTAINER);
		Widget itemContainer = client.getWidget(ComponentID.BANK_ITEM_CONTAINER);
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

		BufferedImage background = getSprite(297);
		int x = screenshot.getWidth() / background.getWidth() + 1;
		y = screenshot.getHeight() / background.getHeight() + 1;
		for (int i = 0; i < x; i++)
		{
			for (int z = 0; z < y; z++)
			{
				graphics.drawImage(background, i * background.getWidth(), z * background.getHeight(), null);
			}
		}

		Widget content = client.getWidget(ComponentID.BANK_CONTENT_CONTAINER);
		Graphics contentGraphics;

		int itemsOffset = 0;
		if (config.info() == BankScreenshotConfig.DisplayMode.FRAME)
		{
			drawFrame(graphics, width, height);

			Widget titleBar = client.getWidget(ComponentID.BANK_TITLE_BAR);

			Graphics titleGraphics = graphics.create(content.getOriginalX(), 0, titleBar.getWidth(), titleBar.getHeight());
			titleGraphics.setClip(0, 0, titleBar.getWidth(), titleBar.getHeight());
			drawWidget(titleGraphics, titleBar, 0, 0);
			titleGraphics.dispose();

			contentGraphics = graphics.create(content.getRelativeX(), content.getRelativeY(), content.getWidth(), height - titleBar.getHeight() - 16);
			contentGraphics.setClip(0, 0, content.getWidth(), height - titleBar.getHeight() - 15);

			int bottomBarY = 0;
			int scrollbarY = 0;

			int itemCountId = WidgetUtil.componentToId(ComponentID.BANK_ITEM_COUNT_TOP);
			for (int i = itemCountId; i < itemCountId + 3; i++)
			{
				Widget child = client.getWidget(12, i);
				drawChildren(graphics, child, child.getRelativeX(), child.getRelativeY());
			}

			Widget settingsBtn = client.getWidget(ComponentID.BANK_SETTINGS_BUTTON);
			drawChildren(graphics, settingsBtn, settingsBtn.getRelativeX(), settingsBtn.getRelativeY());

			Widget equipBtn = client.getWidget(ComponentID.BANK_EQUIPMENT_BUTTON);
			drawChildren(graphics, equipBtn, equipBtn.getRelativeX(), equipBtn.getRelativeY());

			Widget tutorialBtn = client.getWidget(ComponentID.BANK_TUTORIAL_BUTTON);
			drawChildren(graphics, tutorialBtn, tutorialBtn.getRelativeX(), tutorialBtn.getRelativeY());

			for (Widget child : content.getStaticChildren())
			{
				if (child.getId() == ComponentID.BANK_ITEM_CONTAINER)
				{
					itemsOffset = child.getRelativeY();
				}
				else if (child.getId() == ComponentID.BANK_TAB_CONTAINER)
				{
					drawChildren(contentGraphics, child, child.getRelativeX(), child.getRelativeY());
				}
				else if (child.getId() == ComponentID.BANK_INCINERATOR)
				{
					// do nothing
				}
				else if (child.getId() == ComponentID.BANK_SCROLLBAR)
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
			Widget titleBar = client.getWidget(ComponentID.BANK_TITLE_BAR);
			Graphics titleGraphics = graphics.create(0, 0, content.getWidth(), titleBar.getHeight());
			overrideBounds.put(titleBar.getId() | titleBar.getParentId(), new Rectangle(0, 5, content.getWidth(), titleBar.getHeight()));
			drawWidget(titleGraphics, titleBar, 0, 5);
			titleGraphics.dispose();
			itemsOffset = 0;
			contentGraphics = graphics.create(0, titleBar.getHeight() + 16, content.getWidth(), height - titleBar.getHeight() - 16);
		}
		else
		{
			itemsOffset = 0;
			contentGraphics = graphics.create(0, 16, content.getWidth(), height - 16);
		}


		Widget items = client.getWidget(ComponentID.BANK_ITEM_CONTAINER);
		overrideBounds.put(items.getId() | items.getParentId(), new Rectangle(items.getRelativeX(), itemsOffset, items.getWidth(), height));
		drawChildren(contentGraphics, items, items.getRelativeX(), itemsOffset);

		contentGraphics.dispose();
		imageCapture.saveScreenshot(screenshot, "bankscreenshot", "bank", true, true);
	}

	private void drawFrame(Graphics graphics, int width, int height)
	{
		Widget frameParent = client.getWidget(12, 2);
		Widget[] pieces = frameParent.getDynamicChildren();

		Map<Integer, Rectangle> overrides = new HashMap<Integer, Rectangle>()
		{{
			put(310, new Rectangle(0, 0, 25, 30));
			put(311, new Rectangle(width - 25, 0, 25, 30));
			put(312, new Rectangle(0, height - 30, 25, 30));
			put(313, new Rectangle(width - 25, height - 30, 25, 30));
			put(314, new Rectangle(25, -15, width - 50, 36));
			put(2546, new Rectangle(6, 14, width - 12, 26));
			put(173, new Rectangle(25, height - 21, width - 50, 36));
			put(172, new Rectangle(-15, 30, 36, height - 60));
			put(315, new Rectangle(width - 21, 30, 36, height - 60));
			put(535, new Rectangle(width - 3 - 26, 6, 26, 23));
		}};


		for (Widget piece : pieces)
		{
			Rectangle bounds = overrides.get(piece.getSpriteId());
			if (bounds == null)
			{
				continue;
			}

			SpritePixels sp = getPixels(piece.getSpriteId());
			Rectangle clips = graphics.getClipBounds();
			graphics.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
			for (int x = bounds.x; x < bounds.width + bounds.x; x += sp.getMaxWidth())
			{
				for (int y = bounds.y; y < bounds.height + bounds.y; y += sp.getMaxHeight())
				{
					graphics.drawImage(sp.toBufferedImage(), x + sp.getOffsetX(), y + sp.getOffsetY(), null);
				}
			}
			graphics.setClip(clips);
		}
	}

	private void drawScrollbar(Graphics graphics, int x, int y, int width, int height)
	{
		Graphics layer = graphics.create(x, y, width, height);
		layer.setClip(0, 0, width, height);

		BufferedImage sprite = getSprite(792);
		layer.drawImage(sprite, 0, 16, width, height - 32, null);

		sprite = getSprite(790);
		layer.drawImage(sprite, 0, 16, width, height - 32, null);

		sprite = getSprite(789);
		layer.drawImage(sprite, 0, 16, width, 5, null);

		sprite = getSprite(791);
		layer.drawImage(sprite, 0, height - 21, width, 5, null);

		sprite = getSprite(773);
		layer.drawImage(sprite, 0, 0, 16, 16, null);

		sprite = getSprite(788);
		layer.drawImage(sprite, 0, height - 16, 16, 16, null);

		layer.dispose();
	}

	private void drawChildren(Graphics graphics, Widget child, int x, int y)
	{
		if (child == null || child.isHidden())
		{
			return;
		}

		Graphics layer;

		Rectangle bounds = overrideBounds.get(child.getId() | child.getParentId());
		if (bounds != null)
		{
			layer = graphics.create(bounds.x, bounds.y, bounds.width, bounds.height);
			layer.setClip(0, 0, bounds.width, bounds.height);
		}
		else
		{
			layer = graphics.create(x, y, child.getWidth(), child.getHeight());
			layer.setClip(0, 0, child.getWidth(), child.getHeight());
		}
		drawWidget(graphics, child, child.getRelativeX(), child.getRelativeY());

		if (child.getStaticChildren() != null)
		{
			for (Widget children : child.getStaticChildren())
			{
				drawChildren(layer, children, children.getRelativeX(), children.getRelativeY());
			}
		}

		if (child.getDynamicChildren() != null)
		{
			drawDynamicChildren(layer, child, 0, 0);
		}

		layer.dispose();
	}

	private void drawDynamicChildren(Graphics graphics, Widget child, int x, int y)
	{
		if (child.getDynamicChildren() != null)
		{
			Widget[] children = child.getDynamicChildren();

			for (int i = 0; i < children.length; i++)
			{
				Widget child2 = children[i];
				drawWidget(graphics, child2, child2.getRelativeX(), child2.getRelativeY());
			}
		}
	}

	private void drawWidget(Graphics graphics, Widget child, int x, int y)
	{
		if (child == null || child.isHidden() || child.getType() == 0)
		{
			return;
		}

		int width = child.getWidth();
		int height = child.getHeight();

		if (child.getSpriteId() > 0)
		{
			SpritePixels sp = getPixels(child.getSpriteId());
			BufferedImage childImage = sp.toBufferedImage();


			if (child.getSpriteTiling())
			{
				Rectangle clips = graphics.getClipBounds();
				graphics.setClip(x, y, child.getWidth(), child.getHeight());

				for (int dx = x; dx < child.getWidth() + x; dx += sp.getMaxWidth())
				{
					for (int dy = y; dy < child.getHeight() + y; dy += sp.getMaxHeight())
					{

						drawAt(graphics, childImage, dx + sp.getOffsetX(), dy + sp.getOffsetY());
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
			if (child.getId() == ComponentID.BANK_TAB_CONTAINER)
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
			x = child.getRelativeX();
			y = child.getRelativeY();

			Rectangle bounds = overrideBounds.get(child.getId() | child.getParentId());
			if (bounds != null)
			{
				x = bounds.x;
				y = bounds.y;
				width = bounds.width;
				height = bounds.height;
			}

			Graphics textLayer = graphics.create(x, y, width, height);

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

			int textWidth = textLayer.getFontMetrics().stringWidth(text);

			if (child.getXTextAlignment() == 0)
			{
			}
			else if (child.getXTextAlignment() == 1)
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

	private SpritePixels getPixels(int archive)
	{
		if (config.resourcePack())
		{
			SpritePixels pixels = client.getSpriteOverrides().get(archive);
			if (pixels != null)
			{
				return pixels;
			}
		}

		SpritePixels[] sp = client.getSprites(client.getIndexSprites(), archive, 0);
		if (sp == null)
		{
			return null;
		}

		return sp[0];
	}


	private BufferedImage getSprite(int id)
	{
		return getPixels(id).toBufferedImage();
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
