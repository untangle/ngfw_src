Ext.define("Ung.form.DayOfWeekMatcherField", {
    extend: "Ext.form.CheckboxGroup",
    alias: "widget.udayfield",
    columns: 7,
    width: 700,
    i18n: null,
    isDayEnabled: function (dayOfWeekMatcher, dayInt) {
        if (dayOfWeekMatcher.indexOf("any") != -1)
            return true;
        if (dayOfWeekMatcher.indexOf(dayInt.toString()) != -1)
            return true;
        switch (dayInt) {
          case 1:
            if (dayOfWeekMatcher.indexOf("sunday") != -1)
                return true;
            break;
          case 2:
            if (dayOfWeekMatcher.indexOf("monday") != -1)
                return true;
            break;
          case 3:
            if (dayOfWeekMatcher.indexOf("tuesday") != -1)
                return true;
            break;
          case 4:
            if (dayOfWeekMatcher.indexOf("wednesday") != -1)
                return true;
            break;
          case 5:
            if (dayOfWeekMatcher.indexOf("thursday") != -1)
                return true;
            break;
          case 6:
            if (dayOfWeekMatcher.indexOf("friday") != -1)
                return true;
            break;
          case 7:
            if (dayOfWeekMatcher.indexOf("saturday") != -1)
                return true;
            break;
        }
        return false;
    },
    items: [{
        xtype: 'checkbox',
        name: 'sunday',
        dayId: '1',
        boxLabel: 'Sunday',
        hideLabel: true
    },{
        xtype: 'checkbox',
        name: 'monday',
        dayId: '2',
        boxLabel: 'Monday',
        hideLabel: true
    },{
        xtype: 'checkbox',
        name: 'tuesday',
        dayId: '3',
        boxLabel: 'Tuesday',
        hideLabel: true
    },{
        xtype: 'checkbox',
        name: 'wednesday',
        dayId: '4',
        boxLabel: 'Wednesday',
        hideLabel: true
    },{
        xtype: 'checkbox',
        name: 'thursday',
        dayId: '5',
        boxLabel: 'Thursday',
        hideLabel: true
    },{
        xtype: 'checkbox',
        name: 'friday',
        dayId: '6',
        checked: true,
        boxLabel: 'Friday',
        hideLabel: true
    },{
        xtype: 'checkbox',
        name: 'saturday',
        dayId: '7',
        boxLabel: 'Saturday',
        hideLabel: true
    }],
    arrayContains: function(array, value) {
        for (var i = 0 ; i < array.length ; i++) {
            if (array[i] === value)
                return true;
        }
        return false;
    },
    initComponent: function() {
        var i;
        var initValue = "none";
        if ((typeof this.value) == "string") {
            initValue = this.value.split(",");
        }
        this.value = null;
        for (i = 0 ; i < this.items.length ; i++)
            this.items[i].checked = false;
        for (i = 0 ; i < this.items.length ; i++) {
            var item = this.items[i];
            if ( this.arrayContains(initValue, item.dayId) || this.arrayContains(initValue, item.name) || this.arrayContains(initValue, "any")) {
                item.checked = true;
            }
            item.boxLabel = i18n._(item.boxLabel);
        }
        this.callParent(arguments);
    },
    getValue: function() {
        var checkCount = 0;
        var i;
        for (i = 0 ; i < this.items.length ; i++)
            if (this.items.items[i].checked)
                checkCount++;
        if (checkCount == 7)
            return "any";
        var arr = [];
        for (i = 0 ; i < this.items.length ; i++)
            if (this.items.items[i].checked)
                arr.push(this.items.items[i].dayId);
        if (arr.length === 0){
            return "none";
        } else {
            return arr.join();
        }
    }
});

Ext.define("Ung.AppItem", {
    extend: "Ext.Component",
    nodeProperties: null,
    cls: 'app-item',
    statics: {
        //Global map to keep loading flag of the apps
        loadingFlags: {},
        template: new Ext.Template('<div class="app-icon"><img src="/skins/{skin}/images/admin/apps/{name}_80x80.png"/><div class="app-name">{text}</div></div>',
                                   '<div class="app-install"><i class="material-icons">get_app</i></div>',
                                   '<div class="app-loader"><div class="loader"><svg class="circular" viewBox="25 25 50 50"><circle class="path" cx="50" cy="50" r="8" fill="none" stroke-width="3" stroke-miterlimit="10"/></svg></div></div>',
                                   '<div class="app-done"><i class="material-icons">check</i> ' + i18n._('DONE') + '</div>'),
        setLoading: function(name, loadingFlag) {
            Ung.AppItem.loadingFlags[name] = loadingFlag;
            var app = Ung.AppItem.getApp(name);
            if (app != null) {
                app.syncProgress();
            }
        },
        // get the app item having a item name
        getApp: function(name) {
            return Ext.getCmp("app-item_" + name);
        }
    },
    initComponent: function() {
        this.id = "app-item_" + this.nodeProperties.name;
        this.callParent(arguments);
    },
    afterRender: function() {
        this.callParent(arguments);
        this.getEl().set({
            'name': this.nodeProperties.name
        });
        var html = Ung.AppItem.template.applyTemplate({
            skin: rpc.skinSettings.skinName,
            id: this.getId(),
            name: this.nodeProperties.name,
            text: this.nodeProperties.displayName
        });
        this.getEl().insertHtml("afterBegin", html);
        this.getEl().on("click", function(e) {
            e.preventDefault();
            this.installNode();
        }, this);
        this.syncProgress();
    },
    //Sync progress bar status
    syncProgress: function() {
        if(Ung.AppItem.loadingFlags[this.nodeProperties.name]) {
            if(this.rendered) {
                this.addCls('progress');
            }
        }
    },
    // before Destroy
    beforeDestroy: function() {
        this.callParent(arguments);
    },
    // install node
    installNode: function( completeFn ) {
        if(Ung.AppItem.loadingFlags[this.nodeProperties.name]) {
            return;
        }
        Ung.Main.installNode(this.nodeProperties, this, completeFn);
    }
});

Ext.define("Ung.ConfigItem", {
    extend: "Ext.Component",
    cls: 'app-item',
    statics: {
        template: new Ext.Template('<div class="app-icon app-config-icon {iconCls}"></div><div class="app-name">{text}</div>')
    },
    afterRender: function() {
        this.callParent(arguments);
        var html = Ung.ConfigItem.template.applyTemplate({
            iconCls: this.item.iconClass,
            text: this.item.displayName
        });
        this.getEl().insertHtml("afterBegin", html);
        this.getEl().on("click", function(e) {
            if (e!=null) {
                e.stopEvent();
            }
            this.handler();
        }, this);
    },
    handler: function() {
        Ung.Main.openConfig(this.item);
    }
});

