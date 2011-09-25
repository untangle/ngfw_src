from uvm.manager import Manager

class UvmManager(Manager):
    def __init__( self, uvmContext):
        self.__uvmContext = uvmContext

    def api_gc( self ):
        self.__uvmContext.doFullGC()

Manager.managers.append( UvmManager )
