package wizardPages;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import objects.SensorSetting;
import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page for setting what sensors need to trigger to signify a detection
 * See line 110
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */

public class Page_DetectionCriteria extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	
	private List<DetectionCriteria> testList;
	
	private boolean isCurrentPage = false;
	public static boolean changed = true;
	
	protected Page_DetectionCriteria(STORMData data) {
		super("Detection Criteria");
		this.data = data;	
	}
	
	public class DetectionCriteria {
		
		private Button removeSensorButton;
		private Text minText;
		private Button addSensorButton;
		private Combo addSensorMenu;
		private Button removeTestButton;
		public HashMap<String, Integer> activeTests;
		private HashMap<String, String> activeTestsText;
		
		public DetectionCriteria(HashMap<String, Integer> tests) {
			activeTests = new HashMap<String, Integer>();
			activeTestsText = new HashMap<String, String>();
			for(String sensorName: tests.keySet()) {
				activeTests.put(sensorName, tests.get(sensorName));
				activeTestsText.put(sensorName, Integer.toString(tests.get(sensorName)));
			}
		}
		
		public void buildUI(final int count) {			
			Map<String, SensorSetting> sensors = data.getSet().getSensorSettings();
			
			// Add an "or" between tests
			if(count > 0) {
				new Label(container, SWT.NULL); // Blank filler
				Label orText = new Label(container, SWT.NULL);
				orText.setText("OR");
				orText.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
				new Label(container, SWT.NULL); // Blank filler
			}
			
			// Creates the group that contains all the tests
			Group group = new Group(container, SWT.SHADOW_NONE);
			group.setText("Criteria " + Integer.valueOf(count+1).toString());
			group.setLayout(new GridLayout(3,false));
			group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			
			// Creates the headers for the group
			new Label(group, SWT.NULL); // Blank filler
			Image questionMark = new Image(container.getDisplay(), getClass().getResourceAsStream("/QuestionMark.png"));
			
			Composite sensorComposite = new Composite(group, SWT.NULL);
			sensorComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
			sensorComposite.setLayout(new GridLayout(2, false));
			Label sensorLabel = new Label(sensorComposite, SWT.LEFT);
			sensorLabel.setText("Sensor");
			sensorLabel.setFont(Constants.boldFontSmall);
			CLabel sensorQ = new CLabel(sensorComposite, SWT.NULL);
			sensorQ.setImage(questionMark);
			sensorQ.setBottomMargin(0);
			sensorQ.addListener(SWT.MouseUp, new Listener(){
				@Override
				public void handleEvent(Event event) {
					MessageDialog.openInformation(container.getShell(), "Sensor Help", "All sensors that were defined at the Detection "
							+ "Threshold page are selectable. 'Any Technology' is a wildcard for any of the available sensors.");
				}
			});
			
			Composite minComposite = new Composite(group, SWT.NULL);
			minComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
			minComposite.setLayout(new GridLayout(2, false));
			Label minLabel = new Label(minComposite, SWT.LEFT);
			minLabel.setText("Minimum Sensors to Signify Leak");
			minLabel.setFont(Constants.boldFontSmall);
			CLabel minQ = new CLabel(minComposite, SWT.NULL);
			minQ.setImage(questionMark);
			minQ.setBottomMargin(0);
			minQ.addListener(SWT.MouseUp, new Listener(){
				@Override
				public void handleEvent(Event event) {
					MessageDialog.openInformation(container.getShell(), "Minimum Sensors Help", "Specifies how many unique sensors or "
							+ "surface surveys must exceed the user-defined value to have confidence that a leak occurred. You can create "
							+ "multiples tests with any combination of sensors. To clarify, a full campaign of surface surveys monitoring an "
							+ "area is counted as one sensor, so it is advised not to require more than one surface survey. In most cases, "
							+ "you can leave it at the default where one of any detecting sensor can identify a leak."
							+ "\n\nAs an example, if both a cheap and expensive pressure sensor are available, you may consider creating two "
							+ "criteria - either one expensive sensor or two cheap sensors need to trigger to have confidence in the leak.");
				}
			});
			
			for(final String sensorName: activeTests.keySet()) {
				
				// Button that allows user to remove the sensor
				removeSensorButton = new Button(group, SWT.PUSH);
				removeSensorButton.setText("-");
				removeSensorButton.setToolTipText("Remove this sensor from the test");
				removeSensorButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						activeTests.remove(sensorName);
						activeTestsText.remove(sensorName);
						checkForErrors(); //Check if an errors exist after changes
						data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
						loadPage(false);
					}
			    });
				
				// Label for the sensor
				Label sensorAliasLabel = new Label(group, SWT.LEFT);
				sensorAliasLabel.setText(sensorName);
				sensorAliasLabel.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
				
				// Text area for the minimum to be set
				minText = new Text(group, SWT.BORDER | SWT.SINGLE);
				minText.setText(Integer.toString(activeTests.get(sensorName)));
				minText.setForeground(Constants.black);
				minText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
				minText.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						minText = ((Text)e.getSource());
						
						// Save new value into maps and set red text if necessary
						activeTestsText.put(sensorName, minText.getText());
						try { //The try statement checks if it is a real number
							if(Integer.valueOf(minText.getText()) < 0) {//Also check that the number is positive
								activeTests.put(sensorName, Integer.valueOf(minText.getText()));
								minText.setForeground(Constants.red);
							} else {
								activeTests.put(sensorName, Integer.valueOf(minText.getText()));
								minText.setForeground(Constants.black);
							}
						} catch (Exception ex) {
							minText.setForeground(Constants.red);
						}
						
						checkForErrors(); // Check if an errors exist after changes
						data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
					}
				});
			}
			
			if(activeTests.size() != sensors.keySet().size() + 1) { 
				
				// Button that allows user to add another sensor to the test
				addSensorButton = new Button(group, SWT.PUSH);
				addSensorButton.setText("+");
				addSensorButton.setToolTipText("Add an additional sensor to the test");
				addSensorButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						if(!addSensorMenu.getText().equals("")) {
							String sensorAlias = addSensorMenu.getText();
							activeTests.put(sensorAlias, 1);
							activeTestsText.put(sensorAlias, Integer.toString(1));
							checkForErrors(); //Check if an error exist after changes
							data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
							loadPage(false);
						}
					}
			    });
				
				// Drop down menu to add sensors that aren't added yet
				addSensorMenu = new Combo(group, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
				addSensorMenu.setToolTipText("Add an additional sensor to the test");
				for(String sensor: sensors.keySet()) {
					String sensorAlias = sensors.get(sensor).getAlias();
					if(!activeTests.containsKey(sensorAlias))
						addSensorMenu.add(sensorAlias);
				}
				if(!activeTests.containsKey("Any Technology"))
					addSensorMenu.add("Any Technology");
			} else {
				new Label(group, SWT.NULL); // Blank filler
				new Label(group, SWT.NULL); // Blank filler
			}
			
			// If not the first criteria, allow option to remove the criteria altogether
			if(count > 0) {
				removeTestButton = new Button(group, SWT.PUSH);
				removeTestButton.setText(" Remove Test ");
				removeTestButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 1, 1));
				removeTestButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						int size = testList.size(); //Necessary to avoid concurrent modifications
						for(int i=1; i<size; i++) { //Starts at 1 to skip first test
							if(testList.get(i).removeTestButton.getEnabled()) {
								testList.remove(i);
								break;
							}
						}
						data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
						loadPage(false);
					}
			    });
			}
		}
	}
	
	public void checkForErrors() {
		boolean minError = false;
		boolean negError = false;
		boolean sumError = false;
		for(DetectionCriteria test: testList) {
			int sum = 0;
			for(String sensorName: test.activeTests.keySet()) {
				sum += test.activeTests.get(sensorName);
				if(!Constants.isValidInt(test.activeTestsText.get(sensorName))) //Not a valid number
					minError = true;
				if(test.activeTests.get(sensorName) < 0)
					negError = true;
			}
			if(sum==0)
				sumError = true;
		}
		errorFound(sumError, "  Must have at least one sensor.");
		errorFound(minError, "  Min is not a real number.");
		errorFound(negError, "  Min cannot be negative.");
	}
	
	@Override
	public void createControl(final Composite parent) {
		
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());
		
		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
        sc.setExpandHorizontal(true);
        sc.setExpandVertical(true);
        
        container = new Composite(sc, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 3;
			
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
		DREAMWizard.nextButton.setVisible(true);
		removeChildren(container);
		
		// Initialize and reset some variables
		if(changed) {
			// Copy the Inference Test settings from data.getSet().getInferenceTest() to local
			testList = new ArrayList<DetectionCriteria>();
			for(HashMap<String, Integer> masterActiveTests: data.getSet().getInferenceTest().getActiveTests()) {
				testList.add(new DetectionCriteria(masterActiveTests));
			}
		}
				
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Detection Criteria");
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
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("How many detections are required to have confidence in a leak?");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoLabel.setLayoutData(infoGridData);
		
		// Builds each test into the user interface
		int count = 0;
		for(DetectionCriteria inferenceTest: testList) {
			inferenceTest.buildUI(count);
			count++;
		}
		
		// Button that allows user to add another test
		Button addTestButton = new Button(container, SWT.PUSH);
		addTestButton.setText(" Add a new criteria ");
		addTestButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				HashMap<String, Integer> test = new HashMap<String, Integer>();
				test.put("Any Technology", 1);
				testList.add(new DetectionCriteria(test)); //Save to local class
				data.getSet().getInferenceTest().addActiveTest(test); //Save to InferenceTest class
				loadPage(false);
			}
	    });
		
		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if(reset && (Constants.skipToEnd || Constants.autoTest))
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		changed = false;
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	
	@Override
	public void setPageCurrent(final boolean current) {
		isCurrentPage = current;
	}

}
