package utilities;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import utilities.Constants.FileType;
import wizardPages.DREAMWizard;

public class AutoTesting {
	
	public static String hdf5Directory;
	public static String iamDirectory;
	public static StringWriter error1 = new StringWriter();
	public static StringWriter error2 = new StringWriter();
	
	public static void main(String args[]) {
		Constants.autoTest = true;
		System.out.println("Test 1 - HDF5 Example...");
		try {
			Constants.fileType = FileType.H5;
			hdf5Directory = Constants.userDir + File.separator + "unitTests" + File.separator + "H5_test";
			DREAMWizard.main(args);
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(error1));
		}
		System.out.println("\nTest 2 - IAM Example...");
		try {
			Constants.fileType = FileType.IAM;
			iamDirectory = Constants.userDir + File.separator + "unitTests" + File.separator + "IAM_test";
			DREAMWizard.main(args);
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(error2));
		}
		
		// Write out the summary for the tests
		System.out.println("\n-- Auto Test Summary --");
		if(error1.toString().contains("SWTException: Widget is disposed")) //Error when closing programmatically
			System.out.println("Test 1 - HDF5 Example: Success");
		else {
			System.out.println("Test 1 - HDF5 Example: Failure");
			System.out.println(error1.toString());
		}
		if(error2.toString().contains("SWTException: Widget is disposed")) //Error when closing programmatically
			System.out.println("Test 2 - IAM Example: Success");
		else {
			System.out.println("Test 2 - IAM Example: Failure");
			System.out.println(error2.toString());
		}
	}
}
