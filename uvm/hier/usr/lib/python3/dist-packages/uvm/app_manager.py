from uvm.manager import Manager

class AppManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__appManager = self.__uvmContext.appManager()

    def api_instantiate(self,packageName):
        appProperties = None
        if ( Manager.policyId == None ): app = self.__appManager.instantiate( packageName )
        else: app = self.__appManager.instantiate( packageName, Manager.policyId )
        appSettings = app.getAppSettings()
        print((appSettings["id"]))
        return appSettings

    def api_start( self, appIdString ):
        self.__appManager.app( int(appIdString) ).start()

    def api_stop( self, appIdString ):
        self.__appManager.app( int(appIdString) ).stop()

    def api_destroy( self, appIdString ):
        self.__appManager.destroy( int(appIdString) )

    def get_instances(self):
        if ( Manager.policyId == None ): instanceIds = self.__appManager.appInstancesIds()
        else: instanceIds = self.__appManager.appInstancesIds( Manager.policyId )

        apps = []
        for appId in instanceIds["list"]:
            app = self.__get_app(appId)
            if app == None:
                print(("App Missing: " + str(appId)))
                continue

            appSettings = app.getAppSettings()
            appProperties = app.getAppProperties()
            appRunState = app.getRunState()

            policyId = None
            if ( "policyId" in appSettings): policyId = appSettings["policyId"]

            if ( policyId == None or policyId == "null" ): 
                policy = "Service"
            else: 
                policy = self.__get_policy_name( policyId )
            
            apps.append( (appSettings['id'], appProperties['name'], policy, appRunState ) )
        
        apps =  sorted( apps, key=lambda v: v[2]) # sort by rack/policy
        return apps

    def api_instances(self):
        for v in self.get_instances():
            print(("%-4s\t%-21s\t%-15s\t%s" % (v[0], v[1], v[3], v[2])))

    def api_sessions(self,appIdString = None):
        if ( appIdString != None ): appIds = [ int(appIdString) ]
        else: appIds = self.__appManager.appInstances()["list"]

        for appId in appIds: 
            self.__print_sessions(appId)

    def __get_policy_name(self,policyId):
        app = self.__appManager.app( "policy-manager" )
        if app == None:
            if (policyId == 1):
                return "Default Policy"
            return "Policy-%i" % policyId
        else:
            return app.getPolicyName( policyId )

    def __print_sessions( self, appId ):
        app = self.__get_app( appId )
        if ( app == None ): 
            return

        sessions = app.liveSessions()["list"]
        if ( sessions == None ):
            print(("NULL Session Desc (appId:%i)" % ( appId )))
            return

        print(("Live sessions for %s" % ( app.getAppProperties()["name"])))
        print("Protocol CState SState Client:Client_Port -> Server:Server_Port Created Last Activity")
        for session in sessions: self.__print_session(session)

    def __print_session(self,session):
        print(("%s\t%15s : %-5d  -> %15s : %-5d\n" % ( self.formatProtocol(session["protocol"])[1], session["clientAddr"],session["clientPort"], session["serverAddr"],session["serverPort"] ),))

    def __get_app( self, appId, raiseException = False ):
        app = self.__appManager.app( appId )
        if ( app == None ):
            print(("NULL App (appId:%i)" % (appId)))
            if ( raiseException ): raise Exception("NULL App Context (appId:%i)" % (appId))
            return None
        return app

Manager.managers.append( AppManager )
