package geogebra.phone.gui.view.euclidian;

import geogebra.html5.gui.FastClickHandler;
import geogebra.html5.main.AppW;
import geogebra.phone.PhoneLookAndFeel;
import geogebra.phone.gui.view.HeaderPanel;
import geogebra.phone.gui.view.euclidian.toolbar.ToolBarP;
import geogebra.web.gui.GuiManagerW;
import geogebra.web.gui.app.GGWToolBar;
import geogebra.web.gui.util.StandardButton;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class EuclidianHeaderPanel extends SimplePanel implements HeaderPanel, FastClickHandler {

	private PopupPanel panel;
	
	public EuclidianHeaderPanel(AppW app) {
		panel = new PopupPanel();
		ScrollPanel content = new ScrollPanel();

		GGWToolBar ggwToolbar = (GGWToolBar) app.getToolbar();

		final ToolBarP toolBar = new ToolBarP(ggwToolbar);
		toolBar.init(app);
		toolBar.buildGui();
		((GuiManagerW)app.getGuiManager()).setToolBarForUpdate(toolBar);

		content.add(toolBar);
		panel.add(content);
		panel.setAutoHideEnabled(true);

		// TODO: set icon of actual tool
		StandardButton openToolBarButton = new StandardButton(GGWToolBar.getMyIconResourceBundle().mode_point_32(), null, 32);
		openToolBarButton.addStyleName("toolbar_button");
		openToolBarButton.addFastClickHandler(this);
		add(openToolBarButton);
	}

	public void onClick(Widget source) {
		panel.show();

		// FIXME replace with dynamic value
		if (panel.getOffsetHeight() > Window.getClientHeight()
		        - PhoneLookAndFeel.PHONE_HEADER_HEIGHT) {
			panel.setHeight((Window.getClientHeight() - PhoneLookAndFeel.PHONE_HEADER_HEIGHT)
			        + "px");
		}
	}

}
