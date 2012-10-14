from mod_python import apache
import pprint

from uvm import Uvm

# global objects that we retrieve from the uvm
uvmContext = None
captureNode = None
captureSettings = None
brandingSettings = None

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

    # setup the global variables
    global_setup(req)

    # call the node to logout the user
    exitResult = captureNode.userLogout(address)

    if (exitResult == 0):
        page = page.replace('$.ExitMessage.$', 'You have successfully logged out')
        page = page.replace('$.ExitStyle.$', 'styleNormal')
    else:
        page = page.replace('$.ExitMessage.$', 'You were already logged out')
        page = page.replace('$.ExitStyle.$', 'styleProblem')

    page = page.replace('$.CompanyName.$', brandingSettings['companyName'])
    page = page.replace('$.PageTitle.$', captureSettings['basicLoginPageTitle'])

    # return the logout page we just created
    return(page)

#-----------------------------------------------------------------------------

def global_setup(req,appid=None):

    global uvmContext
    global captureNode
    global captureSettings
    global brandingSettings

    # first we get the uvm context
    uvmContext = Uvm().getUvmContext()

    # if no appid provided we lookup capture node by name
    # otherwise we use the appid passed to us
    if (appid == None):
        captureNode = uvmContext.nodeManager().node("untangle-node-capture")
    else:
        captureNode = uvmContext.nodeManager().node(long(appid))

    # if we can't find the node then we are done
    # otherwise load the node settings
    if (captureNode == None):
        raise Exception("The uvm node manager could not locate untangle-node-capture")
    else:
        captureSettings = captureNode.getSettings()

    # lookup the branding node by name since there should only be one
    brandingNode = uvmContext.nodeManager().node("untangle-node-branding");

    # if branding isn't installed then setup the defaults
    # otherwise load the node settings
    if (brandingNode == None):
        brandingSettings = {}
        brandingSettings['companyName'] = 'Untangle'
    else:
        brandingSettings = brandingNode.getSettings()

    # add some headers to prevent caching any of our stuff
    req.headers_out.add("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0")
    req.headers_out.add("Pragma", "no-cache")
    req.headers_out.add("Expires", "Sat, 1 Jan 2000 00:00:00 GMT");

#-----------------------------------------------------------------------------
