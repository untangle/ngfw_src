## A rush script for retrieving the network information

bm = Untangle::RemoteUvmContext.benchmarkManager()
puts "Benchmark manager is currently: #{bm.isEnabled()}"
bm.isEnabled( ARGV[0] == "true" ? true : false  )
puts "Benchmark manager is now set to: #{bm.isEnabled()}"


benchmarks = bm.getBenchmarks()
benchmarks.each do |benchmark|
  benchmark["avg"] = []
  benchmark["total"].each_index do |t| 
    
    benchmark["avg"] << ((benchmark["count"][t]  == 0 ) ? 0 : ( benchmark["total"][t] / benchmark["count"][t] ))
  end

  puts "Benchmark<#{benchmark["nodeName"]}, #{benchmark["name"]}>: #{benchmark["min"].inspect} #{benchmark["max"].inspect} #{benchmark["avg"].inspect}"
end

