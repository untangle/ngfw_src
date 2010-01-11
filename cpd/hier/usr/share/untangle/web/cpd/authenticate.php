<?php

include( "lib.php" );

$username = $_REQUEST["username"];
$password = $_REQUEST["password"];

if ( $username == "test" and $password == "test" ) {
    if ( replace_host( $username )) {
        print( "<success/>" );
        die();
    }
}

print( "<fail/>" );

?>