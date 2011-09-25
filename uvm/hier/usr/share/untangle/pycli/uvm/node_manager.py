from uvm.manager import Manager

class NodeManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__nodeManager = self.__uvmContext.nodeManager()

    def api_instantiate(self,packageName,*args):
        nodeDesc = None
        if ( Manager.policy == None ): nodeDesc = self.__nodeManager.instantiate( packageName,args )
        else: nodeDesc = self.__nodeManager.instantiate( packageName, Manager.policy, args )
        tid = nodeDesc["tid"]
        print tid["id"]
        return tid

    def api_start( self, tidString ):
        tid = self.buildTid( tidString )
        self.__nodeManager.nodeContext( tid ).node().start()

    def api_stop( self, tidString ):
        tid = self.buildTid( tidString )
        self.__nodeManager.nodeContext( tid ).node().stop()

    def api_destroy( self, tidString ):
        tid = self.buildTid( tidString )
        self.__nodeManager.destroy( tid )

    def get_instances(self):
        if ( Manager.policy == None ): instances = self.__nodeManager.nodeInstances()
        else: instances = self.__nodeManager.nodeInstances( Manager.policy )

        nodes = []
        for tid in instances["list"]:
            nodeContext, node = self.__get_node( tid, True )

            policy = None
            if ( tid.has_key( "policy" )): policy = tid["policy"]

            if ( policy == None or policy == "null" ): policy = "Service"
            else: policy = self.getPolicyString( policy )
            
            nodes.append( (tid['id'], nodeContext.getNodeDesc()["name"], policy, node.getRunState()) )
        
        print nodes
        nodes =  sorted( nodes, key=lambda v: v[2]) # sort by rack/policy
        return nodes

    def api_instances(self):
        for v in self.get_instances():
            print "%-4s\t%-25s\t%-15s\t%s" % (v[0], v[1], v[3], v[2])

    def api_sessions(self,tidString = None):
        if ( tidString != None ): tids = [ self.buildTid( tidString )]
        else: tids = self.__nodeManager.nodeInstances()["list"]

        for tid in tids: self.__print_sessions(tid)

    def __print_sessions( self, tid ):
        nodeContext, node = self.__get_node( tid )
        if ( nodeContext == None or node == None ): return
        sessions = node.liveSessionDescs()["list"]
        if ( sessions == None ):
            print "NULL Session Desc (tid:%s)" % ( tid["id"] )
            return

        print "Live sessions for %s" % ( nodeContext.getNodeDesc()["name"])
        print "Protocol\tClient_State\tServer_State\tClient_Addr:Client_Port -> Server_Addr:Server_Port\t",
        print "Created\t\tLast Activity"
        for session in sessions: self.__print_session(session)

    def __print_session(self,session):
        stats = session["stats"]
        print "%s\t%-6s\t%-6s\t%15s:%-5d -> %15s:%-5d" % ( self.formatProtocol(session["protocol"])[1], self.formatState( session["clientState"] ), self.formatState( session["serverState"] ), session["clientAddr"],session["clientPort"], session["serverAddr"],session["serverPort"] ),
        print "\t%s\t%s" % ( self.formatTime( stats["creationDate"] ), self.formatTime( stats["lastActivityDate"] )),

    def __get_node( self, tid, raiseException = False ):
        nodeContext = self.__nodeManager.nodeContext( tid )
        if ( nodeContext == None ):
            print "NULL Node Context (tid:%s)" % (tid["id"])
            if ( raiseException ): raise Exception("NULL Node Context (tid:%s)" % (tid["id"]))
            return [ None, None ]
        node = nodeContext.node()
        if ( node == None ):
            print "NULL Node (tid:%s)" % (tid["id"])
            if ( raiseException ): raise Exception("NULL Node (tid:%s)" % (tid["id"]))
            return [ None, None ]

        return [ nodeContext, node ]

Manager.managers.append( NodeManager )
