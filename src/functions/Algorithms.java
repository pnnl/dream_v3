package functions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

//import com.google.common.collect.*;

import org.eclipse.core.runtime.IProgressMonitor;

import functions.ObjectiveSelection.OBJECTIVE;
import objects.Campaign;
import objects.Sensor;
import objects.SensorSetting;
import objects.TimeStep;
import objects.Scenario;
import objects.PointSensor;
import objects.Surface;
import results.ObjectiveGraph;
import results.ResultPrinter;
import utilities.Constants;
import utilities.Point3i;
import visualization.DomainVisualization;
import wizardPages.DREAMWizard.STORMData;

/**
 * Any new functions should extend this class, that will allow pluggable pieces.
 * @author port091
 * @author whit162
 */

public class Algorithms implements AlgorthmSelection {
	protected Integer currentRun;
	protected Integer iteration; 
	protected ALGORITHM algorithm;

	private IProgressMonitor monitor; // So we can update the status
	private ObjectiveGraph theGraph; // Scatterplot representation of the run
	private DomainVisualization viewer; // Graphical representation of the run
	//private Objectives myObjectives = new Objectives();
	//private HashMap<Scenario, ObjectiveValues> objectives = new HashMap<Scenario, ObjectiveValues>();
	//private ObjectiveValues objectives; // objectives for each campaign
	
	
	/**
	 * Based on the user selection, chooses an algorithm to run
	 * @param set
	 * @throws IOException 
	 */
	public boolean runInternal(STORMData data, int run) throws IOException {
		currentRun = run;
		algorithm = data.getAlgorithm();
		//if(algorithm.equals(ALGORITHM.GRID_SEARCH))
			//return gridSearch(data);
		if(algorithm.equals(ALGORITHM.MONTE_CARLO))
			return monteCarlo(data);
		else if(algorithm.equals(ALGORITHM.SIMULATED_ANNEALING))
			return simuatedAnnealing(data);
		else if(algorithm.equals(ALGORITHM.HEURISTIC))
			return heuristic(data);
		else if(algorithm.equals(ALGORITHM.NSGAII))
			return nsgaii(data);
		return false;
	}
	
	/*private boolean gridSearch(STORMData data) throws IOException {
		System.out.println("Start Grid Search simulation with "+data.getSet().getIterations()+" iterations");
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter("Node Numbers.txt"));
		// Loop through the user-designated number of iterations
		String sensorType = data.getSet().getDataTypes().get(0);
		Campaign thisCampaign = new Campaign(data.getSet(), outputWriter); //create random campaign
		thisCampaign.getSensors().clear();
		List<Integer> detectableNodes = data.getSet().getDetectableNodesWithConstraints(sensorType, thisCampaign);
		HashSet<Integer> nodes = new HashSet<Integer>(detectableNodes);

		Set nodeCombs = Sets.combinations(nodes,1);
		List<Set> combs = new ArrayList<Set>(nodeCombs);
		for(iteration=0; iteration<combs.size(); iteration++) {
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			// Create a random campaigns, calculate objectives, save results
			Campaign campaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			campaign.getSensors().clear();
			for (int node:new ArrayList<Integer>(combs.get(iteration))) {
				campaign.getSensors().add(new PointSensor(detectableNodes, sensorType, data.getSet()));
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().clear();
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().add(0,node);
			}
			for (Sensor sensor:campaign.getSensors()) {
				sensor.getTimes().set(0, data.getSet().getNodeStructure().getTimeAt(0));
			}
			saveCampaign(campaign, data); //calculate objectives and save campaign
		}

		nodeCombs = Sets.combinations(nodes,2);
	    combs = new ArrayList<Set>(nodeCombs);
		for(iteration=0; iteration<combs.size(); iteration++) {
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			// Create a random campaigns, calculate objectives, save results
			Campaign campaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			campaign.getSensors().clear();
			for (int node:new ArrayList<Integer>(combs.get(iteration))) {
				campaign.getSensors().add(new PointSensor(detectableNodes, sensorType, data.getSet()));
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().clear();
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().add(0,node);
			}
			for (Sensor sensor:campaign.getSensors()) {
				sensor.getTimes().set(0, data.getSet().getNodeStructure().getTimeAt(0));
			}
			saveCampaign(campaign, data); //calculate objectives and save campaign
		}

		nodeCombs = Sets.combinations(nodes,3);
	    combs = new ArrayList<Set>(nodeCombs);
		for(iteration=0; iteration<combs.size(); iteration++) {
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			// Create a random campaigns, calculate objectives, save results
			Campaign campaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			campaign.getSensors().clear();
			for (int node:new ArrayList<Integer>(combs.get(iteration))) {
				campaign.getSensors().add(new PointSensor(detectableNodes, sensorType, data.getSet()));
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().clear();
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().add(0,node);
			}
			for (Sensor sensor:campaign.getSensors()) {
				sensor.getTimes().set(0, data.getSet().getNodeStructure().getTimeAt(0));
			}
			saveCampaign(campaign, data); //calculate objectives and save campaign
		}

		nodeCombs = Sets.combinations(nodes,4);
	    combs = new ArrayList<Set>(nodeCombs);
		for(iteration=0; iteration<combs.size(); iteration++) {
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			// Create a random campaigns, calculate objectives, save results
			Campaign campaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			campaign.getSensors().clear();
			for (int node:new ArrayList<Integer>(combs.get(iteration))) {
				campaign.getSensors().add(new PointSensor(detectableNodes, sensorType, data.getSet()));
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().clear();
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().add(0,node);
			}
			for (Sensor sensor:campaign.getSensors()) {
				sensor.getTimes().set(0, data.getSet().getNodeStructure().getTimeAt(0));
			}
			saveCampaign(campaign, data); //calculate objectives and save campaign
		}

		nodeCombs = Sets.combinations(nodes,5);
	    combs = new ArrayList<Set>(nodeCombs);
		for(iteration=0; iteration<combs.size(); iteration++) {
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			// Create a random campaigns, calculate objectives, save results
			Campaign campaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			campaign.getSensors().clear();
			for (int node:new ArrayList<Integer>(combs.get(iteration))) {
				campaign.getSensors().add(new PointSensor(detectableNodes, sensorType, data.getSet()));
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().clear();
				campaign.getSensors().get(campaign.getSensors().size()-1).getLocations().add(0,node);
			}
			for (Sensor sensor:campaign.getSensors()) {
				sensor.getTimes().set(0, data.getSet().getNodeStructure().getTimeAt(0));
			}
			saveCampaign(campaign, data); //calculate objectives and save campaign
		}

		outputWriter.flush();
		outputWriter.close();
		
		iteration = null;
		return false;
	}*/

	
	/**
	 * Monte Carlo makes completely random campaigns at each iteration, no spin up required.
	 * Random campaign are subject to user constraints.
	 * @throws IOException 
	 */
	private boolean monteCarlo(STORMData data) throws IOException {
		System.out.println("Start Monte Carlo simulation with "+data.getSet().getIterations()+" iterations");
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter("Node Numbers.txt"));
		// Loop through the user-designated number of iterations
		for(iteration=0; iteration<data.getSet().getIterations(); iteration++) {
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			System.out.println("--== Iteration "+Constants.decimalFormat.format(iteration+1)+" ==--");

			// Create a random campaigns, calculate objectives, save results
			Campaign campaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			saveCampaign(campaign, data); //calculate objectives and save campaign
			System.out.println("New Campaign: "+campaign.printWeightedObjectiveSummary(data));
		}
		outputWriter.flush();
		outputWriter.close();
		
