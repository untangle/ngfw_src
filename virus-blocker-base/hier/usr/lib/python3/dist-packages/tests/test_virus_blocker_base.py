"""virus_blocker_base tests"""

import subprocess
from datetime import datetime
import sys
import socket
import platform
import pytest
import unittest
import runtests

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr

default_policy_id = 1
app = None
appSSL = None
appSSLData = None
canRelay = True
clamavNotReady = False
testsite = global_functions.TEST_SERVER_HOST
testsiteIP = socket.gethostbyname(testsite)

def addPassSite(app, site, enabled=True, description="description"):
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.app.GenericRule", "string": site }
    rules = app.getPassSites()
    rules["list"].append(newRule)
    app.setPassSites(rules)

def nukePassSites(app):
    rules = app.getPassSites()
    rules["list"] = []
    app.setPassSites(rules)

def createSSLInspectRule(port="25"):
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
                    "conditionType": "PROTOCOL",
                    "invert": False,
                    "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleCondition",
                    "value": "TCP"
                },
                {
                    "conditionType": "DST_PORT",
                    "invert": False,
                    "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRuleCondition",
                    "value": port
                }
            ]
        },
        "description": "Inspect" + port,
        "javaClass": "com.untangle.app.ssl_inspector.SslInspectorRule",
        "live": True,
        "ruleId": 1
    }

def enableFileExtensionScan(app, stringtype):
    virusSettings = app.getSettings()
    for index, extension in enumerate(virusSettings["httpFileExtensions"]["list"]):
        if (extension["string"] == stringtype):
            virusSettings["httpFileExtensions"]["list"][index]["enabled"] = True
            app.setSettings(virusSettings)


