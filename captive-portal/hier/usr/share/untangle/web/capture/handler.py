from mod_python import apache
from mod_python import util
from mod_python import Cookie
from uvm import Uvm
import zipfile
import urllib
import pprint
import imp
import sys
import time
import os
import uvm.i18n_helper

sys.path.insert(0,'@PREFIX@/usr/lib/python%d.%d/' % sys.version_info[:2])

from uvm.settings_reader import get_app_settings_item
from uvm.settings_reader import get_appid_settings
from uvm.settings_reader import get_app_settings
from uvm.settings_reader import get_settings_item

_ = uvm.i18n_helper.get_translation('untangle').lgettext

## PythonOption ApplicationPath /
#mod_python.session.application_path /

PSOCookieSession_Path = "/"

class HandlerCookie:
    cookie_key = "__ngfwcp"
    def __init__(self,req,appid=None):
        self.req = req
        if appid == None:
            args = split_args(self.req.args);
            appid = args['APPID']
        self.captureSettings = load_capture_settings(self.req,appid)
        self.cookie = Cookie.get_cookies(self.req, Cookie.MarshalCookie, secret=str(self.captureSettings["secretKey"]))

    def is_valid(self):
        if self.get_field("username") != None:
            return True

        return False

    def get_field(self,key):
        value = self.cookie.get(self.cookie_key, None)
        if value:
            if ((type(value) is Cookie.MarshalCookie) and
                key in value.value ):
                return value.value[key]
        return None

    def set(self,username):
        value = {
            "username": username
        }
        cookie = Cookie.MarshalCookie(self.cookie_key, value, secret=str(self.captureSettings["secretKey"]))
        cookie.path = "/"
        cookie.expires = time.time() + int(self.captureSettings["sessionCookiesTimeout"])
        Cookie.add_cookie(self.req, cookie)

    def expire(self):
        value = {}
        cookie = Cookie.MarshalCookie(self.cookie_key, value, secret=str(self.captureSettings["secretKey"]))
        cookie.path = "/"
        cookie.expires = 0
        Cookie.add_cookie(self.req, cookie)

#-----------------------------------------------------------------------------
# This is the default function that gets called when a client is redirected
# to the captive portal because they have not yet authenticated.

