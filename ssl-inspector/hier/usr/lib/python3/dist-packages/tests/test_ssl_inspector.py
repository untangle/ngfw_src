"""ssl_inspector tests"""
import base64
import datetime
import os
import pytest
import runtests
import shlex
import subprocess
import sys
import time
import unittest

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
app = None
appWeb = None
ssl_app_2 = None
policy_app = None
secondRackId = None
pornServerName="www.pornhub.com"
testedServerName="news.ycombinator.com"
testedServerURLParts = testedServerName.split(".")
testedServerDomainWildcard = "*" + testedServerURLParts[-2] + "." + testedServerURLParts[-1]
dropboxIssuer="C = US, ST = California, L = San Francisco, O = \\\"Dropbox, Inc\\\""
hostnameVerifyBypassDetail = "Hostname Verification Bypassed"


def createSSLInspectRule(url=testedServerDomainWildcard):
    return {
        "action": {
            "actionType": "INSPECT",
            "flag": False,
            "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleAction"
        },
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "conditionType": "SSL_INSPECTOR_SNI_HOSTNAME",
                    "invert": False,
                    "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleCondition",
                    "value": url
                }
            ]
        },
        "description": url,
        "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRule",
        "enabled": True,
        "ruleId": 1
    };


def findRule(target_description):
    found = False
    for rule in appData['ignoreRules']['list']:
        if rule['description'] == target_description:
            found = True
            break
    return found
    

def addBlockedUrl(url, blocked=True, flagged=True, description="description"):
    newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.app.GenericRule", "string": url }
    rules = appWeb.getBlockedUrls()
    rules["list"].append(newRule)
    appWeb.setBlockedUrls(rules)


def nukeBlockedUrls():
    rules = appWeb.getBlockedUrls()
    rules["list"] = []
    appWeb.setBlockedUrls(rules)


def search_term_rule_add(termWords, blocked=True, flagged=True, description="description"):
    newTerm =  {
        "blocked": blocked,
        "flagged": flagged,
        "description": description,
        "javaClass": "com.untangle.uvm.app.GenericRule",
        "string": termWords,
        }
    webSettings = appWeb.getSettings()
    webSettings["searchTerms"]["list"].append(newTerm)
    appWeb.setSettings(webSettings)


def search_term_rules_clear():
    webSettings = appWeb.getSettings()
    webSettings["searchTerms"]["list"] = []
    appWeb.setSettings(webSettings)


def createHostnameBypassRule(hostname, enabled=True, description="test bypass"):
    return {
        "javaClass": "com.untangle.uvm.app.GenericRule",
        "string": hostname,
        "enabled": enabled,
        "isGlobal": False,
        "description": description
    }


def addHostnameBypassEntry(app, hostname, enabled=True, isGlobal=False, description="test"):
    appData = app.getSettings()
    rule = createHostnameBypassRule(hostname, enabled=enabled, description=description)
    rule['isGlobal'] = isGlobal
    appData['hostnameVerificationBypassList']['list'].append(rule)
    app.setSettings(appData)


def clearHostnameBypassList(app):
    appData = app.getSettings()
    appData['hostnameVerificationBypassList']['list'] = []
    app.setSettings(appData)


def getHostnameBypassList(app):
    return app.getSettings()['hostnameVerificationBypassList']['list']


# ---------------------------------------------------------------------------
# SNI SSRF (NGFW-15841) — raw TLS ClientHello sender used by the SNI-SSRF tests.
#
# Python's ssl module IDNA-encodes server_hostname before sending, which
# either errors out or silently strips the SNI extension for payloads that
# contain ':', '/', CR/LF, leading '-', or exceed the 255-byte cap.  None of
# those reach the firewall via ssl.wrap_socket, so they can't drive the patched
# code path.  This minimal builder constructs a TLS 1.2 ClientHello byte-for-
# byte and writes any SNI bytes the test wants, bypassing client-side cleaning.
# The SNI argument is passed base64-encoded so any byte value survives shell
# escaping.
# ---------------------------------------------------------------------------
_RAW_SNI_CLIENT_SCRIPT = r'''
import base64, os, socket, struct, sys
def build(sni):
    sn = b"\x00" + struct.pack("!H", len(sni)) + sni
    snl = struct.pack("!H", len(sn)) + sn
    sni_ext = struct.pack("!HH", 0x0000, len(snl)) + snl
    extensions = sni_ext
    ext_block = struct.pack("!H", len(extensions)) + extensions
    cipher_suites = struct.pack("!H", 2) + b"\x00\x3c"
    body = (b"\x03\x03" + os.urandom(32) + b"\x00"
            + cipher_suites + b"\x01\x00" + ext_block)
    hs = b"\x01" + struct.pack("!I", len(body))[1:] + body
    return b"\x16\x03\x01" + struct.pack("!H", len(hs)) + hs
host, port, sni_b64 = sys.argv[1], int(sys.argv[2]), sys.argv[3]
sni = base64.b64decode(sni_b64)
ch = build(sni)
s = socket.create_connection((host, port), timeout=5)
s.sendall(ch)
s.settimeout(2)
try: s.recv(4096)
except socket.timeout: pass
finally: s.close()
'''


