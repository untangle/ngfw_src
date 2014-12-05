import errno
import json
import os
import re
from netaddr import IPNetwork, IPAddress

class IdpsSettings:
    #
    # NGFW settings management
    #
    def __init__( self, nodeId ):
        self.nodeId = nodeId
        self.file_name = "/usr/share/untangle/settings/untangle-node-idps/settings_" + self.nodeId + ".js"

    def load( self ):
        self.settings_file = open( self.file_name )
        self.settings = json.load( self.settings_file )
        self.settings_file.close()
        
    def exists( self ):
        return os.path.exists( self.file_name )

    def create( self, conf, rules ):
        #
        # Create a new settings file based on the processed
        # rule set and default variables from snort configuration.
        #
        self.settings = { 
            "variables": {
                "list": []
            },
            "rules": {
                "list": []
            },
            "interfaces": {
                "list": []
            }
        }
        
        network_settings_file = open( "/usr/share/untangle/settings/untangle-vm/network.js" )
        network_settings = json.load( network_settings_file )
        network_settings_file.close()
        
        default_interfaces = []
        default_home_net = []
        for interface in network_settings["interfaces"]["list"]:
            default_interfaces.append(interface["systemDev"])
            if interface["isWan"] == False and "v4StaticAddress" in interface:
                network = IPNetwork( interface["v4StaticAddress"] + "/" + str(interface["v4StaticPrefix"]) ).cidr
                default_home_net.append( network )
                for alias in interface["v4Aliases"]["list"]:
                    network = IPNetwork( alias["staticAddress"] + "/" + str( alias["staticPrefix"] ) ).cidr
                    default_home_net.append( network )

        self.settings["interfaces"]["list"] = default_interfaces
        default_home_net = set(default_home_net)
        
        for variable in rules.get_variables():
            definition = "default value"
            description = "default description"
        
            for default_variable in conf.get_variables():
                if default_variable["key"] == variable:
                    if default_variable["key"] == "HOME_NET":
                        definition = ",".join(map(str,default_home_net))
                        if len(default_home_net) > 1:
                            definition = "[" + definition + "]"
                    else:
                        definition = default_variable["value"]
                    description = default_variable["description"]
                    break
        
            self.settings["variables"]["list"].append( { 
                "variable": variable,
                "definition": definition,
                "description": description
            } );
         
        for rule in rules.get_rules():
            self.settings["rules"]["list"].append( { 
                "sid": rule.options["sid"],
                "name": rule.options["sid"],
                "description": rule.options["msg"],
                "live": False,
                "log": rule.enabled == True and rule.action == "alert",
                "category": rule.options["classtype"],
                "text": rule.build()
            } );
        
    def save( self ):
        settings_file = open( self.file_name, "w" )
        json.dump( self.settings, settings_file, False, True, True, True, None, 0 )
        settings_file.close()

    def get_rules( self ):
       return self.settings["rules"]["list"]

    def get_variables( self ):
        return self.settings["variables"]["list"]

    def get_variable( self, key ):
        for variable in self.settings["variables"]["list"]:
            if key == variable["variable"]:
                return variable["definition"]

    def get_interfaces( self ):
        return self.settings["interfaces"]["list"]
