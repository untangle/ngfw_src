from uvm.manager import Manager

class SystemManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__systemManager = self.__uvmContext.systemManager()

    def api_upgrade(self):
        self.__systemManager.upgrade()

Manager.managers.append( SystemManager )
