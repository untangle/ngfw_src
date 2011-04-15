from uvm.manager import Manager

class UvmManager(Manager):
    def __init__( self, remoteContext):
        self.__remoteContext = remoteContext

    def api_gc( self ):
        self.__remoteContext.doFullGC()

Manager.managers.append( UvmManager )
