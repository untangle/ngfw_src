#!/bin/sh


function write()
{
    cat <<EOF
GET /proxy/fwd/asdf HTTP/1.1
Host: localhost
Connection: close

EOF
    yes "now is the time for all good men to come the the aid of their country"
}


write | nc localhost 80 >/dev/null
