from uvm.manager import Manager

class ToolboxManager(Manager):
    def __init__(self, remoteContext):
        self.__remoteContext = remoteContext
        self.__toolbox = self.__remoteContext.toolboxManager()

    def api_install(self, mackageName):
        key = self.__toolbox.install( mackageName )
        self.doAptTailLog( key )

    def api_installandinstantiate(self,mackageName):
        policy = self.__remoteContext.policyManager().getDefaultPolicy()
        key = self.__toolbox.installAndInstantiate(mackageName, policy)
        self.doAptTailLog( key )

    def api_uninstall(self, mackageName):
        self.__toolbox.uninstall( mackageName )

    def api_update(self):
        self.__toolbox.update()

    def api_upgrade(self):
        key = self.__toolbox.upgrade()
        self.doAptTailLog( key )

    def api_requestinstall(self, mackageName):
        self.__toolbox.requestInstall( mackage )

    def api_available(self):
        self.__print_mackages( self.__toolbox.available())

    def api_installed(self):
        self.__print_mackages( self.__toolbox.installed())

    def api_uninstalled(self):
        self.__print_mackages( self.__toolbox.uninstalled())

    def api_upgradable(self):
        self.__print_mackages( self.__toolbox.upgradable())

    def api_uptodate(self):
        pkgs = self.__toolbox.upgradable()
        for pkg in pkgs:
            print "name: %s\tinstalled: %s\tavailable: %s" % ( pkg["name"], pkg["installedVersion"], pkg["availableVersion"])

    def api_register(self, mackageName):
        print "Registering the mackage: ", mackageName
        self.__toolbox.register(mackageName)

    def api_unregister(self, mackageName):
        print "Unregistering the mackage: ", mackageName
        self.__toolbox.unregister(mackageName)

    def api_apttail( self ):
        print "implement me"

    def __print_mackages(self,mackages):
        for mackage in mackages:
            print "%-30s installed: %-40savailable: %s" % ( mackage["name"], mackage["installedVersion"], mackage["availableVersion"])

Manager.managers.append( ToolboxManager )