		iteration = null;
		return false;
	}
	
	/**
	 * Simulated Annealing requires a brief spin-up and mutates based on the last campaign.
	 * Campaigns are scored based on the selected objective. A decreasing temperature 
	 * function determines whether the next mutation is based on the better or worse campaign.
	 * @throws IOException 
	 */
	private boolean simuatedAnnealing(STORMData data) throws IOException {
		System.out.println("Start Simulated Annealing simulation with "+Constants.decimalFormat.format(data.getSet().getIterations())+" iterations");
		iteration = 0;
		monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
		System.out.println("--== Iteration 1 ==--");
		BufferedWriter outputWriter = null;
		outputWriter = new BufferedWriter(new FileWriter("Node Numbers.txt"));
		// Spin-up: create a random campaign, calculate objectives, save results
		Campaign currentCampaign = new Campaign(data.getSet(), outputWriter); //create random campaign
		saveCampaign(currentCampaign, data); //calculate objectives and save campaign
		
		System.out.println("Current Campaign: "+currentCampaign.printWeightedObjectiveSummary(data));
		
		// Loop through the user-designated number of iterations
		for(iteration=1; iteration<data.getSet().getIterations(); iteration++) {
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			System.out.println("--== Iteration "+Constants.decimalFormat.format(iteration+1)+" ==--");
			
			// Clone the last campaign, apply random mutation, calculate objectives, save results
			Campaign newCampaign = new Campaign(data.getSet(), currentCampaign); //clone campaign
			newCampaign.randomMutation(data.getSet(), outputWriter); //mutate campaign
			saveCampaign(newCampaign, data); // calculate objectives and save campaign

			System.out.println("Current Campaign: "+currentCampaign.printWeightedObjectiveSummary(data));
			System.out.println("New Campaign:     "+newCampaign.printWeightedObjectiveSummary(data));
			
			Float ttd0 = currentCampaign.getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float cost0 = currentCampaign.getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float vad0 = currentCampaign.getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
			//Float per0 = currentCampaign.getWeightedObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, data.getSet().getScenarioWeights());
			
			Float ttd1 = newCampaign.getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float cost1 = newCampaign.getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float vad1 = newCampaign.getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
			//Float per1 = newCampaign.getWeightedObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, data.getSet().getScenarioWeights());

			float p2  = (1-20*((ttd1-ttd0)/ttd0));
			float p3a = (1-50*((cost1-cost0)/cost0));
			float p3b = (1-50*((vad1-vad0)/vad0));
			
			boolean acc1 = ttd1<=ttd0 && cost1<=cost0 && vad1<=vad0; 
			boolean acc2 = ttd1>ttd0 && Constants.random.nextFloat()<p2; 
			boolean acc3 = ttd1<=ttd0 && ((Constants.random.nextFloat()<p3a) || (Constants.random.nextFloat()<p3b)); 

			boolean rej1 = ttd1>ttd0 && cost1>cost0 && vad1>vad0; 

			boolean acc  = (!rej1) && (acc1 || acc2 || acc3);
			
			//System.out.println( acc );
			//System.out.println( String.format("%b %b %b %b %b",acc1,acc2,acc3,rej1,acc) );
			
			// If new campaign is worse than current, compare with temperature to decide whether to swap
			if(!acc) {
				double temperature = Constants.evaluateCoolingExpression(data.getCoolingEq(), iteration, data.getSet().getIterations());
				double randomValue = Constants.random.nextDouble(); //seeded random number for consistent results
				if (temperature > randomValue) {
					currentCampaign = new Campaign(data.getSet(), newCampaign);
					System.out.println("New campaign was worse, but kept anyway.");
				} else {
					System.out.println("New campaign was worse and was not kept.");
				}
			} else {
				currentCampaign = new Campaign(data.getSet(), newCampaign);
				System.out.println("New campaign was better and was kept.");
			}
		}
		outputWriter.flush();
		outputWriter.close();
		
		iteration = null;
		return false;
	}

	private boolean heuristic(STORMData data) throws IOException {
		System.out.println("Start Heuristic simulation with "+Constants.decimalFormat.format(data.getSet().getIterations())+" iterations");
		iteration = 0;
		monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
		System.out.println("--== Iteration 1 ==--");
		BufferedWriter outputWriter = null;
		
		outputWriter = new BufferedWriter(new FileWriter("Node Numbers.txt"));
		// Spin-up: create a random campaign, calculate objectives, save results

		for(iteration=1; iteration<data.getSet().getIterations(); iteration++) {
			
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
			System.out.println("--== Iteration "+Constants.decimalFormat.format(iteration+1)+" ==--");

			Campaign currentCampaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			currentCampaign.getSensors().clear();
			// choose number of sensors, loop
			int nSensors = data.getSet().getMinSensors()+Constants.random.nextInt(data.getSet().getMaxSensors()-data.getSet().getMinSensors()+1);
			for (int iSensor=0 ; iSensor<nSensors ; iSensor++) {
				// choose sensor type
				int iSensorType = Constants.random.nextInt(data.getSet().getDataTypes().size());
				String sensorType = data.getSet().getDataTypes().get(iSensorType);
				// compute overlaps for sensor type
				List<Integer> detectableNodes = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign);
				List<Double> overlaps = compute_overlaps(data,currentCampaign,sensorType,detectableNodes);
				// convert overlaps to cdf
				List<Double> cdf = compute_cdf(data,overlaps);
				// sample cdf
				Integer i = sample_cdf(data,cdf);
				if (i==-999) continue;
				int node = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign).get(i);
				if (sensorType.contains("gravity")) {
					boolean skip = false;
					detectableNodes = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign);
					Surface newSurface = new Surface(detectableNodes, sensorType, data.getSet());
					newSurface.getLocations().clear();
					int nx = data.getSet().getNodeStructure().getX().size();
					int ny = data.getSet().getNodeStructure().getY().size();
					int nz = data.getSet().getNodeStructure().getZ().size();
					List<Integer> newNodes = new ArrayList<Integer>();
					while (true) {
						detectableNodes = remove_includedNodes(data, newNodes, sensorType, detectableNodes);
						overlaps = compute_overlaps(data,currentCampaign,sensorType,detectableNodes);
						if(Collections.max(overlaps)==0) {
							skip=true;
							break;
						}
						cdf = compute_cdf(data,overlaps);
						i = sample_cdf(data,cdf);
						node = detectableNodes.get(i);
						if (newNodes.contains(node)) continue;
						newNodes.add(node);
						int iMin=+9999999;
						int iMax=-9999999;
						int jMin=+9999999;
						int jMax=-9999999;
						for (Integer thisNode:newNodes) {
							Point3i start = Constants.nodeNumberToIJK(thisNode, data.getSet().getNodeStructure().getIJKDimensions());
							if (start.getI()<iMin) iMin=start.getI();
							if (start.getJ()<jMin) jMin=start.getJ();
							if (start.getI()>iMax) iMax=start.getI();
							if (start.getJ()>jMax) jMax=start.getJ();
						}
						if ( ((1+iMax-iMin)*(1+jMax-jMin))>data.getSet().getMaxSurveyLoactions() ) {
							newNodes.clear();
						} else if ( ((1+iMax-iMin)*(1+jMax-jMin))>data.getSet().getMinSurveyLocations() ) {
							for (int ii=iMin ; ii<iMax ; ii++) {
								for (int jj=jMin ; jj<jMax ; jj++) {
									int newNode=Constants.ijkToNodeNumber(ii, jj, nz, nx, ny);
									newSurface.getLocations().add(newNode);
								}
							}
							break;
						}
					}
					if (skip) continue;

					newSurface.getTimes().clear();
					int nTimes = 1+Constants.random.nextInt(4);
					for ( int iTime=0 ; iTime<nTimes; iTime++ ) {
						List<TimeStep> times = data.getSet().getNodeStructure().getTimeSteps();
						int it = Constants.random.nextInt(times.size());
						newSurface.getTimes().add(iTime,data.getSet().getNodeStructure().getTimeAt(it));
					}
					currentCampaign.addSensor(newSurface);
				} else {
					detectableNodes = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign);
					PointSensor newSensor = new PointSensor(detectableNodes, sensorType, data.getSet());
					newSensor.getLocations().clear();
					newSensor.getLocations().add(node);
					//newSensor.getTimes().clear();
					//List<TimeStep> times = data.getSet().getNodeStructure().getTimeSteps();
					//int it = Constants.random.nextInt(times.size());
					//newSensor.getTimes().add(0,data.getSet().getNodeStructure().getTimeAt(it));
					currentCampaign.addSensor(newSensor);
				}
				currentCampaign.updateVerticalWellsPublic(data.getSet().getNodeStructure());
			}
			currentCampaign.updateVerticalWellsPublic(data.getSet().getNodeStructure());
			currentCampaign.calculateCostOfCampaign(data.getSet());
			currentCampaign.calculateObjectives(data); //calculate objectives
			saveCampaign(currentCampaign, data); // calculate objectives and save campaign
		}

		outputWriter.flush();
		outputWriter.close();
		
		iteration = null;
		return false;
	}

	private boolean nsgaii(STORMData data) throws IOException {
		System.out.println("Start Heuristic simulation with "+Constants.decimalFormat.format(data.getSet().getIterations())+" iterations");
		iteration = 0;
		monitor.subTask("iteration " + Constants.decimalFormat.format(iteration+1));
		System.out.println("--== Iteration 1 ==--");
		BufferedWriter outputWriter = null;
		
		Constants.random.nextInt(12345);
		
		outputWriter = new BufferedWriter(new FileWriter("Node Numbers.txt"));
		// Spin-up: create a random campaign, calculate objectives, save results
		List<Campaign> campaigns = new ArrayList<Campaign>();
		
		for(iteration=1; iteration<=data.getSet().getIterations(); iteration++) {
			
			if(monitor.isCanceled()) return true; //ends if user cancels
			monitor.subTask("iteration " + Constants.decimalFormat.format(iteration));
			System.out.println("--== Iteration "+Constants.decimalFormat.format(iteration)+" ==--");

			Campaign currentCampaign = new Campaign(data.getSet(), outputWriter); //create random campaign
			currentCampaign.getSensors().clear();
			// choose number of sensors, loop
			int nSensors = 1+Constants.random.nextInt(data.getSet().getMaxSensors());
			for (int iSensor=0 ; iSensor<nSensors ; iSensor++) {
				// choose sensor type
				int iSensorType = Constants.random.nextInt(data.getSet().getDataTypes().size());
				String sensorType = data.getSet().getDataTypes().get(iSensorType);
				// compute overlaps for sensor type
				List<Integer> detectableNodes = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign);
				List<Double> overlaps = compute_overlaps(data,currentCampaign,sensorType,detectableNodes);
				// convert overlaps to cdf
				List<Double> cdf = compute_cdf(data,overlaps);
				// sample cdf
				Integer i = sample_cdf(data,cdf);
				if (i==-999) continue;
				int node = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign).get(i);
				if (sensorType.contains("gravity")) {
					boolean skip = false;
					detectableNodes = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign);
					Surface newSurface = new Surface(detectableNodes, sensorType, data.getSet());
					newSurface.getLocations().clear();
					int nx = data.getSet().getNodeStructure().getX().size();
					int ny = data.getSet().getNodeStructure().getY().size();
					int nz = data.getSet().getNodeStructure().getZ().size();
					List<Integer> newNodes = new ArrayList<Integer>();
					while (true) {
						detectableNodes = remove_includedNodes(data, newNodes, sensorType, detectableNodes);
						overlaps = compute_overlaps(data,currentCampaign,sensorType,detectableNodes);
						if(Collections.max(overlaps)==0) {
							skip=true;
							break;
						}
						cdf = compute_cdf(data,overlaps);
						i = sample_cdf(data,cdf);
						node = detectableNodes.get(i);
						if (newNodes.contains(node)) continue;
						newNodes.add(node);
						int iMin=+9999999;
						int iMax=-9999999;
						int jMin=+9999999;
						int jMax=-9999999;
						for (Integer thisNode:newNodes) {
							Point3i start = Constants.nodeNumberToIJK(thisNode, data.getSet().getNodeStructure().getIJKDimensions());
							if (start.getI()<iMin) iMin=start.getI();
							if (start.getJ()<jMin) jMin=start.getJ();
							if (start.getI()>iMax) iMax=start.getI();
							if (start.getJ()>jMax) jMax=start.getJ();
						}
						if ( ((1+iMax-iMin)*(1+jMax-jMin))>data.getSet().getMaxSurveyLoactions() ) {
							newNodes.clear();
						} else if ( ((1+iMax-iMin)*(1+jMax-jMin))>data.getSet().getMinSurveyLocations() ) {
							for (int ii=iMin ; ii<iMax ; ii++) {
								for (int jj=jMin ; jj<jMax ; jj++) {
									int newNode=Constants.ijkToNodeNumber(ii, jj, nz, nx, ny);
									newSurface.getLocations().add(newNode);
								}
							}
							break;
						}
					}
					if (skip) continue;

					newSurface.getTimes().clear();
					int nTimes = 1+Constants.random.nextInt(4);
					for ( int iTime=0 ; iTime<nTimes; iTime++ ) {
						List<TimeStep> times = data.getSet().getNodeStructure().getTimeSteps();
						int it = Constants.random.nextInt(times.size());
						newSurface.getTimes().add(iTime,data.getSet().getNodeStructure().getTimeAt(it));
					}
					currentCampaign.addSensor(newSurface);
				} else {
					detectableNodes = data.getSet().getDetectableNodesWithConstraints(sensorType, currentCampaign);
					PointSensor newSensor = new PointSensor(detectableNodes, sensorType, data.getSet());
					newSensor.getLocations().clear();
					newSensor.getLocations().add(node);
					currentCampaign.addSensor(newSensor);
				}
				currentCampaign.updateVerticalWellsPublic(data.getSet().getNodeStructure());
			}
			currentCampaign.updateVerticalWellsPublic(data.getSet().getNodeStructure());
			currentCampaign.calculateCostOfCampaign(data.getSet());
			currentCampaign.calculateObjectives(data); //calculate objectives
			saveCampaign(currentCampaign, data); // calculate objectives and save campaign
			campaigns.add(currentCampaign);
		}
		
		int nGen = 50;
		int nPop = 300;
		int nTournament = 10;
		
		//System.out.println("campaigns "+campaigns.size());
		for(int generation=1; generation<nGen; generation++) {
			System.out.println("campaign.size "+campaigns.size());
			campaigns = removeDuplicates_objectiveSpace(data,campaigns);
			System.out.println("campaign.size "+campaigns.size());
			System.out.println("generation "+generation);
			HashSet<Campaign> population = new HashSet<Campaign>();
			//population.addAll(n_best(data, campaigns, 0, 5));
			//population.addAll(n_best(data, campaigns, 1, 5));
			//population.addAll(n_best(data, campaigns, 2, 5));
			//population.addAll(n_best(data, campaigns, 3, 5));
			List<Campaign> remaining = new ArrayList<Campaign>(campaigns);
			while (population.size()<nPop) {
				System.out.println("... computing Pareto ranks");
				Map<Integer,List<Campaign>> output = peel_one_rank(data, remaining);
				System.out.println(output.get(1).size()+" "+output.get(2).size());
				remaining = output.get(2);
				if ( (population.size()+output.get(1).size())<=nPop ) {
					population.addAll( output.get(1) );
				} else {
					System.out.println("... computing crowding distances");
					population.addAll( select_by_crowding_distance(data,output.get(1),nPop-population.size()) );
				}
				break;
			}
			System.out.println(population.size());
			for (Campaign campaign:population) System.out.print( campaign.getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights())+" " );
			System.out.println();
			for (Campaign campaign:population) System.out.print( campaign.getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights())+" " );
			System.out.println();
			System.out.println("... generating new campaigns");
			List<Campaign> popList = new ArrayList<Campaign>(population);
			List<Campaign> thisGeneration = new ArrayList<Campaign>();
			for (int iPop=0 ; iPop<nPop ; iPop++) {
				System.out.println("...generation "+generation+", pop "+iPop);
				HashSet<Campaign> tournament = new HashSet<Campaign>();
				while (tournament.size()<nTournament) {
					//System.out.println("Help I'm trapped in while loop 1");
					tournament.add( popList.get(Constants.random.nextInt(popList.size())) );
				}
				List<Campaign> tournamentList = new ArrayList<>(tournament);
				Campaign iCampaign = select_by_crowding_distance(data,tournamentList,1).get(0);
				Campaign jCampaign = select_by_crowding_distance(data,tournamentList,1).get(0);
				while (iCampaign==jCampaign) {
					//System.out.println("Help I'm trapped in while loop 2");
					iCampaign = select_by_crowding_distance(data,tournamentList,1).get(0);
					jCampaign = select_by_crowding_distance(data,tournamentList,1).get(0);
					//System.out.println(tournamentList.size()+" "+iCampaign.hashCode()+" "+jCampaign.hashCode());
				}
				Campaign newCampaign = new Campaign(data.getSet(), outputWriter); //create random campaign
				newCampaign.getSensors().clear();
				if (Constants.random.nextDouble()<1) {
					List<Sensor> tempSensors = new ArrayList<Sensor>();
					for (Sensor sensor:iCampaign.getSensors()) tempSensors.add(sensor);
					for (Sensor sensor:jCampaign.getSensors()) tempSensors.add(sensor);
					Collections.shuffle(tempSensors);
					int nSensors = data.getSet().getMinSensors()+Constants.random.nextInt(data.getSet().getMaxSensors()-data.getSet().getMinSensors());
					if (nSensors>tempSensors.size()) nSensors=tempSensors.size();
					for (int iSensor=0 ; iSensor<nSensors; iSensor++ ) {
						newCampaign.addSensor(tempSensors.get(iSensor));
					}
				} else {
					List<Sensor> tempSensors = new ArrayList<Sensor>();
					for (Sensor sensor:iCampaign.getSensors()) tempSensors.add(sensor);
					Collections.shuffle(tempSensors);
					int nSensors = data.getSet().getMinSensors()+Constants.random.nextInt(data.getSet().getMaxSensors()-data.getSet().getMinSensors());
					if (nSensors>tempSensors.size()) nSensors=tempSensors.size();
					for (int iSensor=0 ; iSensor<nSensors; iSensor++ ) {
						newCampaign.addSensor(tempSensors.get(iSensor));
					}
				}
				newCampaign.randomMutationWeights(data.getSet(), outputWriter,0,0,1); //mutate campaign
				newCampaign.updateVerticalWellsPublic(data.getSet().getNodeStructure());
				newCampaign.calculateCostOfCampaign(data.getSet());
				newCampaign.calculateObjectives(data); //calculate objectives
				saveCampaign(newCampaign, data); // calculate objectives and save campaign
				thisGeneration.add(newCampaign);
			}
			campaigns = new ArrayList<Campaign>();
			campaigns.addAll(population);
			campaigns.addAll(thisGeneration);
			//System.out.println(campaigns.size());
			//for (Campaign campaign:campaigns) System.out.print( campaign.getWeightedObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, data.getSet().getScenarioWeights())+" " );
			//System.out.println();
			//for (Campaign campaign:campaigns) System.out.print( campaign.getWeightedObjectiveValue(OBJECTIVE.TTD, data.getSet().getScenarioWeights())+" " );
			//System.out.println();
		}

		outputWriter.flush();
		outputWriter.close();
		
		iteration = null;
		return false;
	}
	
	private List<Campaign> removeDuplicates_objectiveSpace(STORMData data, List<Campaign> campaigns) {
		Set<Integer> duplicates = new HashSet<Integer>();
		for (int i=0; i<campaigns.size(); i++) {
			Float ttdi  = campaigns.get(i).getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
			//Float costi = campaigns.get(i).getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
			//Float vadi  = campaigns.get(i).getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float perci = campaigns.get(i).getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights());
			for (int j=0; j<campaigns.size(); j++) {
				Float ttdj  = campaigns.get(j).getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
				//Float costj = campaigns.get(j).getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
				//Float vadj  = campaigns.get(j).getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
				Float percj = campaigns.get(j).getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights());
				//if (ttdi==ttdj & costi==costj & vadi==vadj & perci==percj) {
				if (ttdi==ttdj & perci==percj) {
					if (!duplicates.contains(i) & !duplicates.contains(j)) { 
						if (Constants.random.nextBoolean()) {
							duplicates.add(i);
						} else {
							duplicates.add(j);
						}
					}
				}
			}
		}
		List<Campaign> campaigns_unique = new ArrayList<Campaign>();
		for (int i=0; i<campaigns.size(); i++) {
			if (!duplicates.contains(i)) campaigns_unique.add(campaigns.get(i));
		}
		return campaigns_unique;
	}

	/*private List<Campaign> n_best(STORMData data, List<Campaign> campaigns, Integer objType, Integer n) {
		List<Double> vals = new ArrayList<Double>();
		for (int i=0; i<campaigns.size(); i++) {
			if (objType==0) {
				vals.add( (double) campaigns.get(i).getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights()));
			} else if (objType==1) {
				vals.add( (double) campaigns.get(i).getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights()));
			} else if (objType==2) {
				vals.add( (double) campaigns.get(i).getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights()));
			} else if (objType==3) {
				vals.add( (double) campaigns.get(i).getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights()));
			}
		}
		List<Double> uVals = new ArrayList<>(vals);
		Collections.sort(vals);
		List<Campaign> best = new ArrayList<Campaign>();
		for (int i=0; i<campaigns.size(); i++) {
			if (uVals.get(i)<=vals.get(n)) {
				best.add(campaigns.get(i));
			}
		}
		return best;
	}*/

	private Map<Integer,List<Campaign>> peel_one_rank(STORMData data, List<Campaign> campaigns) {
		Set<Campaign> nextRank = new HashSet<Campaign>();
		for (int i=0; i<campaigns.size(); i++) {
			Float ttdi  = campaigns.get(i).getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
			//Float costi = campaigns.get(i).getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
			//Float vadi  = campaigns.get(i).getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float perci = campaigns.get(i).getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights());
			for (int j=0; j<campaigns.size(); j++) {
				Float ttdj  = campaigns.get(j).getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
				//Float costj = campaigns.get(j).getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
				//Float vadj  = campaigns.get(j).getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
				Float percj = campaigns.get(j).getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights());
				//if ( ((ttdj<=ttdi) & (costj<=costi) & (vadj<=vadi) & (percj>=perci)) & ((ttdj<ttdi) | (costj<costi) | (vadj<vadi) | (percj>perci)) ) {
				if ( ((ttdj<=ttdi) & (percj>=perci)) & ((ttdj<ttdi) | (percj>perci)) ) {
					nextRank.add(campaigns.get(i));
					break;
				}
			}
		}
		Set<Campaign> thisRank = new HashSet<Campaign>();
		for (Campaign iCampaign:campaigns) {
			if (!nextRank.contains(iCampaign)) {
				thisRank.add(iCampaign);
			}
		}
		Map<Integer,List<Campaign>> outputs = new HashMap<Integer, List<Campaign>>();
		outputs.put(1,new ArrayList<>(thisRank));
		outputs.put(2,new ArrayList<>(nextRank));
		return outputs;
	}
	
	private List<Campaign> select_by_crowding_distance(STORMData data, List<Campaign> campaigns, int n) {
		Float ttdmin  = +Float.MAX_VALUE;
		Float ttdmax  = -Float.MAX_VALUE;
		Float costmin = +Float.MAX_VALUE;
		Float costmax = -Float.MAX_VALUE;
		Float vadmin  = +Float.MAX_VALUE;
		Float vadmax  = -Float.MAX_VALUE;
		Float percmin = +Float.MAX_VALUE;
		Float percmax = -Float.MAX_VALUE;
		for (Campaign campaign:campaigns) {
			Float ttdi  = campaign.getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float costi = campaign.getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float vadi  = campaign.getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float perci = campaign.getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights());
			if (ttdi<ttdmin) ttdmin=ttdi;
			if (ttdi>ttdmax) ttdmax=ttdi;
			if (costi<costmin) costmin=costi;
			if (costi>costmax) costmax=costi;
			if (vadi<vadmin) vadmin=vadi;
			if (vadi>vadmax) vadmax=vadi;
			if (perci<percmin) percmin=perci;
			if (perci>percmax) percmax=perci;
		}
		List<Double> distance = new ArrayList<Double>();
		for (int i=0 ; i<campaigns.size() ; i++) {
			distance.add(Double.MAX_VALUE);
			Float ttdi  = campaigns.get(i).getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float costi = campaigns.get(i).getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float vadi  = campaigns.get(i).getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
			Float perci = campaigns.get(i).getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights());
			ttdi  = (ttdi-ttdmin)/(ttdmax-ttdmin);
			costi = (costi-costmin)/(costmax-costmin);
			vadi  = (vadi-vadmin)/(vadmax-vadmin);
			perci = (perci-percmin)/(percmax-percmin);
			for (int j=0 ; j<campaigns.size() ; j++) {
				if (i==j) continue;
				Float ttdj  = campaigns.get(j).getObjectiveValue(OBJECTIVE.TTD, Constants.weightObjectives, data.getSet().getScenarioWeights());
				Float costj = campaigns.get(j).getObjectiveValue(OBJECTIVE.COST, Constants.weightObjectives, data.getSet().getScenarioWeights());
				Float vadj  = campaigns.get(j).getObjectiveValue(OBJECTIVE.VAD_AT_DETECTION, Constants.weightObjectives, data.getSet().getScenarioWeights());
				Float percj = campaigns.get(j).getObjectiveValue(OBJECTIVE.SCENARIOS_DETECTED, Constants.weightObjectives, data.getSet().getScenarioWeights());
				ttdj  = (ttdj-ttdmin)/(ttdmax-ttdmin);
				costj = (costj-costmin)/(costmax-costmin);
				vadj  = (vadj-vadmin)/(vadmax-vadmin);
				percj = (percj-percmin)/(percmax-percmin);
				Double d0 = (double) Math.abs(ttdj-ttdi)+Math.abs(costj-costi)+Math.abs(vadj-vadi)+Math.abs(percj-perci);
				if (d0<distance.get(i)) distance.set(i,d0);
			}
		}
		//System.out.println(distance);
		List<Double> cdf = compute_cdf(data,distance);
		List<Campaign> campaignsDC = new ArrayList<Campaign>();
		for (Campaign campaign:campaigns) {
			campaignsDC.add(campaign);
		}
		//System.out.println(cdf);
		HashSet<Campaign> output = new HashSet<Campaign>();
		//System.out.println("Help I'm trapped in while loop 3");
		//System.out.println(campaigns.size()+" "+cdf.size()+" "+output.size()+" "+n);
		//System.out.println(cdf);
		while (output.size()<n) {
			//int ii = sample_cdf(data, cdf);
			int ii = Constants.random.nextInt(cdf.size());
			output.add( campaignsDC.get(ii) );
			campaignsDC.remove(ii);
			cdf.remove(ii);
			Double cdfMin = Collections.min(cdf);
			Double cdfMax = Collections.max(cdf);
			for ( int i=0 ; i<cdf.size() ; i++) {
				cdf.set(i,(cdf.get(i)-cdfMin)/(cdfMax-cdfMin));
			}
		}
		return new ArrayList<Campaign>(output);
	}

	private List<Integer> remove_includedNodes(STORMData data, List<Integer> newNodes, String sensorType, List<Integer> detectableNodes ) {
		for (int node : newNodes) {
			for (int i=0; i<detectableNodes.size(); i++) {
				if ( detectableNodes.get(i)==node) {
					detectableNodes.remove(i);
				}
			}
		}
		return detectableNodes;
	}

	private List<Scenario> find_undetected(STORMData data, Campaign thisCampaign ) {
		List<Scenario> undetected = new ArrayList<Scenario>();
		for (Scenario scenario:data.getSet().getScenarios()) {
			Boolean detect = false;
			for (Sensor sensor:thisCampaign.getSensors()) {
				if (sensor.getTTD(data.getSet(), scenario)!=null) {
					detect = true;
					break;
				}
			}
			if (!detect) {
				undetected.add(scenario);
			}
		}
		return undetected;
	}
	
	private List<Double> compute_overlaps(STORMData data, Campaign thisCampaign, String sensorType, List<Integer> detectableNodes) {
		List<Double> overlaps = new ArrayList<Double>();
		for (Sensor sensor:thisCampaign.getSensors()) {
			if (sensorType==sensor.getSensorType()) {
				for (int node:sensor.getLocations()) {
					for (int i=0 ; i<detectableNodes.size() ; i++) {
						if (node==detectableNodes.get(i)) {
							detectableNodes.remove(i);
						}
					}
				}
			}
		}
		List<Scenario> undetected = find_undetected(data,thisCampaign);
		SensorSetting sensorSetting = data.getSet().getSensorSettings(sensorType);
		String specificType = sensorSetting.getSpecificType();
		for (int node:detectableNodes) {
			Double overlap = 0.0;
			for (Scenario scenario:undetected) {
				if (data.getSet().getDetectionMap().get(specificType).get(scenario)!=null) {
					if (data.getSet().getDetectionMap().get(specificType).get(scenario).get(node)!=null) {
						//overlap += data.getSet().getTotalVolumeDegraded(scenario);
						overlap += 1.0;
					}
				}
			}
			overlaps.add(overlap);
		}
		return overlaps;
	}

	private List<Double> compute_cdf(STORMData data, List<Double>overlaps) {
		List<Double> cdf = new ArrayList<Double>();
		for (double overlap:overlaps) cdf.add( (double) overlap);
		for (int i=0 ; i<cdf.size() ; i++) cdf.set(i, Math.pow(cdf.get(i),1));
		Double overlapSum = 0.0;
		for (int i=0 ; i<cdf.size() ; i++) overlapSum += cdf.get(i);
		for (int i=0 ; i<cdf.size() ; i++) cdf.set(i, cdf.get(i)/overlapSum);
		for (int i=1 ; i<cdf.size() ; i++) cdf.set(i, cdf.get(i)+cdf.get(i-1));
		return cdf;
	}
	
	private Integer sample_cdf(STORMData data, List<Double> cdf) {
		if (Collections.min(cdf)==Collections.max(cdf)) {
			return Constants.random.nextInt(cdf.size());
		}
		Double randD = Constants.random.nextDouble();
		for (Integer i=0 ; i<cdf.size() ; i++)
			if (cdf.get(i)>randD) return i;
		return -999;
	}


	
	/**					**\
	 * Helper Methods	 *
	\* 					 */
	
	public void setResultsDirectory(String resultsDirectory) {
		ResultPrinter.resultsDirectory = resultsDirectory;
	}
	
	public void setObjectiveGraph(ObjectiveGraph theGraph) {
		this.theGraph = theGraph;
	}

	public void setDomainViewer(DomainVisualization domainViewer) {
		this.viewer = domainViewer;
	}
	
	public void setMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}
	
	private void saveCampaign(Campaign campaign, STORMData data) {
		campaign.calculateObjectives(data); //calculate objectives
		ResultPrinter.saveCampaign(currentRun, campaign); //store campaign
		// Save the new campaign in the objective graph
		if (theGraph != null)
			theGraph.addCampaign(currentRun, campaign);
		// Save the new campaign in the viewer
		if(viewer != null)
			viewer.addSingleCampaign(campaign);
		// Add completed work to the monitor
		if(monitor != null)
			monitor.worked(1);
	}

}
