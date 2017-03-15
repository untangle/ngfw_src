Ext.define('Webui.config.email', {
    extend: 'Ung.ConfigWin',
    panelOutgoingServer: null,
    panelEmailSafeList: null,
    panelQuarantine: null,
    gridSafelistGlobal: null,
    gridSafelistUser: null,
    gridSafelistUserDetails: null,
    quarantinableAddressesGrid: null,
    quarantineForwardsGrid: null,
    userQuarantinesGrid: null,
    safelistDetailsWin: null,
    quarantinesDetailsWin: null,
    loadedGlobalSafelist: false,

    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._("Configuration"),
            action: Ext.bind(function() {
                this.cancelAction();
            }, this)
        }, {
            title: i18n._('Email')
        }];
        this.getMailSettings();
        this.buildOutgoingServer();
        if( this.isMailLoaded() ) {
            this.getMailAppSettings();
            this.buildEmailSafeList();
            this.buildQuarantine();
        }
        // builds the tab panel with the tabs
        var pageTabs = [this.panelOutgoingServer];
        if( this.isMailLoaded() ) {
            pageTabs.push( this.panelEmailSafeList );
            pageTabs.push( this.panelQuarantine );
        }
        this.buildTabPanel(pageTabs);
        this.tabs.setActiveTab(this.panelOutgoingServer);
        this.callParent(arguments);
    },
    afterRender: function() {
        this.callParent(arguments);
        var smtpLoginCmp = Ext.getCmp('email_smtpLogin');
        var useAuthentication = smtpLoginCmp != null && smtpLoginCmp.getValue()!=null && smtpLoginCmp.getValue().length > 0;
        Ext.getCmp('email_smtpUseAuthentication').setValue(useAuthentication);
        Ext.getCmp('email_smtpLogin').setVisible(useAuthentication);
        Ext.getCmp('email_smtpPassword').setVisible(useAuthentication);

        Ung.Util.clearDirty(this.panelOutgoingServer);

        if ( this.isMailLoaded() ) {
            var sendDigest = Ext.getCmp('quarantine_sendDailyDigest').getValue();
            if (sendDigest) {
                Ext.getCmp('quarantine_dailySendingTime').enable();
            } else {
                Ext.getCmp('quarantine_dailySendingTime').disable();
            }
            Ung.Util.clearDirty(this.panelQuarantine);
        }
    },

    getMailNode: function(forceReload) {
        if (forceReload || this.rpc.smtpNode === undefined) {
            try {
                this.rpc.smtpNode = rpc.appManager.node("smtp");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.smtpNode;
    },
    isMailLoaded: function(forceReload) {
        return this.getMailNode(forceReload) != null;
    },
    getMailSettings: function(forceReload) {
        if (forceReload || this.rpc.mailSettings === undefined) {
            try {
                this.rpc.mailSettings = Ung.Main.getMailSender().getSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.mailSettings;
    },
    getMailAppSettings: function(forceReload) {
        if (forceReload || this.rpc.smtpAppSettings === undefined) {
            try {
                this.rpc.smtpAppSettings = this.getMailNode().getSmtpSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.smtpAppSettings;
    },
    getSafelistAdminView: function(forceReload) {
        if (forceReload || this.rpc.safelistAdminView === undefined) {
            try {
                this.rpc.safelistAdminView = this.getMailNode().getSafelistAdminView();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.safelistAdminView;
    },
    getQuarantineMaintenenceView: function(forceReload) {
        if (forceReload || this.rpc.quarantineMaintenenceView === undefined) {
            try {
                this.rpc.quarantineMaintenenceView = this.getMailNode().getQuarantineMaintenenceView();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.quarantineMaintenenceView;
    },
    getFormattedTime: function(hours, minutes) {
        var hh = hours < 10 ? "0" + hours: hours;
        var mm = minutes < 10 ? "0" + minutes: minutes;
        return hh + ":" + mm;
    },
    sendTestMessage: function(emailAddress) {
        var message = Ung.Main.getMailSender().sendTestMessage( Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.testEmailResultMessage = result;
            var color = result == 'Completed' ? 'green': 'red';
            Ext.MessageBox.hide();
            Ext.getCmp('email-test-success').setText(this.testEmailResultMessage).setVisible(true).getEl().dom.style.color = color;
        }, this), emailAddress);
    },
    buildOutgoingServer: function() {
        var onUpdateSendMethod = Ext.bind(function( elem, checked ) {
            if (! checked) return;
            this.getMailSettings().sendMethod = elem.inputValue;
            if (elem.inputValue == "CUSTOM") {
                Ext.getCmp('email_smtpHost').enable();
                Ext.getCmp('email_smtpPort').enable();
                Ext.getCmp('email_smtpUseAuthentication').enable();
                Ext.getCmp('email_smtpLogin').enable();
                Ext.getCmp('email_smtpPassword').enable();
            } else {
                Ext.getCmp('email_smtpHost').disable();
                Ext.getCmp('email_smtpPort').disable();
                Ext.getCmp('email_smtpUseAuthentication').disable();
                Ext.getCmp('email_smtpLogin').disable();
                Ext.getCmp('email_smtpPassword').disable();
            }
        }, this);
        var onRenderSendMethod = Ext.bind(function( elem ) {
            elem.setValue(this.getMailSettings().sendMethod);
            elem.clearDirty();
        }, this);

        this.panelOutgoingServer = Ext.create('Ext.panel.Panel', {
            name: 'Outgoing Server',
            helpSource: 'email_outgoing_server',
            title: i18n._('Outgoing Server'),
            cls: 'ung-panel',
            autoScroll: true,
            listeners: {
                'activate': {
                    fn: function () {
                        Ext.create('Ext.tip.ToolTip',{
                            html: 'It is recommended to use a valid email address. (example: untangle@mydomain.com)',
                            target: 'email_fromAddress',
                            showDelay: 200,
                            dismissDelay: 0,
                            hideDelay: 0
                            });
                        Ext.create('Ext.tip.ToolTip',{
                            html: 'Some servers may require this but other servers may not support it.',
                            target: 'email_smtpUseAuthentication',
                            showDelay: 200,
                            dismissDelay: 0,
                            hideDelay: 0
                        });
                    }
                }
            },

            onEmailTest: Ext.bind(function(saveBefore) {
                var emailTestMessage = i18n._("Enter an email address to send a test message and then press Send. That email account should receive an email shortly after running the test. If not, the email settings may not be correct.<br/><br/>It is recommended to verify that the email settings work for sending to both internal (your domain) and external email addresses.");
                if(!this.emailMessageBox) {
                    this.emailMessageBox = Ext.create('Ext.Window',{
                        layout: 'fit',
                        width: 500,
                        height: 300,
                        modal: true,
                        title: i18n._('Email Test'),
                        closeAction: 'hide',
                        plain: false,
                        items: Ext.create('Ext.panel.Panel',{
                            border: false,
                            items: [{
                                xtype: 'fieldset',
                                height: 300,
                                items: [{
                                    xtype: 'label',
                                    html: '<strong style="margin-bottom:20px;font-size:11px;display:block;">'+emailTestMessage+'</strong>',
                                    width: 400,
                                    height: 100
                                },{
                                    xtype: 'textfield',
                                    name: 'Email Address',
                                    id: 'email-address-test',
                                    vtype: 'email',
                                    validateOnBlur: true,
                                    allowBlank: false,
                                    fieldLabel: i18n._('Email Address'),
                                    emptyText: i18n._("[enter email]"),
                                    width: 380
                                },{
                                    xtype: 'label',
                                    style: 'font-weight:bold;color:green;font-size:11px;display:block;',
                                    width: 300,
                                    height: 100,
                                    id: 'email-test-success',
                                    visible: false
                                }]
                            }]
                        }),
                        buttons: [{
                            text: 'Send',
                            disabled: false,
                            handler: Ext.bind(function() {
                                var emailAddress = Ext.getCmp('email-address-test');
                                if(emailAddress.validate()===true) {
                                    Ext.MessageBox.wait(i18n._('Sending Email...'), i18n._('Please wait'));
                                    this.sendTestMessage(emailAddress.getValue());
                                }
                            }, this)
                        },{
                            text: 'Close',
                            handler: Ext.bind(function() {
                                this.emailMessageBox.destroy();
                                this.emailMessageBox = null;
                            }, this)
                        }]
                    });
                } else {
                    Ext.getCmp('email-address-test').setValue('').clearInvalid();
                    Ext.getCmp('email-test-success').setVisible(false);
                }
                this.emailMessageBox.show();
            }, this),

            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._('Outgoing Email Server'),
                items: [{
                    xtype: 'component',
                    margin: '0 200 10 0',
                    html: Ext.String.format(i18n._("The Outgoing Email Server settings determine how the {0} Server sends emails such as reports, quarantine digests, etc. In most cases the cloud hosted mail relay server is preferred. Alternatively, you can configure mail to be sent directly to mail account servers. You can also specify a valid SMTP server that will relay mail for the {0} Server."), rpc.companyName)
                }, {
                    xtype: 'radio',
                    name: "sendMethod",
                    id: 'email_sendMethod_relay',
                    boxLabel: i18n._('Send email using the cloud hosted mail relay server'),
                    hideLabel: true,
                    inputValue: "RELAY",
                    style: "margin-left: 50px;",
                    listeners: {
                        "change": onUpdateSendMethod,
                        "afterrender": onRenderSendMethod
                    }
                }, {
                    xtype: 'radio',
                    name: "sendMethod",
                    id: 'email_sendMethod_direct',
                    boxLabel: i18n._('Send email directly'),
                    hideLabel: true,
                    inputValue: "DIRECT",
                    style: "margin-left: 50px;",
                        listeners: {
                            "change": onUpdateSendMethod,
                            "afterrender": onRenderSendMethod
                        }
                }, {
                    xtype: 'radio',
                    name: "sendMethod",
                    id: 'email_sendMethod_custom',
                    boxLabel: i18n._('Send email using the specified SMTP Server'),
                    hideLabel: true,
                    inputValue: "CUSTOM",
                    style: "margin-left: 50px;",
                        listeners: {
                            "change": onUpdateSendMethod,
                            "afterrender": onRenderSendMethod
                        }
                }, {
                    xtype: 'fieldset',
                    height: 150,
                    style: "margin-left: 50px;",
                    defaults: {
                        labelWidth: 230,
                        labelAlign: 'right'
                    },
                    items: [{
                        xtype: 'textfield',
                        name: 'Server Address or Hostname',
                        id: 'email_smtpHost',
                        fieldLabel: i18n._('Server Address or Hostname'),
                        width: 400,
                        value: this.getMailSettings().smtpHost
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'textfield',
                            name: 'Server Port',
                            id: 'email_smtpPort',
                            fieldLabel: i18n._('Server Port'),
                            labelWidth: 230,
                            labelAlign: 'right',
                            width: 280,
                            value: this.getMailSettings().smtpPort,
                            vtype: "port",
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        if (newValue == 465) {
                                            Ext.getCmp('email_smtpPort_TLS_Warning').setVisible(true);
                                        } else {
                                            Ext.getCmp('email_smtpPort_TLS_Warning').setVisible(false);
                                        }
                                    }, this)
                                }
                            }

                        },{
                            xtype: 'component',
                            id: 'email_smtpPort_TLS_Warning',
                            hidden: (this.getMailSettings().smtpPort != 465),
                            html: "<b>" + "<font color=\"red\">&nbsp;" + i18n._("Warning:") + "</font>&nbsp;" + i18n._("SMTPS (465) is deprecated and not supported. Use STARTTLS (587).") + "</b>"
                        }]
                    }, {
                        xtype: 'checkbox',
                        name: 'Use Authentication',
                        id: 'email_smtpUseAuthentication',
                        fieldLabel: i18n._('Use Authentication'),
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    Ext.getCmp('email_smtpLogin').setVisible(checked);
                                    Ext.getCmp('email_smtpPassword').setVisible(checked);
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'textfield',
                        hidden: true,
                        name: 'Login',
                        id: 'email_smtpLogin',
                        fieldLabel: i18n._('Login'),
                        width: 375,
                        value: this.getMailSettings().authUser
                    }, {
                        xtype: 'textfield',
                        hidden: true,
                        name: 'Password',
                        id: 'email_smtpPassword',
                        inputType: 'password',
                        fieldLabel: i18n._('Password'),
                        width: 375,
                        value: this.getMailSettings().authPass
                    }]
                }]
            }, {
                title: i18n._('Email From Address'),
                items: [{
                    xtype: 'component',
                    margin: '0 0 5 0',
                    html: Ext.String.format(i18n._("The {0} Server will send email from this address."), rpc.companyName)
                }, {
                    xtype: 'textfield',
                    name: 'Email From Address',
                    id: 'email_fromAddress',
                    emptyText: i18n._("[enter email]"),
                    vtype: 'email',
                    hideLabel: true,
                    allowBlank: false,
                    width: 300,
                    value: this.getMailSettings().fromAddress
                }]
            }, {
                title: i18n._('Email Test'),
                items: [{
                    xtype: 'component',
                    margin: '0 0 5 0',
                    html: i18n._('The Email Test will send an email to a specified address with the current configuration. If the test email is not received your settings may be incorrect.')
                },{
                    xtype: 'button',
                    text: i18n._("Email Test"),
                    iconCls: 'test-icon',
                    name: "emailTestButton",
                    handler: Ext.bind(function() {
                        if (Ung.Util.isDirty(this.panelOutgoingServer)) {
                            Ext.Msg.show({
                               title: i18n._('Save Changes?'),
                               msg: Ext.String.format(i18n._("Your current settings have not been saved yet.{0}Would you like to save your settings before executing the test?"), '<br>'),
                               buttons: Ext.Msg.YESNOCANCEL,
                               fn: Ext.bind(function(btnId) {
                                       if (btnId == 'yes') {
                                           if (this.validate()) {
                                            Ext.MessageBox.wait(i18n._('Saving...'), i18n._('Please wait'));
                                            // save mail settings
                                            Ung.Main.getMailSender().setSettings(Ext.bind(function(result, exception) {
                                                if(Ung.Util.handleException(exception)) return;
                                                Ext.MessageBox.hide();
                                                Ung.Util.clearDirty(this.panelOutgoingServer);
                                                // send test mail
                                                this.panelOutgoingServer.onEmailTest();
                                            }, this), this.getMailSettings());
                                           }
                                       }
                               }, this),
                               animEl: 'elId',
                               icon: Ext.MessageBox.QUESTION
                            });
                        } else {
                            this.panelOutgoingServer.onEmailTest();
                        }
                    }, this)
                }]
            }]
        });
    },
    buildEmailSafeList: function() {
        var showDetailColumn = Ext.create('Ext.grid.column.Action',{
            header: i18n._("Show Detail"),
            width: 100,
            iconCls: 'icon-detail-row',
            init: function(grid) {
                this.grid = grid;
            },
            handler: function(view, rowIndex, colIndex, item, e, record) {
                // select current row
                this.grid.getSelectionModel().select(record);
                // show details
                this.grid.onShowDetail(record);
            }
        });
        this.loadedGlobalSafelist = true;
        this.gridSafelistGlobal = Ext.create('Ung.grid.Panel', {
            name: 'Global',
            title: i18n._('Global Safe List'),
            hasEdit: false,
            settingsCmp: this,
            flex: 1,
            dataFn: Ext.bind(function(handler) {
                this.getSafelistAdminView().getSafelistContents(Ext.bind(function(result, exception) {
                    handler({list: Ung.Util.buildJsonListFromStrings(result, 'emailAddress')}, exception);
                }, this),'GLOBAL');
            }, this),
            emptyRow: {
                "emailAddress": ""
            },
            sortField: 'emailAddress',
            fields: [{
                name: 'emailAddress'
            }],
            columns: [{
                header: i18n._("Email Address"),
                flex: 1,
                width: 450,
                dataIndex: 'emailAddress'
            }],
            rowEditorInputLines: [{
                xtype: 'textfield',
                name: "Email Address",
                dataIndex: "emailAddress",
                fieldLabel: i18n._("Email Address"),
                emptyText: i18n._("[enter email]"),
                vtype: 'email',
                allowBlank: false,
                width: 400
            }]
        });

        this.gridSafelistUser = Ext.create('Ung.grid.Panel', {
            name: 'Per User',
            title: i18n._('Per User Safe Lists'),
            selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
            hasEdit: false,
            hasAdd: false,
            hasDelete: false,
            settingsCmp: this,
            flex: 1,
            margin: '5 0 0 0',
            dataFn: this.getSafelistAdminView().getUserSafelistCounts,
            /*testData: [
                {id: 5, emailAddress: "aaa@aaa.com", count: 353},
                {id: 6, emailAddress: "bbb@bbb.com", count: 890}
            ],*/
            tbar: [{
                xtype: 'button',
                text: i18n._('Purge Selected'),
                tooltip: i18n._('Purge Selected'),
                iconCls: 'purge-icon',
                name: 'Purge Selected',
                handler: Ext.bind(function() {
                    var selectedRecords = this.gridSafelistUser.getSelectionModel().selected;
                    if(selectedRecords === undefined || selectedRecords.length == 0) {
                        return;
                    }
                    var accounts = [];
                    selectedRecords.each(function(item, index, length) {
                        accounts.push(item.data.emailAddress);
                    });

                    Ext.MessageBox.wait(i18n._("Purging..."), i18n._("Please wait"));
                    this.getSafelistAdminView().deleteSafelists(Ext.bind(function(result, exception) {
                        Ext.MessageBox.hide();
                        if(Ung.Util.handleException(exception)) return;
                        this.gridSafelistUser.reload();
                    }, this), accounts);
                }, this)
            }],
            plugins: [showDetailColumn],
            sortField: 'emailAddress',
            fields: [{
                name: 'id'
            }, {
                name: 'emailAddress'
            }, {
                name: 'count'
            }],
            columns: [{
                header: i18n._("Account Address"),
                width: 350,
                dataIndex: 'emailAddress',
                flex: 1
            }, {
                header: i18n._("Safe List Size"),
                width: 150,
                align: 'right',
                dataIndex: 'count'
            }, showDetailColumn],
            onShowDetail: Ext.bind(function(record) {
                if(!this.safelistDetailsWin) {
                    this.buildGridSafelistUserDetails();
                    this.safelistDetailsWin = Ext.create('Webui.config.email.EmailAddressDetails', {
                        items: this.gridSafelistUserDetails,
                        settingsCmp: this,
                        showForCurrentAccount: function(emailAddress) {
                            this.account = emailAddress;
                            var newTitle = i18n._('Safe List Details for:') + " " + emailAddress;
                            this.setTitle(newTitle);

                            this.show();
                            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
                            this.settingsCmp.getSafelistAdminView().getSafelistContents(
                                Ext.bind(function(result, exception) {
                                    Ext.MessageBox.hide();
                                    if(Ung.Util.handleException(exception)) return;
                                    //result = ['test@aaa.com', 'test1@bbb.com']; //TODO: comment it when not testing
                                    var data = Ung.Util.buildJsonListFromStrings(result, 'sender');

                                    this.settingsCmp.gridSafelistUserDetails.reload({data: data});
                                }, this), emailAddress);
                        }
                    });
                    this.subCmps.push(this.safelistDetailsWin);
                }

                this.safelistDetailsWin.showForCurrentAccount(record.get('emailAddress'));
            }, this)
        });
        this.panelEmailSafeList = Ext.create('Ext.panel.Panel', {
            name: 'Safe List',
            helpSource: 'email_safe_list',
            title: i18n._('Safe List'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [this.gridSafelistGlobal, this.gridSafelistUser]
        });
    },
    buildGridSafelistUserDetails: function() {
        this.gridSafelistUserDetails = Ext.create('Ung.grid.Panel',{
            name: 'gridSafelistUserDetails',
            selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
            hasEdit: false,
            hasAdd: false,
            hasDelete: false,
            tbar: [{
                text: i18n._('Purge Selected'),
                tooltip: i18n._('Purge Selected'),
                iconCls: 'purge-icon',
                name: 'Purge Selected',
                handler: Ext.bind(function() {
                    var selectedRecords = this.gridSafelistUserDetails.getSelectionModel().selected;
                    if(selectedRecords === undefined || selectedRecords.length == 0) {
                        return;
                    }
                    var senders = [];
                    selectedRecords.each(function(item, index, length) {
                        senders.push(item.data.sender);
                    });
                    Ext.MessageBox.wait(i18n._("Purging..."), i18n._("Please wait"));
                    this.getSafelistAdminView().removeFromSafelists(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        //result = ['test@aaa.com', 'test1@bbb.com']; //TODO: comment it when not testing
                        var data = Ung.Util.buildJsonListFromStrings(result, 'sender');
                        this.gridSafelistUserDetails.reload({data: data});
                        Ext.MessageBox.hide();
                    }, this), this.safelistDetailsWin.account, senders);

                    this.gridSafelistUser.reload();
                }, this)
            }],
            sortField: 'sender',
            fields: [{
                name: 'sender'
            }],
            columns: [{
                header: i18n._("Email Address"),
                flex: 1,
                width: 400,
                dataIndex: 'sender'
            }]
        });
    },

    buildQuarantine: function() {
        var showDetailColumn = Ext.create('Ext.grid.column.Action', {
            header: i18n._("Show Detail"),
            width: 100,
            iconCls: 'icon-detail-row',
            init: function(grid) {
                this.grid = grid;
            },
            handler: function(view, rowIndex, colIndex, item, e, record) {
                // select current row
                this.grid.getSelectionModel().select(record);
                // show details
                this.grid.onShowDetail(record);
            }
        });

        var publicUrl;
        try {
            publicUrl = rpc.networkManager.getPublicUrl();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        this.panelQuarantine = Ext.create('Ext.panel.Panel',{
            name: 'panelQuarantine',
            helpSource: 'email_quarantine',
            title: i18n._('Quarantine'),
            cls: 'ung-panel',
            autoScroll: true,
            reserveScrollbar: true,
            layout: { type: 'vbox', pack: 'start', align: 'stretch' },
            items: [{
                xtype: 'fieldset',
                defaults: {
                    labelWidth: 230
                },
                items: [{
                    xtype: 'textfield',
                    name: 'Maximum Holding Time (days)',
                    fieldLabel: i18n._('Maximum Holding Time (days)'),
                    allowBlank: false,
                    value: this.getMailAppSettings().quarantineSettings.maxMailIntern/(1440*60*1000),
                    regex: /^([0-9]|[0-9][0-9])$/,
                    regexText: i18n._('Maximum Holding Time must be a number in range 0-99'),
                    width: 270,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                var millisecValue = newValue * 1440*60*1000;
                                this.getMailAppSettings().quarantineSettings.maxMailIntern = millisecValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'checkbox',
                    name: 'Send Daily Quarantine Digest Emails',
                    id: 'quarantine_sendDailyDigest',
                    fieldLabel: i18n._('Send Daily Quarantine Digest Emails'),
                    checked: this.getMailAppSettings().quarantineSettings.sendDailyDigests,
                    width: 320,
                    listeners: {
                        "afterrender": {
                            fn: Ext.bind(function(elem) {
                                if(elem.getValue()) {
                                    Ext.getCmp('quarantine_dailySendingTime').enable();
                                } else {
                                    Ext.getCmp('quarantine_dailySendingTime').disable();
                                }
                            }, this)
                        },
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getMailAppSettings().quarantineSettings.sendDailyDigests = newValue;
                                if(newValue) {
                                    Ext.getCmp('quarantine_dailySendingTime').enable();
                                } else {
                                    Ext.getCmp('quarantine_dailySendingTime').disable();
                                }
                            }, this)
                        }

                    }
                }, {
                    xtype: 'timefield',
                    name: 'Digest Sending Time',
                    id: 'quarantine_dailySendingTime',
                    fieldLabel: i18n._('Quarantine Digest Sending Time'),
                    allowBlank: false,
                    increment: 1,
                    width: 320,
                    value: this.getFormattedTime(this.getMailAppSettings().quarantineSettings.digestHourOfDay, this.getMailAppSettings().quarantineSettings.digestMinuteOfDay),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                if (newValue && newValue instanceof Date) {
                                    this.getMailAppSettings().quarantineSettings.digestMinuteOfDay = newValue.getMinutes();
                                    this.getMailAppSettings().quarantineSettings.digestHourOfDay = newValue.getHours();
                                }
                            }, this)
                        }
                    }
                }, {
                    xtype: 'component',
                    html: Ext.String.format(i18n._('Users can also request Quarantine Digest Emails manually at this link: <b>https://{0}/quarantine/</b>'), publicUrl)
                }]
            },
            this.userQuarantinesGrid = Ext.create('Ung.grid.Panel', {
                title: i18n._('User Quarantines'),
                name: 'User Quarantines',
                selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
                hasEdit: false,
                hasAdd: false,
                hasDelete: false,
                settingsCmp: this,
                height: 250,
                dataFn: this.getQuarantineMaintenenceView().listInboxes,
                /*testData: [
                    {id: 34, address: "aaa@ddd.com", numMails: 115, totalSz: 150124},
                    {id: 35, address: "bbb@ddd.com", numMails: 76, totalSz: 560777}
                ],*/
                tbar: [{
                    text: i18n._('Purge Selected'),
                    tooltip: i18n._('Purge Selected'),
                    iconCls: 'purge-icon',
                    name: 'Purge Selected',
                    handler: Ext.bind(function() {
                        var selectedRecords = this.userQuarantinesGrid.getSelectionModel().selected;
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var accounts = [];
                        selectedRecords.each(function(item, index, length) {
                            accounts.push(item.data.address);
                        });

                        Ext.MessageBox.wait(i18n._("Purging..."), i18n._("Please wait"));
                        this.getQuarantineMaintenenceView().deleteInboxes(Ext.bind(function(result, exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.userQuarantinesGrid.reload();
                        }, this), accounts);
                    }, this)
                }, {
                    text: i18n._('Release Selected'),
                    tooltip: i18n._('Release Selected'),
                    iconCls: 'release-icon',
                    name: 'Release Selected',
                    handler: Ext.bind(function() {
                        var selectedRecords = this.userQuarantinesGrid.getSelectionModel().selected;
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var accounts = [];
                        selectedRecords.each(function(item, index, length) {
                            accounts.push(item.data.address);
                        });

                        Ext.MessageBox.wait(i18n._("Releasing..."), i18n._("Please wait"));
                        this.getQuarantineMaintenenceView().rescueInboxes(Ext.bind(function(result, exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.userQuarantinesGrid.reload();
                        }, this), accounts);
                    }, this)
                }, {
                    xtype: 'tbfill'
                }, {
                    xtype: 'tbtext',
                    text: Ext.String.format(i18n._('Total Disk Space Used: {0} MB'), i18n.numberFormat((this.getQuarantineMaintenenceView().getInboxesTotalSize()/(1024 * 1024)).toFixed(3)))
                }],
                plugins: [showDetailColumn],
                sortField: 'address',
                fields: [{
                    name: 'address'
                }, {
                    name: 'totalMails'
                }, {
                    name: 'totalSz'
                }],
                columns: [
                {
                    header: i18n._("Account Address"),
                    width: 200,
                    dataIndex: 'address',
                    flex: 1
                }, {
                    header: i18n._("Message Count"),
                    width: 185,
                    align: 'right',
                    dataIndex: 'totalMails'
                }, {
                    header: i18n._("Data Size (kB)"),
                    width: 185,
                    align: 'right',
                    dataIndex: 'totalSz',
                    renderer: function(value) {
                        return i18n.numberFormat((value /1024.0).toFixed(3));
                    }
                }, showDetailColumn],
                onShowDetail: Ext.bind(function(record) {
                    if(!this.quarantinesDetailsWin) {
                        this.buildUserQuarantinesGrid();
                        this.quarantinesDetailsWin = Ext.create('Webui.config.email.EmailAddressDetails', {
                            items: this.userQuarantinesDetailsGrid,
                            settingsCmp: this,
                            showForCurrentAccount: function(emailAddress) {
                                this.account = emailAddress;
                                var newTitle = i18n._('Email Quarantine Details for:') + " " + emailAddress;
                                this.setTitle(newTitle);
                                this.show();
                                //load Quarantines Details
                                this.settingsCmp.userQuarantinesDetailsGrid.reload();
                            }
                        });
                        this.subCmps.push(this.quarantinesDetailsWin);
                    }
                    this.quarantinesDetailsWin.showForCurrentAccount(record.get('address'));
                }, this)
            }), {
                xtype: 'container',
                style: 'margin: 20px 0 5px 0',
                border: false,
                html: i18n._('Email addresses on this list will have quarantines automatically created. All other emails will be marked and not quarantined.')
            },
            this.quarantinableAddressesGrid = Ext.create('Ung.grid.Panel',{
                title: i18n._('Quarantinable Addresses'),
                name: 'Quarantinable Addresses',
                settingsCmp: this,
                height: 250,
                dataExpression: "getMailAppSettings().quarantineSettings.allowedAddressPatterns.list",
                recordJavaClass: "com.untangle.node.smtp.EmailAddressRule",
                emptyRow: {
                    "address": ""
                },
                fields: [{
                    name: 'address'
                }],
                columns: [{
                    header: i18n._("Quarantinable Address"),
                    flex: 1,
                    width: 400,
                    dataIndex: 'address',
                    editor: {
                        xtype: 'textfield',
                        emptyText: i18n._("[enter email address rule]"),
                        allowBlank: false
                    }
                }],
                rowEditorInputLines: [{
                    xtype: 'textfield',
                    name: "Address",
                    dataIndex: "address",
                    fieldLabel: i18n._("Address"),
                    emptyText: i18n._("[enter email address rule]"),
                    allowBlank: false,
                    width: 450
                }]
            }), {
                xtype: 'container',
                style: 'margin: 20px 0 5px 0',
                border: false,
                html: i18n._('This is a list of email addresses whose quarantine digest gets forwarded to another account. This is common for distribution lists where the whole list should not receive the digest.')
            },
            this.quarantineForwardsGrid = Ext.create('Ung.grid.Panel', {
                title: i18n._('Quarantine Forwards'),
                name: 'Quarantine Forwards',
                settingsCmp: this,
                height: 250,
                dataExpression: "getMailAppSettings().quarantineSettings.addressRemaps.list",
                recordJavaClass: "com.untangle.node.smtp.EmailAddressPairRule",
                emptyRow: {
                    "address1": "",
                    "address2": ""
                },
                fields: [{
                    name: 'address1'
                }, {
                    name: 'address2'
                }],
                columns: [
                {
                    header: i18n._("Distribution List Address"),
                    width: 250,
                    dataIndex: 'address1',
                    editor: {
                        xtype: 'textfield',
                        emptyText: i18n._("distributionlistrecipient@example.com"),
                        vtype: 'email',
                        allowBlank: false
                    }
                },
                {
                    header: i18n._("Send to Address"),
                    width: 250,
                    dataIndex: 'address2',
                    flex:1,
                    editor: {
                        xtype: 'textfield',
                        emptyText: i18n._("quarantinelistowner@example.com"),
                        vtype: 'email',
                        allowBlank: false
                    }
                }],
                rowEditorLabelWidth: 160,
                rowEditorInputLines: [{
                    xtype: 'textfield',
                    name: "Distribution List Address",
                    dataIndex: "address1",
                    fieldLabel: i18n._("Distribution List Address"),
                    emptyText: i18n._("distributionlistrecipient@example.com"),
                    width: 450,
                    vtype: 'email',
                    allowBlank: false
                },
                {
                    xtype: 'textfield',
                    name: "Send To Address",
                    dataIndex: "address2",
                    fieldLabel: i18n._("Send To Address"),
                    emptyText: i18n._("quarantinelistowner@example.com"),
                    width: 450,
                    vtype: 'email',
                    allowBlank: false
                }]
            })]
        });
    },

    buildUserQuarantinesGrid: function() {
        this.userQuarantinesDetailsGrid = Ext.create('Ung.grid.Panel', {
            name: 'userQuarantinesDetailsGrid',
            selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
            hasEdit: false,
            hasAdd: false,
            hasDelete: false,
            columnMenuDisabled: false,
            dataFn: Ext.bind(function(handler) {
                if(this.userQuarantinesDetailsGrid != null && this.quarantinesDetailsWin.account!= null) {
                    this.getQuarantineMaintenenceView().getInboxRecords(Ext.bind(function(result, exception) {
                        if(result && result.list) {
                            for(var i=0; i< result.list.length; i++) {
                                /* copy values from mailSummary to object */
                                result.list[i].subject = result.list[i].mailSummary.subject;
                                result.list[i].sender = result.list[i].mailSummary.sender;
                                result.list[i].quarantineCategory = result.list[i].mailSummary.quarantineCategory;
                                result.list[i].quarantineDetail = result.list[i].mailSummary.quarantineDetail;
                                result.list[i].size = result.list[i].mailSummary.quarantineSize;
                            }
                        }
                        handler(result, exception);
                        //handler({list:[]}); //For testData
                    }, this), this.quarantinesDetailsWin.account);
                } else {
                    handler({ list: [] });
                }
            }, this),
            tbar: [{
                text: i18n._('Purge Selected'),
                tooltip: i18n._('Purge Selected'),
                iconCls: 'purge-icon',
                name: 'Purge Selected',
                handler: Ext.bind(function() {
                    var selectedRecords = this.userQuarantinesDetailsGrid.getSelectionModel().selected;
                    if(selectedRecords === undefined || selectedRecords.length == 0) {
                        return;
                    }
                    var emails = [];
                    selectedRecords.each(function(item, index, length) {
                        emails.push(item.data.mailID);
                    });

                    Ext.MessageBox.wait(i18n._("Purging..."), i18n._("Please wait"));
                    this.getQuarantineMaintenenceView().purge(Ext.bind(function(result, exception) {
                        Ext.MessageBox.hide();
                        if(Ung.Util.handleException(exception)) return;
                        //load Quarantines Details
                        this.userQuarantinesDetailsGrid.reload();
                        this.userQuarantinesGrid.reload();
                    }, this), this.quarantinesDetailsWin.account, emails);
                }, this)
            }, {
                text: i18n._('Release Selected'),
                tooltip: i18n._('Release Selected'),
                iconCls: 'release-icon',
                name: 'Release Selected',
                handler: Ext.bind(function() {
                    var selectedRecords = this.userQuarantinesDetailsGrid.getSelectionModel().selected;
                    if(selectedRecords === undefined || selectedRecords.length == 0) {
                        return;
                    }
                    var emails = [];
                    selectedRecords.each(function(item, index, length) {
                        emails.push(item.data.mailID);
                    });

                    Ext.MessageBox.wait(i18n._("Releasing..."), i18n._("Please wait"));
                    this.getQuarantineMaintenenceView().rescue(Ext.bind(function(result, exception) {
                        Ext.MessageBox.hide();
                        if(Ung.Util.handleException(exception)) return;
                        //load Quarantines Details
                        this.userQuarantinesDetailsGrid.reload();
                        this.userQuarantinesGrid.reload();
                    }, this), this.quarantinesDetailsWin.account, emails);
                }, this)
            }],
            plugins: ['gridfilters'],
            sortField: 'quarantinedDate',
            fields: [{
                name: 'mailID'
            }, {
                name: 'quarantinedDate',
                mapping: 'internDate'
            }, {
                name: 'size'
            }, {
                name: 'sender'
            }, {
                name: 'subject'
            }, {
                name: 'quarantineCategory'
            }, {
                name: 'quarantineDetail'
            }],
            columns: [//{
                //header: i18n._("MailID"),
                //width: 200,
                //dataIndex: 'mailID',
                //sortable: false
                //},
                {
                header: i18n._("Date"),
                width: 140,
                dataIndex: 'quarantinedDate',
                renderer: function(value) {
                    var date = new Date();
                    date.setTime( value );
                    return i18n.timestampFormat(value);
                }/*
                TODO: ext5
                ,
                filter: { type: 'datetime',
                    dataIndex: 'quarantinedDate',
                    date: {
                        format: 'Y-m-d'
                    },
                    time: {
                        format: 'H:i:s A',
                        increment: 1
                    },
                    validateRecord : function (record) {
                        var me = this,
                        key,
                        pickerValue,
                        val1 = record.get(me.dataIndex);

                        var val = new Date(val1.time);
                        if(!Ext.isDate(val)){
                            return false;
                        }
                        val = val.getTime();

                        for (key in me.fields) {
                            if (me.fields[key].checked) {
                                pickerValue = me.getFieldValue(key).getTime();
                                if (key == 'before' && pickerValue <= val) {
                                    return false;
                                }
                                if (key == 'after' && pickerValue >= val) {
                                    return false;
                                }
                                if (key == 'on' && pickerValue != val) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                  }*/
            }, {
                header: i18n._("Sender"),
                width: 180,
                dataIndex: 'sender',
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("Subject"),
                width: 150,
                flex: 1,
                dataIndex: 'subject',
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("Size (KB)"),
                width: 85,
                dataIndex: 'size',
                renderer: function(value) {
                    return i18n.numberFormat((value /1024.0).toFixed(3));
                },
                filter: {
                    type: 'numeric'
                }
            }, {
                header: i18n._("Category"),
                width: 85,
                dataIndex: 'quarantineCategory',
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("Detail"),
                width: 85,
                dataIndex: 'quarantineDetail',
                renderer: function(value) {
                    var detail = value;
                    if (isNaN(parseFloat(detail))) {
                        if (detail == "Message determined to be a fraud attempt") {
                            return i18n._("Phish");
                        }
                    } else {
                        return i18n.numberFormat(parseFloat(detail).toFixed(3));
                    }
                    return detail;
                },
                filter: {
                    type: 'numeric'
                }
            }]
        });
    },
    validate: function() {
        //validate Outgoing Server settings
        var hostCmp = Ext.getCmp('email_smtpHost');
        var portCmp = Ext.getCmp('email_smtpPort');
        var useAuthenticationCmp = Ext.getCmp('email_smtpUseAuthentication');
        var loginCmp = Ext.getCmp('email_smtpLogin');
        var passwordCmp = Ext.getCmp('email_smtpPassword');
        var fromAddressCmp = Ext.getCmp('email_fromAddress');

        //validate port
        if (!portCmp.isValid()) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("The port must be an integer number between 1 and 65535."),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelOutgoingServer);
                    portCmp.focus(true);
                }, this)
            );
            return false;
        }

        // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED
        if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length == 0) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Password must be specified if a Login is specified.'),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelOutgoingServer);
                    passwordCmp.focus(true);
                }, this)
            );
            return false;
        }
        else if(loginCmp.getValue().length == 0 && passwordCmp.getValue().length > 0) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Login must be specified if a Password is specified.'),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelOutgoingServer);
                    loginCmp.focus(true);
                }, this)
            );
            return false;
        }

        // CHECK THAT A HOSTNAME/IP IS GIVEN if using specified SMTP server
        if ((this.getMailSettings().sendMethod == "CUSTOM") && (hostCmp.getValue().length == 0)) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Server Address or Hostname must be specified for an SMTP server.'),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelOutgoingServer);
                    hostCmp.focus(true);
                }, this)
            );
            return false;
        }

        // CHECK THAT A FROM ADDRESS IS SPECIFIED
        if (fromAddressCmp.getValue().length == 0) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._('A From Address must be specified.'),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelOutgoingServer);
                    fromAddressCmp.focus(true);
                }, this)
            );
            return false;
        }

        // SAVE SETTINGS
        if (this.getMailSettings().sendMethod == "CUSTOM") {
            this.getMailSettings().smtpHost = hostCmp.getValue();
            this.getMailSettings().smtpPort = portCmp.getValue();
            //set login/password to blank if the 'Use Authentication' is not checked
            var useAuth = useAuthenticationCmp.getValue();
            this.getMailSettings().authUser = useAuth == true ? loginCmp.getValue(): '';
            this.getMailSettings().authPass = useAuth == true ? passwordCmp.getValue(): '';
        }
        this.getMailSettings().fromAddress = fromAddressCmp.getValue();
        return true;
    },

    save: function (isApply) {
        this.saveSemaphore = this.isMailLoaded() ? 2: 1;
        // save mail settings
        Ung.Main.getMailSender().setSettings(Ext.bind(function(result, exception) {
            this.afterSave(exception, isApply);
        }, this), this.getMailSettings());

        if( this.isMailLoaded() ) {
            var quarantineSettings = this.getMailAppSettings().quarantineSettings;
            // save mail node settings
            quarantineSettings.allowedAddressPatterns.javaClass="java.util.LinkedList";
            quarantineSettings.allowedAddressPatterns.list = this.quarantinableAddressesGrid.getList();
            quarantineSettings.addressRemaps.list = this.quarantineForwardsGrid.getList();
            this.getMailNode().setSmtpSettingsWithoutSafelists(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                // save global safelist
                if ( this.loadedGlobalSafelist == true) {
                    var gridSafelistGlobalValues = this.gridSafelistGlobal.getList();
                    var globalList = [];
                    for(var i=0; i<gridSafelistGlobalValues.length; i++) {
                        globalList.push(gridSafelistGlobalValues[i].emailAddress);
                    }
                    this.getSafelistAdminView().replaceSafelist(Ext.bind(function(result, exception) {
                        this.afterSave(exception, isApply);
                    }, this), 'GLOBAL', globalList);
                } else {
                    // Decrement the save semaphore
                    this.afterSave(null, isApply);
                }
            }, this), this.getMailAppSettings());
        }
    },
    afterSave: function(exception, isApply) {
        if(Ung.Util.handleException(exception)) return;

        this.saveSemaphore--;
        if (this.saveSemaphore == 0) {
            if(isApply) {
                this.getMailSettings(true);
                if( this.isMailLoaded() ) {
                    this.getMailAppSettings(true);
                }
                this.clearDirty();
                Ext.MessageBox.hide();
            } else {
                Ext.MessageBox.hide();
                this.closeWindow();
            }
        }
    }
});

//email address details window for Safe List and Quarantine
Ext.define('Webui.config.email.EmailAddressDetails', {
    extend: 'Ung.Window',
    settingsCmp: null,
    account: null,
    initComponent: function() {
        this.bbar= ['->',{
            name: 'Cancel',
            iconCls: 'cancel-icon',
            text: i18n._('Cancel'),
            handler: Ext.bind(function() {
                this.cancelAction();
            }, this)
        },'-',{
            name: 'Help',
            iconCls: 'icon-help',
            text: i18n._('Help'),
            handler: Ext.bind(function() {
                this.settingsCmp.helpAction();
            }, this)
        },'-'];
        this.callParent(arguments);
    }
});
//# sourceURL=email.js
