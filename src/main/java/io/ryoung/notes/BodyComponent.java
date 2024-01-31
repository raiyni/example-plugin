package io.ryoung.notes;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.Text;

@Setter
@Builder
public class BodyComponent implements LayoutableRenderableEntity
{
	private String body;

	@Builder.Default
	private Color color = Color.WHITE;

	private Font font;

	@Builder.Default
	private Point preferredLocation = new Point();

	@Builder.Default
	private Dimension preferredSize = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);

	@Builder.Default
	@Getter
	private final Rectangle bounds = new Rectangle();

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Prevent NPEs
		final String text = MoreObjects.firstNonNull(this.body, "");

		final Font textFont = MoreObjects.firstNonNull(this.font, graphics.getFont());
		final FontMetrics lfm = graphics.getFontMetrics(textFont);

		final int fmHeight = lfm.getHeight();
		final int baseX = preferredLocation.x;
		final int baseY = preferredLocation.y + fmHeight;
		int x = baseX;
		int y = baseY;

		final int fullWidth = getLineWidth(text, lfm);
		final TextComponent textComponent = new TextComponent();

		if (preferredSize.width < fullWidth)
		{
			int leftSmallWidth = preferredSize.width;

			final String[] splitLines = lineBreakText(text, leftSmallWidth, lfm);

			int lineCount = splitLines.length;

			for (int i = 0; i < lineCount; i++)
			{
				final String t = splitLines[i];
				textComponent.setPosition(new Point(x, y));
				textComponent.setText(t);
				textComponent.setColor(color);
				textComponent.setFont(font);
				textComponent.render(graphics);

				y += fmHeight;
			}

			final Dimension dimension = new Dimension(preferredSize.width, y - baseY);
			bounds.setLocation(preferredLocation);
			bounds.setSize(dimension);
			return dimension;
		}

		if (!text.isEmpty())
		{
			textComponent.setPosition(new Point(x, y));
			textComponent.setText(text);
			textComponent.setColor(color);
			textComponent.setFont(font);
			textComponent.render(graphics);
		}

		y += fmHeight;

		final Dimension dimension = new Dimension(preferredSize.width, y - baseY);
		bounds.setLocation(preferredLocation);
		bounds.setSize(dimension);
		return dimension;
	}

	private static int getLineWidth(final String line, final FontMetrics metrics)
	{
		return metrics.stringWidth(Text.removeTags(line));
	}

	private static String[] lineBreakText(String text, int maxWidth, FontMetrics metrics)
	{
		final String[] words = text.split(" ");

		if (words.length == 0)
		{
			return new String[0];
		}

		final StringBuilder wrapped = new StringBuilder(words[0]);
		int spaceLeft = maxWidth - metrics.stringWidth(wrapped.toString());

		for (int i = 1; i < words.length; i++)
		{
			final String word = words[i];
			final int wordLen = metrics.stringWidth(word);
			final int spaceWidth = metrics.stringWidth(" ");

			if (wordLen + spaceWidth > spaceLeft)
			{
				wrapped.append('\n').append(word);
				spaceLeft = maxWidth - wordLen;
			}
			else
			{
				wrapped.append(' ').append(word);
				spaceLeft -= spaceWidth + wordLen;
			}
		}

		return wrapped.toString().split("\n");
	}
}
