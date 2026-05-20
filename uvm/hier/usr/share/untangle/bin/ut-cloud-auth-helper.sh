#!/bin/bash

# This script extracts encrypted fields from the cloud auth credentials JSON
# shipped by the untangle-cloud-auth-credentials package.

get_json_value() {
  local json_key=$1
  # Remove , space \n and \t
  python3 -m simplejson.tool $2 | grep "$json_key" | awk '{print $2}' | sed 's/[",[:space:]]//g' | tr -d '\t' | tr -d '\n'
}

encryptedAuthRequest()
{
    shift
    get_json_value "encrypted_auth_request" "$1credentials.json"
}

$1 "$@"
