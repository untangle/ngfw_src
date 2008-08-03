from uvm.manager import Manager

class UvmManager(Manager):
    def __init__( self, remoteContext):
        self.__remoteContext = remoteContext

    def api_gc( self ):
        self.__remoteContext.doFullGC()

    def api_loadrup( self ):
        self.__remoteContext.loadRup()

    def api_setproperty( self, key, value ):
        self.__remoteContext.setProperty( key, value )

    def api_restartcliserver( self ):
        self.__remoteContext.restartCliServer()

    def api_stopcliserver( self ):
        self.__remoteContext.stopCliServer()

Manager.managers.append( UvmManager )
