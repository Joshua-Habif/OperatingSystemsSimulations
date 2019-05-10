import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
//uncomment to debug certain parts of this program
//import java.util.Arrays; only used for debugging
import java.util.Collections;

/**
 * 
 * @author Joshua Habif 
 * Please refer any bugs or questions to jh5653@nyu.edu
 * 
 * Last modified on May 07, 2019.
 * 
 * This program simulates a demand paging handler in an operating system. 
 * The input is given as command line arguments: in the following order:
 * the machine size in words; the page size in words; the size of each process, i.e., the references are to virtual addresses;
 * the ‘‘job mix’’, which determines A, B, and C, as described below; the number of references for each process;
 * the replacement algorithm, FIFO, RANDOM, or LRU.
 *
 */

public class Pager {
	
	private static BufferedWriter output;
	private static BufferedReader randomReader;
	private static final int FIRSTWORD = 111;
	private static int machineSize, pageSize, processSize, jobMix, numOfRef;
	private static int[] evictions;
	private static int[] pageFaults;
	private static int[] runningTime;
	private static int[] runningSum;
	private static double[][] jobProbTable;
	private static int[] nextWords;
	private static boolean[] touched;
	private static String reAlgo;
	private static ArrayList<Pair> frameTable;
	
	
	public static void main(String[] args) throws IOException {
		
		doIO(args);
		doFrameTable();
		determineProb();
		
		pager();
		
		//prepare final output
		echoIN();
		summarize();
		output.flush();
		
		//uncomment for debugging
		//System.out.println(Arrays.deepToString(jobProbTable));
		
		//prevent leak
		output.close();
	}
	
	public static void pager() throws IOException{
	
		int numberOfProc;
		if(jobMix == 1) {
			numberOfProc = 1;
			nextWords  = new int[] {-1};
			touched = new boolean[] {false};
			evictions = new int[] {0};
			pageFaults = new int[] {0};
			runningTime = new int[] {0};
			runningSum = new int[] {0};
		}
		else {
			numberOfProc = 4;
			nextWords  = new int[] {-1,-1,-1,-1};
			touched = new boolean[] {false, false, false, false};
			evictions = new int[] {0,0,0,0};
			pageFaults = new int[] {0,0,0,0};
			runningTime = new int[] {0,0,0,0};
			runningSum = new int[] {0,0,0,0};
		}
		
		int curProcess = 0;
		int clock=0;
		int add = 0;
		for(int counter = 0; counter < (numberOfProc*numOfRef); counter += add) {
			
			if(curProcess == 4)
				curProcess = 0;
			
			add=0;
			
			int curWord = -1;
			
			for(int q=0; ((q<3) && (runningTime[curProcess] < numOfRef)) ; q++) {	
				
				if(!touched[curProcess]) {
					touched[curProcess] = true;
					curWord = ((FIRSTWORD*(curProcess+1)) % processSize);
				}
				else {
					curWord = nextWords[curProcess];
				}
				
				//uncomment for debugging
				//output.write(((curProcess+1) + " references word " + curWord + "\n"));
				
				//check if word is in table
				
				Pair curPair = new Pair(curProcess, curWord/pageSize);

				if(frameTable.contains(curPair)) {
					//hit
					for(Pair p : frameTable) {
						if(p.equals(curPair))
							p.setLastUsed(clock); 
					}
				}
				else {
					//fault
					pageFaults[curProcess]++;
					//try to find a free frame
					Pair freeFrame = getFreeFrame();
					if(freeFrame != null) {
						freeFrame.setX(curProcess);
						freeFrame.setY(curWord/pageSize);
						freeFrame.setLastUsed(clock);
						freeFrame.setLoadTime(clock);
					}
					else {
						//no free frame was found, must evict a page
						Pair evict = null;
						if(reAlgo.equals("lru")) {
							evict = LRU();
						}
						else if(reAlgo.equals("fifo")) {
							evict = FIFO();
						}
						else {
							evict = random();
						}
						evictions[evict.getX()]++;
						runningSum[evict.getX()] += (clock - evict.getLoadTime());
						//use the room made by the eviction to load a new page
						int evictIndex = -1;
						for(int i=frameTable.size()-1; i>=0; i--) {
							if(frameTable.get(i) == evict) {
								evictIndex = i;
								break;
							}
						}
						
						//uncomment for debug 
						//output.write("evicting page " + evict.getY() + 
						//		" of " + evict.getX() + " from frame " + evictIndex+"\n");
						
						frameTable.set(evictIndex, curPair);
						curPair.setLoadTime(clock);
						curPair.setLastUsed(clock); 
						
					}
				}
				runningTime[curProcess]++;
				clock++;
				add++;
				nextWords[curProcess] = fetchWord(curWord, curProcess);
			}

			if(numberOfProc != 1)
				curProcess++;
		}
		
	}	
	
