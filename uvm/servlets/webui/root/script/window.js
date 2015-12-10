// Standard Ung window
Ext.define('Ung.Window', {
    extend: 'Ext.window.Window',
    statics: {
        cancelAction: function(dirty, closeWinFn) {
            if (dirty) {
                Ext.MessageBox.confirm(i18n._('Warning'), i18n._('There are unsaved settings which will be lost. Do you want to continue?'),
                function(btn) {
                    if (btn == 'yes') {
                        closeWinFn();
                    }
                });
            } else {
                closeWinFn();
            }
        }
    },
    width: 800,
    modal: true,
    // window title
    title: null,
    // breadcrumbs
    breadcrumbs: null,
    draggable: false,
    resizable: false,
    // sub componetns - used by destroy function
    subCmps: null,
    // size to rack right side on show
    sizeToRack: true,
    layout: 'fit',
    constructor: function(config) {
        var defaults = {
            closeAction: 'cancelAction'
        };
        Ext.applyIf(config, defaults);
        this.subCmps = [];
        this.callParent(arguments);
        },
    initComponent: function() {
        if (!this.title) {
            this.title = '<span id="title_' + this.getId() + '"></span>';
        }
        this.callParent(arguments);
    },
    afterRender: function() {
        this.callParent(arguments);
        if (this.name && this.getEl()) {
            this.getEl().set({
                'name': this.name
                });
        }
        if (this.breadcrumbs) {
            this.subCmps.push(new Ung.Breadcrumbs({
                renderTo: 'title_' + this.getId(),
                elements: this.breadcrumbs
            }));
        }
    },

    beforeDestroy: function() {
        Ext.destroy(this.subCmps);
        this.callParent(arguments);
    },
    // on show position and size
    onShow: function() {
        this.doSize();
        this.callParent(arguments);
    },
    doSize: function() {
        if (this.sizeToRack) {
            this.setSizeToRack();
        }
    },
    setSizeToRack: function () {
        var objSize = Ung.Main.viewport.getSize();
        var left = 0;
        if( objSize.width > 1000 ) {
            left = Ung.Main.menuWidth;
            objSize.width = objSize.width - Ung.Main.menuWidth;
        }
        this.setPosition(left, 0);
        this.setSize(objSize);
    },
    // to override if needed
    isDirty: function() {
        return false;
    },
    validateComponents: function (components) {
        var invalidFields = [];
        var validResult;
        for( var i = 0; i < components.length; i++ ) {
            if( ( validResult = ( Ext.isFunction(components[i].isValid) ? components[i].isValid() : Ung.Util.isValid(components[i]) ) ) != true ){
                invalidFields.push(
                    ( components[i].fieldLabel ? "<b>" +components[i].fieldLabel + "</b>: " : "" ) +
                    ( components[i].activeErrors ? components[i].activeErrors.join( ", ") : validResult )
                );
            }
        }
        if( invalidFields.length ){
            Ext.MessageBox.alert(
                i18n._("Warning"),
                i18n._("One or more fields contain invalid values. Settings cannot be saved until these problems are resolved.") +
                "<br><br>" +
                invalidFields.join( "<br>" )
            );
            return false;
        }
        return true;
    },
    cancelAction: function(handler) {
        if (this.isDirty()) {
            Ext.MessageBox.confirm(i18n._('Warning'), i18n._('There are unsaved settings which will be lost. Do you want to continue?'),
               Ext.bind(function(btn) {
                   if (btn == 'yes') {
                       this.closeWindow(handler);
                   }
               }, this));
        } else {
            this.closeWindow(handler);
        }
    },
    close: function() {
        //Need to override default Ext.Window method to fix issue #10238
        if (this.fireEvent('beforeclose', this) !== false) {
            this.cancelAction();
        }
    },
    // the close window action
    // to override
    closeWindow: function(handler) {
        this.hide();
        if(handler) {
            handler();
        }
    }
});

