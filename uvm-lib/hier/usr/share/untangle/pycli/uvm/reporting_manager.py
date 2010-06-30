from uvm.manager import Manager
from datetime import datetime
from time import strptime
from string import atoi

class ReportingManager(Manager):
    def __init__(self, remoteContext):
        self.__remoteContext = remoteContext
        self.__reportingManager = self.__remoteContext.reportingManager()

    def api_isreportingenabled(self):
        print ('%s' % self.__reportingManager.isReportingEnabled()).lower()

    def api_arereportsavailable(self):
        print ('%s' % self.__reportingManager.isReportsAvailable()).lower()

    def api_isreportingrunning(self):
        print ('%s' % self.__reportingManager.isRunning()).lower()

    def api_startreports(self):
        print "Report generation starting"
        self.__reportingManager.startReports()

    def api_stopreports(self):
        print "Report generation stopping"
        self.__reportingManager.stopReports()

    def api_preparereports( self, *args ):
        parsedArgs = 0
        outputBaseDirName = "/tmp"
        daysToKeep = 90
        midnight = datetime.now().replace( minute =  0, second = 0, hour = 0, microsecond = 0 )
        args = list( args )
        if ( args.count( "-o" ) > 0 ):
            outputBaseDirName = args[args.index( "-o" ) + 1]
            parsedArgs += 2
        if ( args.count( "-d" ) > 0 ):
            daysToKeep = int( args[args.index( "-d" ) + 1] )
            parsedArgs += 2
        if ( args.count( "-n" ) > 0 ):
            arg = args[args.index( "-n" ) + 1]
            midnight = datetime( *strptime( arg, "%Y-%m-%d" )[0:6] )
            parsedArgs += 2

        if ( parsedArgs != len( args )):
            raise Exception( "invalid arguments: " + args )

        if ( daysToKeep < 1 ): daysToKeep = 1

        print "Preparing for report generation to ", outputBaseDirName
        midnight = self.buildDate( atoi( midnight.strftime( "%s" ) ) )
        self.__reportingManager.prepareReports( outputBaseDirName, midnight, daysToKeep )

Manager.managers.append( ReportingManager )

