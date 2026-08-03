package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.stage.StageStyle;
import net.querz.mcaselector.text.Translation;

public class ConfirmationDialog extends Alert {

	public ConfirmationDialog(Window owner, Translation title, Translation headerText, String cssPrefix) {
		super(
				AlertType.WARNING,
				"",
				ButtonType.OK,
				ButtonType.CANCEL
		);
		initStyle(StageStyle.TRANSPARENT);
		getDialogPane().getStyleClass().add("studio-confirmation-dialog-pane");
		getDialogPane().getStyleClass().add(cssPrefix + "-confirmation-dialog-pane");
		getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
		titleProperty().bind(title.getProperty());
		headerTextProperty().bind(headerText.getProperty());
		contentTextProperty().bind(Translation.DIALOG_CONFIRMATION_QUESTION.getProperty());

		Label symbol = new Label("!");
		symbol.getStyleClass().add("studio-dialog-symbol");
		setGraphic(symbol);
		Button confirm = (Button) getDialogPane().lookupButton(ButtonType.OK);
		Button cancel = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
		confirm.getStyleClass().add("studio-dialog-confirm");
		cancel.getStyleClass().add("studio-dialog-cancel");
		if ("unsaved-changes".equals(cssPrefix)) {
			contentTextProperty().unbind();
			setContentText("Opening another world will clear it.");
			confirm.setText("Continue");
			cancel.setText("Keep editing");
		}
		setOnShown(e -> getDialogPane().getScene().setFill(Color.TRANSPARENT));
	}
}
