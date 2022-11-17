package visualization;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import objects.Campaign;
import objects.Scenario;
import objects.ScenarioSet;
import objects.Sensor;
import results.ResultPrinter;
import utilities.Point3f;
import utilities.Point3i;

/**
 * Back end for interfacing between DomainViewer and the DREAM data.
 * The main display has three different kinds of data:
 * 1) leak - all locations that exceed the leak definition
 * 2) sensor - all locations that exceed the monitoring capability
 * 3) campaign - actual locations of sensors for proposed campaigns
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class DomainVisualization {

	private Shell shell;
	private Tree tree_campaignTree;
	private Table table_sensorTable1;
	private Table table_sensorTable2;
	private Table table_sensorTable3;
	private DomainViewer domainViewer;

	private ScenarioSet set;

	private Button button_showMesh;
	private Button button_renderUniform;

	private Slider slider_scaleX;
	private Slider slider_scaleY;
	private Slider slider_scaleZ;

	private Slider slider_tickX;
	private Slider slider_tickY;
	private Slider slider_tickZ;

	private Text text_labelX;
	private Text text_labelY;
	private Text text_labelZ;
	
	private Slider slider_time;

	private Map<String, LeakTableItem> leakTableItems;
	private Map<String, SensorTableItem> sensorTableItems;
	private Map<String, CampaignTableItem> campaignTableItems;
	private Map<Integer, TreeParentCampaignItem> rankedCampaignMap;
	
	private boolean algorithmFinished = false;
	private boolean leakOldSelection = true;
	private boolean sensorOldSelection = true;
	private boolean campaignOldSelection = true;
	private TableItem leakSelectAll;
	private TableItem sensorSelectAll;
	private TableItem campaignSelectAll;
	
	private boolean resetTreeRequired = false;
	
	public DomainVisualization(Display display, ScenarioSet set, Boolean show) {
		shell = new Shell(display, SWT.DIALOG_TRIM | SWT.MODELESS);
		
		shell.setText("DREAM Visualization");
		shell.setLayout(new FillLayout());
		this.set = set;

		// Main composite
		Composite composite = new Composite(shell, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		composite.setLayout(gridLayout);

		// GL canvas
		domainViewer = new DomainViewer(display, composite, this, set.getNodeStructure());
		GridData visGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 24);
		visGridData.widthHint = 680;
		visGridData.heightHint = 660;
		domainViewer.setLayoutData(visGridData);
		
		// Controls		
		Composite composite_scale = new Composite(composite, SWT.BORDER);
		GridLayout gridLayout_scale = new GridLayout();
		gridLayout_scale.numColumns = 3;
		composite_scale.setLayout(gridLayout_scale);
		GridData controlsGridData = new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1);
		controlsGridData.widthHint = 450;
		composite_scale.setLayoutData(controlsGridData);

		Label label_controls = new Label(composite_scale, SWT.NONE);
		label_controls.setText("Controls");

		Label label_scaleX = new Label(composite_scale, SWT.NONE);
		label_scaleX.setText("    Scale X    ");

		slider_scaleX = new Slider(composite_scale, SWT.NONE);
		slider_scaleX.setValues(50, 0, 100, 5, 5, 5);
		slider_scaleX.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetCampaigns();
			}
		});

		button_showMesh = new Button(composite_scale, SWT.CHECK);
		button_showMesh.setText("Show mesh");
		button_showMesh.setSelection(true);

		Label label_scaleY = new Label(composite_scale, SWT.NONE);
		label_scaleY.setText("    Scale Y    ");

		slider_scaleY = new Slider(composite_scale, SWT.NONE);
		slider_scaleY.setValues(50, 0, 100, 5, 5, 5);
		slider_scaleY.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetCampaigns();
			}
		});

		button_renderUniform = new Button(composite_scale, SWT.CHECK);
		button_renderUniform.setText("Render uniform");
		button_renderUniform.setSelection(true);
		button_renderUniform.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetCampaigns();
			}
		});

		Label label_scaleZ = new Label(composite_scale, SWT.NONE);
		label_scaleZ.setText("    Scale Z    ");

		slider_scaleZ = new Slider(composite_scale, SWT.NONE);
		slider_scaleZ.setValues(50, 0, 100, 5, 5, 5);
		slider_scaleZ.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetCampaigns();
			}
		});
		
		GridData textGridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		String unit = set.getNodeStructure().getUnit("x");
		
		text_labelX = new Text(composite_scale, SWT.BORDER | SWT.SINGLE);
		text_labelX.setText("X ("+unit+")");
		text_labelX.setLayoutData(textGridData);

		Label label_tickX = new Label(composite_scale, SWT.NONE);
		label_tickX.setText("    Ticks    ");

		slider_tickX = new Slider(composite_scale, SWT.NONE);
		slider_tickX.setValues(5, 1, set.getNodeStructure().getX().size(), 5, 1, 5);
		slider_tickX.setLayoutData(textGridData);
		
		text_labelY = new Text(composite_scale, SWT.BORDER | SWT.SINGLE);
		text_labelY.setText("Y ("+unit+")");
		text_labelY.setLayoutData(textGridData);

		Label label_tickY = new Label(composite_scale, SWT.NONE);
		label_tickY.setText("    Ticks    ");

		slider_tickY = new Slider(composite_scale, SWT.NONE);
		slider_tickY.setValues(5, 1, set.getNodeStructure().getY().size(), 5, 1, 5);
		slider_tickY.setLayoutData(textGridData);
		
		text_labelZ = new Text(composite_scale, SWT.BORDER | SWT.SINGLE);
		text_labelZ.setText("Z ("+unit+")");
		text_labelZ.setLayoutData(textGridData);

		Label label_tickZ = new Label(composite_scale, SWT.NONE);
		label_tickZ.setText("    Ticks    ");

		slider_tickZ = new Slider(composite_scale, SWT.NONE);
		slider_tickZ.setValues(5, 1, set.getNodeStructure().getZ().size(), 5, 1, 5);
		slider_tickZ.setLayoutData(textGridData);
		
		// Create tabs and parameter selection
		TabFolder tab = new TabFolder(composite, SWT.NONE);
		GridData tabGridData = new GridData(SWT.FILL, SWT.NONE, true, false, 2, 6);
		tabGridData.widthHint = 450;
		tabGridData.heightHint = 160;
		tab.setLayoutData(tabGridData);
		
		TabItem tab1 = new TabItem(tab, SWT.NONE);
		tab1.setText("  Leak Plume  ");
		table_sensorTable1 = buildLeakTable(tab);
		tab1.setControl(table_sensorTable1);
		
		TabItem tab2 = new TabItem(tab, SWT.NONE);
		tab2.setText("  Detectable Plume  ");
		table_sensorTable2 = buildSensorTable(tab);
		tab2.setControl(table_sensorTable2);
		
		TabItem tab3 = new TabItem(tab, SWT.NONE);
		tab3.setText("  Monitoring Plan  ");
		table_sensorTable3 = buildCampaignTable(tab);
		tab3.setControl(table_sensorTable3);
		
		// Time slider
		int maxTime = set.getNodeStructure().getTimeSteps().size()-1; //Max time
		String timeUnit = set.getNodeStructure().getUnit("times");
		DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
		
		Label time_text = new Label(composite, SWT.NONE);
		time_text.setText("Time = Any        ");
		
		slider_time = new Slider(composite, SWT.NONE);
		// The last time (max time+1, default) should show all time steps for survey
		slider_time.setValues(maxTime+1, 0, maxTime+1, 1, 1, 1);
		//slider_time.setMaximum(maxTime);
		//slider_time.setSelection(maxTime);
		slider_time.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				domainViewer.reset(); //Requires a reset
				domainViewer.resetCampaigns();
				int newTime = slider_time.getSelection();
				if(set.getNodeStructure().getTimeAt(newTime)==null)
					time_text.setText("Time = Any        ");
				else
					time_text.setText("Time = "+df.format(set.getNodeStructure().getTimeAt(newTime))+" "+timeUnit);
			}
		});
		GridData timeGridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		slider_time.setLayoutData(timeGridData);
		
		// Tree
		Label label_campaigns = new Label(composite, SWT.NONE);
		label_campaigns.setText("Campaigns:");
		tree_campaignTree = buildParetoTree(composite);
		GridData scenarioTreeGridData = new GridData(SWT.NONE, SWT.NONE, true, true, 2, 10);
		scenarioTreeGridData.widthHint = 450;
		scenarioTreeGridData.heightHint = 220;
		tree_campaignTree.setLayoutData(scenarioTreeGridData);	

		
		shell.pack();
		if(show) shell.open();
		else this.hide();
		
		shell.addListener(SWT.Close, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				event.doit = false;
				shell.setVisible(false);
			}
		});
	}

	public void show() {
		if(!shell.isVisible()) {
			if(shell != null && !shell.isDisposed()) {
				shell.getDisplay().syncExec(new Runnable() {
					public void run() {
						if(resetTreeRequired) {
							rebuildTree();
						}
						if (!shell.getMinimized())
						{
							shell.setMinimized(true);
						}
						shell.setMinimized(false);
						shell.setActive();
						shell.setVisible(true);
						domainViewer.show();
					}
				});
			}				
		}
	}

	public void hide() {
		if(shell.isVisible()) {
			if(shell != null && !shell.isDisposed()) {
				shell.getDisplay().syncExec(new Runnable() {
					public void run() {
						setVisible(false);
						domainViewer.hide();
					}
				});
			}				
		}
		
	}

	private void setVisible(boolean show) {
		shell.setVisible(true);
	}

	public void dispose() {
		if(shell != null && !shell.isDisposed()) {
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					domainViewer.dispose();
					shell.dispose();
				}
			});
		}
	}

	public String getXLabel() {
		if(text_labelX == null)
			return "X";
		return text_labelX.getText();
	}

	public String getYLabel() {
		if(text_labelY == null)
			return "Y";
		return text_labelY.getText();
	}

	public String getZLabel() {
		if(text_labelZ == null)
			return "Z";
		return text_labelZ.getText();
	}
	
	public Point3i getMeshColor() {
		return new Point3i(120, 120, 120);
	}

	public boolean drawMesh() {
		return button_showMesh != null ? button_showMesh.getSelection() : true;
	}

	public float getScaleX() {
		return slider_scaleX != null ? slider_scaleX.getSelection()/50.0f : 1f;
	}

	public float getScaleY() {
		return slider_scaleY != null ? slider_scaleY.getSelection()/50.0f : 1f;
	}

	public float getScaleZ() {
		return slider_scaleZ != null ? slider_scaleZ.getSelection()/50.0f : 1f;
	}	

	public int getTickX() {
		return slider_tickX != null ? set.getNodeStructure().getX().size()/slider_tickX.getSelection() : 5;
	}

	public int getTickY() {
		return slider_tickY != null ? set.getNodeStructure().getY().size()/slider_tickY.getSelection() : 5;
	}

	public int getTickZ() {
		return slider_tickZ != null ? set.getNodeStructure().getZ().size()/slider_tickZ.getSelection() : 5;
	}
	
	public int getScaleTime() {
		return slider_time != null ? slider_time.getSelection() : set.getNodeStructure().getTimeSteps().size();
	}
	
	public boolean renderUniform() {
		return button_renderUniform != null ? button_renderUniform.getSelection() : false;
	}	

	public List<Float> getTrueCellBoundsX() { //TODO: use the vertex information, not guessing like this
		List<Float> xs = set.getNodeStructure().getX();
		List<Float> cellBoundsX = new ArrayList<Float>();		
		for(int x = 1; x < xs.size(); x++) {
			float half = (xs.get(x)-xs.get(x-1))/2;
			if(x == 1)
				cellBoundsX.add(xs.get(x-1)-half);
			cellBoundsX.add(xs.get(x-1)+half);
			if(x == xs.size()-1) 
				cellBoundsX.add(xs.get(x)+half);
		}
		return cellBoundsX;
	}

	public List<Float> getTrueCellBoundsY() {
		List<Float> ys = set.getNodeStructure().getY();
		List<Float> cellBoundsY = new ArrayList<Float>();
		for(int y = 1; y < ys.size(); y++) {
			float half = (ys.get(y)-ys.get(y-1))/2;
			if(y == 1)
				cellBoundsY.add(ys.get(y-1)-half);
			cellBoundsY.add(ys.get(y-1)+half);
			if(y == ys.size()-1) 
				cellBoundsY.add(ys.get(y)+half);
		}
		return cellBoundsY;
	}

	public List<Float> getTrueCellBoundsZ() {
		List<Float> zs = set.getNodeStructure().getZ();	
		List<Float> cellBoundsZ = new ArrayList<Float>();
		for(int z = 1; z < zs.size(); z++) {
			float half = (Math.abs(zs.get(z))-Math.abs(zs.get(z-1)))/2;
			if(z == 1)
				cellBoundsZ.add(zs.get(z-1)-half);
			cellBoundsZ.add(zs.get(z-1)+half);
			if(z == zs.size()-1) 
					cellBoundsZ.add(zs.get(z)+half);
		}
		return cellBoundsZ;
	}

	public List<Float> getRenderCellBoundsX() {
		List<Float> xs = getTrueCellBoundsX();
		List<Float> ys = getTrueCellBoundsY();
		List<Float> zs = getTrueCellBoundsZ();
		if(renderUniform()) {
			float deltaX = (xs.get(xs.size()-1) - xs.get(0))/xs.size();
			float deltaY = (ys.get(ys.size()-1) - ys.get(0))/ys.size();
			float deltaZ = (zs.get(zs.size()-1) - zs.get(0))/zs.size();
			float max = Math.max(Math.max(deltaX, deltaY), deltaZ);	
			// set them to the max delta
			List<Float> tempXs = new ArrayList<Float>();
			for(int i = 0; i < xs.size(); i++) {
				tempXs.add(i*max);
			}		
			xs = tempXs;
		}
		// scale	
		float scaleX = getScaleX();
		List<Float> tempXs = new ArrayList<Float>();
		for(int i = 0; i < xs.size(); i++) {
			if(renderUniform()) tempXs.add(xs.get(i)*scaleX);
			else tempXs.add((xs.get(i)-xs.get(0))*scaleX);
		}
		xs = tempXs;
		return xs;
	}

	public List<Float> getRenderCellBoundsY() {
		List<Float> xs = getTrueCellBoundsX();
		List<Float> ys = getTrueCellBoundsY();
		List<Float> zs = getTrueCellBoundsZ();
		if(renderUniform()) {
			float deltaX = (xs.get(xs.size()-1) - xs.get(0))/xs.size();
			float deltaY = (ys.get(ys.size()-1) - ys.get(0))/ys.size();
			float deltaZ = (zs.get(zs.size()-1) - zs.get(0))/zs.size();
			float max = Math.max(Math.max(deltaX, deltaY), deltaZ);	
			// set them to the max delta
			List<Float> tempYs = new ArrayList<Float>();
			for(int i = 0; i < ys.size(); i++) {
				tempYs.add(i*max);
			}
			ys = tempYs;
		}
		// scale	
		float scaleY = getScaleY();
		List<Float> tempYs = new ArrayList<Float>();
		for(int i = 0; i < ys.size(); i++) {
			if(renderUniform())tempYs.add(ys.get(i)*scaleY);
			else tempYs.add((ys.get(i)-ys.get(0))*scaleY);
		}
		ys = tempYs;
		return ys;
	}

	public List<Float> getRenderCellBoundsZ() {
		List<Float> xs = getTrueCellBoundsX();
		List<Float> ys = getTrueCellBoundsY();
		List<Float> zs = getTrueCellBoundsZ();
		if(renderUniform()) {
			float deltaX = (xs.get(xs.size()-1) - xs.get(0))/xs.size();
			float deltaY = (ys.get(ys.size()-1) - ys.get(0))/ys.size();
			float deltaZ = (zs.get(zs.size()-1) - zs.get(0))/zs.size();
			float max = Math.max(Math.max(deltaX, deltaY), deltaZ);	
			// set them to the max delta
			List<Float> tempZs = new ArrayList<Float>();
			for(int i = 0; i < zs.size(); i++) {
				tempZs.add(i*max);
			}
			zs = tempZs;	
		}
		// scale	
		float scaleZ = getScaleZ();
		List<Float> tempZs = new ArrayList<Float>();
		for(int i = 0; i < zs.size(); i++) {
			if(renderUniform()) tempZs.add(zs.get(i)*scaleZ);
			else tempZs.add((zs.get(i)-zs.get(0))*scaleZ);
		}
		zs = tempZs;
		if (getZAxialPosition()) {
			Collections.reverse(zs);
		}
		return zs;
	}

	public Point3f getRenderDistance() {
		List<Float> cellBoundsX = getRenderCellBoundsX();
		List<Float> cellBoundsY = getRenderCellBoundsY();
		List<Float> cellBoundsZ = getRenderCellBoundsZ();

		return new Point3f(
				cellBoundsX.get(cellBoundsX.size()-1), 
				cellBoundsY.get(cellBoundsY.size()-1), 
				cellBoundsZ.get(cellBoundsZ.size()-1));
	}
	
	public boolean getZAxialPosition() {
		return set.getNodeStructure().getPositive().equals("down");
	}
	
	public void checkSelectAll() {
		if(leakSelectAll != null) {
			boolean newValue = leakSelectAll.getChecked();
			if(leakOldSelection != newValue) {
				for(LeakTableItem leakItem: leakTableItems.values()) {
					leakItem.getTableItem().setChecked(newValue);
				}
				leakOldSelection = newValue;
			}
		}
		if(sensorSelectAll != null) {
			boolean newValue = sensorSelectAll.getChecked();
			if(sensorOldSelection != newValue) {
				for(SensorTableItem sensorItem: sensorTableItems.values()) {
					sensorItem.getTableItem().setChecked(newValue);
				}
				sensorOldSelection = newValue;
			}
		}
		if(campaignSelectAll != null) {
			boolean newValue = campaignSelectAll.getChecked();
			if(campaignOldSelection != newValue) {
				for(CampaignTableItem campaignItem: campaignTableItems.values()) {
					campaignItem.getTableItem().setChecked(newValue);
				}
				campaignOldSelection = newValue;
			}
		}
	}
	
	////////////////////////////////
	//////// Leak Plume Tab ////////
	////////////////////////////////
	public List<String> getAllLeaksToRender() {
		List<String> leaks = new ArrayList<String>();
		for(String key: leakTableItems.keySet()) {
			leaks.add(key);
		}
		return leaks;
	}
	
	public boolean renderLeak(String leak) {
		if(leakTableItems.containsKey(leak))
			return leakTableItems.get(leak).getTableItem().getChecked();
		return false;
	}
	
	public List<Point3i> getLeakNodes(String leak) {
		List<Point3i> nodes = new ArrayList<Point3i>();
		Set<Integer> nodeNumbers = new HashSet<Integer>();
		for(Scenario scenario : set.getLeakNodes().get(leak).keySet())
			nodeNumbers.addAll(set.getLeakNodes().get(leak).get(scenario));
		for(Integer nodeNumber : nodeNumbers)
			nodes.add(set.getNodeStructure().nodeNumberToIJK(nodeNumber));
		return nodes;
	}
	
	public Point3i getColorOfLeak(String leak) {
		Color color = leakTableItems.get(leak).getColor();
		return new Point3i(color.getRed(), color.getGreen(), color.getBlue());
	}
	
	public float getLeakTransparency(String leak) {
		return leakTableItems.get(leak).getTransparency();
	}
	
	private Table buildLeakTable(Composite composite) {
		final Table table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		final int rowHeight = 12;

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tableColumn_leakType = new TableColumn(table, SWT.CENTER);
		tableColumn_leakType.setText("Include");

		TableColumn tableColumn_transparency = new TableColumn(table, SWT.CENTER);
		tableColumn_transparency.setText("Transparency");

		TableColumn tableColumn_displayColor = new TableColumn(table, SWT.CENTER);
		tableColumn_displayColor.setText("Color");

		TableColumn tableColumn_selectColor = new TableColumn(table, SWT.CENTER);
		tableColumn_selectColor.setText("");

		leakTableItems = new HashMap<String, LeakTableItem>();
		for(String parameter: set.getLeakNodes().keySet()) {
			LeakTableItem leakTableItem = new LeakTableItem(table, rowHeight, parameter);
			leakTableItems.put(parameter, leakTableItem);
		}

		// resize the row height using a MeasureItem listener
		table.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				// height cannot be per row so simply set
				event.height = rowHeight; // lame
				if(event.index % 4 == 1)
					event.width = 40;
				if(event.index % 4 == 2)
					event.width = 40;
				if(event.index % 4 == 0) // label
					event.width = 100;
				if(event.index % 4 == 3) // button
					event.width = 60;
			}
		});

		table.addListener(SWT.EraseItem, new Listener() {
			// Copied from stack overflow, used to stop highlight color
			public void handleEvent(Event event) {
				// Selection:
				event.detail &= ~SWT.SELECTED;
				// Expect: selection now has no visual effect.
				// Actual: selection remains but changes from light blue to white.

				// MouseOver:
				event.detail &= ~SWT.HOT;
				// Expect: mouse over now has no visual effect.
				// Actual: behavior remains unchanged.

				GC gc = event.gc;
				TableItem item = (TableItem) event.item;
				gc.setBackground(item.getBackground(event.index));
				gc.fillRectangle(event.x, event.y, event.width, event.height);
			}
		});
		
		leakSelectAll = new TableItem(table, SWT.CENTER);
		leakSelectAll.setText("Select all");
		leakSelectAll.setChecked(true);

		tableColumn_leakType.pack();
		tableColumn_transparency.pack();
		tableColumn_displayColor.pack();
		tableColumn_selectColor.pack();

		return table;
	}
	
	private class LeakTableItem {

		private TableItem tableItem;
		private float transparency = 0;
		private Color color;

		public LeakTableItem(Table table, int rowHeight, String sensorType) {
			tableItem = new TableItem(table, SWT.CENTER);
			tableItem.setText(sensorType);
			tableItem.setChecked(true);
			tableItem.setText(0, sensorType);
			
			// Transparency slider
			final Slider slider = new Slider(table, SWT.NONE);
			slider.setMaximum(255);
			this.transparency = (60.0f/255.0f);
			slider.setSelection(60);
			slider.computeSize(SWT.DEFAULT, rowHeight);
			slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					transparency = (slider.getSelection()/255.0f);
					domainViewer.reset();
				}
			});
			TableEditor editor2 = new TableEditor(table);
			editor2.grabHorizontal = true;
			editor2.grabVertical = true;
			editor2.verticalAlignment = SWT.BOTTOM;
			editor2.minimumHeight = rowHeight;
			editor2.setEditor(slider, tableItem, 1);
			
			// Color
			int itemCount = table.getItemCount();
			float[][] colors = new float[][]{
				{1.0f, 0.0f, 0.0f, 0.8f}, 
				{0.0f, 0.0f, 1.0f, 0.8f}, 
				{0.0f, 1.0f, 0.0f, 0.03f}, 
				{0.0f, 1.0f, 1.0f, 0.03f}, 
				{1.0f, 1.0f, 0.8f, 0.03f}};
			float[] sensorColor = colors[itemCount%5];
			color = new Color(shell.getDisplay(), (int)sensorColor[0]*255, (int)sensorColor[1]*255, (int)sensorColor[2]*255);
			tableItem.setBackground(2, color);
			
			
			// Color select button
			Button button = new Button(table, SWT.PUSH | SWT.FLAT | SWT.CENTER);	
			button.setText("Select...");			
			button.computeSize(SWT.DEFAULT, rowHeight);
			TableEditor editor = new TableEditor(table);
			editor.grabHorizontal = true;
			editor.grabVertical = true;
			editor.verticalAlignment = SWT.BOTTOM;
			editor.minimumHeight = rowHeight;
			editor.setEditor(button, tableItem, 3);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					ColorDialog dialog = new ColorDialog(shell);
					RGB rgb = dialog.open();
					if (rgb != null) {
						color = new Color(shell.getDisplay(), rgb);
						tableItem.setBackground(2, color);
						// Color change requires reset
						domainViewer.reset();
						domainViewer.resetCampaigns();						
					}
				}
			});   
		}

		public float getTransparency() {
			return transparency;
		}

		public TableItem getTableItem() {
			return tableItem;
		}

		public Color getColor() {
			return color;
		}
	}
	
	////////////////////////////////////////////////
	//////// (Sensors) Detectable Plume Tab ////////
	////////////////////////////////////////////////
	public List<String> getAllSensorsToRender() {
		List<String> sensors = new ArrayList<String>();
		for(String key: sensorTableItems.keySet()) {
			sensors.add(key);
		}
		return sensors;
	}
	
	public boolean renderSensor(String sensor) {
		if(sensorTableItems.containsKey(sensor))
			return sensorTableItems.get(sensor).getTableItem().getChecked();
		return false;
	}
	
	public List<Point3i> getSensorNodes(String sensor) {
		List<Point3i> nodes = new ArrayList<Point3i>();
		Set<Integer> nodeNumbers = new HashSet<Integer>();
		if(set.getSensorSettings()!=null && set.getSensorSettings().containsKey(sensor))
			nodeNumbers.addAll(set.getSensorSettings().get(sensor).getDetectableNodes());
		for(Integer nodeNumber: nodeNumbers)
			nodes.add(set.getNodeStructure().nodeNumberToIJK(nodeNumber));
		return nodes;
	}
	
	public Point3i getColorOfSensor(String sensor) {
		Color color = sensorTableItems.get(sensor).getColor();
		return new Point3i(color.getRed(), color.getGreen(), color.getBlue());
	}
	
	public float getSensorTransparency(String sensor) {
		return sensorTableItems.get(sensor).getTransparency();
	}
	
	private Table buildSensorTable(Composite composite) {
		final Table table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		final int rowHeight = 12;

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tableColumn_sensorType = new TableColumn(table, SWT.CENTER);
		tableColumn_sensorType.setText("Include");

		TableColumn tableColumn_transparency = new TableColumn(table, SWT.CENTER);
		tableColumn_transparency.setText("Transparency");

		TableColumn tableColumn_displayColor = new TableColumn(table, SWT.CENTER);
		tableColumn_displayColor.setText("Color");

		TableColumn tableColumn_selectColor = new TableColumn(table, SWT.CENTER);
		tableColumn_selectColor.setText("");

		sensorTableItems = new HashMap<String, SensorTableItem>();
		if(set.isSensorAliasInitialized()) {
			for(String type: set.getDataTypes()) {
				SensorTableItem sensorTableItem = new SensorTableItem(table, rowHeight, type);
				sensorTableItems.put(type, sensorTableItem);
			}
		}

		// resize the row height using a MeasureItem listener
		table.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				// height cannot be per row so simply set
				event.height = rowHeight; // lame
				if(event.index % 4 == 1)
					event.width = 40;
				if(event.index % 4 == 2)
					event.width = 40;
				if(event.index % 4 == 0) // label
					event.width = 100;
				if(event.index % 4 == 3) // button
					event.width = 60;
			}
		});

		table.addListener(SWT.EraseItem, new Listener() {
			// Copied from stack overflow, used to stop highlight color
			public void handleEvent(Event event) {
				// Selection:
				event.detail &= ~SWT.SELECTED;
				// Expect: selection now has no visual effect.
				// Actual: selection remains but changes from light blue to white.

				// MouseOver:
				event.detail &= ~SWT.HOT;
				// Expect: mouse over now has no visual effect.
				// Actual: behavior remains unchanged.

				GC gc = event.gc;
				TableItem item = (TableItem) event.item;
				gc.setBackground(item.getBackground(event.index));
				gc.fillRectangle(event.x, event.y, event.width, event.height);
			}
		});
		
		if(set.isSensorAliasInitialized()) {
			sensorSelectAll = new TableItem(table, SWT.CENTER);
			sensorSelectAll.setText("Select all");
			sensorSelectAll.setChecked(true);
		}

		tableColumn_sensorType.pack();
		tableColumn_transparency.pack();
		tableColumn_displayColor.pack();
		tableColumn_selectColor.pack();

		return table;
	}
	
	private class SensorTableItem {

		private TableItem tableItem;
		private float transparency = 0;
		private Color color;

		public SensorTableItem(Table table, int rowHeight, String sensorType) {
			tableItem = new TableItem(table, SWT.CENTER);
			tableItem.setText(sensorType);
			tableItem.setChecked(true);
			tableItem.setText(0, set.getSensorAlias(sensorType));
			
			// Transparency slider
			final Slider slider = new Slider(table, SWT.NONE);
			slider.setMaximum(255);
			this.transparency = (60.0f/255.0f);
			slider.setSelection(60);
			slider.computeSize(SWT.DEFAULT, rowHeight);
			slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					transparency = (slider.getSelection()/255.0f);
					domainViewer.reset();
				}
			});
			TableEditor editor2 = new TableEditor(table);
			editor2.grabHorizontal = true;
			editor2.grabVertical = true;
			editor2.verticalAlignment = SWT.BOTTOM;
			editor2.minimumHeight = rowHeight;
			editor2.setEditor(slider, tableItem, 1);
			
			// Color
			int itemCount = table.getItemCount() + set.getLeakNodes().size();
			float[][] colors = new float[][]{
				{1.0f, 0.0f, 0.0f, 0.8f}, 
				{0.0f, 0.0f, 1.0f, 0.8f}, 
				{0.0f, 1.0f, 0.0f, 0.03f}, 
				{0.0f, 1.0f, 1.0f, 0.03f}, 
				{1.0f, 1.0f, 0.8f, 0.03f}};
			float[] sensorColor = colors[itemCount%5];
			color = new Color(shell.getDisplay(), (int)sensorColor[0]*255, (int)sensorColor[1]*255, (int)sensorColor[2]*255);
			tableItem.setBackground(2, color);
			
			
			// Color select button
			Button button = new Button(table, SWT.PUSH | SWT.FLAT | SWT.CENTER);	
			button.setText("Select...");			
			button.computeSize(SWT.DEFAULT, rowHeight);
			TableEditor editor = new TableEditor(table);
			editor.grabHorizontal = true;
			editor.grabVertical = true;
			editor.verticalAlignment = SWT.BOTTOM;
			editor.minimumHeight = rowHeight;
			editor.setEditor(button, tableItem, 3);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					ColorDialog dialog = new ColorDialog(shell);
					RGB rgb = dialog.open();
					if (rgb != null) {
						color = new Color(shell.getDisplay(), rgb);
						tableItem.setBackground(2, color);
						// Color change requires reset
						domainViewer.reset();
						domainViewer.resetCampaigns();						
					}
				}
			});   
		}

		public float getTransparency() {
			return transparency;
		}

		public TableItem getTableItem() {
			return tableItem;
		}

		public Color getColor() {
			return color;
		}
	}
	
	/////////////////////////////////////////////////
	//////// (Campaigns) Monitoring Plan Tab ////////
	/////////////////////////////////////////////////
	public List<String> getAllCampaignsToRender() {
		List<String> campaigns = new ArrayList<String>();
		if(rankedCampaignMap == null)
			return campaigns;
		for(TreeParentCampaignItem rank: rankedCampaignMap.values()) {
			for(TreeCampaignItem campaignItem: rank.children) {
				campaigns.add(campaignItem.getUUID());
			}
		}
		return campaigns;		
	}
	
	public boolean renderCampaign(String sensor) {
		if(campaignTableItems.containsKey(sensor))
			return campaignTableItems.get(sensor).getTableItem().getChecked();
		return false;
	}
	
	public List<Sensor> getSensorsInCampaign(String uuid) {
		if(rankedCampaignMap != null) {
			synchronized(rankedCampaignMap) {
				for(TreeParentCampaignItem rank: rankedCampaignMap.values()) {
					for(TreeCampaignItem campaignItem: rank.children) {
						if(campaignItem.getUUID().equals(uuid))
							return campaignItem.getCampaign().getSensors();
					}
				}
			}
		}
		return new ArrayList<Sensor>();
	}
	
	public Point3i getColorOfCampaign(String sensor) {
		Color color = campaignTableItems.get(sensor).getColor();
		return new Point3i(color.getRed(), color.getGreen(), color.getBlue());
	}
	
	public float getCampaignTransparency(String sensor) {
		return campaignTableItems.get(sensor).getTransparency();
	}
	
	private Table buildCampaignTable(Composite composite) {
		final Table table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		final int rowHeight = 12;

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tableColumn_sensorType = new TableColumn(table, SWT.CENTER);
		tableColumn_sensorType.setText("Include");

		TableColumn tableColumn_transparency = new TableColumn(table, SWT.CENTER);
		tableColumn_transparency.setText("Transparency");

		TableColumn tableColumn_displayColor = new TableColumn(table, SWT.CENTER);
		tableColumn_displayColor.setText("Color");

		TableColumn tableColumn_selectColor = new TableColumn(table, SWT.CENTER);
		tableColumn_selectColor.setText("");

		campaignTableItems = new HashMap<String, CampaignTableItem>();
		if(set.isSensorAliasInitialized()) {
			for(String type: set.getDataTypes()) {
				CampaignTableItem sensorTableItem = new CampaignTableItem(table, rowHeight, type);
				campaignTableItems.put(type, sensorTableItem);
			}
		}

		// resize the row height using a MeasureItem listener
		table.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				// height cannot be per row so simply set
				event.height = rowHeight; // lame
				if(event.index % 4 == 1)
					event.width = 40;
				if(event.index % 4 == 2)
					event.width = 40;
				if(event.index % 4 == 0) // label
					event.width = 100;
				if(event.index % 4 == 3) // button
					event.width = 60;
			}
		});

		table.addListener(SWT.EraseItem, new Listener() {
			// Copied from stack overflow, used to stop highlight color
			public void handleEvent(Event event) {
				// Selection:
				event.detail &= ~SWT.SELECTED;
				// Expect: selection now has no visual effect.
				// Actual: selection remains but changes from light blue to white.

				// MouseOver:
				event.detail &= ~SWT.HOT;
				// Expect: mouse over now has no visual effect.
				// Actual: behavior remains unchanged.

				GC gc = event.gc;
				TableItem item = (TableItem) event.item;
				gc.setBackground(item.getBackground(event.index));
				gc.fillRectangle(event.x, event.y, event.width, event.height);
			}
		});
		
		if(set.isSensorAliasInitialized()) {
			campaignSelectAll = new TableItem(table, SWT.CENTER);
			campaignSelectAll.setText("Select all");
			campaignSelectAll.setChecked(true);
		}

		tableColumn_sensorType.pack();
		tableColumn_transparency.pack();
		tableColumn_displayColor.pack();
		tableColumn_selectColor.pack();
		
		return table;
	}
	
	private class CampaignTableItem {

		private TableItem tableItem;
		private float transparency = 0;
		private Color color;

		public CampaignTableItem(Table table, int rowHeight, String sensorType) {
			tableItem = new TableItem(table, SWT.CENTER);
			tableItem.setText(sensorType);
			tableItem.setChecked(true);
			tableItem.setText(0, set.getSensorAlias(sensorType));
			table.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					if(event.detail == SWT.CHECK){
						domainViewer.reset();
						domainViewer.resetCampaigns();
					}
				}
			});
			
			// Transparency slider
			final Slider slider = new Slider(table, SWT.NONE);
			slider.setMaximum(255);
			this.transparency = (180f/255.0f);
			slider.setSelection(150);
			slider.computeSize(SWT.DEFAULT, rowHeight);
			slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					transparency = (slider.getSelection()/255.0f);
					domainViewer.reset();
					domainViewer.resetCampaigns();
				}
			});
			TableEditor editor2 = new TableEditor(table);
			editor2.grabHorizontal = true;
			editor2.grabVertical = true;
			editor2.verticalAlignment = SWT.BOTTOM;
			editor2.minimumHeight = rowHeight;
			editor2.setEditor(slider, tableItem, 1);
			
			// Color
			int itemCount = table.getItemCount() + set.getLeakNodes().size();
			float[][] colors = new float[][]{
				{1.0f, 0.0f, 0.0f, 0.8f}, 
				{0.0f, 0.0f, 1.0f, 0.8f}, 
				{0.0f, 1.0f, 0.0f, 0.03f}, 
				{0.0f, 1.0f, 1.0f, 0.03f}, 
				{1.0f, 1.0f, 0.8f, 0.03f}};
			float[] sensorColor = colors[itemCount%5];
			color = new Color(shell.getDisplay(), (int)sensorColor[0]*255, (int)sensorColor[1]*255, (int)sensorColor[2]*255);
			tableItem.setBackground(2, color);
			
			// Color select button
			Button button = new Button(table, SWT.PUSH | SWT.FLAT | SWT.CENTER);	
			button.setText("Select...");			
			button.computeSize(SWT.DEFAULT, rowHeight);
			TableEditor editor = new TableEditor(table);
			editor.grabHorizontal = true;
			editor.grabVertical = true;
			editor.verticalAlignment = SWT.BOTTOM;
			editor.minimumHeight = rowHeight;
			editor.setEditor(button, tableItem, 3);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					ColorDialog dialog = new ColorDialog(shell);
					RGB rgb = dialog.open();
					if (rgb != null) {
						color = new Color(shell.getDisplay(), rgb);
						tableItem.setBackground(2, color);
						// Color change requires reset
						domainViewer.reset();
						domainViewer.resetCampaigns();						
					}
				}
			});   
		}

		public float getTransparency() {
			return transparency;
		}

		public TableItem getTableItem() {
			return tableItem;
		}

		public Color getColor() {
			return color;
		}
	}
	
	///////////////////////////////////////
	//////// Campaigns Check Boxes ////////
	///////////////////////////////////////
	
	// Adds one campaign at a time during the iterations
	public void addSingleCampaign(final Campaign campaign) {
		rankedCampaignMap = new TreeMap<Integer, TreeParentCampaignItem>(Collections.reverseOrder());
		rankedCampaignMap.put(0, new TreeParentCampaignItem(0));
		TreeCampaignItem treeItem = new TreeCampaignItem(campaign);
		rankedCampaignMap.get(0).addChild(treeItem);
		clearViewer();
	}
	
	// Adds the best campaigns after the iterations have concluded, typically showing the top 100
	// Top campaigns are ranked by pareto if selected, or else the last 100 iterations
	public void addRankedCampaigns(int count) {
		algorithmFinished = true;
		int total = 0;
		// Add pareto ranked campaigns to the viewer after the algorithm has concluded
		// Only add the top number of campaigns based on input
		rankedCampaignMap = new TreeMap<Integer, TreeParentCampaignItem>();
		for (int rank=0; rank<ResultPrinter.getRankedCampaigns().size(); rank++) {
			rankedCampaignMap.put(rank, new TreeParentCampaignItem(rank));
			for(Campaign campaign : ResultPrinter.getRankedCampaigns().get(rank)) {
				TreeCampaignItem treeItem = new TreeCampaignItem(campaign);
				rankedCampaignMap.get(rank).addChild(treeItem);
				total++;
			}
			// Right now we finish up each pareto rank and then check if we have enough
			if(total>count) {
				clearViewer();
				return;
			}
		}
		clearViewer();
	}
	
	public boolean checkCampaign(String uuid) {
		if(rankedCampaignMap != null) {
			synchronized(rankedCampaignMap) {
				for(TreeParentCampaignItem rank: rankedCampaignMap.values()) {
					for(TreeCampaignItem campaignItem: rank.children) {
						if(campaignItem.getUUID().equals(uuid)) {
							if(campaignItem.getTreeItem(null) != null)
								return campaignItem.getTreeItem(null).getChecked();
						}
					}
				}
			}
		}
		return false;
	}
	
	private void rebuildTree() {		
		if(rankedCampaignMap == null)
			return;
		for(TreeParentCampaignItem rank: rankedCampaignMap.values())
			rank.clear();
		tree_campaignTree.removeAll();
		for(TreeParentCampaignItem rank: rankedCampaignMap.values()) {
			TreeItem level1 = rank.getTreeItem(tree_campaignTree);
			for(TreeCampaignItem child: rank.children) {
				child.getTreeItem(level1);
			}
		}
		domainViewer.resetCampaigns();
	}
	
	public void clearViewer() {
		if(shell != null && !shell.isDisposed()) {
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					rebuildTree();
				}
			});
		}
	}
	
	private Tree buildParetoTree(Composite composite) {
		Tree tree = new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//	domainViewer.resetCampaigns();
				if(e.detail != 32)
					return;
				for(TreeParentCampaignItem rank: rankedCampaignMap.values()) {
					if(rank.getTreeItem(null) != null && rank.getTreeItem(null).equals(e.item)) {
						// select all children
						for(TreeCampaignItem child: rank.children) {
							if(child.getTreeItem(null) != null) {
								child.getTreeItem(null).setChecked(rank.getTreeItem(null).getChecked());
							}
						}
						return;
					} // check the children
					for(TreeCampaignItem child: rank.children) {
						if(child.getTreeItem(null) != null) {
							child.getTreeItem(null).setChecked(child.getTreeItem(null).getChecked());
							return;
						}
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Required but not used
			}
		});
		tree.pack();
		return tree;
	}
	
	private class TreeParentCampaignItem {
		private int paretoRank;
		private TreeItem treeItem;
		private List<TreeCampaignItem> children;
		public TreeParentCampaignItem(int paretoRank) {
			this.paretoRank = paretoRank;
			children = new ArrayList<TreeCampaignItem>();
		}
		public TreeItem getTreeItem(Tree tree) {
			if(treeItem == null) {
				treeItem = new TreeItem(tree, SWT.NONE);
				treeItem.setExpanded(true);
				treeItem.setChecked(!algorithmFinished);
				if(algorithmFinished)
					treeItem.setText("Pareto Rank " + paretoRank + ": (" + children.size() + " campaigns)");
				else
					treeItem.setText("Current Campaign");
			}
			return treeItem;
		}
		public void clear() {
			treeItem = null;
			for(TreeCampaignItem child: children)
				child.clear();
		}
		public void addChild(TreeCampaignItem child) {
			children.add(child);
		}
	}
	
	private class TreeCampaignItem {
		private String name;
		private TreeItem treeItem;
		private String uuid;
		private Campaign campaign;
		public TreeCampaignItem(Campaign campaign) {
			String name = campaign.getSummary(set.getNodeStructure());
			this.campaign = new Campaign(set, campaign);
			this.name = name;
			uuid = UUID.randomUUID().toString();
		}
		private Campaign getCampaign() {
			return campaign;
		}
		public String getUUID() {
			return uuid;
		}
		public TreeItem getTreeItem(TreeItem parent) {
			if(parent != null && treeItem == null) {
				treeItem = new TreeItem(parent, SWT.NONE);
				treeItem.setText(name);
				treeItem.setChecked(!algorithmFinished);
			}
			return treeItem;
		}
		public void clear() {
			treeItem = null;
		}
	}
	
}
