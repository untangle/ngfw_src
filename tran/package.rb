# -*-ruby-*-

require "#{ALPINE_HOME}/tran/test/package.rb"
require "#{ALPINE_HOME}/tran/reporting/package.rb"
require "#{ALPINE_HOME}/tran/ftp/package.rb"
require "#{ALPINE_HOME}/tran/http/package.rb"
require "#{ALPINE_HOME}/tran/mail/package.rb"
require "#{ALPINE_HOME}/tran/spyware/package.rb"
require "#{ALPINE_HOME}/tran/nat/package.rb"

require "#{ALPINE_HOME}/tran/airgap/package.rb"
require "#{ALPINE_HOME}/tran/firewall/package.rb"
require "#{ALPINE_HOME}/tran/openvpn/package.rb"
require "#{ALPINE_HOME}/tran/protofilter/package.rb"
require "#{ALPINE_HOME}/tran/boxbackup/package.rb"
require "#{ALPINE_HOME}/tran/sigma/package.rb"
require "#{ALPINE_HOME}/tran/httpblocker/package.rb"
require "#{ALPINE_HOME}/tran/ids/package.rb"

## Base Transforms
require "#{ALPINE_HOME}/tran/spam/package.rb"
require "#{ALPINE_HOME}/tran/virus/package.rb"

## SPAM based transforms
require "#{ALPINE_HOME}/tran/clamphish/package.rb"
require "#{ALPINE_HOME}/tran/spamassassin/package.rb"

## Virus based transforms
require "#{ALPINE_HOME}/tran/clam/package.rb"
require "#{ALPINE_HOME}/tran/hauri/package.rb"
require "#{ALPINE_HOME}/tran/kav/package.rb"
