// Global Variables
// the main object instance
var main=null;

// Main object class
Ung.Main=function() {
}
Ung.Main.prototype = {
    disableThreads: false, // in development environment is useful to disable
                            // threads.
    leftTabs: null,
    appsSemaphore: null,
    apps: null,
    config: null,
    nodes: null,
    // the Ext.Viewport object for the application
    viewport: null,
    initSemaphore: null,
    policySemaphore: null,
    contentLeftWidth: 220,
    // the application build version
    version: null,
    iframeWin: null,
    upgradeStatus:null,
    upgradeLastCheckTime: null,
    // init function
    init: function() {
    	//JSONRpcClient.toplevel_ex_handler=Ung.Util.handleException;
        this.initSemaphore=10;
        rpc = {};
        // get JSONRpcClient
        rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
        // get language manager
        rpc.jsonrpc.RemoteUvmContext.languageManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.languageManager=result;
            // get translations for main module
            rpc.languageManager.getTranslations(function (result, exception) {
                Ung.Util.handleException(exception);
                i18n=new Ung.I18N({"map":result.map});
                Ext.MessageBox.wait(i18n._("Initializing..."), i18n._("Please wait"));
                this.postinit();// 1
            }.createDelegate(this),"untangle-libuvm");
        }.createDelegate(this));
        // get skin manager
        rpc.jsonrpc.RemoteUvmContext.skinManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.skinManager=result;
            // Load Current Skin
            rpc.skinManager.getSkinSettings(function (result, exception) {
                Ung.Util.handleException(exception);
                var skinSettings=result;
                Ung.Util.loadCss("/skins/"+skinSettings.administrationClientSkin+"/css/ext-skin.css");
                Ung.Util.loadCss("/skins/"+skinSettings.administrationClientSkin+"/css/admin.css");
                this.postinit();// 2
            }.createDelegate(this));
        }.createDelegate(this));
        // get node manager
        rpc.jsonrpc.RemoteUvmContext.nodeManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.nodeManager=result;
            this.postinit();// 3
        }.createDelegate(this));
        // get policy manager
        rpc.jsonrpc.RemoteUvmContext.policyManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.policyManager=result;
            this.postinit();// 4
        }.createDelegate(this));
        // get toolbox manager
        rpc.jsonrpc.RemoteUvmContext.toolboxManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.toolboxManager=result;
            rpc.toolboxManager.getUpgradeSettings(function (result, exception) {
                Ung.Util.handleException(exception);
                rpc.upgradeSettings=result;
                this.postinit();// 5
            }.createDelegate(this));
            
        }.createDelegate(this));
        // get admin manager
        rpc.jsonrpc.RemoteUvmContext.adminManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.adminManager=result;
            this.postinit();// 6
        }.createDelegate(this));
        // get version
        rpc.jsonrpc.RemoteUvmContext.version(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.version=result;
            this.postinit();// 7
        }.createDelegate(this));
        // get network manager
        rpc.jsonrpc.RemoteUvmContext.networkManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.networkManager=result;
            this.postinit();// 8
        }.createDelegate(this));
        // get message manager & message key
        rpc.jsonrpc.RemoteUvmContext.messageManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.messageManager=result;
            rpc.messageManager.getMessageKey(function (result, exception) {
                Ung.Util.handleException(exception);
                rpc.messageKey=result;
                this.postinit();// 9
            }.createDelegate(this));
        }.createDelegate(this));
        // get branding manager
        rpc.jsonrpc.RemoteUvmContext.brandingManager(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.brandingManager=result;
            rpc.brandingManager.getBaseSettings(function (result, exception) {
                Ung.Util.handleException(exception);
                rpc.brandingBaseSettings=result;
                document.title=rpc.brandingBaseSettings.companyName;
                this.postinit();// 10
            }.createDelegate(this));
        }.createDelegate(this));

    },
    postinit: function() {
        this.initSemaphore--;
        if(this.initSemaphore!==0) {
            return;
        }
        if(!rpc.upgradeSettings.autoUpgrade) {
        	//do not upgrade automaticaly
        	this.startApplication()
        } else {
            //check for upgrades
        	Ext.MessageBox.wait(i18n._("Checking for upgrades..."), i18n._("Please wait"));
            rpc.toolboxManager.getUpgradeStatus(function(result, exception) {
            	Ung.Util.handleException(exception,function() {
            		this.startApplication.defer(1500,this);
            	}.createDelegate(this),"outsideAlert");
                var upgradeStatus=result;
                if(!upgradeStatus.upgrading && upgradeStatus.upgradesAvailable) {
                    this.upgrade();
                } else {
                    this.startApplication();
                }
            }.createDelegate(this),true);
        }
    },
    warnOnUpgrades : function(handler) {
    	if(main.upgradeStatus!=null && main.upgradeStatus.upgradesAvailable) {
            main.warnOnUpgradesCallback(main.upgradeStatus,handler);
    	} else {
            if(main.upgradeLastCheckTime!=null && (new Date()).getTime()-main.upgradeLastCheckTime<300000 && main.upgradeStatus!=null) {
                main.warnOnUpgradesCallback(main.upgradeStatus,handler);
            } else {
                Ext.MessageBox.wait(i18n._("Checking for upgrades..."), i18n._("Please wait"));
                rpc.toolboxManager.getUpgradeStatus(function(result, exception,opt,handler) {
                	main.upgradeLastCheckTime=(new Date()).getTime();
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message, function (handler) {
                        	main.upgradeStatus={};
                            main.warnOnUpgradesCallback(main.upgradeStatus,handler);
                        }.createDelegate(this,[handler]));
                        return;
                    }
                    
                    main.upgradeStatus=result;
                    main.warnOnUpgradesCallback(main.upgradeStatus,handler);
                }.createDelegate(this,[handler],true),true);
    		}
    	}
    },
    warnOnUpgradesCallback : function (upgradeStatus,handler) {
        if(upgradeStatus!=null) {
            if(upgradeStatus.upgrading) {
                Ext.MessageBox.alert(i18n._("Failed"), "Upgrade in progress.");
                return;
            } else if(upgradeStatus.upgradesAvailable){
                Ext.MessageBox.alert(i18n._("Failed"), "Upgrades are available, please click Upgrade button in Config panel.");
                return;
            }
		}
        handler.call(this);
    },
    startApplication: function() {
    	Ext.MessageBox.wait(i18n._("Starting..."), i18n._("Please wait"));

        this.initExtI18n();
        this.initExtGlobal();
        this.initExtVTypes();
        Ext.EventManager.onWindowResize(Ung.Util.resizeWindows);
        // initialize viewport object
        var contentLeftArr=[
            '<div id="contentleft">',
                '<div id="logo"><img src="/images/BrandingLogo.gif" border="0"/></div>',
                '<div id="leftHorizontalRuler"></div>',
                '<div id="leftTabs"></div>',
                '<div id="help"></div>',
            '</div>'];
        var contentRightArr=[
            '<div id="contentright">',
                '<div id="racks" style="display:none;">',
                    '<div id="rack_list"><div id="rack_select_container"></div>',
                    '</div>',
                    '<div id="rack_nodes">',
                        '<div id="security_nodes"></div>',
                        '<div id="nodes_separator" style="display:none;"><div id="nodes_separator_text"></div></div>',
                        '<div id="other_nodes"></div>',
                    '</div>',
                '</div>',
            '</div>'];

        this.viewport = new Ext.Viewport({
            layout:'border',
            items:[{
                    region:'west',
                    id: 'west',
                    html: contentLeftArr.join(""),
                    width: this.contentLeftWidth,
                    border: false
                 },{
                    region:'center',
                    id: 'center',
                    html: contentRightArr.join(""),
                    border: false,
                    cls: 'centerRegion',
                    bodyStyle: 'background-color: transparent;',
                    autoScroll: true
                }
             ]
        });
        Ext.QuickTips.init();

        this.buildLeftTabs();
        main.systemStats=new Ung.SystemStats({});

        Ext.getCmp("west").on("resize", function() {
            var newHeight=Math.max(this.getEl().getHeight()-220,100);
            main.leftTabs.setHeight(newHeight);
        });
        Ext.getCmp("west").fireEvent("resize");
        var buttonCmp=new Ext.Button({
            name: 'Help',
            renderTo: 'help',
            iconCls: 'iconHelp',
            text: i18n._('Help'),
            handler: function() {
            	var helpSource=main.leftTabs.getActiveTab().helpSource;
                main.openHelp(helpSource);
            }
        });
        buttonCmp=new Ext.Button({
            name: 'What Apps should I use?',
            id: 'help_empty_rack',
            renderTo: 'contentright',
            iconCls: 'iconHelp',
            text: i18n._('What Apps should I use?'),
            show : function() {
            	Ung.Button.prototype.show.call(this);
                this.getEl().alignTo("contentright","c-c");
            }, 
            handler: function() {
    	        main.warnOnUpgrades(function() {
                     main.openStore("wizard",i18n._('What Apps should I use?'));
                }.createDelegate(this));
            }.createDelegate(this)
        });
        buttonCmp.hide();
        buttonCmp=new Ext.Button({
            id: "my_account_button",
            name: "My Account",
            height: '42px',
            renderTo: 'appsItems',
            text: i18n._("My Account"),
            show : function() {
                Ung.Button.prototype.show.call(this);
                this.getEl().alignTo("appsItems","c-c",[0,10]);
            }, 
            handler: function() {
            	main.warnOnUpgrades(function() {
                     main.openStore("my_account",i18n._("My Account"));
                }.createDelegate(this));
            }
        });
        buttonCmp.hide();
        this.loadConfig();
        this.loadPolicies();
    },
    openStore : function (action,title) {
        var currentLocation = window.location;
        var query = "host=" + currentLocation.hostname;
        query += "&port=" + currentLocation.port;
        query += "&protocol=" + currentLocation.protocol.replace(/:$/, "");
        query += "&action="+action;

        var url = "../library/launcher?" + query;
        var iframeWin = main.getIframeWin();
        iframeWin.show();
        iframeWin.setTitle(title);
        window.frames["iframeWin_iframe"].location.href = url;
    },
    initExtI18n: function(){
        Ext.form.Field.prototype.invalidText=i18n._('The value in this field is invalid');
        Ext.form.TextField.prototype.blankText=i18n._('This field is required');
    },
    initExtGlobal: function(){
    	
    	// init quick tips
    	Ext.QuickTips.init();

    	//hide/unhide Field and label
        Ext.override(Ext.form.Field, {
            showContainer: function() {
                this.enable();
                this.show();
                this.getEl().up('.x-form-item').setDisplayed(true); // show entire container and children (including label if applicable)
            },
            
            hideContainer: function() {
                this.disable(); // for validation
                this.hide();
                this.getEl().up('.x-form-item').setDisplayed(false); // hide container and children (including label if applicable)
            },
            
            setContainerVisible: function(visible) {
                if (visible) {
                    this.showContainer();
                } else {
                    this.hideContainer();
                }
                return this;
            }
        });
    },
    // Add the additional 'advanced' VTypes
    initExtVTypes: function(){
        Ext.apply(Ext.form.VTypes, {
          ipAddress: function(val, field) {
            var ipAddrMaskRe = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
            return ipAddrMaskRe.test(val);
          },

          ipAddressText: i18n._('Invalid IP Address.'),

          ipAddressMatcher: function(val, field) {
            var ipAddrMaskRe = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
            return ipAddrMaskRe.test(val);
          },

          ipAddressMatcherText: i18n._('Invalid IP Address.'),

          port: function(val, field) {
            var minValue = 1;
            var maxValue = 65535;
            return minValue <= val && val <= maxValue;
          },

          portText: String.format(i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),

          portMatcher: function(val, field) {
            var minValue = 1;
            var maxValue = 65535;
            return (minValue <= val && val <= maxValue) || (val == 'any' || val == 'all' || val == 'n/a' || val == 'none');
          },

          portMatcherText: String.format(i18n._("The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none."), 1, 65535),

          password: function(val, field) {
            if (field.initialPassField) {
              var pwd = Ext.getCmp(field.initialPassField);
              return (val == pwd.getValue());
            }
            return true;
          },

          passwordText: i18n._('Passwords do not match')
        });
    },
    upgrade : function () {
        Ext.MessageBox.wait(i18n._("Downloading updates..."), i18n._("Please wait"));
        Ung.MessageManager.startUpgradeMode();
        rpc.toolboxManager.upgrade(function(result, exception) {
            Ung.Util.handleException(exception);
        }.createDelegate(this));
    },
    getLoggingManager : function(forceReload) {
        if (forceReload || rpc.loggingManager === undefined) {
            rpc.loggingManager = rpc.jsonrpc.RemoteUvmContext.loggingManager()
        }
        return rpc.loggingManager;
    },

    getAppServerManager : function(forceReload) {
        if (forceReload || rpc.appServerManager === undefined) {
            rpc.appServerManager = rpc.jsonrpc.RemoteUvmContext.appServerManager()
        }
        return rpc.appServerManager;
    },

    getBrandingManager : function(forceReload) {
        if (forceReload || rpc.brandingManager === undefined) {
            rpc.brandingManager = rpc.jsonrpc.RemoteUvmContext.brandingManager()
        }
        return rpc.brandingManager;
    },
    
    // get branding settings
    getBrandingBaseSettings : function(forceReload) {
        if (forceReload || rpc.brandingBaseSettings === undefined) {
            rpc.brandingBaseSettings = main.getBrandingManager().getBaseSettings();
        }
        return rpc.brandingBaseSettings;
    },        

    getLicenseManager : function(forceReload) {
    	// default functionality is to reload license manager as it might change in uvm
    	if (typeof forceReload === 'undefined') {
    		forceReload = true;
    	}
        if (forceReload || rpc.licenseManager === undefined) {
            rpc.licenseManager = rpc.jsonrpc.RemoteUvmContext.licenseManager()
        }
        return rpc.licenseManager;
    },

    getAppAddressBook : function(forceReload) {
        if (forceReload || rpc.appAddressBook === undefined) {
            rpc.appAddressBook = rpc.jsonrpc.RemoteUvmContext.appAddressBook()
        }
        return rpc.appAddressBook;
    },

    getMailSender : function(forceReload) {
        if (forceReload || rpc.mailSender === undefined) {
            rpc.mailSender = rpc.jsonrpc.RemoteUvmContext.mailSender()
        }
        return rpc.mailSender;
    },

    unactivateNode: function(mackageDesc) {
        rpc.nodeManager.nodeInstances(function (result, exception) {
                Ung.Util.handleException(exception);
                var tids=result;
                if(tids.length>0) {
                    Ext.MessageBox.alert(this.name+" "+i18n._("Warning"),
                    String.format(i18n._("{0} cannot be removed because it is being used by the following rack:{1}You must remove the product from all racks first."), this.displayName,"<br><b>"+tids[0].policy.name+"</b><br><br>"));
                    return;
                } else {
                    Ung.AppItem.updateStateForNode(this.name,"unactivating");
                    rpc.toolboxManager.uninstall(function (result, exception) {
                       Ung.Util.handleException(exception);
                        main.loadApps();
                        /*
                        rpc.toolboxManager.unregister(function (result, exception) {
                            Ung.Util.handleException(exception);
                            main.loadApps();
                        }.createDelegate(this), this.name);
                        */
                    }.createDelegate(this), this.name);
                }
        }.createDelegate(mackageDesc), mackageDesc.name);
    },
    // build left tabs
    buildLeftTabs: function () {
        this.leftTabs = new Ext.TabPanel({
            renderTo: 'leftTabs',
            activeTab: 0,
            height: 400,
            deferredRender:false,
            defaults:{autoScroll: true},
            items:[{
                title: i18n._('Apps'),
                helpSource: 'apps',
                tbar : [{xtype: 'tbtext', text: i18n._("Click to learn more")}],
                html:'<div id="appsItems"></div>',name:'Apps'},
                {title:i18n._('Config'),
                tbar : [{xtype: 'tbtext', text: i18n._("Click to learn more")}],   
                html:'<div id="configItems"></div>',
                helpSource: 'config',
                name:'Config'}
            ],
            listeners : {
                "render" : {
                    fn : function() {
                        this.addNamesToPanels();
                    }
                }
            }

        });
    },
    // open context sensitive help 
    openHelp: function(source) {
		var url = "../library/launcher?";
		url += "action=help";
        if(source) {
            url += "&source=" + source;
        }
        window.open(url);
    },

    // load policies list
    loadPolicies: function() {
    	Ext.MessageBox.wait(i18n._("Loading Rack..."), i18n._("Please wait"));
        rpc.policyManager.getPolicies( function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.policies=result;
            this.buildPolicies();
        }.createDelegate(this));
    },
    getNodeMackageDesc: function(Tid) {
        var i;
        if(this.myApps!==null) {
            for(i=0;i<this.myApps.length;i++) {
                if(this.myApps[i].name==Tid.nodeName) {
                    return this.myApps[i];
                }
            }
        }
        return null;
    },
    createNode: function (Tid, md, statDesc,licenseStatus,runState) {
        var node={};
        node.tid=Tid.id;
        node.Tid=Tid;
        node.md=md;
        node.name=md.name;
        node.displayName=md.displayName;
        node.licenseStatus=licenseStatus;
        node.image='image?name='+node.name;
        node.blingers=statDesc;
        node.runState=runState;
        return node;
    },
    buildApps: function () {
        //destroy Apps
        if(main.apps!=null) {
            for(var i=0; i<main.apps.length; i++) {
                Ext.destroy(main.apps[i]);
            }
            this.apps=null;
        }
        //build Apps
        this.apps=[];
        for(var i=0;i<rpc.rackView.applications.list.length;i++) {
            var application=rpc.rackView.applications.list[i];
            var appCmp=new Ung.AppItem(application);
            if(appCmp.isValid) {
                this.apps.push(appCmp);
            }
        }
        if(this.apps.length>0) {
        	Ext.getCmp("my_account_button").hide();
        } else {
        	Ext.getCmp("my_account_button").show();
        }
    },
    buildNodes: function() {
        //build nodes
        Ung.MessageManager.stop();
        this.destoyNodes();
        this.nodes=[];
        for(var i=0;i<rpc.rackView.instances.list.length;i++) {
            var instance=rpc.rackView.instances.list[i];
            var node=this.createNode(instance.tid, instance.mackageDesc,
                rpc.rackView.statDescs.map[instance.tid.id],
                rpc.rackView.licenseStatus.map[instance.mackageDesc.name],
                rpc.rackView.runStates.map[instance.tid.id]);
            this.nodes.push(node);
        }
        for(var i=0;i<this.nodes.length;i++) {
            var node=this.nodes[i];
            this.addNode(node);
        }
        this.updateSeparator();
        if(!main.disableThreads) {
            Ung.MessageManager.start(true);
        }
        if(Ext.MessageBox.isVisible() && Ext.MessageBox.getDialog().title==i18n._("Please wait")) {
            Ext.MessageBox.hide();
        }

    },

    // load the rack view for current policy
    loadRackView: function() {
    	Ext.MessageBox.wait(i18n._("Loading Rack..."), i18n._("Please wait"));
        rpc.toolboxManager.getRackView(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.rackView=result;
            main.buildApps();
            main.buildNodes();
        }.createDelegate(this), rpc.currentPolicy);
    },
    loadApps: function() {
        rpc.toolboxManager.getRackView(function (result, exception) {
            Ung.Util.handleException(exception);
            rpc.rackView=result;
            main.buildApps();
        }.createDelegate(this), rpc.currentPolicy);
    },

    installNode: function(mackageDesc, appItem) {
        if(mackageDesc!==null) {
        	if(main.getNode(mackageDesc.name)!=null) {
        		appItem.hide();
        		return;
        	}
            Ung.AppItem.updateStateForNode(mackageDesc.name, "installing");
            rpc.nodeManager.instantiate(function (result, exception) {
                Ung.Util.handleException(exception);
            }.createDelegate(this), mackageDesc.name, rpc.currentPolicy);
        }
    },
    getIframeWin: function() {
        if(this.iframeWin==null) {
            this.iframeWin=new Ung.Window({
                id: 'iframeWin',
                title:'',
                layout: 'fit',
                items: {
                    html: '<iframe id="iframeWin_iframe" name="iframeWin_iframe" width="100%" height="100%" />'
                },
                closeAction:'closeActionFn',
                closeActionFn: function() {
                    this.hide();
                    window.frames["iframeWin_iframe"].location.href="about:blank";
                    if (this.breadcrumbs){
                        Ext.destroy(this.breadcrumbs);
                    }
                }

            });
            this.iframeWin.render();
        }
        return this.iframeWin;
    },
    openInRightFrame : function(title, url) {
        var iframeWin=main.getIframeWin();
        iframeWin.show();
        if (typeof title == 'string') {
            iframeWin.setTitle(title);
        } else { // the title represents breadcrumbs
            iframeWin.setTitle('<span id="title_' + iframeWin.getId() + '"></span>');
            iframeWin.breadcrumbs = new Ung.Breadcrumbs({
				renderTo : 'title_' + iframeWin.getId(),
				elements : title
			})            
        }
        window.frames["iframeWin_iframe"].location.href=url;
    },
    // load Config
    loadConfig: function() {
        this.config =
            [{"name":"networking","displayName":i18n._("Networking"),"iconClass":"iconConfigNetwork","helpSource":"networking_config"},
            {"name":"administration","displayName":i18n._("Administration"),"iconClass":"iconConfigAdmin","helpSource":"administration_config"},
            {"name":"email","displayName":i18n._("Email"),"iconClass":"iconConfigEmail","helpSource":"email_config"},
            {"name":"localDirectory","displayName":i18n._("Local Directory"),"iconClass":"iconConfigDirectory","helpSource":"local_directory_config"},
            {"name":"upgrade","displayName":i18n._("Upgrade"),"iconClass":"iconConfigUpgrade","helpSource":"upgrade_config"},
            {"name":"system","displayName":i18n._("System"),"iconClass":"iconConfigSetup","helpSource":"system_config"},
            {"name":"systemInfo","displayName":i18n._("System Info"),"iconClass":"iconConfigSupport","helpSource":"system_info_config"}];
        this.buildConfig();
    },
    // build config buttons
    buildConfig: function() {
        var out=[];
        for(var i=0;i<this.config.length;i++) {
            var item=this.config[i];
            var appItemCmp=new Ung.ConfigItem({
            	item:item
            });
        }
    },
    // click Config Button
    clickConfig: function(configItem) {
        switch(configItem.name){
            case "networking":
                var alpacaUrl = "/alpaca/";
                var breadcrumbs = [{
                    title : i18n._("Configuration"),
                    action : function() {
                        main.iframeWin.closeActionFn();
                    }.createDelegate(this)
                }, {
                    title : i18n._('Networking')
                }];
                
                main.openInRightFrame(breadcrumbs, alpacaUrl);
                break;
            case "administration":
                Ung.Util.loadResourceAndExecute("Ung.Administration","script/config/administration.js", function() {
                    main.administrationWin=new Ung.Administration(configItem);
                    main.administrationWin.show();
                });
                break;
            case "email":
                Ung.Util.loadResourceAndExecute("Ung.Email","script/config/email.js", function() {
                    main.emailWin=new Ung.Email(configItem);
                    main.emailWin.show();
                });
                break;
            case "system":
                Ung.Util.loadResourceAndExecute("Ung.System","script/config/system.js", function() {
                    main.systemWin=new Ung.System(configItem);
                    main.systemWin.show();
                });
                break;
            case "systemInfo":
                Ung.Util.loadResourceAndExecute("Ung.SystemInfo","script/config/systemInfo.js", function() {
                    main.systemInfoWin=new Ung.SystemInfo(configItem);
                    main.systemInfoWin.show();
                });
                break;
            case "upgrade":
                Ung.Util.loadResourceAndExecute("Ung.Upgrade","script/config/upgrade.js", function() {
                    main.upgradeWin=new Ung.Upgrade(configItem);
                    main.upgradeWin.show();
                });
                break;
            case "localDirectory":
                Ung.Util.loadResourceAndExecute("Ung.LocalDirectory","script/config/localDirectory.js", function() {
                    main.localDirectoryWin=new Ung.LocalDirectory(configItem);
                    main.localDirectoryWin.show();
                });
                break;
            default:
                Ext.MessageBox.alert(i18n._("Failed"),"TODO: implement config "+configItem.name);
                break;
        }
    },

    destoyNodes: function () {
        if(this.nodes!==null) {
            for(var i=0;i<this.nodes.length;i++) {
                var node=this.nodes[i];
                var cmp=Ung.Node.getCmp(this.nodes[i].tid);
                if(cmp) {
                    cmp.destroy();
                    cmp=null;
                }
            }
        }
    },

    getNodePosition: function(place, viewPosition) {
        var placeEl=document.getElementById(place);
        var position=0;
        if(placeEl.hasChildNodes()) {
            for(var i=0;i<placeEl.childNodes.length;i++) {
                if(placeEl.childNodes[i].getAttribute('viewPosition')-viewPosition<0) {
                    position=i+1;
                } else {
                    break;
                }
            }
        }
        return position;
    },
    addNode: function (node, startNode) {
        var nodeWidget=new Ung.Node(node);
        var place=(node.md.type=="NODE")?'security_nodes':'other_nodes';
        var position=this.getNodePosition(place,node.md.viewPosition);
        nodeWidget.render(place,position);
        Ung.AppItem.updateStateForNode(node.name, "installed");
        if(startNode) {
        	main.startNode(nodeWidget)
        }
    },
    startNode : function(nodeWidget) {
        if(nodeWidget.name!="untangle-node-openvpn") {
            nodeWidget.start();
        } else {
        	Ext.MessageBox.alert(i18n._("OpenVPN warning"), i18n._("OpenVPN can not be automatically turned on.<br>Please configure its settings first."));
        }
    },
    getNode : function(nodeName) {
    	if(main.nodes) {
            for (var i = 0; i < main.nodes.length; i++) {
                if (nodeName == main.nodes[i].name) {
                    return main.nodes[i];
                    break;
                }
            }
    	}
        return null;
    },
    isNodeRunning : function(nodeName) {
    	var node = main.getNode(nodeName);
    	if (node != null) {
    		 var nodeCmp = Ung.Node.getCmp(node.tid);
    		 if (nodeCmp != null && nodeCmp.isRunning()){
    		 	return true;
    		 }
    	}
    	return false;
    },
    // Show - hide Services header in the rack
    updateSeparator: function() {
    	if(this.nodes.length==0) {
    	    document.getElementById("racks").style.display="none";
    	    Ext.getCmp("help_empty_rack").show();
    	} else {
    		Ext.getCmp("help_empty_rack").hide();
    		document.getElementById("racks").style.display="";
            var hasUtil=false;
            var hasService=false;
            for(var i=0;i<this.nodes.length;i++) {
                if(this.nodes[i].md.type!="NODE") {
            	   hasService=true;
            	   if(this.nodes[i].md.type!="SERVICE") {
            	       hasUtil=true
            	   }
            	}
            }
            document.getElementById("nodes_separator_text").innerHTML=(hasService && hasUtil)?i18n._("Services & Utilities"):hasService?i18n._("Services"):"";
            document.getElementById("nodes_separator").style.display= hasService?"":"none";
            if(hasService) {
                document.getElementById("racks").style.backgroundPosition="0px 100px";
            } else {
                document.getElementById("racks").style.backgroundPosition="0px 50px";
            }
    	}
    },
    // build policies select box
    buildPolicies: function () {
        var out=[];
        out.push('<select id="rack_select" onchange="main.changePolicy()">');
        for(var i=0;i<rpc.policies.length;i++) {
            var selVirtualRack=rpc.policies[i]["default"]===true?"selected":"";

            if(rpc.policies[i]["default"]===true) {
                rpc.currentPolicy=rpc.policies[i];
            }
            out.push('<option value="'+rpc.policies[i].id+'" '+selVirtualRack+'>'+i18n._(rpc.policies[i].name)+'</option>');
        }
        out.push('<option value="SHOW_POLICY_MANAGER" class="ungButton">Policy Manager</option>');
        out.push('</select>');
        Ext.get("rack_select_container").dom.innerHTML=out.join('');
        this.loadRackView();
    },
    // change current policy
    changePolicy: function () {
        var rack_select=document.getElementById('rack_select');
        if(rack_select.selectedIndex>=0) {
        	if(rack_select.value == "SHOW_POLICY_MANAGER") {
        		//select previous value
				for (index = 0; index < rack_select.options.length; index++) {
					if (rack_select.options[index].value == rpc.currentPolicy.id) {
						rack_select.options[index].selected = true;
						break;
					}
				}
                Ung.Util.loadResourceAndExecute("Ung.PolicyManager","script/config/policyManager.js", function() {
                    main.policyManagerWin=new Ung.PolicyManager({"name":"policyManager", "helpSource":"policy_manager"});
                    main.policyManagerWin.show();
                });
	       	} else {
	            rpc.currentPolicy=rpc.policies[rack_select.selectedIndex];
	            this.loadRackView();
        	}
        }
    }
};
