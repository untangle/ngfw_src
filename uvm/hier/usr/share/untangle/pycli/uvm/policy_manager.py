from uvm.manager import Manager

class PolicyManager(Manager):
    def __init__( self, remoteContext ):
        self.__remoteContext = remoteContext
        self.__policyManager = self.__remoteContext.policyManager()

    def api_addpolicy( self, policyName, notes = "no description" ):
        self.__policyManager.addPolicy( policyName, notes )

    def api_listpolicies( self ):
        for policy in self.__policyManager.getPolicies():
            print self.getPolicyString( policy )
                                        
Manager.managers.append( PolicyManager )

