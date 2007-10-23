#! /usr/bin/ruby

require 'socket'
require 'timeout'

BYTES_TO_READ = 3
port, timeout = ARGV

begin
  Timeout::timeout(timeout.to_i) {
    s = TCPSocket.new('localhost', port.to_i)
    s.puts "junk"
    s.read(BYTES_TO_READ)
  }
  exit 0
rescue Timeout::Error
  exit 1
rescue Errno::ECONNREFUSED
  exit 2
end
