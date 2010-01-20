// Copyright (c) 2003-2009 Untangle, Inc.
// All rights reserved.
function acceptAgreement(){
    var agree = document.getElementById('agree'),agreeValue; 
    if(agree){
        agreeValue = agree.type == 'hidden' ? true : agree.checked 
        if(agreeValue === true){
                        
            window.location.href = "http://www.untangle.com";
            return;
                                    
        }else{
            document.getElementById("agree-error").style.display = 'block';
        }
    }
}
function authenticateUser()
{
    var e = document.getElementById("authenticateUser");
    if (e) {
        e.disabled = true;
    }

    var req = false;
    document.getElementById("login-error").style.display = 'none';
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
            if ( req.responseText.indexOf( "<success/>" ) >= 0 ) {
                //alert( "You have been authenticated." );
                window.location.href = "http://www.untangle.com";
                return;
            }
            
            
            var e = document.getElementById("authenticateUser");

            if (e) {
                e.disabled = false;
            }
            document.getElementById("login-error").style.display = 'block';
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
        username = "";
    }

    if ( password != null ) {
        password = password.value;
    }
    if ( password == null ) {
        password = "";
    }
    
    req.open("POST", "authenticate.php", true);
    req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    req.send("username=" + escape( username ) + "&password=" + escape( password ));
};