// Node Class
Ext.define("Ung.Node", {
    extend: "Ext.Component",
    statics: {
        // Get node component by nodeId
        getCmp: function(nodeId) {
            return Ext.getCmp("node_" + nodeId);
        },
        template: new Ext.Template(
            '<div class="node-cap" style="display:{isNodeEditable}"></div><div class="node-image"><img src="{image}"/></div>', '<div class="node-label">{displayName}</div>',
            '<div class="node-faceplate-info">{licenseMessage}</div>',
            '<div class="node-metrics" id="node-metrics_{id}"></div>',
            '<div class="node-state" id="node-state_{id}" name="State"></div>',
            '<div class="{nodePowerCls}" id="node-power_{id}" name="Power"></div>',
            '<div class="node-buttons" id="node-buttons_{id}"></div>')
    },
    cls: "node",
    // ---Node specific attributes------
    // node name
    name: null,
    // node image
    image: null,
    // mackage description
    md: null,
    // --------------------------------
    hasPowerButton: null,
    // node state
    state: null, // on, off, attention
    // is powered on,
    powerOn: null,
    // running state
    runState: null, // RUNNING, INITIALIZED
    // settings Component
    settings: null,
    // settings Window
    settingsWin: null,
    // settings Class name
    settingsClassName: null,
    // list of available metrics for this node/app
    metrics: null,
    // which metrics are shown on the faceplate
    activeMetrics: [0,1,2,3],
    faceplateMetrics: null,
    buttonsPanel: null,
    subCmps: null,
    //can the node be edited on the gui
    isNodeEditable: true,
    constructor: function(config) {
        this.id = "node_" + config.nodeId;
        config.helpSource=config.displayName.toLowerCase().replace(/ /g,"_");
        if(config.runState==null) {
            config.runState="INITIALIZED";
        }
        this.subCmps = [];
        if( config.nodeSettings.policyId != null ) {
            this.isNodeEditable = (config.nodeSettings.policyId == rpc.currentPolicy.policyId);
        }
        this.callParent(arguments);
    },
    // before Destroy
    beforeDestroy: function() {
        if(this.getEl()) {
            this.getEl().stopAnimation();
            this.getEl().clearListeners();
        }
        if(this.settingsWin && this.settingsWin.isVisible()) {
            this.settingsWin.closeWindow();
        }
        Ext.destroy(this.subCmps);
        if(this.hasPowerButton) {
            var powerButton = Ext.get('node-power_' + this.getId());
            if(powerButton) {
                powerButton.clearListeners();
            }
        }
        this.callParent(arguments);
    },
    afterRender: function() {
        this.callParent(arguments);
        Ung.NodePreview.removeNode(this.name);
        this.getEl().set({
            'name': this.name
        });
        if(this.fadeIn) {
            var el=this.getEl();
            el.scrollIntoView(Ung.Main.appsPanel.getEl());
            el.setOpacity(0.5);
            el.fadeIn({opacity: 1, duration: 2500, callback: function() {
                el.setOpacity(1);
                el.frame("#63BE4A", 1, { duration: 1000 });
            }});
        }
        if(testMode && this.license) {
            this.license.trial = Math.random()>0.5;
            this.license.valid = Math.random()>0.5;
        }
        var templateHTML = Ung.Node.template.applyTemplate({
            'id': this.getId(),
            'image': this.image,
            'isNodeEditable': this.isNodeEditable ? "none": "",
            'displayName': this.displayName,
            'nodePowerCls': this.hasPowerButton?((this.license && !this.license.valid)?"node-power node-power-expired":"node-power"):"",
            'licenseMessage': this.getLicenseMessage()
        });
        this.getEl().insertHtml("afterBegin", templateHTML);
        
        if(rpc.skinInfo.appsViewType == "list") {
            if(this.isNodeEditable) {
                this.getEl().on('click', this.loadSettings, this);
            }
        } else {
            this.buttonsPanel = Ext.create('Ext.container.Container',{
                renderTo: 'node-buttons_' + this.getId(),
                width: 290,
                layout: 'hbox',
                style: {marginTop: '5px'},
                defaults: {
                    style: {marginLeft: '6px'}
                },
                items: [{
                    xtype: "button",
                    name: "Show Settings",
                    iconCls: 'node-settings-icon',
                    text: i18n._('Settings'),
                    handler: Ext.bind(function() {
                        this.loadSettings();
                    }, this)
                },{
                    xtype: "button",
                    name: "Help",
                    iconCls: 'icon-help',
                    minWidth: 25,
                    tooltip: i18n._('Help'),
                    handler: Ext.bind(function() {
                        this.onHelpAction();
                    }, this)
                },{
                    xtype: "button",
                    name: "Buy",
                    id: 'node-buy-button_'+this.getId(),
                    iconCls: 'icon-buy',
                    hidden: !(this.license != null && this.license.trial), //show only if trial license
                    cls: 'buy-button',
                    text: i18n._('Buy Now'),
                    handler: Ext.bind(this.onBuyNowAction, this)
                }]
            });
            this.subCmps.push(this.buttonsPanel);
            if(this.hasPowerButton) {
                Ext.get('node-power_' + this.getId()).on('click', this.onPowerClick, this);
                this.subCmps.push(Ext.create('Ext.tip.ToolTip', {
                    html: [
                       '<div style="text-align: left;">',
                       i18n._("The <B>Status Indicator</B> shows the current operating condition of a particular application."),
                       '<br/><font color="#00FF00"><b>' + i18n._("Green") + '</b></font> ' +
                           i18n._('indicates that the application is ON and operating normally.'),
                       '<br/><font color="#FF0000"><b>' + i18n._("Red") + '</b></font> ' +
                           i18n._('indicates that the application is ON, but that an abnormal condition has occurred.'),
                       '<br/><font color="#FFFF00"><b>' + i18n._("Yellow") + '</b></font> ' +
                           i18n._('indicates that the application is saving or refreshing settings.'),
                       '<br/><b>' + i18n._("Clear") + '</b> ' + i18n._('indicates that the application is OFF, and may be turned ON by the user.'),
                       '</div>'].join(''),
                    target: 'node-state_' + this.getId(),
                    showDelay: 20,
                    dismissDelay: 0,
                    hideDelay: 0
                }));
                this.subCmps.push(Ext.create('Ext.tip.ToolTip', {
                    html: i18n._('The <B>Power Button</B> allows you to turn a application ON and OFF.'),
                    target: 'node-power_' + this.getId(),
                    showDelay: 20,
                    dismissDelay: 0,
                    hideDelay: 0
                }));
            }
            if(!this.isNodeEditable) {
                this.subCmps.push(Ext.create('Ext.tip.ToolTip', {
                    html: i18n._('This app belongs to the parent rack shown above.<br/> To access the settings for this app, select the parent rack.'),
                    target: 'node_' + this.nodeId,
                    showDelay: 20,
                    dismissDelay: 0,
                    hideDelay: 0
                }));
            }
            this.initMetrics();
        }
        this.nodeStateContainer = document.getElementById('node-state_' + this.getId());
        if(!this.isNodeEditable) {
            this.nodeStateContainer.style.cursor = "default";
        }
        this.updateRunState(this.runState, true);
    },
    // is runState "RUNNING"
    isRunning: function() {
        return (this.runState == "RUNNING");
    },
    setState: function(state) {
        this.state = state;
        if(this.hasPowerButton && this.nodeStateContainer) {
            this.nodeStateContainer.className = "node-state icon-state-" + this.state;
        }
    },
    setPowerOn: function(powerOn) {
        this.powerOn = powerOn;
    },
    updateRunState: function(runState, force) {
        if(runState!=this.runState || force || this.state=="attention") {
            this.runState = runState;
            switch ( runState ) {
              case "RUNNING":
                this.setPowerOn(true);
                this.setState("on");
                break;
              case "INITIALIZED":
              case "LOADED":
                this.setPowerOn(false);
                this.setState("off");
                break;
            default:
                alert("Unknown runState: " + runState);
            }
        }
        if(!force) {
            var panelStatus = this.getSettingsAppPanel();
            if(panelStatus) {
                panelStatus.updatePower(this.isRunning());
            }
            if(this.name=="reports") {
                rpc.reportsEnabled = this.isRunning();
                Ung.Main.updateReportsDependencies();
            }
        }
    },
    getSettingsAppPanel: function() {
        if(this.settingsWin && this.settingsWin.isVisible() && this.settingsWin.panelStatus) {
            return this.settingsWin.panelStatus;
        }
        return null;
    },
    updateMetrics: function() {
        if (this.powerOn && this.metrics) {
            if(this.faceplateMetrics!=null) {
                this.faceplateMetrics.update(this.metrics);
            }
            var panelStatus = this.getSettingsAppPanel();
            if(panelStatus) {
                panelStatus.updateMetrics(this.metrics);
            }
        } else {
            this.resetMetrics();
        }
    },
    resetMetrics: function() {
        if(this.faceplateMetrics!=null) {
            this.faceplateMetrics.reset();
        }
    },
    onPowerClick: function() {
        if (!this.powerOn) {
            this.start();
        } else {
            this.stop();
        }
    },
    start: function (handler) {
        if(this.state=="attention") {
            return;
        }
        this.loadNode(Ext.bind(function() {
            this.setPowerOn(true);
            this.setState("attention");
            this.rpcNode.start(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception, Ext.bind(function(message, details) {
                    var title = Ext.String.format( i18n._( "Unable to start {0}" ), this.displayName );
                    Ung.Util.showWarningMessage(title, details);
                    this.updateRunState(this.runState);
                }, this),"noAlert")) return;
                this.rpcNode.getRunState(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.updateRunState(result);
                    if(Ext.isFunction(handler)) {
                        handler();
                    }
                }, this));
            }, this));
        }, this));
    },
    stop: function () {
        if(this.state=="attention") {
            return;
        }
        this.loadNode(Ext.bind(function() {
            this.setPowerOn(false);
            this.setState("attention");
            this.rpcNode.stop(Ext.bind(function(result, exception) {
                this.resetMetrics();
                if(Ung.Util.handleException(exception)) return;
                this.rpcNode.getRunState(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.updateRunState(result);
                }, this));
            }, this));
        }, this));
    },
    // on click help
    onHelpAction: function() {
        Ung.Main.openHelp(this.helpSource);
    },
    //on Buy Now Action
    onBuyNowAction: function() {
        Ung.Main.openLibItemStore( this.name.replace("-node-","-libitem-"));
    },
    getRpcNode: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        if (this.rpcNode === undefined) {
            rpc.nodeManager.node(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.rpcNode = result;
                handler.call(this);
            }, this), this.nodeId);
        } else {
            handler.call(this);
        }
    },
    getNodeProperties: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        if(this.rpcNode == null) {
            return;
        }
        if (this.nodeProperties === undefined) {
            this.rpcNode.getNodeProperties(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.nodeProperties = result;
                handler.call(this);
            }, this));
        } else {
            handler.call(this);
        }
    },
    // load Node
    loadNode: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        Ext.bind(this.getRpcNode, this, [Ext.bind(this.getNodeProperties, this,[handler])]).call(this);
    },
    loadSettings: function() {
        Ext.MessageBox.wait(i18n._("Loading Settings..."), i18n._("Please wait"));
        this.settingsClassName = 'Webui.'+this.name+'.settings';
        Ext.require([this.settingsClassName], function() {
            this.initSettings();
        }, this);
    },
    // init settings
    initSettings: function() {
        Ext.bind(this.loadNode, this,[Ext.bind(this.preloadSettings, this)]).call(this);
    },
    //get node settings async before node settings load
    preloadSettings: function(handler) {
        var nodeClass = Ext.ClassManager.get(this.settingsClassName);
        if( nodeClass != null && Ext.isFunction( nodeClass.preloadSettings ) ) {
            nodeClass.preloadSettings(this);
        } else if(Ext.isFunction(this.rpcNode.getSettings)) {
            this.rpcNode.getSettings(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.openSettings.call(this, result);
            }, this));
        } else {
            this.openSettings.call(this, null);
        }
    },
    // open settings window
    openSettings: function(settings) {
        var items=null;
        if (this.settingsClassName !== null) {
            this.settingsWin=Ext.create(this.settingsClassName, {
                name: this.name,
                nodeId: this.nodeId,
                nodeProperties: this.nodeProperties,
                displayName: this.displayName,
                helpSource: this.helpSource,
                rpcNode: this.rpcNode,
                settings: settings
            });
        } else {
            this.settingsWin = Ext.create('Ung.NodeWin',{
                name: this.name,
                nodeId: this.nodeId,
                nodeProperties: this.nodeProperties,
                displayName: this.displayName,
                helpSource: this.helpSource,
                rpcNode: this.rpcNode,
                items: [{
                    bodyStyle: "padding: 15px 5px 5px 15px;",
                    html: Ext.String.format(i18n._("Error: There is no settings class for the node '{0}'."), this.name)
                }]
            });
        }
        this.settingsWin.addListener("hide", Ext.bind(function() {
            if ( Ext.isFunction(this.beforeClose)) {
                this.beforeClose();
            }
            this.destroy();
        }, this.settingsWin));
        this.settingsWin.show();
        Ext.MessageBox.hide();
    },

    // remove node
    removeAction: function() {
        this.setState("attention");
        if(this.getEl()) {
            this.getEl().mask();
            this.getEl().fadeOut({ opacity: 0.1, duration: 2500, remove: false, useDisplay:false});
        }
        rpc.nodeManager.destroy(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception, Ext.bind(function() {
                if(this.getEl()) {
                    this.getEl().unmask();
                    this.getEl().stopAnimation();
                }
            }, this),"alert")) return;
            var nodeName = "";
            if (this) {
                if(this.getEl()) {
                    this.getEl().stopAnimation();
                }
                nodeName = this.name;
                var cmp = this;
                Ext.destroy(cmp);
                cmp = null;
                for (var i = 0; i < Ung.Main.nodes.length; i++) {
                    if (nodeName == Ung.Main.nodes[i].name) {
                        Ung.Main.nodes.splice(i, 1);
                        break;
                    }
                }
            }
            if(nodeName == "reports") {
                rpc.reportsEnabled = false;
                delete rpc.nodeReports;
                delete rpc.reportsManager;
                Ung.Main.updateReportsDependencies();
            } else {
                Ung.dashboard.loadDashboard();
            }
            if(nodeName == "policy-manager") {
                Ung.Main.loadPolicies();
            } else {
                Ung.Main.updateAppsView();
            }
            
        }, this), this.nodeId);
    },
    // initialize faceplate metrics
    initMetrics: function() {
        if(this.metrics != null && this.metrics.list != null) {
            if( this.metrics.list.length > 0 ) {
                this.faceplateMetrics = Ext.create('Ung.FaceplateMetric', {
                    nodeName: this.name,
                    displayName: this.displayName,
                    parentId: this.getId(),
                    parentNodeId: this.nodeId,
                    metrics: this.metrics
                });
                this.faceplateMetrics.render('node-metrics_' + this.getId());
                this.subCmps.push(this.faceplateMetrics);
            }
        }
    },
    getLicenseMessage: function() {
        var licenseMessage = "";
        if (!this.license) {
            return licenseMessage;
        }
        if(this.license.trial) {
            if(this.license.expired) {
                licenseMessage = i18n._("Free trial expired!");
            } else if (this.license.daysRemaining < 2) {
                licenseMessage = i18n._("Free trial.") + " " + i18n._("Expires today.");
            } else if (this.license.daysRemaining < 32) {
                licenseMessage = i18n._("Free trial.") + " " + Ext.String.format("{0} ", this.license.daysRemaining) + i18n._("days remain.");
            } else {
                licenseMessage = i18n._("Free trial.");
            }
        } else if (!this.license.valid) {
            licenseMessage = this.license.status;
        }
        return licenseMessage;
    },
    updateLicense: function (license) {
        this.license=license;
        if(testMode && this.license) {
            this.license.trial = Math.random() > 0.5;
            this.license.valid = Math.random() > 0.5;
        }
        if(this.getEl()) {
            this.getEl().down("div[class=node-faceplate-info]").dom.innerHTML=this.getLicenseMessage();
            document.getElementById("node-power_"+this.getId()).className=this.hasPowerButton?(this.license && !this.license.valid)?"node-power node-power-expired":"node-power":"";
            var nodeBuyButton=Ext.getCmp("node-buy-button_"+this.getId());
            if(nodeBuyButton) {
                if(this.license && this.license.trial) {
                    nodeBuyButton.show();
                } else {
                    nodeBuyButton.hide();
                }
            }
        }
        var panelStatus = this.getSettingsAppPanel();
        if(panelStatus) {
            panelStatus.updateLicense(this.license);
        }
    }
});

