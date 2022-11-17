package hdf5Tool;

import gridviz.GridError;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5Datatype;
import ncsa.hdf.object.h5.H5File;
import utilities.Constants;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Dataset;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FileBrowser.java
 *
 * Created on Aug 12, 2013, 10:33:55 AM
 */
/**
 *
 * @author port091
 * @author whit162
 */
public class FileConverter extends javax.swing.JFrame {

	private static final long serialVersionUID = 8513730341976192873L;

	private String fileType;
	private String folderStructure;
	private int usedCores;
		
	private JTextField jTextField_inputDir;
	private JTextField jTextField_outputDir;

	private File file_inputDir;
	private File file_outputDir;
	private File saveCurrentDirectory;

	private CheckList checkList_timesteps;
	private CheckList checkList_scenarios;
	private CheckList checkList_parameters;	
	
	private ProgressMonitor monitor;
	private int processedTasks;
	
	private ParseRawFiles parser;

	private JLabel statusLabel;
	
	private JPanel mainPanel = new JPanel();
		
	public FileConverter() {
		// Hack that allows my directory to start where I want it TODO: Remove before release
		if(Constants.homeDirectory.contains("whit162"))
			saveCurrentDirectory = new File("C:\\Users\\whit162\\Documents\\Projects\\DreamProject\\FileConversionTests");
		// End hack
		parser = new ParseRawFiles();
		initComponents();
	}
	
