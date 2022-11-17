package wizardPages;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import utilities.Constants;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import objects.Scenario;
import wizardPages.DREAMWizard.STORMData;

/**
 * Select which scenarios should be considered and give them relative weights.
 */

public class Page_ScenarioWeighting extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	private Text equationText;
	private String weightEquation;
	private Group scenarioGroup;
	
	// Maps for storing information on the UI
	private Map<Scenario, ScenarioData> scenarioData; //scenario, ScenarioWeight
	
	private boolean isCurrentPage = false;
	public static boolean changed = true;
	
	protected Page_ScenarioWeighting(final STORMData data) {
		super("Scenario Weighting");
		this.data = data;
	}
	
	@Override
	public void createControl(final Composite parent) {
		
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.getVerticalBar().setIncrement(20);
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
		layout.horizontalSpacing = 8;
		layout.verticalSpacing = 8;
		layout.numColumns = 5;
		container.setLayout(layout);
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	
	@Override
	public void setPageCurrent(final boolean current) {
		isCurrentPage = current;
	}
	
	// This class represents each row in the scenario weighting
	public class ScenarioData {
		boolean isIncluded;
		float vadCalc;
		float varA;
		float varB;
		float weight;
		
		Label vadCalcLabel;
		Text varAText;
		Text varBText;
		Label weightLabel;
		
		public ScenarioData() {
			isIncluded = true;
			varA = 1;
			varB = 1;
			weight = 0;
		}
		
		public void buildScenarioUI(Scenario scenario) {			
			// Include Button
			Button includeButton = new Button(scenarioGroup, SWT.CHECK);
			includeButton.setText(scenario.toString());
			includeButton.setSelection(isIncluded);
			//Add a listener for actions with the check box
			includeButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { 
					//required to have this... not sure when it is triggered.
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					isIncluded = ((Button)e.getSource()).getSelection();
					vadCalcLabel.setEnabled(isIncluded);
					varAText.setEnabled(isIncluded);
					varBText.setEnabled(isIncluded);
					weightLabel.setEnabled(isIncluded);
					
					//What if errors are negated when parameters are unchecked?
					//We have to search through all possible errors to see if any are negated
					boolean checkError = true;
					boolean varAError = false;
					boolean varBError = false;
					
					for(ScenarioData sd: scenarioData.values()) {
						if(!sd.isIncluded) continue; //Skip unchecked parameters
						else checkError = false;
						// Variable A
						if(!Constants.isValidFloat(sd.varAText.getText()))
							varAError = true;
						// Variable B
						if(!Constants.isValidFloat(sd.varBText.getText()))
							varBError = true;
					}
					errorFound(checkError, "  Must select at least one scenario.");
					errorFound(varAError, "  Variable A is not a real number.");
					errorFound(varBError, "  Variable B is not a real number.");
					changed = true;
				}
			});
			
			// Volume of aquifer degraded
			vadCalc = data.getSet().getTotalVolumeDegraded(scenario);
			String xUnit = data.getSet().getNodeStructure().getUnit("x");
			vadCalcLabel = new Label(scenarioGroup, SWT.NULL);
			vadCalcLabel.setText(Float.toString(vadCalc)+" "+xUnit+"^3");
			vadCalcLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			// Variable A Input
			varAText = new Text(scenarioGroup, SWT.BORDER | SWT.SINGLE);
			varAText.setText(Constants.decimalFormat.format(varA));
			varAText.setForeground(Constants.black);
			varAText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			varAText.addModifyListener(new ModifyListener(){
				@Override
				public void modifyText(ModifyEvent e) {
					varAText = ((Text)e.getSource());
					boolean varAError = false;
					for(ScenarioData sd: scenarioData.values()) {
						if(!sd.isIncluded) continue; //Skip unchecked scenarios
						if(Constants.isValidFloat(sd.varAText.getText())) {//Valid number
							sd.varA = Float.valueOf(sd.varAText.getText());
							sd.varAText.setForeground(Constants.black);
							sd.weight = Constants.evaluateWeightExpression(weightEquation, sd.vadCalc, sd.varA, sd.varB).floatValue();
							sd.weightLabel.setText(Constants.percentageFormat.format(sd.weight));
						} else { //Not a valid number
							sd.varAText.setForeground(Constants.red);
							varAError = true;
						}
					}
					errorFound(varAError, "  Variable A is not a real number.");
				}
			});
			
			// Variable B Input
			varBText = new Text(scenarioGroup, SWT.BORDER | SWT.SINGLE);
			varBText.setText(Constants.decimalFormat.format(varB));
			varBText.setForeground(Constants.black);
			varBText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			varBText.addModifyListener(new ModifyListener(){
				@Override
				public void modifyText(ModifyEvent e) {
					varBText = ((Text)e.getSource());
					boolean varBError = false;
					for(ScenarioData sd: scenarioData.values()) {
						if(!sd.isIncluded) continue; //Skip unchecked scenarios
						if(Constants.isValidFloat(sd.varBText.getText())) {//Valid number
							sd.varB = Float.valueOf(sd.varBText.getText());
							sd.varBText.setForeground(Constants.black);
							sd.weight = Constants.evaluateWeightExpression(weightEquation, sd.vadCalc, sd.varA, sd.varB).floatValue();
							sd.weightLabel.setText(Constants.percentageFormat.format(sd.weight));
						} else { //Not a valid number
							sd.varBText.setForeground(Constants.red);
							varBError = true;
						}
					}
					errorFound(varBError, "  Variable B is not a real number.");
				}
			});
			
			// Final Weight
			weight = Constants.evaluateWeightExpression(weightEquation, vadCalc, varA, varB).floatValue();
			weightLabel = new Label(scenarioGroup, SWT.NULL);
			weightLabel.setText(Constants.percentageFormat.format(weight));
			weightLabel.setFont(Constants.boldFontSmall);
			weightLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
	}
	
	@Override
	public void loadPage(boolean reset) {
		
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		DREAMWizard.convertDataButton.setEnabled(false);
		DREAMWizard.nextButton.setEnabled(true);
		removeChildren(container);
		
		// Initialize and reset some variables
		if(changed) {
			scenarioData = new HashMap<Scenario, ScenarioData>();
			for(Scenario scenario : data.getSet().getAllScenarios())
				scenarioData.put(scenario, new ScenarioData());
			weightEquation = "v*a*b";
		}
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Scenario Weighting");
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
		
		Label infoLabel = new Label(container, SWT.WRAP );
		infoLabel.setText("Assign weights to each scenario, manually or with an equation. Alternatively, these weights be be set equal if preferred.");
		GridData infoGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, ((GridLayout)container.getLayout()).numColumns, 3);
		infoGridData.widthHint = container.getSize().x - 20;
		infoLabel.setLayoutData(infoGridData);
		
		// Allow the user to input a weight equation
		Label equationLabel = new Label(container, SWT.NULL);
		equationLabel.setText("Weight Equation");
		equationText = new Text(container, SWT.BORDER | SWT.SINGLE);
		equationText.setText(weightEquation);
		equationText.setForeground(Constants.black);
		equationText.setToolTipText("(Optional) Enter a new equation for calculating scenario weights");
		equationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		equationText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e) {
				String equation = ((Text)e.getSource()).getText().replaceAll("\\s", "").replaceAll("[-+*/^(]$", "").toLowerCase();
				boolean equationError = false;
				for(ScenarioData sd: scenarioData.values()) {
					if(!sd.isIncluded) continue; //Skip unchecked scenarios
					if(Constants.isValidWeightEquation(equation)) {//Valid equation
						sd.weight = Constants.evaluateWeightExpression(equation, sd.vadCalc, sd.varA, sd.varB).floatValue();
						sd.weightLabel.setText(Constants.percentageFormat.format(sd.weight));
					} else {
						equationError = true;
					}
				}
				errorFound(equationError, "  Weight equation has errors.");
				if(equationError) {
					equationText.setForeground(Constants.red);
				} else {
					weightEquation = equation;
					equationText.setForeground(Constants.black);
				}
			}
		});
		
		// If the user selects this, scale all weights to 1
		Button scaleWeightButton = new Button(container, SWT.PUSH);
		scaleWeightButton.setText("  Scale Weights  ");
		scaleWeightButton.setLayoutData(new GridData(SWT.RIGHT, SWT.NULL, false, false, 1, 1));
		scaleWeightButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				float maxWeight = 0;
				// First, determine the maximum weight
				for(ScenarioData sd: scenarioData.values()) {
					if(sd.weight > maxWeight) maxWeight = sd.weight;
				}
				// Change the weight equation
				if(maxWeight != 1.0) //No point in dividing by 1
					equationText.setText(equationText.getText()+"/"+maxWeight);
			}
		});
		
		// If the user selects this, all the weight fields reset to equal
		Button resetWeightButton = new Button(container, SWT.PUSH);
		resetWeightButton.setText("  Set Weights Equal  ");
		resetWeightButton.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
		resetWeightButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				weightEquation = "1";
				equationText.setText("1");
			}
		});
		
		// The user can load weights from a file, loads weights into variable A
		// The file should be one scenario per line: scenario, weight (order doesn't matter)
		Button loadFile = new Button(container, SWT.PUSH);
		loadFile.setText("  Load weights  ");
		loadFile.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
		loadFile.addListener(SWT.Selection,  new Listener() {
			@Override
			public void handleEvent(Event art0) {
				try {
					FileDialog fileDialog = new FileDialog(container.getShell());
					fileDialog.setFilterExtensions(new String[] {"*.txt","*.csv"});
					String weightFile = fileDialog.open();
					File weightSave = new File(weightFile);
					BufferedReader fileReader = new BufferedReader(new FileReader(weightSave));
					String line;
					while((line = fileReader.readLine()) != null) {
						// Allows both tab delimited or comma delimited files
						String[] tokens;
						if(line.contains("\t"))
							tokens = line.split("\t");
						else
							tokens = line.split(",");
						for(Scenario scenario: scenarioData.keySet()) {
							if(scenario.toString().equals(tokens[0])) {
								scenarioData.get(scenario).varAText.setText(tokens[1]);
								break;
							}
						}
					}
					fileReader.close();
					equationText.setText("a");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		
		
		//// Scenario Weights ////
		scenarioGroup = new Group(container, SWT.SHADOW_NONE);
		scenarioGroup.setText("Scenario Weights");
		scenarioGroup.setFont(Constants.boldFontSmall);
		scenarioGroup.setLayout(new GridLayout(5,false));
		scenarioGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		
		//Headers
		Image questionMark = new Image(container.getDisplay(), getClass().getResourceAsStream("/QuestionMark.png"));
		
		Composite scenarioComposite = new Composite(scenarioGroup, SWT.NULL);
		scenarioComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		scenarioComposite.setLayout(new GridLayout(2, false));
		Label scenarioLabel = new Label(scenarioComposite, SWT.LEFT);
		scenarioLabel.setText("Scenario");
		scenarioLabel.setFont(Constants.boldFontSmall);
		CLabel scenarioQ = new CLabel(scenarioComposite, SWT.NULL);
		scenarioQ.setImage(questionMark);
		scenarioQ.setBottomMargin(0);
		scenarioQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Scenario Help", "Scenarios names are determined from the input file. "
						+ "Scenarios can be deselected to remove them from all future analysis within DREAM.");
			}
		});
		
		Composite vadComposite = new Composite(scenarioGroup, SWT.NULL);
		vadComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		vadComposite.setLayout(new GridLayout(2, false));
		Label vadLabel = new Label(vadComposite, SWT.LEFT);
		vadLabel.setText("Max Volume of Aquifer Degraded (v)");
		vadLabel.setFont(Constants.boldFontSmall);
		CLabel vadQ = new CLabel(vadComposite, SWT.NULL);
		vadQ.setImage(questionMark);
		vadQ.setBottomMargin(0);
		vadQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Volume of Aquifer Degraded Help", "Volume of aquifer degraded is "
						+ "calculated based on the leak definition from the previous page. v = the sum of (area of each leak node exceeding "
						+ "the threshold x porosity). Porosity is either a constant or can be set individually for each node with the "
						+ "input HDF5 file. The maximum value at any time step can be used as a variable (v) with the scenario weighting "
						+ "equation.");
			}
		});
		
		Composite aComposite = new Composite(scenarioGroup, SWT.NULL);
		aComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		aComposite.setLayout(new GridLayout(2, false));
		Label aLabel = new Label(aComposite, SWT.RIGHT);
		aLabel.setText("Variable A (a)");
		aLabel.setFont(Constants.boldFontSmall);
		CLabel aQ = new CLabel(aComposite, SWT.NULL);
		aQ.setImage(questionMark);
		aQ.setBottomMargin(0);
		aQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Variable A Help", "Variable A can be manually set by the user or "
						+ "can be read in from a CSV file, and can be used as a variable in the weight equation (a). It can represent "
						+ "variables such as: scenario likelihood, potential remediation cost, proximity to an area of concern, etc. "
						+ "Do not both setting this variable if you intend for all weights to be equal.");
			}
		});
		
		Composite bComposite = new Composite(scenarioGroup, SWT.NULL);
		bComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		bComposite.setLayout(new GridLayout(2, false));
		Label bLabel = new Label(bComposite, SWT.RIGHT);
		bLabel.setText("Variable B (b)");
		bLabel.setFont(Constants.boldFontSmall);
		CLabel bQ = new CLabel(bComposite, SWT.NULL);
		bQ.setImage(questionMark);
		bQ.setBottomMargin(0);
		bQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Variable B Help", "Variable B can be manually set by the user or "
						+ "can be read in from a CSV file, and can be used as a variable in the weight equation (b). It can represent "
						+ "variables such as: scenario likelihood, potential remediation cost, proximity to an area of concern, etc. "
						+ "Do not both setting this variable if you intend for all weights to be equal.");
			}
		});
		
		Composite weightComposite = new Composite(scenarioGroup, SWT.NULL);
		weightComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		weightComposite.setLayout(new GridLayout(2, false));
		Label weightLabel = new Label(weightComposite, SWT.RIGHT);
		weightLabel.setText("Weight");
		weightLabel.setFont(Constants.boldFontSmall);
		CLabel weightQ = new CLabel(weightComposite, SWT.NULL);
		weightQ.setImage(questionMark);
		weightQ.setBottomMargin(0);
		weightQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Weight Help", "Weight is calculated based on the equation above "
						+ "which uses the variables for each scenario. The 'Scale Weights' button will maintain the same relative "
						+ "weights while scaling the numbers from 0 to 1, which is recommended when weights are very large. The "
						+ "'Set Weights Equal' button replaces the equation with 1, setting all weights equal.\n\nWhy do we set "
						+ "scenario weights?\nDREAM optimizes monitoring campaigns by testing random sensor placements and "
						+ "iterating towards 'better' solutions based on obectives - weighting scenarios can influence which monitoring "
						+ "campaign is preferred. DREAM can also factor scenario weighting in pareto optimization to rank the top "
						+ "performing monitoring campaigns.");
			}
		});
		
		// A row for each scenario can be used to determine weights
		for(Scenario scenario : data.getSet().getAllScenarios()) {
			scenarioData.get(scenario).buildScenarioUI(scenario);
		}
		
		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		DREAMWizard.visLauncher.setEnabled(true);
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if(Constants.skipToEnd || Constants.autoTest) {
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
		}
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		
		// Calculates a leak based on the parameters selected above, updating volume of aquifer degraded
		// This allows the user to advance simply by clicking next
		if(data.getSet().countLeakNodes()==0)
			errorFound(true, "  No nodes were found for the provided parameter.");
		
		if(changed) {
			//Store scenario weighting information
			data.getSet().getScenarios().clear(); //A list of active scenarios
			data.getSet().getScenarioWeights().clear(); //Weights for active scenarios
			boolean equal = true;
			float firstWeight = scenarioData.get(data.getSet().getAllScenarios().get(0)).weight;
			for(Scenario scenario : data.getSet().getAllScenarios()) {
				if(!scenarioData.get(scenario).isIncluded) continue; //Skip unchecked scenarios
				data.getSet().getScenarios().add(scenario); //Add active scenario
				data.getSet().getScenarioWeights().put(scenario, scenarioData.get(scenario).weight); //Add scenario weight
				if(equal && scenarioData.get(scenario).weight!=firstWeight) //If weights differ, set to false
					equal = false;
			}
			data.getSet().setEqualWeights(equal);
			// Removing scenarios may change detectable nodes space
			DREAMWizard.resetPages(false, false, true, false, false, false, false);
			changed = false;
		}
		
		System.out.println("Number of scenarios = " + data.getSet().getScenarios().size() + " (" + data.getSet().getAllScenarios().size() + " available)");
	}
}
