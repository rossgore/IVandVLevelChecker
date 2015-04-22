import java.util.*;
import java.io.*;

public class LevelOneValidityCheck {
	private String inputFilename;
	private String outputFilename;

	private static final int ID = 0;
	private static final int OUTCOME = 1;
	private static final int OTHER_COLS = 2;

	private int samples = 0; 
	private int variables = 0;

	private double [][] dataset;
	private String [][] datalabels;
	private String [] varlabels;
	
	int [] colOfSVAlwaysVars;
	int [] svAlwaysOperator;
	double [] svAlwaysVals;
	
	int [] colOfSVNeverVars;
	int [] svNeverOperator;
	double [] svNeverVals;
	
	int [] colOfSPAlwaysLeftVars;
	int [] spAlwaysOperator;
	int [] colOfSPAlwaysRightVars;
	
	int [] colOfSPNeverLeftVars;
	int [] spNeverOperator;
	int [] colOfSPNeverRightVars;
	
	boolean [] svAlwaysResults;
	boolean [] svNeverResults;
	boolean [] spAlwaysResults;
	boolean [] spNeverResults;
	

	private File inputFile;

	public LevelOneValidityCheck (String pInputFilename, ArrayList<String> pSVAlwaysToTest, ArrayList<String> pSVNeverToTest, ArrayList<String> pSPAlwaysToTest, ArrayList<String> pSPNeverToTest){
		inputFile = new File(pInputFilename);
		outputFilename = pInputFilename.replace(".csv","")+"-level-one-report.csv";
		this.setInputFilename(pInputFilename);
		this.setNumberOfVariables(computeNumberOfVariables());
		this.setNumberOfSamples(computeNumberOfSamples());
		copyData(samples, variables);
		
		// get pieces for sv always
		colOfSVAlwaysVars = parseLeftVarForIndex(pSVAlwaysToTest);
		svAlwaysOperator  = parsePredicateForOperator(pSVAlwaysToTest);
		svAlwaysVals   = parseRightVarForValue(pSVAlwaysToTest);
		
		// get pieces for sv never
		colOfSVNeverVars = parseLeftVarForIndex(pSVNeverToTest);
		svNeverOperator  = parsePredicateForOperator(pSVNeverToTest);
		svNeverVals   = parseRightVarForValue(pSVNeverToTest);
		
		// get results for sv 
		svAlwaysResults = testSVAlwaysPreds(colOfSVAlwaysVars, svAlwaysOperator, svAlwaysVals);
		svNeverResults = testSVNeverPreds(colOfSVNeverVars, svNeverOperator, svNeverVals);
		
		// get pieces for sp always
		colOfSPAlwaysLeftVars = parseLeftVarForIndex(pSPAlwaysToTest);
		spAlwaysOperator  = parsePredicateForOperator(pSPAlwaysToTest);
		colOfSPAlwaysRightVars   = parseRightVarForIndex(pSPAlwaysToTest);
		
		// get pieces for sp never
		colOfSPNeverLeftVars = parseLeftVarForIndex(pSPNeverToTest);
		spNeverOperator  = parsePredicateForOperator(pSPNeverToTest);
		colOfSPNeverRightVars   = parseRightVarForIndex(pSPNeverToTest);
		
		// get results for sp 
		spAlwaysResults = testSPAlwaysPreds(colOfSPAlwaysLeftVars, spAlwaysOperator, colOfSPAlwaysRightVars);
		spNeverResults = testSPNeverPreds(colOfSPNeverLeftVars, spNeverOperator, colOfSPNeverRightVars);
		
		writeReport(pSVAlwaysToTest, pSVNeverToTest, pSPAlwaysToTest, pSPNeverToTest);
		
	}
	