	private void initComponents() {

		setTitle("DREAM HDF5 Converter");
		setBackground(new java.awt.Color(240, 240, 240));
		getContentPane().setBackground(new java.awt.Color(240, 240, 240));
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		
		checkList_parameters = new CheckList();
		checkList_timesteps = new CheckList();
		checkList_scenarios = new CheckList();
		checkList_parameters.setEnabled(false);
		checkList_timesteps.setEnabled(false);
		checkList_scenarios.setEnabled(false);
		
		JPanel jPanel_parameters = new JPanel();
		JLabel jLabel_scenarios = new JLabel();
		JPanel jPanel_timesteps = new JPanel();
		JLabel jLabel_timesteps = new JLabel();
		JLabel jLabel_parameters = new JLabel();
		JPanel jPanel_scenarios = new JPanel();
		JButton jButton_done = new JButton("Convert...");
		JLabel jLabel_outputDir = new JLabel("Output directory:");
		JLabel jLabel_inputDir = new JLabel("Input directory:");
		jTextField_inputDir = new JTextField();
		jTextField_outputDir = new JTextField();
		file_outputDir = new File("C:\\");
		JButton jButton_outputDir = new JButton("Select");
		JButton jButton_inputDir = new JButton("Select");
		
		// Blue border
		jPanel_parameters.setBackground(new java.awt.Color(33,57,156));
		jPanel_timesteps.setBackground(new java.awt.Color(33,57,156));
		jPanel_scenarios.setBackground(new java.awt.Color(33,57,156));
		
		// Scenarios:
		jLabel_scenarios.setText("Scenarios");
		javax.swing.GroupLayout jPanel_scenariosLayout = new javax.swing.GroupLayout(jPanel_scenarios);
		jPanel_scenarios.setLayout(jPanel_scenariosLayout);
		jPanel_scenariosLayout.setHorizontalGroup(
				jPanel_scenariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_scenarios, 0, 294, Short.MAX_VALUE)
				);
		jPanel_scenariosLayout.setVerticalGroup(
				jPanel_scenariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_scenarios, 0, 184, Short.MAX_VALUE)
				);
		
		// Timesteps:
		jLabel_timesteps.setText("Time steps");
		javax.swing.GroupLayout jPanel_timestepsLayout = new javax.swing.GroupLayout(jPanel_timesteps);
		jPanel_timesteps.setLayout(jPanel_timestepsLayout);
		jPanel_timestepsLayout.setHorizontalGroup(
				jPanel_timestepsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_timesteps, 0, 294, Short.MAX_VALUE)
				);
		jPanel_timestepsLayout.setVerticalGroup(
				jPanel_timestepsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_timesteps, 0, 184, Short.MAX_VALUE)
				);
		
		// Parameters:
		jLabel_parameters.setText("Parameters");
		javax.swing.GroupLayout jPanel_parametersLayout = new javax.swing.GroupLayout(jPanel_parameters);
		jPanel_parameters.setLayout(jPanel_parametersLayout);
		jPanel_parametersLayout.setHorizontalGroup(
				jPanel_parametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_parameters, 0, 245, Short.MAX_VALUE)
				);
		jPanel_parametersLayout.setVerticalGroup(
				jPanel_parametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_parameters, 0, 394, Short.MAX_VALUE)
				);
		
		// Buttons
		jButton_done.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					jButton_doneActionPerformed(evt);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		jButton_inputDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					jButton_inputDirActionPerformed(evt);
				} catch (GridError e) {
					e.printStackTrace();
				}
			}
		});
		jButton_outputDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButton_outputDirActionPerformed(evt);
			}
		});
		
		// Top panel
		jTextField_inputDir.setEnabled(false);
		jTextField_outputDir.setEnabled(false);
		
		statusLabel = new JLabel("");
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addGap(8, 8, 8)
										.addComponent(jLabel_inputDir)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jTextField_inputDir, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jButton_inputDir)
										.addGap(69, 69, 69))
										.addGroup(layout.createSequentialGroup()
												.addComponent(jLabel_outputDir)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(jTextField_outputDir, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(jButton_outputDir)
												.addGap(69, 69, 69))
												.addGroup(layout.createSequentialGroup()
																.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																		.addComponent(jPanel_scenarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																		.addComponent(jLabel_scenarios)
																		.addComponent(jPanel_timesteps, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																		.addComponent(jLabel_timesteps))
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(jLabel_parameters)
																				.addComponent(jPanel_parameters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))																																				
																				.addComponent(statusLabel, javax.swing.GroupLayout.Alignment.LEADING)			
																				.addComponent(jButton_done, javax.swing.GroupLayout.Alignment.TRAILING))
																				.addContainerGap())
				);
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jTextField_inputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jLabel_inputDir)
								.addComponent(jButton_inputDir))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jTextField_outputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(jLabel_outputDir)
										.addComponent(jButton_outputDir))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(jLabel_scenarios)
														.addComponent(jLabel_parameters))
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																.addGroup(layout.createSequentialGroup()
																		.addComponent(jPanel_scenarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(jLabel_timesteps)
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(jPanel_timesteps, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
																		.addComponent(jPanel_parameters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(statusLabel)
																		.addComponent(jButton_done)
																		.addContainerGap())
				);

		pack();
	}

	private void jButton_doneActionPerformed(final ActionEvent evt) throws Exception {
		// Save selected scenarios, timesteps, and parameters
		parser.setSelected(checkList_scenarios.getListData(), checkList_timesteps.getListData(),
				checkList_parameters.getListData());
		if (parser.getSelectedParameters().size() < 1 || parser.getSelectedScenarios().size() < 1
				|| parser.getSelectedTimes().size() < 1) {
			statusLabel.setText("Select at least one parameter, scenario, and time.");
			statusLabel.setForeground(Color.red);
			return;
		} else {
			// Initialize global variables
			processedTasks = 0;

			// Create the HDF5 output directory if it doesn't exist
			if (!file_outputDir.exists())
				file_outputDir.mkdir();

			// Use them all.. well okay, all but one :)
			final int cores = Runtime.getRuntime().availableProcessors() - 1;
			// Might not use all available threads if not enough scenarios
			if(parser.getSelectedScenarios().size()<cores)
				usedCores = parser.getSelectedScenarios().size();
			// ExecutorService service = Executors.newFixedThreadPool(cores);
			System.out.println("Using " + usedCores + " cores for the conversion...");
			if (parser.getSelectedScenarios().size() == parser.getScenarios().length)
				System.out.println("Selected Scenarios: All");
			else
				System.out.println("Selected Scenarios: " + parser.getSelectedScenarios().toString());
			if (parser.getSelectedTimes().size() == parser.getTimes().length)
				System.out.println("Selected Times: All");
			else
				System.out.println("Selected Times: " + parser.getSelectedTimes().toString());
			if (parser.getSelectedParameters().size() == parser.getParameters().length)
				System.out.println("Selected Parameter: All");
			else
				System.out.println("Selected Parameters: " + parser.getSelectedParameters().toString());

			final List<Thread> runningThreads = new ArrayList<Thread>();
			final List<Thread> threadsToRun = new ArrayList<Thread>();

			for (String scenarioName : parser.getSelectedScenarios()) {
				FileConverterThread runnable = new FileConverterThread(scenarioName);
				Thread thread = new Thread(runnable);
				threadsToRun.add(thread);
			}

			MonitorRunnable monitorRunnable = new MonitorRunnable(null, cores, parser.getSelectedScenarios().size());
			new Thread(monitorRunnable).start();

			Thread runningAllStuffThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!threadsToRun.isEmpty()) {
						if (runningThreads.size() < cores - 1) { // probably not
							Thread thread = threadsToRun.remove(0);
							thread.start();
							runningThreads.add(thread);
						} else {
							List<Thread> deadThreads = new ArrayList<Thread>();
							for (Thread thread : runningThreads) {
								if (!thread.isAlive()) {
									deadThreads.add(thread);
								}
							}
							for (Thread dead : deadThreads) {
								runningThreads.remove(dead); // remove the dead...!
							}
						}
						try {
							Thread.sleep(100); // mmmm maybe?
							// Listening for a cancel
							if (monitor.isCanceled()) {
								// Kill everything
								for (Thread killable : runningThreads)
									killable.interrupt();
								runningThreads.clear();
								threadsToRun.clear(); // maybe java can collect these???
								return;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					while (!runningThreads.isEmpty()) {
						List<Thread> deadThreads = new ArrayList<Thread>();
						for (Thread thread : runningThreads) {
							if (!thread.isAlive())
								deadThreads.add(thread);
						}
						for (Thread dead : deadThreads) {
							runningThreads.remove(dead); // remove the dead...!
						}
						try {
							Thread.sleep(1000); // mmmm maybe?
							// Listening for a cancel
							if (monitor.isCanceled()) {
								// kill everything
								for (Thread killable : runningThreads)
									killable.interrupt();
								runningThreads.clear();
								threadsToRun.clear(); // maybe java can collect these???
								return;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			runningAllStuffThread.start();
		}
	}

	private class MonitorRunnable implements Runnable {

		private int totalTasks;

		MonitorRunnable(ExecutorService service, int cores, int totalTasks) {
			this.totalTasks = totalTasks;
			monitor = new ProgressMonitor(FileConverter.this, "Converting files on "+usedCores+" core"+(usedCores==1 ? "" : "s"), "0/"+totalTasks, 0, totalTasks);
			processedTasks = 0;
		}

		@Override
		public void run() {
			while(!monitor.isCanceled()) {
				// or maybe some done check too?
				try {
					monitor.setNote(processedTasks + "/" + totalTasks);
					monitor.setProgress(processedTasks);
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace(); // Ignore and continue
					JOptionPane.showMessageDialog(FileConverter.this, Arrays.toString(e.getStackTrace()), e.getMessage(), JOptionPane.ERROR_MESSAGE);
				}
			}
			monitor.setProgress(processedTasks);
			JOptionPane.showMessageDialog(FileConverter.this, (!monitor.isCanceled() ? "Success, h5 files are located here: " + file_outputDir.getAbsolutePath() : "Canceled File Conversion"));
		}
	}
	
	
	// Each thread is assigned a scenario to both to read in the data and write the file
	// At this point, we've already filtered down to only selected scenarios
	private class FileConverterThread implements Runnable {

		private String scenarioName;
		public FileConverterThread(String scenarioName) {
			this.scenarioName = scenarioName;
		}
		
		@Override
		public void run() {
			long startTime = System.currentTimeMillis();
			
			// Extract the data and statistics from the selected directory
			if(fileType.equals("STOMP")) { //Scenario per folder
				File scenarioFolder = new File(file_inputDir, scenarioName);
				parser.extractStompData(scenarioFolder);
			} else if(fileType.equals("NUFT")) { //Scenario per file
				parser.extractNuftData(file_inputDir, scenarioName);
			} else if(fileType.equals("Tecplot")) { //Scenario per file
				File scenarioFile = new File(file_inputDir, scenarioName + ".dat");
				parser.extractTecplotData(scenarioFile);
			} else if(fileType.equals("TOUGH2")) { //Scenario per folder
				File scenarioFolder = new File(file_inputDir, scenarioName);
				parser.extractToughData(scenarioFolder);
			}
			H5File hdf5File = null;
			try {
				FileFormat hdf5Format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
				File hdf5FileLocation = new File(file_outputDir, scenarioName + ".h5");
				hdf5File = (H5File)hdf5Format.createFile(hdf5FileLocation.getAbsolutePath(), FileFormat.FILE_CREATE_DELETE);
				hdf5File.open();
				
				writeH5Files(parser, hdf5File, scenarioName);
				
				statusLabel.setText("The " + fileType + " files were successfully converted to H5 files.");
				statusLabel.setForeground(new java.awt.Color(5, 70, 5));
				long endTime = (System.currentTimeMillis()-startTime)/1000;
				System.out.println("Finished reading "+scenarioName+", writing to "+hdf5File+"... took "+Constants.formatSeconds(endTime));
			} catch (Exception e) {
				e.printStackTrace();
				statusLabel.setText("Error converting the files.");
				statusLabel.setForeground(Color.RED);
				JOptionPane.showMessageDialog(FileConverter.this, Arrays.toString(e.getStackTrace()), e.getMessage(), JOptionPane.ERROR_MESSAGE);
				hdf5File.delete(); // Remove the file
			} finally {
				if(hdf5File != null)  {
					try {
						hdf5File.close();
					} catch (HDF5Exception e) {
						System.out.println("Error closing the HDF5 File.");
						e.printStackTrace();
					}
				}
				processedTasks++; // For the monitor 
			}
		}
	}
	
	
	private void jButton_inputDirActionPerformed(ActionEvent evt) throws GridError {
		// Open a folder
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(saveCurrentDirectory);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnValue = chooser.showOpenDialog(null);
		
		if(returnValue == JFileChooser.APPROVE_OPTION) {
			parser = new ParseRawFiles(); //reset from previous conversions
			saveCurrentDirectory = chooser.getSelectedFile();
			file_inputDir = chooser.getSelectedFile();
			file_outputDir = new File(file_inputDir.getAbsolutePath() + "_hdf5");
			
			if(file_inputDir != null && file_inputDir.isDirectory()) {
				
				statusLabel.setText("Loading directory: " + file_inputDir);
				statusLabel.setForeground(Color.BLACK);
				FileConverter.this.validate();
				FileConverter.this.repaint();
				
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));	
				getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				Thread readThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						long startTime = System.currentTimeMillis();
						
						// Figure out the model type and folder structure
						fileType = "";
						folderStructure = "scenario per file"; //Default until a folder is seen
						boolean error = false;
						for (File fileLoop : file_inputDir.listFiles()) {
							if(fileLoop.isDirectory()) {
								folderStructure = "scenario per folder";
								for(File file : fileLoop.listFiles()) {
									// STOMP and TOUGH2 require a folder per scenario
									if(file.getName().startsWith("plot."))
										fileType = "STOMP";
									else if(file.getName().endsWith(".OUT"))
										fileType = "TOUGH2";
								}
							} else {
								if(fileLoop.getName().startsWith("plot.")) {
									error = true; //we expect scenario per folder
									fileType = "STOMP";
								} else if(fileLoop.getName().endsWith(".ntab")) {
									fileType = "NUFT";
								} else if(fileLoop.getName().endsWith(".dat")) {
									fileType = "Tecplot";
								} else if(fileLoop.getName().endsWith(".OUT")) {
									error = true; //we expect scenario per folder
									fileType = "TOUGH2";
								}
							}
						}
						
						// When file structure is incorrect
						if(error) { //when we don't have scenarios per folder
							statusLabel.setText("Error: Expecting "+fileType+" file structure as folder per scenario.");
							statusLabel.setForeground(Color.RED);
							return;
						}
						if(fileType.equals("")) { //no files found
							statusLabel.setText("Error: No model outputs were found.");
							statusLabel.setForeground(Color.RED);
							return;
						}
						
						// Extract the scenarios, timeSteps, and parameters from the selected directory
						if(fileType.equals("STOMP"))
							parser.extractStompStructure(file_inputDir);
						else if(fileType.equals("NUFT"))
							parser.extractNuftStructure(file_inputDir);
						else if(fileType.equals("Tecplot"))
							parser.extractTecplotStructure(file_inputDir);
						else if(fileType.equals("TOUGH2"))
							parser.extractToughStructure(file_inputDir);
						
						// If no scenarios were detected, throw an error and bypass the rest of the method
						if(parser.getScenarios().length==0 || parser.getTimes().length==0 || parser.getParameters().length==0) {
							statusLabel.setText("Unable to parse the files. No scenarios, times, or parameters detected");
							statusLabel.setForeground(Color.RED);
							return;
						}
						Object[] scenarios = parser.getScenarios();
						Object[] timeSteps = parser.getTimes();
						Object[] parameters = parser.getParameters();
						
						System.out.println("Scenarios: " + Arrays.toString(scenarios));
						System.out.println("Times: " + Arrays.toString(timeSteps));
						System.out.println("Data: " + Arrays.toString(parameters));
						
						checkList_timesteps.setListData(timeSteps, true);
						checkList_scenarios.setListData(scenarios, true);
						checkList_parameters.setListData(parameters, false);
						
						checkList_parameters.setEnabled(true);
						checkList_timesteps.setEnabled(true);
						checkList_scenarios.setEnabled(true);
						
						statusLabel.setText("");
						statusLabel.setForeground(Color.BLACK);
						jTextField_outputDir.setText(file_outputDir.getAbsolutePath());
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								jTextField_inputDir.setText(file_inputDir.getAbsolutePath());
								
								setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
								getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
								
								FileConverter.this.repaint();
							}
						});
						long endTime = (System.currentTimeMillis()-startTime)/1000;
						System.out.println("Loaded "+fileType+" files with "+folderStructure+"... took "+Constants.formatSeconds(endTime));
						statusLabel.setText("Loaded "+fileType+" files with "+folderStructure+".");
						statusLabel.setForeground(new java.awt.Color(5, 70, 5));
					}
				});
				readThread.start();
			}	
		} else {
			return;
		}
	}
	
	
	private void jButton_outputDirActionPerformed(ActionEvent evt) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(file_outputDir.getAbsolutePath()));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnValue = chooser.showOpenDialog(null);
		if(returnValue == JFileChooser.APPROVE_OPTION) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			file_outputDir = chooser.getSelectedFile();
		} else {
			return;
		}		
		jTextField_outputDir.setText(file_outputDir.getAbsolutePath());
	}
	
	
	private void writeH5Files(ParseRawFiles gp, H5File hdf5File, String scenarioName) throws Exception {
		// Get the root
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		Datatype dtype = hdf5File.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, -1);
		
		// Initialize the attribute dimension
		long[] attrDims = { 1 }; //1D of size one
		Datatype attrType = new H5Datatype(Datatype.CLASS_STRING, 10, -1, -1);
		
		// Get the dimensions of the grid
		long[] dims3D = {gp.getX().length, gp.getY().length, gp.getZ().length};
		
		// Make sure that we have XYZ and time units, and porosity
		// Made an assumption that if the file doesn't have x-units none of the other coordinates will have units
		// Might need to query the user for a constant porosity across the domain
		if(gp.getUnit("x").equals("") || gp.getUnit("times").equals("") ||
				gp.getPorosity() == null || gp.getUnit("positive").equals(""))
			addXYZandTimeUnits(dims3D);
		
		/////////////////////////////////////////////////////////
		// Data Group: porosity, steps, times, vertex-xyz, xyz //
		/////////////////////////////////////////////////////////
		Group dataGroup = hdf5File.createGroup("data", root);
		
		// Writing x variable with units
		Dataset d = hdf5File.createScalarDS("x", dataGroup, dtype, new long[]{gp.getX().length}, null, null, 0, gp.getX());
		String[] classValue = { gp.getUnit("x") }; //X units will always exist, add as attribute
		Attribute attr = new Attribute("unit", attrType, attrDims, classValue);
		d.writeMetadata(attr);
		
		// Writing y variable with units
		d = hdf5File.createScalarDS("y", dataGroup, dtype, new long[]{gp.getY().length}, null, null, 0, gp.getY());
		classValue = new String[]{ gp.getUnit("y") }; //Y units will always exist, add as attribute
		attr = new Attribute("unit", attrType, attrDims, classValue);
		d.writeMetadata(attr);
		
		// Writing z variable with units
		d = hdf5File.createScalarDS("z", dataGroup, dtype, new long[]{gp.getZ().length}, null, null, 0, gp.getZ());
		classValue = new String[]{ gp.getUnit("z") }; //Z units will always exist, add as attribute
		attr = new Attribute("unit", attrType, attrDims, classValue);
		Attribute orientationZ = new Attribute("positive", attrType,
				attrDims, new String[] {gp.getPositive()});
		d.writeMetadata(attr);
		d.writeMetadata(orientationZ);
		
		// Writing x vertex variable with units
		d = hdf5File.createScalarDS("vertex-x", dataGroup, dtype, new long[]{gp.getVertexX().length}, null, null, 0, gp.getVertexX());
		classValue = new String[]{ gp.getUnit("x") }; //X units will always exist, add as attribute
		attr = new Attribute("unit", attrType, attrDims, classValue);
		d.writeMetadata(attr);
		
		// Writing y vertex variable with units
		d = hdf5File.createScalarDS("vertex-y", dataGroup, dtype, new long[]{gp.getVertexY().length}, null, null, 0, gp.getVertexY());
		classValue = new String[]{ gp.getUnit("y") }; //Y units will always exist, add as attribute
		attr = new Attribute("unit", attrType, attrDims, classValue);
		d.writeMetadata(attr);
		
		// Writing z vertex variable with units
		d = hdf5File.createScalarDS("vertex-z", dataGroup, dtype, new long[]{gp.getVertexZ().length}, null, null, 0, gp.getVertexZ());
		classValue = new String[]{ gp.getUnit("z") }; //Z units will always exist, add as attribute
		attr = new Attribute("unit", attrType, attrDims, classValue);
		Attribute orientationVertexZ = new Attribute("positive", attrType,
				attrDims, new String[] {gp.getPositive()});
		d.writeMetadata(attr);
		d.writeMetadata(orientationVertexZ);
		
		// Writing time variable with units
		d = hdf5File.createScalarDS("times", dataGroup, dtype, new long[]{gp.getSelectedTimesArray().length}, null, null, 0, gp.getSelectedTimesArray());
		classValue = new String[]{ gp.getUnit("times") };
		attr = new Attribute("unit", attrType, attrDims, classValue);
		d.writeMetadata(attr); //Add the units as an attribute
		
		// Writing step variable, no units
		float[] steps = new float[gp.getSelectedTimesArray().length];
		for(int i=0; i<gp.getSelectedTimesArray().length; i++)
			steps[i] = i;
		hdf5File.createScalarDS("steps", dataGroup, dtype, new long[]{steps.length}, null, null, 0, steps);
		
		// Writing porosity variable, no units
		hdf5File.createScalarDS("porosity", dataGroup, dtype, dims3D, null, null, 0, gp.getPorosity()); //Porosity is a fraction (no units)
		

		///////////////////////////////////////////////////////
		// Plot groups per step with data for all parameters //
		///////////////////////////////////////////////////////
		for(float time: gp.getSelectedTimesArray()) {
			int index = gp.getSelectedTimeIndex(time);
			Group timeStepGroup = hdf5File.createGroup("time" + index, root);
			for(String parameter: gp.getSelectedParameters()) {
				try {
					d = hdf5File.createScalarDS(parameter, timeStepGroup, dtype, dims3D, null, null, 0,
							gp.getData(scenarioName, parameter)[index]);
					if (!gp.getUnit(parameter).equals("")) { // Add units as an attribute if they exist
						classValue[0] = gp.getUnit(parameter);
						attr = new Attribute("unit", attrType, attrDims, classValue);
						d.writeMetadata(attr);
					}
				} catch (Exception theException) {
					System.out.println("No parameter for the scenario.");
				}
			}
		}
		
		////////////////////////////////////////////////////////
		// Statistics Group: min, avg, max for all parameters //
		////////////////////////////////////////////////////////
		Group statisticsGroup = hdf5File.createGroup("statistics", root);
		for(String parameter: gp.getSelectedParameters()) {
			hdf5File.createScalarDS(parameter, statisticsGroup, dtype, new long[]{3}, null, null, 0, gp.getStatistics(scenarioName, parameter));
		}
		hdf5File.close();
	}
	/**
	 * Adds the XYZ distance units, Time Units, and/or Porosity values to our unit HashMap.
	 * @param dims3D - The array for our porosity.
	 */
	private synchronized void addXYZandTimeUnits(final long[] dims3D) {
		//Remove all to make sure their aren't any duplicates.
		mainPanel.removeAll();
		if (parser.getUnit("x").equals("") || parser.getUnit("times").equals("") || parser.getPorosity() == null
				|| parser.getPositive().equals("")) {
			JComboBox<String> ZOrientation = new JComboBox<String>(new String[] {"up", "down"});	
			JComboBox<String> distanceList = new JComboBox<String>(new String[] {"m", "ft"});
			JComboBox<String> timeList = new JComboBox<String>(new String[] {"years", "months", "days"});
			
			JTextField porosityText = new JTextField();
			//Our option pane that calls our JPanel creation method.
			int option = JOptionPane.showConfirmDialog(null, theDialogBoxes(distanceList, timeList,
					porosityText, ZOrientation),
						"Set Units, Set Porosity, or Set Elevation/Depth", JOptionPane.OK_CANCEL_OPTION);
			//When user clicks ok.
			if (option == JOptionPane.OK_OPTION) {
				//Put units into our unit HashMap.
				if (parser.getUnit("x").equals("")) {
					String distance =  distanceList.getSelectedItem().toString();
					parser.setUnit("x", distance);
					parser.setUnit("y", distance);
					parser.setUnit("z", distance);
				}
				String ZOrient = ZOrientation.getSelectedItem().toString();
				parser.setPositive(ZOrient);
				if (parser.getUnit("times").equals("")) {
					String time = timeList.getSelectedItem().toString();
					parser.setUnit("times", time);
				}
				if (parser.getPorosity() == null) {
					float[] porosity = new float[(int)dims3D[0]*(int)dims3D[1]*(int)dims3D[2]];
					float input = 999;
					try {
						input = Float.parseFloat(porosityText.getText());
						//If the input doesn't fall into range, force the user to input the porosity again.
						while (input > 1 || input < 0) {
							input = Float.parseFloat(JOptionPane.showInputDialog(FileConverter.this,
									"Please enter a Porosity in the domain (Between 0 and 1)", 0.1));
						}
						Arrays.fill(porosity, input);
					} catch (final NumberFormatException theException) {
						theException.printStackTrace();
					}
					parser.setPorosity(porosity);
				}
			}
		}
	}
	
	
	/**
	 * This method creates the JPanel for our option pane.
	 * @param distanceList - The list of distance units for our drop down menu.
	 * @param timeList - The list of time units for our drop down menu.
	 * @param porosityText - The porosity JTextField
	 * @return - The JPanel for our option pane.
	 */
	private JPanel theDialogBoxes(final JComboBox<String> distanceList
			, final JComboBox<String> timeList, final JTextField porosityText,
			final JComboBox<String> theZOrientation) {
		mainPanel.setLayout(new GridLayout(0,1));
		if (parser.getUnit("x").equals("") || parser.getUnit("times").equals("") || parser.getPorosity() == null
				|| parser.getPositive().equals("")) {

			
			Label distanceLabel = new Label();
			distanceLabel.setText("XYZ Units:");
			
			Label timeLabel = new Label();
			timeLabel.setText("Time Units:");
			
			Label ZOrientationLabel = new Label();
			ZOrientationLabel.setText("Z-Axis Positive Direction: ");
			
			Label porosityLabel = new Label();
			porosityLabel.setText("Specify Porosity Value");
			
			porosityText.setText("0.1");
			
			mainPanel.add(distanceLabel);
			mainPanel.add(distanceList);
			
			mainPanel.add(timeLabel);
			mainPanel.add(timeList);
			
			mainPanel.add(ZOrientationLabel);
			mainPanel.add(theZOrientation);
			
			mainPanel.add(porosityLabel);
			mainPanel.add(porosityText);
			
			//If we already have these units, remove them from the JPanel they don't need to be set.
			if (!parser.getUnit("x").equals("")) {
				mainPanel.remove(distanceList);
				mainPanel.remove(distanceLabel);
			}
			if (!parser.getUnit("times").equals("")) {
				mainPanel.remove(timeList);
				mainPanel.remove(timeLabel);
			} 
			if (parser.getPorosity() != null) {
				mainPanel.remove(porosityText);
				mainPanel.remove(porosityLabel);
			}
		}
		return mainPanel;
	}

	
	public static void main(String args[]) {
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new FileConverter().setVisible(true);
			}
		});
	}
}
