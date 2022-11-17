package wizardPages;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import dialog.InitUnitsDialog;
import hdf5Tool.HDF5Interface;
import utilities.AutoTesting;
import utilities.Constants;
import utilities.Constants.FileType;
import wizardPages.DREAMWizard.STORMData;

/**
 * Select HDF5 files, IAM files, or DREAM save files.
 * Triggers the node structure to be populated.
 */

public class Page_InputDirectory extends DreamWizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	private String directory = getInputFolder();
	private String[] selectedFiles;
	private boolean selectDirectory = true;
	
	private Composite directoryComposite;
	private GridData directoryGridData;
	private Text directoryText;
	
	private Composite fileComposite;
	private GridData fileGridData;
	private Text fileText;
	
	private boolean isCurrentPage = false;
	private boolean changed = true;
	
	protected Page_InputDirectory(final STORMData data) {
		super("Input Directory");
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

	public void completePage() throws Exception {
		isCurrentPage = false;
		
		// Possible if they had a valid directory cached
		if(selectedFiles==null) {
			boolean fileError = validDirCheck(directory);
			errorFound(fileError, "  Directory must contain an h5 or iam files.");
		}
		
		// Cache directory and save to constants
		saveParentFolder();
		Constants.homeDirectory = directory;
		
		// If we are using a dream save file, get the real selected files before loading ScenarioSet
		if(Constants.fileType==FileType.DREAM) {
			//TODO
		}
		
		if(changed) {
			// Reset everything at this point
			data.getSet().clearRun();
			HDF5Interface.statistics.clear();
			// Read in scenario and parameter information from the files
			data.setupScenarioSet(directory, selectedFiles);
			data.getSet().setupSensorSettings();
			data.getSet().setupInferenceTest();
			data.getSet().setScenarioEnsemble(directory.substring(directory.lastIndexOf(File.separator) + 1));
			DREAMWizard.resetPages(true, true, true, true, true, true, true);
			changed = false;
		}
		
		if(!Constants.skipToEnd && !Constants.autoTest) {
			// Open attribute input dialog if values missing
			boolean missingParameterUnits = true;
			// If at least one of the parameters has units, assume they are all set
			// Parameters like pH and fractions may not have units
			for (String parameter : data.getSet().getNodeStructure().getParameters()) {
				if (data.getSet().getNodeStructure().getUnits().containsKey(parameter)) {
					missingParameterUnits = false;
					break;
				}
			}
			if (data.getSet().getNodeStructure().getUnit("x").equals("")
					|| data.getSet().getNodeStructure().getUnit("times").equals("")
					|| data.getSet().getNodeStructure().getPositive().equals("")
					|| !data.getSet().getNodeStructure().porosityIsSet()
					|| missingParameterUnits) {
				InitUnitsDialog dialog = new InitUnitsDialog(container.getShell(), data.getSet().getNodeStructure(), missingParameterUnits);
				dialog.open();
			}
		}
	}

	@Override
	public void loadPage(boolean reset) {
		
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		DREAMWizard.convertDataButton.setEnabled(true);
		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.nextButton.setVisible(true);
		removeChildren(container);

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel1.setText("Input Directory");
		infoLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLabel1.setFont(Constants.boldFont);

		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener(){ //TODO: We want to link directly to the page on the user manual
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

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel.setText("Browse to a folder or select individual subsurface simulation files (.h5 or .iam). The folder name will become the "
				+ "leak ensemble name.");
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 4));

		// Radio button to allow user to choose a directory or separate files
		Composite radioComposite = new Composite(container, SWT.NULL);
		radioComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		radioComposite.setLayout(new GridLayout(3, false));
		Button radio1 = new Button(radioComposite, SWT.RADIO);
		radio1.setText("Directory");
		radio1.setSelection(selectDirectory);
		radio1.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				selectDirectory = true;
				toggleView();
			}
		});
		Button radio2 = new Button(radioComposite, SWT.RADIO);
		radio2.setText("Files");
		radio2.setSelection(!selectDirectory);
		radio2.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				selectDirectory = false;
				toggleView();
			}
		});
		Image questionMark = new Image(container.getDisplay(), getClass().getResourceAsStream("/QuestionMark.png"));
		CLabel typeQ = new CLabel(radioComposite, SWT.NULL);
		typeQ.setImage(questionMark);
		typeQ.setBottomMargin(0);
		typeQ.setTopMargin(0);
		typeQ.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Input Directory Help", "DREAM can select either a directory or "
						+ "individual files. Three file types are available: H5, IAM, or DREAM save files. H5 files are generated by "
						+ "selecting 'Launch Converter' in the footer, which converts subsurface simulation outputs (e.g., NUFT, STOMP) "
						+ "to a structured file storage with a value for each parameter at each model node and time step. IAM files are "
						+ "generated to support Open-IAM, which simulates leaks as reduced order representations. H5 files are larger, "
						+ "but provide more flexibility as the user can set their own leak and detection values. IAM files are smaller, "
						+ "representing only fixed leak and detection spaces. DREAM save files load a previous set of inputs.");
			}
		});
		
		/////////////////////////////
		//// Directory Selection ////
		/////////////////////////////
		directoryComposite = new Composite(container, SWT.NULL);
		directoryGridData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
		directoryComposite.setLayoutData(directoryGridData);
		directoryComposite.setLayout(new GridLayout(2, false));
		
		Button buttonSelectDir = new Button(directoryComposite, SWT.PUSH);
		buttonSelectDir.setText(" Select a Directory ");
		buttonSelectDir.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
				directoryDialog.setFilterPath(directoryText.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					directory = dir;
					directoryText.setText(dir);
					changed = true;
				}
			}
		});
		
		directoryText = new Text(directoryComposite, SWT.BORDER | SWT.SINGLE | SWT.FILL);
		GridData directoryGrid = new GridData(SWT.FILL, SWT.CENTER, true, false);
		directoryGrid.minimumWidth = 700;
		directoryText.setLayoutData(directoryGrid);
		directoryText.setText(directory);
		directoryText.setForeground(Constants.black);
		directoryText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				File folder = new File(((Text) e.getSource()).getText());
				boolean dirError = !folder.isDirectory(); //check if exists and is directory
				boolean fileError = true; //check if the directory contains allowed files
				if (dirError == true) {
					((Text) e.getSource()).setForeground(Constants.red);
					fileError = false;
				} else {
					((Text) e.getSource()).setForeground(Constants.black);
					directory = ((Text) e.getSource()).getText();
					fileError = validDirCheck(folder.getPath());
				}
				errorFound(dirError, "  Invalid directory.");
				errorFound(fileError, "  Directory must contain an h5 or iam files.");
				changed = true;
			}
		});
		
		////////////////////////
		//// File Selection ////
		////////////////////////
		fileComposite = new Composite(container, SWT.NULL);
		fileGridData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 5);
		fileComposite.setLayoutData(fileGridData);
		fileComposite.setLayout(new GridLayout(2, false));
		
		Button buttonSelectFile = new Button(fileComposite, SWT.PUSH);
		buttonSelectFile.setText("   Select Files   ");
		buttonSelectFile.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog fileDialog = new FileDialog(container.getShell(), SWT.MULTI);
				fileDialog.setFilterPath(fileText.getText());
				fileDialog.setFilterExtensions(new String[] {"*.h5","*.iam","*.dream"});
				String firstFile = fileDialog.open();
				if (firstFile != null) {
					selectedFiles = fileDialog.getFileNames();
					File file = new File(firstFile);
					directory = file.getParent();
					Constants.setFileType(file);
					String print = String.join("\n", selectedFiles);
					fileText.setText(print); //Updates the text
					changed = true;
				}
			}
		});
		
		fileText = new Text(fileComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData filesGrid = new GridData(SWT.FILL, SWT.TOP, true, true, 1, 5);
		filesGrid.minimumWidth = 700;
		filesGrid.minimumHeight = 200;
		fileText.setLayoutData(filesGrid);
		if(selectedFiles!=null) {
			String print = String.join("\n", selectedFiles);
			fileText.setText(print);
		}
		fileText.setForeground(Constants.black);
		fileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String files = ((Text)e.getSource()).getText();
				String[] fileList = files.split("\r\n");
				boolean fileError = false;
				for (String filePath : fileList) {
					if(filePath=="") continue; //skip blank lines
					File testFile = new File(directory+File.separator+filePath);
					if (!testFile.exists()) //not sure how they would do this
						fileError = true;
				}
				if(fileError) {
					((Text) e.getSource()).setForeground(Constants.red);
				} else {
					((Text) e.getSource()).setForeground(Constants.black);
					selectedFiles = fileList;
				}
				errorFound(fileError, "  Must select h5, iam, or dream files.");
				changed = true;
			}
		});
		
		// Determines which view to show: directory or files
		toggleView();

		// Overrides the directory for auto testing, overrides temp save location //TODO: this should eventually be from a save file
		if(Constants.autoTest && Constants.fileType == FileType.H5)
			directory = AutoTesting.hdf5Directory;
		else if(Constants.autoTest && Constants.fileType == FileType.IAM)
			directory = AutoTesting.iamDirectory;
		
		Label noteLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		noteLabel.setText(
				"More info: The \"Launch Converter\" button will allow file format conversions from ASCII to HDF5 for common subsurface simulation output formats (currently: NUFT, STOMP). If the file converter is incompatible with the desired output file format, specific formatting requirements are given in the user manual. ");
		GridData noteGridData = new GridData(GridData.FILL_HORIZONTAL);
		noteGridData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
		noteGridData.verticalSpan = 4;
		noteGridData.widthHint = 500;
		noteLabel.setLayoutData(noteGridData);

		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if((Constants.skipToEnd || Constants.autoTest) && Constants.foundDirectory)
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
	}

	private boolean validDirCheck(final String folderDir) {
		boolean error = true;
		File folder = new File(folderDir);
		// Ignore any files that aren't an acceptable file type
		FilenameFilter fileTypeFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if(name.endsWith(".h5") || name.endsWith(".iam") || name.endsWith(".dream"))
					return true;
				return false;
			}
		};
		File[] fList = folder.listFiles(fileTypeFilter);
		ArrayList<String> addFileNames = new ArrayList<String>();
		// First file determines the fileType
		Constants.setFileType(fList[0]);
		// Make sure we only grab files of the same type (in case of h5 and IAM in the same folder)
		for (File file : fList) {
			if(Constants.isCorrectFileType(file)) {
				addFileNames.add(file.getName());
				error = false;
			}
		}
		// Convert list to array
		selectedFiles = addFileNames.toArray(new String[addFileNames.size()]);
		return error;
	}
	
	// When the user does something that saves files, store directory default in temp
	private void saveParentFolder() {
		try {
			String fileName = "directorySave.txt";
			File directorySaveFile = new File(Constants.tempDir, fileName);
			FileUtils.writeStringToFile(directorySaveFile, directory);
			System.out.println("Saving directory for next time: "+directory);
		} catch (IOException e) {
			System.out.println("Warning: Error saving parent results folder");
		}
	}
	
	// Retrieves the parent results folder from temp
	private String getInputFolder() {
		String fileName = "directorySave.txt";
		File directorySaveFile = new File(Constants.tempDir, fileName);
		try(FileInputStream inputStream = new FileInputStream(directorySaveFile)) {     
			String saveFileAsString = IOUtils.toString(inputStream);
			Constants.foundDirectory = true;
			return saveFileAsString;
		} catch (IOException e1) {
			System.out.println("Error reading the saved input folder.");
			return Constants.homeDirectory;
		}
	}
	
	private void toggleView() {
		directoryComposite.setVisible(selectDirectory);
		directoryGridData.exclude = !selectDirectory;
		fileComposite.setVisible(!selectDirectory);
		fileGridData.exclude = selectDirectory;
		container.pack();
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