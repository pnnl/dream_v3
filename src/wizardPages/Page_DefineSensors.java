package wizardPages;

import java.awt.Desktop;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import dialog.E4DDialog;
import dialog.E4DRunDialog;
import gravity.HeatMapWindow;
import gravity.Heatmap;
import gravity.RunPyScript;
import hdf5Tool.HDF5Interface;
import mapView.CoordinateSystemDialog;
import objects.E4DSensorSettings;
import objects.SensorSetting;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.SensorType;
import objects.SensorSetting.Trigger;
import utilities.Constants;
import utilities.Constants.FileType;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page for defining specifications for the sensors to be used in the algorithm.
 * See line 608
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */

public class Page_DefineSensors extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private GridData compositeGridData; //for composite with units
	private GridLayout compositeGridLayout; //for composite with units
	private STORMData data;
	
	protected Map<String, Integer> num_duplicates;
	private Map<String, SensorData> sensorData;
	private Button runE4DButton;
	
	private boolean isCurrentPage = false;
	public static boolean changed = true;
	
	protected Page_DefineSensors(STORMData data) {
		super("Define Sensors");
		this.data = data;	 
	}
	
	public class SensorData {
		
		private Button addButton;
		public String parameter;
		public String sensorName;
		public String alias;
		public String cost;
		public Trigger trigger;
		public DeltaType deltaType;
		public float detectionThreshold;
		public SensorType sensorType;
		public float topZ;
		public float bottomZ;
		
		private Float globalTopZBound;
		private Float globalBottomZBound;
		public boolean isIncluded;
		private boolean isDuplicate;
		
		private Label solutionNodeLabel;
		
		private Text aliasText;
		
		private Composite costComposite;
		private Label costUnit;
		private Text costText;
		
		private Composite detectionComposite;
		private Text detectionText;
		private Label detectionUnit;
				
		private Combo sensorTypeCombo;
		
		private Composite bottomComposite;
		private Text bottomText;
		private Label bottomUnit;
		
		private Composite topComposite;
		private Text topText;
		private Label topUnit;
		
		private Combo thresholdCombo;
		public String specificType;
		
		private GridData unitData;
		
		// New class for storing the data about one particular parameter (IAM)
		public SensorData(String specificType, SensorSetting sensorSettings) {
			String[] tokens = specificType.split("_");
			parameter = sensorName = tokens[0];
			alias = sensorName;
			cost = sensorSettings.getSensorCostEq();
			trigger = Constants.getTriggerFromSpecificType(specificType);
			deltaType = sensorSettings.getDeltaType();
			detectionThreshold = Float.parseFloat(tokens[2]);
			sensorType = SensorType.POINT_SENSOR;
			// up means bottom = min & top = max, down means bottom = max & top = min
			if(data.getSet().getNodeStructure().getPositive().equals("up")) {
				topZ = globalTopZBound = data.getSet().getNodeStructure().getGlobalMaxZ();
				bottomZ = globalBottomZBound = data.getSet().getNodeStructure().getGlobalMinZ();
			} else {
				topZ = globalTopZBound = data.getSet().getNodeStructure().getGlobalMinZ();
				bottomZ = globalBottomZBound = data.getSet().getNodeStructure().getGlobalMaxZ();
			}
			this.specificType = specificType;
			alias = tokens[0] + "_" + tokens[2];
		}
		
		//Class for storing the data about one particular parameter (HDF5)
		public SensorData(SensorSetting sensorSettings, String sensorName) {
			
			isDuplicate = !sensorSettings.getParameter().equals(sensorName);
			
			isIncluded = false; //By default
			parameter = sensorSettings.getParameter();
			this.sensorName = sensorName;
			alias = sensorName;
			cost = sensorSettings.getSensorCostEq();
			trigger = Trigger.ABOVE_THRESHOLD;
			deltaType = sensorSettings.getDeltaType();
			detectionThreshold = sensorSettings.getDetectionThreshold();
			sensorType = SensorType.POINT_SENSOR;
			// up means bottom = min & top = max, down means bottom = max & top = min
			if(data.getSet().getNodeStructure().getPositive().equals("up")) {
				topZ = globalTopZBound = data.getSet().getNodeStructure().getGlobalMaxZ();
				bottomZ = globalBottomZBound = data.getSet().getNodeStructure().getGlobalMinZ();
			} else {
				topZ = globalTopZBound = data.getSet().getNodeStructure().getGlobalMinZ();
				bottomZ = globalBottomZBound = data.getSet().getNodeStructure().getGlobalMaxZ();
			}
			
			// Trigger should be relative delta when pressure
			if(parameter.toLowerCase().contains("pressure") || parameter.toLowerCase().equals("p") || parameter.contains("Electrical Conductivity"))
				trigger = Trigger.RELATIVE_CHANGE;
			
			// Trigger should be maximum threshold when pH
			else if(parameter.toLowerCase().equals("ph"))
				trigger = Trigger.BELOW_THRESHOLD;
			
			// Exceptions for Gravity
			if(sensorName.contains("gravity")) {
				sensorType = SensorType.SURFACE;
				sensorSettings.setSensorCostEq("5a*s");
				cost = "5a*s";
			}
			
			// Exceptions for ERT
			if(sensorName.contains("Electrical Conductivity")) {
				alias = "ERT_" + detectionThreshold;
				//sensorType = SensorType.CROSS_WELL;
			}
		}
		public String getParameter() {
			return parameter;
		}
		public Trigger getTrigger() {
			return trigger;
		}
		public DeltaType getDeltaType() {
			return deltaType;
		}
		public float getThreshold() {
			return detectionThreshold;
		}
		public SensorType getSensorType() {
			return sensorType;
		}
		
		public void buildUI(String parameter) {
			// Creates dataGrid information for each field
			GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
			gridData.widthHint = 60; //sets desired width for the text area
			
			// A duplicate parameter will be labeled with _1 and will not be in some maps
			String shortParameter = parameter;
			if(parameter.contains("_"))
				shortParameter = parameter.substring(0, parameter.indexOf("_"));
			
			//Add a button here
			if(isDuplicate){
				addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						sensorData.remove(sensorName);
						data.getSet().getSensorSettings().remove(sensorName);
						loadPage(false);
					}
			    });
			    addButton.setText("-");
			}
			else{
				addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						if(!num_duplicates.containsKey(parameter)) { //first duplicate
							num_duplicates.put(parameter, 1);
						} else { //multiple duplicates
							num_duplicates.put(parameter, num_duplicates.get(parameter) + 1);
						}
						String newParameter = parameter + "_" + num_duplicates.get(parameter);
						data.getSet().addSensorSetting(newParameter, parameter);
						sensorData.put(newParameter, new SensorData(data.getSet().getSensorSettings(newParameter), newParameter));
						loadPage(false);
						if(num_duplicates.get(parameter)==100) { //rare, but prevent more than 99 duplicates so statistics doesn't throw an error
							for(SensorData temp: sensorData.values()) {
								if(temp.sensorName==parameter)
									temp.addButton.setEnabled(false);
							}
						}
					}
			    });
			    addButton.setText("+");
			}
			
		    
			// Include button
			Button includeButton = new Button(container,  SWT.CHECK);
			includeButton.setSelection(isIncluded);
			includeButton.setText(parameter);
			for(SensorData temp: sensorData.values()) {
				if(temp.isIncluded) {
					errorFound(false, "  Must select a monitoring parameter.");
					break;
				}
				errorFound(true, "  Must select a monitoring parameter.");
			}
			includeButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { 
					//required to have this... not sure when it is triggered.
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					isIncluded = ((Button)e.getSource()).getSelection();
					toggleEnabled();
					
					//Special handling if errors are negated when parameters are unchecked...
					//We have to search through all possible errors to see if any are negated
					boolean checkError = true;
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					boolean costError = false;
					boolean detectionError = false;
					boolean zeroError = false;
					boolean botError = false;
					boolean botBoundError = false;
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) //Skip unchecked parameters
							continue;
						else
							checkError = false;
						//Alias
						for(SensorData temp2: sensorData.values()) {
							if(!temp2.isIncluded) //Skip unchecked parameters
								continue;
							if(temp.alias.trim().equals(temp2.alias.trim()) && !temp.sensorName.equals(temp2.sensorName)) {
								duplicateError = true;
								temp.aliasText.setForeground(Constants.red);
							}
						}
						if(temp.alias.contains(",")) //Contains a comma
							commaError = true;
						if(temp.alias.isEmpty()) //No alias
							emptyError = true;
						//Cost
						if(!Constants.isValidCostEquation(temp.costText.getText(), data.getSet().getNodeStructure().getTimeSteps()))
							costError = true;
						//Detection
						if(!Constants.isValidFloat(temp.detectionText.getText()))
							detectionError = true;
						//Zone bottom
						if(!Constants.isValidFloat(temp.bottomText.getText()))
							botError = true;
						else {
							float botZValue = Float.parseFloat(temp.bottomText.getText());
							if (botZValue < temp.globalBottomZBound || botZValue > temp.topZ)
								botBoundError = true;
						}
						//Zone top
						if(!Constants.isValidFloat(temp.topText.getText()))
							topError = true;
						else {
							float topZValue = Float.parseFloat(temp.topText.getText());
							if (topZValue < temp.bottomZ || topZValue > temp.globalTopZBound)
								topBoundError = true;
						}
					}
					errorFound(checkError, "  Must select a monitoring parameter.");
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
					errorFound(costError, "  Cost is not a real number.");
					errorFound(detectionError, "  Detection is not a real number.");
					errorFound(zeroError, "  Number of surveys cannot be 0.");
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
					changed = true;
					
					//Special handling of red text for duplicates
					if (duplicateError==false)
						for(SensorData data: sensorData.values())
							if (data.isIncluded && !data.alias.contains(",") && !data.alias.isEmpty())
								data.aliasText.setForeground(Constants.black);
				}
			});
			
			
			// Alias Input
			aliasText = new Text(container, SWT.BORDER | SWT.SINGLE);
			aliasText.setText(sensorData.get(parameter).alias);
			aliasText.setForeground(Constants.black);
			aliasText.addModifyListener(new ModifyListener(){
				@Override
				public void modifyText(ModifyEvent e){
					aliasText = ((Text)e.getSource());
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						//temp.aliasText.setForeground(black);
						for(SensorData temp2: sensorData.values()) {
							if(!temp2.isIncluded) continue; //Skip unchecked parameters
							if(temp.aliasText.getText().trim().equals(temp2.aliasText.getText().trim()) &&
									!temp.sensorName.equals(temp2.sensorName)) {
								temp.aliasText.setForeground(Constants.red);
								duplicateError = true;
							}
						}
						if(temp.aliasText.getText().contains(",")) { //Contains a comma
							temp.aliasText.setForeground(Constants.red);
							commaError = true;
						}
						if(temp.aliasText.getText().trim().isEmpty()) { //Empty alias
							temp.aliasText.setForeground(Constants.red);
							emptyError = true;
						}
						if (duplicateError==false && commaError==false && emptyError==false) { //No errors
							temp.aliasText.setForeground(Constants.black);
							temp.alias = temp.aliasText.getText();
						}
					}
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
					changed = true;
				}
			});
			GridData aliasTextData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			aliasTextData.widthHint = 60;
			aliasText.setLayoutData(aliasTextData);
			
			// The border gives the appearance of a single component
			costComposite = new Composite(container, SWT.BORDER);
			costComposite.setLayoutData(compositeGridData);
			costComposite.setLayout(compositeGridLayout); //Specifies two fields for composite
			
			costUnit = new Label(costComposite, SWT.NONE);
			costUnit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			costUnit.setText(data.getSet().getCostUnit());
			costUnit.setForeground(Constants.grey);
			costUnit.addListener(SWT.MouseDown, new Listener() {
				public void handleEvent(Event event) {
					costText.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
					costText.setSelection(0); //Sets the cursor to the front of the text
				}
			});
			
			costText = new Text(costComposite, SWT.SINGLE | SWT.LEFT);
			costText.setText(String.valueOf(sensorData.get(parameter).cost));
			costText.setForeground(Constants.black);
			costText.setToolTipText(getCostToolTip(sensorType));
			costText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					//Remove all spaces and trailing math operators
					String equation = ((Text)e.getSource()).getText().replaceAll("\\s", "").replaceAll("[-+*/^(]$", "").toLowerCase();
					boolean costError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidCostEquation(equation, data.getSet().getNodeStructure().getTimeSteps())) { //Valid equation
							temp.costText.setForeground(Constants.black);
							temp.cost = equation;
						} else { //Not a valid equation
							temp.costText.setForeground(Constants.red);
							costError = true;
						}
					}
					errorFound(costError, "  Cost is not a valid equation or cost is a negative number.");
					changed = true;
				}
			});
			GridData costTextData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			costTextData.widthHint = 60;
			costText.setLayoutData(costTextData);
			
			
			// Detection Type
			thresholdCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			thresholdCombo.add(Trigger.BELOW_THRESHOLD.toString());
			thresholdCombo.add(Trigger.ABOVE_THRESHOLD.toString());
			thresholdCombo.add(Trigger.RELATIVE_CHANGE.toString());
			thresholdCombo.add(Trigger.ABSOLUTE_CHANGE.toString());
			thresholdCombo.setText(trigger.toString());
			thresholdCombo.setToolTipText(getDetectionTypeToolTip(trigger));
			thresholdCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if(((Combo)e.getSource()).getText().equals(Trigger.ABOVE_THRESHOLD.toString())) {
						trigger = Trigger.ABOVE_THRESHOLD;
						detectionUnit.setText(data.getSet().getNodeStructure().getUnit(parameter));
					} else if(((Combo)e.getSource()).getText().equals(Trigger.BELOW_THRESHOLD.toString())) {
						trigger = Trigger.BELOW_THRESHOLD;
						detectionUnit.setText(data.getSet().getNodeStructure().getUnit(parameter));
					} else if(((Combo)e.getSource()).getText().equals(Trigger.RELATIVE_CHANGE.toString())) {
						trigger = Trigger.RELATIVE_CHANGE;
						detectionUnit.setText("%");
					} else { //(((Combo)e.getSource()).getText().equals(Trigger.ABSOLUTE_DELTA.toString()))
						trigger = Trigger.ABSOLUTE_CHANGE;
						detectionUnit.setText(data.getSet().getNodeStructure().getUnit(parameter));
					}
					thresholdCombo.setToolTipText(getDetectionTypeToolTip(trigger));
					errorFound(false, "  No nodes were found for the provided parameters.");
					if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;
					detectionComposite.layout();
					changed = true;
				}
			});
			GridData thresholdComboData = new GridData(SWT.FILL, SWT.CENTER, false, false);
			thresholdComboData.widthHint = 105;
			thresholdCombo.setLayoutData(thresholdComboData);
			
			
			//// Detection Value ////
			// We want to display units within the same text box, so we need to do some fancy magic
			// Essentially we are creating a composite with two fields within it: (1) text and (2) unit label
			// It should look *mostly* the same as other input fields, but the user is unable to edit units
			
			//Get the units for each parameter
			String unit;
			unit = data.getSet().getNodeStructure().getUnit(shortParameter);
			if (unit.contains("kg")) {
				unit = "kg/m^3";
			}
			
			// The border gives the appearance of a single component
			detectionComposite = new Composite(container, SWT.BORDER);
			detectionComposite.setLayoutData(compositeGridData);
			detectionComposite.setLayout(compositeGridLayout); //Specifies two fields for composite
			
			detectionText = new Text(detectionComposite, SWT.SINGLE | SWT.RIGHT);
			detectionText.setLayoutData(gridData);
			detectionText.setText((sensorData.get(parameter).deltaType==DeltaType.INCREASE ? "+" : "") + String.valueOf(sensorData.get(parameter).detectionThreshold)); //Need to maintain plus sign
			detectionText.setForeground(Constants.black);
			detectionText.setToolTipText(HDF5Interface.getStatisticsString(shortParameter));
			detectionText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					detectionText = ((Text)e.getSource());
					boolean detectionError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidFloat(temp.detectionText.getText())) { //Valid number
							temp.detectionText.setForeground(Constants.black);
							temp.detectionThreshold = Float.valueOf(temp.detectionText.getText());
						} else { //Not a valid number
							temp.detectionText.setForeground(Constants.red);
							detectionError = true;
						}
					}
					errorFound(detectionError, "  Detection is not a real number.");
					errorFound(false, "  No nodes were found for the provided parameters.");
					if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;
					changed = true;
				}
			});
			unitData = new GridData(SWT.FILL, SWT.CENTER, false, false);
			detectionUnit = new Label(detectionComposite, SWT.NONE);
			detectionUnit.setLayoutData(unitData);
			if (trigger == Trigger.RELATIVE_CHANGE) {
				detectionUnit.setText("%");
			} else {
				detectionUnit.setText(unit);
			}
			detectionUnit.setForeground(Constants.grey);
			detectionUnit.setToolTipText(HDF5Interface.getStatisticsString(shortParameter));
			detectionUnit.addListener(SWT.MouseDown, new Listener() {
				public void handleEvent(Event event) {
					detectionText.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
					detectionText.setSelection(detectionText.toString().length()); //Sets the cursor to the end of the text
				}
			});
			
			
			//// Deployment Method ////
			// This field determines the sensor subclass, which affects how detectable nodes work
			sensorTypeCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			sensorTypeCombo.add(SensorType.POINT_SENSOR.toString());
			sensorTypeCombo.add(SensorType.SURFACE.toString());
			//sensorTypeCombo.add(SensorType.BOREHOLE.toString());
			//sensorTypeCombo.add(SensorType.CROSS_WELL.toString());
			sensorTypeCombo.setText(sensorType.toString());
			sensorTypeCombo.setToolTipText(getMethodToolTip(sensorType));
			sensorTypeCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if(((Combo)e.getSource()).getText().equals(SensorType.POINT_SENSOR.toString())) {
						sensorType = SensorType.POINT_SENSOR;
						costText.setText("500+5t");
					} else if(((Combo)e.getSource()).getText().equals(SensorType.SURFACE.toString())) {
						sensorType = SensorType.SURFACE;
						costText.setText("5a*s");
					/*} else if(((Combo)e.getSource()).getText().equals(SensorType.BOREHOLE.toString())) {
						sensorType = SensorType.BOREHOLE;
					} else { //if(((Combo)e.getSource()).getText().equals(SensorType.CROSS_WELL.toString())) {
						sensorType = SensorType.CROSS_WELL;*/
					}
					sensorTypeCombo.setToolTipText(getMethodToolTip(sensorType));
					toggleEnabled();
					errorFound(false, "  No nodes were found for the provided parameters.");
					changed = true;
				}
			});
			GridData sensorTypeComboData = new GridData(SWT.FILL, SWT.CENTER, false, false);
			sensorTypeComboData.widthHint = 65;
			sensorTypeCombo.setLayoutData(sensorTypeComboData);
			
			
			//// Zone Bottom Value ////
			// We want to display units within the same text box, so we need to do some fancy magic
			// Essentially we are creating a composite with two fields within it: (1) text and (2) unit label
			// It should look *mostly* the same as other input fields, but the user is unable to edit units
			
			//Get the units for the Z value
			unit = data.getSet().getNodeStructure().getUnit("z");
			// The border gives the appearance of a single component
			bottomComposite = new Composite(container, SWT.BORDER);
			bottomComposite.setLayoutData(compositeGridData);
			bottomComposite.setLayout(compositeGridLayout); //Specifies two fields for composite
			
			bottomText = new Text(bottomComposite, SWT.SINGLE | SWT.RIGHT);
			bottomText.setLayoutData(gridData);
			bottomText.setText(String.valueOf(sensorData.get(parameter).topZ));
			bottomText.setForeground(Constants.black);
			bottomText.setToolTipText("Global zone bottom = " + globalBottomZBound + unit);
			bottomText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					bottomText = ((Text)e.getSource());
					boolean botError = false;
					boolean botBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidFloat(temp.bottomText.getText())) { //Valid number
							float botZValue = Float.valueOf(temp.bottomText.getText());
							if (botZValue < globalBottomZBound || botZValue > topZ) {
								temp.bottomText.setForeground(Constants.red);
								botBoundError = true;
							} else {
								temp.bottomText.setForeground(Constants.black);
								temp.topZ = botZValue;
							}
						} else { //Not a valid number
							temp.bottomText.setForeground(Constants.red);
							botError = true;
						}
					}
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
					changed = true;
				}
			});
			
			bottomUnit = new Label(bottomComposite, SWT.NONE);
			bottomUnit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			bottomUnit.setText(unit);
			bottomUnit.setForeground(Constants.grey);
			bottomUnit.setToolTipText("Global zone bottom = " + globalBottomZBound + unit);
			bottomUnit.addListener(SWT.MouseDown, new Listener() {
				public void handleEvent(Event event) {
					bottomText.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
					bottomText.setSelection(bottomText.toString().length()); //Sets the cursor to the end of the text
				}
			});
			
			
			//// Zone Top Value ////
			// We want to display units within the same text box, so we need to do some fancy magic
			// Essentially we are creating a composite with two fields within it: (1) text and (2) unit label
			// It should look *mostly* the same as other input fields, but the user is unable to edit units
			
			// The border gives the appearance of a single component
			topComposite = new Composite(container, SWT.BORDER);
			topComposite.setLayoutData(compositeGridData);
			topComposite.setLayout(compositeGridLayout); //Specifies two fields for composite
			
			topText = new Text(topComposite, SWT.SINGLE | SWT.RIGHT);
			topText.setLayoutData(gridData);
			topText.setText(String.valueOf(sensorData.get(parameter).bottomZ));
			topText.setForeground(Constants.black);
			topText.setToolTipText("Global zone top = " + globalTopZBound + unit);
			topText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					topText = ((Text)e.getSource());
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidFloat(temp.topText.getText())) { //Valid number
							float topZValue = Float.valueOf(temp.topText.getText());
							if (topZValue < bottomZ || topZValue > globalTopZBound) {
								temp.topText.setForeground(Constants.red);
								topBoundError = true;
							} else {
								temp.topText.setForeground(Constants.black);
								temp.bottomZ = topZValue;
							}
						} else { //Not a valid number
							temp.topText.setForeground(Constants.red);
							topError = true;
						}
					}
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
					changed = true;
				}
			});
			
			topUnit = new Label(topComposite, SWT.NONE);
			topUnit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			topUnit.setText(unit);
			topUnit.setForeground(Constants.grey);
			topUnit.setToolTipText("Global zone top = " + globalTopZBound + unit);
			topUnit.addListener(SWT.MouseDown, new Listener() {
				public void handleEvent(Event event) {
					topText.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
					topText.setSelection(topText.toString().length()); //Sets the cursor to the end of the text
				}
			});
			
			// Now hide or show fields depending on file type and sensor type
			toggleEnabled();
		}
		
		private void toggleEnabled() {
			costComposite(isIncluded);
			if(data.fileType=="iam") {
				setIAMFields();
			} else {
				if(sensorType==SensorType.POINT_SENSOR)
					setPointSensorFields();
				else if(sensorType==SensorType.SURFACE)
					setSurfaceFields();
				//else if(sensorType==SensorType.CROSS_WELL)
					//setCrossWellFields();
			}
		}
		// Some fields should be enabled depending on file type or deployment method
		private void setIAMFields() {
			addButton.setVisible(false); //fixed field for IAM
			aliasText.setEnabled(false); //fixed field for IAM
			thresholdCombo.setEnabled(false); //fixed field for IAM
			detectionComposite(false); //fixed field for IAM
			sensorTypeCombo.setEnabled(false); //fixed field for IAM
			bottomComposite(isIncluded);
			topComposite(isIncluded);
		}
		private void setPointSensorFields() {
			addButton.setEnabled(true);
			aliasText.setEnabled(isIncluded);
			thresholdCombo.setEnabled(isIncluded);
			detectionComposite(isIncluded);
			sensorTypeCombo.setEnabled(isIncluded);
			bottomComposite(isIncluded);
			topComposite(isIncluded);
		}
		private void setSurfaceFields() {
			addButton.setEnabled(true);
			aliasText.setEnabled(isIncluded);
			thresholdCombo.setEnabled(isIncluded);
			detectionComposite(isIncluded);
			sensorTypeCombo.setEnabled(isIncluded);
			bottomComposite(false); //fixed field for surface
			topComposite(false); //fixed field for surface
		}
		/*private void setCrossWellFields() {
			addButton.setVisible(false); //fixed field for ERT
			aliasText.setEnabled(false); //fixed field for ERT
			thresholdCombo.setEnabled(false); //fixed field for ERT
			detectionComposite(false); //fixed field for ERT
			sensorTypeCombo.setEnabled(isIncluded);
			bottomComposite(false); //fixed field for ERT
			topComposite(false); //fixed field for ERT
		}*/
		// Special handling because cost field is a composite
		private void costComposite(boolean show) {
			if(costComposite != null && !costComposite.isDisposed()) {
				costComposite.setEnabled(show);
				costText.setEnabled(show);
				if(show) {
					costComposite.setBackground(Constants.white);
					costUnit.setBackground(Constants.white);
				} else {
					costComposite.setBackground(container.getBackground());
					costUnit.setBackground(container.getBackground());
				}
			}
		}
		// Special handling because detection field is a composite
		private void detectionComposite(boolean show) {
			if(detectionComposite != null && !detectionComposite.isDisposed()) {
				detectionComposite.setEnabled(show);
				detectionText.setEnabled(show);
				if(show) {
					detectionComposite.setBackground(Constants.white);
					detectionUnit.setBackground(Constants.white);
				} else {
					detectionComposite.setBackground(container.getBackground());
					detectionUnit.setBackground(container.getBackground());
				}
			}
		}
		// Special handling because bottom field is a composite
		private void bottomComposite(boolean show) {
			if(bottomComposite != null && !bottomComposite.isDisposed()) {
				bottomComposite.setEnabled(show);
				bottomText.setEnabled(show);
				if(show) {
					bottomComposite.setBackground(Constants.white);
					bottomUnit.setBackground(Constants.white);
				} else {
					bottomComposite.setBackground(container.getBackground());
					bottomUnit.setBackground(container.getBackground());
				}
			}
		}
		// Special handling because top field is a composite
		private void topComposite(boolean show) {
			if(topComposite != null && !topComposite.isDisposed()) {
				topComposite.setEnabled(show);
				topText.setEnabled(show);
				if(show) {
					topComposite.setBackground(Constants.white);
					topUnit.setBackground(Constants.white);
				} else {
					topComposite.setBackground(container.getBackground());
					topUnit.setBackground(container.getBackground());
				}
			}
		}
	}
	
	@Override
	public void loadPage(boolean reset) {

		isCurrentPage = true;
		if(!DREAMWizard.errorMessage.getText().contains("  No nodes were found for the provided parameters."))
			DREAMWizard.errorMessage.setText("");
		DREAMWizard.convertDataButton.setEnabled(false);
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.nextButton.setEnabled(true);
		removeChildren(container);
		
		// Initialize and reset some variables
		if(changed && reset) {
			num_duplicates = new HashMap<String, Integer>();
			sensorData = new TreeMap<String, SensorData>();
			E4DSensorSettings.addERTSensor(data.getSet()); //initialize ERT matrix if needed
			
			// If we are dealing with H5 files, add all possible data types
			if(data.fileType=="hdf5") {
				for(String parameter: data.getSet().getAllPossibleParameters()) {
					if(data.getSensorSettings(parameter) != null) // Adds all sensors from the list
						sensorData.put(parameter, new SensorData(data.getSet().getSensorSettings(parameter), parameter));
					else // If the user went back after findings nodes, some sensors were removed and saved in another map
						sensorData.put(parameter, new SensorData(data.getSet().getRemovedSensorSettings(parameter), parameter));
				}
				data.getSet().resetRemovedSensorSettings();
			}
			// IAM values are already stored in detectionMap, but E4D files might also be stored in detectionMap
			else {
				for(String specificType: data.getSet().getDetectionMap().keySet()) {
					String parameter = specificType.substring(0, specificType.indexOf("_"));
					sensorData.put(parameter, new SensorData(specificType, data.getSet().getSensorSettings(parameter)));
				}
			}
		}
		
		// We want to display units within the same text box, so we need to do some fancy magic
		// Essentially we are creating a composite with two fields within it: (1) prefix symbol and (2) cost value
		// It should look *mostly* the same as other input fields, but the user is unable to edit units
		// GridData layout for the main composite
		compositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		compositeGridData.widthHint = 60; //sets desired width for the composite
		// GridLayout for the two fields within the composite
		compositeGridLayout = new GridLayout(2, false);
		compositeGridLayout.marginHeight = 1;
		compositeGridLayout.marginWidth = 0;
		compositeGridLayout.horizontalSpacing = 0;
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Define Available Sensors");
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
		infoLabel.setText("Describe the sensors that will be available during the optimization algorithm. Select the parameter "
				+ "that the sensor monitors, define the cost per sensor (which varies by deployment method), specify the "
				+ "threshold that the sensor can detect, and constain sensors by operating "
				+(data.getSet().getNodeStructure().getPositive().equals("up")?"elevations":"depths")
				+ " Note: hover over fields for tips and statistics across the domain.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoGridData.widthHint = 200;
		infoLabel.setLayoutData(infoGridData);
		
		// Headers
		new Label(container, SWT.NULL);	// Blank filler
		org.eclipse.swt.graphics.Image questionMark = new org.eclipse.swt.graphics.Image(container.getDisplay(), getClass().getResourceAsStream("/QuestionMark.png"));
		String timeUnit = data.getSet().getNodeStructure().getUnit("times");
		String xUnit = data.getSet().getNodeStructure().getUnit("x");
		
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
						+ "represent a value at each model node and timestep. Check all parameters that should be considered for monitoring. "
						+ "The plus (+) button can be selected to duplicate a parameter if there are multiple sensor options (e.g., cheap vs. "
						+ "expensive).");
			}
		});
		
		Composite aliasComposite = new Composite(container, SWT.NULL);
		aliasComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		aliasComposite.setLayout(new GridLayout(2, false));
		Label aliasLabel = new Label(aliasComposite, SWT.LEFT);
		aliasLabel.setText("Sensor Alias");
		aliasLabel.setFont(Constants.boldFontSmall);
		CLabel aliasQ = new CLabel(aliasComposite, SWT.NULL);
		aliasQ.setImage(questionMark);
		aliasQ.setBottomMargin(0);
		aliasQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Sensor Alias Help", "A unique name for the sensor. This is particularly "
						+ "helpful for duplicate sensors of the same parameter.");
			}
		});
		
		Composite costComposite = new Composite(container, SWT.NULL);
		costComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		costComposite.setLayout(new GridLayout(2, false));
		Label costLabel = new Label(costComposite, SWT.LEFT);
		costLabel.setText("Cost per Sensor");
		costLabel.setFont(Constants.boldFontSmall);
		CLabel costQ = new CLabel(costComposite, SWT.NULL);
		costQ.setImage(questionMark);
		costQ.setBottomMargin(0);
		costQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Cost Help", "Cost for each sensor, which can be entered as a fixed "
						+ "value or an equation. Point sensors allow 't' to represent the number of "+timeUnit+" that the sensor is deployed "
						+ "and 'i' to represent the number of installations, in case the sensor is allowed to move. Surface surveys allow 's' "
						+ "to represent the number of surveys after the original baseline and 'a' to represent the area of each survey in "
						+ xUnit+"^3.");
			}
		});
		
		Composite typeComposite = new Composite(container, SWT.NULL);
		typeComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		typeComposite.setLayout(new GridLayout(2, false));
		Label typeLabel = new Label(typeComposite, SWT.LEFT);
		typeLabel.setText("Detection Type");
		typeLabel.setFont(Constants.boldFontSmall);
		CLabel typeQ = new CLabel(typeComposite, SWT.NULL);
		typeQ.setImage(questionMark);
		typeQ.setBottomMargin(0);
		typeQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Detection Type Help", "The detection type can be set independently for "
						+ "each parameter and defines the type of change that a sensor is able to detect. Above threshold means the sensor "
						+ "detects when a parameter exceeds the value. Below threshold means the sensor detects when the parameter falls below "
						+ "the value. Absolute change means the sensor detects change from the original value. Relative change means the "
						+ "sensor detects change from a original value as a percent. Both change types can be limited in the positive or "
						+ "negative direction by appending a + or - before the value.");
			}
		});
		
		Composite thresholdComposite = new Composite(container, SWT.NULL);
		thresholdComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		thresholdComposite.setLayout(new GridLayout(2, false));
		Label thresholdLabel = new Label(thresholdComposite, SWT.LEFT);
		thresholdLabel.setText("Detection Threshold");
		thresholdLabel.setFont(Constants.boldFontSmall);
		CLabel thresholdQ = new CLabel(thresholdComposite, SWT.NULL);
		thresholdQ.setImage(questionMark);
		thresholdQ.setBottomMargin(0);
		thresholdQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Detection Threshold Help", "The detection threshold corresponds to the "
						+ "detection type and determines the detectable space. The detectable space is made of all model nodes that "
						+ "exceed the defined threshold at any scenario. The placement of sensors is limited to this space so that DREAM "
						+ "has fewer potential solutions to explore.");
			}
		});
		
		Composite methodComposite = new Composite(container, SWT.NULL);
		methodComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		methodComposite.setLayout(new GridLayout(2, false));
		Label methodLabel = new Label(methodComposite, SWT.LEFT);
		methodLabel.setText("Deployment Method");
		methodLabel.setFont(Constants.boldFontSmall);
		CLabel methodQ = new CLabel(methodComposite, SWT.NULL);
		methodQ.setImage(questionMark);
		methodQ.setBottomMargin(0);
		methodQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Deployment Method Help", "This field determines the type of "
						+ "sensor. DREAM currently supports point sensors and surface surveys. Point sensors are located at a single "
						+ "location within a well and detect scalar changes in a parameter. Surface surveys involve periodic measurements "
						+ "taken by a survey crew to detect changes - it is assumed that a survey is conducted at time 0, and all future "
						+ "surveys are detecting change from this time. More deployment methods are planned for future releases of "
						+ "DREAM.");
			}
		});
		
		Composite bottomComposite = new Composite(container, SWT.NULL);
		bottomComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		bottomComposite.setLayout(new GridLayout(2, false));
		Label bottomLabel = new Label(bottomComposite, SWT.LEFT);
		bottomLabel.setText("Zone Bottom");
		bottomLabel.setFont(Constants.boldFontSmall);
		CLabel bottomQ = new CLabel(bottomComposite, SWT.NULL);
		bottomQ.setImage(questionMark);
		bottomQ.setBottomMargin(0);
		bottomQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Zone Bottom Help", "This determines the greatest depth that "
						+ "point sensors may be placed. This field is greyed out for surface surveys because they are all conducted "
						+ "at the surface.");
			}
		});
		
		Composite topComposite = new Composite(container, SWT.NULL);
		topComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		topComposite.setLayout(new GridLayout(2, false));
		Label topLabel = new Label(topComposite, SWT.LEFT);
		topLabel.setText("Zone Top");
		topLabel.setFont(Constants.boldFontSmall);
		CLabel topQ = new CLabel(topComposite, SWT.NULL);
		topQ.setImage(questionMark);
		topQ.setBottomMargin(0);
		topQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Zone Top Help", "This determines the minimum depth that "
						+ "point sensors may be placed. This field is greyed out for surface surveys because they are all "
						+ "conducted at the surface.");
			}
		});
		
		
		// This loops through each sensor and creates a row with input values
		for(String parameter: sensorData.keySet()) {
			sensorData.get(parameter).buildUI(parameter);
		}
		
		// Find Detectable Nodes Button
		Button findDetectableNodes = new Button(container, SWT.BALLOON);
		findDetectableNodes.setText("  Find Detectable Nodes  ");
		GridData detectableNodesData = new GridData(SWT.BEGINNING, SWT.END, false, false, 2, 1);
		findDetectableNodes.setLayoutData(detectableNodesData);
		findDetectableNodes.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {				
				// Checks if there are any new sensor settings to be added to detectionMap
				// Also saves sensorSetting information (i.e. cloudNodes, detectableNodes, sensorAliases, etc.)
				findDetectableNodes();
			}	       
		});
		
		Button launchGravityButton = new Button(container, SWT.BALLOON);
		launchGravityButton.setText("  Launch Gravity Map  ");
		launchGravityButton.setVisible(Constants.buildDev);
		launchGravityButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(final Event theEvent) {
				CoordinateSystemDialog dialog = new CoordinateSystemDialog(container.getShell(), false, false, true);
				dialog.open(); 
				RunPyScript py = new RunPyScript();
				try {
					File dir = new File(dialog.getOutputDir());
					File[] listOfFiles = dir.listFiles((d, name) -> name.endsWith(".in"));
					py.calculateGravity(listOfFiles);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				// When we start doing this program
				Heatmap heatMap = new Heatmap(dialog.getOutputDir());
				try {
					Image tempImage = heatMap.getHeatMap(1,0);
					HeatMapWindow window = new HeatMapWindow(tempImage, getContainer().getShell().getSize().x,
							getContainer().getShell().getSize().y, heatMap.parseTimeSteps(), heatMap);
					window.run();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		// Resulting nodes for full solution space (unique nodes that trigger on any scenario)
		Group solutionGroup = new Group(container, SWT.SHADOW_NONE);
		solutionGroup.setText("Detectable Nodes for Each Sensor");
		solutionGroup.setFont(Constants.boldFontSmall);
		solutionGroup.setLayout(new GridLayout(4,true));
		GridData tempData = new GridData(SWT.FILL, SWT.FILL, true, false);
		tempData.horizontalSpan = 10;
		solutionGroup.setLayoutData(tempData);
		for(String label: sensorData.keySet()){
			if (data.getSet().getSensorSettings(label) == null ) {
				data.getSet().resetSensorSettings(label);		
			}
			SensorData sensor = sensorData.get(label);
			sensor.solutionNodeLabel = new Label(solutionGroup, SWT.WRAP);
			if(data.getSet().getSensorSettings(label).getDetectableNodes().size() > 0)
				sensor.solutionNodeLabel.setText(label+": "+data.getSet().getSensorSettings(label).getDetectableNodes().size());
			else
				sensor.solutionNodeLabel.setText(label+": Not set");
		}
		
		// If the user has the E4D module installed, allow the E4D buttons to show up
		String e4dModuleDirectory = Constants.userDir + File.separator + "e4d";
		File e4dDirectory = new File(e4dModuleDirectory);
		if (e4dDirectory.exists()) {
			final File e4dWellList = new File(Constants.userDir, "e4d" + File.separator + "ertWellLocationsIJ_" + data.getSet().getScenarioEnsemble() + "_" + data.getSet().getScenarios().size() + ".txt");
			
			Composite composite_E4D = new Composite(container, SWT.NULL);
			GridLayout gridLayout_E4D = new GridLayout();
			gridLayout_E4D.numColumns = 3;
			composite_E4D.setLayout(gridLayout_E4D);
			GridData gridData_E4D = new GridData(GridData.FILL_HORIZONTAL);
			gridData_E4D.horizontalSpan=8;
			composite_E4D.setLayoutData(gridData_E4D);			
			
			//Add an info icon to explain the E4D Buttons
			Label infoLinkE4D = new Label(composite_E4D, SWT.NULL);
		  	infoLinkE4D.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
	  		infoLinkE4D.addListener(SWT.MouseUp, new Listener(){
	  			@Override
	  			public void handleEvent(Event event) {
	  				MessageDialog.openInformation(container.getShell(), "Additional information", "After finding triggering locations, the user may write input files for the E4D model. E4D is a three-dimensional (3D) "
	  						+ "modeling and inversion code designed for subsurface imaging and monitoring using static and time-lapse 3D electrical resistivity (ER) or spectral induced polarization (SIP) data.");	
	  			}
	  		});
	  		
	  		// Save the E4D files
	  		Button writeE4DButton = new Button(composite_E4D, SWT.PUSH);
	  		writeE4DButton.setText("  Write E4D File  ");
	  		writeE4DButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					
					// Begin by identifying the parameter to build the file from
					List<String> list = new ArrayList<String>();
					String selectedParameter = null;
					int maximumWells = 30; //default value
					for(String label: sensorData.keySet()) {
						if (label.contains("Pressure"))
							list.add(label);
					}
					if (list.size() > 0) { // If pressure parameters are detected, open dialog
						E4DDialog dialog = new E4DDialog(container.getShell(), list);
						dialog.open();
						selectedParameter = dialog.getParameter();
						maximumWells = dialog.getMaximumWells();
						if(dialog.getReturnCode() == 1) // If the dialog box is closed, do nothing
							return;
					} else if (list.isEmpty()) { // If no pressure parameters, throw error
						DREAMWizard.errorMessage.setText("No pressure parameter exists to create an E4D file.");
						return;
					}
					
					// Returns the best well that fall within the threshold (30 by default)
					ArrayList<Point3i> wells = null;
					try {
						wells = data.runWellOptimizationE4D(selectedParameter, maximumWells);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					// For this to be empty, no change was seen at any node with the selected parameter (very rare)
					if (wells==null) {
						DREAMWizard.errorMessage.setText("No change was detected with the selected pressure parameter.");
						return;
					}
					
					// Now that we have our wells, print it out
					StringBuilder ijStringBuilder = new StringBuilder();
					for(Point3i well: wells)
						ijStringBuilder.append(Point3i.toCleanString(well) + "\n");
					File e4dWellFile = new File(Constants.userDir, "e4d" + File.separator + "ertWellLocationsIJ_" + data.getSet().getScenarioEnsemble() + "_" + data.getSet().getScenarios().size() + ".txt");
					try{
						e4dWellFile.createNewFile();
						FileUtils.writeStringToFile(e4dWellFile, ijStringBuilder.toString());
						MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
						dialog.setText("Write E4D File: Success");
						dialog.setMessage("An E4D file was created that provides the " + maximumWells + " best well locations across all scenarios based on the " + selectedParameter + " parameter. "
								+ "E4D will use these well locations to reduce computatational time.\n\nDirectory: " + e4dWellFile.getAbsolutePath());
						dialog.open();
					} catch (IOException e1) {
						MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
						dialog.setText("Write E4D File: Failed");
						dialog.setMessage("The program was unable to write out the optimized E4D well locations.\n\nDirectory: " + e4dWellFile.getAbsolutePath());
						dialog.open();
						e1.printStackTrace();
					}
					runE4DButton.setEnabled(e4dWellList.exists());
				}
			});
	  		
	  		// If the user has a well list that matches the scenario ensemble and size, allow the run E4D button to show up
	  		runE4DButton = new Button(composite_E4D, SWT.PUSH);
	  		runE4DButton.setText("  Run E4D  ");
	  		runE4DButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					
					
					String[] list = new String[3];
					list[0] = list[1] = list[2] = "";
					for(String label: sensorData.keySet()) {
						if(label.toLowerCase().contains("brine saturation") || label.toLowerCase().contains("aqueous saturation"))
							list[0] = label;
						if(label.toLowerCase().contains("gas saturation"))
							list[1] = label;
						if(label.toLowerCase().contains("salt") || label.toLowerCase().contains("salinity"))
							list[2] = label;
					}
					E4DRunDialog dialog = new E4DRunDialog(container.getShell(), data.getSet().getScenarioEnsemble(), list[0], list[1], list[2], sensorData);
					dialog.open();
					if(dialog.getReturnCode() == 1) // If the dialog box is closed, do nothing
						return;
					if(System.getProperty("os.name").contains("Windows")) { // TODO: Is there a different script to run the Mac version?
						try {
							data.runE4DWindows(dialog, e4dWellList);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					loadPage(false);
				}
	  		});
	  		runE4DButton.setEnabled(e4dWellList.exists());
		}
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		fixMacBug();
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if(Constants.skipToEnd || Constants.autoTest) {
			if(Constants.fileType == FileType.H5) {
				//sensorData.get("Aqueous Pressure").isIncluded = true;
				//sensorData.get("Aqueous Pressure").detectionThreshold = 0.002f;
				//sensorData.get("gravity").isIncluded = true;
				//sensorData.get("gravity").detectionThreshold = 0.2f;
				sensorData.get("pressure").isIncluded = true;
				sensorData.get("pressure").detectionThreshold = 20f;
			} else if(Constants.fileType == FileType.IAM) {
				sensorData.get("Pressure").isIncluded = true;
			}
			DREAMWizard.errorMessage.setText("");
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
		}
	} //ends load page
	
	@Override
	public void createControl(final Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());
		
		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
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
		GridData compositeData = new GridData(GridData.FILL, GridData.FILL, true, true);
		compositeData.heightHint = 400;
		compositeData.minimumHeight = 400;
		sc.setLayoutData(compositeData);
        sc.setExpandHorizontal(true);
        sc.getVerticalBar().setIncrement(20);
        sc.setExpandVertical(true);
        
        container = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 8;
		layout.verticalSpacing = 8;
		layout.numColumns = 9;
		container.setLayout(layout);
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setControl(rootContainer);
		setPageComplete(true);
	}
	
	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		
		// Checks if there are any new sensor settings to be added to detectionMap
		// Also saves sensorSetting information (i.e. cloudNodes, detectableNodes, sensorAliases, etc.)
		if(changed) {
			findDetectableNodes();
		}
		
		// May need to remove unselected sensor settings
		for(SensorData sensor : sensorData.values()) {
			if (!sensor.isIncluded)
				data.getSet().removeSensorSettings(sensor.sensorName);
		}
		
		// Count the total nodes to verify that some detectable nodes were found
		HashSet<Integer> nodes = new HashSet<Integer>();
		for(String label: data.getSet().getSensorSettings().keySet())
			nodes.addAll(data.getSet().getSensorSettings().get(label).getDetectableNodes());
		if(nodes.size()==0)
			errorFound(true, "  No nodes were found for the provided parameters.");
		
		// Write out some setup information
		System.out.println("Number of sensors = " + data.getSet().getSensorSettings().size());
		System.out.println("Number of time steps = " + data.getSet().getNodeStructure().getTimeSteps().size());
		Float firstTime = data.getSet().getNodeStructure().getTimeAt(0);
		System.out.println("Average volume of aquifer degraded at first time step (" + firstTime.toString() + ") = " + data.getSet().getAverageVolumeDegradedAtTimesteps().get(firstTime).toString());
		Float lastTime = data.getSet().getNodeStructure().getTimeAt(data.getSet().getNodeStructure().getTimeSteps().size()-1);
		System.out.println("Average volume of aquifer degraded at last time step (" + lastTime.toString() + ") = " + data.getSet().getAverageVolumeDegradedAtTimesteps().get(lastTime).toString());
	}
	
	//We want to do the same process when the page is completed or the "Find Detectable Nodes" is selected
	private void findDetectableNodes() {
		//Removal portion of the code where it removes the sensor name from sensor settings.
		// sensorSettings needs to only have selected sensors now
		// Also create a map of sensors where we need to find nodes
		ArrayList<SensorData> newSensors = new ArrayList<SensorData>();
		ArrayList<SensorData> activeSensors = new ArrayList<SensorData>();
		
		// Loop through the input sensor data
		for(String label: sensorData.keySet()) {
			SensorData sensor = sensorData.get(label);
			
			if (!sensor.isIncluded)
				// Remove from sensorSettings if the sensor is not included
				data.getSet().removeSensorSettings(sensor.sensorName);
			else {
				// Add the sensor to sensorSettings
				data.getSet().addSensorSetting(sensor.sensorName, sensor.parameter);
				// Set values in sensorSettings from sensorData
				data.getSet().getSensorSettings(sensor.sensorName).setUserSettings(sensor.cost, sensor.detectionThreshold,
						sensor.trigger, sensor.deltaType, sensor.bottomZ, sensor.topZ, sensor.alias, sensor.sensorType);
				// Set the specificType
				sensor.specificType = data.getSet().getSensorSettings(label).getSpecificType();
				activeSensors.add(sensor);
				if(!data.getSet().getDetectionMap().containsKey(sensor.specificType) && !sensor.parameter.contains("Electrical Conductivity"))
					newSensors.add(sensor); //if these settings are new to the detectionMap, we need to add them
			}
			data.getSet().addSensorAlias(label, sensor.alias);
		}
		
		// Based on the list of H5 sensors above, add results to detectionMap
		// Calculate the sum of nodes that detect (cloudNodes)
		// Run pareto optimization to get the final set of detectable nodes (detectableNodes)
		data.setupSensors(newSensors, activeSensors);
		
		// Write the number of detectable nodes to the display
		for(String label: sensorData.keySet()) {
			SensorData sensor = sensorData.get(label);
			if(sensor.isIncluded) {
				sensor.solutionNodeLabel.setText(label+": "+data.getSet().getSensorSettings(label).getDetectableNodes().size());
			} else {
				sensor.solutionNodeLabel.setText(label+": Not set");
			}
		}
		// Changing detectable space or selected parameters may change all future pages
		DREAMWizard.resetPages(false, false, false, true, true, true, true);
		changed = false;
	}
	
	//Hack to fix a bug on mac that would replace the contents of whatever field was selected with the alias of the first selected monitoring parameter.
	//This gets around the problem by selecting that alias field so that it replaces itself - not a real fix to the problem.
	public void fixMacBug() {
		if(System.getProperty("os.name").contains("Mac")) {
			for(String sensor : sensorData.keySet()) {
				if(sensorData.get(sensor).isIncluded) {
					sensorData.get(sensor).aliasText.setFocus();
					break;
				}
			}
		}
	}
	
	public class TextWithSuffix {
		
        public TextWithSuffix(final Composite parent, Composite baseComposite, Text text, Label label, float value, String suffix) {
            // The border gives the appearance of a single component
            baseComposite = new Composite(parent, SWT.BORDER);
            baseComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            final GridLayout baseCompositeGridLayout = new GridLayout(2, false);
            baseCompositeGridLayout.marginHeight = 0;
            baseCompositeGridLayout.marginWidth = 0;
            baseComposite.setLayout(baseCompositeGridLayout);
            
            // You can set the background color and force it on the children (the Text and Label objects) to create the illusion of a single component
            baseComposite.setBackground(Constants.white);
            baseComposite.setBackgroundMode(SWT.INHERIT_FORCE);
            
            text = new Text(baseComposite, SWT.SINGLE | SWT.RIGHT);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            
            label = new Label(baseComposite, SWT.NONE);
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            label.setText(suffix);
        }
    }
	
	private String getCostToolTip(SensorType sensorType) {
		String timeUnit = (data.getSet().getNodeStructure().getUnit("times")==null ? "" : 
			" (" + data.getSet().getNodeStructure().getUnit("times") + ")");
		String areaUnit = (data.getSet().getNodeStructure().getUnit("x")==null ? "" :
			" (" + data.getSet().getNodeStructure().getUnit("x") + "^2)");
		String toolTip = "Enter a fixed value or an equation with 't' representing time"+timeUnit+ "and "
				+ "'i' representing the number of installations, in case the sensor is allowed to move";
		if(sensorType == SensorType.SURFACE)
			toolTip = "Enter a fixed value or an equation with 's' representing the number of surface surveys "
					+ "over time and 'a' representing the spatial area of the surface measurements" + areaUnit;
		return toolTip;
	}
	
	private String getDetectionTypeToolTip(Trigger trigger) {
		String toolTip = "Leak when concentration is below value";
		if(trigger == Trigger.ABOVE_THRESHOLD)
			toolTip = "Leak when concentration is above value";
		else if(trigger == Trigger.RELATIVE_CHANGE)
			toolTip = "Leak when change from original concentration relative to the initial concentration (decimal) exceeds value";
		else if(trigger == Trigger.ABSOLUTE_CHANGE)
			toolTip = "Leak when change from original concentration exceeds value";
		return toolTip;
	}
	
	private String getMethodToolTip(SensorType sensorType) {
		String toolTip = "This technology measures parameters at a point";
		if(sensorType == SensorType.SURFACE)
			toolTip = "This technology uses periodic surface surveys to detect changes";
		/*else if(sensorType == SensorType.BOREHOLE)
			toolTip = "This technology uses periodic surveys down a borehole to detect changes";
		else if(sensorType == SensorType.CROSS_WELL)
			toolTip = "This technology measures changes in parameters between two wells";*/
		return toolTip;
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