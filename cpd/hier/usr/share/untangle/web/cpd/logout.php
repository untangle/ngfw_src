<?php

include "lib.php";

open_db_connection();
$cpd_settings = get_cpd_settings();

?><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>

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
    
    field.value = newValue;
}

function logout()
{
    window.location.href = "/users/logout";
}
     </script>

    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
    <title>Logout of Captive Portal</title>
  </head>

    <body onload="updateTimeout();">
    <div style="margin-top:25px;">
      Time remaining in session: <input type="text" id="timeout">0 s</span>
    <button onclick="logout()" style="display:block;margin-top:10px;">Logout</button>
    </div>
  </body>
</html>
