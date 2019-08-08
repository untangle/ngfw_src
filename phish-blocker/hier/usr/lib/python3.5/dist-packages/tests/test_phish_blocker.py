"""phish_blocker tests"""

import time
import subprocess
import socket
import re
import unittest
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
app = None
appData = None
appSSL = None
canRelay = True
mail_app = True
smtpServerHost = global_functions.TEST_SERVER_HOST

def getLatestMailSender():
    global mail_app
    remote_control.run_command("rm -f mailpkg.tar*") # remove all previous mail packages
    results = remote_control.run_command("wget -q -t 1 --timeout=3 http://test.untangle.com/test/mailpkg.tar")
    #  check if the download was successful
    if (results != 0):
        # check if existing mail app is already there
        mail_result = remote_control.run_command("test -x mailsender.py")
        if (mail_result != 0):
            mail_app = False
    # print("Results from getting mailpkg.tar <%s>" % results)
    results = remote_control.run_command("tar -xvf mailpkg.tar")
    # print("Results from untaring mailpkg.tar <%s>" % results)

def sendPhishMail(mailfrom="test", host=smtpServerHost, useTLS=False):
    mailResult = None
    if useTLS:
        mailResult = remote_control.run_command("python mailsender.py --from=" + mailfrom + "@test.untangle.com --to=qa@test.untangle.com ./phish-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180 --starttls", stdout=False, nowait=False)
    else:
        mailResult = remote_control.run_command("python mailsender.py --from=" + mailfrom + "@test.untangle.com --to=qa@test.untangle.com ./phish-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180")
    return mailResult

