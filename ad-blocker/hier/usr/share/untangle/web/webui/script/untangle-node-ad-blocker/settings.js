Ext.define('Webui.untangle-node-ad-blocker.settings', {
    extend:'Ung.NodeWin',
    totalFiltersAvailable: null,
    totalFiltersEnabled:  null,
    totalCookiesAvailable: null,
    totalCookiesEnabled:  null,
    gridEventLog: null,
    initComponent: function() {
        this.lastUpdate = this.getRpcNode().getListLastUpdate();
        this.genericRuleFields = Ung.Util.getGenericRuleFields(this);
        this.buildStatus();
        this.buildAdFilters();
        this.buildCookieFilters();
        this.buildPassLists();

        this.buildTabPanel([this.panelStatus, this.panelFilters, this.panelCookies, this.panelPassLists]);
        this.callParent(arguments);
    },
    buildFiltersLength: function() {
        var list=this.settings.rules.list;
        var userList=this.settings.userRules.list;
        this.totalFiltersAvailable = list.length + userList.length;
        this.totalFiltersEnabled = 0;
        for(var i=0; i< list.length; i++) {
            if(list[i].enabled) {
                this.totalFiltersEnabled++;
            }
        }
        for(i=0; i< userList.length; i++) {
            if(userList[i].enabled) {
                this.totalFiltersEnabled++;
            }
        }
    },
    buildCookiesLength: function() {
        var list=this.settings.cookies.list;
        var userList=this.settings.userCookies.list;
        this.totalCookiesAvailable = list.length + userList.length;
        this.totalCookiesEnabled = 0;
        for(var i=0; i< list.length; i++) {
            if(list[i].enabled) {
                this.totalCookiesEnabled++;
            }
        }
        for(i=0; i< userList.length; i++) {
            if(userList[i].enabled) {
                this.totalCookiesEnabled++;
            }
        }
    },
    // Status Panel
    buildStatus: function() {
        this.buildFiltersLength();
        this.buildCookiesLength();
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            name: 'Status',
            helpSource: 'ad_blocker_status',
            title: this.i18n._('Status'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: this.i18n._('Statistics'),
                labelWidth: 230,
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 200
                },
                items: [{
                    fieldLabel: this.i18n._('Total Filters Available'),
                    name: "total_filters_available",
                    value: this.totalFiltersAvailable
                }, {
                    fieldLabel: this.i18n._('Total Filters Enabled'),
                    name: "total_filters_enabled",
                    value: this.totalFiltersEnabled
                }, {
                    fieldLabel: this.i18n._('Total Cookie Rules Available'),
                    name: "total_cookies_available",
                    value: this.totalCookiesAvailable
                }, {
                    fieldLabel: this.i18n._('Total Cookie Rules Enabled'),
                    name: "total_cookies_enabled",
                    value: this.totalCookiesEnabled
                }]
            }, {
                title: this.i18n._('Status'),
                items: [{
                    xtype: 'checkbox',
                    boxLabel: this.i18n._('Block Ads'),
                    hideLabel: true,
                    name: 'Block Ads',
                    checked: this.settings.scanAds,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.scanAds = checked;
                    }, this)
                }, {
                    xtype: 'checkbox',
                    boxLabel: this.i18n._('Block Tracking & Ad Cookies'),
                    hideLabel: true,
                    name: 'Block Tracking & Ad Cookies',
                    checked: this.settings.scanCookies,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.scanCookies = checked;
                    }, this)
                }]
            }, {
                title: this.i18n._('Update filters'),
                items: [{
                    xtype: 'button',
                    text: this.i18n._("Update"),
                    handler: Ext.bind(function() {
                        Ext.MessageBox.wait(i18n._("Updating filters..."), i18n._("Please wait"));
                        this.getRpcNode().updateList(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)){
                                Ext.MessageBox.hide();
                                return;
                            }
                            this.getRpcNode().getSettings(Ext.bind(function(result,exception) {
                                if(Ung.Util.handleException(exception)){
                                    Ext.MessageBox.hide();
                                    return;
                                }
                                this.settings.rules = result.rules;
                                this.settings.lastUpdate = result.lastUpdate;
                                this.buildFiltersLength();
                                this.buildCookiesLength();
                                this.lastUpdate = this.settings.lastUpdate;
                                this.down("field[name='total_filters_available']").setValue(this.totalFiltersAvailable);
                                this.down("field[name='total_filters_enabled']").setValue(this.totalFiltersEnabled);
                                this.down("field[name='total_cookies_available']").setValue(this.totalCookiesAvailable);
                                this.down("field[name='total_cookies_enabled']").setValue(this.totalCookiesEnabled);
                                this.down("component[name='last_update_timestamp']").update(this.formatLastUpdateText());

                                this.gridAdFiltersStandard.reload();
                                Ext.MessageBox.hide();
                            }, this));
                        }, this));
                    }, this)
                }]
            }, {
                title: this.i18n._('Note'),
                name: "last_update_timestamp",
                html: this.formatLastUpdateText()
            }]
        });
    },
    formatLastUpdateText: function() {
        return Ext.String.format(this.i18n._("The current filter list was last modified on {1}. You are free to disable filters and add new ones, however it is not required."),
            rpc.companyName, (this.lastUpdate !== null ? i18n._(this.lastUpdate): i18n._("unknown")));
    },
    // Add Blocker Filters Panel
    buildAdFilters: function() {
        this.gridAdFiltersStandard = this.buildAdFiltersSubtabs('AdFiltersStandard', this.i18n._('Standard Filters'), 'rules', false);
        this.gridAdFiltersUser = this.buildAdFiltersSubtabs('AdFiltersUser', this.i18n._('User Defined Filters'), 'userRules', true);

        this.panelFilters = Ext.create('Ext.tab.Panel',{
            activeTab: 0,
            title: this.i18n._('Ad Filters'),
            helpSource: 'ad_blocker_ad_filters',
            deferredRender: false,
            items: [ this.gridAdFiltersStandard, this.gridAdFiltersUser]
        });
    },
    // Cookie Filters Panel
    buildCookieFilters: function() {
        this.gridCookiesStandard = this.buildCookieSubtabs('CookiesStandard', this.i18n._('Standard Cookie Filters'), 'cookies', false);
        this.gridCookiesUser = this.buildCookieSubtabs('CookiesUser', this.i18n._('User Defined Cookie Filters'), 'userCookies', true);

        this.panelCookies = Ext.create('Ext.tab.Panel',{
            activeTab: 0,
            title: this.i18n._('Cookie Filters'),
            helpSource: 'ad_blocker_cookie_filters',
            deferredRender: false,
            items: [ this.gridCookiesStandard, this.gridCookiesUser]
        });
    },

    buildAdFiltersSubtabs: function(name, nameStr, property, isEditable) {
        var grid = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            name: name,
            hasEdit: isEditable,
            hasDelete: isEditable,
            hasAdd: isEditable,
            hasInlineEditor:  isEditable,
            title: this.i18n._(nameStr),
            dataProperty: property,
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true,
                "blocked": true
            },
            sortField: 'string',
            fields: [{
                name: 'string',
                type: 'string'
            }, {
                name: 'enabled'
            }, {
                name: 'blocked'
            }, {
                name: 'flagged'
            }],
            columns: [{
                xtype:'checkcolumn',
                header: "<b>" + this.i18n._("Enable") + "</b>",
                dataIndex: 'enabled',
                resizable: false,
                width: 55
            }, {
                header: this.i18n._("Rule"),
                width: 200,
                dataIndex: 'string',
                flex:1,
                editable: isEditable,
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter rule]"),
                    allowBlank: false
                }
            }, {
                header: this.i18n._("Action"),
                width: 100,
                dataIndex: 'blocked',
                editable: isEditable,
                resizable: false,
                renderer: function(value) {
                    return (value === true) ? i18n._('Block') : i18n._('Pass');
                }
            }, {
                header: this.i18n._("Slow"),
                width: 55,
                editable: false,
                dataIndex: 'flagged',
                resizable: false,
                renderer: function(value) {
                    return (value === true) ? i18n._('Yes') : '';
                }
            }],
            rowEditorInputLines: [{
                xtype:'checkbox',
                dataIndex: "enabled",
                fieldLabel: this.i18n._("Enable")
            }, {
                xtype:'textfield',
                dataIndex: "string",
                fieldLabel: this.i18n._("Rule"),
                emptyText: this.i18n._("[enter rule]"),
                allowBlank: false,
                width: 400
            }, {
                xtype: "combo",
                dataIndex: "blocked",
                fieldLabel: this.i18n._("Action"),
                editable: false,
                store: [[true, this.i18n._('Block')], [false, this.i18n._('Pass')]],
                width: 200,
                queryMode: 'local'
            }]
        });
        return grid;
    },

    buildCookieSubtabs: function(name, nameStr, property, isEditable) {
        var grid = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            name: name,
            hasEdit: isEditable,
            hasDelete: isEditable,
            hasAdd: isEditable,
            hasInlineEditor:  isEditable,
            title: this.i18n._(nameStr),
            dataProperty: property,
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true
            },
            sortField: 'string',
            fields: [{
                name: 'string',
                type: 'string'
            }, {
                name: 'enabled'
            }],
            columns: [{
                xtype:'checkcolumn',
                header: "<b>" + this.i18n._("Enable") + "</b>",
                dataIndex: 'enabled',
                resizable: false,
                width: 55
            }, {
                header: this.i18n._("Rule"),
                width: 200,
                dataIndex: 'string',
                flex:1,
                editable: isEditable,
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter rule]"),
                    allowBlank: false
                }
            }],
            rowEditorInputLines: [{
                xtype:'checkbox',
                dataIndex: "enabled",
                fieldLabel: this.i18n._("Enable")
            }, {
                xtype:'textfield',
                dataIndex: "string",
                fieldLabel: this.i18n._("Rule"),
                emptyText: this.i18n._("[enter rule]"),
                allowBlank: false,
                width: 400
            }]
        });
        return grid;
    },
    // Pass Lists Panel
    buildPassLists: function() {
        this.buildPassedUrls();
        this.buildPassedClients();
        
        this.panelPassLists = Ext.create('Ext.tab.Panel',{
            activeTab: 0,
            title: this.i18n._('Pass Lists'),
            name: 'Pass Lists',
            helpSource: 'ad_blocker_pass_lists',
            deferredRender: false,
            items: [ this.gridPassedUrls, this.gridPassedClients]
        });
    },
    // Passed Sites
    buildPassedUrls: function() {
        var urlValidator = Ext.bind(function(fieldValue) {
            if (fieldValue.indexOf("https://") === 0) {
                return this.i18n._("URL specified cannot be passed because it uses secure http (https)");
            }
            if (fieldValue.indexOf("http://") === 0) {
                fieldValue = fieldValue.substr(7);
            }
            if (fieldValue.indexOf("www.") === 0) {
                fieldValue = fieldValue.substr(4);
            }
            if (fieldValue.indexOf("/") === (fieldValue.length - 1)) {
                fieldValue = fieldValue.substring(0, fieldValue.length - 1);
            }
            if (fieldValue.trim().length === 0) {
                return this.i18n._("Invalid URL specified");
            }
            return true;
        }, this);


        this.gridPassedUrls = Ext.create('Ung.grid.Panel',{
            name: 'Passed Sites',
            settingsCmp: this,
            title: this.i18n._("Passed Sites"),
            dataProperty: "passedUrls",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("Site"),
                width: 200,
                dataIndex: 'string',
                editor:{
                    xtype:'textfield',
                    allowBlank: false,
                    emptyText: this.i18n._("[enter site]"),
                    validator: urlValidator,
                    blankText: this.i18n._("Invalid URL specified")
                }
            }, {
                xtype:'checkcolumn',
                header: this.i18n._("Pass"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: this.i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex:1,
                editor:{
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype: 'textfield',
                name: "Site",
                dataIndex: "string",
                fieldLabel: this.i18n._("Site"),
                emptyText: this.i18n._("[enter site]"),
                allowBlank: false,
                width: 400,
                validator: urlValidator
            }, {
                xtype: 'checkbox',
                name: "Pass",
                dataIndex: "enabled",
                fieldLabel: this.i18n._("Pass")
            }, {
                xtype: 'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
    },
    // Passed IP Addresses
    buildPassedClients: function() {
        this.gridPassedClients = Ext.create('Ung.grid.Panel',{
            name: 'Passed Client IP addresses',
            settingsCmp: this,
            title: this.i18n._("Passed Client IP addresses"),
            dataProperty: "passedClients",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "1.2.3.4",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("IP Address/Range"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter ip]"),
                    allowBlank: false
                }
            }, {
                xtype:'checkcolumn',
                header: this.i18n._("Pass"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: this.i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex:1,
                editor: {
                    xtype:'textfield'
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                dataIndex: "string",
                fieldLabel: this.i18n._("IP address/range"),
                emptyText: this.i18n._("[enter ip]"),
                allowBlank: false,
                width: 400
            }, {
                xtype:'checkbox',
                dataIndex: "enabled",
                fieldLabel: this.i18n._("Pass")
            }, {
                xtype:'textarea',
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
    },
    // private method
    alterUrls: function(saveList) {
        if (saveList !== null) {
            var list = saveList.list;
            for (var i = 0; i < list.length; i++) {
                list[i].string = this.alterUrl(list[i].string);
            }
        }
    },
    // private method
    alterUrl: function(value) {
        if (value.indexOf("http://") === 0) {
            value = value.substr(7);
        }
        if (value.indexOf("www.") === 0) {
            value = value.substr(4);
        }
        if (value.indexOf("/") === (value.length - 1)) {
            value = value.substring(0, value.length - 1);
        }
        return value.trim();
    },
    // save function
    beforeSave: function(isApply, handler) {
        this.getSettings().rules.list = this.gridAdFiltersStandard.getList();
        this.getSettings().userRules.list = this.gridAdFiltersUser.getList();
        this.getSettings().cookies.list = this.gridCookiesStandard.getList();
        this.getSettings().userCookies.list = this.gridCookiesUser.getList();
        this.getSettings().passedClients.list = this.gridPassedClients.getList();
        this.getSettings().passedUrls.list = this.gridPassedUrls.getList();
        this.alterUrls(this.getSettings().passedUrls);
        handler.call(this, isApply);
    },
    afterSave: function() {
        this.buildFiltersLength();
        this.buildCookiesLength();
        this.down("field[name='total_filters_available']").setValue(this.totalFiltersAvailable);
        this.down("field[name='total_filters_enabled']").setValue(this.totalFiltersEnabled);
        this.down("field[name='total_cookies_available']").setValue(this.totalCookiesAvailable);
        this.down("field[name='total_cookies_enabled']").setValue(this.totalCookiesEnabled);
    }
});
//# sourceURL=ad-blocker-settings.js