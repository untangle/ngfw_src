"""branding_manager tests"""
import json
import re
import unittest
import urllib.error
import urllib.request
import pytest

from tests.common import NGFWTestCase
import runtests.overrides as overrides
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

Login_username = overrides.get("Login_username", default="admin")
Login_password = overrides.get("Login_password", default="passwd")

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
        global appData
        # Checking default information
        assert(default_company_name in appData['companyName'])
        assert(default_company_url in appData['companyUrl'])
        assert(default_contact_name in appData['contactName'])
        assert(default_contact_email in appData['contactEmail'])
        assert(default_banner_message in appData['bannerMessage'])
        
    @pytest.mark.failure_behind_ngfw
    def test_019_check_blockpage_branding(self):
        # Get blockpage
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", ignore_certificate=True, all_parameters=True, uri="www.playboy.com"),stdout=True)
        
        # Verify Title of blockpage as default company name
        myRegex = re.compile('<title>(.*?)</title>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.split("|")[0]
        matchText = matchText.strip()
        print("looking for: \"%s\""%default_company_name)
        print("in :\"%s\""%matchText)
        assert(default_company_name in matchText)
        
        # Verify email address is in the contact link in blockpage
        myRegex = re.compile(r'mailto:(.*?)\?', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print("looking for: \"%s\""%default_contact_email)
        print("in :\"%s\""%matchText)
        assert(default_contact_email in matchText)
        
        # Verify contact name is in the mailto in blockpage
        myRegex = re.compile('mailto:.*?>(.*?)</a>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print("looking for: \"%s\""%default_contact_name)
        print("in :\"%s\""%matchText)
        assert(default_contact_name in matchText)
        
        # Verify URL is in the Logo box in blockpage
        myRegex = re.compile('<a href=\"(.*?)\"><img .* src=\"/images/BrandingLogo', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        print("looking for: \"%s\""%default_company_url)
        print("in :\"%s\""%matchText)
        assert(default_company_url in matchText)
        
    @pytest.mark.failure_behind_ngfw
    def test_020_check_login_page_branding(self):
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
        
    def test_129_logo_upload_zip_extension_blocked(self):
        """
        Verify the v1 upload servlet's per-handler extension allowlist drops a
        wrong-extension upload.

        Posts a logo upload with filename='evil.zip'. The Logo handler's
        allowlist is {gif,png,jpg,jpeg}, so SafeUpload.safeUploadName collapses
        the basename to 'upload' (no extension survives). The handler's
        endsWith() checks then fail and it throws
        "Branding logo extension must be one of: .gif, .jpeg, .jpg, .png".
        """
        opener = global_functions.build_admin_http_opener(Login_username, Login_password)
        boundary, body = global_functions.build_upload_multipart_body(
            "logo", "", b"x" * 16, filename="evil.zip")

        req = urllib.request.Request(
            "http://localhost/admin/upload",
            data=body,
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
        )
        try:
            response = opener.open(req)
            response_text = response.read().decode()
        except urllib.error.HTTPError as e:
            response_text = e.read().decode()

        response_json = json.loads(response_text)
        assert response_json.get("success") == False, \
            f"Expected logo .zip rejected, got: {response_text}"
        msg = response_json.get("msg") or ""
        assert "extension must be one of" in msg, \
            f"Expected LogoUploadHandler extension rejection, got: {msg}"
        msg_upper = msg.upper()
        assert all(ext in msg_upper for ext in (".GIF", ".PNG", ".JPG", ".JPEG")), \
            f"Expected allowlist {{gif,png,jpg,jpeg}} in rejection, got: {msg}"

    @classmethod
    def final_extra_tear_down(cls):
        global appWeb
        if appWeb != None:
            global_functions.uvmContext.appManager().destroy( appWeb.getAppSettings()["id"] )
            appWeb = None

test_registry.register_module("branding-manager", BrandingManagerTests)
