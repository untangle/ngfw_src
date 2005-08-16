package com.metavize.tran.ids;

import java.util.List;

import com.metavize.mvvm.tran.Transform;

public interface IDSTransform extends Transform {
	IDSSettings getIDSSettings();
	void setIDSSettings(IDSSettings settings);

	List<IDSLog> getLogs(int limit);
}
