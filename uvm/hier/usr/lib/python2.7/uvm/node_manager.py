from uvm.manager import Manager

class NodeManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__nodeManager = self.__uvmContext.nodeManager()

    def api_instantiate(self,packageName):
        nodeProperties = None
        if ( Manager.policyId == None ): node = self.__nodeManager.instantiate( packageName )
        else: node = self.__nodeManager.instantiate( packageName, Manager.policyId )
        nodeSettings = node.getNodeSettings()
        print nodeSettings["id"]
        return nodeSettings

    def api_start( self, nodeIdString ):
        self.__nodeManager.node( int(nodeIdString) ).start()

    def api_stop( self, nodeIdString ):
        self.__nodeManager.node( int(nodeIdString) ).stop()

    def api_destroy( self, nodeIdString ):
        self.__nodeManager.destroy( int(nodeIdString) )

    def get_instances(self):
        if ( Manager.policyId == None ): instanceIds = self.__nodeManager.nodeInstancesIds()
        else: instanceIds = self.__nodeManager.nodeInstancesIds( Manager.policyId )

        nodes = []
        for nodeId in instanceIds["list"]:
            node = self.__get_node(nodeId)
            if node == None:
                print "Node Missing: " + str(nodeId)
                continue

            nodeSettings = node.getNodeSettings()
            nodeProperties = node.getNodeProperties()
            nodeRunState = node.getRunState()

            policyId = None
            if ( nodeSettings.has_key( "policyId" )): policyId = nodeSettings["policyId"]

            if ( policyId == None or policyId == "null" ): 
                policy = "Service"
            else: 
                policy = self.__get_policy_name( policyId )
            
            nodes.append( (nodeSettings['id'], nodeProperties['name'], policy, nodeRunState ) )
        
        nodes =  sorted( nodes, key=lambda v: v[2]) # sort by rack/policy
        return nodes

    def api_instances(self):
        for v in self.get_instances():
            print "%-4s\t%-21s\t%-15s\t%s" % (v[0], v[1], v[3], v[2])

    def api_sessions(self,nodeIdString = None):
        if ( nodeIdString != None ): nodeIds = [ int(nodeIdString) ]
        else: nodeIds = self.__nodeManager.nodeInstances()["list"]

        for nodeId in nodeIds: 
            self.__print_sessions(nodeId)

    def __get_policy_name(self,policyId):
        node = self.__nodeManager.node( "policy-manager" )
        if node == None:
            if (policyId == 1):
                return "Default Policy"
            return "Policy-%i" % policyId
        else:
            return node.getPolicyName( policyId )

    def __print_sessions( self, nodeId ):
        node = self.__get_node( nodeId )
        if ( node == None ): 
            return

        sessions = node.liveSessions()["list"]
        if ( sessions == None ):
            print "NULL Session Desc (nodeId:%i)" % ( nodeId )
            return

        print "Live sessions for %s" % ( node.getNodeProperties()["name"])
        print "Protocol CState SState Client:Client_Port -> Server:Server_Port Created Last Activity"
        for session in sessions: self.__print_session(session)

    def __print_session(self,session):
        print "%s\t%15s : %-5d  -> %15s : %-5d\n" % ( self.formatProtocol(session["protocol"])[1], session["clientAddr"],session["clientPort"], session["serverAddr"],session["serverPort"] ),

    def __get_node( self, nodeId, raiseException = False ):
        node = self.__nodeManager.node( nodeId )
        if ( node == None ):
            print "NULL Node (nodeId:%i)" % (nodeId)
            if ( raiseException ): raise Exception("NULL Node Context (nodeId:%i)" % (nodeId))
            return None
        return node

Manager.managers.append( NodeManager )
