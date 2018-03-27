from uvm.manager import Manager

class UvmManager(Manager):
    def __init__( self, uvmContext):
        self.__uvmContext = uvmContext

    def api_version( self ):
        print(self.__uvmContext.version())

    def api_gc( self ):
        self.__uvmContext.gc()
        
    def api_setnetcapdebuglevel( self, debugLevel ):
        self.__uvmContext.netcapManager().setNetcapDebugLevel( debugLevel )

    def api_setjnetcapdebuglevel( self, debugLevel ):
        self.__uvmContext.netcapManager().setJNetcapDebugLevel( debugLevel )

Manager.managers.append( UvmManager )
