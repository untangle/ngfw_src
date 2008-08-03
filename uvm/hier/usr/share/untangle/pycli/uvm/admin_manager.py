from uvm.manager import Manager

class AdminManager(Manager):
    __registrationKeys = [ "companyName", "firstName", "lastName", "emailAddr", "numSeats",  "address1", "address2", "city", "state", "zipcode", "phone", ]
    def __init__(self, remoteContext):
        self.__remoteContext = remoteContext
        self.__adminManager = self.__remoteContext.adminManager()

    def api_getreginfo(self):
        regInfo = self.__adminManager.getRegistrationInfo()
        if ( regInfo == None ): print "No registration info found!"
        else : print self.__printRegistrationInfo( regInfo )

    def api_shutdown(self):
        self.__remoteContext.shutdown()

    def __printRegistrationInfo( self, regInfo ):
        isFirst = True
        print "RegistrationInfo ["
        for field in ToolboxManager.__registrationKeys:
            if ( regInfo.hasKey( field )):
                if ( not isFirst ): print ",",
                isFirst = False
                print " %s = %s" % ( field, regInfo["field"] ),

        misc = regInfo["misc"]["map"]
        for field in misc.keys():
            if ( not isFirst ): print ",",
            isFirst = False
            print " %s = %s" % ( field, misc[field] ),

        print " ]"

Manager.managers.append( AdminManager )
