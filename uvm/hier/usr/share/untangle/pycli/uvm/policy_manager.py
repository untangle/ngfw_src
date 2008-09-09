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

    def api_getpolicy( self, policy_name ):
        print self.getPolicyString( self.__policyManager.getPolicy( policy_name ))
                                        
Manager.managers.append( PolicyManager )

