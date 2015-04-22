import java.util.*;
import java.io.*;
import java.math.*;

public class LevelThreeValidityCheck {
	private String inputFilename;
	private String outputFilename;

	private static final int ID = 0;
	private static final int OUTCOME = 1;
	private static final int OTHER_COLS = 2;

	private int samples = 0; 
	private int variables = 0;
	private static double totalWins = 0;
	
	private static final String WIN = "1";
	private static final String LOSS = "0";

	private double [][] dataset;
	private String [][] datalabels;
	private String [] varlabels;
	
	private File inputFile;
	private String customPredicate;
	private double suspHypothesis;
	private double outcomeCutoff;
	ArrayList<String> varList;

	public LevelThreeValidityCheck (String pInputFilename, double pOutcomeCutoff, String pCustomPredicate, double pSuspHypothesis, ArrayList<String> pVarList){
		inputFile = new File(pInputFilename);
		outputFilename = pInputFilename.replace(".csv","")+"-level-three-report.csv";
		outcomeCutoff = pOutcomeCutoff;
		customPredicate = pCustomPredicate;
		suspHypothesis = pSuspHypothesis;
		varList = pVarList;
		this.setInputFilename(pInputFilename);
		this.setNumberOfVariables(computeNumberOfVariables());
		this.setNumberOfSamples(computeNumberOfSamples());
		copyData(samples, variables, outcomeCutoff);
		double [][] valsForVarsInList = getValues(varList); 
		BigDecimal[] testResults = evalExpr(customPredicate, varList, valsForVarsInList);
		double totalWinningWhereTrue = getWinningCasesWhereTrue(testResults);
		double totalCasesWhereTrue = getTotalCasesWhereTrue(testResults);
		writeReport(suspHypothesis, totalWinningWhereTrue, totalCasesWhereTrue);
	}
	
	public BigDecimal[] evalExpr(String exprToTest, ArrayList<String> listOfVars, double[][] valsForVarsInList)
	{
		BigDecimal [] resultsToReturn = new BigDecimal[samples];
		for (int j=0; j<resultsToReturn.length; j++)
		{
			Expression expr = new Expression(exprToTest);
			for (int i=0; i<valsForVarsInList.length; i++)
			{
				String curVar = listOfVars.get(i);
				expr.with(curVar,""+valsForVarsInList[i][j]);
			}
			resultsToReturn[j] = expr.eval();
		}
		return resultsToReturn;
	}

	public double[][] getValues(ArrayList<String> list)
	{
		int [] indexOfDataCols = new int[list.size()];
		for (int i=0; i<indexOfDataCols.length; i++)
		{
				indexOfDataCols[i] = getIndex(list.get(i), varlabels);
		}
		double toReturn [][] = new double[indexOfDataCols.length][samples];
		for(int i=0; i<indexOfDataCols.length; i++)
		{
			int indexOfData = indexOfDataCols[i];
			for (int j=0; j<samples; j++)
			{
				toReturn[i][j] = dataset[j][indexOfData];
			}
		}
		return toReturn;
	}
	

	public void writeReport(double guess, double totalPredWin, double totalPredTrue){
		double actualSusp = totalPredWin/totalPredTrue;
		double winPrct = (totalPredWin/totalWins)*100;
		String output = "The hypothesisized contribution rate of ["+ customPredicate+"] was "+String.format("%.4f", guess)+"\nThe contribution rate of the predicate actually was "+
			            String.format("%.4f", actualSusp)+"\nIt was true in "+String.format("%.2f", winPrct)+"% of the cases meeting/exceeding the threshold\n";
		try
		{
			PrintWriter out = new PrintWriter(outputFilename);
			out.println(output);
			out.close();
			System.out.println("Level Three IV&V Report compiled successfully in "+outputFilename);
			
		}
		catch(Exception e)
		{
			System.out.println("Error in compiling Level Three IV&V Report. No report was generated.");
		}
		
	}
	
	public double getWinningCasesWhereTrue(BigDecimal [] predTruthList)
	{
		double sum = 0;
		for (int i=0; i<predTruthList.length; i++)
		{
			double predTruth = predTruthList[i].doubleValue();
			double winOccured = Double.parseDouble(datalabels[i][OUTCOME].trim());
			if (predTruth >= 1.0 && winOccured >= 1.0)
			{
				sum++;
			}
		}
		return sum;
	}
	
	public double getTotalCasesWhereTrue(BigDecimal [] predTruthList)
	{
		double sum = 0;
		for (int i=0; i<predTruthList.length; i++)
		{
			sum+= predTruthList[i].doubleValue();
		}
		return sum;
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

	private void copyData(int pSamples, int pVariables, double outcomeCutoff){
		totalWins = 0;
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
				varlabels[i] = tok.nextToken();
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

				// last token goes to datalabels [OUTCOME]
				String score = tok.nextToken();
				double scoreForCompare = Double.parseDouble(score.trim());
				if (scoreForCompare >= outcomeCutoff)
				{
					datalabels[i][OUTCOME] = WIN;
				}
				else
				{
					datalabels[i][OUTCOME] = LOSS;
				}
				totalWins += Double.parseDouble(datalabels[i][OUTCOME].trim());
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private int computeNumberOfVariables(){
		// goes through the first line of a csv file
		// number of vars = # of tokens - 2
		// -2 b/c there is 1 traceID and 1 win/Fail outcome token
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