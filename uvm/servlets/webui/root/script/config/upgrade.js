if (!Ung.hasResource["Ung.Upgrade"]) {
    Ung.hasResource["Ung.Upgrade"] = true;

     Ext.define('Ung.Upgrade', {
        extend: 'Ung.ConfigWin',
        gridUpgrade: null,
        panelSettings: null,
        initComponent: function() {
            this.breadcrumbs = [{
                title: i18n._("Configuration"),
                action: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            }, {
                title: i18n._('Upgrade')
            }];
            this.buildUpgrade();
            this.buildSettings();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.gridUpgrade, this.panelSettings]);
            this.callParent(arguments);
        },
        afterRender: function() {
            this.callParent(arguments);
            this.loadGridUpgrade();
            Ung.Util.clearDirty(this.panelSettings);
        },
        getSystemSettings: function(forceReload) {
            if (forceReload || this.rpc.systemSettings === undefined) {s
                try {
                    this.rpc.systemSettings = rpc.systemManager.getSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.systemSettings;
        },
        loadGridUpgrade: function() {
            Ext.MessageBox.wait(i18n._("Checking for available upgrades..."), i18n._("Please wait"));
            rpc.aptManager.getUpgradeStatus(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                var upgradeStatus=result;
                if(upgradeStatus.upgrading) {
                    Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Upgrade in progress."));
                } else {
                    rpc.aptManager.getUpgradeStatus(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        rpc.aptManager.upgradable(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            Ext.MessageBox.hide();
                            if(!this.isVisible()) return;
                            var upgradeList = result;
                            var upgradeData = [];
                            if (upgradeList.length > 0) {
                                Ext.getCmp("configItem_upgrade").setIconCls("icon-config-upgrade-available");
                                Ext.getCmp("config_start_upgrade_button").enable();
                                var somethingVisibleAdded = false;
                                var totalSize=0;
                                for (var i = 0; i < upgradeList.length; i++) {
                                    var md = upgradeList[i];
                                    var displayName = md.displayNane;
                                    totalSize+=md.size;
                                    if (displayName == null) {
                                    if (md.shortDescription != null)
                                        displayName = md.shortDescription;
                                    else
                                        displayName = md.name;
                                    } 
                                    if (displayName != null) {
                                        displayName = displayName.replace("Untangle",main.getOemManager().getOemName());
                                    }
                                    somethingVisibleAdded = true;
                                        upgradeData.push({
                                        image: "image?name=" + md.name,
                                        name: md.name,
                                        displayName: displayName,
                                        availableVersion: md.availableVersion,
                                        size: Math.round(md.size / 1000)
                                    });
                                }
                                if (!somethingVisibleAdded) {
                                    upgradeData.push({
                                        image: "image?name=unknown",
                                        name: "unknown",
                                        displayName: this.i18n._("Various Updates"),
                                        availableVersion: this.i18n._("N/A"),
                                        size: Math.round(totalSize / 1000)
                                    });
                                }
                                this.gridUpgrade.getDockedItems('toolbar[dock="top"]')[0].items.get(0).getEl().dom.innerHTML=Ext.String.format(i18n._("Upgrades are available. There are {0} packages. Total size is {1} MBs."),upgradeList.length,Ung.Util.bytesToMBs(totalSize));
                            } else {
                                Ext.getCmp("config_start_upgrade_button").disable();
                                this.gridUpgrade.getDockedItems('toolbar[dock="top"]')[0].items.get(0).getEl().dom.innerHTML=i18n._("No upgrades available.");
                            }
                            this.gridUpgrade.getStore().proxy.data = upgradeData;
                            this.gridUpgrade.getStore().load();
                        }, this));
                    }, this),true);
                }
            }, this), false);

        },
        buildUpgrade: function() {
            this.gridUpgrade = Ext.create('Ext.grid.Panel',{
                // private fields
                name: 'Upgrade',
                helpSource: 'upgrade',
                parentId: this.getId(),
                title: this.i18n._('Upgrade'),
                enableColumnHide: false,
                enableColumnMove: false,
                disableSelection: true, //TODO: find extjs4 solution
                tbar: [{xtype: 'tbtext', text: i18n._("Checking for upgrades...")}],
                isDirty: function() { return false;},
                store: Ext.create('Ext.data.Store', {
                    data:[],
                    proxy: {
                        type: 'memory',
                        reader: {
                            type: 'json',
                            root: ''
                        }
                    },
                    fields: [{
                            name: 'image'
                        }, {
                            name: 'name'
                        }, {
                            name: 'displayName'
                        }, {
                            name: 'availableVersion'
                        }, {
                            name: 'size'
                    }]
                }),
                columns: [{
                    header: "",
                    width: 70,
                    menuDisabled: true,
                    sortable: true,
                    dataIndex: 'image',
                    renderer: function(value) {
                        return "<img src='" + value + "'/>";
                    }
                }, {
                    header: this.i18n._("name"),
                    width: 190,
                    sortable: true,
                    menuDisabled: true,
                    flex: 1,
                    dataIndex: 'displayName'

                }, {
                    header: this.i18n._("new version"),
                    width: 230,
                    sortable: true,
                    menuDisabled: true,
                    dataIndex: 'availableVersion'

                }, {
                    header: this.i18n._("size (kb)"),
                    width: 110,
                    menuDisabled: true,
                    sortable: true,
                    align: 'right', 
                    dataIndex: 'size'
                }],
                buttonAlign: 'center',
                buttons: [{
                    id: 'config_start_upgrade_button',
                    text: i18n._('Upgrade'),
                    name: "Upgrade",
                    iconCls: 'icon-upgrade',
                    disabled: true,
                    handler: function() {
                        main.upgrade();
                    }
                }]
            });

        },
        buildSettings: function() {
            var upgradeTime=new Date();
            upgradeTime.setTime(0);
            upgradeTime.setHours(this.getSystemSettings().autoUpgradeHour);
            upgradeTime.setMinutes(this.getSystemSettings().autoUpgradeMinute);
            this.panelSettings = Ext.create('Ext.panel.Panel',{
                // private fields
                name: 'Upgrade Settings',
                helpSource: 'upgrade_setup',
                parentId: this.getId(),
                title: this.i18n._('Upgrade Settings'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset'
                },
                items: [{
                    title: this.i18n._('Automatic Upgrade'),
                    items: [{
                        xtype: 'radio',
                        boxLabel: this.i18n._('Automatically Install Upgrades'),
                        hideLabel: true,
                        name: 'Automatically Install Upgrades',
                        checked: this.getSystemSettings().autoUpgrade,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getSystemSettings().autoUpgrade = checked;
                                }, this)
                            }
                        }
                    }, {
                        cls: 'description',
                        border: false,
                        html: this.i18n._("If new upgrades are available at the specified upgrade time they will be automatically downloaded and installed. During the install the system may be rebooted resulting in momentary loss of connectivicty.")
                    }, {
                        xtype: 'radio',
                        boxLabel: this.i18n._('Do Not Automatically Install Upgrades'),
                        hideLabel: true,
                        name: 'Automatically Install Upgrades',
                        checked: !this.getSystemSettings().autoUpgrade,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getSystemSettings().autoUpgrade = !checked;
                                }, this)
                            }
                        }
                    }, {
                        cls: 'description',
                        border: false,
                        html: this.i18n._("If new upgrades are available at the specified upgrade time they will be not be installed. All upgrades must be manually installed using the button on the Upgrade tab.")
                    }, {
                        cls: 'description',
                        border: false,
                        html: "<i>" + this.i18n._("Note: Turning off Automatic Upgrades does not disable signature & list updates") + "</i>"
                    }]
                }, {
                    title: this.i18n._('Automatic Upgrade Schedule'),
                    items: [{
                        xtype: 'udayfield',
                        name: 'Upgrade Days',
                        value: this.getSystemSettings().autoUpgradeDays,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().autoUpgradeDays = elem.getValue();
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'timefield',
                        name: 'Upgrade Time',
                        width: 90,
                        hideLabel: true,
                        value: upgradeTime,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    if (newValue && newValue instanceof Date) {
                                        this.getSystemSettings().autoUpgradeMinute = newValue.getMinutes();
                                        this.getSystemSettings().autoUpgradeHour = newValue.getHours();
                                    }
                                }, this)
                            }
                        }
                    }]
                }]
            });
        },
        save: function (isApply) {
            rpc.systemManager.setSettings(Ext.bind(function(result, exception) {
                Ext.MessageBox.hide();
                if(Ung.Util.handleException(exception)) return;
                if (!isApply) {
                    this.closeWindow();
                } else {
                    this.loadGridUpgrade();
                    this.clearDirty();
                }
            }, this), this.getSystemSettings());
        }         
     });
}
//@ sourceURL=upgrade.js
