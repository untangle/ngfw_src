from uvm.manager import Manager

class NodeManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__nodeManager = self.__uvmContext.nodeManager()

    def api_instantiate(self,packageName,*args):
        nodeProperties = None
        if ( Manager.policyId == None ): nodeProperties = self.__nodeManager.instantiate( packageName,args )
        else: nodeProperties = self.__nodeManager.instantiate( packageName, Manager.policyId, args )
        nodeSettings = nodeProperties["nodeSettings"]
        print nodeSettings["id"]
        return nodeSettings

    def api_start( self, nodeIdString ):
        self.__nodeManager.nodeContext( int(nodeIdString) ).node().start()

    def api_stop( self, nodeIdString ):
        self.__nodeManager.nodeContext( int(nodeIdString) ).node().stop()

    def api_destroy( self, nodeIdString ):
        self.__nodeManager.destroy( int(nodeIdString) )

    def get_instances(self):
        if ( Manager.policyId == None ): instances = self.__nodeManager.nodeInstances()
        else: instances = self.__nodeManager.nodeInstances( Manager.policyId )

        nodes = []
        for nodeSettings in instances["list"]:
            nodeContext, node = self.__get_node( nodeSettings, True )

            policyId = None
            if ( nodeSettings.has_key( "policyId" )): policyId = nodeSettings["policyId"]

            if ( policyId == None or policyId == "null" ): 
                policy = "Service"
            else: 
                policy = self.getPolicyString( policyId )
            
            nodes.append( (nodeSettings['id'], nodeContext.getNodeProperties()["name"], policy, node.getRunState()) )
        
        nodes =  sorted( nodes, key=lambda v: v[2]) # sort by rack/policy
        return nodes

    def api_instances(self):
        for v in self.get_instances():
            print "%-4s\t%-25s\t%-15s\t%s" % (v[0], v[1], v[3], v[2])

    def api_sessions(self,nodeIdString = None):
        if ( nodeIdString != None ): nodeIds = [ int(nodeIdString) ]
        else: nodeIds = self.__nodeManager.nodeInstances()["list"]

        for nodeId in nodeIds: self.__print_sessions(nodeId)

    def __print_sessions( self, nodeId ):
        nodeContext, node = self.__get_node( nodeId )
        if ( nodeContext == None or node == None ): return
        sessions = node.liveSessionDescs()["list"]
        if ( sessions == None ):
            print "NULL Session Desc (nodeId:%i)" % ( nodeId )
            return

        print "Live sessions for %s" % ( nodeContext.getNodeProperties()["name"])
        print "Protocol CState SState Client:Client_Port -> Server:Server_Port Created Last Activity"
        for session in sessions: self.__print_session(session)

    def __print_session(self,session):
        stats = session["stats"]
        print "%s\t%-6s\t%-6s\t%15s:%-5d -> %15s:%-5d" % ( self.formatProtocol(session["protocol"])[1], self.formatState( session["clientState"] ), self.formatState( session["serverState"] ), session["clientAddr"],session["clientPort"], session["serverAddr"],session["serverPort"] ),
        print "\t%s\t%s\n" % ( self.formatTime( stats["creationDate"] ), self.formatTime( stats["lastActivityDate"] )),

    def __get_node( self, nodeId, raiseException = False ):
        nodeContext = self.__nodeManager.nodeContext( nodeId )
        if ( nodeContext == None ):
            print "NULL Node Context (nodeId:%i)" % (nodeId)
            if ( raiseException ): raise Exception("NULL Node Context (nodeId:%i)" % (nodeId))
            return [ None, None ]
        node = nodeContext.node()
        if ( node == None ):
            print "NULL Node (nodeId:%i)" % (nodeId)
            if ( raiseException ): raise Exception("NULL Node (nodeId:%i)" % (nodeId))
            return [ None, None ]

        return [ nodeContext, node ]

Manager.managers.append( NodeManager )
