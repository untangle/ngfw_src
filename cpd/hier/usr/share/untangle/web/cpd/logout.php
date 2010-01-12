<?php

include( "lib.php" );

if ( remove_host()) {
    print( "<success/>" );
    die();    
}

print( "<fail/>" );

?>