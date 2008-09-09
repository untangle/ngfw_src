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
            Ung.Upgrade.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Upgrade.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 5 tabs
        },
        initSubCmps : function() {
            this.buildUpgrade();
            this.buildSetup();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.gridUpgrade, this.panelSetup]);
            this.tabs.activate(this.gridUpgrade);
            this.loadGridUpgrade();
        },
        getUpgradeSettings : function(forceReload) {
            if (forceReload || this.rpc.upgradeSettings === undefined) {
                this.rpc.upgradeSettings = rpc.toolboxManager.getUpgradeSettings();
            }
            return this.rpc.upgradeSettings;
        },
        loadGridUpgrade : function() {
            rpc.toolboxManager.upgradable(function(result, exception) {
                if (exception) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                    return;
                }
                var upgradeList = result;
                // var upgradeList=[]; /for test
                // for(var i=0;i<main.nodes.length;i++) {
                // upgradeList.push(main.nodes[i].md);
                // }
                if (upgradeList.length > 0) {
                    Ext.getCmp("config_start_upgrade_button").enable();
                    var upgradeData = [];
                    var somethingVisibleAdded = false;
                    for (var i = 0; i < upgradeList.length; i++) {
                        var md = upgradeList[i];
                        if (md.type != "CASING" && md.type != "LIB_ITEM" && md.type != "TRIAL" && md.type != "BASE") {
                            somethingVisibleAdded = true;
                            upgradeData.push({
                                image : "image?name=" + md.name,
                                name : md.name,
                                displayName : md.displayName,
                                availableVersion : md.availableVersion,
                                type : md.type == "LIBRARY" ? this.i18n._("System Component") : md.type == "NODE"
                                        ? this.i18n._("Product")
                                        : this.i18n._("Unknown"),
                                size : Math.round(md.size / 1000)
                            })
                        }
                    }
                    if (!somethingVisibleAdded) {
                        var size = 0;
                        for (var i = 0; i < upgradeList.length; i++) {
                            size += upgradeList[i].size;
                        }
                        upgradeData.push({
                            image : "image?name=unknown",
                            name : md.name,
                            displayName : this.i18n._("Various Updates"),
                            availableVersion : this.i18n._("N/A"),
                            type : this.i18n._("Misc."),
                            size : Math.round(md.size / 1000)
                        });
                    }
                } else {
                    Ext.getCmp("config_start_upgrade_button").disable();
                }
                this.gridUpgrade.getStore().proxy.data = {
                    list : upgradeData
                };
                this.gridUpgrade.getStore().load();
            }.createDelegate(this));

        },
        buildUpgrade : function() {
            this.gridUpgrade = new Ext.grid.GridPanel({
                // private fields
                name : 'Upgrade',
                parentId : this.getId(),
                title : this.i18n._('Upgrade'),
                enableHdMenu : false,
                enableColumnMove: false,
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
                buttonAlign : 'center',
                buttons : [{
                    id : 'config_start_upgrade_button',
                    text : i18n._('Upgrade'),
                    name : "Upgrade",
                    iconCls : 'iconUpgrade',
                    disabled : true,
                    handler : function() {
                        this.gridUpgrade.upgrade();
                    }.createDelegate(this)
                }],
                // called when the component is rendered
                onRender : function(container, position) {
                    Ext.grid.GridPanel.prototype.onRender.call(this, container, position);
                    this.getGridEl().child("div[class*=x-grid3-viewport]").set({
                        'name' : "Table"
                    });
                },
                upgrade : function() {
                    if (Ung.Upgrade.PerformUpgradeThread.started) {
                        return;
                    }
                    Ext.MessageBox.wait(i18n._("Downloading updates..."), i18n._("Please wait"));
                    rpc.toolboxManager.upgrade(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        var key = result;
                        
                    }.createDelegate(this));
                }

            });

        },
        buildSetup : function() {
            this.panelSetup = new Ext.Panel({
                // private fields
                name : 'Upgrade Setup',
                parentId : this.getId(),
                title : this.i18n._('Upgrade Setup'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Automatic Upgrade'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
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
                        bodyStyle : 'padding:5px 0px 5px 30px;',
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
                        bodyStyle : 'padding:5px 0px 5px 30px;',
                        html : this.i18n
                                ._("If new upgrades are available at the specified upgrade time they will be not be installed. All upgrades must be manually installed using the button on the Upgrade tab.")
                    }, {
                        html : "<i>" + this.i18n._("Note: Turning off Automatic Upgrades does not disable signature & list updates")
                                + "</i>"
                    }]
                }, {
                    title : this.i18n._('Upgrade Time'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
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
                        value : this.getUpgradeSettings().period.hour + ":" + this.getUpgradeSettings().period.minute,
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
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.saveSemaphore = 1;
                // save language settings

                rpc.toolboxManager.setUpgradeSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getUpgradeSettings());
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                Ext.MessageBox.hide();
                this.cancelAction();
            }
        }

    });
    Ung.Upgrade.PerformUpgradeThread = {
        // update interval in millisecond
        updateTime : 1000,
        key : null,
        started : false,
        intervalId : null,
        cycleCompleted : true,

        start : function(key) {
            this.stop();
            this.key = key;
            this.intervalId = window.setInterval("Ung.Upgrade.PerformUpgradeThread.run()", this.updateTime);
            this.started = true;
        },

        stop : function() {
            if (this.intervalId !== null) {
                window.clearInterval(this.intervalId);
            }
            this.cycleCompleted = true;
            this.started = false;
        },
        run : function() {
            if (!this.cycleCompleted) {
                return;
            }
            //TODO: getProgress is no longer available, what is the alternative?
            rpc.toolboxManager.getProgress(function(result, exception) {
               if (exception) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message, function() {
                        this.cycleCompleted = true;
                    }.createDelegate(this));
                    return;
               }
               this.cycleCompleted = true;
               try {
                   var ipList=result;
                   if(ipList.list!=null && ipList.list.length>0) {
                       var hasNodeInstantiated=false;
                       for(var i=0;i<ipList.length;i++) {
                           var msg=ipList.list[i];
                            if (msg.javaClass.indexOf("DownloadComplete") != -1) {
                            } else if (msg.javaClass.indexOf("DownloadProgress") != -1) {
                            } 
                        }
                    }
                } catch (err) {
                    Ext.MessageBox.alert("Exception in PerformUpgradeThread", err.message);
                }
            }.createDelegate(this), this.key);
        }
    };

}
