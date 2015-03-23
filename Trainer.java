import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class Trainer {

	ArrayList<Double> rewards = new ArrayList<Double>();

	ArrayList<Integer> counts = new ArrayList<Integer>();

	int num_of_arms = 5;

	int range = 4; // 0 - 2 has 3 numbers

	public Trainer(){
		
		int total_num_of_combos = (int) Math.pow(range, num_of_arms);
		
		int count = 0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("rewards.txt"));
			
			String line = null;
			while ((line = br.readLine()) != null) {
				
				rewards.add(Double.parseDouble(line.trim()));

			}
			
			br.close();
		} catch (IOException e) {
			// init rewards and count		
			for (int i = 0; i < total_num_of_combos; i++){
				rewards.add(0.0);
			}

		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("counts.txt"));
			
			String line = null;
			while ((line = br.readLine()) != null) {
				counts.add(Integer.parseInt(line.trim()));


			}
			
			br.close();
		} catch (IOException e) {
			// init rewards and count		

			for (int i = 0; i < total_num_of_combos; i++){
				counts.add(0);
			}
		}
		
			
		if (counts.size() != total_num_of_combos){
			for (int i = 0; i < total_num_of_combos; i++){
				counts.add(0);
			}			
		}		
		
		if (rewards.size() != total_num_of_combos){
			for (int i = 0; i < total_num_of_combos; i++){
				rewards.add(0.0);
			}			
		}
				
	}
	
	public int pull_arm(){
		//  choose an arm to pull
		
		double[] probs = new double[counts.size()];
		
		int weight = 0; // by default set to size - 1	

		int total_counts = 0;

		for (int m = 0; m < counts.size(); m++){
			
			total_counts = total_counts + counts.get(m);
			
			if (counts.get(m) == 0){
				return m;
			}
		}
		
		
			// all tested before
			
			double[] ucb_values = new double[counts.size()];
				
			for (int i = 0; i < rewards.size(); i++){
				double bonus = Math.pow((2 * Math.log(total_counts)) / counts.get(i), 0.5);
				
				ucb_values[i] = rewards.get(i) + bonus;
			}			
			int max_weight = 0;
		
			double max_reward = -1.0;
		
			for (int o = 0; o < rewards.size(); o++){
				if (rewards.get(o) > max_reward){
					max_reward = rewards.get(o);
					weight = o;
				}
			}
					
	//System.out.println(weight);
						

		return weight;
	}

	public int pickMove(State old_state, int[][] legalMoves){
/*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
*/
				
		NewState original = convertState(old_state);
		
		int[][] field = original.getField();
		int nextPiece = original.getNextPiece();
		int top[] = original.getTop();	
		int turn = original.getTurnNumber();
		
		double max_reward = -1.0;
				
		int move = 0;		

		int weight = pull_arm();
		
		double max_row = 0.0;
		
				
		///		
		
		int converted_weight = Integer.parseInt(convert(weight, range));


		
		int[] array_weights = new int[num_of_arms];
		
		String temp = Integer.toString(converted_weight);

		int counter = 0;

		for (int i = temp.length() - 1; i >= 0; i--)
		{
			
			
		    array_weights[num_of_arms - 1 - counter] = temp.charAt(i) - '0';
			counter++;
		}
	
		int[][][] allLegalMoves = original.allLegalMoves();
	
				
		for (int i=0; i<legalMoves.length; i++) {		
		
			NewState s = new NewState(field, nextPiece, top, turn);
			s.newMove(i);
			
			double row_cleared = s.getRowsCleared();
						
			
			
			int[][] s_field = s.getField();
			int s_nextPiece = s.getNextPiece();
			int s_top[] = s.getTop();	
			int s_turn = s.getTurnNumber();

			int next_reward = 0;

			for (int j = 0; j < 7; j++){
				for (int o = 0; o < allLegalMoves[j].length; o++){

					NewState second_state = new NewState(s_field, j, s_top, s_turn);
					
					second_state.newMove(o);	
					
					if (next_reward < second_state.getRowsCleared()){
						next_reward = second_state.getRowsCleared();
					}
				}
			}
			
			// clearing in future is better
			// give a smaller weightage
			row_cleared += (next_reward * 0.5);
						
			// avoid having weight with pure 0 value
			
			double reward = 0.0;
			
			// log it to smooth it out
			
			reward += Math.pow(array_weights[0] + 0.1, 2) * ( 1.0 / (adjacentHeightDifferenceSquare(s) + 0.1) );
			
			reward += Math.pow(array_weights[1] + 0.1, 2) * (1.0 / (averageHeight(s) + 0.1) );
						
			reward += Math.pow(array_weights[2] + 0.1, 2) * (1.0 / (getMaxHeight(s) + 0.1) );
								
			reward += Math.pow(array_weights[3] + 0.1, 2) * (compactness(s) + 0.1) ;
				
			reward += Math.pow(array_weights[4] + 0.1, 2) * (percent_area_below_max_height(s) + 0.1) ;
							
			//reward += Math.pow(array_weights[4] + 0.1, 2) * (1.0 / (max_min_diff(s) + 0.1) );
			
			//reward += Math.pow(array_weights[5] + 0.1, 2) * (1.0 / (height_diff_from_max(s) + 0.1) );
			
			
			//reward += Math.pow(array_weights[2] + 0.1, 2) * (1.0 / (getNumHoles(s) + 0.1) );
			
								
			if (row_cleared > max_row){
				// this is a better move as we clear more row
				
				max_row = row_cleared * 1.0;
				
				max_reward = reward;
				
				move = i;
				
			} else if (row_cleared == max_row) {
				// both clear same number of rows // see which gives a better reward
				
				if (reward > max_reward){
					max_reward = reward;
					move = i;
				}			
			}
			
			// the bigger the reward the better
		}
		
		System.out.println(weight);
		
		counts.set(weight, counts.get(weight) + 1);
				
		int n = counts.get(weight);
				
		double new_reward = ( (n - 1) / (n * 1.0)) * rewards.get(weight) + (1.0 / n) * (max_row * 1.0);
		
		rewards.set(weight, new_reward);
		
		
		String getWriteString = getWriteString(rewards);
		try{
			File file = new File("rewards.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String getWriteAnotherString = getWriteAnotherString(counts);
		
		try{
			File file = new File("counts.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteAnotherString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return move;
	}
	
	
	private double height_diff_from_max(NewState s){
		
		int count = 0;
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		int max_height = getMaxHeight(s);
		
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
		
		//System.out.println((10 * max_height) - sum);
		
		
		return ( (10 * max_height) - sum);
	}
	
	private double averageHeight(NewState s){
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
				
		return (sum / 10.0);
	}
	
	private double adjacentHeightDifferenceSquare(NewState s){
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length - 1; i++) {
			sum = sum + Math.pow(colHeights[i] - colHeights[i + 1], 2);
		}
				
		return sum;
	}
	
	private double percent_area_below_max_height(NewState s){
		int[][] field = s.getField();
		
		int max_height = getMaxHeight(s);
				
		int count = 0;
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] != 0) {
					count++;
				}
			}
		}
		
		if (count == 0){
			return 1.5; // everything below area // no blocks
		}		
		
		return ( count / (getMaxHeight(s) * 10.0) );
	}
	
	private double compactness(NewState s){
		int[][] field = s.getField();
		
		int[] colHeights = s.getTop();
		
		double sum = 0.0; // sum the area occupied by the blocks
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
						
		int count = 0;
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] != 0) {
					count++;
				}
			}
		}
		
		
		if (count == 0){
			return 1.5; // everything below area // no blocks
		}
		
						
		return ( count / (sum) );
	}
	
	private int getNumHoles(NewState s) {
		int numHoles = 0;
		int[][] field = s.getField();
		
		// lower holes are worse
		for (int i=field.length - 1; i>=0; i--) {
			for (int j=field[0].length - 1; j>=0; j--) {
				if (field[i][j] == 0) {
					if (isHole(s, field, i, j)) {
						numHoles = numHoles + 1;
					}
				}
			}
		}
		
		// return numHoles;
		return 0;
	}

	private boolean isHole(NewState s, int[][] field, int i, int j) {
		int count = 0;
		
		if (i-1 < 0 || j-1 < 0 || field[i-1][j-1] != 0) {
			count++;
		}
		
		if (i-1 < 0 || field[i-1][j] != 0) {
			count++;
		}
		
		if (i-1 < 0 || j+1 >= s.COLS || field[i-1][j+1] != 0) {
			count++;
		}
		
		if (j-1 < 0 || field[i][j-1] != 0) {
			count++;
		}
		
		if (j+1 >= s.COLS || field[i][j+1] != 0) {
			count++;
		}
		
		if (i+1 >= s.ROWS || j+1 >= s.COLS || field[i+1][j+1] != 0) {
			count++;
		}
		
		if (i+1 >= s.ROWS || field[i+1][j] != 0) {
			count++;
		}
		
		if (i+1 >= s.ROWS || j+1 >= s.COLS || field[i+1][j+1] != 0) {
			count++;
		}
		
		
		if (count == 8) {
			return true;
		}
		
		return false;
	}
	
	
	public int getMaxHeight(NewState s){
		int[] colHeights = s.getTop();
		
		int max = -1;
		
		for (int i=0; i<colHeights.length; i++) {
			if (colHeights[i] > max) {
				max = colHeights[i]; 
			}
		}
		
		return max;
	}
	
	public int max_min_diff(NewState s){
		
		int[] colHeights = s.getTop();
		
		int min = 100;
		int max = -1;
		
		for (int i=0; i<colHeights.length; i++) {
			if (colHeights[i] > max) {
				max = colHeights[i]; 
			}
			
			if (colHeights[i] < min) {
				min = colHeights[i]; 
			}
		}
		
		return max-min;
	}
	
	public static String convert(int number, int base)
	{
	    int quotient = number / base;
	    int remainder = number % base;

	    if(quotient == 0) // base case
	    {
	        return Integer.toString(remainder);      
	    }
	    else
	    {
	        return convert(quotient, base) + Integer.toString(remainder);
	    }            
	}
	
	private NewState convertState(State curState) {
		NewState s = new NewState(curState.getField(), curState.getNextPiece(), curState.getTop(), curState.getTurnNumber());
		return s;
	}
	
	
	private static String getWriteString(ArrayList<Double> arr) {
		String writeString = "";
		
		for (int i=0; i<arr.size(); i++) {
			writeString = writeString + arr.get(i) + "\n";
		}
				
		return writeString;
	}
	
	private static String getWriteAnotherString(ArrayList<Integer> arr) {
		String writeString = "";
		
		for (int i=0; i<arr.size(); i++) {
			writeString = writeString + arr.get(i) + "\n";
		}
				
		return writeString;
	}
	
}