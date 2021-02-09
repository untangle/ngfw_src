#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

showPublicKey()
{
    echo $2 | $3 pubkey
}

$1 "$@"