	public static Pair LRU() {

		//clone the frame table
		ArrayList<Pair> sorted = new ArrayList<Pair>();
		for(int i=0; i<frameTable.size(); i++) {
			//uncomment to create a deep copy
			/*Pair deepClone = new Pair(frameTable.get(i).getX(),
									  frameTable.get(i).getY());
			deepClone.setLastUsed(frameTable.get(i).getLastUsed());*/
			
			sorted.add(frameTable.get(i));
		}

		LRUComparator comp = new LRUComparator();
		Collections.sort(sorted, comp);
		//uncomment for debug
		//System.out.println("frameTable\t" + frameTable.toString());
		//System.out.println("sorted:\t" + sorted.toString());
		return sorted.get(0);
	}
	
	public static Pair FIFO() {
		
		//clone the frame table
		ArrayList<Pair> sorted = new ArrayList<Pair>();
		for(int i=0; i<frameTable.size(); i++) {
			//uncomment to create a deep copy
			/*Pair deepClone = new Pair(frameTable.get(i).getX(),
									  frameTable.get(i).getY());
			deepClone.setLastUsed(frameTable.get(i).getLastUsed());*/
			sorted.add(frameTable.get(i));
		}
		
		FIFOComparator comp = new FIFOComparator();
		Collections.sort(sorted, comp);
		
		return sorted.get(0);
		
	}
	
	
	public static Pair random() throws IOException {
		//uncomment to debug
		int randomEviction = randomOS();
		//output.write(randomEviction+"\n");
		//System.out.println((randomEviction % (machineSize/pageSize)));
		return frameTable.get((randomEviction % (machineSize/pageSize)));
		//return frameTable.get((randomOS() % (machineSize/pageSize)));
	}
	
	public static Pair getFreeFrame() {

		for(int i = frameTable.size(); i>0; i--) {
			if(frameTable.get(i-1).getX() == -1) {			
				return frameTable.get(i-1); 
			}	
		}
		return null;
	}

	
	public static void doFrameTable() {
		//prepare frameTable
		frameTable = new ArrayList<Pair>();
		int size = machineSize/pageSize;
		for(int i=0; i<size; i++) {frameTable.add(new Pair());}
	}
	
	public static void doIO(String[] args) throws IOException {
		
		if(args.length < 6)
			throw new IllegalArgumentException();
		
		//process command line arguments
		machineSize = Integer.parseInt(args[0]);
		pageSize = Integer.parseInt(args[1]);
		processSize = Integer.parseInt(args[2]);
		jobMix = Integer.parseInt(args[3]);
		numOfRef = Integer.parseInt(args[4]);
		reAlgo = args[5];
	
		//we will use BufferedReader for better performance
		output = new BufferedWriter(new OutputStreamWriter(System.out, "ASCII"), 4096);
		
		//prepare randomOs file for reading
		File random = new File("random-numbers.txt");
		randomReader = new BufferedReader(new FileReader(random));
	}
	