class VirusBlockerBaseTests(NGFWTestCase):

    @staticmethod
    def appNameSSLInspector():
        return "ssl-inspector"

    @classmethod
    def initial_extra_setup(cls):
        global app, md5StdNum, appSSL, appSSLData, canRelay
        app = cls._app.getName()
        #For pppoe server clamav takes more time to get ready for connection so need to validate it first
        if app == "virus_blocker_lite":
            global clamavNotReady
            clamavNotReady = global_functions.clamav_not_ready_for_connections()

        # download eicar and trojan files before installing virus
        # blocker, using HTTPS to avoid any scanner potentially
        # sitting on our path
        cls.ftp_user_name, cls.ftp_password = global_functions.get_live_account_info("ftp")
        remote_control.run_command("rm -f /tmp/eicar /tmp/std_022_ftpVirusBlocked_file /tmp/temp_022_ftpVirusPassSite_file")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/eicar", uri="https://test.untangle.com/virus/eicar.com"))
        assert (result == 0)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/std_022_ftpVirusBlocked_file", uri="https://test.untangle.com/virus/fedexvirus.zip"))
        assert (result == 0)
        md5StdNum = remote_control.run_command("\"md5sum /tmp/std_022_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
        cls.md5StdNum = md5StdNum
        # print("md5StdNum <%s>" % md5StdNum)
        assert (result == 0)

        try:
            canRelay = global_functions.send_test_email(mailhost=testsiteIP)
        except Exception as e:
            canRelay = False

        if (global_functions.uvmContext.appManager().isInstantiated(cls.appNameSSLInspector())):
            if cls.skip_instantiated():
                pytest.skip('app %s already instantiated' % cls.appNameSSLInspector())
            else:
                appSSL = global_functions.uvmContext.appManager().app(cls.appNameSSLInspector())
        else:
            appSSL = global_functions.uvmContext.appManager().instantiate(cls.appNameSSLInspector(), default_policy_id)

        appSSLData = appSSL.getSettings()
        # Enable cloud connection
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['cloudEnabled'] = True
        global_functions.uvmContext.systemManager().setSettings(system_settings)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

    # test that client can http download zip
    def test_012_httpNonVirusNotBlocked(self):
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/test/test.zip") + " 2>&1 | grep -q text123")
        assert (result == 0)

    # test that client can http download pdf
    def test_013_httpNonVirusPDFNotBlocked(self):
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/test/test.pdf"))
        assert (result == 0)

    # test that client can block virus http download zip
    @pytest.mark.failure_behind_ngfw
    def test_015_httpEicarBlocked(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        pre_events_scan = global_functions.get_app_metric_value(self._app,"scan")
        pre_events_block = global_functions.get_app_metric_value(self._app,"block")

        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/virus/eicar.zip") + " 2>&1 | grep -q blocked")
        assert (result == 0)

        post_events_scan = global_functions.get_app_metric_value(self._app,"scan")
        post_events_block = global_functions.get_app_metric_value(self._app,"block")

        assert(pre_events_scan < post_events_scan)
        assert(pre_events_block < post_events_block)

    # test that client can block virus http download exe
    def test_016_httpVirusBlocked(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        if clamavNotReady:
            raise unittest.SkipTest(" ClamAV not ready to accept connections")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/virus/virus.exe") + " 2>&1 | grep -q blocked")
        assert (result == 0)

    # test that client can block virus http download zip
    def test_017_httpVirusZipBlocked(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        if clamavNotReady:
            raise unittest.SkipTest(" ClamAV not ready to accept connections")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://" + testsite + "/virus/fedexvirus.zip") + " 2>&1 | grep -q blocked")
        assert (result == 0)

    # test that client can block a partial fetch after full fetch (using cache)
    def test_018_httpPartialVirusBlockedWithCache(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        if clamavNotReady:
            raise unittest.SkipTest(" ClamAV not ready to accept connections")
        result = remote_control.run_command(global_functions.build_curl_command(uri="http://" + testsite + "/virus/virus.exe", location=True) + " 2>&1 | grep -q blocked")
        assert (result == 0)
        result = remote_control.run_command(global_functions.build_curl_command(uri="http://" + testsite + "/virus/virus.exe", location=True, range="'5-'") + " 2>&1 | grep -q blocked")
        assert (result == 0)

    # test that client can download virus http zip from pass site
    @pytest.mark.failure_behind_ngfw
    def test_019_httpEicarPassSite(self):
        addPassSite(self._app, testsite)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://" + testsite + "/virus/eicar.zip") + " 2>&1 | grep -q blocked")
        nukePassSites(self._app)
        assert (result == 1)

    # test that client can ftp download zip
    def test_021_ftpNonVirusNotBlocked(self):
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", user=self.ftp_user_name, password=self.ftp_password, uri="ftp://" + global_functions.ftp_server + "/test.zip"))
        assert (result == 0)

    # test that client can ftp download PDF
    def test_023_ftpNonVirusPDFNotBlocked(self):
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", user=self.ftp_user_name, password=self.ftp_password, uri="ftp://" + global_functions.ftp_server + "/test/test.pdf"))
        assert (result == 0)

    # test that client can block virus ftp download zip
    @pytest.mark.failure_behind_ngfw
    def test_025_ftpVirusBlocked(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        if clamavNotReady:
            raise unittest.SkipTest(" ClamAV not ready to accept connections")
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        remote_control.run_command("rm -f /tmp/temp_025_ftpVirusBlocked_file")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/temp_025_ftpVirusBlocked_file", user=self.ftp_user_name, password=self.ftp_password, uri="ftp://" + global_functions.ftp_server + "/virus/fedexvirus.zip"))
        assert (result == 0)
        md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_025_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
        print("md5StdNum <%s> vs md5TestNum <%s>" % (md5StdNum, md5TestNum))
        assert (md5StdNum != md5TestNum)

        events = global_functions.get_events(self.displayName(),'Infected Ftp Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "s_server_addr", global_functions.ftp_server,
                                            "c_client_addr", remote_control.client_ip,
                                            "uri", "fedexvirus.zip",
                                            self.shortName() + '_clean', False )
        assert( found )

    # test that client can ftp download zip from pass site
    @pytest.mark.failure_behind_ngfw
    def test_027_ftpVirusPassSite(self):
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        addPassSite(self._app, global_functions.ftp_server)
        remote_control.run_command("rm -f /tmp/temp_027_ftpVirusPassSite_file")
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/temp_027_ftpVirusPassSite_file", user=self.ftp_user_name, password=self.ftp_password, uri="ftp://" + global_functions.ftp_server + "/virus/fedexvirus.zip"))
        nukePassSites(self._app)
        assert (result == 0)
        md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_027_ftpVirusPassSite_file | awk '{print $1}'\"", stdout=True)
        print("md5StdNum <%s> vs md5TestNum <%s>" % (md5StdNum, md5TestNum))
        assert (md5StdNum == md5TestNum)

    @pytest.mark.failure_behind_ngfw
    def test_100_eventlog_httpVirus(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        fname = sys._getframe().f_code.co_name
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://" + testsite + f"/virus/eicar.zip?arg={fname}") + " 2>&1 | grep -q blocked")
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Infected Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host", testsite,
                                            "uri", ("/virus/eicar.zip?arg=%s" % fname),
                                            self.shortName() + '_clean', False )
        assert( found )

    def test_101_eventlog_httpNonVirus(self):
        fname = sys._getframe().f_code.co_name
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://" + testsite + f"/test/test.zip?arg={fname}") + " 2>&1 | grep -q text123")
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host", testsite,
                                            "uri", ("/test/test.zip?arg=%s" % fname),
                                            self.shortName() + '_clean', True )
        assert( found )

    @pytest.mark.failure_behind_ngfw
    def test_102_eventlog_ftpVirus(self):
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        if clamavNotReady:
            raise unittest.SkipTest(" ClamAV not ready to accept connections")
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        fname = sys._getframe().f_code.co_name
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/temp_022_ftpVirusBlocked_file", user=self.ftp_user_name, password=self.ftp_password, uri="ftp://" + global_functions.ftp_server + "/virus/fedexvirus.zip"))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Infected Ftp Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "uri", "fedexvirus.zip",
                                            self.shortName() + '_clean', False )
        assert( found )

    def test_103_eventlog_ftpNonVirus(self):
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        fname = sys._getframe().f_code.co_name
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", user=self.ftp_user_name, password=self.ftp_password, uri="ftp://" + global_functions.ftp_server + "/test.zip"))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Ftp Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "uri", "test.zip",
                                            self.shortName() + '_clean', True )
        assert( found )

    def test_104_eventlog_smtpVirus(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + testsiteIP)
        if clamavNotReady:
            raise unittest.SkipTest(" ClamAV not ready to accept connections")
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        # download the email script
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/email_script.py", uri="http://" + testsite + "/test/email_script.py"))
        assert (result == 0)
        result = remote_control.run_command("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # email the file
        result = remote_control.run_command("/tmp/email_script.py --server=%s --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/eicar" % (testsiteIP, fname))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Infected Email Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "addr", "junk@test.untangle.com",
                                            "subject", str(fname),
                                            self.shortName() + '_clean', False,
                                            min_date=startTime )
        assert( found )

    def test_105_eventlog_smtpNonVirus(self):
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + testsite)
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        print("fname: %s" % fname)
        result = remote_control.run_command("echo '%s' > /tmp/attachment-%s" % (fname, fname))
        assert (result == 0)
        # download the email script
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/email_script.py", uri="http://" + testsite + "/test/email_script.py"))
        assert (result == 0)
        result = remote_control.run_command("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # email the file
        result = remote_control.run_command("/tmp/email_script.py --server=%s --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/attachment-%s" % (testsiteIP, fname, fname))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Email Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "addr", "junk@test.untangle.com",
                                            "subject", str(fname),
                                            self.shortName() + '_clean', True,
                                            min_date=startTime )
        assert( found )

    def test_106_eventlog_smtpVirusPassList(self):
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + testsite)
        addPassSite(self._app, testsiteIP)
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        result = remote_control.run_command("echo '%s' > /tmp/attachment-%s" % (fname, fname))
        if result != 0:
            nukePassSites(self._app)
            assert( False )
        # download the email script
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/email_script.py", uri="http://" + testsite + "/test/email_script.py"))
        if result != 0:
            nukePassSites(self._app)
            assert( False )
        result = remote_control.run_command("chmod 775 /tmp/email_script.py")
        if result != 0:
            nukePassSites(self._app)
            assert( False )
        # email the file
        result = remote_control.run_command("/tmp/email_script.py --server=%s --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/eicar" % (testsiteIP, fname))
        nukePassSites(self._app)
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Email Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "addr", "junk@test.untangle.com",
                                            "subject", str(fname),
                                            self.shortName() + '_clean', True,
                                            min_date=startTime )
        assert( found )

    def test_110_eventlog_smtpSSLVirus(self):
        if platform.machine().startswith('arm'):
            raise unittest.SkipTest("local scanner not available on ARM")
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + testsiteIP)
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        # download the email script
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/email_script.py", uri="http://" + testsite + "/test/email_script.py"))
        assert (result == 0)
        result = remote_control.run_command("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # Turn on SSL Inspector
        appSSLData['processEncryptedMailTraffic'] = True
        appSSLData['ignoreRules']['list'].insert(0,createSSLInspectRule("25"))
        appSSL.setSettings(appSSLData)
        appSSL.start()
        # email the file
        result = remote_control.run_command("/tmp/email_script.py --server=%s --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/eicar --starttls" % (testsiteIP, fname),nowait=False)
        appSSL.stop()
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Infected Email Events',None,1)
        # print(events['list'][0])
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "addr", "junk@test.untangle.com",
                                            "subject", str(fname),
                                            's_server_addr', testsiteIP,
                                            self.shortName() + '_clean', False,
                                            min_date=startTime)
        assert( found )

    # test ftp using large test file
    @pytest.mark.slow
    def test_120_ftpLargeClean(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        if clamavNotReady:
            raise unittest.SkipTest(" ClamAV not ready to accept connections")    
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        md5LargePDFClean = "06b3cc0a1430c2aaf449b46c72fecee5"
        remote_control.run_command("rm -f /tmp/temp_120_ftpVirusClean_file")
        result = remote_control.run_command(global_functions.build_wget_command(timeout=20, output_file="/tmp/temp_120_ftpVirusClean_file", user=self.ftp_user_name, password=self.ftp_password, uri="ftp://" + global_functions.ftp_server + "/debian-live-8.6.0-amd64-standard.iso"))
        assert (result == 0)
        md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_120_ftpVirusClean_file | awk '{print $1}'\"", stdout=True)
        print("md5LargePDFClean <%s> vs md5TestNum <%s>" % (md5LargePDFClean, md5TestNum))
        assert (md5LargePDFClean == md5TestNum)

    def test_200_scanFileExtension(self):
        """test that "Scan" option in advanced tab is scanned, using zip file"""
        #find 'zip' file extension and enable scan option
        enableFileExtensionScan(self._app, "zip")

        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/test/test.zip") + " 2>&1" )

        events = global_functions.get_events(self.displayName(),'Scanned Web Events',None,1)
        assert(events != None)

        found = global_functions.check_events(events.get("list"), 50, 
                                                'host', 'test.untangle.com',
                                                'c_client_addr', remote_control.client_ip,
                                                'uri', '/test/test.zip')

        assert(found)

    @pytest.mark.failure_behind_ngfw
    def test_300_disableAllScans(self):
        virusSettings = self._app.getSettings()

        self._app.clearAllEventHandlerCaches()

        virusSettings['enableCloudScan'] = False
        virusSettings['enableLocalScan'] = False
        self._app.setSettings(virusSettings)

        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/virus/eicar.zip") + " 2>&1 | grep -q blocked")

        virusSettings['enableCloudScan'] = True
        virusSettings['enableLocalScan'] = True
        self._app.setSettings(virusSettings)
        assert (result != 0)

    @classmethod
    def final_extra_tear_down(cls):
        global appSSL
        if appSSL != None:
            global_functions.uvmContext.appManager().destroy( appSSL.getAppSettings()["id"] )
            appSSL = None
