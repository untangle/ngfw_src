"""tunnel_vpn tests"""
import copy
import os
import time
import subprocess

import unittest
import pytest

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import runtests.overrides as overrides

default_policy_id = 1
app = None
TUNNEL_ID = 200
VPN_TUNNEL_URI = overrides.get( "VPN_TUNNEL_URI", default="http://10.112.56.29/openvpn-ats-test-tunnelvpn-config.zip")
TUNNEL_VPN_ISOLATED_CLIENT = overrides.get( "TUNNEL_VPN_ISOLATED_CLIENT", default="192.168.100.56")
CLIENT_TAGGED_CONDITION = {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "conditionType": "CLIENT_TAGGED",
                    "invert": False,
                    "javaClass": "com.untangle.app.tunnel_vpn.TunnelVpnRuleCondition",
                    "value": "test-tag"
                }
            ]
        }

DEFAULT_RULE_DESCRIPTION = "Route all traffic over any available Tunnel."
DEFAULT_CONDITION = {
            "javaClass": "java.util.LinkedList",
            "list": []
        }

def create_tunnel_rule(vpn_enabled=True,vpn_ipv6=True,rule_id=50,vpn_tunnel_id=TUNNEL_ID, description=DEFAULT_RULE_DESCRIPTION, conditions=DEFAULT_CONDITION):
    return {
            "conditions": conditions,
            "description": description,
            "enabled": vpn_enabled,
            "ipv6Enabled": vpn_ipv6,
            "javaClass": "com.untangle.app.tunnel_vpn.TunnelVpnRule",
            "ruleId": rule_id,
            "tunnelId": vpn_tunnel_id
    }

def create_tunnel_profile(username=None,vpn_enabled=True,provider="tunnel-Arista",password=None,name="tunnel-Arista",vpn_tunnel_id=TUNNEL_ID):
    if (password):
        return {
            "allTraffic": False,
            "enabled": vpn_enabled,
            "javaClass": "com.untangle.app.tunnel_vpn.TunnelVpnTunnelSettings",
            "name": name,
            "password": password, 
            "username": username,
            "provider": "Arista",
            "tags": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "tunnelId": vpn_tunnel_id
        }
    else:
        return {
            "allTraffic": False,
            "enabled": vpn_enabled,
            "javaClass": "com.untangle.app.tunnel_vpn.TunnelVpnTunnelSettings",
            "name": name,
            "provider": "Arista",
            "tags": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "tunnelId": vpn_tunnel_id
        }

def create_trigger_rule(action, tag_target, tag_name, tag_lifetime_sec, description, field, operator, value, field2, operator2, value2):
    return {
        "description": description,
        "action": action,
        "tagTarget": tag_target,
        "tagName": tag_name,
        "tagLifetimeSec": tag_lifetime_sec,
        "enabled": True,
        "javaClass": "com.untangle.uvm.event.TriggerRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "javaClass": "com.untangle.uvm.event.EventRuleCondition",
                "comparator": operator,
                "field": field,
                "fieldValue": value
            }, {
                "javaClass": "com.untangle.uvm.event.EventRuleCondition",
                "comparator": operator2,
                "field": field2,
                "fieldValue": value2
            }]
        },
        "ruleId": 1
    }


