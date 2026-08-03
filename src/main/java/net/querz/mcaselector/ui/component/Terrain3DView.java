package net.querz.mcaselector.ui.component;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;
import net.querz.mcaselector.tile.TileMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A simplified isometric renderer built from MCA Selector's real surface colors
 * and per-column heights. Unlike a textured heightfield, every sampled column
 * receives a discrete top and visible side faces, so builds read as miniature
 * block geometry instead of a stretched map image.
 */
public class Terrain3DView extends StackPane {

	private static final int COLUMNS = 104;
	private static final int MIN_ROWS = 54;
	private static final int MAX_ROWS = 78;

	private final TileMap tileMap;
	private final Canvas canvas = new Canvas();
	private final Label status = new Label("Preparing isometric world…");
	private final Label camera = new Label();
	private final PauseTransition refreshDelay = new PauseTransition(Duration.millis(420));

	private SurfaceCell[][] cells;
	private int rows;
	private short floorHeight = 62;
	private short ceilingHeight = 192;
	private double heading = Math.toRadians(-42);
	private double pitch = 0.48;
	private double zoom = 1.0;
	private double pressX;
	private double pressY;
	private double startHeading;
	private double startPitch;
	private boolean active;

	public Terrain3DView(TileMap tileMap) {
		this.tileMap = tileMap;
		getStyleClass().add("terrain-3d-view");
		setMinSize(0, 0);
		setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		canvas.widthProperty().bind(widthProperty());
		canvas.heightProperty().bind(heightProperty());
		canvas.widthProperty().addListener((o, oldValue, newValue) -> render());
		canvas.heightProperty().addListener((o, oldValue, newValue) -> render());
		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::startOrbit);
		canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::orbit);
		canvas.setOnScroll(event -> {
			zoom = clamp(zoom * (event.getDeltaY() > 0 ? 1.08 : 0.92), 0.72, 1.85);
			render();
			event.consume();
		});

		Button reset = new Button("Reset view");
		reset.getStyleClass().add("terrain-reset");
		reset.setOnAction(e -> resetCamera());
		status.getStyleClass().add("terrain-status");
		camera.getStyleClass().add("terrain-camera");
		Label hint = new Label("Drag to rotate and tilt  •  Scroll to zoom");
		hint.getStyleClass().add("terrain-hint");
		HBox hud = new HBox(10, status, camera, hint, reset);
		hud.getStyleClass().add("terrain-hud");
		hud.setAlignment(Pos.CENTER_LEFT);
		hud.setMaxWidth(Region.USE_PREF_SIZE);
		hud.setMaxHeight(Region.USE_PREF_SIZE);

		getChildren().addAll(canvas, hud);
		StackPane.setAlignment(hud, Pos.BOTTOM_CENTER);
		StackPane.setMargin(hud, new javafx.geometry.Insets(0, 0, 20, 0));

		refreshDelay.setOnFinished(e -> rebuildSurface());
		tileMap.setOnUpdate(map -> {
			if (active) {
				refreshDelay.playFromStart();
			}
		});
		updateCameraLabel();
	}

	public void activate() {
		active = true;
		setVisible(true);
		setManaged(true);
		tileMap.requestVisibleTerrainData();
		refreshDelay.playFromStart();
	}

	public void deactivate() {
		active = false;
		refreshDelay.stop();
		setVisible(false);
		setManaged(false);
	}

	private void rebuildSurface() {
		if (!active || tileMap.getWidth() < 2 || tileMap.getHeight() < 2) {
			return;
		}

		int imageWidth = Math.max(2, (int) Math.round(tileMap.getWidth()));
		int imageHeight = Math.max(2, (int) Math.round(tileMap.getHeight()));
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.web("#14232a"));
		WritableImage surface = tileMap.snapshot(parameters, new WritableImage(imageWidth, imageHeight));
		PixelReader pixels = surface.getPixelReader();

		rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS,
				(int) Math.round(COLUMNS * tileMap.getHeight() / tileMap.getWidth())));
		cells = new SurfaceCell[rows][COLUMNS];
		List<Short> sampledHeights = new ArrayList<>(rows * COLUMNS);

		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				double sourceX = (column + 0.5) * tileMap.getWidth() / COLUMNS;
				double sourceY = (row + 0.5) * tileMap.getHeight() / rows;
				short height = tileMap.getTerrainHeightAtScreen(sourceX, sourceY);
				if (height == Short.MIN_VALUE || height == 0) {
					continue;
				}
				int pixelX = Math.max(0, Math.min(imageWidth - 1, (int) sourceX));
				int pixelY = Math.max(0, Math.min(imageHeight - 1, (int) sourceY));
				Color color = pixels.getColor(pixelX, pixelY);
				cells[row][column] = new SurfaceCell(column, row, height, normalize(color));
				sampledHeights.add(height);
			}
		}

		if (sampledHeights.isEmpty()) {
			floorHeight = 62;
			ceilingHeight = 192;
		} else {
			sampledHeights.sort(Short::compare);
			floorHeight = percentile(sampledHeights, 0.03);
			ceilingHeight = (short) Math.max(floorHeight + 40, percentile(sampledHeights, 0.995));
		}
		TileMap.TerrainDataProgress progress = tileMap.getTerrainDataProgress();
		boolean ready = progress.totalRegions() > 0 && progress.loadedRegions() >= progress.totalRegions();
		status.setText(ready ? "Isometric world  •  " + progress.totalRegions() +
				(progress.totalRegions() == 1 ? " region" : " regions") :
				"Building world… " + progress.loadedRegions() + "/" + progress.totalRegions());
		if (!ready) {
			tileMap.requestVisibleTerrainData();
		}
		render();
	}

	private void render() {
		if (!active || canvas.getWidth() < 2 || canvas.getHeight() < 2) {
			return;
		}
		GraphicsContext graphics = canvas.getGraphicsContext2D();
		graphics.setFill(new LinearGradient(0, 0, 0, canvas.getHeight(), false, CycleMethod.NO_CYCLE,
				new Stop(0, Color.web("#13272f")), new Stop(0.55, Color.web("#1c3538")),
				new Stop(1, Color.web("#31443f"))));
		graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

		if (cells == null) {
			return;
		}

		double cellSize = Math.min(10.2, canvas.getWidth() / (COLUMNS + rows) * 1.28) * zoom;
		double heightScale = cellSize * 0.16;
		double centerX = canvas.getWidth() / 2;
		double centerY = canvas.getHeight() * 0.59;
		double cos = Math.cos(heading);
		double sin = Math.sin(heading);

		List<SurfaceCell> ordered = new ArrayList<>(COLUMNS * rows);
		for (SurfaceCell[] row : cells) {
			for (SurfaceCell cell : row) {
				if (cell != null) {
					ordered.add(cell);
				}
			}
		}
		ordered.sort(Comparator.comparingDouble(cell -> depth(cell.column + 0.5, cell.row + 0.5, sin, cos)));

		for (SurfaceCell cell : ordered) {
			drawColumn(graphics, cell, centerX, centerY, cellSize, heightScale, cos, sin);
		}
	}

	private void drawColumn(GraphicsContext graphics, SurfaceCell cell, double centerX, double centerY,
			double cellSize, double heightScale, double cos, double sin) {
		double x0 = cell.column - COLUMNS / 2.0;
		double z0 = cell.row - rows / 2.0;
		double h = displayHeight(cell.height);

		Point[] top = new Point[]{
				project(x0, z0, h, centerX, centerY, cellSize, heightScale, cos, sin),
				project(x0 + 1, z0, h, centerX, centerY, cellSize, heightScale, cos, sin),
				project(x0 + 1, z0 + 1, h, centerX, centerY, cellSize, heightScale, cos, sin),
				project(x0, z0 + 1, h, centerX, centerY, cellSize, heightScale, cos, sin)
		};

		int[][] directions = {{0, -1, 0, 1}, {1, 0, 1, 2}, {0, 1, 3, 2}, {-1, 0, 0, 3}};
		for (int edgeIndex = 0; edgeIndex < directions.length; edgeIndex++) {
			int[] direction = directions[edgeIndex];
			double towardViewer = direction[0] * sin + direction[1] * cos;
			if (towardViewer <= 0.001) {
				continue;
			}
			Short rawNeighborHeight = neighborHeight(cell.column + direction[0], cell.row + direction[1]);
			if (rawNeighborHeight == null) {
				continue;
			}
			double neighborHeight = displayHeight(rawNeighborHeight);
			if (neighborHeight >= h) {
				continue;
			}
			// Cap exposed skirts so unloaded edges and deep ocean floors cannot
			// turn a coherent miniature into a field of vertical spikes.
			double bottomHeight = Math.max(neighborHeight, h - 20);
			int first = direction[2];
			int second = direction[3];
			Point bottomSecond = project(
					x0 + (second == 1 || second == 2 ? 1 : 0),
					z0 + (second >= 2 ? 1 : 0), bottomHeight,
					centerX, centerY, cellSize, heightScale, cos, sin);
			Point bottomFirst = project(
					x0 + (first == 1 || first == 2 ? 1 : 0),
					z0 + (first >= 2 ? 1 : 0), bottomHeight,
					centerX, centerY, cellSize, heightScale, cos, sin);
			Color side = shade(cell.color, edgeIndex == 1 || edgeIndex == 3 ? 0.58 : 0.70);
			fillPolygon(graphics, side, top[first], top[second], bottomSecond, bottomFirst);
		}

		fillPolygon(graphics, topColor(cell.color), top);
		graphics.setStroke(Color.color(1, 1, 1, 0.055));
		graphics.setLineWidth(0.45);
		strokePolygon(graphics, top);
	}

	private Short neighborHeight(int column, int row) {
		if (column < 0 || column >= COLUMNS || row < 0 || row >= rows || cells[row][column] == null) {
			return null;
		}
		return cells[row][column].height;
	}

	private Point project(double gridX, double gridZ, double height, double centerX, double centerY,
			double cellSize, double heightScale, double cos, double sin) {
		double rotatedX = gridX * cos - gridZ * sin;
		double depth = gridX * sin + gridZ * cos;
		return new Point(
				centerX + rotatedX * cellSize,
				centerY + depth * cellSize * pitch - (height - floorHeight) * heightScale);
	}

	private double displayHeight(short height) {
		return clamp(height, floorHeight - 4, ceilingHeight);
	}

	private static short percentile(List<Short> values, double quantile) {
		int index = (int) Math.round((values.size() - 1) * quantile);
		return values.get(Math.max(0, Math.min(values.size() - 1, index)));
	}

	private static double depth(double gridX, double gridZ, double sin, double cos) {
		return gridX * sin + gridZ * cos;
	}

	private void startOrbit(MouseEvent event) {
		pressX = event.getX();
		pressY = event.getY();
		startHeading = heading;
		startPitch = pitch;
	}

	private void orbit(MouseEvent event) {
		heading = startHeading + (event.getX() - pressX) * 0.009;
		pitch = clamp(startPitch + (event.getY() - pressY) * 0.003, 0.28, 0.72);
		updateCameraLabel();
		render();
	}

	private void resetCamera() {
		heading = Math.toRadians(-42);
		pitch = 0.48;
		zoom = 1.0;
		updateCameraLabel();
		render();
	}

	private void updateCameraLabel() {
		int degrees = Math.floorMod((int) Math.round(Math.toDegrees(heading)), 360);
		camera.setText(degrees + "°");
	}

	private static Color normalize(Color source) {
		if (source.getOpacity() < 0.1) {
			return Color.web("#3f5660");
		}
		return Color.color(source.getRed(), source.getGreen(), source.getBlue(), 1);
	}

	private static Color shade(Color color, double brightness) {
		return Color.color(
				clamp(color.getRed() * brightness, 0, 1),
				clamp(color.getGreen() * brightness, 0, 1),
				clamp(color.getBlue() * brightness, 0, 1), 1);
	}

	private static Color brighten(Color color, double brightness) {
		return shade(color, brightness);
	}

	private static Color topColor(Color color) {
		return Color.color(
				clamp(color.getRed() * 1.12 + 0.035, 0, 1),
				clamp(color.getGreen() * 1.12 + 0.035, 0, 1),
				clamp(color.getBlue() * 1.12 + 0.035, 0, 1), 1);
	}

	private static void fillPolygon(GraphicsContext graphics, Color color, Point... points) {
		double[] x = new double[points.length];
		double[] y = new double[points.length];
		for (int i = 0; i < points.length; i++) {
			x[i] = points[i].x;
			y[i] = points[i].y;
		}
		graphics.setFill(color);
		graphics.fillPolygon(x, y, points.length);
	}

	private static void strokePolygon(GraphicsContext graphics, Point... points) {
		double[] x = new double[points.length];
		double[] y = new double[points.length];
		for (int i = 0; i < points.length; i++) {
			x[i] = points[i].x;
			y[i] = points[i].y;
		}
		graphics.strokePolygon(x, y, points.length);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private record SurfaceCell(int column, int row, short height, Color color) {}
	private record Point(double x, double y) {}
}
