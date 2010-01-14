package com.untangle.uvm.addrbook;

public interface DirectoryEntry {
    public enum Type { USER, GROUP };
    
    public Type getType();
    
    public UserEntry getUserEntry();
    
    public GroupEntry getGroupEntry();
}
