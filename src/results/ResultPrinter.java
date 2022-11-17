package results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import functions.ObjectiveSelection.OBJECTIVE;
import objects.Campaign;
import objects.Scenario;
import objects.ScenarioSet;
import objects.Sensor;
import utilities.Constants;
import utilities.StatisticsTools;
import wizardPages.DREAMWizard.STORMData;

/**
 * Utility methods for printing results
 * @author whit162
 */
public class ResultPrinter {

	// Where to put the results
	public static String resultsDirectory;
	// This is where we will store all the campaigns from the algorithm, for each run
	public static ArrayList<ArrayList<Campaign>> allCampaignsMap; //Run, list<campaigns> (by iteration)
	// Store all the campaigns based on pareto rank, across all runs
	public static ArrayList<ArrayList<Campaign>> rankedCampaigns; //Pareto rank, list<campaigns>	
	
	//TODO: Can we remove these?
	// Print a file for each run, for each type, that contains all iterations,
	// their times to detection, and their campaign
	public static TimeToDetectionPlots ttdPlots;
	
	
	/**
	 * Runs at the start of each optimization, in case previous results exist
	 */
	public static void clearResults() {
		allCampaignsMap = new ArrayList<ArrayList<Campaign>>();
		rankedCampaigns = new ArrayList<ArrayList<Campaign>>();
	}
	
	/**
	 * After the algorithm has completed, this method gets called
	 * Calls post-processing and prints outputs with results
	 * @param data
	 */
	public static void postProcessingResults(STORMData data) {
		// Prints one file for each run, showing all campaign iterations
		try {
			printAllCampaigns(data, 0);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to print all campaigns");
		}
		// Prints the best campaigns across all runs, top pareto ranks
		if(Constants.paretoRank) {
			StatisticsTools.calculateParetoRanks(data.getObjectives(), data.getSet());
			try {
				printRankedCampaigns(data, 0);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Failed to print all campaigns");
			}
		}
	}
	
	//TODO: Should this be removed or moved??
	public static void newTTDPlots(ScenarioSet set, int run) {
		if(ttdPlots != null) ttdPlots.dispose(); //only display the current run
		ttdPlots = new TimeToDetectionPlots(set.getIterations(), set.getNodeStructure().getTimeSteps().get(set.getNodeStructure().getTimeSteps().size()-1).getRealTime(), run);
	}
	
	/**
	 * Primary method for storing campaigns at each iteration
	 * @param run
	 * @param campaign
	 */
	public static void saveCampaign(int run, Campaign campaign) {
		if(allCampaignsMap.size()<=run)
			allCampaignsMap.add(new ArrayList<Campaign>());
		allCampaignsMap.get(run).add(campaign);
	}
	
	/**
	 * Primary method for storing pareto-ranked campaigns
	 * Incoming rank must be 0-indexed
	 * @param rank
	 * @param campaign
	 */
	public static void saveRankedCampign(int rank, Campaign campaign) {
		if(rankedCampaigns.size() == rank)
			rankedCampaigns.add(new ArrayList<Campaign>());
		if(!rankedCampaigns.get(rank).contains(campaign))
			rankedCampaigns.get(rank).add(campaign);
	}
	
	/**
	 * Prints out details for each campaign tested by the algorithm by order of iteration
	 * Each campaign lists:
	 *		iteration, % scenarios detected, average TTD, range of TTD, scenarios not detected, 
	 * 		number of wells, cost of campaign, volume of aquifer degraded, and a list of sensors
	 * @param data
	 * @param limit (int)
	 * @throws IOException
	 */
	public static void printAllCampaigns(STORMData data, int limit) throws IOException {
		// Create one file for each run
		for(int run=0; run<allCampaignsMap.size(); run++) {
			// Create the header for the file
			List<String> lines = new ArrayList<String>();
			lines.add("Iteration,"
					+ "Scenarios with Leak Detected,"
					+ "Scenarios with No Leak Detected,"
					+ "Average TTD of Successful Scenarios,"
					+ (data.getObjectives().contains(OBJECTIVE.VAD_AT_DETECTION) ? "Average VAD at TTD," : "")
					+ (data.getObjectives().contains(OBJECTIVE.COST) ? "Average Cost of Successful Scenarios," : "")
					+ "Number of Wells,"
					+ "Sensor Types (x y z)");
			
			// Loop over all campaigns (unsorted)
			int count = 0;
			if(limit==0) limit = Integer.MAX_VALUE; //Entering 0 for limit means no limit
			for(int iter=0; iter<allCampaignsMap.get(run).size(); iter++) {
				lines.add(printCampaign(data, iter+1, allCampaignsMap.get(run).get(iter)));
				count++;
				if(count>=limit) break;
			}
			
			// Write to file
			String fileName = "Run"+String.valueOf(run+1)+"_AllCampaigns.csv";
			File campaignFile = new File(resultsDirectory, fileName);
			FileUtils.writeLines(campaignFile, lines);
		}
	}
	