@pytest.mark.tunnel_vpn
class TunnelVpnTests(NGFWTestCase):

    force_start = True
    
    @staticmethod
    def module_name():
        return "tunnel-vpn"

    @staticmethod
    def vendorName():
        return "Untangle"

    # verify client is online
    def test_010_client_is_online(self):
        """
        Verify LAN client online
        """
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        """
        Verify valid license
        """
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_create_vpn_tunnel(self):
        """
        Verify tunnel connection and follows rules
        """
        currentWanIP = global_functions.get_public_ip_address()
        if (currentWanIP == ""):
            raise unittest.SkipTest("Unable to get WAN IP")
        print("Original WAN IP: " + str(currentWanIP))

        result = subprocess.call(global_functions.build_wget_command(log_file="/dev/null", output_file="/tmp/config.zip",uri=VPN_TUNNEL_URI), shell=True)
        if (result != 0):
            raise unittest.SkipTest("Unable to download VPN file: " + VPN_TUNNEL_URI)
        self._app.importTunnelConfig("/tmp/config.zip", "Untangle", TUNNEL_ID)

        appData = self._app.getSettings()
        appData['rules']['list'].append(create_tunnel_rule())
        appData['tunnels']['list'].append(create_tunnel_profile())
        self._app.setSettings(appData)

        # wait for vpn tunnel to form
        timeout = 60
        connected = False
        while (not connected and timeout > 0):
            newWanIP = global_functions.get_public_ip_address()
            if (currentWanIP != newWanIP and newWanIP != ""):
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                connected = True
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
            else:
                time.sleep(1)
                timeout-=1

        assert True is global_functions.is_vpn_running(interface=f"tun{TUNNEL_ID}",route_table=f"tunnel.{TUNNEL_ID}"), "vpn running"
        assert True is global_functions.is_vpn_routing(route_table=f"tunnel.{TUNNEL_ID}"), "vpn routing"

        # remove the rule to see if traffic is sent through WAN by default
        appData['rules']['list'][:] = []
        self._app.setSettings(appData)
        time.sleep(5)
        newWanIP2 = global_functions.get_public_ip_address()

        assert True is global_functions.is_vpn_running(interface=f"tun{TUNNEL_ID}",route_table=f"tunnel.{TUNNEL_ID}"), "vpn running"
        assert True is global_functions.is_vpn_routing(route_table=f"tunnel.{TUNNEL_ID}"), "vpn routing"

        # remove the added tunnel
        appData['tunnels']['list'][:] = []
        self._app.setSettings(appData)

        assert global_functions.is_vpn_running(interface=f"tun{TUNNEL_ID}",route_table=f"tunnel.{TUNNEL_ID}") is False, "vpn running"
        assert global_functions.is_vpn_routing(route_table=f"tunnel.{TUNNEL_ID}") is False, "vpn routing"

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)
        assert(connectStatus == "CONNECTED")
        # If the VPN tunnel is routed without rules, fail the test
        assert(currentWanIP == newWanIP2)

    def test_030_create_vpn_any_tunnel(self):
        """
        Test any tunnel
        """
        currentWanIP = global_functions.get_public_ip_address()
        if (currentWanIP == ""):
            raise unittest.SkipTest("Unable to get WAN IP")
        print("Original WAN IP: " + str(currentWanIP))
        result = subprocess.call(global_functions.build_wget_command(log_file="/dev/null", output_file="/tmp/config.zip",uri=VPN_TUNNEL_URI), shell=True)
        if (result != 0):
            raise unittest.SkipTest("Unable to download VPN file: " + VPN_TUNNEL_URI)
        self._app.importTunnelConfig("/tmp/config.zip", "Untangle", TUNNEL_ID)

        appData = self._app.getSettings()
        appData['rules']['list'].append(create_tunnel_rule(vpn_tunnel_id=-1))
        appData['tunnels']['list'].append(create_tunnel_profile())
        self._app.setSettings(appData)

        # wait for vpn tunnel to form
        timeout = 60
        connected = False
        pingPcLanResult = ""
        while (not connected and timeout > 0):
            newWanIP = global_functions.get_public_ip_address()
            if (currentWanIP != newWanIP):
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                connected = True
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                pingPcLanResult = remote_control.run_command("ping -c 1 %s" % TUNNEL_VPN_ISOLATED_CLIENT)
            else:
                time.sleep(1)
                timeout-=1

        assert True is global_functions.is_vpn_running(interface=f"tun{TUNNEL_ID}",route_table=f"tunnel.{TUNNEL_ID}"), "vpn running"
        assert True is global_functions.is_vpn_routing(route_table=f"tunnel.{TUNNEL_ID}"), "vpn routing"

        # remove the added tunnel
        appData['rules']['list'][:] = []
        appData['tunnels']['list'][:] = []
        self._app.setSettings(appData)

        assert False is global_functions.is_vpn_running(interface=f"tun{TUNNEL_ID}",route_table=f"tunnel.{TUNNEL_ID}"), "vpn running"
        assert False is global_functions.is_vpn_routing(route_table=f"tunnel.{TUNNEL_ID}"), "vpn routing"

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)
        assert(connectStatus == "CONNECTED")
        assert(pingPcLanResult == 0)


    def test_040_verify_tunnel_connection_with_client_tagged_rule(self):
        """
        Verify tunnel connection for client tagged condition
        """
        currentWanIP = global_functions.get_public_ip_address()
        if (currentWanIP == ""):
            raise unittest.SkipTest("Unable to get WAN IP")
        print("Original WAN IP: " + str(currentWanIP))

        # Create trigger rule for SessionEvent to Tag host

        settings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_trigger_rule("TAG_HOST", "localAddr", "test-tag", 30, "test tag rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        global_functions.uvmContext.eventManager().setSettings( settings )

        time.sleep(4)

        # Create a tunnel: Tunnel1 and tunnel VPN rule to route all hosts tagged with 'test-tag' over Tunnel1.
        result = subprocess.call(global_functions.build_wget_command(log_file="/dev/null", output_file="/tmp/config.zip",uri=VPN_TUNNEL_URI), shell=True)
        if (result != 0):
            raise unittest.SkipTest("Unable to download VPN file: " + VPN_TUNNEL_URI)
        self._app.importTunnelConfig("/tmp/config.zip", "Untangle", TUNNEL_ID)

        appData = self._app.getSettings()
        appData['rules']['list'].append(create_tunnel_rule(conditions=CLIENT_TAGGED_CONDITION, description="Route all hosts tagged with \"test-tag\" over Tunnel1."))
        appData['tunnels']['list'].append(create_tunnel_profile(name="Tunnel1"))
        self._app.setSettings(appData)

        # wait for vpn tunnel to form
        timeout = 60
        connected = False
        while (not connected and timeout > 0):
            newWanIP = global_functions.get_public_ip_address()
            if (currentWanIP != newWanIP and newWanIP != ""):
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                connected = True
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
            else:
                time.sleep(1)
                timeout-=1

        assert True is global_functions.is_vpn_running(interface=f"tun{TUNNEL_ID}",route_table=f"tunnel.{TUNNEL_ID}"), "vpn running"
        assert True is global_functions.is_vpn_routing(route_table=f"tunnel.{TUNNEL_ID}"), "vpn routing"

        # Set original event settings
        global_functions.uvmContext.eventManager().setSettings( orig_settings )

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)
        assert(connectStatus == "CONNECTED")
    
    def test_50_password_encryption_setting_process_for_Tunnel_VPN(self):
        """
        Verify tunnel vpn password encryption setting process
        """
        org_appData = self._app.getSettings()
        appData = copy.deepcopy(org_appData)
        complex_password = "R7u!9f@Q2m#X1$zP8_s!\n"
        appData['tunnels']['list'].append(create_tunnel_profile(name="Tunnel1",password="testing",vpn_tunnel_id="202",username="test"))
        appData['tunnels']['list'].append(create_tunnel_profile(name="Tunnel2",vpn_tunnel_id="201"))
        appData['tunnels']['list'].append(create_tunnel_profile(name="Tunnel1",password=complex_password,vpn_tunnel_id="203",username="test"))
        self._app.setSettings(appData)

        appData = self._app.getSettings()
        for tunnel in appData['tunnels']['list']:
            if tunnel['tunnelId'] == 202:
                tunnel_with_password = tunnel
                tunnelId202 = tunnel['tunnelId']
            elif tunnel['tunnelId'] == 201:
                tunnel_with_no_password = tunnel
                tunnelId201 = tunnel['tunnelId']
            elif tunnel['tunnelId'] == 203:
                tunnel_with_complex_password = tunnel
                tunnelId203 = tunnel['tunnelId']

        assert tunnel_with_password.get('encryptedTunnelVpnPassword') is not None, "encryptedTunnelVpnPassword is missing"
        assert tunnel_with_password.get('password') is None, "Password is not None"

        assert tunnel_with_no_password.get('encryptedTunnelVpnPassword') is None, "encryptedTunnelVpnPassword is not none"
        assert tunnel_with_no_password.get('password') is None, "Password is not None"

        encrypted_password = tunnel_with_complex_password.get('encryptedTunnelVpnPassword')
        expected_decrypted = complex_password.rstrip("\n")
        decrypted_password = global_functions.uvmContext.systemManager().getDecryptedPassword(encrypted_password)
        assert decrypted_password == expected_decrypted, \
            "Decrypted password does not match original! (%s != %s)" % (decrypted_password, expected_decrypted)
        
        filename3 = f"@PREFIX@/usr/share/untangle/settings/tunnel-vpn/tunnel-{tunnelId203}/auth.txt"
        filename1 = f"@PREFIX@/usr/share/untangle/settings/tunnel-vpn/tunnel-{tunnelId202}/auth.txt"
        filename2 = f"@PREFIX@/usr/share/untangle/settings/tunnel-vpn/tunnel-{tunnelId201}/auth.txt"
        second_line = None
        with open(filename3, 'r') as file:        
            file.seek(0)  # Reset the file pointer to the beginning of the file
            lines = file.readlines()  # Now read the lines again
            if len(lines) > 1:
                second_line = lines[1].strip()  # Removing trailing newline
            else:
                print("The file doesn't contain a second line.")
        assert second_line == expected_decrypted, f"Expected '{expected_decrypted}' but got '{second_line}'"

        with open(filename1, 'r') as file:        
            file.seek(0)  # Reset the file pointer to the beginning of the file
            lines = file.readlines()  # Now read the lines again
            if len(lines) > 1:
                second_line = lines[1].strip()  # Removing trailing newline
            else:
                print("The file doesn't contain a second line.")
        assert second_line == "testing", f"Expected 'testing' but got '{second_line}'"

        with open(filename2, 'r') as file:        
            file.seek(0)  # Reset the file pointer to the beginning of the file
            lines = file.readlines()  # Now read the lines again
            if len(lines) > 1:
                second_line = lines[1].strip()  # Removing trailing newline
            else:
                print("The file doesn't contain a second line.")
        assert second_line == "password", f"Expected 'testing' but got '{second_line}'"
        
        # set to original settings
        self._app.setSettings(org_appData)

    def test_060_command_injection_prevented(self):
        """
        Verify that backtick command injection in provider argument is prevented by execCommand
        """
        exploit_marker = "/tmp/tunnel-vpn-exploit-test"
        dummy_config = "/tmp/tunnel-vpn-dummy.ovpn"

        # Clean up marker file if it exists from a previous run
        if os.path.exists(exploit_marker):
            os.remove(exploit_marker)

        # Create a dummy .ovpn file — content doesn't matter,
        # the script just needs a file that exists with the right extension
        with open(dummy_config, "w") as f:
            f.write("client\n")

        # Call importTunnelConfig with a provider containing backtick injection
        # With old execSafe, this would execute: touch /tmp/tunnel-vpn-exploit-test
        # With new execCommand, backticks are passed as literal string data
        malicious_provider = "test`touch " + exploit_marker + "`"
        try:
            self._app.importTunnelConfig(dummy_config, malicious_provider, TUNNEL_ID)
        except Exception:
            # The import will fail due to unknown provider — that's expected,
            # we only care that the injected command did NOT execute
            pass

        assert not os.path.exists(exploit_marker), \
            "Command injection via backticks was executed! " + exploit_marker + " should not exist"

    def test_061_upload_tunnel_vpn_argument_injection_blocked(self):
        """Verify /admin/upload with type=tunnel_vpn ignores malicious argument field (command injection fix)"""
        import zipfile
        import io
        import urllib.request

        exploit_marker = "/tmp/tunnel-vpn-upload-exploit-test"
        if os.path.exists(exploit_marker):
            os.remove(exploit_marker)

        # Build a minimal zip containing a dummy .ovpn file.
        # The handler will reject it due to an unknown provider — that's expected.
        # We only care that the malicious argument is not shell-executed.
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w') as zf:
            zf.writestr("dummy.ovpn", "client\n")
        config_bytes = buf.getvalue()

        malicious_provider = f"test`touch {exploit_marker}`"
        opener = global_functions.build_admin_http_opener()
        boundary, body = global_functions.build_upload_multipart_body(
            "tunnel_vpn", malicious_provider, config_bytes, filename="dummy.zip"
        )

        req = urllib.request.Request(
            "http://localhost/admin/upload",
            data=body,
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
        )
        try:
            opener.open(req)
        except Exception:
            pass

        time.sleep(1)
        assert not os.path.exists(exploit_marker), \
            "Command injection via tunnel_vpn upload argument field executed! " + exploit_marker + " should not exist"

    def test_070_route_up_script_eval_injection_blocked(self):
        """
        Verify tunnel-vpn-route-up.sh rejects shell metacharacters in
        OpenVPN-pushed route values instead of executing them via `eval`
        (NGFW-15705 / Vuln 1).

        Pre-fix: the script's `eval $command` executes the injected `touch`
        and the marker file appears -> test FAILS, vulnerability confirmed.

        Post-fix: the input fails IPv4 validation, the route is rejected
        with a logger warning, no shell expansion happens -> test PASSES.
        """
        script = "/usr/share/untangle/bin/tunnel-vpn-route-up.sh"
        exploit_marker = "/tmp/tunnel-vpn-route-up-injection-test"

        if os.path.exists(exploit_marker):
            os.remove(exploit_marker)

        # Mimic openvpn --route-up env. `dev=tun99` keeps the script past its
        # interface_id check; the malicious route_network_1 carries the payload.
        env = os.environ.copy()
        env["dev"] = "tun99"
        env["ifconfig_local"] = "10.99.0.2"
        env["ifconfig_remote"] = "10.99.0.1"
        env["route_vpn_gateway"] = "10.99.0.1"
        env["route_network_1"] = "10.20.0.0; touch " + exploit_marker
        env["route_netmask_1"] = "255.255.0.0"
        env["route_gateway_1"] = "10.20.0.1"

        subprocess.call([script], env=env)

        assert not os.path.exists(exploit_marker), \
            "Shell injection via OpenVPN-pushed route value executed! " \
            + exploit_marker + " should not exist"

    def test_080_pid_file_injection_blocked(self):
        """
        Verify TunnelVpnManager.recycleTunnel() rejects shell metacharacters
        in the openvpn PID file instead of executing them via shell-string
        `execOutput("kill -INT " + pid)` (NGFW-15705 / Vuln 3).

        Pre-fix: PID contents `"99999 ; touch /tmp/marker"` get concatenated
        into `kill -INT 99999 ; touch /tmp/marker`, which ut-exec-launcher
        runs via `subprocess.Popen(shell=True)` -> marker exists -> test
        FAILS, vulnerability confirmed.

        Post-fix: PID is validated against `\\d{1,10}` and dispatched as
        structured argv via execCommand("kill", List.of(...)) -> no shell
        expansion -> marker absent -> test PASSES.
        """
        fake_tunnel_id = 250
        pid_dir = "/run/tunnelvpn"
        pid_file = os.path.join(pid_dir, f"tunnel-{fake_tunnel_id}.pid")
        exploit_marker = "/tmp/tunnel-vpn-pid-injection-test"

        if os.path.exists(exploit_marker):
            os.remove(exploit_marker)

        org_appData = self._app.getSettings()
        appData = copy.deepcopy(org_appData)
        appData['tunnels']['list'].append(create_tunnel_profile(
            name="injection-test",
            vpn_tunnel_id=fake_tunnel_id))
        self._app.setSettings(appData)

        try:
            # Seed PID file AFTER setSettings — saving may trigger
            # restartProcesses which sweeps /run/tunnelvpn/ first.
            os.makedirs(pid_dir, exist_ok=True)
            with open(pid_file, "w") as f:
                # PID 99999 won't exist -> kill fails -> `;` runs touch
                f.write("99999 ; touch " + exploit_marker)

            # Trigger the kill path inside TunnelVpnManager.recycleTunnel.
            # TunnelVpnApp.recycleTunnel runs tunnelVpnManager.recycleTunnel
            # FIRST (the path under test), then tunnelVpnMonitor.recycleTunnel
            # which NPEs because our fake tunnel has no live status entry.
            # The NPE is expected and irrelevant -- the kill code already
            # executed (or didn't, post-fix) before the NPE was thrown.
            try:
                self._app.recycleTunnel(fake_tunnel_id)
            except Exception:
                pass
            time.sleep(2)

            assert not os.path.exists(exploit_marker), \
                "PID file shell injection executed via execOutput! " \
                + exploit_marker + " should not exist"
        finally:
            if os.path.exists(pid_file):
                os.remove(pid_file)
            if os.path.exists(exploit_marker):
                os.remove(exploit_marker)
            self._app.setSettings(org_appData)

test_registry.register_module("tunnel-vpn", TunnelVpnTests)
