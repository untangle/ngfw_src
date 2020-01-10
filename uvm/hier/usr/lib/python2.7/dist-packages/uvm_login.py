# $Id: uvm_login.py 37268 2014-02-26 23:43:48Z dmorris $

import base64
import cgi
import gettext
import grp
import os
import pwd
import re
import sets
import urllib
import os.path
import sys
import traceback
import uvm
import threading

from mod_python import apache, Session, util

def authenhandler(req):
    if req.notes.get('authorized', 'false') == 'true':
        return apache.OK
    else:
        options = req.get_options()

        if options.has_key('Realm'):
            realm = options['Realm']
            apache.log_error('Auth failure [Not authorized]. Redirecting to auth page. (realm: %s)' % realm)
            apache.log_error('Not logged in. Redirect to auth page. (realm: %s)' % realm)
            login_redirect(req, realm)
        else:
            apache.log_error('Auth failure [No realm specified]. Redirecting to auth page.')
            return apache.DECLINED

def get_settings_item(a,b):
    return None
def get_app_settings_item(a,b):
    return None
def get_uvm_settings_item(a,b):
    return None

try:
    from uvm.settings_reader import get_settings_item
    from uvm.settings_reader import get_app_settings_item
    from uvm.settings_reader import get_uvm_settings_item
except ImportError:
    pass

SESSION_TIMEOUT = 1800

def headerparserhandler(req):
    options = req.get_options()

    realm = None
    if options.has_key('Realm'):
        realm = options['Realm']
    else:
        apache.log_error('no realm specified')
        return apache.DECLINED

    # if the token is in the URL, extract it and send it to the login page
    token = None
    if req.args != None:
        try:
            dict = util.FieldStorage(req)
            if dict.has_key('token'):
                token = dict['token']
        except:
            pass

    sess = Session.Session(req, lock=0)
    sess.lock()
    sess.set_timeout(SESSION_TIMEOUT)

    username = session_user(sess, realm)

    if None == username and realm == 'Reports':
        username = session_user(sess, 'Administrator')

    if None == username and realm == 'SetupWizard':
        username = session_user(sess, 'Administrator')

    if None == username and realm == 'SetupWizard' and not wizard_password_required():
        username = 'setupwizard'
        save_session_user(sess, realm, username)

    if None == username and is_local_process_uid_authorized(req):
        username = 'localadmin'
        log_login(req, username, True, None)
        save_session_user(sess, realm, username)

    #if sess.has_key('apache_realms'):
    #    apache.log_error('DEBUG apache_realms: %s' % sess['apache_realms'])
    #    if sess['apache_realms'].has_key(realm):
    #        apache.log_error('DEBUG apache_realms[%s]: %s' % (realm, sess['apache_realms'][realm]))
    #else:
    #    apache.log_error('DEBUG apache_realms: %s' % None)

    sess.save()
    sess.unlock()

    if username != None:
        pw = base64.encodestring('%s' % username).strip()
        req.headers_in['Authorization'] = "BASIC % s" % pw
        req.notes['authorized'] = 'true'
        return apache.OK

    apache.log_error('Auth failure [Username not specified]. Redirecting to auth page. (realm: %s)' % realm)
    login_redirect(req, realm, token)

def session_user(sess, realm):
    if sess.has_key('apache_realms') and sess['apache_realms'].has_key(realm):
        realm_record = sess['apache_realms'][realm]

        if realm_record != None and realm_record.has_key('username'):
            return realm_record['username']

    return None

def wizard_password_required():
    # If the wizard settings are missing, do not required the password
    if not os.path.exists("@PREFIX@/usr/share/untangle/conf/wizard.js"):
        return False

    passwordRequired = get_settings_item("@PREFIX@/usr/share/untangle/conf/wizard.js","passwordRequired")
    if passwordRequired:
        return True

    # If the wizard has not been completed, do not require the password
    wizardComplete = get_settings_item("@PREFIX@/usr/share/untangle/conf/wizard.js","wizardComplete")
    if wizardComplete == False:
        return False
    
    return True

