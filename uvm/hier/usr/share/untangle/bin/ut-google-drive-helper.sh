#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

refreshToken()
{
    shift
    python3 -m simplejson.tool $1.gd/credentials.json | grep refresh_token | awk '{print $2}' | sed 's/\"//g'
}

$1 "$@"