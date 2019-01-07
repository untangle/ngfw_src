"""branding_manager tests"""
import re
import unittest

from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

app = None
appWeb = None
newCompanyName = "Some new long name"
newURL = "https://test.untangle.com/cgi-bin/myipaddress.py"
newContactName = "Skynet"
newContactEmail = "skynet@untangle.com"

default_policy_id = 1

def setDefaultBrandingManagerSettings():
    appData = {
        "javaClass": "com.untangle.app.branding_manager.BrandingManagerSettings",
        "companyName": "Untangle",
        "companyUrl": "http://untangle.com/",
        "contactName": "your network administrator",
        "contactEmail": None,
        "bannerMessage": None,
        "defaultLogo": True
    }
    app.setSettings(appData)
    
class BrandingManagerTests(unittest.TestCase):
    
    @staticmethod
    def module_name():
        return "branding-manager"

    @staticmethod
    def appNameWeb():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initial_setup(self):
        global appData, app, appWeb
        if (uvmContextLongTimeout.appManager().isInstantiated(self.module_name())):
            print("ERROR: App %s already installed" % self.module_name())
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContextLongTimeout.appManager().instantiate(self.module_name(), default_policy_id)
        appData = app.getSettings()
        if (uvmContextLongTimeout.appManager().isInstantiated(self.appNameWeb())):
            print("ERROR: App %s already installed" % self.appNameWeb())
            raise Exception('app %s already instantiated' % self.appNameWeb())
        appWeb = uvmContextLongTimeout.appManager().instantiate(self.appNameWeb(), default_policy_id)

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContextLongTimeout.licenseManager().isLicenseValid(self.module_name()))

    def test_020_changeBranding(self):
        global app, appWeb, appData
        appData['companyName'] = newCompanyName;
        appData['companyUrl'] = newURL;
        appData['contactName'] = newContactName;
        appData['contactEmail'] = newContactEmail;
        app.setSettings(appData)
        # test blockpage has all the changes
        result = remote_control.run_command("wget -q -O - \"$@\" www.playboy.com",stdout=True)

        # Verify Title of blockpage as company name
        myRegex = re.compile('<title>(.*?)</title>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.split("|")[0]
        matchText = matchText.strip()
        print("looking for: \"%s\""%newCompanyName)
        print("in :\"%s\""%matchText)
        assert(newCompanyName in matchText)

        # Verify email address is in the contact link
        myRegex = re.compile('mailto:(.*?)\?', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print("looking for: \"%s\""%newContactEmail)
        print("in :\"%s\""%matchText)
        assert(newContactEmail in matchText)

        # Verify contact name is in the mailto
        myRegex = re.compile('mailto:.*?>(.*?)<\/a>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print("looking for: \"%s\""%newContactName)
        print("in :\"%s\""%matchText)
        assert(newContactName in matchText)

        # Verify URL is in the Logo box
        myRegex = re.compile('<a href\=\"(.*?)\"><img .* src\=\"\/images\/BrandingLogo', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        print("looking for: \"%s\""%newURL)
        print("in :\"%s\""%matchText)
        assert(newURL in matchText)
       
        # Check login page for branding
        internalAdmin = None
        # print("IP address <%s>" % internalAdmin)
        result = remote_control.run_command("wget -q -O - \"$@\" " + global_functions.get_http_url() ,stdout=True)
        # print("page is <%s>" % result)
        # Verify Title of blockpage as company name
        myRegex = re.compile('<title>(.*?)</title>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.split("|")[0]
        matchText = matchText.strip()
        print("looking for: \"%s\""%newCompanyName)
        print("in :\"%s\""%matchText)
        assert(newCompanyName in matchText)

    def test_021_changeBranding_bannerMessage_added(self):
        global app, appWeb, appData
        appData['companyName'] = newCompanyName;
        appData['companyUrl'] = newURL;
        appData['contactName'] = newContactName;
        appData['contactEmail'] = newContactEmail;
        appData['bannerMessage'] = "A regulation banner requirement containing a mix of text including <b>html</b> and\nmultiple\nlines"
        app.setSettings(appData)

        internalAdmin = None
        result = remote_control.run_command("wget -q -O - \"$@\" " + global_functions.get_http_url() ,stdout=True)
        myRegex = re.compile('.*A regulation banner requirement containing a mix of text including <b>html<\/b> and<br\/>multiple<br\/>lines.*', re.DOTALL|re.MULTILINE)
        if re.match(myRegex,result):
            assert(True)
        else:
            assert(False)
        
    def test_022_changeBranding_bannerMessage_removed(self):
        global app, appWeb, appData
        appData['companyName'] = newCompanyName;
        appData['companyUrl'] = newURL;
        appData['contactName'] = newContactName;
        appData['contactEmail'] = newContactEmail;
        appData['bannerMessage'] = ""
        app.setSettings(appData)

        internalAdmin = None
        result = remote_control.run_command("wget -q -O - \"$@\" " + global_functions.get_http_url() ,stdout=True)
        myRegex = re.compile('.*A regulation banner requirement containing a mix of text including <b>html<\/b> and<br\/>multiple<br\/>lines.*', re.DOTALL|re.MULTILINE)
        if re.match(myRegex,result):
            assert(False)
        else:
            assert(True)
        
    @staticmethod
    def final_tear_down(self):
        global app, appWeb
        if app != None:
            # Restore original settings to return to initial settings
            setDefaultBrandingManagerSettings()
            uvmContextLongTimeout.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if appWeb != None:
            uvmContextLongTimeout.appManager().destroy( appWeb.getAppSettings()["id"] )
            appWeb = None

test_registry.register_module("branding-manager", BrandingManagerTests)
