
Ext.define('Webui.untangle-node-reports.settings', {
    extend:'Ung.NodeWin',
    panelUsers: null,
    panelSyslog: null,
    panelData: null,
    gridReportsUsers: null,
    gridHostnameMap: null,
    gridReportEntries: null,
    gridAlertEventLog: null,
    gridEmailTemplates: null,

    // pull from backend
    configCategories: ['Hosts', 'Devices', 'Network', 'Administration', 'System', 'Shield'],
    appCategories: [],
    chartTypeMap: Ung.Util.createStoreMap([
            ["TEXT", i18n._("Text")],
            ["PIE_GRAPH", i18n._("Pie Graph")],
            ["TIME_GRAPH", i18n._("Time Graph")],
            ["TIME_GRAPH_DYNAMIC", i18n._("Time Graph Dynamic")],
            ["EVENT_LIST", i18n._("Event List")]
    ]),
    emailChartTypes: ["TEXT", "PIE_GRAPH", "TIME_GRAPH", "TIME_GRAPH_DYNAMIC"],
    emailRecommendedReportIds: [],
    emailIntervals: [
        [86400, i18n._("Daily")],
        [604800, i18n._("Weekly")],
        [2419200, i18n._("Monthly")]
    ],
    fixedReportsAllowGraphs: true,
    getAppSummary: function() {
        return i18n._("Reports records network events to provide administrators the visibility and data necessary to investigate network activity.");
    },
    initComponent: function(container, position) {
        this.buildPasswordValidator();
        this.buildUsers();
        this.buildSyslog();
        this.buildHostnameMap();
        this.buildReportEntries();
        this.buildAlertRules();
        this.buildData();
        this.buildEmailTemplates();

        var panels = [this.gridReportEntries, this.panelData, this.panelAlertRules, this.panelEmailTemplates, this.panelUsers, this.panelSyslog, this.gridHostnameMap ];

        this.buildTabPanel(panels);

        this.callParent(arguments);
    },

    listeners: {
        beforeshow: function () {
            this.buildAvailableAppCategories();
            this.buildFixedReportsAllowGraphs();
            this.buildRecommendedReportIds();
        }
    },

    /*
     * From array of report identifiers, return comma separated string of report titles.
     */
    reportIdToNameRenderer: function(value, metaData){
        var reportNames = [];
        var allAdded = false;
        var typeAdded = [];
        for(var i = 0; i < value.list.length; i++){
            for(var j = 0; j < this.settings["reportEntries"].list.length; j++){
                var entry = this.settings["reportEntries"].list[j];
                if(value.list[i] == entry.uniqueId){
                    reportNames.push(entry.title);
                }else if(
                    (value.list.indexOf("_recommended") > -1) &&
                    (allAdded == false)){
                    /* Special case: _recommended */
                    reportNames.push(i18n._("Recommended"));
                    allAdded = true;
                }
            }
        }
        if(reportNames.length == 0){
            reportNames.push(i18n._("None"));
        }
        value = reportNames.join(", ");
        metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(value) + '"';
        return value;
    },
    /*
     * From chart identifier, return human-readable name.
     */
    chartTypeRenderer: function(value) {
        return this.chartTypeMap[value] ? this.chartTypeMap[value] : value;
    },
    viewRenderer: function (value, metaData, record) {
        if ((this.configCategories.indexOf(record.getData().category) < 0) &&
            (this.appCategories.indexOf(record.getData().category) < 0)) {
            return '<div style="font-size: 10px; line-height: 1; text-align: center; color: coral;">not installed</div>';
        }
        return '<div style="text-align: center;"><i role="button" class="x-action-col-icon x-action-col-0 material-icons" style="font-size: 16px;">visibility</i></div>';
    },
    chartRenderer: function (value) {
        if ((this.configCategories.indexOf(value) < 0) &&
            (this.appCategories.indexOf(value) < 0)) {
            return value + ' (<span style="color: coral;">' + i18n._('not installed') + '</span>)';
        }
        return value;
    },
    /*
     * Build array of category names based on whether applications are installed.
     */
    buildAvailableAppCategories: function() {
        this.appCategories = [];
        rpc.reportsManager.getCurrentApplications(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            for (var i = 0; i < result.list.length; i++) {
                this.appCategories.push(result.list[i].displayName);
            }
        }, this));
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
    buildFixedReportsAllowGraphs: function(){
        rpc.reportsManager.fixedReportsAllowGraphs(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.fixedReportsAllowGraphs = result;
        }, this));
    },
    buildRecommendedReportIds: function(){
        rpc.reportsManager.getRecommendedReportIds(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.emailRecommendedReportIds = result.list;
        }, this));
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
            pwd.clearInvalid();
            confirmPwd.clearInvalid();
            return true;
        };
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ung.panel.Status', {
            settingsCmp: this,
            helpSource: 'reports_status',
            itemsToAppend: [{
                title: i18n._('View Reports'),
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
    // Users panel
    buildUsers: function() {
        var fieldID = "" + Math.round( Math.random() * 1000000 );

        for(var i = 0; i < this.getSettings().reportsUsers.list.length; i++){
            if(this.getSettings().reportsUsers.list[i].emailAddress == "admin"){
                this.getSettings().reportsUsers.list[i].readOnly = true;
            }
        }

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

        this.panelUsers = Ext.create('Ext.panel.Panel',{
            name: 'Users',
            helpSource: 'reports_users',
            title: i18n._('Users'),
            cls: 'ung-panel',
            layout: { type: 'vbox', align: 'stretch' },
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._('Reports Users'),
                items: [{
                    xtype: 'component',
                    margin: '0 0 10 0',
                    html: i18n._('Reports Users are users that can view reports and receive email reports but do not have administration privileges.')
                }, this.gridReportsUsers = Ext.create('Ung.grid.Panel',{
                    title: i18n._("Reports Users"),
                    height: 350,
                    hasEdit: true,
                    hasReadOnly: true,
                    settingsCmp: this,
                    plugins:[changePasswordColumn],
                    dataProperty: 'reportsUsers',
                    recordJavaClass: "com.untangle.node.reports.ReportsUser",
                    emptyRow: {
                        emailAddress: "",
                        emailAlerts: true,
                        emailSummaries: true,
                        emailTemplateIds : {
                            javaClass: "java.util.LinkedList",
                            list: [1]
                        },
                        onlineAccess: false,
                        password: null,
                        passwordHashBase64: null
                    },
                    sortField: "emailAddress",
                    fields: [{
                        name: "emailAddress"
                    },{
                        name: "emailAlerts"
                    },{
                        name: "emailSummaries"
                    },{
                        name: "emailTemplateIds",
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
                        header: i18n._("Email Alerts"),
                        dataIndex: "emailAlerts",
                        width: 100,
                        resizable: false
                    }, {
                        xtype:'checkcolumn',
                        header: i18n._("Email Reports"),
                        dataIndex: "emailSummaries",
                        width: 100,
                        resizable: false
                    }, {
                        header: i18n._("Email Templates"),
                        dataIndex: "emailTemplateIds",
                        width: 150,
                        resizable: false,
                        renderer: Ext.bind(function(value){
                            var titles = [];
                            for(var i = 0; i < this.settings["emailTemplates"].list.length; i++){
                                var template = this.settings["emailTemplates"].list[i];
                                if(value.list.indexOf(template.templateId) > -1){
                                    titles.push(template.title);
                                }
                            }
                            return titles.join(", ");
                        }, this)
                    }, {
                        xtype:'checkcolumn',
                        header: i18n._("Online Access"),
                        dataIndex: "onlineAccess",
                        width: 100,
                        resizable: false
                    },
                    changePasswordColumn
                    ]
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

        this.gridReportsUsers.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            name: "edit",
            pageOwner: this,
            inputLines: [{
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
                dataIndex: "emailAlerts",
                fieldLabel: i18n._("Email Alerts"),
                width: 300
            },{
                xtype:'checkbox',
                dataIndex: "emailSummaries",
                fieldLabel: i18n._("Email Reports"),
                width: 300
            },{
                xtype: 'checkboxgroup',
                name: 'emailTemplates',
                dataIndex: "emailTemplateIds",
                fieldLabel: i18n._("Email Templates"),
                columns: 1,
                vertical: true,
                items:[],
                getValue: function(){
                    var values = {
                        "javaClass": "java.util.LinkedList",
                        "list": []
                    };
                    this.items.each(function(item){
                        if(item.getValue() == true){
                            values.list.push(item.inputValue);
                        }
                    });
                    return values;
                }
            }],
            syncComponents: function () {
                /* Rebuild email template checkbox group with available template */
                var settings = this.pageOwner.settings;
                var emailTemplatesCheckboxes = this.down("[name=emailTemplates]");
                var items = emailTemplatesCheckboxes.items;
                var userEmailTemplates = this.record.get("emailTemplateIds") || {list:[]};
                emailTemplatesCheckboxes.removeAll();
                for(var i = 0; i < settings["emailTemplates"].list.length; i++){
                    var template = settings["emailTemplates"].list[i];
                    var templateChecked = (userEmailTemplates.list.indexOf(template.templateId) > -1) ? true : false;
                    items.add(new Ext.form.Checkbox({
                        boxLabel: template.title + " (" + template.description + ")",
                        name: "list",
                        inputValue: template.templateId,
                        checked: templateChecked
                    }));
                }
            },
        }));
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
                    xtype: 'component',
                    html: i18n._('Warning: Syslog logging can be computationally expensive for servers processing millions of events. Caution is advised.'),
                    cls: 'warning',
                    margin: '0 0 10 10'
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
        this.googleDriveConfigured = Ung.Main.isGoogleDriveConfigured();

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
                    html: i18n._("Keep event data for this number of days. The smaller the number the lower the disk space requirements.")
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
                    margin: '5 0 15 0',
                    html: i18n._('If enabled, Configuration Backup uploads reports data backup files to Google Drive.')
                }, {
                    xtype: 'component',
                    html: (this.googleDriveConfigured ? i18n._("The Google Connector is configured.") : i18n._("The Google Connector is unconfigured.")),
                    style: (this.googleDriveConfigured ? {color:'green'} : {color:'red'}),
                    cls: (this.googleDriveConfigured ? null : 'warning')
                }, {
                    xtype: "button",
                    margin: '10 0 15 0',
                    disabled: this.googleDriveConfigured,
                    text: i18n._("Configure Google Drive"),
                    handler: Ung.Main.configureGoogleDrive
                },{
                    xtype: "checkbox",
                    disabled: !this.googleDriveConfigured,
                    boxLabel: i18n._("Upload Data to Google Drive"),
                    hideLabel: true,
                    checked: this.getSettings().googleDriveUploadData,
                    listeners: {
                        "change": Ext.bind(function(elem, checked) {
                            this.getSettings().googleDriveUploadData = checked;
                        }, this),
                        "render": function(obj) {
                            obj.getEl().set({'data-qtip': i18n._("If enabled and configured Configuration Backup will upload backups to google drive.")});
                        }
                    }
                },{
                    xtype: "checkbox",
                    disabled: !this.googleDriveConfigured,
                    boxLabel: i18n._("Upload CSVs to Google Drive"),
                    hideLabel: true,
                    checked: this.getSettings().googleDriveUploadCsv,
                    listeners: {
                        "change": Ext.bind(function(elem, checked) {
                            this.getSettings().googleDriveUploadCsv = checked;
                        }, this),
                        "render": function(obj) {
                            obj.getEl().set({'data-qtip': i18n._("If enabled and configured Configuration Backup will upload backups to google drive.")});
                        }
                    }
                },{
                    xtype: "textfield",
                    disabled: !this.googleDriveConfigured,
                    regex: /^[\w\. \/]+$/,
                    regexText: i18n._("The field can have only alphanumerics, spaces, or periods."),
                    fieldLabel: i18n._("Google Drive Directory"),
                    labelWidth: 150,
                    value: this.getSettings().googleDriveDirectory,
                    listeners: {
                        "change": Ext.bind(function(elem, checked) {
                            this.getSettings().googleDriveDirectory = checked;
                        }, this),
                        "render": function(obj) {
                            obj.getEl().set({'data-qtip': i18n._("The destination directory in google drive.")});
                        }
                    }
                }]
            },{
                title: i18n._("Import / Restore Data Backup Files"),
                labelWidth: 150,
                items: [{
                    xtype: 'form',
                    name: 'uploadDataForm',
                    url: 'upload',
                    border: false,
                    items: [{
                        xtype: 'filefield',
                        fieldLabel: i18n._('File'),
                        name: 'uploadDataFile',
                        width: 500,
                        labelWidth: 50,
                        allowBlank: false,
                        validateOnBlur: false
                    },{
                        xtype: 'button',
                        text: i18n._("Upload"),
                        handler: Ext.bind(function() {
                            this.panelData.onUpload();
                        }, this)
                    },{
                        xtype: 'hidden',
                        name: 'type',
                        value: 'reportsDataRestore'
                    }]
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
            }],
            onUpload: Ext.bind(function() {
                var form = this.panelData.down('form[name="uploadDataForm"]');
                form.submit({
                    waitMsg: i18n._('Please wait while data is imported...'),
                    success: Ext.bind(function( form, action ) {
                        var filefield = this.panelData.down('filefield[name="uploadDataFile"]');
                        if ( filefield) {
                            filefield.reset();
                        }
                        Ext.MessageBox.alert( i18n._("Succeeded"), i18n._("Upload Data Succeeded"));
                    }, this ),
                    failure: Ext.bind(function( form, action ) {
                        var errorMsg = i18n._("Upload Data Failed") + " " + action.result;
                        Ext.MessageBox.alert(i18n._("Failed"), errorMsg);
                    }, this )
                });
            }, this)
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
        this.gridReportEntries = Ext.create('Ung.grid.Panel',{
            name: 'All Reports',
            helpSource: 'reports_manage_reports',
            settingsCmp: this,
            hasReadOnly: true,
            hasAdd: false,
            hasEdit: true,
            hasImportExport: true,
            changableFields: ['enabled'],
            title: i18n._("All Reports"),
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
                "pieStyle": "PIE",
                "type": "PIE_GRAPH"
            },
            dataProperty: "reportEntries",
            sortField: 'displayOrder',
            columnsDefaultSortable: false,
            fields: ['uniqueId', 'enabled', 'readOnly', 'type', 'title', 'category', 'description', 'displayOrder', 'units', 'table', 'conditions',
                'pieGroupColumn', 'pieSumColumn', 'timeDataInterval', 'timeDataColumns', 'orderByColumn', 'orderDesc', 'javaClass'],
            //filterFields: ['title', 'description', 'units', 'displayOrder'],
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
                renderer: Ext.bind( this.chartTypeRenderer, this)
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
                defaultRenderer: Ext.bind(this.viewRenderer, this),
                handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                    this.viewReport(Ext.clone(record.getData()));
                }, this)
            }, {
                header: i18n._("Category"),
                dataIndex: 'category',
                hidden: true,
                renderer: Ext.bind(this.chartRenderer, this)
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
                    name: "Done",
                    iconCls: 'save-icon',
                    text: i18n._('Done'),
                    handler: function() {
                        this.up('window').cancelAction();
                    }
                }," "],
                items: Ext.create('Ung.panel.Reports', {
                    initEntry: reportEntry,
                    hideCustomization: true,
                    responsiveFormulas: {
                        insideSettingsWin: function() {
                            return true;
                        }
                    }
                }),
                listeners: {
                    "hide": {
                        fn: function() {
                            var panelReports = this.down('[name=panelReports]');
                            if (panelReports.autoRefreshEnabled) {
                                panelReports.stopAutoRefresh(true);
                            }
                        }
                    }
                }
            });
            this.subCmps.push(this.winViewReport);
        } else {
            var panelReports = this.winViewReport.down('[name=panelReports]');
            panelReports.categoryList.getSelectionModel().deselectAll();
            panelReports.categoryList.getSelectionModel().select(panelReports.categoryList.getStore().findRecord('category', reportEntry.category));
            panelReports.entryList.getSelectionModel().select(panelReports.entryList.getStore().findRecord('entryId', reportEntry.uniqueId));
        }
        this.winViewReport.show();
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
                html: " " + i18n._("<b>Alert Rules</b> process all events to log and/or alert administrators when special or noteworthy events occur.")
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
                this.down('textfield[dataIndex=thresholdGroupingField]').setDisabled(!thresholdEnabled);
            }
        }));
    },
    // Email Templates Panel
    buildEmailReportGrid: function(dataIndex){
        return Ext.create('Ung.grid.Panel',{
            helpSource: 'reports_manage_reports',
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasImportExport: false,
            changableFields: ['enabled'],
            // hasReadOnly: true,
            // hasReorder: true,
            features: [{
                ftype: 'grouping'
            }],
            groupField: 'category',
            recordJavaClass: "com.untangle.node.reports.EmailReportEntry",
            javaClass: "com.untangle.node.reports.EmailReport",
            dataIndex: dataIndex,
            /*
             * Build rows dynamically from existing reports using list of unique report identifers
             * to determien whether enabled flag should be set or not.
             */
            dataFn: function(handler){
                var settings = this.settingsCmp.settings;

                var results = {
                    javaClass: "com.untangle.node.reports.ReportEntry",
                    list: []
                };

                var enabledIds = this.up("[name=edit]").record.get(this.dataIndex);
                var categories = this.dataIndex.match(/config/i) ? this.settingsCmp.configCategories : this.settingsCmp.appCategories;
                for(var i = 0; i < settings["reportEntries"].list.length; i++){
                    var entry = settings["reportEntries"].list[i];

                    if(this.settingsCmp.fixedReportsAllowGraphs == false &&
                        entry.type.indexOf("_GRAPH") != -1){
                        continue;
                    }
                    if((categories.indexOf(entry.category) != -1) &&
                        (this.settingsCmp.emailChartTypes.indexOf(entry.type) != -1)){

                        var enabled = false;
                        if( typeof(enabledIds) != "undefined" ){
                            if(enabledIds.list.indexOf(entry.uniqueId) > -1){
                                /* Standard checkbox */
                                enabled = true;
                            }else if(enabledIds.list.indexOf("_recommended") > -1 &&
                                this.settingsCmp.emailRecommendedReportIds.indexOf(entry.uniqueId) > -1 ){
                                /* Special case: _recommended */
                                enabled = true;
                            }
                        }

                        entry.enabled = enabled;
                        results.list.push(entry);
                    }
                }

                handler(results);
            },
            getValue: function(){
                var enabledUniqueIds = {
                    "javaClass": "java.util.LinkedList",
                    "list": []
                };
                this.getStore().each(function(record){
                    if(record.get("enabled") == true){
                        enabledUniqueIds.list.push(record.get("uniqueId"));
                    }
                });
                return enabledUniqueIds;
            },
            setValue: function(incomingRecords, record){
                record.set(this.dataIndex, incomingRecords);
            },
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
                renderer: Ext.bind( this.chartTypeRenderer, this)
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
                header: i18n._("View"),
                xtype: 'actioncolumn',
                width: 70,
                defaultRenderer: Ext.bind(this.viewRenderer, this),
                handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                    this.viewReport(Ext.clone(record.getData()));
                }, this)
            }, {
                header: i18n._("Category"),
                dataIndex: 'category',
                hidden: true,
                renderer: Ext.bind(this.chartRenderer, this)
            }]
        });
    },
    buildEmailTemplates: function() {
        var emailIntervals = [];
        for(var i = 0; i < this.emailIntervals.length; i++){
            if(this.emailIntervals[i][0] <= (this.getSettings().dbRetention * 86400)){
                emailIntervals.push(this.emailIntervals[i]);
            }
        }

        this.panelEmailTemplates = Ext.create('Ext.panel.Panel',{
            name: 'emailTemplates',
            helpSource: 'reports_email_templates',
            title: i18n._('Email Templates'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('Note'),
                flex: 0,
                html: " " + i18n._("<b>Email Templates</b> must be associated with Users.")
            },  this.gridEmailTemplates = Ext.create('Ung.grid.Panel',{
                flex: 1,
                name: 'Email Templates',
                settingsCmp: this,
                hasCopy: true,
                copyField: 'title',
                addAtTop: false,
                title: i18n._("Email Templates"),
                hasReadOnly: true,
                dataProperty:'emailTemplates',
                recordJavaClass: "com.untangle.node.reports.EmailTemplate",
                emptyRow: {
                    "templateId": -1,
                    "title": "",
                    "description": "",
                    "interval": 86400,
                    "intervalWeekStart": 1,
                    "readOnly": false,
                    "enabledConfigIds" : {
                        "javaClass": "java.util.LinkedList",
                        "list": []

                    },
                    "enabledAppIds" : {
                        "javaClass": "java.util.LinkedList",
                        "list": []
                    }
                },
                fields: [{
                    name: 'templateId'
                }, {
                    name: 'title'
                }, {
                    name: 'description'
                }, {
                    name: 'interval'
                }, {
                    name: 'intervalWeekStart'
                }, {
                    name: 'mobile'
                }, {
                    name: 'readOnly'
                }, {
                    name: 'enabledConfigIds'
                }, {
                    name: 'enabledAppIds'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: i18n._("Template Id"),
                    width: 65,
                    dataIndex: 'templateId',
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    header: i18n._("Title"),
                    width: 200,
                    dataIndex: 'title',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        emptyText: i18n._("[no title]"),
                        allowBlank: false,
                        blankText: i18n._("The title cannot be blank.")
                    },
                }, {
                    header: i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        emptyText: i18n._("[no description]"),
                        allowBlank: false,
                        blankText: i18n._("The description cannot be blank.")
                    },
                }, {
                    header: i18n._("Interval"),
                    width: 200,
                    dataIndex: 'interval',
                    flex: 1,
                    editor: {
                        xtype: 'combo',
                        editable: false,
                        queryMode: 'local',
                        store: this.emailIntervals,
                        dataIndex: 'interval'
                    },
                    renderer: Ext.bind(function(value, metaData){
                        for(var i = 0; i < this.emailIntervals.length;i++){
                            if(this.emailIntervals[i][0] == value){
                                return this.emailIntervals[i][1];
                            }
                        }
                        return value;
                    }, this)
                }, {
                    header: i18n._("Mobile"),
                    width: 50,
                    dataIndex: 'mobile',
                    flex: 1,
                    editor: {
                        xtype: 'checkbox',
                        editable: false,
                        dataIndex: 'mobile'
                    },
                    renderer: Ext.bind(function(value, metaData){
                        value = ( value == true ? i18n._("Yes") : i18n._("No"));
                        return value;
                    }, this)
                }, {
                    header: i18n._("Config"),
                    width: 200,
                    dataIndex: 'enabledConfigIds',
                    flex: 1,
                    renderer: Ext.bind(this.reportIdToNameRenderer, this)
                }, {
                    header: i18n._("Apps"),
                    width: 200,
                    dataIndex: 'enabledAppIds',
                    flex: 1,
                    renderer: Ext.bind(this.reportIdToNameRenderer, this)
                }]
            })]
        });
        this.gridEmailTemplates.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            name: "edit",
            pageOwner: this,
            inputLines: [{
                xtype:'textfield',
                name: "Title",
                dataIndex: "title",
                fieldLabel: i18n._("Title"),
                emptyText: i18n._("[no title]"),
                width: 500
            },{
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            },{
                xtype: 'container',
                layout: 'hbox',
                items: [{
                    xtype: 'combo',
                    name: 'Interval',
                    editable: false,
                    fieldLabel: i18n._("Interval"),
                    queryMode: 'local',
                    store: emailIntervals,
                    dataIndex: 'interval',
                    listeners: {
                        "change": {
                            fn: Ext.bind( function(combo, newValue){
                                combo.up("[name=edit]").down("[name=IntervalWeekStart]").setVisible(newValue ==604800);
                            }, this)
                        },
                    }
                },{
                    xtype: 'combo',
                    name: 'IntervalWeekStart',
                    editable: false,
                    fieldLabel: i18n._("Start of week"),
                    queryMode: 'local',
                    store: Ung.Util.getDayOfWeekList(),
                    dataIndex: 'intervalWeekStart',
                    margin: '0 0 0 10',
                    hidden: true
                }]
            },{
                xtype: 'checkbox',
                name: 'Mobile',
                editable: false,
                fieldLabel: i18n._("Mobile"),
                dataIndex: 'mobile'
            }, {
                xtype: 'label',
                html: i18n._('Config'),
            },
            this.buildEmailReportGrid("enabledConfigIds"),
            {
                xtype: 'label',
                html: i18n._('Apps'),
            },
            this.buildEmailReportGrid("enabledAppIds")
            ],
            syncComponents: function () {
                this.down("[dataIndex=enabledConfigIds]").clearDirty();
                this.down("[dataIndex=enabledAppIds]").clearDirty();
                this.down("[name=IntervalWeekStart]").setVisible(this.down("[name=Interval]").getValue() == 604800);
            },
        }));
    },
    beforeSave: function(isApply,handler) {
        this.getSettings().reportsUsers.list = this.gridReportsUsers.getList();
        this.getSettings().hostnameMap.list = this.gridHostnameMap.getList();
        this.getSettings().reportEntries.list = this.gridReportEntries.getList();
        this.getSettings().alertRules.list = this.gridAlertRules.getList();
        this.getSettings().emailTemplates.list = this.gridEmailTemplates.getList();

        this.getSettings().syslogHost = this.panelSyslog.down("textfield[name=syslogHost]").getValue();
        this.getSettings().syslogPort = this.panelSyslog.down("numberfield[name=syslogPort]").getValue();
        this.getSettings().syslogProtocol = this.panelSyslog.down("combo[name=syslogProtocol]").getValue();

        // when saving set the dashboard flag
        Ung.dashboard.reportEntriesModified = true;

        handler.call(this, isApply);
    },
    validate: function () {
        var components = this.query("component[toValidate]");
        return this.validateComponents(components);
    }
});
//# sourceURL=reports-settings.js