def index(req):

    # get the original destination and other arguments passed
    # in the URL when the redirect was generated
    args = split_args(req.args);
    if (not 'AUTHCODE' in args): args['AUTHCODE'] = "Empty"
    if (not 'AUTHMODE' in args): args['AUTHMODE'] = "Empty"
    if (not 'METHOD' in args):   args['METHOD'] = "Empty"
    if (not 'NONCE' in args):    args['NONCE'] = "Empty"
    if (not 'APPID' in args):    args['APPID'] = "Empty"
    if (not 'HOST' in args):     args['HOST'] = "Empty"
    if (not 'URI' in args):      args['URI'] = "Empty"

    # load the configuration data
    appid = args['APPID']
    captureSettings = load_capture_settings(req,appid)
    captureApp = None

    authcode = args['AUTHCODE']
    authmode = args['AUTHMODE']

    if (authcode != "Empty"):
        if (captureSettings.get("authenticationType") == "GOOGLE") or ((captureSettings.get("authenticationType") == "ANY_OAUTH") and (authmode == "GOOGLE")):
            # Here we call the relay server with the authcode that was returned to the client
            # This will confirm the user is actually authenticated and return the email address
            altres = urllib.urlopen("https://auth-relay.untangle.com/cgi-bin/getAccessToken?authType=GOOGLE&authCode=%s" % authcode)
            altraw = altres.read()

            if ("ERROR:" in altraw):
                page = "<HTML><HEAD><TITLE>Login Failure</TITLE></HEAD><BODY><H1>" + altraw + "</H1></BODY></HTML>"
                return(page)

            nonce = args['NONCE']
            host = args['HOST']
            uri = args['URI']
            raw = urllib.unquote(uri).decode('utf8')
            address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)
            if captureApp == None:
                captureApp = load_rpc_manager(appid)
            captureApp.googleLogin(address,altraw)
            redirectUrl = captureSettings.get('redirectUrl')
            if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
                target = str(redirectUrl)
            else:
                if ((host == 'Empty') or (uri == 'Empty')):
                    page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                    return(page)
                raw = urllib.unquote(uri).decode('utf8')
                if (nonce == 'a1b2c3d4e5f6'):
                    target = str("https://" + host + raw)
                else:
                    target = str("http://" + host + raw)
            util.redirect(req, target)
            return

        if (captureSettings.get("authenticationType") == "FACEBOOK") or ((captureSettings.get("authenticationType") == "ANY_OAUTH") and (authmode == "FACEBOOK")):
            # Here we call the relay server with the authcode that was returned to the client
            # This will confirm the user is actually authenticated and return the email address
            altres = urllib.urlopen("https://auth-relay.untangle.com/cgi-bin/getAccessToken?authType=FACEBOOK&authCode=%s" % authcode)
            altraw = altres.read()

            if ("ERROR:" in altraw):
                page = "<HTML><HEAD><TITLE>Login Failure</TITLE></HEAD><BODY><H1>" + altraw + "</H1></BODY></HTML>"
                return(page)

            nonce = args['NONCE']
            host = args['HOST']
            uri = args['URI']
            raw = urllib.unquote(uri).decode('utf8')
            address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)
            if captureApp == None:
                captureApp = load_rpc_manager(appid)
            captureApp.facebookLogin(address,altraw)
            redirectUrl = captureSettings.get('redirectUrl')
            if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
                target = str(redirectUrl)
            else:
                if ((host == 'Empty') or (uri == 'Empty')):
                    page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                    return(page)
                raw = urllib.unquote(uri).decode('utf8')
                if (nonce == 'a1b2c3d4e5f6'):
                    target = str("https://" + host + raw)
                else:
                    target = str("http://" + host + raw)
            util.redirect(req, target)
            return

        if (captureSettings.get("authenticationType") == "MICROSOFT") or ((captureSettings.get("authenticationType") == "ANY_OAUTH") and (authmode == "MICROSOFT")):
            # Here we call the relay server with the authcode that was returned to the client
            # This will confirm the user is actually authenticated and return the email address
            altres = urllib.urlopen("https://auth-relay.untangle.com/cgi-bin/getAccessToken?authType=MICROSOFT&authCode=%s" % authcode)
            altraw = altres.read()

            if ("ERROR:" in altraw):
                page = "<HTML><HEAD><TITLE>Login Failure</TITLE></HEAD><BODY><H1>" + altraw + "</H1></BODY></HTML>"
                return(page)

            nonce = args['NONCE']
            host = args['HOST']
            uri = args['URI']
            raw = urllib.unquote(uri).decode('utf8')
            address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)
            if captureApp == None:
                captureApp = load_rpc_manager(appid)
            captureApp.microsoftLogin(address,altraw)
            redirectUrl = captureSettings.get('redirectUrl')
            if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
                target = str(redirectUrl)
            else:
                if ((host == 'Empty') or (uri == 'Empty')):
                    page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                    return(page)
                raw = urllib.unquote(uri).decode('utf8')
                if (nonce == 'a1b2c3d4e5f6'):
                    target = str("https://" + host + raw)
                else:
                    target = str("http://" + host + raw)
            util.redirect(req, target)
            return

    # if configured for any OAuth provider create and return the selection page
    if (captureSettings.get("authenticationType") == "ANY_OAUTH"):
        page = generate_page(req,captureSettings,args)
        return(page)

    if captureSettings.get("sessionCookiesEnabled") == True and 'Cookie' in req.headers_in:
        cookie = HandlerCookie(req)
        if cookie.get_field("username") != None:
            # Process cookie if exists.
            address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

            if captureApp == None:
                captureApp = load_rpc_manager(appid)
            if captureApp.isUserInCookieTable(address,cookie.get_field("username")):
                # User was found in expired cookie table.
                captureApp.removeUserFromCookieTable(address)
                cookie.expire()
            elif ((cookie != None) and
                (cookie.is_valid() == True) and
                (captureApp.userLogin(address,cookie.get_field("username")) == 0)):
                # Cookie checks out.  Active them, let them through.
                redirectUrl = captureSettings.get('redirectUrl')
                if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
                    target = str(redirectUrl)
                else:
                    nonce = args['NONCE']
                    host = args['HOST']
                    uri = args['URI']
                    raw = urllib.unquote(uri).decode('utf8')
                    if ((host == 'Empty') or (uri == 'Empty')):
                        page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                        return(page)
                    if (nonce == 'a1b2c3d4e5f6'):
                        target = str("https://" + host + raw)
                    else:
                        target = str("http://" + host + raw)
                util.redirect(req, target)
                return

    # if not using a custom capture page we generate and return a standard page
    if (captureSettings.get('pageType') != 'CUSTOM'):
        page = generate_page(req,captureSettings,args)
        return(page)

    # if we make it here they are using a custom page so we have to
    # look to see if they are also using a custom.py script
    rawpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/"
    webpath = "/capture/custom_" + str(args['APPID']) + "/"

    # found a custom.py file so load it up, grab the index function reference
    # and call the index function to generate the capture page
    if (os.path.exists(rawpath + "custom.py")):
        cust = import_file(rawpath + "custom.py")
        if not cust:
            raise Exception("Unable to locate or import custom.py")
        func = getattr(cust,"index")
        if not func:
            raise Exception("Unable to locate index function in custom.py")
        if not hasattr(func,'__call__'):
            raise Exception("The index in custom.py is not a callable function")
        page = func(req,rawpath,webpath,str(args['APPID']),str(args['HOST']),str(args['URI']))
    # no custom.py file so we generate the capture page ourselves
    else:
        page = generate_page(req,captureSettings,args)

    # return the capture page we just created
    return(page)

