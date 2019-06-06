"""directory_connector tests"""
import time
import string
import random
import subprocess
import requests

import unittest
import pytest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
from uvm import Uvm

default_policy_id = 1
app = None

AD_RESULT = 1
RADIUS_RESULT = 1

# pdb.set_trace()

def create_ad_settings(ldap_secure=False):
    """
    Create Active Directory settings
    Need to send Radius setting even though it's not used in this case.
    """
    if ldap_secure == True:
        ldap_port = 636
    else:
        ldap_port = 389
    return {
        "apiEnabled": True,
        "activeDirectorySettings": {
            "LDAPHost": global_functions.AD_SERVER,
            "LDAPSecure": ldap_secure,
            "LDAPPort": ldap_port,
            "OUFilter": "",
            "OUFilters": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "domain": global_functions.AD_DOMAIN,
            "javaClass": "com.untangle.app.directory_connector.ActiveDirectorySettings",
            "superuser": global_functions.AD_ADMIN,
            "superuserPass": global_functions.AD_PASSWORD,
            "enabled": True,
            "servers": {
                "javaClass": "java.util.LinkedList",
                "list": [{
                    "LDAPHost": global_functions.AD_SERVER,
                    "LDAPSecure": ldap_secure,
                    "LDAPPort": ldap_port,
                    "OUFilter": "",
                    "OUFilters": {
                        "javaClass": "java.util.LinkedList",
                        "list": []
                    },
                    "domain": global_functions.AD_DOMAIN,
                    "enabled": True,
                    "javaClass": "com.untangle.app.directory_connector.ActiveDirectoryServer",
                    "superuser": global_functions.AD_ADMIN,
                    "superuserPass": global_functions.AD_PASSWORD
                }]
            }
        },
        "radiusSettings": {
            "port": 1812,
            "enabled": False,
            "authenticationMethod": "PAP",
            "javaClass": "com.untangle.app.directory_connector.RadiusSettings",
            "server": global_functions.RADIUS_SERVER,
            "sharedSecret": "mysharedsecret"
        },
        "googleSettings": {
            "javaClass": "com.untangle.app.directory_connector.GoogleSettings",
            "authenticationEnabled": True
        }
    }

def create_radius_settings():
    """
    Create RADIUS settings
    Need to send Active Directory setting even though it's not used in this case.
    """
    return {
        "apiEnabled": True,
        "activeDirectorySettings": {
                    "LDAPHost": global_functions.AD_SERVER,
                    "LDAPSecure": True,
                    "LDAPPort": "636",
                    "OUFilter": "",
                    "OUFilters": {
                        "javaClass": "java.util.LinkedList",
                        "list": []
                    },
                    "domain": global_functions.AD_DOMAIN,
                    "enabled": True,
                    "javaClass": "com.untangle.app.directory_connector.ActiveDirectorySettings",
                    "superuser": global_functions.AD_ADMIN,
                    "superuserPass": global_functions.AD_PASSWORD,
            "servers": {
                "javaClass": "java.util.LinkedList",
                "list": [{
                    "LDAPHost": global_functions.AD_SERVER,
                    "LDAPSecure": True,
                    "LDAPPort": "636",
                    "OUFilter": "",
                    "OUFilters": {
                        "javaClass": "java.util.LinkedList",
                        "list": []
                    },
                    "domain": global_functions.AD_DOMAIN,
                    "enabled": True,
                    "javaClass": "com.untangle.app.directory_connector.ActiveDirectoryServer",
                    "superuser": global_functions.AD_ADMIN,
                    "superuserPass": global_functions.AD_PASSWORD
                }]
            }
        },
        "radiusSettings": {
            "port": 1812,
            "enabled": True,
            "authenticationMethod": "PAP",
            "javaClass": "com.untangle.app.directory_connector.RadiusSettings",
            "server": global_functions.RADIUS_SERVER,
            "sharedSecret": "chakas"
        },
        "googleSettings": {
            "javaClass": "com.untangle.app.directory_connector.GoogleSettings",
            "authenticationEnabled": True
        }
    }

def get_list_of_username_mapped():
    """
    Get list of mapped users
    """
    entries = uvmContext.hostTable().getHosts()['list']
    usernames = []
    for entry in entries:
        print(entry)
        if entry['usernameDirectoryConnector'] != None and entry['usernameDirectoryConnector'] != "":
            usernames.append(entry['usernameDirectoryConnector'])
    return usernames

def add_ad_settings(ldap_secure=False):
    """
    Add Active Directory Settings, with or without secure enabled
    """
    # test the settings before saving them.
    test_result_string = app.getActiveDirectoryManager().getStatusForSettings(create_ad_settings(ldap_secure)["activeDirectorySettings"]["servers"]["list"][0])
    print('AD test_result_string %s' % test_result_string)
    if ("PASS" in test_result_string['status']):
        # settings are good so save them
        app.setSettings(create_ad_settings())
        return 0
    else:
        # settings failed
        return 1

