import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;

public class StatDebugGUI extends JFrame{
	
	public static final String [] SUPPORTED_OPERATORS = {">", ">=", "=", "<=", "<"};
	public static final  int    GREATER_THAN = 0;
	public static final  int    GREATER_THAN_EQ = 1;
	public static final  int    EQUAL = 2;
	public static final  int    LESS_THAN_EQ = 3;
	public static final  int    LESS_THAN = 4;
	
	private static final String [] DEFAULT_LEFT_NAMES = {"VariableName"};
	private static final String [] DEFAULT_RIGHT_NAMES = {"OtherVariableName"};
	private static final String ALWAYS_STRING = "Always                                              ";
	private static final String NEVER_STRING  = "Never                                               "; 
	private static int MIN = 0;
	private static int MAX = 1;
	
	private static final JLabel [] blankDivider = new JLabel[9];
	private static final JLabel [] sliderPad = new JLabel[3];
    
	
	// gui variables used throughout class
    private static JButton fileButton;
    
    private static File selectedFile;
	
	// build a single variable predicate
	private static JLabel singleVarReqLabel;
	private static JComboBox singleVarLeftSideList;
	private static JComboBox singleVarOperatorList; 
	private static JTextField singleVarRightSide;
	private static JRadioButton singleVarAlwaysButton;
	private static JRadioButton singleVarNeverButton;
	private static JButton      singleVarSubmitReq;
	private static JTextArea    singleVarAlwaysList;
	private static JScrollPane svAlwaysPane;
	private static JTextArea    singleVarNeverList;
	private static JScrollPane svNeverPane;
	
	// build a custom scalar pairs predicate
	private static JLabel spReqLabel;
	private static JComboBox spLeftSideList;
	private static JComboBox spOperatorList; 
	private static JComboBox spRightSideList;
	private static JRadioButton spAlwaysButton;
	private static JRadioButton spNeverButton;
	private static JButton spSubmitReq;
	private static JTextArea spAlwaysList;
	private static JTextArea spNeverList;
	private static JScrollPane spAlwaysPane;
	private static JScrollPane spNeverPane;
	
	
	// Level 2 stuff
	private static JLabel  cutoffLabel;
	private static JSlider outcomeCutoff;
	private static JLabel outcomeValueLabel;
	private static JTextField outcomeValueTextField;
	private static JTextField minTextField;
	private static JTextField medTextField;
	private static JTextField maxTextField;
	private static JLabel minLabel;
	private static JLabel medLabel;
	private static JLabel maxLabel;
    
    private static JLabel predSpecLabel;
    private static JCheckBox elasticPredBox;
    private static JCheckBox staticPredBox;
    
    private static JButton calcButton;
	private static JButton clearButton;
    
    private static JLabel predTypeLabel;
    private static JCheckBox singleVarPredBox;
    private static JCheckBox scalarPairPredBox;
    private static JCheckBox compoundPredBox;
	
	private static JLabel includeLabel;
	private static JLabel excludeLabel;

    private static JRadioButton filterPredsBox;
    private static JTextField filterTextField;
    private static JRadioButton excludesPredsBox;
    private static JTextField excludesTextField;


	// Level 3 stuff
	private static JLabel customPredsLabel;
	private static JTextField customPredsTextField;
	private static JLabel customSuspLabel;
	private static JTextField customSuspTextField;

	// non-array spacing and padding 
	private static JLabel levelOneDivider;
	private static JLabel levelTwoDivider;
	private static JLabel levelThreeDivider;
	private static JLabel computeDivider;
	private static JLabel finalDivider;
    

    /**
     * Creates & displays window.
     */
    public StatDebugGUI() {
        initComponents();
        
        // show the window
        pack();
        setVisible(true);
    }
    
