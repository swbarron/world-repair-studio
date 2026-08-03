package net.querz.mcaselector.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.ui.component.OptionBar;
import net.querz.mcaselector.ui.component.SelectionInspector;
import net.querz.mcaselector.ui.component.StatusBar;
import net.querz.mcaselector.ui.component.TileMapBox;
import net.querz.mcaselector.ui.component.WorkspaceSidebar;
import net.querz.mcaselector.ui.component.WorkspaceToolbar;
import net.querz.mcaselector.ui.component.WorldTabBar;
import net.querz.mcaselector.ui.component.WelcomeView;
import net.querz.mcaselector.ui.component.Terrain3DView;
import net.querz.mcaselector.ui.dialog.PreviewDisclaimerDialog;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Window extends Application {

	private final int width = 1280, height = 780;

	private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);

	private Stage primaryStage;
	private String title = "";
	private OptionBar optionBar;
	private TileMapBox tileMapBox;

	private final List<Dialog<?>> trackedDialogs = new ArrayList<>();

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		String version;
		try {
			version = FileHelper.getManifestAttributes().getValue("Application-Version");
		} catch (IOException ex) {
			version = "dev";
		}

		title = "World Repair Studio " + version;
		primaryStage.setTitle(title);
		primaryStage.getIcons().add(FileHelper.getIconFromResources("img/icon"));

		TileMap tileMap = new TileMap(this, width, height);

		BorderPane pane = new BorderPane();
		pane.getStyleClass().add("workspace-root");

		// Native menu bar remains available for the full, advanced command set.
		optionBar = new OptionBar(tileMap, primaryStage);
		pane.setTop(optionBar);

		// Terrain is the full-bleed canvas. Navigation and editing panels float over it.
		tileMapBox = new TileMapBox(tileMap, primaryStage);
		Terrain3DView terrain3D = new Terrain3DView(tileMap);
		terrain3D.deactivate();
		StackPane mapLayers = new StackPane(tileMapBox);
		mapLayers.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		mapLayers.setPrefSize(width, height);
		mapLayers.getStyleClass().add("map-workspace");
		// Canvas has no intrinsic layout size. Bind the isometric layer to the
		// full map surface so its backdrop and HUD cannot collapse around their
		// preferred content and read as a dark panel in the middle of the world.
		terrain3D.prefWidthProperty().bind(mapLayers.widthProperty());
		terrain3D.prefHeightProperty().bind(mapLayers.heightProperty());

		WorkspaceToolbar workspaceToolbar = new WorkspaceToolbar(tileMap, primaryStage,
				optionBar.getHeightSlider(), terrain3D::deactivate, terrain3D::activate);
		WorldTabBar worldTabs = new WorldTabBar(tileMap, primaryStage);
		WorkspaceSidebar sidebar = new WorkspaceSidebar(tileMap, primaryStage);
		SelectionInspector inspector = new SelectionInspector(tileMap, primaryStage);
		StatusBar statusBar = new StatusBar(tileMap);

		sidebar.setMaxWidth(196);
		sidebar.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
		inspector.setMaxWidth(224);
		inspector.setMaxHeight(520);
		workspaceToolbar.setMaxWidth(790);
		workspaceToolbar.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
		worldTabs.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

		// Detached islands preserve visible world between every control group.
		StackPane editor = new StackPane(mapLayers, terrain3D, statusBar, sidebar, inspector, workspaceToolbar, worldTabs);
		StackPane.setAlignment(worldTabs, javafx.geometry.Pos.TOP_CENTER);
		StackPane.setAlignment(workspaceToolbar, javafx.geometry.Pos.TOP_CENTER);
		StackPane.setAlignment(sidebar, javafx.geometry.Pos.CENTER_LEFT);
		StackPane.setAlignment(inspector, javafx.geometry.Pos.CENTER_RIGHT);
		StackPane.setMargin(worldTabs, new javafx.geometry.Insets(14, 14, 0, 14));
		StackPane.setMargin(workspaceToolbar, new javafx.geometry.Insets(68, 14, 0, 14));
		StackPane.setMargin(sidebar, new javafx.geometry.Insets(126, 0, 20, 14));
		StackPane.setMargin(inspector, new javafx.geometry.Insets(126, 14, 20, 0));
		StackPane.setAlignment(statusBar, javafx.geometry.Pos.BOTTOM_LEFT);
		StackPane.setMargin(statusBar, new javafx.geometry.Insets(0, 0, 14, 14));
		statusBar.translateXProperty().bind(javafx.beans.binding.Bindings.when(sidebar.visibleProperty()).then(210).otherwise(0));

		WelcomeView welcome = new WelcomeView(tileMap, primaryStage);
		StackPane center = new StackPane(editor, welcome);
		pane.setCenter(center);
		tileMap.setOnUpdate(map -> {
			boolean worldOpen = !map.getDisabled() && net.querz.mcaselector.config.ConfigProvider.WORLD.getWorldDirs() != null;
			welcome.setVisible(!worldOpen);
			welcome.setManaged(!worldOpen);
			if (!worldOpen) {
				terrain3D.deactivate();
			}
		});

		sidebar.visibleProperty().bind(primaryStage.widthProperty().greaterThanOrEqualTo(900));
		sidebar.managedProperty().bind(sidebar.visibleProperty());
		inspector.visibleProperty().bind(primaryStage.widthProperty().greaterThanOrEqualTo(1120));
		inspector.managedProperty().bind(inspector.visibleProperty());

		Scene scene = new Scene(pane, width, height);

		Font.loadFont(Objects.requireNonNull(Window.class.getClassLoader().getResource("font/NotoSans-Regular.ttf")).toExternalForm(), 10);
		Font.loadFont(Objects.requireNonNull(Window.class.getClassLoader().getResource("font/NotoSansMono-Regular.ttf")).toExternalForm(), 10);
		Font.loadFont(Objects.requireNonNull(Window.class.getClassLoader().getResource("font/NotoSansMono-Bold.ttf")).toExternalForm(), 10);


		URL cssRes = Window.class.getClassLoader().getResource("style/base.css");
		if (cssRes != null) {
			String styleSheet = cssRes.toExternalForm();
			scene.getStylesheets().add(styleSheet);
		}
		URL workspaceCss = Window.class.getClassLoader().getResource("style/workspace.css");
		if (workspaceCss != null) {
			scene.getStylesheets().add(workspaceCss.toExternalForm());
		}

		scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
		scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
		primaryStage.focusedProperty().addListener((obs, o, n) -> {
			if (!n) {
				pressedKeys.clear();
			}
		});

		primaryStage.setOnCloseRequest(e -> {
			DialogHelper.quit(tileMap, primaryStage);
			e.consume();
		});
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(820);
		primaryStage.setMinHeight(560);

		if (version.contains("pre")) {
			new PreviewDisclaimerDialog(primaryStage).showAndWait();
		}

		tileMap.requestFocus();

		primaryStage.focusedProperty().addListener((v, o, n) -> {
			if (n) {
				trackedDialogs.forEach(d -> {
					((Stage) d.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
					((Stage) d.getDialogPane().getScene().getWindow()).setAlwaysOnTop(false);
				});
			}
		});

		Logging.updateThreadContext();
		primaryStage.show();
	}

	public void trackDialog(Dialog<?> dialog) {
		trackedDialogs.add(dialog);
	}

	public void untrackDialog(Dialog<?> dialog) {
		trackedDialogs.remove(dialog);
	}

	public void setTitleSuffix(String suffix) {
		if (suffix == null || suffix.isEmpty()) {
			primaryStage.setTitle(title);
		} else {
			String displayName = new File(suffix).getName();
			primaryStage.setTitle(title + " — " + displayName);
		}
	}

	public boolean isKeyPressed(KeyCode keyCode) {
		return pressedKeys.contains(keyCode);
	}

	public OptionBar getOptionBar() {
		return optionBar;
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public TileMapBox getTileMapBox() {
		return tileMapBox;
	}
}
