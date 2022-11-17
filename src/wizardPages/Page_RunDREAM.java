package wizardPages;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import functions.AlgorthmSelection.ALGORITHM;
import functions.ObjectiveSelection.OBJECTIVE;
import gravity.RunPyScript;
import objects.Campaign;
import objects.NodeStructure;
import objects.Scenario;
import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Review the summary of your setup, and choose what kind of run you would like to execute with DREAM.
 * See line 186
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_RunDREAM extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	
	private Button bestTTDTableButton;
	private Button vadButton;
	private Button startOptimizationButton;
	private Button pyPlotBtn;
	private Button showVizButton;
	private Button showGraphButton;
	private Button paretoRankButton;
	private Button detectionMapButton;
	private Button objectiveWeightingButton;
	
	private Text outputFolder;
	private Text runsText;
	private Text iterationsText;
	private Text coolingEqText;
	private Label coolingEqLabel;
	
	private Combo algorithmSelect;
	private Combo graphYAxisSelect;	
	private Combo graphXAxisSelect;
	
	private boolean outputError;
	private boolean runsError;
	private boolean iterationsError;
	private boolean coolingEqError;
	
	private boolean enablePyPlot = false;
	
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private String outputs = getParentFolder() + File.separator + "Results_" + dtf.format(LocalDateTime.now());
	private int runs = 1;
	
	private boolean isCurrentPage = false;
	public static boolean changed = true;
	
	protected Page_RunDREAM(STORMData data) {
		super("Run DREAM");
		this.data = data;
	}
	
	@Override
	public void createControl(final Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event e) {
				sc.setFocus();
			}
		});
		sc.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				int wheelCount = event.count;
				wheelCount = (int) Math.ceil(wheelCount / 3.0f);
				while (wheelCount < 0) {
					sc.getVerticalBar().setIncrement(4);
					wheelCount++;
				}

				while (wheelCount > 0) {
					sc.getVerticalBar().setIncrement(-4);
					wheelCount--;
				}
				sc.redraw();
			}
		});

		container = new Composite(sc, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		layout.numColumns = 2;
		container.setLayout(layout);

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);

	}

	@Override
	public void loadPage(boolean reset) {
		
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		DREAMWizard.convertDataButton.setEnabled(false);
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.nextButton.setVisible(false);
		removeChildren(container);
		
		container.layout();
		NodeStructure structure = data.getSet().getNodeStructure();
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Run DREAM");
		infoLabel1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, ((GridLayout)container.getLayout()).numColumns - 1, 2));
		infoLabel1.setFont(Constants.boldFont);
		
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// We essentially create a local copy of the pdf so that it works when packaged in a JAR
				try {
			        Path tempOutput = Files.createTempFile("user_manual", ".pdf");
			        tempOutput.toFile().deleteOnExit();
			        InputStream is = getClass().getResourceAsStream("/user_manual.pdf");
			        Files.copy(is, tempOutput, StandardCopyOption.REPLACE_EXISTING);
			        Desktop.getDesktop().open(tempOutput.toFile());
			        is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		// Starts the left side of the page
		GridData summaryGD = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 5);
		summaryGD.widthHint = 260;
		summaryGD.heightHint = SWT.FILL;
		
		Text summary = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL );
		summary.setEditable(false);
		summary.setText(data.getSet().toString());
		summary.setLayoutData(summaryGD);
		
		//// Optimization Settings ////
		Group outputGroup = new Group(container, SWT.SHADOW_NONE);
		outputGroup.setText("Output Directory");
		outputGroup.setFont(Constants.boldFontSmall);
		outputGroup.setLayout(new GridLayout(4,false));
		outputGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		// Starts the right side of the page	
		final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
		Button buttonSelectDir = new Button(outputGroup, SWT.PUSH);
		buttonSelectDir.setText("Select Output Directory");
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				directoryDialog.setFilterPath(outputFolder.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					outputFolder.setText(dir);
				}
			}
		});		
	
		outputFolder = new Text(outputGroup, SWT.BORDER | SWT.SINGLE);
		outputFolder.setText(outputs);
		outputFolder.setForeground(Constants.black);
		GridData outputLayout = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		outputLayout.widthHint = 200;
		outputFolder.setLayoutData(outputLayout);
		outputFolder.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				outputError = false;
	            try {
	            	File resultsFolder = new File(((Text)e.getSource()).getText());
	            	outputs = resultsFolder.getCanonicalPath();
	            	((Text)e.getSource()).setForeground(Constants.black);
	            	saveParentFolder(); //Save parent results folder
	            } catch (Exception ex) {
	            	((Text)e.getSource()).setForeground(Constants.red);
	            	outputError = true;
	            }
	            errorFound(outputError, "  Results folder must use valid characters.");
				if (outputError) {
					bestTTDTableButton.setEnabled(false);
					vadButton.setEnabled(false);
					startOptimizationButton.setEnabled(false);
					detectionMapButton.setEnabled(false);
				} else {
					bestTTDTableButton.setEnabled(true);
					vadButton.setEnabled(true);
					if (!runsError && !iterationsError)
						startOptimizationButton.setEnabled(true);
					detectionMapButton.setEnabled(true);
				}
			}
		});
		
		//// Optimization Settings ////
		Group runGroup = new Group(container, SWT.SHADOW_NONE);
		runGroup.setText("Optimization Settings");
		runGroup.setFont(Constants.boldFontSmall);
		runGroup.setLayout(new GridLayout(4,true));
		runGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		//Determine the number of complete runs to conduct
		Label runsLabel = new Label(runGroup, SWT.RIGHT);
		runsLabel.setText("Total Runs");
		runsText = new Text(runGroup, SWT.BORDER | SWT.SINGLE);
		runsText.setText(String.valueOf(runs));
		runsText.setForeground(Constants.black);
		runsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		runsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				runsError = false;
				if(Constants.isValidInt(((Text)e.getSource()).getText()) && Integer.parseInt(((Text)e.getSource()).getText()) > 0) {
					((Text)e.getSource()).setForeground(Constants.black); //Valid number
					runs = Integer.parseInt(((Text)e.getSource()).getText());
				} else { //Not a valid number
					((Text)e.getSource()).setForeground(Constants.red);
					runsError = true;
				}
				errorFound(runsError, "  Runs is not a real positive number.");
				if (iterationsError || runsError || coolingEqError)
					startOptimizationButton.setEnabled(false);
				else if (!outputError)
					startOptimizationButton.setEnabled(true);
			}
		});
		
		//Determine the number of iterations to run
		Label iterationLabel = new Label(runGroup, SWT.RIGHT);
		iterationLabel.setText("Total Iterations (n)");
		iterationsText = new Text(runGroup, SWT.BORDER | SWT.SINGLE);
		iterationsText.setText(String.valueOf(data.getSet().getIterations()));
		iterationsText.setForeground(Constants.black);
		iterationsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		iterationsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				iterationsError = false;
				if(Constants.isValidInt(((Text)e.getSource()).getText()) && Integer.parseInt(((Text)e.getSource()).getText()) > 0) {
					((Text)e.getSource()).setForeground(Constants.black); //Valid number
					data.getSet().setIterations(Integer.parseInt(((Text)e.getSource()).getText()));
				} else { //Not a valid number
					((Text)e.getSource()).setForeground(Constants.red);
					iterationsError = true;
				}
				errorFound(iterationsError, "  Iterations is not a real positive number.");
				if (iterationsError || runsError || coolingEqError)
					startOptimizationButton.setEnabled(false);
				else if (!outputError)
					startOptimizationButton.setEnabled(true);
			}
		});
		
		//Select the function we want for the algorithm
		Label algorithmLabel = new Label(runGroup, SWT.RIGHT);
		algorithmLabel.setText("Select Algorithm");
		algorithmSelect = new Combo(runGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		for(ALGORITHM algorithm: ALGORITHM.values()) {
			algorithmSelect.add(algorithm.toString());
		}
		algorithmSelect.setText(data.getAlgorithm().toString());
		algorithmSelect.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				data.setAlgorithm(((Combo)e.getSource()).getText());
				updateAlgorithms();
			}
		});
		coolingEqLabel = new Label(runGroup, SWT.RIGHT);
		coolingEqLabel.setText("Cooling Equation");
		coolingEqText = new Text(runGroup, SWT.BORDER | SWT.SINGLE);
		coolingEqText.setText(data.getCoolingEq());
		coolingEqText.setForeground(Constants.black);
		String toolTip = "Enter a cooling equation with 'i' representing iteration and " + 
		"'n' representing the total number of iterations. Values should remain between 0 and 1.";
		coolingEqText.setToolTipText(toolTip);
		coolingEqText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		coolingEqText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				//Remove all spaces and trailing math operators
				String equation = ((Text)e.getSource()).getText().replaceAll("\\s", "").replaceAll("[-+*/^(]$", "").toLowerCase();
				coolingEqError = false;
				if(Constants.isValidCoolingEquation(equation)) { //Valid equation
					coolingEqText.setForeground(Constants.black);
					data.setCoolingEq(equation);
				} else { //Not a valid equation
					coolingEqText.setForeground(Constants.red);
					coolingEqError = true;
				}
				errorFound(coolingEqError, "  Cooling equation is not a valid.");
				if (iterationsError || runsError || coolingEqError)
					startOptimizationButton.setEnabled(false);
				else if (!outputError)
					startOptimizationButton.setEnabled(true);
			}
		});
		
		// Use Weighting Button
		objectiveWeightingButton = new Button(runGroup, SWT.CHECK);
		objectiveWeightingButton.setText("Weight Objectives?");
		objectiveWeightingButton.setSelection(Constants.weightObjectives);
		objectiveWeightingButton.setToolTipText("This algorithm compares campaigns to determine which is objectively better. This determines whether "
				+ "objectives are weighted based on scenario weighting values.");
		// Monte Carlo is the only algorithm that currently doesn't compare campaigns
		objectiveWeightingButton.setEnabled(data.getAlgorithm()!=ALGORITHM.MONTE_CARLO);
		objectiveWeightingButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { 
				//required to have this... not sure when it is triggered.
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				Constants.weightObjectives = ((Button)e.getSource()).getSelection();
			}
		});
		new Label(runGroup, SWT.NULL);
		new Label(runGroup, SWT.NULL);
		new Label(runGroup, SWT.NULL);
		updateAlgorithms();
		
		// Select the objectives we want for the algorithm
		// Includes TTD, VAD at Detection, Cost
		Label objectiveLabel = new Label(runGroup, SWT.RIGHT);
		objectiveLabel.setText("Select Objectives");
		// If only one scenario, remove Scenarios Detected as objective
		if(data.getSet().getScenarios().size()==1)
			data.getObjectives().remove(OBJECTIVE.SCENARIOS_DETECTED);
		int count = 0;
		for (OBJECTIVE objective : data.getObjectives()) {
			count++; //To track filler
			if(count==4)
				new Label(runGroup, SWT.NULL);
			Button objectiveButton = new Button(runGroup, SWT.CHECK);
			objectiveButton.setText(objective.toString());
			objectiveButton.setSelection(data.getObjectives().contains(objective));
			objectiveButton.setToolTipText(getObjectiveTooltip(objective));
			// TTD should be disabled as required, all others can be toggled
			objectiveButton.setEnabled(objective!=OBJECTIVE.TTD);
			if(objective!=OBJECTIVE.TTD) {
				objectiveButton.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetDefaultSelected(SelectionEvent e) { 
						//required to have this... not sure when it is triggered.
					}
					@Override
					public void widgetSelected(SelectionEvent e) {
						Boolean selected = ((Button)e.getSource()).getSelection();
						data.setObjectives(selected, objective); //Stored for the algorithm
						graphSelectOptions(selected, objective.toString()); //Adds or removes for the graph options
						updateGraphedObjectives();
					}
				});
			}
		}
		// Helps with layout, ensures that we have enough to fill the column, button on the left
		for(int i=0; i < Math.ceil(data.getObjectives().size()/3.0)*3-data.getObjectives().size(); i++) {
			new Label(runGroup, SWT.NULL);
		}
		
		//Begin the process for determining array optimization
		startOptimizationButton = new Button(runGroup, SWT.BALLOON);
		startOptimizationButton.setSelection(true);
		startOptimizationButton.setText("Start Optimization");
		startOptimizationButton.setBackground(Constants.green);
		//startOptimizationButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		startOptimizationButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				saveParentFolder(); //Save parent results folder
				// Automatically print solution space as long as domain is not huge
				if(data.getSet().getNodeStructure().getTotalNodes() < 100000000)
					printSolutionSpace();
				int ittr = data.getSet().getIterations();
				data.setWorkingDirectory(outputFolder.getText());
				data.getSet().setIterations(ittr);
				RunPyScript py = new RunPyScript();