def add_radius_settings():
    """
    Add RADIUS settings
    """
    # test the settings before saving them.
    test_result_string = app.getRadiusManager().getRadiusStatusForSettings(create_radius_settings(), "normal", "passwd")
    print('RADIUS test_result_string %s' % test_result_string)
    if ("success" in test_result_string):
        # settings are good so save them
        app.setSettings(create_radius_settings())
        return 0
    else:
        # settings failed
        return 1

def register_username(http_admin_url, user):
    """
    Register user name
    """
    register_url = http_admin_url + "userapi/registration\?username=" + user + "\&domain=adtesting.int\&hostname=adtest2\&action=login"
    result = remote_control.run_command(("wget -q -O /dev/null " + register_url))
    return result

def register_username_old(http_admin_url, user):
    """
    Register old user name
    """
    register_url = http_admin_url + "adpb/registration\?username=" + user + "\&domain=adtesting.int\&hostname=adtest2\&action=login"
    result = remote_control.run_command(("wget -q -O /dev/null " + register_url))
    return result

def find_name_in_host_table (hostname='test'):
    """
    Find name in host table
    """
    #  Test for username in session
    found_test_session = False
    remote_control.run_command("nohup netcat -d -4 test.untangle.com 80 >/dev/null 2>&1", stdout=False, nowait=True)
    time.sleep(2) # since we launched netcat in background, give it a second to establish connection
    host_list = uvmContext.hostTable().getHosts()
    session_list = host_list['list']
    # find session generated with netcat in session table.
    for i in range(len(session_list)):
        print(session_list[i])
        # print("------------------------------")
        if (session_list[i]['address'] == remote_control.client_ip) and (session_list[i]['username'] == hostname):
            found_test_session = True
            break
    remote_control.run_command("pkill netcat")
    return found_test_session

