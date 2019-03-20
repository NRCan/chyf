----------------------------------------------
CHyF Data Tools Application
----------------------------------------------
This application provides tools for preprocessing CHyF compliant 
data to add additional attributes.  Currently two processes are supported:
* Adding slope, aspect, and elevation statistics to catchment polygons
* Adding distance to water 2D to catchment polygons


----------------------------------------------
--- Requirements ---
----------------------------------------------
Java version 8 or newer must be installed and included on
the classpath.  To test "run java -version" from a command line.
Depending on the version and jdk installed you 
may see 1.<VERSION> or just <VERSION>. (1.8 OR 8.0). 


---------------------------------------
--- Running on Windows ---
---------------------------------------
To run on windows double click the run_win.bat file.

---------------------------------------
--- Running on Linux ---
---------------------------------------
To run on linux, run the run_linux.sh script.


---------------------------------------
--- Sample Data ---
---------------------------------------
Two sample datasets exist in the packaged application that can
be used for testing the running of the application.

* testdata/slopeaspectelevation - datasets for testing the Slope, Aspect, and Elevation statistics computations
* testdata/distance2water2d - datasets for testing the distance to water 2d statistics computations

