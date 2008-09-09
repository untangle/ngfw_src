#!/usr/bin/env ruby

require "ftools"

## A simple script to build a sample quarantine, just pass in an email
## address and it will create a quarantine with 100 messages in it.
## Run this script from the base directory of the UVM (parent of dist)
## while the UVM is not running.

UVM_DIRECTORY="dist/usr/share/untangle/"
QUARANTINE_DIRECTORY="#{UVM_DIRECTORY}/quarantine"
SUMMARY_CLOSED="#{QUARANTINE_DIRECTORY}/summary.closed"
SUMMARY_OPEN="#{QUARANTINE_DIRECTORY}/summary.open"

unless File.directory?( UVM_DIRECTORY )
  puts "Execute this script from the parent of the dist directory."
  exit -1
end


unless ARGV.length == 1
  puts <<EOF
USAGE: build_sample_quarantine.rb <email-address>
\tRun this command from the parent of the 'dist' directory
EOF

  exit -2
end

email_address = ARGV[0]

unless File.exists?( SUMMARY_CLOSED )
  puts "This script must run when the UVM is stopped."
  exit -1
end

`grep -q '#{email_address}' #{SUMMARY_CLOSED}`

if $? == 0
  puts "The email address #{email_address} already has an address."
  exit -2
end

## Inboxes are keyed based on the time
inbox="inboxes/#{(Time.new.to_f * 1000).to_i}"

num_messages=100

File.open( SUMMARY_CLOSED, "a" ) do |f|
  f.puts <<EOF
--
#{email_address}
#{inbox}
20000
#{num_messages}
EOF
end

File.makedirs( "#{QUARANTINE_DIRECTORY}/#{inbox}" )

summary = File.open( "#{QUARANTINE_DIRECTORY}/#{inbox}/index.mqi", "w" )

## A list of a few random emails addresses to choose from.
from_addresses = [ "nokia@yahoo.com", "GFHWEJLXCO@hotmail.com", "whatever@google.com", "borsec@untangle.com", "spaco@gmail.com", "doit@randomness.net", "foospammer@hotmail.com" ]

summary.puts <<EOF
################################################
#             Inbox Index File                 #
#                                              #
#               DO NOT MODIFY                  #
#                                              #
# Although this is a text file, it is intended #
# only for machine reading and writing.        #
################################################

Ver:3
Address:#{email_address}
EOF

num_messages.times do
  message_id="meta#{rand( 10000 )}.mime"
  subject=`/usr/games/fortune | head -n 1`.strip
  from_address=from_addresses[rand( from_addresses.length )]
  summary.puts <<EOF
--SEP--
Ver:3
#{message_id}
#{((Time.new - rand( 60 *  60 * 24 * 28 )).to_f * 1000).to_i}
#{1000 + rand(2000)}
1
0:#{email_address}
0:#{from_address}
0:#{subject}
0:SPAM
0:#{"%10.1f" % ( 75 * rand )}
0

EOF
  
  File.open( "#{QUARANTINE_DIRECTORY}/#{inbox}/#{message_id}", "w" ) do |f|
    f.puts <<EOF
Received: from localhost.localdomain (bebe.metaloft.com [10.0.0.44])
	by mv-edgeguard; Thu, 15 May 2008 19:23:17 -0700
Received: from [216.129.106.51] (helo=release)
	by mail.metavize.com with esmtp (Exim 4.50)
	id 1FRtnn-0007Rk-10
	for dmorris@metavize.com; Fri, 07 Apr 2006 09:27:15 -0700
Received: from [201.8.255.178] (helo=201008255178.user.veloxzone.com.br)
	by release with smtp (Exim 4.50)
	id 1FRtnV-0006MX-QG
	for dmorris@metavize.com; Fri, 07 Apr 2006 09:26:59 -0700
X-Apparently-To: dmorris@metavize.com
Received: from  (HELO 8)
        as user @ by www..com.ar with Mosap;
        Fri, 07 Apr 2006 10:19:04 -0700
Date: Fri, 07 Apr 2006 15:19:04 -0200
Message-Id: <68TG87FE.0Y24.GFHWEJLXCO@hotmail.com>
From: "Liliana Marion" <#{from_address}>
To:     <#{email_address}>
Subject: #{subject}
X-Mailer: Evolution/1.0-5mdk



Revolutionary "Hoodia" which works
effectively burning fats without hunger,
chemicals intake or heavy exercise.
Suppress your appetite and enjoying your
very nice V-Shape body in just a week.

You won't regret.

http://043.hooddiaastyle.com


9Sl3N
EOF
  end  
end

summary.close

File.cp( SUMMARY_CLOSED, SUMMARY_OPEN )