@pytest.mark.directory_connector
class DirectoryConnectorTests(unittest.TestCase):
    """
    Directory connector tests
    """
    @staticmethod
    def module_name():
        """
        App name
        """
        return "directory-connector"

    @staticmethod
    def vendorName():
        """
        Vendor name
        """
        return "Untangle"

    @staticmethod
    def initial_setup(self):
        global app, AD_RESULT, AD_RESULT, RADIUS_RESULT
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        AD_RESULT = subprocess.call(["ping", "-c", "1", global_functions.AD_SERVER], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        RADIUS_RESULT = subprocess.call(["ping", "-c", "1", global_functions.RADIUS_SERVER], stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        # enable the API for testing
        appSettings = app.getSettings()
        appSettings['apiEnabled'] = True
        app.setSettings(appSettings)

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        """
        Verify client is online
        """
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_015_setADSettings_NonSecure(self):
        """
        Test and save settings for test AD server, non secure
        """
        if (AD_RESULT != 0):
            raise unittest.SkipTest("No AD server available")
        result = add_ad_settings(ldap_secure=False)
        print('result %s' % result)

        assert (result == 0)

    def test_016_setADSettings_Secure(self):
        """
        Test and save settings for test AD server, secure
        """
        if (AD_RESULT != 0):
            raise unittest.SkipTest("No secure AD server available")
        result = add_ad_settings(ldap_secure=True)
        print('result %s' % result)

        assert (result == 0)

    def test_030_checkUserRegistrationScript(self):
        """
        checkUserRegistration
        """
        # remove leading and trailing spaces.
        http_admin = global_functions.get_http_url()
        assert(http_admin)

        test_name = "randomName-" + "".join( [random.choice(string.ascii_letters) for i in range(15)] )
        test_name_lower = test_name.lower()
        result = register_username(http_admin, test_name)
        user_list = get_list_of_username_mapped()
        # print('test_name %s' % test_name)
        # print('result %s' % result)
        # print('user_list %s' % user_list)
        found_username = find_name_in_host_table(test_name_lower)
        assert(found_username)
        assert (result == 0)
        assert (test_name_lower in user_list)

        events = global_functions.get_events('Directory Connector','API Events',None,1)
        assert(events != None)
        found_in_reports = global_functions.check_events( events.get('list'), 5,
                                            "login_name",test_name_lower,
                                            "client_addr", remote_control.client_ip)
        assert( found_in_reports )

    def test_031_checkUserRegistrationScriptMixedCase(self):
        """
        checkUserRegistration, mixed character case in username
        """
        # remove leading and trailing spaces.
        http_admin = global_functions.get_http_url().title()
        assert(http_admin)

        test_name = "randomName-" + "".join( [random.choice(string.ascii_letters) for i in range(15)] )
        # Force at least one upper-case character
        result = register_username(http_admin, test_name.title())
        user_list = get_list_of_username_mapped()
        # print('result %s' % result)
        # print('num %s' % numUsers)
        test_name = test_name.lower()
        found_username = find_name_in_host_table(test_name)

        assert(found_username)
        assert (result == 0)
        assert (test_name in user_list)

    def test_032_checkUserRegistrationScriptOld(self):
        """
        Check old user registration
        """
        # remove leading and trailing spaces.
        http_admin = global_functions.get_http_url()
        assert(http_admin)

        test_name = "randomName-" + "".join( [random.choice(string.ascii_letters) for i in range(15)] )
        test_name = test_name.lower()
        result = register_username_old(http_admin, test_name)
        user_list = get_list_of_username_mapped()
        # print('result %s' % result)
        # print('num %s' % numUsers)
        found_username = find_name_in_host_table(test_name)

        assert(found_username)
        assert (result == 0)
        assert (test_name in user_list)

    def test_033_checkUserNotificationLoginScriptDownload(self):
        """
        Check download of user notification script
        """
        # remove leading and trailing spaces.
        http_admin = global_functions.get_http_url()
        assert(http_admin)
        
        script_uri = "userapi/registration?download=download"
        script_url = http_admin + script_uri
        response = requests.get(script_url)
        # print(user_script)

        assert ("serverLocation" in response.text)
        assert ("secret" in response.text)
        assert ("urlProtocol" in response.text)

    def test_040_checkADSettings_NonSecure(self):
        """
        Check AD settings, non-secure
        """
        if (AD_RESULT != 0):
            raise unittest.SkipTest("No AD server available")
        result = add_ad_settings(ldap_secure=False)
        print('result %s' % result)
        assert (result == 0)

        string_to_find = "authentication success"
        appData = app.getSettings()["activeDirectorySettings"]["servers"]["list"][0]
        appAD = app.getActiveDirectoryManager()
        appADData = appAD.getStatusForSettings(appData)  # if settings are successful
        print("appADData : %s" % appADData)
        # found = appADData.count(string_to_find)

        assert ("PASS" in appADData['status'])

    def test_041_checkADSettings_Secure(self):
        """
        Check AD settings, secure
        """
        if (AD_RESULT != 0):
            raise unittest.SkipTest("No secure AD server available")
        result = add_ad_settings(ldap_secure=True)
        print('result %s' % result)
        assert (result == 0)

        string_to_find = "authentication success"
        appData = app.getSettings()["activeDirectorySettings"]["servers"]["list"][0]
        appAD = app.getActiveDirectoryManager()
        appADData = appAD.getStatusForSettings(appData)  # if settings are successful
        print("appADData : %s" % appADData)
        # found = appADData.count(string_to_find)

        assert ("PASS" in appADData['status'])

    def test_050_checkListOfADUsers_NonSecure(self):
        """
        Get list of AD users, non-secure
        """
        global appData
        if (AD_RESULT != 0):
            raise unittest.SkipTest("No AD server available")
        # Check for a list of Active Directory Users
        result = add_ad_settings(ldap_secure=False)
        print('result %s' % result)
        assert (result == 0)

        appData = app.getSettings()["activeDirectorySettings"]["servers"]["list"][0]
        appAD = app.getActiveDirectoryManager()
        appADData = appAD.getUsers(None)  # list of users in AD
        result = 1
        # check for known user "tempuser" in AD user list
        for i in range(len(appADData)):
            userName = appADData[i]['uid']
            if (global_functions.AD_USER in userName):
                result = 0
            # print('userName %s' % userName)
        assert (result == 0)

    def test_051_checkListOfADUsers_Secure(self):
        """
        Get list of AD users, secure
        """
        global appData
        if (AD_RESULT != 0):
            raise unittest.SkipTest("No AD server available")
        # Check for a list of Active Directory Users
        result = add_ad_settings(ldap_secure=True)
        print('result %s' % result)
        assert (result == 0)

        appData = app.getSettings()["activeDirectorySettings"]["servers"]["list"][0]
        appAD = app.getActiveDirectoryManager()
        appADData = appAD.getUsers(None)  # list of users in AD
        result = 1
        # check for known user "tempuser" in AD user list
        for i in range(len(appADData)):
            userName = appADData[i]['uid']
            if (global_functions.AD_USER in userName):
                result = 0
            # print('userName %s' % userName)
        assert (result == 0)

    def test_060_setRadiusSettings(self):
        """
        Test and save settings for test Radius server
        """
        if (RADIUS_RESULT != 0):
            raise unittest.SkipTest("No RADIUS server available")
        app.setSettings(create_radius_settings())

        attempts = 0
        while attempts < 3:
            test_result_string = app.getRadiusManager().getRadiusStatusForSettings(create_radius_settings(), "normal", "passwd")
            if ("success" in test_result_string):
                break
            else:
                attempts += 1
        print('test_result_string %s attempts %s' % (test_result_string, attempts) ) # debug line
        assert ("success" in test_result_string)

    @staticmethod
    def final_tear_down(self):
        """
        Tear down
        """
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None

test_registry.register_module("directory-connector", DirectoryConnectorTests)
