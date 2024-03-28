import copy
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import tests.global_functions as global_functions
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import runtests.overrides as overrides


SYSLOG_SERVER1 = {"enabled": False, "host": "192.168.56.195", "javaClass": "com.untangle.uvm.event.SyslogServer", "port": 514, "protocol": "UDP", "serverId": -1, "tag": "uvm-to-192.168.56.195" }
SYSLOG_SERVER2 = {"enabled": False, "host": "192.168.56.196", "javaClass": "com.untangle.uvm.event.SyslogServer", "port": 514, "protocol": "UDP", "serverId": -1, "tag": "uvm-to-192.168.56.196" }
SYSLOG_SERVER3 = {"enabled": False, "host": "192.168.56.199", "javaClass": "com.untangle.uvm.event.SyslogServer", "port": 514, "protocol": "UDP", "serverId": -1, "tag": "uvm-to-192.168.56.199" }
SYSLOG_SERVER4 = {"enabled": False, "host": "192.168.56.200", "javaClass": "com.untangle.uvm.event.SyslogServer", "port": 514, "protocol": "UDP", "serverId": -1, "tag": "uvm-to-192.168.56.200" }


@pytest.mark.syslog
class SysLogTests(NGFWTestCase):

    not_an_app= True

    @staticmethod
    def module_name():
        return "syslog"
    
    def test_050_disable_syslog(self):
        syslogSettings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = False
        uvmContext.eventManager().setSettings( syslogSettings )
        syslogUpdatedSettings = uvmContext.eventManager().getSettings()
        if "syslogServers" in syslogSettings.keys() and syslogSettings['syslogServers']:
            assert (len(syslogUpdatedSettings['syslogServers']['list']) == 0)
        else:
          #No Logservers configured
          pass
        uvmContext.eventManager().setSettings(orig_settings)

    def test_050_enable_syslog(self):
        #covering scenario of setup where default syslog is enabled and configured
        syslogSettings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        syslogSettings["syslogPort"] = 514
        syslogSettings["syslogProtocol"] = "UDP"
        syslogSettings["syslogHost"] = "192.168.56.195"
        uvmContext.eventManager().setSettings(syslogSettings)
        #this will add rule with server id as 1, the first server is assigned server id 1 always
        if (len(syslogSettings['syslogRules']['list']) == 1):
           if "syslogServers" in syslogSettings['syslogRules']['list'][0].keys() and syslogSettings['syslogRules']['list'][0]['syslogServers']:
              syslogSettings['syslogRules']['list'][0]['syslogServers']['list'].append(1)
        uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = uvmContext.eventManager().getSettings()
        assert(len(syslogSettings['syslogRules']['list'][0]['syslogServers']['list']) == 1)
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == 1)
        if (len(syslogUpdatedSettings['syslogRules']['list']) == 1):
           if "syslogServers" in syslogUpdatedSettings['syslogRules']['list'][0].keys() and syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']:
              syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'].clear()
              syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'].append(2)
        uvmContext.eventManager().setSettings(syslogUpdatedSettings)
        syslogUpdatedSettings = uvmContext.eventManager().getSettings()
        assert (len(syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list']) == 1)
        #server ID should be updated to 2 for the syslogRule
        assert (syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'][0] == 2)
        uvmContext.eventManager().setSettings(orig_settings)

    def test_050_enable_syslog_withouthostnameset(self):
        #covering scenario of setup where default syslog is enabled and sysloghost not set configured
        syslogSettings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        syslogSettings["syslogPort"] = 514
        syslogSettings["syslogProtocol"] = "UDP"
        #Removed sysLogHost, initially sysloghostname is not set in backend, in UI its mandatory
        syslogSettings.pop("syslogHost", None)
        uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = uvmContext.eventManager().getSettings()
        assert(len(syslogSettings['syslogRules']['list'][0]['syslogServers']['list']) == 0)
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == 0)
        uvmContext.eventManager().setSettings(orig_settings)


    def test_050_multiple_syslogservers(self):
        initial_logservers = 0
        syslogSettings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        if "syslogServers" in syslogSettings.keys():
           syslogSettings['syslogServers'] = {"javaClass": "java.util.LinkedList","list": [] }
        else:
           initial_logservers = len(syslogSettings['syslogServers']['list'])
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER1)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER2)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER3)
        uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = uvmContext.eventManager().getSettings()
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == initial_logservers + 3)
        uvmContext.eventManager().setSettings(orig_settings)


    def test_050_delete_syslogservers(self):
        initial_logservers = 0
        syslogSettings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        #Default setup list will not be present. UI payload will contain server list, need to generate for unittest
        #Testcase covers both enabled and disabled syslogserver scenario
        if "syslogServers" in syslogSettings.keys():
           syslogSettings['syslogServers'] = {"javaClass": "java.util.LinkedList","list": [] }
        else:
           initial_logservers = len(syslogSettings['syslogServers']['list'])
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER1)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER2)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER3)
        uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = uvmContext.eventManager().getSettings()
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == initial_logservers +3)
        key = 'host'
        # Fetch Added SyslogServers
        syslog_list = syslogUpdatedSettings['syslogServers']['list']
        # Deleting  server 1 and 3
        syslog_list = [item for item in syslog_list if item.get(key) != '192.168.56.195']
        syslog_list = [item for item in syslog_list if item.get(key) != '192.168.56.199']
        syslogUpdatedSettings['syslogServers'] = {"javaClass": "java.util.LinkedList","list": [] }
        # Adding new server4 and server2 to payload
        for server in syslog_list:
           syslogUpdatedSettings['syslogServers']['list'].append(server)
        syslogUpdatedSettings['syslogServers']['list'].append(SYSLOG_SERVER4)
        uvmContext.eventManager().setSettings(syslogUpdatedSettings)
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == initial_logservers + 2)
        uvmContext.eventManager().setSettings(orig_settings)



test_registry.register_module("syslog", SysLogTests)
