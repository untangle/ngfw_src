<?php

include "lib.php";
session_cache_limiter('nocache');
session_start();

$_SESSION["init"] = true;
if ( $_REQUEST["method"] != null ) {
    if ( $_REQUEST["method"] == "GET" ) {
        $_SESSION["server_name"] = $_REQUEST["server_name"];
        $_SESSION["path"] = $_REQUEST["path"];
        $_SESSION["ssl"] = $_REQUEST["ssl"];
    } else {
        $_SESSION["server_name"] = null;
        $_SESSION["path"] = null;
    }
}
$logout_success = false;
if ($_REQUEST["logout"]=='Y'){
    $logout_success = true;
}
session_write_close();

open_db_connection();
$skin_settings = get_skin_settings();
$branding_settings = get_branding_settings();
$cpd_settings = get_cpd_settings();

$redirectUrl = $cpd_settings[""];

switch ( $cpd_settings["pageType"] ) {
case "BASIC_MESSAGE":
    include "basicmessage.php";
    break;

case "BASIC_LOGIN":
    include "basiclogin.php";
    break;

case "CUSTOM":
    include "custom.php";
    break;
}

?>