#-----------------------------------------------------------------------------
# Called as a POST method by authpage.html when the Login button is clicked.
# Arguments include username and password along with several hidden fields
# that store the details of the page originally requested.

def authpost(req,username,password,method,nonce,appid,host,uri):
    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # load the app settings
    captureSettings = load_capture_settings(req,appid)

    # setup the uvm and retrieve the app object so we can make the RPC call
    captureApp = load_rpc_manager(appid)

    # call the app to authenticate the user
    authResult = captureApp.userAuthenticate(address, username, urllib.quote(password))

    # on successful login redirect to the redirectUrl if not empty
    # otherwise send them to the page originally requested
    if (authResult == 0):
        if captureSettings.get("sessionCookiesEnabled") == True:
            # Hand the user a cookie
            cookie = HandlerCookie(req,appid)
            cookie.set(username)
        redirectUrl = captureSettings.get('redirectUrl')
        if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
            target = str(redirectUrl)
        else:
            if ((host == 'Empty') or (uri == 'Empty')):
                page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                return(page)
            raw = urllib.unquote(uri).decode('utf8')
            if (nonce == 'a1b2c3d4e5f6'):
                target = str("https://" + host + raw)
            else:
                target = str("http://" + host + raw)
        util.redirect(req, target)
        return

    # authentication failed so re-create the list of args that
    # we can pass to the login page generator
    args = {}
    args['METHOD'] = method
    args['NONCE'] = nonce
    args['APPID'] = appid
    args['HOST'] = host
    args['URI'] = uri

    # pass the request object and post arguments to the page generator
    if (authResult == 1):
        page = generate_page(req,captureSettings,args, _("Invalid username or password. Please try again.") )
    elif (authResult == 2):
        page = generate_page(req,captureSettings,args, _("You are already logged in from another location.") )
    else:
        page = generate_page(req,captureSettings,args, _("The server returned an unexpected error.") )

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# Called as a POST method by infopage.html when the Continue button is clicked.
# Static arguments are the hidden fields that store the details of the page
# originally requested.  The agree field is special.  When no agree box is
# included on the page, it is configured as a hidden field with the value
# set to 'agree'.  When the agree box is enabled, it is configured as a checkbox
# which will return 'agree' if checked.  If unchecked, it will not be included
# in the POST data.  To handle this scenario, we use a function parameter
# default of 'empty' which will cause app.userActivate to return false.