def _send_raw_sni_from_client(host, port, sni_bytes):
    """Send a raw TLS ClientHello with arbitrary SNI bytes from the LAN client.

    sni_bytes is a Python bytes object — may contain any value (CR/LF/NUL/etc.).
    The whole helper script is base64-encoded into a single shell command so
    runtests.remote_control.run_command doesn't have to handle multi-line
    quoting.  Returns the run_command exit code.
    """
    script_b64 = base64.b64encode(_RAW_SNI_CLIENT_SCRIPT.encode()).decode()
    sni_b64 = base64.b64encode(sni_bytes).decode()
    cmd = (f"echo {script_b64} | base64 -d | "
           f"python3 - {shlex.quote(host)} {port} {shlex.quote(sni_b64)}")
    return remote_control.run_command(cmd)


def _inspector_log_path(app_obj):
    """Return /var/log/uvm/app-<N>.log for the running ssl-inspector instance.

    Takes the test class's self._app rather than reading the module-level `app`
    global, which is only populated when SslInspectorTests.module_name() runs
    during full-class setup.  When ATS invokes individual test methods, that
    global may still be None.
    """
    return f"/var/log/uvm/app-{app_obj.getAppSettings()['id']}.log"


def _log_line_count(path):
    return int(subprocess.check_output(
        f"wc -l {shlex.quote(path)} | cut -d' ' -f1",
        shell=True).decode().strip())


def _log_lines_since(path, start_line, pattern):
    """awk-grep new log lines >= start_line whose body matches pattern."""
    out = subprocess.check_output(
        f"awk 'NR >= {start_line} && /{pattern}/' {shlex.quote(path)} || true",
        shell=True).decode()
    return [ln for ln in out.split('\n') if ln.strip()]


