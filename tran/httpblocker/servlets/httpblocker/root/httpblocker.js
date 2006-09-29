// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function unblockSite(forAll)
{
    var req = false;

    if (window.XMLHttpRequest) {
        req = new XMLHttpRequest();
        if (req.overrideMimeType) {
            req.overrideMimeType('text/xml');
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

    req.onreadystatechange = function() {
        if (req.readyState == 4) {
            window.location.reload();
        }
    };

    var hostname = window.location.hostname;
    req.open('POST', "unblock", true);
    req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    req.send("site=" + hostname + "&global=" + forAll);
};

