import os
import sys
import subprocess
import simplejson as json

class SystemProperties:

    def getInterface(self, name):
        NETCONFIG_JSON_OBJ = json.loads(open('/usr/share/untangle/settings/untangle-vm/network.js', 'r').read())
        for intf in NETCONFIG_JSON_OBJ['interfaces']['list']:
            if intf['name'] == name:
                return intf
        return None

    def internalInterfaceIP(self):
        # FIXME
        return "UnknownIP"
        
        # intf = self.getInterface("Internal")

        # while intf['configType'] == 'bridge':
        #     intf = self.getInterface(intf['bridgedTo'])

        # if intf['primaryAddressStr'] != None:
        #     return intf['primaryAddressStr'].split("/")[0]
        # else:
        #     return "UnknownIP"


        
