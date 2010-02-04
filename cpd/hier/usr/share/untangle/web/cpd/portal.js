// Copyright (c) Untangle, Inc.
// All rights reserved.
var _popup,popupopened = false;
function acceptAgreement(){
    var agree = document.getElementById('agree'),agreeValue; 
    if(agree){
        agreeValue = agree.type == 'hidden' ? true : agree.checked 
        if(agreeValue === true){
            try{
                showPleaseWait();
                authenticateUser("agree-error");

            }catch(exn){            
                hidePleaseWait();
                showError("An error occured. Please try again.")
            }
            return;
        }else{
            showError("In order to continue, you must check the box below.");
            document.getElementById("agree-error").style.display = 'block';
        }
    }
}
function hideLogoutSuccess(){
    var ls = document.getElementById('logout-success');
    if(ls){
        ls.style.display = 'none';
    }
}
function showError(errorMessage){
    hideLogoutSuccess();
    document.getElementById('agree-error').style.display = 'block';
    document.getElementById('agree-error').innerHTML = errorMessage;
}
function showPleaseWait(){
    hideLogoutSuccess();
    document.getElementById('authenticateUser').style.display = 'none';
    document.getElementById('please-wait').style.display = '';
}
function hidePleaseWait(){
    document.getElementById('authenticateUser').style.display = '';
    document.getElementById('please-wait').style.display = 'none';
}
function getHttpReqObj(){
    var req = false;
    if (window.XMLHttpRequest) {
        req = new XMLHttpRequest();
        if (req.overrideMimeType) {
            req.overrideMimeType("text/xml");
        }
    } else if (window.ActiveXObject) {
        try {
            req = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (exn) {
            try {
                req = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (exn) {

            }
        }
    } 
    return req;   
}
function authenticateUserWrapper(errorfield){
    try{
        hideLogoutSuccess();    
        showPleaseWait();
        window.setTimeout(function(){authenticateUser(errorfield);},2000);
    
    }catch(exn){            
        hidePleaseWait();
        showError("An error occured. Please try again.")
    }
}
function authenticateUser( errorField )
{

    var e = document.getElementById("authenticateUser");
    if (e) {
        e.disabled = true;
    }
    document.getElementById(errorField).style.display = 'none';

    var req = getHttpReqObj();

    req.onreadystatechange = function()
    {
        if (req.readyState == 4) {
            var v = JSON.parse( req.responseText );
            if ( v["authenticate"] == true ) {
                if(displayLogoutButton){
                    showLogoutPopup();                    
                }else{
                    redirectUser();                       
                }
                return;
            }

            var e = document.getElementById("authenticateUser");

            if (e) {
                e.disabled = false;
            }
            hidePleaseWait();
            document.getElementById(errorField).style.display = 'block';
            
        }
    };

    var password, username;
    username = document.getElementById( "username" );
    password = document.getElementById( "password" );

    if ( username != null ) {
        username = username.value;
    }
    if ( username == null ) {
        username = "captive portal user";
    }

    if ( password != null ) {
        password = password.value;
    }
    if ( password == null ) {
        password = "empty";
    }
    
    req.open("POST", "/users/authenticate", true);
    if(req.overrideMimeType){
        req.overrideMimeType("application/json");
    }
    req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    req.send("username=" + escape( username ) + "&password=" + escape( password ));
};
function showLogoutPopup(){
    if ( displayLogoutButton ) {
        var _top  = 5,_left = screen.width-315;
	
    	_popup = window.open("logout.php","cpd_logout", "height=90,width=300,status=no,toolbar=no,address=no,menubar=no,location=no,top="+_top+",left="+_left);
    	try{
            _popup.blur();
        }catch(exn){        
        }
        window.focus();    	
    	window.setTimeout(function()
            {
                if(_hasPopupBlocker(_popup) || popupopened == false){
                    /* In a future release, this should not be an "alert" - this should instead be a "div based dialog"*/
                    alert('The logout window was blocked by the browser\'s pop-up blocker.\nTo display the logout window, please adjust the pop-up blocker settings. \nTo proceed to site you wanted to access, click OK.');                    
                }else{    
                    _popup.blur();                
                    window.focus();                       
                    _popup.focus();
                }
                redirectUser();
            },3000);     
    }
}
function redirectUser()
{
    var t = redirectUrl;
    if ( t == null ) {
        t = "http://guide.untangle.com/captive-portal";
    }
    
    window.location.href = t;

}
function _hasPopupBlocker(poppedWindow) { 
    var result = false; 
 
    try { 
        if (typeof poppedWindow == 'undefined') {     
            // Safari with popup blocker... leaves the popup window handle undefined 
            result = true; 
        } 
        else if (poppedWindow && poppedWindow.closed) { 
            // This happens if the user opens and closes the client window... 
            // Confusing because the handle is still available, but it's in a "closed" state. 
            // We're not saying that the window is not being blocked, we're just saying 
            // that the window has been closed before the test could be run. 
            result = false; 
        } 
        else if (poppedWindow && poppedWindow.logout) { 
            // This is the actual test. The client window should be fine. 
            result = false; 
        } 
        else { 
            // Else we'll assume the window is not OK 
            result = true; 
        } 
 
    } catch (err) { 
        //if (console) { 
        //    console.warn("Could not access popup window", err); 
        //} 
    } 
 
    return result; 
}
var BrowserDetect = {
	init: function () {
		this.browser = this.searchString(this.dataBrowser) || "An unknown browser";
		this.version = this.searchVersion(navigator.userAgent)
			|| this.searchVersion(navigator.appVersion)
			|| "an unknown version";
		this.OS = this.searchString(this.dataOS) || "an unknown OS";
	},
	searchString: function (data) {
		for (var i=0;i<data.length;i++)	{
			var dataString = data[i].string;
			var dataProp = data[i].prop;
			this.versionSearchString = data[i].versionSearch || data[i].identity;
			if (dataString) {
				if (dataString.indexOf(data[i].subString) != -1)
					return data[i].identity;
			}
			else if (dataProp)
				return data[i].identity;
		}
	},
	searchVersion: function (dataString) {
		var index = dataString.indexOf(this.versionSearchString);
		if (index == -1) return;
		return parseFloat(dataString.substring(index+this.versionSearchString.length+1));
	},
	dataBrowser: [
		{
			string: navigator.userAgent,
			subString: "Chrome",
			identity: "Chrome"
		},
		{ 	string: navigator.userAgent,
			subString: "OmniWeb",
			versionSearch: "OmniWeb/",
			identity: "OmniWeb"
		},
		{
			string: navigator.vendor,
			subString: "Apple",
			identity: "Safari",
			versionSearch: "Version"
		},
		{
			prop: window.opera,
			identity: "Opera"
		},
		{
			string: navigator.vendor,
			subString: "iCab",
			identity: "iCab"
		},
		{
			string: navigator.vendor,
			subString: "KDE",
			identity: "Konqueror"
		},
		{
			string: navigator.userAgent,
			subString: "Firefox",
			identity: "Firefox"
		},
		{
			string: navigator.vendor,
			subString: "Camino",
			identity: "Camino"
		},
		{		// for newer Netscapes (6+)
			string: navigator.userAgent,
			subString: "Netscape",
			identity: "Netscape"
		},
		{
			string: navigator.userAgent,
			subString: "MSIE",
			identity: "Explorer",
			versionSearch: "MSIE"
		},
		{
			string: navigator.userAgent,
			subString: "Gecko",
			identity: "Mozilla",
			versionSearch: "rv"
		},
		{ 		// for older Netscapes (4-)
			string: navigator.userAgent,
			subString: "Mozilla",
			identity: "Netscape",
			versionSearch: "Mozilla"
		}
	],
	dataOS : [
		{
			string: navigator.platform,
			subString: "Win",
			identity: "Windows"
		},
		{
			string: navigator.platform,
			subString: "Mac",
			identity: "Mac"
		},
		{
			   string: navigator.userAgent,
			   subString: "iPhone",
			   identity: "iPhone/iPod"
	    },
		{
			string: navigator.platform,
			subString: "Linux",
			identity: "Linux"
		}
	]

};
BrowserDetect.init();