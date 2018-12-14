import unittest2
import time
import subprocess
import sys
import os
import socket
import re
import global_functions
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import ipaddr

default_policy_id = 1
app = None
appData = None
appSSL = None
appSSLData = None
canRelay = True
smtpServerHost = global_functions.TEST_SERVER_HOST

def getLatestMailSender():
    remote_control.run_command("rm -f mailpkg.tar") # remove all previous mail packages
    results = remote_control.run_command("wget -q http://test.untangle.com/test/mailpkg.tar")
    # print("Results from getting mailpkg.tar <%s>" % results)
    results = remote_control.run_command("tar -xvf mailpkg.tar")
    # print("Results from untaring mailpkg.tar <%s>" % results)

def sendSpamMail(host=smtpServerHost, useTLS=False):
    mailResult = None
    randomAddress = global_functions.random_email()
    print("randomAddress: " + randomAddress)
    if useTLS:
        mailResult = remote_control.run_command("python mailsender.py --from=atstest@test.untangle.com --to=" + randomAddress + " ./spam-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180 --starttls", stdout=False, nowait=False)
    else:
        mailResult = remote_control.run_command("python mailsender.py --from=atstest@test.untangle.com --to=" + randomAddress + " ./spam-mail/ --host=" + host + " --reconnect --series=30:0,150,100,50,25,0,180")
    return mailResult, randomAddress

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
    };

