package hdf5Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;

import objects.NodeStructure;
import objects.Scenario;
import objects.ScenarioSet;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.SensorType;
import objects.SensorSetting.Trigger;
import objects.TimeStep;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5File;
import utilities.Constants;
import utilities.Point3i;

/**
 * Utility functions for use in reading and parsing hdf5 files to DREAM formats
 * @author port091
 * @author whit162
 */ 

public class HDF5Interface { 
	
	// Points to all the hdf5 files in the directory
	public static Map<Scenario, H5File> hdf5Files = new HashMap<Scenario, H5File>();
	// Stores global statistics - min, average, max
	public static Map<String, float[]> statistics = new HashMap<String, float[]>();
	
	// Read one file to extract the Node Structure information from H5 files
	public static NodeStructure readNodeStructureH5 (String input) {
		NodeStructure nodeStructure = null;
		statistics.clear();
		try {
			// Equivalent to opening to line 80 and 81 (Theoretically)
			H5File hdf5File  = new H5File(input, HDF5Constants.H5F_ACC_RDONLY);
			hdf5File.open();
			
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
			// Get the data group
			for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
				String name = root.getMemberList().get(rootIndex).getName();
				if(name.startsWith("data")) {
					HashMap<Point3i, Float> porosity = new HashMap<Point3i, Float>();
					HashMap<String, String> units = new HashMap<String, String>();
					String positive = "";
					List<TimeStep> times = new ArrayList<TimeStep>();
					List<Float> xValues = new ArrayList<Float>();
					List<Float> yValues = new ArrayList<Float>();
					List<Float> zValues = new ArrayList<Float>();
					List<Float> edgex = new ArrayList<Float>();
					List<Float> edgey = new ArrayList<Float>();
					List<Float> edgez = new ArrayList<Float>();
					for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
						Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
						int dataset_id = dataset.open();
						float[] temp =  (float[])dataset.read();
						if(dataset.getName().equals("times")) {
							for(int i=0; i<temp.length; i++)
								times.add(new TimeStep(i, temp[i], Math.round(temp[i])));
						}
						else if(dataset.getName().equals("porosity")) {
							long size = dataset.getDims()[0] * dataset.getDims()[1] * dataset.getDims()[2];
							temp = new float[(int)size];
							H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, temp);
							int counter = 0;
							for(int i=1; i<=dataset.getDims()[0]; i++) {
								for(int j=1; j<=dataset.getDims()[1]; j++) {
									for(int k=1; k<=dataset.getDims()[2]; k++) {
										porosity.put(new Point3i(i, j, k), temp[counter]);
										counter++;
									}
								}
							}
						}
						else if(dataset.getName().equals("x")) xValues = Constants.arrayToList(temp);
						else if(dataset.getName().equals("y")) yValues = Constants.arrayToList(temp);
						else if(dataset.getName().equals("z")) zValues = Constants.arrayToList(temp);
						else if(dataset.getName().equals("vertex-x")) edgex = Constants.arrayToList(temp);
						else if(dataset.getName().equals("vertex-y")) edgey = Constants.arrayToList(temp);
						else if(dataset.getName().equals("vertex-z")) edgez = Constants.arrayToList(temp);
						// Save attributes, including units for x, y, z and positive direction for z
						if(dataset.hasAttribute()) { //Right now we only list two attributes - units and positive
							if(extractAttribute(dataset, "unit")!=null)
								units.put(dataset.getName(), extractAttribute(dataset, "unit"));
							if(extractAttribute(dataset, "positive")!=null)
								positive = extractAttribute(dataset, "positive");
						}
						dataset.close(dataset_id);
					}
					nodeStructure = new NodeStructure(xValues, yValues, zValues, edgex, edgey, edgez, times, porosity, units, positive);
				} else if(name.startsWith("statistics")) {
					for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
						Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
						int dataset_id = dataset.open();
						float[] temp = (float[])dataset.read();
						if (statistics.containsKey(dataset.getName())) {
							temp[0] = Math.min(temp[0], statistics.get(dataset.getName())[0]);
							temp[1] = temp[1] + statistics.get(dataset.getName())[1]; //Sum averages now, divide by time step later
							temp[2] = Math.max(temp[2], statistics.get(dataset.getName())[2]);
						}
						statistics.put(dataset.getName(), temp);
						dataset.close(dataset_id);
					}
				// Starts with letters, ends with numbers, e.g. Plot1 or Time1
				} else if(Pattern.matches("^[A-Za-z]+.*[0-9]+$", name) && nodeStructure.getParameters().isEmpty()) {
					for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
						Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
						nodeStructure.addParameter(dataset.getName());
						// If we have units stored for parameters, we want to save them
						if(dataset.hasAttribute()) //Right now we only list one attribute - units
							nodeStructure.addUnit(dataset.getName(), extractAttribute(dataset, "unit"));
					}
				}
			}
			hdf5File.close();
		} catch (Exception e) {
			System.out.println("Error loading Node Struture from " + input);
			e.printStackTrace();
		}
		return nodeStructure;
	}
	
	
	public static List<Scenario> queryScenarioNamesFromFiles(String directory, String[] list) {
		List<Scenario> scenarios = new ArrayList<Scenario>();
		for(String fileName: list) {
			File file = new File(directory+File.separator+fileName);
			Scenario scenario = new Scenario(fileName.replaceAll("\\.h5" , ""));
			scenarios.add(scenario);
			H5File hdf5File = new H5File(file.getAbsolutePath(), HDF5Constants.H5F_ACC_RDONLY);
			hdf5Files.put(scenario, hdf5File);
		}
		Collections.sort(scenarios, Scenario.scenarioSort); //Sort the scenarios
		return scenarios;
	}
	
	// Create a "detection map" for the entered parameters: type, trigger, deltaType, value
	public static void createDetectionMap(IProgressMonitor monitor, ScenarioSet set, String parameter, Trigger trigger, DeltaType deltaType, float threshold, SensorType sensorType) {
		long startTime = System.currentTimeMillis();
		String specificType = Constants.getSpecificType(parameter, trigger, deltaType, threshold, sensorType);
		
		// Check if this detection map already exists
		if(set.getDetectionMap().containsKey(specificType)) {
			System.out.println("You already have a detection map for "+specificType+"! Sweet! Don't need to make another!");
			monitor.worked(hdf5Files.size());
			return;
		}
			
		// Create the detection map if it doesn't exist yet
		Map<Integer, Float> baseline = new HashMap<Integer, Float>(); //Stores values at the initial time
		for(H5File hdf5File: hdf5Files.values()) { // For every scenario
			Scenario scenario = set.getScenario(hdf5File.getName().replaceAll("\\.h5" , ""));
			if(monitor.isCanceled()) {
				set.getDetectionMap().remove(specificType);
				return;
			}
			monitor.subTask("creating detection map: " + parameter + " - " + scenario.toString());
			set.setDetectionMap(specificType, scenario);
			try {
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
					
					// Skip these
					if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
						continue;
					
					int timeIndex = Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", ""));
					
					// First time step sets the baseline
					if(timeIndex == 0) {
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(parameter)) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									baseline.put(Constants.indexToNodeNumber(index, set.getNodeStructure().getIJKDimensions()), dataRead[index]);
								}
							}
						}
					
					// When looping through other timesteps, compare with the baseline
					} else {
						float timestep = set.getNodeStructure().getTimeAt(timeIndex);
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(parameter)) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									int nodeNumber = Constants.indexToNodeNumber(index, set.getNodeStructure().getIJKDimensions());
									if(sensorType==SensorType.SURFACE) {
										Point3i ijk = Constants.indexToIJK(index, set.getNodeStructure().getIJKDimensions());
										//System.out.println(index+", "+nodeNumber+", "+ijk.toString());
										if(ijk.getK()!=set.getNodeStructure().getIJKDimensions().getK())
											continue;
									}
									// If the node triggers, save the TTD in detection map
									if(sensorTriggered(trigger, deltaType, threshold, dataRead[index], baseline.get(nodeNumber)))
										set.addToDetectionMap(specificType, scenario, nodeNumber, timestep);
								}
							}
						}
					}
				}
				hdf5File.close();
			} catch (Exception e) {
				System.out.println("Unable to read values from the hdf5 files...");
				e.printStackTrace();
			}
			monitor.worked(1);
		}
		
		long elapsedTime = (System.currentTimeMillis() - startTime)/1000;
		System.out.println("You just created a detection map for "+specificType+" in "+Constants.formatSeconds(elapsedTime)+"! Awesome! So Fast!");
	}
	
	/*public static void createDetectionMap(ScenarioSet set, SensorSetting setting, String specificType) { //TODO: Can we remove this? It's only used for the sandbox...
		
		long startTime = System.currentTimeMillis();
		
		Map<Integer, Float> baseline = new HashMap<Integer, Float>(); //Stores values at the initial time
		Point3i structure = set.getNodeStructure().getIJKDimensions();
		for(H5File hdf5File: hdf5Files.values()) { // For every scenario
			Scenario scenario = set.getScenario(hdf5File.getName().replaceAll("\\.h5" , ""));
			try {
				set.setDetectionMap(specificType, scenario);
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
					
					// Skip these
					if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
						continue;
					
					int timeIndex = Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", ""));
					
					// First time step sets the baseline
					if(timeIndex == 0) {
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(setting.getParameter())) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									baseline.put(Constants.indexToNodeNumber(index, structure), dataRead[index]);
								}
							}
						}
					
					// When looping through other timesteps, compare with the baseline
					} else {
						float timestep = set.getNodeStructure().getTimeAt(timeIndex);
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(setting.getParameter())) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									int nodeNumber = Constants.indexToNodeNumber(index, structure);
									// If the node triggers, save the TTD in detection map
									if(sensorTriggered(setting.getTrigger(), setting.getDeltaType(), setting.getDetectionThreshold(), dataRead[index], baseline.get(nodeNumber)))
										set.addToDetectionMap(specificType, scenario, nodeNumber, timestep);
								}
							}
						}
					}
				}
				hdf5File.close();
			} catch (Exception e) {
				System.out.println("Unable to read values from the hdf5 files...");
				e.printStackTrace();
			}
		}
		
		long elapsedTime = (System.currentTimeMillis() - startTime)/1000;
		System.out.println("You just created a detection map for " + specificType + " in " + Constants.formatSeconds(elapsedTime) + "! Awesome! So Fast!");
	}*/
	
	// If the user chooses to use variance as an objective, create a map of variance
	// Much easier to make this map if we have a list of detectable nodes
	// May explore two options - (1) save values to calculate variance later and (2) TODO running variance
	public static void createVarianceMap(IProgressMonitor monitor, ScenarioSet set) {
		
		long startTime = System.currentTimeMillis();
		if(set.getVarianceMap().size()!=0) return; //This is their second launch with the same settings, don't recalculate
		monitor.beginTask("Creating variance map for objective", set.getScenarios().size()+1);
		Point3i structure = set.getNodeStructure().getIJKDimensions();
		Map<String, Map<Integer, Map<Float, Map<Scenario, Float>>>> valueMap; //Needed for variance calculation
		valueMap = new HashMap<String, Map<Integer, Map<Float, Map<Scenario, Float>>>>(); //Specific Type, <Node Number, <Time <Scenario, Value>>>
		
		for(H5File hdf5File: hdf5Files.values()) {
			Scenario scenario = set.getScenario(hdf5File.getName().replaceAll("\\.h5" , ""));
			if(!set.getScenarios().contains(scenario)) continue; //Skip unused scenarios
			if(monitor.isCanceled()) return; //End if user canceled
			monitor.subTask("Processing " + scenario.toString());
			try {
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
					
					// Skip these
					if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
						continue;
					// Data at each time step
					int timeIndex = Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", ""));
					float timestep = set.getNodeStructure().getTimeAt(timeIndex);
					Object group =  root.getMemberList().get(rootIndex);
					for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
						Object child = ((Group)group).getMemberList().get(groupIndex);
						String type = ((Dataset)child).getName();
						// Only look at dataTypes that are enabled
						if(child instanceof Dataset && set.getDataTypes().contains(type)) {
							String specificType = set.getSensorSettings(type).getSpecificType();
							if(!valueMap.containsKey(specificType))
								valueMap.put(specificType, new HashMap<Integer, Map<Float, Map<Scenario, Float>>>());
							int dataset_id = ((Dataset)child).open();
							float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
							H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
							((Dataset)child).close(dataset_id);
							for(int index=0; index<dataRead.length; index++) {
								int nodeNumber = Constants.indexToNodeNumber(index, structure);
								if(!set.getSensorSettings(type).getDetectableNodes().contains(nodeNumber))
									continue; //Only save detectable nodes
								// Initialize value map
								if(!valueMap.get(specificType).containsKey(nodeNumber))
									valueMap.get(specificType).put(nodeNumber, new HashMap<Float, Map<Scenario, Float>>());
								if(!valueMap.get(specificType).get(nodeNumber).containsKey(timestep))
									valueMap.get(specificType).get(nodeNumber).put(timestep, new HashMap<Scenario, Float>());
								// Store values
								valueMap.get(specificType).get(nodeNumber).get(timestep).put(scenario, dataRead[index]);
							}
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Unable to read values from the hdf5 files...");
				e.printStackTrace();
			}
			monitor.worked(1);
		}
		// Now calculate variance map from stored values
		monitor.subTask("Calculating variance");
		for(String specificType : valueMap.keySet()) {
			for(Integer nodeNumber : valueMap.get(specificType).keySet()) {
				for(Float time : valueMap.get(specificType).get(nodeNumber).keySet()) {
					// Calculate the mean across scenarios
					float mean = 0;
					for(Scenario scenario : valueMap.get(specificType).get(nodeNumber).get(time).keySet()) {
						mean += valueMap.get(specificType).get(nodeNumber).get(time).get(scenario);
					}
					mean = mean / valueMap.get(specificType).get(nodeNumber).get(time).size();
					// Calculate the variance for each scenario and store
					for(Scenario scenario : valueMap.get(specificType).get(nodeNumber).get(time).keySet()) {
						float value = valueMap.get(specificType).get(nodeNumber).get(time).get(scenario);
						float variance = (mean - value) * (mean - value);
						set.setVarianceMap(specificType, scenario, time, nodeNumber, variance);
					}
				}
			}
		}
		monitor.worked(1);
		long elapsedTime = (System.currentTimeMillis() - startTime)/1000;
		System.out.println("You just created a variance map in " + Constants.formatSeconds(elapsedTime) + "! We had enough memory!");
	}
	
	
	// Instead of querying files for each value, generate a map with TTD at each node number for specific sensor settings
	public static HashMap<Integer, Float> goalSeek(ScenarioSet set, String parameter, Set<Integer> inputNodes) {
		HashMap<Integer, Float> absoluteChange = new HashMap<Integer, Float>();
		//Initialize
		for(Integer node: inputNodes)
			absoluteChange.put(node, (float)0);
		Point3i structure = set.getNodeStructure().getIJKDimensions();
		for(H5File hdf5File: hdf5Files.values()) { // Only checked scenarios
			Scenario scenario = set.getScenario(hdf5File.getName().replaceAll("\\.h5" , ""));
			if(!set.getScenarios().contains(scenario)) continue; // Skip if scenario is not being looked at
			Map<Integer, Float> baseline = new HashMap<Integer, Float>(); //stores values at the initial timestep
			Map<Integer, Float> comparison = new HashMap<Integer, Float>(); //stores values at the specified timestep
			try {
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
					
					// Skip these
					if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
						continue;
					
					// First time step sets the baseline
					else if(Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", "")) == 0) {
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(parameter)) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									baseline.put(Constants.indexToNodeNumber(index, structure), dataRead[index]);
								}
							}
						}
					}
					
					// The time step we are comparing against
					else if(Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", "")) == set.getNodeStructure().getTimeSteps().size()-1) {
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(parameter)) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									comparison.put(Constants.indexToNodeNumber(index, structure), dataRead[index]);
								}
							}
						}
					}
				}
				hdf5File.close();
			} catch (Exception e) {
				System.out.println("Unable to read detection values from the hdf5 files...");
				e.printStackTrace();
			}
			
			// Calculate the absolute change since baseline, aggregated across all active scenarios
			for(Integer node: inputNodes) {
				Float aggregateChange = comparison.get(node) - baseline.get(node) + absoluteChange.get(node);
				absoluteChange.put(node, aggregateChange);
			}
		}
		return absoluteChange;
	}
	
	
	// Determines if the node surpasses the threshold at the given time
	public static Boolean sensorTriggered(Trigger trigger, DeltaType deltaType, Float threshold, Float currentValue, Float valueAtTime0) {
		Boolean triggered = false;
		if(currentValue==null) return triggered;
		
		// See if we exceeded threshold
		if(trigger==Trigger.ABOVE_THRESHOLD) {
			triggered = threshold <= currentValue;
		} else if(trigger==Trigger.BELOW_THRESHOLD) {
			triggered = threshold >= currentValue;
		} else if(trigger==Trigger.RELATIVE_CHANGE) {
			float change = valueAtTime0 == 0 ? 0 : ((currentValue - valueAtTime0) / valueAtTime0)*100;
			if(deltaType==DeltaType.INCREASE) triggered = threshold <= change;
			else if(deltaType==DeltaType.DECREASE) triggered = threshold >= change;
			else if(deltaType==DeltaType.BOTH) triggered = threshold <= Math.abs(change);
		} else { //if(setting.getTrigger()==Trigger.ABSOLUTE_CHANGE)
			float change = currentValue - valueAtTime0;
			if(deltaType==DeltaType.INCREASE) triggered = threshold <= change;
			else if(deltaType==DeltaType.DECREASE) triggered = threshold >= change;
			else if(deltaType==DeltaType.BOTH) triggered = threshold <= Math.abs(change);
		}
		return triggered;
	}
	
	
	// E4D asks for a storage file to accompany the leakage files, we need to verify that the timesteps align
	public static Boolean checkTimeSync(IProgressMonitor monitor, String location1, String location2, int size) {
		float[] times1 = new float[size];
		float[] times2 = new float[size];
		// Read times from the storage file
		try {
			H5File storageH5  = new H5File(location1, HDF5Constants.H5F_ACC_RDONLY);
			storageH5.open();
			
			// Get the root node
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)storageH5.getRootNode()).getUserObject();
			// Get to the "data" group
			for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
				String name = root.getMemberList().get(rootIndex).getName();
				if(!name.startsWith("data")) continue; //We only want the data group, skip all others
				// Get to the "times" variable and read in values
				for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
					Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
					int dataset_id = dataset.open();
					if(!dataset.getName().equals("times")) continue; //We only want the times variable, skip all others
					times1 = (float[])dataset.read();
					H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, times1);
				}
			}
			storageH5.close();
		} catch (Exception e) {
			System.out.println("Unable to read time values from the hdf5 storage file...");
			e.printStackTrace();
		}
		// Read times from the leakage file
		try {
			H5File leakageH5  = new H5File(location1, HDF5Constants.H5F_ACC_RDONLY);
			leakageH5.open();
			
			// Get the root node
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)leakageH5.getRootNode()).getUserObject();
			// Get to the "data" group
			for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
				String name = root.getMemberList().get(rootIndex).getName();
				if(!name.startsWith("data")) continue; //We only want the data group, skip all others
				// Get to the "times" variable and read in values
				for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
					Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
					int dataset_id = dataset.open();
					if(!dataset.getName().equals("times")) continue; //We only want the times variable, skip all others
					times2 = (float[])dataset.read();
					H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, times2);
				}
			}
			leakageH5.close();
		} catch (Exception e) {
			System.out.println("Unable to read time values from the hdf5 leakage file...");
			e.printStackTrace();
		}
		// Now compare the times and return a result
		for(int i=0; i<times1.length; i++) {
			if(times1[i]!=times2[i])
				return false;
		}
		return true;
	}
	
	
	// This extracts an attribute from an HDF5 file
	// Also checks for plural version (added "s")
	@SuppressWarnings("unchecked")
	private static String extractAttribute(Dataset dataset, String find) throws Exception {
		List<Attribute> attributes = dataset.getMetadata();
		for(Attribute a: attributes) {
			if(a.getName().equals(find) || a.getName().equals(find+"s")) {
				Object obj = a.getValue();
				return ((String[]) obj)[0];
			}
		}
		return null;
	}
	
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public static Float getStatistic(String dataType, int index) {
		// 0 = minimum
		// 1 = average
		// 2 = maximum
		if(!statistics.isEmpty() && !dataType.contains("Electrical Conductivity")) {
			return statistics.get(dataType)[index];
		}
		return null;
	}
	
	public static String getStatisticsString(String dataType) {
		if(!statistics.isEmpty() && !dataType.contains("Electrical Conductivity")) {
			float min = statistics.get(dataType)[0];
			float avg = statistics.get(dataType)[1];
			float max = statistics.get(dataType)[2];
			return "Minimum = " + Constants.percentageFormat.format(min) + "; Average = " + Constants.percentageFormat.format(avg) + "; Maximum = " + Constants.percentageFormat.format(max);
		}
		return "";
	}
}
