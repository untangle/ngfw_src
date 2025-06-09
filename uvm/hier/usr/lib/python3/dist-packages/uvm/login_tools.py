import hashlib
import html
import base64
import sys
import requests
import json
import crypt
import time
import os
import urllib
import urllib3

from uvm import Uvm
from mod_python import apache

def get_app_settings_item(a,b):
    return None
def get_uvm_settings_item(a,b):
    return None

try:
    from uvm.settings_reader import get_app_settings_item
    from uvm.settings_reader import get_uvm_settings_item
except ImportError:
    pass

# While not common, lack of the pyopt library would
# prevent anyone from logging in.
try:
    import pyotp
    pyopt_imported = True
except ImportError:
    pyopt_imported = False
    pass

def get_auth_uri():
    try:
        return Uvm().getUvmContext().uriManager().getUri('https://auth.edge.arista.com/v1/CheckTokenAccess')
    except:
        return 'https://auth.edge.arista.com/v1/CheckTokenAccess'


class Totp:
    """
    Manaage TOTP for time-based OTP
    """
    cmd_file_path = "/usr/share/untangle/conf/cmd_totp.conf"

    @classmethod
    def enabled(cls, req):
        """
        Detect if TOTP is enabled for this login

        :returns True if TOTP is enabled, False otherwise
        """
        if os.path.isfile(Totp.cmd_file_path) is False:
            # No seed file
            return False

        if Totp.is_local(req):
            # Not enabled for a local console session
            return False

        if pyopt_imported is False:
            # If we're unable to import the pyopt library,
            # we're not enabled but we want to be very explicit about this case!
            apache.log_error("Error!  Unable to import pyotp")
            return False

        return True

    @classmethod
    def is_local(cls, req):
        """
        Detect if client is using the local console.
        :param req          Request object.
        :returns            True if client is using local console, False othewise.
        """
        (client_addr, client_port) = req.useragent_addr

        return client_addr == "127.0.0.1" or client_addr == "::1"

    @classmethod
    def validate(cls, totp_code, logger):
        """
        Determine if specified totp_code is valid.

        :param totp_code    String of TOTP code
        :param logger       Logger object.
        :returns            True if totp_code matches, False otherwise.
        """
        if totp_code is None:
            logger.log_failure("T")
            return False

        raw_uri=None
        with open(Totp.cmd_file_path, "r") as file:
            raw_uri=file.read()

        # Format of file is:
        # otpauth://totp/Edge%20Threat%20Management%20Appliance%20Login%20%28FirstName%20LastName%29?secret=O7OLTABU3XUPCD4Q7MWCZQR2I4JXF5MQTT5MYDHE5SHIGTWROUKZ2IIP6UPLTPBIZWKPYBBB4LSX2CAKWYS6RXWGSKKZDBWMC45N4SQ
        # We need to extract the secret query parameter

        raw_uri = raw_uri.replace('\/', '/')
        parsed_uri = urllib3.util.url.parse_url(raw_uri)
        uri_qs = urllib.parse.parse_qs(parsed_uri.query)
        secret=uri_qs['secret'][0]

        totp = pyotp.TOTP(secret)
        if totp.verify(totp_code, valid_window=1):
            logger.log_success("I")
            return True

        logger.log_failure("T")
        return False

# We define two logger classes so that when this module is imported
# outside of a running apache instance, we can still run the functions
# here. Otherwise we will fail to import since we would import
# uvm_login.
class UVMLoginLogger:
    """
    Logger class that logs login attempts via the uvm_login.log_login
    function.
    """
    def __init__(self, should_log):
        self._request = None
        self._realm = None
        self._username = None
        self._should_log = should_log

    def set_request(self, request):
        self._request = request

    def set_realm(self, realm):
        self._realm = realm

    def set_username(self, username):
        self._username = username

    def log_failure(self, reason):
        if self._should_log:
            uvm_login.log_login(self._request,
                                self._username,
                                False,
                                reason)

    def log_success(self, reason):
        if self._should_log:
            uvm_login.log_login(self._request,
                                self._username,
                                True,
                                reason)

    def logout_success(self, reason):
        if self._should_log:
            uvm_login.log_login(self._request,
                                self._username,
                                True,
                                reason)

