# $Id$
import hashlib
import html
import base64
import sys
import re
import pycurl
import json
import crypt
import urllib.parse
from io import StringIO
from mod_python import apache, Session, util

if "@PREFIX@" != '' and '@' not in '@PREFIX@':
    sys.path.insert(0, '@PREFIX@/usr/lib/python3/dist-packages')

from uvm import login_tools
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
    args = urllib.parse.parse_qs(req.args)
    form = {k:v[0] for k,v in args.items()}
    for k, field in req.form.items():
        if k in form:
            continue
        if type(k) is bytes:
            k = k.decode("utf-8")
        v = field.value
        if type(v) is bytes:
            v = v.decode('utf-8')
        form[k] = v

    url = form['url']
    realm = form['realm']
    if 'fragment' in form:
        fragment = form['fragment']

    error_msg = None
    if 'username' in form or 'password' in form:
        error_msg = '%s' % html.escape(_('Error: Username and Password do not match'))

    connection = req.connection
    (addr, port) = connection.local_addr
    is_local = re.match('127\.', req.useragent_ip)
    if req.useragent_ip == '::1':
        is_local = True
    if port == 80 and not get_uvm_settings_item('system','httpAdministrationAllowed') and not is_local:
        write_error_page(req, "Permission denied")
        return

    if token != None and get_uvm_settings_item('system','cloudEnabled'):
        if login_tools.valid_token(token):
            sess = Session.Session(req, lock=0)
            sess.lock()
            sess.set_timeout(uvm_login.SESSION_TIMEOUT)
            uvm_login.save_session_user(sess, realm, "token")
            sess.save()
            sess.unlock()

            if url == None:
                return apache.OK
            else:
                url = re.sub('[^A-Za-z0-9-_/.#?=]','',url) # sanitize input
                if 'fragment' in form and form['fragment'] != '':
                    url = url + form['fragment']
                util.redirect(req, url)
                return

    if 'username' in form and 'password' in form:
        username = form['username']
        password = form['password']

        if login_tools.valid_login(req, realm, username, password):
            sess = Session.Session(req, lock=0)
            sess.lock()
            sess.set_timeout(uvm_login.SESSION_TIMEOUT)
            uvm_login.save_session_user(sess, realm, username)
            sess.save()
            sess.unlock()

            if url == None:
                return apache.OK
            else:
                url = re.sub('[^A-Za-z0-9-_/.#?=]','',url) # sanitize input
                if 'fragment' in form and form['fragment'] != '':
                    url = url + form['fragment']
                util.redirect(req, url)
                return

    company_name = uvm_login.get_company_name()
    title = _("Administrator Login")
    # some i18n company_names cause exception here, so wrap to handle this
    # revert to "Administrator Login" if exception occurs
    try:
        title = html.escape(_("%s Administrator Login") % company_name)
    except:
        pass

    host = html.escape(req.hostname)

    login_tools.write_login_form(req, title, host, error_msg)

def logout(req, url=None, realm='Administrator'):
    sess = Session.Session(req, lock=0)
    sess.lock()
    sess.set_timeout(uvm_login.SESSION_TIMEOUT)
    uvm_login.delete_session_user(sess, realm)
    sess.save()
    sess.unlock()

    if url == None:
        return apache.OK
    else:
        url = re.sub('[^A-Za-z0-9-_/.#?=]','',url) # sanitize input
        util.redirect(req, url)
        return

# internal methods ------------------------------------------------------------


def write_error_page(req, msg):
    req.content_type = "text/html; charset=utf-8"
    req.send_http_header()

    us = _("Server")
    try:
        us = _("%s Server") % get_company_name()
    except:
        pass

    if not type(us) is str:
        us = us.encode("utf-8")
    if not type(msg) is str:
        msg = msg.encode("utf-8")

    html_string = """\
<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">
<html xmlns=\"http://www.w3.org/1999/xhtml\">
<head>
<title>%s</title>
<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />
<style type=\"text/css\">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
</head>
<body>
<div id=\"main\" style=\"width:500px;margin:50px auto 0 auto;\">
<div class=\"main-top-left\"></div><div class=\"main-top-right\"></div><div class=\"main-mid-left\"><div class=\"main-mid-right\"><div class=\"main-mid\">
<center>
<img alt=\"\" src=\"/images/BrandingLogo.png\" style=\"max-width: 150px; max-height: 140px;\"/><br /><br />
<b>%s</b><br /><br />
<em>%s</em>
</center><br /><br />
</div></div></div><div class=\"main-bot-left\"></div><div class=\"main-bot-right\"></div>
</div>
</body>
</html>
""" % (us, us, html.escape(msg))

    req.write(html_string)
