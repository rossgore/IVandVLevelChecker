import java.util.*;
import java.io.*;
import java.text.Normalizer;

public class LevelTwoValidityCheck {

	private String inputFilename;
	private String outputFilename;
	
	private File matrixFile;

	private static final int ID = 0;
	private static final int OUTCOME = 1;

	private static final int MEAN = 0;
	private static final int STD_DEV = 1;

	private static final int OTHER_COLS = 2;
	private static final int STATS_COLS = 2;

	private static final int PRED_SIZE = 3;

	private static final int LARGE = 2;
	private static final int MED = 1;
	private static final int SMALL = 0;
	
	private static final String WIN = "1";
	private static final String LOSS = "0";

	private static double lowOutcomeCutoff;
	private static double highOutcomeCutoff;
	
	private static double totalWins = 0;


	private int samples = 0; 
	private int variables = 0;
	
	private static boolean [][] inclusionMatrix;

	private double [][] dataset;
	private String [][] datalabels;
	private String [] varlabels;

	private double [][] statsset;

	private double [][][] differenceset;

	private double [][] pass_static_single_preds;
	private double [][] pass_elastic_single_preds;
	

	private double [][] fail_static_single_preds;
	private double [][] fail_elastic_single_preds;

	private double [][][] pass_scalarpair_preds;
	private double [][][] fail_scalarpair_preds;
	
	private double [][][] pass_static_cb_preds;
	private double [][][] fail_static_cb_preds;
	
	private double [][][] pass_elastic_cb_preds;
	private double [][][] fail_elastic_cb_preds;
	
	private double [][][][] pass_scalar_pair_cb_preds;
	private double [][][][] fail_scalar_pair_cb_preds;

	private File inputFile;
	
	private ArrayList<String> totalPredsList = new ArrayList<String>();
	private ArrayList<Double> totalSuspList = new ArrayList<Double>();
	private ArrayList<Double> totalFailingCasesList = new ArrayList<Double>();
	private ArrayList<Double> totalHMList = new ArrayList<Double>();
	
	private ArrayList<String> containsList = new ArrayList<String>();
	private ArrayList<String> excludesList = new ArrayList<String>();

	public LevelTwoValidityCheck (String pInputFilename, boolean includeSingleVar, boolean includeSP, boolean includeCB,
	                            boolean includeStatic, boolean includeElastic, boolean showAllOption, 
								boolean containsOpt, String containsStr,
								boolean excludesOpt, String excludesStr,
								boolean suspOpt, double suspThresh,
	                            boolean top20Option, boolean bottom20Option, boolean failedCasesOption, double lowOutcomeCutoffChoice,
	                            double highOutcomeCutoffChoice, File pMatrixFile){
		lowOutcomeCutoff = lowOutcomeCutoffChoice;
		highOutcomeCutoff = highOutcomeCutoffChoice;
		matrixFile = pMatrixFile;
		inputFile = new File(pInputFilename);
		outputFilename = pInputFilename.replace(".csv","")+"-level-two-report.csv";
		this.setInputFilename(pInputFilename);
		this.setNumberOfVariables(computeNumberOfVariables());
		this.setNumberOfSamples(computeNumberOfSamples());
		createInclusionMatrix(matrixFile, variables);
		copyData(samples, variables, lowOutcomeCutoff, highOutcomeCutoff);
		computeStats(samples, variables);
		computeSingleStaticPreds(samples, variables);
		computeSingleElasticPreds(samples, variables);
		
		double [][] staticPredSusp = compute2DSusp(pass_static_single_preds, fail_static_single_preds);
		double [][] elasticPredSusp = compute2DSusp(pass_elastic_single_preds, fail_elastic_single_preds);
		
		double [][] staticPredFC = compute2DFC(pass_static_single_preds, fail_static_single_preds);
		double [][] elasticPredFC = compute2DFC(pass_elastic_single_preds, fail_elastic_single_preds);
		
		computeCompoundStaticPreds(samples, variables);
		double [][][] compoundStaticSusp = compute3DSusp(pass_static_cb_preds, fail_static_cb_preds);
		double [][][] compoundStaticFC = compute3DFC(pass_static_cb_preds, fail_static_cb_preds);
		
		computeCompoundElasticPreds(samples, variables);
		double [][][] compoundElasticSusp = compute3DSusp(pass_elastic_cb_preds, fail_elastic_cb_preds);
		double [][][] compoundElasticFC = compute3DFC(pass_elastic_cb_preds, fail_elastic_cb_preds);
		
		// scalar pair stuff
		computeDiffs(samples, variables);
		computeScalarPairPreds(samples, variables);
		double [][][] scalarPairSusp = compute3DSusp(pass_scalarpair_preds, fail_scalarpair_preds);
		double [][][] scalarPairFC = compute3DFC(pass_scalarpair_preds, fail_scalarpair_preds);
		
		computeCompoundScalarPairs(samples, variables);
		double [][][][] compoundSPSusp = compute4DSusp(pass_scalar_pair_cb_preds, fail_scalar_pair_cb_preds);
		double [][][][] compoundSPFC = compute4DFC(pass_scalar_pair_cb_preds, fail_scalar_pair_cb_preds);
		
		if (includeSingleVar){
			if (includeStatic)
			{
				addSinglePreds(staticPredSusp, staticPredFC, true, showAllOption, containsOpt, containsStr, excludesOpt, excludesStr, suspOpt, suspThresh);
				//printSinglePredSusp(staticPredSusp, true);
			}
			if (includeElastic)
			{
				addSinglePreds(elasticPredSusp, elasticPredFC, false, showAllOption, containsOpt, containsStr, excludesOpt, excludesStr, suspOpt, suspThresh);
				//printSinglePredSusp(elasticPredSusp, false);
			}
		}
		if (includeSP)
		{
			addScalarPairSusp(scalarPairSusp, scalarPairFC, showAllOption, containsOpt, containsStr, excludesOpt, excludesStr, suspOpt, suspThresh);
			//printScalarPairPredSusp(scalarPairSusp);
		}
		if (includeCB)
		{
			if (includeStatic)
			{
				addStaticCompSusp(compoundStaticSusp, compoundStaticFC, showAllOption, containsOpt, containsStr, excludesOpt, excludesStr, suspOpt, suspThresh);
			}
			if (includeElastic)
			{
				addElasticCompSusp(compoundStaticSusp, compoundStaticFC, showAllOption, containsOpt, containsStr, excludesOpt, excludesStr, suspOpt, suspThresh);
			}
			if (includeSP)
			{
				addScalarPairCompSusp(compoundSPSusp, compoundSPFC, showAllOption, containsOpt, containsStr, excludesOpt, excludesStr, suspOpt, suspThresh);
			}
			
		}
		
		writeSortedPredsListToFile(top20Option, bottom20Option, failedCasesOption);
		
	}
	
