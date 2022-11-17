package wizardPages;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import hdf5Tool.HDF5Interface;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;
import utilities.Constants;
import utilities.Constants.FileType;
import wizardPages.DREAMWizard.STORMData;

/**
 * Using parameters provided from input files, set a criteria and threshold to determine the leak space.
 */

public class Page_LeakDefinition extends DreamWizardPage implements AbstractWizardPage {
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	private Combo addMenu;
	private Button calculateButton;
	private boolean showCalculateButton;
	private Label listNodes;
	private boolean showAdd = true;
	
	// Maps for storing information on the UI
	private Map<String, LeakData> leakData; //parameter, leakData
	
	private boolean isCurrentPage = false;
	public static boolean changed = true;
	
	protected Page_LeakDefinition(final STORMData data) {
		super("Leak Definition");
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
		layout.numColumns = 4;
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
	
	// This class represents each row in the leak definition
	public class LeakData {
		private Trigger leakTrigger;
		private DeltaType leakDeltaType;
		private float leakThreshold;
		
		private Composite leakComposite;
		private Text leakText;
		private Label leakUnit;
		GridData unitData;
		
		public LeakData(String parameter) {
			leakTrigger = Trigger.ABOVE_THRESHOLD;
			leakDeltaType = DeltaType.BOTH;
			leakThreshold = 0;
			
			// Trigger should be relative delta when pressure or ERT
			if(parameter.toLowerCase().contains("pressure") || parameter.toLowerCase().equals("p") || parameter.contains("Electrical Conductivity"))
				leakTrigger = Trigger.RELATIVE_CHANGE;
						
			// Trigger should be maximum threshold when pH
			else if(parameter.toLowerCase().equals("ph"))
				leakTrigger = Trigger.BELOW_THRESHOLD;
		}
		
		public void buildLeakUI(String parameter) {
			// A minus button will allow the user to remove the parameter
			Button minusButton = new Button(container, SWT.PUSH);
			minusButton.setText("-");
			minusButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					leakData.remove(parameter);
					showAdd = true;
					loadPage(false);
				}
		    });
		    
		    // If IAM, we need to set the trigger and threshold
		    if(Constants.fileType==FileType.IAM) {
		    	leakTrigger = data.getSet().getSensorSettings(parameter).getTrigger();
				leakThreshold = data.getSet().getSensorSettings(parameter).getDetectionThreshold();
		    }
		    
		    // The name of the selected parameter
			Label parameterLabel = new Label(container, SWT.LEFT);
			parameterLabel.setText(parameter);
			parameterLabel.setLayoutData(new GridData(SWT.NULL, SWT.CENTER, false, false));
			
