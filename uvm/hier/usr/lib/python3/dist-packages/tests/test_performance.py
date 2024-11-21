import copy
import datetime
import os
import os.path
import shutil
import subprocess
import time
from uvm import Uvm

from tests.common import NGFWTestCase
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import runtests.overrides as overrides
from . import global_functions

import unittest

wan_ip = None
orig_netsettings = None

# Can be adjusted downward for less reliable results
Performance_throughput_iterations = overrides.get("Performance_throughput_iterations", default=3)
# Can be adjusted downward for less reliable results
Performance_throughput_duration = overrides.get("Performance_throughput_duration", default=30)
# Test buffer sizes.
# NOTE: 8192 was the previously hardcoded value
Performance_throughput_tcp_buffer_sizes = overrides.get("Performance_throughput_tcp_buffer_sizes", default=[8192, 32768, 65536, 131072, 262144, 524288])
# Order applications by "least" to "most" affect on throughput
Performance_throughput_applications = overrides.get("Performance_throughput_applications", default=["router", "web-filter", "application-control"])

class PerformanceTests(NGFWTestCase):
    not_an_app = True
    run_order = 3

    @staticmethod
    def module_name():
        return "performance"

    @classmethod
    def initial_extra_setup(cls):
        global orig_netsettings, wan_ip
        if orig_netsettings == None:
            orig_netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        wan_ip = global_functions.uvmContext.networkManager().getFirstWanAddress()

    def test_100_tcp_throughput(self):
        """
        Run iperf through system against different TCP receive buffer sizes to determine effect on throughput
        """
        # Skip if apps are running
        for application_name in Performance_throughput_applications:
            if application_name != "router":
                if (global_functions.uvmContext.appManager().isInstantiated(application_name)):
                    raise unittest.SkipTest('app %s already instantiated' % application_name)

        # Skip if iperf not installed
        iperf_avail = global_functions.verify_iperf_configuration(wan_ip, ignore_running=True)
        if (not iperf_avail):
            raise unittest.SkipTest("iperf_server test client unreachable")

        # Start iperf server
        iperf_server_running = remote_control.run_command("pidof iperf", host=global_functions.iperf_server)
        if iperf_server_running == 1:
            remote_control.run_command(global_functions.build_iperf_command(mode="server", fork=True), global_functions.iperf_server)

        # Test expects that iperf server is DIRECTLY on the WAN network,
        # other in other wordstwo hops away from the client.  
        iperf_server_hop_count = global_functions.get_host_hops(global_functions.iperf_server)
        if iperf_server_hop_count > 2:
            raise unittest.SkipTest(f"iperf server is too far away at {iperf_server_hop_count} hops");

        results = {}

        ##
        ## Add bypass rule so traffic does not go throug uvm.
        ## This is effective "linespeed", reducing uvm's processing effect.
        ## From this value, we can calculate and compare uvm's effect on throughput.
        ##
        network_settings = copy.deepcopy(orig_netsettings)
        network_settings["bypassRules"]["list"] = [{
                "bypass": True,
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "conditionType": "SRC_ADDR",
                            "invert": False,
                            "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                            "value": remote_control.client_ip
                        },{
                            "conditionType": "DST_ADDR",
                            "invert": False,
                            "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                            "value": global_functions.iperf_server
                        }
                    ]
                },
                "description": "ats performance linespeed",
                "enabled": True,
                "javaClass": "com.untangle.uvm.network.BypassRule",
                "ruleId": -1
        }]
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)

        ## Run and get bypass throughput
        bypass_throughput = []
        for i in range(Performance_throughput_iterations):
            iperf_results = global_functions.get_iperf_results(duration=Performance_throughput_duration)
            bypass_throughput.append(iperf_results["throughput"])
        bypass_throughput_average = sum(bypass_throughput) / len(bypass_throughput)

        ## Remove bypass rule
        network_settings["bypassRules"]["list"] = []
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)

        ##
        ## Loop through various TCP receive sizes
        ##
        untangle_vm_conf_filename = "/usr/share/untangle/conf/untangle-vm.conf"
        untangle_vm_conf_tmp_filename = f"{untangle_vm_conf_filename}.tmp"
        untangle_vm_conf_original_filename = f"{untangle_vm_conf_filename}.orig"

        shutil.copyfile(untangle_vm_conf_filename, untangle_vm_conf_original_filename)

        original_tcp_buffer_size = None
        for tcp_buffer_size in Performance_throughput_tcp_buffer_sizes:
            results[tcp_buffer_size] = {}
            ##
            ## Update untangle-vm.conf with TCP buffer size
            ##
            vm_conf = []
            with open(untangle_vm_conf_filename, "r") as file:
                for line in file:
                    if line.startswith("tcp_buffer_size="):
                        line = f"tcp_buffer_size=\"{str(tcp_buffer_size)}\"\n"
                    vm_conf.append(line)
                file.close()

            with open(untangle_vm_conf_tmp_filename, "w") as file:
                for line in vm_conf:
                    file.write(line)
                file.close()

            os.replace(untangle_vm_conf_tmp_filename, untangle_vm_conf_filename)
            ## Restart uvm
            global_functions.uvmContext = global_functions.restart_uvm()

            ##
            ## Loop through applications
            ##
            ## These are the "name" fields from appProprties.json files.
            ##
            ## The "router" app is always enabled and never removed.
            ##
            default_policy_id = 1
            for application_name in Performance_throughput_applications:
                results[tcp_buffer_size][application_name] = {}

                ##
                ## Start application with default settings
                ##
                application = None
                if application_name != "router":
                    application = global_functions.uvmContext.appManager().instantiate(application_name, default_policy_id)

                ## Loop through app to get average throughput
                application_throughput = []
                for i in range(Performance_throughput_iterations):
                    iperf_results = global_functions.get_iperf_results(duration=Performance_throughput_duration)
                    application_throughput.append(iperf_results["throughput"])
                application_average = sum(application_throughput) / len(application_throughput)

                if application != None:
                    global_functions.uvmContext.appManager().destroy( application.getAppSettings()["id"] )
                    application = None

                results[tcp_buffer_size][application_name] = {
                    "throughput": application_average,
                }

        ##
        ## Report
        ## 
        print("=" * 40)
        print("Summary:")
        print(f"average bypass throughput: {global_functions.to_si_prefix(bypass_throughput_average)}")
        print("By size:")
        for size in results:
            for application_name in results[size]:
                current_throughput = results[size][application_name]["throughput"]
                print(f"{size:6} {application_name:20}: {global_functions.to_si_prefix(current_throughput)}, of bypass {(current_throughput / bypass_throughput_average) * 100:.2f}%")

        print("By size, relative to last size:")
        last_size = None
        for size in results:
            for application_name in results[size]:
                current_throughput = results[size][application_name]["throughput"]
                if last_size is not None:
                    last_throughput = results[last_size][application_name]["throughput"]
                    print(f"{size:6} vs {last_size:6}: {application_name:20}: {global_functions.to_si_prefix(current_throughput)} vs {global_functions.to_si_prefix(last_throughput)} >? {current_throughput > last_throughput}, diff-t={global_functions.to_si_prefix(abs(current_throughput-last_throughput))}, diff-p={(abs(current_throughput-last_throughput)/current_throughput) * 100:.2f}%")

            last_size = size
        print("=" * 40)

        ##
        ## Restore orginal untangle-vm.conf
        shutil.copyfile(untangle_vm_conf_original_filename, untangle_vm_conf_filename)
        global_functions.uvmContext = global_functions.restart_uvm()

        ##
        ## Stop iperf server
        ##
        iperf_running = 0
        timeout = 60
        while iperf_running == 0 and timeout > 0:
            timeout -= 1
            remote_control.run_command("pkill --signal 9 iperf", host=global_functions.iperf_server)
            time.sleep(1)
            iperf_running = remote_control.run_command("pidof iperf", host=global_functions.iperf_server)

    @classmethod
    def final_extra_tear_down(cls):

        # Restore original settings to return to initial settings
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

test_registry.register_module("performance", PerformanceTests)
