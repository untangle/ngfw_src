if (!Ung.hasResource["Ung.Email"]) {
    Ung.hasResource["Ung.Email"] = true;

    Ext.define('Ung.Email', {
        extend: 'Ung.ConfigWin',
        panelOutgoingServer: null,
        panelFromSafeList: null,
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
            // keep initial mail settings
            this.getMailSettings();
            this.buildOutgoingServer();
            if( this.isMailLoaded() ) {
                this.getMailNodeSettings();
                this.buildFromSafeList();
                this.buildQuarantine();
            }
            // builds the tab panel with the tabs
            var pageTabs = [this.panelOutgoingServer];
            if( this.isMailLoaded() ) {
                pageTabs.push( this.panelFromSafeList );
                pageTabs.push( this.panelQuarantine );
            }
            this.buildTabPanel(pageTabs);
            this.tabs.setActiveTab(this.panelOutgoingServer);
            this.callParent(arguments);
        },

        onRender: function(container, position) {
            this.callParent(arguments);
            Ext.defer(this.initSubCmps,1, this);
        },
        initSubCmps: function() {
            var smtpLoginCmp = Ext.getCmp('email_smtpLogin');
            var useAuthentication = smtpLoginCmp != null && smtpLoginCmp.getValue()!=null && smtpLoginCmp.getValue().length > 0;
            Ext.getCmp('email_smtpUseAuthentication').setValue(useAuthentication);
            Ext.getCmp('email_smtpLogin').setVisible(useAuthentication);
            Ext.getCmp('email_smtpPassword').setVisible(useAuthentication);
            
            var useSmtp = Ext.getCmp('email_smtpEnabled').getValue();
            if(useSmtp == false) {
                Ext.getCmp('email_smtpHost').disable();
                Ext.getCmp('email_smtpPort').disable();
                Ext.getCmp('email_smtpUseAuthentication').disable();
                Ext.getCmp('email_smtpLogin').disable();
                Ext.getCmp('email_smtpPassword').disable();
            }
            else {
                Ext.getCmp('email_smtpHost').enable();
                Ext.getCmp('email_smtpPort').enable();
                Ext.getCmp('email_smtpUseAuthentication').enable();
                Ext.getCmp('email_smtpLogin').enable();
                Ext.getCmp('email_smtpPassword').enable();
            }
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
            if (forceReload || this.rpc.mailNode === undefined) {
                try {
                    this.rpc.mailNode = rpc.nodeManager.node("untangle-casing-mail");
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.mailNode;
        },
        isMailLoaded: function(forceReload) {
            return this.getMailNode(forceReload) != null;
        },
        getMailSettings: function(forceReload) {
            if (forceReload || this.rpc.mailSettings === undefined) {
                try {
                    this.rpc.mailSettings = main.getMailSender().getSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.mailSettings;
        },
        getMailNodeSettings: function(forceReload) {
            if (forceReload || this.rpc.mailNodeSettings === undefined) {
                try {
                    this.rpc.mailNodeSettings = this.getMailNode().getMailNodeSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.mailNodeSettings;
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
            var hh = hours < 10 ? "0" + hours : hours;
            var mm = minutes < 10 ? "0" + minutes : minutes;
            return hh + ":" + mm;
        },
        sendTestMessage: function(emailAddress){
            var message = main.getMailSender().sendTestMessage( Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.testEmailResultMessage = (( result == true ) ? this.i18n._( 'Test email sent.  Check your mailbox for successful delivery.' ) : this.i18n._('Warning!  Test failed.  Check your settings.' ));
                var color = result === true ? 'green' : 'red';
                Ext.MessageBox.hide();
                Ext.getCmp('email-test-success').setText(this.testEmailResultMessage).setVisible(true).getEl().dom.style.color = color;
                
                
            }, this), emailAddress);                
        },        
        buildOutgoingServer: function() {
            this.panelOutgoingServer = Ext.create('Ext.panel.Panel', {
                name: 'Outgoing Server',
                helpSource: 'outgoing_server',
                parentId: this.getId(),
                title: this.i18n._('Outgoing Server'),
                cls: 'ung-panel',
                autoScroll: true,
                listeners: {
                    'activate': {
                        fn: function (){
                            Ext.create('Ext.tip.ToolTip',{
                                html: 'It is recommended to use a valid email address. (example: untangle@mydomain.com)',
                                target: 'email_fromAddress',
                                autoWidth: true,
                                autoHeight: true,
                                showDelay: 200,
                                dismissDelay: 0,
                                hideDelay: 0
                                });
                            Ext.create('Ext.tip.ToolTip',{
                                html: 'Some servers may require this but other servers may not support it.',
                                target: 'email_smtpUseAuthentication',
                                autoWidth: true,
                                autoHeight: true,
                                showDelay: 200,
                                dismissDelay: 0,
                                hideDelay: 0
                            })
                        }
                    }
                },

                onEmailTest: Ext.bind(function(saveBefore) {
                    var emailTestMessage = this.i18n._("Enter an email address to send a test message and then press \"Send\". That email account should receive an email shortly after running the test. If not, the email settings may not be correct.<br/><br/>It is recommended to verify that the email settings work for sending to both internal (your domain) and external email addresses.");
                    if(!this.emailMessageBox){
                        this.emailMessageBox = Ext.create('Ext.Window',{
                            layout: 'fit',
                            width: 500,
                            height: 300,
                            modal: true,
                            title: this.i18n._('Email Test'),
                            closeAction: 'hide',
                            plain: false,
                            items: Ext.create('Ext.panel.Panel',{
                                header: false,
                                border: false,
                                items: [{
                                    xtype: 'fieldset',
                                    height: 300,
                                    items: [{
                                        xtype: 'label',
                                        html: '<strong style="margin-bottom:20px;font-size:11px;display:block;">'+emailTestMessage+'</strong>',
                                        width: 400,
                                        height: 100
                                    },
                                    {
                                        xtype: 'textfield',
                                        name: 'Email Address',
                                        id: 'email-address-test',
                                        vtype: 'email',
                                        validateOnBlur: true,
                                        allowBlank: false,
                                        fieldLabel: this.i18n._('Email Address'),
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
                                handler: Ext.bind(function(){
                                    var emailAddress = Ext.getCmp('email-address-test');
                                    if(emailAddress.validate()===true){
                                        Ext.MessageBox.wait(this.i18n._('Sending Email...'), this.i18n._('Please wait'));
                                        this.sendTestMessage(emailAddress.getValue());
                                    }                                    
                                }, this)
                            },{
                                text: 'Close',
                                handler: Ext.bind(function(){
                                    this.emailMessageBox.destroy();
                                    this.emailMessageBox = null;
                                }, this)
                            }]
                        });
                    }else{
                        Ext.getCmp('email-address-test').setValue('').clearInvalid();                    
                        Ext.getCmp('email-test-success').setVisible(false);
                    }
                    this.emailMessageBox.show();
                }, this),

                defaults: {
                    xtype: 'fieldset',
                    autoHeight: true,
                    buttonAlign: 'left'
                },
                items: [{
                    title: this.i18n._('Outgoing Email Server (SMTP)'),
                    items: [{
                        cls: 'description',
                        border: false,
                        html: Ext.String.format(this.i18n
                                ._("The Outgoing Email Server settings determine how the {0} Server sends emails such as reports, quarantine digests, etc. <br/>In most cases the default setting should work. If not, specify an valid SMTP server that will relay mail for the {0} Server."),
                                main.getBrandingManager().getCompanyName())
                    }, {
                        xtype: 'radio',
                        id: 'email_smtpDisabled',
                        name: 'email_smtpEnabled',
                        boxLabel: this.i18n._('Send email directly (default)'),
                        hideLabel: true,
                        style: "margin-left: 50px;",
                        checked: this.getMailSettings().useMxRecords,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        this.getMailSettings().useMxRecords = true;
                                        Ext.getCmp('email_smtpHost').disable();
                                        Ext.getCmp('email_smtpPort').disable();
                                        Ext.getCmp('email_smtpUseAuthentication').disable();
                                        Ext.getCmp('email_smtpLogin').disable();
                                        Ext.getCmp('email_smtpPassword').disable();
                                    }
                                    else {
                                        this.getMailSettings().useMxRecords = false;
                                        Ext.getCmp('email_smtpHost').enable();
                                        Ext.getCmp('email_smtpPort').enable();
                                        Ext.getCmp('email_smtpUseAuthentication').enable();
                                        Ext.getCmp('email_smtpLogin').enable();
                                        Ext.getCmp('email_smtpPassword').enable();
                                    }
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'radio',
                        id: 'email_smtpEnabled',
                        name: 'email_smtpEnabled',
                        boxLabel: this.i18n._('Send Email using the specified SMTP Server'),
                        hideLabel: true,
                        style: "margin-left: 50px;",
                        checked: !this.getMailSettings().useMxRecords
                    }, {
                        xtype: 'fieldset',
                        height: 150,
                        style: "margin-left: 50px;",
                        items: [{
                            xtype: 'textfield',
                            labelWidth: 230,
                            labelAlign: 'right',                        
                            name: 'Server Address or Hostname',
                            id: 'email_smtpHost',
                            fieldLabel: this.i18n._('Server Address or Hostname'),
                            width: 400,
                            value: this.getMailSettings().smtpHost
                        }, {
                            xtype: 'textfield',
                            name: 'Server Port',
                            id: 'email_smtpPort',
                            fieldLabel: this.i18n._('Server Port'),
                            labelWidth: 230,
                            labelAlign: 'right',                        
                            width: 280,
                            value: this.getMailSettings().smtpPort,
                            vtype: "port"

                        }, {
                            xtype: 'checkbox',
                            labelWidth: 230,
                            labelAlign: 'right',                        
                            itemCls: 'left-indent-5',
                            name: 'Use Authentication',
                            id: 'email_smtpUseAuthentication',
                            fieldLabel: this.i18n._('Use Authentication.'),
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
                            fieldLabel: this.i18n._('Login'),
                            labelWidth: 230,
                            labelAlign: 'right',                        
                            width: 375,
                            value: this.getMailSettings().authUser
                        }, {
                            xtype: 'textfield',
                            hidden: true,
                            name: 'Password',
                            labelWidth: 230,
                            labelAlign: 'right',                        
                            id: 'email_smtpPassword',
                            inputType: 'password',
                            fieldLabel: this.i18n._('Password'),
                            width: 375,
                            value: this.getMailSettings().authPass
                        }]
                    }]
                }, {
                    title: this.i18n._('Email From Address'),
                    items: [{
                        cls: 'description',
                        border: false,
                        html: Ext.String.format(this.i18n._("The {0} Server will send email from this address."),
                                             main.getBrandingManager().getCompanyName())
                    }, {
                        xtype: 'textfield',
                        name: 'Email From Address',
                        id: 'email_fromAddress',
                        vtype: 'email',
                        hideLabel: true,
                        allowBlank: false,
                        width: 300,
                        value: this.getMailSettings().fromAddress
                     
                        
                    }]
                }, {
                    title: this.i18n._('Email Test'),
                    items: [{
                        cls: 'description',
                        border: false,
                        html: this.i18n._('The Email Test will send an email to a specified address with the current configuration. If the test email is not received your settings may be incorrect.')
                    },{
                        xtype: 'button',
                        text: this.i18n._("Email Test"),
                        iconCls: 'test-icon',
                        name: "emailTestButton",
                        handler: Ext.bind(function() {
                            if (Ung.Util.isDirty(this.panelOutgoingServer)) {
                                Ext.Msg.show({
                                   title: this.i18n._('Save Changes?'),
                                   msg: Ext.String.format(this.i18n._("Your current settings have not been saved yet.{0}Would you like to save your settings before executing the test?"), '<br>'),
                                   buttons: Ext.Msg.YESNOCANCEL,
                                   fn: Ext.bind(function(btnId) {
                                           if (btnId == 'yes') {
                                               if (this.validateOutgoingServer()) {
                                                Ext.MessageBox.wait(this.i18n._('Saving...'), this.i18n._('Please wait'));
                                                // save mail settings
                                                main.getMailSender().setSettings(Ext.bind(function(result, exception) {
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
        buildFromSafeList: function() {
            var showDetailColumn = Ext.create('Ext.grid.column.Action',{
                header: this.i18n._("Show Detail"),
                width: 100,
                iconCls: 'icon-detail-row',
                init: function(grid) {
                    this.grid = grid;
                },
                handler: function(view, rowIndex) {
                    var record = view.getStore().getAt(rowIndex);
                    // select current row
                    this.grid.getSelectionModel().select(record);
                    // show details
                    this.grid.onShowDetail(record);
                }
            });
            
            this.loadedGlobalSafelist = true;
            this.gridSafelistGlobal = Ext.create('Ung.EditorGrid', {
                name: 'Global',
                title: this.i18n._('Global'),
                hasEdit: false,
                settingsCmp: this,
                anchor: "100% 48%",
                height: 250,
                style: "margin-bottom:10px;",
                autoScroll: true,
                emptyRow: {
                    "emailAddress": this.i18n._("[no email address]")
                },
                fields: [ 
                {
                    name: 'id'
                },
                {
                    name: 'emailAddress'
                }],
                columns: [{
                    header: this.i18n._("email address"),
                    flex: 1,
                    width: 450,
                    dataIndex: 'emailAddress'
                }],
                sortField: 'emailAddress',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                    {
                        xtype: 'textfield',
                        name: "Email Address",
                        dataIndex: "emailAddress",
                        fieldLabel: this.i18n._("Email Address"),
                        allowBlank: false,
                        width: 300
                    }],
                dataRoot: '',
                dataFn: Ext.bind(function() { 
                    var safeListContents = this.getSafelistAdminView().getSafelistContents('GLOBAL');
                    var results=[]
                    if ( safeListContents && safeListContents.length > 0) {
                        for ( var i = 0; i < safeListContents.length; i++) {
                            results.push({emailAddress: safeListContents[i]});
                        }
                    }
                    this.loadedGlobalSafelist = true;
                    return results;
                }, this)
            });
                
            this.gridSafelistUser = Ext.create('Ung.EditorGrid', {
                name: 'Per User',
                title: this.i18n._('Per User'),
                selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
                hasEdit: false,
                hasAdd: false,
                hasDelete: false,
                settingsCmp: this,
                paginated: false,
                anchor: "100% 50%",
                height: 250,
                autoScroll: true,
                dataFn: this.getSafelistAdminView().getUserSafelistCounts,
                /*data: [//TODO: remove this after testing
                    {id: 5, emailAddress: "aaa@aaa.com", count: 353},
                    {id: 6, emailAddress: "bbb@bbb.com", count: 890}
                ],*/
                tbar: [{
                    xtype: 'button',
                    text: this.i18n._('Purge Selected'),
                    tooltip: this.i18n._('Purge Selected'),
                    iconCls: 'purge-icon',
                    name: 'Purge Selected',
                    parentId: this.getId(),
                    handler: Ext.bind(function() {
                        var selectedRecords = this.gridSafelistUser.getSelectionModel().selected;
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var accounts = [];
                        selectedRecords.each(function(item, index, length) {
                            accounts.push(item.data.emailAddress);
                        });
                        
                        Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                        this.getSafelistAdminView().deleteSafelists(Ext.bind(function(result, exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.gridSafelistUser.reloadGrid();
                        }, this), accounts);
                    }, this)
                }],
                fields: [{
                    name: 'id'
                }, {
                    name: 'emailAddress'
                }, {
                    name: 'count'
                }],
                columns: [{
                        header: this.i18n._("account address"),
                        width: 350,
                        dataIndex: 'emailAddress',
                        flex: 1
                    }, {
                        header: this.i18n._("safe list size"),
                        width: 150,
                        align: 'right',
                        dataIndex: 'count'
                    }, showDetailColumn],
                plugins: [showDetailColumn],
                sortField: 'emailAddress',
                columnsDefaultSortable: true,
                
                onShowDetail: Ext.bind(function(record) {
                    if(!this.safelistDetailsWin) {
                        this.buildGridSafelistUserDetails();
                        this.safelistDetailsWin = Ext.create('Ung.EmailAddressDetails', {
                            detailsPanel: this.gridSafelistUserDetails,
                            settingsCmp: this,
                            showForCurrentAccount: function(emailAddress) {
                                this.account = emailAddress;  
                                var newTitle = this.settingsCmp.i18n._('Email From-SafeList Details for: ') + emailAddress;
                                this.setTitle(newTitle);
                                
                                this.show();
                                Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
                                this.settingsCmp.getSafelistAdminView().getSafelistContents(
                                    Ext.bind(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.settingsCmp.gridSafelistUserDetails.reloadGrid({data: result});
                                    }, this), emailAddress); 
                            }          
                        });
                        this.subCmps.push(this.safelistDetailsWin);
                    }

                    this.safelistDetailsWin.showForCurrentAccount(record.get('emailAddress'));
                }, this)
            });
                
            this.panelFromSafeList = Ext.create('Ext.panel.Panel', {
                name: 'From-Safe List',
                helpSource: 'from_safe_list',
                parentId: this.getId(),
                title: this.i18n._('From-Safe List'),
                height: 550,
                cls: 'ung-panel',
                autoScroll: true,
                items: [this.gridSafelistGlobal, this.gridSafelistUser]
            });
        },
        buildGridSafelistUserDetails: function() {
            this.gridSafelistUserDetails = Ext.create('Ung.EditorGrid',{
                anchor: "100% 100%",
                name: 'gridSafelistUserDetails',
                selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
                hasEdit: false,
                hasAdd: false,
                hasDelete: false,
                paginated: false,
                tbar: [{
                    text: this.i18n._('Purge Selected'),
                    tooltip: this.i18n._('Purge Selected'),
                    iconCls: 'purge-icon',
                    name: 'Purge Selected',
                    parentId: this.getId(),
                    handler: Ext.bind(function() {
                        var selectedRecords = this.gridSafelistUserDetails.getSelectionModel().selected;
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var senders = [];
                        selectedRecords.each(function(item, index, length) {
                            senders.push(item.data.sender);
                        });
                        
                        Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                        this.getSafelistAdminView().removeFromSafelists(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            this.gridSafelistUserDetails.reloadGrid({data: result});
                            Ext.MessageBox.hide();
                        }, this), this.safelistDetailsWin.account, senders);
                        
                        this.gridSafelistUser.reloadGrid();
                    }, this)
                }],
                data: [],
                fields: ['sender'],
                columns: [{
                    header: this.i18n._("email address"),
                    flex: 1,
                    width: 400,
                    dataIndex: 'sender'
                }],
                sortField: 'sender',
                columnsDefaultSortable: true
            });
        },

        buildQuarantine: function() {
            var showDetailColumn = Ext.create('Ext.grid.column.Action', {
                header: this.i18n._("Show Detail"),
                width: 100,
                iconCls: 'icon-detail-row',
                init: function(grid) {
                    this.grid = grid;
                },
                handler: function(view, rowIndex) {
                    var record = view.getStore().getAt(rowIndex);
                    // select current row
                    this.grid.getSelectionModel().select(record);
                    // show details
                    this.grid.onShowDetail(record);
                }
            });
            
            this.panelQuarantine = Ext.create('Ext.panel.Panel',{
                name: 'panelQuarantine',
                helpSource: 'quarantine',
                parentId: this.getId(),
                title: this.i18n._('Quarantine'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    anchor: "98% 25%",
                    autoHeight: true,
                    autoScroll: true,
                    buttonAlign: 'left'
                },
                items: [{
                    items: [{
                        xtype: 'textfield',
                        labelWidth: 230,
                        labelAlign: 'left',
                        name: 'Maximum Holding Time (days) (max 36)',
                        fieldLabel: this.i18n._('Maximum Holding Time (days) (max 36)'),
                        allowBlank: false,
                        value: this.getMailNodeSettings().quarantineSettings.maxMailIntern/(1440*60*1000),
                        regex: /^([0-9]|[0-2][0-9]|3[0-6])$/,
                        regexText: this.i18n._('Maximum Holding Time must be a number in range 0-36'),
                        width: 270,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    var millisecValue = newValue * 1440*60*1000;
                                    this.getMailNodeSettings().quarantineSettings.maxMailIntern = millisecValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'checkbox',
                        name: 'Send Daily Quarantine Digest Emails',
                        id: 'quarantine_sendDailyDigest',
                        labelWidth: 230,
                        labelAlign: 'left',
                        fieldLabel: this.i18n._('Send Daily Quarantine Digest Emails'),
                        checked: this.getMailNodeSettings().quarantineSettings.sendDailyDigests,
                        width: 320,
                        listeners: {
                            "render": {
                                fn: Ext.bind(function(elem) {
                                        if(elem.getValue()){
                                        Ext.getCmp('quarantine_dailySendingTime').enable();
                                    }else{
                                        Ext.getCmp('quarantine_dailySendingTime').disable();
                                    }
                                }, this)
                            },
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getMailNodeSettings().quarantineSettings.sendDailyDigests = newValue;
                                    if(newValue){
                                        Ext.getCmp('quarantine_dailySendingTime').enable();
                                    }else{
                                        Ext.getCmp('quarantine_dailySendingTime').disable();
                                    }
                                }, this)
                            }

                        }
                    }, {
                        xtype: 'timefield',
                        name: 'Digest Sending Time',
                        id: 'quarantine_dailySendingTime',
                        labelWidth: 230,
                        labelAlign: 'left',
                        fieldLabel: this.i18n._('Quarantine Digest Sending Time'),
                        allowBlank: false,
                        // format: "H:i",
                        increment: 1,
                        width: 320,
                        value: this.getFormattedTime(this.getMailNodeSettings().quarantineSettings.digestHourOfDay, this.getMailNodeSettings().quarantineSettings.digestMinuteOfDay),
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    var dt = Ext.Date.parse(newValue, "g:i A");
                                    if (dt) { 
                                        this.getMailNodeSettings().quarantineSettings.digestHourOfDay = dt.getHours();
                                        this.getMailNodeSettings().quarantineSettings.digestMinuteOfDay = dt.getMinutes();
                                    }
                                }, this)
                            }
                        }
                    }, {
                        cls: 'description',
                        border: false,
                        html: Ext.String.format(this.i18n._('Users can also request Quarantine Digest Emails manually at this link: <b>https://{0}/quarantine/</b>'),
                         /*rpc.networkManager.getPublicAddress()*/"TODO:replace with valid PublicAddress string after networkManager refactoring!")
                    }]
                }, {
                    title: this.i18n._('User Quarantines'),
                    layout: "anchor",
                    items: [ this.userQuarantinesGrid = Ext.create('Ung.EditorGrid',{
                        anchor: "100% 100%",
                        name: 'User Quarantines',
                        selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
                        hasEdit: false,
                        hasAdd: false,
                        hasDelete: false,
                        paginated: false,
                        autoGenerateId: true,
                        settingsCmp: this,
                        height: 250,
                        autoScroll: true,
                        dataFn: this.getQuarantineMaintenenceView().listInboxes,
                        /*data: [//TODO: remove this after testing
                            {id: 34, address: "aaa@ddd.com", numMails: 115, totalSz: 150124},
                            {id: 35, address: "bbb@ddd.com", numMails: 76, totalSz: 560777}
                        ],*/
                        tbar: [{
                            text: this.i18n._('Purge Selected'),
                            tooltip: this.i18n._('Purge Selected'),
                            iconCls: 'purge-icon',
                            name: 'Purge Selected',
                            parentId: this.getId(),
                            handler: Ext.bind(function() {
                                var selectedRecords = this.userQuarantinesGrid.getSelectionModel().selected;
                                if(selectedRecords === undefined || selectedRecords.length == 0) {
                                    return;
                                }
                                var accounts = [];
                                selectedRecords.each(function(item, index, length) {
                                    accounts.push(item.data.address);
                                });
                                
                                Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                                this.getQuarantineMaintenenceView().deleteInboxes(Ext.bind(function(result, exception) {
                                    Ext.MessageBox.hide();
                                    if(Ung.Util.handleException(exception)) return;
                                    this.userQuarantinesGrid.reloadGrid();
                                }, this), accounts);
                            }, this)
                        }, {
                            text: this.i18n._('Release Selected'),
                            tooltip: this.i18n._('Release Selected'),
                            iconCls: 'release-icon',
                            name: 'Release Selected',
                            parentId: this.getId(),
                            handler: Ext.bind(function() {
                                var selectedRecords = this.userQuarantinesGrid.getSelectionModel().selected;
                                if(selectedRecords === undefined || selectedRecords.length == 0) {
                                    return;
                                }
                                var accounts = [];
                                selectedRecords.each(function(item, index, length) {
                                    accounts.push(item.data.address);
                                });
                                
                                Ext.MessageBox.wait(this.i18n._("Releasing..."), this.i18n._("Please wait"));
                                this.getQuarantineMaintenenceView().rescueInboxes(Ext.bind(function(result, exception) {
                                    Ext.MessageBox.hide();
                                    if(Ung.Util.handleException(exception)) return;
                                    this.userQuarantinesGrid.reloadGrid();
                                }, this), accounts);
                            }, this)
                        }, {
                            xtype: 'tbfill'
                        }, {
                            xtype: 'tbtext', 
                            text: Ext.String.format(this.i18n._('Total Disk Space Used: {0} MB'), i18n.numberFormat((this.getQuarantineMaintenenceView().getInboxesTotalSize()/(1024 * 1024)).toFixed(3)))
                        }],
                        fields: [{
                            name: 'address'
                        }, {
                            name: 'numMails'
                        }, {
                            name: 'totalSz'
                        }],
                        columns: [
                        {
                            header: this.i18n._("account address"),
                            width: 200,
                            dataIndex: 'address',
                            flex: 1
                            
                        }, {
                            header: this.i18n._("message count"),
                            width: 185,
                            align: 'right',
                            dataIndex: 'numMails'
                        }, {
                            header: this.i18n._("data size (kB)"),
                            width: 185,
                            align: 'right',
                            dataIndex: 'totalSz',
                            renderer: function(value) {
                                return i18n.numberFormat((value /1024.0).toFixed(3));
                            }
                        }, showDetailColumn],
                        plugins: [showDetailColumn],
                        sortField: 'address',
                        columnsDefaultSortable: true,
                        
                        onShowDetail: Ext.bind(function(record) {
                            if(!this.quarantinesDetailsWin) {
                                this.buildUserQuarantinesGrid();
                                this.quarantinesDetailsWin = Ext.create('Ung.EmailAddressDetails', {
                                    detailsPanel: this.userQuarantinesDetailsGrid,
                                    settingsCmp: this,
                                    showForCurrentAccount: function(emailAddress) {
                                        this.account = emailAddress;  
                                        var newTitle = this.settingsCmp.i18n._('Email Quarantine Details for: ') + emailAddress;
                                        this.setTitle(newTitle);
                                        this.show();
                                        //load Quarantines Details
                                        this.settingsCmp.userQuarantinesDetailsGrid.reloadGrid();
                                    }          
                                });
                                this.subCmps.push(this.quarantinesDetailsWin);
                            }
                            this.quarantinesDetailsWin.showForCurrentAccount(record.get('address'));
                        }, this)
                    })
                ]}, {
                    title: this.i18n._('Quarantinable Addresses'),
                    layout: "anchor",
                    items: [{
                        cls: 'description',
                        border: false,
                        html: this.i18n._('Email addresses on this list will have quarantines automatically created. All other emails will be marked and not quarantined.')
                    },
                    this.quarantinableAddressesGrid = Ext.create('Ung.EditorGrid',{
                        anchor: "100%",
                        name: 'Quarantinable Addresses',
                        settingsCmp: this,
                        height: 250,
                        autoScroll: true,
                        autoGenerateId: true,
                        paginated: false,
                        emptyRow: {
                            "address": "email@example.com",
                            "category": this.i18n._("[no category]"),
                            "description": this.i18n._("[no description]"),
                            "javaClass": "com.untangle.node.mail.papi.EmailAddressRule"
                        },
                        recordJavaClass: "com.untangle.node.mail.papi.EmailAddressRule",
                        dataFn: Ext.bind(function() { 
                            return this.getMailNodeSettings().quarantineSettings.allowedAddressPatterns;
                        }, this),
                        fields: [{
                            name: 'id'
                        },{
                            name: 'address'
                        }],
                        columns: [{
                            header: this.i18n._("quarantinable address"),
                            flex: 1,
                            width: 400,
                            dataIndex: 'address',
                            editor: {
                                xtype: 'textfield'
                            }
                        }],
                        rowEditorInputLines: [{
                            xtype: 'textfield',
                            name: "Address",
                            dataIndex: "address",
                            fieldLabel: this.i18n._("Address"),
                            allowBlank: false,
                            width: 450
                        }]
                    })]
                }, {
                    title: this.i18n._('Quarantine Forwards'),
                    items: [{
                        cls: 'description',
                        border: false,
                        html: this.i18n._('This is a list of email addresses whose quarantine digest gets forwarded to another account. This is common for distribution lists where the whole list should not receive the digest.')
                    }, this.quarantineForwardsGrid = Ext.create('Ung.EditorGrid',{
                        name: 'Quarantine Forwards',
                        settingsCmp: this,
                        height: 250,
                        autoScroll: true,
                        paginated: false,
                        emptyRow: {
                            "address1": this.i18n._("distributionlistrecipient@example.com"),
                            "address2": this.i18n._("quarantinelistowner@example.com"),
                            "category": this.i18n._("[no category]"),
                            "description": this.i18n._("[no description]"),
                            "javaClass": "com.untangle.node.mail.papi.EmailAddressPairRule"
                        },
                        recordJavaClass: "com.untangle.node.mail.papi.EmailAddressPairRule",
                        dataFn: Ext.bind( function() { 
                            return this.getMailNodeSettings().quarantineSettings.addressRemaps;
                        }, this),
                        fields: [{
                            name: 'id'
                        }, {
                            name: 'address1'
                        }, {
                            name: 'address2'
                        }, {
                            name: 'category'
                        }, {
                            name: 'description'
                        }],
                        columns: [
                        {
                            header: this.i18n._("distribution list address"),
                            width: 250,
                            dataIndex: 'address1',
                            editor: {
                                xtype: 'textfield'
                            }
                        }, 
                        {
                            header: this.i18n._("send to address"),
                            width: 250,
                            dataIndex: 'address2',
                            flex:1,
                            editor: {
                                xtype: 'textfield'
                            }
                        }],
                        rowEditorLabelWidth: 160,
                        rowEditorInputLines: [
                        {
                            xtype: 'textfield',
                            name: "Distribution List Address",
                            dataIndex: "address1",
                            fieldLabel: this.i18n._("Distribution List Address"),
                            width: 450
                        },
                        {
                            xtype: 'textfield',
                            name: "Send To Address",
                            dataIndex: "address2",
                            fieldLabel: this.i18n._("Send To Address"),
                            width: 450
                        },
                        {
                            xtype: 'textfield',
                            name: "Category",
                            dataIndex: "category",
                            fieldLabel: this.i18n._("Category"),
                            width: 450
                        }, 
                        {
                            xtype: 'textfield',
                            name: "Description",
                            dataIndex: "description",
                            fieldLabel: this.i18n._("Description"),
                            width: 450
                        }]
                    })]
                }]
            });
        },
        
        buildUserQuarantinesGrid: function() {
            this.userQuarantinesDetailsGrid = Ext.create('Ung.EditorGrid', {
                anchor: "100% 100%",
                name: 'userQuarantinesDetailsGrid',
                selModel: Ext.create('Ext.selection.CheckboxModel', {singleSelect: false}),
                hasEdit: false,
                hasAdd: false,
                hasDelete: false,
                autoGenerateId: true,
                
                tbar: [{
                    text: this.i18n._('Purge Selected'),
                    tooltip: this.i18n._('Purge Selected'),
                    iconCls: 'purge-icon',
                    name: 'Purge Selected',
                    parentId: this.getId(),
                    handler: Ext.bind(function() {
                        var selectedRecords = this.userQuarantinesDetailsGrid.getSelectionModel().selected;
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var emails = [];
                        selectedRecords.each(function(item, index, length) {
                            emails.push(item.data.mailID);
                        });
                        
                        Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                        this.getQuarantineMaintenenceView().purge(Ext.bind(function(result, exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            //load Quarantines Details
                            this.userQuarantinesDetailsGrid.reloadGrid();
                            this.userQuarantinesGrid.reloadGrid();
                        }, this), this.quarantinesDetailsWin.account, emails);
                        
                    }, this)
                }, {
                    text: this.i18n._('Release Selected'),
                    tooltip: this.i18n._('Release Selected'),
                    iconCls: 'release-icon',
                    name: 'Release Selected',
                    parentId: this.getId(),
                    handler: Ext.bind(function() {
                        var selectedRecords = this.userQuarantinesDetailsGrid.getSelectionModel().selected;
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var emails = [];
                        selectedRecords.each(function(item, index, length) {
                            emails.push(item.data.mailID);
                        });
                        
                        Ext.MessageBox.wait(this.i18n._("Releasing..."), this.i18n._("Please wait"));
                        this.getQuarantineMaintenenceView().rescue(Ext.bind(function(result, exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            //load Quarantines Details
                            this.userQuarantinesDetailsGrid.reloadGrid();
                            this.userQuarantinesGrid.reloadGrid();
                        }, this), this.quarantinesDetailsWin.account, emails);
                        
                    }, this)
                }],
                dataRoot: '',
                dataFn: Ext.bind(function() {
                    /*return [//just for test
                        {mailID:"aaa", mailSummary: {subject:"a aaa"+Math.random(), sender: "aaa@fff.com", category: "cat a", size: 10354}},
                        {mailID:"bbb", mailSummary: {subject:"b bbb"+Math.random(), sender: "bbb@fff.com", category: "cat b", size: 40300}}
                    ];*/
                    if(this.userQuarantinesDetailsGrid != null) {
                        return this.getQuarantineMaintenenceView().getInboxRecords(this.quarantinesDetailsWin.account, 0, Ung.Util.maxRowCount, []);
                    } else {
                        return [];
                    }
                }, this),
                initialLoad: function() {}, //do nothing here
                fields: [{
                    name: 'mailID'
                }, {
                    name: 'quarantinedDate',
                    mapping: 'internDateAsDate'
                }, {
                    name: 'size'
                }, {
                    name: 'sender',
                    mapping: 'mailSummary'
                }, {
                    name: 'subject',
                    mapping: 'mailSummary'
                }, {
                    name: 'category',
                    mapping: 'mailSummary'
                }, {
                    name: 'quarantineDetail',
                    mapping: 'mailSummary'
                }],
                columns: [{
                    header: this.i18n._("MailID"),
                    width: 150,
                    dataIndex: 'mailID',
                    sortable: false
                }, {
                    header: this.i18n._("Date"),
                    width: 150,
                    dataIndex: 'quarantinedDate',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("Sender"),
                    width: 150,
                    dataIndex: 'sender',
                    renderer: function(value) {
                        return value.sender;
                    }
                }, {
                    header: this.i18n._("Subject"),
                    width: 150,
                    flex: 1,
                    dataIndex: 'subject',
                    renderer: function(value) {
                        return value.subject;
                    }
                }, {
                    header: this.i18n._("Size (KB)"),
                    width: 85,
                    dataIndex: 'size',
                    renderer: function(value) {
                        return i18n.numberFormat((value /1024.0).toFixed(3));
                    }
                    
                }, {
                    header: this.i18n._("Category"),
                    width: 85,
                    dataIndex: 'category',
                    sortable: false,
                    renderer: function(value) {
                        return value.quarantineCategory;
                    }
                }, {
                    header: this.i18n._("Detail"),
                    width: 85,
                    dataIndex: 'quarantineDetail',
                    renderer: function(value) {
                        var detail = value.quarantineDetail;
                        if (isNaN(parseFloat(detail))) {
                            if (detail == "Message determined to be a fraud attempt") {
                                return this.i18n._("Identity Theft");
                            }
                        } else {
                            return i18n.numberFormat(parseFloat(detail).toFixed(3));
                        }
                        return detail;
                    }
                }],
                sortField: 'quarantinedDate',
                columnsDefaultSortable: true
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
                Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        portCmp.focus(true);
                    }, this) 
                );
                return false;
            }
            
            // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED
            if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length == 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Password" must be specified if a "Login" is specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        passwordCmp.focus(true);
                    }, this) 
                );
                return false;
            }
            else if(loginCmp.getValue().length == 0 && passwordCmp.getValue().length > 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Login" must be specified if a "Password" is specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        loginCmp.focus(true);
                    }, this) 
                );
                return false;
            }
            
            // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
            if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length > 0 && hostCmp.getValue().length == 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Hostname" must be specified if "Login" or "Password" are specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        hostCmp.focus(true);
                    }, this) 
                );
                return false;
            }

            // CHECK THAT A FROM ADDRESS IS SPECIFIED
            if (fromAddressCmp.getValue().length == 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "From Address" must be specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        fromAddressCmp.focus(true);
                    }, this) 
                );
                return false;
            }
            
            // SAVE SETTINGS
            if (!this.getMailSettings().useMxRecords) {
                this.getMailSettings().smtpHost = hostCmp.getValue();  
                this.getMailSettings().smtpPort = portCmp.getValue();  
                //set login/password to blank if the 'Use Authentication' is not checked
                var useAuth = useAuthenticationCmp.getValue();
                this.getMailSettings().authUser = useAuth == true ? loginCmp.getValue() : '';  
                this.getMailSettings().authPass = useAuth == true ? passwordCmp.getValue() : '';  
            }
            this.getMailSettings().fromAddress = fromAddressCmp.getValue();
                
            return true;
        },
        
        applyAction: function()
        {
            this.commitSettings(Ext.bind(this.reloadSettings, this));
        },
        reloadSettings: function()
        {
            this.getMailSettings(true);
            this.clearDirty();
            Ext.MessageBox.hide();
        },
        // save function
        saveAction: function()
        {
            this.commitSettings(Ext.bind(this.completeSaveAction, this));
        },
        completeSaveAction: function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
        },
        commitSettings: function(callback) {
            if (!this.validate()) {
                return;
            }

            this.saveSemaphore = this.isMailLoaded() ? 3 : 1;
            Ext.MessageBox.show({
                title: this.i18n._('Please wait'),
                msg: this.i18n._('Saving...'),
                modal: true,
                wait: true,
                waitConfig: {interval: 100},
                progressText: " ",
                width: 200
            });
                
            // save mail settings
            main.getMailSender().setSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception,callback);
            }, this), this.getMailSettings());
            
            if( this.isMailLoaded() ) {
                var quarantineSettings = this.getMailNodeSettings().quarantineSettings;
                // save mail node settings 
                quarantineSettings.allowedAddressPatterns.javaClass="java.util.ArrayList";
                quarantineSettings.allowedAddressPatterns.list = this.quarantinableAddressesGrid.getFullSaveList();
                quarantineSettings.addressRemaps.list = this.quarantineForwardsGrid.getFullSaveList();
                
                delete quarantineSettings.secretKey;

                this.getMailNode().setMailNodeSettingsWithoutSafelists(Ext.bind(function(result, exception) {
                    this.afterSave(exception,callback);
                }, this), this.getMailNodeSettings());
                
                // save global safelist
                if ( this.loadedGlobalSafelist == true ) {
                    var gridSafelistGlobalValues = this.gridSafelistGlobal.getFullSaveList();
                    var globalList = [];
                    for(var i=0; i<gridSafelistGlobalValues.length; i++) {
                        globalList.push(gridSafelistGlobalValues[i].emailAddress);
                    }
                    this.getSafelistAdminView().replaceSafelist(Ext.bind(function(result, exception) {
                        this.afterSave(exception,callback);
                    }, this), 'GLOBAL', globalList);
                } else {
                    /* Decrement the save semaphore */
                    this.afterSave(null,callback);
                }
            }            
        },
        afterSave: function(exception,callback) {
            if(Ung.Util.handleException(exception)) return;

            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                callback();
            }
        }
    });
    
    //email address details window for Safe List and Quarantine
    Ext.define('Ung.EmailAddressDetails', {
        extend: 'Ung.Window',
        settingsCmp: null,
        // the certPanel
        detailsPanel: null,
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
                text: this.settingsCmp.i18n._('Help'),
                handler: Ext.bind(function() {
                    this.settingsCmp.helpAction();
                }, this)
            },'-'];
            this.items=this.detailsPanel;
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.hide();
        },
        // to override
        showForCurrentAccount: function(emailAddress) {
            this.account = emailAddress;  
            this.show();
        },            
        // the proceed actions
        // to override
        proceedAction: function() {
            main.todo();
        }
    });
}
//@ sourceURL=emailNew.js
