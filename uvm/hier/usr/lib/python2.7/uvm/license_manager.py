from uvm.manager import Manager
import time

class LicenseManager(Manager):
    def __init__( self, remoteContext ):
        self.__remoteContext = remoteContext
        self.__licenseManager = self.__remoteContext.licenseManager()

    def api_reloadlicenses( self ):
        self.__licenseManager.reloadLicenses()

Manager.managers.append( LicenseManager )