@pytest.mark.ssl_inspector
class SslInspectorTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = SslInspectorTests._app
        return "ssl-inspector"

    @staticmethod
    def appWeb():
        return "web-filter"

    @classmethod
    def initial_extra_setup(cls):
        global appData, appWeb, appWebData, ssl_app_2, policy_app, secondRackId

        global_functions.get_latest_client_test_pkg("web")

        appData = cls._app.getSettings()
        if (global_functions.uvmContext.appManager().isInstantiated(cls.appWeb())):
            raise Exception('app %s already instantiated' % cls.appWeb())
        appWeb = global_functions.uvmContext.appManager().instantiate(cls.appWeb(), default_policy_id)
        appWebData = appWeb.getSettings()

        appData['ignoreRules']['list'].insert(0,createSSLInspectRule(testedServerDomainWildcard))
        cls._app.setSettings(appData)

        if (global_functions.uvmContext.appManager().isInstantiated("policy-manager")):
            raise Exception('app policy-manager already instantiated')
        policy_app = global_functions.uvmContext.appManager().instantiate("policy-manager")
        secondRackId = global_functions.addRack(policy_app, name="Second Rack")
        ssl_app_2 = global_functions.uvmContext.appManager().instantiate("ssl-inspector", secondRackId)
        
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)
            
    def test_011_license_valid(self):
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_012_checkServerCertificate(self):
        result = remote_control.run_command('echo -n | openssl s_client -connect %s:443 -servername %s 2>/dev/null | grep -qi -e "arista" -e "untangle"' % (testedServerName, testedServerName))
        assert (result == 0)

    def test_015_checkWebFilterBlockInspected(self):
        addBlockedUrl(testedServerName)
        remote_control.run_command(global_functions.build_curl_command(trace_ascii_file="/tmp/ssl_test_015.trace", output_file="/tmp/ssl_test_015.output", uri=f"https://{testedServerName}"))
        nukeBlockedUrls()
        result = remote_control.run_command('grep blockpage /tmp/ssl_test_015.trace')
        assert (result == 0)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"https://{pornServerName}") +  " 2>&1 | grep -q blockpage" )
        assert (result == 0)        

    @pytest.mark.failure_in_podman
    def test_020_checkIgnoreCertificate(self):
        if findRule('Ignore Dropbox'):
            result = remote_control.run_command('echo -n | openssl s_client -connect www.dropbox.com:443 -servername www.dropbox.com 2>/dev/null | grep -q \'%s\'' % (dropboxIssuer))
            assert (result == 0)
        else:
            raise unittest.SkipTest('SSL Inspector does not have Ignore Dropbox rule')

    def test_030_checkSslInspectorInspectorEventLog(self):
        remote_control.run_command(global_functions.build_curl_command(trace_ascii_file="/tmp/ssl_test_030.trace", output_file="/tmp/ssl_test_030.output", uri=f"https://{testedServerName}"))
        events = global_functions.get_events('SSL Inspector','All Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "ssl_inspector_status","INSPECTED",
                                            "ssl_inspector_detail",testedServerName)
        assert( found )

    def test_040_checkWebFilterEventLog(self):
        addBlockedUrl(testedServerName)
        remote_control.run_command(global_functions.build_curl_command(trace_ascii_file="/tmp/ssl_test_040.trace", output_file="/tmp/ssl_test_040.output", uri=f"https://{testedServerName}"))
        nukeBlockedUrls()

        events = global_functions.get_events('Web Filter','Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host", testedServerName,
                                            "web_filter_blocked", True)
        assert( found )

    # Query eventlog
    def test_060_queryEventLog(self):
        termTests = [{
            "host": "www.bing.com",
            "uri":  "/search?q=oneterm&qs=n&form=QBRE",
            "term": "oneterm"
        },{
            "host": "www.bing.com",
            "uri":  "/search?q=two+terms&qs=n&form=QBRE",
            "term": "two terms"
        },{
            "host": "www.bing.com",
            "uri":  "/search?q=%22quoted+terms%22&qs=n&form=QBRE",
            "term": '"quoted terms"'
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=oneterm",
            "term": "oneterm"
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=%22quoted+terms%22",
            "term": '"quoted terms"'
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=two+terms",
            "term": "two terms"
        }]
        host = "www.bing.com"
        uri = "/search?q=oneterm&qs=n&form=QBRE"
        for t in termTests:
            eventTime = datetime.datetime.now()
            result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1", uri=f"https://{t['host']}{t['uri']}"))
            assert( result == 0 )

            events = global_functions.get_events('Web Filter','All Search Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                "host", t["host"],
                                                "term", t["term"])
            assert( found )

    def test_100_hostnameVerificationEnabled(self):
        """Verify hostname verification enabled by default, normal HTTPS works without bypass pipe"""
        appData = self._app.getSettings()
        assert appData.get('verifyServerCertHostname') == True
        result = remote_control.run_command(global_functions.build_curl_command(uri="https://" + testedServerName + "/"))
        assert (result == 0)
        events = global_functions.get_events('SSL Inspector', 'All Sessions', None, 5)
        event = global_functions.find_event(events.get('list'), 5,
            "ssl_inspector_status", "INSPECTED",
            "ssl_inspector_detail", testedServerName)
        assert event is not None
        assert hostnameVerifyBypassDetail not in event.get("ssl_inspector_detail", "")

    def test_101_hostnameVerificationBypassList(self):
        """Verify bypass list entry adds bypass pipe to detail"""
        appData = self._app.getSettings()
        appData['hostnameVerificationBypassList']['list'].append(createHostnameBypassRule(testedServerName))
        self._app.setSettings(appData)
        try:
            result = remote_control.run_command(global_functions.build_curl_command(uri="https://" + testedServerName + "/"))
            assert (result == 0)
            events = global_functions.get_events('SSL Inspector', 'All Sessions', None, 5)
            event = global_functions.find_event(events.get('list'), 5, "ssl_inspector_status", "INSPECTED")
            assert event is not None
            assert hostnameVerifyBypassDetail in event.get("ssl_inspector_detail", "")
        finally:
            appData['hostnameVerificationBypassList']['list'] = []
            self._app.setSettings(appData)

    def test_102_hostnameVerificationGlobalDisable(self):
        """Verify global disable adds bypass pipe to detail"""
        appData = self._app.getSettings()
        appData['verifyServerCertHostname'] = False
        self._app.setSettings(appData)
        try:
            result = remote_control.run_command(global_functions.build_curl_command(uri="https://" + testedServerName + "/"))
            assert (result == 0)
            events = global_functions.get_events('SSL Inspector', 'All Sessions', None, 5)
            event = global_functions.find_event(events.get('list'), 5, "ssl_inspector_status", "INSPECTED")
            assert event is not None
            assert hostnameVerifyBypassDetail in event.get("ssl_inspector_detail", "")
        finally:
            appData['verifyServerCertHostname'] = True
            self._app.setSettings(appData)

    def test_103_hostnameVerificationBlindTrust(self):
        """Verify blind trust skips hostname verification without bypass pipe"""
        appData = self._app.getSettings()
        appData['serverBlindTrust'] = True
        self._app.setSettings(appData)
        try:
            result = remote_control.run_command(global_functions.build_curl_command(uri="https://" + testedServerName + "/"))
            assert (result == 0)
            events = global_functions.get_events('SSL Inspector', 'All Sessions', None, 5)
            event = global_functions.find_event(events.get('list'), 5, "ssl_inspector_status", "INSPECTED")
            assert event is not None
            assert hostnameVerifyBypassDetail not in event.get("ssl_inspector_detail", "")
        finally:
            appData['serverBlindTrust'] = False
            self._app.setSettings(appData)

    def test_104_hostnameVerificationWildcardBypass(self):
        """Verify wildcard bypass entry matches and adds bypass pipe"""
        appData = self._app.getSettings()
        appData['hostnameVerificationBypassList']['list'].append(createHostnameBypassRule(testedServerDomainWildcard, description="wildcard test"))
        self._app.setSettings(appData)
        try:
            result = remote_control.run_command(global_functions.build_curl_command(uri="https://" + testedServerName + "/"))
            assert (result == 0)
            events = global_functions.get_events('SSL Inspector', 'All Sessions', None, 5)
            event = global_functions.find_event(events.get('list'), 5, "ssl_inspector_status", "INSPECTED")
            assert event is not None
            assert hostnameVerifyBypassDetail in event.get("ssl_inspector_detail", "")
        finally:
            appData['hostnameVerificationBypassList']['list'] = []
            self._app.setSettings(appData)

    def test_105_hostnameVerificationDisabledBypassEntry(self):
        """Verify disabled bypass entry does not add bypass pipe"""
        appData = self._app.getSettings()
        appData['hostnameVerificationBypassList']['list'].append(createHostnameBypassRule(testedServerName, enabled=False, description="disabled entry"))
        self._app.setSettings(appData)
        try:
            result = remote_control.run_command(global_functions.build_curl_command(uri="https://" + testedServerName + "/"))
            assert (result == 0)
            events = global_functions.get_events('SSL Inspector', 'All Sessions', None, 5)
            event = global_functions.find_event(events.get('list'), 5,
                "ssl_inspector_status", "INSPECTED",
                "ssl_inspector_detail", testedServerName)
            assert event is not None
            assert hostnameVerifyBypassDetail not in event.get("ssl_inspector_detail", "")
        finally:
            appData['hostnameVerificationBypassList']['list'] = []
            self._app.setSettings(appData)

    def test_551_https_with_sni_packet_split(self):
        """ Verify no exceptions with split Hello TLS packets"""
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        app_id = self._app.getAppSettings()["id"]
        log_file = f"/var/log/uvm/app-{app_id}.log"

        count = 0
        exceptions = 0

        # Not the entire packet, but a little bit beyond the position of the SNI server record.
        max_index=1600
        # Hit as many places as we could be out of bounds with split
        packet_split_iteration = 2

        for index in range(2, max_index,packet_split_iteration):
            last_log_line = subprocess.check_output(f"wc -l {log_file} | cut -d' ' -f1", shell=True).decode("utf-8").strip()
            last_log_line = int(last_log_line) + 1
            result = remote_control.run_command(f"./web/https_client.py -i {index}")

            log_invalid_exception = subprocess.check_output(f"awk 'NR >= {last_log_line} && /WARN  Exception calling extractSNIhostname/{{ print NR, $0 }}' {log_file}", shell=True).decode("utf-8")
            print(log_invalid_exception)
            for log in log_invalid_exception.split("\n"):
                if len(log) == 0:
                    continue
                exceptions += 1
                break

            count += 1

        print(f"count={count}, exceptions={exceptions}")
        assert exceptions == 0, "exceptions"

    def test_610_web_search_rules(self):
        """check the https web rule searches log correctly"""
        term = "boobs"

        termTests = [{
            "host": "www.bing.com",
            "uri":  ("/search?q=%s&qs=n&form=QBRE" % term),
        },{
            "host": "search.yahoo.com",
            "uri": ("/search?p=%s" % term),
        },{
            "host": "www.google.com",
            "uri":  ("/search?hl=en&q=%s" % term),
        }]
        search_term_rule_add(term)
        for t in termTests:
            eventTime = datetime.datetime.now()
            result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1", uri=f"https://{t['host']}{t['uri']}"))
            assert( result == 0 )
            events = global_functions.get_events("Web Filter",'All Search Events',None,1)
            found = global_functions.check_events( events.get('list'), 5,
                                                   "host", t["host"],
                                                   "term", term,
                                                   'blocked', True,
                                                   'flagged', True )
            assert( found )
        search_term_rules_clear()

    def test_650_web_search_rules(self):
        """check the web filter search terms with $ are evaluated and blocked if set
            %24 will be converted to $
        """
        term = "a$$"
        termTests = [{
            "host": "www.google.com",
            "uri":  ("/search?hl=en&q=%s" % "a%24%24"),
        }]
        search_term_rule_add(term)
        for t in termTests:
            eventTime = datetime.datetime.now()
            result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1", uri=f"https://{t['host']}{t['uri']}"))
            events = global_functions.get_events("Web Filter",'All Search Events',None,1)
            found = global_functions.check_events( events.get('list'), 5,
                                                   "host", t["host"],
                                                   "term", term,
                                                   'blocked', True,
                                                   'flagged', True )
            assert( found )
        search_term_rules_clear()


    def test_655_web_search_rules(self):
        """check the web filter search terms with $ are evaluated and blocked if set"""
        term = "a$$"
        termTests = [{
            "host": "www.google.com",
            "uri":  ("/search?hl=en&q=%s" % "a\$\$"),
        }]
        search_term_rule_add(term)
        for t in termTests:
            eventTime = datetime.datetime.now()
            result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user_agent="Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1", uri=f"https://{t['host']}{t['uri']}"))
            events = global_functions.get_events("Web Filter",'All Search Events',None,1)
            found = global_functions.check_events( events.get('list'), 5,
                                                   "host", t["host"],
                                                   "term", term,
                                                   'blocked', True,
                                                   'flagged', True )
            assert( found )
        search_term_rules_clear()




    def test_700_youtube_safe_search(self):
        """Verify activation of YouTube Safe Search"""
        raise unittest.SkipTest('Youtube Safe Search requires JS rendering web engine')
        fname = sys._getframe().f_code.co_name
        settings = appWeb.getSettings()
        settings["enableHttpsSni"] = False
        settings["enableHttpsSniCertFallback"] = False
        settings["restrictYoutube"] = True
        appWeb.setSettings(settings)
        
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"https://www.youtube.com/watch?v=esXGGdVL7ug") +  " 2>&1 | grep -q \'Loading icon\">'" )
        assert( result != 0 )

    # -----------------------------------------------------------------------
    # SNI SSRF (NGFW-15841) — SNI-driven SSRF in SSL Inspector CertCacheManager.
    #
    # Pre-fix: SNI bytes from the TLS ClientHello flowed straight into
    #   new URL("https://" + sni).openConnection().connect()
    # in CertCacheManagerImpl.fetchServerCertificate, letting an attacker steer
    # the firewall's outbound TCP to an arbitrary IP:port (and inject CR/LF
    # bytes into the cert-cache exception WARN line).
    #
    # Fix: validate the SNI at the inspector call site against SafeType.HOSTNAME
    # (LDH regex + length cap, explicit null/empty reject) before passing it to
    # the cache.  Malformed values fall back to session.getServerAddr() — the
    # same primitive the SNI-absent branch already uses.  The cache method
    # itself runs the same check at entry as defense-in-depth.  Log lines that
    # interpolate the cache key are routed through a safeForLog() wrapper that
    # strips CR/LF/TAB and truncates to 80 chars.
    #
    # These tests verify the security-relevant behavior end-to-end on a live
    # appliance: a malformed SNI must not produce an outbound prefetch keyed by
    # the attacker bytes; the SNI SSRF WARN must fire single-line, CR/LF-stripped.
    # -----------------------------------------------------------------------
    def test_720_sni_ssrf_legit_sni_no_warn(self):
        """V1 — A legitimate LDH hostname in SNI must not trigger the SNI SSRF
        WARN.  Negative regression check on the validator."""
        log_file = _inspector_log_path(self._app)
        start = _log_line_count(log_file) + 1

        remote_control.run_command(
            f"echo | openssl s_client -connect {testedServerName}:443 "
            f"-servername {testedServerName} -quiet 2>/dev/null >/dev/null"
        )
        time.sleep(2)

        warns = _log_lines_since(log_file, start, "SNI SSRF")
        assert warns == [], (
            f"Legitimate SNI {testedServerName!r} unexpectedly triggered "
            f"SNI SSRF WARN(s): {warns}"
        )

    def test_721_sni_ssrf_malformed_sni_with_port_path_rejected(self):
        """V2 — The audit payload (SNI with port + path + query) must be
        rejected by the SafeType.HOSTNAME check; SNI SSRF WARN must fire and
        name the IP-fallback target."""
        log_file = _inspector_log_path(self._app)
        uvm_log = "/var/log/uvm/uvm.log"
        # Snapshot both log line counts BEFORE the trigger so the post-trigger
        # grep is bounded to lines this test produced.  Without the uvm.log
        # snapshot, a prior unpatched-build run (or any future test that uses
        # the same payload) would leave a "CertCache Search 192.168.1.1:8443"
        # line lying around forever and fail this test on every later run.
        inspector_start = _log_line_count(log_file) + 1
        uvm_start = _log_line_count(uvm_log) + 1

        malicious_sni = b"192.168.1.1:8443/api/restart?x=1"
        _send_raw_sni_from_client(testedServerName, 443, malicious_sni)
        time.sleep(2)

        warns = _log_lines_since(
            log_file, inspector_start,
            "SNI SSRF: rejected malformed SNI for cert prefetch")
        assert warns, (
            "Expected SNI SSRF WARN for malformed SNI "
            f"{malicious_sni!r}; got none in new log lines.")
        assert any(b"192.168.1.1:8443" in w.encode() for w in warns), (
            f"SNI SSRF WARN should echo the rejected SNI bytes; got: {warns}")

        # The cert cache must not have been searched with the attacker payload —
        # if it had, we'd see a CertCache Search line keyed by the malicious SNI
        # in uvm.log.  Bounded to new lines so prior runs don't poison this.
        bad_searches = _log_lines_since(
            uvm_log, uvm_start,
            "CertCache Search 192.168.1.1:8443")
        assert not bad_searches, (
            f"Cert cache was searched with the attacker payload; the "
            f"validator must reject before the cache key is used: "
            f"{bad_searches}")

    def test_722_sni_ssrf_crlf_sni_no_log_injection(self):
        """V3 — SNI bytes containing CR/LF must be stripped to spaces by
        safeForLog so the WARN line cannot inject a synthetic log entry."""
        log_file = _inspector_log_path(self._app)
        start = _log_line_count(log_file) + 1

        injection_marker = "sni-ssrf-log-injected-v3"
        sni_bytes = f"evil.example.com\r\n{injection_marker}".encode()
        _send_raw_sni_from_client(testedServerName, 443, sni_bytes)
        time.sleep(2)

        warn_lines = _log_lines_since(
            log_file, start,
            "SslInspectorParserEventHandler.*SNI SSRF")
        assert len(warn_lines) == 1, (
            f"Expected exactly one SNI SSRF WARN line; got {len(warn_lines)}: "
            f"{warn_lines}")
        single = warn_lines[0]

        # safeForLog replaces \r and \n with spaces — the raw bytes must not
        # survive into the WARN line on the inspector's own log path.
        assert "\r" not in single, (
            f"safeForLog failed to strip CR from inspector WARN: {single!r}")
        # Embedded \n would have split the message across two awk records, so
        # the fact we got exactly one line is itself the no-CR/LF-survival
        # assertion; double-check the marker is present in the surviving line
        # rather than on a synthetic following line.
        assert injection_marker in single, (
            f"SNI SSRF WARN should still echo the (stripped) bad SNI bytes "
            f"including the marker '{injection_marker}': {single!r}")

    def test_723_sni_ssrf_leading_dash_sni_rejected(self):
        """V4 — SNI starting with '-' must be rejected (argv-option-injection
        defence — SafeType regex requires position 0 to be [A-Za-z0-9])."""
        log_file = _inspector_log_path(self._app)
        start = _log_line_count(log_file) + 1

        malicious_sni = b"-rfevil.example.com"
        _send_raw_sni_from_client(testedServerName, 443, malicious_sni)
        time.sleep(2)

        warns = _log_lines_since(
            log_file, start,
            "SNI SSRF: rejected malformed SNI for cert prefetch")
        assert warns, (
            f"Expected SNI SSRF WARN for leading-dash SNI {malicious_sni!r}; "
            f"got none.")
        assert any(b"-rfevil.example.com" in w.encode() for w in warns), (
            f"SNI SSRF WARN should echo the rejected SNI: {warns}")

    def test_724_sni_ssrf_oversize_sni_rejected(self):
        """V10 — SNI exceeding the SafeType.HOSTNAME 253-char cap must be
        rejected, and the WARN must show safeForLog's 80-char truncation
        (trailing '...')."""
        log_file = _inspector_log_path(self._app)
        start = _log_line_count(log_file) + 1

        long_sni = (b"a" * 254) + b".example.com"
        _send_raw_sni_from_client(testedServerName, 443, long_sni)
        time.sleep(2)

        warns = _log_lines_since(
            log_file, start,
            "SNI SSRF: rejected malformed SNI for cert prefetch")
        assert warns, (
            f"Expected SNI SSRF WARN for oversize SNI ({len(long_sni)} bytes); "
            "got none.")
        assert any("..." in w for w in warns), (
            f"Expected safeForLog truncation marker ('...') in WARN; "
            f"got: {warns}")

    def test_106_hostnameVerificationGlobalBypassPropagation(self):
        """Verify global bypass entries propagate across SSL Inspector instances"""
        # verify both start with empty bypass lists
        list1 = getHostnameBypassList(self._app)
        list2 = getHostnameBypassList(ssl_app_2)
        assert len(list1) == 0, "Instance 1 bypass list should be empty initially"
        assert len(list2) == 0, "Instance 2 bypass list should be empty initially"

        try:
            # add a global entry and a non-global entry on instance 1
            addHostnameBypassEntry(self._app, "global.example.com", isGlobal=True, description="global entry")
            addHostnameBypassEntry(self._app, "local.example.com", isGlobal=False, description="local entry")

            # verify instance 1 has both entries
            list1 = getHostnameBypassList(self._app)
            assert len(list1) == 2, "Instance 1 should have 2 entries, got %d" % len(list1)

            # verify instance 2 has only the global entry
            list2 = getHostnameBypassList(ssl_app_2)
            assert len(list2) == 1, "Instance 2 should have 1 global entry, got %d" % len(list2)
            assert list2[0]['string'] == "global.example.com"

            # clear all entries on instance 1 (this removes the global entry too)
            clearHostnameBypassList(self._app)

            # verify instance 2 no longer has the global entry
            list2 = getHostnameBypassList(ssl_app_2)
            assert len(list2) == 0, "Instance 2 should have 0 entries after clearing, got %d" % len(list2)

        finally:
            clearHostnameBypassList(self._app)

    @classmethod
    def final_extra_tear_down(cls):
        global appWeb, ssl_app_2, policy_app, secondRackId

        if ssl_app_2 != None:
            global_functions.uvmContext.appManager().destroy(ssl_app_2.getAppSettings()["id"])
            ssl_app_2 = None
        if policy_app != None:
            global_functions.nukeRules(policy_app)
            global_functions.removeRack(policy_app, secondRackId)
            global_functions.uvmContext.appManager().destroy(policy_app.getAppSettings()["id"])
            policy_app = None
        if appWeb != None:
            global_functions.uvmContext.appManager().destroy( appWeb.getAppSettings()["id"])
            appWeb = None

