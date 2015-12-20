
Ext.define('Webui.untangle-node-reports.settings', {
    extend:'Ung.NodeWin',
    hasDefaultAppStatus: false,
    panelStatus: null,
    panelEmail: null,
    panelSyslog: null,
    panelData: null,
    gridReportsUsers: null,
    gridHostnameMap: null,
    gridReportEntries: null,
    gridAlertEventLog: null,
    initComponent: function(container, position) {
        this.buildPasswordValidator();
        this.buildStatus();
        this.buildEmail();
        this.buildSyslog();
        this.buildHostnameMap();
        this.buildReportEntries();
        this.buildAlertRules();
        this.buildData();

        var panels = [this.panelStatus, this.gridReportEntries, this.panelData, this.panelAlertRules, this.panelEmail, this.panelSyslog, this.gridHostnameMap ];
        
        this.buildTabPanel(panels);
        this.callParent(arguments);
    },
    getAlertRuleConditions: function () {
        return [
            {name:"FIELD_CONDITION",displayName: i18n._("Field condition"), type: "editor", editor: Ext.create('Ung.FieldConditionWindow',{}), visible: true, disableInvert: true, allowMultiple: true, allowBlank: false, formatValue: function(value) {
                var result= "";
                if(value) {
                    result = value.field + " " + value.comparator + " " + value.value;
                }
                return result;
            }}
        ];
    },
    buildPasswordValidator: function() {
        this.passwordValidator = function( fieldValue ){
            // Get field container
            var panel = this.up("panel");
            // Walk fields looking for "_password_" and "_confirm_password_"
            var suffix = this.id.substr( this.id.lastIndexOf("_") + 1 );
            var fields = panel.query("textfield[id$="+suffix+"]");
            var pwd = null;
            var confirmPwd = null;
            for( var i = 0; i < fields.length; i++ ){
                if( fields[i].id.match(/_confirm_password_/) ){
                    confirmPwd = fields[i];
                }else if( fields[i].id.match(/_password_/) ){
                    pwd = fields[i];
                }
            }
            if(pwd.getValue() != confirmPwd.getValue() ){
                pwd.markInvalid();
                confirmPwd.markInvalid();
                return i18n._('Passwords do not match');
            }
            // validate password not empty if onlineAccess checked
            var onlineAccess=Ext.getCmp("add_reports_online_reports_" + suffix );
            if(onlineAccess.getValue() &&  pwd.getValue().length==0) {
                return i18n._("A password must be set to enable Online Access!");
            }
            pwd.clearInvalid();
            confirmPwd.clearInvalid();
            return true;
        };
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            title: i18n._('Status'),
            name: 'Status',
            helpSource: 'reports_status',
            autoScroll: true,
            cls: 'ung-panel',
            items: [this.buildAppStatus(), {
                title: i18n._('Status'),
                xtype: 'fieldset',
                items: [{
                    xtype: 'component',
                    html: i18n._('Click to open the reports in a new window.')
                }, {
                    xtype: 'button',
                    margin: '10 0 0 0',
                    text: i18n._('View Reports'),
                    name: 'View Reports',
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        var viewReportsUrl = "../reports/";
                        window.open(viewReportsUrl);
                    }, this)
                }]
            }]
        });
    },
    // Email panel
    buildEmail: function() {
        var fieldID = "" + Math.round( Math.random() * 1000000 );

        var emailTime=new Date();
        emailTime.setTime(0);
        emailTime.setHours(this.getSettings().generationHour);
        emailTime.setMinutes(this.getSettings().generationMinute);
        
        // Change the password for a user.
        var changePasswordColumn = Ext.create('Ung.grid.EditColumn',{
            header: i18n._("Change Password"),
            width: 130,
            resizable: false,
            iconClass: 'icon-edit-row',
            handler: function(view, rowIndex, colIndex, item, e, record) {
                this.grid.rowEditorChangePassword.populate(record);
                this.grid.rowEditorChangePassword.show();
            }
        });

        this.panelEmail = Ext.create('Ext.panel.Panel',{
            name: 'Email',
            helpSource: 'reports_email',
            title: i18n._('Email'),
            cls: 'ung-panel',
            layout: { type: 'vbox', align: 'stretch' },
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._("Daily Sending Time"),
                labelWidth: 150,
                items: [{
                    xtype: 'timefield',
                    fieldLabel: i18n._("Scheduled time to email report summary"),
                    labelWidth: 260,
                    width: 360,
                    name: 'Email Time',
                    value: emailTime,
                    toValidate: true,
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
                title: i18n._('Email'),
                flex: 1,
                layout: 'fit',
                items: [ this.gridReportsUsers = Ext.create('Ung.grid.Panel',{
                    title: i18n._("Reports Users"),
                    hasEdit: false,
                    settingsCmp: this,
                    plugins:[changePasswordColumn],
                    dataProperty: 'reportsUsers',
                    recordJavaClass: "com.untangle.node.reports.ReportsUser",
                    emptyRow: {
                        emailAddress: "",
                        emailSummaries: true,
                        onlineAccess: false,
                        password: null,
                        passwordHashBase64: null
                    },
                    sortField: "emailAddress",
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
                    columns: [{
                        header: i18n._("Email Address (username)"),
                        dataIndex: "emailAddress",
                        width: 200,
                        editor: {
                            xtype:'textfield',
                            vtype: 'email',
                            emptyText: i18n._("[enter email address]"),
                            allowBlank: false,
                            blankText: i18n._("The email address cannot be blank.")
                        },
                        flex:1
                    }, {
                        xtype:'checkcolumn',
                        header: i18n._("Email Summaries"),
                        dataIndex: "emailSummaries",
                        width: 100,
                        resizable: false
                    }, {
                        xtype:'checkcolumn',
                        header: i18n._("Online Access"),
                        dataIndex: "onlineAccess",
                        width: 100,
                        resizable: false
                    }, changePasswordColumn ],
                    rowEditorInputLines: [{
                        xtype:'textfield',
                        dataIndex: "emailAddress",
                        fieldLabel: i18n._("Email Address (username)"),
                        vtype: 'email',
                        emptyText: i18n._("[enter email address]"),
                        allowBlank: false,
                        blankText: i18n._("The email address name cannot be blank."),
                        width: 300
                    },{
                        xtype:'checkbox',
                        dataIndex: "emailSummaries",
                        fieldLabel: i18n._("Email Summaries"),
                        width: 300
                    },{
                        xtype:'checkbox',
                        dataIndex: "onlineAccess",
                        id: "add_reports_online_reports_" + fieldID,
                        fieldLabel: i18n._("Online Access"),
                        width: 300
                    },{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype:'textfield',
                            inputType: "password",
                            name: "Password",
                            dataIndex: "password",
                            id: "add_reports_user_password_" + fieldID,
                            msgTarget: "title",
                            fieldLabel: i18n._("Password"),
                            width: 300,
                            minLength: 3,
                            minLengthText: Ext.String.format(i18n._("The password is shorter than the minimum {0} characters."), 3),
                            validator: this.passwordValidator
                        },{
                            xtype: 'label',
                            html: i18n._("(required for Online Access)"),
                            cls: 'boxlabel'
                        }]
                    }, {
                        xtype:'textfield',
                        inputType: "password",
                        name: "Confirm Password",
                        dataIndex: "password",
                        id: "add_reports_confirm_password_" + fieldID,
                        fieldLabel: i18n._("Confirm Password"),
                        width: 300,
                        validator: this.passwordValidator
                    }]
                })]
            }]
        });
        /* Create the row editor for updating the password */
        this.gridReportsUsers.rowEditorChangePassword = Ext.create('Ung.RowEditorWindow',{
            grid: this.gridReportsUsers,
            //TODO extjs5 fix positioninig
            width: 300,
            height: 150,
            draggable: true,
            resizable: true,
            ownerCt: this,
            inputLines: [{
                xtype:'textfield',
                inputType: "password",
                name: "Password",
                dataIndex: "password",
                id: "edit_reports_user_password_"  + fieldID,
                fieldLabel: i18n._("Password"),
                width: 300,
                minLength: 3,
                minLengthText: Ext.String.format(i18n._("The password is shorter than the minimum {0} characters."), 3),
                validator: this.passwordValidator
            },
            {
                xtype:'textfield',
                inputType: "password",
                name: "Confirm Password",
                dataIndex: "password",
                id: "edit_reports_confirm_password_"  + fieldID,
                fieldLabel: i18n._("Confirm Password"),
                width: 300,
                validator: this.passwordValidator
            }]
        });
        this.gridReportsUsers.subCmps.push(this.gridReportsUsers.rowEditorChangePassword);
    },
    // syslog panel
    buildSyslog: function() {
        this.panelSyslog = Ext.create('Ext.panel.Panel',{
            name: 'Syslog',
            helpSource: 'reports_syslog',
            title: i18n._('Syslog'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._('Syslog'),
                height: 350,
                items: [{
                    xtype: 'component',
                    margin: '0 0 10 0',
                    html: i18n._('If enabled logged events will be sent in real-time to a remote syslog for custom processing.')
                }, {
                    xtype: 'radio',
                    boxLabel: Ext.String.format(i18n._('{0}Disable{1} Syslog Events. (This is the default setting.)'), '<b>', '</b>'),
                    hideLabel: true,
                    name: 'syslogEnabled',
                    checked: !this.getSettings().syslogEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind( function(elem, checked) {
                                this.getSettings().syslogEnabled = !checked;
                                if (checked) {
                                    this.panelSyslog.down("textfield[name=syslogHost]").disable();
                                    this.panelSyslog.down("numberfield[name=syslogPort]").disable();
                                    this.panelSyslog.down("combo[name=syslogProtocol]").disable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'radio',
                    boxLabel: Ext.String.format(i18n._('{0}Enable{1} Syslog Events.'), '<b>', '</b>'),
                    hideLabel: true,
                    name: 'syslogEnabled',
                    checked: this.getSettings().syslogEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind( function(elem, checked) {
                                this.getSettings().syslogEnabled = checked;
                                if (checked) {
                                    this.panelSyslog.down("textfield[name=syslogHost]").enable();
                                    this.panelSyslog.down("numberfield[name=syslogPort]").enable();
                                    this.panelSyslog.down("combo[name=syslogProtocol]").enable();
                                }
                            }, this)
                        }
                    }
                }, {
                    xtype: 'container',
                    margin: '0 0 0 40',
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: i18n._('Host'),
                        name: 'syslogHost',
                        width: 300,
                        value: this.getSettings().syslogHost,
                        toValidate: true,
                        allowBlank: false,
                        blankText: i18n._("A Host must be specified."),
                        disabled: !this.getSettings().syslogEnabled,
                        validator: Ext.bind( function( value ){
                            if( value == '127.0.0.1' ||
                                value == 'localhost' ){
                                return i18n._("Host cannot be localhost address.");
                            }
                            return true;
                        }, this)
                    },{
                        xtype: 'numberfield',
                        fieldLabel: i18n._('Port'),
                        name: 'syslogPort',
                        width: 200,
                        value: this.getSettings().syslogPort,
                        toValidate: true,
                        allowDecimals: false,
                        minValue: 0,
                        allowBlank: false,
                        blankText: i18n._("You must provide a valid port."),
                        vtype: 'port',
                        disabled: !this.getSettings().syslogEnabled
                    },{
                        xtype: 'combo',
                        name: 'syslogProtocol',
                        editable: false,
                        fieldLabel: i18n._('Protocol'),
                        queryMode: 'local',
                        store: [["UDP", i18n._("UDP")],
                                ["TCP", i18n._("TCP")]],
                        value: this.getSettings().syslogProtocol,
                        disabled: !this.getSettings().syslogEnabled
                    }]
                }]
            }]
        });
    },
    // database panel
    buildData: function() {
        var directoryConnectorLicense;
        try {
            directoryConnectorLicense = Ung.Main.getLicenseManager().isLicenseValid("untangle-node-adconnector") || Ung.Main.getLicenseManager().isLicenseValid("untangle-node-directory-connector");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        var directoryConnectorNode;
        try {
            directoryConnectorNode = rpc.nodeManager.node("untangle-node-directory-connector");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        this.googleDriveConfigured =
            directoryConnectorLicense != null &&
            directoryConnectorLicense &&
            directoryConnectorNode != null &&
            directoryConnectorNode.getGoogleManager() != null &&
            directoryConnectorNode.getGoogleManager().isGoogleDriveConnected();

        this.panelData = Ext.create('Ext.panel.Panel',{
            name: 'Data',
            // helpSource: 'reports_data', //DISABLED
            title: i18n._('Data'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._("Data Retention"),
                labelWidth: 150,
                items: [{
                    xtype: 'component',
                    margin: '0 0 5 0',
                    html: i18n._("Keep event data for this number of days. The smaller the number the lower the disk space requirements and resource usage during report generation.")
                },{
                    xtype: 'numberfield',
                    fieldLabel: i18n._('Data Retention days'),
                    name: 'Data Retention days',
                    id: 'reports_daysToKeepDB',
                    value: this.getSettings().dbRetention,
                    toValidate: true,
                    labelWidth: 150,
                    width: 200,
                    allowDecimals: false,
                    minValue: 1,
                    maxValue: 366,
                    hideTrigger:true,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().dbRetention = newValue;
                            }, this)
                        }
                    }
                }]
            },{
                title: i18n._("Google Drive Backup"),
                labelWidth: 150,
                items: [{
                    xtype: 'container',
                    margin: '5 0 15 20',
                    html: i18n._('If enabled, Configuration Backup uploads backup files to Google Drive.')
                }, {
                    xtype: 'component',
                    html: (this.googleDriveConfigured ? i18n._("The Google Connector is configured.") : i18n._("The Google Connector is unconfigured.")),
                    style: (this.googleDriveConfigured ? {color:'green'} : {color:'red'}),
                    cls: (this.googleDriveConfigured ? null : 'warning')
                }, {
                    xtype: "button",
                    disabled: this.googleDriveConfigured,
                    name: "configureGoogleDrive",
                    text: i18n._("Configure Google Drive"),
                    handler: Ext.bind(this.configureGoogleDrive, this )
                },{
                    xtype: "checkbox",
                    style: {marginTop: '15px'},
                    disabled: !this.googleDriveConfigured,
                    boxLabel: i18n._("Upload to Google Drive"),
                    tooltip: i18n._("If enabled and configured Configuration Backup will upload backups to google drive."),
                    hideLabel: true,
                    checked: this.getSettings().googleDriveEnabled,
                    listeners: {
                        "change": Ext.bind(function(elem, checked) {
                            this.getSettings().googleDriveEnabled = checked;
                        }, this)
                    }
                },{
                    xtype: "textfield",
                    disabled: !this.googleDriveConfigured,
                    regex: /^[\w\. ]+$/,
                    regexText: i18n._("The field can have only alphanumerics, spaces, or periods."),
                    fieldLabel: i18n._("Google Drive Directory"),
                    labelWidth: 150,
                    tooltip: i18n._("The destination directory in google drive."),
                    value: this.getSettings().googleDriveDirectory,
                    listeners: {
                        "change": Ext.bind(function(elem, checked) {
                            this.getSettings().googleDriveDirectory = checked;
                        }, this)
                    }
                }]
            },{
                title: i18n._('Data'),
                height: 350,
                hidden: !rpc.isExpertMode,
                items: [{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Host'),
                    name: 'databaseHost',
                    width: 300,
                    value: this.getSettings().dbHost,
                    allowBlank: false,
                    blankText: i18n._("A Host must be specified."),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().dbHost = newValue;
                        }, this )
                    }
                },{
                    xtype: 'numberfield',
                    fieldLabel: i18n._('Port'),
                    name: 'databasePort',
                    width: 200,
                    value: this.getSettings().dbPort,
                    allowDecimals: false,
                    minValue: 0,
                    allowBlank: false,
                    blankText: i18n._("You must provide a valid port."),
                    vtype: 'port',
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().dbPort = newValue;
                        }, this )
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('User'),
                    name: 'databaseUser',
                    width: 300,
                    value: this.getSettings().dbUser,
                    allowBlank: false,
                    blankText: i18n._("A User must be specified."),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().dbUser = newValue;
                        }, this )
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Password'),
                    name: 'databasePassword',
                    width: 300,
                    value: this.getSettings().dbPassword,
                    allowBlank: false,
                    blankText: i18n._("A Password must be specified."),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getSettings().dbPassword = newValue;
                        }, this )
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Name'),
                    name: 'databaseName',
                    width: 300,
                    value: this.getSettings().dbName,
                    allowBlank: false,
                    blankText: i18n._("A Name must be specified."),
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
        this.gridHostnameMap = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            name: 'Name Map',
            helpSource: 'reports_name_map',
            title: i18n._("Name Map"),
            dataProperty: 'hostnameMap',
            recordJavaClass: "com.untangle.node.reports.ReportsHostnameMapEntry",
            emptyRow: {
                "address": "1.2.3.4",
                "hostname": ""
            },
            fields: [{
                name: 'id'
            }, {
                name: 'address',
                sortType: 'asIp'
            }, {
                name: 'hostname'
            }],
            columns: [{
                header: i18n._("IP Address"),
                width: 200,
                dataIndex: 'address',
                editor: {
                    xtype:'textfield',
                    vtype: 'ipAddress',
                    emptyText: i18n._("[enter IP address]"),
                    allowBlank: false
                }
            }, {
                header: i18n._("Name"),
                width: 200,
                dataIndex: 'hostname',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter name]"),
                    regex: /^[^'"]+$/,
                    regexText: i18n._("Quotes and double quotes are not allowed"),
                    allowBlank: false
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Subnet",
                dataIndex: "address",
                fieldLabel: i18n._("IP Address"),
                emptyText: i18n._("[enter IP address]"),
                vtype: 'ipAddress',
                allowBlank: false,
                width: 300
            },{
                xtype:'textfield',
                name: "Name",
                dataIndex: "hostname",
                fieldLabel: i18n._("Name"),
                emptyText: i18n._("[enter name]"),
                regex: /^[^'"]+$/,
                regexText: i18n._("Quotes and double quotes are not allowed"),
                allowBlank: false,
                width: 300
            }]
        });
    },
    // Manage Reports Panel
    buildReportEntries: function() {
        var chartTypes = [["TEXT", i18n._("Text")],["PIE_GRAPH", i18n._("Pie Graph")],["TIME_GRAPH", i18n._("Time Graph")],["TIME_GRAPH_DYNAMIC", i18n._("Time Graph Dynamic")]];
        var chartTypeMap = Ung.Util.createStoreMap(chartTypes);
        var chartTypeRenderer = function(value) {
            return chartTypeMap[value]?chartTypeMap[value]:value;
        };
            this.gridReportEntries= Ext.create('Ung.grid.Panel',{
                name: 'Manage Reports',
                helpSource: 'reports_manage_reports',
                settingsCmp: this,
                hasReadOnly: true,
                changableFields: ['enabled'],
                title: i18n._("Manage Reports"),
                features: [{
                    ftype: 'grouping'
                }],
                groupField: 'category',
                recordJavaClass: "com.untangle.node.reports.ReportEntry",
                emptyRow: {
                    "uniqueId": null,
                    "enabled": true,
                    "readOnly": false,
                    "displayOrder": 500,
                    "type": "PIE_GRAPH"
                },
                dataProperty: "reportEntries",
                sortField: 'displayOrder',
                columnsDefaultSortable: false,
                fields: ['uniqueId', 'enabled', 'readOnly', 'type', 'title', 'category', 'description', 'displayOrder', 'units', 'table', 'conditions', 
                         'pieGroupColumn', 'pieSumColumn', 'timeDataInterval', 'timeDataColumns', 'orderByColumn', 'orderDesc', 'javaClass'],
                columns: [{
                    header: i18n._("Title"),
                    width: 230,
                    dataIndex: 'title'
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Enabled"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 55
                }, {
                    header: i18n._("Type"),
                    width: 110,
                    dataIndex: 'type',
                    renderer: chartTypeRenderer
                }, {
                    header: i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1
                }, {
                    header: i18n._("Units"),
                    width: 90,
                    dataIndex: 'units'
                }, {
                    header: i18n._("Display Order"),
                    width: 90,
                    dataIndex: 'displayOrder'
                }, {
                    header: i18n._("View"),
                    xtype: 'actioncolumn',
                    width: 70,
                    items: [{
                        iconCls: 'icon-play-row',
                        tooltip: i18n._('View Report'),
                        handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                            this.viewReport(Ext.clone(record.getData()));
                        }, this)
                    }]
                }]
            });
        this.gridReportEntries.setRowEditor(Ext.create('Ung.window.ReportEditor', {
            parentCmp: this
        }));
    },
    viewReport: function(reportEntry) {
        if(!this.winViewReport) {
            this.winViewReport = Ext.create('Ung.Window', {
                title: i18n._('View Report'),
                bbar: ['->', {
                    name: "Cancel",
                    iconCls: 'cancel-icon',
                    text: i18n._('Cancel'),
                    handler: function() {
                        this.up('window').cancelAction();
                    }
                }," "],
                items: Ext.create('Ung.panel.Reports',{
                    width: 1000,
                    height: 600,
                    hasEntriesSection: false
                }),
                listeners: {
                    "hide": {
                        fn: function() {
                            var panelReports = this.down('panel[name=panelReports]');
                            if(panelReports.autoRefreshEnabled) {
                                panelReports.stopAutoRefresh(true);
                            }
                        }
                    }
                }
            });
            this.subCmps.push(this.winViewReport);
        }
        this.winViewReport.show();
        this.winViewReport.down('panel[name=panelReports]').loadReportEntry(reportEntry);
    },
    // AlertRules Panel
    buildAlertRules: function() {
        this.panelAlertRules = Ext.create('Ext.panel.Panel',{
            name: 'alertRules',
            helpSource: 'reports_alert_rules',
            title: i18n._('Alert Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('Note'),
                flex: 0,
                html: i18n._(" <b>Alert Rules</b> process all events to log and/or alert administrators when special or noteworthy events occur.")
            },  this.gridAlertRules= Ext.create('Ung.grid.Panel',{
                flex: 1,
                name: 'Alert Rules',
                settingsCmp: this,
                hasReorder: true,
                addAtTop: false,
                title: i18n._("Alert Rules"),
                dataProperty:'alertRules',
                recordJavaClass: "com.untangle.node.reports.AlertRule",
                emptyRow: {
                    "ruleId": null,
                    "enabled": true,
                    "log": false,
                    "alert": false,
                    "alertLimitFrequency": false,
                    "alertLimitFrequencyMinutes": 0,
                    "description": ""
                },
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'log'
                }, {
                    name: 'alert'
                }, {
                    name: 'alertLimitFrequency'
                }, {
                    name: 'alertLimitFrequencyMinutes'
                }, {
                    name: 'thresholdEnabled'
                }, {
                    name: 'thresholdLimit'
                }, {
                    name: 'thresholdTimeframeSec'
                }, {
                    name: 'thresholdGroupingField'
                }, {
                    name: 'conditions'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Log Alert"),
                    dataIndex: 'log',
                    width:150
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Send Alert"),
                    dataIndex: 'alert',
                    width:150
                }]
            })]
        });
        this.gridAlertRules.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            inputLines: [{
                xtype:'checkbox',
                name: "Enable Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Rule")
            }, {
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.node.reports.AlertRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getAlertRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('And the following conditions:'),
                items:[{
                    xtype:'checkbox',
                    labelWidth: 160,
                    dataIndex: "thresholdEnabled",
                    fieldLabel: i18n._("Enable Thresholds"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridAlertRules.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                },{
                    xtype:'fieldset',
                    collapsible: false,
                    items: [{
                        xtype:'numberfield',
                        labelWidth: 160,
                        width: 230,
                        dataIndex: "thresholdLimit",
                        fieldLabel: i18n._("Exceeds Threshold Limit")
                    },{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'numberfield',
                            labelWidth: 160,
                            width: 230,
                            dataIndex: "thresholdTimeframeSec",
                            allowDecimals: false,
                            allowBlank: false,
                            minValue: 60,
                            maxValue: 60*24*60*7, // 1 week
                            fieldLabel: i18n._('Over Timeframe')
                        }, {
                            xtype: 'label',
                            html: i18n._("(seconds)"),
                            cls: 'boxlabel'
                        }]
                    },{
                        xtype:'textfield',
                        labelWidth: 160,
                        dataIndex: "thresholdGroupingField",
                        fieldLabel: i18n._("Grouping Field")
                    }]
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items:[{
                    xtype:'checkbox',
                    labelWidth: 160,
                    dataIndex: "log",
                    fieldLabel: i18n._("Log Alert")
                }, {
                    xtype:'checkbox',
                    labelWidth: 160,
                    dataIndex: "alert",
                    fieldLabel: i18n._("Send Alert"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridAlertRules.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                },{
                    xtype:'fieldset',
                    collapsible: false,
                    items: [{
                        xtype:'checkbox',
                        labelWidth: 160,
                        dataIndex: "alertLimitFrequency",
                        fieldLabel: i18n._("Limit Send Frequency")
                    },{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'numberfield',
                            labelWidth: 160,
                            width: 230,
                            dataIndex: "alertLimitFrequencyMinutes",
                            allowDecimals: false,
                            allowBlank: false,
                            minValue: 0,
                            maxValue: 24*60*7, // 1 weeks
                            fieldLabel: i18n._('To once per')
                        }, {
                            xtype: 'label',
                            html: i18n._("(minutes)"),
                            cls: 'boxlabel'
                        }]
                    }]
                }]
            }],
            syncComponents: function () {
                var sendAlert=this.down('checkbox[dataIndex=alert]').getValue();
                this.down('checkbox[dataIndex=alertLimitFrequency]').setDisabled(!sendAlert);
                this.down('numberfield[dataIndex=alertLimitFrequencyMinutes]').setDisabled(!sendAlert);

                var thresholdEnabled=this.down('checkbox[dataIndex=thresholdEnabled]').getValue();
                this.down('numberfield[dataIndex=thresholdLimit]').setDisabled(!thresholdEnabled);
                this.down('numberfield[dataIndex=thresholdTimeframeSec]').setDisabled(!thresholdEnabled);
                this.down('textfield[dataIndex=thresholdGroupingField]').setDisabled(!thresholdEnabled);            }
        }));
    },
    beforeSave: function(isApply,handler) {
        this.getSettings().reportsUsers.list = this.gridReportsUsers.getList();
        this.getSettings().hostnameMap.list = this.gridHostnameMap.getList();
        this.getSettings().reportEntries.list = this.gridReportEntries.getList();
        //console.log(this.getSettings().reportEntries.list);
        this.getSettings().alertRules.list = this.gridAlertRules.getList();

        this.getSettings().syslogHost = this.panelSyslog.down("textfield[name=syslogHost]").getValue();
        this.getSettings().syslogPort = this.panelSyslog.down("numberfield[name=syslogPort]").getValue();
        this.getSettings().syslogProtocol = this.panelSyslog.down("combo[name=syslogProtocol]").getValue();

        handler.call(this, isApply);
    },
    validate: function () {
        var components = this.query("component[toValidate]");
        return this.validateComponents(components);
    },
    // There is no way to select the google tab because we don't get a callback once the settings are loaded.
    configureGoogleDrive: function() {
        var node = Ung.Main.getNode("untangle-node-directory-connector");
        if (node != null) {
            var nodeCmp = Ung.Node.getCmp(node.nodeId);
            if (nodeCmp != null) {
                nodeCmp.loadSettings();
            }
        }
    }
});
//# sourceURL=reports-settings.js