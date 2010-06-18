if (!Ung.hasResource["Ung.Upgrade"]) {
    Ung.hasResource["Ung.Upgrade"] = true;

    Ung.Upgrade = Ext.extend(Ung.ConfigWin, {
        gridUpgrade : null,
        panelSetup : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Upgrade')
            }];
            this.buildUpgrade();
            this.buildSetup();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.gridUpgrade, this.panelSetup]);
            this.tabs.activate(this.gridUpgrade);
            Ung.Upgrade.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Upgrade.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
        },
        initSubCmps : function()
        {
            this.loadGridUpgrade();
        },
        getUpgradeSettings : function(forceReload) {
            if (forceReload || this.rpc.upgradeSettings === undefined) {
            	try {
                    this.rpc.upgradeSettings = rpc.toolboxManager.getUpgradeSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.upgradeSettings;
        },
        loadGridUpgrade : function() {
            Ext.MessageBox.wait(i18n._("Checking for available upgrades..."), i18n._("Please wait"));
            rpc.toolboxManager.getUpgradeStatus(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                var upgradeStatus=result;
                if(upgradeStatus.upgrading) {
                    Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Upgrade in progress."));
                } else {
                    rpc.toolboxManager.getUpgradeStatus(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        rpc.toolboxManager.upgradable(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            Ext.MessageBox.hide();
                            if(!this.isVisible()) return;
                            var upgradeList = result;
                            if (upgradeList.length > 0) {
                            	Ext.getCmp("configItem_upgrade").setIconCls("icon-config-upgrade-available");
                                Ext.getCmp("config_start_upgrade_button").enable();
                                var upgradeData = [];
                                var somethingVisibleAdded = false;
                                var totalSize=0;
                                for (var i = 0; i < upgradeList.length; i++) {
                                    var md = upgradeList[i];
				    var mtype;
				    var displayName = md.displayNane;
                    displayName.replace("Untangle",main.getBrandingManager().getCompanyName());
                    totalSize+=md.size;
				    // Leave out only libitems
				    switch (md.type) {
				    case "LIB_ITEM":
				    case "TRIAL":
				      // Skip
				      continue;
				    case "LIBRARY":
				    case "BASE":
				    case "CASING":
				      mtype = this.i18n._("System Component");
				      break;
				    case "NODE":
				    case "SERVICE":
				      mtype = this.i18n._("Product");
				      break;
				    case "UNKNOWN":
				      mtype = this.i18n._("System Component");
				      break;
				    }
				    if (displayName == null) {
				      if (md.shortDescription != null)
					displayName = md.shortDescription;
				      else
					displayName = md.name;
				    }
				    somethingVisibleAdded = true;
				    upgradeData.push({
				      image : "image?name=" + md.name,
				      name : md.name,
				      displayName : displayName,
				      availableVersion : md.availableVersion,
				      type : mtype,
				      size : Math.round(md.size / 1000)
				    });
                                }
                                if (!somethingVisibleAdded) {
                                    upgradeData.push({
                                        image : "image?name=unknown",
                                        name : md.name,
                                        displayName : this.i18n._("Various Updates"),
                                        availableVersion : this.i18n._("N/A"),
                                        type : this.i18n._("Misc."),
                                        size : Math.round(totalSize / 1000)
                                    });
                                }
                                this.gridUpgrade.getTopToolbar().items.get(0).getEl().innerHTML=String.format(i18n._("Upgrades are available. There are {0} packages. Total size is {1} MBs."),upgradeList.length,Ung.Util.bytesToMBs(totalSize));
                            } else {
                                Ext.getCmp("config_start_upgrade_button").disable();
                                this.gridUpgrade.getTopToolbar().items.get(0).getEl().innerHTML=i18n._("No upgrades available.");
                            }
                            this.gridUpgrade.getStore().proxy.data = {
                                list : upgradeData
                            };
                            this.gridUpgrade.getStore().load();
                        }.createDelegate(this));
                    }.createDelegate(this),true);
                }
            }.createDelegate(this), false);

        },
        buildUpgrade : function() {
            this.gridUpgrade = new Ext.grid.GridPanel({
                // private fields
                name : 'Upgrade',
                helpSource : 'upgrade',
                parentId : this.getId(),
                title : this.i18n._('Upgrade'),
                enableHdMenu : false,
                enableColumnMove: false,
                disableSelection: true,
                tbar: [{xtype: 'tbtext', text: "."}],
                store : new Ext.data.Store({
                    proxy : new Ung.MemoryProxy({
                        root : 'list'
                    }),
                    reader : new Ext.data.JsonReader({
                        totalProperty : "totalRecords",
                        root : 'list',
                        fields : [{
                            name : 'image'
                        }, {
                            name : 'name'
                        }, {
                            name : 'displayName'
                        }, {
                            name : 'availableVersion'
                        }, {
                            name : 'type'
                        }, {
                            name : 'size'
                        }]
                    })
                }),
                columns : [{
                    header : "",
                    width : 70,
                    sortable : true,
                    dataIndex : 'image',
                    renderer : function(value) {
                        return "<img src='" + value + "'/>";
                    }
                }, {
                	id: 'displayName',
                    header : this.i18n._("name"),
                    width : 190,
                    sortable : true,
                    dataIndex : 'displayName'

                }, {
                    header : this.i18n._("new version"),
                    width : 230,
                    sortable : true,
                    dataIndex : 'availableVersion'

                }, {
                    header : this.i18n._("type"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'type'
                }, {
                    header : this.i18n._("size (kb)"),
                    width : 110,
                    sortable : true,
                    align: 'right', 
                    dataIndex : 'size'
                }],
                autoExpandColumn: 'displayName',
                buttonAlign : 'center',
                buttons : [{
                    id : 'config_start_upgrade_button',
                    text : i18n._('Upgrade'),
                    name : "Upgrade",
                    iconCls : 'icon-upgrade',
                    disabled : true,
                    handler : function() {
                        main.upgrade();
                    }.createDelegate(this)
                }],
                // called when the component is rendered
                onRender : function(container, position) {
                    Ext.grid.GridPanel.prototype.onRender.call(this, container, position);
                    this.getGridEl().child("div[class*=x-grid3-viewport]").set({
                        'name' : "Table"
                    });
                }
            });

        },
        buildSetup : function() {
            // keep initial upgrade settings
            this.initialUpgradeSettings = Ung.Util.clone(this.getUpgradeSettings());
            
            var upgradeTime=new Date();
            upgradeTime.setTime(0);
            upgradeTime.setHours(this.getUpgradeSettings().period.hour);
            upgradeTime.setMinutes(this.getUpgradeSettings().period.minute);
        	
            this.getUpgradeSettings().period.hour + ":" + this.getUpgradeSettings().period.minute
            this.panelSetup = new Ext.Panel({
                // private fields
                name : 'Upgrade Setup',
                helpSource : 'upgrade_setup',
                parentId : this.getId(),
                title : this.i18n._('Upgrade Setup'),
                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Automatic Upgrade'),
                    items : [{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Automatically Install Upgrades'),
                        hideLabel : true,
                        name : 'Automatically Install Upgrades',
                        checked : this.getUpgradeSettings().autoUpgrade,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getUpgradeSettings().autoUpgrade = checked;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        cls: 'description',
                        border : false,
                        html : this.i18n
                                ._("If new upgrades are available at the specified upgrade time they will be automatically downloaded and installed. During the install the system may be rebooted resulting in momentary loss of connectivicty.")
                    }, {
                        xtype : 'radio',
                        boxLabel : this.i18n._('Do Not Automatically Install Upgrades'),
                        hideLabel : true,
                        name : 'Automatically Install Upgrades',
                        checked : !this.getUpgradeSettings().autoUpgrade,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getUpgradeSettings().autoUpgrade = !checked;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        cls: 'description',
                        border : false,
                        html : this.i18n
                                ._("If new upgrades are available at the specified upgrade time they will be not be installed. All upgrades must be manually installed using the button on the Upgrade tab.")
                    }, {
                        cls: 'description',
                        border : false,
                        html : "<i>" + this.i18n._("Note: Turning off Automatic Upgrades does not disable signature & list updates")
                                + "</i>"
                    }]
                }, {
                    title : this.i18n._('Upgrade Time'),
                    items : [{
                        xtype : 'checkbox',
                        name : 'Sunday',
                        boxLabel : this.i18n._('Sunday'),
                        hideLabel : true,
                        checked : this.getUpgradeSettings().period.sunday,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getUpgradeSettings().period.sunday = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Monday',
                        boxLabel : this.i18n._('Monday'),
                        hideLabel : true,
                        checked : this.getUpgradeSettings().period.monday,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getUpgradeSettings().period.monday = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Tuesday',
                        boxLabel : this.i18n._('Tuesday'),
                        hideLabel : true,
                        checked : this.getUpgradeSettings().period.tuesday,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getUpgradeSettings().period.tuesday = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Wednesday',
                        boxLabel : this.i18n._('Wednesday'),
                        hideLabel : true,
                        checked : this.getUpgradeSettings().period.wednesday,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getUpgradeSettings().period.wednesday = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Thursday',
                        boxLabel : this.i18n._('Thursday'),
                        hideLabel : true,
                        checked : this.getUpgradeSettings().period.thursday,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getUpgradeSettings().period.thursday = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Friday',
                        boxLabel : this.i18n._('Friday'),
                        hideLabel : true,
                        checked : this.getUpgradeSettings().period.friday,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getUpgradeSettings().period.friday = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Saturday',
                        boxLabel : this.i18n._('Saturday'),
                        hideLabel : true,
                        checked : this.getUpgradeSettings().period.saturday,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getUpgradeSettings().period.saturday = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'timefield',
                        name : 'Update Time',
                        width : 90,
                        hideLabel : true,
                        // format : "H:i",
                        value : upgradeTime,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    // this.getUpgradeSettings().period.monday =
                                    // newValue;
                                    if (newValue != "") {
                                        var v = elem.parseDate(newValue);
                                        this.getUpgradeSettings().period.minute = v.dateFormat("i");
                                        this.getUpgradeSettings().period.hour = v.dateFormat("H");
                                    }
                                }.createDelegate(this)
                            }
                        }
                    }]
                }]
            });

        },
        
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        reloadSettings : function()
        {
            this.initialUpgradeSettings = Ung.Util.clone(this.getUpgradeSettings(true));
            this.loadGridUpgrade();
        },
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
        },
        // save function
        commitSettings : function(callback)
        {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.saveSemaphore = 1;
                // save language settings

                rpc.toolboxManager.setUpgradeSettings(function(result, exception) {
                    this.afterSave(exception,callback);
                }.createDelegate(this), this.getUpgradeSettings());
            }
        },
        afterSave : function(exception, callback)
        {
            if(Ung.Util.handleException(exception)) return;

            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                callback();
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getUpgradeSettings(), this.initialUpgradeSettings);
        }
    });
}
