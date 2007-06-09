# -*-ruby-*-

require "#{SRC_HOME}/tran/test/package.rb"
require "#{SRC_HOME}/tran/reporting/package.rb"
require "#{SRC_HOME}/tran/ftp-casing/package.rb"
require "#{SRC_HOME}/tran/http-casing/package.rb"
require "#{SRC_HOME}/tran/mail-casing/package.rb"
require "#{SRC_HOME}/tran/spyware/package.rb"
require "#{SRC_HOME}/tran/router/package.rb"

# require "#{SRC_HOME}/tran/airgap/package.rb"
require "#{SRC_HOME}/tran/firewall/package.rb"
require "#{SRC_HOME}/tran/openvpn/package.rb"
require "#{SRC_HOME}/tran/protofilter/package.rb"
require "#{SRC_HOME}/tran/sigma/package.rb"
require "#{SRC_HOME}/tran/webfilter/package.rb"
require "#{SRC_HOME}/tran/ips/package.rb"

## Base Nodes
require "#{SRC_HOME}/tran/spam-base/package.rb"
require "#{SRC_HOME}/tran/virus-base/package.rb"

## SPAM based nodes
require "#{SRC_HOME}/tran/phish/package.rb"
require "#{SRC_HOME}/tran/spamassassin/package.rb"

## Virus based nodes
require "#{SRC_HOME}/tran/clam/package.rb"
require "#{SRC_HOME}/tran/hauri/package.rb"
require "#{SRC_HOME}/tran/kav/package.rb"
