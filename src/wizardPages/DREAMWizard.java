package wizardPages;

import hdf5Tool.FileConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import objects.E4DSensorSettings;
import objects.Scenario;
import objects.ScenarioSet;
import objects.SensorSetting;
import objects.SensorSetting.SensorType;
import results.ObjectiveGraph;
import results.ResultPrinter;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import dialog.E4DRunDialog;
import hdf5Tool.HDF5Interface;
import hdf5Tool.IAMInterface;
import utilities.Constants;
import utilities.Point3i;
import utilities.Constants.FileType;
import visualization.DomainVisualization;
import wizardPages.Page_DefineSensors.SensorData;
import wizardPages.Page_LeakDefinition.LeakData;
import functions.Algorithms;
import functions.ObjectiveSelection.OBJECTIVE;
import functions.AlgorthmSelection.ALGORITHM;

/**
 * Parent class of UI, contains information about which pages to add and in what order
 * Also contains main function for launching DREAM
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */

public class DREAMWizard extends Wizard {

	private STORMData data;
	private DomainVisualization domainViewer;
	private WizardDialog dialog;

	public static Label errorMessage;
	public static Button convertDataButton;
	public static Button visLauncher;
	public static Button nextButton;
	public static Button finishButton;
	public static Button cancelButton;
	
	private boolean wasCancelled;
	
	private ObjectiveGraph myGraph;
		
	public DREAMWizard() {
		super();
		setWindowTitle(null);
		setWindowTitle("DREAM Wizard");
		setNeedsProgressMonitor(true);	
		ImageData imgData = new ImageData(getClass().getResourceAsStream("/dream.png"));
		imgData.height = 100;
		this.setDefaultPageImageDescriptor(ImageDescriptor.createFromImageData(imgData));
		this.setTitleBarColor(new RGB(255,255,255));
		data = new STORMData(this);
	}

	public void setDialog(WizardDialog dialog) {
		this.dialog = dialog;		
	}

	public void createViewer(Boolean show) throws FileNotFoundException {
		File file = new File("creation_errors.txt");
		PrintStream out = null;
		closeViewer(); // Close the old viewer
		try {
			// Create a new viewer, this crashes sometimes when it is first called
			out = new PrintStream(file);
			this.domainViewer = new DomainVisualization(Display.getCurrent(), data.getSet(), show);
			file.delete();
		} catch (Exception e) {
			try {
				Thread.sleep(1000);
				this.domainViewer = new DomainVisualization(Display.getCurrent(), data.getSet(), show);	
			} catch (Exception e1) {
				System.out.println("Warning: Error while loading the domain visualization: details in creation_errors.txt.");
				e1.printStackTrace();//to console
				e1.printStackTrace(out);// to file
			}
			e.printStackTrace(out);
		}
		out.close();
	}
	
	private boolean viewerExists() {
		return this.domainViewer != null;
	}
	
	private void closeViewer() {
		if(this.domainViewer != null) {
			this.domainViewer.dispose();
			this.domainViewer = null;
		}
	}
	
	public void hideViewer() {
		if(this.domainViewer != null)
			this.domainViewer.hide();
	}
	
	private void showViewer() {
		if(this.domainViewer != null) 
			this.domainViewer.show();
	}
	
	@Override
	public IWizardPage getNextPage(final IWizardPage current) {

		AbstractWizardPage currentPage = ((AbstractWizardPage)current);
		IWizardPage next = super.getNextPage(current);
		AbstractWizardPage nextPage = ((AbstractWizardPage)next);

		// If we haven't loaded this page yet, load it
		if(nextPage==null || (!currentPage.isPageCurrent() && !nextPage.isPageCurrent())) {
			System.out.println("LOAD: " + currentPage);
			currentPage.loadPage(true);
			return next;
		}
		
		// Already loaded the page, going back to it reload
		else if(!currentPage.isPageCurrent() && nextPage.isPageCurrent()) {
			nextPage.setPageCurrent(false);
			System.out.println("RELOAD: " + currentPage);
			currentPage.loadPage(false);
			return next;
		}

		// Otherwise finalize this page
		System.out.println("COMPLETE " + currentPage);
		try {
			currentPage.completePage();
			if(!errorMessage.getText().equals(""))
				return current; //Stay on page if there is an error message
			return next;
		} catch (Exception e) {
			currentPage.loadPage(false);
			System.out.println("Something went wrong, stay on this page.");
			return current;
		}
	}

