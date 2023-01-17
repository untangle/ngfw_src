"""branding_manager tests"""
import re
import unittest
import pytest

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

app = None
appWeb = None
default_company_name   = "Arista"
default_company_url    = "http://edge.arista.com"
default_contact_name   = "your network administrator"
default_contact_email  = ""
default_banner_message = ""

default_policy_id = 1
    
@pytest.mark.branding_manager
class BrandingManagerTests(NGFWTestCase):
    
    @staticmethod
    def module_name():
        global app
        app = BrandingManagerTests._app
        return "branding-manager"

    @staticmethod
    def appNameWeb():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @classmethod
    def initial_extra_setup(cls):
        global appData, appWeb

        appData = cls._app.getSettings()

        if global_functions.uvmContext.appManager().isInstantiated(cls.appNameWeb()):
            if cls.skip_instantiated():
                pytest.skip('app %s already instantiated' % cls.appNameWeb())
            else:
                appWeb = global_functions.uvmContext.appManager().app(cls.appNameWeb())
        else:
            appWeb = global_functions.uvmContext.appManager().instantiate(cls.appNameWeb(), default_policy_id)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(global_functions.uvmContextLongTimeout.licenseManager().isLicenseValid(self.module_name()))
        
    @pytest.mark.failure_behind_ngfw
    def test_015_check_default_branding(self):
        # Get blockpage information
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", ignore_certificate=True, all_parameters=True, uri="www.playboy.com"),stdout=True)
        
        # Verify Title of blockpage as default company name
        myRegex = re.compile('<title>(.*?)</title>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.split("|")[0]
        matchText = matchText.strip()
        print("looking for: \"%s\""%default_company_name)
        print("in :\"%s\""%matchText)
        assert(default_company_name in matchText)
        
        # Verify email address is in the contact link
        myRegex = re.compile(r'mailto:(.*?)\?', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print("looking for: \"%s\""%default_contact_email)
        print("in :\"%s\""%matchText)
        assert(default_contact_email in matchText)
        
        # Verify contact name is in the mailto
        myRegex = re.compile('mailto:.*?>(.*?)</a>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print("looking for: \"%s\""%default_contact_name)
        print("in :\"%s\""%matchText)
        assert(default_contact_name in matchText)
        
        # Verify URL is in the Logo box
        myRegex = re.compile('<a href=\"(.*?)\"><img .* src=\"/images/BrandingLogo', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        print("looking for: \"%s\""%default_company_url)
        print("in :\"%s\""%matchText)
        assert(default_company_url in matchText)
        
        # Check login page for branding
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", ignore_certificate=True, all_parameters=True, uri=global_functions.get_http_url()),stdout=True)
        # Verify Title of blockpage as company name
        myRegex = re.compile('<title>(.*?)</title>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.split("|")[0]
        matchText = matchText.strip()
        print("looking for: \"%s\""%default_company_name)
        print("in :\"%s\""%matchText)
        assert(default_company_name in matchText)

    def test_021_changeBranding_bannerMessage(self):
        global app, appWeb, appData
        
        # TODO Just like the changes above, I think this may be unnecessary. Not sure though. Do we need to test multi-line?
        appData['bannerMessage'] = "A regulation banner requirement containing a mix of text including <b>html</b> and\nmultiple\nlines"
        app.setSettings(appData)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", all_parameters=True, uri=global_functions.get_http_url()),stdout=True)
        myRegex = re.compile('.*A regulation banner requirement containing a mix of text including <b>html</b> and<br/>multiple<br/>lines.*', re.DOTALL|re.MULTILINE)
        assert(re.match(myRegex, result))
            
        appData['bannerMessage'] = default_banner_message
        app.setSettings(appData)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", ignore_certificate=True, all_parameters=True, uri=global_functions.get_http_url()),stdout=True)
        myRegex = re.compile('.*A regulation banner requirement containing a mix of text including <b>html</b> and<br/>multiple<br/>lines.*', re.DOTALL|re.MULTILINE)
        assert(not re.match(myRegex, result))
        
    @classmethod
    def final_extra_tear_down(cls):
        global appWeb
        if appWeb != None:
            global_functions.uvmContext.appManager().destroy( appWeb.getAppSettings()["id"] )
            appWeb = None

test_registry.register_module("branding-manager", BrandingManagerTests)
