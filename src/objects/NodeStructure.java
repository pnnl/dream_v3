package objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import utilities.Constants;
import utilities.Point3i;
import utilities.Constants.FileType;
import utilities.Point3f;

/**
 * Basic structure the grid containing the data
 * Includes xyz, xyz edges, timesteps, parameters, units, porosity
 * 
 * @author port091
 * @author rodr144
 * @author whit162
 */
public class NodeStructure {

	private List<Float> x;
	private List<Float> y;
	private List<Float> z;
	private List<Float> edgex;
	private List<Float> edgey;
	private List<Float> edgez;
	private String positive;
	private float globalMaxZ;
	private float globalMinZ;

	private List<TimeStep> timeSteps;
	private List<String> parameters;
	private HashMap<String, String> units; //<dataType, unit>
	private Point3i ijkDimensions;
	
	private HashMap<Point3i, Float> porosityOfNode;
	private float porosity; //For large grids we run out of member with porosityOfNode, so this is an option when not read from file
	
	/**
	 * Initializes node structure from new H5 files (includes edge, porosity, units)
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param edgex
	 * @param edgey
	 * @param edgez
	 * @param timeSteps
	 * @param porosity
	 * @param units
	 */
	public NodeStructure(List<Float> x, List<Float> y, List<Float> z, List<Float> edgex, List<Float> edgey, List<Float> edgez, List<TimeStep> timeSteps, HashMap<Point3i, Float> porosity, HashMap<String, String> units, String positive) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.edgex = edgex;
		this.edgey = edgey;
		this.edgez = edgez;
		this.positive = positive;
		this.timeSteps = timeSteps;
		parameters = new ArrayList<String>();
		this.units = units;
		ijkDimensions = new Point3i(x.size(), y.size(), z.size());
		porosityOfNode = porosity;
		findGlobalZ();
	}
	
	/**
	 * Initializes node structure from old H5 files (no edge, porosity, units)
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param timeSteps
	 */
	public NodeStructure(List<Float> x, List<Float> y, List<Float> z, List<TimeStep> timeSteps) {
		this.x = x;
		this.y = y;
		this.z = z;
		edgex = setEdge(x);
		edgey = setEdge(y);
		edgez = setEdge(z);
		positive = "";
		this.timeSteps = timeSteps;
		parameters = new ArrayList<String>();
		units = new HashMap<String, String>();
		ijkDimensions = new Point3i(x.size(), y.size(), z.size());
		porosity = 0;
		findGlobalZ();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Node structure:\n");
		builder.append("\tDimensions: " + ijkDimensions.toString() + "\n");
		builder.append("\tData types: " + parameters.toString() + "\n");
		builder.append("Units: " + units.toString() + "\n");
		builder.append("\tx: " + x.toString() + "\n");
		builder.append("\ty: " + y.toString() + "\n");
		builder.append("\tz: " + z.toString() + "\n");
		builder.append("\tpositive: " + positive + "\n");
		builder.append("\tTime steps: " + timeSteps.toString() + "\n");
		return builder.toString();
	}
	
	private void findGlobalZ() {
		globalMinZ = Float.MAX_VALUE;
		globalMaxZ = -Float.MAX_VALUE;
		for(Float zVal : z) {
			if(zVal < globalMinZ) {
				globalMinZ = zVal;
			}
			if(zVal > globalMaxZ) {
				globalMaxZ = zVal;
			}
		}
	}
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public static List<Float> setEdge(List<Float> cells) {
		List<Float> cellBounds = new ArrayList<Float>();		
		for(int i=1; i<cells.size(); i++) {
			float half = (cells.get(i)-cells.get(i-1))/2;
			if(i == 1)
				cellBounds.add(cells.get(i-1)-half);
			cellBounds.add(cells.get(i-1)+half);
			if(i == cells.size()-1) 
				cellBounds.add(cells.get(i)+half);
		}
		return cellBounds;
	}
	
	// Returns a time from index
	public Float getTimeAt(int index) {
		if(index > timeSteps.size()-1)
			return null;
		if(timeSteps.get(index).getTimeStep() == index)
			return timeSteps.get(index).getRealTime();
		return null; // Does not exist
	}
	
	public Integer getTimeIndex(float time) {
		for (int index=0; index<timeSteps.size(); index++) {
			if(timeSteps.get(index).getRealTime() == time)
				return index;
		}
		return null; //didn't find an index
	}
	
	public Float getMaxTime() {
		TimeStep timeStep = timeSteps.get(timeSteps.size()-1);
		return timeStep.getRealTime();
	}
	
	// Returns I for a given X
	public int getI(float x) {
		return this.x.indexOf(x) + 1;
	}
	
	// Returns J for a given Y
	public int getJ(float y) {
		return this.y.indexOf(y) + 1;
	}
	
	// Returns K for a given Z
	public int getK(float z) {
		return this.z.indexOf(z) + 1;
	}
	
	public float getZFromK(int k) {
		return z.get(k-1);
	}
	
	// Returns the total possible volume of aquifer degraded
	public float getPossibleVAD() {
		float volume = 0;
		float totalVolume = 0;
		for(int i=1; i<edgex.size(); i++) {
			for(int j=1; j<edgey.size(); j++) {
				for(int k=1; k<edgez.size(); k++) {
					float lengthx = edgex.get(i) - edgex.get(i-1);
					float lengthy = edgey.get(j) - edgey.get(j-1);
					float lengthz = edgez.get(k) - edgez.get(k-1);
					if (Constants.fileType==FileType.H5) {
						volume = lengthx*lengthy*lengthz*porosityOfNode.get(new Point3i(i,j,k));
					} else {
						volume = lengthx*lengthy*lengthz*this.porosity;
					}
					totalVolume += volume;
				}
			}
		}
		return totalVolume;
	}
		
	// Returns the volume of aquifer degraded for a cube (factors porosity)
	public float getVolumeOfNode(Point3i location){
		float lengthx = edgex.get(location.getI()) - edgex.get(location.getI()-1);
		float lengthy = edgey.get(location.getJ()) - edgey.get(location.getJ()-1);
		float lengthz = edgez.get(location.getK()) - edgez.get(location.getK()-1);
		float totalVolume = Math.abs(lengthx*lengthy*lengthz);
		float porosity = this.porosity;
		if(porosityOfNode!=null) porosity = porosityOfNode.get(location);
		return totalVolume*porosity;
	}
	
	public float getVolumeOfDomain() {
		float lengthx = edgex.get(edgex.size() - 1);
		float lengthy = edgey.get(edgey.size() - 1);
		float lengthz = edgez.get(edgez.size() - 1);
		float totalVolume = Math.abs(lengthx*lengthy*lengthz);
		float porosity = this.porosity;
		if(porosityOfNode!=null) porosity = getMaxPorosity();
		return totalVolume*porosity;
	}
	
	public float getMaxPorosity() {
		float maxPorosity = 0;
		for(float porosity : porosityOfNode.values()) {
			if(porosity > maxPorosity)
				maxPorosity = porosity;
		}
		return maxPorosity;
	}
	
	// Returns the XY area of a node
	public float getAreaOfNode(Point3i location) {
		float lengthx = edgex.get(location.getI()) - edgex.get(location.getI()-1);
		float lengthy = edgey.get(location.getJ()) - edgey.get(location.getJ()-1);
		return Math.abs(lengthx*lengthy);
	}
	
	// Returns the volume of aquifer degraded for a set of nodes (factors porosity)
	public float getVolumeOfNodeSet(HashSet<Integer> nodes) {
		float volume = 0;
		for(Integer node: nodes) {
			Point3i location = nodeNumberToIJK(node);
			volume += getVolumeOfNode(location);
		}
		return volume;
	}
	
	// Converts IJK to node number (IJK must be 1-indexed)
	public int ijkToNodeNumber(int i, int j, int k) {
		return ijkToNodeNumber(new Point3i(i, j, k));
	}
	public int ijkToNodeNumber(Point3i ijk) {
		return Constants.ijkToNodeNumber(ijk, ijkDimensions);
	}
	
	// Converts node number to IJK (IJK must be 1-indexed)
	public Point3i nodeNumberToIJK(int nodeNumber) {
		return Constants.nodeNumberToIJK(nodeNumber, ijkDimensions);
	}
	
	public List<Point3i> nodeNumberToIJKList(List<Integer> nodeNumList) {
		List<Point3i> tempList = new ArrayList<Point3i>();
		for (int i = 0; i < nodeNumList.size(); i++ )
			tempList.add(Constants.nodeNumberToIJK(nodeNumList.get(i), ijkDimensions));
		return tempList;
	}
	
	// Return XYZ from IJK
	public Point3f getXYZFromIJK(Point3i node) {
		return new Point3f(x.get(node.getI()-1), y.get(node.getJ()-1), z.get(node.getK()-1));
	}
	
	// Return XYZ from Node Number
	public Point3f getXYZFromNodeNumber(int nodeNumber) {
		return getXYZFromIJK(nodeNumberToIJK(nodeNumber));
	}
	
	// Return Node Number from XYZ
	public int getNodeNumberFromXYZ(Point3f point) {
		return getNodeNumber(getI(point.getX()), getJ(point.getY()), getK(point.getZ()));
	}
	
	// Return Node Number from IJK indices
	public int getNodeNumber(int i, int j, int k) {
		int iMax = ijkDimensions.getI();
		int jMax = ijkDimensions.getJ();
		return (k-1) * iMax * jMax + (j-1) * iMax + i;
	}
	
	// Return the timestep array
	public List<TimeStep> getTimeSteps() {
		return timeSteps;
	}
	
	// Sets the timestep array separate from NodeStructure initialization
	public void setTimeSteps(List<TimeStep> timeSteps) {
		this.timeSteps = timeSteps;
	}
	
	// Return X list
	public List<Float> getX() {
		return x;
	}
	
	// Return Y list
	public List<Float> getY() {
		return y;
	}
	
	// Return Z list
	public List<Float> getZ() {
		return z;
	}
	
	// Returns X edge list
	public List<Float> getEdgeX() {
		return edgex;
	}
	
	// Returns Y edge list
	public List<Float> getEdgeY() {
		return edgey;
	}
	
	// Returns Z edge list
	public List<Float> getEdgeZ() {
		return edgez;
	}
	
	// Returns global max Z
	public float getGlobalMaxZ() {
		return globalMaxZ;
	}
	
	// Returns global min Z
	public float getGlobalMinZ() {
		return globalMinZ;
	}
	
	// Return the list of parameters
	public List<String> getParameters() {
		return parameters;
	}
	
	// Sets the parameters array separate from NodeStructure initialization
	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}
	
	// Adds parameters one at a time
	public void addParameter(String parameter) {
		parameters.add(parameter);
	}
	
	// Assigns units for a specific parameter
	public void addUnit(String parameter, String unit) {
		units.put(parameter, unit);
	}
	
	// Returns the unit for a given parameter
	public String getUnit(String parameter) {
		if(units.containsKey(parameter))
			return units.get(parameter);
		return "";
	}
	
	// Returns the units
	public HashMap<String, String> getUnits() {
		return units;
	}
	
	// Assigns a positive direction
	public void addPositive(String positive) {
		this.positive = positive;
	}
	
	// Returns the positive direction for the z-axis
	public String getPositive() {
		return positive;
	}
	
	// Returns the total number of nodes in the domain
	public int getTotalNodes() {
		return ijkDimensions.getI() * ijkDimensions.getJ() * ijkDimensions.getK();
	}
	
	// Returns the total number of surface nodes in the domain
	public int getTotalSurfaceNodes() {
		return ijkDimensions.getI() * ijkDimensions.getJ();
	}
	
	// Returns the dimensions for the domain
	public Point3i getIJKDimensions() {
		return ijkDimensions;
	}
	
	// Returns a list of all the neighboring nodes
	public List<Integer> getNeighborNodes(Point3i node) {
		List<Integer> neighborNodes = new ArrayList<Integer>();
		int iMax = ijkDimensions.getI();
		int jMax = ijkDimensions.getJ();
		int kMax = ijkDimensions.getK();

		for (int iN = node.getI() - 1; iN < node.getI() + 2; iN++) {
			for (int jN = node.getJ() - 1; jN < node.getJ() + 2; jN++) {
				for (int kN = node.getK() - 1; kN < node.getK() + 2; kN++) {
					if ((iN > 0 && iN <= iMax) && (jN > 0 && jN <= jMax)
							&& (kN > 0 && kN <= kMax)) {
						if (!(iN == node.getI() && jN == node.getJ() && kN == node.getK()))
							neighborNodes.add((kN - 1) * iMax * jMax + (jN - 1) * iMax + iN);
					}
				}
			}
		}
		return neighborNodes;
	}
	
	// Returns a list of Node Numbers that reside within a well
	public List<Integer> getNodesInWell(VerticalWell well) {
		List<Integer> nodesInWell = new ArrayList<Integer>();
		for(int k = 1; k <= ijkDimensions.getK(); k++)
			nodesInWell.add(getNodeNumber(well.getI(), well.getJ(), k));
		return nodesInWell;
	}
	
	// Check if porosity values have been added
	public boolean porosityIsSet() {
		if(porosityOfNode==null && porosity==0) return false;
		return true;
	}
	
	/**
	 * This function assumes that the porosity is listed in an order that increments i, then j, then k.
	 * It also ignores any lines that it cannot intepret as a float and continues on to the next.
	 * If we have found precisely the number of floats that we expect, the porosity is set and it returns true.
	 * Otherwise, porosity is not set and false is returned.
	 * @param file
	 * @throws IOException
	 */
	
	public boolean setPorositiesFromIJKOrderedFile(File file) throws IOException {
		HashMap<Point3i, Float> porosity = new HashMap<Point3i, Float>();

		BufferedReader fileReader = new BufferedReader(new FileReader(file));
		String line = "";
		int nodeNumber = 1;
		Point3i currentPoint = new Point3i(-1,-1,-1);
		while((line = fileReader.readLine()) != null){ 
			try{
				float f = Float.valueOf(line);
				currentPoint = nodeNumberToIJK(nodeNumber);
				porosity.put(currentPoint, f);
				nodeNumber++;
			} catch(NumberFormatException e){
				System.out.println("Error reading porosity from file");
				e.printStackTrace();
			}
		}
		fileReader.close();
		if(currentPoint.equals(ijkDimensions)) {
			this.porosityOfNode = porosity;
			return true;
		}
		return false;
	}
	
	public void setPorositiesFromZone(int iMin, int iMax, int jMin, int jMax, int kMin, int kMax, float porosity){
		porosityOfNode = new HashMap<Point3i, Float>();
		for(int i = iMin; i<=iMax; i++) {
			for(int j = jMin; j<=jMax; j++) {
				for(int k = kMin; k<=kMax; k++) {
					porosityOfNode.put(new Point3i(i,j,k), porosity);
				}
			}
		}
	}
	
	public void setPorosity(float porosity) {
		this.porosity = porosity;
	}
	
	public void writePorositiesToIJKFile(File file) throws FileNotFoundException, UnsupportedEncodingException{
		PrintWriter writer = new PrintWriter(file.getAbsolutePath(), "UTF-8");
		for(int k = 1; k<=ijkDimensions.getK(); k++){
			for(int j = 1; j<=ijkDimensions.getJ(); j++){
				for(int i=1; i<=ijkDimensions.getI(); i++){
					float value = porosityOfNode.get(new Point3i(i,j,k));
					writer.println(String.valueOf(value));
				}
			}
		}
		writer.close();
	}
	
	public float getDepthFromK(int k) {
		if(positive.equals("up")) {
			return globalMaxZ - getZFromK(k);
		} else { //(positive.equals("down"))
			return getZFromK(k) - globalMinZ;
		}
	}
	
	// Reset everything in NodeStructure
	public void clear() {
		x = null;
		y = null;
		z = null;
		edgex = null;
		edgey = null;
		edgez = null;
		timeSteps = null;
		parameters = null;
		units = null;
		ijkDimensions = null;
		porosityOfNode = null;
	}
}