class StderrLoginLogger:
    """
    Logger class that logs login attempts to sys.stderr via
    print().
    """
    def __init__(self, should_log):
        self._realm = None
        self._username = None
        self._should_log = should_log

    def set_request(self, request):
        return

    def set_realm(self, realm):
        self._realm = realm

    def set_username(self, username):
        self._username = username

    def log_success(self, reason):
        if self._should_log:
            print(f"Successful login  of user: {self._username}"
                  f" on realm: {self._realm}",
                  f"reason: {reason}",
                  file=sys.stderr)

    def log_failure(self, reason):
        if self._should_log:
            print(
                f"Failure to log in user: {self._username}"
                f" on realm: {self._realm}"
                f"reason: {reason}",
                file=sys.stderr)

    def logout_success(self, reason):
        if self._should_log:
            print(f"Successful logut  of user: {self._username}"
                    f" on realm: {self._realm}",
                    f"reason: {reason}",
                    file=sys.stderr)

class NullLogger:
    """
    Logger that doesn't do anything (for when you want an empty logger
    to pass around).
    """
    def log_success(self, reason):
        pass

    def log_failure(self, reason):
        pass
    
    def log_success(self, reason):
        pass



# Default logger just logs to stderr.
loggerFactory = StderrLoginLogger

# Try to use the uvm_login module to do logging, but this may not be
# possible, if we are running outside of apache.
try:
    import uvm_login
    loggerFactory = UVMLoginLogger
except:
    pass


def get_logger(req, realm, username, password):
    """
    Construct a logger from loggerFactory and call relevant
    setters.
    """
    logger = loggerFactory(True)
    logger.set_request(req)
    logger.set_realm(realm)
    logger.set_username(username)
    return logger

def valid_login(req, realm, username, password, totp=None):
    logger = get_logger(req, realm, username, password)
    if realm == 'Administrator':
        return admin_valid_login(req, realm, username, password, totp, logger)
    elif realm == 'Reports':
        if admin_valid_login(req, 'Administrator', username, password, totp, NullLogger()):
            return True
        else:
            return reports_valid_login(req, realm, username, password, totp, logger)
    else:
        return False


def getuid():
    uid = None
    with open('/usr/share/untangle/conf/uid', 'r') as uidfile:
        uid = uidfile.read().replace('\n', '')
    return uid


AUTH_REQUEST_HEADER_TOKEN = 'B132C885-962B-4D63-8B2F-441B7A43CD93'


def valid_token(token):
    """
    Returns true if token is valid.

    token -- a token string that we will check against auth.edge.arista.com
    """
    try:
        uid = getuid()
        postdata = json.dumps({"token": token, "resourceId": uid})
        response = requests.post(
            get_auth_uri(),
            data=postdata,
            headers={
                "Content-Type": 'application/json',
                'Accept': 'application/json',
                'AuthRequest': AUTH_REQUEST_HEADER_TOKEN})
        response.raise_for_status()
        value = response.json()
        return value
    except Exception as e:
        print(
            f"auth: login_tools.valid_token(): caught error: {e}",
            file=sys.stderr)
        return False



def reports_valid_login(req, realm, username, password, totp=None, logger=StderrLoginLogger(True)):
    users = get_app_settings_item('reports','reportsUsers')
    if users == None:
        return False
    if users['list'] == None:
        return False
    for user in users['list']:
        if user['emailAddress'] != username:
            continue
        if check_password(user, password, logger):
            if Totp.enabled(req):
                return Totp.validate(totp, logger)
            else:
                logger.log_success("I")
                return True
        else:
            return False

    logger.log_failure("U")
    return False

