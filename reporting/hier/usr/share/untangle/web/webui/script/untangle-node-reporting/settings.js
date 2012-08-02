if (!Ung.hasResource["Ung.Reporting"]) {
    Ung.hasResource["Ung.Reporting"] = true;
    Ung.NodeWin.registerClassName('untangle-node-reporting', 'Ung.Reporting');

    Ext.define('Ung.Reporting', {
        extend:'Ung.NodeWin',
        panelStatus: null,
        panelGeneration: null,
        panelEmail: null,
        panelSyslog: null,
        panelDatabase: null,
        gridReportingUsers: null,
        gridHostnameMap: null,
        initComponent: function(container, position) {
            this.buildStatus();
            this.buildGeneration();
            this.buildEmail();
            this.buildSyslog();
            this.buildHostnameMap();
            this.buildDatabase();

            // only show DB settings if set to something other than localhost
            if (this.getSettings().dbHost != "localhost") { 
                this.buildTabPanel([this.panelStatus, this.panelGeneration, this.panelEmail, this.panelSyslog, this.gridHostnameMap, this.panelDatabase]);
            } else {
                this.buildTabPanel([this.panelStatus, this.panelGeneration, this.panelEmail, this.panelSyslog, this.gridHostnameMap]);
            }
            this.tabs.setActiveTab(this.panelStatus);
            this.clearDirty();
            this.callParent(arguments);
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
                                window.open(viewReportsUrl);
                            }, this)
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
                        buttons: [{
                            xtype: 'button',
                            text: this.i18n._('Generate Today\'s Reports'),
                            name: 'Generate Reports',
                            iconCls: 'action-icon',
                            handler: Ext.bind(function(callback) {
                                Ext.MessageBox.wait(i18n._("Generating today's reports... This may take a few minutes."), i18n._("Please wait"));
                                this.getRpcNode().runDailyReport(Ext.bind(function(result, exception) {
                                    Ext.MessageBox.hide();
                                    if(Ung.Util.handleException(exception)) return;
                                }, this));
                            }, this)
                        }]
                    }]
                }]
            });
        },
        // Generation panel
        buildGeneration: function() {
            var fieldID = "" + Math.round( Math.random() * 1000000 );

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
                    xtype: 'fieldset'
                },
                items: [{
                    title: this.i18n._("Daily Reports"),
                    items: [{
                        border: false,
                        cls: 'description',
                        html: this.i18n._('Daily Reports covers the previous day. Daily reports will be generated on the selected days.')
                    },  {
                        xtype: 'udayfield',
                        name: 'Daily Days',
                        value: this.getSettings().generateDailyReports,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().generateDailyReports = elem.getValue();
                                }, this)
                            }
                        }
                    }]
                },{
                    title: this.i18n._("Weekly Reports"),
                    items: [{
                        border: false,
                        cls: 'description',
                        html: this.i18n._('Weekly Reports covers the previous week. Weekly reports will be generated on the selected days.')
                    },  {
                        xtype: 'udayfield',
                        name: 'Weekly Days',
                        value: this.getSettings().generateWeeklyReports,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().generateWeeklyReports = elem.getValue();
                                }, this)
                            }
                        }
                    }]
                },{
                    title: this.i18n._("Monthly Reports"),
                    items: [{
                        border: false,
                        cls: 'description',
                        html: this.i18n._('Monthly Reports are generated on the 1st and cover the previous month.')
                    },  {
                        xtype: 'checkbox',
                        name: "Monthly Enabled",
                        boxLabel: this.i18n._("Enabled"),
                        hideLabel: true,
                        checked: this.getSettings().generateMonthlyReports,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().generateMonthlyReports = elem.getValue();
                                }, this)
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
                        xtype: 'timefield',
                        fieldLabel: this.i18n._('Generation Time'),
                        name: 'Generation Time',
                        width: 90,
                        hideLabel: true,
                        value: generationTime,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    if (newValue && newValue instanceof Date) {
                                        this.getSettings().generationMinute = newValue.getMinutes();
                                        this.getSettings().generationHour = newValue.getHours();
                                    }
                                }, this)
                            }
                        }
                    }]
                }, {
                    title: this.i18n._("Data Retention"),
                    labelWidth: 150,
                    items: [{
                        border: false,
                        cls: 'description',
                        html: this.i18n._("Keep event data for this number of days. The smaller the number the lower the disk space requirements and resource usage during report generation.")
                    },{
                        border: false,
                        cls: "description",
                        html: Ext.String.format("{0}" + this.i18n._("Warning") + ":{1} " +  this.i18n._("Depending on the server and network, increasing this value may cause performance issues."),"<font color=\"red\">","</font>")
                    },{
                        xtype: 'numberfield',
                        fieldLabel: this.i18n._('Data Retention days'),
                        name: 'Data Retention days',
                        id: 'reporting_daysToKeepDB',
                        value: this.getSettings().dbRetention,
                        labelWidth: 150,
                        labelAlign: 'right',
                        width: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 65,
                        hideTrigger:true,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().dbRetention = newValue;
                                }, this)
                            }
                        }
                    }]
                }]
            });
        },
        // Email panel
        buildEmail: function() {
            var fieldID = "" + Math.round( Math.random() * 1000000 );

            // Change the password for a user.
            var changePasswordColumn = Ext.create('Ung.grid.EditColumn',{
                header: this.i18n._("change password"),
                width: 130,
                fixed: true,
                iconClass: 'icon-edit-row',
                handler: function(view,rowIndex,colIndex)
                {
                    var record = view.getStore().getAt(rowIndex);
                    this.grid.rowEditorChangePassword.populate(record);
                    this.grid.rowEditorChangePassword.show();
                }
            });

            this.panelEmail = Ext.create('Ext.panel.Panel',{
                // private fields
                name: 'Email',
                helpSource: 'email',
                parentId: this.getId(),
                title: this.i18n._('Email'),
                layout: "anchor",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    anchor: "98%",
                    xtype: 'fieldset'
                },
                items: [{
                    title: this.i18n._('Email'),
                    height: 350,
                    items: [ this.gridReportingUsers = Ext.create('Ung.EditorGrid',{
                        width: 710,
                        name: 'ReportingUsers',
                        title: this.i18n._("Reporting Users"),
                        hasEdit: false,
                        settingsCmp: this,
                        paginated: false,
                        height: 300,
                        emptyRow: {
                            javaClass: "com.untangle.node.reporting.ReportingUser",
                            emailAddress: "reportrecipient@example.com",
                            emailSummaries: true,
                            onlineAccess: false,
                            password: null,
                            passwordHashBase64: null
                        },
                        dataProperty: 'reportingUsers',
                        recordJavaClass: "com.untangle.node.reporting.ReportingUser",
                        plugins:[changePasswordColumn],
                        fields: [{
                            name: "emailAddress"
                        },{
                            name: "emailSummaries"
                        },{
                            name: "onlineAccess"
                        },{
                            name: "password"
                        },{
                            name: "passwordHashBase64"
                        }],
                        sortField: "emailAddress",
                        columnsDefaultSortable: true,
                        columns: [{
                            header: this.i18n._("Email Address (username)"),
                            dataIndex: "emailAddress",
                            width: 200,
                            editor: {
                                xtype:'textfield',
                                vtype: "email",
                                allowBlank: false,
                                blankText: this.i18n._("The email address cannot be blank.")
                            },
                            flex:1
                        }, {
                            xtype:'checkcolumn',
                            header: this.i18n._("Email Summaries"),
                            dataIndex: "emailSummaries",
                            width: 100,
                            fixed: true
                        }, { 
                            xtype:'checkcolumn',
                            header: this.i18n._("Online Access"),
                            dataIndex: "onlineAccess",
                            width: 100,
                            fixed: true
                        }, changePasswordColumn ],
                        rowEditorInputLines: [
                            {
                                xtype:'textfield',
                                dataIndex: "emailAddress",
                                fieldLabel: this.i18n._("Email Address (username)"),
                                allowBlank: false,
                                blankText: this.i18n._("The email address name cannot be blank."),
                                width: 300
                            },
                            {
                                xtype:'checkbox',
                                dataIndex: "emailSummaries",
                                fieldLabel: this.i18n._("Email Summaries"),
                                width: 300
                            },
                            {
                                xtype:'checkbox',
                                dataIndex: "onlineAccess",
                                id: "add_reporting_online_reports_" + fieldID,
                                fieldLabel: this.i18n._("Online Access"),
                                width: 300
                            },
                            {
                                xtype:'textfield',
                                inputType: "password",
                                name: "Password",
                                dataIndex: "password",
                                id: "add_reporting_user_password_" + fieldID,
                                msgTarget: "title",
                                fieldLabel: this.i18n._("Password"),
                                boxLabel: this.i18n._("(required for 'Online Access')"),
                                width: 300,
                                minLength: 3,
                                minLengthText: Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                            },
                            {
                                xtype:'textfield',
                                inputType: "password",
                                name: "Confirm Password",
                                dataIndex: "password",
                                id: "add_reporting_confirm_password_" + fieldID,
                                fieldLabel: this.i18n._("Confirm Password"),
                                width: 300
                            }]
                    })]
                },{
                    title: this.i18n._("Email Attachment Settings"),
                    items: [{
                        xtype: 'checkbox',
                        boxLabel: this.i18n._('Attach Detailed Report Logs to Email (CSV Zip File)'),
                        name: 'Email Detail',
                        hideLabel: true,
                        checked: this.getSettings().emailDetail,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().emailDetail = newValue;
                                }, this)
                            }
                        }
                    },{
                        xtype: 'numberfield',
                        fieldLabel: this.i18n._('Attachment size limit (MB)'),
                        name: 'Attachement size limit',
                        id: 'reporting_attachment_size_limit',
                        value: this.getSettings().attachmentSizeLimit,
                        labelWidth: 150,
                        labelAlign:'right',
                        width: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,
                        maxValue: 30,
                        hideTrigger: true,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().attachmentSizeLimit = newValue;
                                }, this)
                            }
                        }
                    }]
                }]
            });
            /* Create the row editor for updating the password */
            this.gridReportingUsers.rowEditorChangePassword = Ext.create('Ung.RowEditorWindow',{
                grid: this.gridReportingUsers,
                inputLines: [
                    {
                        xtype:'textfield',
                        inputType: "password",
                        name: "Password",
                        dataIndex: "password",
                        id: "edit_reporting_user_password_"  + fieldID,
                        fieldLabel: this.i18n._("Password"),
                        width: 300,
                        minLength: 3,
                        minLengthText: Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                    }, 
                    {
                        xtype:'textfield',
                        inputType: "password",
                        name: "Confirm Password",
                        dataIndex: "password",
                        id: "edit_reporting_confirm_password_"  + fieldID,
                        fieldLabel: this.i18n._("Confirm Password"),
                        width: 300
                    }],
                validate: Ext.bind(function(inputLines) {
                    //validate password match
                    var pwd = Ext.getCmp("edit_reporting_user_password_" + fieldID);
                    var confirmPwd = Ext.getCmp("edit_reporting_confirm_password_" + fieldID);
                    if(pwd.getValue() != confirmPwd.getValue()) {
                        pwd.markInvalid();
                        return this.i18n._('Passwords do not match');
                    }
                    // validate password not empty if onlineAccess checked
                    var onlineAccess=Ext.getCmp("add_reporting_online_reports_" + fieldID);
                    if(onlineAccess.getValue() &&  pwd.getValue().length==0) {
                        return this.i18n._("A password must be set to enable Online Access!");
                    } else {
                        return true;
                    }
                }, this)
            });
        },
        // syslog panel
        buildSyslog: function() {
            this.panelSyslog = Ext.create('Ext.panel.Panel',{
                // private fields
                name: 'Syslog',
                helpSource: 'syslog',
                parentId: this.getId(),
                title: this.i18n._('Syslog'),
                layout: "anchor",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    anchor: "98%",
                    xtype: 'fieldset'
                },
                items: [{
                    title: this.i18n._('Syslog'),
                    height: 350,
                    items: [{
                        html: this.i18n._('If enabled logged events will be sent in real-time to a remote syslog for custom processing.') + "<br/>",
                        cls: 'description',
                        border: false
                    }, {
                        xtype: 'radio',
                        boxLabel: Ext.String.format(this.i18n._('{0}Disable{1} Syslog Events. (This is the default setting.)'), '<b>', '</b>'),
                        hideLabel: true,
                        name: 'syslogEnabled',
                        checked: !this.getSettings().syslogEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind( function(elem, checked) {
                                    this.getSettings().syslogEnabled = !checked;
                                    if (checked) {
                                        Ext.getCmp('reporting_syslog_host').disable();
                                        Ext.getCmp('reporting_syslog_port').disable();
                                        Ext.getCmp('reporting_syslog_protocol').disable();
                                    }
                                }, this)
                            }
                        }
                    },{
                        xtype: 'radio',
                        boxLabel: Ext.String.format(this.i18n._('{0}Enable{1} Syslog Events.'), '<b>', '</b>'),
                        hideLabel: true,
                        name: 'syslogEnabled',
                        checked: this.getSettings().syslogEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind( function(elem, checked) {
                                    this.getSettings().syslogEnabled = checked;
                                    if (checked) {
                                        Ext.getCmp('reporting_syslog_host').enable();
                                        Ext.getCmp('reporting_syslog_port').enable();
                                        Ext.getCmp('reporting_syslog_protocol').enable();
                                    }
                                }, this)
                            }
                        }
                    }, {
                        border: false,
                        autoWidth: true,
                        items: [{
                            xtype: 'textfield',
                            fieldLabel: this.i18n._('Host'),
                            name: 'syslogHost',
                            width: 300,
                            itemCls: 'left-indent-1',
                            id: 'reporting_syslog_host',
                            value: this.getSettings().syslogHost,
                            allowBlank: false,
                            blankText: this.i18n._("A \"Host\" must be specified."),
                            disabled: !this.getSettings().syslogEnabled
                        },{
                            xtype: 'numberfield',
                            fieldLabel: this.i18n._('Port'),
                            name: 'syslogPort',
                            width: 200,
                            itemCls: 'left-indent-1',
                            id: 'reporting_syslog_port',
                            value: this.getSettings().syslogPort,
                            allowDecimals: false,
                            allowNegative: false,
                            allowBlank: false,
                            blankText: this.i18n._("You must provide a valid port."),
                            vtype: 'port',
                            disabled: !this.getSettings().syslogEnabled
                        },{
                            xtype: 'combo',
                            name: 'syslogProtocol',
                            itemCls: 'left-indent-1',
                            id: 'reporting_syslog_protocol',
                            editable: false,
                            fieldLabel: this.i18n._('Protocol'),
                            mode: 'local',
                            triggerAction: 'all',
                            listClass: 'x-combo-list-small',
                            store: new Ext.data.SimpleStore({
                                fields: ['key', 'name'],
                                data:[
                                    ["UDP", this.i18n._("UDP")],
                                    ["TCP", this.i18n._("TCP")]
                                ]
                            }),
                            displayField: 'name',
                            valueField: 'key',
                            value: this.getSettings().syslogProtocol,
                            disabled: !this.getSettings().syslogEnabled
                        }]
                    }]
                }]
            });
        },
        // database panel
        buildDatabase: function() {
            this.panelDatabase = Ext.create('Ext.panel.Panel',{
                // private fields
                name: 'Database',
                helpSource: 'database',
                parentId: this.getId(),
                title: this.i18n._('Database'),
                layout: "anchor",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    anchor: "98%",
                    xtype: 'fieldset'
                },
                items: [{
                    title: this.i18n._('Database'),
                    height: 350,
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Host'),
                        name: 'databaseHost',
                        width: 300,
                        itemCls: 'left-indent-1',
                        value: this.getSettings().dbHost,
                        allowBlank: false,
                        blankText: this.i18n._("A \"Host\" must be specified."),
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().dbHost = newValue;
                            }, this )
                        }
                    },{
                        xtype: 'numberfield',
                        fieldLabel: this.i18n._('Port'),
                        name: 'databasePort',
                        width: 200,
                        itemCls: 'left-indent-1',
                        value: this.getSettings().dbPort,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank: false,
                        blankText: this.i18n._("You must provide a valid port."),
                        vtype: 'port',
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().dbPort = newValue;
                            }, this )
                        }
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('User'),
                        name: 'databaseUser',
                        width: 300,
                        itemCls: 'left-indent-1',
                        value: this.getSettings().dbUser,
                        allowBlank: false,
                        blankText: this.i18n._("A \"User\" must be specified."),
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().dbUser = newValue;
                            }, this )
                        }
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Password'),
                        name: 'databasePassword',
                        width: 300,
                        itemCls: 'left-indent-1',
                        value: this.getSettings().dbPassword,
                        allowBlank: false,
                        blankText: this.i18n._("A \"Password\" must be specified."),
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().dbPassword = newValue;
                            }, this )
                        }
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Name'),
                        name: 'databaseName',
                        width: 300,
                        itemCls: 'left-indent-1',
                        value: this.getSettings().dbName,
                        allowBlank: false,
                        blankText: this.i18n._("A \"Name\" must be specified."),
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().dbName = newValue;
                            }, this )
                        }
                    }]
                }]
            });
        },
        // Hostname Map grid
        buildHostnameMap: function() {
            this.gridHostnameMap = Ext.create('Ung.EditorGrid',{
                settingsCmp: this,
                name: 'Name Map',
                helpSource: 'ip_addresses',
                title: this.i18n._("Name Map"),
                paginated: false,
                emptyRow: {
                    javaClass: "com.untangle.node.reporting.ReportingUser",
                    "address": "1.2.3.4",
                    "hostname": this.i18n._("[no name]")
                },
                dataProperty: 'hostnameMap',
                recordJavaClass: "com.untangle.node.reporting.ReportingHostnameMapEntry",
                // the list of fields
                fields: [{
                    name: 'id'
                }, {
                    name: 'address'
                }, {
                    name: 'hostname'
                }],
                // the list of columns for the column model
                columns: [{
                    header: this.i18n._("IP Address"),
                    width: 200,
                    dataIndex: 'address',
                    editor: {
                        vtype: 'ipAddress',
                        xtype:'textfield'
                    }
                }, {
                    header: this.i18n._("Name"),
                    width: 200,
                    dataIndex: 'hostname',
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
                        dataIndex: "address",
                        fieldLabel: this.i18n._("IP Address"),
                        vtype: 'ipAddress',
                        allowBlank: false,
                        width: 300
                    }, 
                    {
                        xtype:'textfield',
                        name: "Name",
                        dataIndex: "hostname",
                        fieldLabel: this.i18n._("Name"),
                        allowBlank: false,
                        width: 300
                    }]
            });
        },
        beforeSave: function(isApply,handler) {
            this.getSettings().reportingUsers.list = this.gridReportingUsers.getPageList();
            this.getSettings().hostnameMap.list = this.gridHostnameMap.getPageList();

            this.getSettings().syslogHost = Ext.getCmp('reporting_syslog_host').getValue();
            this.getSettings().syslogPort = Ext.getCmp('reporting_syslog_port').getValue();
            this.getSettings().syslogProtocol = Ext.getCmp('reporting_syslog_protocol').getValue();

            handler.call(this, isApply);
        }
    });
}
//@ sourceURL=reporting-settings.js
