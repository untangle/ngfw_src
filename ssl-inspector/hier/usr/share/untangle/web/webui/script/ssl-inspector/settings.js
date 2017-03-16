// SSL Inspector Settings
Ext.define('Webui.ssl-inspector.settings', {
    extend: 'Ung.AppWin',
    panelAppConfiguration: null,
    gridTrustList: null,
    gridIgnoreRules: null,
    gridEventLog: null,
    getAppSummary: function() {
        var certStatus = Ung.Main.getCertificateManager().validateActiveInspectorCertificates();
        return i18n._("SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrytped streams.<BR><BR>" + certStatus);
    },
    initComponent: function() {
        this.buildPanelConfiguration();
        this.buildGridIgnoreRules();

        this.buildTabPanel([this.panelAppConfiguration, this.gridIgnoreRules]);
        this.callParent(arguments);
    },
    getConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any", i18n._("any")]], visible: true},
            {name:"USERNAME",displayName: i18n._("Username"), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
            {name:"CLIENT_HOSTNAME",displayName: i18n._("Client Hostname"), type: "text", visible: true},
            {name:"SERVER_HOSTNAME",displayName: i18n._("Server Hostname"), type: "text", visible: true},
            {name:"SRC_MAC", displayName: i18n._("Client MAC Address"), type: "text", visible: true },
            {name:"DST_MAC", displayName: i18n._("Server MAC Address"), type: "text", visible: true },
            {name:"CLIENT_MAC_VENDOR",displayName: i18n._("Client MAC Vendor"), type: "text", visible: true},
            {name:"SERVER_MAC_VENDOR",displayName: i18n._("Server MAC Vendor"), type: "text", visible: true},
            {name:"CLIENT_IN_PENALTY_BOX",displayName: i18n._("Client in Penalty Box"), type: "boolean", visible: rpc.isExpertMode},
            {name:"SERVER_IN_PENALTY_BOX",displayName: i18n._("Server in Penalty Box"), type: "boolean", visible: rpc.isExpertMode},
            {name:"CLIENT_HAS_NO_QUOTA",displayName: i18n._("Client has no Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"SERVER_HAS_NO_QUOTA",displayName: i18n._("Server has no Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"CLIENT_QUOTA_EXCEEDED",displayName: i18n._("Client has exceeded Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"SERVER_QUOTA_EXCEEDED",displayName: i18n._("Server has exceeded Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"CLIENT_QUOTA_ATTAINMENT",displayName: i18n._("Client Quota Attainment"), type: "text", visible: rpc.isExpertMode},
            {name:"SERVER_QUOTA_ATTAINMENT",displayName: i18n._("Server Quota Attainment"), type: "text", visible: rpc.isExpertMode},
//            {name:"HTTP_HOST",displayName: i18n._("HTTP: Hostname"), type: "text", visible: rpc.isExpertMode},
//            {name:"HTTP_REFERER",displayName: i18n._("HTTP: Referer"), type: "text", visible: true},
//            {name:"HTTP_URI",displayName: i18n._("HTTP: URI"), type: "text", visible: rpc.isExpertMode},
//            {name:"HTTP_URL",displayName: i18n._("HTTP: URL"), type: "text", visible: true},
//            {name:"HTTP_CONTENT_TYPE",displayName: i18n._("HTTP: Content Type"), type: "text", visible: rpc.isExpertMode},
//            {name:"HTTP_CONTENT_LENGTH",displayName: i18n._("HTTP: Content Length"), type: "text", visible: rpc.isExpertMode},
            {name:"HTTP_USER_AGENT",displayName: i18n._("HTTP: Client User Agent"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT_OS",displayName: i18n._("HTTP: Client User OS"), type: "text", visible: false},
//            {name:"APPLICATION_CONTROL_APPLICATION",displayName: i18n._("Application Control: Application"), type: "text", visible: rpc.isExpertMode},
//            {name:"APPLICATION_CONTROL_CATEGORY",displayName: i18n._("Application Control: Category"), type: "text", visible: rpc.isExpertMode},
//            {name:"APPLICATION_CONTROL_PROTOCHAIN",displayName: i18n._("Application Control: ProtoChain"), type: "text", visible: rpc.isExpertMode},
//            {name:"APPLICATION_CONTROL_DETAIL",displayName: i18n._("Application Control: Detail"), type: "text", visible: rpc.isExpertMode},
//            {name:"APPLICATION_CONTROL_CONFIDENCE",displayName: i18n._("Application Control: Confidence"), type: "text", visible: rpc.isExpertMode},
//            {name:"APPLICATION_CONTROL_PRODUCTIVITY",displayName: i18n._("Application Control: Productivity"), type: "text", visible: rpc.isExpertMode},
//            {name:"APPLICATION_CONTROL_RISK",displayName: i18n._("Application Control: Risk"), type: "text", visible: rpc.isExpertMode},
//            {name:"PROTOCOL_CONTROL_SIGNATURE",displayName: i18n._("Application Control Lite: Signature"), type: "text", visible: rpc.isExpertMode},
//            {name:"PROTOCOL_CONTROL_CATEGORY",displayName: i18n._("Application Control Lite: Category"), type: "text", visible: rpc.isExpertMode},
//            {name:"PROTOCOL_CONTROL_DESCRIPTION",displayName: i18n._("Application Control Lite: Description"), type: "text", visible: rpc.isExpertMode},
//            {name:"WEB_FILTER_CATEGORY",displayName: i18n._("Web Filter: Category"), type: "text", visible: rpc.isExpertMode},
//            {name:"WEB_FILTER_CATEGORY_DESCRIPTION",displayName: i18n._("Web Filter: Category Description"), type: "text", visible: rpc.isExpertMode},
//            {name:"WEB_FILTER_FLAGGED",displayName: i18n._("Web Filter: Site is Flagged"), type: "boolean", visible: rpc.isExpertMode},
            {name:"DIRECTORY_CONNECTOR_GROUP",displayName: i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
            {name:"SSL_INSPECTOR_SNI_HOSTNAME",displayName: i18n._("SSL: SNI Host Name"), type: "text", visible: true},
            {name:"SSL_INSPECTOR_SUBJECT_DN",displayName: i18n._("SSL: Certificate Subject"), type: "text", visible: true},
            {name:"SSL_INSPECTOR_ISSUER_DN",displayName: i18n._("SSL: Certificate Issuer"), type: "text", visible: true},
            {name:"CLIENT_COUNTRY",displayName: i18n._("Client Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
            {name:"SERVER_COUNTRY",displayName: i18n._("Server Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}
        ];
    },
    buildPanelConfiguration: function() {
        this.buildTrustGrid();

        this.panelAppConfiguration = Ext.create('Ext.form.Panel',{
            name: 'Configuration',
            helpSource: 'ssl_inspector_configuration',
            title: i18n._("Configuration"),
            cls: 'ung-panel',
            autoScroll: true,
            trackResetOnLoad: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._("Description"),
                html: i18n._("The SSL Inspector is an SSL decryption engine that allows other applications and services to process port 443 HTTPS and port 25 SMTPS traffic just like unencrypted port 80 HTTP and port 25 SMTP traffic. To do this, the application generates new SSL certificates on the fly which it uses to perform a the man-in-the-middle style inspection of traffic. To eliminate certificate security warnings on client computers and devices, you should download the root certificate and add it to the list of trusted authorities on each client connected to your network.")
            }, {
                xtype: 'fieldset',
                layout: 'column',
                items: [{
                    xtype: 'button',
                    margin: '0 5 0 5',
                    minWidth: 200,
                    text: i18n._("Download Root Certificate"),
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        var downloadForm = document.getElementById('downloadForm');
                        downloadForm["type"].value = "root_certificate_download";
                        downloadForm.submit();
                    }, this)
                },{
                    xtype: 'displayfield',
                    margin: '0 5 0 5',
                    columnWidth: 1,
                    value: i18n._("Click here to download the root certificate.")
                }]
            },{
                xtype: 'fieldset',
                layout: 'column',
                items: [{
                    xtype: 'button',
                    margin: '0 5 0 5',
                    minWidth: 200,
                    text: i18n._('Download Root Certificate Installer'),
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        var downloadForm = document.getElementById('downloadForm');
                        downloadForm["type"].value = "root_certificate_installer_download";
                        downloadForm.submit();
                    }, this)
                },{
                    xtype: 'component',
                    margin: '0 5 0 5',
                    columnWidth: 1,
                    html: i18n._('Click here to download the root certificate installer.  It installs the root certificate appropriately on a Windows device.')
                }]
            }, {
                title: i18n._("Options"),
                labelWidth: 230,
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("Enable SMTPS Traffic Processing"),
                    labelWidth: 240,
                    name: 'scanMailTraffic',
                    checked: this.settings.processEncryptedMailTraffic,
                    handler: Ext.bind(function(elem, checked) {
                                this.settings.processEncryptedMailTraffic = checked;
                            }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("Enable HTTPS Traffic Processing"),
                    labelWidth: 240,
                    name: 'scanWebTraffic',
                    checked: this.settings.processEncryptedWebTraffic,
                    handler: Ext.bind(function(elem, checked) {
                                this.settings.processEncryptedWebTraffic = checked;
                            }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("Block Invalid HTTPS Traffic"),
                    labelWidth: 240,
                    name: 'blockInvalidTraffic',
                    checked: this.settings.blockInvalidTraffic,
                    handler: Ext.bind(function(elem, checked) {
                                this.settings.blockInvalidTraffic = checked;
                            }, this)
                },{
                    xtype: 'displayfield',
                    padding: '0 100 0 20',
                    value:  i18n._("When the SSL Inspector detects non-HTTPS traffic on port 443, it will normally ignore this traffic and allow it to flow unimpeded.  If you enable this checkbox, non-HTTPS traffic will instead be blocked.")
                }]
            }, {
                xtype: 'fieldset',
                layout: { type: 'hbox', align: 'left' },
                defaults: {
                    margin: '0 0 0 30',
                    labelWidth: 50
                },
                title: i18n._("Client Connection Protocols"),
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("SSLv2Hello"),
                    labelWidth: 70,
                    name: 'client_SSLv2Hello',
                    checked: this.settings.client_SSLv2Hello,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.client_SSLv2Hello = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("SSLv3"),
                    labelWidth: 40,
                    name: 'client_SSLv3',
                    checked: this.settings.client_SSLv3,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.client_SSLv3 = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("TLSv1"),
                    labelWidth: 40,
                    name: 'client_TLSv10',
                    checked: this.settings.client_TLSv10,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.client_TLSv10 = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("TLSv1.1"),
                    name: 'client_TLSv11',
                    checked: this.settings.client_TLSv11,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.client_TLSv11 = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("TLSv1.2"),
                    name: 'client_TLSv12',
                    checked: this.settings.client_TLSv12,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.client_TLSv12 = checked;
                        }, this)
                }]
            }, {
                xtype: 'fieldset',
                layout: { type: 'hbox', align: 'left' },
                defaults: {
                    margin: '0 0 0 30',
                    labelWidth: 50
                },
                title: i18n._("Server Connection Protocols"),
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("SSLv2Hello"),
                    labelWidth: 70,
                    name: 'server_SSLv2Hello',
                    checked: this.settings.server_SSLv2Hello,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.server_SSLv2Hello = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("SSLv3"),
                    labelWidth: 40,
                    name: 'server_SSLv3',
                    checked: this.settings.server_SSLv3,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.server_SSLv3 = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("TLSv1"),
                    labelWidth: 40,
                    name: 'server_TLSv10',
                    checked: this.settings.server_TLSv10,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.server_TLSv10 = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("TLSv1.1"),
                    name: 'server_TLSv11',
                    checked: this.settings.server_TLSv11,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.server_TLSv11 = checked;
                        }, this)
                },{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("TLSv1.2"),
                    name: 'server_TLSv12',
                    checked: this.settings.server_TLSv12,
                    handler: Ext.bind(function(elem, checked) {
                        this.settings.server_TLSv12 = checked;
                        }, this)
                }]
            }, {
                title: i18n._("Server Trust"),
                labelWidth: 230,
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: i18n._("Trust All Server Certificates"),
                    labelWidth: 200,
                    name: 'serverBlindTrust',
                    checked: this.settings.serverBlindTrust,
                    handler: Ext.bind(function(elem, checked) {
                                this.settings.serverBlindTrust = checked;
                            }, this)
                },{
                    xtype: 'displayfield',
                    padding: '0 100 10 20',
                    value:  i18n._("When this check box is enabled, the inspector will blindly trust all server certificates.  When clear, the inspector will only trust server certificates signed by a well known and trusted root certificate authority, or certificates that you have added to the list below.")
                }, {
                    xtype: 'button',
                    text: i18n._("Upload Trusted Certificate"),
                    handler: Ext.bind(function() { this.handleCertificateUpload(); }, this)
                }, {
                    xtype: 'displayfield',
                    padding: '10 0 0 0',
                    value:  i18n._("<b>NOTE:</b> When uploading or deleting trusted certificates, changes are applied immediately.")
                }, this.gridTrustList ]
            }]
        });
    },

    actionRenderer: function(value) {
        switch(value.actionType) {
          case 'INSPECT': return i18n._("Inspect");
          case 'IGNORE': return i18n._("Ignore");
        default: return "Unknown Action: " + value;
        }
    },

    buildTrustGrid: function() {
        this.gridTrustList = Ext.create('Ung.grid.Panel',{
            title: i18n._("Trusted Certificates &nbsp;&nbsp; (click any cell to see details)"),
            autoGenerateId: true,
            height: 400,
            hasDelete: false,
            hasEdit: false,
            hasAdd: false,
            dataFn: this.getRpcApp().getTrustCatalog,
            fields: [{
                name: 'certAlias',
                mapping: 'certAlias'
            }, {
                name: 'issuedTo',
                mapping: 'issuedTo'
            }, {
                name: 'issuedBy',
                mapping: 'issuedBy'
            }, {
                name: 'dateValid',
                mapping: 'dateValid'
            }, {
                name: 'dateExpire',
                mapping: 'dateExpire'
            }],
            columns: [{
                header: i18n._("Alias"),
                flex: 1,
                width: 180,
                sortable: true,
                dataIndex: 'certAlias'
            }, {
                header: i18n._("Issued To"),
                flex: 1,
                width: 220,
                sortable: true,
                dataIndex: 'issuedTo'
            }, {
                header: i18n._("Issued By"),
                flex: 1,
                width: 220,
                sortable: true,
                dataIndex: 'issuedBy'
            }, {
                header: i18n._("Date Valid"),
                flex: 1,
                width: 180,
                sortable: true,
                dataIndex: 'dateValid'
            }, {
                header: i18n._("Date Expires"),
                flex: 1,
                width: 180,
                sortable: true,
                dataIndex: 'dateExpire'
            }, {
                header: i18n._("Delete"),
                xtype: 'actioncolumn',
                width: 80,
                items: [{
                    id: 'certRemove',
                    iconCls: 'icon-delete-row',
                    tooltip: i18n._("Click to delete"),
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        this.getRpcApp().removeTrustedCertificate(record.get("certAlias"));
                        this.gridTrustList.reload();
                    }, this)
                }]
            }]
        });

        this.gridTrustList.addListener('cellclick', function(grid, element, columnIndex, dataRecord) {
            if (columnIndex == 1) Ext.MessageBox.alert(dataRecord.data.certAlias + ' - Issued To',dataRecord.data.issuedTo);
            if (columnIndex == 2) Ext.MessageBox.alert(dataRecord.data.certAlias + ' - Issued By',dataRecord.data.issuedBy);
            if (columnIndex == 3) Ext.MessageBox.alert(dataRecord.data.certAlias + ' - Date Valid',dataRecord.data.dateValid);
            if (columnIndex == 4) Ext.MessageBox.alert(dataRecord.data.certAlias + ' - Date Expires',dataRecord.data.dateExpire);
        }, this.gridTrustList);

    },

    handleCertificateUpload: function() {
        this.uploadCertificateWin = Ext.create('Ext.Window', {
            title: i18n._("Upload Trusted Certificate"),
            layout: 'fit',
            modal: true,
            width: 600,
            height: 200,
            autoScroll: true,
            items: [{
                xtype: "form",
                name: "upload_trusted_cert_form",
                url: "upload",
                items: [{
                    xtype: 'filefield',
                    fieldLabel: i18n._("File"),
                    name: "filename",
                    margin: "10 10 10 10",
                    width: 560,
                    labelWidth: 50,
                    allowBlank: false,
                    validateOnBlur: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: i18n._("Alias"),
                    name: "argument",
                    margin: "10 10 10 10",
                    width: 200,
                    labelWidth: 50,
                    allowBlank: false
                }, {
                    xtype: "button",
                    text: i18n._("Upload Certificate"),
                    name: "Upload Certificate",
                    width: 200,
                    margin: "10 10 10 80",
                    handler: Ext.bind(function() {
                        this.handleFileUpload();
                    }, this)
                }, {
                    xtype: "button",
                    text: i18n._("Cancel"),
                    name: "Cancel",
                    width: 200,
                    margin: "10 10 10 10",
                    handler: Ext.bind(function() {
                        this.uploadCertificateWin.close();
                    }, this)
                }, {
                    xtype: "hidden",
                    name: "type",
                    value: "trusted_cert"
                    }]
                }]
        });

        this.uploadCertificateWin.show();
    },

    handleFileUpload: function() {
        var prova = this.uploadCertificateWin.down('form[name="upload_trusted_cert_form"]');
        var fileText = prova.down('filefield[name="filename"]');
        var nameText = prova.down('textfield[name="argument"]');
        if (fileText.getValue().length === 0) {
            Ext.MessageBox.alert(i18n._("Invalid or missing File"), i18n._("Please select a certificate to upload."));
            return false;
            }
        if (nameText.getValue().length === 0) {
            Ext.MessageBox.alert(i18n._("Invalid or missing Alias"), i18n._("Please enter a unique alias or nickname for the certificate."));
            return false;
        }
        var form = prova.getForm();
        form.submit({
            waitMsg: i18n._("Inspecting File..."),
            success: Ext.bind(function(form, action) {
                this.uploadCertificateWin.close();
                this.gridTrustList.reload();
            }, this),
            failure: Ext.bind(function(form, action) {
                this.uploadCertificateWin.close();
                Ext.MessageBox.alert(i18n._("Upload Failure"), action.result.msg);
            }, this)
        });
        return true;
    },

    buildGridIgnoreRules: function() {
        this.gridIgnoreRules = Ext.create('Ung.grid.Panel',{
            name: "gridIgnoreRules",
            helpSource: 'ssl_inspector_rules',
            settingsCmp: this,
            height: 500,
            hasReorder: true,
            addAtTop: false,
            title: i18n._("Rules"),
            qtip: i18n._("Ignore rules are used to configure traffic which should be ignored by the inspection engine."),
            dataProperty: "ignoreRules",
            recordJavaClass: "com.untangle.app.ssl_inspector.SslInspectorRule",
            emptyRow: {
                "live": true,
                "ruleId": 0,
                "description": "",
                "action": ""
            },
            fields: [{
                name: 'live'
            },{
                name: 'ruleId'
            },{
                name: 'description'
            },{
                name: 'action'
            },{
                name: 'conditions'
            }],
            columns:[
            {
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Enabled"),
                dataIndex: 'live',
                resizable: false
            },
            {
                header: i18n._("Rule ID"),
                dataIndex: 'ruleId',
                width: 50,
                renderer: function(value) {
                    if (value == 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                header: i18n._("Description"),
                dataIndex:'description',
                flex:1,
                width: 200
            }, {
                header: i18n._("Action"),
                dataIndex:'action',
                width: 150,
                renderer: this.actionRenderer
            }],
            rowEditorInputLines: [{
                xtype: "checkbox",
                name: "Enabled",
                dataIndex: "live",
                fieldLabel: i18n._( "Enabled" ),
                width: 360
            }, {
                xtype: "textfield",
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._( "Description" ),
                emptyText: i18n._("[no description]"),
                width: 480
            }, {
                xtype: "fieldset",
                autoScroll: true,
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype: 'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.app.ssl_inspector.SslInspectorRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action:'),
                items:[{
                    xtype: "container",
                    name: "Action",
                    dataIndex: "action",
                    fieldLabel: i18n._("Action"),
                    items: [{
                        xtype: "combo",
                        name: "actionType",
                        allowBlank: false,
                        fieldLabel: i18n._("Action"),
                        editable: false,
                        store: [['INSPECT', i18n._('Inspect')],
                                ['IGNORE', i18n._('Ignore')]],
                        valueField: "value",
                        displayField: "displayName",
                        queryMode: "local"
                    }],
                    setValue: function(value) {
                        var actionType  = this.down('combo[name="actionType"]');
                        actionType.setValue(value.actionType);
                    },
                    getValue: function() {
                        var actionType  = this.down('combo[name="actionType"]').getValue();
                        var action = {
                            javaClass: "com.untangle.app.ssl_inspector.SslInspectorRuleAction",
                            actionType: actionType,
                            //must override toString in order for all objects not to appear the same
                            toString: function() {
                                return Ext.encode(this);
                            }
                        };
                        return action;
                    },
                    isValid: function (){
                        var actionType  = this.down('combo[name="actionType"]');
                        var isValid = actionType.isValid();
                        this.activeErrors=actionType.activeErrors;
                        return isValid;
                    }
                }]
            }]
        });
    },
    beforeSave: function(isApply, handler) {
        this.settings.ignoreRules.list=this.gridIgnoreRules.getList();
        handler.call(this, isApply);
    }
});

//# sourceURL=ssl-settings.js
