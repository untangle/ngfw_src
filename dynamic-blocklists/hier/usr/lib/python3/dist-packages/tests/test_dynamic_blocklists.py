import os
import shlex
import subprocess
import copy
import re
from datetime import datetime, timezone, timedelta
import time
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
        Checks 'count' and 'lastUpdated' fields against current UTC time.
        """
        global app, orig_dbl_settings

        dblSettings = copy.deepcopy(orig_dbl_settings)
        dblSettings['enabled'] = True
        dblSettings['configurations'][0]['enabled'] = True
        app.setSettingsV2(dblSettings)

        conf = app.getSettingsV2()['configurations'][0]
        assert conf['enabled']

        # Fetch live IP list
        url = "http://opendbl.net/lists/etknown.list"
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        content = response.text.strip().splitlines()

        ip_lines = [line for line in content if re.match(r"^\d{1,3}(?:\.\d{1,3}){3}$", line)]
        expected_ip_count = len(ip_lines)
        assert expected_ip_count > 0, "Expected some IP addresses in the source list."

        # Record current UTC time before running the job
        before_run_ts = int(time.time())

        app.runJobsByConfigIdsV2([conf['id']])

        updated_conf = app.getSettingsV2()['configurations'][0]
        assert updated_conf['enabled']
        assert updated_conf['count'] == expected_ip_count

        # Record UTC time again after running job
        after_run_ts = int(time.time())

        # Acceptable range: must fall between before_run_ts and after_run_ts
        assert before_run_ts <= updated_conf['lastUpdated'] <= after_run_ts, \
            f"lastUpdated timestamp {updated_conf['lastUpdated']} not within expected UTC range ({before_run_ts}-{after_run_ts})"

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


    def test_016_DBL_Blocking(self):
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

    def _get_result_type(self, result):
        """
        Extract the 'type' field from a runJobsByConfigIdsV2 result.
        Handles jabsorb JABSORB-mode Map serialization which wraps the map under
        a 'map' key (e.g. {"javaClass": "...", "map": {"type": "Error", ...}}).
        """
        if result is None:
            return None
        result_data = result.get("map", result)
        return result_data.get("type")

    def test_017_run_jobs_invalid_uuid_format_rejected(self):
        """
        Verify that runJobsByConfigIdsV2 rejects configIds that are not valid UUIDs.
        A non-UUID string must never be used to look up or execute a cron command.
        """
        result = app.runJobsByConfigIdsV2(["not-a-valid-uuid"])
        assert self._get_result_type(result) == "Error", \
            f"Expected Error response for non-UUID configId, got: {result}"

    def test_018_run_jobs_injection_chars_in_id_rejected(self):
        """
        Verify that configIds containing shell injection characters are rejected by
        UUID format validation before any cron lookup or command execution occurs.
        A sentinel file must not be created by the injected payload.
        """
        poc_file = "/tmp/dbl_injection_test.txt"
        subprocess.call(f"rm -f {poc_file}", shell=True)

        # Looks UUID-ish but contains injection payload after a semicolon
        malicious_id = "12345678-1234-1234-1234-123456789012; touch " + poc_file

        result = app.runJobsByConfigIdsV2([malicious_id])
        assert self._get_result_type(result) == "Error", \
            f"Expected Error response for injection configId, got: {result}"

        time.sleep(1)
        assert not os.path.exists(poc_file), \
            "Command injection succeeded via runJobsByConfigIdsV2 (sentinel file was created)"

    def test_019_run_jobs_unknown_uuid_returns_error(self):
        """
        Verify that a well-formed UUID that has no matching cron job entry
        returns an Error response without executing any command.
        """
        # This UUID is valid but will not match any entry in the dbl-crons file
        unknown_uuid = "00000000-0000-0000-0000-000000000000"
        result = app.runJobsByConfigIdsV2([unknown_uuid])
        assert self._get_result_type(result) == "Error", \
            f"Expected Error for unknown UUID, got: {result}"

    def test_020_run_jobs_mixed_valid_invalid_ids(self):
        """
        Verify that when a list contains both invalid and valid UUIDs,
        only the invalid ones are skipped and the overall response reflects
        whether any valid UUID succeeded.
        """
        # Mix: one known-bad format, one unknown-but-valid UUID
        result = app.runJobsByConfigIdsV2(["bad-id", "00000000-0000-0000-0000-000000000000"])
        # Both should fail (bad format + not in cron), so the response must be Error
        assert self._get_result_type(result) == "Error", \
            f"Expected Error when all configIds fail, got: {result}"


    def test_021_cron_job_source_url_is_shell_quoted(self):
        """
        Verify that when sync-settings writes /etc/cron.d/dbl-crons, user-supplied
        fields containing shell metacharacters (source URL, parsingMethod) are
        shell-quoted via shlex.quote() so they cannot break out and execute arbitrary
        commands through the generated cron job.
        """
        global app, orig_dbl_settings

        cron_file = "/etc/cron.d/dbl-crons"
        sentinel_file = "/tmp/dbl_cron_write_injection_test.txt"
        subprocess.call(["rm", "-f", sentinel_file])

        # Craft a source URL with a shell injection payload embedded after a semicolon.
        # Without proper quoting in the cron file, cron would execute:
        #   /usr/share/untangle/bin/fetch_ip_list.py ... http://example.com/list.txt
        #   ; touch /tmp/dbl_cron_write_injection_test.txt ...
        malicious_url = "http://example.com/list.txt; touch " + sentinel_file

        dblSettings = copy.deepcopy(orig_dbl_settings)
        dblSettings['enabled'] = True
        dblSettings['configurations'][0]['enabled'] = True
        dblSettings['configurations'][0]['source'] = malicious_url

        # setSettingsV2 triggers sync-settings, which calls write_cron_file()
        app.setSettingsV2(dblSettings)
        time.sleep(3)  # Allow sync-settings to finish writing the cron file

        assert os.path.exists(cron_file), f"Cron file was not created at {cron_file}"

        with open(cron_file) as f:
            cron_content = f.read()

        # The raw unquoted URL must NOT appear bare in the cron file.
        # Note: the URL text also appears inside the single-quoted form produced by
        # shlex.quote(), so we strip that quoted form out first and then check that
        # no bare copy of the URL remains.
        cron_without_quoted_url = cron_content.replace(shlex.quote(malicious_url), "QUOTED_URL")
        assert malicious_url not in cron_without_quoted_url, (
            "Shell injection payload found unquoted in cron file — "
            "source URL is not being properly shell-escaped."
        )

        # shlex.quote() wraps the URL in single quotes so the semicolon is inert.
        # Confirm the properly-quoted form IS present in the cron file.
        assert shlex.quote(malicious_url) in cron_content, (
            f"Expected shell-quoted URL '{shlex.quote(malicious_url)}' not found in cron file.\n"
            f"Cron content:\n{cron_content}"
        )

        # Writing the cron file must never execute the injected payload.
        assert not os.path.exists(sentinel_file), (
            "Sentinel file was created — shell injection occurred during cron file write."
        )

        app.setSettingsV2(orig_dbl_settings)


test_registry.register_module("dynamic-blocklists", DynamicBlocklistsTests)


