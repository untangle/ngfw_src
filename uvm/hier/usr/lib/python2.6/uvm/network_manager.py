from uvm.manager import Manager

class NetworkManager(Manager):
    def __init__( self, uvmContext):
        self.__uvmContext = uvmContext
        self.__networkManager = self.__uvmContext.networkManager()

    def api_refreshnetworkconfig( self ):
        self.__networkManager.refreshNetworkConfig()

Manager.managers.append( NetworkManager )
