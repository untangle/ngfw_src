// Copyright (c) 2003-2009 Untangle, Inc.
// All rights reserved.
function acceptAgreement(){
    var agree = document.getElementById('agree'),agreeValue; 
    if(agree){
        agreeValue = agree.type == 'hidden' ? true : agree.checked 
        if(agreeValue === true){
            authenticateUser("agree-error");
            return;
        }else{
            document.getElementById("agree-error").style.display = 'block';
        }
    }
}

function authenticateUser( errorField )
{
    var e = document.getElementById("authenticateUser");
    if (e) {
        e.disabled = true;
    }

    var req = false;
    document.getElementById(errorField).style.display = 'none';
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
            } catch (exn) {}
        }
    }

    req.onreadystatechange = function()
    {
        if (req.readyState == 4) {
            var v = JSON.parse( req.responseText );
            if ( v["authenticate"] == true ) {
                /* Wait a little bit to redirect, this way it guarantees the ipset is updated. */
                setTimeout( redirectUser, 2000 );
                return;
            }

            var e = document.getElementById("authenticateUser");

            if (e) {
                e.disabled = false;
            }
            document.getElementById(errorField).style.display = 'block';
            //alert( "Unable to authenticate you, please try again." );
        }
    };

    var password, username;
    username = document.getElementById( "username" );
    password = document.getElementById( "password" );

    if ( username != null ) {
        username = username.value;
    }
    if ( username == null ) {
        username = "<basic>";
    }

    if ( password != null ) {
        password = password.value;
    }
    if ( password == null ) {
        password = "<empty>";
    }
    
    req.open("POST", "/users/authenticate", true);
    req.overrideMimeType("application/json");
    req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    req.send("username=" + escape( username ) + "&password=" + escape( password ));
};

function redirectUser()
{
    var t = redirectUrl;

    if ( displayLogoutButton ) {
        var _top  = 5,_left = screen.width-315,
	
	x = window.open("logout.php","cpd_logout", "height=90,width=300,status=no,toolbar=no,address=no,menubar=no,location=no,top="+_top+",left="+_left);
	x.focus()        
    }

    if ( t == null ) {
        t = "http://guide.untangle.com/captive-portal";
    }
    
    window.location.href = t;
}