def admin_valid_login(req, realm, username, password, totp=None, logger=StderrLoginLogger(True)):
    """
    Returns True if this request with username/password is a valid
    login.
    """
    users = get_uvm_settings_item('admin','users')
    if users == None:
        return False
    if users['list'] == None:
        return False
    for user in users['list']:
        if user['username'] != username:
            continue
        if check_password(user, password, logger):
            if Totp.enabled(req):
                return Totp.validate(totp, logger)
            else:
                logger.log_success("I")
                return True
        else:
            return False

    logger.log_failure("U")
    return False

def check_password(user_dict, password, logger):
    """
    Check the password against whatever type of hash is stored in
    user_dict, logging the result to logger, and returning True of the
    password matches, False if not.

    user_dict -- dictionary for the user.
    password -- password string.
    logger -- logger with log_sucess() and log_failure(string)
    methods.

    returns -- T/F.
    """
    pw_hash_shadow = user_dict.get('passwordHashShadow')
    if pw_hash_shadow:
        if pw_hash_shadow == crypt.crypt(password, pw_hash_shadow):
            return True
        else:
            logger.log_failure("P")
            return False
    else:
        pw_hash_base64 = user_dict['passwordHashBase64']
        pw_hash = base64.b64decode(pw_hash_base64)
        raw_pw = pw_hash[0:len(pw_hash) - 8]
        salt = pw_hash[len(pw_hash) - 8:]
        b = password.encode('utf-8') + salt
        if raw_pw == hashlib.md5(b).digest():
            return True
        else:
            logger.log_failure("P")
            return False

def write_login_form(req, title, host, error_msg):
    timestamp = time.time()
    login_url = html.escape(req.unparsed_uri)
    req.content_type = "text/html; charset=utf-8"
    req.send_http_header()

    if error_msg == None:
        error_msg = ''

    username_str = html.escape(_("Username:"))
    password_str = html.escape(_("Password:"))
    totp_str = html.escape(_("TOTP:"))
    login_str = html.escape(_("Login"))

    if not type(title) is str:
        title = html.escape(title).encode("utf-8")
    if not type(host) is str:
        host = html.escape(host).encode("utf-8")

    try:
        default_username = get_uvm_settings_item('admin','defaultUsername')
        if default_username == None:
            default_username = "admin"
        else:
            default_username = str(default_username)
    except:
        default_username = ""

    focus_field_id = "password"
    if default_username == "":
        focus_field_id = "username"

    banner_msg = get_app_settings_item('branding-manager','bannerMessage')
    if banner_msg != None and banner_msg != "":
        banner_msg = banner_msg.replace("\n", "<br/>")
        banner_msg = "<p>" + banner_msg + "</p>"
    else:
        banner_msg = ""

    if Totp.enabled(req):
        totp_field = f"""\
        <input id="totp" type="totp" name="totp" placeholder="{totp_str}"/>
"""
    else:
        totp_field = ""

    html_string = f"""\
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="initial-scale=1.0, width=device-width">
<meta name="description" content="loginPage">
<title>{title}</title>
<script type="text/javascript">if (top.location!=location) top.location.href=document.location.href;</script>
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
</head>
<body>

<header>
    <img src="/images/BrandingLogo.png?{timestamp}" style="max-width: 300px; max-height: 48px;">
</header>

<div class="form-login">
    <form method="post" action="{login_url}">
        <h2>{title}</h2>
        <p class="server">{host}</p>
        <div class="banner">{banner_msg}</div>
        <p class="error">{error_msg}</p>
        <input id="fragment" type="hidden"   name="fragment" value=""/>
        <input id="username" type="text"     name="username" value="{default_username}" placeholder="{username_str}"/>
        <input id="password" type="password" name="password" placeholder="{password_str}"/>
        {totp_field}
        <button type="submit">{login_str}</button>
    </form>
</div>

<script type="text/javascript">document.getElementById('{focus_field_id}').focus();</script>
<script type="text/javascript">document.getElementById('fragment').value=window.location.hash;</script>

</body>
</html>"""


    req.write(html_string)