Ext.define("Ung.NodePreview", {
    extend: "Ext.Component",
    cls: 'node',
    statics: {
        template: new Ext.Template('<div class="node-image"><img src="{image}"/></div>', '<div class="node-label">{displayName}</div>'),
        removeNode: function(nodeName) {
            Ext.destroy(Ext.getCmp("node_preview_"+nodeName));
        }
    },
    constructor: function(config) {
        Ung.Main.nodePreviews[config.name] = Ext.clone(config);
        this.id = "node_preview_" + config.name;
        this.callParent(arguments);
    },
    afterRender: function() {
        var templateHTML = Ung.NodePreview.template.applyTemplate({
            'id': this.getId(),
            'image': '/skins/'+rpc.skinSettings.skinName+'/images/admin/apps/'+this.name+'_80x80.png',
            'displayName': this.displayName
        });
        this.getEl().insertHtml("afterBegin", templateHTML);
        this.getEl().scrollIntoView(Ung.Main.appsPanel.getEl());
        this.getEl().setOpacity(0.1);
        this.getEl().fadeIn({ opacity: 0.6, duration: 12000});
    },
    beforeDestroy: function() {
        if(this.getEl()) {
            this.getEl().stopAnimation();
        }
        if(Ung.Main.nodePreviews[this.name] !== undefined) {
            delete Ung.Main.nodePreviews[this.name];
        }
        this.callParent(arguments);
    }
});

