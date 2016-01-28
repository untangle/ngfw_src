//Reports servlet main
var rpc = {}; // the main json rpc object
var testMode = false;

// Main object class
Ext.define("Ung.Main", {
    singleton: true,
    debugMode: false,
    buildStamp: null,
    // the Ext.Viewport object for the application
    viewport: null,
    init: function(config) {
        Ext.MessageBox.wait(i18n._("Starting..."), i18n._("Please wait"));
        Ext.apply(this, config);
        if (Ext.isGecko) {
            document.onkeypress = function(e) {
                if (e.keyCode==27) {
                    return false;
                }
                return true;
            };
        }
        JSONRpcClient.toplevel_ex_handler = Ung.Util.handleException;
        JSONRpcClient.max_req_active = 25;

        this.initSemaphore = 4;
        if(Ext.supports.LocalStorage) {
            Ext.state.Manager.setProvider(Ext.create('Ext.state.LocalStorageProvider'));
        }
        rpc = {};
        rpc.jsonrpc = new JSONRpcClient("/reports/JSON-RPC");
        
        rpc.jsonrpc.ReportsContext.skinManager(Ext.bind(function(result,exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.skinManager = result;
            rpc.skinManager.getSettings(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                rpc.skinSettings = result;
                Ung.Util.loadCss("/skins/"+rpc.skinSettings.skinName+"/css/common.css");
                Ung.Util.loadCss("/skins/"+rpc.skinSettings.skinName+"/css/admin.css");
            },this));
        }, this));
        
        rpc.jsonrpc.ReportsContext.reportsManager(Ext.bind(function(result,exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.reportsManager = result;
            rpc.reportsManager.isReportsEnabled(Ext.bind(function( result, exception ) {
                if(Ung.Util.handleException(exception)) return;
                rpc.reportsEnabled = result;
                if(rpc.reportsEnabled) {
                    rpc.reportsManager.getCurrentApplications(Ext.bind(function( result, exception ) {
                        if(Ung.Util.handleException(exception)) return;
                        rpc.currentApplications = result.list;
                        rpc.reportsManager.getPoliciesInfo(Ext.bind(function( result, exception ) {
                            if(Ung.Util.handleException(exception)) return;
                            rpc.policyNamesMap = {};
                            rpc.policyNamesMap[0] = i18n._("No Rack");
                            var policy;
                            for(var i=0; i<result.list.length; i++) {
                                policy=result.list[i];
                                rpc.policyNamesMap[policy.policyId] = policy.name;
                            }
                            this.startApplication();
                        },this));
                    },this));
                } else {
                    this.startApplication();
                }
            },this));
            rpc.reportsManager.getTimeZoneOffset(Ext.bind(function( result, exception ) {
                if(Ung.Util.handleException(exception)) return;
                rpc.timeZoneOffset = result;
                this.startApplication();
            }, this));
        }, this));

        rpc.jsonrpc.ReportsContext.languageManager(Ext.bind(function( result, exception ) {
            if(Ung.Util.handleException(exception)) return;
            rpc.languageManager = result;
            // get translations for main module
            rpc.languageManager.getTranslations(Ext.bind(function( result, exception ) {
                if(Ung.Util.handleException(exception)) return;
                rpc.translations = result;
                this.startApplication();
            }, this), "untangle");
            
            rpc.languageManager.getLanguageSettings(Ext.bind(function( result, exception ) {
                if(Ung.Util.handleException(exception)) return;
                rpc.languageSettings = result;
                if(rpc.languageSettings.language) {
                    Ung.Util.loadScript('/ext6/classic/locale/locale-' + rpc.languageSettings.language + '.js');
                    this.startApplication();
                }
            },this));
        }, this));
    },
    getPolicyName: function(policyId) {
        if (Ext.isEmpty(policyId)){
            return i18n._( "Services" );
        }
        if (rpc.policyNamesMap[policyId] !== undefined) {
            return rpc.policyNamesMap[policyId];
        } else {
            return i18n._( "Unknown Rack" );
        }
    },
    buildReportsNotEnabled: function() {
        var items = [{
            xtype: "component",
            margin: 30,
            style: 'font-family: sans-serif; font-weight:bold; font-size: 20px;',
            html: i18n._("Reports is not installed into your rack or it is not turned on.")
        }];
        return items;
    },
    buildReportsViewer: function() {
        var treeNodes = [ {
            text : i18n._('Summary'),
            category : 'Summary',
            leaf : true,
            icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/icons/icon_summary.png'
        }, {
            text : i18n._('Host Viewer'),
            category : 'Host Viewer',
            leaf : true,
            icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/icons/icon_hosts.png'
        }, {
            text : i18n._('Device List'),
            category : 'Device List',
            leaf : true,
            icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/icons/icon_hosts.png' //FIXME icon
        }, {
            text : i18n._("Configuration"),
            leaf : false,
            expanded : true,
            children : [ {
                text : i18n._('Network'),
                category : 'Network',
                leaf : true,
                icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_network_17x17.png'
            }, {
                text : i18n._('Administration'),
                category : 'Administration',
                leaf : true,
                icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_admin_17x17.png'
            }, {
                text : i18n._('System'),
                category : 'System',
                leaf : true,
                icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/config/icon_config_system_17x17.png'
            }, {
                text : i18n._("Shield"),
                category : "Shield",
                leaf : true,
                icon :'/skins/'+rpc.skinSettings.skinName+'/images/admin/apps/untangle-node-shield_17x17.png'
            } ]
        }];
        if (rpc.currentApplications) {
            var i, app, apps = [];
            for (i = 0; i < rpc.currentApplications.length; i++) {
                app = rpc.currentApplications[i];
                if(app.name != 'untangle-node-branding-manager' && app.name != 'untangle-node-live-support' ) {
                    apps.push({
                        text : app.displayName,
                        category : app.displayName,
                        leaf : true,
                        icon : '/skins/'+rpc.skinSettings.skinName+'/images/admin/apps/'+app.name+'_17x17.png'
                    });
                }
            }
            if(apps.length > 0) {
                treeNodes.push({
                    text : i18n._("Applications"),
                    leaf : false,
                    expanded : true,
                    children : apps
                });
            }
        }

        var items = [{
            xtype : 'treepanel',
            region : 'west',
            margin : '1 1 0 1',
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
            store: Ext.create('Ext.data.TreeStore', {
                root : {
                    expanded : true,
                    children : treeNodes
                }
            }),
            selModel : {
                selType : 'rowmodel',
                listeners : {
                    select : Ext.bind(function(rowModel, record, rowIndex, eOpts) {
                        this.panelReports.setConfig("icon", record.get("icon"));
                        this.panelReports.setTitle(record.get("category"));
                        this.panelReports.setCategory(record.get("category"));
                    }, this)
                }
            }
        }, this.panelReports = Ext.create('Ung.panel.Reports', {
            region : "center",
            webuiMode: false
        })];
        return items;
    },
    startApplication: function() {
        this.initSemaphore--;
        if (this.initSemaphore != 0) {
            return;
        }

        i18n = Ext.create('Ung.I18N',{
            map: rpc.translations,
            timeoffset: (new Date().getTimezoneOffset()*60000) + rpc.timeZoneOffset
        });

        this.viewport = Ext.create('Ext.container.Viewport',{
            layout:'border',
            items: [
            {
                xtype:'panel',
                region: 'north',
                layout:'border',
                padding: '7 5 7 7',
                height: 70,
                border: false,
                style: 'background-color: #F0F0F0;',
                bodyStyle: 'background-color: #F0F0F0;',
                items: [{
                    xtype:'container',
                    html: '<img src="/images/BrandingLogo.png?'+(new Date()).getTime()+'" border="0" height="50"/>',
                    region: 'west',
                    width: 100
                }, {
                    xtype: 'label',
                    height: 60,
                    style: 'font-family: sans-serif;font-weight:bold;font-size:37px;padding-left:15px;',
                    text: i18n._('Reports'),
                    region: 'center'
                }, {
                    xtype:'container',
                    region: 'east',
                    height: 60,
                    width: 350,
                    margin: '0 10 0 0',
                    layout: {type: 'vbox', align: 'right'},
                    items: [{
                        xtype: 'component',
                        html: '<a class="link" href="/auth/logout?url=/reports&realm=Reports">'+i18n._('Logout')+"</a>",
                        margin: '0 0 10 0'
                    }]
                }]
            }, {
                xtype:'panel',
                border: false,
                region:"center",
                layout:"border"
            }]
        });
        
        var contentItems = rpc.reportsEnabled? this.buildReportsViewer(): this.buildReportsNotEnabled();
        this.viewport.down("panel[region=center]").add(contentItems);
        
        Ext.MessageBox.hide();
        if(rpc.reportsEnabled) {
            this.viewport.down("treepanel").getSelectionModel().select(0);
        }
    }
});


//Ext overrides used in reports serlvet
Ext.override(Ext.grid.column.Column, {
    defaultRenderer: Ext.util.Format.htmlEncode
});

Ext.apply(Ext.data.SortTypes, {
    // Timestamp sorting
    asTimestamp: function(value) {
        return value.time;
    },
    // Ip address sorting. may contain netmask.
    asIp: function(value){
        if(Ext.isEmpty(value)) {
            return null;
        }
        var i, len, parts = (""+value).replace(/\//g,".").split('.');
        for(i = 0, len = parts.length; i < len; i++){
            parts[i] = Ext.String.leftPad(parts[i], 3, '0');
        }
        return parts.join('.');
    }
});