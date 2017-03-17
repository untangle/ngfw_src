# $Id$
import md5
import cgi
import base64
import sys
import re
import pycurl
import json
from StringIO import StringIO

from mod_python import apache, Session, util
from psycopg2 import connect

sys.path.insert(0,'@PREFIX@/usr/lib/python%d.%d/' % sys.version_info[:2])
import uvm_login

def get_app_settings_item(a,b):
    return None
def get_uvm_settings_item(a,b):
    return None

try:
    from uvm.settings_reader import get_app_settings_item
    from uvm.settings_reader import get_uvm_settings_item
except ImportError:
    pass

# pages -----------------------------------------------------------------------

def login(req, url=None, realm='Administrator', token=None):
    uvm_login.setup_gettext()

    options = req.get_options()

    args = util.parse_qs(req.args or '')

    error_msg = None
    if req.form.has_key('username') or req.form.has_key('password'):
        error_msg = '%s' % cgi.escape(_('Error: Username and Password do not match'))

    if token != None:
        if _valid_token(req, token):
            sess = Session.Session(req)
            sess.set_timeout(uvm_login.SESSION_TIMEOUT)
            uvm_login.save_session_user(sess, realm, "token")
            sess.save()
            sess.unlock()

            if url == None:
                return apache.OK
            else:
                url = re.sub('[^A-Za-z0-9-_/.#?=]','',url) # sanitize input
                util.redirect(req, url, text="Login Successful")

    if req.form.has_key('username') and req.form.has_key('password'):
        username = req.form['username']
        password = req.form['password']
        # debug
        # req.log_error("User:Pass = %s %s" % (username,password))

        if _valid_login(req, realm, username, password):
            sess = Session.Session(req)
            sess.set_timeout(uvm_login.SESSION_TIMEOUT)
            uvm_login.save_session_user(sess, realm, username)
            sess.save()
            sess.unlock()

            if url == None:
                return apache.OK
            else:
                url = re.sub('[^A-Za-z0-9-_/.#?=]','',url) # sanitize input
                util.redirect(req, url, text="Login Successful")

    company_name = uvm_login.get_company_name()
    title = _("Administrator Login")
    # some i18n company_names cause exception here, so wrap to handle this
    # revert to "Administrator Login" if exception occurs
    try:
        title = cgi.escape(_("%s Administrator Login") % company_name)
    except:
        pass

    host = cgi.escape(req.hostname)

    _write_login_form(req, title, host, error_msg)

def logout(req, url=None, realm='Administrator'):
    sess = Session.Session(req)
    sess.set_timeout(uvm_login.SESSION_TIMEOUT)
    uvm_login.delete_session_user(sess, realm)
    sess.save()
    sess.unlock()

    if url == None:
        return apache.OK
    else:
        url = re.sub('[^A-Za-z0-9-_/.#?=]','',url) # sanitize input
        util.redirect(req, url, text="Logout Successfull")

# internal methods ------------------------------------------------------------

def _valid_login(req, realm, username, password):
    if realm == 'Administrator':
        return _admin_valid_login(req, realm, username, password)
    elif realm == 'Reports':
        if _admin_valid_login(req, 'Administrator', username, password, False):
            return True;
        else:
            return _reports_valid_login(req, realm, username, password)
    else:
        return False

def _valid_token(req, token):
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
        print (body)
        return (body == "true")
    except:
        return False

def _reports_valid_login(req, realm, username, password, log=True):
    users = get_app_settings_item('reports','reportsUsers')
    if users == None:
        return False;
    if users['list'] == None:
        return False;
    for user in users['list']:
        if user['emailAddress'] != username:
            continue;
        pw_hash_base64 = user['passwordHashBase64']
        pw_hash = base64.b64decode(pw_hash_base64)
        raw_pw = pw_hash[0:len(pw_hash) - 8]
        salt = pw_hash[len(pw_hash) - 8:]
        if raw_pw == md5.new(password + salt).digest():
            if log:
                uvm_login.log_login(req, username, False, True, None)
            return True
        else:
            if log:
                uvm_login.log_login(req, username, False, False, 'P')
            return False
    if log:
        uvm_login.log_login(req, username, False, False, 'U')
    return False

def _admin_valid_login(req, realm, username, password, log=True):
    users = get_uvm_settings_item('admin','users')
    if users == None:
        return False;
    if users['list'] == None:
        return False;
    for user in users['list']:
        if user['username'] != username:
            continue;
        pw_hash_base64 = user['passwordHashBase64']
        pw_hash = base64.b64decode(pw_hash_base64)
        raw_pw = pw_hash[0:len(pw_hash) - 8]
        salt = pw_hash[len(pw_hash) - 8:]
        if raw_pw == md5.new(password + salt).digest():
            if log:
                uvm_login.log_login(req, username, False, True, None)
            return True
        else:
            if log:
                uvm_login.log_login(req, username, False, False, 'P')
            return False
    if log:
        uvm_login.log_login(req, username, False, False, 'U')
    return False

def _write_login_form(req, title, host, error_msg):
    login_url = cgi.escape(req.unparsed_uri)
    req.content_type = "text/html; charset=utf-8"
    req.send_http_header()

    if error_msg == None:
        error_msg = ''

    server_str = cgi.escape(_("Server:"))
    username_str = cgi.escape(_("Username:"))
    password_str = cgi.escape(_("Password:"))
    login_str = cgi.escape(_("Login"))

    if not type(title) is str:
        title = cgi.escape(title).encode("utf-8")
    if not type(host) is str:
        host = cgi.escape(host).encode("utf-8")

    default_username = get_uvm_settings_item('admin','defaultUsername')
    if default_username == None:
        default_username = "admin"
    focus_field_id = "password"
    if default_username == "":
        focus_field_id = "username"

    banner_msg = get_app_settings_item('branding-manager','bannerMessage')
    if banner_msg != None and banner_msg != "":
        banner_msg = banner_msg.replace("\n", "<br/>")
        banner_msg = "<p>" + banner_msg.encode('utf-8') + "</p>"
    else:
        banner_msg = ""

    html = """\
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
    <img src="/images/BrandingLogo.png">
</header>

<div class="form-login">
    <form method="post" action="%s">
        <h2>%s</h2>
        <p class="server">%s</p>
        <div class="banner">%s</div>
        <p class="error">%s</p>
        <input id="username" type="text" name="username" value="%s" placeholder="%s"/>
        <input id="password" type="password" name="password" placeholder="%s"/>
        <button type="submit">%s</button>
    </form>
</div>

<script type="text/javascript">document.getElementById('%s').focus();</script>

</body>
</html>""" % (title, login_url, title, host, banner_msg, error_msg, default_username, username_str, password_str, login_str, focus_field_id)

    req.write(html)
