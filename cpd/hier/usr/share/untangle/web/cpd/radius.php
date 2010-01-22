<?php
/*
Copyright (c) 2003-2007, Michael Bretterklieber <michael@bretterklieber.com>
All rights reserved.

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions 
are met:

1. Redistributions of source code must retain the above copyright 
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright 
   notice, this list of conditions and the following disclaimer in the 
   documentation and/or other materials provided with the distribution.
3. The names of the authors may not be used to endorse or promote products 
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY 
OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

This code cannot simply be copied and put under the GNU Public License or 
any other GPL-like (LGPL, GPL2) License.

    $Id: radius-auth.php,v 1.3 2007/03/18 21:17:02 mbretter Exp $

Modified by Sébastien Delafond <seb@untangle.com>
Fri, 15 Jan 2010 06:08:10 -0800
*/

if(!extension_loaded('radius')) {
  dl('radius.so');
}

class RadiusException extends Exception {}

$res = radius_auth_open();

function radius_connect($server, $port, $shared_secret)
{
  global $res;
  if (!radius_add_server($res, $server, $port, $shared_secret, 3, 3) ||
      !radius_create_request($res, RADIUS_ACCESS_REQUEST) ||
      !radius_put_string($res, RADIUS_NAS_IDENTIFIER, isset($HTTP_HOST) ? $HTTP_HOST : 'localhost') ||
      !radius_put_int($res, RADIUS_SERVICE_TYPE, RADIUS_FRAMED) ||
      !radius_put_int($res, RADIUS_FRAMED_PROTOCOL, RADIUS_PPP) ||
      !radius_put_string($res, RADIUS_CALLING_STATION_ID, isset($REMOTE_HOST) ? $REMOTE_HOST : '127.0.0.1') == -1) {
    throw new RadiusException('RadiusError:' . radius_strerror($res). "\n");
  }
}

function radius_set_username($username) {
  global $res;

  if (!radius_put_string($res, RADIUS_USER_NAME, $username)) {
    throw new RadiusException('RadiusError:' . radius_strerror($res). "\n");
  }
}

function radius_set_password_chap($password) {
  global $res;

  /* generate Challenge */
  mt_srand(time());
  $chall = mt_rand();

  // FYI: CHAP = md5(ident + plaintextpass + challenge)
  $chapval = pack('H*', md5(pack('Ca*',1 , $password . $chall)));

  // Radius wants the CHAP Ident in the first byte of the CHAP-Password
  $pass = pack('C', 1) . $chapval;


  if (!radius_put_attr($res, RADIUS_CHAP_PASSWORD, $pass)) {
    throw new RadiusException('RadiusError: RADIUS_CHAP_PASSWORD:' . radius_strerror($res). "<br>");
  }
  if (!radius_put_attr($res, RADIUS_CHAP_CHALLENGE, $chall)) {
    throw new RadiusException('RadiusError: RADIUS_CHAP_CHALLENGE:' . radius_strerror($res). "<br>");
  }
}

function radius_set_password_mschapv1($password) {
  include_once('mschap.php'); // FIXME

  global $res;

  $challenge = GenerateChallenge();

  if (!radius_put_vendor_attr($res, RADIUS_VENDOR_MICROSOFT, RADIUS_MICROSOFT_MS_CHAP_CHALLENGE, $challenge)) {
    throw new RadiusException('RadiusError: RADIUS_MICROSOFT_MS_CHAP_CHALLENGE:' . radius_strerror($res). "<br>");
    exit;
  }

  $ntresp = ChallengeResponse($challenge, NtPasswordHash($password));
  $lmresp = str_repeat ("\0", 24);

  $resp = pack('CCa48',1 , 1, $lmresp . $ntresp);
  
  if (!radius_put_vendor_attr($res, RADIUS_VENDOR_MICROSOFT, RADIUS_MICROSOFT_MS_CHAP_RESPONSE, $resp)) {
    throw new RadiusException('RadiusError: RADIUS_MICROSOFT_MS_CHAP_RESPONSE:' . radius_strerror($res). "<br>");
  }
}

function radius_set_password_mschapv2($password) {
  include_once('mschap.php');

  global $res;

  $authChallenge = GenerateChallenge(16);

  if (!radius_put_vendor_attr($res, RADIUS_VENDOR_MICROSOFT, RADIUS_MICROSOFT_MS_CHAP_CHALLENGE, $authChallenge)) {
    throw new RadiusException('RadiusError: RADIUS_MICROSOFT_MS_CHAP_CHALLENGE:' . radius_strerror($res). "<br>");
  }

  // we have no client, therefore we generate the Peer-Challenge
  $peerChallenge = GeneratePeerChallenge();

  $ntresp = GenerateNTResponse($authChallenge, $peerChallenge, $username, $password);
  $reserved = str_repeat ("\0", 8);

  $resp = pack('CCa16a8a24',1 , 1, $peerChallenge, $reserved, $ntresp);

  if (!radius_put_vendor_attr($res, RADIUS_VENDOR_MICROSOFT, RADIUS_MICROSOFT_MS_CHAP2_RESPONSE, $resp)) {
    throw new RadiusException('RadiusError: RADIUS_MICROSOFT_MS_CHAP2_RESPONSE:' . radius_strerror($res). "<br>");
  }
}

function radius_set_password_pap($password) {
  global $res;

  if (!radius_put_string($res, RADIUS_USER_PASSWORD, $password)) {
    throw new RadiusException('RadiusError:' . radius_strerror($res). "<br>");
  }
}

function radius_send_auth_request() {
  global $res;

  if (!radius_put_int($res, RADIUS_SERVICE_TYPE, RADIUS_FRAMED) ||
      !radius_put_int($res, RADIUS_FRAMED_PROTOCOL, RADIUS_PPP)) {
    throw new RadiusException('RadiusError:' . radius_strerror($res). "\n");
  }

  $req = radius_send_request($res);
  if (!$req) {
    throw new RadiusException('RadiusError:' . radius_strerror($res). "\n");
  }

  switch($req) {
  case RADIUS_ACCESS_ACCEPT:
    return true;
  case RADIUS_ACCESS_REJECT:
    return false;
  default:
    return false;
  }
}

function radius_disconnect() {
  global $res;

  radius_close($res);
}

?>
