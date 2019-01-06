"""virus_blocker tests"""
import time
import platform
import subprocess

import unittest2
from tests.global_functions import uvmContext
import tests.remote_control as remote_control
import tests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

from tests.virus_blocker_base_tests import VirusBlockerBaseTests

# we have three special files with known MD5's that we use for testing the cloud scanner and memory mode
md5SmallVirus = "0f14ddcbb42bd6a8af5b820a4f52572b"
md5LargeVirus = "e223ff196471639c8cc4b8d3d1d444a9"
md5LargeClean = "b3215c06647bc550406a9c8ccc378756"

#
# Just extends the virus base tests
#
class VirusBlockTests(VirusBlockerBaseTests):

    @staticmethod
    def module_name():
        return "virus-blocker"

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
        result = subprocess.call("pidof bdamserver >/dev/null 2>&1", shell=True)
        assert ( result == 0 )

        # give it up to 20 minutes to download signatures for the first time
        print("Waiting for server to start...")
        for i in xrange(1200):
            time.sleep(1)
            result = subprocess.call("netcat -n -z 127.0.0.1 1344 >/dev/null 2>&1", shell=True)
            if result == 0:
                break
        print("Number of sleep cycles waiting for bdamserver %d" % i)

        # do a scan - this forces it to wait until the signatures are done downloading
        result = subprocess.call("touch /tmp/bdamtest ; bdamclient -p 127.0.0.1:1344 /tmp/bdamtest >/dev/null 2>&1", shell=True)
        assert (result == 0)

#
# All of the tests below this point are run using memory mode instead of file mode scanning.
# We do this by setting the hidden memoryMode flag which forces the app to operate as it would
# on a system with no disk.  In this mode, only the file MD5 hash is checked with the cloud scanner.
#

    # turn on forceMemoryMode to test the logic used when we have no disk (i.e., Asus ARM Router)
    def test_210_enableForceMemoryScanMode(self):
        virusSettings = self.app.getSettings()
        assert (virusSettings['forceMemoryMode'] == False)

        virusSettings['forceMemoryMode'] = True
        self.app.setSettings(virusSettings)

        virusSettings = self.app.getSettings()
        assert (virusSettings['forceMemoryMode'] == True)

    # clear anything cached to force files to be downloaded again
    def test_220_clearEventHandlerCache(self):
        self.app.clearAllEventHandlerCaches()

    # test the cloud scanner with http using our special small test virus
    def test_230_httpCloudSmallBlocked(self):
        md5TestNum = ""
        counter = 5
        # loop since the connection can fail to return a result.
        while (md5SmallVirus == md5TestNum) and (counter > 0):
            counter -= 1
            remote_control.run_command("rm -f /tmp/temp_230_httpVirusBlocked_file")
            result = remote_control.run_command("wget -q -O /tmp/temp_230_httpVirusBlocked_file http://test.untangle.com/test/UntangleVirus.exe")
            assert (result == 0)
            md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_230_httpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
            print("md5SmallVirus <%s> vs md5TestNum <%s>" % (md5SmallVirus, md5TestNum))
        assert (md5SmallVirus != md5TestNum)

    # test the cloud scanner with http using our special large test virus
    def test_240_httpCloudLargeBlocked(self):
        md5TestNum = ""
        counter = 5
        # loop since the connection can fail to return a result.
        while (md5LargeVirus == md5TestNum) and (counter > 0):
            counter -= 1
            remote_control.run_command("rm -f /tmp/temp_240_httpVirusBlocked_file")
            result = remote_control.run_command("wget -q -O /tmp/temp_240_httpVirusBlocked_file http://test.untangle.com/test/UntangleLargeVirus.exe")
            assert (result == 0)
            md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_240_httpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
            print("md5LargeVirus <%s> vs md5TestNum <%s>" % (md5LargeVirus, md5TestNum))
        assert (md5LargeVirus != md5TestNum)

    # test the cloud scanner with ftp using our special small test virus
    def test_250_ftpCloudSmallBlocked(self):
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest2.SkipTest("FTP server not available")
        md5TestNum = ""
        counter = 5
        # loop since the connection can fail to return a result.
        while (md5SmallVirus == md5TestNum) and (counter > 0):
            counter -= 1
            remote_control.run_command("rm -f /tmp/temp_250_ftpVirusBlocked_file")
            result = remote_control.run_command("wget --user=" + self.ftp_user_name + " --password='" + self.ftp_password + "' -q -O /tmp/temp_250_ftpVirusBlocked_file ftp://" + global_functions.ftp_server + "/test/UntangleVirus.exe")
            assert (result == 0)
            md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_250_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
            print("md5SmallVirus <%s> vs md5TestNum <%s>" % (md5SmallVirus, md5TestNum))
        assert (md5SmallVirus != md5TestNum)

    # test the cloud scanner with ftp using our special large test virus
    def test_260_ftpCloudLargeBlocked(self):
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest2.SkipTest("FTP server not available")
        md5TestNum = ""
        counter = 5
        # loop since the connection can fail to return a result.
        while (md5LargeVirus == md5TestNum) and (counter > 0):
            counter -= 1
            remote_control.run_command("rm -f /tmp/temp_260_ftpVirusBlocked_file")
            result = remote_control.run_command("wget --user=" + self.ftp_user_name + " --password='" + self.ftp_password + "' -q -O /tmp/temp_260_ftpVirusBlocked_file ftp://" + global_functions.ftp_server + "/test/UntangleLargeVirus.exe")
            assert (result == 0)
            md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_260_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
            print("md5LargeVirus <%s> vs md5TestNum <%s>" % (md5LargeVirus, md5TestNum))
        assert (md5LargeVirus != md5TestNum)

    # test the cloud scanner with http using our special large clean file
    def test_270_httpMemoryLargeClean(self):
        remote_control.run_command("rm -f /tmp/temp_270_httpMemoryClean_file")
        result = remote_control.run_command("wget -q -O /tmp/temp_270_httpMemoryClean_file http://test.untangle.com/5MB.zip")
        assert (result == 0)
        md5TestNum = remote_control.run_command("\"md5sum /tmp/temp_270_httpMemoryClean_file | awk '{print $1}'\"", stdout=True)
        print("md5LargeClean <%s> vs md5TestNum <%s>" % (md5LargeClean, md5TestNum))
        assert (md5LargeClean == md5TestNum)

    # turn off forceMemoryMode when we are finished
    def test_280_disableForceMemoryScanMode(self):
        virusSettings = self.app.getSettings()
        assert (virusSettings['forceMemoryMode'] == True)

        virusSettings['forceMemoryMode'] = False
        self.app.setSettings(virusSettings)

        virusSettings = self.app.getSettings()
        assert (virusSettings['forceMemoryMode'] == False)

test_registry.register_module("virus-blocker", VirusBlockTests)
