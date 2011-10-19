from uvm.manager import Manager
import time

class LicenseManager(Manager):
    def __init__( self, remoteContext ):
        self.__remoteContext = remoteContext
        self.__licenseManager = self.__remoteContext.licenseManager()

    def api_reloadlicenses( self ):
        self.__licenseManager.reloadLicenses()

    def api_reloadlicenses( self ):
        self.__licenseManager.reloadLicenses()

    def api_getlicensestatus( self, identifier ):
        self.print_status( self.__licenseManager.getLicenseStatus( identifier ))

    def api_getmackagelicense( self, packageName ):
        self.print_status( self.__licenseManager.getPackageStatus( packageName ))

    def print_status( self, status ):
        num_days = int((( status["expirationDate"]["time"] / 1000.0 ) - time.time()) / ( 60 * 60 * 24 ))
        if ( num_days < 0 ): num_days = 0
        print "identifier:  %s" % status["identifier"]
        print "mackage:     %s" % status["packageName"]
        print "has license: %s" % status["hasLicense"]
        print "type:        %s" % status["type"]
        print "expires:     %s day(s)" % num_days
        print "is expired:  %s" % status["isExpired"]

Manager.managers.append( LicenseManager )
