package io.ryoung.areamute;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
public class AreaMuteOverlay extends Overlay
{
	private static final float STROKE_WIDTH = 2.0f;
	private static final Color CHUNK_BORDER_COLOR = Color.RED;

	private final AreaMutePlugin plugin;

	private final Client client;

	@Inject
	public AreaMuteOverlay(Client client, AreaMutePlugin plugin)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setStroke(new BasicStroke(STROKE_WIDTH));
		graphics.setColor(CHUNK_BORDER_COLOR);

		int regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
		WorldPoint wp = WorldPoint.fromRegion(regionId, 1, 0, client.getPlane());
		WorldPoint wp2 = WorldPoint.fromRegion(regionId, 4, 0, client.getPlane());

		log.info("{} {}", client.getBaseX(), client.getBaseY());
		log.info("{} {}", wp.getX(), wp.getY());
		log.info("{}", WorldPoint.isInScene(client, wp.getX() + 64, wp.getY() + 64));

		LocalPoint lp0 = LocalPoint.fromWorld(client, wp);
		LocalPoint lp1 = LocalPoint.fromWorld(client,wp2);

		if (lp0 == null || lp1 == null) {
			return null;
		}

		Point p0 = Perspective.localToCanvas(client, lp0, client.getPlane());
		Point p1 = Perspective.localToCanvas(client, lp1, client.getPlane());

		if (p0 == null || p1 == null) {
			return null;
		}

		graphics.drawLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());


//		int unpackedX = packed >> 8;
//		int unpackedY = packed & 0xFF;


//		int off = 16 * Perspective.LOCAL_TILE_SIZE;
//		int max = Perspective.SCENE_SIZE * Perspective.LOCAL_TILE_SIZE;
//		LocalPoint[] points =
//			{
//				new LocalPoint(off, off),
//				new LocalPoint(off, max - off),
//				new LocalPoint(max - off, max - off),
//				new LocalPoint(max - off, off),
//			};
//
//		for (int i = 0; i < 4; ++i)
//		{
//			LocalPoint lp0 = points[i];
//			LocalPoint lp1 = points[(i + 1) % 4];
//
//			Point p0 = Perspective.localToCanvas(client, lp0, client.getPlane());
//			Point p1 = Perspective.localToCanvas(client, lp1, client.getPlane());
//			if (p0 != null && p1 != null)
//			{
//				graphics.drawLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());
//			}
//		}

		return null;
	}
}
