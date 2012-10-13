from mod_python import apache
from mod_python import util
import pprint

from uvm import Uvm

#-----------------------------------------------------------------------------
# This is the default function that gets called when a client is redirected
# to the captive portal because they have not yet authenticated.

def index(req):

    # get the original destination and other arguments passed
    # in the URL when the redirect was generated
    args = split_args(req.args);
    if (not 'METHOD' in args):  args['METHOD'] = "Empty"
    if (not 'NONCE' in args):   args['NONCE'] = "Empty"
    if (not 'APPID' in args):   args['APPID'] = "Empty"
    if (not 'HOST' in args):    args['HOST'] = "Empty"
    if (not 'URI' in args):     args['URI'] = "Empty"

    # pass the reqest object and arguments to the page generator
    page = generate_page(req,args)

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# Called as a POST method by authpage.html when the Login button is clicked.
# Arguments include username and password along with several hidden fields
# that store the details of the page originally requested.

def authpost(req,username,password,method,nonce,appid,host,uri):

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # call the node to authenticate the user
    uvmContext = Uvm().getUvmContext()
    captureNode = uvmContext.nodeManager().node(long(appid))
    captureSettings = captureNode.getSettings()
    authResult = captureNode.userAuthenticate(address, username, password)

    # on successful login redirect to the redirectUrl if not empty
    # otherwise send them to the page originally requested
    if (authResult == 0):
        if (len(captureSettings['redirectUrl']) != 0):
            target = str(captureSettings['redirectUrl'])
        else:
            target = str("http://" + host + uri)

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
        page = generate_page(req,args,"Invalid username or password. Please try again.")
    elif (authResult == 2):
        page = generate_page(req,args,"You are already logged in from another location.")
    else:
        page = generate_page(req,args,"The server returned an unexpected error.")

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
# default of 'empty' which will cause node.userActivate to return false.

def infopost(req,method,nonce,appid,host,uri,agree='empty'):

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # call the node to authenticate the user
    uvmContext = Uvm().getUvmContext()
    captureNode = uvmContext.nodeManager().node(long(appid))
    captureSettings = captureNode.getSettings()
    authResult = captureNode.userActivate(address,agree)

    # on successful login redirect to the redirectUrl if not empty
    # otherwise send them to the page originally requested
    if (authResult == 0):
        if (len(captureSettings['redirectUrl']) != 0):
            target = str(captureSettings['redirectUrl'])
        else:
            target = str("http://" + host + uri)
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
        page = generate_page(req,args,"You must enable the checkbox above to continue.")
    else:
        page = generate_page(req,args,"The server returned an unexpected error.")

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# This function generates the actual

def generate_page(req,args,extra=''):

    # read the node and branding settings from the uvm
    uvmContext = Uvm().getUvmContext()
    captureNode = uvmContext.nodeManager().node(long(args['APPID']))
    brandingNode = uvmContext.nodeManager().node("untangle-node-branding");

    if (captureNode == None):
        captureSettings = None
    else:
        captureSettings = captureNode.getSettings()

    if (brandingNode == None):
        brandingSettings = {}
        brandingSettings['companyName'] = 'Untangle'
    else:
        brandingSettings = brandingNode.getSettings()

    # use the path from the request filename to locate the correct template
    if (captureSettings['pageType'] == 'BASIC_LOGIN'): name = req.filename[:req.filename.rindex('/')] + "/authpage.html"
    if (captureSettings['pageType'] == 'BASIC_MESSAGE'): name = req.filename[:req.filename.rindex('/')] + "/infopage.html"
    if (captureSettings['pageType'] == 'CUSTOM'): name = req.filename[:req.filename.rindex('/')] + "/custom.html"
    file = open(name, "r")
    page = file.read();
    file.close()

    if (captureSettings['pageType'] == 'BASIC_LOGIN'):
        page = page.replace('$.CompanyName.$', brandingSettings['companyName'])
        page = page.replace('$.PageTitle.$', captureSettings['basicLoginPageTitle'])
        page = page.replace('$.WelcomeText.$', captureSettings['basicLoginPageWelcome'])
        page = page.replace('$.MessageText.$', captureSettings['basicLoginMessageText'])
        page = page.replace('$.UserLabel.$', captureSettings['basicLoginUsername'])
        page = page.replace('$.PassLabel.$', captureSettings['basicLoginPassword'])
        page = page.replace('$.FooterText.$', captureSettings['basicLoginFooter'])

    if (captureSettings['pageType'] == 'BASIC_MESSAGE'):
        page = page.replace('$.CompanyName.$', brandingSettings['companyName'])
        page = page.replace('$.PageTitle.$', captureSettings['basicMessagePageTitle'])
        page = page.replace('$.WelcomeText.$', captureSettings['basicMessagePageWelcome'])
        page = page.replace('$.MessageText.$', captureSettings['basicMessageMessageText'])
        page = page.replace('$.FooterText.$', captureSettings['basicMessageFooter'])

        if (captureSettings['basicMessageAgreeBox'] == True):
            page = page.replace('$.AgreeText.$', captureSettings['basicMessageAgreeText'])
            page = page.replace('$.AgreeBox.$','checkbox')
        else:
            page = page.replace('$.AgreeText.$', '')
            page = page.replace('$.AgreeBox.$','hidden')

    # plug the values into the hidden form fields of the authentication page
    # page by doing  search and replace for each of the placeholder text tags
    page = page.replace('$.method.$', args['METHOD'])
    page = page.replace('$.nonce.$', args['NONCE'])
    page = page.replace('$.appid.$', args['APPID'])
    page = page.replace('$.host.$', args['HOST'])
    page = page.replace('$.uri.$', args['URI'])

    # replace the text in the problem section with the agumented value
    page = page.replace('$.ProblemText.$',extra)

    debug = "<BR><HR><BR>";
    debug += "<BR>===== ARGUMENTS =====<BR>\r\n"
    debug += pprint.pformat(args)
    debug +="<BR>===== CAPTURE SETTINGS =====<BR>\r\n"
    debug += pprint.pformat(captureSettings)
    debug += "<BR>===== BRANDING SETTINGS =====<BR>\r\n"
    debug += pprint.pformat(brandingSettings)
    page = page.replace('<!--DEBUG-->', debug)

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# Pulls page arguments out of a URI and stores them in a list.

def split_args(args):

    canon_args = {}                     # Start an empty list
    if args == None:                    # Return the empty list if no args
        return canon_args

    arglist = args.split('&')           # Split into list of name=value strings

    for arg in arglist:                 # Now split each name=value and
        tmp = arg.split('=')            # turn them into sub-lists
        if len(tmp) == 1:               # with name in the first part
            canon_args[tmp[0]] = None   # and value in the second part
        else:
            canon_args[tmp[0].upper()] = tmp[1]
    return canon_args

#-----------------------------------------------------------------------------