    /**
     * Initializes GUI window by adding components.
     */
    private void initComponents() {
    	
    	// set up the window
        setTitle("IV&V Checker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(565, 810));
        
        
        // set up file choice
        fileButton = new JButton("       Choose Log File To Upload .....       ");
		calcButton = new JButton("Click Here To Perform IV&V Checks");
		clearButton = new JButton("Clear Fields");
		
		// set up oppurtunity to build always/never predicates
		singleVarReqLabel = new JLabel("Single Variable Req:");
		singleVarLeftSideList = new JComboBox(DEFAULT_LEFT_NAMES);
		singleVarLeftSideList.setPreferredSize(new Dimension(120, 30));
		singleVarOperatorList = new JComboBox(SUPPORTED_OPERATORS); 
		singleVarRightSide = new JTextField("Enter #", 8);
		singleVarSubmitReq = new JButton("Add Req");
		singleVarAlwaysButton = new JRadioButton(ALWAYS_STRING);
		singleVarNeverButton = new JRadioButton(NEVER_STRING);
		singleVarAlwaysList = new JTextArea(5, 22);
		singleVarAlwaysList.setEditable(false);
		svAlwaysPane = new JScrollPane(singleVarAlwaysList);
		singleVarNeverList = new JTextArea(5, 23);
		singleVarNeverList.setEditable(false);
		svNeverPane = new JScrollPane(singleVarNeverList);
		
		spReqLabel = new JLabel("Scalar Pair Req:      ");
		spLeftSideList = new JComboBox(DEFAULT_LEFT_NAMES);
		spLeftSideList.setPreferredSize(new Dimension(120, 30));
		spOperatorList = new JComboBox(SUPPORTED_OPERATORS); 
		spRightSideList = new JComboBox(DEFAULT_RIGHT_NAMES);
		spRightSideList.setPreferredSize(new Dimension(120, 30));
		spSubmitReq = new JButton("Add Req");
		spAlwaysButton = new JRadioButton(ALWAYS_STRING);
		spAlwaysList = new JTextArea(5, 22);
		spAlwaysList.setEditable(false);
		spAlwaysPane = new JScrollPane(spAlwaysList);
		spNeverButton = new JRadioButton(NEVER_STRING);
		spNeverList = new JTextArea(5, 23);
		spNeverList.setEditable(false);
		spNeverPane = new JScrollPane(spNeverList);
		
		
		// set up level 2 exploration
		cutoffLabel = new JLabel("Choose Success Cutoff Value: ");
		outcomeCutoff = new JSlider(0, 100);
		outcomeCutoff.setMajorTickSpacing(20);
		outcomeCutoff.setMinorTickSpacing(5);
		outcomeCutoff.setPreferredSize(new Dimension(350, 30));
		outcomeCutoff.setPaintTicks(true);
		
		outcomeValueLabel = new JLabel("Value:");
		outcomeValueTextField = new JTextField("N/A", 3);
		minLabel = new JLabel("Min");
		medLabel = new JLabel("Mid");
		maxLabel = new JLabel("Max");
		minTextField = new JTextField(" N/A", 3);
		minTextField.setEditable(false);
		medTextField = new JTextField(" N/A", 3);
		medTextField.setEditable(false);
		maxTextField = new JTextField(" N/A", 3);
		maxTextField.setEditable(false);
		
        // set up pred type choice
        predTypeLabel = new JLabel("Condition Types:");
        singleVarPredBox = new JCheckBox("Single Variable");
        singleVarPredBox.setSelected(true);
        scalarPairPredBox = new JCheckBox("Scalar Pairs");
        scalarPairPredBox.setSelected(true);
        compoundPredBox = new JCheckBox("Compound      ");

        // set up pred type choice
        predSpecLabel = new JLabel("Cond Specificity:");
        staticPredBox = new JCheckBox("Static              ");
        staticPredBox.setSelected(true);
        elasticPredBox = new JCheckBox("Elastic");
        
        // set up include/exclude choice
		includeLabel = new JLabel("Including these terms ");
        filterPredsBox = new JRadioButton("");
        filterTextField = new JTextField("Include Term1; Term2; Term3; ...",30);
		excludeLabel = new JLabel("Excluding these terms");
        excludesPredsBox = new JRadioButton("");
        excludesTextField = new JTextField("Exclude Term1; Term2; Term3; ...",30);
		customPredsLabel = new JLabel("Test that ");
		customPredsTextField = new JTextField("(Var1 > Var2) || Var4 > 100", 24);
		customPredsTextField.setToolTipText("Visit http://goo.gl/KjWAzL for syntax and supported functions.");
		customSuspLabel = new JLabel("has susp ");
		customSuspTextField = new JTextField("0.##  (i.e. 0.67)", 8);
		
	    // instantiate padding cheats
		finalDivider = new JLabel("-------------------------------------------------------------------------------------");
		sliderPad[0] = new JLabel("            ");
		sliderPad[1] = new JLabel("                   ");
		sliderPad[2] = new JLabel("                 ");
	
		blankDivider[0] = new JLabel("                              ");
		blankDivider[1] = new JLabel("                                                ");
		blankDivider[2] = new JLabel("                                                                                                                          ");
		blankDivider[3] = new JLabel("                                                                                                                          ");
		blankDivider[4] = new JLabel("                                                                                                                          ");
		blankDivider[5] = new JLabel("                                                            ");
		blankDivider[6] = new JLabel("                       ");
		blankDivider[7] = new JLabel("                       ");
		blankDivider[8] = new JLabel("                  ");
		
		
		//Group the radio buttons.
		ButtonGroup singleVarLevelOneGroup = new ButtonGroup();
		singleVarAlwaysButton.setSelected(true);
		singleVarLevelOneGroup.add(singleVarAlwaysButton);
		singleVarLevelOneGroup.add(singleVarNeverButton);
		
		ButtonGroup spLevelOneGroup = new ButtonGroup();
		spAlwaysButton.setSelected(true);
		spLevelOneGroup.add(spNeverButton);
		spLevelOneGroup.add(spAlwaysButton);
		
		ButtonGroup group = new ButtonGroup();
		group.add(filterPredsBox);
		group.add(excludesPredsBox);
       
		levelOneDivider= new JLabel("---------------- Level 1 IV&V Check: Specify Requirements -------------------");
		levelTwoDivider= new JLabel("------------ Level 2 IV&V Check: Identify the Conditions of Success -------------");
		levelThreeDivider= new JLabel("--------------- Level 3 IV&V Check: Test Specific Hypotheses -----------------");
		computeDivider = new JLabel("                                                                                                                          ");
		
        
        
        
        // initialize window layout & add components
        setLayout(new FlowLayout(FlowLayout.LEFT));
		getContentPane().add(blankDivider[0]);
        getContentPane().add(fileButton);
		getContentPane().add(blankDivider[1]);
        getContentPane().add(levelOneDivider);
		
		// add custom always/never predicate support
		
		// single variable
		getContentPane().add(singleVarReqLabel);
		getContentPane().add(singleVarLeftSideList);
		getContentPane().add(singleVarOperatorList); 
		getContentPane().add(singleVarRightSide);
		getContentPane().add(singleVarSubmitReq);
		getContentPane().add(singleVarAlwaysButton);
		getContentPane().add(singleVarNeverButton);
		getContentPane().add(svAlwaysPane);
		getContentPane().add(svNeverPane);
		
		// add divider for spacing
		getContentPane().add(blankDivider[2]);
		
		// scalar pair
		getContentPane().add(spReqLabel);
		getContentPane().add(spLeftSideList);
		getContentPane().add(spOperatorList); 
		getContentPane().add(spRightSideList);
		getContentPane().add(spSubmitReq);
		getContentPane().add(spAlwaysButton);
		getContentPane().add(spNeverButton);
		getContentPane().add(spAlwaysPane);
		getContentPane().add(spNeverPane);
		
		getContentPane().add(blankDivider[3]);
		getContentPane().add(levelTwoDivider);
		
		
		getContentPane().add(cutoffLabel);
		getContentPane().add(outcomeCutoff);
		getContentPane().add(outcomeValueLabel);
		getContentPane().add(outcomeValueTextField);
		getContentPane().add(sliderPad[0]);
		getContentPane().add(minLabel);
		getContentPane().add(minTextField);
		getContentPane().add(sliderPad[1]);
		getContentPane().add(medLabel);
		getContentPane().add(medTextField);
		getContentPane().add(sliderPad[2]);
		getContentPane().add(maxLabel);
		getContentPane().add(maxTextField);
		
		
		getContentPane().add(blankDivider[4]);
        getContentPane().add(predTypeLabel);
        getContentPane().add(singleVarPredBox);
        getContentPane().add(scalarPairPredBox);
        getContentPane().add(compoundPredBox);
		
		
        getContentPane().add(predSpecLabel);
		getContentPane().add(staticPredBox);
        getContentPane().add(elasticPredBox);
        
		
		getContentPane().add(blankDivider[5]);
		
		getContentPane().add(filterPredsBox);
		getContentPane().add(includeLabel);
        getContentPane().add(filterTextField);
		//getContentPane().add(blankDivider6);
		
		getContentPane().add(excludesPredsBox);
		getContentPane().add(excludeLabel);
        getContentPane().add(excludesTextField);
		getContentPane().add(blankDivider[7]);
		getContentPane().add(levelThreeDivider);
		
		getContentPane().add(customPredsLabel);
        getContentPane().add(customPredsTextField);
		getContentPane().add(customSuspLabel);
		getContentPane().add(customSuspTextField);
		
		getContentPane().add(finalDivider);
		//getContentPane().add(computeDivider);
		getContentPane().add(blankDivider[8]);
        getContentPane().add(calcButton);
		getContentPane().add(clearButton);
			
        
        // create & assign action listener for button
        calcButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	calcButtonActionPerformed(evt);
            }
        });
		
        singleVarSubmitReq.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
				if (selectedFile != null)
				{
					try
					{
						String var = String.valueOf(singleVarLeftSideList.getSelectedItem()).trim();
						String op  = String.valueOf(singleVarOperatorList.getSelectedItem()).trim();
						double value =  Double.parseDouble(String.valueOf(singleVarRightSide.getText()).trim());
						var = var.replaceAll("\\s+","");
						op = op.replaceAll("\\s+","");
						String requirement = var+" "+op+" "+value+"\n";
						if (singleVarNeverButton.isSelected())
						{
							singleVarNeverList.setText(singleVarNeverList.getText() + requirement);
						}
						else
						{
							singleVarAlwaysList.setText(singleVarAlwaysList.getText() + requirement);
						}
					}
					catch (Exception e)
					{
						System.out.println("Trouble parsing single variable requirement.");
						return;
					}
				}
            }
        });
		
        spSubmitReq.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
				if (selectedFile != null)
				{
					try
					{
						String var1 = String.valueOf(spLeftSideList.getSelectedItem()).trim();
						String op  = String.valueOf(spOperatorList.getSelectedItem()).trim();
						String var2 = String.valueOf(spRightSideList.getSelectedItem()).trim();
						var1 = var1.replaceAll("\\s+","");
						op = op.replaceAll("\\s+","");
						var2=var2.replaceAll("\\s+","");
						String requirement = var1+" "+op+" "+var2+"\n";
						if (spNeverButton.isSelected())
						{
							spNeverList.setText(spNeverList.getText() + requirement);
						}
						else
						{
							spAlwaysList.setText(spAlwaysList.getText() + requirement);
						}
					}
					catch (Exception e)
					{
						System.out.println("Trouble parsing scalar pairs requirement.");
						return;
					}
				}
            }
        });
		
        outcomeCutoff.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
				if (selectedFile != null)
				{
					double min = Double.parseDouble(minTextField.getText());
					double max = Double.parseDouble(maxTextField.getText());
					double slidervalue = Double.parseDouble(String.valueOf(outcomeCutoff.getValue()));
					
					double valToSet = min + ((max-min)*(slidervalue/100.0));
					
					outcomeValueTextField.setText(String.format("%.2f", valToSet));
				}
            }
        });
		
		//When Press Enter After Change...do this
		 outcomeValueTextField.addActionListener(new ActionListener() {    
		 	public void actionPerformed(ActionEvent e) {
		        if (selectedFile != null)
				{
	                try
	                {
						double enteredValue = Double.parseDouble(outcomeValueTextField.getText());
						double min = Double.parseDouble(minTextField.getText());
						double max = Double.parseDouble(maxTextField.getText());
						
						if (enteredValue >= min && enteredValue <= max)
						{
							double valToSet = (enteredValue-min)/(max-min);
							valToSet = valToSet*100;
							
		                    outcomeCutoff.setValue((int) valToSet);
						}
						else
						{
		                    outcomeValueTextField.setText("ERR");
		                    outcomeValueTextField.setToolTipText("Set Value in Range between min and max") ;
						}
						
	                }
	                catch(Exception ex)
	                {
	                    outcomeValueTextField.setText("ERR");
	                    outcomeValueTextField.setToolTipText("Set Value in Range between min and max") ;   
	                }
				}     
		    }
		  });
		  
		clearButton.addActionListener(new ActionListener() {
			 public void actionPerformed(ActionEvent ae) {
				 singleVarAlwaysList.setText("");
				 singleVarNeverList.setText("");
				 spAlwaysList.setText("");
				 spNeverList.setText("");
				 outcomeValueTextField.setText(medTextField.getText());
				 outcomeCutoff.setValue(50);
				 filterTextField.setText("Include Term1; Term2; Term3; ...");
				 excludesTextField.setText("Exclude Term1; Term2; Term3; ...");
				 customPredsTextField.setText("(Var1 > Var2) || Var4 > 100");
				 customSuspTextField.setText("0.##  (i.e. 0.67)");
		   }
		});
		
        fileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
              JFileChooser fileChooser = new JFileChooser();
              int returnValue = fileChooser.showOpenDialog(null);
              if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
				
				double min = findOutcome(selectedFile, MIN);
				double max = findOutcome(selectedFile, MAX);
				double med = (max+min)/2;
				
				String [] varNames = getVarnames(selectedFile);
				
				// update display
				minTextField.setText(String.format("%.2f", min));
				medTextField.setText(String.format("%.2f", med));
				maxTextField.setText(String.format("%.2f", max));
				outcomeValueTextField.setText(String.format("%.2f", med));
				DefaultComboBoxModel singleVarLeftSideListModel = new DefaultComboBoxModel( varNames );
				DefaultComboBoxModel spLeftSideListModel = new DefaultComboBoxModel( varNames );
				DefaultComboBoxModel spRightSideListModel = new DefaultComboBoxModel( varNames );
				singleVarLeftSideList.setModel( singleVarLeftSideListModel );
				spLeftSideList.setModel(spLeftSideListModel);
				spRightSideList.setModel(spRightSideListModel);
              }
            }
          });
    }
    
    /**
     * Calls method to calculate predicates and susp.
     * 
     * @param evt Button event detected
     */
    private void calcButtonActionPerformed(ActionEvent evt) {
		
		if (selectedFile != null)
		{
			// Level 1 stuff
			ArrayList<String> svAlwaysToTest = new ArrayList<String>();
			StringTokenizer tok = new StringTokenizer(singleVarAlwaysList.getText(), "\n");
			while(tok.hasMoreTokens())
			{
				String predicate = tok.nextToken();
				svAlwaysToTest.add(predicate);
			}
			
			ArrayList<String> svNeverToTest = new ArrayList<String>();
			tok = new StringTokenizer(singleVarNeverList.getText(), "\n");
			while(tok.hasMoreTokens())
			{
				String predicate = tok.nextToken();
				svNeverToTest.add(predicate);
			}
			
			ArrayList<String> spAlwaysToTest = new ArrayList<String>();
			tok = new StringTokenizer(spAlwaysList.getText(), "\n");
			while(tok.hasMoreTokens())
			{
				String predicate = tok.nextToken();
				spAlwaysToTest.add(predicate);
			}
			
			ArrayList<String> spNeverToTest = new ArrayList<String>();
			tok = new StringTokenizer(spNeverList.getText(), "\n");
			while(tok.hasMoreTokens())
			{
				String predicate = tok.nextToken();
				spNeverToTest.add(predicate);
			}
			
			
			
			// LEVEL 2 Stuff
			boolean incSingleVar = singleVarPredBox.isSelected();
			boolean incScalarPairs = scalarPairPredBox.isSelected();
			boolean incCompBool = compoundPredBox.isSelected();
			boolean incStatic = staticPredBox.isSelected();
			boolean incElastic = elasticPredBox.isSelected();
		
			boolean contains = filterPredsBox.isSelected();
			String containsText = filterTextField.getText();
			boolean excludes = excludesPredsBox.isSelected();
			String excludesText = excludesTextField.getText();
			boolean showAll = ((contains || excludes) == false);
			boolean suspLimit = false;
			double suspThreshold = 0;
			boolean top20 = false;
			boolean bottom20 = false;
			boolean failedCasesOpt = true;
			
			double min = Double.parseDouble(minTextField.getText());
			double max = Double.parseDouble(maxTextField.getText());
			double slidervalue = Double.parseDouble(String.valueOf(outcomeCutoff.getValue()));
			double cutoffValue = min + ((max-min)*(slidervalue/100.0));

			String suspHypString =  customSuspTextField.getText();
			double suspHypothesis  = 4;
			
			try{
					suspHypothesis = Double.parseDouble(suspHypString);
					if (suspHypothesis < 0.0 || suspHypothesis > 1.0)
					{
			            customSuspTextField.setText("ERR");
			            customSuspTextField.setToolTipText("Set Value in Range between 0.0 and 1.0") ;
					}
			}
			catch (NumberFormatException e)
            {
	            customSuspTextField.setText("ERR");
	            customSuspTextField.setToolTipText("Set Value in Range between 0.0 and 1.0") ;
				return;
            }
			
			String customPredicate = customPredsTextField.getText();
			ArrayList<String> varList = new ArrayList<String>();
			try
			{
				Expression expr = new Expression(customPredicate);
				String [] varNames = getVarnames(selectedFile);
				Iterator<String> exprTokens = expr.getExpressionTokenizer();
				
				while(exprTokens.hasNext())
				{
					String curToken = exprTokens.next().trim().replaceAll("\\s+","");
					for (int i=0; i<varNames.length; i++)
					{
						String curVarInList = varNames[i].trim().replaceAll("\\s+","");
						if (curToken.equals(curVarInList))
						{
							varList.add(curVarInList);
						}
					}
				}
				for (int i=0; i<varList.size(); i++)
				{
					expr.with(varList.get(i), "1");
				}
				expr.eval();
				
			}
			catch (Exception exception)
			{
				customPredsTextField.setText(exception.getMessage());
				return;
			}	
			LevelOneValidityCheck leveOne = new LevelOneValidityCheck(selectedFile.getAbsolutePath(), svAlwaysToTest, svNeverToTest, spAlwaysToTest, spNeverToTest);
	    	LevelTwoValidityCheck levelTwo = new LevelTwoValidityCheck(selectedFile.getAbsolutePath(), incSingleVar, incScalarPairs, incCompBool, incStatic, incElastic, 
				                                                 showAll, contains, containsText, excludes, excludesText, 
																 suspLimit, suspThreshold, top20, bottom20, failedCasesOpt, cutoffValue);
			LevelThreeValidityCheck levelThree = new LevelThreeValidityCheck(selectedFile.getAbsolutePath(), cutoffValue, customPredicate, suspHypothesis, varList);
			
		}
		
    }
    
    // brings up main window when run
    public static void main(String [] args)
    {
    	StatDebugGUI gui = new StatDebugGUI();
    }
	
	public static String[] getVarnames(File file)
	{
		ArrayList<String> varList = new ArrayList<String>();
		String line="";
		try{
			Scanner fileScanner = new Scanner(file);
			// eat the headers
			line = fileScanner.nextLine();	
			fileScanner.close();
		} catch (Exception e){
			e.printStackTrace();
		}	
		
		StringTokenizer tok = new StringTokenizer(line, ",");
		// skip the very first token its an id
		String buf = tok.nextToken();
		while (tok.hasMoreTokens())
		{
			buf = tok.nextToken();
			varList.add(buf.trim());
		}
		// -1 here avoids outcome variable
		String [] varNames = new String[varList.size()-1];
		
		for (int i=0; i<varNames.length; i++)
		{
			varNames[i] = varList.get(i);
		}
		return varNames;
	}
	
	public static double findOutcome(File file, int choice)
	{
		ArrayList<Double> outcomeList = new ArrayList<Double>();
		try{
			Scanner fileScanner = new Scanner(file);
			// eat the headers
			String line = fileScanner.nextLine();	

			while(fileScanner.hasNextLine()){
				line = fileScanner.nextLine();
				StringTokenizer tok = new StringTokenizer(line, ",");
				String buf = "";
				while (tok.hasMoreTokens())
				{
					buf = tok.nextToken();
				}
				outcomeList.add(Double.parseDouble(buf));
			}
			fileScanner.close();
		} catch (Exception e){
			e.printStackTrace();
		}	
		double threshold;
		if (choice == MIN)
		{
			threshold = Double.MAX_VALUE;
			for (int i=0; i< outcomeList.size(); i++)
			{
				if (outcomeList.get(i) < threshold)
				{
					threshold = outcomeList.get(i);
				}
			}
		}
		else
		{
			threshold = Double.MIN_VALUE;
			for (int i=0; i< outcomeList.size(); i++)
			{
				if (outcomeList.get(i) > threshold)
				{
					threshold = outcomeList.get(i);
				}
			}
		}
		
		return threshold;
	}


}
