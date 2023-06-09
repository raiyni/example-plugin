package io.ryoung.areamute;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Slf4j
public class AreaMuteOverlay extends Overlay
{
	private static final float STROKE_WIDTH = 2f;

	private final AreaMutePlugin plugin;

	private final Client client;

	private final AreaMuteConfig config;

	@Inject
	public AreaMuteOverlay(Client client, AreaMutePlugin plugin, AreaMuteConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(WidgetID.WORLD_MAP_GROUP_ID);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
		if (map == null)
		{
			return null;
		}

		WorldMap worldMap = client.getWorldMap();
		Rectangle worldMapRect = map.getBounds();

		graphics.setClip(worldMapRect);
		graphics.setStroke(new BasicStroke(STROKE_WIDTH));

		Point mp = client.getMouseCanvasPosition();
		if (worldMapRect.contains(mp.getX(), mp.getY()))
		{
			int regionId = plugin.getRegionIdFromCursor();
			drawRegrion(graphics, map, worldMap, regionId, config.hoveredColor());
		}

		for (Integer regionId : plugin.getRegions())
		{
			drawRegrion(graphics, map, worldMap, regionId, config.mutedColor());
		}


		return null;
	}

	void drawRegrion(Graphics2D graphics, Widget map, WorldMap worldMap, int regionId, Color color)
	{
		float pixelsPerTile = worldMap.getWorldMapZoom();
		Rectangle worldMapRect = map.getBounds();

		int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
		int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

		int regionPixelSize = (int) Math.ceil(AreaMutePlugin.REGION_SIZE * pixelsPerTile);

		Point worldMapPosition = worldMap.getWorldMapPosition();

		graphics.setColor(color);

		int x = (regionId >>> 8) << 6;
		int y = (regionId & 0xff) << 6;

		int yTileOffset = -(worldMapPosition.getY() - heightInTiles / 2 - y);
		int xTileOffset = x + widthInTiles / 2 - worldMapPosition.getX();

		int xPos = ((int) (xTileOffset * pixelsPerTile)) + (int) worldMapRect.getX();
		int yPos = (worldMapRect.height - (int) (yTileOffset * pixelsPerTile)) + (int) worldMapRect.getY() - regionPixelSize;

		graphics.drawRect(xPos, yPos, regionPixelSize, regionPixelSize);
	}

}
