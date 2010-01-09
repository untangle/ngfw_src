<?php

include( "session.php" );

$username = $_REQUEST["username"];
$password = $_REQUEST["password"];

if ( $username == "test" and $password == "test" ) {
    replace_host( $username );
    print( "<success/>" );
    die();
}

print( "<fail/>" );

?>