Ext.define("Ung.SettingsWin", {
    extend: "Ung.Window",
    // config i18n
    i18n: null,
    // holds the json rpc results for the settings classes
    rpc: null,
    // tabs (if the window has tabs layout)
    tabs: null,
    dirtyFlag: false,
    hasApply: true,
    layout: 'fit',
    // build Tab panel from an array of tab items
    constructor: function(config) {
        config.rpc = {};
        var objSize = Ung.Main.viewport.getSize();
        Ext.applyIf(config, {
            height: objSize.height,
            width: objSize.width - Ung.Main.menuWidth,
            x: Ung.Main.menuWidth,
            y: 0
        });
        this.callParent(arguments);
    },
    buildTabPanel: function(itemsArray) {
        Ext.get("racks").hide();
        if(this.hasReports) {
            var reportCategory = this.reportCategory;
            if ( reportCategory == null ) reportCategory = this.displayName; 
            itemsArray.push(Ext.create('Ung.panel.Reports',{
                category: reportCategory
            }));
        }
        
        this.tabs = Ext.create('Ext.tab.Panel',{
            activeTab: 0,
            deferredRender: false,
            items: itemsArray
        });
        this.items=this.tabs;
        this.tabs.on('afterrender', function() {
            Ext.get("racks").show();
            Ext.defer(this.openTarget,1, this);
        }, this);
    },
    openTarget: function() {
        if(Ung.Main.target) {
            var targetTokens = Ung.Main.target.split(".");
            if(targetTokens.length >= 3 && targetTokens[2] !=null ) {
                var tabIndex = this.tabs.items.findIndex('name', targetTokens[2]);
                if(tabIndex != -1) {
                    this.tabs.setActiveTab(tabIndex);
                    if(targetTokens.length >= 4 && targetTokens[3] !=null ) {
                            var activeTab = this.tabs.getActiveTab();
                        var compArr = this.tabs.query('[name="'+targetTokens[3]+'"]');
                        if(compArr.length > 0) {
                            var comp = compArr[0];
                            if(comp) {
                                console.log(comp, comp.xtype, comp.getEl(), comp.handler);
                                if(comp.xtype == "panel") {
                                    var tabPanel = comp.up('tabpanel');
                                    if(tabPanel) {
                                        tabPanel.setActiveTab(comp);
                                    }
                                } else if(comp.xtype == "button") {
                                    comp.getEl().dom.click();
                                }
                            }
                        }
                    }
                }
            }
            Ung.Main.target = null;
        }
    },
    helpAction: function() {
        var helpSource;
        if(this.tabs && this.tabs.getActiveTab()!=null) {
            if( this.tabs.getActiveTab().helpSource != null ) {
                helpSource = this.tabs.getActiveTab().helpSource;
            } else if( Ext.isFunction(this.tabs.getActiveTab().getHelpSource)) {
                helpSource = this.tabs.getActiveTab().getHelpSource();
            }

        }
        if(!helpSource) {
            helpSource = this.helpSource;
        }

        Ung.Main.openHelp(helpSource);
    },
    closeWindow: function(handler) {
        Ext.get("racks").show();
        this.hide();
        Ext.destroy(this);
        if(handler) {
            handler();
        }
    },
    isDirty: function() {
        return this.dirtyFlag || Ung.Util.isDirty(this.tabs);
    },
    markDirty: function() {
        this.dirtyFlag=true;
    },
    clearDirty: function() {
        this.dirtyFlag=false;
        Ung.Util.clearDirty(this.tabs);
    },
    applyAction: function() {
        this.saveAction(true);
    },
    saveAction: function (isApply) {
        if(!this.isDirty()) {
            if(!isApply) {
                this.closeWindow();
            }
            return;
        }
        if(!this.validate(isApply)) {
            return;
        }
        Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
        // Give the browser time to "breath" to bring up save progress bar.
        Ext.Function.defer(
            function(){
                if(Ext.isFunction(this.beforeSave)) {
                    this.beforeSave(isApply, this.save);
                } else {
                    this.save.call(this, isApply);
                }
            },
            100,
            this
        );
    },
    //To Override
    save: function(isApply) {
        Ext.MessageBox.hide();
        if (!isApply) {
            this.closeWindow();
        } else {
            this.clearDirty();
            if(Ext.isFunction(this.afterSave)) {
                this.afterSave.call(this);
            }
        }
    },
    // validation functions, to override if needed
    validate: function() {
        return true;
    }
});
// Node Settings Window
Ext.define("Ung.NodeWin", {
    extend: "Ung.SettingsWin",
    hasReports: true,
    node: null,
    constructor: function(config) {
        this.id = "nodeWin_" + config.name + "_" + rpc.currentPolicy.policyId;
        this.callParent(arguments);
    },
    initComponent: function() {
        if (this.helpSource == null) {
            this.helpSource = this.helpSource;
        }
        this.breadcrumbs = [{
            title: i18n._(rpc.currentPolicy.name),
            action: Ext.bind(function() {
                this.cancelAction(); // TODO check if we need more checking
            }, this)
        }, {
            title: this.displayName
        }];
        if(this.bbar==null) {
            this.bbar=["-",{
                name: "Remove",
                id: this.getId() + "_removeBtn",
                iconCls: 'node-remove-icon',
                text: i18n._('Remove'),
                handler: Ext.bind(function() {
                    this.removeAction();
                }, this)
            },"-",{
                name: 'Help',
                id: this.getId() + "_helpBtn",
                iconCls: 'icon-help',
                    text: i18n._('Help'),
                handler: Ext.bind(function() {
                    this.helpAction();
                }, this)
            },'->',{
                name: "Save",
                id: this.getId() + "_saveBtn",
                iconCls: 'save-icon',
                text: i18n._('OK'),
                handler: Ext.bind(function() {
                    Ext.Function.defer(this.saveAction,1, this,[false]);
                }, this)
            },"-",{
                name: "Cancel",
                id: this.getId() + "_cancelBtn",
                iconCls: 'cancel-icon',
                text: i18n._('Cancel'),
                handler: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            },"-"];
            if(this.hasApply) {
                this.bbar.push({
                    name: "Apply",
                    id: this.getId() + "_applyBtn",
                    iconCls: 'apply-icon',
                    text: i18n._('Apply'),
                    handler: Ext.bind(function() {
                        Ext.Function.defer(this.applyAction,1, this);
                    }, this)
                },"-");
            }
        }
        this.callParent(arguments);
    },
    removeAction: function() {
        var message = this.displayName + " " +
            i18n._("will be uninstalled from this policy.") + "\n" +
            i18n._("All of its settings will be lost.") + "\n" + "\n" +
            i18n._("Would you like to continue?");
        Ext.Msg.confirm(i18n._("Warning:"), message, Ext.bind(function(btn, text) {
            if (btn == 'yes') {
                var nodeCmp = Ung.Node.getCmp(this.nodeId);
                this.closeWindow();
                if(nodeCmp) {
                    nodeCmp.removeAction();
                }
            }
        }, this));
    },
    // get rpcNode object
    getRpcNode: function() {
        return this.rpcNode;
    },
    // get node settings object
    getSettings: function(handler) {
        if (handler !== undefined || this.settings === undefined) {
            if(Ext.isFunction(handler)) {
                this.getRpcNode().getSettings(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.settings = result;
                    handler.call(this);
                }, this));
            } else {
                try {
                    this.settings = this.getRpcNode().getSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
        }
        return this.settings;
    },
    save: function(isApply) {
        this.getRpcNode().setSettings( Ext.bind(function(result,exception) {
            Ext.MessageBox.hide();
            if(Ung.Util.handleException(exception)) return;
            if (!isApply) {
                this.closeWindow();
                return;
            } else {
                Ext.MessageBox.wait(i18n._("Reloading..."), i18n._("Please wait"));
                this.getSettings(function() {
                    this.clearDirty();
                    if(Ext.isFunction(this.afterSave)) {
                        this.afterSave.call(this);
                    }
                    Ext.MessageBox.hide();
                });
            }
        }, this), this.getSettings());
    },
    reload: function() {
        var nodeCmp = Ung.Node.getCmp(this.nodeId);
        this.closeWindow();
        if(nodeCmp) {
            nodeCmp.loadSettings();
        }
    }
});

