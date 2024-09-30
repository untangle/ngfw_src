#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

get_json_value() {
  local json_key=$1
  python3 -m simplejson.tool $2 | grep "$json_key" | awk '{print $2}' | sed 's/\"//g'
}

refreshToken()
{
    shift
    get_json_value "refresh_token" "$1.gd/credentials.json"
}

appId()
{
    shift
    get_json_value "app_id" "$1.gd/credentials.json"
}

clientId()
{
    shift
    get_json_value "client_id" "$1.gd/credentials.json"
}

clientSecret()
{
    shift
    get_json_value "client_secret" "$1.gd/credentials.json"
}

scopes()
{
    shift
    get_json_value "scopes" "$1.gd/credentials.json"
}

redirectUrl()
{
    shift
    get_json_value "redirect_url" "$1.gd/credentials.json"
}

$1 "$@"