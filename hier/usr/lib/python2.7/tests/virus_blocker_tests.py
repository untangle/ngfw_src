import unittest2
import time
import sys
import os
import platform
import subprocess
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
from tests.virus_blocker_base_tests import VirusBlockerBaseTests
import remote_control
import test_registry
import global_functions

# we have three special files with known MD5's that we use for testing the cloud scanner and memory mode
md5SmallVirus = "0f14ddcbb42bd6a8af5b820a4f52572b"
md5LargeVirus = "e223ff196471639c8cc4b8d3d1d444a9"
md5LargeClean = "b3215c06647bc550406a9c8ccc378756"

#
# Just extends the virus base tests
#
class VirusBlockTests(VirusBlockerBaseTests):

    @staticmethod
    def nodeName():
        return "untangle-node-virus-blocker"

    @staticmethod
    def shortName():
        return "virus_blocker"

    @staticmethod
    def displayName():
        return "Virus Blocker"

    # on every platform except ARM verify the bit defender daemon is running
    def test_009_bdamserverIsRunning(self):
        if platform.machine().startswith('arm'):
            return

        # check that server is running
        time.sleep(1)
        result = os.system("pidof bdamserver >/dev/null 2>&1")
        assert ( result == 0 )

        # give it up to 20 minutes to download signatures for the first time
        print "Waiting for server to start..."
        for i in xrange(1200):
            time.sleep(1)
            result = os.system("cat /var/log/bdamserver.log | grep -q 'Server is started' >/dev/null 2>&1")
            if result == 0:
                break
        print "Number of sleep cycles waiting for bdamserver %d" % i

        # do a scan - this forces it to wait until the signatures are done downloading
        result = os.system("touch /tmp/bdamtest ; bdamclient -p 127.0.0.1:1344 /tmp/bdamtest >/dev/null 2>&1")
        assert (result == 0)

#
# All of the tests below this point are run using memory mode instead of file mode scanning.
# We do this by setting the hidden memoryMode flag which forces the app to operate as it would
# on a system with no disk.  In this mode, only the file MD5 hash is checked with the cloud scanner.
#

    # turn on forceMemoryMode to test the logic used when we have no disk (i.e., Asus ARM Router)
    def test_210_enableForceMemoryScanMode(self):
        virusSettings = self.node.getSettings()
        assert (virusSettings['forceMemoryMode'] == False)

        virusSettings['forceMemoryMode'] = True
        self.node.setSettings(virusSettings)

        virusSettings = self.node.getSettings()
        assert (virusSettings['forceMemoryMode'] == True)

    # clear anything cached to force files to be downloaded again
    def test_220_clearEventHandlerCache(self):
        self.node.clearAllEventHandlerCaches()

    # test the cloud scanner with http using our special small test virus
    def test_230_httpCloudSmallBlocked(self):
        remote_control.runCommand("rm -f /tmp/temp_230_httpVirusBlocked_file")
        result = remote_control.runCommand("wget -q -O /tmp/temp_230_httpVirusBlocked_file http://test.untangle.com/test/UntangleVirus.exe")
        assert (result == 0)
        md5TestNum = remote_control.runCommand("\"md5sum /tmp/temp_230_httpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
        print "md5SmallVirus <%s> vs md5TestNum <%s>" % (md5SmallVirus, md5TestNum)
        assert (md5SmallVirus != md5TestNum)

    # test the cloud scanner with http using our special large test virus
    def test_240_httpCloudLargeBlocked(self):
        remote_control.runCommand("rm -f /tmp/temp_240_httpVirusBlocked_file")
        result = remote_control.runCommand("wget -q -O /tmp/temp_240_httpVirusBlocked_file http://test.untangle.com/test/UntangleLargeVirus.exe")
        assert (result == 0)
        md5TestNum = remote_control.runCommand("\"md5sum /tmp/temp_240_httpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
        print "md5LargeVirus <%s> vs md5TestNum <%s>" % (md5LargeVirus, md5TestNum)
        assert (md5LargeVirus != md5TestNum)

    # test the cloud scanner with ftp using our special small test virus
    def test_250_ftpCloudSmallBlocked(self):
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftpServer ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest2.SkipTest("FTP server not available")
        remote_control.runCommand("rm -f /tmp/temp_250_ftpVirusBlocked_file")
        result = remote_control.runCommand("wget -q -O /tmp/temp_250_ftpVirusBlocked_file ftp://" + global_functions.ftpServer + "/test/UntangleVirus.exe")
        assert (result == 0)
        md5TestNum = remote_control.runCommand("\"md5sum /tmp/temp_250_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
        print "md5SmallVirus <%s> vs md5TestNum <%s>" % (md5SmallVirus, md5TestNum)
        assert (md5SmallVirus != md5TestNum)

    # test the cloud scanner with ftp using our special large test virus
    def test_260_ftpCloudLargeBlocked(self):
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftpServer ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest2.SkipTest("FTP server not available")
        remote_control.runCommand("rm -f /tmp/temp_260_ftpVirusBlocked_file")
        result = remote_control.runCommand("wget -q -O /tmp/temp_260_ftpVirusBlocked_file ftp://" + global_functions.ftpServer + "/test/UntangleLargeVirus.exe")
        assert (result == 0)
        md5TestNum = remote_control.runCommand("\"md5sum /tmp/temp_260_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
        print "md5LargeVirus <%s> vs md5TestNum <%s>" % (md5LargeVirus, md5TestNum)
        assert (md5LargeVirus != md5TestNum)

    # test the cloud scanner with http using our special large clean file
    def test_270_httpMemoryLargeClean(self):
        remote_control.runCommand("rm -f /tmp/temp_270_httpMemoryClean_file")
        result = remote_control.runCommand("wget -q -O /tmp/temp_270_httpMemoryClean_file http://test.untangle.com/5MB.zip")
        assert (result == 0)
        md5TestNum = remote_control.runCommand("\"md5sum /tmp/temp_270_httpMemoryClean_file | awk '{print $1}'\"", stdout=True)
        print "md5LargeClean <%s> vs md5TestNum <%s>" % (md5LargeClean, md5TestNum)
        assert (md5LargeClean == md5TestNum)

    # turn off forceMemoryMode when we are finished
    def test_280_disableForceMemoryScanMode(self):
        virusSettings = self.node.getSettings()
        assert (virusSettings['forceMemoryMode'] == True)

        virusSettings['forceMemoryMode'] = False
        self.node.setSettings(virusSettings)

        virusSettings = self.node.getSettings()
        assert (virusSettings['forceMemoryMode'] == False)

test_registry.registerNode("virus-blocker", VirusBlockTests)
