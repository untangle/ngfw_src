// Copyright (c) 2003-2009 Untangle, Inc.
// All rights reserved.

function unblockSite(global) {
    var e = document.getElementById("unblockNowButton");
    if (e) {
        e.disabled = true;
    }

    e = document.getElementById("unblockGlobalButton");
    if (e) {
        e.disabled = true;
    }

    var req = false;

    if (window.XMLHttpRequest) {
        req = new XMLHttpRequest();
        if (req.overrideMimeType) {
            req.overrideMimeType('text/xml');
        }
    } else if (window.ActiveXObject) {
        try {
            req = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (ex) {
            try {
                req = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (exn) {}
        }
    }

    req.onreadystatechange = function() {
        if (req.readyState == 4) {
            if ( req.responseText.indexOf( "<success/>" ) >= 0 ) {
                window.location.href = url;
            } else {
                document.getElementById( "invalid-password" ).style.display = "";

                var button = document.getElementById("unblockNowButton");
                if (button) {
                    button.disabled = false;
                }
                
                button = document.getElementById("unblockGlobalButton");
                if (button) {
                    button.disabled = false;
                }
            }
        }
    };

    var hostname = window.location.hostname;
    var password = document.getElementById( "unblockPassword" );

    if ( password != null ) {
        password = password.value;
    }
    if ( password == null ) {
        password = "";
    }
    req.open('POST', "unblock", true);
    req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    req.send("nonce=" + escape( nonce ) + "&tid=" + escape( tid ) + "&global=" + escape( global) + "&password=" + escape( password ));
}