def infopost(req,method,nonce,appid,host,uri,agree='empty'):

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # load the app settings
    captureSettings = load_capture_settings(req,appid)

    # setup the uvm and app objects so we can make the RPC call
    captureApp = load_rpc_manager(appid)

    # call the app to authenticate the user
    authResult = captureApp.userActivate(address,agree)

    # on successful login redirect to the redirectUrl if not empty
    # otherwise send them to the page originally requested
    if (authResult == 0):
        redirectUrl = captureSettings.get('redirectUrl')
        if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
            target = str(redirectUrl)
        else:
            if ((host == 'Empty') or (uri == 'Empty')):
                page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                return(page)
            raw = urllib.unquote(uri).decode('utf8')
            if (nonce == 'a1b2c3d4e5f6'):
                target = str("https://" + host + raw)
            else:
                target = str("http://" + host + raw)
        util.redirect(req, target)
        return

    # authentication failed so re-create the list of args that
    # we can pass to the login page generator
    args = {}
    args['METHOD'] = method
    args['NONCE'] = nonce
    args['APPID'] = appid
    args['HOST'] = host
    args['URI'] = uri

    # pass the request object and post arguments to the page generator
    if (authResult == 1):
        page = generate_page(req,captureSettings,args, _("You must enable the checkbox above to continue.") )
    else:
        page = generate_page(req,captureSettings,args, _("The server returned an unexpected error.") )

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# This function generates the actual captive portal page

