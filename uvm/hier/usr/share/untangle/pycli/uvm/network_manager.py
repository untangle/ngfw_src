from uvm.manager import Manager

class NetworkManager(Manager):
    def __init__( self, remoteContext):
        self.__remoteContext = remoteContext
        self.__networkManager = self.__remoteContext.networkManager()

    def api_updateaddress( self ):
        self.__networkManager.refreshNetworkConfig()

    def api_refreshnetworkconfig( self ):
        self.__networkManager.refreshNetworkConfig()

Manager.managers.append( NetworkManager )