// Metric Manager object
Ung.MetricManager = {
    // update interval in millisecond
    updateFrequency: 3000,
    started: false,
    intervalId: null,
    cycleCompleted: true,
    firstToleratedError: null,
    errorToleranceInterval: 300000, //5 minutes

    start: function(now) {
        this.stop();
        if(now) {
            Ung.MetricManager.run();
        }
        this.setFrequency(this.updateFrequency);
        this.started = true;
    },
    setFrequency: function(timeMs) {
        this.currentFrequency = timeMs;
        if (this.intervalId !== null) {
            window.clearInterval(this.intervalId);
        } else {
            Ung.MetricManager.run();
        }
        this.intervalId = window.setInterval(function() {Ung.MetricManager.run();}, timeMs);
    },
    stop: function() {
        if (this.intervalId !== null) {
            window.clearInterval(this.intervalId);
        }
        this.cycleCompleted = true;
        this.started = false;
    },
    run: function () {
        if (!this.cycleCompleted) {
            return;
        }
        this.cycleCompleted = false;
        rpc.metricManager.getMetricsAndStats(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception, Ext.bind(function() {
                //Tolerate Error 500: Internal Server Error after an install
                //Keep silent for maximum 5 minutes of sequential error messages because apache may reload
                if ( exception.code == 500 || exception.code == 12031 ) {
                    if(this.firstToleratedError==null) {
                        this.firstToleratedError=(new Date()).getTime();
                        this.cycleCompleted = true;
                        return;
                    } else if( ((new Date()).getTime() - this.firstToleratedError ) < this.errorToleranceInterval ) {
                        this.cycleCompleted = true;
                        return;
                    }
                }
                // After a hostname change and the certificate is regenerated.
                else if ( exception.code == 12019 ) {
                    Ext.MessageBox.alert(i18n._("System Busy"), "Please refresh the page", Ext.bind(function() {
                        this.cycleCompleted = true;
                    }, this));
                    return;
                }
                // otherwise call handleException but without "noAlert"
                Ung.Util.handleException(exception, Ext.bind(function() {
                    this.cycleCompleted = true;
                }, this));
            }, this),"noAlert")) return;
            this.firstToleratedError=null; //reset error tolerance on a good response
            this.cycleCompleted = true;

            // update system stats
            Ung.Main.stats = result.systemStats;
            Ung.Main.systemStats.update(result.systemStats);
            Ung.dashboard.updateStats();

            var i;
            for (i = 0; i < Ung.Main.nodes.length; i++) {
                var nodeCmp = Ung.Node.getCmp(Ung.Main.nodes[i].nodeId);

                if (nodeCmp && nodeCmp.isRunning()) {
                    nodeCmp.metrics = result.metrics[Ung.Main.nodes[i].nodeId];
                    nodeCmp.updateMetrics();
                }
            }
        }, this));
    }
};

//Metric Manager object
Ung.LicenseLoader = {
    // update interval in millisecond
    updateFrequency: 60000,
    //how many times to check (50 times x 1 minute = 50 minutes)
    count: 50,
    started: false,
    intervalId: null,
    check: function() {
        this.count = 50;
        if(!this.started) {
            this.intervalId = window.setInterval(function() {Ung.LicenseLoader.run();}, this.updateFrequency);
            this.started = true;
        }
        return true;
    },
    stop: function() {
        if (this.intervalId !== null) {
            window.clearInterval(this.intervalId);
        }
        this.started = false;
    },
    run: function () {
        this.count--;
        if(this.count>0) {
            Ung.Main.reloadLicenses();
        } else {
            this.stop();
        }
    }
};

