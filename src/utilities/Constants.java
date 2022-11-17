package utilities;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.objecthunter.exp4j.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import objects.TimeStep;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.SensorType;
import objects.SensorSetting.Trigger;

/**
 * Constants for use throughout code, the booleans at the top and random seed can be changed to alter functionality
 * @author port091
 * @author rodr144
 */

public class Constants {
	
	// These probably be eventually wrapped into the code directly, but for now give us functionality for debugging/developing extensions without breaking the working release
	public static boolean autoTest = false; //Running AutoTesting.java will set this to true
	public static boolean skipToEnd = false; //Devs can skip clicking, go directly to RunDREAM
	public static boolean buildDev = false;
	public static boolean runThreaded = true;
	public static boolean runScripts = true;
	public static boolean useParetoOptimal = true;
	public static boolean weightObjectives = true;
	public static boolean runningJar = false; 
	public static boolean paretoRank = true;
	public static boolean showGraph = true;
	public static boolean showViz = true;
	public static boolean showPyPlots = false;
	public static FileType fileType;
	
	static File userDirectory = new File(System.getProperty("user.dir"));
	public static String userDir = userDirectory.getPath();
	public static String parentDir = userDirectory.getParent();
	public static String tempDir = System.getProperty("java.io.tmpdir")+File.separator+"dream";
	public static String selectedDir = "";
	public static boolean foundDirectory = false;
	
	public static Color black = new Color(Display.getCurrent(), 0, 0, 0);
	public static Color red = new Color(Display.getCurrent(), 255, 0, 0);
	public static Color green = new Color(Display.getCurrent(), 0, 100, 0);
	public static Color grey = new Color(Display.getCurrent(), 109, 109, 109);
	public static Color white = new Color(Display.getCurrent(), 255, 255, 255);
	public static Font boldFont = new Font(Display.getCurrent(), new FontData("Helvetica", 12, SWT.BOLD));
	public static Font boldFontSmall = new Font(Display.getCurrent(), new FontData("Helvetica", 10, SWT.BOLD));
	public static Font normalFontSmall = new Font(Display.getCurrent(), new FontData("Helvitica", 10, SWT.NORMAL));

	public static Random random = new Random(1); //Right now this is seeded, this way we have reproducible results. Should probably un-seed for release.
	
	public static DecimalFormat decimalFormat = new DecimalFormat("###.###");
	public static DecimalFormat percentageFormat = new DecimalFormat("###.##");
	public static DecimalFormat exponentialFormat = new DecimalFormat("0.00000000E00");
	public static DecimalFormat decimalFormatForCost = new DecimalFormat("0.00");
	public static DecimalFormat integerFormat = new DecimalFormat("#");
	
	public static String formatCost(String unit, float cost) {
		DecimalFormat costFormat;
		if(cost<1000)
			costFormat = new DecimalFormat("###.##");
		else if(cost<1000000) {
			cost /= 1000;
			costFormat = new DecimalFormat("###.##K");
		} else {
			cost /= 1000000;
			costFormat = new DecimalFormat("###.##M");
		}
		return unit + costFormat.format(cost);
	}
		
	public static String homeDirectory = System.getProperty("user.home");
	
	/*	
	 *  Example of node number vs. index. Each cell has: 
	 *  1) i,j,k			- each of the three dimensions are 1 <= dim <= dimMax
	 *  2) node number		- 1-indexed, IJK ordered, used by DREAM to store which nodes are triggered and to query from nodes
	 *  3) index			- 0-indexed, KJI ordered, used in reading values from the hdf5 files.
	 * 
	 *  _________________________    _________________________
	 *  | 1,1,1 | 2,1,1 | 3,1,1 |    | 1,1,2 | 2,1,2 | 3,1,2 |
	 * 	| 1     | 2     | 3     |    | 13    | 14    | 15    |
	 * 	| 0     | 8     | 16    |    | 1     | 9     | 17    |
	 * 	|_______|_______|_______|    |_______|_______|_______|  
	 * 	| 1,2,1 | 2,2,1 | 3,2,1 |    | 1,2,2 | 2,2,2 | 3,2,2 |
	 * 	| 4     | 5     | 6     |    | 16    | 17    | 18    |
	 * 	| 2     | 10    | 18    |    | 3     | 11    | 19    |
	 * 	|_______|_______|_______|    |_______|_______|_______| 
	 * 	| 1,3,1 | 2,3,1 | 3,3,1 |    | 1,3,2 | 2,3,2 | 3,3,2 |
	 * 	| 7     | 8     | 9     |    | 19    | 20    | 21    |
	 * 	| 4     | 12    | 20    |    | 5     | 13    | 21    |
	 * 	|_______|_______|_______|    |_______|_______|_______|  
	 * 	| 1,4,1 | 2,4,1 | 3,4,1 |    | 1,4,2 | 2,4,2 | 3,4,2 |
	 * 	| 10    | 11    | 12    |    | 22    | 23    | 24    |
	 * 	| 6     | 14    | 22    |    | 7     | 15    | 23    |
	 * 	|_______|_______|_______|    |_______|_______|_______|
	 * 
	 */
	
