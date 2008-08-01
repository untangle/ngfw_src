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
    // init function
    init: function() {
        this.initSemaphore=9;
        rpc = {};
        // get JSONRpcClient
        rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");

        // get skin manager
        rpc.jsonrpc.RemoteUvmContext.skinManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.skinManager=result;
            // Load Current Skin
            rpc.skinManager.getSkinSettings(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
                var skinSettings=result;
                Ung.Util.loadCss("skins/"+skinSettings.administrationClientSkin+"/css/ext-skin.css");
                Ung.Util.loadCss("skins/"+skinSettings.administrationClientSkin+"/css/admin.css");
                this.postinit();// 1
            }.createDelegate(this));
        }.createDelegate(this));
        // get language manager
        rpc.jsonrpc.RemoteUvmContext.languageManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.languageManager=result;
            // get translations for main module
            rpc.languageManager.getTranslations(function (result, exception) {
                if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
                    i18n =new Ung.I18N({"map":result.map});
                    this.postinit();// 2
                }.createDelegate(this),"main");
        }.createDelegate(this));

        // get node manager
        rpc.jsonrpc.RemoteUvmContext.nodeManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.nodeManager=result;
            this.postinit();// 3
        }.createDelegate(this));
        // get policy manager
        rpc.jsonrpc.RemoteUvmContext.policyManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.policyManager=result;
            this.postinit();// 4
        }.createDelegate(this));
        // get toolbox manager
        rpc.jsonrpc.RemoteUvmContext.toolboxManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.toolboxManager=result;
            this.postinit();// 5
        }.createDelegate(this));
        // get admin manager
        rpc.jsonrpc.RemoteUvmContext.adminManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.adminManager=result;
            this.postinit();// 6
        }.createDelegate(this));
        // get version
        rpc.jsonrpc.RemoteUvmContext.version(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.version=result;
            this.postinit();// 7
        }.createDelegate(this));
        // get network manager
        rpc.jsonrpc.RemoteUvmContext.networkManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.networkManager=result;
            this.postinit();// 8
        }.createDelegate(this));
        rpc.jsonrpc.RemoteUvmContext.messageManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.messageManager=result;
            this.postinit();// 9
        }.createDelegate(this));

    },
    postinit: function() {
        this.initSemaphore--;
        if(this.initSemaphore!==0) {
            return;
        }

        this.initExtI18n();
        this.initExtGlobal();
        this.initExtVTypes();
        
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
                    '<div id="rack_list">',
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
            var newHeight=Math.max(this.getEl().getHeight()-180,100);
            main.leftTabs.setHeight(newHeight);
        });
        Ext.getCmp("west").fireEvent("resize");
        var buttonCmp=new Ext.Button({
            name: 'Help',
            renderTo: 'help',
            iconCls: 'iconHelp',
            text: i18n._('Help'),
            handler: function() {
                var rackBaseHelpLink = main.getHelpLink("rack");
                window.open(rackBaseHelpLink);
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
                rpc.adminManager.generateAuthNonce(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    var currentLocation = window.location;
                    var query = result;
                    query += "&host=" + currentLocation.hostname;
                    query += "&port=" + currentLocation.port;
                    query += "&protocol=" + currentLocation.protocol.replace(/:$/, "");
                    query += "&action=wizard";

                    var url = "../library/launcher?" + query;
                    var iframeWin = main.getIframeWin();
                    iframeWin.show();
                    iframeWin.setTitle("");
                    window.frames["iframeWin_iframe"].location.href = url;
                }.createDelegate(this));
                
            }
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
                rpc.adminManager.generateAuthNonce(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    var currentLocation = window.location;
                    var query = result;
                    query += "&host=" + currentLocation.hostname;
                    query += "&port=" + currentLocation.port;
                    query += "&protocol=" + currentLocation.protocol.replace(/:$/, "");
                    query += "&action=my_account";

                    var url = "../library/launcher?" + query;
                    var iframeWin = main.getIframeWin();
                    iframeWin.show();
                    iframeWin.setTitle("");
                    window.frames["iframeWin_iframe"].location.href = url;
                }.createDelegate(this));
            }
        });
        buttonCmp.hide();
        this.loadConfig();
        this.loadPolicies();
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

          portText: i18n.sprintf(i18n._("The port must be an integer number between %d and %d."), 1, 65535),

          portMatcher: function(val, field) {
            var minValue = 1;
            var maxValue = 65535;
            return (minValue <= val && val <= maxValue) || (val == 'any' || val == 'all' || val == 'n/a' || val == 'none');
          },

          portMatcherText: i18n.sprintf(i18n._("The port must be an integer number between %d and %d or one of the following values: any, all, n/a, none."), 1, 65535),

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

    getLicenseManager : function(forceReload) {
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

    unactivateNode: function(mackageDesc) {
        rpc.nodeManager.nodeInstances(function (result, exception) {
                if(exception) {
                    Ext.MessageBox.alert(i18n._("Failed"),exception.message);
                    return;
                }
                var tids=result;
                if(tids.length>0) {
                    Ext.MessageBox.alert(this.name+" "+i18n._("Warning"),
                    i18n.sprintf(i18n._("$s cannot be removed from the toolbox because it is being used by the following policy rack:<br><b>%s</b><br><br>You must remove the product from all policy racks first."), this.displayName,tids[0]. policy.name));
                    return;
                } else {
                    Ung.AppItem.updateStateForNode(this.name,"unactivating");
                    rpc.toolboxManager.uninstall(function (result, exception) {
                        if(exception) {
                            Ext.MessageBox.alert(i18n._("Failed"),exception.message);
                            return;
                        }
                        rpc.toolboxManager.unregister(function (result, exception) {
                            if(exception) {
                                Ext.MessageBox.alert(i18n._("Failed"),exception.message);
                                return;
                            }
                            main.loadApps();
                        }.createDelegate(this), this.name);
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
                tbar : [{xtype: 'tbtext', text: i18n._("Click to learn more")}],
                html:'<div id="appsItems"></div>',name:'Apps'},
                {title:i18n._('Config'),
                tbar : [{xtype: 'tbtext', text: i18n._("Click to learn more")}],   
                html:'<div id="configItems"></div>',
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
    // get help link
    getHelpLink: function(source,focus) {
        var baseLink="http://www.untangle.com/docs/get.php?";
        if(source) {
            source=source.toLowerCase().replace(" ","_");
        }
        var helpLink=baseLink+"version="+rpc.version+"&source="+source;
        if(focus) {
            focus=focus.toLowerCase().replace(" ","_");
            helpLink+="&focus="+focus;
        }
        return helpLink;
    },

    // load policies list
    loadPolicies: function() {
        rpc.policyManager.getPolicies( function (result, exception) {
            if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return; }
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
    createNode: function (Tid, md, statDesc) {
        var node={};
        node.tid=Tid.id;
        node.Tid=Tid;
        node.md=md;
        node.name=md.name;
        node.displayName=md.displayName;
        node.image='image?name='+node.name;
        //node.blingers=eval([{'type':'ActivityBlinger','bars':['ACTIVITY 1','ACTIVITY 2','ACTIVITY 3','ACTIVITY 4']},{'type':'SystemBlinger'}]);
        node.blingers=statDesc;
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
            this.apps.push(new Ung.AppItem(application));
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
            var node=this.createNode(instance.tid, instance.mackageDesc,rpc.rackView.statDescs.map[instance.tid.id]);
            this.nodes.push(node);
        }
        for(var i=0;i<this.nodes.length;i++) {
            var node=this.nodes[i];
            this.addNode(node);
        }
        this.updateSeparator();
        this.loadNodesRunStates();

    },
    // load the rack view for current policy
    loadRackView: function() {
        rpc.toolboxManager.getRackView(function (result, exception) {
            if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message);
                return;
            }
            rpc.rackView=result;
            main.buildApps();
            main.buildNodes();
        }.createDelegate(this), rpc.currentPolicy);
    },
    loadApps: function() {
        rpc.toolboxManager.getRackView(function (result, exception) {
            if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message);
                return;
            }
            rpc.rackView=result;
            main.buildApps();
        }.createDelegate(this), rpc.currentPolicy);
    },
    // load run states for all Nodes
    loadNodesRunStates: function() {
        rpc.nodeManager.allNodeStates(function (result, exception) {
            if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message);
                return;
            }
            var allNodeStates=result;
            for(var i=0;i<this.nodes.length;i++) {
                var nodeCmp=Ung.Node.getCmp(this.nodes[i].tid);
                if(nodeCmp) {
                    nodeCmp.updateRunState(allNodeStates.map[this.nodes[i].tid]);
                }
            }
            if(!main.disableThreads) {
                Ung.MessageManager.start();
            }
        }.createDelegate(this));
    },

    installNode: function(mackageDesc, targetPolicy) {
        if(mackageDesc!==null) {
            Ung.AppItem.updateStateForNode(mackageDesc.name, "installing")
            var policy=null;
            if(targetPolicy==null) {
                policy = rpc.currentPolicy;
            } else {
                policy = targetPolicy;
            }
            rpc.nodeManager.instantiate(function (result, exception) {
                if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
//This happens now on node instantiate
//              var instance = result;
//              rpc.toolboxManager.getRackView(function (result, exception) {
//                    if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message);
//                        return;
//                    }
//                    rpc.rackView=result;
//                    var instance=this;
//                    var node=main.createNode(instance.tid, instance.mackageDesc, rpc.rackView.statDescs.map[instance.tid.id]);
//                    main.nodes.push(node);
//                    main.addNode(node);
//                    main.updateSeparator();
//                    main.buildApps();
//                }.createDelegate(instance), rpc.currentPolicy);

            }.createDelegate(this), mackageDesc.name, policy);
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
                }

            });
            this.iframeWin.render();
        }
        return this.iframeWin;
    },
    // load Config
    loadConfig: function() {
        this.config =
            [{"name":"networking","displayName":i18n._("Networking"),"iconClass":"iconConfigNetwork"},
            {"name":"administration","displayName":i18n._("Administration"),"iconClass":"iconConfigAdmin"},
            {"name":"racks","displayName":i18n._("Racks"),"iconClass":"iconConfigAdmin"},
            {"name":"email","displayName":i18n._("Email"),"iconClass":"iconConfigEmail"},
            {"name":"userDirectory","displayName":i18n._("User Directory"),"iconClass":"iconConfigDirectory"},
            {"name":"upgrade","displayName":i18n._("Upgrade"),"iconClass":"iconConfigUpgrade"},
            {"name":"system","displayName":i18n._("System"),"iconClass":"iconConfigSetup"},
            {"name":"systemInfo","displayName":i18n._("System Info"),"iconClass":"iconConfigSupport"}];
        this.buildConfig();
    },
    // build config buttons
    buildConfig: function() {
        var out=[];
        for(var i=0;i<this.config.length;i++) {
            var item=this.config[i];
            var buttonCmp=new Ung.Button({
                id: "configItem_"+item.name,
                name: "configItem_"+item.name,
                configIndex: i,
                height: '42px',
                renderTo: 'configItems',
                cls:'toolboxButton',
                text: item.displayName,
                handler: function() {main.clickConfig(main.config[this.configIndex]);},
                iconCls: item.iconClass
            });
        }
    },
    // click Config Button
    clickConfig: function(configItem) {
        switch(configItem.name){
            case "networking":
                rpc.adminManager.generateAuthNonce(function (result, exception) {
                    if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
                    var alpacaUrl = "/alpaca/?" + result;
                    var iframeWin=main.getIframeWin();
                    iframeWin.show();
                    iframeWin.setTitle("Networking");
                    window.frames["iframeWin_iframe"].location.href=alpacaUrl;
                }.createDelegate(this));
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
            case "racks":
                Ung.Util.loadResourceAndExecute("Ung.Racks","script/config/racks.js", function() {
                    main.racksWin=new Ung.Racks(configItem);
                    main.racksWin.show();
                });
                break;
            case "userDirectory":
                Ung.Util.loadResourceAndExecute("Ung.UserDirectory","script/config/userDirectory.js", function() {
                    main.userDirectoryWin=new Ung.UserDirectory(configItem);
                    main.userDirectoryWin.show();
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
        	if(!nodeWidget.isRunning() && node.name!="untangle-node-openvpn") {
            	nodeWidget.onPowerClick();
        	}
        }
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
            out.push('<option value="'+rpc.policies[i].id+'" '+selVirtualRack+'>'+i18n._(rpc.policies[i].name)+'<\/option>');
        }
        out.push('<\/select>');
        Ext.get("rack_list").insertHtml("afterBegin", out.join(''));
        this.loadRackView();
    },
    // change current policy
    changePolicy: function () {
        var rack_select=document.getElementById('rack_select');
        if(rack_select.selectedIndex>=0) {
            rpc.currentPolicy=rpc.policies[rack_select.selectedIndex];
            this.loadRackView();
        }
    }
};
