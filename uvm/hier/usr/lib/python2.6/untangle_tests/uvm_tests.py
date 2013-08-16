import socket
import unittest2
import os
import subprocess
import sys
import re
import urllib2
import time
reload(sys)
sys.setdefaultencoding("utf-8")
import re
import subprocess
import ipaddr
import system_props
import time
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import TestDict
from untangle_tests import ClientControl
from untangle_tests import SystemProperties

node = None
nodeFW = None

uvmContext = Uvm().getUvmContext()
systemProperties = SystemProperties()
clientControl = ClientControl()
defaultRackId = 1
orig_netsettings = None
test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

class UvmTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "uvm"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        # save original network settings
        global orig_netsettings
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        assert (result == 0)

    def test_011_helpLinks(self):
        output, error = subprocess.Popen(['find',
                                          '%s/usr/share/untangle/web/webui/script/'%systemProperties.getPrefix(),
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

            webUiFile = open( line )
            assert( webUiFile )
            pat  = re.compile(r'''^.*helpSource:\s*['"]*([a-zA-Z_]*)['"\s,]*$''')
            pat2 = re.compile(r'''.*URL=http://wiki.*.untangle.com/(.*)">.*$''')
            for line in webUiFile.readlines():
                match = pat.match(line)
                if match != None:
                    helpSource = match.group(1)
                    assert(helpSource)

                    url = "http://www.untangle.com/docs/get.php?source=" + helpSource + "&uid=0000-0000-0000-0000&version=10.0.0&webui=true&lang=en"
                    print "Checking %s = %s " % (helpSource, url)
                    ret = urllib2.urlopen( url )
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


TestDict.registerNode("uvm", UvmTests)
