#! /usr/bin/ruby

require 'socket'
require 'timeout'

BYTES_TO_READ = 3
port, timeout = ARGV

failures = 0

begin
  Timeout::timeout(timeout.to_i) {
    s = TCPSocket.new('localhost', port.to_i)
    s.puts "PING SPAMC/1.2"
    s.read(BYTES_TO_READ)
  }
  exit 0
rescue Timeout::Error
  failures += 1
  if failures == 2 then
    exit 1
  else
    retry
  end
rescue Errno::ECONNREFUSED
  exit 0 # the 1st-gen nannies will handle this
end
