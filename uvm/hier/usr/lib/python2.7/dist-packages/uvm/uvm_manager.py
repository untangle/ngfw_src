from uvm.manager import Manager

class UvmManager(Manager):
    def __init__( self, uvmContext):
        self.__uvmContext = uvmContext

    def api_version( self ):
        print(self.__uvmContext.version())

    def api_gc( self ):
        self.__uvmContext.gc()
        
    def api_setnetcapdebuglevel( self, debugLevel ):
        self.__uvmContext.netcapManager().setNetcapDebugLevel( debugLevel )

    def api_setjnetcapdebuglevel( self, debugLevel ):
        self.__uvmContext.netcapManager().setJNetcapDebugLevel( debugLevel )

    def api_pipeline( self, policy_id, protocol, client_ip, server_ip, client_port, server_port):
        """
        Show application pipeline processing for this policy, IP protocol (e.g.,6=TCP,17=UDP), client ip, server ip, client port, server port.
        """
        pipelines = self.__uvmContext.pipelineFoundry().getPipelineOrder( policy_id, protocol, client_ip, server_ip, client_port, server_port )
        for p in pipelines:
            appName = p['name'] + '*'
            try:
                iterator = iter(p["app"])
                if "appSettings" in p["app"] and "appName" in p["app"]["appSettings"]:
                    appName = p["app"]["appSettings"]["appName"]
            except TypeError:
                pass
            print("{inputFitting:15} {appName:40}({name})".format(inputFitting=p['inputFitting'], appName=appName, name=p['name']))

Manager.managers.append( UvmManager )
