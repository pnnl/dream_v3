package hdf5Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import utilities.Constants;
import utilities.Point3i;

/**
 * @brief Reads a variety of input files to convert into DREAM-compatible Hdf5
 *        files
 * @author Jonathan Whiting
 * @date February 18, 2019
 */
public class ParseRawFiles {
	private ArrayList<String> scenarioNames;
	private ArrayList<Float> times;
	private ArrayList<String> parameters;
	private int commonStartCount;
	private int commonEndCount;

	private ArrayList<String> selectedScenarios;
	private ArrayList<Float> selectedTimes;
	private ArrayList<String> selectedParameters;

	private ArrayList<Float> x;
	private ArrayList<Float> y;
	private ArrayList<Float> z;
	private int nodes;
	private int nodeLimit;

	private ArrayList<Float> vertexX;

	private ArrayList<Float> vertexY;

	private ArrayList<Float> vertexZ;

	private float[] porosity;

	private Map<String, Map<String, float[][]>> dataMap; // scenario <parameter, float[time][nodes]>
	
	private Map<String, Map<String, int[][]>> counterMap; // scenario <parameter, float[time][nodes]> For unstructured grids

	private Map<String, Map<String, float[]>> statistics; // scenario, parameter, float[min, avg, max]

	private Map<String, String> units; // parameter, units
	
	private String positive;
	
	private boolean structuredGrid;
	
	// Only used by STOMP
	private boolean nodal; // Determine whether parameters are given for nodes or vertices
	private ArrayList<Integer> nodeMap; // Maps values to a node number
	// Only used by NUFT, TOUGH2, and Tecplot
	private ArrayList<String> indexMap; // Maps parameters to columns or blocks
	// Only used by Tecplot
	private int elements;
	
	// Initialize variables
	public ParseRawFiles() {
		scenarioNames = new ArrayList<String>();
		times = new ArrayList<Float>();
		parameters = new ArrayList<String>();
		commonStartCount = Integer.MAX_VALUE;
		commonEndCount = Integer.MAX_VALUE;

		selectedScenarios = new ArrayList<String>();
		selectedTimes = new ArrayList<Float>();
		selectedParameters = new ArrayList<String>();

		x = new ArrayList<Float>();
		y = new ArrayList<Float>();
		z = new ArrayList<Float>();
		vertexX = new ArrayList<Float>();
		vertexY = new ArrayList<Float>();
		vertexZ = new ArrayList<Float>();
		positive = "";
		dataMap = new HashMap<String, Map<String, float[][]>>();
		structuredGrid = true;
		counterMap = new HashMap<String, Map<String, int[][]>>();
		statistics = new HashMap<String, Map<String, float[]>>();
		units = new HashMap<String, String>();
		indexMap = new ArrayList<String>();
		
		nodeLimit = 1000000; //TODO: User should set this, but 1 million can be the default
	}
	
	//////////////////////////////////////////
	//////// STOMP Conversion Methods ////////
	//////////////////////////////////////////
	
