from uvm.manager import Manager
import string

class LoggingManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__loggingManager = self.__uvmContext.loggingManager()

    def api_resetlogs(self):
        self.__loggingManager.resetAllLogs()

    def api_logerror(self, *args ):
        if ( len( args ) == 0 ):
            self.__loggingManager.logError( None )
            return
        
        message = string.join( args, " " )
        self.__loggingManager.logError( message )

Manager.managers.append( LoggingManager )