// Config Window (Save/Cancel/Apply)
Ext.define("Ung.ConfigWin", {
    extend: "Ung.SettingsWin",
    // class constructor
    constructor: function(config) {
        this.id = "configWin_" + config.name;
        this.callParent(arguments);
    },
    initComponent: function() {
        if (!this.name) {
            this.name = "configWin_" + this.name;
        }
        if(this.bbar==null) {
            this.bbar=['-',{
                name: 'Help',
                id: this.getId() + "_helpBtn",
                iconCls: 'icon-help',
                text: i18n._("Help"),
                handler: Ext.bind(function() {
                    this.helpAction();
                }, this)
            },'->',{
                name: 'Save',
                id: this.getId() + "_saveBtn",
                iconCls: 'save-icon',
                text: i18n._("OK"),
                handler: Ext.bind(function() {
                    Ext.Function.defer(this.saveAction,1, this,[false]);
                }, this)
            },"-",{
                name: 'Cancel',
                id: this.getId() + "_cancelBtn",
                iconCls: 'cancel-icon',
                text: i18n._("Cancel"),
                handler: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            },"-"];
            if(this.hasApply) {
                this.bbar.push({
                    name: "Apply",
                    id: this.getId() + "_applyBtn",
                    iconCls: 'apply-icon',
                    text: i18n._("Apply"),
                    handler: Ext.bind(function() {
                        Ext.Function.defer(this.applyAction,1, this,[true]);
                    }, this)
                },"-");
            }
        }
        this.callParent(arguments);
    }
});