test_registry.register_module("ssl-inspector", SslInspectorTests)


@pytest.mark.ssl_inspector
class SslInspectorSecurityTests(NGFWTestCase):
    """Security regression tests for SslInspectorManager.

    Uses not_an_app=True so the class setup skips app instantiation entirely.
    This lets the test run on a live NGFW where ssl-inspector and web-filter
    are already installed, without triggering the 'app already instantiated'
    guard in initial_extra_setup that would skip the whole class.

    test_045 only calls certificateManager() which is always available via
    uvmContext — it has no dependency on either app's lifecycle.
    """

    not_an_app = True

    @staticmethod
    def module_name():
        return "ssl-inspector-security"

    def test_045_ssl_inspector_cert_san_no_injection(self):
        """Verify SslInspectorManager does not execute injection payloads from TLS cert CN/SAN.

        SslInspectorManager.generateFakeCertificate() extracts the CN (certSubject) and
        Subject Alternative Names (certSANlist) from the intercepted server certificate,
        then passes them to the ut-certgen script.

        Vulnerable pattern (line 397):
            exec(CERTIFICATE_GENERATOR_SCRIPT + argString)
        where argString is built by concatenating certSubject and certSANlist.

        A server certificate whose CN contains a shell payload such as:
            CN=evil$(touch /tmp/sentinel)
        or a SAN entry of:
            DNS:evil$(touch /tmp/sentinel)
        would cause the shell to execute "touch /tmp/sentinel" when the fake cert is generated.

        Fixed pattern (lines 393-395):
            execCommand(CERTIFICATE_GENERATOR_SCRIPT,
                        List.of(certFileName, certSubject.toString(), certSANlist.toString()))

        execCommand() passes each element as a separate argv entry to execvp() via
        ut-exec-safe-launcher, so the shell is never invoked and $() / backtick
        constructs are treated as literal characters.

        This test:
          1. Generates a self-signed X.509 certificate with injection payloads in the
             CN field and a SAN DNS entry using openssl subprocess.
          2. Simulates the cert subject and SAN extraction that SslInspectorManager
             performs when it intercepts a TLS connection to a server bearing that cert.
          3. Calls CertificateManager.generateServerCertificate() — which uses the same
             CERTIFICATE_GENERATOR_SCRIPT via execCommand() — with the extracted payloads.
          4. Asserts that no sentinel file was created, confirming the shell was never
             invoked and the injection payload was treated as a literal string.
        """
        import time
        import tempfile

        sentinel_file = "/tmp/ssl_inspector_san_injection_sentinel"

        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        # Injection payloads that would execute "touch <sentinel>" if the shell is invoked.
        # These mimic what a malicious TLS server might put in its certificate fields.
        malicious_cn_payload  = f"evil$(touch {sentinel_file})"
        malicious_san_payload = f"DNS:safe.local,DNS:evil$(touch {sentinel_file})"
        backtick_cn_payload   = f"evil`touch {sentinel_file}`"

        cert_manager = global_functions.uvmContext.certificateManager()

        try:
            # -----------------------------------------------------------------------
            # Part 1 — $() subshell in CN (certSubject).
            # SslInspectorManager builds certSubject as "/CN=<value>" and passes it
            # to execCommand().  With the fix the $() is a literal argument; without
            # the fix exec() expands it.
            # -----------------------------------------------------------------------
            cert_subject_with_payload = f"/CN={malicious_cn_payload}"
            try:
                cert_manager.generateServerCertificate(cert_subject_with_payload, "DNS:safe.local")
            except Exception:
                pass  # cert generation may fail for an invalid CN; only the sentinel matters

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via $() in TLS CN succeeded when processed by the "
                "certificate generator: the sentinel file was created. "
                "SslInspectorManager must pass certSubject via execCommand() rather than "
                "building a shell command string with exec()."
            )

            # -----------------------------------------------------------------------
            # Part 2 — $() subshell in SAN DNS entry (certSANlist).
            # SslInspectorManager appends each SAN value as "DNS:<value>" and passes
            # the whole list string to execCommand() as the third argument.
            # -----------------------------------------------------------------------
            try:
                cert_manager.generateServerCertificate("/CN=safe.local", malicious_san_payload)
            except Exception:
                pass

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via $() in TLS SAN entry succeeded when processed by "
                "the certificate generator: the sentinel file was created. "
                "SslInspectorManager must pass certSANlist via execCommand() rather than "
                "concatenating it into a shell command string."
            )

            # -----------------------------------------------------------------------
            # Part 3 — Backtick injection in CN.
            # rejectSuspiciousCharacter() (called in the generateServerCertificate path)
            # must detect the backtick and raise IllegalArgumentException before any
            # exec() call is reached.
            # -----------------------------------------------------------------------
            try:
                cert_manager.generateServerCertificate(f"/CN={backtick_cn_payload}", "DNS:safe.local")
            except Exception:
                pass  # exception from rejectSuspiciousCharacter is the expected outcome

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via backtick in TLS CN succeeded when processed by "
                "the certificate generator: the sentinel file was created. "
                "rejectSuspiciousCharacter() must reject backtick characters in the "
                "certSubject field before any exec() call is reached."
            )

            # -----------------------------------------------------------------------
            # Part 4 — Round-trip through a real X.509 certificate.
            # Generate a self-signed cert with a benign CN and a known SAN, extract
            # those fields (as SslInspectorManager does from a live TLS certificate),
            # then append injection payloads before feeding the strings to the cert
            # generator.  This simulates the full pipeline:
            #   real TLS cert → field extraction → execCommand(CERT_GENERATOR, args)
            # and verifies that shell metacharacters appended by a malicious server
            # to its cert fields do not execute when the cert generator is invoked.
            #
            # NOTE: OpenSSL's config parser treats '$' as a variable sigil and rejects
            # config files that contain '$(…)' in field values.  We therefore create a
            # cert with a safe CN ("safe-base"), extract it, then construct the attack
            # string in Python by appending the payload — exactly what SslInspectorManager
            # would encounter if it read a live cert whose CN field contained those chars.
            # -----------------------------------------------------------------------
            with tempfile.TemporaryDirectory() as tmpdir:
                key_path  = os.path.join(tmpdir, "test.key")
                cert_path = os.path.join(tmpdir, "test.crt")
                san_cfg   = os.path.join(tmpdir, "san.cnf")

                # Build a minimal openssl config with a safe CN and two SAN entries.
                san_config_content = (
                    "[req]\n"
                    "distinguished_name = dn\n"
                    "x509_extensions = v3_req\n"
                    "prompt = no\n"
                    "[dn]\n"
                    "CN = safe-base\n"
                    "[v3_req]\n"
                    "subjectAltName = @alt_names\n"
                    "[alt_names]\n"
                    "DNS.1 = safe.local\n"
                    "DNS.2 = www.safe.local\n"
                )
                with open(san_cfg, "w") as f:
                    f.write(san_config_content)

                # Generate RSA private key.
                rc = subprocess.call(
                    ["openssl", "genrsa", "-out", key_path, "2048"],
                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
                )
                if rc != 0:
                    raise unittest.SkipTest("openssl genrsa not available; skipping Part 4")

                # Generate self-signed cert using the safe config.
                rc = subprocess.call(
                    ["openssl", "req", "-new", "-x509", "-key", key_path,
                     "-out", cert_path, "-days", "1", "-config", san_cfg],
                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
                )
                if rc != 0:
                    raise unittest.SkipTest("openssl req not available; skipping Part 4")

                # Extract the subject string exactly as SslInspectorManager does:
                # it reads each RDN and rebuilds "/TYPE=value" pairs.
                subject_output = subprocess.check_output(
                    ["openssl", "x509", "-noout", "-subject", "-nameopt", "RFC2253",
                     "-in", cert_path],
                    stderr=subprocess.DEVNULL
                ).decode().strip()
                # openssl prints "subject= CN=safe-base" — normalise to "/CN=..." form.
                extracted_subject = "/" + subject_output.replace("subject=", "").strip().replace(",", "/")

                # Extract SAN entries as SslInspectorManager does.
                san_output = subprocess.check_output(
                    ["openssl", "x509", "-noout", "-ext", "subjectAltName", "-in", cert_path],
                    stderr=subprocess.DEVNULL
                ).decode()
                extracted_san_list = ""
                for line in san_output.splitlines():
                    line = line.strip()
                    if line.startswith("DNS:") or line.startswith("IP:"):
                        for entry in line.split(","):
                            entry = entry.strip()
                            if entry:
                                extracted_san_list = (extracted_san_list + "," + entry
                                                      if extracted_san_list else entry)

                # Simulate a malicious server appending injection payloads to its
                # certificate fields.  This produces strings identical to what
                # SslInspectorManager would build from a real compromised TLS cert.
                injected_subject = extracted_subject + f"$(touch {sentinel_file})"
                injected_san = extracted_san_list + f",DNS:evil$(touch {sentinel_file})"

                # Feed the injected values through the certificate generator.
                # With execCommand() the $() is a literal arg — no shell expansion.
                try:
                    cert_manager.generateServerCertificate(injected_subject, injected_san)
                except Exception:
                    pass

                time.sleep(1)
                assert not os.path.exists(sentinel_file), (
                    "Command injection via crafted TLS cert CN/SAN fields succeeded: "
                    f"sentinel file created (subject={injected_subject!r}, "
                    f"san={injected_san!r}). "
                    "SslInspectorManager must use execCommand() with a structured arg "
                    "list to prevent the shell from expanding injection payloads in "
                    "certSubject or certSANlist."
                )

        finally:
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

test_registry.register_module("ssl-inspector-security", SslInspectorSecurityTests)
