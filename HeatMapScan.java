import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

public class HeatMapScan extends GeneralScan {
	final static String FILENAME = "observation_test.dat";

	// For generating random doubles in range
	private final static double min = -1.0;
	private final static double max = 1.0;

	/**
	 * Constructor. Initializes variables.
	 * 
	 * @param raw The original observation data
	 */
	public HeatMapScan(ArrayList<Observation> raw) {
		super(raw);
	}

	/**
	 * Displays heatmap animation.
	 * 
	 * @param scanResult Tally data that is used for the heatmap animation.
	 */
	public void displayHeatmap(ArrayList<int[][]> scanResult) {
		CGDemo cgDemo = new CGDemo(scanResult);

		try {
			cgDemo.displayHeatmap();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write random observations to a observation data file.
	 */
	public static void writeToFile() {
		try {
			long seed = System.currentTimeMillis();
			Random rand = new Random(seed);
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));

			for (long t = 0; t < 8; t++)
				for (int i = 0; i < 16; i++) {
					double x = min + (max - min) * rand.nextDouble();
					double y = min + (max - min) * rand.nextDouble();
					out.writeObject(new Observation(t, x, y));
				}
			out.writeObject(new Observation()); // to mark EOF
			out.close();
		} catch (IOException e) {
			System.out.println("writing to " + FILENAME + "failed: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Reads data from file and saves into an ArrayList.
	 * 
	 * @param data ArrayList where the file's data will be saved
	 */
	public static void readFromFile(ArrayList<Observation> data) {
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
			Observation obs = (Observation) in.readObject();
			while (!obs.isEOF()) {
				data.add(obs);
				obs = (Observation) in.readObject();
			}
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("reading from " + FILENAME + "failed: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Main method. Runs HeatMapScan program.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		//Write observation data file
		writeToFile();

		//Save file data to ArrayList
		ArrayList<Observation> data = new ArrayList<>();
		readFromFile(data); 

		//Start timer
		long start = System.currentTimeMillis();
		
		//Reduce data
		HeatMapScan heatmap = new HeatMapScan(data);
		int[][] reduction = heatmap.getReduction();

		System.out.println("Reduction result: ");
		printArr(reduction);

		//Scan data
		ArrayList<int[][]> scanResult = heatmap.getScan();
		System.out.println("Scan result: ");
		printList(scanResult);
		
		//End timer and compute runtime
		long end = System.currentTimeMillis();
		long elapsedTime = end - start;
		
		System.out.println("Performed Reduce and Scan on " + data.size() + " elements. Elapsed time: " + elapsedTime + " ms.");

		//Run heatmap animation
		heatmap.displayHeatmap(scanResult);
	}

}
