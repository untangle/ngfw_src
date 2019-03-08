# $Id$

import gettext
import cgi
import os
import sys

from mod_python import apache

sys.path.insert(0,'@PREFIX@/usr/lib/python%d.%d/' % sys.version_info[:2])
import uvm_login

gettext.bindtextdomain('untangle-apache2-config')
gettext.textdomain('untangle-apache2-config')
_ = gettext.gettext

# pages -----------------------------------------------------------------------
def isUvmStarting():
    try:
        ret = os.system("/bin/egrep '(launching|booting|starting)' /var/run/uvm.status")
        if ret == 0:
            return True
    except:
        pass
    return False

def status400(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Bad Request"))

def status401(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Unauthorized"))

def status402(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Payment Required"))

def status403(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Forbidden"))

def status404(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Not Found"))

def status405(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Method Not Allowed"))

def status406(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Not Acceptable"))

def status407(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Proxy Authentication Required"))

def status408(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Request Timeout"))

def status409(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Conflict"))

def status410(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Gone"))

def status411(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Length Required"))

def status412(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Precondition Failed"))

def status413(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Request Entity Too Large"))

def status414(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Request-URI Too Long"))

def status415(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Unsupported Media Type"))

def status416(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Requested Range Not Satisfiable"))

def status417(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Expectation Failed"))

def status500(req):
    uvm_login.setup_gettext()
    if isUvmStarting():
        _write_loading_page(req)
        return
    _write_error_page(req, _("Internal Server Error"))

def status501(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Not Implemented"))

def status502(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Bad Gateway"))

def status503(req):
    uvm_login.setup_gettext()
    if isUvmStarting():
        _write_loading_page(req)
        return
    _write_error_page(req, _("Service Unavailable"))

def status504(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("Gateway Timeout"))

def status505(req):
    uvm_login.setup_gettext()
    _write_error_page(req, _("HTTP Version Not Supported"))

# private methods --------------------------------------------------------------

def _write_error_page(req, msg):
    req.content_type = "text/html; charset=utf-8"
    req.send_http_header()

    us = _("Server")
    try:
        us = _("%s Server") % uvm_login.get_company_name()
    except:
        pass

    if not type(us) is str:
        us = us.encode("utf-8")
    if not type(msg) is str:
        msg = msg.encode("utf-8")


    html = """\
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

    <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
    <title>%s</title>
    <script type="text/javascript">if (top.location!=location) top.location.href=document.location.href;</script>
    <style type="text/css">
    /* <![CDATA[ */
    @import url(/images/base.css);
    /* ]]> */
    </style>
    </head>
    <body class="loginPage">
    <div id="main" style="width: 500px; margin: 50px auto 0 auto;">
        <form class="form-signin">
            <center>
                <img style="margin-bottom:10px; max-width: 150px; max-height: 140px;" src="/images/BrandingLogo.png"><br/>
                <span class="form-signin-heading"><strong>%s</strong></span>
             <br/>
                <br/>
                <span class="form-signin-heading"><font color="red"><em>%s</em></font></span>
            </center>
        </form>
    </div>
    </body>
    </html>""" % (us,us, cgi.escape(msg))

    req.write(html)

def _write_loading_page(req):
    req.content_type = "text/html; charset=utf-8"
    req.send_http_header()
    msg = 'Server is starting. Please wait.'

    us = _("Server")
    try:
        us = _("%s Server") % uvm_login.get_company_name()
    except:
        pass

    if not type(us) is str:
        us = us.encode("utf-8")
    if not type(msg) is str:
        msg = msg.encode("utf-8")


    html = """\
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

    <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
    <title>%s</title>
    <script type="text/javascript">if (top.location!=location) top.location.href=document.location.href;</script>
    <script>
    function poll(fn, success_callback, interval) {
        function p() {
            if(fn()) {
                success_callback();
            }
            else {
                setTimeout(p, interval);
            }
        };
        setTimeout(p, interval);
    }
    var numPeriod = 0;
    poll(
        function() {
            var xmlHttp = new XMLHttpRequest();
            // load yourself and see if you get this page again
            // 0fKAvHm1Nzi5adJpzpI3
            xmlHttp.open( "GET", window.location.href, false ); // synchronous request
            xmlHttp.send( null );
            var text = xmlHttp.responseText;
            if (text.indexOf("0fKAvHm1Nzi5adJpzpI3") > -1) {
                var loading_message_element = document.getElementById('loading_message');
                numPeriod = (numPeriod + 1) %% 4;
                loading_message_element.innerHTML = "%s" + Array(numPeriod+1).join(".") + Array(4-numPeriod).join("&nbsp;");
                return false; // got loading page still
            } else {
                return true; // did not get loading page
            }
        },
        function() {
            window.location.reload(true);
        },
        1000
    );
    </script>
    <style type="text/css">
    /* <![CDATA[ */
    @import url(/images/base.css);
    /* ]]> */
    </style>
    </head>
    <body class="loginPage">
    <div id="main" style="width: 500px; margin: 50px auto 0 auto;">
        <form class="form-signin">
            <center>
                <img style="margin-bottom:10px; max-width: 150px; max-height: 140px;" src="/images/BrandingLogo.png"><br/>
             <br/>
                <br/>
                <span class="form-signin-heading">
                    <div id="loading_message" style="color:white; font-size:20px; font-weight:bold; text-align:center">%s&nbsp;&nbsp;&nbsp;</div>
                </span>
            </center>
        </form>
    </div>
    </body>
    </html>""" % (us, cgi.escape(msg), cgi.escape(msg));

    req.write(html)