//				try {
//					py.createPythonGraph(theCSVDirectory, theCSVFile, inputDirectory, theSaveDirectory);
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
				try {
					long startTime = System.currentTimeMillis();
					Constants.random.setSeed(1);
					data.run(runs);
					long time = (System.currentTimeMillis() - startTime) / 1000;
					System.out.println("Iterative procedure took: " + Constants.formatSeconds(time));
					
					//create the dialog box
					MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
					dialog.setText("Completed the Dream Run");
					dialog.setMessage("Dream just completed " + ittr + " iterations in " + Constants.formatSeconds(time) + ". Results can be found at: " + outputFolder.getText());
					enablePyPlot = true;
					loadPage(false);
					if(!Constants.autoTest)
						dialog.open();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
			
		
		pyPlotBtn = new Button(runGroup, SWT.BALLOON);
		pyPlotBtn.setSelection(true);
		pyPlotBtn.setText("Display Python Plots");
		pyPlotBtn.setBackground(Constants.green);
		pyPlotBtn.setEnabled(enablePyPlot);
		pyPlotBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				RunPyScript py = new RunPyScript();
				System.out.println(Constants.selectedDir);
				py.createPythonGraph(outputs, Constants.selectedDir, outputs);
			}
		});
		
		//// Plotting Options ////
		Group plotGroup = new Group(container, SWT.SHADOW_NONE);
		plotGroup.setText("Plotting Options");
		plotGroup.setFont(Constants.boldFontSmall);
		plotGroup.setLayout(new GridLayout(3,true));
		plotGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		// Deselecting the Pareto Rank button will prevent from calculating pareto ranks
		paretoRankButton = new Button(plotGroup, SWT.CHECK);
		paretoRankButton.setText("Calculate Pareto Ranks");
		paretoRankButton.setSelection(Constants.paretoRank);
		paretoRankButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		paretoRankButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { 
				//required to have this... not sure when it is triggered.
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				Constants.paretoRank = ((Button)e.getSource()).getSelection();
			}
		});
		
		// Deselecting the Pareto Rank button will prevent from calculating pareto ranks
		showVizButton = new Button(plotGroup, SWT.CHECK);
		showVizButton.setText("Show Visualization");
		showVizButton.setSelection(Constants.showViz);
		showVizButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		showVizButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { 
				//required to have this... not sure when it is triggered.
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				Constants.showViz = ((Button)e.getSource()).getSelection();
			}
		});
		
		
		// Deselecting the Show Objective Graph button will prevent the visualization from populating (runs fast)
		showGraphButton = new Button(plotGroup, SWT.CHECK);
		showGraphButton.setText("Show Objective Graph");
		showGraphButton.setSelection(Constants.showGraph);
		showGraphButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { 
				//required to have this... not sure when it is triggered.
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				Constants.showGraph = ((Button)e.getSource()).getSelection();
			}
		});
		
		if(Constants.showGraph) {
			// Axis labels
			Label yAxii = new Label(plotGroup, SWT.RIGHT);
			yAxii.setText("Y-Axis Objective");
			Label xAxii = new Label(plotGroup, SWT.RIGHT);
			xAxii.setText("X-Axis Objective");		
			
			// Objective selections
			Label graphSelection = new Label(plotGroup, SWT.RIGHT);
			graphSelection.setText("Initial Graphed Objective");
			graphYAxisSelect = new Combo(plotGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
			graphXAxisSelect = new Combo(plotGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
			for(OBJECTIVE objective : data.getObjectives()) {
				graphYAxisSelect.add(objective.toString());
				graphXAxisSelect.add(objective.toString());
			}
			updateGraphedObjectives();
			
			graphYAxisSelect.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					graphYAxisSelect.select(((Combo)e.getSource()).getSelectionIndex());
					data.setObjectiveAxes(graphYAxisSelect.getText(), graphXAxisSelect.getText());
				}
			});
			graphXAxisSelect.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					graphXAxisSelect.select(((Combo)e.getSource()).getSelectionIndex());
					data.setObjectiveAxes(graphYAxisSelect.getText(), graphXAxisSelect.getText());
				}
			});
		}
		
		
		//// Diagnostic Tools ////
		Group diagnosticGroup = new Group(container, SWT.SHADOW_NONE);
		diagnosticGroup.setText("Diagnostic Tools");
		diagnosticGroup.setFont(Constants.boldFontSmall);
		diagnosticGroup.setLayout(new GridLayout(2,false));
		diagnosticGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		//If a sensor were placed at every node, provide the best possible time to detection
		bestTTDTableButton = new Button(diagnosticGroup, SWT.BALLOON);
		bestTTDTableButton.setSelection(true);
		bestTTDTableButton.setText("Best TTD Possible per Technology");
		bestTTDTableButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				List<List<String>> sensorsToTest = new ArrayList<List<String>>();
				String timeUnit = structure.getUnit("times");
				
				// Run once with each sensor type
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {	
					List<String> justThisOne = new ArrayList<String>();
					justThisOne.add(sensorType);
					sensorsToTest.add(justThisOne);
				}
				// Run once with all sensor types - if there is more than one such type
				if(sensorsToTest.size() > 1) sensorsToTest.add(new ArrayList<String>(data.getSet().getSensorSettings().keySet()));
								
				Map<String, Float> sensorTestedToTTD = new TreeMap<String, Float>();
				Map<String, List<Scenario>> sensorTestedScenariosDetected = new HashMap<String, List<Scenario>>();
				Map<String, Map<Scenario, Float>> ttdPerSensorPerScenarioDetected = new HashMap<String, Map<Scenario, Float>>();
				
				try {
					int count = 0;
					for(List<String> sensors: sensorsToTest) {
						BufferedWriter outputWriter = new BufferedWriter(new FileWriter("Node Numbers"+count+".txt"));
						Campaign campaign = new Campaign(data.getSet(), outputWriter);
						for(String sensorType: sensors) {
							for(int nodeNumber: data.getSet().getSensorSettings().get(sensorType).getDetectableNodes()) {
								List<Integer> detectableNodes = new ArrayList<Integer>();
								detectableNodes.add(nodeNumber);
								campaign.addSensor(detectableNodes, sensorType, data.getSet());
							}
						}
						
						float totalTimeToDetection = 0.0f;
						int detectedScenarios = 0;
						List<Scenario> scenariosDetected = new ArrayList<Scenario>();
						Map<Scenario, Float> ttdForEachDetected = new HashMap<Scenario, Float>();
						for(Scenario scenario: campaign.getObjectiveValues().keySet()) {
							Float timeToDetection = campaign.getObjectiveValue(OBJECTIVE.TTD, false, null);
							detectedScenarios++;
							totalTimeToDetection += timeToDetection;
							scenariosDetected.add(scenario);
							ttdForEachDetected.put(scenario, timeToDetection);
						}
						
						String sensorTested = sensors.size() == 1 ? sensors.get(0) : "Any";
						sensorTestedToTTD.put(sensorTested, (totalTimeToDetection/detectedScenarios));
						sensorTestedScenariosDetected.put(sensorTested, scenariosDetected);
						ttdPerSensorPerScenarioDetected.put(sensorTested, ttdForEachDetected);
						count++;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Now write out the results into a CSV file
				StringBuilder text = new StringBuilder();
				
				// Sensors
				text.append("Parameters");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					text.append("," +sensorType);
				}
				// Average TTD in detected scenarios
				text.append("\nAverage TTD for detected scenarios ("+timeUnit+")");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					text.append("," + Constants.percentageFormat.format(sensorTestedToTTD.get(sensorType)));
				}
				// Detected scenarios
				text.append("\nDetected scenarios");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					int detectedScenarios = sensorTestedScenariosDetected.get(sensorType).size();
					int scenariosTested = data.getSet().getScenarios().size();
					text.append("," + detectedScenarios + " of " + scenariosTested);
				}
				// Weighted percentage of scenarios detected
				text.append("\nDetected scenarios (weighted %)");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					float percent = 0;
					for(Scenario scenario: sensorTestedScenariosDetected.get(sensorType))
						percent += data.getSet().getGloballyNormalizedScenarioWeight(scenario)*100;
					text.append("," + Constants.percentageFormat.format(percent) + "%");
				}
				// Now list best TTD per scenario
				for(Scenario scenario: data.getSet().getScenarios()) {
					text.append("\nBest TTD for "+scenario+" ("+timeUnit+")");
					for(String sensorType: sensorTestedToTTD.keySet()) {
						text.append("," + (ttdPerSensorPerScenarioDetected.get(sensorType).containsKey(scenario) ?
								Constants.percentageFormat.format(ttdPerSensorPerScenarioDetected.get(sensorType).get(scenario)): ""));
					}
				}
				try {
					File outFolder = new File(outputFolder.getText());
					if(!outFolder.exists())
						outFolder.mkdirs();
					File csvOutput = new File(new File(outputFolder.getText()), "best_ttd_table.csv");
					if(!csvOutput.exists())
						csvOutput.createNewFile();
					FileUtils.writeStringToFile(csvOutput, text.toString());
					Desktop.getDesktop().open(csvOutput);
				} catch (IOException e) {		
					JOptionPane.showMessageDialog(null, "Could not write to best_ttd_table.csv, make sure the file is not currently open");
					e.printStackTrace();
				}
			}	       
		});	
		
		//Volume of aquifer degraded
		vadButton = new Button(diagnosticGroup, SWT.BALLOON);
		vadButton.setSelection(true);
		vadButton.setText("Volume of Aquifer Degraded");
		vadButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				saveParentFolder(); //Save parent results folder
				
				HashMap<Float, Float> averages = data.getSet().getAverageVolumeDegradedAtTimesteps();
				HashMap<Float, Float> maximums = data.getSet().getMaxVolumeDegradedAtTimesteps();
				HashMap<Float, Float> minimums = data.getSet().getMinVolumeDegradedAtTimesteps();
				float totalVolume = structure.getPossibleVAD();
				String xUnit = structure.getUnit("x");
				String timeUnit = structure.getUnit("times");
				
				StringBuilder text = new StringBuilder();
				
				// Heading
				text.append("Time ("+timeUnit+"),Average VAD ("+xUnit+"^3),Minimum VAD ("+xUnit+"^3),Maximum VAD ("+xUnit+"^3)");
				text.append(",,Total Possible VAD = " + totalVolume + " " + xUnit + "^3");
				
				ArrayList<Float> years = new ArrayList<Float>(averages.keySet());
				Collections.sort(years);
				
				for(Float time: years){
					text.append("\n" + Constants.percentageFormat.format(time));
					text.append("," + Constants.percentageFormat.format(averages.get(time)));
					text.append("," + Constants.percentageFormat.format(minimums.get(time)));
					text.append("," + Constants.percentageFormat.format(maximums.get(time)));
				}
								
				try {
					File outFolder = new File(outputFolder.getText());
					if(!outFolder.exists())
						outFolder.mkdirs();
					File csvOutput = new File(new File(outputFolder.getText()), "VolumeOfAquiferDegraded.csv");
					if(!csvOutput.exists())
						csvOutput.createNewFile();
					FileUtils.writeStringToFile(csvOutput, text.toString());
					Desktop.getDesktop().open(csvOutput);
				} catch (IOException e) {		
					JOptionPane.showMessageDialog(null, "Could not write to VolumeOfAquiferDegraded.csv, make sure the file is not currently open");
					e.printStackTrace();
				}
			}	       
		});
		
		// This writes out information from the detection map
		detectionMapButton = new Button(diagnosticGroup, SWT.BALLOON);
		detectionMapButton.setSelection(true);
		detectionMapButton.setText("Write Detection Map");
		detectionMapButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				saveParentFolder(); //Save parent results folder
				
				long startTime = System.currentTimeMillis();
				List<Scenario> scenarios = data.getSet().getScenarios(); //All scenarios in the set
				
				// Each parameter should write its own file
				for(String parameter: data.getSet().getDetectionMap().keySet()) {
					// Skip if sensorSettings doesn't contain the parameter
					if(!data.getSet().getSensorSettings().containsKey(parameter.split("_")[0])) continue;
					// Initialize arrays to save statistics across all scenarios
					List<Integer> solutionNodes = new ArrayList<Integer>(data.getSet().getSensorSettings(parameter.split("_")[0]).getDetectableNodes());
					float[][] ttds = new float[solutionNodes.size()][scenarios.size()];
					float[] min = new float[solutionNodes.size()];
					Arrays.fill(min, Float.MAX_VALUE);
					float[] max = new float[solutionNodes.size()];
					Arrays.fill(max, Float.MIN_VALUE);
					float[] avg = new float[solutionNodes.size()];
					float[] count = new float[solutionNodes.size()];
					// Each scenario should be a column
					for(Scenario scenario: data.getSet().getDetectionMap().get(parameter).keySet()) {
						// Each node should be a row
						for(int node: data.getSet().getDetectionMap().get(parameter).get(scenario).keySet()) {
							int i = solutionNodes.indexOf(node);
							float ttd = data.getSet().getDetectionMap().get(parameter).get(scenario).get(node);
							ttds[i][scenarios.indexOf(scenario)] = ttd;
							if(ttd < min[i]) min[i] = ttd;
							if(ttd > max[i]) max[i] = ttd;
							avg[i] += ttd; //sum now, average later
							count[i]++;
						}
					}
					// Writing out the header row
					StringBuilder text = new StringBuilder();
					text.append("XYZ,Detecting Scenarios,Min TTD,Avg TTD,Max TTD");
					for(Scenario scenario: data.getSet().getScenarios())
						text.append("," + scenario.toString());
					text.append("\n");
					// Writing out a row for each node
					for(int node: solutionNodes) {
						int i = solutionNodes.indexOf(node);
						Point3f xyz = structure.getXYZFromNodeNumber(node);
						text.append(xyz+","+count[i]/(float)scenarios.size()*100.0+"%,");
						text.append((min[i]==Float.MAX_VALUE ? "" : min[i]) + ",");
						text.append((avg[i]==0 ? "" : avg[i]/count[i]) + ",");
						text.append((max[i]==Float.MIN_VALUE ? "" : max[i]));
						for(int j=0; j<scenarios.size(); j++)
							text.append("," + (ttds[i][j]==0 ? "" : ttds[i][j]));
						text.append("\n");
					}
					// Write out a file for each parameter
					try {
						File outFolder = new File(outputFolder.getText());
						if(!outFolder.exists())
							outFolder.mkdirs();
						File csvOutput = new File(new File(outputFolder.getText()), "DetectionMap_"+parameter+".csv");
						if(!csvOutput.exists())
							csvOutput.createNewFile();
						FileUtils.writeStringToFile(csvOutput, text.toString());
						//Desktop.getDesktop().open(csvOutput);
					} catch (IOException e) {		
						JOptionPane.showMessageDialog(null, "Could not write out the detection map, make sure the file is not currently open");
						e.printStackTrace();
					}
				}
				long time = (System.currentTimeMillis() - startTime) / 1000;
				MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
				dialog.setText("Finished writing the Detection Map");
				dialog.setMessage("DREAM just finished writing the detection map in "+Constants.formatSeconds(time)+". Results can be found at: " + outputFolder.getText());
				dialog.open();
			}
		});
		detectionMapButton.setVisible(Constants.buildDev);
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if(reset && Constants.autoTest) {
			data.getSet().setIterations(1000);
			showGraphButton.setSelection(false);
			startOptimizationButton.notifyListeners(SWT.Selection, new Event());
			DREAMWizard.finishButton.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	
	private void graphSelectOptions(Boolean selected, String option) {
		if(selected) {
			graphYAxisSelect.add(option);
			graphXAxisSelect.add(option);
		} else {
			graphYAxisSelect.remove(option);
			graphXAxisSelect.remove(option);
		}
	}
	
	
	private void printSolutionSpace(){
		StringBuilder text = new StringBuilder();
		Point3i ijk = data.getSet().getNodeStructure().getIJKDimensions();
		text.append("x y z");
		for(String type: data.getSet().getSensorSettings().keySet()) text.append(" \"" + type + "\"");
		for(int k = 1; k <= ijk.getK(); k++) { 			
			for(int j = 1; j <= ijk.getJ(); j++) { 
				for(int i = 1; i <= ijk.getI(); i++) {
					Point3i node = new Point3i(i, j, k);
					int nodeNumber = data.getSet().getNodeStructure().ijkToNodeNumber(node);
					Point3f xyz = data.getSet().getNodeStructure().getXYZFromIJK(node);
					text.append("\n" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ());
					for(String type: data.getSet().getSensorSettings().keySet()){
						String var = ((data.getSet().getSensorSettings().get(type).getDetectableNodes().contains(nodeNumber)) ? "1" : "0");
						text.append(" " + var);
					}
				}
			}
		}
		text.append("\n");
		String ensemble = data.getSet().getScenarioEnsemble();
		
		try{
			File outFolder = new File(outputFolder.getText());
			if(!outFolder.exists())
				outFolder.mkdirs();
			File outFile = new File(new File(outputFolder.getText()), ensemble+"_solutionSpace.txt");
			if(!outFile.exists())
				outFile.createNewFile();
			FileUtils.writeStringToFile(outFile, text.toString());
		} catch (IOException e) {		
			JOptionPane.showMessageDialog(null, "Could not write to "+ensemble+"_solutionSpace.txt, make sure the file is not currently open");
			e.printStackTrace();
		}
	}

	public void convertFile(final File file) throws IOException {

		List<String> lines = FileUtils.readLines(file);
		StringBuffer fileOut = new StringBuffer();
		for(String line: lines) {
			Map<String, String> nodesToReplace = new HashMap<String, String>();
			// If the line contains any node ids, we need to convert them to xyz locations
			String[] groups = line.split("\\(");
			for(String group: groups) {
				String[] individualSensors = group.split(",");
				for(String individualSensor: individualSensors) {
					String[] parts = individualSensor.split(":");
					if(parts.length == 3) {
						int nodeNumber = Integer.parseInt(parts[0].trim());
						Point3f xyz = data.getSet().getNodeStructure().getXYZFromNodeNumber(nodeNumber);
						nodesToReplace.put(parts[0], xyz.toString());
					} 
				}
			}
			String lineOut = line;
			for(String nodeToReplace: nodesToReplace.keySet()) {
				lineOut = lineOut.replaceAll(nodeToReplace, nodesToReplace.get(nodeToReplace));
			}
			fileOut.append(lineOut + "\n");
		}
		System.out.println(fileOut.toString());
	}
	
	// When the user does something that saves files, store directory default in temp
	private void saveParentFolder() {
		File parentFolder = new File(outputs);
		String parentDirectory = parentFolder.getParent();
		try {
			String fileName = "resultsFolder.txt";
			File directorySaveFile = new File(Constants.tempDir, fileName);
			FileUtils.writeStringToFile(directorySaveFile, parentDirectory);
		} catch (IOException e) {
			System.out.println("Warning: Error saving parent results folder");
		}
	}
	
	// Retrieves the parent results folder from temp
	private String getParentFolder() {
		String fileName = "resultsFolder.txt";
		File directorySaveFile = new File(Constants.tempDir, fileName);
		try(FileInputStream inputStream = new FileInputStream(directorySaveFile)) {     
			return IOUtils.toString(inputStream);
		} catch (IOException e1) {
			return Constants.runningJar ? Constants.userDir : Constants.parentDir;
		}
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
	}

	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	@Override
	public void setPageCurrent(final boolean current) {
		isCurrentPage = current;
	}
	
	private void updateAlgorithms() {
		if(data.getAlgorithm()==ALGORITHM.MONTE_CARLO) {
			objectiveWeightingButton.setVisible(false);
		} else {
			objectiveWeightingButton.setVisible(true);
		}
		if(data.getAlgorithm()==ALGORITHM.SIMULATED_ANNEALING) {
			coolingEqLabel.setText("Cooling Equation");
			coolingEqText.setVisible(true);
		} else {
			coolingEqLabel.setText("");
			coolingEqText.setVisible(false);
		}
	}
	
	private void updateGraphedObjectives() {
		if (data.getObjectives().size() > 1) {
			graphYAxisSelect.select(0);
			graphXAxisSelect.select(1);
			data.setObjectiveAxes(graphYAxisSelect.getText(), graphXAxisSelect.getText());
		}
		// In case there is only one objective left, pick for both
		else {
			graphYAxisSelect.select(0);
			graphXAxisSelect.select(0);
			data.setObjectiveAxes(graphYAxisSelect.getText(), graphXAxisSelect.getText());
		}
	}
	
	private String getObjectiveTooltip(OBJECTIVE objective) {
		if(objective==OBJECTIVE.TTD)
			return "Time to detection is a required objective.";
		else if(objective==OBJECTIVE.VAD_AT_DETECTION)
			return "The volume of aquifer degraded at the time of detection.";
		else if(objective==OBJECTIVE.COST)
			return "Cost includes the cost of the monitoring campaign as defined by prior inputs.";
		else if(objective==OBJECTIVE.SCENARIOS_DETECTED)
			return "Percent of scenarios detected by the campaign.";
		return "";
	}
}