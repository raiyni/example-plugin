package io.ryoung.notes;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.applet.Applet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "On-screen Notes",
	description = "Use shift click or ::notes to manage"
)
public class OnScreenNotesPlugin extends Plugin
{
	public static final String CONFIG_GROUP = "on-screen-notes";
	public static final String NOTE_KEY = "note_";

	public static final String SET_OPTION = "Set Note";
	public static final String DELETE_OPTION = "Delete Note";
	public static final String HIDE_OPTION = "Hide Note";


	private static final String WALK_HERE = "Walk here";
	private static final String TITLE_TITLE = "Note Title<br>(esc to cancel)";
	private static final String BODY_TITLE = "Note Body<br>(esc to cancel, <lt>b<gt> for newlines)";

	@Inject
	private Client client;

	@Inject
	private OnScreenNotesConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private Gson gson;

	private final List<Note> notes = new ArrayList<>();
	private final Map<Note, NoteOverlay> overlays = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		loadNotes();
	}

	@Override
	protected void shutDown() throws Exception
	{
		unloadNotes();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		switch (event.getKey())
		{
			case "defaultTextColor":
			case "defaultBackgroundColor":
			case "defaultTitleColor":
				overlays.values().forEach(NoteOverlay::reload);
				break;
		}
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (!(event.getOverlay() instanceof NoteOverlay))
		{
			return;
		}

		var overlay = (NoteOverlay) event.getOverlay();
		var note = overlay.getNote();
		switch (event.getEntry().getOption())
		{
			case "Edit Note":
				editNote(note);
				break;
			case "Hide Note":
				toggleNote(note);
				break;
			case "Delete Note":
				deleteNote(note);
				break;
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!"notes".equals(event.getCommand()))
		{
			return;
		}

		String[] args = event.getArguments();
		if (args.length == 0 || "help".equals(args[0]))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "Notes command list", null);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "::notes add", null);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "::notes list", null);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "::notes edit <lt>id<gt>", null);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "::notes toggle <lt>id<gt>", null);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "::notes delete <lt>id | all<gt>", null);
			return;
		}

		switch (args[0])
		{
			case "add":
				addNote();

				break;
			case "list":
				int idx = 0;
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "Notes:", null);
				for (Note note : notes)
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", String.format("%d: %s%s", idx++, note.getMenuName(), note.isVisible() ? "*" : ""), null);
				}

				break;
			case "edit":
				if (args.length == 2)
				{
					Note note = getByIdx(args[1]);
					if (note != null)
					{
						editNote(note);
					}
				}

				break;
			case "toggle":
				if (args.length == 2)
				{
					Note note = getByIdx(args[1]);
					if (note != null)
					{
						toggleNote(note);
					}
				}

				break;
			case "delete":
				if (args.length == 2)
				{
					if ("all".equals(args[1]))
					{
						deleteAllNotes();
						return;
					}

					Note note = getByIdx(args[1]);
					if (note != null)
					{
						deleteNote(note);
					}
				}

				break;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
		if (hotKeyPressed && config.useKeybinds() && event.getOption().equals(WALK_HERE))
		{
			client.getMenu()
				.createMenuEntry(-1)
				.setOption("Add")
				.setTarget("Note")
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					addNote();
				});

			if (config.deleteMenu() && !notes.isEmpty())
			{
				var deleteMenu = client.getMenu()
					.createMenuEntry(-2)
					.setOption("Delete")
					.setTarget("Note")
					.setType(MenuAction.RUNELITE)
					.createSubMenu();

				deleteMenu.createMenuEntry(-1)
					.setOption("Delete")
					.setTarget(JagexColors.MENU_TARGET_TAG + "All notes")
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						deleteAllNotes();
					});

				for (Note note : this.notes)
				{
					deleteMenu.createMenuEntry(-1)
						.setOption("Delete")
						.setTarget(JagexColors.MENU_TARGET_TAG + note.getMenuName())
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							deleteNote(note);
						});
				}
			}

			if (config.editMenu() && !notes.isEmpty())
			{
				var toggleMenu = client.getMenu()
					.createMenuEntry(-2)
					.setOption("Edit")
					.setTarget("Note")
					.setType(MenuAction.RUNELITE)
					.createSubMenu();

				for (Note note : this.notes)
				{
					toggleMenu.createMenuEntry(-1)
						.setOption("Edit")
						.setTarget(JagexColors.MENU_TARGET_TAG + note.getMenuName())
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							editNote(note);
						});
				}
			}

			if (config.toggleMenu() && !notes.isEmpty())
			{
				var toggleMenu = client.getMenu()
					.createMenuEntry(-2)
					.setOption("Toggle")
					.setTarget("Note")
					.setType(MenuAction.RUNELITE)
					.createSubMenu();

				for (Note note : this.notes)
				{
					toggleMenu.createMenuEntry(-1)
						.setOption(note.isVisible() ? "Hide" : "Show")
						.setTarget(JagexColors.MENU_TARGET_TAG + note.getMenuName())
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							toggleNote(note);
						});
				}
			}

		}
	}

	private void deleteAllNotes()
	{
		for (Note note : Lists.newArrayList(notes))
		{
			deleteNote(note);
		}
	}

	public Note getByIdx(String str)
	{
		try
		{
			int id = Integer.parseInt(str);
			return notes.get(id);
		}
		catch (Exception e)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "[notes]", "Invalid note id", null);
		}

		return null;
	}

	private void addNote(Note note)
	{
		this.notes.add(note);
		Collections.sort(this.notes);
	}

	private void addNote()
	{

		chatboxPanelManager.openTextInput(TITLE_TITLE)
			.value("")
			.onDone((Consumer<String>) (title) -> clientThread.invoke(() ->
			{
				chatboxPanelManager.openTextInput(BODY_TITLE)
					.value("")
					.onDone((body) ->
					{
						newNote(title, body, UUID.randomUUID() + "");
					}).build();
			}))
			.build();
	}

	private Note newNote(String title, String body, String name)
	{
		var note = Note.builder()
			.title(title)
			.body(body)
			.name(name)
			.build();

		addNote(note);
		saveNote(note);

		loadOverlay(note);

		return note;
	}


	private NoteOverlay loadOverlay(Note note)
	{
		var overlay = this.overlays.get(note);
		if (overlay != null)
		{
			overlayManager.remove(overlay);
		}

		overlay = new NoteOverlay(this, config, note);

		overlays.put(note, overlay);
		overlayManager.add(overlay);

		return overlay;
	}

	public void saveNote(Note note)
	{
		configManager.setConfiguration(CONFIG_GROUP, NOTE_KEY + note.getName(), gson.toJson(note));
	}

	public Note loadNote(String key)
	{
		String[] split = key.split("\\.", 2);
		String value = configManager.getConfiguration(CONFIG_GROUP, split[1]);
		try
		{
			var note = gson.fromJson(value, Note.class);
			if (note == null || Strings.isNullOrEmpty(note.getName()))
			{
				throw new JsonParseException("can't parse");
			}

			return note;
		}
		catch (JsonParseException e)
		{
			log.error("issue parsing key {}: {}", key, value);
			log.error("resetting key {}", key);

			String id = key.substring(NOTE_KEY.length());
			deleteNote(id);
		}

		return null;
	}

	public void loadNotes()
	{
		var keys = configManager.getConfigurationKeys(CONFIG_GROUP + "." + NOTE_KEY);
		for (String key : keys)
		{
			Note note = loadNote(key);
			if (note == null)
			{
				continue;
			}

			addNote(note);
			if (note.isVisible())
			{
				loadOverlay(note);
			}
		}
	}

	public void unloadNotes()
	{
		overlays.values().forEach(overlayManager::remove);
		overlays.clear();

		notes.clear();
	}

	public void changeTitle(Note note)
	{
		final NoteOverlay overlay = overlays.get(note);
		chatboxPanelManager.openTextInput(TITLE_TITLE)
			.value(note.getTitle())
			.onDone((value) ->
			{
				note.setTitle(value);
				saveNote(note);

				if (overlay != null)
				{
					overlay.reload();
				}
			})
			.build();
	}

	public void changeBody(Note note)
	{
		final NoteOverlay overlay = overlays.get(note);
		chatboxPanelManager.openTextInput(BODY_TITLE)
			.value(note.getBody())
			.onDone((value) ->
			{
				note.setBody(value);
				saveNote(note);

				if (overlay != null)
				{
					overlay.reload();
				}
			})
			.build();
	}

	public void changeText(Note note)
	{
		chatboxPanelManager.openTextInput(TITLE_TITLE)
			.value(note.getTitle())
			.onDone((Consumer<String>) (title) -> clientThread.invoke(() ->
			{
				chatboxPanelManager.openTextInput(BODY_TITLE)
					.value(note.getBody())
					.onDone((body) ->
					{
						note.setTitle(title);
						note.setBody(body);
						saveNote(note);

						reloadNote(note);
					}).build();
			}))
			.build();
	}

	public void deleteNote(Note note)
	{
		removeOverlay(note);
		deleteNote(note.getName());
		this.notes.remove(note);
	}

	public void deleteNote(String key)
	{
		configManager.unsetConfiguration(CONFIG_GROUP, NOTE_KEY + key);
		configManager.unsetConfiguration("runelite", key + "_preferredPosition");
		configManager.unsetConfiguration("runelite", key + "_preferredLocation");
		configManager.unsetConfiguration("runelite", key + "_preferredSize");
	}

	public void editNote(Note note)
	{
		chatboxPanelManager.openTextMenuInput("Edit " + note.getMenuName())
			.option("1. Edit text", () ->
				clientThread.invoke(() ->
					changeText(note))
			)
			.option("2. Change colors", () ->
				clientThread.invoke(() ->
					changeColors(note))
			)
			.option("3. Toggle note", () ->
				clientThread.invoke(() ->
					toggleNote(note))
			)
			.option("4. Delete note", () ->
				clientThread.invoke(() ->
					deleteNote(note))
			)
			.option("5. Cancel", Runnables.doNothing())
			.build();
	}

	public void changeColors(Note note)
	{

		chatboxPanelManager.openTextMenuInput("Change Color: " + note.getMenuName())
			.option("1. Title", () ->
				SwingUtilities.invokeLater(() ->
				{
					var win = SwingUtilities.windowForComponent((Applet) client);
					RuneliteColorPicker colorPicker = colorPickerManager.create(win,
						MoreObjects.firstNonNull(note.getTitleColor(), config.defaultTitleColor()), "Title Color: " + note.getMenuName(), true);
					colorPicker.setOnClose(c ->
						clientThread.invoke(() ->
						{
							if (Objects.equals(c, config.defaultTitleColor()))
							{
								return;
							}

							note.setTitleColor(c);
							saveNote(note);
							reloadNote(note);
						}));
					colorPicker.setLocationRelativeTo(win);
					colorPicker.setVisible(true);
				})
			)
			.option("2. Text", () ->
				SwingUtilities.invokeLater(() ->
				{
					var win = SwingUtilities.windowForComponent((Applet) client);
					RuneliteColorPicker colorPicker = colorPickerManager.create(win,
						MoreObjects.firstNonNull(note.getTextColor(), config.defaultTextColor()), "Text Color: " + note.getMenuName(), true);
					colorPicker.setOnClose(c ->
						clientThread.invoke(() ->
						{
							if (Objects.equals(c, config.defaultTextColor()))
							{
								return;
							}

							note.setTextColor(c);
							saveNote(note);
							reloadNote(note);
						}));
					colorPicker.setLocationRelativeTo(win);
					colorPicker.setVisible(true);
				})
			)
			.option("3. Background", () ->
				SwingUtilities.invokeLater(() ->
				{
					var win = SwingUtilities.windowForComponent((Applet) client);
					RuneliteColorPicker colorPicker = colorPickerManager.create(win,
						MoreObjects.firstNonNull(note.getBackgroundColor(), config.defaultBackground()), "Background Color: " + note.getMenuName(), false);
					colorPicker.setOnClose(c ->
						clientThread.invoke(() ->
						{
							if (Objects.equals(c, config.defaultBackground()))
							{
								return;
							}

							note.setBackgroundColor(c);
							saveNote(note);
							reloadNote(note);
						}));
					colorPicker.setLocationRelativeTo(win);
					colorPicker.setVisible(true);
				})
			)
			.option("4. Reset", () ->
			{
				note.setBackgroundColor(null);
				note.setTextColor(null);
				note.setTitleColor(null);

				saveNote(note);
				reloadNote(note);
			})
			.option("5. Cancel", Runnables.doNothing())
			.build();
	}

	public void reloadNote(Note note)
	{
		var overlay = overlays.get(note);
		if (overlay != null)
		{
			overlay.reload();
		}
	}

	public void toggleNote(Note note)
	{
		if (note.isVisible())
		{
			removeOverlay(note);
			note.setVisible(false);
		}
		else
		{
			note.setVisible(true);
			loadOverlay(note);
		}

		saveNote(note);
	}

	public void removeOverlay(Note note)
	{
		final NoteOverlay overlay = overlays.get(note);
		if (overlay != null)
		{
			overlayManager.remove(overlay);
			this.overlays.remove(note);
		}
	}

	@Provides
	OnScreenNotesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OnScreenNotesConfig.class);
	}
}
