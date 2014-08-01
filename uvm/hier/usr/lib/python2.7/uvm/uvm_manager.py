from uvm.manager import Manager

class UvmManager(Manager):
    def __init__( self, uvmContext):
        self.__uvmContext = uvmContext

    def api_version( self ):
        print self.__uvmContext.version()

Manager.managers.append( UvmManager )