Ext.define("Ung.SystemStats", {
    extend: "Ext.Component",
    cls: 'system-stats',
    afterRender: function() {
        this.callParent(arguments);
        var contentSystemStatsArr=[
            '<div class="label" style="width:100px;left:0px;">'+i18n._("Network")+'</div>',
            '<div class="label" style="width:70px;left:91px;" onclick="Ung.Main.showSessions()">'+i18n._("Sessions")+'</div>',
            '<div class="label" style="width:60px;left:149px;" onclick="Ung.Main.showHosts()">'+i18n._("Hosts")+'</div>',
            '<div class="label" style="width:70px;left:198px;">'+i18n._("CPU Load")+'</div>',
            '<div class="label" style="width:75px;left:273px;">'+i18n._("Memory")+'</div>',
            '<div class="label" style="width:40px;right:-5px;">'+i18n._("Disk")+'</div>',
            '<div class="network"><div class="tx">'+i18n._("Tx:")+'<div class="tx-value"></div></div><div class="rx">'+i18n._("Rx:")+'<div class="rx-value"></div></div></div>',
            '<div class="sessions" onclick="Ung.Main.showSessions()"></div>',
            '<div class="hosts" onclick="Ung.Main.showHosts()"></div>',
            '<div class="cpu"></div>',
            '<div class="memory"><div class="free">'+i18n._("F:")+'<div class="free-value"></div></div><div class="used">'+i18n._("U:")+'<div class="used-value"></div></div></div>',
            '<div class="disk"><svg viewBox="0 0 32 32"><circle name="disk_value" r="16" cx="16" cy="16"/></svg></div>'
        ];
        this.getEl().insertHtml("afterBegin", contentSystemStatsArr.join(''));

        // network tooltip
        var networkArr=[
            '<div class="title">'+i18n._("TX Speed:")+'</div>',
            '<div class="values"><span name="tx_speed">&nbsp;</span></div>',
            '<div class="title">'+i18n._("RX Speed:")+'</div>',
            '<div class="values"><span name="rx_speed">&nbsp;</span></div>'
        ];
        this.networkToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=network]"),
            dismissDelay: 0,
            hideDelay: 400,
            width: 330,
            cls: 'extended-stats',
            html: networkArr.join('')
        });

        // sessions tooltip
        var sessionsArr=[
            '<div class="title">'+i18n._("Total Sessions:")+'</div>',
            '<div class="values"><span name="totalSessions">&nbsp;</span></div>',
            '<div class="title">'+i18n._("TCP Sessions:")+'</div>',
            '<div class="values"><span name="uvmTCPSessions">&nbsp;</span></div>',
            '<div class="title">'+i18n._("UDP Sessions:")+'</div>',
            '<div class="values"><span name="uvmUDPSessions">&nbsp;</span></div>',
            '<div class="title">'+i18n._("Click to view Sessions")+'</div>'
        ];
        this.sessionsToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=sessions]"),
            dismissDelay: 0,
            hideDelay: 1000,
            width: 330,
            cls: 'extended-stats',
            html: sessionsArr.join('')
        });

        // hosts tooltip
        var hostsArr=[
            '<div class="title">'+i18n._("Active Hosts:")+'</div>',
            '<div class="values"><span name="activeHosts">&nbsp</span></div>',
            '<div class="title">'+i18n._("Click to view Hosts")+'</div>'
        ];
        this.hostsToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=hosts]"),
            dismissDelay: 0,
            hideDelay: 1000,
            width: 330,
            cls: 'extended-stats',
            html: hostsArr.join('')
        });
        // cpu tooltip
        var cpuArr=[
            '<div class="title">'+i18n._("Number of Processors / Type / Speed:")+'</div>',
            '<div class="values"><span name="num_cpus">&nbsp;</span>, <span name="cpu_model">&nbsp;</span>, <span name="cpu_speed">&nbsp;</span></div>',
            '<div class="title">'+i18n._("Load average (1 min , 5 min , 15 min):")+'</div>',
            '<div class="values"><span name="load_average_1_min">&nbsp;</span>, <span name="load_average_5_min">&nbsp;</span>, <span name="load_average_15_min">&nbsp;</span></div>',
            '<div class="title">'+i18n._("Tasks (Processes)")+'</div>',
            '<div class="values"><span name="tasks">&nbsp;</span>'+'</div>',
            '<div class="title">'+i18n._("Uptime:")+'</div>',
            '<div class="values"><span name="uptime">&nbsp;</span></div>'
        ];
        this.cpuToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=cpu]"),
            dismissDelay: 0,
            hideDelay: 400,
            width: 330,
            cls: 'extended-stats',
            html: cpuArr.join('')
        });

        // memory tooltip
        var memoryArr=[
            '<div class="title">'+i18n._("Total Memory:")+'</div>',
            '<div class="values"><span name="memory_total">&nbsp;</span> MB</div>',
            '<div class="title">'+i18n._("Memory Used:")+'</div>',
            '<div class="values"><span name="memory_used">&nbsp;</span> MB, <span name="memory_used_percent"></span> %</div>',
            '<div class="title">'+i18n._("Memory Free:")+'</div>',
            '<div class="values"><span name="memory_free">&nbsp;</span> MB, <span name="memory_free_percent"></span> %</div>',
            '<div class="title">'+i18n._("Swap Files:")+'</div>',
            '<div class="values"><span name="swap_total">&nbsp;</span> MB '+i18n._("total swap space")+' (<span name="swap_used"></span> MB '+i18n._("used")+')</div>'
        ];
        this.memoryToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=memory]"),
            dismissDelay: 0,
            hideDelay: 400,
            width: 330,
            cls: 'extended-stats',
            html: memoryArr.join('')
        });

        // disk tooltip
        var diskArr=[
            '<div class="title">'+i18n._("Total Disk Space:")+'</div>',
            '<div class="values"><span name="total_disk_space"></span> GB</div>',
            '<div class="title">'+i18n._("Free Disk Space:")+'</div>',
            '<div class="values"><span name="free_disk_space"></span> GB</div>',
            '<div class="title">'+i18n._("Data read:")+'</div>',
            '<div class="values"><span name="disk_reads"></span> MB, <span name="disk_reads_per_second"></span> b/sec</div>',
            '<div class="title">'+i18n._("Data write:")+'</div>',
            '<div class="values"><span name="disk_writes"></span> MB, <span name="disk_writes_per_second"></span> b/sec</div>'
        ];
        this.diskToolTip = Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=disk]"),
            dismissDelay: 0,
            hideDelay: 400,
            width: 330,
            cls: 'extended-stats',
            html: diskArr.join('')
        });

    },
    update: function(stats) {
        var toolTipEl;
        var sessionsText = '<font color="#55BA47">' + stats.uvmSessions + "</font>";
        this.getEl().down("div[class=sessions]").dom.innerHTML=sessionsText;
        var hostsText = '<font color="#55BA47">' + stats.activeHosts + "</font>";
        this.getEl().down("div[class=hosts]").dom.innerHTML=hostsText;
        this.getEl().down("div[class=cpu]").dom.innerHTML=stats.oneMinuteLoadAvg;
        var oneMinuteLoadAvg = stats.oneMinuteLoadAvg;
        var oneMinuteLoadAvgAdjusted = oneMinuteLoadAvg - stats.numCpus;
        var loadText = '<font color="#55BA47">' + i18n._('low') + '</font>';
        if (oneMinuteLoadAvgAdjusted > 1.0) {
            loadText = '<font color="orange">' + i18n._('medium') + '</font>';
        }
        if (oneMinuteLoadAvgAdjusted > 4.0) {
            loadText = '<font color="red">' + i18n._('high') + '</font>';
        }
        this.getEl().down("div[class=cpu]").dom.innerHTML=loadText;

        var txSpeed=(stats.interface_wans_txBps<1000000) ? { value: Math.round(stats.interface_wans_txBps/10)/100, unit:"KB/s" }: {value: Math.round(stats.interface_wans_txBps/10000)/100, unit:"MB/s"};
        var rxSpeed=(stats.interface_wans_rxBps<1000000) ? { value: Math.round(stats.interface_wans_rxBps/10)/100, unit:"KB/s" }: {value: Math.round(stats.interface_wans_rxBps/10000)/100, unit:"MB/s"};
        this.getEl().down("div[class=tx-value]").dom.innerHTML=txSpeed.value+txSpeed.unit;
        this.getEl().down("div[class=rx-value]").dom.innerHTML=rxSpeed.value+rxSpeed.unit;
        var memoryFree=Ung.Util.bytesToMBs(stats.MemFree);
        var memoryUsed=Ung.Util.bytesToMBs(stats.MemTotal-stats.MemFree);
        Ung.Main.totalMemoryMb = Ung.Util.bytesToMBs(stats.MemTotal);
        this.getEl().down("div[class=free-value]").dom.innerHTML=memoryFree+" MB";
        this.getEl().down("div[class=used-value]").dom.innerHTML=memoryUsed+" MB";
        this.getEl().down("[name=disk_value]").setStyle("stroke-dasharray", (stats.totalDiskSpace > 0 ? (1-stats.freeDiskSpace/stats.totalDiskSpace) * 100 : 0) + ' 100');
        if(this.networkToolTip.rendered) {
            toolTipEl=this.networkToolTip.getEl();
            toolTipEl.down("span[name=tx_speed]").dom.innerHTML=txSpeed.value+" "+txSpeed.unit;
            toolTipEl.down("span[name=rx_speed]").dom.innerHTML=rxSpeed.value+" "+rxSpeed.unit;
        }
        if(this.sessionsToolTip.rendered) {
            toolTipEl=this.sessionsToolTip.getEl();
            toolTipEl.down("span[name=totalSessions]").dom.innerHTML=stats.uvmSessions;
            toolTipEl.down("span[name=uvmTCPSessions]").dom.innerHTML=stats.uvmTCPSessions;
            toolTipEl.down("span[name=uvmUDPSessions]").dom.innerHTML=stats.uvmUDPSessions;
        }
        if(this.hostsToolTip.rendered) {
            toolTipEl=this.hostsToolTip.getEl();
            toolTipEl.down("span[name=activeHosts]").dom.innerHTML=stats.activeHosts;
        }

        if(this.cpuToolTip.rendered) {
            toolTipEl=this.cpuToolTip.getEl();
            toolTipEl.down("span[name=num_cpus]").dom.innerHTML=stats.numCpus;
            toolTipEl.down("span[name=cpu_model]").dom.innerHTML=stats.cpuModel;
            toolTipEl.down("span[name=cpu_speed]").dom.innerHTML=stats.cpuSpeed;
            var uptimeAux=Math.round(stats.uptime);
            var uptimeSeconds = uptimeAux%60;
            uptimeAux=parseInt(uptimeAux/60, 10);
            var uptimeMinutes = uptimeAux%60;
            uptimeAux=parseInt(uptimeAux/60, 10);
            var uptimeHours = uptimeAux%24;
            uptimeAux=parseInt(uptimeAux/24, 10);
            var uptimeDays = uptimeAux;

            toolTipEl.down("span[name=uptime]").dom.innerHTML=(uptimeDays>0?(uptimeDays+" "+(uptimeDays==1?i18n._("Day"):i18n._("Days"))+", "):"") + ((uptimeDays>0 || uptimeHours>0)?(uptimeHours+" "+(uptimeHours==1?i18n._("Hour"):i18n._("Hours"))+", "):"") + uptimeMinutes+" "+(uptimeMinutes==1?i18n._("Minute"):i18n._("Minutes"));
            toolTipEl.down("span[name=tasks]").dom.innerHTML=stats.numProcs;
            toolTipEl.down("span[name=load_average_1_min]").dom.innerHTML=stats.oneMinuteLoadAvg;
            toolTipEl.down("span[name=load_average_5_min]").dom.innerHTML=stats.fiveMinuteLoadAvg;
            toolTipEl.down("span[name=load_average_15_min]").dom.innerHTML=stats.fifteenMinuteLoadAvg;
        }
        if(this.memoryToolTip.rendered) {
            toolTipEl=this.memoryToolTip.getEl();
            toolTipEl.down("span[name=memory_used]").dom.innerHTML=memoryUsed;
            toolTipEl.down("span[name=memory_free]").dom.innerHTML=memoryFree;
            toolTipEl.down("span[name=memory_total]").dom.innerHTML=Ung.Util.bytesToMBs(stats.MemTotal);
            toolTipEl.down("span[name=memory_used_percent]").dom.innerHTML=Math.round((stats.MemTotal-stats.MemFree)/stats.MemTotal*100);
            toolTipEl.down("span[name=memory_free_percent]").dom.innerHTML=Math.round(stats.MemFree/stats.MemTotal*100);

            toolTipEl.down("span[name=swap_total]").dom.innerHTML=Ung.Util.bytesToMBs(stats.SwapTotal);
            toolTipEl.down("span[name=swap_used]").dom.innerHTML=Ung.Util.bytesToMBs(stats.SwapTotal-stats.SwapFree);
        }
        if(this.diskToolTip.rendered) {
            toolTipEl=this.diskToolTip.getEl();
            toolTipEl.down("span[name=total_disk_space]").dom.innerHTML=Math.round(stats.totalDiskSpace/10000000)/100;
            toolTipEl.down("span[name=free_disk_space]").dom.innerHTML=Math.round(stats.freeDiskSpace/10000000)/100;
            toolTipEl.down("span[name=disk_reads]").dom.innerHTML=Ung.Util.bytesToMBs(stats.diskReads);
            toolTipEl.down("span[name=disk_reads_per_second]").dom.innerHTML=Math.round(stats.diskReadsPerSecond*100)/100;
            toolTipEl.down("span[name=disk_writes]").dom.innerHTML=Ung.Util.bytesToMBs(stats.diskWrites);
            toolTipEl.down("span[name=disk_writes_per_second]").dom.innerHTML=Math.round(stats.diskWritesPerSecond*100)/100;
        }
    },
    reset: function() {
    }
});