	///////////////////////////////
	//// Convert IJK and Index ////
	///////////////////////////////
	
	// Converts IJK to index (IJK must be 1-indexed)
	public static int ijkToIndex(int i, int j, int k, int jMax, int kMax) {
		return (i-1)*jMax*kMax + (j-1)*kMax + (k-1);
	}
	
	// Converts index to IJK (IJK must be 1-indexed)
	public static Point3i indexToIJK(int index, int jMax, int kMax) {
		int i = (int)Math.floor(index/(jMax*kMax)) + 1;
		int j = (int)Math.floor((index - (i-1)*jMax*kMax)/kMax) + 1;
		int k = index - (i-1)*jMax*kMax - (j-1)*kMax + 1;
		return new Point3i(i, j, k);
	}
	
	public static Point3i indexToIJK(int index, Point3i max) {
		return indexToIJK(index, max.getJ(), max.getK());
	}
	
	////////////////////////////////////
	//// Convert IJK and NodeNumber ////
	////////////////////////////////////
	
	// Converts IJK to node number (IJK must be 1-indexed)
	public static int ijkToNodeNumber(int i, int j, int k, int iMax, int jMax) {
		return (k-1)*iMax*jMax + (j-1)*iMax + (i-1) + 1;
	}
	
	public static int ijkToNodeNumber(Point3i ijk, Point3i max) {
		return ijkToNodeNumber(ijk.getI(), ijk.getJ(), ijk.getK(), max.getI(), max.getJ());
	}
	
	// Converts node number to IJK (IJK must be 1-indexed)
	public static Point3i nodeNumberToIJK(int nodeNumber, int iMax, int jMax) {
		int k = (int)Math.floor((nodeNumber-1)/(iMax*jMax)) + 1;
		int j = (int)Math.floor((nodeNumber-1-(k-1)*iMax*jMax)/iMax) + 1;
		int i = nodeNumber - (j-1)*iMax - (k-1)*iMax*jMax;
		return new Point3i(i, j, k);
	}
	// Converts node number to IJK (IJK must be 1-indexed)
	public static Point3i nodeNumberToIJK(int nodeNumber, Point3i max) {
		return nodeNumberToIJK(nodeNumber, max.getI(), max.getJ());
	}
	
	//////////////////////////////////////
	//// Convert Index and NodeNumber ////
	//////////////////////////////////////
	
	// Converts node number to index
	public static int nodeNumberToIndex(int nodeNumber, int iMax, int jMax, int kMax) {
		Point3i ijk = nodeNumberToIJK(nodeNumber, iMax, jMax);
		return ijkToIndex(ijk.getI(), ijk.getJ(), ijk.getK(), jMax, kMax);
	}
	public static int nodeNumberToIndex(int nodeNumber, Point3i max) {
		return nodeNumberToIndex(nodeNumber, max.getI(), max.getJ(), max.getK());
	}
	
	// Converts node number to index
	public static int indexToNodeNumber(int index, Point3i max) {
		Point3i ijk = indexToIJK(index, max.getJ(), max.getK());
		return ijkToNodeNumber(ijk.getI(), ijk.getJ(), ijk.getK(), max.getI(), max.getJ());
	}
	
