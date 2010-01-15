<?php
/*
Copyright (c) 2003, Michael Bretterklieber <michael@bretterklieber.com>
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

    $Id: mschap.php,v 1.5 2003/01/26 20:31:11 mbretter Exp $
*/

include_once 'des.php';

function NtPasswordHash($plain) 
{
    return mhash (MHASH_MD4, str2unicode($plain));
}

function str2unicode($str) 
{

    for ($i=0;$i<strlen($str);$i++) {
        $a = ord($str{$i}) << 8;
        $uni .= sprintf("%X",$a);
    }
    return pack('H*', $uni);
}

function GenerateChallenge($size = 8) 
{
    mt_srand(hexdec(substr(md5(microtime()), -8)) & 0x7fffffff);
    for($i = 0; $i < $size; $i++) {
        $chall .= pack('C', 1 + mt_rand() % 255);
    }
    return $chall;
}

function ChallengeResponse($challenge, $nthash) 
{
    while (strlen($nthash) < 21)
        $nthash .= "\0";

    $resp1 = des_encrypt_ecb(substr($nthash, 0, 7), $challenge);
    $resp2 = des_encrypt_ecb(substr($nthash, 7, 7), $challenge);
    $resp3 = des_encrypt_ecb(substr($nthash, 14, 7), $challenge);

    return $resp1 . $resp2 . $resp3;
}

// MS-CHAPv2

function GeneratePeerChallenge() 
{
    return GenerateChallenge(16);
}

function NtPasswordHashHash($hash) 
{
    return mhash (MHASH_MD4, $hash);
}

function ChallengeHash($challenge, $peerChallenge, $username) 
{
    return substr(mhash (MHASH_SHA1, $peerChallenge . $challenge . $username), 0, 8);
}

function GenerateNTResponse($challenge, $peerChallenge, $username, $password) 
{
    $challengeHash = ChallengeHash($challenge, $peerChallenge, $username);
    $pwhash = NtPasswordHash($password);
    return ChallengeResponse($challengeHash, $pwhash);
}

?>
