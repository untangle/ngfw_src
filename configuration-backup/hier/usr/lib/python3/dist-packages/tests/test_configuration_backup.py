"""configuration_backup tests"""
import os
import subprocess
import ast
import filecmp
import glob
import pytest
import requests
import re
import datetime
import json

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions


app = None
default_policy_id = 1


@pytest.mark.configuration_backup
class ConfigurationBackupTests(NGFWTestCase):
    
    @staticmethod
    def module_name():
        global app
        app = ConfigurationBackupTests._app
        return "configuration-backup"

    @staticmethod
    def vendorName():
        return "Untangle"

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_backupNow(self):
        self._app.sendBackup()

        events = global_functions.get_events('Configuration Backup','Backup Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               'success', True ) 
        assert( found )

    def test_030_verifyBackupCronjob(self):
        assert( os.path.isfile("/etc/cron.d/untangle-configuration-backup-nightly")  )

    def test_140_compare_cloud_backup(self):
        """Compare a cloud backup with a local backup"""
        
        boxUID = uvmContext.getServerUID()
        #get authentication url and api key
        authUrl,authKey = global_functions.get_live_account_info("UntangleAuth")
        boxBackupUrl,bbKey = global_functions.get_live_account_info("BoxBackup")
        self._app.sendBackup()
        #remove previous backups/backup directories
        subprocess.call("rm -rf /tmp/localBackup*", shell=True)
        subprocess.call("rm -rf /tmp/cloudBackup*", shell=True)

        #download local backup
        subprocess.call(global_functions.build_wget_command(log_file="/dev/null", output_file="/tmp/localBackup.backup", post_data="type=backup", uri="http://localhost/admin/download"), shell=True)
        #extract backup
        subprocess.call("mkdir /tmp/localBackup", shell=True)
        subprocess.call("tar -xf /tmp/localBackup.backup -C /tmp/localBackup", shell=True)
        subprocess.call("tar -xf "+glob.glob("/tmp/localBackup/files*.tar.gz")[0] + " -C /tmp/localBackup", shell=True) #use glob since extracted file has timestamp
        localBackupPath = "/tmp/localBackup/usr"

        #set Token for boxbackup access
        authenticationUrl = authUrl
        authPayload = "{\n  \"token\": 123,\n  \"resourceIds\": [\"%s\"],\n  \"timeoutOverride\": \"5\"\n}" % (boxUID)
        authHeaders = {
            'Content-Type': "application/json",
            'AuthRequest': authKey,
            'Cache-Control': "no-cache"
            }
        requests.request("POST", authenticationUrl, data=authPayload, headers=authHeaders)
        
        #get list of backups for the UID above from boxbackup
        bbUrl = boxBackupUrl
        bbQueryString = {"action":"list","uid":boxUID,"token":"123"}
        bbHeaders = {'Cache-Control': 'no-cache'}
        bbResponse = requests.request("GET", bbUrl, headers=bbHeaders, params=bbQueryString)

        #convert response JSON to literal list
        backupList = json.loads(bbResponse.text)
        # grab the latest cloud backup from the list
        backupListSorted = sorted(backupList, key=lambda x: x["Timestamp"])
        latestBackup = backupListSorted[-1]
        #print("latest backup from cloud: %s" % latestBackup)

        #download the latest backup and save it to /tmp
        dlUrl = boxBackupUrl
        dlQueryString = {"action":"get","uid":boxUID,"token":"123","filename":latestBackup["Name"]}
        dlHeaders = {'Cache-Control': 'no-cache'}
        dlResponse = requests.request("GET", dlUrl, headers=dlHeaders, params=dlQueryString)
        with open("/tmp/cloudBackup.backup", "wb") as f:
            f.write(dlResponse.content)
        #extract cloud backup
        subprocess.call("mkdir /tmp/cloudBackup", shell=True)
        subprocess.call("tar -xf /tmp/cloudBackup.backup -C /tmp/cloudBackup", shell=True)
        subprocess.call("tar -xf "+glob.glob("/tmp/cloudBackup/files*.tar.gz")[0] + " -C /tmp/cloudBackup", shell=True) #use glob since extracted file has timestamp
        cloudBackupPath = "/tmp/cloudBackup/usr"
        
        #compare directories
        def is_same(dir1, dir2):
            compared = filecmp.dircmp(dir1, dir2)
            if (compared.left_only or compared.right_only or compared.diff_files 
                or compared.funny_files):
                return False
            for subdir in compared.common_dirs:
                if not is_same(os.path.join(dir1, subdir), os.path.join(dir2, subdir)):
                    return False
            return True
    
        assert(is_same(localBackupPath, cloudBackupPath))


test_registry.register_module("configuration-backup", ConfigurationBackupTests)
