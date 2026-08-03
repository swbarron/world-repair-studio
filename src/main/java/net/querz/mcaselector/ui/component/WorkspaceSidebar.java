package net.querz.mcaselector.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.DialogHelper;

/** A compact, detached tool palette. World identity lives exclusively in the tabs. */
public class WorkspaceSidebar extends VBox {

	private final Button filter;
	private final Button importSelection;
	private final Button overlays;
	private final Button renderSettings;

	public WorkspaceSidebar(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("workspace-sidebar");
		setPrefWidth(196);
		setMinWidth(196);
		setSpacing(7);

		Label heading = new Label("Tools");
		heading.getStyleClass().add("palette-title");
		getChildren().add(heading);

		Button select = action("Select chunks", "Click or drag on the map to select chunks");
		select.getStyleClass().add("active-tool");
		select.setDisable(true);
		filter = action("Filter chunks…", "Select chunks by their stored data");
		filter.setOnAction(e -> DialogHelper.filterChunks(tileMap, primaryStage));
		overlays = action("Map overlays…", "Configure data overlays on the map");
		overlays.setOnAction(e -> DialogHelper.editOverlays(tileMap, primaryStage));
		importSelection = action("Import selection…", "Load a saved selection CSV");
		importSelection.setOnAction(e -> DialogHelper.importSelection(tileMap, primaryStage));
		getChildren().addAll(select, filter, overlays, importSelection, section("VIEW"));

		renderSettings = action("Map appearance…", "Change map rendering settings");
		renderSettings.setOnAction(e -> DialogHelper.editSettings(tileMap, primaryStage, true));
		Button settings = action("App settings…", "Open application settings");
		settings.setOnAction(e -> DialogHelper.editSettings(tileMap, primaryStage, false));
		getChildren().addAll(renderSettings, settings);

		tileMap.setOnUpdate(this::update);
		update(tileMap);
	}

	private static Label section(String text) {
		Label label = new Label(text);
		label.getStyleClass().add("workspace-section-title");
		return label;
	}

	private static Button action(String text, String tooltip) {
		Button button = new Button(text);
		button.setMaxWidth(Double.MAX_VALUE);
		button.setAlignment(Pos.CENTER_LEFT);
		button.getStyleClass().add("sidebar-action");
		button.setTooltip(new Tooltip(tooltip));
		return button;
	}

	private void update(TileMap tileMap) {
		boolean worldOpen = !tileMap.getDisabled();
		filter.setDisable(!worldOpen);
		importSelection.setDisable(!worldOpen);
		overlays.setDisable(!worldOpen);
		renderSettings.setDisable(!worldOpen);
	}
}
