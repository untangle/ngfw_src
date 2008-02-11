package com.untangle.uvm.toolbox;


public class ConfigItem {
	private String name;
	private String displayName;
    private int viewPosition;
    private String image;
	
	public ConfigItem() {
	}
	
    public ConfigItem(String name, String displayName, 
    		int viewPosition, String image) {
		super();
		this.name = name;
		this.displayName = displayName;
		this.viewPosition = viewPosition;
		this.image = image;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public int getViewPosition() {
		return viewPosition;
	}

	public void setViewPosition(int viewPosition) {
		this.viewPosition = viewPosition;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

}
