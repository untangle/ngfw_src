<?php

include( "lib.php" );

$username = $_REQUEST["username"];
$password = $_REQUEST["password"];

open_db_connection();
$cpd_settings = get_cpd_settings();

$success = false;
print( "Captive Portal Settings\n" );
var_dump( $cpd_settings );

print( "Authentication Type " . $cpd_settings["authentication_type"] );

switch ( $cpd_settings["authentication_type"] ) {
case "NONE":
    $success = true;
    break;

case "RADIUS":
    include_once( "radius.php" );
    $radius_settings = get_radius_server_settings();
    var_dump( $radius_settings );
    if ( $radius_settings["enabled"] = "t" ) {
        printf( "Checking radius server '%s'\n", $radius_settings["host"] );
        $success = radius_authenticate( $username, $password, $radius_settings["host"], $radius_settings["port"], $radius_settings["shared_secret"], "pap" );
    }
    break;
    
case "ACTIVE_DIRECTORY":
    include( "ad.php" );
    $ad_settings = get_ad_settings();
    if ( $ad_settings["enabled"] === true ) {
        $success = ad_authenticate( $username, $password, $ad_settings["base_dn"], $ad_settings["account_suffix"], $ad_settings["domain_controller"]);
    }
    break;
}

if ( $success ) {
    if ( replace_host( $username )) {
        print( "<success/>" );
        die();
    }
}

print( "<fail/>" );

?>