// Status Window (just a close button)
Ext.define("Ung.StatusWin", {
    extend: "Ung.SettingsWin",
    // class constructor
    constructor: function(config) {
        this.id = "statusWin_" + config.name;
        this.callParent(arguments);
    },
    initComponent: function() {
        if (!this.name) {
            this.name = "statusWin_" + this.name;
        }
        if(this.bbar==null) {
            this.bbar=['-',{
                name: 'Help',
                id: this.getId() + "_helpBtn",
                iconCls: 'icon-help',
                text: i18n._("Help"),
                handler: Ext.bind(function() {
                    this.helpAction();
                }, this)
            },"->",{
                name: 'Close',
                id: this.getId() + "_closeBtn",
                iconCls: 'cancel-icon',
                text: i18n._("Close"),
                handler: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            },"-"];
        }
        this.callParent(arguments);
    },
    isDirty: function() {
        return false;
    }
});

// edit window
// has the content and 2 standard buttons:  Cancel/Done
// Done just closes the window and updates the data in the browser but does not save
Ext.define('Ung.EditWindow', {
    extend: 'Ung.Window',
    initComponent: function() {
        if(this.bbar==null) {
            this.bbar=[];
            if(this.helpSource) {
                this.bbar.push('-', {
                    name: 'Help',
                    id: this.getId() + "_helpBtn",
                    iconCls: 'icon-help',
                    text: i18n._("Help"),
                    handler: Ext.bind(function() {
                        this.helpAction();
                    }, this)
                });
            }
            this.bbar.push( '->', {
                name: "Cancel",
                id: this.getId() + "_cancelBtn",
                iconCls: 'cancel-icon',
                text: i18n._('Cancel'),
                handler: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            },'-',{
                name: "Done",
                id: this.getId() + "_doneBtn",
                iconCls: 'apply-icon',
                text: i18n._('Done'),
                handler: Ext.bind(function() {
                    Ext.defer(this.updateAction,1, this);
                }, this)
            },'-');
        }
        this.callParent(arguments);
    },
    // the update actions
    // to override
    updateAction: function() {
        Ung.Util.todo();
    },
    // on click help
    helpAction: function() {
        Ung.Main.openHelp(this.helpSource);
    }
});
