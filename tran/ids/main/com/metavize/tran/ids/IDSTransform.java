package com.metavize.tran.ids;


import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tran.Transform;

public interface IDSTransform extends Transform {
    IDSSettings getIDSSettings();
    void setIDSSettings(IDSSettings settings);
    EventManager<IDSLogEvent> getEventManager();
}
