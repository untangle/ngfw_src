<?php

include "lib.php";

open_db_connection();
$cpd_settings = get_cpd_settings();

?><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>                  
    <style type="text/css">
        body{
            font-family:Arial,sans-serif;
            font-size:12px;
            margin:0 10px;            
        }
        span{
            font-weight:bold;
        }
    </style>
    <script type="text/javascript" src="portal.js"></script>
    <script type="text/javascript">
    var endDate = (new Date()).getTime() + ( 1000 * <?= get_time_remaining() ?> );

function updateTimeout()
{
    var field = document.getElementById( "timeout" );
    var currentTime = new Date().getTime();
    var t = 0;

    var newValue = "";
    
    if ( endDate < currentTime ) {
        newValue = "expired";
    } else {
        t = Math.floor( (endDate - currentTime ) / 1000 );
        newValue = "";
        newValue = ( t % 60 ) + " s";

        if ( t > 60 ) {
            newValue = (Math.floor(( t / 60 ) % 60 )) + " m, " + newValue;
        }

        if ( t >= ( 60 * 60 )) {
            newValue = (Math.floor( t / ( 60 * 60 ))) + " h, "  + newValue;
        }
        setTimeout(updateTimeout, 2100 );
    }
    
    field.innerHTML = newValue;
  
}
function updatePopup(){
    try{
        window.opener.popupopened = true;    
    }catch(exn){        
    }  
}
function logout()
{
    var req = getHttpReqObj();
    req.onreadystatechange = function()
    {
        if (req.readyState == 4) {
            var v = JSON.parse( req.responseText );
            if(BrowserDetect.browser == 'Safari' || BrowserDetect.browser == 'Chrome'){
                self.close();
            }else{
                window.close();
            }
            
        }
    };
    req.open("GET", "/users/logout", true); 
    req.send();   
    
}
     </script>

    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <link href="/images/favicon-captive-portal.png" type="image/png" rel="icon"></link>    
    <title>Logout of Captive Portal</title>
  </head>

    <body onload="updateTimeout();">
    <div style="margin-top:25px;">
      Time remaining in session: <span id="timeout"></span>
    <a href="/cpd/portal.php?logout=Y" target="_logout" onclick="updatePopup();logout()" style="display:block;margin-top:10px;">Logout</button>
    </div>
  </body>
</html>
