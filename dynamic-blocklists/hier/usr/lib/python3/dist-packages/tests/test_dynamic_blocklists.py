import subprocess
import copy
import re
from datetime import datetime, timezone, timedelta

import pytest
import requests

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions


@pytest.mark.dynamic_blocklists
class DynamicBlocklistsTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        return "dynamic-blocklists"

    @staticmethod
    def appWebName():
        return "dynamic-blocklists"

    @staticmethod
    def vendorName():
        return "Untangle"

    @classmethod
    def initial_extra_setup(cls):
        """Initialize DBL app settings and store original configuration."""
        global orig_dbl_settings, app
        app = cls._app
        orig_dbl_settings = app.getSettingsV2()

    def check_ipset_exists(self, ipset_name):
        """Return True if the given ipset exists, False otherwise."""
        result = subprocess.run(
            f"ipset list {ipset_name}",
            shell=True,
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        return result.returncode == 0

    def check_iptables_rule_exists(self, chain_name):
        """Return True if the given iptables chain exists in the filter table."""
        result = subprocess.run(
            f"iptables -t filter -S | grep '{chain_name}'",
            shell=True,
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        return bool(result.stdout.strip().splitlines())

    def test_010_clientIsOnline(self):
        """Verify that the client is online."""
        assert remote_control.is_online() == 0

    def test_011_license_valid(self):
        """Verify that the module has a valid license."""
        assert global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name())

    def test_012_dbl_setup_and_cleanUp(self):
        """
        Verify that DBL setup script runs when the app is enabled and script removes ipset and iptables rules when app is disabled.
        Checks creation and deletion of parent ipset and iptables rules respectively.
        """
        global app, orig_dbl_settings

        dblSettings = copy.deepcopy(orig_dbl_settings)
        #Verify script and rule added if app disabled
        dblSettings['enabled'] = True
        app.setSettingsV2(dblSettings)

        self.assertTrue(self.check_ipset_exists("dblsets"), "The ipset 'dblsets' does not exist.")
        self.assertTrue(self.check_iptables_rule_exists("dynamic-block-list"),
                        "The iptables rule for dynamic-block-list does not exist.")
        
        #Verify script and rule deleted if app disabled
        dblSettings['enabled'] = False
        app.setSettingsV2(dblSettings)

        self.assertFalse(self.check_ipset_exists("dblsets"), "The ipset 'dblsets' exists unexpectedly.")
        self.assertFalse(self.check_iptables_rule_exists("dynamic-block-list"),
                         "The 'dbl' iptables chain exists unexpectedly.")

        app.setSettingsV2(orig_dbl_settings)

    def test_013_reset_to_default(self):
        """
        Verify that DBL settings reset to initial values when resetToDefault API is called.
        Only 'id' fields are allowed to differ.
        """
        global app, orig_dbl_settings

        dblSettings = copy.deepcopy(orig_dbl_settings)
        dblSettings['enabled'] = True
        for conf in dblSettings['configurations']:
            conf['enabled'] = True
        app.setSettingsV2(dblSettings)

        updated_settings = app.getSettingsV2()
        assert updated_settings['enabled'] != orig_dbl_settings['enabled'], \
            f"'enabled' field did not change after update."

        app.onResetDefaultsV2()
        reset_settings = app.getSettingsV2()

        def strip_ids(settings):
            stripped = copy.deepcopy(settings)
            for conf in stripped.get('configurations', []):
                conf.pop('id', None)
            return stripped

        assert strip_ids(orig_dbl_settings) == strip_ids(reset_settings), \
            "Settings after reset do not match defaults (excluding 'id' fields)."
        
        app.setSettingsV2(orig_dbl_settings)


    def test_014_on_demand_job_run_for_specific_confId(self):
        """
        Verify DBL job runs and fetches latest IPs for a specific config ID.
        Checks 'count' and 'lastUpdated' fields against source data.
        """
        global app, orig_dbl_settings

        dblSettings = copy.deepcopy(orig_dbl_settings)
        dblSettings['enabled'] = True
        dblSettings['configurations'][0]['enabled'] = True
        app.setSettingsV2(dblSettings)

        conf = app.getSettingsV2()['configurations'][0]
        assert conf['enabled'] and conf['count'] == 0 and conf['lastUpdated'] == 0

        # Fetch live IP list and timestamp
        url = "http://opendbl.net/lists/etknown.list"
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        content = response.text.strip().splitlines()

        # Parse timestamp from first line
        ts_match = re.search(r"#\s*Last\s+updated\s+(\d{4}-\d{2}-\d{2} \d{2}:\d{2})", content[0])
        expected_last_updated = None
        if ts_match:
            dt_local = datetime.strptime(ts_match.group(1), "%Y-%m-%d %H:%M")
            dt_utc = dt_local - timedelta(hours=5, minutes=30)  # adjust to UTC
            expected_last_updated = int(dt_utc.replace(tzinfo=timezone.utc).timestamp())

        ip_lines = [line for line in content if re.match(r"^\d{1,3}(?:\.\d{1,3}){3}$", line)]
        expected_ip_count = len(ip_lines)
        assert expected_ip_count > 0, "Expected some IP addresses in the source list."

        app.runJobsByConfigIdsV2([conf['id']])

        updated_conf = app.getSettingsV2()['configurations'][0]
        assert updated_conf['enabled']
        assert updated_conf['count'] == expected_ip_count

        if expected_last_updated:
            assert abs(updated_conf['lastUpdated'] - expected_last_updated) < 300, \
                "lastUpdated timestamp differs from source by more than 5 minutes."

        app.setSettingsV2(orig_dbl_settings)
        print(f"Verified on-demand DBL job for conf_id={conf['id']}: "
              f"{expected_ip_count} IPs fetched, lastUpdated={updated_conf['lastUpdated']}")

    def test_015_export_ips(self):
        """
        Verify that exported CSV contains all IPs fetched from the source list.
        """
        global app, orig_dbl_settings

        dblSettings = copy.deepcopy(orig_dbl_settings)
        dblSettings['enabled'] = True
        dblSettings['configurations'][0]['enabled'] = True
        app.setSettingsV2(dblSettings)

        conf = app.getSettingsV2()['configurations'][0]
        app.runJobsByConfigIdsV2([conf['id']])

        exported_ips = [line.strip() for line in app.exportCsvV2(conf['id']).splitlines() if line.strip()]

        url = "http://opendbl.net/lists/etknown.list"
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        expected_ips = re.findall(r"\b\d{1,3}(?:\.\d{1,3}){3}\b", response.text)

        missing_ips = [ip for ip in expected_ips if ip not in exported_ips]
        assert not missing_ips, f"Missing IPs in export: {missing_ips[:10]}"
        print(f"Export verified: {len(exported_ips)} IPs matched from source.")

        app.setSettingsV2(orig_dbl_settings)


    def test_015_DBL_Blocking(self):
        """
        Verify that Ip listed in source should be blocked on client end if DBL and source is enabled.
        """
        global app, orig_dbl_settings
        app.setSettingsV2(orig_dbl_settings)
        # Enable DBL and first configuration
        dblSettings = copy.deepcopy(orig_dbl_settings)
        dblSettings['enabled'] = True
        dblSettings['configurations'][0]['enabled'] = True
        app.setSettingsV2(dblSettings)

        # Run DBL job
        conf = app.getSettingsV2()['configurations'][0]
        app.runJobsByConfigIdsV2([conf['id']])

        # Fetch the IP list from the external source
        url = "http://opendbl.net/lists/etknown.list"
        response = requests.get(url)
        response.raise_for_status()

        ips = [line.strip() for line in response.text.splitlines() if line.strip() and not line.startswith('#')]

        # Pick one pingable IP (not blocked yet)
        pingable_ip = None
        for ip in ips:
            result = subprocess.run(["ping", "-c", "1", ip],
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if result.returncode == 0:
                pingable_ip = ip
                break
        assert pingable_ip is not None, "No pingable IPs found in source list — check list or connectivity."

        # Now test if DBL is blocking it
        pingResult = global_functions.get_wait_for_command_output(command=f"ping -c 1 {pingable_ip}",tries=3)
        # If ping succeeds, fail — means not blocked
        assert pingResult != 0, f"DBL did not block IP {pingable_ip}"
        app.setSettingsV2(orig_dbl_settings)

test_registry.register_module("dynamic-blocklists", DynamicBlocklistsTests)


