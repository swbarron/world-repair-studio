package net.querz.mcaselector.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.DialogHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Browser-like world sessions for fast cross-world chunk transfer. */
public class WorldTabBar extends HBox {

	private final TileMap tileMap;
	private final Stage primaryStage;
	private final HBox tabs = new HBox(5);
	private final List<WorldSession> sessions = new ArrayList<>();
	private String activeKey;

	public WorldTabBar(TileMap tileMap, Stage primaryStage) {
		this.tileMap = tileMap;
		this.primaryStage = primaryStage;
		getStyleClass().add("world-tab-bar");
		setAlignment(Pos.CENTER_LEFT);
		setSpacing(8);

		Button add = new Button("+");
		add.getStyleClass().add("add-world-tab");
		add.setTooltip(new Tooltip("Open another world"));
		add.setOnAction(e -> DialogHelper.openWorld(tileMap, primaryStage));
		setMaxWidth(Region.USE_PREF_SIZE);
		getChildren().addAll(tabs, add);

		tileMap.setOnUpdate(this::syncCurrentWorld);
		syncCurrentWorld(tileMap);
	}

	private void syncCurrentWorld(TileMap map) {
		if (map.getDisabled() || ConfigProvider.WORLD.getWorldDirs() == null) {
			return;
		}
		WorldDirectories dirs = ConfigProvider.WORLD.getWorldDirs();
		List<File> dimensions = ConfigProvider.WORLD.getDimensionDirectories();
		File dimension = ConfigProvider.WORLD.getRegionDir().getParentFile();
		String key = worldKey(dimension, dimensions);
		WorldSession replacement = new WorldSession(key, WorkspaceToolbar.worldName(dimension), dirs, dimensions);
		int index = indexOf(key);
		if (index < 0) {
			sessions.add(replacement);
		} else {
			sessions.set(index, replacement);
		}
		activeKey = key;
		rebuild();
	}

	private void rebuild() {
		tabs.getChildren().clear();
		for (WorldSession session : sessions) {
			Button tab = new Button(session.name());
			tab.getStyleClass().add("world-tab");
			if (session.key().equals(activeKey)) {
				tab.getStyleClass().add("active-world-tab");
			}
			tab.setOnAction(e -> switchTo(session));
			Button close = new Button("x");
			close.getStyleClass().add("close-world-tab");
			close.setVisible(sessions.size() > 1);
			close.setManaged(close.isVisible());
			close.setOnAction(e -> close(session));
			HBox shell = new HBox(tab, close);
			shell.getStyleClass().add("world-tab-shell");
			tabs.getChildren().add(shell);
		}
	}

	private void switchTo(WorldSession session) {
		if (!session.key().equals(activeKey)) {
			DialogHelper.setWorld(session.directories(), session.dimensions(), tileMap, primaryStage);
		}
	}

	private void close(WorldSession session) {
		if (sessions.size() <= 1) {
			return;
		}
		int index = sessions.indexOf(session);
		sessions.remove(session);
		if (session.key().equals(activeKey)) {
			WorldSession next = sessions.get(Math.max(0, Math.min(index, sessions.size() - 1)));
			switchTo(next);
		} else {
			rebuild();
		}
	}

	private int indexOf(String key) {
		for (int i = 0; i < sessions.size(); i++) {
			if (sessions.get(i).key().equals(key)) {
				return i;
			}
		}
		return -1;
	}

	private static String worldKey(File dimension, List<File> dimensions) {
		if (dimensions != null && !dimensions.isEmpty()) {
			for (File candidate : dimensions) {
				if (!"DIM-1".equals(candidate.getName()) && !"DIM1".equals(candidate.getName())
						&& (candidate.getParentFile() == null || !"minecraft".equals(candidate.getParentFile().getName()))) {
					return candidate.getAbsolutePath();
				}
			}
		}
		return WorkspaceToolbar.worldRoot(dimension).getAbsolutePath();
	}

	private record WorldSession(String key, String name, WorldDirectories directories, List<File> dimensions) {}
}
