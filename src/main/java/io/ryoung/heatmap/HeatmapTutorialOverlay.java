package io.ryoung.heatmap;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

public class HeatmapTutorialOverlay extends OverlayPanel
{
	private final Client client;
	private final HeatmapPlugin plugin;

	@Inject
	private HeatmapTutorialOverlay(Client client, HeatmapPlugin plugin)
	{
		super(plugin);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		this.client = client;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isBankVisible())
		{
			return null;
		}

		Widget button = client.getWidget(WidgetInfo.BANK_SETTINGS_BUTTON);
		if (button == null || button.isSelfHidden() || button.getDynamicChildren()[0].getSpriteId() != 195)
		{
			return null;
		}

		Rectangle bounds = button.getBounds();

		graphics.setColor(ColorScheme.BRAND_ORANGE);
		graphics.setStroke(new BasicStroke(2));
		graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

		FontMetrics font = graphics.getFontMetrics();
		int width = font.stringWidth("Right click this button");

		graphics.setColor(ColorScheme.DARKER_GRAY_COLOR);
		graphics.fillRect(bounds.x + bounds.width + 2, bounds.y - 15, width + 6, 30);


		graphics.setColor(ColorScheme.BRAND_ORANGE);
		graphics.drawString("Right click this button", bounds.x + bounds.width + 5, bounds.y);
		graphics.drawString("for Heatmap overlay", bounds.x + bounds.width + 5, bounds.y + 12);

		return super.render(graphics);
	}
}
