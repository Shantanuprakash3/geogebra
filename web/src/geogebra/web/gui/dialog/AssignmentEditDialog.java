package geogebra.web.gui.dialog;

import geogebra.common.main.App;
import geogebra.common.util.Assignment;
import geogebra.common.util.Assignment.Result;
import geogebra.html5.main.AppW;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AssignmentEditDialog extends DialogBoxW implements ClickHandler {

	private AppW app;
	private Assignment assignment;
	private Button btApply, btCancel;
	private FlexTable hintsAndFractiosforResult;
	private VerticalPanel mainWidget;
	private FlowPanel bottomWidget;
	private ExerciseBuilderDialog exerciseBuilderDialog;

	public AssignmentEditDialog(App app, Assignment assignment,
	        ExerciseBuilderDialog exerciseBuilderDialog) {
		super(false, false, null);

		this.app = (AppW) app;
		this.assignment = assignment;
		this.exerciseBuilderDialog = exerciseBuilderDialog;
		this.exerciseBuilderDialog.setVisible(false);
		createGUI();
	}

	private void createGUI() {

		getCaption().setText(app.getMenu("Assignment.Edit"));

		setWidget(mainWidget = new VerticalPanel());

		HorizontalPanel toolNameIconPanel = new HorizontalPanel();
		Image icon = new Image();
		icon.setUrl(exerciseBuilderDialog.getIconFile(assignment
		        .getIconFileName()));
		toolNameIconPanel.add(icon);
		toolNameIconPanel.add(new Label(assignment.getToolName()));

		mainWidget.add(toolNameIconPanel);

		hintsAndFractiosforResult = new FlexTable();
		FlexCellFormatter cellFormatter = hintsAndFractiosforResult
		        .getFlexCellFormatter();

		// cellFormatter.setColSpan(0, 1, 2);

		hintsAndFractiosforResult.setWidget(0, 0,
		        new Label(app.getPlain("Result")));
		hintsAndFractiosforResult.setWidget(0, 1,
		        new Label(app.getPlain("Hint")));
		hintsAndFractiosforResult.setWidget(0, 2,
		        new Label(app.getPlain("Fraction")));

		createHintsAndFractionsTable();

		mainWidget.add(hintsAndFractiosforResult);

		mainWidget.add(bottomWidget = new FlowPanel());
		bottomWidget.setStyleName("DialogButtonPanel");

		btApply = new Button(app.getPlain("Apply"));
		btApply.addClickHandler(this);
		btApply.getElement().getStyle().setMargin(3, Style.Unit.PX);

		bottomWidget.add(btApply);
	}

	private void createHintsAndFractionsTable() {
		int i = 1;
		int k = 0;
		for (Result res : Result.values()) {
			hintsAndFractiosforResult.setWidget(i, k++, new Label(res.name()));
			hintsAndFractiosforResult.setWidget(i, k++,
			        exerciseBuilderDialog.getHintTextBox(assignment, res));
			hintsAndFractiosforResult.setWidget(i, k++,
			        exerciseBuilderDialog.getFractionsLB(assignment, res));
			i++;
			k = 0;
		}
	}

	public void onClick(ClickEvent e) {
		Element target = e.getNativeEvent().getEventTarget().cast();

		if (target == btApply.getElement()) {
			hide();
			exerciseBuilderDialog.show();
			exerciseBuilderDialog.setVisible(true);
		}

	}

}
