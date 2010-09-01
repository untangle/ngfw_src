package com.untangle.uvm.benchmark;

import com.untangle.uvm.security.NodeId;

public interface LocalBenchmarkManager extends RemoteBenchmarkManager
{
    public void updateBenchmark( NodeId tid, String name, Event event, long value );
    
    /**
     * Retrieve a benchmark.  This can optionally create a new benchmark if one doesn't exist.
     * @param tid
     * @param name
     * @param create
     * @return
     */
    public Benchmark getBenchmark( NodeId tid, String name, boolean create );
}