	/**
	 * Alters a string
	 * @param in
	 * @return String
	 */
	public static String transform(String in) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < in.length(); i++){
		    char c = in.charAt(i);
			if(!Character.isDigit(c)) {
				if(Character.isUpperCase(c))
					out.append(Character.toLowerCase(c));
				else
					out.append(Character.toUpperCase(c));
			} else
				out.append((Integer.parseInt(String.valueOf(c)) + 5) % 10);
		}
		return out.toString();
	}
	
	/**
	 * Returns true if the string is a valid float
	 * @param string
	 * @return boolean
	 */
	public static boolean isValidFloat(String string) {
		try {
			Float.parseFloat(string);
			return true;
		} catch (NumberFormatException ne) {
			return false;
		}
	}
	
	/**
	 * Returns true if the string is a valid integer
	 * @param string
	 * @return boolean
	 */
	public static boolean isValidInt(String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (NumberFormatException ne) {
			return false;
		}
	}
	
	/**
	 * Returns true if the string is a valid weight equation
	 * Weights are determined by VAD, VariableA, and VariableB
	 * @param equation
	 * @return boolean
	 */
	public static boolean isValidWeightEquation(String equation) {
		if(equation=="") return false; //Blank equation returns false
		Double temperature = evaluateWeightExpression(equation, 1f, 1f, 1f); //test with dummy values to verify equation
		if(temperature==null)
			return false;
		else
			return true;
	}
	
	/**
	 * Returns a weight value for scenario weighting page,
	 * processing the String equation entered by the user.
	 * For variables: v = volume of aquifer degraded, a = variable A, b = variable B.
	 * If the variable is not expected in a calculation, set it to 0 to ignore.
	 * @param equation
	 * @param vad
	 * @param variableA
	 * @param variableB
	 */
	public static Double evaluateWeightExpression(String equation, Float vad, Float varA, Float varB) {
		// Remove leading equals sign in case they are used to excel
		if(equation.startsWith("="))
			equation = equation.substring(1);
		// Based on transformation at cooling input, all equations will: be lower case, have no spaces, and have no trailing operators
		try {
			return new ExpressionBuilder(equation)
				.variables("v", "a", "b").build()
				.setVariable("v", vad)
				.setVariable("a", varA)
				.setVariable("b", varB)
				.evaluate();
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Costs are provided in equation format, this method checks that the equation is valid
	 * @param equation
	 * @param times
	 */
	public static boolean isValidCostEquation(String equation, List<TimeStep> times) {
		if(equation=="") return false; //Blank equation returns false
		for(TimeStep time: times) { //test that all real times give a positive value
			float t = time.getRealTime();
			Double cost = evaluateCostExpression(equation, t, 1, 1f, 1);
			if(cost==null || cost <= 0)
				return false;
		}
		return true;
	}
	
	/**
	 * Returns a cost value, processing the String equation entered by the user.
	 * For variables: t = time, i = number of installs, a = area of surface sensor, s = number of surface surveys.
	 * If the variable is not expected in a calculation, set it to 0 to ignore.
	 * @param equation
	 * @param time
	 * @param number of installs
	 * @param area
	 * @param number of surveys
	 */
	public static Double evaluateCostExpression(String equation, Float time, Integer installs, Float area, Integer numSurveys) {
		if(Constants.isValidFloat(equation)) //Just return double if it is a number
			return Double.parseDouble(equation);
		// Remove leading equals sign in case they are used to excel
		if(equation.startsWith("="))
			equation = equation.substring(1);
		// Based on transformation at cost inputs, all equations will: be lower case, have no spaces, and have no trailing operators
		try {
			return new ExpressionBuilder(equation)
				.variables("t", "i", "a", "s").build()
				.setVariable("t", time)
				.setVariable("i", installs)
				.setVariable("a", area)
				.setVariable("s", numSurveys)
				.evaluate();
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * The cooling equation can be altered by the user, this method checks that the equation is valid
	 * @param equation
	 */
	public static boolean isValidCoolingEquation(String equation) {
		if(equation=="") return false; //Blank equation returns false
		Double temperature = evaluateCoolingExpression(equation, 1, 100); //test with dummy values to verify equation
		if(temperature==null)
			return false;
		else
			return true;
	}
	
	/**
	 * Returns a temperature value for the Simulated Annealing algorithm,
	 * processing the String equation entered by the user.
	 * For variables: i = iteration, n = total iterations.
	 * If the variable is not expected in a calculation, set it to 0 to ignore.
	 * @param equation
	 * @param iteration
	 * @param total
	 */
	public static Double evaluateCoolingExpression(String equation, int iteration, int total) {
		// Remove leading equals sign in case they are used to excel
		if(equation.startsWith("="))
			equation = equation.substring(1);
		// Based on transformation at cooling input, all equations will: be lower case, have no spaces, and have no trailing operators
		try {
			return new ExpressionBuilder(equation)
					.variables("i", "n").build()
					.setVariable("i", iteration)
					.setVariable("n", total)
					.evaluate();
		} catch (Exception e) {
			return null;
		}
		
	}
	
	@SuppressWarnings("unused")
	public static boolean isValidFile(String string) {
		try {
			File fileTest = new File(string);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static float[] listToArray(List<Float> list) {
		float[] array = new float[list.size()];
		for(int i=0; i<list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	public static List<Float> arrayToList(float[] array) {
		List<Float> list = new ArrayList<Float>();
		for(int i=0; i<array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
	public static String formatSeconds(long sec) {
		String text;
		if(sec>18000)
			text = sec/3600 + " hours"; 
		else if(sec>300)
			text = sec/60 + " minutes";
		else
			text = sec + " seconds";
		return text;
	}
	
	public static String weightedRandomNumber(ArrayList<String> options, ArrayList<Integer> weights) {
		int totalWeight = 0;
		for (int i=0; i<weights.size(); i++) {
			totalWeight += weights.get(i);
		}
		int select = random.nextInt(totalWeight);
		for (int i=0; i<weights.size(); i++) {
			select -= weights.get(i);
			if(select<0) {
				return options.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Enum for determining which type of input file the user is reading
	 * Allowed extensions: .h5, .iam, .dream
	 */
	public enum FileType {
		H5("H5"),
		IAM("IAM"),
		DREAM("DREAM Save");
		
		private String fileType;
		private FileType(String fileType) {
			this.fileType = fileType;
		}
		@Override
		public String toString() {
			return fileType;
		}
	}
	
	public static void setFileType(File input) {
		if(input.getName().endsWith(".h5"))
			fileType = FileType.H5;
		else if(input.getName().endsWith(".iam"))
			fileType = FileType.IAM;
		else if(input.getName().endsWith(".dream"))
			fileType = FileType.DREAM;
	}
	
	public static boolean isCorrectFileType(File input) {
		if(input.getName().endsWith(".h5") && fileType==FileType.H5)
			return true;
		if(input.getName().endsWith(".iam") && fileType==FileType.IAM)
			return true;
		if(input.getName().endsWith(".dream") && fileType==FileType.DREAM)
			return true;
		return false;
	}
	
	/**
	 * Method for converting to the specific type string
	 * @param type
	 * @param trigger
	 * @param deltaType
	 * @param value
	 * @return specificType (string)
	 */
	public static String getSpecificType(String type, Trigger trigger, DeltaType deltaType, float value, SensorType sensorType) {
		StringBuilder specificType = new StringBuilder();
		specificType.append(type);
		if(trigger == Trigger.BELOW_THRESHOLD)
			specificType.append("_below_");
		else if(trigger == Trigger.ABOVE_THRESHOLD)
			specificType.append("_above_");
		else if(trigger == Trigger.RELATIVE_CHANGE)
			specificType.append("_rel_");
		else // if(trigger == Trigger.ABSOLUTE_CHANGE)
			specificType.append("_abs_");
		if((trigger == Trigger.RELATIVE_CHANGE || trigger == Trigger.ABSOLUTE_CHANGE) && deltaType == DeltaType.INCREASE)
			specificType.append("+");
		specificType.append(value);
		if(sensorType == SensorType.POINT_SENSOR)
			specificType.append("_point");
		if(sensorType == SensorType.SURFACE)
			specificType.append("_surface");
		//if(sensorType == SensorType.BOREHOLE)
			//specificType.append("_borehole");
		//if(sensorType == SensorType.CROSS_WELL)
			//specificType.append("_crosswell");
		return specificType.toString();
	}
	public static Trigger getTriggerFromSpecificType(String specificType) {
		if(specificType.contains("rel"))
			return Trigger.RELATIVE_CHANGE;
		else if (specificType.contains("abs"))
			return Trigger.ABSOLUTE_CHANGE;
		else if(specificType.contains("below"))
			return Trigger.BELOW_THRESHOLD;
		else
			return Trigger.ABOVE_THRESHOLD;
	}
}
