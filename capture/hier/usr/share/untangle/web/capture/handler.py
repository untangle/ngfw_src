from mod_python import apache
import pprint

from uvm import Uvm

#-----------------------------------------------------------------------------

def index(req):
    # get the original destination and other arguments passed
    # in the URL when the redirect was generated
    args = split_args(req.args);
    if (not 'METHOD' in args):   args['METHOD'] = "Empty"
    if (not 'NONCE' in args):   args['NONCE'] = "Empty"
    if (not 'APPID' in args):   args['APPID'] = "Empty"
    if (not 'HOST' in args):    args['HOST'] = "Empty"
    if (not 'URI' in args):   args['URI'] = "Empty"

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

def login(req,username,password,method,nonce,appid,host,uri):
    page = "<HTML><HEAD><TITLE>Testing</TITLE></HEAD><BODY>\r\n"
    page += "USERNAME: " + username + "<BR>\r\n"
    page += "PASSWORD: " + password + "<BR>\r\n"
    page += "METHOD: " + method + "<BR>\r\n"
    page += "NONCE: " + nonce + "<BR>\r\n"
    page += "APPID: " + appid + "<BR>\r\n"
    page += "HOST: " + host + "<BR>\r\n"
    page += "URI: " + uri + "<BR>\r\n"
    page += "</BODY></HTML>\r\n"
    return(page)

#-----------------------------------------------------------------------------

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
