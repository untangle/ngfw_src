from uvm.manager import Manager
import string

class LoggingManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__loggingManager = self.__uvmContext.loggingManager()

    def api_resetlogs(self):
        self.__loggingManager.resetAllLogs()

Manager.managers.append( LoggingManager )
