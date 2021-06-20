import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.lang.Math;

public class GeneralScan {
	private final static int ROOT = 0;
	
	//subtree must have less than or equal to this number of leaf nodes to run the direct Schwartz loop during compute()
	public static final int THRESHOLD = 16;
	
	private final int DIM = 2; // dimensions of tally grids
	private int n; // n is size of data, n-1 is size of interior
	private int p; //number of processes
	private int height;
	private boolean reduced;

	private ArrayList<Observation> data;
	private ArrayList<int[][]> interior; //Tally objects in tree's interior
	private ArrayList<int[][]> output; //Output of tally objects after Scan
	private static ForkJoinPool pool;

	/**
	 * Constructor. Initializes variables.
	 * 
	 * @param raw The original observation data
	 */
	public GeneralScan(ArrayList<Observation> raw) {
		n = raw.size();
		height = (int) Math.ceil(log2(n));

		if (1 << height != n)
			throw new IllegalArgumentException(
					"Number of observations must be a power of 2. Current number of observations: " + data.size());

		data = raw;
		
		//Initialize interior array with O(P) space
		p = n / THRESHOLD;
		interior = new ArrayList<int[][]>();
		for(int i = 0; i <= 2*p ; i ++)
			interior.add(init());
		
		output = new ArrayList<int[][]>(n);
		pool = new ForkJoinPool();
		reduced = false;
	}

	/**
	 * Checks if data has been reduced, then calls a ForkJoinPool 
	 * to compute a scan on the data.
	 * 
	 * @return ArrayList of tally objects after scan
	 */
	public ArrayList<int[][]> getScan() {
		if (!reduced)
			getReduction();

		for (int i = 0; i < data.size(); i++)
			output.add(init());

		pool.invoke(new RecursiveScan(ROOT, init()));
		return output;
	}

	/**
	 * Performs scan using Schwartz's loop.
	 * 
	 * @param i Index corresponding to location in binary tree 
	 * @param tallyPrior The tally object before the one at
	 * index i
	 */
	protected void scan(int i, int[][] tallyPrior) {
		int[][] tally = tallyPrior;
		int rm = rightmost(i);

		for (int j = leftmost(i); j <= rm; j++) {
			tally = accumulate(tally, value(j));
			output.set(j - (n - 1), tally);
		}
	}

	/**
	 * Calls a ForkJoinPool to compute a reduction on the data.
	 * 
	 * @return A tally object containing the reduction result
	 */
	public int[][] getReduction() {
		if (!reduced) {
			pool.invoke(new RecursiveReduction(ROOT));
			reduced = true;
		}
		return value(ROOT);
	}

	/**
	 * Performs a reduction using Schwartz's loop.
	 * 
	 * @param i Index corresponding to location in binary tree
	 * @return True after performing reduction
	 */
	protected boolean reduce(int i) {
		int[][] tally = init();
		int rm = rightmost(i);
		
		for (int j = leftmost(i); j <= rm; j++)
			tally = accumulate(tally, value(j));

		interior.set(i, tally);
		return true;
	}

	/**
	 * Initializes a new tally object.
	 * 
	 * @return New tally object
	 */
	protected int[][] init() {
		int[][] seedEl = { { 0, 0 }, { 0, 0 } };
		return seedEl;
	}

	/**
	 * Determines which bucket the observation goes into.
	 * 
	 * @param observation The observation to sort into a bucket
	 * @return A tally object with a 1 for the observation in the 
	 * appropriate bucket
	 */
	protected int[][] prepare(Observation observation) {
		int[][] tally = init();

		if (observation.x < 0 && observation.y < 0)
			tally[1][0] = 1;
		if (observation.x < 0 && observation.y >= 0)
			tally[0][0] = 1;
		if (observation.x >= 0 && observation.y >= 0)
			tally[0][1] = 1;
		if (observation.x >= 0 && observation.y < 0)
			tally[1][1] = 1;

		return tally;
	}