			// A drop-down menu where the user can select a threshold
			Combo triggerSelect = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			triggerSelect.add(Trigger.BELOW_THRESHOLD.toString());
			triggerSelect.add(Trigger.ABOVE_THRESHOLD.toString());
			triggerSelect.add(Trigger.RELATIVE_CHANGE.toString());
			triggerSelect.add(Trigger.ABSOLUTE_CHANGE.toString());
			triggerSelect.setText(leakTrigger.toString());
			if(leakTrigger == Trigger.BELOW_THRESHOLD)
				triggerSelect.setToolTipText("Leak when concentration is below value");
			else if(leakTrigger == Trigger.ABOVE_THRESHOLD)
				triggerSelect.setToolTipText("Leak when concentration is above value");
			else if(leakTrigger == Trigger.RELATIVE_CHANGE)
				triggerSelect.setToolTipText("Leak when change from original concentration relative to the initial concentration (%) exceeds value");
			else if(leakTrigger == Trigger.ABSOLUTE_CHANGE)
				triggerSelect.setToolTipText("Leak when change from original concentration exceeds value");
			triggerSelect.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if(((Combo)e.getSource()).getText().equals(Trigger.ABOVE_THRESHOLD.toString())) {
						leakTrigger = Trigger.ABOVE_THRESHOLD;
						leakUnit.setText(data.getSet().getNodeStructure().getUnit(parameter));
					} else if(((Combo)e.getSource()).getText().equals(Trigger.BELOW_THRESHOLD.toString())) {
						leakTrigger = Trigger.BELOW_THRESHOLD;
						leakUnit.setText(data.getSet().getNodeStructure().getUnit(parameter));
					} else if(((Combo)e.getSource()).getText().equals(Trigger.RELATIVE_CHANGE.toString())) {
						leakTrigger = Trigger.RELATIVE_CHANGE;
						leakUnit.setText("%");
					} else {
						leakTrigger = Trigger.ABSOLUTE_CHANGE;
						leakUnit.setText(data.getSet().getNodeStructure().getUnit(parameter));
					}
					changed = true;
					leakComposite.layout();
				}
			});
			
			triggerSelect.setEnabled(Constants.fileType==FileType.IAM ? false : true); //disable select for IAM
			
			// The border gives the appearance of a single component
			GridData compositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			compositeGridData.widthHint = 60; //sets desired width for the composite
			GridLayout leakGridLayout = new GridLayout(2, false);
			leakGridLayout.marginHeight = 1;
			leakGridLayout.marginWidth = 0;
			leakGridLayout.horizontalSpacing = 0;
			GridData textGridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
			textGridData.widthHint = 60; //sets desired width for the text area
			
			leakComposite = new Composite(container, SWT.BORDER);
			leakComposite.setLayoutData(compositeGridData);
			leakComposite.setLayout(leakGridLayout); //Specifies two fields for composite
			leakComposite.setBackground(Constants.white);
			leakComposite.setEnabled(Constants.fileType==FileType.IAM ? false : true); //disable text for IAM
			
			leakText = new Text(leakComposite, SWT.SINGLE | SWT.RIGHT);
			leakText.setLayoutData(textGridData);
			leakText.setText(String.valueOf(leakThreshold));
			leakText.setForeground(Constants.black);
			leakText.setToolTipText(HDF5Interface.getStatisticsString(parameter));
			leakText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					boolean thresholdError = false;
					for(LeakData ld: leakData.values()) {
						if(Constants.isValidFloat(ld.leakText.getText())) { //Valid number
							ld.leakText.setForeground(Constants.black);
							ld.leakThreshold = Float.valueOf(ld.leakText.getText());
						} else { //Not a valid number
							ld.leakText.setForeground(Constants.red);
							thresholdError = true;
						}
					}
					calculateButton.setEnabled(!thresholdError);
					if(leakText.getText().contains("+")) leakDeltaType = DeltaType.INCREASE;
					else if(leakText.getText().contains("-")) leakDeltaType = DeltaType.DECREASE;
					else leakDeltaType = DeltaType.BOTH;
					changed = true;
				}
			});
			leakText.setEnabled(Constants.fileType==FileType.IAM ? false : true); //disable text for IAM
			
			unitData = new GridData(SWT.FILL, SWT.CENTER, false, false);
			leakUnit = new Label(leakComposite, SWT.NONE);
			leakUnit.setLayoutData(unitData);
			if(leakTrigger == Trigger.RELATIVE_CHANGE) {
				leakUnit.setText("%");
			} else {
				leakUnit.setText(data.getSet().getNodeStructure().getUnit(parameter));
			}
			leakUnit.setForeground(Constants.grey);
			leakUnit.setBackground(Constants.white);
			leakUnit.setToolTipText(HDF5Interface.getStatisticsString(parameter));
			leakUnit.addListener(SWT.MouseDown, new Listener() {
				public void handleEvent(Event event) {
					leakText.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
					leakText.setSelection(leakText.toString().length()); //Sets the cursor to the end of the text
				}
			});
			leakUnit.setEnabled(Constants.fileType==FileType.IAM ? false : true); //disable text for IAM
			
		}
		public Trigger getTrigger() {
			return leakTrigger;
		}
		public DeltaType getDeltaType() {
			return leakDeltaType;
		}
		public float getThreshold() {
			return leakThreshold;
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
		if(changed && reset)
			leakData = new HashMap<String, LeakData>();
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Leak Definition");
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
		infoLabel.setText("Define the leak with available parameters. All nodes that exceed the user-defined threshold will be the leak space. "
				+ "The leak size will determine the volume of aquifer degraded per scenario.");
		GridData infoGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, ((GridLayout)container.getLayout()).numColumns, 3);
		infoGridData.widthHint = container.getSize().x - 20;
		infoLabel.setLayoutData(infoGridData);
		
		//Headers
		new Label(container, SWT.NULL);
		Image questionMark = new Image(container.getDisplay(), getClass().getResourceAsStream("/QuestionMark.png"));
		
		Composite parameterComposite = new Composite(container, SWT.NULL);
		parameterComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		parameterComposite.setLayout(new GridLayout(2, false));
		Label parameterLabel = new Label(parameterComposite, SWT.LEFT);
		parameterLabel.setText("Parameter");
		parameterLabel.setFont(Constants.boldFontSmall);
		CLabel parameterQ = new CLabel(parameterComposite, SWT.NULL);
		parameterQ.setImage(questionMark);
		parameterQ.setBottomMargin(0);
		parameterQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Parameter Help", "Parameters are read in from the input H5 or IAM files and "
						+ "represent a value at each model node and timestep. Multiple parameters can be selected to define the leak, taking the "
						+ "union of the selected parameters. The leak space is all the model nodes that exceed the defined type and threshold at any "
						+ "timestep. A carbon capture application might define the leak where CO2 saturation exceeds a maximum contaminent level.");
			}
		});
		
		Composite typeComposite = new Composite(container, SWT.NULL);
		typeComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		typeComposite.setLayout(new GridLayout(2, false));
		Label typeLabel = new Label(typeComposite, SWT.LEFT);
		typeLabel.setText("Leak Type");
		typeLabel.setFont(Constants.boldFontSmall);
		CLabel typeQ = new CLabel(typeComposite, SWT.NULL);
		typeQ.setImage(questionMark);
		typeQ.setBottomMargin(0);
		typeQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Leak Type Help", "The leak type can be set independently for each parameter "
						+ "and defines the type of change that is expected to signify a leak. Above threshold means the leak space is all nodes "
						+ "exceeding the threshold. Below threshold is all nodes less than the threshold. Absolute change is all nodes that change from "
						+ "the original value by the specified threshold. Relative change is all nodes that change relative to the original value "
						+ "as a percent. Both change types can be limited in the positive or negative direction by appending a + or - before "
						+ "the threshold.");
			}
		});
		
		Composite thresholdComposite = new Composite(container, SWT.NULL);
		thresholdComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		thresholdComposite.setLayout(new GridLayout(2, false));
		Label thresholdLabel = new Label(thresholdComposite, SWT.LEFT);
		thresholdLabel.setText("Leak Threshold");
		thresholdLabel.setFont(Constants.boldFontSmall);
		CLabel thresholdQ = new CLabel(thresholdComposite, SWT.NULL);
		thresholdQ.setImage(questionMark);
		thresholdQ.setBottomMargin(0);
		thresholdQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Leak Threshold Help", "The leak value corresponds to the leak type and determines "
						+ "the leak space. The leak space is made of all model nodes that exceed the defined threshold at any scenario. This space "
						+ "determines how much aquifer had degraded at each timestep and scenario, indicating the size of the leak and potential "
						+ "environmental remediation costs.");
			}
		});
				
		// Initialize the LeakData UI map
		if(leakData.size()==0) {
			for(String parameter : data.getSet().getAllPossibleParameters()) {
				if(parameter.toLowerCase().contains("co2") || parameter.toLowerCase().contains("carbon")) { //add a CO2 or carbon parameter by default
					leakData.put(parameter, new LeakData(parameter));
					break;
				}
			}
		}
		if(leakData.size()==0) { //if no CO2 parameter available, simply add the first parameter
			for(String parameter : data.getSet().getAllPossibleParameters()) {
				if(parameter.toLowerCase().contains("gravity")) continue; //skip gravity
				leakData.put(parameter,  new LeakData(parameter));
				break;
			}
		}
		
		// This loops through each leak parameter and creates a row with input values
		showCalculateButton = leakData.size()!=0;//show calculate button if more than 0 values
		for(String parameter: leakData.keySet()) {
			leakData.get(parameter).buildLeakUI(parameter);
		}
		
		// Interface to add another leak parameter
		if(showAdd) { // Make sure there are still parameters to add
			// A plus button will allow the user to add another parameter
			Button addButton = new Button(container, SWT.PUSH);
		    addButton.setText("+");
		    addButton.setToolTipText("Add an additional sensor to the test");
		    addButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					if(!addMenu.getText().equals(""))
						leakData.put(addMenu.getText(), new LeakData(addMenu.getText()));
					if(addMenu.getItemCount() == 1)
						showAdd = false;
					loadPage(false);
					changed = true;
				}
		    });
		    
		    // A drop-down menu where the user can select a new parameter to add
		    addMenu = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		    addMenu.setToolTipText("Add an additional parameter to the leak definition");
		    for(String parameter : data.getSet().getAllPossibleParameters()) {
		    	if(!leakData.containsKey(parameter) && !parameter.toLowerCase().contains("gravity"))
		    		addMenu.add(parameter);
		    }
		    new Label(container, SWT.NULL);
			new Label(container, SWT.NULL);
		}
		
		// Calculate button to determine VAD per scenario
		calculateButton = new Button(container, SWT.PUSH);
		calculateButton.setText("  Calculate Leak  ");
		calculateButton.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 2, 1));
		calculateButton.setEnabled(showCalculateButton);
		calculateButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				calculateLeak();
			}
		});
		
		// List the number of nodes in the leak
		listNodes = new Label(container, SWT.NULL);
		listNodes.setText("");
		listNodes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		new Label(container, SWT.NULL);
		new Label(container, SWT.NULL);
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		DREAMWizard.visLauncher.setEnabled(data.getSet().countLeakNodes()>0);
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if(Constants.skipToEnd || Constants.autoTest) {
			if(Constants.fileType==FileType.H5) {
				//leakData.get("Aqueous CO2 Mass Fraction").leakTrigger = Trigger.ABOVE_THRESHOLD;
				//leakData.get("Aqueous CO2 Mass Fraction").leakThreshold = (float)1;
				leakData.get("pressure").leakThreshold = (float)0.2;
			}
			DREAMWizard.errorMessage.setText("");
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
		}
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		
		// Calculates a leak based on the parameters selected above, updating volume of aquifer degraded
		// This allows the user to advance simply by clicking next
		// Won't recalculate nodes if nothing has changed
		if(changed) {
			calculateLeak();
		}
	}
	
	private void calculateLeak() {
		data.calculateLeakNodes(leakData);
		data.getSet().calculateVolumeDegraded(leakData);
		if(data.getSet().countLeakNodes()==0) {
			listNodes.setText("0 nodes found");
			listNodes.setForeground(Constants.red);
			changed = true;
			DREAMWizard.visLauncher.setEnabled(false);
			errorFound(true, "  No nodes were found for the provided parameters.");
		} else { //if(data.getSet().countLeakNodes()>0) {
			listNodes.setText(data.getSet().countLeakNodes()+" nodes found");
			listNodes.setForeground(Constants.green);
			changed = false;
			DREAMWizard.visLauncher.setEnabled(true);
			errorFound(false, "  No nodes were found for the provided parameters.");
			
			//Store leak configuration to ScenarioSet
			data.getSet().resetLeakSetting();
			for(String parameter: leakData.keySet()) {
				data.getSet().addLeakSetting(parameter, leakData.get(parameter).getTrigger().toString(), String.valueOf(leakData.get(parameter).getThreshold()));
			}
		}
		changed = false;
	}
}
