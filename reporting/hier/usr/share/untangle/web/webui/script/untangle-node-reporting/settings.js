if (!Ung.hasResource["Ung.Reporting"]) {
    Ung.hasResource["Ung.Reporting"] = true;
    Ung.NodeWin.registerClassName('untangle-node-reporting', 'Ung.Reporting');

    Ung.Reporting = Ext.extend(Ung.NodeWin, {
        panelStatus: null,
        panelGeneration: null,
        gridRecipients: null,
        gridIpMap: null,
        initComponent: function(container, position) {
            // builds the 3 tabs
            this.buildStatus();
            this.buildGeneration();
            this.buildIpMap();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus, this.panelGeneration, this.gridIpMap]);
            this.tabs.activate(this.panelStatus);
            Ung.Reporting.superclass.initComponent.call(this);
        },
        getReportingSettings: function(forceReload) {
            if (forceReload || this.rpc.reportingSettings === undefined) {
                try {
                    this.rpc.reportingSettings = this.getRpcNode().getReportingSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.reportingSettings;
        },
        // get mail settings
        getMailSettings: function(forceReload) {
            if (forceReload || this.rpc.mailSettings === undefined) {
                try {
                    this.rpc.mailSettings = rpc.adminManager.getMailSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.mailSettings;
        },
        getAdminSettings : function(forceReload) {
            if (forceReload || this.rpc.adminSettings === undefined) {
                try {
                    this.rpc.adminSettings = rpc.adminManager.getAdminSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.adminSettings;
        },
        // Status Panel
        buildStatus: function() {
            this.panelStatus = new Ext.Panel({
                title: this.i18n._('Status'),
                name: 'Status',
                helpSource: 'status',
                layout: "form",
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
                            handler: function() {
                                var viewReportsUrl = "../reports/";
                                var breadcrumbs = [{
                                    title: i18n._(rpc.currentPolicy.name),
                                    action: function() {
                                        main.iframeWin.closeActionFn();
                                        this.cancelAction();
                                    }.createDelegate(this)
                                }, {
                                    title: this.node.md.displayName,
                                    action: function() {
                                        main.iframeWin.closeActionFn();
                                    }.createDelegate(this)
                                }, {
                                    title: this.i18n._('View Reports')
                                }];
                                window.open(viewReportsUrl);
                            }.createDelegate(this)
                        }]
                    }, {
                        html: this.i18n._('Report generation for the current day can be forced with the ') + "<b>" + this.i18n._('Generate Today\'s Reports') + "</b>" + this.i18n._(" button.") + "<br/>" +
                            "<b>" + this.i18n._("Caution") + ":  </b>" + this.i18n._("Real-time report generation may cause network slowness."),
                        cls: 'description',
                        border: false
                    }, {
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('I understand the risks.'),
                        name : 'Understand Checkbox',
                        id : 'risks',
                        hideLabel : true,
                        checked : false,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().emailDetail = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        buttonAlign: 'center',
                        footer: false,
                        border: false,
                        buttons: [{
                            xtype: 'button',
                            text: this.i18n._('Generate Today\'s Reports'),
                            name: 'Generate Reports',
                            iconCls: 'action-icon',
                            handler: function(callback) {
                                var understand = Ext.getCmp('risks').getValue();

                                if (understand == false) {
                                    Ext.MessageBox.alert(this.i18n._("Error"), this.i18n._("You must check \"I understand the risks\""));
                                }
                                else {
                                    Ext.MessageBox.wait(i18n._("Generating today's reports..."), i18n._("Please wait"));
                                    this.getRpcNode().runDailyReport(function(result, exception) {
                                        this.afterRun(exception, callback);
                                    }.createDelegate(this));
                                }
                            }.createDelegate(this)
                        }]
                    }]
                }]
            });
        },
        // Generation panel
        buildGeneration: function() {
            var storeData = this.buildReportingUsersData();

            // WEEKLY SCHEDULE
            var weeklySundayCurrent = false;
            var weeklyMondayCurrent = false;
            var weeklyTuesdayCurrent = false;
            var weeklyWednesdayCurrent = false;
            var weeklyThursdayCurrent = false;
            var weeklyFridayCurrent = false;
            var weeklySaturdayCurrent = false;
            var weeklySched = this.getReportingSettings().schedule.weeklySched.list;
            for(var i=0; i<weeklySched.length;i++) {
                switch (weeklySched[i].day)
                    {
                    case 1: //Schedule.SUNDAY
                        weeklySundayCurrent = true;
                        break;
                    case 2: //Schedule.MONDAY:
                        weeklyMondayCurrent = true;
                        break;
                    case 3: //Schedule.TUESDAY:
                        weeklyTuesdayCurrent = true;
                        break;
                    case 4: //Schedule.WEDNESDAY:
                        weeklyWednesdayCurrent = true;
                        break;
                    case 5: //Schedule.THURSDAY:
                        weeklyThursdayCurrent = true;
                        break;
                    case 6: //Schedule.FRIDAY:
                        weeklyFridayCurrent = true;
                        break;
                    case 7: //Schedule.SATURDAY:
                        weeklySaturdayCurrent = true;
                        break;
                    }
            }

            // MONTHLY SCHEDULE
            var schedule = this.getReportingSettings().schedule;
            var monthlyFirstCurrent = schedule.monthlyNFirst;
            var monthlyEverydayCurrent = schedule.monthlyNDaily;
            var monthlyOnceCurrent = ( schedule.monthlyNDayOfWk != -1 /*Schedule.NONE*/ );
            var monthlyOnceDayCurrent = schedule.monthlyNDayOfWk;
            var monthlyNoneCurrent = !( monthlyFirstCurrent || monthlyEverydayCurrent || monthlyOnceCurrent );
            var monthlyOnceComboCurrent = monthlyOnceCurrent ? schedule.monthlyNDayOfWk : 1 /*SUNDAY*/ ;

            var fieldID = "" + Math.round( Math.random() * 1000000 );

            // email reports is a check column
            var onlineReports = new Ext.grid.CheckColumn({
                header : this.i18n._("Online Reports"),
                dataIndex : "onlineReports",
                width : 100,
                fixed : true
            });

            // online reports is a check column
            var emailReports = new Ext.grid.CheckColumn({
                header : this.i18n._("Email Reports"),
                dataIndex : "emailReports",
                width : 100,
                fixed : true
            });

            // Change the password for a user.
            var changePasswordColumn = new Ext.grid.IconColumn({
                header : this.i18n._("change password"),
                width : 130,
                fixed : true,
                iconClass : 'icon-edit-row',
                handle : function(record, index)
                {
                    // populate row editor
                    this.grid.rowEditorChangePassword.populate(record);
                    this.grid.rowEditorChangePassword.show();
                }
            });

            this.panelGeneration = new Ext.Panel({
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
                    items: [ this.gridRecipients = new Ung.EditorGrid({
                        width : 710,
                        name: 'Recipients',
                        title: this.i18n._("Reports Recipients and Users"),
                        hasEdit: false,
                        settingsCmp: this,
                        paginated: false,
                        height: 300,
                        plugins : [emailReports,onlineReports,changePasswordColumn],
                        emptyRow: {
                            emailAddress : "reportrecipient@example.com",
                            emailReports : true,
                            onlineReports : true,
                            clearPassword : null,
                            user : null
                        },
                        autoExpandColumn: "emailAddress",
                        data: storeData,
                        dataRoot: null,
                        autoGenerateId: true,
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
                            id: "emailAddress",
                            header: this.i18n._("Email Address (username)"),
                            dataIndex: "emailAddress",
                            width : 200,
                            editor: new Ext.form.TextField({
                                vtype: "email",
                                allowBlank: false,
                                blankText: this.i18n._("The email address cannot be blank.")
                            })
                        }, emailReports, onlineReports, changePasswordColumn],
                        rowEditorInputLines : [new Ext.form.TextField({
                            dataIndex : "emailAddress",
                            fieldLabel : this.i18n._("Email Address (username)"),
                            allowBlank : false,
                            blankText : this.i18n._("The email address name cannot be blank."),
                            width : 200
                        }), new Ext.form.Checkbox({
                            dataIndex : "emailReports",
                            fieldLabel : this.i18n._("Email Reports"),
                            width : 200
                        }), new Ext.form.Checkbox({
                            dataIndex : "onlineReports",
                            id : "add_reporting_online_reports_" + fieldID,
                            fieldLabel : this.i18n._("Online Reports"),
                            width : 200
                        }), new Ext.form.TextField({
                            inputType: "password",
                            name : "Password",
                            dataIndex : "clearPassword",
                            id : "add_reporting_user_password_" + fieldID,
                            msgTarget : "title",
                            fieldLabel : this.i18n._("Password"),
                            boxLabel : this.i18n._("(required for 'Online Reports')"),
                            width : 200,
                            minLength : 3,
                            minLengthText : String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                        }), new Ext.form.TextField({
                            inputType: "password",
                            name : "Confirm Password",
                            dataIndex : "clearPassword",
                            id : "add_reporting_confirm_password_" + fieldID,
                            fieldLabel : this.i18n._("Confirm Password"),
                            width : 200
                        })],
                        rowEditorValidate: function (inputLines) {
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
                        }.createDelegate(this)
                    })]
                },{
                    title : this.i18n._("Email Attachment Settings"),
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Attach Detailed Report Logs to Email (CSV Zip File)'),
                        name : 'Email Detail',
                        hideLabel : true,
                        checked : this.getReportingSettings().emailDetail,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().emailDetail = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Attachment size limit (MB)'),
                        name : 'Attachement size limit',
                        id: 'reporting_attachment_size_limit',
                        value : this.getReportingSettings().attachmentSizeLimit,
                        width: 30,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 30,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().attachmentSizeLimit = newValue;
                                }.createDelegate(this)
                            }
                        }
                }
            ]
                },{
                    title : this.i18n._("Daily Reports"),
                    labelWidth: 150,
                    items : [{
                        border : false,
                        cls: 'description',
                        html : this.i18n._('Daily Reports are generated at midnight and covers events from the previous 24 hours, up to, but not including the day of generation.')
                    },  {
                        xtype : 'checkbox',
                        name : 'Generate Daily Reports',
                        fieldLabel : this.i18n._('Generate Daily Reports'),
                        boxLabel : this.i18n._('Every Day'),
                        checked : this.getReportingSettings().schedule.daily,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().schedule.daily = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                },{
                    title : this.i18n._("Weekly Reports"),
                    labelWidth: 150,
                    items : [{
                        border : false,
                        cls: 'description',
                        html : this.i18n._('Weekly Reports are generated at midnight and covers events from the previous 7 days, up to, but not including the day of generation.')
                    },  {
                        xtype : 'checkbox',
                        name : 'Sunday',
                        id : 'reporting_weeklySunday',
                        fieldLabel : this.i18n._('Generate Weekly Reports'),
                        boxLabel : this.i18n._('Sunday'),
                        checked : weeklySundayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Monday',
                        id : 'reporting_weeklyMonday',
                        boxLabel : this.i18n._('Monday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyMondayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Tuesday',
                        id : 'reporting_weeklyTuesday',
                        boxLabel : this.i18n._('Tuesday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyTuesdayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Wednesday',
                        id : 'reporting_weeklyWednesday',
                        boxLabel : this.i18n._('Wednesday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyWednesdayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Thursday',
                        id : 'reporting_weeklyThursday',
                        boxLabel : this.i18n._('Thursday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyThursdayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Friday',
                        id : 'reporting_weeklyFriday',
                        boxLabel : this.i18n._('Friday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyFridayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Saturday',
                        id : 'reporting_weeklySaturday',
                        boxLabel : this.i18n._('Saturday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklySaturdayCurrent
                    }]
                },{
                    title : this.i18n._("Monthly Reports"),
                    labelWidth: 150,
                    items : [{
                        border : false,
                        cls: 'description',
                        html : this.i18n._('Monthly Reports are generated at midnight and covers events from the previous 30 days, up to, but not including the day of generation.')
                    },  {
                        xtype : 'radiogroup',
                        name : 'Generate Monthly Reports',
                        fieldLabel : 'Generate Monthly Reports',
                        itemCls: 'x-check-group-alt',
                        columns: 1,
                        items : [{
                            boxLabel : this.i18n._('Never'),
                            name: 'rb-col',
                            id : 'reporting_monthlyNone',
                            checked : monthlyNoneCurrent
                        },{
                            boxLabel : this.i18n._('First Day of Month'),
                            name: 'rb-col',
                            id : 'reporting_monthlyFirst',
                            checked : monthlyFirstCurrent
                        },{
                            boxLabel : this.i18n._('Everyday'),
                            name: 'rb-col',
                            id : 'reporting_monthlyEveryday',
                            checked : monthlyEverydayCurrent
                        },{
                            boxLabel : this.i18n._('Once Per Week'),
                            name: 'rb-col',
                            id : 'reporting_monthlyOnce',
                            checked : monthlyOnceCurrent,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                            Ext.getCmp('reporting_monthlyOnceCombo').setDisabled(!checked);
                                    }
                                }
                            }
                        }]
                    }, {
                        xtype : 'combo',
                        editable : false,
                        mode : 'local',
                        fieldLabel : '',
                        labelSeparator : '',
                        name : "Once Per Week combo",
                        id : 'reporting_monthlyOnceCombo',
                        store : new Ext.data.SimpleStore({
                            fields : ['monthlyOnceValue', 'monthlyOnceName'],
                            data : [[1, this.i18n._("Sunday")],
                                    [2, this.i18n._("Monday")],
                                    [3, this.i18n._("Tuesday")],
                                    [4, this.i18n._("Wednesday")],
                                    [5, this.i18n._("Thursday")],
                                    [6, this.i18n._("Friday")],
                                    [7, this.i18n._("Saturday")]]
                        }),
                        displayField : 'monthlyOnceName',
                        valueField : 'monthlyOnceValue',
                        value : monthlyOnceComboCurrent,
                        disabled : !monthlyOnceCurrent,
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small'
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
                        value : this.getReportingSettings().dbRetention,
                        width: 25,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 65,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().dbRetention = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
            },
{
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
                        value : this.getReportingSettings().fileRetention,
                        width: 25,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 90,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().fileRetention = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }

]});

            /* Create the row editor for updating the password */
            this.gridRecipients.rowEditorChangePassword = new Ung.RowEditorWindow({
                grid : this.gridRecipients,
                inputLines : [new Ext.form.TextField({
                    inputType: "password",
                    name : "Password",
                    dataIndex : "clearPassword",
                    id : "edit_reporting_user_password_"  + fieldID,
                    fieldLabel : this.i18n._("Password"),
                    width : 200,
                    minLength : 3,
                    minLengthText : String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                }), new Ext.form.TextField({
                    inputType: "password",
                    name : "Confirm Password",
                    dataIndex : "clearPassword",
                    id : "edit_reporting_confirm_password_"  + fieldID,
                    fieldLabel : this.i18n._("Confirm Password"),
                    width : 200
                })],
                validate: function(inputLines) {
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
                	
                }.createDelegate(this)
            });

            this.gridRecipients.rowEditorChangePassword.render("containter");
        },
        // IP Map grid
        buildIpMap: function() {
            this.gridIpMap = new Ung.EditorGrid({
                settingsCmp: this,
                name: 'Name Map',
                helpSource: 'ip_addresses',
                title: this.i18n._("Name Map"),
                emptyRow: {
                    "ipMaddr": "0.0.0.0/32",
                    "name": this.i18n._("[no name]"),
                    "description": this.i18n._("[no description]")
                },
                // the column is autoexpanded if the grid width permits
                autoExpandColumn: 'name',
                recordJavaClass: "com.untangle.uvm.node.IPMaskedAddressRule",

                data: this.getReportingSettings().networkDirectory.entries,
                dataRoot: 'list',

                // the list of fields
                fields: [{
                    name: 'id'
                }, {
                    name: 'ipMaddr'
                }, {
                    name: 'name'
                }, {
                    name: 'description'
                }],
                // the list of columns for the column model
                columns: [{
                    id: 'ipMaddr',
                    header: this.i18n._("Name Map"),
                    width: 200,
                    dataIndex: 'ipMaddr',
                    editor: new Ext.form.TextField({})
                }, {
                    id: 'name',
                    header: this.i18n._("name"),
                    width: 200,
                    dataIndex: 'name',
                    editor: new Ext.form.TextField({})
                }],
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [new Ext.form.TextField({
                    name: "Subnet",
                    dataIndex: "ipMaddr",
                    fieldLabel: this.i18n._("Name Map"),
                    allowBlank: false,
                    width: 200
                }), new Ext.form.TextField({
                    name: "Name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Name"),
                    allowBlank: false,
                    width: 200
                })]
            });
        },
        // validation
/*        
        validateClient: function() {
            var recipientsList=this.gridRecipients.getFullSaveList();
            for(var i=0;i<recipientsList.length;i++) {
            	var recipient = recipientsList[i];
            }
            return true;
        },
*/        
        validateServer: function() {
            // ipMaddr list must be validated server side
            var ipMapList = this.gridIpMap.getSaveList();
            var ipMaddrList = [];
            var i;

            // added
            for ( i = 0; i < ipMapList[0].list.length; i++) {
                ipMaddrList.push(ipMapList[0].list[i]["ipMaddr"]);
            }
            // modified
            for ( i = 0; i < ipMapList[2].list.length; i++) {
                ipMaddrList.push(ipMapList[2].list[i]["ipMaddr"]);
            }
            
            if (ipMaddrList.length > 0) {
                try {
                    var result=null;
                    try {
                        result = this.getValidator().validate({
                            list: ipMaddrList,
                            "javaClass": "java.util.ArrayList"
                        });
                    } catch (e) {
                        Ung.Util.rpcExHandler(e);
                    }
                    if (!result.valid) {
                        var errorMsg = "";
                        switch (result.errorCode) {
                        case 'INVALID_IPMADDR' :
                            errorMsg = this.i18n._("Invalid \"IP address\" specified") + ": " + result.cause;
                            break;
                        default :
                            errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                        }

                        this.tabs.activate(this.gridIpMap);
                        this.gridIpMap.focusFirstChangedDataByFieldValue("ipMaddr", result.cause);
                        Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                        return false;
                    }
                } catch (e) {
                    var message = ( e == null ) ? "Unknown" : e.message;
                    if (message == "Unknown") {
                        message = i18n._("Please Try Again");
                    }
                    Ext.MessageBox.alert(i18n._("Failed"), message);
                    return false;
                }
            }

            return true;
        },
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        reloadSettings : function()
        {
            this.getMailSettings(true);
            this.getReportingSettings(true);
            this.getAdminSettings(true);

            this.gridRecipients.clearChangedData();
            this.gridRecipients.store.loadData( this.buildReportingUsersData());
            var cmpIds = this.getEditableFields(),
                i;
            for (i = 0; i < cmpIds.length; i++) {
                if (cmpIds[i].isDirty()){
                    cmpIds[i].originalValue = cmpIds[i].getValue();
                    cmpIds[i].reset();
                }
            }
            /*
            var rdtk = Ext.getCmp("reporting_daysToKeepFiles");
            rdtk.setValue( this.getReportingSettings().fileRetention );
            rdtk.originalValue = rdtk.getValue();
            
            rdtk = Ext.getCmp('reporting_daysToKeepDB');            
            rdtk.setValue( this.getReportingSettings().dbRetention );
            rdtk.originalValue = rdtk.getValue();
            */

            this.gridIpMap.clearChangedData();
            this.gridIpMap.store.loadData( this.getReportingSettings().networkDirectory.entries );

            Ext.MessageBox.hide();
        },
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
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
                if(!this.panelGeneration.rendered) {
                    var activeTab=this.tabs.getActiveTab();
                    this.tabs.activate(this.panelGeneration);
                    this.tabs.activate(activeTab);
                }

                // set weekly schedule
                var weeklySched = [];
                if (Ext.getCmp('reporting_weeklySunday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:1});
                if (Ext.getCmp('reporting_weeklyMonday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:2});
                if (Ext.getCmp('reporting_weeklyTuesday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:3});
                if (Ext.getCmp('reporting_weeklyWednesday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:4});
                if (Ext.getCmp('reporting_weeklyThursday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:5});
                if (Ext.getCmp('reporting_weeklyFriday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:6});
                if (Ext.getCmp('reporting_weeklySaturday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:7});
                this.getReportingSettings().schedule.weeklySched.list = weeklySched;
                
                // set monthly schedule
                var schedule = this.getReportingSettings().schedule;
                schedule.monthlyNFirst = Ext.getCmp('reporting_monthlyFirst').getValue();
                schedule.monthlyNDaily = Ext.getCmp('reporting_monthlyEveryday').getValue();
                var monthlyOnce = Ext.getCmp('reporting_monthlyOnce').getValue();
                schedule.monthlyNDayOfWk = monthlyOnce ? Ext.getCmp('reporting_monthlyOnceCombo').getValue() : -1 //NONE ;
                
                // set Ip Map list
                this.getReportingSettings().networkDirectory.entries.list = this.gridIpMap.getFullSaveList();

                // save email recipients
                var gridRecipientsValues = this.gridRecipients.getFullSaveList();
                var adminSettings = this.getAdminSettings();
                var users = adminSettings.users.set, recipientsList = [], reportingUsers = [], user = null;

                for(var i=0; i<gridRecipientsValues.length; i++) {
                    var recipient = gridRecipientsValues[i];
                    reportingUsers.push(recipient.emailAddress);
                    if ( recipient.emailReports == true ) {
                        recipientsList.push(recipient.emailAddress);
                    }

                    /* If a user already exists, reuse it. */
                    if (( recipient.user != null ) && ( users[recipient.user] != null )) {
                        user = users[recipient.user];
                        user.hasReportsAccess = recipient.onlineReports;
                        user.login = recipient.emailAddress;
                        user.keepUser = true;
                        if ( recipient.clearPassword != null ) {
                            user.clearPassword = recipient.clearPassword;
                        }
                    /* Otherwise, create a user if onlineReports is set or the password is set */
                    } else if ( recipient.onlineReports || recipient.clearPassword ) {
                        user = {
                            "login" : recipient.emailAddress,
                            "name" : this.i18n._("[reports only user]"),
                            "hasWriteAccess" : false,
                            "hasReportsAccess" : recipient.onlineReports,
                            "email" : recipient.emailAddress,
                            "clearPassword" : recipient.clearPassword,
                            "javaClass" : "com.untangle.uvm.User",
                            keepUser : true
                        };

                        /* Append the new user */
                        //TODO: change this seems unsafe!
                        users[Math.round( Math.random() * 1000000 )] = user;
                        //users[this.genAddedId()]=users;
                    }
                }

                /* Delete all of the reporting only users that have not been updated. */
                users = {};

                var c  = 1;
                for ( var id in adminSettings.users.set ) {
                    user = adminSettings.users.set[id];
                    c++;
                    if ( user == null ) {
                        continue;
                    }
                    if ( user.hasWriteAccess || user.keepUser ) {
                        delete user.keepUser;
                        delete user.password;
                        /* Encode all of the strings for safety." */
                        users[c] = user;
                    }
                }
                adminSettings.users.set = users;

                this.getMailSettings().reportEmail = recipientsList.join(",");
                this.getReportingSettings().reportingUsers = reportingUsers.join(",");

                this.getRpcNode().setReportingSettings(function(result, exception) {
                    this.afterSave(exception, callback);
                }.createDelegate(this), this.getReportingSettings());

                // do the save
                rpc.adminManager.setMailSettings(function(result, exception) {
                    this.afterSave(exception, callback);
                }.createDelegate(this), this.getMailSettings());

                rpc.adminManager.setAdminSettings(function(result, exception) {
                    this.afterSave(exception, callback);
                }.createDelegate(this), this.getAdminSettings());
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
                return this.panelGeneration.findBy(function(obj){return obj.xtype =='checkbox' || obj.xtype == 'radiogroup' || obj.xtype == 'numberfield' || obj.xtype == 'textfield' ? true : false;});        
        },
        isDirty: function() {
            if(this.panelGeneration.rendered) {
                var cmpIds = this.getEditableFields(),
                    i;
                for (i = 0; i < cmpIds.length; i++) {
                    if (cmpIds[i].isDirty()){
                        return true;
                    }
                }
                if (this.gridRecipients.isDirty()){
                    return true;
                }
            }
            return this.gridIpMap.isDirty();
        },

        buildReportingUsersData : function()
        {
            var storeData = [];
            var reportEmail = this.getMailSettings().reportEmail || "";
            var adminUsers = this.getAdminSettings().users.set;
            var reportingUsers = this.getReportingSettings().reportingUsers || "", reportingUsersSet = {};

            /* Convert the two comma separated lists to sets. */
            var temp = {}, values, c;

            values = reportEmail.split(",");
            for ( c = 0 ; c < values.length ; c++ ) {
                temp[values[c].trim()] = true;
            }
            reportEmail = temp;

            values = reportingUsers.split(",");

            for ( c = 0 ; c < values.length ; c++ ) {
                values[c] = values[c].trim();
            }
            reportingUsers = values;

            for( c=0 ; c < reportingUsers.length; c++) {
                var email = reportingUsers[c];
                if ( email.length == 0 ) {
                    continue;
                }
                user = this.findAdminUser( adminUsers, email );
                storeData.push({
                    user : user,
                    emailReports : reportEmail[email] != null,
                    onlineReports : user != null && adminUsers[user].hasReportsAccess,
                    clearPassword : null,
                    emailAddress : email
                });
            }

            return storeData;
        },

        findAdminUser : function( adminUsers, emailAddress )
        {
            var id;
            for ( id in adminUsers ) {
                if ( adminUsers[id].login == emailAddress ) {
                    return id;
                }
            }

            /* Use null, new users are created at save time. */
            return null;
        }
    });
}
