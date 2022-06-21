import hashlib
import html
import base64
import sys
import re
import pycurl
import json
import crypt
import sys
import urllib.parse
from io import StringIO
#import uvm_login

def get_app_settings_item(a,b):
    return None
def get_uvm_settings_item(a,b):
    return None

try:
    from uvm.settings_reader import get_app_settings_item
    from uvm.settings_reader import get_uvm_settings_item
except ImportError:
    pass

def valid_login(req, realm, username, password):
    if realm == 'Administrator':
        return admin_valid_login(req, realm, username, password)
    elif realm == 'Reports':
        if admin_valid_login(req, 'Administrator', username, password, False):
            return True
        else:
            return reports_valid_login(req, realm, username, password)
    else:
        return False

def valid_token(req, token):
    try:
        uid=None
        with open('/usr/share/untangle/conf/uid', 'r') as uidfile:
            uid=uidfile.read().replace('\n', '')

        buffer = StringIO()
        postdata = json.dumps({ "token": token, "resourceId": uid  })

        curl = pycurl.Curl()
        curl.setopt( pycurl.POST, 1 )
        curl.setopt( pycurl.POSTFIELDS, postdata )
        curl.setopt( pycurl.NOSIGNAL, 1 )
        curl.setopt( pycurl.CONNECTTIMEOUT, 30 )
        curl.setopt( pycurl.TIMEOUT, 30 )
        #curl.setopt( pycurl.URL, "http://54.152.2.165:1337/AuthenticationService/1/CheckTokenAccess")
        curl.setopt( pycurl.URL, "https://auth.untangle.com/v1/CheckTokenAccess")
        curl.setopt( pycurl.HTTPHEADER, ["Content-type: application/json", "Accept: application/json", "AuthRequest: 4E6FAB77-B2DF-4DEA-B6BD-2B434A3AE981"])
        #curl.setopt( pycurl.VERBOSE, True )
        curl.setopt( pycurl.WRITEDATA, buffer )

        curl.perform()

        body = buffer.getvalue()
        print(body)
        return (body == "true")
    except:
        return False

def reports_valid_login(req, realm, username, password, log=True):
    users = get_app_settings_item('reports','reportsUsers')
    if users == None:
        return False;
    if users['list'] == None:
        return False;
    for user in users['list']:
        if user['emailAddress'] != username:
            continue;

        pw_hash_shadow = user.get('passwordHashShadow')
        if pw_hash_shadow:
            if pw_hash_shadow == crypt.crypt(password, pw_hash_shadow):
                log_login_if_necessary(log, req, username, True, None)
                return True
            else:
                log_login_if_necessary(log, req, username, False, 'P')
                return False
        else:
            pw_hash_base64 = user['passwordHashBase64']
            pw_hash = base64.b64decode(pw_hash_base64)
            raw_pw = pw_hash[0:len(pw_hash) - 8]
            salt = pw_hash[len(pw_hash) - 8:]
            b = password + salt
            if raw_pw == hashlib.md5(b.encode('utf-8')).hexdigest():
                log_login_if_necessary(log, req, username, True, None)
                return True
            else:
                log_login_if_necessary(log, req, username, False, 'P')
                return False
    log_login_if_necessary(log, req, username, False, 'P')
    return False

def admin_valid_login(req, realm, username, password, log=True):
    """
    Returns True if this request with username/password is a valid
    login.
    """
    users = get_uvm_settings_item('admin','users')
    if users == None:
        return False;
    if users['list'] == None:
        return False;
    for user in users['list']:
        if user['username'] != username:
            continue;
        pw_hash_shadow = user.get('passwordHashShadow')
        if pw_hash_shadow:
            if pw_hash_shadow == crypt.crypt(password, pw_hash_shadow):
                log_login_if_necessary(log, req, username, True, None)
                return True
            else:
                log_login_if_necessary(log, req, username, False, 'P')
                return False
        else:
            pw_hash_base64 = user['passwordHashBase64']
            pw_hash = base64.b64decode(pw_hash_base64)
            raw_pw = pw_hash[0:len(pw_hash) - 8]
            salt = pw_hash[len(pw_hash) - 8:]
            b = password.encode('utf-8') + salt
            if raw_pw == hashlib.md5(b).digest():
                log_login_if_necessary(log, req, username, True, None)
                return True
            else:
                log_login_if_necessary(log, req, username, False, 'P')
                return False
    log_login_if_necessary(log, req, username, false, 'U')
    return False


def log_login_if_necessary(should_log, request, username, succeeded,
                           reason):
    """
    if should_log is true, log the login. This will try to import
    uvm_login and use the uvm_login.log_login function if available,
    else it will print to sys.stderr.

    should_log -- should we log the login?
    request -- the request object.
    username -- username that attempted to log in.
    succeeded -- did the login succeed?
    reason -- reason for failure, if the login did not succeed.

    returns -- None
    """
    def logger(req, username, succeeded, reason):
        print(
            f"Login from {username} succeeded: {succeeded}, reason: {reason}",
            file=sys.stderr)
    try:
        import uvm_login
        logger = uvm_login.log_login
    except ImportError:
        pass
    logger(request, username, succeeded, reason)


def write_login_form(req, title, host, error_msg):
    login_url = html.escape(req.unparsed_uri)
    req.content_type = "text/html; charset=utf-8"
    req.send_http_header()

    if error_msg == None:
        error_msg = ''

    server_str = html.escape(_("Server:"))
    username_str = html.escape(_("Username:"))
    password_str = html.escape(_("Password:"))
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
        banner_msg = "<p>" + banner_msg.encode('utf-8') + "</p>"
    else:
        banner_msg = ""

    html_string = """\
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="initial-scale=1.0, width=device-width">
<meta name="description" content="loginPage">
<title>%s</title>
<script type="text/javascript">if (top.location!=location) top.location.href=document.location.href;</script>
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
</head>
<body>

<header>
    <img src="/images/BrandingLogo.png" style="max-width: 150px; max-height: 140px;">
</header>

<div class="form-login">
    <form method="post" action="%s">
        <h2>%s</h2>
        <p class="server">%s</p>
        <div class="banner">%s</div>
        <p class="error">%s</p>
        <input id="fragment" type="hidden"   name="fragment" value=""/>
        <input id="username" type="text"     name="username" value="%s" placeholder="%s"/>
        <input id="password" type="password" name="password" placeholder="%s"/>
        <button type="submit">%s</button>
    </form>
</div>

<script type="text/javascript">document.getElementById('%s').focus();</script>
<script type="text/javascript">document.getElementById('fragment').value=window.location.hash;</script>

</body>
</html>""" % (title, login_url, title, host, banner_msg, error_msg, default_username, username_str, password_str, login_str, focus_field_id)

    req.write(html_string)