	public void writeSortedPredsListToFile(boolean suspTop20, boolean suspBot20, boolean failedCases){
		String output = "";
		
		if (failedCases)
		{
			output+=("Condition, Correlation, Coverage, HarmonicMean\n");
		}
		else
		{
			output+=("----------------------------------------------------------\n");
			output+=("Condition, Correlation, Coverage, HarmonicMean\n");
			output+=("----------------------------------------------------------\n");
		}
		
		
		// this is just bubble sort so its super slow but it made it easy to write
		// if things get bogged down with big enough log files this is an oppurtunity
		// to improve efficiency.
		// print'em
		ArrayList<Double> tempSusp = new ArrayList<Double>();
		ArrayList<Double> tempCases = new ArrayList<Double>();
		ArrayList<Double> tempHM = new ArrayList<Double>();
		ArrayList<String> tempPreds = new ArrayList<String>();
		
		// remove negatives - this means they were never observed
		for (int i=0; i<totalSuspList.size(); i++)
		{
			double score = totalSuspList.get(i);
			if (score > 0.0)
			{
					tempSusp.add(totalSuspList.get(i));
					tempPreds.add(totalPredsList.get(i));
					tempCases.add(totalFailingCasesList.get(i));
					tempHM.add(harmonicMean(totalSuspList.get(i), totalFailingCasesList.get(i)));
			}
		}
		totalSuspList = tempSusp;
		totalPredsList = tempPreds;
		totalFailingCasesList = tempCases;
		totalHMList = tempHM;
		for (int i=0; i<totalSuspList.size()-1; i++)
		{
			for (int j=0; j<totalSuspList.size()-1; j++)
			{
				double leftD = totalHMList.get(j);
				double leftS = totalSuspList.get(j);
				double leftFC = totalFailingCasesList.get(j);
				String leftStr = totalPredsList.get(j);
					
				double rightD = totalHMList.get(j+1);	
				double rightS = totalSuspList.get(j+1);
				double rightFC=totalFailingCasesList.get(j+1);
				String rightStr = totalPredsList.get(j+1);
				
				if (rightD > leftD)
				{
					// swap'em
					totalHMList.set(j, rightD);
					totalSuspList.set(j, rightS);
					totalPredsList.set(j, rightStr);
					totalFailingCasesList.set(j, rightFC);
					
					totalHMList.set(j+1, leftD);
					totalSuspList.set(j+1, leftS);
					totalPredsList.set(j+1, leftStr);
					totalFailingCasesList.set(j+1, leftFC);
					
				}
			}
		}
		
		for (int i=0; i<totalPredsList.size(); i++)
		{
			if (suspTop20 || suspBot20)
			{
				if (suspTop20)
				{
					if (i<20)
					{
						output+=(totalPredsList.get(i));
						output+=String.format("%.4f", totalSuspList.get(i));
						if (failedCases) {
							output+=String.format(", %.4f", totalFailingCasesList.get(i));
							output+=String.format(", %.4f", totalHMList.get(i));
						}
						output+="\n";
					}
				}
				if (suspBot20)
				{
					if (i>totalPredsList.size()-20)
					{
						output+=(totalPredsList.get(i));
						output+=String.format("%.4f", totalSuspList.get(i));
						if (failedCases) {
							output+=String.format(", %.4f", totalFailingCasesList.get(i));
							output+=String.format(", %.4f", totalHMList.get(i));
					     }
						output+="\n";
					}
				}
			}
			else
			{
				output+=(totalPredsList.get(i));
				output+=String.format("%.4f", totalSuspList.get(i));
				if (failedCases) {
					output+=String.format(", %.4f", totalFailingCasesList.get(i));
					output+=String.format(", %.4f", totalHMList.get(i));
				}
				output+="\n";
			}
		}
		try
		{
			PrintWriter out = new PrintWriter(outputFilename);
			out.println(output);
			out.close();
			System.out.println("Level Two IV&V Report compiled successfully in "+outputFilename);
			
		}
		catch(Exception e)
		{
			System.out.println("Error in compiling Level Two IV&V Report. No report was generated.");
		}
		
	}


	public void printSortedPredsList(boolean suspTop20, boolean suspBot20, boolean failedCases){
		if (failedCases)
		{
			System.out.println("----------------------------------------------------------");
			System.out.println("Condition, Suspiciousness, % of Failing Cases");
			System.out.println("----------------------------------------------------------");
		}
		else
		{
			System.out.println("----------------------------------------------------------");
			System.out.println("Condition, Suspiciousness");
			System.out.println("----------------------------------------------------------");
		}
		
		
		// this is just bubble sort so its super slow but it made it easy to write
		// if things get bogged down with big enough log files this is an oppurtunity
		// to improve efficiency.
		// print'em
		ArrayList<Double> tempSusp = new ArrayList<Double>();
		ArrayList<Double> tempCases = new ArrayList<Double>();
		ArrayList<String> tempPreds = new ArrayList<String>();
		
		// remove negatives - this means they were never observed
		for (int i=0; i<totalSuspList.size(); i++)
		{
			double score = totalSuspList.get(i);
			if (score > 0.0)
			{
					tempSusp.add(totalSuspList.get(i));
					tempPreds.add(totalPredsList.get(i));
					tempCases.add(totalFailingCasesList.get(i));
			}
		}
		totalSuspList = tempSusp;
		totalPredsList = tempPreds;
		totalFailingCasesList = tempCases;
		for (int i=0; i<totalSuspList.size()-1; i++)
		{
			for (int j=0; j<totalSuspList.size()-1; j++)
			{
				double leftD = totalSuspList.get(j);
				double leftFC = totalFailingCasesList.get(j);
				String leftStr = totalPredsList.get(j);
					
				double rightD= totalSuspList.get(j+1);
				double rightFC=totalFailingCasesList.get(j+1);
				String rightStr = totalPredsList.get(j+1);
				
				if (rightD > leftD)
				{
					// swap'em
					totalSuspList.set(j, rightD);
					totalPredsList.set(j, rightStr);
					totalFailingCasesList.set(j, rightFC);
					
					totalSuspList.set(j+1, leftD);
					totalPredsList.set(j+1, leftStr);
					totalFailingCasesList.set(j+1, leftFC);
					
				}
			}
		}
		
		for (int i=0; i<totalPredsList.size(); i++)
		{
			if (suspTop20 || suspBot20)
			{
				if (suspTop20)
				{
					if (i<20)
					{
						System.out.print(totalPredsList.get(i));
						System.out.printf("%.4f", totalSuspList.get(i));
						if (failedCases) {System.out.printf(", %.4f", totalFailingCasesList.get(i));}
						System.out.println();
					}
				}
				if (suspBot20)
				{
					if (i>totalPredsList.size()-20)
					{
						System.out.print(totalPredsList.get(i));
						System.out.printf("%.4f", totalSuspList.get(i));
						if (failedCases) {System.out.printf(", %.4f", totalFailingCasesList.get(i));}
						System.out.println();
					}
				}
			}
			else
			{
				System.out.print(totalPredsList.get(i));
				System.out.printf("%.4f", totalSuspList.get(i));
				if (failedCases) {System.out.printf(", %.4f", totalFailingCasesList.get(i));}
				System.out.println();
			}
		}
	}
	
	public void printScalarPairPredSusp(double [][][] matrix){
		for (int i=0; i<matrix.length; i++){
			for (int j=0; j<matrix[0].length; j++){
				if (j > i){
					System.out.print(varlabels[i]+" > "+varlabels[j]+", ");
					System.out.printf("%.2f", matrix[i][j][LARGE]);
					System.out.println();
					System.out.print(varlabels[i]+" = "+varlabels[j]+", ");
					System.out.printf("%.2f", matrix[i][j][MED]);
					System.out.println();
					System.out.print(varlabels[i]+" < "+varlabels[j]+", ");
					System.out.printf("%.2f", matrix[i][j][SMALL]);
					System.out.println();
				}
			}
		}
	}
	
