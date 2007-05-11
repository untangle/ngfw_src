# -*-ruby-*-

require 'tran/test/package.rb'
require 'tran/reporting/package.rb'
require 'tran/ftp/package.rb'
require 'tran/http/package.rb'
require 'tran/mail/package.rb'
require 'tran/spyware/package.rb'
require 'tran/nat/package.rb'

require 'tran/airgap/package.rb'
require 'tran/firewall/package.rb'
require 'tran/openvpn/package.rb'
require 'tran/protofilter/package.rb'
require 'tran/boxbackup/package.rb'
require 'tran/sigma/package.rb'
require 'tran/httpblocker/package.rb'
require 'tran/ids/package.rb'

## Base Transforms
require 'tran/spam/package.rb'
require 'tran/virus/package.rb'

## SPAM based transforms
require 'tran/clamphish/package.rb'
require 'tran/spamassassin/package.rb'

## Virus based transforms
require 'tran/clam/package.rb'
require 'tran/hauri/package.rb'
require 'tran/kav/package.rb'