	public void writeReport(ArrayList<String> svAlwaysConds, ArrayList<String> svNeverConds, ArrayList<String> spAlwaysConds, ArrayList<String> spNeverConds){
		String output = "";
		for (int i = 0; i<svAlwaysConds.size(); i++)
		{
			if (svAlwaysResults[i]){
				output+="The requirement is met: ALWAYS ["+svAlwaysConds.get(i)+"]\n";
			}
			else
			{
				output+="The requirement is NOT met: ALWAYS ["+svAlwaysConds.get(i)+"]\n";
			}
			
		}
		for (int i=0; i<svNeverConds.size(); i++)
		{
			if (svNeverResults[i]){
				output+="The requirement is met: NEVER ["+svNeverConds.get(i)+"]\n";
			}
			else
			{
				output+="The requirement is NOT met: NEVER ["+svNeverConds.get(i)+"]\n";
			}
		}
		for (int i = 0; i<spAlwaysConds.size(); i++)
		{
			if (spAlwaysResults[i]){
				output+="The requirement is met: ALWAYS ["+spAlwaysConds.get(i)+"]\n";
			}
			else
			{
				output+="The requirement is NOT met: ALWAYS ["+spAlwaysConds.get(i)+"]\n";
			}
			
		}
		for (int i=0; i<spNeverConds.size(); i++)
		{
			if (spNeverResults[i]){
				output+="The requirement is met: NEVER ["+spNeverConds.get(i)+"]\n";
			}
			else
			{
				output+="The requirement is NOT met: NEVER ["+spNeverConds.get(i)+"]\n";
			}
		}
		try
		{
			PrintWriter out = new PrintWriter(outputFilename);
			out.println(output);
			out.close();
			System.out.println("Level One IV&V Report compiled successfully in "+outputFilename);
			
		}
		catch(Exception e)
		{
			System.out.println("Error in compiling Level One IV&V Report. No report was generated.");
		}
		
	}
	
