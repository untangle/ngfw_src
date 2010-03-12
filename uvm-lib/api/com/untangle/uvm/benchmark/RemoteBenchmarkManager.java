package com.untangle.uvm.benchmark;

import java.util.List;

import com.untangle.uvm.security.Tid;

public interface RemoteBenchmarkManager {
    public boolean isEnabled();
    
    public void isEnabled(boolean isEnabled);
   
    /**
     * Get all of the benchmarks for every node and the totals.
     * @return List of all of the benchmarks
     */
    public List<Benchmark> getBenchmarks();
    
    /**
     * Get all of the benchmarks for a particular tid.
     * @param tid The tid of the benchmark to lookup or 0 for the totals.
     * @return List of the benchmarks or null if none exist for this tid.
     */
    public List<Benchmark> getBenchmarks(Tid tid);
    
    /**
     * Get all of the benchmarks for a particular node name.
     * @param nodeName The name of the node to lookup, eg untangle-node-spamassasin.
     * @return List of all of the benchmarks that correspond to a particular node.
     */
    public List<Benchmark> getBenchmark(String nodeName);
    
    /**
     * Get the benchmarks for a particular tid and pipeline spec.
     * @param tid
     * @param name
     * @return
     */
    public Benchmark getBenchmark(Tid tid, String name );
    
    /**
     * Reset all of the benchmarks.
     */
    public void resetBenchmarks();
}
