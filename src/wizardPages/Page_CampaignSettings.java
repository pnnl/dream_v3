package wizardPages;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import objects.SensorSetting;
import objects.SensorSetting.SensorType;
import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;

/**
 * Set limitation for the algorithms. See line 131
 * 
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */

public class Page_CampaignSettings extends DreamWizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private GridData compositeGridData; //for composite with units
	private GridLayout compositeGridLayout; //for composite with units
	private STORMData data;

	//private Text costConstraint;
	private Text sensor1;
	private Text sensor2;
	private Text maxCost;
	private Text maxMoves;
	private Text maxWells;
	private Text exclusionRadius;
	private Text wellCost;
	private Text wellDepthCost;
	private Text stationLocation1;
	private Text stationLocation2;
	private Text maxSurveys;
	private String unit;
	private String costToolTip;
	
	private Group pointSensorGroup;
	private Group surfaceGroup;
	private Image questionMark;

	private boolean isCurrentPage = false;
	public static boolean changed = true;

	protected Page_CampaignSettings(STORMData data) { //TODO: refactor to "CampaignConstraints"
		super("Campaign Settings");
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
		layout.horizontalSpacing = 8;
		layout.verticalSpacing = 8;
		container.setLayout(layout);
		layout.numColumns = 4;
		questionMark = new Image(container.getDisplay(), getClass().getResourceAsStream("/QuestionMark.png"));

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		// We want to display units within the same text box, so we need to do some fancy magic
		// Essentially we are creating a composite with two fields within it: (1) prefix symbol and (2) cost value
		// It should look *mostly* the same as other input fields, but the user is unable to edit units
		// GridData layout for the main composite
		compositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		// GridLayout for the two fields within the composite
		compositeGridLayout = new GridLayout(2, false);
		compositeGridLayout.marginHeight = 1;
		compositeGridLayout.marginWidth = 0;
		compositeGridLayout.horizontalSpacing = 0;

		setControl(rootContainer);
		setPageComplete(true);
	}

	/**
	 * Loads Campaign Settings Page.
	 */
	@Override
	public void loadPage(boolean reset) {

		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		DREAMWizard.convertDataButton.setEnabled(false);
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.nextButton.setVisible(true);
		removeChildren(container);
		
		unit = data.getSet().getNodeStructure().getUnit("x");
		costToolTip = "Enter a fixed value or an equation with \'t\' representing time" + 
				(data.getSet().getNodeStructure().getUnit("times")=="" ? "" : 
					" (" + data.getSet().getNodeStructure().getUnit("times") + ")") +
				". Use \'0\' to not constrain with this field.";
		
		createCampaignSettingsLabel();

		createInfoLinkLabel();

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel.setText("Are there any cost or physical constraints on the monitoring campaign?");
		infoLabel.setLayoutData(theGridDataSpecifications());
		
		// Track whether we have different types of sensors
		boolean hasPointSensor = false;
		boolean hasSurface = false;
		for(SensorSetting sensorSetting: data.getSet().getSensorSettings().values()) {
			if(sensorSetting.getSensorType()==SensorType.SURFACE)
				hasSurface = true;
			else
				hasPointSensor = true;
		}
		
		// Tooltip for cost
		costToolTip = "Enter a fixed value or an equation with \'t\' representing time" + 
				(data.getSet().getNodeStructure().getUnit("times")=="" ? "" : 
					" (" + data.getSet().getNodeStructure().getUnit("times") + ")") + ".";
		
		createMaxCostLabel();
		
		createSensorLabels();

		//// Point Sensor Constraints ////
		if(hasPointSensor) {
			pointSensorGroup = new Group(container, SWT.SHADOW_NONE);
			pointSensorGroup.setText("Point Sensors");
			pointSensorGroup.setFont(Constants.boldFontSmall);
			pointSensorGroup.setLayout(new GridLayout(4,false));
			pointSensorGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

			createMaxMovesLabel();

			createMaximumWellsLabel();
	
			createMinimumWellLabel();
			
			createWellCostLabel();
	
			createCostWellDepthLabel();
		}

		//// Surface Monitoring ////
		if (hasSurface) {
			surfaceGroup = new Group(container, SWT.SHADOW_NONE);
			surfaceGroup.setText("Surface Surveys");
			surfaceGroup.setFont(Constants.boldFontSmall);
			surfaceGroup.setLayout(new GridLayout(4, false));
			surfaceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

			createStationLocationLabels();
			
			createMaxSurveysLabel();
		}

		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if(reset && (Constants.skipToEnd || Constants.autoTest))
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * Info label for the monitoring campaign settings.
	 * 
	 * @param TheBoldFont - The font used.
	 */
	private void createCampaignSettingsLabel() {
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel1.setText("Monitoring Campaign Settings");
		infoLabel1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, ((GridLayout)container.getLayout()).numColumns - 1, 2));
		infoLabel1.setFont(Constants.boldFont);
	}

	/**
	 * This method creates the information that details the additional information.
	 */
	private void createInfoLinkLabel() {
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener() {
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
	}

	/**
	 * This method sets the GridData's specifications.
	 * 
	 * @return infoGridData - The specifications for the GridData.
	 */
	private GridData theGridDataSpecifications() {
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		return infoGridData;
	}
	
	/**
	 * This method creates the text for the minimum and maximum number of sensors. If there
	 * is an error (i.e. not a number), the text will turn red color, otherwise black.
	 */
	private void createSensorLabels() {
		Composite sensorComposite = new Composite(container, SWT.NULL);
		sensorComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		sensorComposite.setLayout(new GridLayout(2, false));
		Label sensorLabel = new Label(sensorComposite, SWT.LEFT);
		sensorLabel.setText("Number of Sensors");
		CLabel sensorQ = new CLabel(sensorComposite, SWT.NULL);
		sensorQ.setImage(questionMark);
		sensorQ.setBottomMargin(0);
		sensorQ.setTopMargin(0);
		sensorQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Number of Sensors Help", "Determines a range for the number of "
						+ "sensors that can be included in a monitoring campaign. A sensor is defined as either a point sensor that "
						+ "can be relocated, or a surface survey of fixed size that can be repeated at multiple times. The algorithm "
						+ "will explore different numbers of sensors, but this sets hard limits on how many sensors can be included.");
			}
		});
		
		sensor1 = new Text(container, SWT.BORDER | SWT.SINGLE);
		sensor1.setText(String.valueOf(data.getSet().getMinSensors()));
		sensor1.setForeground(Constants.black);
		sensor1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1)); // Lambda function
		sensor1.setToolTipText("Minimum number of monitoring technologies:\n."
				+ "- Individual point sensors that can be moved if sensor settings allow,\n"
				+ "- Individual surveys that span a consistent area and each have multiple redeployments across time");
		sensor1.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidInt(((Text)theEvent.getSource()).getText());
				int value = Integer.parseInt(((Text)theEvent.getSource()).getText());
				boolean zeroError = value < 1;
				if (numError || zeroError)
					((Text)theEvent.getSource()).setForeground(Constants.red);
				else
					((Text)theEvent.getSource()).setForeground(Constants.black);
				if(!numError && !zeroError && Constants.isValidInt(sensor2.getText())) {
					int value2 = Integer.parseInt(sensor2.getText());
					if(value2 > 0) {
						if(value < value2) {
							data.getSet().setMinSensors(value);
							data.getSet().setMaxSensors(value2);
						} else {
							data.getSet().setMinSensors(value2);
							data.getSet().setMaxSensors(value);
						}
					}
				}
				errorFound(numError, "Min sensors is not a real number. ");
				errorFound(zeroError, "Min sensors must be greater than 0. ");
			} catch (Exception theException) {
				System.out.println("Error parsing sensor 1");
			}
		});
		
		Label separatorLabel = new Label(container, SWT.NULL);
		separatorLabel.setText(" - ");
		sensor2 = new Text(container, SWT.BORDER | SWT.SINGLE);
		sensor2.setText(String.valueOf(data.getSet().getMaxSensors()));
		sensor2.setForeground(Constants.black);
		sensor2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1)); // Lambda function
		sensor2.setToolTipText("Maximum number of monitoring technologies:\n."
				+ "- Individual point sensors that can be moved if sensor settings allow,\n"
				+ "- Individual surveys that span a consistent area and each have multiple redeployments across time");
		sensor2.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidInt(((Text)theEvent.getSource()).getText());
				int value = Integer.parseInt(((Text)theEvent.getSource()).getText());
				boolean zeroError = value < 1;
				if (numError || zeroError)
					((Text)theEvent.getSource()).setForeground(Constants.red);
				else
					((Text)theEvent.getSource()).setForeground(Constants.black);
				if(!numError && !zeroError && Constants.isValidInt(sensor1.getText())) {
					int value1 = Integer.parseInt(sensor1.getText());
					if(value1 > 0) {
						if(value1 < value) {
							data.getSet().setMinSensors(value1);
							data.getSet().setMaxSensors(value);
						} else {
							data.getSet().setMinSensors(value);
							data.getSet().setMaxSensors(value1);
						}
					}
				}
				errorFound(numError, "Max sensors is not a real number. ");
				errorFound(zeroError, "Max sensors must be greater than 0. ");
			} catch (Exception theException) {
				System.out.println("Error parsing sensor 2");
			}
		});
	}
	
	/**
	 * This method creates the text for the cost constraints. If there is a error
	 * (i.e not a number) text will turn red color, otherwise black.
	 */
	private void createMaxCostLabel() {
		Composite maxCostLabelComposite = new Composite(container, SWT.NULL);
		maxCostLabelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		maxCostLabelComposite.setLayout(new GridLayout(3, false));
		Label maxCostLabel = new Label(maxCostLabelComposite, SWT.LEFT);
		maxCostLabel.setText("Maximum Monitoring Budget");
		CLabel maxCostQ = new CLabel(maxCostLabelComposite, SWT.NULL);
		maxCostQ.setImage(questionMark);
		maxCostQ.setBottomMargin(0);
		maxCostQ.setTopMargin(0);
		maxCostQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Maximum Monitoring Budget Help", "Sets a cap on the total cost for "
						+ "a campaign, including sensor costs and well costs defined below. Cost is an optimization objective, so it is "
						+ "not necessary to set a hard maximum. Setting a maximum that is too restrictive may cause problems for the "
						+ "algorithm.");
			}
		});
		Label fillLabel = new Label(maxCostLabelComposite, SWT.LEFT);
		fillLabel.setText("               ");
		
		// The border gives the appearance of a single component
		Composite maxCostComposite = new Composite(container, SWT.BORDER);
		maxCostComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		maxCostComposite.setLayout(compositeGridLayout); //Specifies two fields for composite
		maxCostComposite.setBackground(Constants.white);
		
		Label maxCostUnit = new Label(maxCostComposite, SWT.NULL);
		maxCostUnit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		maxCostUnit.setText(data.getSet().getCostUnit());
		maxCostUnit.setForeground(Constants.grey);
		maxCostUnit.setBackground(Constants.white);
		maxCostUnit.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				maxCost.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
				maxCost.setSelection(0); //Sets the cursor to the front of the text
			}
		});
		
		maxCost = new Text(maxCostComposite, SWT.SINGLE | SWT.LEFT);
		if(data.getSet().getMaxCost().equals("3E38")) {
			maxCost.setMessage("No Limit");
		} else {
			maxCost.setText(data.getSet().getMaxCost());
			maxCost.setForeground(Constants.black);
		}
		maxCost.setToolTipText(costToolTip);
		maxCost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); // Lambda function
		maxCost.addModifyListener(theEvent -> {
			try {
				//Remove all spaces and trailing math operators
				String equation = ((Text)theEvent.getSource()).getText().replaceAll("\\s", "").replaceAll("[-+*/^(]$", "").toLowerCase();
				boolean eqError = !Constants.isValidCostEquation(equation, data.getSet().getNodeStructure().getTimeSteps());
				if(eqError)
					((Text)theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setMaxCost(equation);
				}
				errorFound(eqError, "Max cost is not a real equation. ");
			} catch (Exception theException) {
				System.out.println("Error parsing max cost");
			}
		});
	}
	
	/**
	 * Creates the label text for maximum point sensor moves.
	 */
	private void createMaxMovesLabel() {
		Composite maxMovesLabelComposite = new Composite(pointSensorGroup, SWT.NULL);
		maxMovesLabelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		maxMovesLabelComposite.setLayout(new GridLayout(2, false));
		Label maxMovesLabel = new Label(maxMovesLabelComposite, SWT.LEFT);
		maxMovesLabel.setText("Maximum number of relocations");
		CLabel maxMovesQ = new CLabel(maxMovesLabelComposite, SWT.NULL);
		maxMovesQ.setImage(questionMark);
		maxMovesQ.setBottomMargin(0);
		maxMovesQ.setTopMargin(0);
		maxMovesQ.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Maximum Number of Relocations Help", "Point sensors are installed "
						+ "in a single location, but they can be removed from the original installation location and moved to a new "
						+ "location. This number determines how many relocations (or moves) are allowed by each sensor.");
			}
		});

		maxMoves = new Text(pointSensorGroup, SWT.BORDER | SWT.SINGLE);
		maxMoves.setText(String.valueOf(data.getSet().getMaxMoves()));
		maxMoves.setForeground(Constants.black);
		maxMoves.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		maxMoves.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidFloat(((Text) theEvent.getSource()).getText());
				boolean negError = Float.parseFloat(((Text) theEvent.getSource()).getText()) < 0;
				if (numError || negError)
					((Text) theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setMaxMoves(Integer.parseInt(((Text) theEvent.getSource()).getText()));
				}
				errorFound(negError, "Max redeployments is a negative number. ");
				errorFound(numError, "Max redeployments is not a real number. ");
			} catch (Exception theException) {
				System.out.println("Error parsing max redeployments");
			}
		});
	}

	/**
	 * Creates the label text for the maximum number of wells. Takes directly from
	 * the data set.
	 */
	private void createMaximumWellsLabel() {
		Composite maxWellsComposite = new Composite(pointSensorGroup, SWT.NULL);
		maxWellsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		maxWellsComposite.setLayout(new GridLayout(2, false));
		Label maxWellsLabel = new Label(maxWellsComposite, SWT.LEFT);
		maxWellsLabel.setText("Maximum Number of Wells");
		CLabel maxWellsQ = new CLabel(maxWellsComposite, SWT.NULL);
		maxWellsQ.setImage(questionMark);
		maxWellsQ.setBottomMargin(0);
		maxWellsQ.setTopMargin(0);
		maxWellsQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Maximum Number of Wells Help", "Sets a cap on the number of "
						+ "vertical wells that can be included in a monitoring campaign. The algorithm will explore different numbers "
						+ "of wells, and the number of wells will effect the cost objective, but this sets a hard limit on how many "
						+ "wells can be considered.");
			}
		});
		
		maxWells = new Text(pointSensorGroup, SWT.BORDER | SWT.SINGLE);
		if(data.getSet().getMaxWells()==1000000) {
			maxWells.setMessage("No Limit");
		}
		else {
			maxWells.setText(String.valueOf(data.getSet().getMaxWells()));
			maxWells.setForeground(Constants.black);
		}
		maxWells.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1)); // Lambda function
		maxWells.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidInt(((Text) theEvent.getSource()).getText());
				boolean negError = Integer.parseInt(((Text) theEvent.getSource()).getText()) < 0;
				if (numError || negError)
					((Text) theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setMaxWells(Integer.parseInt(((Text) theEvent.getSource()).getText()));
				}
				errorFound(negError, "Max wells is a negative number. ");
				errorFound(numError, "Max wells is not an integer. ");
			} catch (Exception theException) {
				System.out.println("Error parsing max wells");
			}
		});
	}

	/**
	 * Creates the label text for the minimum distance between wells.
	 */
	private void createMinimumWellLabel() {
		Composite minWellsComposite = new Composite(pointSensorGroup, SWT.NULL);
		minWellsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		minWellsComposite.setLayout(new GridLayout(2, false));
		Label minWellsLabel = new Label(minWellsComposite, SWT.LEFT);
		minWellsLabel.setText("Minimum Distance Between Wells" + (unit.equals("") ? "" : " (" + unit + ")"));
		CLabel minWellsQ = new CLabel(minWellsComposite, SWT.NULL);
		minWellsQ.setImage(questionMark);
		minWellsQ.setBottomMargin(0);
		minWellsQ.setTopMargin(0);
		minWellsQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Minimum Distance Between Wells Help", "Constrains the wells to "
						+ "be a minimum distance from other wells. DREAM is limited by the model resolution that makes up the domain "
						+ "space and assumes that wells are placed in the center of the cell, for the purposes of distance calculations. "
						+ "In reality, a decision-maker will micro-site the recommended monitoring campaign to a location that makes sense.");
			}
		});
		
		exclusionRadius = new Text(pointSensorGroup, SWT.BORDER | SWT.SINGLE);
		exclusionRadius.setText(String.valueOf(data.getSet().getExclusionRadius()));
		exclusionRadius.setForeground(Constants.black);
		exclusionRadius.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1)); // Lambda function
		exclusionRadius.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidFloat(((Text) theEvent.getSource()).getText());
				boolean negError = Float.parseFloat(((Text) theEvent.getSource()).getText()) < 0;
				if (numError || negError)
					((Text) theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setExclusionRadius(Float.parseFloat(((Text) theEvent.getSource()).getText()));
				}
				errorFound(negError, "Min distance is a negative number. ");
				errorFound(numError, "Min distance is not a real number. ");
			} catch (Exception theException) {
				System.out.println("Error parsing minimum distance between wells");
			}
		});
	}

	/**
	 * Creates the label text for the cost per well.
	 */
	private void createWellCostLabel() {
		String timeUnit = data.getSet().getNodeStructure().getUnit("times");
		
		Composite wellCostLabelComposite = new Composite(pointSensorGroup, SWT.NULL);
		wellCostLabelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		wellCostLabelComposite.setLayout(new GridLayout(2, false));
		Label wellCostLabel = new Label(wellCostLabelComposite, SWT.LEFT);
		wellCostLabel.setText("Cost Per Well");
		CLabel wellCostQ = new CLabel(wellCostLabelComposite, SWT.NULL);
		wellCostQ.setImage(questionMark);
		wellCostQ.setBottomMargin(0);
		wellCostQ.setTopMargin(0);
		wellCostQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Well Cost Help", "Cost for each vertical well, which can be entered "
						+ "as a fixed value or an equation. An equation can allow 't' to represent the number of "+timeUnit+" that the "
						+ "well is maintained.");
			}
		});
		
		// The border gives the appearance of a single component
		Composite wellCostComposite = new Composite(pointSensorGroup, SWT.BORDER);
		wellCostComposite.setLayoutData(compositeGridData);
		wellCostComposite.setLayout(compositeGridLayout); //Specifies two fields for composite
		wellCostComposite.setBackground(Constants.white);
		
		Label wellCostUnit = new Label(wellCostComposite, SWT.NONE);
		wellCostUnit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		wellCostUnit.setText(data.getSet().getCostUnit());
		wellCostUnit.setForeground(Constants.grey);
		wellCostUnit.setBackground(Constants.white);
		wellCostUnit.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				wellCost.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
				wellCost.setSelection(0); //Sets the cursor to the front of the text
			}
		});
		
		wellCost = new Text(wellCostComposite, SWT.SINGLE | SWT.LEFT);
		wellCost.setText(String.valueOf(data.getSet().getWellCost()));
		wellCost.setForeground(Constants.black);
		wellCost.setToolTipText(costToolTip);
		wellCost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); // Lambda function
		wellCost.addModifyListener(theEvent -> {
			try {
				//Remove all spaces and trailing math operators
				String equation = ((Text)theEvent.getSource()).getText().replaceAll("\\s", "").replaceAll("[-+*/^(]$", "").toLowerCase();
				boolean eqError = !Constants.isValidCostEquation(equation, data.getSet().getNodeStructure().getTimeSteps());
				if(eqError)
					((Text)theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setWellCost(equation);
				}
				errorFound(eqError, "Well cost is not a real equation. ");
			} catch (Exception theException) {
				System.out.println("Error parsing well cost");
			}
		});
	}

	/**
	 * Creates the label text for the cost per well depth.
	 */
	private void createCostWellDepthLabel() {
		Composite wellDepthCostLabelComposite = new Composite(pointSensorGroup, SWT.NULL);
		wellDepthCostLabelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		wellDepthCostLabelComposite.setLayout(new GridLayout(2, false));
		Label wellDepthCostLabel = new Label(wellDepthCostLabelComposite, SWT.LEFT);
		wellDepthCostLabel.setText("Cost of Well Per " + (unit == "" ? "Unit" : unit) + " Depth");
		CLabel wellDepthCostQ = new CLabel(wellDepthCostLabelComposite, SWT.NULL);
		wellDepthCostQ.setImage(questionMark);
		wellDepthCostQ.setBottomMargin(0);
		wellDepthCostQ.setTopMargin(0);
		wellDepthCostQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Well Cost per Depth Help", "Cost for each "+unit+" depth "
						+ "for all vertical wells in a monitoring campaign. Cost must be entered as a fixed value. ");
			}
		});
		
		// The border gives the appearance of a single component
		Composite wellDepthCostComposite = new Composite(pointSensorGroup, SWT.BORDER);
		wellDepthCostComposite.setLayoutData(compositeGridData);
		wellDepthCostComposite.setLayout(compositeGridLayout); //Specifies two fields for composite
		wellDepthCostComposite.setBackground(Constants.white);
		
		Label wellDepthCostUnit = new Label(wellDepthCostComposite, SWT.NONE);
		wellDepthCostUnit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		wellDepthCostUnit.setText(data.getSet().getCostUnit());
		wellDepthCostUnit.setForeground(Constants.grey);
		wellDepthCostUnit.setBackground(Constants.white);
		wellDepthCostUnit.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				wellDepthCost.setFocus(); //This makes it so when a user clicks the label, the cursor instead goes to the text
				wellDepthCost.setSelection(0); //Sets the cursor to the front of the text
			}
		});
		
		wellDepthCost = new Text(wellDepthCostComposite, SWT.SINGLE);
		wellDepthCost.setText(String.valueOf(data.getSet().getWellDepthCost()));
		wellDepthCost.setForeground(Constants.black);
		wellDepthCost.setToolTipText("Enter a fixed value.");
		wellDepthCost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); // Lambda function
		wellDepthCost.addModifyListener(theEvent -> {
			try {
				//Remove all spaces and trailing math operators
				String equation = ((Text)theEvent.getSource()).getText().replaceAll("\\s", "").replaceAll("[-+*/^(]$", "").toLowerCase();
				equation.replaceAll("t", ""); //this cost doesn't allow time variable
				boolean eqError = !Constants.isValidCostEquation(equation, data.getSet().getNodeStructure().getTimeSteps());
				if(eqError)
					((Text)theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setWellDepthCost(equation);
				}
				errorFound(eqError, "Depth cost is not a real equation. ");
			} catch (Exception theException) {
				System.out.println("Error parsing well depth cost");
			}
		});
	}
	
	/**
	 * Creates the label text for the station locations min/max
	 */
	private void createStationLocationLabels() {
		Composite stationComposite = new Composite(surfaceGroup, SWT.NULL);
		stationComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		stationComposite.setLayout(new GridLayout(2, false));
		Label sensorLabel = new Label(stationComposite, SWT.LEFT);
		sensorLabel.setText("Number of Station Locations Allowed");
		CLabel sensorQ = new CLabel(stationComposite, SWT.NULL);
		sensorQ.setImage(questionMark);
		sensorQ.setBottomMargin(0);
		sensorQ.setTopMargin(0);
		sensorQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Number of Station Locations Allowed Help", "Determines a range "
						+ "for the number of nodes that can be included in a single survey. Since surveys often measure change between "
						+ "surveys, survey size will remain constant across time for a given sensor. By default, size is set at 5%-40% "
						+ "of the total surface nodes. Hovering over this field shows a tooltip with the total number of available "
						+ "surface nodes.");
			}
		});
		
		int maxNodes = data.getSet().getNodeStructure().getTotalSurfaceNodes();
		stationLocation1 = new Text(surfaceGroup, SWT.BORDER | SWT.SINGLE);
		stationLocation1.setText(String.valueOf(data.getSet().getMinSurveyLocations()));
		stationLocation1.setForeground(Constants.black);
		stationLocation1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		stationLocation1.setToolTipText("Min number of nodes for each survey, out of "+maxNodes+" surface nodes.");
		stationLocation1.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidInt(((Text)theEvent.getSource()).getText()); //a real int
				int value = Integer.parseInt(((Text)theEvent.getSource()).getText());
				boolean zeroError = value < 1; //non-zero
				boolean maxError = value > maxNodes; //less than total nodes
				if (numError || zeroError || maxError)
					((Text)theEvent.getSource()).setForeground(Constants.red);
				else
					((Text)theEvent.getSource()).setForeground(Constants.black);
				if(!numError && !zeroError && !maxError && Constants.isValidInt(stationLocation2.getText())) {
					int value2 = Integer.parseInt(stationLocation2.getText());
					if(value2 > 0 && value2 <= maxNodes) {
						if(value2 > value) {
							data.getSet().setMinSurveyLocations(value);
							data.getSet().setMaxSurveyLocations(value2);
						} else {
							data.getSet().setMinSurveyLocations(value2);
							data.getSet().setMaxSurveyLocations(value);
						}
					}
				}
				errorFound(numError, "Min stations is not a real number. ");
				errorFound(zeroError, "Min stations must be greater than 0. ");
				errorFound(maxError, "Max stations must be less than surface nodes. ");
			} catch (Exception theException) {
				System.out.println("Error parsing station location 1");
			}
		});
		
		Label separatorLabel = new Label(surfaceGroup, SWT.NULL);
		separatorLabel.setText(" - ");
		stationLocation2 = new Text(surfaceGroup, SWT.BORDER | SWT.SINGLE);
		stationLocation2.setText(String.valueOf(data.getSet().getMaxSurveyLoactions()));
		stationLocation2.setForeground(Constants.black);
		stationLocation2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		stationLocation2.setToolTipText("Max number of nodes for each survey, out of "+maxNodes+" surface nodes.");
		stationLocation2.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidInt(((Text)theEvent.getSource()).getText()); //a real int
				int value = Integer.parseInt(((Text)theEvent.getSource()).getText());
				boolean zeroError = value < 1; //non-zero
				boolean maxError = value > maxNodes; //less than total nodes
				if (numError || zeroError || maxError)
					((Text)theEvent.getSource()).setForeground(Constants.red);
				else
					((Text)theEvent.getSource()).setForeground(Constants.black);
				// Only save if both min and max values are valid
				if(!numError && !zeroError && !maxError && Constants.isValidInt(stationLocation1.getText())) {
					int value1 = Integer.parseInt(stationLocation1.getText());
					if(value1 > 0 && value1 <= maxNodes) {
						if(value1 < value) {
							data.getSet().setMinSurveyLocations(value1);
							data.getSet().setMaxSurveyLocations(value);
						} else {
							data.getSet().setMinSurveyLocations(value);
							data.getSet().setMaxSurveyLocations(value1);
						}
					}
				}
				errorFound(numError, "Max stations is not a real number. ");
				errorFound(zeroError, "Max stations must be greater than 0. ");
				errorFound(maxError, "Max stations must be less than surface nodes. ");
			} catch (Exception theException) {
				System.out.println("Error parsing location 2");
			}
		});
	}

	/**
	 * Creates the label text for maximum survey redeployments.
	 */
	private void createMaxSurveysLabel() {
		Composite maxSurveyLabelComposite = new Composite(surfaceGroup, SWT.NULL);
		maxSurveyLabelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		maxSurveyLabelComposite.setLayout(new GridLayout(2, false));
		Label maxSurveyLabel = new Label(maxSurveyLabelComposite, SWT.LEFT);
		maxSurveyLabel.setText("Maximum number of repeated surveys");
		CLabel maxSurveyQ = new CLabel(maxSurveyLabelComposite, SWT.NULL);
		maxSurveyQ.setImage(questionMark);
		maxSurveyQ.setBottomMargin(0);
		maxSurveyQ.setTopMargin(0);
		maxSurveyQ.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Maximum Number of Repeated Surveys Help", "Surface surveys usually "
								+ "repeat measurements at the same locations to identify changes over time. This number sets a limit on the number "
								+ "of times a specific survey may be repeated over time.");
			}
		});

		maxSurveys = new Text(surfaceGroup, SWT.BORDER | SWT.SINGLE);
		maxSurveys.setText(String.valueOf(data.getSet().getMaxSurveys()));
		maxSurveys.setForeground(Constants.black);
		maxSurveys.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		maxSurveys.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidFloat(((Text) theEvent.getSource()).getText());
				boolean negError = Float.parseFloat(((Text) theEvent.getSource()).getText()) < 0;
				if (numError || negError)
					((Text) theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setMaxSurveys(Integer.parseInt(((Text) theEvent.getSource()).getText()));
				}
				errorFound(negError, "Max surveys is a negative number. ");
				errorFound(numError, "Max surveys is not a real number. ");
			} catch (Exception theException) {
				System.out.println("Error parsing minimum distance between wells");
			}
		});
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
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}
}