// Faceplate Metric Class
Ext.define("Ung.FaceplateMetric", {
    extend: "Ext.Component",
    html: '<div class="chart"></div><div class="system"><div class="system-box"></div></div>',
    parentId: null,
    parentNodeId: null,
    byteCountCurrent: null,
    byteCountLast: null,
    sessionCountCurrent: null,
    sessionCountTotal: null,
    sessionRequestLast: null,
    sessionRequestTotal: null,
    hasChart: false,
    chart: null,
    chartData: null,
    chartDataLength: 20,
    afterRender: function() {
            this.callParent(arguments);
        var out = [];
        for (var i = 0; i < 4; i++) {
            var top = 1 + i * 15;
            out.push('<div class="system-label" style="top:' + top + 'px;" id="systemName_' + this.getId() + '_' + i + '"></div>');
            out.push('<div class="system-value" style="top:' + top + 'px;" id="systemValue_' + this.getId() + '_' + i + '"></div>');
        }
        var systemBoxEl=this.getEl().down("div[class=system-box]");
        systemBoxEl.insertHtml("afterBegin", out.join(""));
        this.buildActiveMetrics();
        systemBoxEl.on("click", this.showMetricSettings , this);
        this.buildChart();
    },
    beforeDestroy: function() {
        if(this.chart != null ) {
            Ext.destroy(this.chart);
        }
        this.callParent(arguments);
    },
    buildChart: function() {
        var me = this, i;
        for(i=0; i<this.metrics.list.length; i++) {
            if(this.metrics.list[i].name=="live-sessions") {
                this.hasChart = true;
                break;
            }
        }
        //Do not show chart graph for these apps even though they have the live-sessions metrics
        if(this.nodeName === "firewall" ||
           this.nodeName === "openvpn" ||
           this.nodeName === "wan-balancer") {
                this.hasChart = false;
        }
        var chartContainerEl = this.getEl().down("div[class=chart]");
        //Do not build chart graph if the node doesn't have live-session metrics
        if( !this.hasChart ) {
            chartContainerEl.hide();
            return;
        }

        this.chartData = [];
        for(i=0; i<this.chartDataLength; i++) {
            this.chartData.push({time:i, sessions:0});
        }
        this.chart = Ung.charts.nodeChart(chartContainerEl.dom, this.chartData);
        
        chartContainerEl.on("click", function(e) { 
            Ung.Main.showNodeSessions( this.parentNodeId ); 
        }, this);
    },
    buildActiveMetrics: function () {
        var nodeCmp = Ext.getCmp(this.parentId);
        var activeMetrics = nodeCmp.activeMetrics;
        if(activeMetrics.length>4) {
            Ext.MessageBox.alert(i18n._("Warning"), Ext.String.format(i18n._("The app {0} has {1} metrics. The maximum number of metrics is {2}."),nodeCmp.displayName ,activeMetrics.length,4));
        }
        var metricsLen=Math.min(activeMetrics.length,4);
        var i, nameDiv, valueDiv;
        // set all four to blank
        for(i=0; i<4;i++) {
            nameDiv=document.getElementById('systemName_' + this.getId() + '_' + i);
            valueDiv=document.getElementById('systemValue_' + this.getId() + '_' + i);
            nameDiv.innerHTML = "&nbsp;";
            nameDiv.style.display="none";
            valueDiv.innerHTML = "&nbsp;";
            valueDiv.style.display="none";
        }
        // fill in name and value
        for(i=0; i<metricsLen;i++) {
            var metricIndex=activeMetrics[i];
            var metric = nodeCmp.metrics.list[metricIndex];
            if (metric != null && metric !== undefined) {
                nameDiv=document.getElementById('systemName_' + this.getId() + '_' + i);
                valueDiv=document.getElementById('systemValue_' + this.getId() + '_' + i);
                nameDiv.innerHTML = i18n._(metric.displayName);
                nameDiv.style.display="";
                valueDiv.innerHTML = "&nbsp;";
                valueDiv.style.display="";
            }
        }
    },
    showMetricSettings: function() {
        var nodeCmp = Ext.getCmp(this.parentId);
        this.newActiveMetrics=[];
        var i;
        if(this.configWin==null) {
            var configItems=[];
            for(i=0;i<nodeCmp.metrics.list.length;i++) {
                var metric = nodeCmp.metrics.list[i];
                configItems.push({
                    xtype: 'checkbox',
                    boxLabel: i18n._(metric.displayName),
                    hideLabel: true,
                    name: metric.displayName,
                    dataIndex: i,
                    checked: false,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                if(checked && this.newActiveMetrics.length>=4) {
                                    Ext.MessageBox.alert(i18n._("Warning"),i18n._("Please set up to four items."));
                                    elem.setValue(false);
                                    return;
                                }
                                var itemIndex = -1;
                                for(var i=0;i<this.newActiveMetrics.length;i++) {
                                    if(this.newActiveMetrics[i]==elem.dataIndex) {
                                        itemIndex = i;
                                        break;
                                    }
                                }
                                if(checked) {
                                    if(itemIndex == -1) {
                                        this.newActiveMetrics.push(elem.dataIndex);
                                    }
                                } else {
                                    if(itemIndex != -1) {
                                        this.newActiveMetrics.splice(itemIndex,1);
                                    }
                                }
                            }, this)
                        }
                    }
                });
            }
            this.configWin= Ext.create("Ung.Window", {
                metricsCmp: this,
                modal: true,
                title: i18n._("Set up to four"),
                bodyStyle: "padding: 5px 5px 5px 15px;",
                items: configItems,
                autoScroll: true,
                layout: 'auto',
                draggable: true,
                resizable: true,
                buttons: [{
                    name: 'Ok',
                    text: i18n._("Ok"),
                    handler: Ext.bind(function() {
                        this.updateActiveMetrics();
                        this.configWin.hide();
                    }, this)
                },{
                    name: 'Cancel',
                    text: i18n._("Cancel"),
                    handler: Ext.bind(function() {
                        this.configWin.hide();
                    }, this)
                }],
                listeners: {
                    show: {
                        fn: function() {
                            this.setSize({width:260,height:280});
                            this.alignTo(this.metricsCmp.getEl(),"tr-br");
                            var pos=this.getPosition();
                            var sub=pos[1] + 280 - Ung.Main.viewport.getSize().height;
                            if(sub > 0) {
                                this.setPosition( pos[0],pos[1]-sub);
                            }
                        }
                    }
                }
            });
        }

        for(i=0;i<this.configWin.items.length;i++) {
            this.configWin.items.get(i).setValue(false);
        }
        for(i=0 ; i<nodeCmp.activeMetrics.length ; i++ ) {
            var metricIndex = nodeCmp.activeMetrics[i];
            var metricItem=this.configWin.items.get(metricIndex);
            if (metricItem != null)
                metricItem.setValue(true);
        }
        this.configWin.show();
    },
    updateActiveMetrics: function() {
        var nodeCmp = Ext.getCmp(this.parentId);
        nodeCmp.activeMetrics = this.newActiveMetrics;
        this.buildActiveMetrics();
    },
    update: function(metrics) {
        // update counts
        var nodeCmp = Ext.getCmp(this.parentId);
        var activeMetrics = nodeCmp.activeMetrics;
        var i;
        for (i = 0; i < activeMetrics.length; i++) {
            var metricIndex = activeMetrics[i];
            var metric = nodeCmp.metrics.list[metricIndex];
            if (metric != null && metric !== undefined) {
                var newValue="&nbsp;";
                newValue = metric.value;
                var valueDiv = document.getElementById('systemValue_' + this.getId() + '_' + i);
                if(valueDiv!=null) {
                    valueDiv.innerHTML = newValue;
                }
            }
        }
        if( this.hasChart && this.chartData != null ) {
            var reloadChart = this.chartData[0].sessions != 0;
            for(i=0;i<this.chartData.length-1;i++) {
                this.chartData[i].sessions=this.chartData[i+1].sessions;
                reloadChart = (reloadChart || (this.chartData[i].sessions != 0));
            }
            this.currentSessions = this.getCurrentSessions(nodeCmp.metrics);
            reloadChart = (reloadChart || (this.currentSessions!=0));
            this.chartData[this.chartData.length-1].sessions=this.currentSessions;

            if(reloadChart) {
                Ung.charts.nodeChartUpdate(this.chart, this.chartData);
            }
        }
    },
    getCurrentSessions: function(metrics) {
        if(this.currentSessionsMetricIndex == null) {
            this.currentSessionsMetricIndex = -1;
            for(var i=0;i<metrics.list.length; i++) {
                if(metrics.list[i].name=="live-sessions") {
                    this.currentSessionsMetricIndex = i;
                    break;
                }
            }
        }
        if(testMode) {
            if(!this.maxRandomNumber) {
                this.maxRandomNumber=Math.floor((Math.random()*200));
            }
            //Just for test generate random data
            return this.currentSessionsMetricIndex>=0?Math.floor((Math.random()*this.maxRandomNumber)):0;
        }
        return this.currentSessionsMetricIndex>=0?metrics.list[this.currentSessionsMetricIndex].value:0;
    },
    reset: function() {
        if (this.chartData != null) {
            var i;
            for(i = 0; i<this.chartData.length; i++) {
                this.chartData[i].sessions=0;
            }
            Ung.charts.nodeChartUpdate(this.chart, this.chartData);
            for (i = 0; i < 4; i++) {
                var valueDiv = document.getElementById('systemValue_' + this.getId() + '_' + i);
                valueDiv.innerHTML = "&nbsp;";
            }
        }
    }
});