def is_local_process_uid_authorized(req):
    (remote_ip, remote_port) = req.connection.remote_addr

    if remote_ip != "127.0.0.1":
        return False

    # This determines the PID of the connecting process
    # and determines if it is from a process who is owned by root
    # or a user in uvm_login group. If so, auto-authenticate it.
    uids = get_uvmlogin_uids()

    q = remote_ip.split(".")
    q.reverse()
    n = reduce(lambda a, b: long(a) * 256 + long(b), q)
    hexaddr = "%08X" % n
    hexport = "%04X" % remote_port

    # We have to attempt to read /proc/net/tcp several times.
    # This file is not immediately updated synchronously, so we must read it a few times until we find the socket in an established state
    uid = None
    for count in range(0,5):
        try:
            infile = open("/proc/net/tcp", "r")
            # for l in infile.read(500000).splitlines():
            for l in infile.readlines():
                a = l.split()
                if len(a) <= 8:
                    continue

                p = a[1].split(':')
                if len(p) != 2:
                    continue

                if p[0] == hexaddr and p[1] == hexport:
                    try:
                        uid = int(a[7])
                        state = a[3]

                        # If socket state == established
                        if state == "01":
                            # If userid is in list of authorized userids
                            if uid in uids:
                                apache.log_error('UID %s authorized as localadmin on via %s:%s' % (str(uid), str(remote_ip), str(remote_port)))
                                # apache.log_error('%s' % (l))
                                return True
                            else:
                                apache.log_error('UID %s NOT authorized on via %s:%s' % (str(uid), str(remote_ip), str(remote_port)))
                                # apache.log_error('%s' % (l))
                                return False
                    except Exception,e:
                        apache.log_error('Bad line in /proc/net/tcp: %s: %s' % (line, traceback.format_exc(e)))

        except Exception,e:
            apache.log_error('Exception reading /proc/net/tcp: %s' % traceback.format_exc(e))
        finally:
            infile.close()

    if uid == None:
        apache.log_error('Failed to lookup PID for %s:%s' % ( str(remote_ip), str(remote_port) ) )
    # This is commented out because its just for debugging
    # This condition occurs regularly when connecting via a local browser
    # else:
    #     apache.log_error('UID not authorized (%i)' % uid )

    return False

# This function will authenticate root (0) and any user in uvmlogin group
def get_uvmlogin_uids():
    s = sets.Set([0])

    try:
        for username in grp.getgrnam('uvmlogin')[3]:
            try:
                s.add(pwd.getpwnam(username)[2])
            except:
                apache.log_error('bad user %s' % username)
    except:
        apache.log_error('could not get group info')

    return s


def login_redirect(req, realm, token=None):
    url = urllib.quote(req.unparsed_uri)

    if realm == "SetupWizard":
        realm = "Administrator"

    realm_str = urllib.quote(realm)

    if token != None:
        redirect_url = "/auth/login?url=%s&realm=%s&token=%s" % (url, realm_str, token)
    else:
        redirect_url = "/auth/login?url=%s&realm=%s" % (url, realm_str)
    util.redirect(req, redirect_url)

def delete_session_user(sess, realm):
    if sess.has_key('apache_realms'):
        apache_realms = sess['apache_realms']
        if realm in apache_realms:
            del apache_realms[realm]

def save_session_user(sess, realm, username):
    if sess.has_key('apache_realms'):
        apache_realms = sess['apache_realms']
    else:
        sess['apache_realms'] = apache_realms = {}

    if not apache_realms.has_key(realm):
        apache_realms[realm] = {}
    apache_realms[realm]['username'] = username

def setup_gettext():
    lang = get_uvm_language()
    try:
        trans = gettext.translation('untangle-apache2-config',
                                    languages=[lang],
                                    fallback=True)
        trans.install()
    except Exception, e:
        apache.log_error('could not install language: %s lang. %s' % (lang, e))
        import __builtin__
        __builtin__.__dict__['_'] = unicode

def get_company_name():
    company = 'Untangle'

    oemName = get_settings_item("@PREFIX@/usr/share/untangle/conf/oem.js","oemName")
    if oemName != None:
        company = oemName

    brandco = get_app_settings_item('branding-manager','companyName')
    if (brandco != None):
        company = brandco

    if not type(company) is str:
        company = company.encode("utf-8")

    return company

def get_uvm_language():
    lang = 'us'

    setval = get_uvm_settings_item('language','language')
    if (setval != None):
        lang = setval

    return lang

def send_login_event(client_addr, login, local, succeeded, reason):
    # localadmin is used for local machine API calls
    # these are not logged
    if login == "localadmin":
        return
    try:
        uvmContext = uvm.Uvm().getUvmContext()
        uvmContext.adminManager().logAdminLoginEvent( str(login), local, str(client_addr), succeeded, reason )
    except Exception, e:
        apache.log_error('error: %s' % repr(e))

def log_login(req, login, succeeded, reason):
    """ Send a login event to the admin login log

    Arguments:
        req -- http request
        login -- the username attempting to log in
        succeeded -- true if the login was successful, false otherwise
        reason -- string conveys the reason the login failed, or None if the login succeeded
    """
    local = False
    (client_addr, client_port) = req.connection.remote_addr

    if client_addr == "127.0.0.1" or client_addr == "::1":
        local = True

    threading.Thread(target=lambda: send_login_event(client_addr, login, local, succeeded, reason)).start()

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

    html = """\
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
""" % (us, us, cgi.escape(msg))

    req.write(html)
