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

from mod_python import apache, Session, util
from psycopg2 import connect


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
def get_node_settings_item(a,b):
    return None
def get_uvm_settings_item(a,b):
    return None

try:
    from uvm.settings_reader import get_settings_item
    from uvm.settings_reader import get_node_settings_item
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
        dict = util.FieldStorage(req)
        if dict.has_key('token'):
            token = dict['token']

    sess = Session.Session(req, lock=0)
    sess.set_timeout(SESSION_TIMEOUT)

    sess.lock()

    username = session_user(sess, realm)

    if None == username and realm == 'Reports':
        username = session_user(sess, 'Administrator')

    if None == username and realm == 'SetupWizard':
        username = session_user(sess, 'Administrator')

    if None == username and realm == 'SetupWizard' and not is_wizard_complete():
        username = 'setupwizard'
        save_session_user(sess, realm, username)

    if None == username and is_local_process_uid_authorized(req):
        username = 'localadmin'
        log_login(req, username, True, True, None)
        save_session_user(sess, realm, username)

    sess.save()
    sess.unlock()

    if None != username:
        pw = base64.encodestring('%s' % username).strip()
        req.headers_in['Authorization'] = "BASIC % s" % pw
        req.notes['authorized'] = 'true'
        return apache.OK
    else:
        # we only do this as to not present a login screen when access
        # is restricted. a tomcat valve enforces this setting.
        if options.get('UseRemoteAccessSettings', 'no') == 'yes':
            http_enabled = get_uvm_settings_item('system','httpAdministrationAllowed')
            connection = req.connection

            (addr, port) = connection.local_addr
            if not re.match('127\.', connection.remote_ip):
                if port == 80 and not http_enabled:
                    return apache.HTTP_FORBIDDEN

        apache.log_error('Auth failure [Username not specified]. Redirecting to auth page. (realm: %s)' % realm)
        login_redirect(req, realm, token)

def session_user(sess, realm):
    if sess.has_key('apache_realms') and sess['apache_realms'].has_key(realm):
        realm_record = sess['apache_realms'][realm]

        if realm_record != None and realm_record.has_key('username'):
            return realm_record['username']

    return None

def is_wizard_complete():
    if not os.path.exists('/usr/share/untangle/conf/wizard.js'):
        return False

    wizardComplete = get_settings_item("/usr/share/untangle/conf/wizard.js","wizardComplete")
    if wizardComplete != None:
        return wizardComplete
    return False

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
    # There is some race condition in the kernel and sometimes the socket we are looking for will not show up on the first read
    # This loop hack appears to "fix" the issue as it always shows up on the second read if the first fails.
    #
    # Also sometimes /proc/net/tcp reports the incorrect UID. 
    # As a result, we must also try again if the UID is not authorized
    uid = None
    for count in range(0,5):
        # if count > 0:
        #     apache.log_error('Failed to find/authorize UID [%s, %s:%s], attempting again... (try: %i)' % ( str(uid), str(remote_ip), str(remote_port), (count+1) ) )
        try:
            infile = open('/proc/net/tcp', 'r')
            for l in infile.readlines():
                a = l.split()
                if len(a) <= 2:
                    continue

                p = a[1].split(':')
                if len(p) != 2:
                    continue

                if p[0] == hexaddr and p[1] == hexport:
                    try:
                        uid = int(a[7])
                        
                        # Found the UID
                        # if its in the list of enabled UIDs
                        if uid in uids:
                            return True
                    except:
                        apache.log_error('Bad UID: %s' % a[7])

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
        redirect_url = '/auth/login?url=%s&realm=%s&token=%s' % (url, realm_str, token)
    else:
        redirect_url = '/auth/login?url=%s&realm=%s' % (url, realm_str)
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

    realm_record = {}
    realm_record['username'] = username
    apache_realms[realm] = realm_record

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

    oemName = get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if oemName != None:
        company = oemName

    brandco = get_node_settings_item('untangle-node-branding-manager','companyName')
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

def log_login(req, login, local, succeeded, reason):
    (client_addr, client_port) = req.connection.remote_addr
    conn = None
    try:
        conn = connect("dbname=uvm user=postgres")
        curs = conn.cursor()
        sql = ""
        if reason != None and succeeded == False:
            sql = "INSERT INTO reports.admin_logins (client_addr, login, local, succeeded, reason, time_stamp) VALUES ('%s', '%s', '%s', '%s', '%s', now())" % (client_addr, login, local, succeeded, reason)
        else:
            sql = "INSERT INTO reports.admin_logins (client_addr, login, local, succeeded, time_stamp) VALUES ('%s', '%s', '%s', '%s', now())" % (client_addr, login, local, succeeded)
        curs.execute(sql);
        conn.commit()
    except Exception, e:
        apache.log_error('Log Exception %s' % e)
        pass
    finally:
        if (conn != None):
            conn.close()

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
<img alt=\"\" src=\"/images/BrandingLogo.png\" /><br /><br />
<b>%s</b><br /><br />
<em>%s</em>
</center><br /><br />
</div></div></div><div class=\"main-bot-left\"></div><div class=\"main-bot-right\"></div>
</div>
</body>
</html>
""" % (us, us, cgi.escape(msg))

    req.write(html)
