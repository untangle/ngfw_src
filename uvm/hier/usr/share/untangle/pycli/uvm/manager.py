from time import strftime, gmtime

class Manager(object):
    __protocols = { 6 : [ "TCP", "T" ], 17 : [ "UDP", "U" ] }
    __states = { 0 : "CLOSED", 4 : "OPEN", 5 : "H_IN", 6 : "H_OUT" }

    managers = []

    policy = None
    verbosity = 0
    
    def shortNameToPackageName(self,shortName):
        if ( shortName == "http" or shortName == "ftp" or shortName == "mail" ): return "untangle-casing-" + shortName
        return "untangle-node-" + shortName

    def doAptTailLog(self, key ):
        print "Operation started, please apt.log for more information"

    def buildTid(self,tid):
        tid = int( tid )
        return {'javaClass': 'com.untangle.uvm.security.NodeId', 'id': tid, 'name': "%d" % ( tid ) }

    def buildDate(self,seconds):
        return { 'javaClass' : 'java.util.Date', 'time' : ( seconds * 1000 ) }

    def getPolicyString(self,policy):
        return "Policy(%s: %s)" % ( [ "non-default", "default" ][policy["default"]], policy["name"] )

    def formatProtocol(self,protocol):
        return Manager.__protocols[int(protocol)]
    
    def formatState( self, state ):
        return Manager.__states[int(state)]

    def formatTime( self, time ):
        millis = time["time"]
        format = "%H:%M:%S," + str( millis % 1000 )
        return strftime( format, gmtime( millis / 1000 ))
