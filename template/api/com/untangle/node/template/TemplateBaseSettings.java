package com.untangle.node.template;

import java.io.Serializable;

import javax.persistence.Column;

public class TemplateBaseSettings implements Serializable
{
	private boolean sampleSetting;
	
    public TemplateBaseSettings()
    {
    }

    @Column(name="sample_setting", nullable=false)
    public boolean getSampleSetting()
    {
        return this.sampleSetting;
    }

    public void setSampleSetting(boolean newValue)
    {
        this.sampleSetting = newValue;
    }
}
