from mod_python import apache

import sys

if "@PREFIX@" != '':
    sys.path.insert(0, '@PREFIX@/usr/lib/python3/dist-packages')

from uvm import Uvm
from mod_python import Cookie
import uvm.i18n_helper

from uvm import settings_reader

_ = uvm.i18n_helper.get_translation('untangle').lgettext

#-----------------------------------------------------------------------------
# This is the default function that gets called for a client logout request

def index(req):

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # use the path from the request filename to locate the correct template
    name = req.filename[:req.filename.rindex('/')] + "/exitpage.html"
    file = open(name, "r")
    page = file.read();
    file.close()

    # load the app settings
    captureSettings = _load_capture_settings(req)

    # setup the uvm and app objects so we can make the RPC call
    captureList = _load_rpc_manager_list()

    # track the number of successful calls to userLogout
    exitCount = 0

    # call the logout function for each app instance
    cookie_key = "__ngfwcp"
    for app in captureList:
        exitResult = app.userLogout(address)
        cookies = Cookie.get_cookie(req, cookie_key)
        if cookies != None:
            value = {}
            cookie = Cookie.MarshalCookie(cookie_key, value, secret=str(captureSettings["secretKey"]))
            cookie.path = "/"
            cookie.expires = 0
            Cookie.add_cookie(req, cookie)

        if (exitResult == 0):
            exitCount = exitCount + 1

    if (exitCount == 0):
        page = _replace_marker(page,'$.ExitMessage.$', _('You were already logged out') )
        page = _replace_marker(page,'$.ExitStyle.$', 'styleProblem')
    else:
        page = _replace_marker(page,'$.ExitMessage.$', _('You have successfully logged out') )
        page = _replace_marker(page,'$.ExitStyle.$', 'styleNormal')

    page = _replace_marker(page,'$.CompanyName.$', captureSettings['companyName'])
    page = _replace_marker(page,'$.PageTitle.$', captureSettings['basicLoginPageTitle'])

    # return the logout page we just created
    return(page)

#-----------------------------------------------------------------------------
# loads and returns the app RPC objects needed for the authentication calls

def _load_rpc_manager_list(appid=None):

    # create a list for all of the apps we discover
    captureList = list()

    # first we get the uvm context
    uvmContext = Uvm().getUvmContext()

    if (appid == None):
        applist = uvmContext.appManager().appInstancesIds()
        for item in applist['list']:
            app = uvmContext.appManager().app(int(item))
            name = app.getAppSettings()['appName']
            if (name == 'captive-portal'):
                captureList.append(app)
    # appid was passed so use it
    else:
        app = uvmContext.appManager().app(int(appid))
        captureList.append(app)

    # if we can't find the app then throw an exception
    if (len(captureList) == 0):
        raise Exception("The uvm app manager could not locate captive-portal")

    return(captureList)

#-----------------------------------------------------------------------------
# loads the app settings

def _load_capture_settings(req,appid=None):

    companyName = 'Arista'

    oemName = settings_reader.get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if (oemName != None):
        companyName = oemName

    brandco = settings_reader.get_app_settings_item('branding-manager','companyName')
    if (brandco != None):
        companyName = brandco

    if (appid == None):
        captureSettings = settings_reader.get_app_settings('captive-portal')
    else:
        captureSettings = settings_reader.get_appid_settings(int(appid))

    # add the company name to the app settings dictionary
    captureSettings['companyName'] = companyName

    # add some headers to prevent caching any of our stuff
    req.headers_out.add("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0")
    req.headers_out.add("Pragma", "no-cache")
    req.headers_out.add("Expires", "Sat, 1 Jan 2000 00:00:00 GMT");

    return(captureSettings)

#-----------------------------------------------------------------------------
# forces stuff loaded from settings files to be UTF-8 when plugged
# into the page template files

def _replace_marker(page,marker,output):
    if type(marker) == bytes:
        marker = marker.decode("utf-8")
    if type(output) == bytes:
        output = output.decode("utf-8")

    page = page.replace(marker,output)

    return(page)
