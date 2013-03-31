from uvm.manager import Manager

class AptManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__apt = self.__uvmContext.aptManager()

    def api_install(self, packageName):
        key = self.__apt.install( packageName )
        self.doAptTailLog( key )

    def api_installandinstantiate(self,packageName):
        key = self.__apt.installAndInstantiate(packageName, 1L)
        self.doAptTailLog( key )

    def api_update(self):
        self.__apt.update()

    def api_upgrade(self):
        key = self.__apt.upgrade()
        self.doAptTailLog( key )

    def api_requestinstall(self, packageName):
        self.__apt.requestInstall( package )

    def api_available(self):
        self.__print_packages( self.__apt.available())

    def api_installed(self):
        self.__print_packages( self.__apt.installed())

    def api_uninstalled(self):
        self.__print_packages( self.__apt.uninstalled())

    def api_register(self, packageName):
        print "Registering the package: ", packageName
        self.__apt.register(packageName)

    def api_unregister(self, packageName):
        print "Unregistering the package: ", packageName
        self.__apt.unregister(packageName)

    def __print_packages(self,packages):
        for package in packages:
            print "%-30s installed: %-40savailable: %s" % ( package["name"], package["installedVersion"], package["availableVersion"])

Manager.managers.append( AptManager )
