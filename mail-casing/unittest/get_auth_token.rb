## A rush script for retrieving an authentication token for an email address.

## (Wherever the rush shell is)
## To run use:  ./dist/usr/bin/rush ./mail-casing/unittest/get_auth_token.rb <address>
require "cgi"

nm = Untangle::RemoteUvmContext.nodeManager()
tid = nm.nodeInstances( 'untangle-casing-mail' ).first
mail = nm.nodeContext( tid ).node

account = ARGV[0]
puts "Retrieving authentication token for '#{account}'"
token = mail.createAuthToken(account)
puts <<EOF
Account: '#{account}'
Token: '#{token}'
URLEncoded: '#{CGI.escape( token )}'
URL: http://localhost/quarantine/manageuser?tkn=#{CGI.escape( token )}&action=viewibx
EOF
