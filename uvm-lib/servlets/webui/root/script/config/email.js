if (!Ung.hasResource["Ung.Email"]) {
    Ung.hasResource["Ung.Email"] = true;

    Ung.Email = Ext.extend(Ung.ConfigWin, {
        panelOutgoingServer : null,
        panelFromSafeList : null,
        panelQuarantine : null,
        globalSafelistGrid : null,
        userSafelistGrid : null,
        userSafelistDetailsGrid : null,
        quarantinableAddressesGrid : null,
        quarantineForwardsGrid : null,
        userQuarantinesGrid :null,        
        safelistDetailsWin : null,
        quarantinesDetailsWin : null,        
        
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Email')
            }];
            Ung.Email.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Email.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 5 tabs
        },
        initSubCmps : function() {
            this.buildOutgoingServer();
            if( this.isMailLoaded() ) {
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
            this.tabs.activate(this.panelOutgoingServer);
            
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
            
            var smtpLoginCmp = Ext.getCmp('email_smtpLogin');
            var useAuthentication = smtpLoginCmp != null && smtpLoginCmp.getValue().length > 0;
            Ext.getCmp('email_smtpUseAuthentication').setValue(useAuthentication);
            Ext.getCmp('email_smtpLogin').setContainerVisible(useAuthentication);
            Ext.getCmp('email_smtpPassword').setContainerVisible(useAuthentication);
        },
        
        getMailNode : function(forceReload) {
            if (forceReload || this.rpc.mailNode === undefined) {
                this.rpc.mailNode = rpc.nodeManager.node("untangle-casing-mail");
            }
            return this.rpc.mailNode;
        },
        isMailLoaded : function(forceReload) {
            return this.getMailNode(forceReload) != null;
        },
        getMailSettings : function(forceReload) {
            if (forceReload || this.rpc.mailSettings === undefined) {
                this.rpc.mailSettings = main.getMailSender().getMailSettings();
            }
            return this.rpc.mailSettings;
        },
        getMailNodeSettings : function(forceReload) {
            if (forceReload || this.rpc.mailNodeSettings === undefined) {
                this.rpc.mailNodeSettings = this.getMailNode().getMailNodeSettings();
            }
            return this.rpc.mailNodeSettings;
        },
        getSafelistAdminView : function(forceReload) {
            if (forceReload || this.rpc.safelistAdminView === undefined) {
                this.rpc.safelistAdminView = this.getMailNode().getSafelistAdminView();
            }
            return this.rpc.safelistAdminView;
        },
        getQuarantineMaintenenceView : function(forceReload) {
            if (forceReload || this.rpc.quarantineMaintenenceView === undefined) {
                this.rpc.quarantineMaintenenceView = this.getMailNode().getQuarantineMaintenenceView();
            }
            return this.rpc.quarantineMaintenenceView;
        }, 
        getFormattedTime: function(hours, minutes) {
            var hh = hours < 10 ? "0" + hours : hours;
            var mm = minutes < 10 ? "0" + minutes : minutes;
            return hh + ":" + mm;
        },
        loadQuarantinesDetails : function() {
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            this.getQuarantineMaintenenceView().getInboxRecordArray(
                function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.userQuarantinesDetailsGrid.store.loadData(result.inboxRecords);
                }.createDelegate(this), this.quarantinesDetailsWin.account)
        },
        
        
        buildOutgoingServer : function() {
            this.panelOutgoingServer = new Ext.Panel({
                // private fields
                name : 'Outgoing Server',
                parentId : this.getId(),
                title : this.i18n._('Outgoing Server'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                
                onEmailTest : function() {
                    this.emailTestMessage = "<center>" + 
                            this.i18n._("Enter an email address which you would like to send a test message to, and then press \"Proceed\".  You should receive an email shortly after running the test.  If not, your email settings may not be correct.") + 
                            "</center>";
                    if( this.validateOutgoingServer() ) {
	                    Ext.MessageBox.show({
	                        title : this.i18n._('Email Test'),
	                        buttons : { 
                                cancel:this.i18n._('Close'), 
                                ok:this.i18n._('Proceed') 
                            },
                            msg : this.emailTestMessage,
	                        modal : true,
                            prompt : true,
	                        fn: function(btn, emailAddress){
							    if (btn == 'ok'){
                                    Ext.MessageBox.show({
                                        title : this.i18n._('Email Test'),
                                        msg : this.emailTestMessage,
			                            buttons : { 
			                                cancel:this.i18n._('Close'), 
			                                ok:this.i18n._('Proceed') 
			                            },
                                        modal : true,
                                        prompt : true,
                                        progress : true,
                                        wait : true,
                                        waitConfig: {interval: 100},
                                        progressText : this.i18n._('Sending...'),
                                        value : emailAddress,
                                        width : 450
                                    });
			                        var message = rpc.adminManager.sendTestMessage( function(result, exception) {
			                            if (exception) {
			                                Ext.MessageBox.alert(i18n._("Failed"), exception.message);
			                                return;
			                            }
			                            this.testEmailResultMessage = result == true ? this.i18n._('Test email sent.') : this.i18n._('Warning!  Test failed.  Check your settings.');
			    
			                            Ext.MessageBox.show({
			                                title : this.i18n._('Email Test'),
			                                msg : this.emailTestMessage,
	                                        buttons : { 
	                                            cancel:this.i18n._('Close'), 
	                                            ok:this.i18n._('Proceed') 
	                                        },
			                                modal : true,
                                            prompt : true,
			                                progress : true,
			                                waitConfig: {interval: 100},
			                                progressText : this.testEmailResultMessage,
                                            value : emailAddress,
			                                width : 450
			                            });
			                        }.createDelegate(this), emailAddress);
							    }
                            }.createDelegate(this),
                            progress : true,
	                        progressText : ' ',
	                        width : 450
	                    });
                    }
                }.createDelegate(this),

                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Outgoing Email Server (SMTP)'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n
                                ._("The Outgoing Email Server settings determine how the Untangle Server sends emails such as reports, quarantine digests, etc. In most cases the default setting should work, but if not, you should specify an SMTP server that will relay mail for the  Untangle Server.")
                    }, {
                        xtype : 'radio',
                        id : 'email_smtpDisabled',
                        name : 'email_smtpEnabled',
                        boxLabel : this.i18n._('Send email directly (default)'),
                        hideLabel : true,
                        style : "margin-left: 50px;",
                        checked : this.getMailSettings().useMxRecords,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
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
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'radio',
                        id : 'email_smtpEnabled',
                        name : 'email_smtpEnabled',
                        boxLabel : this.i18n._('Send Email using the specified SMTP Server'),
                        hideLabel : true,
                        style : "margin-left: 50px;",
                        checked : !this.getMailSettings().useMxRecords
                    }, {
                        xtype : 'fieldset',
                        height : 150,
                        style : "margin-left: 75px;",
                        items : [{
                            xtype : 'textfield',
                            name : 'Server Address or Hostname',
                            id : 'email_smtpHost',
                            fieldLabel : this.i18n._('Server Address or Hostname'),
                            labelStyle : 'text-align: right; width: 200px;',
                            width : 200,
                            value : this.getMailSettings().smtpHost
                        }, {
                            xtype : 'textfield',
                            name : 'Server Port',
                            id : 'email_smtpPort',
                            fieldLabel : this.i18n._('Server Port'),
                            labelStyle : 'text-align: right; width: 200px;',
                            width : 50,
                            value : this.getMailSettings().smtpPort,
                            vtype : "port"

                        }, {
                            xtype : 'checkbox',
                            name : 'Use Authentication',
                            id : 'email_smtpUseAuthentication',
                            boxLabel : this.i18n._('Use Authentication'),
                            hideLabel : true,
                            style : "margin-left: 100px;",
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
	                                    Ext.getCmp('email_smtpLogin').setContainerVisible(checked);
	                                    Ext.getCmp('email_smtpPassword').setContainerVisible(checked);
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            xtype : 'textfield',
                            name : 'Login',
                            id : 'email_smtpLogin',
                            fieldLabel : this.i18n._('Login'),
                            labelStyle : 'text-align: right; width: 200px;',
                            width : 175,
                            value : this.getMailSettings().authUser
                        }, {
                            xtype : 'textfield',
                            name : 'Password',
                            id : 'email_smtpPassword',
                            inputType: 'password',
                            fieldLabel : this.i18n._('Password'),
                            labelStyle : 'text-align: right; width: 200px;',
                            width : 175,
                            value : this.getMailSettings().authPass
                        }]
                    }]
                }, {
                    title : this.i18n._('Email From Address'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                    	html : this.i18n._("The Untangle Server will send email from this address.")
                    }, {
                        xtype : 'textfield',
                        name : 'Email From Address',
                        id : 'email_fromAddress',
                        style : 'margin-left: 50px;',
                        vtype : 'email',
                        hideLabel : true,
                        allowBlank : false,
                        width : 200,
                        value : this.getMailSettings().fromAddress
                    }]
                }, {
                    title : this.i18n._('Email Test'),
                    buttonAlign : 'center',
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._('The Email Test will send an email to a specified address with the current configuration. If the test email is not received your settings may be incorrect.')
                    }], 
                    buttons : [{
                        text : this.i18n._("Email Test"),
                        iconCls : 'testIcon',
                        name: "emailTestButton",
                        handler : function() {
                            this.panelOutgoingServer.onEmailTest();
                        }.createDelegate(this)
                    }]
                }]
            });
        },
        buildFromSafeList : function() {
//            var valuesGlobal = this.getSafelistAdminView().getSafelistContents('GLOBAL');
//            var storeDataGlobal = [];
//            for(var i=0; i<valuesGlobal.length; i++) {
//                storeDataGlobal.push({id:i, emailAddress: valuesGlobal[i]});
//            }
            
            var smUserSafelist = new Ext.grid.CheckboxSelectionModel({singleSelect:false});
            var showDetailColumn = new Ext.grid.IconColumn({
                header : this.i18n._("Show Detail"),
                width : 100,
                iconClass : 'iconDetailRow',
                handle : function(record, index) {
                    // select current row
                    this.grid.getSelectionModel().selectRow(index);
                    // show details
                    this.grid.onShowDetail(record);
                }
            });
                
            this.panelFromSafeList = new Ext.Panel({
                // private fields
                name : 'From-Safe List',
                parentId : this.getId(),
                title : this.i18n._('From-Safe List'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset'
                },
                items : [{
                    title : this.i18n._('Global'),
                    height : 300,
                    items : [ this.globalSafelistGrid = new Ung.EditorGrid({
                        name : 'Global',
                        hasEdit : false,
//                        paginated : false,
                        settingsCmp : this,
//                        data : storeDataGlobal,
//                        dataRoot: null,
//                        proxyRpcFn : this.getSafelistAdminView().getSafelistContents,
//                        proxyRpcFnArgs : ['GLOBAL'],
                        height : 250,
                        emptyRow : {
                            "emailAddress" : this.i18n._("[no email address]")
                        },
                        autoExpandColumn : 'emailAddress',
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'emailAddress'
                        }],
                        columns : [{
                            id : 'emailAddress',
                            header : this.i18n._("email address"),
                            width : 200,
                            dataIndex : 'emailAddress'
                        }],
                        sortField : 'emailAddress',
                        columnsDefaultSortable : true,
                        rowEditorInputLines : [ new Ext.form.TextField({
                            name : "Email Address",
                            dataIndex: "emailAddress",
                            fieldLabel : this.i18n._("Email Address"),
                            labelStyle : 'width: 80px;',
                            allowBlank : false,
                            width : 200
                        })],
                        store : new Ext.data.Store({
                            proxy : new Ung.RpcProxy(this.getSafelistAdminView().getSafelistContents, ['GLOBAL'], false), 
                            reader : new Ung.JsonListReader({
                                root : null,
                                fields : ['emailAddress']
                            })
                        }) 
                    })]
                },{
                	title : this.i18n._('Per User'),
                    height : 300,
                    items : [ this.userSafelistGrid = new Ung.EditorGrid({
                        name : 'Per User',
                        sm : smUserSafelist,
                        hasEdit : false,
                        hasAdd : false,
                        hasDelete : false,
                        settingsCmp : this,
                        paginated : false,
                        height : 250,
                        proxyRpcFn : this.getSafelistAdminView().getUserSafelistCounts,
                        tbar : [{
                            text : this.i18n._('Purge Selected'),
                            tooltip : this.i18n._('Purge Selected'),
                            iconCls : 'purgeIcon',
                            name : 'Purge Selected',
                            parentId : this.getId(),
                            handler : function() {
                                var selectedRecords = this.userSafelistGrid.getSelectionModel().getSelections();
                                if(selectedRecords === undefined || selectedRecords.length == 0) {
                                    return;
                                }
                                var accounts = [];
                                for(var i=0; i<selectedRecords.length; i++) {
                                    accounts[i] = selectedRecords[i].data.emailAddress;
                                }
                                
                                Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                                this.getSafelistAdminView().deleteSafelists(function(result, exception) {
                                    if (exception) {
                                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                        return;
                                    }
                                    Ext.MessageBox.hide();
                                }.createDelegate(this), accounts);
                                
                                this.userSafelistGrid.store.load();
                            }.createDelegate(this)
                        }],
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'emailAddress'
                        }, {
                            name : 'count'
                        }],
                        columns : [smUserSafelist, {
                            id : 'emailAddress',
                            header : this.i18n._("account address"),
                            width : 200,
                            dataIndex : 'emailAddress'
                        }, {
                            id : 'count',
                            header : this.i18n._("safe list size"),
                            width : 200,
                            dataIndex : 'count'
                        }, showDetailColumn],
                        autoExpandColumn : 'emailAddress',
                        sortField : 'emailAddress',
                        columnsDefaultSortable : true,
                        plugins : [showDetailColumn],
                        
                        onShowDetail : function(record) {
                            if (!this.safelistDetailsWin) {
                                this.buildUserSafelistDetailsGrid();
                                this.safelistDetailsWin = new Ung.EmailAddressDetails({
                                    detailsPanel : this.userSafelistDetailsGrid,
                                    settingsCmp : this,
                                    showForCurrentAccount : function(emailAddress) {
                                        this.account = emailAddress;  
                                        var newTitle = this.settingsCmp.i18n._('Email From-SafeList Details for: ') + emailAddress;
                                        this.setTitle(newTitle);
                                        this.detailsPanel.setTitle(newTitle);
                                        
                                        this.show();
                                                
                                        Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
                                        this.settingsCmp.getSafelistAdminView().getSafelistContents(
                                            function(result, exception) {
                                                Ext.MessageBox.hide();
                                                if (exception) {
                                                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                                    return;
                                                }
                                                this.settingsCmp.userSafelistDetailsGrid.store.loadData(result);
                                            }.createDelegate(this), emailAddress); 
                                    }          
                                });
                            }
                            this.safelistDetailsWin.showForCurrentAccount(record.get('emailAddress'));
                        }.createDelegate(this)
                        
                    })
                ]}]
            });
            
        },
        buildUserSafelistDetailsGrid : function() {
            var smUserSafelistDetails = new Ext.grid.CheckboxSelectionModel({singleSelect:false});
            this.userSafelistDetailsGrid = new Ung.EditorGrid({
                name : 'userSafelistDetailsGrid',
                sm : smUserSafelistDetails,
                hasEdit : false,
                hasAdd : false,
                hasDelete : false,
                paginated : false,
                
                tbar : [{
                    text : this.i18n._('Purge Selected'),
                    tooltip : this.i18n._('Purge Selected'),
                    iconCls : 'purgeIcon',
                    name : 'Purge Selected',
                    parentId : this.getId(),
                    handler : function() {
                            var selectedRecords = this.userSafelistDetailsGrid.getSelectionModel().getSelections();
                            if(selectedRecords === undefined || selectedRecords.length == 0) {
                                return;
                            }
                            var senders = [];
                            for(var i=0; i<selectedRecords.length; i++) {
                                senders[i] = selectedRecords[i].data.sender;
                            }
                            
                            Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                            this.getSafelistAdminView().removeFromSafelists(function(result, exception) {
                                if (exception) {
                                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                    return;
                                }
                                 this.userSafelistDetailsGrid.store.loadData(result);
                                Ext.MessageBox.hide();
                            }.createDelegate(this), this.safelistDetailsWin.account, senders);
                            
                            this.userSafelistGrid.store.load();
                    }.createDelegate(this)
                }], 
                columns : [smUserSafelistDetails, {
                    id : 'sender',
                    header : this.i18n._("email address"),
                    width : 200,
                    dataIndex : 'sender'
                }],
                sortField : 'sender',
                columnsDefaultSortable : true,
                
                store : new Ext.data.Store({
                    proxy : new Ext.data.MemoryProxy(),
                    reader : new Ung.JsonListReader({
                        root : null,
                        fields : ['sender']
                    })
                }) 
            });
        },

        buildQuarantine : function() {
            var sm = new Ext.grid.CheckboxSelectionModel({singleSelect:false});
            var showDetailColumn = new Ext.grid.IconColumn({
                header : this.i18n._("Show Detail"),
                width : 100,
                iconClass : 'iconDetailRow',
                handle : function(record, index) {
                    // select current row
                    this.grid.getSelectionModel().selectRow(index);
                    // show details
                    this.grid.onShowDetail(record);
                }
            });
        	
            this.panelQuarantine = new Ext.Panel({
                name : 'panelQuarantine',
                parentId : this.getId(),
                title : this.i18n._('Quarantine'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                	items: [{
                        xtype : 'textfield',
                        name : 'Maximum Holding Time (days) (max 36)',
                        fieldLabel : this.i18n._('Maximum Holding Time (days) (max 36)'),
                        labelStyle : 'width: 230px;',
                        allowBlank : false,
                        value : this.getMailNodeSettings().quarantineSettings.maxMailIntern/(1440*60*1000),
                        regex : /^([0-9]|[0-2][0-9]|3[0-6])$/,
                        regexText : this.i18n._('Maximum Holding Time must be a number in range 0-36'),
                        width : 70,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMailNodeSettings().quarantineSettings.maxMailIntern = newValue * (1440*60*1000);
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'timefield',
                        name : 'Digest Sending Time',
                        fieldLabel : this.i18n._('Digest Sending Time'),
                        labelStyle : 'width: 230px;',
                        allowBlank : false,
                        format : "H:i",
					    minValue: '00:00',
					    maxValue: '23:59',
					    increment: 1,
                        width : 70,
                        value : this.getFormattedTime(this.getMailNodeSettings().quarantineSettings.digestHourOfDay,this.getMailNodeSettings().quarantineSettings.digestMinuteOfDay),
					    listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var dt = Date.parseDate(newValue, "H:i");
                                    this.getMailNodeSettings().quarantineSettings.digestHourOfDay = dt.getHours();
                                    this.getMailNodeSettings().quarantineSettings.digestMinuteOfDay = dt.getMinutes();
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                	title : this.i18n._('User Quarantines'),
                    height : 300,
                    items : [ this.userQuarantinesGrid = new Ung.EditorGrid({
                        name : 'User Quarantines',
                        sm : sm,
                        hasEdit : false,
                        hasAdd : false,
                        hasDelete : false,
                        paginated : false,
                        autoGenerateId : true,
                        settingsCmp : this,
                        height : 250,
                        proxyRpcFn : this.getQuarantineMaintenenceView().listInboxes,
 
                        tbar : [{
                            text : this.i18n._('Purge Selected'),
                            tooltip : this.i18n._('Purge Selected'),
                            iconCls : 'purgeIcon',
                            name : 'Purge Selected',
                            parentId : this.getId(),
                            handler : function() {
                                var selectedRecords = this.userQuarantinesGrid.getSelectionModel().getSelections();
                                if(selectedRecords === undefined || selectedRecords.length == 0) {
                                    return;
                                }
                                var accounts = [];
                                for(var i=0; i<selectedRecords.length; i++) {
                                	accounts[i] = selectedRecords[i].data.address;
                                }
                                
                                Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                                this.getQuarantineMaintenenceView().deleteInboxes(function(result, exception) {
                                    if (exception) {
                                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                        return;
                                    }
                                    Ext.MessageBox.hide();
                                }.createDelegate(this), accounts);
                                
                                this.userQuarantinesGrid.store.load();
                                
                            }.createDelegate(this)
                        }, {
                            text : this.i18n._('Release Selected'),
                            tooltip : this.i18n._('Release Selected'),
                            iconCls : 'releaseIcon',
                            name : 'Release Selected',
                            parentId : this.getId(),
                            handler : function() {
                                var selectedRecords = this.userQuarantinesGrid.getSelectionModel().getSelections();
                                if(selectedRecords === undefined || selectedRecords.length == 0) {
                                    return;
                                }
                                var accounts = [];
                                for(var i=0; i<selectedRecords.length; i++) {
                                    accounts[i] = selectedRecords[i].data.address;
                                }
                                
                                Ext.MessageBox.wait(this.i18n._("Releasing..."), this.i18n._("Please wait"));
                                this.getQuarantineMaintenenceView().rescueInboxes(function(result, exception) {
                                    if (exception) {
                                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                        return;
                                    }
                                    Ext.MessageBox.hide();
                                }.createDelegate(this), accounts);
                                
                                this.userQuarantinesGrid.store.load();
                                
                            }.createDelegate(this)
                        }, {
                        	xtype: 'tbfill'
                        }, {
                        	xtype: 'tbtext', 
                        	text: i18n.sprintf(this.i18n._('Total Disk Space Used: %s MB'), i18n.numberFormat((this.getQuarantineMaintenenceView().getInboxesTotalSize()/(1024 * 1024)).toFixed(3)))
                        }],
                        fields : [{
                            name : 'address'
                        }, {
                            name : 'numMails'
                        }, {
                            name : 'totalSz'
                        }],
                        columns : [sm, {
                            id : 'address',
                            header : this.i18n._("account address"),
                            width : 200,
                            dataIndex : 'address'
                        }, {
                            id : 'numMails',
                            header : this.i18n._("message count"),
                            width : 200,
                            dataIndex : 'numMails'
                        }, {
                            id : 'totalSz',
                            header : this.i18n._("data size (kB)"),
                            width : 200,
                            dataIndex : 'totalSz',
                            renderer : function(value) {
                                return i18n.numberFormat((value /1024.0).toFixed(3));
                            }
                        }, showDetailColumn],
                        sortField : 'address',
                        autoExpandColumn : 'address',
                        columnsDefaultSortable : true,
                        plugins : [showDetailColumn],
                        
                        onShowDetail : function(record) {
                            if (!this.quarantinesDetailsWin) {
                                this.buildUserQuarantinesGrid();
                                this.quarantinesDetailsWin = new Ung.EmailAddressDetails({
                                    detailsPanel : this.userQuarantinesDetailsGrid,
                                    settingsCmp : this,
                                    showForCurrentAccount : function(emailAddress) {
                                        this.account = emailAddress;  
                                        var newTitle = this.settingsCmp.i18n._('Email Quarantine Details for: ') + emailAddress;
                                        this.setTitle(newTitle);
                                        this.detailsPanel.setTitle(newTitle);
                                        
                                        this.show();
                                                
                                        //load Quarantines Details
                                        this.settingsCmp.loadQuarantinesDetails();
                                        
                                    }          
                                });
                            }
                            this.quarantinesDetailsWin.showForCurrentAccount(record.get('address'));
                        }.createDelegate(this)
                        
                    })
                ]}, {
                	title : this.i18n._('Quarantinable Addresses'),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._('This list is a list of all addresses that will have quarantines automatically created. An email address that does not have a quarantinable address will have their mail marked instead.')
                    },  this.quarantinableAddressesGrid = new Ung.EditorGrid({
                        name : 'Quarantinable Addresses',
                        settingsCmp : this,
                        height : 250,
                        paginated : false,
                        emptyRow : {
                            "address" : this.i18n._("quarantineme@example.com"),
                            "category" : this.i18n._("[no category]"),
                            "description" : this.i18n._("[no description]"),
                            "javaClass" : "com.untangle.node.mail.papi.EmailAddressRule"
                        },
                        recordJavaClass : "com.untangle.node.mail.papi.EmailAddressRule",
                        data : this.getMailNodeSettings().quarantineSettings.allowedAddressPatterns,
                        dataRoot : 'list',
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'address'
                        }, {
                            name : 'category'
                        }, {
                            name : 'description'
                        }],
                        columns : [{
                            id : 'address',
                            header : this.i18n._("quarantinable address"),
                            width : 200,
                            dataIndex : 'address',
                            editor : new Ext.form.TextField({
                            })
                        }, {
                            id : 'description',
                            header : this.i18n._("description"),
                            width : 200,
                            dataIndex : 'description',
                            editor : new Ext.form.TextField({
                            }),
                            renderer : function(value, metadata, record) {
                                return record.data.address == "*" ? this.i18n._("All addresses have quarantines") : value;
                            }.createDelegate(this)
                        }],
                        autoExpandColumn : 'description',
                        rowEditorInputLines : [new Ext.form.TextField({
                            name : "Category",
                            dataIndex: "category",
                            fieldLabel : this.i18n._("Category"),
                            allowBlank : false,
                            width : 300
                        }), new Ext.form.TextField({
                            name : "Address",
                            dataIndex: "address",
                            fieldLabel : this.i18n._("Address"),
                            allowBlank : false,
                            width : 300
                        }), new Ext.form.TextField({
                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            allowBlank : false,
                            width : 400
                        })]
                    })]
                }, {
                    title : this.i18n._('Quarantine Forwards'),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._('This is a list of email addresses whose quarantine digest get forward to another account. This is common for distribution lists where the whole list should not receive the digest.')
                    }, this.quarantineForwardsGrid = new Ung.EditorGrid({
                        name : 'Quarantine Forwards',
                        settingsCmp : this,
                        height : 250,
                        paginated : false,
                        emptyRow : {
                            "address1" : this.i18n._("distributionlistrecipient@example.com"),
                            "address2" : this.i18n._("quarantinelistowner@example.com"),
                            "category" : this.i18n._("[no category]"),
                            "description" : this.i18n._("[no description]"),
                            "javaClass" : "com.untangle.node.mail.papi.EmailAddressPairRule"
                        },
                        recordJavaClass : "com.untangle.node.mail.papi.EmailAddressPairRule",
                        data : this.getMailNodeSettings().quarantineSettings.addressRemaps,
                        dataRoot : 'list',
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'address1'
                        }, {
                            name : 'address2'
                        }, {
                            name : 'category'
                        }, {
                            name : 'description'
                        }],
                        columns : [{
                            id : 'address1',
                            header : this.i18n._("distribution list address"),
                            width : 200,
                            dataIndex : 'address1',
                            editor : new Ext.form.TextField({
                            })
                        }, {
                            id : 'address2',
                            header : this.i18n._("send to address"),
                            width : 200,
                            dataIndex : 'address2',
                            editor : new Ext.form.TextField({
                            })
                        }],
                        rowEditorInputLines : [new Ext.form.TextField({
                            name : "Distribution List Address",
                            dataIndex: "address1",
                            fieldLabel : this.i18n._("Distribution List Address"),
                            width : 300
                        }), new Ext.form.TextField({
                            name : "Send To Address",
                            dataIndex: "address2",
                            fieldLabel : this.i18n._("Send To Address"),
                            width : 300
                        }), new Ext.form.TextField({
                            name : "Category",
                            dataIndex: "category",
                            fieldLabel : this.i18n._("Category"),
                            width : 300
                        }), new Ext.form.TextField({
                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            width : 400
                        })]
                    })]
                }]
            });
        },
        
        buildUserQuarantinesGrid : function() {
            var smUserQuarantinesDetails = new Ext.grid.CheckboxSelectionModel({singleSelect:false});
            this.userQuarantinesDetailsGrid = new Ung.EditorGrid({
                name : 'userQuarantinesDetailsGrid',
                sm : smUserQuarantinesDetails,
                hasEdit : false,
                hasAdd : false,
                hasDelete : false,
                paginated : false,
                autoGenerateId : true,
                
                tbar : [{
                    text : this.i18n._('Purge Selected'),
                    tooltip : this.i18n._('Purge Selected'),
                    iconCls : 'purgeIcon',
                    name : 'Purge Selected',
                    parentId : this.getId(),
                    handler : function() {
                        var selectedRecords = this.userQuarantinesDetailsGrid.getSelectionModel().getSelections();
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var emails = [];
                        for(var i=0; i<selectedRecords.length; i++) {
                            emails[i] = selectedRecords[i].data.mailID;
                        }
                        
                        Ext.MessageBox.wait(this.i18n._("Purging..."), this.i18n._("Please wait"));
                        this.getQuarantineMaintenenceView().purge(function(result, exception) {
                            if (exception) {
                                Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                return;
                            }
                            //load Quarantines Details
                            this.loadQuarantinesDetails();
                        }.createDelegate(this), this.quarantinesDetailsWin.account, emails);
                        
                        this.userQuarantinesGrid.store.load();
                    }.createDelegate(this)
                }, {
                    text : this.i18n._('Release Selected'),
                    tooltip : this.i18n._('Release Selected'),
                    iconCls : 'releaseIcon',
                    name : 'Release Selected',
                    parentId : this.getId(),
                    handler : function() {
                        var selectedRecords = this.userQuarantinesDetailsGrid.getSelectionModel().getSelections();
                        if(selectedRecords === undefined || selectedRecords.length == 0) {
                            return;
                        }
                        var emails = [];
                        for(var i=0; i<selectedRecords.length; i++) {
                            emails[i] = selectedRecords[i].data.mailID;
                        }
                        
                        Ext.MessageBox.wait(this.i18n._("Releasing..."), this.i18n._("Please wait"));
                        this.getQuarantineMaintenenceView().rescue(function(result, exception) {
                            if (exception) {
                                Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                return;
                            }
                            
                            //load Quarantines Details
                            this.loadQuarantinesDetails();
                        }.createDelegate(this), this.quarantinesDetailsWin.account, emails);
                        
                        this.userQuarantinesGrid.store.load();
                    }.createDelegate(this)
                }], 
                fields : [{
                    name : 'mailID'
                }, {
                    name : 'internDateAsDate'
                }, {
                    name : 'size'
                }, {
                    name : 'mailSummary'
                }],
                columns : [smUserQuarantinesDetails, {
                    id : 'mailID',
                    header : this.i18n._("MailID"),
                    width : 150,
                    dataIndex : 'mailID'
                }, {
                    id : 'internDateAsDate',
                    header : this.i18n._("Date"),
                    width : 150,
                    dataIndex : 'internDateAsDate',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    id : 'sender',
                    header : this.i18n._("Sender"),
                    width : 150,
                    dataIndex : 'mailSummary',
                    renderer : function(value) {
                        return value.sender;
                    }
                }, {
                    id : 'subject',
                    header : this.i18n._("Subject"),
                    width : 150,
                    dataIndex : 'mailSummary',
                    renderer : function(value) {
                        return value.subject;
                    }
                }, {
                    id : 'size',
                    header : this.i18n._("Size (KB)"),
                    width : 85,
                    dataIndex : 'size',
                    renderer : function(value) {
                        return i18n.numberFormat((value /1024.0).toFixed(3));
                    }
                    
                }, {
                    id : 'category',
                    header : this.i18n._("Category"),
                    width : 85,
                    dataIndex : 'mailSummary',
                    renderer : function(value) {
                        return value.quarantineCategory;
                    }
                }, {
                    id : 'detail',
                    header : this.i18n._("Detail"),
                    width : 85,
                    dataIndex : 'mailSummary',
                    renderer : function(value) {
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
//                sortField : 'internDateAsDate',
                columnsDefaultSortable : true,
                autoExpandColumn : 'subject',
                data : [],
                dataRoot: null
            });
        },
        

        validateClient : function() {
            return  this.validateOutgoingServer();
        },

        //validate Outgoing Server settings
        validateOutgoingServer : function() {
            var hostCmp = Ext.getCmp('email_smtpHost');
            var portCmp = Ext.getCmp('email_smtpPort');
            var useAuthenticationCmp = Ext.getCmp('email_smtpUseAuthentication');
            var loginCmp = Ext.getCmp('email_smtpLogin');
            var passwordCmp = Ext.getCmp('email_smtpPassword');
            var fromAddressCmp = Ext.getCmp('email_fromAddress');

            //validate port
            if (!portCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535),
                    function () {
                        this.tabs.activate(this.panelActiveDirectoryConnector);
                        portCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            
            // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED
            if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length == 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Password" must be specified if a "Login" is specified.'),
                    function () {
                        this.tabs.activate(this.panelActiveDirectoryConnector);
                        passwordCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            else if(loginCmp.getValue().length == 0 && passwordCmp.getValue().length > 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Login" must be specified if a "Password" is specified.'),
                    function () {
                        this.tabs.activate(this.panelActiveDirectoryConnector);
                        loginCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            
            // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
            if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length > 0 && hostCmp.getValue().length == 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "Hostname" must be specified if "Login" or "Password" are specified.'),
                    function () {
                        this.tabs.activate(this.panelActiveDirectoryConnector);
                        hostCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }

            // CHECK THAT A FROM ADDRESS IS SPECIFIED
            if (fromAddressCmp.getValue().length == 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('A "From Address" must be specified.'),
                    function () {
                        this.tabs.activate(this.panelActiveDirectoryConnector);
                        fromAddressCmp.focus(true);
                    }.createDelegate(this) 
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
        
        // save function
        saveAction : function() {
            if (this.validate()) {
                this.saveSemaphore = 3;
                Ext.MessageBox.show({
                   title : this.i18n._('Please wait'),
                   msg : this.i18n._('Saving...'),
                   modal : true,
                   wait : true,
                   waitConfig: {interval: 100},
                   progressText : " ",
                   width : 200
                });
                
                // save mail settings
                main.getMailSender().setMailSettings(function(result, exception) {
	                 if (exception) {
		                 Ext.MessageBox.alert(i18n._("Failed"), exception.message);
		                 return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getMailSettings());
                
                // save mail node settings
                this.getMailNode().setMailNodeSettingsWithoutSafelists(function(result, exception) {
                     if (exception) {
                         Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                         return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getMailNodeSettings());

                // save global safelist
                var globalSafelistGridValues = this.globalSafelistGrid.getFullSaveList();
                var globalList = [];
	            for(var i=0; i<globalSafelistGridValues.length; i++) {
	                globalList.push(globalSafelistGridValues[i].emailAddress);
	            }
                this.getSafelistAdminView().replaceSafelist(function(result, exception) {
                     if (exception) {
                         Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                         return;
                    }
                    this.afterSave();
                }.createDelegate(this), 'GLOBAL', globalList);
                
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                Ext.MessageBox.hide();
                this.cancelAction();
            }
        }
    });
    
    // email address details window for Safe List and Quarantine
    Ung.EmailAddressDetails = Ext.extend(Ung.ButtonsWindow, {
    	settingsCmp : null,
        // the certPanel
        detailsPanel : null,
        account : null,
        onRender : function(container, position) {
            Ung.EmailAddressDetails.superclass.onRender.call(this, container, position);
            this.initSubComponents.defer(1, this);
        },
        initSubComponents : function(container, position) {
            this.detailsPanel.render(this.getContentEl());
        },
        listeners : {
            'show' : {
                fn : function() {
                    this.detailsPanel.setHeight(this.getContentHeight());
                },
                delay : 1
            }
        },
        initButtons : function() {
            this.subCmps.push(new Ext.Button({
                name : 'Cancel',
                renderTo : 'button_margin_right_' + this.getId(),
                iconCls : 'cancelIcon',
                text : i18n._('Cancel'),
                handler : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }));
            this.subCmps.push(new Ext.Button({
                name : 'Help',
                renderTo : 'button_left_' + this.getId(),
                iconCls : 'iconHelp',
                text : this.settingsCmp.i18n._('Help'),
                handler : function() {
                    this.helpAction();
                }.createDelegate(this)
            }));
        },
        // to override
        showForCurrentAccount : function(emailAddress) {
            this.account = emailAddress;  
            this.show();
        },        	
        // the proceed actions
        // to override
        proceedAction : function() {
            main.todo();
        },
        beforeDestroy : function() {
            Ext.destroy(this.detailsPanel);
            Ung.ButtonsWindow.superclass.beforeDestroy.call(this);
        }
    });
}