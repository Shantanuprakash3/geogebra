package geogebra.web.gui.view.spreadsheet;

import geogebra.common.awt.GColor;
import geogebra.common.awt.GPoint;
import geogebra.common.gui.SetLabels;
import geogebra.common.gui.view.spreadsheet.CellRange;
import geogebra.common.gui.view.spreadsheet.MyTableInterface;
import geogebra.common.gui.view.spreadsheet.SpreadsheetViewInterface;
import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.ModeSetter;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.main.App;
import geogebra.common.main.settings.AbstractSettings;
import geogebra.common.main.settings.SettingListener;
import geogebra.common.main.settings.SpreadsheetSettings;
import geogebra.html5.main.AppW;
import geogebra.html5.main.TimerSystemW;
import geogebra.html5.util.SpreadsheetTableModelW;
import geogebra.web.gui.layout.DockManagerW;

import java.util.HashMap;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.RequiresResize;
//import geogebra.web.gui.inputfield.MyTextField;
//import geogebra.web.gui.view.Gridable;

public class SpreadsheetViewW implements SpreadsheetViewInterface,
        SettingListener, RequiresResize, SetLabels {

	private static final long serialVersionUID = 1L;

	// ggb fields
	protected AppW app;
	private Kernel kernel;

	// spreadsheet table and row header
	MyTableW table;
	protected SpreadsheetTableModelW tableModel;
	//private JTableHeader tableHeader;
	public Canvas bluedot;

	// moved to kernel
	// if these are increased above 32000, you need to change traceRow to an
	// int[]
	// public static int MAX_COLUMNS = 9999; // TODO make sure this is actually
	// used
	// public static int MAX_ROWS = 9999; // TODO make sure this is actually
	// used

	private static int DEFAULT_COLUMN_WIDTH = 70;
	public static final int ROW_HEADER_WIDTH = 35; // wide enough for "9999"

	// TODO: should traceDialog belong to the SpreadsheetTraceManager?
	//private TraceDialog traceDialog;

	// fields for split panel, fileBrowser and stylebar
	//private JScrollPane spreadsheet;
	protected FocusPanel spreadsheetWrapper;
	//private FileBrowserPanel fileBrowser;
	private int defaultDividerLocation = 150;
	private SpreadsheetStyleBarW styleBar;
	//private JPanel restorePanel;

	// toolbar manager
	SpreadsheetToolbarManagerW toolbarManager;

	// file browser defaults
	public static final String DEFAULT_URL = "http://www.geogebra.org/static/data/data.xml";
	//public static final int DEFAULT_MODE = FileBrowserPanel.MODE_FILE;
	
	// private int initialBrowserMode = DEFAULT_MODE;
	// file browser settings
	// private String initialURL = DEFAULT_URL;

	// current toolbar mode
	private int mode = -1;

	//private JSplitPane splitPane;
	//private FormulaBar formulaBar;
	//private JPanel spreadsheetPanel;

	//private SpreadsheetViewDnD dndHandler;

	private boolean repaintScheduled = false;// to repaint less often, make it quicker


	
	// panel that contains the spreadsheet table and headers
	private AbsolutePanel spreadsheet;
	
	GPoint scrollPos = new GPoint();

	/******************************************************
	 * Construct spreadsheet view as a split panel. Left panel holds file tree
	 * browser, right panel holds spreadsheet.
	 */
	public SpreadsheetViewW(AppW app) {
		super();

		this.app = app;
		kernel = app.getKernel();

		// Initialize settings and register listener
		app.getSettings().getSpreadsheet().addListener(this);
	
		createGUI();

		updateFonts();
		attachView();

		// Create tool bar manager to handle tool bar mode changes
		toolbarManager = new SpreadsheetToolbarManagerW(app, this);

//		dndHandler = new SpreadsheetViewDnD(app, this);

		settingsChanged(settings());

		this.spreadsheet.addDomHandler(new TouchStartHandler() {
			public void onTouchStart(TouchStartEvent event) {
				if (event.getTouches().length() > 1) {
					Touch t0 = event.getTouches().get(0);
					Touch t1 = event.getTouches().get(1);
					scrollPos.setLocation(
					        getHorizontalScrollPosition()
					                + (t0.getScreenX() + t1
					                .getScreenX()) / 2,
					        getVerticalScrollPosition()
					                + (t0
					                .getScreenY() + t1.getScreenY()) / 2);
				}
			}
		}, TouchStartEvent.getType());

		this.spreadsheet.addDomHandler(new TouchMoveHandler() {
			public void onTouchMove(TouchMoveEvent event) {
				if (event.getTouches().length() > 1) {
					Touch t0 = event.getTouches().get(0);
					Touch t1 = event.getTouches().get(1);

					int x = (t0.getScreenX() + t1.getScreenX()) / 2;
					int y = (t0.getScreenY() + t1.getScreenY()) / 2;

					table.setHorizontalScrollPosition(scrollPos.x - x);
					table.setVerticalScrollPosition(scrollPos.y - y);
				}
			}
		}, TouchMoveEvent.getType());
	}

	
	private void createGUI(){
		
		// Build the spreadsheet table and enclosing scrollpane
		buildSpreadsheet();
		
		spreadsheetWrapper = new FocusPanel(spreadsheet);
		SpreadsheetKeyListenerW sskl = new SpreadsheetKeyListenerW(app, table);
		spreadsheetWrapper.addKeyDownHandler(sskl);
		spreadsheetWrapper.addKeyPressHandler(sskl);
		

	//	this.addScrollHandler(new ScrollHandler() {
	//		public void onScroll(ScrollEvent se) {
				//verticalScrollPosition = getVerticalScrollPosition();
				//horizontalScrollPosition = getHorizontalScrollPosition();
	//			requestFocus();
				//spreadsheet.setFocus(true);
	//		}
	//	});

		spreadsheetWrapper.addFocusHandler(new FocusHandler() {
			public void onFocus(FocusEvent fe) {
		//		verticalScrollPosition = getVerticalScrollPosition();
		//		horizontalScrollPosition = getHorizontalScrollPosition();
				Scheduler.get().scheduleDeferred(onFocusCommand);
			}
		});
		
		
		// Build the spreadsheet panel: formulaBar above, spreadsheet in Center
				/*spreadsheetPanel = new JPanel(new BorderLayout());
				spreadsheetPanel.add(spreadsheet, BorderLayout.CENTER);

				// Set the spreadsheet panel as the right component of this JSplitPane
				splitPane = new JSplitPane();
				splitPane.setRightComponent(spreadsheetPanel);
				splitPane.setBorder(BorderFactory.createEmptyBorder());

				// Set the browser as the left component or to null if showBrowserPanel
				// == false
				setShowFileBrowser(settings().showBrowserPanel());

				setLayout(new BorderLayout());
				add(splitPane, BorderLayout.CENTER);

				setBorder(BorderFactory.createEmptyBorder());
				addFocusListener(this);*/

		// Create row header
		/*rowHeader = new SpreadsheetRowHeader(app, table);

		// Set column width
		table.headerRenderer.setPreferredSize(new Dimension(
				(table.preferredColumnWidth),
				(SpreadsheetSettings.TABLE_CELL_HEIGHT)));

		// Put the table and the row header into a scroll plane
		// The scrollPane is named as spreadsheet
		spreadsheet = new JScrollPane();
		spreadsheet.setBorder(BorderFactory.createEmptyBorder());
		spreadsheet.setRowHeaderView(rowHeader);
		spreadsheet.setViewportView(table);

		// save the table header
		tableHeader = table.getTableHeader();

		// Create and set the scrollpane corners
		Corner upperLeftCorner = new Corner(); // use FlowLayout
		upperLeftCorner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1,
				MyTableD.HEADER_GRID_COLOR));
		upperLeftCorner.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				table.selectAll();
			}
		});
		spreadsheet.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER,
				upperLeftCorner);
		spreadsheet.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER,
				new Corner());
		spreadsheet.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER,
				new Corner());

		// Add a resize listener to the table so it can auto-enlarge if needed
		table.addComponentListener(this);
*/
		
	}
	

	private void buildSpreadsheet() {

		// Create the spreadsheet table model and the table
		tableModel = (SpreadsheetTableModelW) app.getSpreadsheetTableModel();
		table = new MyTableW(this, tableModel);

		spreadsheet = new AbsolutePanel();
		spreadsheet.add(table.getContainer());
		
	}

	Scheduler.ScheduledCommand onFocusCommand = new Scheduler.ScheduledCommand() {
		public void execute() {
		//	setVerticalScrollPosition(verticalScrollPosition);
		//	setHorizontalScrollPosition(horizontalScrollPosition);
		}
	};

	
	
	// ===============================================================
	// Corners
	// ===============================================================

	/*private static class Corner extends JComponent {
		private static final long serialVersionUID = -4426785169061557674L;

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(MyTableD.BACKGROUND_COLOR_HEADER);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}*/

	// ===============================================================
	// Defaults
	// ===============================================================


	public void setDefaultSelection() {
		setSpreadsheetScrollPosition(0, 0);
		table.setInitialCellSelection(0, 0);
	}

	// ===============================================================
	// getters/setters
	// ===============================================================

	public AppW getApplication() {
		return app;
	}

	public MyTableInterface getSpreadsheetTable() {
		return table;
	}

	/*public JViewport getRowHeader() {
		return spreadsheet.getRowHeader();
	}*/

	public void rowHeaderRevalidate() {
		//TODO//spreadsheet.getRowHeader().revalidate();
	}

	/*public JViewport getColumnHeader() {
		return spreadsheet.getColumnHeader();
	}*/

	public void columnHeaderRevalidate() {
		//TODO//spreadsheet.getColumnHeader().revalidate();
	}

	/*public JTableHeader getTableHeader() {
		return tableHeader;
	}*/

	public int getMode() {
		return mode;
	}

	/**
	 * get spreadsheet styleBar
	 */
	public SpreadsheetStyleBarW getSpreadsheetStyleBar() {
		if (styleBar == null) {
			styleBar = new SpreadsheetStyleBarW(this);
		}
		return styleBar;
	}

	// ===============================================================
	// VIEW Implementation
	// ===============================================================

	public void attachView() {
		// clearView();
		kernel.notifyAddAll(this);
		kernel.attach(this);
	}

	public void detachView() {
		kernel.detach(this);
		// clearView();
		// kernel.notifyRemoveAll(this);
	}



	public void add(GeoElement geo) {

		// Application.debug(new Date() + " ADD: " + geo);

		update(geo);
		GPoint location = geo.getSpreadsheetCoords();

		// autoscroll to new cell's location
		if (scrollToShow && location != null) {
			table.scrollRectToVisible(table.getCellRect(location.y, location.x,
			        true));
		}

		//scheduleRepaint();
	}

	public void remove(GeoElement geo) {
		// Application.debug(new Date() + " REMOVE: " + geo);
		//table.setRepaintAll();

		if (app.getTraceManager().isTraceGeo(geo)) {
			app.getTraceManager().removeSpreadsheetTraceGeo(geo);
			//TODO if (isTraceDialogVisible())
			//TODO	traceDialog.updateTraceDialog();
		}

		GPoint location = geo.getSpreadsheetCoords();

		switch (geo.getGeoClassType()) {
		case BOOLEAN:
		case BUTTON:
		case LIST:
			table.oneClickEditMap.remove(geo);
		}
		//scheduleRepaint();
		
		if (location != null) {
			table.updateCellFormat(location.y, location.x);
			table.syncRowHeaderHeight(location.y);
		}	
		
		// update the rowHeader height in case an oversized element has been
		// removed and the table row has resized itself 
		table.renderSelectionDeferred();
	}

	public void rename(GeoElement geo) {

		/*
		 * if(app.getTraceManager().isTraceGeo(geo))
		 * app.getTraceManager().updateTraceSettings(geo);
		 */
/*TODO
		if (isTraceDialogVisible()) {
			traceDialog.updateTraceDialog();
		}
*/
	}

	public void updateAuxiliaryObject(GeoElement geo) {
		// ignore
	}

	public void repaintView() {
		repaint();
	}

	public void clearView() {

		// restore defaults;
		app.getSettings().restoreDefaultSpreadsheetSettings();
		setDefaultSelection();
		table.oneClickEditMap.clear();
		tableModel.clearView();
		
	}

	/** Respond to changes in mode sent by GUI manager */
	public void setMode(int mode,ModeSetter m) {
		if(m != geogebra.common.kernel.ModeSetter.TOOLBAR){
			return;
		}
		
		this.mode = mode;
/*TODO
		if (isTraceDialogVisible()) {
			traceDialog.toolbarModeChanged(mode);
		}

		// String command = kernel.getModeText(mode); // e.g. "Derivative"

		toolbarManager.handleModeChange(mode);
*/

		toolbarManager.handleModeChange(mode);
//		switch(mode){
//		case EuclidianConstants.MODE_SPREADSHEET_SUM:
//		case EuclidianConstants.MODE_SPREADSHEET_AVERAGE:
//		case EuclidianConstants.MODE_SPREADSHEET_COUNT:
//		case EuclidianConstants.MODE_SPREADSHEET_MIN:
//		case EuclidianConstants.MODE_SPREADSHEET_MAX:
//			
//			// Handle autofunction modes
//			
//			table.setTableMode(MyTable.TABLE_MODE_AUTOFUNCTION);
//
//			break;
//		default:
//		}
	}

	/**
	 * Clear table and set to default layout. This method is called on startup
	 * or when new window is called
	 */
	public void restart() {

		clearView();
		tableModel.clearView();
		//TODO//updateColumnWidths();
		updateFonts();

		app.getTraceManager().loadTraceGeoCollection();

		table.oneClickEditMap.clear();

		// clear the formats and call settingsChanged
		settings().setCellFormat(null);

	}

	/** Resets spreadsheet after undo/redo call. */
	public void reset() {
		if (app.getTraceManager() != null)
			app.getTraceManager().loadTraceGeoCollection();
	}

	public void update(GeoElement geo) {

		//table.setRepaintAll();
		GPoint location = geo.getSpreadsheetCoords();
		if (location != null && location.x < Kernel.MAX_SPREADSHEET_COLUMNS_VISIBLE
				&& location.y < Kernel.MAX_SPREADSHEET_ROWS_VISIBLE) {

			// TODO: rowHeader and column
			// changes should be handled by a table model listener

			if (location.y >= tableModel.getRowCount()) {
				tableModel.setRowCount(location.y + 1);
				//TODO//spreadsheet.getRowHeader().revalidate();
			}
			if (location.x >= tableModel.getColumnCount()) {
				tableModel.setColumnCount(location.x + 1);
				//TODO//JViewport cH = spreadsheet.getColumnHeader();

				// bugfix: double-click to load ggb file gives cH = null
				//TODO//if (cH != null)
				//TODO	cH.revalidate();
			}

			// Mark this cell to be resized by height
			table.cellResizeHeightSet.add(new GPoint(location.x, location.y));

			// put geos with special editors in the oneClickEditMap
			if (geo.isGeoBoolean() || geo.isGeoButton() || geo.isGeoList()) {
				table.oneClickEditMap.put(location, geo);
			}

			// Update the cell format, it may change with this geo's properties
			table.updateCellFormat(location.y, location.x);
		}
	}

	final public void updateVisualStyle(GeoElement geo) {
		update(geo);
	}

	private boolean scrollToShow = false;

	public void setScrollToShow(boolean scrollToShow) {
		this.scrollToShow = scrollToShow;
	}

	// =====================================================
	// Formula Bar
	// =====================================================

	/*public FormulaBar getFormulaBar() {
		if (formulaBar == null) {
			// Build the formula bar
			formulaBar = new FormulaBar(app, this);
			formulaBar.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0,
							SystemColor.controlShadow), BorderFactory
							.createEmptyBorder(4, 4, 4, 4)));
		}
		return formulaBar;
	}*/

	/*public void updateFormulaBar() {
		if (formulaBar != null && settings().showFormulaBar())
			formulaBar.update();
	}*/

	// =====================================================
	// Tracing
	// =====================================================

	public void showTraceDialog(GeoElement geo, CellRange traceCell) {
		
		// not implemented yet
		
		//if (traceDialog == null) {
		//	traceDialog = new TraceDialog(app, geo, traceCell);
		//} else {
		//	traceDialog.setTraceDialogSelection(geo, traceCell);
		//}
		//traceDialog.setVisible(true);
	}

	/*public boolean isTraceDialogVisible() {
		return (traceDialog != null && traceDialog.isVisible());
	}*/

	/*public CellRange getTraceSelectionRange(int anchorColumn, int anchorRow) {
		if (traceDialog == null) {
			return null;
		}
		return traceDialog.getTraceSelectionRange(anchorColumn, anchorRow);
	}*/

	public void setTraceDialogMode(boolean enableMode) {
		if (enableMode) {
			table.setSelectionRectangleColor(GColor.GRAY);
			// table.setFocusable(false);
		} else {
			table.setSelectionRectangleColor(MyTableW.SELECTED_RECTANGLE_COLOR);
			// table.setFocusable(true);
		}
	}

	// ===============================================================
	// XML
	// ===============================================================

	/**
	 * returns settings in XML format
	 */
	public void getXML(StringBuilder sb, boolean asPreference) {
		sb.append("<spreadsheetView>\n");

		int width = spreadsheetWrapper.getOffsetWidth();// getPreferredSize().width;
		int height = spreadsheetWrapper.getOffsetHeight();// getPreferredSize().height;

		sb.append("\t<size ");
		sb.append(" width=\"");
		sb.append(width);
		sb.append("\"");
		sb.append(" height=\"");
		sb.append(height);
		sb.append("\"");
		sb.append("/>\n");

		sb.append("\t<prefCellSize ");
		sb.append(" width=\"");
		sb.append(table.preferredColumnWidth);
		sb.append("\"");
		sb.append(" height=\"");
		sb.append(table.minimumRowHeight
			//FIXME: temporarily, otherwise:
			//table.getRowHeight()
		);
		sb.append("\"");
		sb.append("/>\n");

		if (!asPreference) {

			// column widths
			for (int col = 1; col < table.getColumnCount(); col++) {
				int colWidth = table.getColumnWidth(col-1);
				// if (colWidth != DEFAULT_COLUMN_WIDTH)
				if (colWidth != table.preferredColumnWidth)
					sb.append("\t<spreadsheetColumn id=\"" + (col-1)
							+ "\" width=\"" + colWidth + "\"/>\n");
			}

			// row heights
			for (int row = 1; row < table.getRowCount(); row++) {
				int rowHeight = table.getGrid().getRowFormatter().getElement(row).getOffsetHeight();
				if (rowHeight != table.minimumRowHeight
						//FIXME: temporarily, otherwise
						//table.getRowHeight()
					)
					sb.append("\t<spreadsheetRow id=\"" + row + "\" height=\""
							+ rowHeight + "\"/>\n");
			}

			// initial selection
			sb.append("\t<selection ");

			sb.append(" hScroll=\"");
			sb.append(0
				//FIXME: might not be the same for Desktop and Web
				//getHorizontalScrollPosition()
				//spreadsheet.getHorizontalScrollBar().getValue()
			);
			sb.append("\"");

			sb.append(" vScroll=\"");
			sb.append(0
				//FIXME: might not be the same for Desktop and Web
				//getVerticalScrollPosition()
				//spreadsheet.getVerticalScrollBar().getValue()
			);
			sb.append("\"");

			sb.append(" column=\"");
			sb.append(table.anchorSelectionColumn
				//getColumnModel().getSelectionModel().getAnchorSelectionIndex()
			);
			sb.append("\"");

			sb.append(" row=\"");
			sb.append(table.anchorSelectionRow
				//table.getSelectionModel().getAnchorSelectionIndex()
			);
			sb.append("\"");

			sb.append("/>\n");
		}

		// layout
		sb.append("\t<layout ");

		sb.append(" showFormulaBar=\"");
		sb.append(settings().showFormulaBar() ? "true" : "false");
		sb.append("\"");

		sb.append(" showGrid=\"");
		sb.append(settings().showGrid() ? "true" : "false");
		sb.append("\"");

		sb.append(" showHScrollBar=\"");
		sb.append(settings().showHScrollBar() ? "true" : "false");
		sb.append("\"");

		sb.append(" showVScrollBar=\"");
		sb.append(settings().showVScrollBar() ? "true" : "false");
		sb.append("\"");

		sb.append(" showColumnHeader=\"");
		sb.append(settings().showColumnHeader() ? "true" : "false");
		sb.append("\"");

		sb.append(" showRowHeader =\"");
		sb.append(settings().showRowHeader() ? "true" : "false");
		sb.append("\"");

		sb.append(" allowSpecialEditor=\"");
		sb.append(settings().allowSpecialEditor() ? "true" : "false");
		sb.append("\"");

		sb.append(" allowToolTips=\"");
		sb.append(settings().allowToolTips() ? "true" : "false");
		sb.append("\"");

		sb.append(" equalsRequired=\"");
		sb.append(settings().equalsRequired() ? "true" : "false");
		sb.append("\"");

		sb.append("/>\n");

		// ---- end layout

		// file browser
		/*TODO if (fileBrowser != null) {
			sb.append("\t<spreadsheetBrowser ");

			if (!settings().initialFilePath().equals(settings().defaultFile())
					|| settings().initialURL() != DEFAULT_URL
					|| settings().initialBrowserMode() != DEFAULT_MODE) {
				sb.append(" default=\"");
				sb.append("false");
				sb.append("\"");

				sb.append(" dir=\"");
				sb.append(settings().initialFilePath());
				sb.append("\"");

				sb.append(" URL=\"");
				sb.append(settings().initialURL());
				sb.append("\"");

				sb.append(" mode=\"");
				sb.append(settings().initialBrowserMode());
				sb.append("\"");

			} else {

				sb.append(" default=\"");
				sb.append("true");
				sb.append("\"");
			}

			sb.append("/>\n");
		}*/

		// cell formats
		if (!asPreference)
			table.getCellFormatHandler().getXML(sb);

		sb.append("</spreadsheetView>\n");

		// Application.debug(sb);

	}

	// ===============================================================
	// Update
	// ===============================================================

	/*public void setLabels() {
		if (traceDialog != null)
			traceDialog.setLabels();

		if (table != null)
			table.setLabels();
		if (formulaBar != null) {
			formulaBar.setLabels();
		}
	}*/

	Scheduler.ScheduledCommand updateTableFonts = new Scheduler.ScheduledCommand() {
		public void execute() {
			table.updateFonts();
		}
	};
	
	public void updateFonts() {
		
		table.updateFonts();
	
/*TODO
		Font font = app.getPlainFont();

		MyTextField dummy = new MyTextField(app);
		dummy.setFont(font);
		dummy.setText("9999"); // for row header width
		int h = dummy.getPreferredSize().height;
		int w = dummy.getPreferredSize().width;
		rowHeader.setFixedCellWidth(w);

		// TODO: column widths are not set from here
		// need to revise updateColumnWidths() to do this correctly
		dummy.setText("MMMMMMMMMM"); // for column width
		h = dummy.getPreferredSize().height;
		w = dummy.getPreferredSize().width;
		table.setRowHeight(h);
		table.setPreferredColumnWidth(w);
		table.headerRenderer.setPreferredSize(new Dimension(w, h));

		table.setFont(app.getPlainFont());

		table.headerRenderer.setFont(font);

		// Adjust row heights for tall LaTeX images
		table.fitAll(true, false);

		if (fileBrowser != null)
			fileBrowser.updateFonts();

		if (formulaBar != null)
			formulaBar.updateFonts(font);
		*/
	}

	/*public void setColumnWidth(int col, int width) {
		// Application.debug("col = "+col+" width = "+width);
		TableColumn column = table.getColumnModel().getColumn(col);
		column.setPreferredWidth(width);
		// column.
	}*/

	public void setRowHeight(int row, int height) {
		table.setRowHeight(row, height);
	}

	/*TODO public void updateColumnWidths() {
		Font font = app.getPlainFont();

		int size = font.getSize();
		if (size < 12) {
			size = 12; // minimum size
		}
		double multiplier = (size) / 12.0;
		table.setPreferredColumnWidth((int) (SpreadsheetSettings.TABLE_CELL_WIDTH * multiplier));
		for (int i = 0; i < table.getColumnCount(); ++i) {
			table.getColumnModel().getColumn(i)
					.setPreferredWidth(table.preferredColumnWidth());
		}

	}*/

	public void setColumnWidthsFromSettings() {

		int prefWidth = table.preferredColumnWidth();
		HashMap<Integer, Integer> widthMap = settings().getWidthMap();
		
		for (int col = 0; col < table.getColumnCount(); ++col) {
			if (widthMap.containsKey(col)) {
				table.setColumnWidth(col, widthMap.get(col));
			} else {
				table.setColumnWidth(col, prefWidth);
			}
		}
	}
	

	public void setRowHeightsFromSettings() {

		// first set all row heights the same
		int prefHeight = settings().preferredRowHeight();
		table.setRowHeight(prefHeight);

		// now set custom row heights
		HashMap<Integer, Integer> heightMap = settings().getHeightMap();
		App.debug("height map size: " + heightMap.size());
		for (int row = 0; row < table.getRowCount(); ++row) {
			if (heightMap.containsKey(row)) {
				table.setRowHeight(row, heightMap.get(row));
			}
		}
	}

	/*public void updateRowHeader() {
		if (rowHeader != null) {
			rowHeader.updateRowHeader();
		}
	}*/

	public void setSpreadsheetScrollPosition(int hScroll, int vScroll) {
		table.setHorizontalScrollPosition(hScroll);
		table.setVerticalScrollPosition(vScroll);
		
		settings().setHScrollBalValue(hScroll);
		settings().setVScrollBalValue(vScroll);
	}

	// ==========================================================
	// Handle spreadsheet resize.
	//
	// Adds extra rows and columns to fill the enclosing scrollpane.
	// This is sometimes needed when rows or columns are resized
	// or the application window is enlarged.

	/**
	 * Tests if the spreadsheet fits the enclosing scrollpane viewport. Adds
	 * rows or columns if needed to fill the viewport.
	 */
	/*public void expandSpreadsheetToViewport() {

		if (table.getWidth() < spreadsheet.getWidth()) {

			int newColumns = (spreadsheet.getWidth() - table.getWidth())
					/ table.preferredColumnWidth();
			table.removeComponentListener(this);
			tableModel.setColumnCount(table.getColumnCount() + newColumns);
			table.addComponentListener(this);

		}
		if (table.getHeight() < spreadsheet.getHeight()) {
			int newRows = (spreadsheet.getHeight() - table.getHeight())
					/ table.getRowHeight();
			table.removeComponentListener(this);
			tableModel.setRowCount(table.getRowCount() + newRows);
			table.addComponentListener(this);

		}
*/
		// if table has grown after resizing all rows or columns, then select
		// all again
		// TODO --- why doesn't this work:
		/*
		 * if(table.isSelectAll()){ table.selectAll(); }
		 */

	/*}*/

	// Listener for a resized column or row

	/*public void componentResized(ComponentEvent e) {
		expandSpreadsheetToViewport();
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}*/

	// ===============================================================
	// Data Import & File Browser
	// ===============================================================

	/*public boolean loadSpreadsheetFromURL(File f) {

		boolean succ = false;

		URL url = null;
		try {
			url = f.toURI().toURL();
			succ = loadSpreadsheetFromURL(url);
		}

		catch (IOException ex) {
			ex.printStackTrace();
		}

		return succ;
	}

	public boolean loadSpreadsheetFromURL(URL url) {

		boolean succ = table.copyPasteCut.pasteFromURL(url);
		if (succ) {
			app.storeUndoInfo();
		}
		return succ;
	}*/

	/*public FileBrowserPanel getFileBrowser() {
		if (fileBrowser == null && AppD.hasFullPermissions()) {
			fileBrowser = new FileBrowserPanel(this);
			fileBrowser.setMinimumSize(new Dimension(50, 0));
			// initFileBrowser();
			// fileBrowser.setRoot(settings.initialPath(),
			// settings.initialBrowserMode());
		}
		return fileBrowser;
	}*/

	/*public void setShowFileBrowser(boolean showFileBrowser) {

		if (showFileBrowser) {
			splitPane.setLeftComponent(getFileBrowser());
			splitPane.setDividerLocation(defaultDividerLocation);
			splitPane.setDividerSize(4);
			initFileBrowser();

		} else {
			splitPane.setLeftComponent(null);
			splitPane.setLastDividerLocation(splitPane.getDividerLocation());
			splitPane.setDividerLocation(0);
			splitPane.setDividerSize(0);
		}

	}*/

	/*public void minimizeBrowserPanel() {
		splitPane.setDividerLocation(10);
		splitPane.setDividerSize(0);
		splitPane.setLeftComponent(getRestorePanel());
	}*/

	/*public void restoreBrowserPanel() {
		splitPane.setDividerLocation(splitPane.getLastDividerLocation());
		splitPane.setDividerSize(4);
		splitPane.setLeftComponent(getFileBrowser());

	}*/

	/**
	 * Returns restorePanel, if none exists a new one is built. RestorePanel is
	 * a slim vertical bar that holds the place of the minimized fileBrowser.
	 * When clicked it restores the file browser to full size.
	 * 
	 */
	/*public JPanel getRestorePanel() {
		if (restorePanel == null) {
			restorePanel = new JPanel();
			restorePanel.setMinimumSize(new Dimension(10, 0));
			restorePanel.setBorder(BorderFactory.createEtchedBorder(1));
			restorePanel.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					restoreBrowserPanel();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					restorePanel.setBackground(Color.LIGHT_GRAY);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					restorePanel.setBackground(null);
				}
			});
		}
		restorePanel.setBackground(null);
		return restorePanel;
	}*/

	/*public void setBrowserDefaults(boolean doRestore) {

		if (doRestore) {
			settings().setInitialFilePath(settings().defaultFile());
			settings().setInitialURL(DEFAULT_URL);
			settings().setInitialBrowserMode(FileBrowserPanel.MODE_FILE);
			// initFileBrowser();

		} else {
			settings().setInitialFilePath(fileBrowser.getRootString());
			settings().setInitialBrowserMode(fileBrowser.getMode());
		}
	}*/

	/*public void initFileBrowser() {
		// don't init file browser without full permissions (e.g. unsigned
		// applets)
		if (!AppD.hasFullPermissions() || !settings().showBrowserPanel())
			return;

		if (settings().initialBrowserMode() == FileBrowserPanel.MODE_FILE)
			setFileBrowserDirectory(settings().initialFilePath(), settings()
					.initialBrowserMode());
		else
			setFileBrowserDirectory(settings().initialURL(), settings()
					.initialBrowserMode());
	}*/

	/*public boolean setFileBrowserDirectory(String rootString, int mode) {
		settings().setInitialBrowserMode(mode);
		return getFileBrowser().setRoot(rootString, mode);
	}*/

	/*
	 * public void setDefaultFileBrowserDirectory() { if(this.DEFAULT_MODE ==
	 * FileBrowserPanel.MODE_FILE) setFileBrowserDirectory(String rootString,
	 * int mode) else setFileBrowserDirectory(DEFAULT_URL,
	 * FileBrowserPanel.MODE_URL); }
	 */

	// ================================================
	// Spreadsheet Settings
	// ================================================

	public void setEnableAutoComplete(boolean enableAutoComplete) {
		table.setEnableAutoComplete(enableAutoComplete);
	}

	
	public void setShowRowHeader(boolean showRowHeader) {
		table.setShowRowHeader(showRowHeader);
	}

	public void setShowColumnHeader(boolean showColumnHeader) {
		table.setShowColumnHeader(showColumnHeader);
	}

	public void setShowVScrollBar(boolean showVScrollBar) {
		table.setShowVScrollBar(showVScrollBar);
	}

	public void setShowHScrollBar(boolean showHScrollBar) {
		table.setShowHScrollBar(showHScrollBar);
	}

	public void setShowGrid(boolean showGrid) {
		//table.setShowGrid(showGrid);
		if (showGrid) {
			table.getGrid().getElement().removeClassName("off");
		} else {
			table.getGrid().getElement().addClassName("off");
		}
		if (this.isVisibleStyleBar()) {
			getSpreadsheetStyleBar().updateStyleBar();
		}
	}

	public boolean getAllowToolTips() {
		return settings().allowToolTips();
	}

	public void setAllowToolTips(boolean allowToolTips) {
		// do nothing yet
	}

	/*public void setShowFormulaBar(boolean showFormulaBar) {
		if (showFormulaBar)
			spreadsheetPanel.add(getFormulaBar(), BorderLayout.NORTH);
		else if (formulaBar != null)
			spreadsheetPanel.remove(formulaBar);

		if (formulaBar != null)
			formulaBar.update();
		this.revalidate();
		this.repaint();
		getSpreadsheetStyleBar().updateStyleBar();
	}*/

	public boolean getShowFormulaBar() {
		return settings().showFormulaBar();
	}

	public boolean isVisibleStyleBar() {
		return styleBar == null || styleBar.isVisible();
	}

	public void setColumnSelect(boolean isColumnSelect) {
		// do nothing yet
	}

	public boolean isColumnSelect() {
		return settings().isColumnSelect();
	}

	public void setAllowSpecialEditor(boolean allowSpecialEditor) {
		repaint();
	}

	public boolean allowSpecialEditor() {
		return settings().allowSpecialEditor();
	}

	/**
	 * sets requirement that commands entered into cells must start with "="
	 */
	public void setEqualsRequired(boolean isEqualsRequired) {
		table.setEqualsRequired(isEqualsRequired);
	}

	/**
	 * gets requirement that commands entered into cells must start with "="
	 */
	public boolean isEqualsRequired() {
		return settings().equalsRequired();
	}

	boolean allowSettingUpate = true;

	public void updateCellFormat(String cellFormat) {
		if (!allowSettingUpate)
			return;

		settings().removeListener(this);
		settings().setCellFormat(cellFormat);
		settings().addListener(this);
	}

	/*protected void updateAllRowSettings() {
		if (!allowSettingUpate)
			return;

		settings().removeListener(this);
		settings().setPreferredRowHeight(table.getRowHeight());
		settings().getHeightMap().clear();
		for (int row = 0; row < table.getRowCount(); row++) {
			int rowHeight = table.getRowHeight(row);
			if (rowHeight != table.getRowHeight())
				settings().getHeightMap().put(row, rowHeight);
		}
		settings().addListener(this);
	}*/

	protected void updateRowHeightSetting(int row, int height) {
		if (!allowSettingUpate)
			return;

		settings().removeListener(this);
		settings().getHeightMap().put(row, height);
		settings().addListener(this);
	}

	protected void updatePreferredRowHeight(int preferredRowHeight) {
		if (!allowSettingUpate)
			return;

		settings().removeListener(this);
		settings().getHeightMap().clear();
		settings().setPreferredRowHeight(preferredRowHeight);
		settings().addListener(this);
	}

	protected void updateColumnWidth(int col, int colWidth) {
		if (!allowSettingUpate)
			return;

		settings().removeListener(this);
		settings().getWidthMap().put(col, colWidth);
		settings().addListener(this);
	}

	protected void updatePreferredColumnWidth(int colWidth) {
		if (!allowSettingUpate)
			return;

		settings().removeListener(this);
		settings().getWidthMap().clear();
		settings().setPreferredColumnWidth(table.preferredColumnWidth);
		settings().addListener(this);
	}

	/*protected void updateAllColumnWidthSettings() {
		if (!allowSettingUpate)
			return;

		settings().removeListener(this);
		settings().setPreferredColumnWidth(table.preferredColumnWidth);
		settings().getWidthMap().clear();
		for (int col = 0; col < table.getColumnCount(); col++) {
			TableColumn column = table.getColumnModel().getColumn(col);
			int colWidth = column.getWidth();
			if (colWidth != table.preferredColumnWidth)
				settings().getWidthMap().put(col, colWidth);
		}
		settings().addListener(this);
	}*/

	protected SpreadsheetSettings settings() {
		return app.getSettings().getSpreadsheet();
	}

	public void settingsChanged(AbstractSettings settings0) {
		Scheduler.get().scheduleDeferred(deferredSettingsChanged);
	}

	Scheduler.ScheduledCommand deferredSettingsChanged = new Scheduler.ScheduledCommand() {
		public void execute() {
			settingsChangedCommand();
		}
	};
	
	public void settingsChangedCommand() {

		allowSettingUpate = true;

		// layout
		setShowColumnHeader(settings().showColumnHeader());
		setShowRowHeader(settings().showRowHeader());
		setShowVScrollBar(settings().showVScrollBar());
		setShowHScrollBar(settings().showHScrollBar());
		setShowGrid(settings().showGrid());
		setAllowToolTips(settings().allowToolTips());
		//?//setShowFormulaBar(settings().showFormulaBar());
		setColumnSelect(settings().isColumnSelect());
		setAllowSpecialEditor(settings().allowSpecialEditor());
		setEqualsRequired(settings().equalsRequired());
		setEnableAutoComplete(settings().isEnableAutoComplete());

		
		// browser panel
		/*? if (AppD.hasFullPermissions()) {
			settings().removeListener(this);
			if (settings().initialBrowserMode() < 0)
				settings().setInitialBrowserMode(FileBrowserPanel.MODE_FILE);
			if (settings().defaultFile() == null)
				settings().setDefaultFile(System.getProperty("user.dir"));
			if (settings().initialFilePath() == null)
				settings().setInitialFilePath(System.getProperty("user.dir"));
			if (settings().initialURL() == null)
				settings().setInitialURL(DEFAULT_URL);
			settings().addListener(this);
		}*/

		//?//setShowFileBrowser(settings().showBrowserPanel());

		// row height and column widths
		setRowHeightsFromSettings();
		setColumnWidthsFromSettings();
		
		// cell format
		getSpreadsheetTable().getCellFormatHandler().processXMLString(
			settings().cellFormat());

		// preferredSize
		setPreferredSize(
			settings().preferredSize().getWidth(),
			settings().preferredSize().getHeight());

		// initial position
		// TODO not working yet ...
		// setSpreadsheetScrollPosition(settings.scrollPosition().x,
		// settings.scrollPosition().y);
		// getTable().setInitialCellSelection(settings.selectedCell().x,
		// settings.selectedCell().y);

		allowSettingUpate = true;
		onResize();
	}

	public void setPreferredSize(int width, int height) {
		//getScrollPanel().setWidth(width + "px");
		//getScrollPanel().setHeight(height + "px");
		spreadsheetWrapper.setWidth(width + "px");
		spreadsheetWrapper.setHeight(height + "px");
	}

	// ================================================
	// Focus
	// ================================================

	/*protected boolean hasViewFocus() {
		boolean hasFocus = false;
		try {
			if (((LayoutW) app.getGuiManager().getLayout()).getDockManager()
					.getFocusedPanel() != null) co
				hasFocus = ((LayoutW) app.getGuiManager().getLayout()).getDockManager()
						.getFocusedPanel().isAncestorOf(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return hasFocus;
	}*/

	// transfer focus to the table
	//@Override
	public void requestFocus() {
		//if (table != null)
		//	table.requestFocus();

		Scheduler.get().scheduleDeferred(requestFocusCommand);
	}

	Scheduler.ScheduledCommand requestFocusCommand = new Scheduler.ScheduledCommand() {
		public void execute() {
			spreadsheetWrapper.setFocus(true);
		}
	};


	// test all components of SpreadsheetView for hasFocus
	//@Override
	public boolean hasFocus() {
		return ((DockManagerW)app.getGuiManager().getLayout().getDockManager()).getFocusedViewId() == App.VIEW_SPREADSHEET; /*TODO
		if (table == null)
			return false;
		return table.hasFocus()
				|| rowHeader.hasFocus()
				|| (table.getTableHeader() != null && table.getTableHeader()
						.hasFocus())
				|| spreadsheet.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER)
						.hasFocus()
				|| (formulaBar != null && formulaBar.hasFocus());
		*/
	}

	/*public void focusGained(FocusEvent arg0) {

	}

	public void focusLost(FocusEvent arg0) {
		getTable().repaint();

	}*/

	public int getViewID() {
		return App.VIEW_SPREADSHEET;
	}

	/*public int[] getGridColwidths() {
		int[] colWidths = new int[2 + tableModel.getHighestUsedColumn()];
		colWidths[0] = rowHeader.getWidth();
		for (int c = 0; c <= tableModel.getHighestUsedColumn(); c++) {
			colWidths[c + 1] = table.getColumnModel().getColumn(c).getWidth();
		}
		return colWidths;
	}*/

	/*public int[] getGridRowHeights() {
		int[] rowHeights = new int[2 + tableModel.getHighestUsedRow()];

		if (table.getTableHeader() == null)
			rowHeights[0] = 0;
		else
			rowHeights[0] = table.getTableHeader().getHeight();

		for (int r = 0; r <= tableModel.getHighestUsedRow(); r++) {
			rowHeights[r + 1] = table.getRowHeight(r);
		}
		return rowHeights;
	}*/

	/*public Component[][] getPrintComponents() {
		return new Component[][] {
				{ spreadsheet.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER),
						spreadsheet.getColumnHeader() },
				{ spreadsheet.getRowHeader(), table } };
	}*/

	public boolean isShowing() {
		// if this is attached, we shall make sure its parents are visible too
		return spreadsheetWrapper.isVisible()
			&& spreadsheetWrapper.isAttached()
			&& table != null
			&& table.getGrid().isVisible()
			&& table.getGrid().isAttached();
	}

	protected void onLoad() {
		// this may be important if the view is added/removed from the DOM
//TODO: is this needed with stand alone spreadsheetView?
	//	super.onLoad();
		repaint();
	}


	/**
	 * This method is called from timers.
	 * Only call this method if you really know what you're doing.
	 * Otherwise just call repaint().
	 */
	public void doRepaint() {
		table.repaint();
	}
	
	
	public final void repaint() {

    	if (waitForRepaint == TimerSystemW.SLEEPING_FLAG){
    		getApplication().ensureTimerRunning();
    		waitForRepaint = TimerSystemW.SPREADSHEET_LOOPS;
    	}
	}
	
	private int waitForRepaint = TimerSystemW.SLEEPING_FLAG;
	
	 
	
	/**
	 * timer system suggests a repaint
	 */
	public boolean suggestRepaint(){
		if (waitForRepaint == TimerSystemW.SLEEPING_FLAG){
			return false;
		}

		if (waitForRepaint == TimerSystemW.REPAINT_FLAG){
			if (isShowing()){
				doRepaint();	
				waitForRepaint = TimerSystemW.SLEEPING_FLAG;
			}
			return true;
		}
		
		waitForRepaint--;
		return true;
	}


	

	/**
	 * This method is used from add and remove, to ensure it is executed after
	 * the loop is executed - so in theory, if there is a loop with add methods,
	 * all the add methods come first, and repaint is called only afterwards
	 */
	public void scheduleRepaint() {
		if (!repaintScheduled) {
			repaintScheduled = true;
			Scheduler.get().scheduleDeferred(scheduleRepaintCommand);
		}
	}

	Scheduler.ScheduledCommand scheduleRepaintCommand = new Scheduler.ScheduledCommand() {
		public void execute() {
			repaintScheduled = false;
			repaint();
		}
	};

	public FocusPanel getFocusPanel() {
		return spreadsheetWrapper;
	}

//	public ScrollPanel getScrollPanel() {
//		return this;
//	}
	
	public void startBatchUpdate() {
		// TODO Auto-generated method stub
		
	}

	public void endBatchUpdate() {
		// TODO Auto-generated method stub
		
	}

	public void onResize() {

		if (getFocusPanel().getParent() == null) {
			return;
		}
		int width = getFocusPanel().getParent().getOffsetWidth();
		int height = getFocusPanel().getParent().getOffsetHeight();
		// App.debug("spreadsheet wrapper size: " + width + " , " + height);
		
		getFocusPanel().setWidth(width + "px");
		getFocusPanel().setHeight(height + "px");

		if(table != null){
			table.setSize(width, height);
			table.setRenderFirstTime();
			table.repaintAll();
		}
	}

	public int getHorizontalScrollPosition() {
	    return table.getHorizontalScrollPosition();
    }


	public int getVerticalScrollPosition() {
	    return table.getVerticalScrollPosition();
    }


	@Override
	public final void setLabels() {
		if (this.styleBar != null) {
			styleBar.setLabels();
		}
	}
}
