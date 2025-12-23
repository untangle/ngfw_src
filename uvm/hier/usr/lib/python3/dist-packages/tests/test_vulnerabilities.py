import subprocess
import pytest

from tests.common import NGFWTestCase
import tests.global_functions as global_functions
from tests.global_functions import uvmContext
import runtests.test_registry as test_registry


@pytest.mark.vulnerabilities
class VulnerabilitiesTests(NGFWTestCase):

    not_an_app= True

    @staticmethod
    def module_name():
        return "vulnerabilities"
    
    def test_010_mod_python_publisher(self):

        BASE_URL = global_functions.get_http_url()
        GET_APP_SETTINGS_ITEM_URL = '/get_app_settings_item?appname=reports&itemname=reportsUsers'
        GET_UVM_SETTINGS_ITEM_URL = '/get_uvm_settings_item?basename=admin&itemname=users'
        GET_APPID_SETTINGS = '/get_appid_settings?appid=1'
        GET_APP_SETTINGS = '/get_app_settings?appname=captive-portal'
        GET_SETTINGS_ITEM = '/get_settings_item?file=/usr/share/untangle/conf/oem.js&itemname=oemName'

        CURL_EXTRA_ARGS = '-s -o /dev/null -w "%{http_code}"'

        # Test auth/index.py
        AUTH_INDEX_URL = 'auth'
        command = global_functions.build_curl_command(uri=BASE_URL + AUTH_INDEX_URL + GET_APP_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + AUTH_INDEX_URL + GET_UVM_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)

        # Test handler.py
        CAPTURE_HANDLER_URL = 'capture/handler.py'
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_APP_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_APPID_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_APP_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_SETTINGS_ITEM, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + '/import_file?filename=/usr/share/untangle/bin/ut-enable-support-access.py', 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)

        # Test logout.py
        CAPTURE_LOGOUT_URL = 'capture/logout/logout.py'
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_APP_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_APPID_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_APP_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_SETTINGS_ITEM, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
    
    def test_020_mod_apache_status(self):

        URL = global_functions.get_http_url() + '/server-status'
        CURL_EXTRA_ARGS = '-s -o /dev/null -w "%{http_code}"'

        command = global_functions.build_curl_command(uri=URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)

    def test_030_no_exec_manager_api(self):
        """execManager should no longer be available over uvmContext as an API"""
        try:
            uvmContext.execManager()
        except Exception as e:
            assert(True)
            return
        assert(False)


test_registry.register_module("vulnerabilities", VulnerabilitiesTests)