	/**
	 * Combines two tally objects.
	 * 
	 * @param tally1 First tally object
	 * @param tally2 Second tally object
	 * @return One tally object containing the sum of the two parameters
	 */
	protected int[][] combine(int[][] tally1, int[][] tally2) {
		int[][] combination = new int[DIM][DIM];
		for (int i = 0; i < DIM; i++) {
			for (int j = 0; j < DIM; j++) {
				combination[i][j] = tally1[i][j] + tally2[i][j];
			}
		}
		return combination;
	}

	/**
	 * Combine and replace left tally with result.
	 * 
	 * @param accumulator Tally on the left that will also
	 * be used to save combination result
	 * @param right Tally on the right
	 * @return One tally object containing the sum of the two parameters
	 */
	protected int[][] accumulate(int[][] accumulator, int[][] right) {
		return combine(accumulator, right);
	}

	protected int size() {
		return (n - 1) + n;
	}

	protected int[][] value(int i) {
		if (i < n - 1)
			return interior.get(i);
		else
			return prepare(data.get(i - (n - 1)));
	}

	/**
	 * Calculates log2 n. loga b = log10 b / log10 a, so log2 n = log10 n / log10 2.
	 * 
	 * @param n Number to find log2 of
	 * @return log2(n)
	 */
	protected static int log2(int n) {
		return (int) (Math.log10(n) / Math.log10(2));
	}

	protected int leftmost(int i) {
		while (!isLeaf(i))
			i = left(i);
		return i;
	}

	protected int rightmost(int i) {
		while (!isLeaf(i))
			i = right(i);
		return i;
	}

	protected int parent(int i) {
		return (i - 1) / 2;
	}

	protected int left(int i) {
		return i * 2 + 1;
	}

	protected int right(int i) {
		return left(i) + 1;
	}

	protected boolean isLeaf(int i) {
		return right(i) >= size();
	}

	/**
	 * Counts the number of leaves in a subtree.
	 * 
	 * @param i Index corresponding to location of the starting node of subtree
	 * @return Number of leaves
	 */
	protected int countLeaves(int i) {
		if (isLeaf(i))
			return 1;
		else
			return countLeaves(left(i)) + countLeaves(right(i));
	}
	
	static void printArr(int[][] arr) {
		for (int[] row : arr)
			System.out.println(Arrays.toString(row));
		
		System.out.println(); //space between each tally object
	}

	static void printList(ArrayList<int[][]> list) {
		for (int[][] arr2D : list) 
			printArr(arr2D);
	}

	/**
	 * Runs a reduction using a ForkJoinPool.
	 */
	class RecursiveReduction extends RecursiveAction {
		private int i;
		private static final long serialVersionUID = 1L;

		public RecursiveReduction(int i) {
			this.i = i;
		}

		@Override
		protected void compute() {
			// If the portion of the leaves in the subtree is under the threshold run Schwartz loop
			if (countLeaves(i) <= THRESHOLD) {
				reduce(i);
				return;
			}
			invokeAll(new RecursiveReduction(left(i)), new RecursiveReduction(right(i)));
			interior.set(i, combine(value(left(i)), value(right(i))));
		}
	}

	/**
	 * Runs a scan using a ForkJoinPool.
	 */
	class RecursiveScan extends RecursiveAction {
		private int i;
		private int[][] tallyPrior;
		private static final long serialVersionUID = 1L;

		public RecursiveScan(int i, int[][] tallyPrior) {
			this.i = i;
			this.tallyPrior = tallyPrior;
		}

		@Override
		protected void compute() {
			// If the portion of the leaves in the subtree is under the threshold run Schwartz loop
			if (countLeaves(i) <= THRESHOLD) {
				scan(i, tallyPrior);
				return;
			} else {
				invokeAll(new RecursiveScan(left(i), tallyPrior),
						new RecursiveScan(right(i), combine(tallyPrior, value(left(i)))));
			}
		}
	}
}
