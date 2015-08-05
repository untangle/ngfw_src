//Reports servlet main
Ext.Loader.setConfig({
    enabled: true,
    disableCaching: false,
    paths: {
        'Ext.ux': '/ext5/examples/ux'
    }
});

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
        
        rpc.jsonrpc.ReportsContext.reportingManagerNew(Ext.bind(function(result,exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.reportingManagerNew = result;
            rpc.reportingManagerNew.isReportingEnabled(Ext.bind(function( result, exception ) {
                if(Ung.Util.handleException(exception)) return;
                rpc.isReportingEnabled = result;
                if(rpc.isReportingEnabled) {
                    rpc.reportingManagerNew.getCurrentApplications(Ext.bind(function( result, exception ) {
                        if(Ung.Util.handleException(exception)) return;
                        rpc.currentApplications = result.list;
                        this.startApplication();
                    },this));
                } else {
                    this.startApplication();
                }
            },this));
            rpc.reportingManagerNew.getTimeZoneOffset(Ext.bind(function( result, exception ) {
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
            }, this), "untangle-libuvm");
            
            rpc.languageManager.getLanguageSettings(Ext.bind(function( result, exception ) {
                if(Ung.Util.handleException(exception)) return;
                rpc.languageSettings = result;
                if(rpc.languageSettings.language) {
                    Ung.Util.loadScript('/ext5/packages/ext-locale/build/ext-locale-' + rpc.languageSettings.language + '.js');
                    this.startApplication();
                }
            },this));
        }, this));
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
      //TODO: initialize policy map
        rpc.policyNamesMap = {};
        
        var treeNodes = [ {
            text : i18n._('Summary'),
            category : 'Summary',
            leaf : true
        }, {
            text : i18n._('Host Viewer'),
            category : 'Host Viewer',
            leaf : true
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
                icon : "/reports/node-icons/untangle-node-shield.png"
            } ]
        }];
        if (rpc.currentApplications) {
            var i, app, apps = [];
            for (i = 0; i < rpc.currentApplications.length; i++) {
                app = rpc.currentApplications[i];
                if(app.name != 'untangle-node-branding' && app.name != 'untangle-node-support' ) {
                    apps.push({
                        text : app.displayName,
                        category : app.displayName,
                        leaf : true,
                        viewPosition : app.viewPosition,
                        icon : '/skins/'+rpc.skinSettings.skinName+'/images/apps/'+app.name+'_17x17.png'
                    });
                }
                
            }
            apps.sort(function(a, b) {
                return a.viewPosition - b.viewPosition;
            });
            treeNodes.push({
                text : i18n._("Applications"),
                leaf : false,
                expanded : true,
                children : apps
            });
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
            store : Ext.create('Ext.data.TreeStore', {
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
        var contentItems = rpc.isReportingEnabled? this.buildReportsViewer(): this.buildReportsNotEnabled();
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
                    layout: {type: 'vbox', align: 'right'},
                    items: [{
                        xtype: 'component',
                        html: '<a href="/auth/logout?url=/reports&realm=Reports">'+i18n._('Logout')+"</a>",
                        margin: '0 10 10 0'
                    }, {
                        xtype: 'component',
                        html: '<a href="/reports?old">'+i18n._('Go to the old reports page')+"</a>",
                        margin: '0 10 0 0'
                    }]
                }]
            }, {
                xtype:'panel',
                border: false,
                region:"center",
                layout:"border",
                items: contentItems
            }]
        });
        
        Ext.MessageBox.hide();
        if(rpc.isReportingEnabled) {
            this.viewport.down("treepanel").getSelectionModel().select(0);
        }
    }
});