def generate_page(req,captureSettings,args,extra=''):

    # We use the path from the request filename to locate the correct template
    # and start with the OAuth selection page if that authentication type is
    # enabled. Otherwise we use the configured page type to decide.

    if (captureSettings.get("authenticationType") == "ANY_OAUTH"):
        name = req.filename[:req.filename.rindex('/')] + "/pickpage.html"

    elif (captureSettings.get('pageType') == 'BASIC_LOGIN'):
        name = req.filename[:req.filename.rindex('/')] + "/authpage.html"

    elif (captureSettings.get('pageType') == 'BASIC_MESSAGE'):
        name = req.filename[:req.filename.rindex('/')] + "/infopage.html"

    elif (captureSettings.get('pageType') == 'CUSTOM'):
        name = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/custom.html"

    else:
        page = "<html><head><title>Captive Portal Error</title></head><body><h2>Invalid Captive Portal configuration</h2></body></html>"
        return(page)

    webfile = open(name, "r")
    page = webfile.read();
    webfile.close()

    if (not 'certificateDetection' in captureSettings):
        captureSettings['certificateDetection'] = 'DISABLE_DETECTION'

    if captureSettings['certificateDetection'] == 'CHECK_CERTIFICATE':
        page = replace_marker(page,'$.SecureEndpointCheck.$','checkSecureEndpoint(false);')
    elif captureSettings['certificateDetection'] == 'REQUIRE_CERTIFICATE':
        page = replace_marker(page,'$.SecureEndpointCheck.$','checkSecureEndpoint(true);')
    else:
        page = replace_marker(page,'$.SecureEndpointCheck.$','')

    if (captureSettings.get('pageType') == 'BASIC_LOGIN'):
        page = replace_marker(page,'$.CompanyName.$', captureSettings.get('companyName'))
        page = replace_marker(page,'$.PageTitle.$', captureSettings.get('basicLoginPageTitle'))
        page = replace_marker(page,'$.WelcomeText.$', captureSettings.get('basicLoginPageWelcome'))
        page = replace_marker(page,'$.MessageText.$', captureSettings.get('basicLoginMessageText'))
        page = replace_marker(page,'$.UserLabel.$', captureSettings.get('basicLoginUsername'))
        page = replace_marker(page,'$.PassLabel.$', captureSettings.get('basicLoginPassword'))
        page = replace_marker(page,'$.FooterText.$', captureSettings.get('basicLoginFooter'))

    if (captureSettings.get('pageType') == 'BASIC_MESSAGE'):
        page = replace_marker(page,'$.CompanyName.$', captureSettings.get('companyName'))
        page = replace_marker(page,'$.PageTitle.$', captureSettings.get('basicMessagePageTitle'))
        page = replace_marker(page,'$.WelcomeText.$', captureSettings.get('basicMessagePageWelcome'))
        page = replace_marker(page,'$.MessageText.$', captureSettings.get('basicMessageMessageText'))
        page = replace_marker(page,'$.FooterText.$', captureSettings.get('basicMessageFooter'))

        if (captureSettings.get('basicMessageAgreeBox') == True):
            page = replace_marker(page,'$.AgreeText.$', captureSettings.get('basicMessageAgreeText'))
            page = replace_marker(page,'$.AgreeBox.$','checkbox')
        else:
            page = replace_marker(page,'$.AgreeText.$', '')
            page = replace_marker(page,'$.AgreeBox.$','hidden')

    if (captureSettings.get('pageType') == 'CUSTOM'):
        path = "/capture/custom_" + str(args['APPID'])
        page = replace_marker(page,'$.CustomPath.$',path)

    if (captureSettings.get("authenticationType") == "ANY_OAUTH"):
        uvmContext = Uvm().getUvmContext()
        networkSettings = uvmContext.networkManager().getNetworkSettings()

        target = ""
        port = None

        if (captureSettings.get("alwaysUseSecureCapture" == True)):
            target += "https://"
            if (networkSettings.get('httpsPort') != 443):
                port = str(httpsPort)
        else:
            target += "http://"
            if (networkSettings.get('httpPort') != 80):
                port = str(httpPort)

        target += req.hostname
        if (port != None):
            target += ":"
            target += port

        target += "/capture/handler.py/index"
        target += "?nonce=" + args['NONCE']
        target += "&method=" + args['METHOD']
        target += "&appid=" + args['APPID']
        target += "&host=" + args['HOST']
        target += "&uri=" + args['URI']

        page = replace_marker(page,'$.GoogleState.$', urllib.quote(target + "&authmode=GOOGLE").encode('utf8'))
        page = replace_marker(page,'$.FacebookState.$', urllib.quote(target + "&authmode=FACEBOOK").encode('utf8'))
        page = replace_marker(page,'$.MicrosoftState.$', urllib.quote(target + "&authmode=MICROSOFT").encode('utf8'))

    # plug the values into the hidden form fields of the authentication page
    # page by doing  search and replace for each of the placeholder text tags
    page = replace_marker(page,'$.method.$', args['METHOD'])
    page = replace_marker(page,'$.nonce.$', args['NONCE'])
    page = replace_marker(page,'$.appid.$', args['APPID'])
    page = replace_marker(page,'$.host.$', args['HOST'])
    page = replace_marker(page,'$.uri.$', args['URI'])

    # replace the text in the problem section with the agumented value
    page = replace_marker(page,'$.ProblemText.$',extra)

    # debug = create_debug(args,captureSettings)
    debug = ""
    page = replace_marker(page,'<!--DEBUG-->',debug)

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# Pulls page arguments out of a URI and stores them in a list.

def split_args(args):

    canon_args = {}                     # Start an empty list
    if args == None:                    # Return the empty list if no args
        return(canon_args)

    arglist = args.split('&')           # Split into list of name=value strings

    for arg in arglist:                 # Now split each name=value and
        tmp = arg.split('=')            # turn them into sub-lists
        if len(tmp) == 1:               # with name in the first part
            canon_args[tmp[0]] = None   # and value in the second part
        else:
            canon_args[tmp[0].upper()] = tmp[1]
    return(canon_args)

#-----------------------------------------------------------------------------
# loads and returns the app RPC object needed for the authentication calls

