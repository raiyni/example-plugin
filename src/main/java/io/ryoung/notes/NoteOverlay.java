package io.ryoung.notes;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
public class NoteOverlay extends OverlayPanel
{
	private static final Splitter LINE_SPLITTER = Splitter.on("<b>").trimResults();
	private final OnScreenNotesPlugin plugin;
	private final OnScreenNotesConfig config;

	private TitleComponent titleComponent;
	private List<BodyComponent> body = new ArrayList<>();

	private String oldName;

	@Setter
	@Getter
	private Note note;

	NoteOverlay(
		OnScreenNotesPlugin onScreenNotesPlugin,
		OnScreenNotesConfig config,
		Note note
	)
	{
		super(onScreenNotesPlugin);
		this.plugin = onScreenNotesPlugin;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.HIGH);

		panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
		panelComponent.setGap(new Point(0, 2));

//		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Notes overlay");

		this.note = note;
		this.oldName = note.getMenuName();
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
		removeMenuEntries();
		addMenuEntries();

		setTitle(note.getTitle());
		setBody(note.getBody());

		this.panelComponent.setBackgroundColor(MoreObjects.firstNonNull(note.getBackgroundColor(), config.defaultBackground()));
		this.oldName = note.getMenuName();
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
				.color(MoreObjects.firstNonNull(note.getTitleColor(), config.defaultTitleColor()))
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
					.color(MoreObjects.firstNonNull(note.getTextColor(), config.defaultTextColor()))
					.build());
			}
		}
	}

	private void addMenuEntries()
	{

		addMenuEntry(RUNELITE_OVERLAY,"Edit Note", note.getMenuName());
		addMenuEntry(RUNELITE_OVERLAY,"Hide Note", note.getMenuName());
		addMenuEntry(RUNELITE_OVERLAY,"Delete Note", note.getMenuName());
	}

	private void removeMenuEntries()
	{
		removeMenuEntry(RUNELITE_OVERLAY,"Edit Note", this.oldName);
		removeMenuEntry(RUNELITE_OVERLAY,"Hide Note", this.oldName);
		removeMenuEntry(RUNELITE_OVERLAY,"Delete Note", this.oldName);
	}
}
