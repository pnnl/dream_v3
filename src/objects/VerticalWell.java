package objects;

import utilities.Point3f;
import utilities.Point3i;

/**
 * Represents a well with varying depth
 */

public class VerticalWell {
	
	protected int i;
	protected int j;
	protected float depth;
	protected float installTime;
		
	public VerticalWell(int i, int j, int k, float time, NodeStructure nodeStructure) {
		
		this.i = i;
		this.j = j;
		depth = nodeStructure.getDepthFromK(k);
		installTime = time;
	}
	
	public void mergeWells(VerticalWell toMerge) {
		if(toMerge.getDepth() > depth)
			depth = toMerge.getDepth();
		if(toMerge.getInstallTime() < installTime)
			installTime = toMerge.getInstallTime();
	}
	
	@Override 
	public String toString()
	{
		return "[i=" + i + ", j=" + j + "]";	
	}
	
	public int getI() {
		return i;
	}
	
	public int getJ() {
		return j;
	}
	
	public Point3i getSurfaceIJK() {
		return new Point3i(i, j, 0);
	}
	
	public Point3f getSurfaceXYZ() {
		return new Point3f(i, j);
	}
	
	public void newDepth(int k, NodeStructure nodeStructure) {
		float newDepth = nodeStructure.getDepthFromK(k);
		if(newDepth > depth)
			depth = k;
	}
	
	public float getDepth() {
		return depth;
	}
	
	public float getInstallTime() {
		return installTime;
	}
	
	public boolean isAt(int i, int j) {
		return this.i==i && this.j==j;
	}
	
	public void moveTo(Point3i ijk) {
		this.i = (ijk.getI());
		this.j = (ijk.getJ());
	}
}