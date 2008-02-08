#! /usr/bin/ruby

require 'socket'
require 'timeout'

BYTES_TO_READ = 1
port, timeout = ARGV
failures      = 0
f             = open("/var/log/uvm/banner-nanny.log", "a")
logMsg        = ""
s             = nil
rc            = 0

logMsg += Time.now.strftime("%Y %m %d %H:%M:%S")
logMsg += " | port=#{port} | "

begin
  begin
    Timeout::timeout(timeout.to_i) {
      s = TCPSocket.new('localhost', port.to_i)
      s.puts "PING SPAMC/1.2"
      s.read(BYTES_TO_READ)
    }
    rc = 0
  rescue Timeout::Error, Errno::ECONNRESET => e
    failures += 1
    logMsg += "failure ##{failures} (e.class) | "
    if failures == 3 then
      logMsg += "final-failure(3 times) | "
      rc = 1
    else
      s.close()
      logMsg += "retrying | "
      retry
    end
  rescue Errno::ECONNREFUSED
    logMsg += "final-failure(ECONNREFUSED) | "
    rc = 0 # the 1st-gen nannies will handle this
  ensure
    logMsg += "in 1st ensure clause | "
    s.close()
  end
rescue # do nothing
  logMsg "in main rescue | "
ensure
  begin
    logMsg += "about to exit with rc=#{rc}\n"
    if rc != 0 then
      puts "failure"
      f.write(logMsg)
      f.flush()
    else
      puts "success"
    end
    Process.exit!(rc)
  rescue Exception => e
    f.write(Time.now.strftime("%Y %m %d %H:%M:%S") + " | whut! | #{e.class} | #{e.message} | #{e.backtrace.inspect}\n")
    f.flush()
    Process.exit!(rc)
  end
end
