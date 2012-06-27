<?php

include "config.php";
session_cache_limiter('nocache');
session_start();

/* Clear the username variable, the user should only go here if they got redirected */
if ( $_SESSION["init"] == true ) {
    unset($_SESSION["username"]);
    session_write_close();
} else {
    session_destroy();
}

$server_name=urlencode( $_SERVER["SERVER_NAME"] );
$method=urlencode( $_SERVER["REQUEST_METHOD"] );
$path=urlencode( $_SERVER["SCRIPT_URL"] );
$ssl=urlencode( $_SERVER["HTTPS"] );
$local_server_name = $_SERVER["SERVER_ADDR"];

$protocol = "http";
if (( $ssl == "on" ) || ( $https_redirect )) {
    $protocol = "https";
    $local_server_name = $local_server_name.':'.$https_port;
}

/* if HTTPS use hostname so cert will match, otherwise use the IP as it will always work */
/* this doesn't work because untangle incorrectly returns 127.0.0.1 when the client looks up untangle's hostname */
/* commenting out this section to avoid this */
// if ($protocol == "https") {
//     $hostname = getHostbyAddr($_SERVER["SERVER_ADDR"]);
//     if ($hostname == null || $hostname == "")
//         $hostname = $_SERVER["SERVER_ADDR"];
//     $local_server_name = $hostname.":".$https_port;
// } 

header( "Location: $protocol://$local_server_name/cpd/index.php?server_name=$server_name&method=$method&path=$path&ssl=$ssl");

?>
