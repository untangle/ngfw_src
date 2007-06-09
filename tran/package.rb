# -*-ruby-*-

require "#{SRC_HOME}/tran/test/package.rb"
require "#{SRC_HOME}/tran/reporting/package.rb"
require "#{SRC_HOME}/tran/ftp/package.rb"
require "#{SRC_HOME}/tran/http/package.rb"
require "#{SRC_HOME}/tran/mail/package.rb"
require "#{SRC_HOME}/tran/spyware/package.rb"
require "#{SRC_HOME}/tran/nat/package.rb"

# require "#{SRC_HOME}/tran/airgap/package.rb"
require "#{SRC_HOME}/tran/firewall/package.rb"
require "#{SRC_HOME}/tran/openvpn/package.rb"
require "#{SRC_HOME}/tran/protofilter/package.rb"
require "#{SRC_HOME}/tran/sigma/package.rb"
require "#{SRC_HOME}/tran/httpblocker/package.rb"
require "#{SRC_HOME}/tran/ids/package.rb"

## Base Transforms
require "#{SRC_HOME}/tran/spam/package.rb"
require "#{SRC_HOME}/tran/virus/package.rb"

## SPAM based transforms
require "#{SRC_HOME}/tran/clamphish/package.rb"
require "#{SRC_HOME}/tran/spamassassin/package.rb"

## Virus based transforms
require "#{SRC_HOME}/tran/clam/package.rb"
require "#{SRC_HOME}/tran/hauri/package.rb"
require "#{SRC_HOME}/tran/kav/package.rb"
