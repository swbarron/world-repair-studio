package net.querz.mcaselector.ui.component;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.DialogHelper;

import java.io.File;
import java.util.List;

/**
 * High-frequency commands that should not require remembering a menu path.
 * The complete command set remains in {@link OptionBar}.
 */
public class WorkspaceToolbar extends HBox {

	private final MenuButton dimension = new MenuButton("Dimension");
	private final Button filter = command("Filter chunks", "Build a reusable selection from chunk data");
	private final Button goTo = command("Go to…", "Jump to block coordinates");
	private final Button resetZoom = command("Reset zoom", "Return the map to its default zoom");

	public WorkspaceToolbar(TileMap tileMap, Stage primaryStage, HeightSlider heightSlider,
			Runnable show2D, Runnable show3D) {
		getStyleClass().add("workspace-toolbar");
		setSpacing(8);

		Button openWorld = command("Open World…", "Open a Minecraft Java world folder");
		openWorld.setOnAction(e -> DialogHelper.openWorld(tileMap, primaryStage));

		Button select = command("□", "Selection mode: click or drag on the map to select chunks");
		select.setAccessibleText("Selection mode");
		select.getStyleClass().add("active-tool");
		select.getStyleClass().add("icon-command");
		select.setDisable(true);

		ToggleButton map2D = viewMode("2D Map", "Edit and select chunks on the top-down map");
		ToggleButton terrain3D = viewMode("Isometric 3D", "Rotate a simplified block rendering of terrain and builds");
		ToggleGroup viewModes = new ToggleGroup();
		map2D.setToggleGroup(viewModes);
		terrain3D.setToggleGroup(viewModes);
		map2D.setSelected(true);
		map2D.setOnAction(e -> {
			if (map2D.isSelected()) {
				show2D.run();
			} else {
				map2D.setSelected(true);
			}
		});
		terrain3D.setOnAction(e -> {
			if (terrain3D.isSelected()) {
				show3D.run();
			} else {
				terrain3D.setSelected(true);
			}
		});
		HBox viewSwitcher = new HBox(map2D, terrain3D);
		viewSwitcher.getStyleClass().add("view-switcher");

		filter.setOnAction(e -> DialogHelper.filterChunks(tileMap, primaryStage));
		goTo.setOnAction(e -> DialogHelper.gotoCoordinate(tileMap, primaryStage));
		resetZoom.setText("100%");
		resetZoom.setOnAction(e -> tileMap.setScale(1));

		dimension.getStyleClass().add("dimension-menu");
		heightSlider.getStyleClass().add("workspace-height-slider");
		heightSlider.setCompact(true);
		heightSlider.setPrefWidth(174);
		heightSlider.setMaxWidth(174);
		Label heightLabel = new Label("Y");
		heightLabel.getStyleClass().add("toolbar-field-label");

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		getChildren().addAll(openWorld, select, filter, viewSwitcher, spacer, dimension, heightLabel,
				heightSlider, resetZoom, goTo);

		openWorld.visibleProperty().bind(primaryStage.widthProperty().lessThan(900));
		openWorld.managedProperty().bind(openWorld.visibleProperty());
		filter.visibleProperty().bind(primaryStage.widthProperty().lessThan(900));
		filter.managedProperty().bind(filter.visibleProperty());
		heightLabel.visibleProperty().bind(primaryStage.widthProperty().greaterThanOrEqualTo(980));
		heightLabel.managedProperty().bind(heightLabel.visibleProperty());
		heightSlider.visibleProperty().bind(primaryStage.widthProperty().greaterThanOrEqualTo(980));
		heightSlider.managedProperty().bind(heightSlider.visibleProperty());

		tileMap.setOnUpdate(this::update);
		update(tileMap);
	}

	private static Button command(String text, String tooltip) {
		Button button = new Button(text);
		button.getStyleClass().add("workspace-command");
		button.setTooltip(new Tooltip(tooltip));
		return button;
	}

	private static ToggleButton viewMode(String text, String tooltip) {
		ToggleButton button = new ToggleButton(text);
		button.getStyleClass().add("view-mode");
		button.setTooltip(new Tooltip(tooltip));
		return button;
	}

	private void update(TileMap tileMap) {
		boolean worldOpen = !tileMap.getDisabled() && ConfigProvider.WORLD.getWorldDirs() != null;
		filter.setDisable(!worldOpen);
		goTo.setDisable(!worldOpen);
		resetZoom.setDisable(!worldOpen);
		dimension.setDisable(!worldOpen);

		if (!worldOpen) {
			dimension.setText("Dimension");
			dimension.getItems().clear();
			return;
		}

		File currentDimension = ConfigProvider.WORLD.getRegionDir().getParentFile();
		dimension.setText(dimensionName(currentDimension));
		dimension.getItems().clear();

		List<File> dimensions = ConfigProvider.WORLD.getDimensionDirectories();
		if (dimensions == null) {
			return;
		}
		for (File candidate : dimensions) {
			MenuItem item = new MenuItem(dimensionName(candidate));
			item.setDisable(candidate.equals(currentDimension));
			item.setOnAction(e -> DialogHelper.setWorld(
					FileHelper.detectWorldDirectories(candidate), dimensions, tileMap, primaryStage(tileMap)));
			dimension.getItems().add(item);
		}
	}

	private static Stage primaryStage(TileMap tileMap) {
		return tileMap.getWindow().getPrimaryStage();
	}

	static String dimensionName(File directory) {
		return switch (directory.getName()) {
			case "DIM-1" -> "The Nether";
			case "DIM1" -> "The End";
			default -> directory.getParentFile() != null
					&& "minecraft".equals(directory.getParentFile().getName()) ? directory.getName() : "Overworld";
		};
	}

	public static String worldName(File dimensionDirectory) {
		File world = worldRoot(dimensionDirectory);
		return world == null ? "World" : world.getName();
	}

	public static File worldRoot(File dimensionDirectory) {
		File world = dimensionDirectory;
		if ("DIM-1".equals(world.getName()) || "DIM1".equals(world.getName())) {
			world = world.getParentFile();
		} else if (world.getParentFile() != null && "minecraft".equals(world.getParentFile().getName())) {
			File dimensions = world.getParentFile().getParentFile();
			if (dimensions != null && dimensions.getParentFile() != null) {
				world = dimensions.getParentFile();
			}
		}
		return world;
	}
}