	/**
	 * Prints out details for the top campaigns based on pareto rank
	 * Each campaign lists:
	 *		iteration or rank, % scenarios detected, average TTD, range of TTD, scenarios not detected, 
	 * 		number of wells, cost of campaign, volume of aquifer degraded, and a list of sensors
	 * @param data
	 * @param limit (int)
	 * @throws IOException
	 */
	public static void printRankedCampaigns(STORMData data, int limit) throws IOException {
		// Create the header for the file
		List<String> lines = new ArrayList<String>();
		lines.add("Pareto Rank,"
				+ "Scenarios with Leak Detected,"
				+ "Scenarios with No Leak Detected,"
				+ "Average TTD of Successful Scenarios,"
				+ (data.getObjectives().contains(OBJECTIVE.VAD_AT_DETECTION) ? "Average VAD at TTD," : "")
				+ (data.getObjectives().contains(OBJECTIVE.COST) ? "Average Cost of Successful Scenarios," : "")
				+ "Number of Wells,"
				+ "Sensor Types (x y z)");
		
		// Loop over pareto-ranked campaigns
		int count = 0;
		if(limit==0) limit = Integer.MAX_VALUE; //Entering 0 for limit means no limit
		for(int rank=0; rank<rankedCampaigns.size(); rank++) {
			for(int num=0; num<rankedCampaigns.get(rank).size(); num++) {
				lines.add(printCampaign(data, rank+1, rankedCampaigns.get(rank).get(num)));
				count++;
				if(count>=limit) break;
			}
			// Don't include the last rank, that is just carryover
			if(count>=limit || rank==10) break;
		}
		
		// Write to file
		String fileName = "Top"+(limit==Integer.MAX_VALUE ? "" : limit)+"Campaigns.csv";
		File campaignFile = new File(resultsDirectory, fileName);
		FileUtils.writeLines(campaignFile, lines);
	}
	
	/**
	 * Pass in a campaign and return a string with the following values:
	 *		iteration or rank, % scenarios detected, average TTD, range of TTD, scenarios not detected, 
	 * 		number of wells, cost of campaign, volume of aquifer degraded, and a list of sensors
	 * @param data
	 * @param number (iteration or pareto rank)
	 * @param campaign
	 * @return String
	 */
	private static String printCampaign(STORMData data, int number, Campaign campaign) {
		// Scenarios with leak detected
		ArrayList<Scenario> detectingScenarios = campaign.getDetectingScenarios();
		// Scenarios with no leak detected
		ArrayList<Scenario> nonDetectingScenarios = campaign.getNonDetectingScenarios();

		///////////////////////////////////////////////////
		//// Write out a single line for this campaign ////
		///////////////////////////////////////////////////
		StringBuilder line = new StringBuilder();
		// Iteration
		line.append(number+",");
		// Detecting Scenarios
		float percent = campaign.getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, false, null);
		if(detectingScenarios.size()==0)
			line.append("None");
		else {
			line.append(Constants.percentageFormat.format(percent)+"%:");
			for(Scenario scenario : detectingScenarios)
				line.append(" "+scenario.toString());
		}
		// Non-detecting Scenarios
		percent = 100 - percent;
		if(nonDetectingScenarios.size()==0)
			line.append(",None");
		else {
			line.append(","+Constants.percentageFormat.format(percent)+"%:");
			for(Scenario scenario : nonDetectingScenarios)
				line.append(" "+scenario.toString());
		}
		// Average TTD of successful scenarios
		float averageTTD = campaign.getObjectiveValue(OBJECTIVE.TTD, false, null);
		// Average VAD at TTD
		float averageVAD = campaign.getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, false, null);
		if(averageTTD==Float.MAX_VALUE) { //For non-detection
			// Average TTD of successful scenarios
			line.append(",No detection,");
			// Average VAD at TTD of successful scenarios
			line.append("No detection,");
		} else {
			// Average TTD of successful scenarios
			line.append(","+Constants.decimalFormat.format(averageTTD)+" "+data.getSet().getNodeStructure().getUnit("times")+",");
			// Average VAD at TTD of successful scenarios
			line.append((data.getObjectives().contains(OBJECTIVE.VAD_AT_DETECTION) ? Constants.decimalFormat.format(averageVAD)+" "+data.getSet().getNodeStructure().getUnit("x")+"^3," : ""));
		}
		// Average cost
		float averageCost = campaign.getObjectiveValue(OBJECTIVE.COST, false, null);
		line.append((data.getObjectives().contains(OBJECTIVE.COST) ? data.getSet().getCostUnit()+Constants.decimalFormatForCost.format(averageCost)+"," : ""));
		// Number of wells
		int numberOfWells = campaign.getVerticalWells().size();
		line.append(numberOfWells);
		// List the sensors
		for(Sensor sensor: campaign.getSensors())
			line.append(","+sensor.getFullSummary());
		
		return line.toString();
	}
	
	public static ArrayList<ArrayList<Campaign>> getAllCampaigns() {
		return allCampaignsMap;
	}
	
	public static ArrayList<ArrayList<Campaign>> getRankedCampaigns() {
		return rankedCampaigns;
	}
	
}
