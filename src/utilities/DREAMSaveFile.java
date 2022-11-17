package utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * This file reads a save file and stores values to enable DREAM to run headlessly
 * @author whit162
 *
 */
public class DREAMSaveFile {
	private boolean isDirectory;
	private String directory;
	private String[] files;
	private ArrayList<String[]> leakData;
	private String weightEquation;
	private ArrayList<String[]> weights;
	private ArrayList<String[]> sensorData;
	private ArrayList<ArrayList<String[]>> criteria;
	private String maxBudget;
	private String maxSensors;
	private String maxMoves;
	private String maxWells;
	private String minDistanceWells;
	private String costPerWell;
	private String costPerDepthWell;
	private String stationLocations;
	private String repeatedSurveys;
	private String outputDirectory;
	private String runs;
	private String iterations;
	private String algorithm;
	private String algorithmEquation;
	private boolean weightObjectives;
	private boolean objTTD;
	private boolean objCost;
	private boolean objVADatTTD;
	private boolean objPercentScenarios;
	private boolean paretoRanks;
	private boolean visualization;
	private boolean objectiveGraph;
	
	public void readSaveFile(File saveFile) {
		String section = "";
		String line;
		int count = 0;
		leakData = new ArrayList<String[]>();
		weights = new ArrayList<String[]>();
		sensorData = new ArrayList<String[]>();
		criteria = new ArrayList<ArrayList<String[]>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(saveFile));
			while ((line = br.readLine()) != null) {
				line = line.trim().toLowerCase(); // Strip spaces, case insensitive
				if (line.startsWith("--")) {
					section = line.replaceAll("--", "").trim();
					count = 0;
				}
				
				//////// Input Directory ////////
				else if (section.equals("inputdirectory")) {
					// first line should be "Directory" or "Files"
					if (line.contains("directory"))
						isDirectory = true;
					else if (line.contains("files"))
						isDirectory = false;
					// second line should be the directory path
					if (line.contains(File.separator))
						directory = line;
					// (optional) third line is a comma-separated list of files
					if (line.contains(",")) {
						files = line.split(",");
					}
				//////// Initialize Units ////////
				} else if (section.equals("initializeunits")) {
					//TODO
				//////// Leak Definition ////////
				} else if (section.equals("leakdefinition")) {
					// parameter, type, threshold
					leakData.add(line.split(","));
				//////// Scenario Weighting ////////
				} else if (section.equals("scenarioweighting")) {
					// first line is the weighting equation
					if (!line.contains(","))
						weightEquation = line;
					// scenario, variable a, variable b
					else
						weights.add(line.split(","));
				//////// Define Sensors ////////
				} else if (section.equals("definesensors")) {
					// parameter, alias, cost, type, threshold, method, bottom, top
					sensorData.add(line.split(","));
				//////// Detection Criteria ////////
				} else if (section.equals("detectioncriteria")) {
					if (line.contains(","))
						criteria.get(count).add(line.split(","));
					if (line.contains("or"))
						count++;
				//////// Monitoring Campaign Settings ////////
				} else if (section.equals("monitoringcampaignsettings")) {
					if (count==0)
						maxBudget = line;
					else if (count==1)
						maxSensors = line;
					else if (count==2)
						maxMoves = line;
					else if (count==3)
						maxWells = line;
					else if (count==4)
						minDistanceWells = line;
					else if (count==5)
						costPerWell = line;
					else if (count==6)
						costPerDepthWell = line;
					else if (count==7)
						stationLocations = line;
					else if (count==8)
						repeatedSurveys = line;
					count++;
				//////// Exclude Locations ////////
				} else if (section.equals("excludelocations")) {
					//TODO
				//////// Run DREAM ////////
				} else if (section.equals("rundream")) {
					if (count==0)
						outputDirectory = line;
					if (count==1)
						runs = line;
					if (count==2)
						iterations = line;
					if (count==3)
						algorithm = line;
					if (count==4)
						algorithmEquation = line;
					if (count==5)
						weightObjectives = Boolean.valueOf(line);
					if (count==6)
						objTTD = Boolean.valueOf(line);
					if (count==7)
						objCost = Boolean.valueOf(line);
					if (count==8)
						objVADatTTD = Boolean.valueOf(line);
					if (count==9)
						objPercentScenarios = Boolean.valueOf(line);
					if (count==10)
						paretoRanks = Boolean.valueOf(line);
					if (count==11)
						visualization = Boolean.valueOf(line);
					if (count==12)
						objectiveGraph = Boolean.valueOf(line);
					count++;
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error loading DREAM from " + saveFile.getName());
			e.printStackTrace();
		}
	}
}