	public boolean [] testSVAlwaysPreds(int [] colsOfVars, int [] operators, double [] valuesToTestAgainst)
	{
		boolean [] svResults = new boolean[colsOfVars.length];
		for (int curIndexInList=0; curIndexInList<svResults.length; curIndexInList++)
		{
			//go through dataset for each var col index given
			int indexOfCurVarCol = colsOfVars[curIndexInList];
			int currentOp = operators[curIndexInList];
			svResults[curIndexInList] = true;
			if (currentOp == StatDebugGUI.GREATER_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] && (dataset[i][indexOfCurVarCol] > valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.GREATER_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] && (dataset[i][indexOfCurVarCol] >= valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.EQUAL)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] && (dataset[i][indexOfCurVarCol] == valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] && (dataset[i][indexOfCurVarCol] <= valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] && (dataset[i][indexOfCurVarCol] < valuesToTestAgainst[curIndexInList]);
				}
			}
		}	
		return svResults;
	}
	
	public boolean [] testSVNeverPreds(int [] colsOfVars, int [] operators, double [] valuesToTestAgainst)
	{
		boolean [] svResults = new boolean[colsOfVars.length];
		for (int curIndexInList=0; curIndexInList<svResults.length; curIndexInList++)
		{
			//go through dataset for each var col index given
			int indexOfCurVarCol = colsOfVars[curIndexInList];
			int currentOp = operators[curIndexInList];
			svResults[curIndexInList] = false;
			if (currentOp == StatDebugGUI.GREATER_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] || (dataset[i][indexOfCurVarCol] > valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.GREATER_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] || (dataset[i][indexOfCurVarCol] >= valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.EQUAL)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] || (dataset[i][indexOfCurVarCol] == valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] || (dataset[i][indexOfCurVarCol] <= valuesToTestAgainst[curIndexInList]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					svResults[curIndexInList] = svResults[curIndexInList] || (dataset[i][indexOfCurVarCol] < valuesToTestAgainst[curIndexInList]);
					//if (svResults[curIndexInList]) System.out.println((dataset[i][indexOfCurVarCol] +" <= ("+StatDebugGUI.LESS_THAN+") "+ valuesToTestAgainst[curIndexInList]));
					System.out.println((dataset[i][indexOfCurVarCol] +" <= ("+StatDebugGUI.LESS_THAN+") "+ valuesToTestAgainst[curIndexInList]));
				}
			}
			// if svResults[i] is true then the NEVER condition was observed and we need to report that is FALSE that the requirement is met
			// if svResults[i] is false then the NEVER condition was not observed and we need report that it is TRUE the requirement is met
		}	
		for (int i=0; i<svResults.length; i++)
		{
			svResults[i] = !svResults[i];
		}
		return svResults;
	}
	
	public boolean [] testSPAlwaysPreds(int [] colsOfLeftVar, int [] operators, int [] colsOfRightVar)
	{
		boolean [] spResults = new boolean[colsOfLeftVar.length];
		for (int curIndexInList=0; curIndexInList<spResults.length; curIndexInList++)
		{
			//go through dataset for each var col index given
			int indexOfCurLeftVarCol = colsOfLeftVar[curIndexInList];
			int currentOp = operators[curIndexInList];
			int indexOfCurRightVarCol = colsOfRightVar[curIndexInList];
			spResults[curIndexInList] = true;
			if (currentOp == StatDebugGUI.GREATER_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] && (dataset[i][indexOfCurLeftVarCol] > dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.GREATER_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] && (dataset[i][indexOfCurLeftVarCol] >= dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.EQUAL)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] && (dataset[i][indexOfCurLeftVarCol] == dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] && (dataset[i][indexOfCurLeftVarCol] <= dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] && (dataset[i][indexOfCurLeftVarCol] < dataset[i][indexOfCurRightVarCol]);
				}
			}
		}	
		return spResults;
	}
	
	public boolean [] testSPNeverPreds(int [] colsOfLeftVar, int [] operators, int [] colsOfRightVar)
	{
		boolean [] spResults = new boolean[colsOfLeftVar.length];
		for (int curIndexInList=0; curIndexInList<spResults.length; curIndexInList++)
		{
			//go through dataset for each var col index given
			int indexOfCurLeftVarCol = colsOfLeftVar[curIndexInList];
			int currentOp = operators[curIndexInList];
			int indexOfCurRightVarCol = colsOfRightVar[curIndexInList];
			spResults[curIndexInList] = false;
			if (currentOp == StatDebugGUI.GREATER_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] || (dataset[i][indexOfCurLeftVarCol] > dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.GREATER_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] || (dataset[i][indexOfCurLeftVarCol] >= dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.EQUAL)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] || (dataset[i][indexOfCurLeftVarCol] == dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN_EQ)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] || (dataset[i][indexOfCurLeftVarCol] <= dataset[i][indexOfCurRightVarCol]);
				}
			}
			else if (currentOp == StatDebugGUI.LESS_THAN)
			{
				for (int i=0; i<dataset.length; i++)
				{
					spResults[curIndexInList] = spResults[curIndexInList] || (dataset[i][indexOfCurLeftVarCol] < dataset[i][indexOfCurRightVarCol]);
				}
			}
		}
		// if spResults[i] is true then the NEVER condition was observed and we need to report that is FALSE that the requirement is met
		// if spResults[i] is false then the NEVER condition was not observed and we need report that it is TRUE the requirement is met
		for (int i=0; i<spResults.length; i++)
		{
			spResults[i] = !spResults[i];
		}	
		return spResults;
	}
	
	public int[] parseLeftVarForIndex(ArrayList<String> predList)
	{
		int [] toReturn = new int[predList.size()];
		for (int i=0; i<predList.size(); i++)
		{
			String s = predList.get(i);
			StringTokenizer tok = new StringTokenizer(s);
			String leftSide = tok.nextToken();
			toReturn[i] = getIndex(leftSide, varlabels);
		}
		return toReturn;
	}
	
	public int[] parsePredicateForOperator(ArrayList<String> predList)
	{
		int [] toReturn = new int[predList.size()];
		for (int i=0; i<predList.size(); i++)
		{
			String s = predList.get(i);
			StringTokenizer tok = new StringTokenizer(s);
			String leftSide = tok.nextToken();
			String op = tok.nextToken();
			toReturn[i] = getIndex(op, StatDebugGUI.SUPPORTED_OPERATORS);
		}
		return toReturn;
	}
	
	public double[] parseRightVarForValue(ArrayList<String> predList)
	{
		double [] toReturn = new double[predList.size()];
		for (int i=0; i<predList.size(); i++)
		{
			String s = predList.get(i);
			StringTokenizer tok = new StringTokenizer(s);
			String leftSide = tok.nextToken();
			String op = tok.nextToken();
			String rightSide = tok.nextToken();
			toReturn[i] = Double.parseDouble(rightSide);
		}
		return toReturn;
	}
	
	public int[] parseRightVarForIndex(ArrayList<String> predList)
	{
		int [] toReturn = new int[predList.size()];
		for (int i=0; i<predList.size(); i++)
		{
			String s = predList.get(i);
			StringTokenizer tok = new StringTokenizer(s);
			String leftSide = tok.nextToken();
			String op = tok.nextToken();
			String rightSide = tok.nextToken();
			toReturn[i] = getIndex(rightSide, varlabels);
		}
		return toReturn;
	}
	

	public int getNumberOfSamples(){
		return samples;
	}

	public void setNumberOfSamples(int pSamples){
		samples = pSamples;
	}

	public int getNumberOfVariables(){
		return variables;
	}

	public void setNumberOfVariables(int pVariables){
		variables = pVariables;
	}

	private void copyData(int pSamples, int pVariables){
		dataset = new double [pSamples][pVariables];
		datalabels = new String [pSamples][OTHER_COLS];
		varlabels = new String [pVariables];

		int lineNumber = 0;
		int tokNumber = 0;
		try{
			Scanner fileScanner = new Scanner(inputFile);

			// grab the headers and copy the variable labels
			String line = fileScanner.nextLine();
			StringTokenizer tok = new StringTokenizer(line, ",");
			// eat the first token its the label header
			String buf = tok.nextToken();
			for (int i=0; i<varlabels.length; i++){
				varlabels[i] = tok.nextToken().replaceAll("\\s+","");
			}

			// grab the remaining rows and copy appropriately
			// first col is an ID and the last col is an outcome
			// everything else is a data sample for a variable

			for (int i=0; i<dataset.length; i++){
				line = fileScanner.nextLine();
				tok = new StringTokenizer(line, ",");
				// first token goes to datalabels [ID];
				datalabels[i][ID] = tok.nextToken();

				// copy inner variable tokens as doubles into the dataset
				for (int j=0; j<dataset[0].length; j++){
					dataset[i][j] = Double.parseDouble(tok.nextToken());
				}

				// last token goes to datalabels [OUTCOME] and we don't care here.
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private int computeNumberOfVariables(){
		// goes through the first line of a csv file
		// number of vars = # of tokens - 2
		// -2 b/c there is 1 traceID and 1 Pass/Fail outcome token
		int varNumber = 0;
		try{
			Scanner fileScanner = new Scanner(inputFile);

			// grab the headers
			String line = fileScanner.nextLine();
			StringTokenizer tok = new StringTokenizer(line, ",");
			while (tok.hasMoreTokens()){
				String buf = tok.nextToken();
				varNumber++;
			}
			fileScanner.close();
		} catch (Exception e){
			e.printStackTrace();
		}	
		return varNumber-OTHER_COLS;
	}

	private int computeNumberOfSamples(){
		// goes through and counts the number of rows in a csv file
		// First line is a header and is ignored.
		int lineNumber = 0;
		try{
			Scanner fileScanner = new Scanner(inputFile);
			// eat the headers
			String line = fileScanner.nextLine();	

			while(fileScanner.hasNextLine()){
				line = fileScanner.nextLine();
				lineNumber++;
			}
			fileScanner.close();
		} catch (Exception e){
			e.printStackTrace();
		}	
		return lineNumber;
	}

	public String getInputFilename(){
		return inputFilename;
	}

	public void setInputFilename(String pInputFilename){
		inputFilename = pInputFilename;
	}

	public String getOutputFilename(){
		return outputFilename;
	}

	public void setOutputFilename(String pOutputFilename){
		outputFilename = pOutputFilename;
	}
	
	public static int getIndex(String s, String[] list)
	{
		for (int i=0; i<list.length; i++)
		{
			if (s.trim().replaceAll("\\s+","").equals(list[i].trim().replaceAll("\\s+","")))
			{
				return i;
			}
		}
		return -1;
	}

}