class SpamBlockerBaseTests(unittest2.TestCase):

    @staticmethod
    def shortName():
        return "untangle"

    @staticmethod
    def appNameSpamCase():
        return "smtp"

    @staticmethod
    def appNameSSLInspector():
        return "ssl-inspector"

    @staticmethod
    def initial_setup(self):
        global app, appData, appSP, appDataSP, appSSL, appSSLData, canRelay
        if (uvmContext.appManager().isInstantiated(self.appName())):
            raise unittest2.SkipTest('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), default_policy_id)
        appData = app.getSettings()
        appSP = uvmContext.appManager().app(self.appNameSpamCase())
        appDataSP = appSP.getSmtpSettings()
        if uvmContext.appManager().isInstantiated(self.appNameSSLInspector()):
            raise Exception('app %s already instantiated' % self.appNameSSLInspector())
        appSSL = uvmContext.appManager().instantiate(self.appNameSSLInspector(), default_policy_id)
        # appSSL.start() # leave app off. app doesn't auto-start
        appSSLData = appSSL.getSettings()
        try:
            canRelay = global_functions.send_test_email(mailhost=smtpServerHost)
        except Exception,e:
            canRelay = False
        self.canRelay = canRelay
        getLatestMailSender()
        # flush quarantine.
        curQuarantine = appSP.getQuarantineMaintenenceView()
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            if checkAddress['address']:
                curQuarantine.deleteInbox(checkAddress['address'])

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.appName()))

    def test_020_smtpTest(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 30
        app.setSettings(appData)
        # Get the IP address of test.untangle.com
        test_untangle_IP = socket.gethostbyname(smtpServerHost)

        result, to_address = sendSpamMail()
        
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
        assert( events != None )
        assert( events.get('list') != None )

        print(events['list'][0])
        found = global_functions.check_events( events.get('list'), 10,
                                               's_server_addr', test_untangle_IP,
                                               's_server_port', 25,
                                               'addr', to_address,
                                               'c_client_addr', remote_control.clientIP)
        assert( found ) 

    def test_030_adminQuarantine(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 30
        app.setSettings(appData)
        result, to_address = sendSpamMail()
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
       
        if (events == None):
            raise unittest2.SkipTest('Unable to run admin quarantine since there are no quarantine events')
        
        # Get adminstrative quarantine list of email addresses
        addressFound = False
        curQuarantine = appSP.getQuarantineMaintenenceView()
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            print(checkAddress)
            if (checkAddress['address'] == to_address) and (checkAddress['totalMails'] > 0): addressFound = True
        assert(addressFound)

    def test_040_userQuarantine(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 30
        app.setSettings(appData)
        result, to_address = sendSpamMail()
        # Get user quarantine list of email addresses
        addressFound = False
        curQuarantine = appSP.getQuarantineUserView()
        curQuarantineList = curQuarantine.getInboxRecords(to_address)
        #print(curQuarantineList)
        assert(len(curQuarantineList['list']) > 0)

    def test_050_userQuarantinePurge(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 30
        app.setSettings(appData)
        result, to_address = sendSpamMail()
        # Get user quarantine list of email addresses
        addressFound = False
        curQuarantine = appSP.getQuarantineUserView()

        curQuarantineList = curQuarantine.getInboxRecords(to_address)
        initialLen = len(curQuarantineList['list'])
        mailId = curQuarantineList['list'][0]['mailID'];
        print(mailId)
        curQuarantine.purge(to_address, [mailId]);

        curQuarantineListAfter = curQuarantine.getInboxRecords(to_address)
        assert(len(curQuarantineListAfter['list']) == initialLen - 1);

    def test_060_adminQuarantineDeleteAccount(self):
        # Get adminstrative quarantine list of email addresses
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        # send some spam
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['strength'] = 30
        app.setSettings(appData)
        result, to_address = sendSpamMail()
        addressFound = False
        curQuarantine = appSP.getQuarantineMaintenenceView()
        curQuarantine.deleteInbox(to_address)
        curQuarantineList = curQuarantine.listInboxes()
        for checkAddress in curQuarantineList['list']:
            if (checkAddress['address'] == to_address) and (checkAddress['totalMails'] > 0): addressFound = True
        assert(not addressFound)

    def test_070_checkForSMTPHeaders(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)

        appData['smtpConfig']['blockSuperSpam'] = False
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['addSpamHeaders'] = True
        appData['smtpConfig']['msgAction'] = "MARK"
        app.setSettings(appData)

        result, to_address = sendSpamMail()
        # check for email file if there is no timeout
        emailFound = False
        timeout = 5
        emailContext = ""
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # Check to see if the delivered email file is present
            emailContext = remote_control.run_command("wget -q --timeout=5 -O - http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + to_address + " 2>&1 | grep spam-status" ,stdout=True)
            if (emailContext != ""):
                emailFound = True
                
        # Either found email file or timed out so kill mail sink
        appData['smtpConfig']['msgAction'] = "QUARANTINE"
        app.setSettings(appData)
        assert (timeout != 0)
        # look for added header in delivered email
        lines = emailContext.split("\n")
        spamScore = 0
        requiredScore = 0

        # some dev boxes score this < 0, so don't check the score
        # just check that the headers were added
        for line in lines:
            if 'X-spam-status' in line:
                # print(line)
                match = re.search(r'\sscore\=([0-9.-]+)\srequired\=([0-9.]+) ', line)
                spamScore =  match.group(1)
                requiredScore =  match.group(2)
                print("spamScore: " + spamScore + " requiredScore: " + requiredScore)
                return
        assert False 
        # assert(float(spamScore) > 0)
        # assert(float(requiredScore) > 0)
        # assert(float(requiredScore) > float(spamScore))

    def test_080_checkAllowTLS(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        appData['smtpConfig']['scanWanMail'] = True
        app.setSettings(appData)
        # Make sure SSL Inspector is off
        appSSL.stop()
        tlsSMTPResult, to_address = sendSpamMail(useTLS=True)
        # print("TLS 1 : " + str(tlsSMTPResult))
        assert(tlsSMTPResult != 0)
        appData['smtpConfig']['allowTls'] = True
        app.setSettings(appData)
        tlsSMTPResult, to_address = sendSpamMail(host=global_functions.TEST_SERVER_HOST, useTLS=True)
        # print("TLS 2 : " + str(tlsSMTPResult))
        assert(tlsSMTPResult == 0)
        
    
    def test_090_checkTLSwSSLInspector(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + smtpServerHost)
        test_untangle_IP = socket.gethostbyname(smtpServerHost)
        appData['smtpConfig']['scanWanMail'] = True
        appData['smtpConfig']['allowTls'] = False
        appData['smtpConfig']['strength'] = 30
        app.setSettings(appData)
        # Turn on SSL Inspector
        appSSLData['processEncryptedMailTraffic'] = True
        appSSLData['ignoreRules']['list'].insert(0,createSSLInspectRule("25"))
        appSSL.setSettings(appSSLData)
        appSSL.start()
        tlsSMTPResult, to_address = sendSpamMail(useTLS=True)
        # print("TLS 090 : " + str(tlsSMTPResult))
        appSSL.stop()
        assert(tlsSMTPResult == 0)
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,1)
        assert( events != None )
        assert( events.get('list') != None )

        print(events['list'][0])
        found = global_functions.check_events( events.get('list'), 5,
                                               's_server_addr', test_untangle_IP,
                                               's_server_port', 25,
                                               'addr', to_address,
                                               'c_client_addr', remote_control.clientIP)
        assert( found ) 
            
    @staticmethod
    def final_tear_down(self):
        global app,appSSL
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if appSSL != None:
            uvmContext.appManager().destroy( appSSL.getAppSettings()["id"] )
            appSSL = None
