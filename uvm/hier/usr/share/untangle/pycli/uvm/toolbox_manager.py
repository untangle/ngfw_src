from uvm.manager import Manager

class ToolboxManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__toolbox = self.__uvmContext.toolboxManager()

    def api_install(self, packageName):
        key = self.__toolbox.install( packageName )
        self.doAptTailLog( key )

    def api_installandinstantiate(self,packageName):
        policy = self.__uvmContext.policyManager().getDefaultPolicy()
        key = self.__toolbox.installAndInstantiate(packageName, policy)
        self.doAptTailLog( key )

    def api_uninstall(self, packageName):
        self.__toolbox.uninstall( packageName )

    def api_update(self):
        self.__toolbox.update()

    def api_upgrade(self):
        key = self.__toolbox.upgrade()
        self.doAptTailLog( key )

    def api_requestinstall(self, packageName):
        self.__toolbox.requestInstall( package )

    def api_available(self):
        self.__print_packages( self.__toolbox.available())

    def api_installed(self):
        self.__print_packages( self.__toolbox.installed())

    def api_uninstalled(self):
        self.__print_packages( self.__toolbox.uninstalled())

    def api_register(self, packageName):
        print "Registering the package: ", packageName
        self.__toolbox.register(packageName)

    def api_unregister(self, packageName):
        print "Unregistering the package: ", packageName
        self.__toolbox.unregister(packageName)

    def __print_packages(self,packages):
        for package in packages:
            print "%-30s installed: %-40savailable: %s" % ( package["name"], package["installedVersion"], package["availableVersion"])

Manager.managers.append( ToolboxManager )
