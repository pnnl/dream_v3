package gravity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import results.ResultPrinter;

/**
 * Runs gravity script.
 * 
 * @author huan482
 *
 */
public class RunPyScript {
	/** Name of the gravity script we're running. */
	private static final String GRAVITY_SCRIPT_NAME = "gravgrad.py";

	private static final String ALEX_PLOT_SCRIPT = "plot1.py";
	
	private boolean printToConsole = true;

	private int myCounter = 0;

	// Loop through all the files given through
	public void calculateGravity(final File[] allFiles) throws IOException {
		for (File f : allFiles) {
			myCounter++;
			runGravityCalculationScript(f.getAbsolutePath());
		}
	}

	public void createPythonGraph(final String theCSVDirectory, final String theCSVFile,
			final String inputDirectory, final String theSaveDirectory) {
		runPlot1PyScript(theCSVDirectory, theCSVFile, inputDirectory, theSaveDirectory);
	}
	/**
	 * Execute the the gravity calculation by getting the .in file and outputting
	 * the .fwd out.
	 * 
	 * @param fileName - The absolute path of the .in file inputted.
	 * @throws IOException
	 */
	private void runGravityCalculationScript(final String fileName) throws IOException {
		try {
			InputStream in = ResultPrinter.class.getResourceAsStream("/" + GRAVITY_SCRIPT_NAME);
			File fileOut = new File(System.getProperty("java.io.tmpdir"), GRAVITY_SCRIPT_NAME);
			OutputStream out = FileUtils.openOutputStream(fileOut);
			IOUtils.copy(in, out);
			in.close();
			out.close();
		} catch (Exception theException) {
			System.out.println("Unable to copy the script to the temp directory");
			theException.printStackTrace();
		}
		String script = System.getProperty("java.io.tmpdir") + File.separator + GRAVITY_SCRIPT_NAME;
		// The String our script is represented.
		StringBuilder command = new StringBuilder();
		command.append("py -3 \"" + script + "\"");
		command.append(" \"" + fileName + "\"" + " " + myCounter);
		try {
			Process p = Runtime.getRuntime().exec(command.toString());
			if (printToConsole) {
				BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = bri.readLine()) != null)
					System.out.println(line);
				bri.close();
				while ((line = bre.readLine()) != null)
					System.out.println(line);
				bre.close();
			}
		} catch (Exception e) {
			System.out.println("Install python version 3.6 or higher to gravity calculation.");
			e.printStackTrace();
		}
	}
	
	
	private void runPlot1PyScript(final String theCSVDirectory, final String theCSVFile,
			final String inputDirectory, final String theSaveDirectory) {
		try {
			InputStream in = ResultPrinter.class.getResourceAsStream("/" + ALEX_PLOT_SCRIPT);
			File fileOut = new File(System.getProperty("java.io.tmpdir"), ALEX_PLOT_SCRIPT);
			OutputStream out = FileUtils.openOutputStream(fileOut);
			IOUtils.copy(in, out);
			in.close();
			out.close();
		} catch (Exception theException) {
			System.out.println("Unable to copy the script to the temp directory");
			theException.printStackTrace();
		}
		String script = System.getProperty("java.io.tmpdir") + File.separator + ALEX_PLOT_SCRIPT;
		// The String our script is represented.
		StringBuilder command = new StringBuilder();
		command.append("py -3 \"" + script + "\"");
		command.append(" \"" + theCSVDirectory + "\"" + " " + 
		" \"" + theCSVFile + "\"" +
	    " " + " \"" + inputDirectory + "\"" + " " + " \"" 
		+ theSaveDirectory + "\"" + " ");
		
		try {
			Process p = Runtime.getRuntime().exec(command.toString());
			if (printToConsole) {
				BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = bri.readLine()) != null)
					System.out.println(line);
				bri.close();
				while ((line = bre.readLine()) != null)
					System.out.println(line);
				bre.close();
			}
		} catch (Exception e) {
			System.out.println("Install python version 3.6 or higher to gravity calculation.");
			e.printStackTrace();
		}
		
	}
}
