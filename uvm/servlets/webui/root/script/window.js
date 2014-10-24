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
    layout: 'anchor',
    defaults: {
        anchor: '100% 100%',
        autoScroll: true,
        autoWidth: true
    },
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
        Ext.QuickTips.init();
    },

    beforeDestroy: function() {
        Ext.each(this.subCmps, Ext.destroy);
        this.callParent(arguments);
    },
    // on show position and size
    onShow: function() {
        if (this.sizeToRack) {
            this.setSizeToRack();
        }
        this.callParent(arguments);
    },
    setSizeToRack: function () {
        var objSize = main.viewport.getSize();
        objSize.width = objSize.width - main.contentLeftWidth;
        this.setPosition(main.contentLeftWidth, 0);
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
        var objSize = main.viewport.getSize();
        Ext.applyIf(config, {
            height: objSize.height,
            width: objSize.width - main.contentLeftWidth,
            x: main.contentLeftWidth,
            y: 0
        });
        this.callParent(arguments);
    },
    buildTabPanel: function(itemsArray) {
        Ext.get("racks").hide();
        this.tabs = Ext.create('Ext.tab.Panel',{
            activeTab: 0,
            deferredRender: false,
            parentId: this.getId(),
            items: itemsArray
        });
        this.items=this.tabs;
        this.tabs.on('afterrender', function() {
            Ext.get("racks").show();
            Ext.defer(this.openTarget,1, this);
        }, this);
    },
    openTarget: function() {
        if(main.target) {
            var targetTokens = main.target.split(".");
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
            main.target = null;
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

        } else {
            helpSource = this.helpSource;
        }

        main.openHelp(helpSource);
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
        if(Ext.isFunction(this.beforeSave)) {
            this.beforeSave(isApply, this.save);
        } else {
            this.save.call(this, isApply);
        }
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
    node: null,
    constructor: function(config) {
        var nodeName=config.node.name;
        this.id = "nodeWin_" + nodeName + "_" + rpc.currentPolicy.policyId;
        // initializes the node i18n instance
        config.i18n = Ung.i18nModuleInstances[nodeName];
        this.callParent(arguments);
    },
    initComponent: function() {
        if (this.helpSource == null) {
            this.helpSource = this.node.helpSource;
        }
        this.breadcrumbs = [{
            title: i18n._(rpc.currentPolicy.name),
            action: Ext.bind(function() {
                this.cancelAction(); // TODO check if we need more checking
            }, this)
        }, {
            title: this.node.displayName
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
        this.node.removeAction();
    },
    // get rpcNode object
    getRpcNode: function() {
        return this.node.rpcNode;
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
    // get Validator object
    getValidator: function() {
        if (this.node.rpcNode.validator === undefined) {
            try {
                this.node.rpcNode.validator = this.getRpcNode().getValidator();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.node.rpcNode.validator;
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
        var nodeWidget=this.node;
        this.closeWindow();
        nodeWidget.onSettingsAction();
    }
});
Ung.NodeWin._nodeScripts = {};

// Dynamically loads javascript file for a node
Ung.NodeWin.loadNodeScript = function(settingsCmp, handler) {
    var scriptFile = Ung.Util.getScriptSrc('settings.js');
    Ung.Util.loadScript('script/' + settingsCmp.name + '/' + scriptFile, Ext.bind(function() {
        this.settingsClassName = Ung.NodeWin.getClassName(this.name);
        handler.call(this);
    },settingsCmp));
};

Ung.NodeWin.classNames = {};
// Static function get the settings class name for a node
Ung.NodeWin.getClassName = function(name) {
    var className = Ung.NodeWin.classNames[name];
    return className === undefined ? null: className;
    };
// Static function to register a settings class name for a node
Ung.NodeWin.registerClassName = function(name, className) {
    Ung.NodeWin.classNames[name] = className;
};
Ung.NodeWin.register = function(nodeName) {
    if (!Ung.hasResource['Webui.'+nodeName+'.settings']) {
        Ung.hasResource['Webui.'+nodeName+'.settings'] = true;
        Ung.NodeWin.registerClassName(nodeName, 'Webui.'+nodeName+'.settings');
    }
}

// Config Window (Save/Cancel/Apply)
Ext.define("Ung.ConfigWin", {
    extend: "Ung.SettingsWin",
    // class constructor
    constructor: function(config) {
        this.id = "configWin_" + config.name;
        // for config elements we have the untangle-libuvm translation map
        this.i18n = i18n;
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
        // for config elements we have the untangle-libuvm translation map
        this.i18n = i18n;
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

// update window
// has the content and 3 standard buttons: Save, Cancel, Apply
Ext.define('Ung.UpdateWindow', {
    extend: 'Ung.Window',
    initComponent: function() {
        if(this.bbar==null) {
            this.bbar=[
                '->',
                {
                    name: "Save",
                    id: this.getId() + "_saveBtn",
                    iconCls: 'save-icon',
                    text: i18n._('Save'),
                    handler: Ext.bind(function() {
                        Ext.Function.defer(this.saveAction,1, this);
                    }, this)
                },'-',{
                    name: "Cancel",
                    id: this.getId() + "_cancelBtn",
                    iconCls: 'cancel-icon',
                    text: i18n._('Cancel'),
                    handler: Ext.bind(function() {
                        this.cancelAction();
                    }, this)
                },'-',{
                    name: "Apply",
                    id: this.getId() + "_applyBtn",
                    iconCls: 'apply-icon',
                    text: i18n._('Apply'),
                    handler: Ext.bind(function() {
                        Ext.Function.defer(this.applyAction,1, this, []);
                    }, this)
                },'-'];
        }
        this.callParent(arguments);
    },
    // the update actions
    // to override
    updateAction: function() {
        Ung.Util.todo();
    },
    saveAction: function() {
        Ung.Util.todo();
    },
    applyAction: function() {
        Ung.Util.todo();
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
            this.bbar.push(
                '->',
                {
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
        main.openHelp(this.helpSource);
    }
});

// Manage list popup window
Ext.define("Ung.ManageListWindow", {
    extend: "Ung.UpdateWindow",
    // the editor grid
    grid: null,
    layout: 'fit',
    initComponent: function() {
        this.items=this.grid;
        this.callParent(arguments);
    },
    closeWindow: function(skipLoad) {
        if(!skipLoad) {
            this.grid.reload();
        }
        this.hide();
    },
    isDirty: function() {
        return this.grid.isDirty();
    },
    updateAction: function() {
        this.hide();
    },
    saveAction: function() {
        this.applyAction(Ext.bind(this.hide, this));
    }
});
