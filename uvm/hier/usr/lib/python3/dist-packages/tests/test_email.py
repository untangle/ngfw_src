import pytest
import copy
import datetime
import shutil
import subprocess
import time

from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests
import unittest
import runtests.test_registry as test_registry

from uvm import Uvm

@pytest.mark.email_tests
class EmailTests(NGFWTestCase):

    not_an_app= True
    original_mail_settings = None

    @staticmethod
    def module_name():
        return "email-tests"

    @classmethod
    def initial_extra_setup(cls):
        if EmailTests.original_mail_settings is None:
            EmailTests.original_mail_settings = global_functions.uvmContext.mailSender().getSettings()

    @pytest.mark.slow
    def test_010_mail_send_method_modes(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        origMailsettings = global_functions.uvmContext.mailSender().getSettings()
        #Default sendMethod Mode is DIRECT
        assert (origMailsettings['sendMethod'] == 'DIRECT')

        #Upgrade scenario simulating RELAY method 
        newMailsettings = copy.deepcopy(origMailsettings)
        newMailsettings['sendMethod'] = 'RELAY'

        #Updating existing sendMethod mode to RELAY
        global_functions.uvmContext.mailSender().setSettings(newMailsettings)
        time.sleep(10) # give it time for exim to restart

        #Upgrade method scenario is simulated, sendMethod mode set to RELAY
        updatedMailSettings = global_functions.uvmContext.mailSender().getSettings()
        assert (updatedMailSettings['sendMethod'] == 'RELAY')

        #Restart UVM, this should set the sendMethod mode to DIRECT
        uvmContext = global_functions.restart_uvm()
        restartedMailSettings = uvmContext.mailSender().getSettings()
        assert (restartedMailSettings['sendMethod'] == 'DIRECT')
    def test_100_blocked_queue(self):
        """
        Non-blocking flush on network settings change with DIRECT method

        This non-standard test works as follows:
        1. Get a new uvm context with a 10 second timeout instead of 240 (non-standard)
        2. Wipe out exim spool directory and restart exim to ensure a "fresh" system condition.
        3. Configure mail sender as direct.
        4. Create a new email alert for "plenty of disk space"
        5. Wait for event to be generated
        6. Wait for exim to try its first delivery attempt.
        7. Perform network settings change and verify it doesn't take 10 seconds.

        When restarting networking, we attempt to flush mail queue since
        network settings may materially change settings that didn't work previously like WAN.
        However, with some email environments where messages will "never" be delivered,
        this causes network settings to block for the exim timeout period.
        Make sure we don't block for more than an acceptable amount of time. 
        """
        alert_description = "TEST Free disk space is actually fine"
        exim_spool_directory = "/var/spool/exim4"
        exim_service = "exim4"
        exim_log_filename = "/var/log/exim4/mainlog"

        tester_email_address = "tester@domain.com"
        orig_mail_settings = EmailTests.original_mail_settings

        # uvm_context_timeout = 60
        uvm_context_timeout = 30
        local_uvm_context = Uvm().getUvmContext(timeout=uvm_context_timeout)

        # Wipe out exim spool directory
        subprocess.call(f"systemctl stop {exim_service}", shell=True)
        subprocess.call(f"killall {exim_service} 2>/dev/null", shell=True)
        shutil.rmtree(f"{exim_spool_directory}/")
        subprocess.call(f"systemctl start {exim_service}", shell=True)
        # add administrator
        adminsettings = global_functions.uvmContext.adminManager().getSettings()
        # Test in direct mode
        mail_settings = copy.deepcopy(EmailTests.original_mail_settings)
        mail_settings["fromAddress"] = tester_email_address
        mail_settings["sendMethod"] = "DIRECT"
        mail_settings["smtpHost"] = "invalid.smtp.server" 
        mail_settings["smtpPort"] = 1234
        local_uvm_context.mailSender().setSettings(mail_settings)
        last_exim_log_line = subprocess.check_output(f"wc -l {exim_log_filename} | cut -d' ' -f1", shell=True).decode("utf-8").strip()
        print(f"last_exim_log_line={last_exim_log_line}")
        adminsettings = global_functions.uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append({
            "description": "System Administrator",
            "emailSummaries": True,
            "emailAlerts": True,
            "emailAddress": "admin_test@example.com",
            "javaClass": "com.untangle.uvm.AdminUserSettings",
            "username": "admin_test"
        })
        
        global_functions.uvmContext.adminManager().setSettings(adminsettings)

        original_event_settings = local_uvm_context.eventManager().getSettings()
        event_settings = copy.deepcopy(original_event_settings)
        event_settings["alertRules"]["list"].append({
               "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "comparator": "=",
                            "field": "class",
                            "fieldValue": "*SystemStatEvent*",
                            "javaClass": "com.untangle.uvm.event.EventRuleCondition"
                        },
                        {
                            "comparator": ">",
                            "field": "diskFreePercent",
                            "fieldValue": ".1",
                            "javaClass": "com.untangle.uvm.event.EventRuleCondition"
                        }
                    ]
                },
                "description": alert_description,
                "email": True,
                "emailLimitFrequency": True,
                "emailLimitFrequencyMinutes": 60,
                "enabled": True,
                "javaClass": "com.untangle.uvm.event.AlertRule",
                "log": True,
                "thresholdEnabled": False
        })
        local_uvm_context.eventManager().setSettings(event_settings)

        # Wait for alert event to be generated
        found_event = global_functions.get_wait_for_events(
            prefix="alert events",
            report_category="Events",
            report_title='Alert Events',
            matches={
                "description": alert_description
            },
            tries=60)
        print(f"found={found_event}")

        found_delivery_attempt = False
        if found_event:
            # Wait for exim to try its first attempt
            max_tries = 60
            tries = 0
            while tries < max_tries:
                print(f"awk 'NR >= {last_exim_log_line} && / \<\= {tester_email_address} H=localhost /{{ print NR, $0 }}' {exim_log_filename}")
                log_exim_attempt = subprocess.check_output(f"awk 'NR >= {last_exim_log_line} && / <= {tester_email_address} H=localhost /{{ print NR, $0 }}' {exim_log_filename}", shell=True).decode("utf-8")
                print(f"log_exim_attempt={log_exim_attempt}")
                if log_exim_attempt.strip() != "":
                    found_delivery_attempt = True
                    break
                tries += 1
                time.sleep(1)

        local_uvm_context.eventManager().setSettings(original_event_settings)
        # restore admin settings
        global_functions.uvmContext.adminManager().setSettings(orig_adminsettings)
        print(f"found_delivery_attempt={found_delivery_attempt}")

        elapsed_time = None
        if found_delivery_attempt:
            # Perform a network settings save
            start_time = datetime.datetime.now()
            network_settings = local_uvm_context.networkManager().getNetworkSettings()
            try:
                network_settings = local_uvm_context.networkManager().setNetworkSettings(network_settings)
            except:
                pass
            elapsed_time = (datetime.datetime.now() - start_time).total_seconds()
            print(f"elaspsed_time={elapsed_time}")

        # ?? verify exim still running properly

        # Restore system
        # re-wipe out exim spool directory
        subprocess.call(f"systemctl stop {exim_service}", shell=True)
        subprocess.call(f"killall {exim_service} 2>/dev/null", shell=True)
        shutil.rmtree(f"{exim_spool_directory}/")
        subprocess.call(f"systemctl start {exim_service}", shell=True)

        assert found_event, "found event"
        assert found_delivery_attempt, "found delivery attempt"
        assert elapsed_time < uvm_context_timeout, f"network settings completed in under {uvm_context_timeout}s"
        global_functions.uvmContext.mailSender().setSettings(orig_mail_settings)

    def test_020_safecheckparam_mailsender_send_test_message(self):
        """MailSenderImpl.sendTestMessage(recipient: EMAIL).

        sendTestMessage drops the recipient straight into an exim envelope.
        The EMAIL SafeType rejects shell metachars, embedded whitespace and
        addresses without a valid TLD.
        """
        mail_sender = global_functions.uvmContext.mailSender()
        # INVALID — semicolon inside local part (shell-style injection)
        with pytest.raises(Exception):
            mail_sender.sendTestMessage("admin;id@example.com")
        # INVALID — no TLD
        with pytest.raises(Exception):
            mail_sender.sendTestMessage("admin@localhost")
        # VALID — well-formed RFC-style address. The exim delivery may
        # subsequently fail (no real SMTP) but the validator should accept.
        try:
            mail_sender.sendTestMessage("ats-test@example.com")
        except Exception as e:
            assert "Invalid value in" not in str(e), \
                f"validator unexpectedly rejected a well-formed EMAIL: {e!r}"

    @staticmethod
    def _get_smtp_app():
        """Return the smtp app handle, or None if not installed."""
        try:
            app = global_functions.uvmContext.appManager().app("smtp")
            if app is None:
                return None
            return app
        except Exception:
            return None

    def test_200_quarantine_path_traversal_deleteInbox(self):
        """Quarantine deleteInbox must reject path-traversal addresses."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineMaintenenceView()
        traversal_addresses = [
            "../../../etc/passwd",
            "../../shadow",
            "user@example.com/../../etc",
            "user\\..\\..\\windows",
        ]
        for addr in traversal_addresses:
            with pytest.raises(Exception):
                quarantine.deleteInbox(addr)

    def test_210_quarantine_path_traversal_getInboxRecords(self):
        """Quarantine getInboxRecords must reject path-traversal addresses."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineUserView()
        traversal_addresses = [
            "../../../etc/passwd",
            "..%2f..%2fetc/shadow",
            "evil/../../../root",
        ]
        for addr in traversal_addresses:
            with pytest.raises(Exception):
                quarantine.getInboxRecords(addr)

    def test_220_quarantine_path_traversal_purge(self):
        """Quarantine purge must reject path-traversal account addresses."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineUserView()
        with pytest.raises(Exception):
            quarantine.purge("../../etc/passwd", ["fake-mail-id"])

    def test_230_quarantine_path_traversal_rescue(self):
        """Quarantine rescue must reject path-traversal account addresses."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineUserView()
        with pytest.raises(Exception):
            quarantine.rescue("../../etc/shadow", ["fake-mail-id"])

    def test_240_quarantine_control_char_rejection(self):
        """Quarantine must reject addresses containing control characters."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineMaintenenceView()
        control_char_addresses = [
            "user\x00@example.com",
            "user\n@example.com",
            "user\t@example.com",
        ]
        for addr in control_char_addresses:
            with pytest.raises(Exception):
                quarantine.deleteInbox(addr)

    def test_250_quarantine_slash_rejection(self):
        """Quarantine must reject addresses with slashes even without '..'."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineMaintenenceView()
        with pytest.raises(Exception):
            quarantine.deleteInbox("user/subdir@example.com")
        with pytest.raises(Exception):
            quarantine.deleteInbox("user\\subdir@example.com")

    def test_260_quarantine_valid_address_accepted(self):
        """Quarantine operations with valid addresses must not be blocked
        by the security validator (they may raise NoSuchInboxException which
        is the expected non-security error)."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineMaintenenceView()
        valid_addresses = [
            "user@example.com",
            "admin+tag@test.example.org",
            "dotted.name@company.co.uk",
            "_user@example.com",
            "+tag@example.com",
            "-list@example.com",
            "user_name@example.com",
            "user%tag@example.com",
        ]
        for addr in valid_addresses:
            try:
                quarantine.deleteInbox(addr)
            except Exception as e:
                err = str(e)
                assert "Invalid" not in err and "path traversal" not in err.lower(), \
                    f"valid address '{addr}' wrongly rejected by security validator: {e!r}"

    def test_270_quarantine_remap_traversal(self):
        """Quarantine remapSelfService must reject path-traversal targets."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        quarantine = smtp_app.getQuarantineUserView()
        with pytest.raises(Exception):
            quarantine.remapSelfService("legit@example.com", "../../../etc/passwd")

    def test_300_safecheck_email_address_rule_injection(self):
        """@SafeCheck(EMAIL_WILDCARD) on EmailAddressRule must reject
        addresses with shell metacharacters not in the allowed charset."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        settings = smtp_app.getSmtpSettings()
        orig_settings = copy.deepcopy(settings)
        try:
            injections = [
                "admin;rm -rf /@example.com",
                "user$(id)@example.com",
                "../../../etc/passwd",
            ]
            for injection_addr in injections:
                settings = copy.deepcopy(orig_settings)
                settings['quarantineSettings']['allowedAddressPatterns'] = {
                    "javaClass": "java.util.LinkedList",
                    "list": [{
                        "javaClass": "com.untangle.app.smtp.EmailAddressRule",
                        "addr": injection_addr
                    }]
                }
                with pytest.raises(Exception):
                    smtp_app.setSmtpSettings(settings)
        finally:
            smtp_app.setSmtpSettings(orig_settings)

    def test_310_safecheck_email_address_rule_valid(self):
        """@SafeCheck(EMAIL_WILDCARD) must accept valid wildcard addresses."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        settings = smtp_app.getSmtpSettings()
        orig_settings = copy.deepcopy(settings)
        try:
            valid_addrs = [
                "*",
                "*@example.com",
                "*@hhs.com",
                "*@*.com",
                "admin@example.org",
                "_user@example.com",
                "+tag@example.com",
                "-list@example.com",
            ]
            for valid_addr in valid_addrs:
                settings = copy.deepcopy(orig_settings)
                settings['quarantineSettings']['allowedAddressPatterns'] = {
                    "javaClass": "java.util.LinkedList",
                    "list": [{
                        "javaClass": "com.untangle.app.smtp.EmailAddressRule",
                        "addr": valid_addr
                    }]
                }
                try:
                    smtp_app.setSmtpSettings(settings)
                except Exception as e:
                    assert "Invalid value in" not in str(e), \
                        f"valid wildcard address '{valid_addr}' rejected: {e!r}"
        finally:
            smtp_app.setSmtpSettings(orig_settings)

    def test_315_safecheck_email_pair_rule_valid(self):
        """@SafeCheck(EMAIL_GLOB) on EmailAddressPairRule must accept valid
        email and glob addresses in quarantine forwards."""
        smtp_app = self._get_smtp_app()
        if smtp_app is None:
            raise unittest.SkipTest("smtp app not installed")

        settings = smtp_app.getSmtpSettings()
        orig_settings = copy.deepcopy(settings)
        try:
            valid_pairs = [
                ("user@example.com", "admin@example.com"),
                ("*@example.com", "admin@example.com"),
                ("*@hhs.com", "admin@example.com"),
                ("*@*.com", "admin@example.com"),
                ("_user@example.com", "admin@example.com"),
                ("+tag@example.com", "admin@example.com"),
                ("-list@example.com", "admin@example.com"),
            ]
            for addr1, addr2 in valid_pairs:
                settings = copy.deepcopy(orig_settings)
                settings['quarantineSettings']['addressRemaps'] = {
                    "javaClass": "java.util.LinkedList",
                    "list": [{
                        "javaClass": "com.untangle.app.smtp.EmailAddressPairRule",
                        "address1": addr1,
                        "address2": addr2
                    }]
                }
                try:
                    smtp_app.setSmtpSettings(settings)
                except Exception as e:
                    assert "Invalid value in" not in str(e), \
                        f"valid forward pair ('{addr1}', '{addr2}') rejected: {e!r}"
        finally:
            smtp_app.setSmtpSettings(orig_settings)

    @classmethod
    def final_extra_tear_down(cls):
        if EmailTests.original_mail_settings is not None:
            global_functions.uvmContext.mailSender().setSettings(EmailTests.original_mail_settings)

test_registry.register_module("email", EmailTests)