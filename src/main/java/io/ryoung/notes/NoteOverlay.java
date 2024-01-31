package io.ryoung.notes;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.client.ui.ColorScheme;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
public class NoteOverlay extends OverlayPanel
{
	private static final Splitter LINE_SPLITTER = Splitter.on("<b>").trimResults();
	private final OnScreenNotesPlugin plugin;

	private TitleComponent titleComponent;
	private List<BodyComponent> body = new ArrayList<>();

	@Setter
	@Getter
	private Note note;

	NoteOverlay(
		OnScreenNotesPlugin onScreenNotesPlugin,
		Note note
	)
	{
		super(onScreenNotesPlugin);
		this.plugin = onScreenNotesPlugin;

		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.HIGH);

		panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
		panelComponent.setGap(new Point(0, 2));

//		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Notes overlay");
		addMenuEntry(RUNELITE_OVERLAY, OnScreenNotesPlugin.SET_OPTION, "Title", (entry) -> plugin.changeTitle(this.note));
		addMenuEntry(RUNELITE_OVERLAY, OnScreenNotesPlugin.SET_OPTION, "Body", (entry) -> plugin.changeBody(this.note));
		addMenuEntry(RUNELITE_OVERLAY, OnScreenNotesPlugin.HIDE_OPTION, "", (entry) -> plugin.toggleNote(this.note));
		addMenuEntry(RUNELITE_OVERLAY, OnScreenNotesPlugin.DELETE_OPTION, "", (entry) -> plugin.deleteNote(this.note));

		this.note = note;
		reload();
	}


	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (titleComponent != null)
		{
			panelComponent.getChildren().add(titleComponent);
		}

		body.forEach(panelComponent.getChildren()::add);

		return super.render(graphics);
	}

	@Override
	public String getName()
	{
		return this.note.getName();
	}

	public void reload()
	{
		setTitle(note.getTitle());
		setBody(note.getBody());
	}

	private void setTitle(String title)
	{
		if (Strings.isNullOrEmpty(title))
		{
			titleComponent = null;
		}
		else
		{
			titleComponent = TitleComponent.builder()
				.text(title)
				.color(ColorScheme.BRAND_ORANGE)
				.build();
		}
	}

	private void setBody(String body)
	{
		this.body.clear();

		if (!Strings.isNullOrEmpty(body))
		{
			List<String> lines = LINE_SPLITTER.splitToList(body);
			for (String line : lines)
			{
				this.body.add(BodyComponent.builder()
					.body(line)
					.build());
			}
		}
	}
}