	@Override
	public void addPages() {

		addPage(new Page_Welcome());	
		addPage(new Page_InputDirectory(data));
		addPage(new Page_LeakDefinition(data));
		addPage(new Page_ScenarioWeighting(data));
		addPage(new Page_DefineSensors(data));
		addPage(new Page_DetectionCriteria(data));
		addPage(new Page_CampaignSettings(data));
		addPage(new Page_ExcludeLocations(data));
		addPage(new Page_RunDREAM(data));
	}
	
	public static void resetPages(boolean leakDefinition, boolean scenarioWeighting, boolean detectionThreshold,
			boolean detectionCriteria, boolean campaignSettings, boolean excludeLocations, boolean runDREAM) {
		Page_LeakDefinition.changed = leakDefinition;
		Page_ScenarioWeighting.changed = scenarioWeighting;
		Page_DefineSensors.changed = detectionThreshold;
		Page_DetectionCriteria.changed = detectionCriteria;
		Page_CampaignSettings.changed = campaignSettings;
		Page_ExcludeLocations.changed = excludeLocations;
		Page_RunDREAM.changed = runDREAM;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	public static void main(final String[] args) {
		final Display display = Display.getDefault();
		final Shell shell = new Shell(display);
		
		if(!Constants.autoTest && !Constants.skipToEnd) {
			// Pop up the disclaimer, exit on cancel
			MessageBox messageBox = new MessageBox(shell, SWT.OK | SWT.CANCEL );
			messageBox.setMessage("This material was prepared as an account of work sponsored by an agency of the United States Government.  Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any of their employees, nor any jurisdiction or organization that has cooperated in the development of these materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process disclosed, or represents that its use would not infringe privately owned rights."
					+ "\n\nReference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed herein do not necessarily state or reflect those of the United States Government or any agency thereof."
					+ "\n\n\t\t PACIFIC NORTHWEST NATIONAL LABORATORY"
					+ "\n\t\t\t\u0020\u0020\u0020\u0020\u0020\u0020 operated by"
					+ "\n\t\t\t\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020 BATTELLE"
					+ "\n\t\t\t\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020 for the"
					+ "\n\t\t\u0020\u0020\u0020 UNITED STATES DEPARTMENT OF ENERGY"
					+ "\n\t\t\u0020\u0020\u0020\u0020\u0020\u0020\u0020 under Contract DE-AC05-76RL01830"
					);
			messageBox.setText("General Disclaimer");
			int response = messageBox.open();
			if (response == SWT.CANCEL)
				System.exit(0); // Exit if they don't accept
		}

		final DREAMWizard wizard = new DREAMWizard();
		
		WizardDialog.setDefaultImage(new Image(Display.getDefault(), DREAMWizard.class.getResourceAsStream("/icon.png")));
		
		WizardDialog wizardDialog = new WizardDialog(null, wizard) {
			{
				setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE | SWT.MAX | SWT.MIN | SWT.ICON);
			}
			
			@Override
			protected void createButtonsForButtonBar(final Composite parent) {
				
				GridData errorMessageData = new GridData(GridData.HORIZONTAL_ALIGN_END);
				errorMessageData.horizontalSpan = 4;
				errorMessageData.grabExcessHorizontalSpace = true;
				errorMessageData.horizontalAlignment = GridData.FILL;
				errorMessage = new Label(parent, SWT.RIGHT);
				errorMessage.setForeground(display.getSystemColor(SWT.COLOR_RED));
				errorMessage.setLayoutData(errorMessageData);
				
				convertDataButton = new Button(parent, SWT.PUSH); 	
				convertDataButton.setText("Launch Converter");
				convertDataButton.setToolTipText("Convert simulation data to DREAM h5 format"); 	
				convertDataButton.addSelectionListener(new SelectionListener() 
				{ 
					@Override 
					public void widgetSelected(SelectionEvent e) { 
						FileConverter browser = new FileConverter();
						browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						browser.setVisible(true); 
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						FileConverter browser = new FileConverter();
						browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						browser.setVisible(true); 
					} 
				}); 

				visLauncher = new Button(parent, SWT.PUSH); 	
				visLauncher.setText("Launch Visualization"); 	
				visLauncher.addSelectionListener(new SelectionListener() 
				{ 
					@Override 
					public void widgetSelected(SelectionEvent e) { 
						File file = new File("errors_start.txt");
						PrintStream out = null;
						// Since detectable nodes can change if the user selects a different detection criteria, we need to force the reset every time
						try {
							out = new PrintStream(file);  
							wizard.launchVisWindow(true, true);
							file.delete();
						} catch (Exception e1) {
							e1.printStackTrace(out);
						}
						out.close();
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						widgetSelected(e); 
					} 
				}); 


				super.createButtonsForButtonBar(parent);
				nextButton = super.getButton(IDialogConstants.NEXT_ID);

				// Hide the buttons we don't use
				cancelButton = super.getButton(IDialogConstants.CANCEL_ID);	
				finishButton = super.getButton(IDialogConstants.FINISH_ID);
				((GridData)cancelButton.getLayoutData()).exclude = true;
				((GridData)finishButton.getLayoutData()).exclude = true;
			}
		};
 
		wizard.setDialog(wizardDialog);
		wizardDialog.setTitleAreaColor(new RGB(255, 255, 255));//32,62,72));
		wizardDialog.open();
	}

