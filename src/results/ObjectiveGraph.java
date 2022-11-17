package results;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import functions.ObjectiveSelection.OBJECTIVE;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;

import javax.swing.AbstractAction;
import objects.Campaign;
import utilities.StatisticsTools;
import wizardPages.DREAMWizard.STORMData;

public class ObjectiveGraph extends JFrame {
	// This allows 10 pareto ranks + unranked
	protected final static int PARETO_RANKS = 10;
	
	private static final long serialVersionUID = 1L;

	private STORMData data;

	private OBJECTIVE yAxisObjective;
	private OBJECTIVE xAxisObjective;
	private ArrayList<OBJECTIVE> selectedParetoObjectives;
	private ArrayList<OBJECTIVE> previousParetoObjectives;

	private XYPlot plot;
	private JFreeChart chart;
	private JPanel myControlPanel;
	private JButton recalculateBtn;
	private JLabel paretoRank;
	private JComboBox<String> xAxisDropDown;
	private JComboBox<String> yAxisDropDown;
	private boolean reset;

	private XYSeriesCollection xySeriesCollection;
	// First element of this list will be for unranked, following elements for ranked
	private List<XYSeries> allParetoSeries;
	private List<Color> gradientColors;

	public ObjectiveGraph(STORMData data) {
		paretoRank = new JLabel("0");
		this.data = data;
		this.yAxisObjective = data.getyAxis();
		this.xAxisObjective = data.getxAxis();
		gradientColors = new ArrayList<Color>();
		allParetoSeries = new ArrayList<XYSeries>();
		xySeriesCollection = new XYSeriesCollection();
		selectedParetoObjectives = data.getObjectives();
		previousParetoObjectives = data.getObjectives();
		reset = true;
		for (int i=0; i < PARETO_RANKS+1; i++) {
			allParetoSeries.add(new XYSeries(yAxisObjective + " and " + xAxisObjective));
			xySeriesCollection.addSeries(allParetoSeries.get(i));
		}
		populateGradientColor();
		createScatterPlot();
		myControlPanel = new JPanel();
		createLayout(myControlPanel);
		this.add(myControlPanel, BorderLayout.SOUTH);
	}
	
	public void createScatterPlot() {
		chart = ChartFactory.createScatterPlot(yAxisObjective.toString()+" and "+xAxisObjective.toString(), 
				yAxisObjective.toString()+" ("+getUnits(yAxisObjective)+")",
				xAxisObjective.toString()+" ("+getUnits(xAxisObjective)+")",
				xySeriesCollection, PlotOrientation.HORIZONTAL, true, false, false);
		plot = chart.getXYPlot();
		plot.setBackgroundPaint(new Color(255, 255, 255));
		for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++) {
			plot.getRenderer().setSeriesShape(i, new Ellipse2D.Double(4, 4, 4, 4));
			plot.getRenderer().setSeriesPaint(i, gradientColors.get(i));
		}
		createLegend(false); //Do not include pareto rank colors
		chart.getLegend().setPosition(RectangleEdge.RIGHT);
		ChartPanel panel = new ChartPanel(chart);

		// setContentPane(panel);
		this.add(panel, BorderLayout.CENTER);
		setSize(900, 650);
		// setLocationRelativeTo(null);
		setVisible(true);
		// pack();
		// If we don't dispose, the program doesn't terminate when closed
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	/**
	 * After each iteration, add the campaign to the scatterplot
	 * 
	 * @param run
	 * @param campaign
	 */
	public void addCampaign(int run, final Campaign campaign) {
		XYDataItem point = calculateValues(campaign);
		if(point.getXValue() < 9e30 && point.getYValue() < 9e30)
			allParetoSeries.get(0).add(point);
	}
	
