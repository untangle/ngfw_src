from uvm.manager import Manager

class NodeManager(Manager):
    def __init__(self, remoteContext):
        self.__remoteContext = remoteContext
        self.__nodeManager = self.__remoteContext.nodeManager()

    def api_instantiate(self,mackageName,*args):
        nodeDesc = None
        if ( Manager.policy == None ): nodeDesc = self.__nodeManager.instantiate( mackageName,args )
        else: nodeDesc = self.__nodeManager.instantiate( mackageName, Manager.policy, args )
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

    def api_neverstarted(self,tidString):
        tid = self.buildTid( tidString )
        nodeContext, node = self.__get_node( tid )
        if ( nodeContext == None or node == None ): return
        print ('%s' % node.neverStarted()).lower()

    def api_instances(self):
        if ( Manager.policy == None ): instances = self.__nodeManager.nodeInstances()
        else: instances = self.__nodeManager.nodeInstances( Manager.policy )

        for tid in instances["list"]:
            nodeContext, node = self.__get_node( tid, True )

            policy = None
            if ( tid.has_key( "policy" )): policy = tid["policy"]

            if ( policy == None ): policy = "null"
            else: policy = self.getPolicyString( policy )
            print "%s\t%-25s\t%s\t%s" % ( tid["name"], nodeContext.getNodeDesc()["name"], policy, node.getRunState())

    def api_sessions(self,tidString = None):
        if ( tidString != None ): tids = [ self.buildTid( tidString )]
        else: tids = self.__nodeManager.nodeInstances()["list"]

        for tid in tids: self.__print_sessions(tid)

    def api_dumpsessions(self):
        for tid in self.__nodeManager.nodeInstances()["list"]:
            nodeContext, node = self.__get_node( tid )
            if ( nodeContext == None or node == None ): continue
            node.dumpSessions()

    def __print_sessions( self, tid ):
        nodeContext, node = self.__get_node( tid )
        if ( nodeContext == None or node == None ): return
        sessions = node.liveSessionDescs()["list"]
        if ( sessions == None ):
            print "NULL Session Desc (tid:%s)" % ( tid["id"] )
            return

        print "Live sessions for %s" % ( nodeContext.getNodeDesc()["name"])
        print "ID\t\tC State\tC Addr : C Port\tS State\tS Addr : S Port\t",
        print "Created\t\tLast Activity\tC->T B\tT->S B\tS->T B\tT->C B"
        for session in sessions: self.__print_session(session)

    def __print_session(self,session):
        stats = session["stats"]
        print "%s%d\t%s\t%s:%d\t%s\t%s:%d" % ( self.formatProtocol(session["protocol"])[1], session["id"], self.formatState( session["clientState"] ), session["clientAddr"],session["clientPort"], self.formatState( session["serverState"] ), session["serverAddr"],session["serverPort"] ),
        print "\t%s\t%s" % ( self.formatTime( stats["creationDate"] ),
                             self.formatTime( stats["lastActivityDate"] )),
        print "\t%d\t%d\t%d\t%d" % ( stats["c2tBytes"], stats["t2sBytes"], stats["s2tBytes"], stats["t2cBytes"] )

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
