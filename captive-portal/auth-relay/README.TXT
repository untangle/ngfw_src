This directory contains files that are required on auth-relay.untangle.com
to support OAuth2 authentication of Captive Portal Users.

The flow of the OAuth2 transaction is as follows:

1) An unauthenticated LAN client behind Untangle tries to access a web page

2) Captive Portal returns a redirect to the external OAuth2 provider, with
our normal redirect URL encoded and passed in the state parameter.
The app also allows access, based on SNI host name, to the external domains
required for the client to interact with and login to the external provider.

3) After succesfully login, the external provider will redirect the client
to the auth-relay callback. This script will decode the original capture
redirect stored in the state parameter, and will append the authcode
received from the external OAuth2 provider, and return to the client.

4) The client will again hit the Captive Portal handler.py script but this
time the request will include the authcode added by the auth-relay
callback handler.

5) The handler.py script will detect the authcode, and pass it to
getAccessToken on auth-relay, which will exchange the code for an
access token, and then use the access token to retrieve the email address
of the user. The email address will be returned to the handler.py script
which will use it to call the Captive Portal userLogin function, logging
in the client and allowing network access.

------------------------------------------------------------------------------

/var/www/html/callback.php

This PHP script is the redirect target called by the external OAuth2 provider
when a user is sucessfully authenticated.


/usr/lib/cgi-bin/getAccessToken

This Python script is called by the Untangle server in front of the Captive
Portal user to perform final validation of the client and retrieve the
email address used to login.

/var/cache/apache2/facebook_auth.txt
/var/cache/apache2/facebook_user.txt
/var/cache/apache2/google_auth.txt
/var/cache/apache2/google_user.txt
/var/cache/apache2/microsoft_auth.txt
/var/cache/apache2/microsoft_user.txt

These files are used to load the auth and user data instead of fetching from
the external providers when the development flag file exists:

  Development Flag File: /var/cache/apache2/development.flag
