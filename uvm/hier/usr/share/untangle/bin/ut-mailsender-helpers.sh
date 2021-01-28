#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

resetSmtpPort()
{
   shift

   # arg1 is port, arg2 is file.
   /bin/sed -e "s/ driver = smtp/ driver = smtp\n port = $1/g" -i $2
}

$1 "$@"