@pytest.mark.phish_blocker
class PhishBlockerTests(NGFWTestCase):

    @staticmethod
    def module_name():
        global app
        app = PhishBlockerTests._app
        return "phish-blocker"

    @staticmethod
    def vendorName():
        return "untangle"

    @staticmethod
    def appNameSpamCase():
        return "smtp"

    @staticmethod
    def appNameSSLInspector():
        return "ssl-inspector"

    @classmethod
    def initial_extra_setup(cls):
        # FIXME: same as SpamBlockerBaseTests

        global appData, appSP, appDataSP, appSSL, canRelay

        appData = cls._app.getSettings()
        appSP = uvmContext.appManager().app(cls.appNameSpamCase())
        appDataSP = appSP.getSmtpSettings()
        if uvmContext.appManager().isInstantiated(cls.appNameSSLInspector()):
            raise Exception('app %s already instantiated' % cls.appNameSSLInspector())
        appSSL = uvmContext.appManager().instantiate(cls.appNameSSLInspector(), default_policy_id)
        # appSSL.start() # leave app off. app doesn't auto-start
        try:
            canRelay = global_functions.send_test_email(mailhost=smtpServerHost)
        except Exception as e:
            canRelay = False
        getLatestMailSender()
        
        # flush quarantine.
        curQuarantine = appSP.getQuarantineMaintenenceView()
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            if checkAddress['address']:
                curQuarantine.deleteInbox(checkAddress['address'])
            
    # verify daemon is running
    def test_009_clamdIsRunning(self):
        # wait for freshclam to finish updating sigs
        freshClamResult = subprocess.call("freshclam >/dev/null 2>&1", shell=True)
        # wait for clam to get ready - trying to fix occasional failure of later tests
        timeout = 180
        result = 1
        while (result and timeout > 0):
            time.sleep(5)
            timeout -= 5
            result = subprocess.call("pidof clamd >/dev/null 2>&1", shell=True)
        assert (result == 0)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_smtpQuarantinedPhishBlockerTest(self):
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + smtpServerHost)
        if (not mail_app):
            raise unittest.SkipTest('Unable download mailsendder app')
        pre_events_quarantine = global_functions.get_app_metric_value(app,"quarantine")

        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 5
        app.setSettings(appData)
        # Get the IP address of test.untangle.com
        result = remote_control.run_command("host "+smtpServerHost, stdout=True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = match.group()

        # sometimes the load is very high >7 and sending mail will fail
        # sleep for a while for the load to go down
        try:
            if float(file("/proc/loadavg","r").readline().split(" ")[0]) > 3:
                time.sleep(30)
        except:
            pass
                
        timeout = 12
        found = False
        email_index = 20;
        while (not found and timeout > 0):
            time.sleep(10)
            email_index += 1
            from_address = "test0" + str(email_index)
            sendPhishMail(mailfrom=from_address)

            events = global_functions.get_events('Phish Blocker','All Phish Events',None,5)
            assert(events != None)
            # print(events['list'][0])
            found = global_functions.check_events( events.get('list'), 5,
                                                'c_server_addr', ip_address_testuntangle,
                                                's_server_port', 25,
                                                'addr', 'qa@test.untangle.com',
                                                'c_client_addr', remote_control.client_ip,
                                                'phish_blocker_action', 'Q')
            timeout -= 1
            
        assert( found )
            
        # Check to see if the faceplate counters have incremented. 
        post_events_quarantine = global_functions.get_app_metric_value(app,"quarantine")
        assert(pre_events_quarantine < post_events_quarantine)
        
    def test_030_smtpMarkPhishBlockerTest(self):
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + smtpServerHost)
        if (not mail_app):
            raise unittest.SkipTest('Unable download mailsendder app')
            
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 5
        appData['smtpConfig']['msgAction'] = "MARK"
        app.setSettings(appData)
        # Get the IP address of test.untangle.com
        ip_address_testuntangle = socket.gethostbyname(smtpServerHost)

        timeout = 12
        found = False
        email_index = 20;
        while (not found and timeout > 0):
            time.sleep(3)
            email_index += 1
            from_address = "test0" + str(email_index)
            sendPhishMail(mailfrom=from_address)

            events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                'c_server_addr', ip_address_testuntangle,
                                                's_server_port', 25,
                                                'addr', 'qa@test.untangle.com',
                                                'c_client_addr', remote_control.client_ip,
                                                'phish_blocker_action', 'M')
            timeout -= 1

        assert( found )

    def test_040_smtpDropPhishBlockerTest(self):
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through' + smtpServerHost)
        if (not mail_app):
            raise unittest.SkipTest('Unable download mailsendder app')
            
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 5
        appData['smtpConfig']['msgAction'] = "DROP"
        app.setSettings(appData)
        # Get the IP address of test.untangle.com
        ip_address_testuntangle = socket.gethostbyname(smtpServerHost)
        sendPhishMail(mailfrom="test040")

        events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@test.untangle.com',
                                            'c_client_addr', remote_control.client_ip,
                                            'phish_blocker_action', 'D')
        assert( found )

    def test_050_checkTLSBypass(self):
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + smtpServerHost)
        if (not mail_app):
            raise unittest.SkipTest('Unable download mailsendder app')
            
        tlsSMTPResult = sendPhishMail(host=smtpServerHost, useTLS=True)
        # print("TLS  : " + str(tlsSMTPResult))
        assert(tlsSMTPResult == 0)
       
    def test_060_checkTLSwSSLInspector(self):
        global appSSL
        if (not canRelay):
            raise unittest.SkipTest('Unable to relay through ' + smtpServerHost)
        if (not mail_app):
            raise unittest.SkipTest('Unable download mailsendder app')
            
        ip_address_testuntangle = socket.gethostbyname(smtpServerHost)
        appSSL.start()
        tlsSMTPResult = sendPhishMail(mailfrom="test060", host=smtpServerHost, useTLS=True)
        # print("TLS  : " + str(tlsSMTPResult))
        appSSL.stop()
        assert(tlsSMTPResult == 0)

        events = global_functions.get_events('Phish Blocker','All Phish Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', ip_address_testuntangle,
                                            's_server_port', 25,
                                            'addr', 'qa@test.untangle.com',
                                            'c_client_addr', remote_control.client_ip,
                                            'phish_blocker_action', 'D')
    
    @classmethod
    def final_extra_tear_down(cls):
        global appSSL
        if appSSL != None:
            uvmContext.appManager().destroy( appSSL.getAppSettings()["id"] )
            appSSL = None

test_registry.register_module("phish-blocker", PhishBlockerTests)
