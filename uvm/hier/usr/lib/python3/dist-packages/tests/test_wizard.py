import pytest
import re
import json
import urllib.error
import urllib.request
from http.cookiejar import CookieJar

from tests.common import NGFWTestCase
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import tests.global_functions as global_functions

@pytest.mark.setup_wizard
class SetupWizard(NGFWTestCase):

    not_an_app = True

    @staticmethod
    def module_name():
        return "setup-wizard"

    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    # Checks the oem url and license agreement url
    def test_020_about_license_agreement(self):
        oem_url = global_functions.uvmContext.oemManager().getOemUrl()
        match = re.search('^.*edge.arista.com$', oem_url)
        assert(match)
        
        license_url = global_functions.uvmContext.oemManager().getLicenseAgreementUrl();
        match = re.search('^.*edge.arista.com/legal$', license_url)
        assert(match)

    def test_030_safecheckparam_setup_context(self):
        """SetupContextImpl.setLanguage / setAdminPassword (setup bridge).

        The /setup/JSON-RPC servlet is always loaded and the SetupContext
        object is registered globally on every box. Jabsorb requires a
        per-bridge nonce on every call (system.getNonce returns it); the
        bridge is stored in the HttpSession, so a CookieJar is needed to
        keep getNonce and the subsequent calls on the same bridge.

        Only negative cases are exercised — happy paths would persist
        changes to the admin password or active language.

          setLanguage(language: ALPHANUM, source: ALPHANUM)
          setAdminPassword(password: OPAQUE_SECRET, email: EMAIL,
                           installType: ALPHANUM)
        """
        url = "http://localhost/setup/JSON-RPC"
        opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(CookieJar()))

        def post(body):
            req = urllib.request.Request(
                url, data=json.dumps(body).encode("utf-8"), method="POST",
                headers={"Content-Type": "text/plain; charset=utf-8"})
            try:
                raw = opener.open(req, timeout=10).read()
            except urllib.error.HTTPError as e:
                raw = e.read()
            return json.loads(raw.decode("utf-8", "replace"))

        nonce_resp = post({"method": "system.getNonce", "params": [], "id": 0})
        nonce = nonce_resp.get("result")
        assert isinstance(nonce, str) and nonce, \
            "system.getNonce did not return a usable nonce: {}".format(nonce_resp)

        def rpc(method, params, call_id):
            return post({"method": method, "params": params,
                         "nonce": nonce, "id": call_id})

        def is_safecheck_rejection(r):
            err = r.get("error")
            if not isinstance(err, dict):
                return False
            if err.get("code") == 490:
                return True
            return "Invalid value in" in (err.get("msg") or err.get("message") or "")

        # setLanguage — both args carry @SafeCheckParam(ALPHANUM)
        r = rpc("SetupContext.setLanguage", ["en;id", "official"], 1)
        assert is_safecheck_rejection(r), \
            "setLanguage(arg0=semicolon) not rejected by @SafeCheckParam: {}".format(r)
        r = rpc("SetupContext.setLanguage", ["en", "off icial"], 2)
        assert is_safecheck_rejection(r), \
            "setLanguage(arg1=space) not rejected by @SafeCheckParam: {}".format(r)

        # setAdminPassword — OPAQUE_SECRET / EMAIL / ALPHANUM
        r = rpc("SetupContext.setAdminPassword",
                ["pwd\nINJECT", "a@b.com", "default"], 3)
        assert is_safecheck_rejection(r), \
            "setAdminPassword(arg0 control char) not rejected: {}".format(r)
        r = rpc("SetupContext.setAdminPassword",
                ["Passw0rd!", "not-an-email", "default"], 4)
        assert is_safecheck_rejection(r), \
            "setAdminPassword(arg1 bad email) not rejected: {}".format(r)
        r = rpc("SetupContext.setAdminPassword",
                ["Passw0rd!", "a@b.com", "default;id"], 5)
        assert is_safecheck_rejection(r), \
            "setAdminPassword(arg2 semicolon) not rejected: {}".format(r)

test_registry.register_module("setup-wizard", SetupWizard)