from uvm.manager import Manager
from datetime import datetime
from time import strptime
from string import atoi

class ReportingManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__reportingManager = self.__uvmContext.reportingManager()

    def api_isreportingenabled(self):
        print ('%s' % self.__reportingManager.isReportingEnabled()).lower()

    def api_arereportsavailable(self):
        print ('%s' % self.__reportingManager.isReportsAvailable()).lower()

Manager.managers.append( ReportingManager )

