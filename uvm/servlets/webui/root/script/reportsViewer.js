Ext.define('Ung.reportsViewer', {
    extend : 'Ext.Container',
    layout: "border",
    initComponent : function() {
        if (!rpc.reportsEnabled) {
            this.items = [{
                region: 'center',
                xtype: 'component',
                cls: 'main-panel',
                padding: 10,
                html: i18n._("Reports application is required for this feature. Please install and enable the Reports application.")
            }];
            this.callParent(arguments);
            return;
        }
        var treeApps = [ {
            text : i18n._('Summary'),
            category : 'Summary',
            leaf : true,
            icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/icons/icon_summary.png'
        }, {
            text : i18n._('Hosts'),
            category : 'Hosts',
            leaf : true,
            icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_hosts_16x16.png'
        }, {
            text : i18n._('Devices'),
            category : 'Devices',
            leaf : true,
            icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_devices_16x16.png'
        }, {
            text : i18n._("Configuration"),
            leaf : false,
            expanded : true,
            children : [ {
                text : i18n._('Network'),
                category : 'Network',
                leaf : true,
                icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_network_16x16.png'
            }, {
                text : i18n._('Administration'),
                category : 'Administration',
                leaf : true,
                icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_admin_16x16.png'
            }, {
                text : i18n._('Events'),
                category : 'Events',
                leaf : true,
                icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_events_16x16.png'
            }, {
                text : i18n._('System'),
                category : 'System',
                leaf : true,
                icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_system_16x16.png'
            }, {
                text : i18n._("Shield"),
                category : "Shield",
                leaf : true,
                icon :'/skins/'+rpc.skinSettings.skinName+'/images/admin/apps/shield_17x17.png' 
            } ]
        }];

        this.items = [ {
            xtype : 'treepanel',
            region : 'west',
            autoScroll : true,
            rootVisible : false,
            title : i18n._('Reports'),
            enableDrag : false,
            width : 200,
            minWidth : 65,
            maxWidth : 350,
            split : true,
            collapsible: true,
            collapsed: false,
            store : Ext.create('Ext.data.TreeStore', {
                root : {
                    expanded : true,
                    children : treeApps
                }
            }),
            selModel : {
                selType : 'rowmodel',
                listeners : {
                    select : Ext.bind(function(rowModel, record, rowIndex, eOpts) {
                        this.panelReports.setConfig("icon", record.get("icon"));
                        this.panelReports.down('#panelEntries').setTitle(record.get("category"));
                        this.panelReports.setCategory(record.get("category"), this.initialEntry);
                    }, this)
                }
            }
        }, this.panelReports = Ext.create('Ung.panel.Reports', {
            region : "center",
            header: false
        }) ];
        this.callParent(arguments);
        
        this.treepanel = this.down("treepanel"); 
        Ung.Main.getReportsManager().getCurrentApplications(Ext.bind(function( result, exception ) {
            if(Ung.Util.handleException(exception)) return;
            if(!this.getEl()) return;
            var currentApplications = result.list;
            if (currentApplications) {
                var i, app, apps = [];
                for (i = 0; i < currentApplications.length; i++) {
                    app = currentApplications[i];
                    if(app.name != 'branding-manager' && app.name != 'live-support' ) {
                        apps.push({
                            text : app.displayName,
                            category : app.displayName,
                            leaf : true,
                            icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/apps/'+app.name+'_17x17.png'
                        });
                    }
                }
                if(apps.length > 0) {
                    this.treepanel.getStore().getRoot().appendChild({
                        text : i18n._("Applications"),
                        leaf : false,
                        expanded : true,
                        children : apps
                    });
                }
            }
            this.openTarget();
        },this));
    },
    openTarget: function() {
        if(!this.getEl()) return;
        if(Ung.Main.target) {
            var targetTokens = Ung.Main.target.split(".");
            if(targetTokens.length >= 2 && targetTokens[1] !=null ) {
                var rootApp = this.treepanel.getStore().getRoot();
                rootApp.cascadeBy({
                    before: function(app) {
                        if(app.get("category") == targetTokens[1]){
                            if(targetTokens.length >= 4 && targetTokens[2] !=null && targetTokens[3] !=null ) {
                                this.initialEntry={type: targetTokens[2], entryId: targetTokens[3]};
                            }
                            this.treepanel.getSelectionModel().select(app);
                            delete this.initialEntry;
                            return false;
                        }
                    },
                    scope: this
                });
            }
            delete Ung.Main.target;
        } else {
            this.treepanel.getSelectionModel().select(0);
        }
    }
});