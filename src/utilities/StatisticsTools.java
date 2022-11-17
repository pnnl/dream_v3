package utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import results.ResultPrinter;
import functions.ObjectiveSelection.OBJECTIVE;
import objects.Campaign;
import objects.ScenarioSet;
import wizardPages.DREAMWizard.STORMData;

public class StatisticsTools {
	
	/**
	 * At the end of the algorithm, pareto rank campaigns based on selected objectives
	 * We also sort campaigns within each rank and remove duplicates
	 */
	public static void calculateParetoRanks(ArrayList<OBJECTIVE> objectives, ScenarioSet set) {
		if(ResultPrinter.allCampaignsMap==null) return;
		ResultPrinter.rankedCampaigns.clear();
		System.out.println("Calculating pareto ranks based on selected objectives: "+objectives);
		
		// Start by storing campaigns from all runs in a flat map
		ArrayList<Campaign> campaigns = new ArrayList<Campaign>();
		for(int run=0; run<ResultPrinter.allCampaignsMap.size(); run++) {
			for(int itt=0; itt<ResultPrinter.allCampaignsMap.get(run).size(); itt++) {
				campaigns.add(ResultPrinter.allCampaignsMap.get(run).get(itt));
			}
		}
		// Initially, all models start at rank 0
		int currentRank = 0;
		while (true) {
			long startTime = System.currentTimeMillis();
			ArrayList<Campaign> nextRank = new ArrayList<Campaign>();
			ArrayList<Integer> skipCampaigns = new ArrayList<Integer>();
			if(currentRank < 10) {
				System.out.print("computing rank "+String.valueOf(currentRank+1)+", "+campaigns.size()+" candidates are still being ranked...");
				for(int i=0; i<campaigns.size(); i++) {
					if(skipCampaigns.contains(i)) continue; //This campaign was already found to be worse, skip
					boolean topRank = true;
					for(int j=0; j<campaigns.size(); j++) {
						if(i>=j) continue; //Check that 1 and 2 are different
						if(skipCampaigns.contains(j)) continue; //This campaign was already found to be worse, skip
						// Compare the performance of both models for each objective
						int count = 0;
						for(OBJECTIVE objective : objectives) {
							float value1 = campaigns.get(i).getObjectiveValue(objective, Constants.weightObjectives, set.getScenarioWeights());
							float value2 = campaigns.get(j).getObjectiveValue(objective, Constants.weightObjectives, set.getScenarioWeights());
							if(value1>value2)
								count++;
						}
						// If model i has higher values (worse) for all objectives, model 1 needs to move to the next rank
						// Once we find one current-rank model that's better than model 1, we don't need to compare it to any others
						if(count==objectives.size()) {
							nextRank.add(campaigns.get(i));
							topRank = false;
							break;
						}
						// If model i has lower values (better) for all objectives, model j should move to the next rank
						// Knowing that model j is worse, we can skip it later
						else if(count==0) {
							nextRank.add(campaigns.get(j));
							skipCampaigns.add(j);
						}
					}
					// If we looked through all campaigns and didn't find anything better by all objectives, store the campaign
					if(topRank)
						ResultPrinter.saveRankedCampign(currentRank, campaigns.get(i));
				}
			}
			// If we compared all the current-rank models to each other and the next_rank list is still empty, we're done ranking
		    // Otherwise we need to bump all the losers to the next rank and restart the campaign loop
			if(nextRank.size()>0 && currentRank<10) {
				currentRank++;
				campaigns = nextRank;
				long time = (System.currentTimeMillis() - startTime) / 1000;
				System.out.println(Constants.formatSeconds(time));
			} else {
				System.out.println("The remaining "+campaigns.size()+" candidates were automatically added to Unranked");
				for(Campaign campaign : campaigns)
					ResultPrinter.saveRankedCampign(currentRank, campaign);
				break;
			}
		}
		
		// Post-processing, sort based on number of wells, number of sensors, and name
		for(int rank=0; rank<ResultPrinter.rankedCampaigns.size(); rank++) {
			Collections.sort(ResultPrinter.rankedCampaigns.get(rank), new Comparator<Campaign>() {
				public int compare(Campaign campaign1, Campaign campaign2) {
					Integer well1 = campaign1.getVerticalWells().size();
					Integer well2 = campaign2.getVerticalWells().size();
					if(well1 != well2)
						return well1.compareTo(well2);
					Integer sensors1 = campaign1.getSensors().size();
					Integer sensors2 = campaign2.getSensors().size();
					if(sensors1 != sensors2)
						return sensors1.compareTo(sensors2);
					String name1 = campaign1.getSummary(set.getNodeStructure());
					String name2 = campaign2.getSummary(set.getNodeStructure());
					return name1.compareTo(name2);
				}
			});
		}
	}
	
	/**
	 * At the end of the algorithm, pareto rank campaigns based on selected objectives
	 * We also sort campaigns within each rank and remove duplicates
	 */
	public static void calculatePCA(STORMData data) {
		if(ResultPrinter.allCampaignsMap==null) return;
		System.out.println("Calculating PCA values based on selected objectives: "+data.getObjectives());
		// Start by storing campaigns from all runs in a flat map
		ArrayList<Campaign> campaigns = new ArrayList<Campaign>();
		for(int run=0; run<ResultPrinter.allCampaignsMap.size(); run++) {
			for(int itt=0; itt<ResultPrinter.allCampaignsMap.get(run).size(); itt++) {
				campaigns.add(ResultPrinter.allCampaignsMap.get(run).get(itt));
			}
		}
	
		// retrieve set of objective values as a matrix
		float[][] x = new float[campaigns.size()][data.getObjectives().size()];
		for(int i=0; i<campaigns.size(); i++) {
			int j = 0;
			for(OBJECTIVE objective : data.getObjectives()) {
				x[i][j] = campaigns.get(i).getObjectiveValue(objective, Constants.weightObjectives, data.getSet().getScenarioWeights());
			}
			j = j+1;
		}
	}
}
