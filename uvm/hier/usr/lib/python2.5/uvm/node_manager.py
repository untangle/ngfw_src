from uvm.manager import Manager

class NodeManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__nodeManager = self.__uvmContext.nodeManager()

    def api_instantiate(self,packageName,*args):
        nodeDesc = None
        if ( Manager.policy == None ): nodeDesc = self.__nodeManager.instantiate( packageName,args )
        else: nodeDesc = self.__nodeManager.instantiate( packageName, Manager.policy, args )
        nodeId = nodeDesc["nodeId"]
        print nodeId["id"]
        return nodeId

    def api_start( self, nodeIdString ):
        nodeId = self.buildNodeId( nodeIdString )
        self.__nodeManager.nodeContext( nodeId["id"] ).node().start()

    def api_stop( self, nodeIdString ):
        nodeId = self.buildNodeId( nodeIdString )
        self.__nodeManager.nodeContext( nodeId["id"] ).node().stop()

    def api_destroy( self, nodeIdString ):
        nodeId = self.buildNodeId( nodeIdString )
        self.__nodeManager.destroy( nodeId )

    def get_instances(self):
        if ( Manager.policy == None ): instances = self.__nodeManager.nodeInstances()
        else: instances = self.__nodeManager.nodeInstances( Manager.policy )

        nodes = []
        for nodeId in instances["list"]:
            nodeContext, node = self.__get_node( nodeId, True )

            policy = None
            if ( nodeId.has_key( "policy" )): policy = nodeId["policy"]

            if ( policy == None or policy == "null" ): policy = "Service"
            else: policy = self.getPolicyString( policy )
            
            nodes.append( (nodeId['id'], nodeContext.getNodeDesc()["name"], policy, node.getRunState()) )
        
        nodes =  sorted( nodes, key=lambda v: v[2]) # sort by rack/policy
        return nodes

    def api_instances(self):
        for v in self.get_instances():
            print "%-4s\t%-25s\t%-15s\t%s" % (v[0], v[1], v[3], v[2])

    def api_sessions(self,nodeIdString = None):
        if ( nodeIdString != None ): nodeIds = [ self.buildNodeId( nodeIdString )]
        else: nodeIds = self.__nodeManager.nodeInstances()["list"]

        for nodeId in nodeIds: self.__print_sessions(nodeId)

    def __print_sessions( self, nodeId ):
        nodeContext, node = self.__get_node( nodeId )
        if ( nodeContext == None or node == None ): return
        sessions = node.liveSessionDescs()["list"]
        if ( sessions == None ):
            print "NULL Session Desc (nodeId:%s)" % ( nodeId["id"] )
            return

        print "Live sessions for %s" % ( nodeContext.getNodeDesc()["name"])
        print "Protocol\tClient_State\tServer_State\tClient_Addr:Client_Port -> Server_Addr:Server_Port\t",
        print "Created\t\tLast Activity"
        for session in sessions: self.__print_session(session)

    def __print_session(self,session):
        stats = session["stats"]
        print "%s\t%-6s\t%-6s\t%15s:%-5d -> %15s:%-5d" % ( self.formatProtocol(session["protocol"])[1], self.formatState( session["clientState"] ), self.formatState( session["serverState"] ), session["clientAddr"],session["clientPort"], session["serverAddr"],session["serverPort"] ),
        print "\t%s\t%s\n" % ( self.formatTime( stats["creationDate"] ), self.formatTime( stats["lastActivityDate"] )),

    def __get_node( self, nodeId, raiseException = False ):
        nodeContext = self.__nodeManager.nodeContext( nodeId["id"] )
        if ( nodeContext == None ):
            print "NULL Node Context (nodeId:%s)" % (nodeId["id"])
            if ( raiseException ): raise Exception("NULL Node Context (nodeId:%s)" % (nodeId["id"]))
            return [ None, None ]
        node = nodeContext.node()
        if ( node == None ):
            print "NULL Node (nodeId:%s)" % (nodeId["id"])
            if ( raiseException ): raise Exception("NULL Node (nodeId:%s)" % (nodeId["id"]))
            return [ None, None ]

        return [ nodeContext, node ]

Manager.managers.append( NodeManager )
