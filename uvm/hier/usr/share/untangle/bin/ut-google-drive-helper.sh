#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

get_json_value() {
  local json_key=$1
  # Remove , space \n and \t
  python3 -m simplejson.tool $2 | grep "$json_key" | awk '{print $2}' | sed 's/[",[:space:]]//g' | tr -d '\t' | tr -d '\n'
}

appId()
{
    shift
    get_json_value "app_id" "$1.gd/credentials.json"
}

encryptedApiKey()
{
    shift
    get_json_value "encrypted_api_key" "$1.gd/credentials.json"
}

clientId()
{
    shift
    get_json_value "client_id" "$1.gd/credentials.json"
}

encryptedClientSecret()
{
    shift
    get_json_value "encrypted_client_secret" "$1.gd/credentials.json"
}

scopes()
{
    shift
    get_json_value "scopes" "$1.gd/credentials.json"
}

redirectUri()
{
    shift
    get_json_value "redirect_uri" "$1.gd/credentials.json"
}

authUri()
{
    shift
    get_json_value "auth_uri" "$1.gd/credentials.json"
}

tokenUri()
{
    shift
    get_json_value "token_uri" "$1.gd/credentials.json"
}

$1 "$@"