	/**
	 * Extracting scenarios, times, parameters, and xyz from the input directory.
	 * Only works for STOMP files, requiring a folder per scenario.
	 * @param parentDirectory
	 */
	public void extractStompStructure(File parentDirectory) {
		// Loop through the list of directories in the parent folder
		for (File directory : parentDirectory.listFiles()) {
			if (!directory.isDirectory())
				continue; // We only want folders - skip files
			// Add all the scenarios from folder names
			String scenarioName = directory.getName();
			if (!scenarioNames.contains(scenarioName))
				scenarioNames.add(scenarioName);
			// A quick check that we actually have STOMP files in the directory
			if (directory.listFiles().length == 0) {
				System.out.println("No STOMP files were found in the selected directory.");
				return;
			}
			// Read the first file to get the parameters
			File firstFile = directory.listFiles()[0];
			nodeMap = new ArrayList<Integer>();
			String line;
			String parameter = "";
			boolean blankLine = false;
			boolean header = true;
			try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
				while ((line = br.readLine()) != null) { // We actually have to read the whole file... parameters are
															// scattered throughout
					// We are assuming this is always the last line of the header, good until proven otherwise
					if (line.contains("Number of Vertices"))
						header = false;
					// These are the criteria to isolate the parameter text above blocks
					else if (!header && blankLine) {
						// New clean parameter
						parameter = line.split(",")[0].trim().replaceAll("\\(", "_").replaceAll("\\)", ""); 
						// If it has Nodal/Node in the name, it is giving values for edges
						if (parameter.contains("Nodal") || line.contains("Node"))
							nodal = true;
						// We want to normalize the x, y, z
						if (parameter.startsWith("X or R-Direction Node Positions")
								|| parameter.startsWith("Radial-Direction Node Positions")
								|| parameter.startsWith("X-Direction Surface Positions")
								|| parameter.startsWith("X-Direction Node Positions")
								|| parameter.startsWith("X-Direction Nodal Vertices")) {
							parameter = "x";
						} else if (parameter.startsWith("Y or Theta-Direction Node Positions")
								|| parameter.startsWith("Theta-Direction Node Positions")
								|| parameter.startsWith("Y-Direction Surface Positions")
								|| parameter.startsWith("Y-Direction Node Positions")
								|| parameter.startsWith("Y-Direction Nodal Vertices")) {
							parameter = "y";
						} else if (parameter.startsWith("Z-Direction Surface Positions")
								|| parameter.startsWith("Z-Direction Node Positions")
								|| parameter.startsWith("Z-Direction Nodal Vertices")) {
							parameter = "z";
							// Store a list of parameters, skipping some
						} else if (!parameters.contains(parameter) && !parameter.equals("Node Volume")
								&& !parameter.equals("Node Map") && !parameter.toLowerCase().contains("porosity"))
							parameters.add(parameter);
						// Save units for all parameters if they are available
						if (line.contains(",") && !line.contains("null")) { // This means they give units
							String unit = line.split(",")[1].trim();
							units.put(parameter, unit); // Save units
						}
					// These are the criteria to isolate the xyz values
					} else if (!header && !line.equals("") && (parameter.equals("x") || parameter.equals("y")
							|| parameter.equals("z") || parameter.equals("Node Map"))) {
						String[] tokens = line.trim().split("\\s+"); // The line is space delimited
						for (String token : tokens) { // Loop through the tokens
							Float value = null;
							try {
								value = Float.parseFloat(token); // Parse value into float
								// All values should be stored in xyz for now, handle nodal later
								if (parameter.equals("x") && !x.contains(value))
									x.add(value); // Store x values
								else if (parameter.equals("y") && !y.contains(value))
									y.add(value); // Store y values
								else if (parameter.equals("z") && !z.contains(value))
									z.add(value); // Store z values
								// nodeMap gives a 1-indexed node number
								else if (parameter.equals("Node Map"))
									nodeMap.add(value.intValue());
							} catch (Exception e) {
								System.out.println("Error parsing the " + parameter + " value: " + token);
							}
						}
					}
					blankLine = line.isEmpty(); //true for empty lines
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (nodal) { // Provided values are at the edge of each cell, calculate the center
				vertexX = x; vertexY = y; vertexZ = z;
				x = calculateCenters(vertexX);
				y = calculateCenters(vertexY);
				z = calculateCenters(vertexZ);
			} else { // Provided values are at the center of each cell, calculate the edge
				vertexX = calculateEdges(x);
				vertexY = calculateEdges(y);
				vertexZ = calculateEdges(z);
			}
			// All STOMP grids are structured, no need to handle unstructured
			nodes = x.size() * y.size() * z.size();
			// Read the header of every file to get the times
			for (File subFile : directory.listFiles()) {
				try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
					while ((line = br.readLine()) != null) { // We just need to read the header for each file
						if (line.contains("Time =") & line.contains(",yr")) {
							units.put("times", "Years");
							String year = line.substring(line.indexOf(",wk") + 3, line.indexOf(",yr")).trim();
							try {
								Float timeStep = Math.round(Float.parseFloat(year) * 1000f) / 1000f; // This rounds to 3
																										// decimal
																										// places
								if (!times.contains(timeStep))
									times.add(timeStep);
							} catch (Exception e) {
								System.out.println("Years Error: " + year);
							}
							break; // No need to read the rest of the file
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		cleanParameters();
	}

	/**
	 * Extracting data, statistics, and porosity from a scenario directory.
	 * Only works for STOMP files, requiring a folder per scenario.
	 * @param directory
	 */
	public void extractStompData(File directory) {
		String scenarioName = directory.getName();
		dataMap.put(scenarioName, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
		statistics.put(scenarioName, new HashMap<String, float[]>()); // Initialize statistics for this scenario
		for(String parameter : selectedParameters) {
			dataMap.get(scenarioName).put(parameter, new float[selectedTimes.size()][nodes]);
			statistics.get(scenarioName).put(parameter, new float[3]);
		}
		System.out.println("Reading variables: " + selectedParameters.toString());
		
		// Loop through the list of files in each directory
		for (File dataFile : directory.listFiles()) {
			System.out.print("    Reading " + scenarioName + "/" + dataFile.getName() + "...");
			long startTime = System.currentTimeMillis();
			String line;
			String parameter = "";
			Integer timeIndex = 0;
			int count = 0;
			boolean blankLine = false;
			boolean header = true;
			try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
				while ((line = br.readLine()) != null) { // We are reading the entire file
					// Need to skip files that aren't selected times
					if (line.contains("Time =") & line.contains(",yr")) {
						String year = line.substring(line.indexOf(",wk") + 3, line.indexOf(",yr")).trim();
						float time = Math.round(Float.parseFloat(year) * 1000f) / 1000f; // Rounds to 3 decimal places
						if (selectedTimes.contains(time))
							timeIndex = selectedTimes.indexOf(time); // Parse value into float
						else {
							System.out.println(" skipped (not selected)");
							break; // Stop reading this file and move to the next
						}
					}
					// We are assuming this is always the last line of the header, good until proven otherwise
					else if (line.contains("Number of Vertices"))
						header = false;
					// These are the criteria to isolate the parameter text above blocks
					else if (!header && blankLine) {
						// Reset for the new parameter
						count = 0;
						// New clean parameter
						parameter = line.split(",")[0].trim().replaceAll("\\(", "_").replaceAll("\\)", "");
						parameter = parameter.substring(commonStartCount, parameter.length() - commonEndCount);
						if (parameter.toLowerCase().contains("porosity"))
							parameter = "porosity"; // Override if porosity
					// These are the criteria to isolate the data
					} else if (!header && !line.equals("")
							&& (selectedParameters.contains(parameter) || parameter.equals("porosity"))) {
						String[] tokens = line.trim().split("\\s+"); // The line is space delimited
						for (String token : tokens) { // Loop through the tokens
							try {
								//int index = nodeMap.get(count)-1;
								float value = Float.parseFloat(token); // Parse value into float
								if(parameter=="porosity") { // Special handling for porosity
									if(porosity==null) porosity = new float[nodes];
									porosity[count] = value;
								} else {// All other values go here
									dataMap.get(scenarioName).get(parameter)[timeIndex][count] = value;
									if(!structuredGrid) counterMap.get(scenarioName).get(parameter)[timeIndex][count]++;
									if (value < statistics.get(scenarioName).get(parameter)[0])
										statistics.get(scenarioName).get(parameter)[0] = value; // Min
									statistics.get(scenarioName).get(parameter)[1] += value/nodes/selectedTimes.size(); // Avg
									if (value > statistics.get(scenarioName).get(parameter)[2])
										statistics.get(scenarioName).get(parameter)[2] = value; // Max
								}
							} catch (Exception e) {
								System.out.println("Error parsing the " + parameter + " value: " + token);
							}
							count++;
						}
					}
					if (line.isEmpty())
						blankLine = true;
					else
						blankLine = false;
				}
			} catch (IOException e) {
				System.out.println(" error reading the file");
				e.printStackTrace();
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			if (!parameter.equals(""))
				System.out.println(" took " + Constants.formatSeconds(endTime));
		}
		// STOMP orders KJI and needs to be reordered into an IJK index
		for(String parameter : selectedParameters) {
			for(int timeIndex=0; timeIndex<selectedTimes.size(); timeIndex++)
				dataMap.get(scenarioName).get(parameter)[timeIndex] = reorderIJKtoKJI(dataMap.get(scenarioName).get(parameter)[timeIndex]);
		}
		if(porosity!=null)
			porosity = reorderIJKtoKJI(porosity);
	}

	/////////////////////////////////////////
	//////// NUFT Conversion Methods ////////
	/////////////////////////////////////////
	
	/**
	 * Extracting scenarios, times, parameters, and xyz from the input directory.
	 * Only works for NUFT files, requiring a file per scenario.
	 * @param parentDirectory
	 */
	public void extractNuftStructure(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.ntab"); // Ignore any files in the directory that aren't NUFT
																	// files
		// A quick check that we actually have NUFT files in the directory
		if (directory.listFiles(fileFilter).length == 0) {
			System.out.println("No NUFT files were found in the selected directory.");
			return;
		}
		// Add all the scenarios and parameters from file names
		for (File subFile : directory.listFiles(fileFilter)) {
			String scenarioName = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
			if (!scenarioNames.contains(scenarioName))
				scenarioNames.add(scenarioName);
			String parameter = subFile.getName().replace(".ntab", "").replaceAll("\\d+", "").replaceAll("\\.", "_");
			if (!parameters.contains(parameter))
				parameters.add(parameter);
		}
		// Read the first file to get the times and xyz
		File firstFile = directory.listFiles(fileFilter)[0];
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
			while ((line = br.readLine()) != null) { // We actually have to read the whole file... times are scattered
														// throughout
				// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
				String[] tokens = line.split("\\s+"); // The line is space delimited
				if (line.startsWith("index")) { // The header lists all the times
					// Create a map for each column to a value
					for (String token : tokens) {
						indexMap.add(token);
						if (token.equalsIgnoreCase("volume")) {
							indexMap.add("data");
							break; // Volume is the last index before times
						}
					}
					// Now we add the time
					for (int i = indexMap.indexOf("data"); i < tokens.length; i++) {
						String temp = tokens[i];
						if (temp.contains("y")) {
							units.put("times", "Years");
						}
						String token = tokens[i].replaceAll("[^0-9.]", ""); // Replace letters
						times.add(Float.parseFloat(token));
					}
				} else { // Break when we finish reading the header
					float xValue = Float.parseFloat(tokens[indexMap.indexOf("x")]);
					float yValue = Float.parseFloat(tokens[indexMap.indexOf("y")]);
					float zValue = Float.parseFloat(tokens[indexMap.indexOf("z")]);
					if (!x.contains(xValue))
						x.add(xValue);
					if (!y.contains(yValue))
						y.add(yValue);
					if (!z.contains(zValue))
						z.add(zValue);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Unstructured grids will be mapped to a non-uniform structured grid
		// Assume that anything with more than 10,000 x values is unstructured
		if(x.size()>10000) structuredGrid = false;
		if(!structuredGrid) {
			int oldSize = x.size() * y.size() * z.size();
			// Keep halving the number of nodes until below the hard limit
			while(x.size()*y.size()*z.size()>nodeLimit) {
				x = reduceArray(x);
				y = reduceArray(y);
				z = reduceArray(z);
			}
			System.out.println("Reduced the number of nodes from "+oldSize+" to "+x.size()*y.size()*z.size());
		}
		nodes = x.size() * y.size() * z.size();
		// Provided values are at the nodes (center) of each cell
		vertexX = calculateEdges(x);
		vertexY = calculateEdges(y);
		vertexZ = calculateEdges(z);
		cleanParameters();
	}
	
	/**
	 * Extracting data, statistics, and porosity from a scenario directory.
	 * Only works for NUFT files, requiring a folder per scenario.
	 * @param directory
	 */
	public void extractNuftData(File directory, String scenarioThread) {
		FileFilter fileFilter = new WildcardFileFilter("*.ntab"); // Ignore any files in the directory that aren't NUFT
																	// files
		for (File subFile : directory.listFiles(fileFilter)) {
			String scenarioName = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
			if (!scenarioThread.equals(scenarioName))
				continue; // Skip all but the scenario assigned to this thread
			if (!dataMap.containsKey(scenarioName)) {
				dataMap.put(scenarioName, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
				statistics.put(scenarioName, new HashMap<String, float[]>()); // Initialize statistics for this scenario
				if(!structuredGrid) counterMap.put(scenarioName, new HashMap<String, int[][]>());
				for(String parameter : selectedParameters) {
					dataMap.get(scenarioName).put(parameter, new float[selectedTimes.size()][nodes]);
					statistics.get(scenarioName).put(parameter, new float[3]);
					if(!structuredGrid) counterMap.get(scenarioName).put(parameter, new int[selectedTimes.size()][nodes]);
				}
			}
			String parameter = subFile.getName().replace(".ntab", "").replaceAll("\\d+", "").replaceAll("\\.", "_");
			parameter = parameter.substring(commonStartCount, parameter.length() - commonEndCount);
			if (parameter.toLowerCase().contains("porosity"))
				parameter = "porosity"; // Override if porosity
			if (!selectedParameters.contains(parameter) && !parameter.equals("porosity"))
				continue; // Skip parameters that weren't selected
			long startTime = System.currentTimeMillis();
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
				while ((line = br.readLine()) != null) { // Read each line
					// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
					String[] tokens = line.split("\\s+"); // The line is space delimited
					if (!tokens[0].equalsIgnoreCase("index")) { // Ignore the header
						//Assume i comes first, j comes second, k comes third
						//These IJK values are already 1-indexed
						int index = 0;
						if(structuredGrid) {
							index = Constants.nodeNumberToIndex(Integer.parseInt(tokens[indexMap.indexOf("index")]), x.size(), y.size(), z.size());
						} else {
							int i = x.indexOf(getKeyInInterval(Double.parseDouble(tokens[indexMap.indexOf("x")]), x))+1;
							int j = x.indexOf(getKeyInInterval(Double.parseDouble(tokens[indexMap.indexOf("y")]), x))+1;
							int k = x.indexOf(getKeyInInterval(Double.parseDouble(tokens[indexMap.indexOf("z")]), x))+1;
							index = Constants.ijkToIndex(i, j, k, y.size(), z.size());
						}
						for (int ii = indexMap.indexOf("data"); ii < tokens.length; ii++) { // Only read data
							// Determine the time index or skip if time was not selected
							float time = times.get(ii - indexMap.indexOf("data"));
							if (!selectedTimes.contains(time))
								continue; // Skip times that weren't selected
							int timeIndex = selectedTimes.indexOf(time);
							// Store the value
							float value = Float.parseFloat(tokens[ii]);
							if(parameter.equals("porosity")) {
								if(porosity==null) porosity = new float[nodes];
								porosity[index] = value;
							} else {
								if(!structuredGrid) //Unstructured grid
									counterMap.get(scenarioName).get(parameter)[timeIndex][index]++;
								dataMap.get(scenarioName).get(parameter)[timeIndex][index] += value;
								if (value < statistics.get(scenarioName).get(parameter)[0])
									statistics.get(scenarioName).get(parameter)[0] = value; // Min
								statistics.get(scenarioName).get(parameter)[1] += value / nodes / selectedTimes.size(); // Avg
								if (value > statistics.get(scenarioName).get(parameter)[2])
									statistics.get(scenarioName).get(parameter)[2] = value; // Max
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			// For unstructured grids, need to average values and fill blanks
			if(!structuredGrid) {//Unstructured grid
				unstructuredFillBlanks();
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			System.out.println("    Reading " + subFile.getName() + "... took " + Constants.formatSeconds(endTime));
		}
	}
	
	/**
	 * Extracting scenarios, times, parameters, and xyz from the input directory.
	 * Only works for TecPlot files, requiring a file per scenario.
	 * @param directory
	 */
	public void extractTecplotStructure(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.dat"); // Ignore any files in the directory that aren't
																	// Tecplot files
		// A quick check that we actually have Tecplot files in the directory
		if (directory.listFiles(fileFilter).length == 0) {
			System.out.println("No Tecplot files were found in the selected directory.");
			return;
		}
		// Add all the scenarios from file names.
		for (File subFile : directory.listFiles(fileFilter)) {
			String scenarioName = subFile.getName().split("\\.")[0];
			if (!scenarioNames.contains(scenarioName))
				scenarioNames.add(scenarioName);
		}
		// Read the first file to get the times, parameters, and xyz
		File firstFile = directory.listFiles(fileFilter)[0];
		try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
			String line;
			String key = "";
			int index = 0;
			int countNodes = 0;
			while ((line = br.readLine()) != null) { // We actually have to read the whole file... times are scattered
														// throughout
				// Add the parameters and units while generating an indexMap
				if (line.contains("VARIABLES")) {
					String[] tokens = line.split("\""); // Split by quotes to isolate parameter and unit
					for (String token : tokens) {
						if (token.contains("=") || token.trim().equals(""))
							continue; // Skip everything but the parameters
						String parameter = token.split(",")[0].trim().toLowerCase();
						if (parameter.toLowerCase().contains("porosity"))
							parameter = "porosity";
						indexMap.add(parameter); // Store the order which parameters are provided
						if (!parameters.contains(parameter) && !parameter.equals("x") && !parameter.equals("y")
								&& !parameter.equals("z") && !parameter.equals("volume")
								&& !parameter.equals("porosity")) // Skip these
							parameters.add(parameter); // Save parameters
						if (token.contains(",")) { // This means they give units
							String unit = token.split(",")[1].trim();
							units.put(parameter, unit); // Save units
						}
					}
				}
				// Add the times from this line, scattered through file
				else if (line.contains("ZONE")) { // This lists the zone name, which includes the timestep
					String[] tokens = line.split("\""); // Zone name is wrapped in quotes
					String time = tokens[1].replaceAll("[^0-9.]", "");
					try {
						times.add(Float.parseFloat(time)); // Parse value into float
					} catch (Exception e) {
						System.out.println("Years Error: " + time);
					}
					// Also add the number of nodes, as this will be used to count off blocks
					tokens = line.split("[ ,]"); // Header info is comma delimited
					for (int i = 0; i < tokens.length; i++) {
						try {
							if (tokens[i].equals("NODES")) {
								nodes = Integer.parseInt(tokens[i + 2]);
							} else if (tokens[i].equals("ELEMENTS")) {
								elements = Integer.parseInt(tokens[i + 2]);
							}
						} catch (Exception theException) {
							System.out.println("NODE/ELEMENTS: " + tokens[i]);
						}
					}
				}
				// This lists the data, from which we will get xyz (which are actually at edges)
				else if (!line.contains("=")
						&& (key.equals("") || key.equals("x") || key.equals("y") || key.equals("z"))) {
					key = indexMap.get(index);
					String[] tokens = line.split("\\s+");  // The line is space delimited
					for (String token : tokens) {
						countNodes++;
						float value = Float.parseFloat(token);
						if (key.equals("x") & !x.contains(value))
							x.add(value);
						else if (key.equals("y") & !y.contains(value))
							y.add(value);
						else if (key.equals("z") & !z.contains(value))
							z.add(value);
					}
					// When the counter is high enough, we have finished with the parameter
					if (countNodes >= nodes) {
						index++; // On to the next parameter
						countNodes = 0; // Reset the counter
					}
				}
			}
			System.out.println(units.toString() + " These are the units");
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Unstructured grids will be mapped to a non-uniform structured grid
		// Assume that anything with more than 10,000 x values is unstructured
		if(x.size()>10000) structuredGrid = false;
		if(!structuredGrid) {
			int oldSize = x.size() * y.size() * z.size();
			// Keep halving the number of nodes until below the hard limit
			while(x.size()*y.size()*z.size()>nodeLimit) {
				x = reduceArray(x);
				y = reduceArray(y);
				z = reduceArray(z);
			}
			System.out.println("Reduced the number of nodes from "+oldSize+" to "+x.size()*y.size()*z.size());
		}
		nodes = x.size() * y.size() * z.size();
		// Provided values are at the edge of each cell, calculate the center
		vertexX = x; vertexY = y; vertexZ = z;
		x = calculateCenters(vertexX);
		y = calculateCenters(vertexY);
		z = calculateCenters(vertexZ);
	}
	
	/**
	 * Extracting data, statistics, and porosity from a scenario directory.
	 * Only works for TecPlot files, requiring a file per scenario.
	 * @param directory
	 */
	public void extractTecplotData(File subFile) {
		String scenarioName = subFile.getName().split("\\.")[0];
		System.out.print("Reading " + subFile.getName() + "...");
		long startTime = System.currentTimeMillis();
		// Initialize global variables
		if(!structuredGrid) counterMap.put(scenarioName, new HashMap<String, int[][]>());
		dataMap.put(scenarioName, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
		statistics.put(scenarioName, new HashMap<String, float[]>()); // Initialize statistics for this scenario
		for (String parameter : selectedParameters) {
			if(!structuredGrid) //Unstructured grid
				counterMap.get(scenarioName).put(parameter, new int[selectedTimes.size()][nodes]);
			dataMap.get(scenarioName).put(parameter, new float[selectedTimes.size()][nodes]);
			statistics.get(scenarioName).put(parameter, new float[3]);
		}
		int index = 0;
		String line;
		String parameter = indexMap.get(index);
		int timeIndex = 0;
		//float[] tempData = new float[elements];
		boolean nextHeader = false;
		int countElements = 0;
		int countNodes = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
			while ((line = br.readLine()) != null) {
				// Get the time index from the header
				if (line.contains("ZONE") && !nextHeader) {
					nextHeader = false;// This lists the zone name, which includes the timestep
					String[] tokens = line.split("\""); // Zone name is wrapped in quotes
					float time = Float.parseFloat(tokens[1].replaceAll("[^0-9.]", ""));
					if (!selectedTimes.contains(time))
						continue; // The timestep wasn't selected, skip it
					timeIndex = selectedTimes.indexOf(time); // Parse value into float

				}
				// This is the data - we want all selected parameters and porosity
				if (!line.contains("=") && !line.trim().isEmpty() && !nextHeader) {
					String[] tokens = line.split("\\s+"); // The line is space delimited
					// Count the numbers so we know when we finish the block - x, y, z based on node count
					if (parameter.equals("x") || parameter.equals("y") || parameter.equals("z")) {
						countNodes += tokens.length;
					// Read in data for all selected parameters and porosity, saving statistics as we go
					// Any parameters that weren't selected are skipped
					} else if (selectedParameters.contains(parameter)) {
						for (String token : tokens) {
							float value = Float.parseFloat(token);
							int nIndex = Constants.nodeNumberToIndex(countElements+1, x.size(), y.size(), z.size());
							if(!structuredGrid) //Unstructured grid
								counterMap.get(scenarioName).get(parameter)[timeIndex][nIndex]++;
							dataMap.get(scenarioName).get(parameter)[timeIndex][nIndex] += value;
							if (value < statistics.get(scenarioName).get(parameter)[0])
								statistics.get(scenarioName).get(parameter)[0] = value; // Min
							statistics.get(scenarioName).get(parameter)[1] += value / nodes / selectedTimes.size(); // Avg
							if (value > statistics.get(scenarioName).get(parameter)[2])
								statistics.get(scenarioName).get(parameter)[2] = value; // Max
							countElements++;
						}
					// Only the first porosity is stored (special handling)
					} else if (parameter.equals("porosity") && porosity == null) {
						for (String token : tokens) {
							float value = Float.parseFloat(token);
							porosity = new float[nodes];
							porosity[index] = value;
							countElements++;
						}
					// Count the numbers so we know when we finish the block - parameters based on elements count
					} else { // Unselected parameters
						countElements += tokens.length;
					}
					// When the counter is high enough, we have finished with the parameter and should save
					if (countNodes >= nodes || countElements >= elements) {
						// Tecplot orders values differently and needs to be reordered
						if (selectedParameters.contains(parameter)) {
							dataMap.get(scenarioName).get(parameter)[timeIndex] = reorderIJKtoKJI(dataMap.get(scenarioName).get(parameter)[timeIndex]);
						} else if(parameter.toLowerCase().contains("porosity")) {
							porosity = reorderIJKtoKJI(porosity);
						}
						countNodes = 0; // Reset the counter
						countElements = 0; // Reset the counter
						System.out.print(parameter + " ");
						if (index < indexMap.size() - 1)
							index++;
						else if (index == indexMap.size() - 1 && !line.toLowerCase().contains("zone"))
							nextHeader = true;
						else
							index = 3; // Index of the first parameter that is not x, y, z
						parameter = indexMap.get(index);
					}
				}
			}
			// For unstructured grids, need to average values and fill blanks
			if(!structuredGrid) {//Unstructured grid
				unstructuredFillBlanks();
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			System.out.println(" took " + Constants.formatSeconds(endTime));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracting scenarios, times, parameters, and xyz from the input directory.
	 * Only works for TOUGH2 files, requiring a folder per scenario.
	 * @param parentDirectory
	 */
	public void extractToughStructure(File parentDirectory) {
		// The out files don't provide column headers, so we need to read them from a separate file
		// Values are hard-coded because they are determined by Fortran code
		// Populate index map, parameters, and units
		indexMap.add("x"); units.put("x", "m"); //0
		indexMap.add("y"); units.put("y", "m"); //1
		indexMap.add("z"); units.put("z", "m"); //2
		indexMap.add("pressure"); parameters.add("pressure"); units.put("pressure", "Pa"); //3
		indexMap.add("co2 saturation"); parameters.add("co2 saturation"); //4
		indexMap.add("xcl salt mass fraction"); parameters.add("xcl salt mass fraction"); //5
		indexMap.add("temperature"); parameters.add("temperature"); //6
		indexMap.add("co2 density"); parameters.add("co2 density"); units.put("co2 density", "kg/m^3"); //7
		indexMap.add("brine density"); parameters.add("brine density"); units.put("brine density", "kg/m^3"); //8
		indexMap.add("density matrix"); parameters.add("density matrix"); units.put("density matrix", "kg/m^3"); //9
		indexMap.add("porosity"); //10 - don't add as parameter
		indexMap.add("permeability x"); //11 - don't add as parameter
		indexMap.add("permeability y"); //12 - don't add as parameter
		indexMap.add("permeability z"); //13 - don't add as parameter
		
		// Get scenario names and times from list of directories in the parent folder
		FileFilter fileFilter = new WildcardFileFilter("*.OUT"); // Ignore any files in the directory that aren't TOUGH files
		boolean doOnce = true;
		for (File directory : parentDirectory.listFiles()) {
			if (!directory.isDirectory()) continue; // We only want folders - skip files
			// A quick check that we actually have TOUGH files in the directory
			if (directory.listFiles().length == 0) {
				System.out.println("No files were found for the following scenario: "+directory.getName());
				continue;
			}
			// Add all the scenarios from folder names
			String scenarioName = directory.getName();
			if (!scenarioNames.contains(scenarioName))
				scenarioNames.add(scenarioName);
			// Get the times from the file names in the first directory
			if (doOnce) {
				for (File subfile : directory.listFiles(fileFilter)) {
					String[] t = subfile.getName().split("\\.")[0].split("_");
					String time = t[t.length - 1].replaceAll("\\D+", "");
					times.add(Float.parseFloat(time));
				}
				doOnce = false;
			}
		}
		
		// Get xyz from the first file
		long startTime = System.currentTimeMillis();
		for (File directory : parentDirectory.listFiles()) {
			if (!directory.isDirectory()) continue; // We only want folders - skip files
			File firstFile = directory.listFiles(fileFilter)[0];
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
				while ((line = br.readLine()) != null) { // We actually have to read the whole file...
					// index is pulled from the structure map earlier
					String[] tokens = line.trim().split("\\s+"); // The line is space delimited
					for (String parameter : indexMap.subList(0, 2)) {
						float value = Float.parseFloat(tokens[indexMap.indexOf(parameter)]);
						if (parameter.equals("x") && !x.contains(value))
							x.add(value);
						else if (parameter.equals("y") && !y.contains(value))
							y.add(value);
						else if (parameter.equals("z") && !z.contains(value))
							z.add(value);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			break; //Only need to read one file
		}
		long endTime = (System.currentTimeMillis()-startTime)/1000;
		System.out.println("Reading node structure took "+Constants.formatSeconds(endTime));
		
		// Unstructured grids will be mapped to a non-uniform structured grid
		// Assume that anything with more than 10,000 x values is unstructured
		if(x.size()>10000) structuredGrid = false; //TODO: May want a more sophisticated method in the future
		if(!structuredGrid) {
			// Keep halving the number of nodes until below the hard limit
			int oldSize = x.size() * y.size() * z.size();
			while(x.size()*y.size()*z.size()>nodeLimit) {
				x = reduceArray(x);
				y = reduceArray(y);
				z = reduceArray(z);
			}
			System.out.println("Reduced the number of nodes from "+oldSize+" to "+x.size()*y.size()*z.size());
		}
		nodes = x.size() * y.size() * z.size();
		// Provided values are at the nodes (center) of each cell, calculate edges
		vertexX = calculateEdges(x);
		vertexY = calculateEdges(y);
		vertexZ = calculateEdges(z);
		cleanParameters();
	}
	
	/**
	 * Extracting data, statistics, and porosity from a scenario directory.
	 * Only works for TOUGH2 files, requiring a folder per scenario.
	 * @param directory
	 */
	//TODO: Might have to assign the 0's as a value when calculating wet bulk density.
	public void extractToughData(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.OUT"); // Ignore any files in the directory that aren't TOUGH
																	// files
		String scenarioName = directory.getName();
		if (!selectedScenarios.contains(scenarioName))
			return; // Make sure this is a selected scenario
		// Initialize global variables
		if(!structuredGrid) counterMap.put(scenarioName, new HashMap<String, int[][]>());
		dataMap.put(scenarioName, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
		statistics.put(scenarioName, new HashMap<String, float[]>()); // Initialize statistics for this scenario
		for (String parameter : selectedParameters) {
			if(!structuredGrid) //Unstructured grid
				counterMap.get(scenarioName).put(parameter, new int[selectedTimes.size()][nodes]);
			dataMap.get(scenarioName).put(parameter, new float[selectedTimes.size()][nodes]);
			statistics.get(scenarioName).put(parameter, new float[3]);
		}
		System.out.println("Reading variables: " + selectedParameters.toString());
		// Loop through the list of files in each directory
		for (File dataFile : directory.listFiles(fileFilter)) {
			// Verify that the file represents a selected time step
			String[] t = dataFile.getName().split("\\.")[0].split("_");
			// Determine the time index or skip if time was not selected
			Float time = Float.parseFloat(t[t.length - 1].replaceAll("\\D+", ""));
			if (!selectedTimes.contains(time))
				continue;
			int timeIndex = selectedTimes.indexOf(time);
			System.out.print("    Reading " + scenarioName + "/" + dataFile.getName() + "...");
			long startTime = System.currentTimeMillis();
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
				while ((line = br.readLine()) != null) { // We are reading the entire file
					// index is pulled from the structure map earlier
					String[] tokens = line.trim().split("\\s+"); // The line is space delimited
					//Assume i comes first, j comes second, k comes third
					//These IJK values are 0-indexed, so we need to add 1 to each
					int i=0; int j=0; int k=0;
					if(structuredGrid) { //Structured grid
						i = x.indexOf(Float.valueOf(tokens[0]))+1;
						j = y.indexOf(Float.valueOf(tokens[1]))+1;
						k = z.indexOf(Float.valueOf(tokens[2]))+1;
					} else if(!structuredGrid) {
						i = x.indexOf(getKeyInInterval(Double.parseDouble(tokens[0]), x))+1;
						j = y.indexOf(getKeyInInterval(Double.parseDouble(tokens[1]), y))+1;
						k = z.indexOf(getKeyInInterval(Double.parseDouble(tokens[2]), z))+1;
					}
					int index = Constants.ijkToIndex(i, j, k, y.size(), z.size());
					// i and k does not contain the parsed values.
					for (String parameter : indexMap.subList(3, indexMap.size()-1)) {
						float value = Float.parseFloat(tokens[indexMap.indexOf(parameter)]);
						if (dataMap.get(scenarioName).containsKey(parameter)) {
							// Store the value
							if(!structuredGrid) //Unstructured grid
								counterMap.get(scenarioName).get(parameter)[timeIndex][index]++; //TODO: This is where it breaks; array out of bounds
							dataMap.get(scenarioName).get(parameter)[timeIndex][index] += value;
							if (value < statistics.get(scenarioName).get(parameter)[0]) // Min
								statistics.get(scenarioName).get(parameter)[0] = value;
							else if (value > statistics.get(scenarioName).get(parameter)[2]) // Max
								statistics.get(scenarioName).get(parameter)[2] = value;
							statistics.get(scenarioName).get(parameter)[1] += value / nodes / selectedTimes.size(); // Avg
						} else if (parameter.contains("porosity")) {
							if(porosity==null) porosity = new float[nodes];
							porosity[index] += value;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			// For unstructured grids, need to average values and fill blanks
			if(!structuredGrid) {//Unstructured grid
				unstructuredFillBlanks();
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			System.out.println("    Reading " + dataFile.getName() + "... took " + Constants.formatSeconds(endTime));
		}
	}
	
	////////////////////////////////////////////////
	//// Helper methods for dealing with  grids ////
	////////////////////////////////////////////////
	
	/**
	 * When nodes are listed as KJI, this reorders to IJK for the H5 storage
	 * @param original float[]
	 * @return replacement float[]
	 */
	public float[] reorderIJKtoKJI(float[] original) {
		float[] replacement = new float[original.length];
		int counter = 0;
		for (int i = 0; i < x.size(); i++) {
			for (int j = 0; j < y.size(); j++) {
				for (int k = 0; k < z.size(); k++) {
					int nodeNumber = k * x.size() * y.size() + j * x.size() + i;
					replacement[counter] = original[nodeNumber];
					counter++;
				}
			}
		}
		return replacement;
	}
	
	/**
	 * Converts an unstructured grid to a structured grid.
	 * Halves the total number of x and y values.
	 * Maintains the spacing for a non-uniform grid.
	 * 
	 * @param ArrayList<Float>
	 * @return reduced ArrayList<Float>
	 */
	private ArrayList<Float> reduceArray(final ArrayList<Float> input) {
		Collections.sort(input);
		ArrayList<Float> output = new ArrayList<Float>(); //Initialize array
		// Increment through array by 2s
		for(int i=0; i<input.size(); i+=2)
			output.add(input.get(i));
		output.add(input.get(input.size()-1)); //Add the last value
		return output;
	}
	
	/**
	 * Maps a data point to the nearest neighbor on a grid when interpolating from unstructured to structured
	 * 
	 * @param theValue
	 * @param keyList
	 * @return the nearest key in the interval.
	 */
	private float getKeyInInterval(final double theValue, final ArrayList<Float> theXYZVal) {
		float minDiff = Float.MAX_VALUE;
		float nearest = 0;
		float diff = 0;
		for (double key : theXYZVal) {
			diff = (float) Math.abs(theValue - key);
			if (diff < minDiff) {
				nearest = (float) key;
				minDiff = diff;
			} else if (diff > minDiff) {
				break; //grid is linear, so break when the diff starts increasing
			}
		}
		return nearest;
	}
	
	/**
	 * Loops over the CounterMap to determine nodes with either 0 or more than one value.
	 * Multiple values are averaged, while no values are filled from neighbors.
	 */
	private void unstructuredFillBlanks() {
		boolean checkPorosity = false;
		for (String scen : counterMap.keySet()) {
			for (String param: counterMap.get(scen).keySet()) {
				for (int i = 0; i < counterMap.get(scen).get(param).length; i++) {
					for (int j=0; j < counterMap.get(scen).get(param)[i].length; j++) {
						// Blank value needs to be interpolated to neighbors
						if(counterMap.get(scen).get(param)[i][j] == 0)
							dataMap.get(scen).get(param)[i][j] = valueFromNeighbors(scen, param, i, j);
						// Multiple values at the same point need to be averaged
						if(counterMap.get(scen).get(param)[i][j] > 1)
							dataMap.get(scen).get(param)[i][j] /= counterMap.get(scen).get(param)[i][j];
					}
					// Also need to handle porosity, but just once
					if(!checkPorosity) {
						for (int j=0; j<porosity.length; j++) {
							// Blank value needs to be interpolated to neighbors
							if(counterMap.get(scen).get(param)[i][j] == 0)
								porosity[j] = valueFromNeighbors(scen, param, i, j);
							// Multiple values at the same point need to be averaged
							if(counterMap.get(scen).get(param)[i][j] > 1)
								porosity[j] /= counterMap.get(scen).get(param)[i][j];
						}
						checkPorosity = true;
					}
				}
			}
		}
	}
	
	/**
	 * When converting an unstructured grid to structured, there may be some empty values.
	 * Empty values should be filled by interpolation with neighbors.
	 * This function returns an average of all filled values that are neighbors.
	 * @param scenario
	 * @param parameter
	 * @param time index
	 * @param node index
	 * @return float
	 */
	private float valueFromNeighbors(String scen, String param, int time, int index) {
		float sum = 0;
		int count = 0;
		Point3i center = Constants.indexToIJK(index, y.size(), z.size());
		int i = center.getI(); //Remember, these are 1-indexed
		int j = center.getJ();
		int k = center.getK();
		ArrayList<Integer> neighbors = new ArrayList<Integer>();
		// We will exit this loop with a break statement, so the 1000 is just a conservative placeholder
		for(int loop=1; loop<1000; loop++) {
			// Check in four cardinal directions, track which nodes have values, then average
			// In case none of the neighbors have values, keep extending outward until values are found
			// Not worrying about diagonals, because that just gets messy...
			// The if statements are to prevent checking for nodes off the edge of the grid
			if(i<x.size()-loop) neighbors.add(Constants.ijkToIndex(i+loop, j, k, y.size(), z.size())); //right
			if(i>loop) neighbors.add(Constants.ijkToIndex(i-loop, j, k, y.size(), z.size())); //left
			if(j<y.size()-loop) neighbors.add(Constants.ijkToIndex(i, j+loop, k, y.size(), z.size())); //down
			if(j>loop) neighbors.add(Constants.ijkToIndex(i, j-loop, k, y.size(), z.size())); //up
			// Break once we have found a match
			if(neighbors.size()>1) break;
		}
		for(int neighbor : neighbors) {
			if(counterMap.get(scen).get(param)[time][neighbor] != 0) {
				sum += dataMap.get(scen).get(param)[time][neighbor];
				count++;
			}
		}
		if(count==0)
			System.out.println("Error: no neighbors found for IJK = " + center.toString());
		else
			System.out.println("Found results in "+count+" loops.");
		return sum/count;
	}
	
	/**
	 * When only cell centers are provided, calculate the edges.
	 * Assumes that edges are at the halfway point between centers.
	 * @param ArrayList<Float> cellCenters
	 * @return ArrayList<Float> cellEdges
	 */
	private static ArrayList<Float> calculateEdges(ArrayList<Float> cellCenters) {
		ArrayList<Float> cellEdges = new ArrayList<Float>();
		for (int i = 1; i < cellCenters.size(); i++) {
			float half = (cellCenters.get(i) - cellCenters.get(i - 1)) / 2;
			if (i == 1)
				cellEdges.add(Float.valueOf(cellCenters.get(i - 1) - half).floatValue());
			cellEdges.add(Float.valueOf(cellCenters.get(i - 1) + half).floatValue());
			if (i == cellCenters.size() - 1)
				cellEdges.add(Float.valueOf(cellCenters.get(i) + half).floatValue());
		}
		return cellEdges;
	}
	
	/**
	 * When only cell edges are provided, calculate the centers.
	 * Assumes that centers are at the halfway point between edges.
	 * @param ArrayList<Float> cellEdges
	 * @return ArrayList<Float> cellCenters
	 */
	private static ArrayList<Float> calculateCenters(ArrayList<Float> cellEdges) {
		ArrayList<Float> cellCenters = new ArrayList<Float>();
		for (int i = 1; i < cellEdges.size(); i++) {
			cellCenters.add(cellEdges.get(i) - (cellEdges.get(i) - cellEdges.get(i - 1)) / 2);
		}
		return cellCenters;
	}
	
	/**
	 * Loops through parameters to clean the string values
	 * Removes common text at beginning or end of all parameters
	 */
	private void cleanParameters() {
		for (int i = 1; i < parameters.size(); i++) {
			char[] first = parameters.get(i - 1).toLowerCase().toCharArray();
			char[] second = parameters.get(i).toLowerCase().toCharArray();
			int minLength = Math.min(first.length, second.length); // So we don't exceed the array length
			int startCount = 0;
			int endCount = 0;
			// Finding the number of starting characters in common
			for (int j = 0; j < minLength; j++) {
				if (first[j] == second[j])
					startCount = j + 1;
				else
					break;
			}
			// Finding the number of ending characters in common
			for (int j = 1; j < minLength; j++) {
				if (first[first.length - j] == second[second.length - j])
					endCount = j;
				else
					break;
			}
			if (startCount < commonStartCount)
				commonStartCount = startCount;
			if (endCount < commonEndCount)
				commonEndCount = endCount;
		}
		// Now remove the common start and end from all parameters
		for (int i = 0; i < parameters.size(); i++) {
			String replacement = parameters.get(i).substring(commonStartCount,
					parameters.get(i).length() - commonEndCount);
			parameters.set(i, replacement);
		}
	}

	///////////////////////////
	//// Getters & Setters ////
	///////////////////////////

	public String[] getScenarios() {
		return scenarioNames.toArray(new String[scenarioNames.size()]);
	}

	public Float[] getTimes() {
		return times.toArray(new Float[times.size()]);
	}

	public String[] getParameters() {
		return parameters.toArray(new String[parameters.size()]);
	}

	public void setSelected(JCheckBox[] listScenarios, JCheckBox[] listTimes, JCheckBox[] listParameters) {
		selectedScenarios.clear();
		selectedTimes.clear();
		selectedParameters.clear();
		for (JCheckBox scenario : listScenarios) {
			if (scenario.isSelected())
				selectedScenarios.add(scenario.getText());
		}
		for (JCheckBox time : listTimes) {
			if (time.isSelected())
				selectedTimes.add(Float.parseFloat(time.getText()));
		}
		for (JCheckBox parameter : listParameters) {
			if (parameter.isSelected())
				selectedParameters.add(parameter.getText());
		}
	}

	public ArrayList<String> getSelectedScenarios() {
		return selectedScenarios;
	}

	public ArrayList<Float> getSelectedTimes() {
		return selectedTimes;
	}

	public Float[] getSelectedTimesArray() {
		return selectedTimes.toArray(new Float[selectedTimes.size()]);
	}

	public int getSelectedTimeIndex(float time) {
		return selectedTimes.indexOf(time);
	}

	public ArrayList<String> getSelectedParameters() {
		return selectedParameters;
	}

	public Float[] getX() {
		return x.toArray(new Float[x.size()]);
	}

	public Float[] getY() {
		return y.toArray(new Float[y.size()]);
	}

	public Float[] getZ() {
		return z.toArray(new Float[z.size()]);
	}

	public Float[] getVertexX() {
		return vertexX.toArray(new Float[vertexX.size()]);
	}

	public Float[] getVertexY() {
		return vertexY.toArray(new Float[vertexY.size()]);
	}

	public Float[] getVertexZ() {
		return vertexZ.toArray(new Float[vertexZ.size()]);
	}

	public float[] getPorosity() {
		return porosity;
	}

	public void setPorosity(float[] porosity) {
		this.porosity = porosity;
	}

	public float[][] getData(String scenarioName, String parameter) {
		if (dataMap.get(scenarioName).containsKey(parameter))
			return dataMap.get(scenarioName).get(parameter);
		return null;
	}

	public float[] getStatistics(String scenarioName, String parameter) {
		if (statistics.get(scenarioName).containsKey(parameter))
			return statistics.get(scenarioName).get(parameter);
		return null;
	}

	public String getUnit(String parameter) {
		if (units.containsKey(parameter))
			return units.get(parameter);
		return "";
	}

	public void setUnit(final String theParameter, final String theValue) {
		units.put(theParameter, theValue);
	}

	public String getPositive() {
		return positive;
	}
	
	public void setPositive(String positive) {
		this.positive = positive;
	}
	
}