	public void addElasticCompSusp(double [][][] matrix, double [][][] fcMatrix, boolean pShowAllOption, boolean pContains, String pContainsStr,
	                              boolean pExcludes, String pExcludesStr, boolean pSuspLimit, double pSuspThreshold)
	{
		for (int i=0; i<matrix.length; i++){
			for (int j=0; j<matrix[0].length; j++){
				double iSmall = statsset[i][MEAN] - statsset[i][STD_DEV];
				double iBig = statsset[i][MEAN] + statsset[i][STD_DEV];
				
				double jSmall = statsset[j][MEAN] - statsset[j][STD_DEV];
				double jBig = statsset[j][MEAN] + statsset[j][STD_DEV];
				if ((j > i) && pContains==true && pExcludes==false) {
					containsList = parse(pContainsStr);
					if (includes(containsList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",")))
					{
						totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][0]);
						totalFailingCasesList.add(fcMatrix[i][j][0]);
					}
					if (includes(containsList, (varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",")))
					{
						totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][1]);
						totalFailingCasesList.add(fcMatrix[i][j][1]);
						
					}
					if (includes(containsList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+",")))
					{
						totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+",");
						totalSuspList.add(matrix[i][j][2]);
						totalFailingCasesList.add(fcMatrix[i][j][2]);
					}
					if (includes(containsList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",")))
					{
						totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][3]);
						totalFailingCasesList.add(fcMatrix[i][j][3]);
					}
					if (includes(containsList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+",")))
					{
						totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+",");
						totalSuspList.add(matrix[i][j][4]);
						totalFailingCasesList.add(fcMatrix[i][j][4]);
					}
					if (includes(containsList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+",")))
					{
						totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
						totalSuspList.add(matrix[i][j][5]);
						totalFailingCasesList.add(fcMatrix[i][j][5]);
					}
					if (includes(containsList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",")))
					{
						totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][6]);
						totalFailingCasesList.add(fcMatrix[i][j][6]);
					}
					
					if (includes(containsList, (varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",")))
					{
						totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][7]);
						totalFailingCasesList.add(fcMatrix[i][j][7]);
					}
					if (includes(containsList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+",")))
					{
						totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
						totalSuspList.add(matrix[i][j][8]);
						totalFailingCasesList.add(fcMatrix[i][j][8]);
					}
					
				}
				if (pExcludes == true && pContains == true){
					containsList = parse(pContainsStr);
					excludesList = parse(pExcludesStr);	
					if ((j > i)){
						if (includes(containsList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",")) &&
							includes(excludesList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",")) == false)
						{
							totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][0]);
							totalFailingCasesList.add(fcMatrix[i][j][0]);
						}
						
						if (includes(containsList, (varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",")) &&
							includes(excludesList, (varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+","))== false)
						{
							totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][1]);
							totalFailingCasesList.add(fcMatrix[i][j][1]);
						}
						
						if (includes(containsList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+",")) &&
							includes(excludesList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+","))== false)
						{
							totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+",");
							totalSuspList.add(matrix[i][j][2]);
							totalFailingCasesList.add(fcMatrix[i][j][2]);
						}
					
						if (includes(containsList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",")) &&
							includes(excludesList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+","))== false)
						{
							totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][3]);
							totalFailingCasesList.add(fcMatrix[i][j][3]);
						}
						
						
						if (includes(containsList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+",")) &&
							includes(excludesList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+","))== false)
						{
							totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+",");
							totalSuspList.add(matrix[i][j][4]);
							totalFailingCasesList.add(fcMatrix[i][j][4]);
						}
						
						
						if (includes(containsList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+",")) &&
							includes(excludesList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+","))== false)
						{
							totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
							totalSuspList.add(matrix[i][j][5]);
							totalFailingCasesList.add(fcMatrix[i][j][5]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",")) &&
							includes(excludesList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+","))== false)
						{
							totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][6]);
							totalFailingCasesList.add(fcMatrix[i][j][6]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",")) &&
							includes(excludesList, (varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+","))== false)
						{
							totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][7]);
							totalFailingCasesList.add(fcMatrix[i][j][7]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+",")) &&
							includes(excludesList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+","))== false)
						{
							totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
							totalSuspList.add(matrix[i][j][8]);
							totalFailingCasesList.add(fcMatrix[i][j][8]);
						}
					}
				}
				if ((j > i) && pExcludes==true && pContains == false){
					excludesList = parse(pExcludesStr);
					if (includes(excludesList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",")) == false)
					{
						totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][0]);
						totalFailingCasesList.add(fcMatrix[i][j][0]);
					}
					if (includes(excludesList, (varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+","))== false)
					{
						totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][1]);
						totalFailingCasesList.add(fcMatrix[i][j][1]);
						
					}
					if (includes(excludesList, (varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+","))== false)
					{
						totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+",");
						totalSuspList.add(matrix[i][j][2]);
						totalFailingCasesList.add(fcMatrix[i][j][2]);
					}
					if (includes(excludesList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+","))== false)
					{
						totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][3]);
						totalFailingCasesList.add(fcMatrix[i][j][3]);
					}
					if (includes(excludesList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+","))== false)
					{
						totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+",");
						totalSuspList.add(matrix[i][j][4]);
						totalFailingCasesList.add(fcMatrix[i][j][4]);
					}
					if (includes(excludesList, (iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+","))== false)
					{
						totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
						totalSuspList.add(matrix[i][j][5]);
						totalFailingCasesList.add(fcMatrix[i][j][5]);
					}
					if (includes(excludesList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+","))== false)
					{
						totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][6]);
						totalFailingCasesList.add(fcMatrix[i][j][6]);
					}
					
					if (includes(excludesList, (varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+","))== false)
					{
						totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
						totalSuspList.add(matrix[i][j][7]);
						totalFailingCasesList.add(fcMatrix[i][j][7]);
					}
					if (includes(excludesList, (varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+","))== false)
					{
						totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
						totalSuspList.add(matrix[i][j][8]);
						totalFailingCasesList.add(fcMatrix[i][j][8]);
					}
				}
				if (pExcludes == false && pContains == false){
					if ((j > i)){
						
						if (matrix[i][j][0] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][0]);
							totalFailingCasesList.add(fcMatrix[i][j][0]);
						}
						
						if (matrix[i][j][1] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][1]);
							totalFailingCasesList.add(fcMatrix[i][j][1]);
						}
						
						if (matrix[i][j][2] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" < "+ iSmall + " AND "+varlabels[j]+" > "+jBig+",");
							totalSuspList.add(matrix[i][j][2]);
							totalFailingCasesList.add(fcMatrix[i][j][2]);
						}
					
						if (matrix[i][j][3] >= pSuspThreshold)
						{
							totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][3]);
							totalFailingCasesList.add(fcMatrix[i][j][3]);
						}
						
						
						if (matrix[i][j][4] >= pSuspThreshold)
						{
							totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jBig+",");
							totalSuspList.add(matrix[i][j][4]);
							totalFailingCasesList.add(fcMatrix[i][j][4]);
						}
						
						
						if (matrix[i][j][5] >= pSuspThreshold)
						{
							totalPredsList.add(iSmall+" < "+varlabels[i]+" < "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
							totalSuspList.add(matrix[i][j][5]);
							totalFailingCasesList.add(fcMatrix[i][j][5]);
						}
						
						
						if (matrix[i][j][6] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][6]);
							totalFailingCasesList.add(fcMatrix[i][j][6]);
						}
						
						
						if (matrix[i][j][7] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+jSmall+" < " +varlabels[j]+" < "+jSmall+",");
							totalSuspList.add(matrix[i][j][7]);
							totalFailingCasesList.add(fcMatrix[i][j][7]);
						}
						
						
						if (matrix[i][j][8] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" > "+ iBig + " AND "+varlabels[j]+" > "+jBig+",");
							totalSuspList.add(matrix[i][j][8]);
							totalFailingCasesList.add(fcMatrix[i][j][8]);
						}
					}
				}
			}
		}
		
	}
	
	public void addStaticCompSusp(double [][][] matrix, double [][][] fcMatrix, boolean pShowAllOption, boolean pContains, String pContainsStr,
	                              boolean pExcludes, String pExcludesStr, boolean pSuspLimit, double pSuspThreshold)
	{
		for (int i=0; i<matrix.length; i++){
			for (int j=0; j<matrix[0].length; j++){
				if ((j > i) && pContains==true && pExcludes==false) {
					containsList = parse(pContainsStr);
					if (includes(containsList, (varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,")))
					{
						totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,");
						totalSuspList.add(matrix[i][j][0]);
						totalFailingCasesList.add(fcMatrix[i][j][0]);
					}
					if (includes(containsList, (varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,")))
					{
						totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,");
						totalSuspList.add(matrix[i][j][1]);
						totalFailingCasesList.add(fcMatrix[i][j][1]);
					}
					if (includes(containsList, (varlabels[i]+" < 0 AND "+varlabels[j]+" > 0,")))
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,");
						totalSuspList.add(matrix[i][j][2]);
						totalFailingCasesList.add(fcMatrix[i][j][2]);
					}
					if (includes(containsList, (varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,")))
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,");
						totalSuspList.add(matrix[i][j][3]);
						totalFailingCasesList.add(fcMatrix[i][j][3]);
					}
					if (includes(containsList, (varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,")))
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,");
						totalSuspList.add(matrix[i][j][4]);
						totalFailingCasesList.add(fcMatrix[i][j][4]);
					}
					if (includes(containsList, (varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,")))
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,");
						totalSuspList.add(matrix[i][j][5]);
						totalFailingCasesList.add(fcMatrix[i][j][5]);
					}
					if (includes(containsList, (varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,")))
					{
						totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,");
						totalSuspList.add(matrix[i][j][6]);
						totalFailingCasesList.add(fcMatrix[i][j][6]);
					}
					if (includes(containsList, (varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,")))
					{
						totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,");
						totalSuspList.add(matrix[i][j][7]);
						totalFailingCasesList.add(fcMatrix[i][j][7]);
					}
					if (includes(containsList, (varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,")))
					{
						totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,");
						totalSuspList.add(matrix[i][j][8]);
						totalFailingCasesList.add(fcMatrix[i][j][8]);
					}
				}
				if ((j > i) && pExcludes==true && pContains==false){
					excludesList = parse(pExcludesStr);
					if (includes(excludesList, (varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,");
						totalSuspList.add(matrix[i][j][0]);
						totalFailingCasesList.add(fcMatrix[i][j][0]);
					}
					if (includes(excludesList, (varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,");
						totalSuspList.add(matrix[i][j][1]);
						totalFailingCasesList.add(fcMatrix[i][j][1]);
					}
					if (includes(excludesList, (varlabels[i]+" < 0 AND "+varlabels[j]+" > 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,");
						totalSuspList.add(matrix[i][j][2]);
						totalFailingCasesList.add(fcMatrix[i][j][2]);
					}
					if (includes(excludesList, (varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,");
						totalSuspList.add(matrix[i][j][3]);
						totalFailingCasesList.add(fcMatrix[i][j][3]);
					}
					if (includes(excludesList, (varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,");
						totalSuspList.add(matrix[i][j][4]);
						totalFailingCasesList.add(fcMatrix[i][j][4]);
					}
					if (includes(excludesList, (varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,");
						totalSuspList.add(matrix[i][j][5]);
						totalFailingCasesList.add(fcMatrix[i][j][5]);
					}
					if (includes(excludesList, (varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,");
						totalSuspList.add(matrix[i][j][6]);
						totalFailingCasesList.add(fcMatrix[i][j][6]);
					}
					if (includes(excludesList, (varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,");
						totalSuspList.add(matrix[i][j][7]);
						totalFailingCasesList.add(fcMatrix[i][j][7]);
					}
					if (includes(excludesList, (varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,"))==false)
					{
						totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,");
						totalSuspList.add(matrix[i][j][8]);
						totalFailingCasesList.add(fcMatrix[i][j][8]);
					}
				}
				if (pExcludes == false && pContains == false)
				{
					if ((j > i)){
						
						if (matrix[i][j][0] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,");
							totalSuspList.add(matrix[i][j][0]);
							totalFailingCasesList.add(fcMatrix[i][j][0]);
						}
						
						if (matrix[i][j][1] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,");
							totalSuspList.add(matrix[i][j][1]);
							totalFailingCasesList.add(fcMatrix[i][j][1]);
						}
						
						if (matrix[i][j][2] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" > 0,");
							totalSuspList.add(matrix[i][j][2]);
							totalFailingCasesList.add(fcMatrix[i][j][2]);
						}
						
						
						if (matrix[i][j][3] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,");
							totalSuspList.add(matrix[i][j][3]);
							totalFailingCasesList.add(fcMatrix[i][j][3]);
						}
						
						
						if (matrix[i][j][4] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,");
							totalSuspList.add(matrix[i][j][4]);
							totalFailingCasesList.add(fcMatrix[i][j][4]);
						}
						
						
						if (matrix[i][j][5] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,");
							totalSuspList.add(matrix[i][j][5]);
							totalFailingCasesList.add(fcMatrix[i][j][5]);
						}
						
						
						if (matrix[i][j][6] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,");
							totalSuspList.add(matrix[i][j][6]);
							totalFailingCasesList.add(fcMatrix[i][j][6]);
						}
						
						
						if (matrix[i][j][7] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,");
							totalSuspList.add(matrix[i][j][7]);
							totalFailingCasesList.add(fcMatrix[i][j][7]);
						}
						
						
						if (matrix[i][j][8] >= pSuspThreshold){
							totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,");
							totalSuspList.add(matrix[i][j][8]);
							totalFailingCasesList.add(fcMatrix[i][j][8]);
						}
						
					}
				}
				if (pExcludes == true && pContains == true)
				{
					containsList = parse(pContainsStr);
					excludesList = parse(pExcludesStr);	
					if ((j > i)){
						
						if (includes(containsList, (varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,")) &&
							includes(excludesList, (varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,"))==false){
							totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" < 0,");
							totalSuspList.add(matrix[i][j][0]);
							totalFailingCasesList.add(fcMatrix[i][j][0]);
						}
						
						if (includes(containsList, (varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,")) &&
							includes(excludesList, (varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,"))==false){
							totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" == 0,");
							totalSuspList.add(matrix[i][j][1]);
							totalFailingCasesList.add(fcMatrix[i][j][1]);
						}
						
						if (includes(containsList, (varlabels[i]+" < 0 AND "+varlabels[j]+" > 0,")) &&
							includes(excludesList, (varlabels[i]+" < 0 AND "+varlabels[j]+" > 0,"))==false){
							totalPredsList.add(varlabels[i]+" < 0 AND "+varlabels[j]+" > 0,");
							totalSuspList.add(matrix[i][j][2]);
							totalFailingCasesList.add(fcMatrix[i][j][2]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,")) &&
							includes(excludesList, (varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,"))==false){
							totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" < 0,");
							totalSuspList.add(matrix[i][j][3]);
							totalFailingCasesList.add(fcMatrix[i][j][3]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,")) &&
							includes(excludesList, (varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,"))==false){
							totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" == 0,");
							totalSuspList.add(matrix[i][j][4]);
							totalFailingCasesList.add(fcMatrix[i][j][4]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,")) &&
							includes(excludesList, (varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,"))==false){
							totalPredsList.add(varlabels[i]+" == 0 AND "+varlabels[j]+" > 0,");
							totalSuspList.add(matrix[i][j][5]);
							totalFailingCasesList.add(fcMatrix[i][j][5]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,")) &&
							includes(excludesList, (varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,"))==false){
							totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" < 0,");
							totalSuspList.add(matrix[i][j][6]);
							totalFailingCasesList.add(fcMatrix[i][j][6]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,")) &&
							includes(excludesList, (varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,"))==false){
							totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" == 0,");
							totalSuspList.add(matrix[i][j][7]);
							totalFailingCasesList.add(fcMatrix[i][j][7]);
						}
						
						
						if (includes(containsList, (varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,")) &&
							includes(excludesList, (varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,"))==false){
							totalPredsList.add(varlabels[i]+" > 0 AND "+varlabels[j]+" > 0,");
							totalSuspList.add(matrix[i][j][8]);
							totalFailingCasesList.add(fcMatrix[i][j][8]);
						}
						
					}
					
				}
			}
		}
	}
	
	public void addScalarPairCompSusp(double [][][][] matrix, double [][][][] fcMatrix, boolean pShowAllOption, boolean pContains, String pContainsStr,
	                              boolean pExcludes, String pExcludesStr, boolean pSuspLimit, double pSuspThreshold)
	{
		for (int i=0; i<matrix.length; i++){
			for (int j=0; j<matrix[0].length; j++){
				for (int k=0; k<matrix[0][0].length; k++){
					if (pContains == true && pExcludes == false && (j > i) && (k > j))
					{
					   containsList = parse(pContainsStr);	
					   if (includes(containsList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][0]);
							totalFailingCasesList.add(fcMatrix[i][j][k][0]);
					   }
					   if (includes(containsList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][1]);
							totalFailingCasesList.add(fcMatrix[i][j][k][1]);
					   }
					   if (includes(containsList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][2]);
							totalFailingCasesList.add(fcMatrix[i][j][k][2]);
					   }
					   if (includes(containsList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][3]);
							totalFailingCasesList.add(fcMatrix[i][j][k][3]);
					   }
					   if (includes(containsList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][4]);
							totalFailingCasesList.add(fcMatrix[i][j][k][4]);
					   }
					   if (includes(containsList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][5]);
							totalFailingCasesList.add(fcMatrix[i][j][k][5]);
					   }
					   if (includes(containsList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][6]);
							totalFailingCasesList.add(fcMatrix[i][j][k][6]);					   	
					   }
					   if (includes(containsList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][7]);
							totalFailingCasesList.add(fcMatrix[i][j][k][7]);
					   }
					   if (includes(containsList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", "))){
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][8]);
							totalFailingCasesList.add(fcMatrix[i][j][k][8]);
					   }
					   
					}
		
					if (pExcludes == true && pContains == true)
					{
						containsList = parse(pContainsStr);
						excludesList = parse(pExcludesStr);	
						if ( (j > i) && (k > j))
						{
							if(includes(containsList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][0]);
								totalFailingCasesList.add(fcMatrix[i][j][k][0]);	
							}
							
							if(includes(containsList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][1]);
								totalFailingCasesList.add(fcMatrix[i][j][k][1]);
							}
							
							if(includes(containsList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][2]);
								totalFailingCasesList.add(fcMatrix[i][j][k][2]);
							}
							
							if(includes(containsList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][3]);
								totalFailingCasesList.add(fcMatrix[i][j][k][3]);
							}
							
							if(includes(containsList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][4]);
								totalFailingCasesList.add(fcMatrix[i][j][k][4]);	
							}
							
							if(includes(containsList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][5]);
								totalFailingCasesList.add(fcMatrix[i][j][k][5]);
							}
							
							if(includes(containsList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][6]);
								totalFailingCasesList.add(fcMatrix[i][j][k][6]);
							}
							
							if(includes(containsList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][7]);
								totalFailingCasesList.add(fcMatrix[i][j][k][7]);
							}
							
							if(includes(containsList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) &&
							   includes(excludesList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) == false)
							{
								totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][8]);
								totalFailingCasesList.add(fcMatrix[i][j][k][8]);
							}
						}
					}		
					if (pExcludes == true && pContains == false && (j > i) && (k > j))
					{
					   excludesList = parse(pExcludesStr);	
					   if (includes(excludesList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][0]);
							totalFailingCasesList.add(fcMatrix[i][j][k][0]);
					   }
					   if (includes(excludesList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][1]);
							totalFailingCasesList.add(fcMatrix[i][j][k][1]);
					   }
					   if (includes(excludesList, (varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][2]);
							totalFailingCasesList.add(fcMatrix[i][j][k][2]);
					   }
					   if (includes(excludesList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][3]);
							totalFailingCasesList.add(fcMatrix[i][j][k][3]);
					   }
					   if (includes(excludesList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][4]);
							totalFailingCasesList.add(fcMatrix[i][j][k][4]);
					   }
					   if (includes(excludesList, (varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][5]);
							totalFailingCasesList.add(fcMatrix[i][j][k][5]);
					   }
					   if (includes(excludesList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][6]);
							totalFailingCasesList.add(fcMatrix[i][j][k][6]);					   	
					   }
					   if (includes(excludesList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][7]);
							totalFailingCasesList.add(fcMatrix[i][j][k][7]);
					   }
					   if (includes(excludesList, (varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ")) == false){
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
							totalSuspList.add(matrix[i][j][k][8]);
							totalFailingCasesList.add(fcMatrix[i][j][k][8]);
					   }
					}
					if (pExcludes == false && pContains == false)
					{
						if ( (j > i) && (k > j))
						{
							if( (matrix[i][j][k][0]) >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][0]);
								totalFailingCasesList.add(fcMatrix[i][j][k][0]);	
							}
							
							if(matrix[i][j][k][1] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][1]);
								totalFailingCasesList.add(fcMatrix[i][j][k][1]);
							}
							
							if(matrix[i][j][k][2] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" < "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][2]);
								totalFailingCasesList.add(fcMatrix[i][j][k][2]);
							}
							
							if(matrix[i][j][k][3] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][3]);
								totalFailingCasesList.add(fcMatrix[i][j][k][3]);
							}
							
							if(matrix[i][j][k][4] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][4]);
								totalFailingCasesList.add(fcMatrix[i][j][k][4]);	
							}
							
							if(matrix[i][j][k][5] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" == "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][5]);
								totalFailingCasesList.add(fcMatrix[i][j][k][5]);
							}
							
							if(matrix[i][j][k][6] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" < "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][6]);
								totalFailingCasesList.add(fcMatrix[i][j][k][6]);
							}
							
							if(matrix[i][j][k][7] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" == "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][7]);
								totalFailingCasesList.add(fcMatrix[i][j][k][7]);
							}
							
							if(matrix[i][j][k][8] >= pSuspThreshold)
							{
								totalPredsList.add(varlabels[i]+" > "+varlabels[j]+ " AND "+varlabels[i] +" > "+varlabels[k]+", ");
								totalSuspList.add(matrix[i][j][k][8]);
								totalFailingCasesList.add(fcMatrix[i][j][k][8]);
							}
						}
					}		
				}
			}
		}
	}
	
	public void addScalarPairSusp(double [][][] matrix, double [][][] fcMatrix, boolean pShowAllOption, boolean pContains, String pContainsStr,
	                              boolean pExcludes, String pExcludesStr, boolean pSuspLimit, double pSuspThreshold){
		for (int i=0; i<matrix.length; i++){
			for (int j=0; j<matrix[0].length; j++){
				if ((j > i) && pContains==true && pExcludes == false){
					containsList = parse(pContainsStr);
					if (includes(containsList, (varlabels[i]+" > "+varlabels[j]+", ")))
					{
						totalPredsList.add(varlabels[i]+" > "+varlabels[j]+", ");
						totalSuspList.add(matrix[i][j][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][j][LARGE]);
					}
					if (includes(containsList, (varlabels[i]+" = "+varlabels[j]+", ")))
					{
						totalPredsList.add(varlabels[i]+" = "+varlabels[j]+", ");
						totalSuspList.add(matrix[i][j][MED]);
						totalFailingCasesList.add(fcMatrix[i][j][MED]);
					}
					if (includes(containsList, (varlabels[i]+" < "+varlabels[j]+", ")))
					{
						totalPredsList.add(varlabels[i]+" > "+varlabels[j]+", ");
						totalSuspList.add(matrix[i][j][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][j][SMALL]);
					}
					
				}
				if ((j > i) && pExcludes == true && pContains == false){
					excludesList = parse(pExcludesStr);
					if (includes(excludesList, (varlabels[i]+" > "+varlabels[j]+", ")) == false)
					{
						totalPredsList.add(varlabels[i]+" > "+varlabels[j]+", ");
						totalSuspList.add(matrix[i][j][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][j][LARGE]);
					}
					if (includes(excludesList, (varlabels[i]+" = "+varlabels[j]+", ")) == false)
					{
						totalPredsList.add(varlabels[i]+" = "+varlabels[j]+", ");
						totalSuspList.add(matrix[i][j][MED]);
						totalFailingCasesList.add(fcMatrix[i][j][MED]);
					}
					if (includes(excludesList, (varlabels[i]+" < "+varlabels[j]+", ")) == false)
					{
						totalPredsList.add(varlabels[i]+" > "+varlabels[j]+", ");
						totalSuspList.add(matrix[i][j][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][j][SMALL]);
					}
					
				}
				if (pExcludes == false && pContains == false)
				{
					if (j > i){
						if (matrix[i][j][LARGE] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+", ");
							totalSuspList.add(matrix[i][j][LARGE]);
							totalFailingCasesList.add(fcMatrix[i][j][LARGE]);
						}
						if (matrix[i][j][MED] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" = "+varlabels[j]+", ");
							totalSuspList.add(matrix[i][j][MED]);
							totalFailingCasesList.add(fcMatrix[i][j][MED]);
						}
						if (matrix[i][j][SMALL] >= pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+", ");
							totalSuspList.add(matrix[i][j][SMALL]);
							totalFailingCasesList.add(fcMatrix[i][j][SMALL]);
						}
					}
				}
				
				if (pExcludes == true && pContains == true)
				{
					containsList = parse(pContainsStr);
					excludesList = parse(pExcludesStr);	
					if (j > i){
						if (includes(containsList, (varlabels[i]+" > "+varlabels[j]+", ")) &&
							includes(excludesList, (varlabels[i]+" > "+varlabels[j]+", ")) == false)
						{
							totalPredsList.add(varlabels[i]+" > "+varlabels[j]+", ");
							totalSuspList.add(matrix[i][j][LARGE]);
							totalFailingCasesList.add(fcMatrix[i][j][LARGE]);
						}
						if (includes(containsList, (varlabels[i]+" = "+varlabels[j]+", ")) &&
							includes(excludesList, (varlabels[i]+" = "+varlabels[j]+", ")) == false)
						{
							totalPredsList.add(varlabels[i]+" = "+varlabels[j]+", ");
							totalSuspList.add(matrix[i][j][MED]);
							totalFailingCasesList.add(fcMatrix[i][j][MED]);
						}
						if (includes(containsList, (varlabels[i]+" < "+varlabels[j]+", ")) &&
							includes(excludesList, (varlabels[i]+" < "+varlabels[j]+", ")) == false)
						{
							totalPredsList.add(varlabels[i]+" < "+varlabels[j]+", ");
							totalSuspList.add(matrix[i][j][SMALL]);
							totalFailingCasesList.add(fcMatrix[i][j][SMALL]);
						}
					}
				}
				
			}
		}
	}
	
	public void addSinglePreds(double [][] matrix, double [][] fcMatrix, boolean pStatic, boolean pShowAllOption, boolean pContains, String pContainsStr, 
	                           boolean pExcludes, String pExcludesStr, boolean pSuspLimit, double pSuspThreshold){

		for (int i=0; i<matrix.length; i++){
			if (pStatic){
				if (pContains == true && pExcludes == false)
				{
					containsList = parse(pContainsStr);
					if (includes(containsList, (varlabels[i]+" > 0, ")))
					{
						totalPredsList.add(varlabels[i]+" > 0, ");
						totalSuspList.add(matrix[i][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][LARGE]);
						
					}
					if (includes(containsList, (varlabels[i]+" = 0, ")))
					{
						totalPredsList.add(varlabels[i]+" = 0, ");
						totalSuspList.add(matrix[i][MED]);
						totalFailingCasesList.add(fcMatrix[i][MED]);
					}
					if (includes(containsList, (varlabels[i]+" < 0, ")))
					{
						totalPredsList.add(varlabels[i]+" < 0, ");
						totalSuspList.add(matrix[i][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][SMALL]);
					}
				}
				if (pExcludes == true && pContains == false)
				{
					excludesList = parse(pExcludesStr);
					if (includes(excludesList, (varlabels[i]+" > 0, ")) == false)
					{
						totalPredsList.add(varlabels[i]+" > 0, ");
						totalSuspList.add(matrix[i][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][LARGE]);
						
					}
					if (includes(excludesList, (varlabels[i]+" = 0, ")) == false)
					{
						totalPredsList.add(varlabels[i]+" = 0, ");
						totalSuspList.add(matrix[i][MED]);
						totalFailingCasesList.add(fcMatrix[i][MED]);
					}
					if (includes(excludesList, (varlabels[i]+" < 0, ")) == false)
					{
						totalPredsList.add(varlabels[i]+" < 0, ");
						totalSuspList.add(matrix[i][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][SMALL]);
					}
				}
				if (pExcludes == false && pContains == false)
				{
						if (matrix[i][LARGE]>=pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" > 0, ");
							totalSuspList.add(matrix[i][LARGE]);
							totalFailingCasesList.add(fcMatrix[i][LARGE]);
						}
						if (matrix[i][MED]>=pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" = 0, ");
							totalSuspList.add(matrix[i][MED]);
							totalFailingCasesList.add(fcMatrix[i][MED]);
						}
						if (matrix[i][SMALL]>=pSuspThreshold)
						{
							totalPredsList.add(varlabels[i]+" < 0, ");
							totalSuspList.add(matrix[i][SMALL]);
							totalFailingCasesList.add(fcMatrix[i][SMALL]);
						}
				}
				if (pExcludes == true && pContains == true)
				{
					excludesList = parse(pExcludesStr);
					containsList = parse(pContainsStr);
					if (includes(containsList, (varlabels[i]+" > 0, ")) && 
					    includes(excludesList, (varlabels[i]+" > 0, ")) == false)
					{
						totalPredsList.add(varlabels[i]+" > 0, ");
						totalSuspList.add(matrix[i][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][LARGE]);
					}
					if (includes(containsList, (varlabels[i]+" = 0, ")) && 
					    includes(excludesList, (varlabels[i]+" = 0, ")) == false)
					{
						totalPredsList.add(varlabels[i]+" = 0, ");
						totalSuspList.add(matrix[i][MED]);
						totalFailingCasesList.add(fcMatrix[i][MED]);
					}
					if (includes(containsList, (varlabels[i]+" < 0, ")) && 
					    includes(excludesList, (varlabels[i]+" < 0, ")) == false)
					{
						totalPredsList.add(varlabels[i]+" < 0, ");
						totalSuspList.add(matrix[i][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][SMALL]);
					}
					
				}
				
			}	
			else{
				if (pContains == true && pExcludes==false)
				{
					containsList = parse(pContainsStr);
					if (includes(containsList, (varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")))
					{
						totalPredsList.add((varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][LARGE]);
					}
				    if (includes(containsList, (varlabels[i]+" < ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")))
					{
						totalPredsList.add((varlabels[i]+" < ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][SMALL]);
					}
					if (includes(containsList, (statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", ")))
					{
						totalPredsList.add((statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][MED]);
						totalFailingCasesList.add(fcMatrix[i][MED]);
					}
					
				}
				if (pExcludes==true && pContains==false)
				{
					excludesList = parse(pExcludesStr);
					if (includes(excludesList, (varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")) == false)
					{
						totalPredsList.add((varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][LARGE]);
					}
				    if (includes(excludesList, (varlabels[i]+" < ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")) == false)
					{
						totalPredsList.add((varlabels[i]+" < ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][SMALL]);
					}
					if (includes(excludesList, (statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + 
						                       (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", ")) == false)
					{
						totalPredsList.add((statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][MED]);
						totalFailingCasesList.add(fcMatrix[i][MED]);
					}
					
				}
				if (pExcludes == false && pContains==false)
				{
						if (matrix[i][LARGE]>=pSuspThreshold)
						{
							totalPredsList.add((varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", "));
							totalSuspList.add(matrix[i][LARGE]);
							totalFailingCasesList.add(fcMatrix[i][LARGE]);
						}
						if (matrix[i][MED]>=pSuspThreshold)
						{
							totalPredsList.add((statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
							totalSuspList.add(matrix[i][MED]);
							totalFailingCasesList.add(fcMatrix[i][MED]);
						}
						if (matrix[i][SMALL]>=pSuspThreshold)
						{
							totalPredsList.add((varlabels[i]+" < ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
							totalSuspList.add(matrix[i][SMALL]);
							totalFailingCasesList.add(fcMatrix[i][SMALL]);
						}
				}
				if (pExcludes == true && pContains == true)
				{
					excludesList = parse(pExcludesStr);
					containsList = parse(pContainsStr);
					if (includes(containsList, (varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")) &&
					    includes(excludesList, (varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")) == false)
					{
						totalPredsList.add((varlabels[i]+" > ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][LARGE]);
						totalFailingCasesList.add(fcMatrix[i][LARGE]);
					}
					if (includes(containsList, (varlabels[i]+" < ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")) && 
					    includes(excludesList, (varlabels[i]+" < ")+ (statsset[i][MEAN] + statsset[i][STD_DEV]) + (", ")) == false)
					{
						totalPredsList.add((varlabels[i]+" < ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][SMALL]);
						totalFailingCasesList.add(fcMatrix[i][SMALL]);
					}
					if (includes(containsList, (statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", ")) &&
						includes(excludesList, (statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + 
												                   (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", ")) == false)
					{
						totalPredsList.add((statsset[i][MEAN] + statsset[i][STD_DEV]) + (" > " +varlabels[i]+" > ") + (statsset[i][MEAN] - statsset[i][STD_DEV]) + (", "));
						totalSuspList.add(matrix[i][MED]);
						totalFailingCasesList.add(fcMatrix[i][MED]);
					}
					
				}
				
			}
		}
	}

	public void printSinglePredSusp(double [][] matrix, boolean pStatic){

		for (int i=0; i<matrix.length; i++){
			if (pStatic){
				System.out.print(varlabels[i]+" > 0, ");
				System.out.printf("%.2f", matrix[i][LARGE]);
				System.out.println();
				System.out.print(varlabels[i]+" = 0, ");
				System.out.printf("%.2f", matrix[i][MED]);
				System.out.println();
				System.out.print(varlabels[i]+" < 0, ");
				System.out.printf("%.2f", matrix[i][SMALL]);
				System.out.println();
			}	
			else{
				System.out.print(varlabels[i]+" > ");
				System.out.printf("%.3f", (statsset[i][MEAN] + statsset[i][STD_DEV]));
				System.out.print(", ");
				System.out.printf("%.2f", matrix[i][LARGE]);
				System.out.println();
				System.out.print(varlabels[i]+" < ");
				System.out.printf("%.3f", (statsset[i][MEAN] - statsset[i][STD_DEV]));
				System.out.print(", ");
				System.out.printf("%.2f", matrix[i][SMALL]);
				System.out.println();
				System.out.printf("%.3f", (statsset[i][MEAN] + statsset[i][STD_DEV]));
				System.out.print(" > " +varlabels[i]+" > ");
				System.out.printf("%.3f", (statsset[i][MEAN] - statsset[i][STD_DEV]));
				System.out.print(", ");
				System.out.printf("%.2f", matrix[i][MED]);
				System.out.println();
			}
		}
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
	
	public double[][][][] compute4DFC(double [][][][] passMatrix, double [][][][] failMatrix){
		double fcMatrix [][][][] = new double [passMatrix.length][passMatrix[0].length][passMatrix[0][0].length][passMatrix[0][0][0].length];
		for (int i=0; i<fcMatrix.length; i++){
			fcMatrix[i] = compute3DFC(passMatrix[i], failMatrix[i]);
		}
		return fcMatrix;
	}

	public double[][][] compute3DFC(double [][][] passMatrix, double [][][] failMatrix){
		double fcMatrix [][][] = new double [passMatrix.length][passMatrix[0].length][passMatrix[0][0].length];
		for (int i=0; i<fcMatrix.length; i++){
			fcMatrix[i] = compute2DFC(passMatrix[i], failMatrix[i]);
		}
		return fcMatrix;
	}

	public double[][] compute2DFC(double[][] passArray, double[][] failArray){
		double fcMatrix [][] = new double [passArray.length][passArray[0].length];

		for (int i=0; i<fcMatrix.length; i++){
			for (int j=0; j<fcMatrix[0].length; j++){
				double failTraces = failArray[i][j];
				fcMatrix[i][j] = failTraces / totalWins;	
			}
		}
		return fcMatrix;
	}
	
	public double[][][][] compute4DSusp(double [][][][] passMatrix, double [][][][] failMatrix){
		double suspMatrix [][][][] = new double [passMatrix.length][passMatrix[0].length][passMatrix[0][0].length][passMatrix[0][0][0].length];
		for (int i=0; i<suspMatrix.length; i++){
			suspMatrix[i] = compute3DSusp(passMatrix[i], failMatrix[i]);
		}
		return suspMatrix;
	}

	public double[][][] compute3DSusp(double [][][] passMatrix, double [][][] failMatrix){
		double suspMatrix [][][] = new double [passMatrix.length][passMatrix[0].length][passMatrix[0][0].length];
		for (int i=0; i<suspMatrix.length; i++){
			suspMatrix[i] = compute2DSusp(passMatrix[i], failMatrix[i]);
		}
		return suspMatrix;
	}

	public double[][] compute2DSusp(double[][] passArray, double[][] failArray){
		double suspMatrix [][] = new double [passArray.length][passArray[0].length];

		for (int i=0; i<suspMatrix.length; i++){
			for (int j=0; j<suspMatrix[0].length; j++){
				double totalTraces = passArray[i][j] + failArray[i][j];
				double failTraces = failArray[i][j];
				if (totalTraces > 0){
					suspMatrix[i][j] = failTraces / totalTraces;
				}
				else {
					suspMatrix[i][j] = -1.0;
				}	
			}
		}
		return suspMatrix;
	}

	private void computeSingleElasticPreds(int pSamples, int pVariables){
		pass_elastic_single_preds = new double [pVariables] [PRED_SIZE];
		fail_elastic_single_preds = new double [pVariables] [PRED_SIZE];

		for (int i=0; i<dataset.length; i++){
			double failExtent = Double.parseDouble(datalabels[i][OUTCOME].trim());
			double passExtent = 1.0 - failExtent;

			for (int j=0; j<dataset[0].length; j++){
				if (dataset[i][j] < statsset[j][MEAN] - statsset[j][STD_DEV]){
					fail_elastic_single_preds[j][SMALL]+=failExtent;
					pass_elastic_single_preds[j][SMALL]+=passExtent;
				}
				else if (dataset[i][j] > statsset[j][MEAN] + statsset[j][STD_DEV]){
					fail_elastic_single_preds[j][LARGE]+=failExtent;
					pass_elastic_single_preds[j][LARGE]+=passExtent;
				}
				else {
					fail_elastic_single_preds[j][MED]+=failExtent;
					pass_elastic_single_preds[j][MED]+=passExtent;
				}
			}
		}		
	}

	private void computeSingleStaticPreds(int pSamples, int pVariables){
		pass_static_single_preds = new double [pVariables] [PRED_SIZE];
		fail_static_single_preds = new double [pVariables] [PRED_SIZE];

		for (int i=0; i<dataset.length; i++){
			double failExtent = Double.parseDouble(datalabels[i][OUTCOME].trim());
			double passExtent = 1.0 - failExtent;

			for (int j=0; j<dataset[0].length; j++){
				if (dataset[i][j] < 0.0){
					fail_static_single_preds[j][SMALL]+=failExtent;
					pass_static_single_preds[j][SMALL]+=passExtent;
				}
				else if (dataset[i][j] == 0.0){
					fail_static_single_preds[j][MED]+=failExtent;
					pass_static_single_preds[j][MED]+=passExtent;
				}
				else {
					fail_static_single_preds[j][LARGE]+=failExtent;
					pass_static_single_preds[j][LARGE]+=passExtent;
				}
			}
		}		
	}
	
	private void computeCompoundElasticPreds(int pSamples, int pVariables){
		
		pass_elastic_cb_preds = new double [pVariables] [pVariables] [PRED_SIZE*PRED_SIZE];
		fail_elastic_cb_preds = new double [pVariables] [pVariables] [PRED_SIZE*PRED_SIZE];
		
		for (int i=0; i<dataset.length; i++){
			double failExtent = Double.parseDouble(datalabels[i][OUTCOME].trim());
			double passExtent = 1.0 - failExtent;

			for (int j=0; j<dataset[0].length; j++){
				for (int k=0; k<dataset[0].length; k++){
					if (j != k){
						
						double jSmall = statsset[j][MEAN] - statsset[j][STD_DEV];
						double jBig = statsset[j][MEAN] + statsset[j][STD_DEV];
						
						double kSmall = statsset[k][MEAN] - statsset[k][STD_DEV];
						double kBig = statsset[k][MEAN] + statsset[k][STD_DEV];
						
						if (dataset[i][j] < jSmall && dataset[i][k] < kSmall){
							fail_elastic_cb_preds[j][k][0]+=failExtent;
							pass_elastic_cb_preds[j][k][0]+=passExtent;
						}
						if (dataset[i][j] < jSmall && ((dataset[i][k] > kSmall) && (dataset[i][k] < kBig))){
							fail_elastic_cb_preds[j][k][1]+=failExtent;
							pass_elastic_cb_preds[j][k][1]+=passExtent;
						}
						if (dataset[i][j] < jSmall && dataset[i][k] > kBig){
							fail_elastic_cb_preds[j][k][2]+=failExtent;
							pass_elastic_cb_preds[j][k][2]+=passExtent;
						}
						if ( ((dataset[i][j] > jSmall) && (dataset[i][j] < jBig)) && dataset[i][k] < kSmall){
							fail_elastic_cb_preds[j][k][3]+=failExtent;
							pass_elastic_cb_preds[j][k][3]+=passExtent;
						}
						if ( ((dataset[i][j] > jSmall) && (dataset[i][j] < jBig)) && 
						     ((dataset[i][k] > kSmall) && (dataset[i][k] < kBig))){
							fail_elastic_cb_preds[j][k][4]+=failExtent;
							pass_elastic_cb_preds[j][k][4]+=passExtent;
						}
						if ( ((dataset[i][j] > jSmall) && (dataset[i][j] < jBig)) && dataset[i][k] > kBig){
							fail_elastic_cb_preds[j][k][5]+=failExtent;
							pass_elastic_cb_preds[j][k][5]+=passExtent;
						}
						if (dataset[i][j] > jBig && dataset[i][k] < kSmall){
							fail_elastic_cb_preds[j][k][6]+=failExtent;
							pass_elastic_cb_preds[j][k][6]+=passExtent;
						}
						if (dataset[i][j] > jBig && ((dataset[i][k] > kSmall) && (dataset[i][k] < kBig))){
							fail_elastic_cb_preds[j][k][7]+=failExtent;
							pass_elastic_cb_preds[j][k][7]+=passExtent;
						}
						if (dataset[i][j] > jBig && dataset[i][k] > kBig){
							fail_elastic_cb_preds[j][k][8]+=failExtent;
							pass_elastic_cb_preds[j][k][8]+=passExtent;
						}
					}
				}
			}		
		}
	}
	
	
	private void computeCompoundStaticPreds(int pSamples, int pVariables){
		pass_static_cb_preds = new double [pVariables] [pVariables] [PRED_SIZE*PRED_SIZE];
		fail_static_cb_preds = new double [pVariables] [pVariables] [PRED_SIZE*PRED_SIZE];

		for (int i=0; i<dataset.length; i++){
			double failExtent = Double.parseDouble(datalabels[i][OUTCOME].trim());
			double passExtent = 1.0 - failExtent;

			for (int j=0; j<dataset[0].length; j++){
				for (int k=0; k<dataset[0].length; k++){
					if (j != k){
						if (dataset[i][j] < 0.0 && dataset[i][k] < 0.0){
							fail_static_cb_preds[j][k][0]+=failExtent;
							pass_static_cb_preds[j][k][0]+=passExtent;
						}
						if (dataset[i][j] < 0.0 && dataset[i][k] == 0.0){
							fail_static_cb_preds[j][k][1]+=failExtent;
							pass_static_cb_preds[j][k][1]+=passExtent;
						}
						if (dataset[i][j] < 0.0 && dataset[i][k] > 0.0){
							fail_static_cb_preds[j][k][2]+=failExtent;
							pass_static_cb_preds[j][k][2]+=passExtent;
						}
						if (dataset[i][j] == 0.0 && dataset[i][k] < 0.0){
							fail_static_cb_preds[j][k][3]+=failExtent;
							pass_static_cb_preds[j][k][3]+=passExtent;
						}
						if (dataset[i][j] == 0.0 && dataset[i][k] == 0.0){
							fail_static_cb_preds[j][k][4]+=failExtent;
							pass_static_cb_preds[j][k][4]+=passExtent;
						}
						if (dataset[i][j] == 0.0 && dataset[i][k] > 0.0){
							fail_static_cb_preds[j][k][5]+=failExtent;
							pass_static_cb_preds[j][k][5]+=passExtent;
						}
						if (dataset[i][j] > 0.0 && dataset[i][k] < 0.0){
							fail_static_cb_preds[j][k][6]+=failExtent;
							pass_static_cb_preds[j][k][6]+=passExtent;
						}
						if (dataset[i][j] > 0.0 && dataset[i][k] == 0.0){
							fail_static_cb_preds[j][k][7]+=failExtent;
							pass_static_cb_preds[j][k][7]+=passExtent;
						}
						if (dataset[i][j] > 0.0 && dataset[i][k] > 0.0){
							fail_static_cb_preds[j][k][8]+=failExtent;
							pass_static_cb_preds[j][k][8]+=passExtent;
						}
					}
				}
			}		
		}
	}
	
	private void computeCompoundScalarPairs(int pSamples, int pVariables){
		pass_scalar_pair_cb_preds = new double [pVariables][pVariables][pVariables] [PRED_SIZE*PRED_SIZE];
		fail_scalar_pair_cb_preds = new double [pVariables][pVariables][pVariables] [PRED_SIZE*PRED_SIZE];

		for (int i=0; i<differenceset.length; i++){
			// check if its a passing or failing trace
			double failExtent = Double.parseDouble(datalabels[i][OUTCOME].trim());
			double passExtent = 1.0 - failExtent;
			for (int j=0; j<pVariables; j++){
				for (int k=0; k<pVariables; k++){
					for (int l=0; l<pVariables; l++){
						if ((j!= k) && (k != l) && inclusionMatrix[j][k] && inclusionMatrix[j][l]){
							if (differenceset[i][j][k] < 0.0 && differenceset[i][j][l] < 0.0){
								fail_scalar_pair_cb_preds[j][k][l][0]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][0]+=passExtent;
							}
							if (differenceset[i][j][k] < 0.0 && differenceset[i][j][l] == 0.0){
								fail_scalar_pair_cb_preds[j][k][l][1]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][1]+=passExtent;
							}
							if (differenceset[i][j][k] < 0.0 && differenceset[i][j][l] > 0.0){
								fail_scalar_pair_cb_preds[j][k][l][2]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][2]+=passExtent;
							}
							if (differenceset[i][j][k] == 0.0 && differenceset[i][j][l] < 0.0){
								fail_scalar_pair_cb_preds[j][k][l][3]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][3]+=passExtent;
							}
							if (differenceset[i][j][k] == 0.0 && differenceset[i][j][l] == 0.0){
								fail_scalar_pair_cb_preds[j][k][l][4]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][4]+=passExtent;
							}
							if (differenceset[i][j][k] == 0.0 && differenceset[i][j][l] > 0.0){
								fail_scalar_pair_cb_preds[j][k][l][5]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][5]+=passExtent;
							}
							if (differenceset[i][j][k] > 0.0 && differenceset[i][j][l] < 0.0){
								fail_scalar_pair_cb_preds[j][k][l][6]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][6]+=passExtent;
							}
							if (differenceset[i][j][k] > 0.0 && differenceset[i][j][l] == 0.0){
								fail_scalar_pair_cb_preds[j][k][l][7]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][7]+=passExtent;
							}
							if (differenceset[i][j][k] > 0.0 && differenceset[i][j][l] > 0.0){
								fail_scalar_pair_cb_preds[j][k][l][8]+=failExtent;
								pass_scalar_pair_cb_preds[j][k][l][8]+=passExtent;
							}
						}
					}
				}
			}	
		}
	}
	

	private void computeStats(int pSamples, int pVariables){
		statsset = new double [pVariables][STATS_COLS];

		for (int vars = 0; vars<pVariables; vars++){
			double [] buffer = new double [pSamples];
			for (int i=0; i<dataset.length; i++){
				for (int j=0; j<dataset[0].length; j++){
					if (vars == j){
						buffer[i] = dataset[i][j];
					}
				}
			}
			statsset[vars][MEAN] = mean(buffer);
			statsset[vars][STD_DEV] = stddev(buffer);
		}
	}
	
	private void computeScalarPairPreds(int pSamples, int pVariables){
		pass_scalarpair_preds = new double [pVariables][pVariables] [PRED_SIZE];
		fail_scalarpair_preds = new double [pVariables][pVariables] [PRED_SIZE];

		for (int i=0; i<differenceset.length; i++){
			// check if its a passing or failing trace
			double failExtent = Double.parseDouble(datalabels[i][OUTCOME].trim());
			double passExtent = 1.0 - failExtent;
			for (int j=0; j<pVariables; j++){
				for (int k=0; k<pVariables; k++){
					if (inclusionMatrix[j][k]){
						if (differenceset[i][j][k] < 0.0){
							fail_scalarpair_preds[j][k][SMALL]+=failExtent;
							pass_scalarpair_preds[j][k][SMALL]+=passExtent;
						}
						else if (differenceset[i][j][k] == 0.0){
							fail_scalarpair_preds[j][k][MED]+=failExtent;
							pass_scalarpair_preds[j][k][MED]+=passExtent;
						}
						else{
							fail_scalarpair_preds[j][k][LARGE]+=failExtent;
							pass_scalarpair_preds[j][k][LARGE]+=passExtent;
						}
					}
				}
			}
		}	
	}

	private void computeDiffs(int pSamples, int pVariables){

		differenceset = new double[pSamples][pVariables][pVariables];

		for (int i=0; i<dataset.length; i++){
			differenceset[i] = new double [pVariables][pVariables];
			for (int j=0; j<dataset[0].length; j++){
				for (int k=0; k<dataset[0].length; k++){
					differenceset[i][j][k] = dataset[i][j] - dataset[i][k];
				}
			}
		}
	}

	private void copyData(int pSamples, int pVariables, double lOutcomeCutoff, double hOutcomeCutoff){
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
				if ((scoreForCompare >= lOutcomeCutoff) &&
					(scoreForCompare <= hOutcomeCutoff))
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

	public static double sum(double[] a) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return sum;
	}


	public static double mean(double[] a) {
		if (a.length == 0) return Double.NaN;
		double sum = sum(a);
		return sum / a.length;
	}

	public static double var(double[] a) {
		if (a.length == 0) return Double.NaN;
		double avg = mean(a);
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += (a[i] - avg) * (a[i] - avg);
		}
		return sum / (a.length - 1);
	}

	public static double stddev(double[] a) {
		return Math.sqrt(var(a));
	}
	
	public static void createInclusionMatrix(File matrix, int pVariables)
	{
		 inclusionMatrix = new boolean [pVariables][pVariables];
		
		if (matrix == null)
		{
			for (int i=0; i<inclusionMatrix.length; i++)
			{
				for (int j=0; j<inclusionMatrix[0].length; j++)
				{
					inclusionMatrix[i][j] = true;
				}
			}
		}
		else
		{
			for (int i=0; i<inclusionMatrix.length; i++)
			{
				for (int j=0; j<inclusionMatrix[0].length; j++)
				{
					inclusionMatrix[i][j] = false;
				}
			}
			ArrayList<String> matrixRows = readIntoArrayList(matrix);
			// first row is headers lose them
			matrixRows.remove(0);
			// go through the matrix file and put *** in each blank space
			for(int i=0; i<matrixRows.size(); i++)
			{
				String temp = matrixRows.get(i);
				temp = temp.replaceAll(",", ", ");
				matrixRows.set(i, temp);
				
			}
			for(int i=0; i<matrixRows.size(); i++)
			{
				StringTokenizer tok = new StringTokenizer(matrixRows.get(i), ",");
				// this is the variable label (kill it)
				String label = tok.nextToken();
				int j=0;
				while (tok.hasMoreTokens())
				{
					String buf = tok.nextToken();
					if (buf.trim().equals("X"))
					{
						inclusionMatrix[i][j] = true;
						inclusionMatrix[j][i] = true;
					}
					j++;
				}
			}
		}
	}
	
	public static double harmonicMean(double a, double b)
	{
		double numer = 2 * a * b;
		double demo = a + b;
		return numer/demo;
	}
	
	public static ArrayList<String> parse(String listOfTerms)
	{
		ArrayList<String> list = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(listOfTerms, ";");
		while(tok.hasMoreTokens())
		{
			list.add(tok.nextToken().trim());
		}
		return list;
	}
	
	public static boolean includes(ArrayList<String> list, String predicate)
	{
		for (int i=0; i<list.size(); i++)
		{
			String term = list.get(i);
			if (predicate.contains(term))
			{
				return true;
			}
		}
		return false;
	}
	
	public static ArrayList<String> readIntoArrayList(File f) {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			String availalbe;
			while((availalbe = br.readLine()) != null) {
				availalbe = Normalizer.normalize(availalbe, Normalizer.Form.NFD);
				result.add(availalbe);

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

}