def load_rpc_manager(appid=None):

    # first we get the uvm context
    uvmContext = Uvm().getUvmContext()

    # if no appid provided we lookup capture app by name
    # otherwise we use the appid passed to us
    if (appid == None):
        captureApp = uvmContext.appManager().app("captive-portal")
    else:
        captureApp = uvmContext.appManager().app(int(appid))

    # if we can't find the app then throw an exception
    if (captureApp == None):
        raise Exception("The uvm app manager could not locate captive-portal")

    return(captureApp)

#-----------------------------------------------------------------------------
# loads the app settings

def load_capture_settings(req,appid=None):

    captureSettings = None

    # start with our company name
    companyName = 'Untangle'

    # if there is an OEM name configured we use that instead of our company name
    oemName = get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if (oemName != None):
        companyName = oemName

    # if there is a company name in the branding manager it wins over everything else
    brandco = get_app_settings_item('branding-manager','companyName')
    if (brandco != None):
        companyName = brandco

    try:
        if (appid == None):
            captureSettings = get_app_settings('captive-portal')
        else:
            captureSettings = get_appid_settings(int(appid))
    except Exception as e:
        req.log_error("handler.py: Exception loading settings: %s" % str(e))

    if (captureSettings == None):
        req.log_error("handler.py: Unable to load capture settings for appid: %s" % str(appid))
        return None
    if (captureSettings.get('pageType') == None):
        req.log_error("handler.py: Missing required setting: pageType")
        return None

    # add the company name to the app settings dictionary
    captureSettings['companyName'] = companyName

    # add some headers to prevent caching any of our stuff
    req.headers_out.add("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0")
    req.headers_out.add("Pragma", "no-cache")
    req.headers_out.add("Expires", "Mon, 10 Jan 2000 00:00:00 GMT")
    req.headers_out.add("Connection", "close")

    return(captureSettings)

#-----------------------------------------------------------------------------
# builds a string of debug info

def create_debug(args,captureSettings):

    debug = "<BR><HR><BR>";
    debug += "<BR>===== ARGUMENTS =====<BR>\r\n"
    debug += pprint.pformat(args)
    debug += "<BR>===== CAPTURE SETTINGS =====<BR>\r\n"
    debug += pprint.pformat(captureSettings)
    return(debug)

#-----------------------------------------------------------------------------
# generates a simply reply object that the extjs script uses to determine
# the status of a custom upload form post request

def extjs_reply(status,message,filename=""):

    if (status == True):
        result = "{success:true,msg:\"%s\",filename:\"%s\"}" % (message,filename)
    else:
        result = "{success:false,msg:\"%s\",filename:\"%s\"}" % (message,filename)

    return(result)

#-----------------------------------------------------------------------------
# forces stuff loaded from settings files to be UTF-8 when plugged
# into the page template files

def replace_marker(page,marker,output):

    if not type(output) is str:
        output = output.encode("utf-8")

    page = page.replace(marker,output)

    return(page)

#-----------------------------------------------------------------------------
# handler for custom.py integration which dynamically loads the custom.py
# script and calls the handler function passing the apache request object,
# full path to the custom files, and the appid.

def custom_handler(req):

    # first we need to extract the args so we can find the appid
    args = split_args(req.args);

    # make sure we have a valid appid
    if (not 'APPID' in args):
        raise Exception("The appid argument was not passed to custom_hander")

    # construct the absolute and relative paths to the custom files
    rawpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/"
    webpath = "/capture/custom_" + str(args['APPID']) + "/"

    # import the custom.py
    cust = import_file(rawpath + "custom")
    if not cust:
        raise Exception("Unable to locate or import custom.py")

    # get a reference to the handler function
    func = getattr(cust,"handler")
    if not func:
        raise Exception("Unable to locate handler function in custom.py")
    if not hasattr(func,'__call__'):
        raise Exception("The handler in custom.py is not a callable function")

    # call the handler and return anything we get back
    page = func(req,rawpath,webpath,str(args['APPID']))
    return(page)

#-----------------------------------------------------------------------------
def import_file(filename):
    (path, name) = os.path.split(filename)
    (name, ext) = os.path.splitext(name)
    (file, filename, data) = imp.find_module(name, [path])
    return imp.load_module(name, file, filename, data)
