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

$protocol = "http";
if (( $ssl == "on" ) || ( $https_redirect )) {
    $protocol = "https";
}


header( "Location: $protocol://" . $_SERVER["SERVER_ADDR"] . "/cpd/index.php?server_name=$server_name&method=$method&path=$path&ssl=$ssl");
?>