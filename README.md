# IVandVLevelChecker
Model and Simulation Verification and Validation Tool - Inspired by Modeling Checking and Statistical Debugging

This is a M&S Verification and Validation Tool that is inspired by model checking and statistical debugging analysis.
To use it create a log file consisting of a set of simulation trials (i.e. set of inputs) followed by one output.

The log file should be in csv format with headers for each column. 
The first column is a unique identified (i.e. number) for each trial. 
The first column is followed by columns containing the value of each input variable in each trial.
The final column is the value of the output variable in each trial.

Once you have created a log file you can use the tool by downloading the appropriate jar file for your platform and loading the log file. 

The Level 1 checks perform verification on any specified requirements and the Level 2 checks identify conditions correlated with
your output.

Send any questions to ross dot gore at gmail dot com