	public static int fetchWord(int curWord, int process) throws IOException {
		
		int r = randomOS();
		//uncomment for debugging
		//output.write("cur word is :" + curWord+"\n");
		//output.write("random number is " + r + "\n");
		//uncomment for debugging
		//System.out.println((process+1) + " uses random number " + r);
		double y = r / (Integer.MAX_VALUE + 1d);
		
		if(y < jobProbTable[process][0]) {
			//do case 1
			return ((curWord+1) % processSize);
		}
		else if(y < (jobProbTable[process][0]+jobProbTable[process][1])) {
			//do case 2
			return ((curWord-5 + processSize) % processSize); 
		}
		else if(y < (jobProbTable[process][0]+jobProbTable[process][1]+jobProbTable[process][2])) {
			//do case 3
			return ((curWord+4) % processSize);
		}
		else {
			//do case 4 
			return (randomOS() % processSize);
		}
	}
	
	public static void determineProb() {
		if(jobMix == 1) {
			jobProbTable = new double[][] {{1,0,0}};
			
		}
		else if(jobMix == 2) {
			jobProbTable = new double[][] {{1,0,0},
											{1,0,0},
											{1,0,0},
											{1,0,0}};
		}
		else if(jobMix == 3) {
			jobProbTable = new double[][] {{0,0,0},
											{0,0,0},
											{0,0,0},
											{0,0,0}};
		}
		else {
			jobProbTable = new double[][]{
							{0.75, 0.25, 0},
							{0.75, 0, 0.25},
							{0.75, 0.125, 0.125},
							{0.5, 0.125, 0.125}};
		}
	}

	public static int randomOS() throws IOException {
		int random = Integer.parseInt(randomReader.readLine());
		//uncomment for debugging
		//System.out.println(random);
		return random;
		
	}
	
	private static void echoIN() throws IOException {
		
		output.write("The machine size is " + machineSize + "\n"
				+ "The page size is " + pageSize + "\n"
				+ "The process size is " + processSize + "\n"
				+ "The job mix number is " + jobMix + "\n"
				+ "The number of references per process is " +  numOfRef + "\n"
				+ "The replacement algorithm is " + reAlgo + "\n"
				+ "The level of debugging output is 0");
	}	
	
	public static void summarize() throws IOException {
		NumberFormat formatter = new DecimalFormat("#0.00");  
		if(jobMix == 1) {
			if(evictions[0] != 0) {
				output.write("\n\nProcess 1 had " + pageFaults[0] + " page faults and "
							+ formatter.format(((double)runningSum[0]/evictions[0]))
							+ " average residency.\n");
				output.write("\nThe total number of faults is " + pageFaults[0] + 
							" and the overall average residency is " 
							+ formatter.format(((double)runningSum[0]/evictions[0])) + ".\n");
			}
			else {
				output.write("\n\nProcess 1 had " + pageFaults[0] + " page faults."
						+ "\n\tWith no evictions, the average residence is undefined.\n");
				output.write("\nThe total number of faults is " + pageFaults[0] + 
						".\n\tWith no evictions, the overall average residence is undefined.\n");
			}
		}
		else {
			int totalEvictions = 0;
			int totalRunningSums = 0;
			int faults = 0;
			for(int i=0; i<4; i++) {
				if(evictions[i] != 0) {
					output.write("\n\nProcess " + (i+1) +" had " + pageFaults[i] + 
							" page faults and "
							+ formatter.format((double)runningSum[i]/evictions[i]) + 
							" average residency.\n");
					totalRunningSums += runningSum[i];
					totalEvictions += evictions[i];
				}
				else {
					output.write("\n\nProcess " + (i+1) +" had " + pageFaults[i] + 
							" page faults.\n" + "\tWith no evictions, the average "
							 + "residence is undefined.\n");
				}
				faults += pageFaults[i];
			}	
			if(totalEvictions != 0) {
				output.write("\nThe total number of faults is " + faults + 
						" and the overall average residency is " 
						+ formatter.format(totalRunningSums*1.0/totalEvictions)  + ".\n");
			}
			else {
				output.write("\nThe total number of faults is " + faults + 
						".\n\tWith no evictions, the overall average "
						+ "residence is undefined.");
			}
		}
	}
}