	/**
	 * Once algorithm is complete, update graph with pareto ranks
	 * Also call this function if axis or objectives have changed
	 * 
	 * @param run
	 * @param campaign
	 */
	public void updateGraphData() {
		createLegend(true); //Include pareto rank colors
		// First clear the existing series
		for (int i=0; i < PARETO_RANKS+1; i++) {
			allParetoSeries.get(i).clear();
		}
		// Second populate the unranked
		int size = ResultPrinter.rankedCampaigns.size()-1;
		for (int i=0; i < ResultPrinter.rankedCampaigns.get(size).size(); i++) {
			XYDataItem point = calculateValues(ResultPrinter.rankedCampaigns.get(size).get(i));
			if(point.getXValue() < 9e30 && point.getYValue() < 9e30)
				allParetoSeries.get(0).add(point);
		}
		// Third populate the pareto ranks
		for (int rank=1; rank < ResultPrinter.rankedCampaigns.size(); rank++) {
			allParetoSeries.get(rank).clear();
			for (int i=0; i < ResultPrinter.rankedCampaigns.get(rank-1).size(); i++) {
				XYDataItem point = calculateValues(ResultPrinter.rankedCampaigns.get(rank-1).get(i));
				if(point.getXValue() < 9e30 && point.getYValue() < 9e30)
					allParetoSeries.get(rank).add(point);
			}
		}
		paretoRank.setText(String.valueOf(size));
	}
	