// Navigation Breadcrumbs
Ext.define('Ung.Breadcrumbs', {
    extend:'Ext.Component',
    elements: null,
    afterRender: function() {
        this.callParent(arguments);
        if (this.elements != null) {
            for (var i = 0; i < this.elements.length; i++) {
                if (i > 0) {
                    this.getEl().insertHtml('beforeEnd', '<span class="icon-breadcrumbs-separator">&nbsp;&nbsp;&nbsp;&nbsp;</span>');
                }
                var crumb = this.elements[i];
                if (crumb.action) {
                    var crumbEl = document.createElement("span");
                    crumbEl.className = 'breadcrumb-link';
                    crumbEl.innerHTML = crumb.title;
                    crumbEl = Ext.get(crumbEl);
                    crumbEl.on("click", crumb.action, this);
                    this.getEl().appendChild(crumbEl);

                } else {
                    this.getEl().insertHtml('beforeEnd', '<span class="breadcrumb-text" >' + crumb.title + '</span>');
                }
            }
        }
    }
});
Ext.define('Ung.panel.Status', {
    extend:'Ext.panel.Panel',
    name: 'panelStatus',
    cls: 'ung-panel',
    hasPowerSection: false,
    hasLicenseSection: false,
    hasMetrics: false,
    hasChart: false,
    scrollable: true,
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    defaults: {
        xtype: 'fieldset'
    },
    initComponent: function() {
        var me = this;
        if(!this.title) {
            this.title = i18n._('Status');
        }
        this.nodeId = this.settingsCmp.nodeId;
        this.displayName = this.settingsCmp.displayName;
        var node = Ung.Node.getCmp(this.nodeId);
        this.items = [{
            xtype: 'fieldset',
            title: i18n._('Summary'),
            html: this.settingsCmp.getAppSummary()
        }];

        if(node.hasPowerButton) {
            this.hasPowerSection = true;
            var isRunning = node.isRunning();
            this.items.push({
                title: i18n._('Power'),
                name: 'powerSection',
                cls: isRunning? 'app-on': 'app-off',
                items: [{
                    xtype: 'component',
                    name: 'powerStatus',
                    cls: 'app-status',
                    html: this.getPowerMessage(isRunning)
                }, {
                    xtype: 'button',
                    margin: '10 0 0 0',
                    name: 'powerButton',
                    disabled: (node.license && !node.license.valid),
                    iconCls: 'app-power-button',
                    text: isRunning ? i18n._("Disable") : i18n._("Enable"),
                    handler: function(button) {
                        this.updateToAttention();
                        Ung.Node.getCmp(this.nodeId).onPowerClick();
                    },
                    scope: this
                }]
            });
        }
        if(node.license && (node.license.trial || !node.license.valid)) {
            this.hasLicenseSection = true;
            this.items.push({
                title: i18n._('License'),
                name: 'licenseSection',
                items: [{
                    xtype: 'component',
                    name: 'licenseStatus',
                    cls: 'app-status-disabled',
                    html: node.getLicenseMessage()
                }, {
                    xtype: "button",
                    name: 'licenseBuyButton',
                    margin: '10 0 0 0',
                    iconCls: 'icon-buy',
                    hidden: !node.license.trial,
                    cls: 'buy-button',
                    text: i18n._('Buy Now'),
                    handler: function() {
                        Ung.Node.getCmp(this.nodeId).onBuyNowAction();
                    },
                    scope: this
                }]
            });
        }
        if(this.itemsAfterLicense) {
            this.items.push.apply(this.items, this.itemsAfterLicense);
        }
        
        if(node.metrics && node.metrics.list.length>0) {
            this.hasMetrics = true;
            var viewportWidth = Ung.Main.viewport.getWidth();
            var metricsItems = [], metric, hasChart=false;
            for(var i=0; i<node.metrics.list.length; i++) {
                metric = node.metrics.list[i];
                if(metric.name=="live-sessions") {
                    this.hasChart = true;
                    if( node.name === "firewall" ||
                        node.name === "openvpn" ||
                        node.name === "wan-balancer" ){
                        this.hasChart = false;
                    }
                }
                metricsItems.push({
                    fieldLabel: i18n._(metric.displayName),
                    name: metric.name
                });
            }
            if(this.hasChart) {
                this.items.push({
                    title: i18n._('Sessions'),
                    items: [{
                        xtype: 'container',
                        width: 300,
                        height: 130,
                        name: 'sessionsChart'
                    }]
                });
            }
            this.items.push({
                name: 'metrics',
                layout: 'vbox',
                title: i18n._('Metrics'),
                defaults: {
                    margin: '0 10 0 0',
                    xtype: 'displayfield',
                    labelWidth: 190,
                    width: 300
                },
                items: metricsItems
            });
        }
        if(this.itemsToAppend) {
            this.items.push.apply(this.items, this.itemsToAppend);
        }

        this.callParent(arguments);
    },
    afterRender: function() {
        this.callParent(arguments);
        if(this.hasMetrics) {
            this.metricsSection = this.down("fieldset[name=metrics]");
            if(this.hasChart) {
                this.chart = Ung.charts.appStatusChart(this.down("container[name=sessionsChart]").getEl().dom);
            }
        }
    },
    isDirty: function() {
        return false;
    },
    getPowerMessage: function(isRunning) {
        var powerMessage = isRunning ? Ext.String.format(i18n._("{0} is enabled."), this.displayName) : Ext.String.format(i18n._("{0} is disabled."), this.displayName);
        return powerMessage;
    },
    updatePower: function(isRunning) {
        if(this.hasPowerSection) {
            var powerSection = this.down("[name=powerSection]");
            powerSection.removeCls(["app-on","app-off","app-attention"]);
            powerSection.addCls(isRunning ? 'app-on': 'app-off');
            this.down("[name=powerStatus]").update(this.getPowerMessage(isRunning));
            var powerButton = this.down("button[name=powerButton]");
            powerButton.setText(isRunning ? i18n._("Disable") : i18n._("Enable"));
        }
    },
    updateToAttention: function() {
        if(this.hasPowerSection) {
            var powerSection = this.down("[name=powerSection]");
            powerSection.removeCls(["app-on","app-off","app-attention"]);
            powerSection.addCls("app-attention");
        }
    },
    updateLicense: function(license) {
        if(this.hasLicenseSection) {
            var licenseSection = this.down("[name=licenseSection]");
            var licenseSectionVisible = license && (license.trial || !license.valid);
            licenseSection.setVisible(licenseSectionVisible);
            if(licenseSectionVisible) {
                var licenseStatus = this.down("[name=licenseStatus]");
                licenseStatus.update({'html': Ung.Node.getCmp(this.nodeId).getLicenseMessage()});
                var licenseBuyButton = this.down("button[name=licenseBuyButton]");
                licenseBuyButton.setVisible(license && license.trial);
            }
        }
    },
    updateMetrics: function(metrics) {
        if(this.hasMetrics) {
            var metricField, metric, chart, reloadChart, i, j;
            for(i=0; i<metrics.list.length; i++) {
                metric = metrics.list[i];
                if(testMode) {
                    metric.value = Math.floor((Math.random()*200));
                }
                metricField = this.metricsSection.down("displayfield[name="+metric.name+"]");
                if(metricField) {
                    metricField.setValue(metric.value);
                }
                if(this.hasChart && metric.name=="live-sessions") {
                    this.chart.series[0].addPoint({
                        x: (new Date()).getTime(),
                        y: metric.value
                    }, true, true);
                }
            }
        }
    }
});