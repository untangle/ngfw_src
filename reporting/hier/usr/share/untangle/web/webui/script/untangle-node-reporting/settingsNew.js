if (!Ung.hasResource["Ung.Reporting"]) {
    Ung.hasResource["Ung.Reporting"] = true;
    Ung.NodeWin.registerClassName('untangle-node-reporting', 'Ung.Reporting');

    Ext.define('Ung.Reporting', {
        extend:'Ung.NodeWin',
        panelStatus: null,
        panelGeneration: null,
        gridReportingUsers: null,
        gridIpMap: null,
        initComponent: function(container, position) {
            // builds the 3 tabs
            this.buildStatus();
            this.buildGeneration();
            this.buildIpMap();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus, this.panelGeneration, this.gridIpMap]);
            this.tabs.setActiveTab(this.panelStatus);
            Ung.Reporting.superclass.initComponent.call(this);
        },
        // Status Panel
        buildStatus: function() {
            this.panelStatus = Ext.create('Ext.panel.Panel',{
                title: this.i18n._('Status'),
                name: 'Status',
                helpSource: 'status',
               // layout: "form",
                autoScroll: true,
                cls: 'ung-panel',
                items: [{
                    title: this.i18n._('Status'),
                    xtype: 'fieldset',
                    autoHeight: true,
                    items: [{
                        html: this.i18n._('Reports are automatically generated each night.') + "<br/>",
                        cls: 'description',
                        border: false
                    }, {
                        buttonAlign: 'center',
                        footer: false,
                        border: false,
                        buttons: [{
                            xtype: 'button',
                            text: this.i18n._('View Reports'),
                            name: 'View Reports',
                            iconCls: 'action-icon',
                            handler: Ext.bind(function() {
                                var viewReportsUrl = "../reports/";
                                var breadcrumbs = [{
                                    title: i18n._(rpc.currentPolicy.name),
                                    action: Ext.bind(function() {
                                        main.iframeWin.closeActionFn();
                                        this.cancelAction();
                                    },this)
                                }, {
                                    title: this.node.nodeContext.nodeProperties.displayName,
                                    action: Ext.bind(function() {
                                        main.iframeWin.closeActionFn();
                                    },this)
                                }, {
                                    title: this.i18n._('View Reports')
                                }];
                                window.open(viewReportsUrl);
                            },this)
                        }]
                    }, {
                        html: this.i18n._('Report generation for the current day can be forced with the ') + "<b>" + this.i18n._('Generate Today\'s Reports') + "</b>" + this.i18n._(" button.") + "<br/>" +
                            "<b>" + this.i18n._("Caution") + ":  </b>" + this.i18n._("Real-time report generation may cause network slowness."),
                        cls: 'description',
                        border: false
                    }, {
                        buttonAlign: 'center',
                        footer: false,
                        border: false,
                        items: [{
                            xtype: 'button',
                            text: this.i18n._('Generate Today\'s Reports'),
                            name: 'Generate Reports',
                            iconCls: 'action-icon',
                            handler: Ext.bind(function(callback) {
                                Ext.MessageBox.wait(i18n._("Generating today's reports... This may take a few minutes."), i18n._("Please wait"));
                                this.getRpcNode().runDailyReport(Ext.bind(function(result, exception) {
                                    this.afterRun(exception, callback);
                                },this));
                            },this)
                        }]
                    }]
                }]
            });
        },
        // Generation panel
        buildGeneration: function() {
            var fieldID = "" + Math.round( Math.random() * 1000000 );

            // Change the password for a user.
            var changePasswordColumn = Ext.create('Ung.grid.EditColumn',{
                header : this.i18n._("change password"),
                width : 130,
                fixed : true,
                iconClass : 'icon-edit-row',
                handler : function(view,rowIndex,colIndex)
                {
                    var record = view.getStore().getAt(rowIndex);
                    this.grid.rowEditorChangePassword.populate(record);
                    this.grid.rowEditorChangePassword.show();
                }
            });

            var generationTime=new Date();
            generationTime.setTime(0);
            generationTime.setHours(this.getSettings().generationHour);
            generationTime.setMinutes(this.getSettings().generationMinute);

            this.panelGeneration = Ext.create('Ext.panel.Panel',{
                // private fields
                name: 'Generation',
                helpSource: 'generation',
                parentId: this.getId(),
                title: this.i18n._('Generation'),
                layout: "anchor",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    anchor: "98%",
                    xtype: 'fieldset',
                    autoHeight: true
                },
                items: [{
                    title: this.i18n._('Email'),
                    layout:'column',
                    height: 350,
                    items: [ this.gridReportingUsers = Ext.create('Ung.EditorGrid',{
                        width : 710,
                        name: 'ReportingUsers',
                        title: this.i18n._("Reporting Users"),
                        hasEdit: false,
                        settingsCmp: this,
                        paginated: false,
                        height: 300,
                        emptyRow: {
                            emailAddress : "reportrecipient@example.com",
                            emailReports : true,
                            onlineReports : true,
                            clearPassword : null,
                            user : null
                        },
                        data: this.getSettings().reportingUsers.list,
                        dataRoot: null,
                        autoGenerateId: true,
                        plugins:[changePasswordColumn],
                        fields: [{
                            name: "emailAddress"
                        },{
                            name: "emailReports"
                        },{
                            name: "onlineReports"
                        },{
                            name: "clearPassword"
                        },{
                            name: "user"
                        }],
                        sortField: "emailAddress",
                        columnsDefaultSortable: true,
                        columns: [{
                            header: this.i18n._("Email Address (username)"),
                            dataIndex: "emailAddress",
                            width : 200,
                            editor: {
                                xtype:'textfield',
                                vtype: "email",
                                allowBlank: false,
                                blankText: this.i18n._("The email address cannot be blank.")
                            },
                            flex:1
                        },
                        {
                            xtype:'checkcolumn',
                            header : this.i18n._("Email Reports"),
                            dataIndex : "emailReports",
                            width : 100,
                            fixed : true
                        },
                        { 
                            xtype:'checkcolumn',
                            header : this.i18n._("Online Reports"),
                            dataIndex : "onlineReports",
                            width : 100,
                            fixed : true
                        },                      
                            changePasswordColumn
                        ],
                        rowEditorInputLines : [
                        {
                            xtype:'textfield',
                            dataIndex : "emailAddress",
                            fieldLabel : this.i18n._("Email Address (username)"),
                            allowBlank : false,
                            blankText : this.i18n._("The email address name cannot be blank."),
                            width : 300
                        },
                        {
                            xtype:'checkbox',
                            dataIndex : "emailReports",
                            fieldLabel : this.i18n._("Email Reports"),
                            width : 300
                        },
                         {
                            xtype:'checkbox',
                            dataIndex : "onlineReports",
                            id : "add_reporting_online_reports_" + fieldID,
                            fieldLabel : this.i18n._("Online Reports"),
                            width : 300
                        },
                        {
                            xtype:'textfield',
                            inputType: "password",
                            name : "Password",
                            dataIndex : "clearPassword",
                            id : "add_reporting_user_password_" + fieldID,
                            msgTarget : "title",
                            fieldLabel : this.i18n._("Password"),
                            boxLabel : this.i18n._("(required for 'Online Reports')"),
                            width : 300,
                            minLength : 3,
                            minLengthText : Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                        },
                        {
                            xtype:'textfield',
                            inputType: "password",
                            name : "Confirm Password",
                            dataIndex : "clearPassword",
                            id : "add_reporting_confirm_password_" + fieldID,
                            fieldLabel : this.i18n._("Confirm Password"),
                            width : 300
                        }],
                        rowEditorValidate: Ext.bind(function (inputLines) {
                        	//validate password match
                        	var pwd = Ext.getCmp("add_reporting_user_password_" + fieldID);
                        	var confirmPwd = Ext.getCmp("add_reporting_confirm_password_" + fieldID);
                        	if(pwd.getValue() != confirmPwd.getValue()) {
                        		pwd.markInvalid();
                        		return this.i18n._('Passwords do not match');
                        	}
                        	// validate password not empty if onlineReports checked
                        	var onlineReports=Ext.getCmp("add_reporting_online_reports_" + fieldID);
                        	if(onlineReports.getValue() &&  pwd.getValue().length==0) {
                        		return this.i18n._("A password must be set to enable Online Reports!");
                        	} else {
                        		return true;
                        	}
                        },this)
                    })]
                },{
                    title : this.i18n._("Email Attachment Settings"),
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Attach Detailed Report Logs to Email (CSV Zip File)'),
                        name : 'Email Detail',
                        hideLabel : true,
                        checked : this.getSettings().emailDetail,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSettings().emailDetail = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Attachment size limit (MB)'),
                        name : 'Attachement size limit',
                        id: 'reporting_attachment_size_limit',
                        value : this.getSettings().attachmentSizeLimit,
                        labelWidth:150,
                        labelAlign:'right',
                        width: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 30,
                        hideTrigger:true,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSettings().attachmentSizeLimit = newValue;
                                },this)
                            }
                        }
                    }
                            ]
                },{
                    title : this.i18n._("Daily Reports"),
                    items : [{
                        border : false,
                        cls: 'description',
                        html : this.i18n._('Daily Reports covers the previous day. Daily reports will be generated on the selected days.')
                    },  {
                        xtype : 'textfield',
                        id : 'reporting_daily',
                        fieldLabel : this.i18n._('Generate Daily Reports'),
                        value: this.getSettings().generateDailyReports
                    }]
                },{
                    title : this.i18n._("Weekly Reports"),
                    items : [{
                        border : false,
                        cls: 'description',
                        html : this.i18n._('Daily Reports covers the previous week. Weekly reports will be generated on the selected days.')
                    },  {
                        xtype : 'textfield',
                        id : 'reporting_weekly',
                        fieldLabel : this.i18n._('Generate Weekly Reports'),
                        value: this.getSettings().generateWeeklyReports
                    }]
                },{
                    title : this.i18n._("Monthly Reports"),
                    items : [{
                        border : false,
                        cls: 'description',
                        html : this.i18n._('Daily Reports covers the previous month. Monthly reports will be generated on the selected days.')
                    },  {
                        xtype : 'textfield',
                        id : 'reporting_monthly',
                        fieldLabel : this.i18n._('Generate Monthly Reports'),
                        value: this.getSettings().generateMonthlyReports
                    }]
                },{
                    title: this.i18n._("Data Retention"),
                    labelWidth: 150,
                    items: [{
                        border: false,
                        cls: 'description',
                        html: this.i18n._("Limit Data Retention to a number of days. The smaller the number the lower the disk space requirements and resource usage during report generation.")
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Limit Data Retention'),
                        name : 'Limit Data Retention',
                        id: 'reporting_daysToKeepDB',
                        value : this.getSettings().dbRetention,
                        labelWidth:150,
                        labelAlign:'right',
                        width: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 65,
                        hideTrigger:true,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSettings().dbRetention = newValue;
                                },this)
                            }
                        }
                    }]
                },  {
                    title: this.i18n._("Reports Retention"),
                    labelWidth: 150,
                    items: [{
                        border: false,
                        cls: 'description',
                        html: this.i18n._("Keep old reports on the server for this number of days.")
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Reports Retention days'),
                        name : 'Reports Retention days',
                        id: 'reporting_daysToKeepFiles',
                        value : this.getSettings().fileRetention,
                        labelWidth:150,
                        labelAlign:'right',
                        width: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 90,
                        hideTrigger:true,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSettings().fileRetention = newValue;
                                },this)
                            }
                        }
                    }]
                }, {
                    title: this.i18n._("Generation Time"),
                    labelWidth: 150,
                    items: [{
                        border: false,
                        cls: 'description',
                        html: this.i18n._("Scheduled time to generate the reports.")
                    }, {
                        xtype : 'timefield',
                        fieldLabel : this.i18n._('Generation Time'),
                        name : 'Generation Time',
                        width : 90,
                        hideLabel : true,
                        // format : "H:i",
                        value : generationTime,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    // newValue;
                                    if (newValue != null) {
                                        this.getSettings().generationMinute = newValue.getMinutes();
                                        this.getSettings().generationHour   = newValue.getHours();
                                    }
                                },this)
                            }
                        }
                    }]
                }]
            });

            /* Create the row editor for updating the password */
            this.gridReportingUsers.rowEditorChangePassword = Ext.create('Ung.RowEditorWindow',{
                grid : this.gridReportingUsers,
                inputLines : [
                {
                    xtype:'textfield',
                    inputType: "password",
                    name : "Password",
                    dataIndex : "clearPassword",
                    id : "edit_reporting_user_password_"  + fieldID,
                    fieldLabel : this.i18n._("Password"),
                    width : 300,
                    minLength : 3,
                    minLengthText : Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                }, 
                {
                    xtype:'textfield',
                    inputType: "password",
                    name : "Confirm Password",
                    dataIndex : "clearPassword",
                    id : "edit_reporting_confirm_password_"  + fieldID,
                    fieldLabel : this.i18n._("Confirm Password"),
                    width : 300
                }],
                validate: Ext.bind(function(inputLines) {
                	//validate password match
                	var pwd = Ext.getCmp("edit_reporting_user_password_" + fieldID);
                	var confirmPwd = Ext.getCmp("edit_reporting_confirm_password_" + fieldID);
                	if(pwd.getValue() != confirmPwd.getValue()) {
                		pwd.markInvalid();
                		return this.i18n._('Passwords do not match');
                	}
                	// validate password not empty if onlineReports checked
                	var onlineReports=Ext.getCmp("add_reporting_online_reports_" + fieldID);
                	if(onlineReports.getValue() &&  pwd.getValue().length==0) {
                		return this.i18n._("A password must be set to enable Online Reports!");
                	} else {
                		return true;
                	}
                	
                },this)
            });

           // this.gridReportingUsers.rowEditorChangePassword.render("containter");
        },
        // IP Map grid
        buildIpMap: function() {
            this.gridIpMap = Ext.create('Ung.EditorGrid',{
                settingsCmp: this,
                name: 'Name Map',
                helpSource: 'ip_addresses',
                title: this.i18n._("Name Map"),
                emptyRow: {
                    "ipMaskedAddress": "1.2.3.4",
                    "name": this.i18n._("[no name]")
                },
                // the column is autoexpanded if the grid width permits
                recordJavaClass: "com.untangle.uvm.node.IPMaskedAddressRule",

                data: this.getSettings().hostnameMap.map,
                // the list of fields
                fields: [{
                    name: 'id'
                }, {
                    name: 'ipMaskedAddress'
                }, {
                    name: 'name'
                }],
                // the list of columns for the column model
                columns: [{
                    header: this.i18n._("Name Map"),
                    width: 200,
                    dataIndex: 'ipMaskedAddress',
                    editor: {
                        xtype:'textfield'
                    }
                }, {
                    header: this.i18n._("name"),
                    width: 200,
                    dataIndex: 'name',
                    flex:1,
                    editor: {
                        xtype:'textfield'
                    }
                }],
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [
                  {
                    xtype:'textfield',
                    name: "Subnet",
                    dataIndex: "ipMaskedAddress",
                    fieldLabel: this.i18n._("Name Map"),
                    allowBlank: false,
                    width: 300
                }, 
                {
                    xtype:'textfield',
                    name: "Name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Name"),
                    allowBlank: false,
                    width: 300
                }]
            });
        },
        applyAction : function()
        {
            this.commitSettings(Ext.bind(this.reloadSettings,this));
        },
        reloadSettings : function()
        {
            this.getSettings(true);

            this.gridReportingUsers.clearChangedData();
            this.gridReportingUsers.store.loadData( this.getSettings().reportingUsers.list );
            var cmpIds = this.getEditableFields();
            for (var i = 0; i < cmpIds.length; i++) {
                if (cmpIds[i].isDirty()) {
                    cmpIds[i].clearDirty();
                }
            }

            this.gridIpMap.clearChangedData();
            this.gridIpMap.store.loadData( this.getSettings().hostnameMap.map );

            Ext.MessageBox.hide();
        },
        saveAction : function()
        {
            this.commitSettings(Ext.bind(this.completeSaveAction,this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
        },
        // save function
        commitSettings : function(callback)
        {
            if (this.validate()) {
                this.saveSemaphore = 3;
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));

                // FIXME daily
                this.getSettings().generateDailyReports = "any";
                // FIXME weekly
                this.getSettings().generateWeeklyReports = "sunday";
                // FIXME monthly
                this.getSettings().generateMonthlyReports = "sunday";

                // set Ip Map list
                this.getSettings().hostnameMap.map = this.gridIpMap.getFullSaveList();

                // FIXME save email recipients
                this.getSettings().reportingUsers = this.gridReportingUsers.getFullSaveList();

                this.getRpcNode().setSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, callback);
                },this), this.getSettings());
            }
        },
        afterRun: function(exception, callback)
        {
            if(Ung.Util.handleException(exception)) {
                return;
            }
            Ext.MessageBox.hide();
        },
        afterSave: function(exception, callback)
        {
            if(Ung.Util.handleException(exception)) {
                return;
            }

            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                callback();
            }
        },
        getEditableFields : function(){
            return this.panelGeneration.query('checkbox radiogroup numberfield textfield');        
        },
        isDirty: function() {
            if(this.panelGeneration.rendered) {
                var cmpIds = this.getEditableFields(), i;
                for (i = 0; i < cmpIds.length; i++) {
                    if (cmpIds[i].isDirty()){
                        return true;
                    }
                }
                if (this.gridReportingUsers.isDirty()){
                    return true;
                }
            }
            return this.gridIpMap.isDirty();
        }
    });
}
//@ sourceURL=reporting-settingsNew.js
