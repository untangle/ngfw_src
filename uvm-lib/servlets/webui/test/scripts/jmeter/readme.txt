This file contains the necessary information required to run JMeter scripts.

Pre-settings:
- you need to have Java 1.4 or 1.5 installed (only the jre is required)

Environment settings:
- download the latest version of JMeter (windows users should choose the zip file):
    http://jakarta.apache.org/site/downloads/downloads_jmeter.cgi
- unpack the jakarta-jmeter-2.3.1.zip file to some\test\folder
- cd to some\test\folder\jakarta-jmeter-2.3.1\bin
- create folder: scripts (the folder which will contain the testscripts)
- create folder: results (the folder in which JMeter will save the results cvs files)
- create folder: textfiles (the folder which will contain the files needed by JMeter Scripts; these files contain data for parameters - a simple way to avoid databases)
- copy the downloaded scripts (the *.jmx files) to some\test\folder\jakarta-jmeter-2.3.1\bin\scripts
- copy the downloaded textfiles (the *.txt files) to some\test\folder\jakarta-jmeter-2.3.1\bin\textfiles

Running tests under JMeter:
- cd to some\test\folder\jakarta-jmeter-2.3.1\bin
- execute jmeter.bat (if you have the system variables for java set up you will not have any problems; if you don't, you will need to edit this file and modify the appropriate variables)
- click File -> Open
    - cd to some\test\folder\jakarta-jmeter-2.3.1\bin\scripts
    - select the desired test script and click Open
- to run the test press: <Control>+<R> (you can also use the menu)
- to stop the test press: <Control>+<.> (you can also use the menu)
- if a test is running a green square will be placed under the X CloseButton of the window
- to see the results of the requests in real-time click "Aggregate Report" (this is found in the left panel)
- to clear the results press: <Control>+<E> (you can also use the menu)
    - clearing the results will not delete/empty the results file
    - rerunning the test will replace the results file
- if/when errors occur click the "View Results Tree" (this is also found in the left panel, right under "Aggregate Report") to see details about the errors (request and parameters used, http headers, response html code, web-page generated om the html code)
- on the left panel there are multiple controls; these are very well explained in JMeter's documentation; you can also try using the help provided in JMeter, I find it very simple and useful.
- when launching a test that makes https requests, right at the beginning a pop-up will appear asking you for a password. You should just hit Enter every time you get that pop-up. If you do enter a password the tests will fail. The password is then cached, and the only way to get that pop-up again is to restart JMeter.
