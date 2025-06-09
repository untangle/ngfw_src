import copy
import pytest
import subprocess

from tests.common import NGFWTestCase
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
    
    def checkSyslogStatus(self):
        # Check rsyslog service status and reset if failed
        status_output = subprocess.run(
            ["systemctl", "is-failed", "rsyslog"],
            capture_output=True,
            text=True
        )

        if status_output.returncode == 0 and status_output.stdout.strip() == "failed":
            subprocess.run(["systemctl", "reset-failed", "rsyslog"])

    def test_050_disable_syslog(self):
        syslogSettings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = False
        global_functions.uvmContext.eventManager().setSettings( syslogSettings )
        syslogUpdatedSettings = global_functions.uvmContext.eventManager().getSettings()
        if "syslogServers" in syslogSettings.keys() and syslogSettings['syslogServers']:
            assert (len(syslogUpdatedSettings['syslogServers']['list']) == 0)
        else:
          #No Logservers configured
          pass
        global_functions.uvmContext.eventManager().setSettings(orig_settings)

    def test_051_enable_syslog(self):
        self.checkSyslogStatus()
        #covering scenario of setup where default syslog is enabled and configured
        syslogSettings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        syslogSettings["syslogPort"] = 514
        syslogSettings["syslogProtocol"] = "UDP"
        syslogSettings["syslogHost"] = "192.168.56.195"
        if "syslogServers" in syslogSettings:
           syslogSettings.pop("syslogServers")
        for rule in syslogSettings['syslogRules']['list']:
            if "syslogServers" in  rule.keys():
               rule.pop("syslogServers")
        global_functions.uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = global_functions.uvmContext.eventManager().getSettings()
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == 1)
        #check for description field populated in case of setup where syslog server is enabled
        assert("Default Syslog Server" == syslogUpdatedSettings['syslogServers']['list'][0]['description'])
        assert(syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'][0] == 1)
        if "syslogServers" in syslogUpdatedSettings['syslogRules']['list'][0].keys() and syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']:
           syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'].clear()
           syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'].append(2)
        global_functions.uvmContext.eventManager().setSettings(syslogUpdatedSettings)
        syslogUpdatedSettings = global_functions.uvmContext.eventManager().getSettings()
        assert (len(syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list']) == 1)
        #server ID should be updated to 2 for the syslogRule
        print(syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'])
        assert (syslogUpdatedSettings['syslogRules']['list'][0]['syslogServers']['list'][0] == 2)
        global_functions.uvmContext.eventManager().setSettings(orig_settings)

    def test_052_enable_syslog_withouthostnameset(self):
        self.checkSyslogStatus()
        #covering scenario of setup where default syslog is enabled and sysloghost not set configured
        syslogSettings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        syslogSettings["syslogPort"] = 514
        syslogSettings["syslogProtocol"] = "UDP"
        #Removed sysLogHost, initially sysloghostname is not set in backend, in UI its mandatory
        syslogSettings.pop("syslogHost", None)
        global_functions.uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = global_functions.uvmContext.eventManager().getSettings()
        assert(len(syslogSettings['syslogRules']['list'][0]['syslogServers']['list']) == 0)
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == 0)
        global_functions.uvmContext.eventManager().setSettings(orig_settings)


    def test_053_multiple_syslogservers(self):
        self.checkSyslogStatus()
        initial_logservers = 0
        syslogSettings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        if "syslogServers" not in syslogSettings.keys():
           syslogSettings['syslogServers'] = {"javaClass": "java.util.LinkedList","list": [] }
        else:
           initial_logservers = len(syslogSettings['syslogServers']['list'])
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER1)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER2)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER3)
        global_functions.uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = global_functions.uvmContext.eventManager().getSettings()
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == initial_logservers + 3)
        global_functions.uvmContext.eventManager().setSettings(orig_settings)


    def test_054_delete_syslogservers(self):
        self.checkSyslogStatus()
        initial_logservers = 0
        syslogSettings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(syslogSettings)
        syslogSettings["syslogEnabled"] = True
        #Default setup list will not be present. UI payload will contain server list, need to generate for unittest
        #Testcase covers both enabled and disabled syslogserver scenario
        if "syslogServers" not in syslogSettings.keys():
           syslogSettings['syslogServers'] = {"javaClass": "java.util.LinkedList","list": [] }
        else:
           initial_logservers = len(syslogSettings['syslogServers']['list'])
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER1)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER2)
        syslogSettings['syslogServers']['list'].append(SYSLOG_SERVER3)
        global_functions.uvmContext.eventManager().setSettings(syslogSettings)
        syslogUpdatedSettings = global_functions.uvmContext.eventManager().getSettings()
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
        global_functions.uvmContext.eventManager().setSettings(syslogUpdatedSettings)
        assert (len(syslogUpdatedSettings['syslogServers']['list']) == initial_logservers + 2)
        global_functions.uvmContext.eventManager().setSettings(orig_settings)



test_registry.register_module("syslog", SysLogTests)
