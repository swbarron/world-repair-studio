package net.querz.mcaselector.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.config.GlobalConfig;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.DialogHelper;

import java.io.File;
import java.util.Map;

/** Calm starting state that explains the workflow before exposing editing controls. */
public class WelcomeView extends StackPane {

	public WelcomeView(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("welcome-view");

		ImageView logo = new ImageView(FileHelper.getIconFromResources("img/icon"));
		logo.setFitWidth(92);
		logo.setFitHeight(92);
		logo.setPreserveRatio(true);
		logo.getStyleClass().add("welcome-logo");
		Label eyebrow = new Label("WORLD REPAIR STUDIO");
		eyebrow.getStyleClass().add("welcome-eyebrow");
		Label title = new Label("Move worlds forward.");
		title.getStyleClass().add("welcome-title");
		Label explanation = new Label("Open a Minecraft Java world to inspect terrain, select chunks, and move builds safely between worlds. Nothing changes until you confirm an editing action.");
		explanation.getStyleClass().add("welcome-explanation");
		explanation.setWrapText(true);
		explanation.setMaxWidth(520);

		Button open = new Button("Open World…");
		open.getStyleClass().addAll("welcome-open", "primary-command");
		open.setOnAction(e -> DialogHelper.openWorld(tileMap, primaryStage));
		Label note = new Label("Choose the folder that contains level.dat");
		note.getStyleClass().add("welcome-note");

		VBox card = new VBox(13, logo, eyebrow, title, explanation, open, note);
		card.getStyleClass().add("welcome-card");
		card.setAlignment(Pos.CENTER);

		HBox recents = recentWorlds(tileMap, primaryStage);
		VBox content = new VBox(24, card, recents);
		content.setAlignment(Pos.CENTER);
		getChildren().add(content);
	}

	private static HBox recentWorlds(TileMap tileMap, Stage primaryStage) {
		HBox row = new HBox(10);
		row.getStyleClass().add("recent-worlds");
		row.setAlignment(Pos.CENTER);
		int count = 0;
		for (Map.Entry<Long, GlobalConfig.RecentWorld> entry : ConfigProvider.GLOBAL.getRecentWorlds().descendingMap().entrySet()) {
			GlobalConfig.RecentWorld recent = entry.getValue();
			File world = recent.recentWorld();
			if (!world.isDirectory()) {
				continue;
			}
			Button button = new Button(world.getName());
			button.getStyleClass().add("recent-world");
			button.setOnAction(e -> DialogHelper.setWorld(
					FileHelper.detectWorldDirectories(world), recent.dimensionDirectories(), tileMap, primaryStage));
			row.getChildren().add(button);
			if (++count == 3) {
				break;
			}
		}
		if (count == 0) {
			Region placeholder = new Region();
			placeholder.setMinHeight(36);
			row.getChildren().add(placeholder);
		}
		return row;
	}
}