	/**
	 * Based on the objectives the user selects. Populates the X-Axis ArrayList
	 * Objective Value and the Y-Axis ArrayList Objective Value.
	 * Return non-weighted objective values so that values aren't misleading
	 * 
	 * @return
	 */
	private XYDataItem calculateValues(final Campaign theCampaign) {
		float yVal = 0;
		float xVal = 0;
		switch (yAxisObjective.toString()) {
		case "Time to Detection":
			yVal = theCampaign.getObjectiveValue(OBJECTIVE.TTD, false, null);
			break;
		case "Cost":
			yVal = theCampaign.getObjectiveValue(OBJECTIVE.COST, false, null);
			break;
		case "VAD at Detection":
			yVal = theCampaign.getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, false, null);
			break;
		case "Scenarios Detected":
			yVal = theCampaign.getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, false, null);
			break;
		default:
			break;
		}

		switch (xAxisObjective.toString()) {
		case "Time to Detection":
			xVal = theCampaign.getObjectiveValue(OBJECTIVE.TTD, false, null);
			break;
		case "Cost":
			xVal = theCampaign.getObjectiveValue(OBJECTIVE.COST, false, null);
			break;
		case "VAD at Detection":
			xVal = theCampaign.getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, false, null);
			break;
		case "Scenarios Detected":
			xVal = theCampaign.getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, false, null);
			break;
		default:
			break;
		}
		return new XYDataItem(yVal, xVal);
	}

	private void createLayout(final JPanel control) {
		String objectiveList[] = Arrays.stream(data.getObjectives().toArray())
				.map(Object::toString).toArray(String[]::new);
		control.setLayout(new GridBagLayout());
		GridBagConstraints itemConstraints = new GridBagConstraints();
		// GridLayout layout = new GridLayout(2,11);
		// layout.setHgap(25);
		// control.setLayout(layout);

		// Prep Labels to add in the GUI.
		JLabel yObjectiveLabel = new JLabel("Y-Objective");
		itemConstraints.gridx = 0;
		itemConstraints.gridy = 0;
		control.add(yObjectiveLabel, itemConstraints);
		
		JLabel xObjectiveLabel = new JLabel("X-Objective");
		itemConstraints.gridx = 1;
		itemConstraints.gridy = 0;
		control.add(xObjectiveLabel, itemConstraints);

		JLabel objectiveLabel = new JLabel("Pareto Rankings based on the following objectives:");
		itemConstraints.gridx = 2;
		itemConstraints.gridy = 0;
		itemConstraints.gridwidth = data.getObjectives().size();
		control.add(objectiveLabel, itemConstraints);

		JLabel currentRankLabel = new JLabel("Current Rank");
		itemConstraints.gridx = 5 + data.getObjectives().size();
		itemConstraints.gridy = 0;
		itemConstraints.gridwidth = 1;
		control.add(currentRankLabel, itemConstraints);

		// Create Dropdown and then populate them.
		yAxisDropDown = new JComboBox<String>(objectiveList);
		itemConstraints.gridx = 0;
		itemConstraints.gridy = 1;
		itemConstraints.fill = GridBagConstraints.HORIZONTAL;
		control.add(yAxisDropDown, itemConstraints);
		
		xAxisDropDown = new JComboBox<String>(objectiveList);
		itemConstraints.gridx = 1;
		itemConstraints.gridy = 1;
		itemConstraints.fill = GridBagConstraints.HORIZONTAL;
		control.add(xAxisDropDown, itemConstraints);

		for (int i = 0; i < data.getObjectives().size(); i++) {
			if (objectiveList[i].equalsIgnoreCase(xAxisObjective.toString()))
				xAxisDropDown.setSelectedIndex(i);
			if (objectiveList[i].equalsIgnoreCase(yAxisObjective.toString()))
				yAxisDropDown.setSelectedIndex(i);
		}

		setComboListeners(xAxisDropDown, yAxisDropDown);

		/*JButton printParetoRanking = new JButton(new AbstractAction() { //TODO: Do we intend to use this?
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					ResultPrinter.printRankedCampaigns(data, 100); //limit to top 100 campaigns
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});*/
		
		// Start with all objectives checked and added
		// If check box is deselected, remove from pareto objectives
		for (OBJECTIVE objective : data.getObjectives()) {
			JCheckBox obj = new JCheckBox(objective.toString());
			obj.setSelected(true);
			obj.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					JCheckBox checkbox = (JCheckBox) event.getSource();
					if(checkbox.isSelected()) {
						selectedParetoObjectives.add(objective);
					} else {
						selectedParetoObjectives.remove(objective);
					}
					// Now check that pareto objectives have changed
					if(previousParetoObjectives.containsAll(selectedParetoObjectives) && 
							selectedParetoObjectives.containsAll(previousParetoObjectives))
						recalculateBtn.setEnabled(true);
					else
						recalculateBtn.setEnabled(false);
				}
			});
			itemConstraints.gridx = data.getObjectives().indexOf(objective) + 2;
			itemConstraints.gridy = 1;
			control.add(obj, itemConstraints);
		}

		// Recalculate button start disabled, can click once pareto objectives change
		recalculateBtn = new JButton(new AbstractAction("Recalculate") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent event) {
				// Cancel if the user has no objectives selected
				if(selectedParetoObjectives.size()==0) return;
				
				isComponentAvailable(control, false); //disable controls while calculating
				// Recalculate pareto ranks with new objectives
				StatisticsTools.calculateParetoRanks(selectedParetoObjectives, data.getSet());
				updateGraphData();
				isComponentAvailable(control, true); //re-enable controls after calculating
				
				
				// If a graphed objective was removed, update objectives and labels
				updateAxisSelections();
				updateLabels();
				previousParetoObjectives = selectedParetoObjectives;
								
				// Disable button until selected objectives change
				recalculateBtn.setEnabled(false);
			}
		});
		recalculateBtn.setEnabled(false);
		itemConstraints.gridx = data.getObjectives().size() + 3;
		itemConstraints.gridy = 1;
		control.add(recalculateBtn, itemConstraints);
		
		JButton minusBtn = new JButton(new AbstractAction("-") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (!paretoRank.getText().equals("1")) {
					int currentNum = Integer.parseInt(paretoRank.getText().toString()) - 1;
					paretoRank.setText(String.valueOf(currentNum));
					updateGraphData();
				}
			}
		});
		itemConstraints.gridx = 4 + data.getObjectives().size();
		itemConstraints.gridy = 1;
		control.add(minusBtn, itemConstraints);

		itemConstraints.gridx = 5 + data.getObjectives().size();
		itemConstraints.gridy = 1;
		control.add(paretoRank, itemConstraints);

		JButton plusBtn = new JButton(new AbstractAction("+") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (!paretoRank.getText().equals(String.valueOf(ResultPrinter.rankedCampaigns.size()))) {
					int currentNum = Integer.parseInt(paretoRank.getText().toString()) + 1;
					paretoRank.setText(String.valueOf(currentNum));
					updateGraphData();
				}
			}
		});

		itemConstraints.gridx = 6 + data.getObjectives().size();
		itemConstraints.gridy = 1;
		control.add(plusBtn, itemConstraints);
	}

	private void setComboListeners(final JComboBox<?> xCombo, final JComboBox<?> yCombo) {
		yCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent theEvent) {
				if (reset && theEvent.getStateChange() == ItemEvent.SELECTED) {
					yAxisObjective = OBJECTIVE.getObjective(theEvent.getItem().toString());
					updateGraphData();
					updateLabels();
				}
			}
		});
		
		xCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent theEvent) {
				if (reset && theEvent.getStateChange() == ItemEvent.SELECTED) {
					xAxisObjective = OBJECTIVE.getObjective(theEvent.getItem().toString());
					updateGraphData();
					updateLabels();
				}
			}
		});
	}

	private void isComponentAvailable(final Container container, final Boolean isEnabled) {
		Component[] components = container.getComponents();
		for (Component component : components) {
			component.setEnabled(isEnabled);
			if (component instanceof Container) {
				isComponentAvailable((Container) component, isEnabled);
			}
		}
	}

	// First is grey for unranked, then pareto ranks as divergent color color palette
	private void populateGradientColor() {
		gradientColors.add(new Color(127, 127, 127)); //unranked
		gradientColors.add(new Color(165, 0, 38)); //1
		gradientColors.add(new Color(215, 48, 39)); //2
		gradientColors.add(new Color(244, 109, 67)); //3
		gradientColors.add(new Color(253, 174, 97)); //4
		gradientColors.add(new Color(254, 224, 144)); //5
		gradientColors.add(new Color(255, 255, 191)); //6
		gradientColors.add(new Color(171, 217, 233)); //7
		gradientColors.add(new Color(116, 173, 209)); //8
		gradientColors.add(new Color(69, 117, 180)); //9
		gradientColors.add(new Color(49, 54, 149)); //10
	}

	private void createLegend(boolean pareto) {
		Shape shape = new Rectangle(10, 10);
		LegendItemCollection chartLegend = new LegendItemCollection();
		if(pareto) {
			for (int i = 1; i < gradientColors.size(); i++) {
				chartLegend.add(new LegendItem("Rank " + i, null, null, null, shape, gradientColors.get(i)));
			}
		}
		chartLegend.add(new LegendItem("Unranked", null, null, null, shape, gradientColors.get(0)));
		plot.setFixedLegendItems(chartLegend);
	}
	
	private void updateAxisSelections() {
		// First, update the selected objectives
		if (selectedParetoObjectives.size()==1) { //only one objective
			yAxisObjective = selectedParetoObjectives.get(0);
			xAxisObjective = selectedParetoObjectives.get(0);
		} else { //more than one objective
			// If y-axis was removed
			if (!selectedParetoObjectives.contains(yAxisObjective)) {
				for (OBJECTIVE objective : selectedParetoObjectives) {
					if (!objective.equals(xAxisObjective)) { //compare to x-axis objective
						yAxisObjective = objective;
					}
				}
			}
			// If x-axis was removed
			if (!selectedParetoObjectives.contains(xAxisObjective)) {
				for (OBJECTIVE objective : selectedParetoObjectives) {
					if (!objective.equals(yAxisObjective)) { //compare to y-axis objective
						xAxisObjective = objective;
					}
				}
			}
		}
		// Don't accidentally trigger a graph update each time the dropdown options are changed
		reset = false;
		// Empty the combo boxes, repopulate, then set selected
		yAxisDropDown.removeAllItems();
		xAxisDropDown.removeAllItems();
		for (OBJECTIVE objective : selectedParetoObjectives) {
			yAxisDropDown.addItem(objective.toString());
			xAxisDropDown.addItem(objective.toString());
		}
		yAxisDropDown.setSelectedItem(yAxisObjective.toString());
		xAxisDropDown.setSelectedItem(xAxisObjective.toString());
		
		reset = true;
	}
	
	private void updateLabels() {
		// Update title and labels
		chart.setTitle(yAxisObjective + " and " + xAxisObjective);
		plot.getDomainAxis().setLabel(yAxisObjective.toString()+" ("+getUnits(yAxisObjective)+")");
		plot.getRangeAxis().setLabel(xAxisObjective.toString()+" ("+getUnits(xAxisObjective)+")");
	}

	private String getUnits(OBJECTIVE objective) {
		if(objective==OBJECTIVE.TTD)
			return data.getSet().getNodeStructure().getUnit("times");
		else if(objective==OBJECTIVE.COST)
			return data.getSet().getCostUnit();
		else if(objective==OBJECTIVE.VAD_AT_DETECTION)
			return data.getSet().getNodeStructure().getUnit("x")+"^3";
		else if(objective==OBJECTIVE.SCENARIOS_DETECTED)
			return "%";
		return "";
	}

}