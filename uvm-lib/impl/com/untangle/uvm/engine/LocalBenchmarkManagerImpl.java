package com.untangle.uvm.engine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.benchmark.Benchmark;
import com.untangle.uvm.benchmark.Event;
import com.untangle.uvm.benchmark.LocalBenchmarkManager;
import com.untangle.uvm.security.NodeId;

public class LocalBenchmarkManagerImpl implements LocalBenchmarkManager
{
    private boolean isEnabled = false;
    
    /**
     * Map of all of the benchmarks, this is optimized for fast updates but not for 
     * queries.  The queries have to iterate the entire list in order to find a value.
     * The String is the "#{tid}-#{name}".  This can be generated quickly.  An added
     * optimization would be to create a second map that just mapped NodeId to a List
     * of benchmarks and nodeName to a list of benchmarks.  But since querying is not
     * the common use case, this doesn't matter.
     * 
     */
    private final Map<String,Benchmark> benchmarks = new HashMap<String,Benchmark>();
    
    @Override
    public boolean isEnabled()
    {
        return this.isEnabled;
    }
    
    @Override
    public void isEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }
    
    @Override
    public List<Benchmark> getBenchmark(String nodeName) {
        List<Benchmark> benchmarkList = new LinkedList<Benchmark>();
        
        for ( Benchmark benchmark : this.benchmarks.values()) {
            if ( benchmark.getNodeName().equals(nodeName)) {
                benchmarkList.add(benchmark);
            }
        }
        
        return benchmarkList;
    }

    @Override
    public Benchmark getBenchmark(NodeId tid, String name) {
        String key = buildKey(tid,name);
        
        Benchmark benchmark = this.benchmarks.get(key);
        return benchmark;
    }
    
    @Override
    public Benchmark getBenchmark(NodeId tid, String name, boolean create) {
        String key = buildKey(tid,name);
        
        Benchmark benchmark = this.benchmarks.get(key);
        if ( benchmark == null ) {
            benchmark = new Benchmark(tid,tid.getNodeName(),name);
            this.benchmarks.put(key, benchmark);
        }
        
        return benchmark;
    }

    @Override
    public List<Benchmark> getBenchmarks() {
        List<Benchmark> benchmarkList = new LinkedList<Benchmark>();
        
        benchmarkList.addAll(this.benchmarks.values());
        
        return benchmarkList;
    }

    @Override
    public List<Benchmark> getBenchmarks(NodeId tid) {
        List<Benchmark> benchmarkList = new LinkedList<Benchmark>();
        
        for ( Benchmark benchmark : this.benchmarks.values()) {
            if ( benchmark.getNodeId().equals(tid)) {
                benchmarkList.add(benchmark);
            }
        }
        
        return benchmarkList;
    }
    
    @Override
    public void resetBenchmarks()
    {
        for ( Benchmark benchmark : this.benchmarks.values()) {
            benchmark.resetEvents();
        }
    }

    @Override
    public void updateBenchmark(NodeId tid, String name, Event event, long value) {
        String key = buildKey(tid,name);
        
        /* Since this isn't synchronized, it might create one twice but the extra
         * ones will get garbage collected.
         */
        Benchmark benchmark = this.benchmarks.get(key);
        if ( benchmark == null ) {
            benchmark = new Benchmark(tid, tid.getNodeName(), name);
            this.benchmarks.put(key,benchmark);
        }
        benchmark.addEvent(event, value);
    }
    
    private String buildKey(NodeId tid, String name)
    {
        return String.valueOf( tid.getId()) + "-" + name;
    }

}
