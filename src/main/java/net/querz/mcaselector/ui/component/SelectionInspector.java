package net.querz.mcaselector.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.DialogHelper;

/** Context-sensitive selection actions and impact summary. */
public class SelectionInspector extends VBox {

	private final Label selected = new Label("No chunks selected");
	private final Label regions = new Label("Select chunks on the map to begin");
	private final Label mode = new Label("Selection actions are disabled");
	private final Label clipboardSource = new Label("Clipboard is empty");
	private final Button copy;
	private final Button paste;
	private final MenuButton export;
	private final Button clear;
	private final Button delete;

	public SelectionInspector(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("selection-inspector");
		setPrefWidth(224);
		setMinWidth(224);
		setSpacing(9);

		Label heading = new Label("Selection");
		heading.getStyleClass().add("inspector-title");
		selected.getStyleClass().add("selection-count");
		regions.getStyleClass().add("selection-regions");
		regions.setWrapText(true);
		regions.setMaxWidth(190);

		VBox summary = new VBox(4, selected, regions);
		summary.getStyleClass().add("selection-summary");

		copy = action("Copy", "Copy the selected chunks to World Repair Studio's clipboard");
		copy.getStyleClass().add("primary-command");
		copy.setOnAction(e -> {
			DialogHelper.copySelectedChunks(tileMap);
			update(tileMap);
		});

		paste = action("Paste", "Preview copied chunks before placing them");
		paste.setOnAction(e -> {
			DialogHelper.pasteSelectedChunks(tileMap, primaryStage);
			update(tileMap);
		});

		export = new MenuButton("Export…");
		export.setMaxWidth(Double.MAX_VALUE);
		export.getStyleClass().add("inspector-action");
		MenuItem exportChunks = new MenuItem("Export chunks…");
		exportChunks.setOnAction(e -> DialogHelper.exportSelectedChunks(tileMap, primaryStage));
		MenuItem exportSelection = new MenuItem("Save selection CSV…");
		exportSelection.setOnAction(e -> DialogHelper.exportSelection(tileMap, primaryStage));
		export.getItems().addAll(exportChunks, exportSelection);
		clear = action("Clear", "Deselect all chunks without changing the world");
		clear.setOnAction(e -> tileMap.clearSelection());

		delete = action("Delete chunks…", "Permanently delete the selected chunks after confirmation");
		delete.getStyleClass().add("danger-command");
		delete.setOnAction(e -> DialogHelper.deleteSelection(tileMap, primaryStage));

		mode.setWrapText(true);
		mode.setMaxWidth(190);
		mode.getStyleClass().add("workspace-hint");
		clipboardSource.getStyleClass().add("clipboard-source");
		clipboardSource.setWrapText(true);
		clipboardSource.setMaxWidth(190);
		Region spacer = new Region();
		VBox.setVgrow(spacer, Priority.ALWAYS);

		getChildren().addAll(heading, summary, new Separator(), copy, paste, clipboardSource, export, clear,
				new Separator(), mode, spacer, delete);

		tileMap.setOnUpdate(this::update);
		update(tileMap);
	}

	private static Button action(String text, String tooltip) {
		Button button = new Button(text);
		button.setMaxWidth(Double.MAX_VALUE);
		button.setAlignment(Pos.CENTER);
		button.getStyleClass().add("inspector-action");
		button.setTooltip(new Tooltip(tooltip));
		return button;
	}

	private void update(TileMap tileMap) {
		boolean worldOpen = !tileMap.getDisabled();
		boolean inverted = tileMap.getSelection().isInverted();
		int chunks = tileMap.getSelectedChunks();
		boolean hasSelection = inverted || chunks > 0;

		selected.setText(inverted ? "All unexcluded chunks" : chunks == 1 ? "1 chunk" : chunks + " chunks");
		regions.setText(inverted ? "Inverted selection" : chunks == 0 ?
				"Select chunks on the map to begin" : "Across " + tileMap.getSelection().size() +
				(tileMap.getSelection().size() == 1 ? " region" : " regions"));
		mode.setText(hasSelection ?
				"Review the selection count before exporting or deleting. World backups are strongly recommended." :
				"Selection actions become available after chunks are selected.");

		copy.setDisable(!worldOpen || !hasSelection);
		export.setDisable(!worldOpen || !hasSelection);
		clear.setDisable(!worldOpen || !hasSelection);
		delete.setDisable(!worldOpen || !hasSelection);
		paste.setText(tileMap.isInPastingMode() ? "Confirm paste…" : "Paste");
		paste.setDisable(!worldOpen);
		String source = ClipboardContext.getSourceWorld();
		clipboardSource.setText(source == null ? "Clipboard is empty" : "Copied from " + source);
	}
}
