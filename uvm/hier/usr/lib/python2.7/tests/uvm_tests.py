import socket
import unittest2
import os
import subprocess
import sys
import re
import urllib2
import time
import copy
reload(sys)
sys.setdefaultencoding("utf-8")
import re
import subprocess
import ipaddr
import time
import ssl

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import test_registry
import remote_control
import global_functions

node = None
nodeFW = None

defaultRackId = 1
origMailsettings = None
test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

def getLatestMailPkg():
    remote_control.run_command("rm -f mailpkg.tar*") # remove all previous mail packages
    results = remote_control.run_command("wget -q -t 1 --timeout=3 http://test.untangle.com/test/mailpkg.tar")
    # print "Results from getting mailpkg.tar <%s>" % results
    results = remote_control.run_command("tar -xvf mailpkg.tar")
    # print "Results from untaring mailpkg.tar <%s>" % results

class UvmTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "uvm"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def nodeNameSpamCase():
        return "untangle-casing-smtp"

    @staticmethod
    def initialSetUp(self):
        pass

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_helpLinks(self):
        output, error = subprocess.Popen(['find',
                                          '%s/usr/share/untangle/web/webui/script/' % global_functions.get_prefix(),
                                          '-name',
                                          '*.js',
                                          '-type',
                                          'f'], stdout=subprocess.PIPE).communicate()
        assert(output)
        for line in output.splitlines():
            print "Checking file %s..." % line
            assert (line)
            if line == "":
                continue

            hdr = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11',
                   'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                   'Accept-Charset': 'ISO-8859-1,utf-8;q=0.7,*;q=0.3',
                   'Accept-Encoding': 'none',
                   'Accept-Language': 'en-US,en;q=0.8',
                   'Connection': 'keep-alive'}

            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE

            webUiFile = open( line )
            assert( webUiFile )
            pat  = re.compile(r'''^.*helpSource:\s*['"]+([a-zA-Z_]*)['"]+[\s,]*$''')
            pat2 = re.compile(r'''.*URL=http://wiki.*.untangle.com/(.*)">.*$''')
            for line in webUiFile.readlines():
                match = pat.match(line)
                if match != None:
                    helpSource = match.group(1)
                    assert(helpSource)

                    url = "http://wiki.untangle.com/get.php?source=" + helpSource + "&uid=0000-0000-0000-0000&version=11.0.0&webui=true&lang=en"
                    print "Checking %s = %s " % (helpSource, url)
                    req = urllib2.Request( url, headers=hdr) 
                    ret = urllib2.urlopen( req, context=ctx )
                    time.sleep(.1) # dont flood wiki
                    assert(ret)
                    result = ret.read()
                    assert(result)
                    match2 = pat2.match( result )
                    assert(match2)
                    # Check that it redirects somewhere other than /
                    print "Result: \"%s\"" % match2.group(1)
                    assert(match2.group(1))

        assert(True)

    def test_020_aboutInfo(self):
        uid =  uvmContext.getServerUID()
        match = re.search(r'\w{4}-\w{4}-\w{4}.\w{4}', uid)
        assert( match )

        kernel = uvmContext.adminManager().getKernelVersion()
        match = re.search(r'\d.*', kernel)
        assert(match)

        reboot_count = uvmContext.adminManager().getRebootCount()
        match = re.search(r'\d{1,2}', reboot_count)
        assert(match)

        num_hosts = str(uvmContext.hostTable().getCurrentActiveSize())
        match = re.search(r'\d{1,2}', num_hosts)
        assert(match)

        max_num_hosts = str(uvmContext.hostTable().getMaxActiveSize())
        match = re.search(r'\d{1,2}', max_num_hosts)
        assert(match)

    def test_030_testSMTPSettings(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        # Test mail setting in config -> email -> outgoing server
        if (uvmContext.nodeManager().isInstantiated(self.nodeNameSpamCase())):
            print "smtp case present"
        else:
            print "smtp not present"
            uvmContext.nodeManager().instantiate(self.nodeNameSpamCase(), 1)
        nodeSP = uvmContext.nodeManager().node(self.nodeNameSpamCase())
        origNodeDataSP = nodeSP.getSmtpNodeSettings()
        origMailsettings = uvmContext.mailSender().getSettings()
        # print nodeDataSP
        getLatestMailPkg();
        # remove previous smtp log file
        remote_control.run_command("rm -f /tmp/test_030_testSMTPSettings.log /tmp/test@example.com.1")
        # Start mail sink
        remote_control.run_command("python fakemail.py --host=" + remote_control.clientIP +" --log=/tmp/test_030_testSMTPSettings.log --port 6800 --background --path=/tmp/", stdout=False, nowait=True)
        newMailsettings = copy.deepcopy(origMailsettings)
        newMailsettings['smtpHost'] = remote_control.clientIP
        newMailsettings['smtpPort'] = "6800"
        newMailsettings['sendMethod'] = 'CUSTOM'

        uvmContext.mailSender().setSettings(newMailsettings)
        time.sleep(10) # give it time for exim to restart

        nodeDataSP = nodeSP.getSmtpNodeSettings()
        nodeSP.setSmtpNodeSettingsWithoutSafelists(nodeDataSP)
        uvmContext.mailSender().sendTestMessage("test@example.com")
        time.sleep(2)
        # force exim to flush queue
        subprocess.call(["exim -qff >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)
        time.sleep(10)

        # Kill mail sink
        remote_control.run_command("pkill -INT python")
        uvmContext.mailSender().setSettings(origMailsettings)
        nodeSP.setSmtpNodeSettingsWithoutSafelists(origNodeDataSP)
        result = remote_control.run_command("grep -q 'Untangle Server Test Message' /tmp/test@example.com.1")
        assert(result==0)

    def test_040_account_login(self):
        untangleEmail, untanglePassword = global_functions.get_live_account_info("Untangle")
        if untangleEmail == "message":
            raise unittest2.SkipTest('Skipping no accound found:' + str(untanglePassword))

        result = uvmContext.cloudManager().accountLogin( untangleEmail, untanglePassword )
        assert result.get('success')

    def test_041_account_login_invalid(self):
        result = uvmContext.cloudManager().accountLogin( "foobar@untangle.com", "badpassword" )
        assert not result.get('success')

test_registry.registerNode("uvm", UvmTests)