	public void launchVisWindow(final boolean reset, final boolean show) throws FileNotFoundException {
		// If we don't want to reset the vis window and 
		if(!reset && viewerExists()) {
			showViewer();
			return;
		}
		// Otherwise create a new one, this will close the old one of there was one open previously
		try {
			createViewer(show);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class STORMData {
		private ScenarioSet set;
		private Algorithms runner;
		private ALGORITHM algorithm;
		private String coolingEq; //This sets the cooling function for simulated annealing
		private ArrayList<OBJECTIVE> objectives;
		private OBJECTIVE xAxisObjective;
		private OBJECTIVE yAxisObjective;
		private DREAMWizard wizard;
		private ArrayList<Point3i> wells;
		
		public String fileType;
		
		public STORMData(DREAMWizard wizard) {
			this.wizard = wizard;
			set = new ScenarioSet();
			algorithm = ALGORITHM.SIMULATED_ANNEALING;
			coolingEq = "0.01^(i/n)"; //exponential decay by default
			objectives = new ArrayList<OBJECTIVE>();
			objectives.add(OBJECTIVE.TTD);
			objectives.add(OBJECTIVE.COST);
			objectives.add(OBJECTIVE.VAD_AT_DETECTION);
			objectives.add(OBJECTIVE.SCENARIOS_DETECTED);
		}
		
		public void setupScenarioSet(final String directory, final String[] list) {	
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							// We already verified that selected files are of the correct type
							// If the directory has H5 files, handle it this way...
							if(Constants.fileType==FileType.H5) {
								STORMData.this.fileType = "hdf5";
								monitor.beginTask("Loading H5 scenario set", 10);
								
								monitor.subTask("reading Node Structure from first file"); //Load the node structure from the first H5 file
								set.setNodeStructure(HDF5Interface.readNodeStructureH5(directory+File.separator+list[0]));
								monitor.worked(6);
								
								monitor.subTask("reading scenarios from all files"); // Set the scenarios
								set.setupScenarios(HDF5Interface.queryScenarioNamesFromFiles(directory, list));
								monitor.worked(3);
							}
							
							// If the directory has IAM files, handle it this way...
							else if(Constants.fileType==FileType.IAM) {
								STORMData.this.fileType = "iam";
								monitor.beginTask("Loading IAM scenario set", list.length + 2);
								
								monitor.subTask("reading Node Structure from first file");
								set.setNodeStructure(IAMInterface.readNodeStructureIAM(directory+File.separator+list[0]));
								monitor.worked(1);
								
								monitor.subTask("reading scenarios from all files");
								IAMInterface.readIAMFiles(monitor, directory, list, set);
								set.getNodeStructure().setParameters(IAMInterface.getDataTypes()); // Set the data types
								set.setupScenarios(IAMInterface.getScenarios()); // Set the scenarios
								monitor.worked(list.length);
							}
							monitor.subTask("initializing algorithm");
							runner = new Algorithms();
							monitor.subTask("done");
							monitor.worked(1);
							
							if(monitor.isCanceled()) {
								set.getSensorSettings().clear();
								set.getDetectionMap().clear();
								set.getNodeStructure().clear();
							}
						} catch (Exception e) {
							System.out.println("Was the monitor cancelled?\t" + monitor.isCanceled());
						}
					}					
				});
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Loading files was canceled by the user.");
				e.printStackTrace();
			} 
		}
		
		// Called when the user selects the calculate button on the Leak Definition Page
		// Based on selected parameters, we calculate a detection map for the input thresholds
		public void calculateLeakNodes(final Map<String, LeakData> leakData) {
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							// Progress = (number of scenarios * leak parameters) + leak parameters
							int monitorSize = getSet().getAllScenarios().size() * leakData.size() + leakData.size();
							monitor.beginTask("Calculating leak", monitorSize);
							
							// Loop through selected leakData
							set.getLeakNodes().clear();
							for(String parameter : leakData.keySet()) {
								LeakData leak = leakData.get(parameter);
								if(monitor.isCanceled()) break;
								// Generate the detection map based on input thresholds
								// Only do this for H5 variables, IAM is already in detectionMap
								if(fileType=="hdf5") {
									monitor.subTask("Generating detection map from files");
									HDF5Interface.createDetectionMap(monitor, set, parameter, leak.getTrigger(), leak.getDeltaType(), leak.getThreshold(), SensorType.POINT_SENSOR);
								}
								// Add nodes to the leak space per scenario
								monitor.subTask("Adding nodes to the space");
								set.addLeakNodes(parameter, Constants.getSpecificType(parameter, leak.getTrigger(), leak.getDeltaType(), leak.getThreshold(), SensorType.POINT_SENSOR));
								// Calculate the volume of aquifer degraded per scenario and time step
								monitor.subTask("Calculating volume of aquifer degraded");
								monitor.worked(1);
							}
							
							// If the user canceled, clear any added data
							if(monitor.isCanceled()) {
								set.getLeakNodes().clear();
							}
						} catch (Exception e) {
							System.out.println("Was the monitor cancelled?\t" + monitor.isCanceled());
						}
					}
				});
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Dream is out of memory! Select fewer parameters or a better threshold.");
				e.printStackTrace();
			}
		}
		
		public void setupSensors(final ArrayList<SensorData> newSensors, final ArrayList<SensorData> activeSensors) {
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							int monitorSize = getSet().getScenarios().size() * newSensors.size() +1;
							monitor.beginTask("Sensor settings", monitorSize);
							
							// First we generate a TTD matrix based on new selected sensors settings
							// Only do this for H5 variables, IAM is already in detectionMap
							if(data.fileType=="hdf5") {
								for(SensorData sensor: newSensors) {
									if(monitor.isCanceled()) break;
									HDF5Interface.createDetectionMap(monitor, set, sensor.getParameter(), sensor.getTrigger(), sensor.getDeltaType(), sensor.getThreshold(), sensor.getSensorType());
								}
							}
							
							// Last we create a list of detectable nodes from the new detectionMap
							for(SensorData sensor: activeSensors) {
								if(monitor.isCanceled()) break;
								monitor.subTask("calculating detectable nodes: " + sensor.parameter);
								monitor.subTask(sensor.parameter + " - generating a list of detectable nodes");
								set.getSensorSettings(sensor.sensorName).setNodes(set);
							}
							monitor.worked(1);
							
							// If the user canceled, clear any added data
							if(monitor.isCanceled()) {
								for(SensorData sensor: newSensors) {
									set.getDetectionMap().remove(sensor.specificType);
									set.getSensorSettings(sensor.parameter).clearNodes();
								}
							}
							
						} catch (Exception e) {
							System.out.println("Was the monitor cancelled?\t" + monitor.isCanceled());
						}
					}
				});
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Dream is out of memory!  Please reduce your solution space.");
				e.printStackTrace();
			}
		}
		
		public ScenarioSet getSet() {
			return set;
		}
		
		public SensorSetting getSensorSettings(String sensorType) {
			return set.getSensorSettings(sensorType);
		}

		public ALGORITHM getAlgorithm() {
			return algorithm;
		}
		
		public void setAlgorithm(String algorithm) {
			//if(algorithm.equals("Grid Search"))
				//this.algorithm = ALGORITHM.GRID_SEARCH;
			if(algorithm.equals("Monte Carlo"))
				this.algorithm = ALGORITHM.MONTE_CARLO;
			//else if(algorithm.equals("Monte Carlo (Grid)"))
			//	this.algorithm = ALGORITHM.MONTE_CARLO_GRID;
			else if(algorithm.equals("Simulated Annealing"))
				this.algorithm = ALGORITHM.SIMULATED_ANNEALING;
			else if(algorithm.equals("Heuristic"))
				this.algorithm = ALGORITHM.HEURISTIC;
			else if(algorithm.equals("NSGAII"))
				this.algorithm = ALGORITHM.NSGAII;
			//else if(algorithm.equals("Simulated Annealing (Grid)"))
			//	this.algorithm = ALGORITHM.SIMULATED_ANNEALING_GRID;
		}
		
		public String getCoolingEq() {
			return coolingEq;
		}
		
		public void setCoolingEq(String coolingEq) {
			this.coolingEq = coolingEq;
		}
		
		public ArrayList<OBJECTIVE> getObjectives() {
			return objectives;
		}
		
		public void setObjectives(Boolean selected, OBJECTIVE objective) {
			if(selected)
				this.objectives.add(objective);
			else if (this.objectives.contains(objective))
				this.objectives.remove(objective);
		}

		// This is the primary interface to spin up the algorithm, TODO: Add multi-threading per run
		public boolean run(final int runs) throws Exception {
			// Resets the objective graph
			if (Constants.showGraph) {
				myGraph = new ObjectiveGraph(data);
				runner.setObjectiveGraph(wizard.myGraph);
			}
			// Resets the domain visualization
			if (Constants.showViz) {
				wizard.launchVisWindow(true, Constants.showViz);
				runner.setDomainViewer(wizard.domainViewer);
			}
			// Clear previous results
			ResultPrinter.clearResults();
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Running iterative procedure ", set.getIterations()*runs);	
					runner.setMonitor(monitor);
					for(int i = 0; i < runs; i++) {
						if(getNumberOfObj() < 2 && Constants.showGraph) {
							ResultPrinter.newTTDPlots(set, i+1); //already set up for the first iteration
						}
						if(monitor != null) 
							monitor.setTaskName("Running iterative procedure "+String.valueOf(i+1)+"/"+runs);
						try {
							wasCancelled = runner.runInternal(data, i); // Run the optimization
						} catch (IOException e) {
							System.out.println("Failed to successfully complete run "+String.valueOf(i+1));
							e.printStackTrace();
						}
						if(wasCancelled) {
							break;
						}
					}
					if(!wasCancelled) {
						monitor.setTaskName("Printing results");
						// Print files and run pareto ranking (if selected)
						ResultPrinter.postProcessingResults(data);
						
						// Update the graph with pareto rankings (if selected)
						if(Constants.showGraph && Constants.paretoRank)
							myGraph.updateGraphData();
						
						// Update the visualization with pareto rankings (if selected)
						if(Constants.showViz && Constants.paretoRank) {
							domainViewer.addRankedCampaigns(100);
							System.out.println("Do something cool");
						}
					}
				}
			});
			System.gc();
			return wasCancelled;
		}
		
		public void setWorkingDirectory(final String dir) {
			runner.setResultsDirectory(dir);
		}
		
		private int getNumberOfObj() {
			return data.getObjectives().size();
		}
		
		public void setObjectiveAxes(String yAxis, String xAxis) {
			yAxisObjective = OBJECTIVE.getObjective(yAxis);
			xAxisObjective = OBJECTIVE.getObjective(xAxis);
		}
		
		public OBJECTIVE getyAxis() {
			return yAxisObjective;
		}
		
		public OBJECTIVE getxAxis() {
			return xAxisObjective;
		}
		
		public ArrayList<Point3i> runWellOptimizationE4D(final String selectedParameter, final int maximumWells) throws Exception {
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Calculating E4D Wells for " + selectedParameter, 100000);
					try {
						wells = E4DSensorSettings.calculateE4DWells(data, selectedParameter, maximumWells, monitor);
					} catch (Exception e) {
						wells = null;
						e.printStackTrace();
					}
				}

			});
			return wells;
		}
		
		public void runE4DWindows(final E4DRunDialog e4dDialog, final File e4dWellList) throws Exception {
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Running E4D.", 1000);
					StringBuilder text = new StringBuilder();
					String input7 = ""; //We want to add the detection threshold to the file name
					
					// Loop through all the scenarios - E4D needs to run once for each scenario
					for(Scenario scenario: set.getScenarios()) {
						if(monitor.isCanceled()) return;
						monitor.subTask("Looping through the selected scenarios: " + scenario.toString());
						
						// Before we run, do a quick check that the timesteps align between the storage and leakage files
						String storage = e4dDialog.getStorage(); //Storage File Location
						String leakage = Constants.homeDirectory + File.separator + scenario.toString() + ".h5"; //Leakage File Location
						if(!HDF5Interface.checkTimeSync(monitor, storage, leakage, data.getSet().getNodeStructure().getTimeSteps().size())) {
							System.out.println("Error: The time steps don't match between the storage and leakage files.");
							continue;
						}
						
						// Run the Python script with the following input arguments
						try {
							File e4dScript = new File(Constants.userDir, "e4d" + File.separator + "run_dream2e4d_windows.py");
							String input1 = storage;
							String input2 = leakage;
							String input3 = e4dWellList.getPath(); //Well List Location
							String input4 = e4dDialog.getBrineSaturation(); //Brine Saturation Mapping
							String input5 = e4dDialog.getGasSaturation(); //Gas Saturation Mapping
							String input6 = e4dDialog.getSaltConcentration(); //Salt Concentration Mapping
							input7 = String.valueOf(e4dDialog.getDetectionThreshold()); //Detection Threshold
							String command = "python \"" +e4dScript.getAbsolutePath()+ "\" \"" +input1+ "\" \"" +input2+ "\" \"" +input3+ "\" \"" +input4+ "\" \"" +input5+ "\" \"" +input6+ "\" \"" +input7+ "\"";
							File wDirectory = new File(Constants.userDir,"e4d");
							
							Process p = Runtime.getRuntime().exec(command, null, wDirectory);
							
							//Read all the Python outputs to console
							BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
							BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
							String s = null;
							System.out.println("This is the standard output from the E4D code for " + scenario.toString() + ":");
							while((s = stdInput.readLine()) != null)
								System.out.println(s);
							System.out.println("This is the error output from the E4D code for " + scenario.toString() + ":");
							while((s = stdError.readLine()) != null)
								System.out.println(s);
						} catch(Exception e) {
							System.out.println(e);
							System.out.println("Install python3 and required libraries to run E4D");
						}
						monitor.worked(1000 / set.getScenarios().size() - 10);
						monitor.subTask("Writing the scenario results: " + scenario);
						// Read the result matrix from each scenario into a master file
						File detectionMatrix = new File(Constants.userDir, "e4d" + File.separator + "detection_matrix.csv");
						String line = "";
						int lineNum = 0;
						try (BufferedReader br = new BufferedReader(new FileReader(detectionMatrix))) {
							// Read each line, comma delimited
							while ((line = br.readLine()) != null) {
								if(lineNum==0) {
									String[] lineList = line.split(",");
									lineList[0] = scenario.toString();
									line = String.join(",", lineList);
								}
								text.append(line + "\n");
								lineNum++;
							}
							text.append("\n");
						} catch(Exception e) {
							e.printStackTrace();
						}
						monitor.worked(10);
						
						// Write out the current cumulative detection matrix file at each iteration
						File fullDetectionMatrix = new File(Constants.userDir, "e4d/ertResultMatrix_" + set.getScenarioEnsemble() + "_" + set.getScenarios().size() +
								"_" + input7 + ".csv");
						try {
							fullDetectionMatrix.createNewFile();
							FileUtils.writeStringToFile(fullDetectionMatrix, text.toString());
						} catch (IOException e) {
							JOptionPane.showMessageDialog(null, "Could not write to " + fullDetectionMatrix.getName() + ", make sure the file is not currently open");
							e.printStackTrace();
						}
						
						// Delete the extra e4d files that we don't want...
						// TODO: Much better option is for Jeff to not write these files at all...
						File file = new File(Constants.userDir, "e4d/e4d_mod1_log.txt");
						file.delete();
						file = new File(Constants.userDir, "e4d/e4d.log");
						file.delete();
						file = new File(Constants.userDir, "e4d/run_time.txt");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.dpd");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.sig.srv");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh_baseline.h5");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh_w_leak.h5");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.sig");
						file.delete();
						file = new File(Constants.userDir, "e4d/e4d.inp");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.out");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.srv");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.node");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.edge");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.ele");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.face");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.neigh");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.poly.1_orig.node");
						file.delete();
						file = new File(Constants.userDir, "e4d/mesh_build.log");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.edge");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.ele");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.ele.old");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.neigh");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.node");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.poly");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.poly");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.sig");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.cfg");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.poly");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.trn");
						file.delete();
						System.out.println("Successfully deleted several e4d files (i.e. " + file + ")");
					}
				}
			});
		}
		
	}
} 