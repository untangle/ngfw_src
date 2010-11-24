if (!Ung.hasResource["Ung.SystemInfo"]) {
    Ung.hasResource["Ung.SystemInfo"] = true;

    Ung.SystemInfo = Ext.extend(Ung.ConfigWin, {
        panelVersion : null,
        panelRegistration : null,
        panelLicenses : null,
        panelLicenseAgreement : null,
        dirty : false,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('System Info')
            }];
            this.buildVersion();
            this.buildRegistration();
            this.buildLicenses();
            this.buildLicenseAgreement();
            
            // builds the tab panel with the tabs
            var pageTabs = [this.panelVersion, this.panelRegistration, this.panelLicenses, this.panelLicenseAgreement];
            this.buildTabPanel(pageTabs);
            this.tabs.activate(this.panelVersion);
            Ung.SystemInfo.superclass.initComponent.call(this);
        },
        getSystemInfo : function(forceReload) {
            if (forceReload || this.rpc.systemInfo === undefined) {
                try {
                    this.rpc.systemInfo = rpc.adminManager.getSystemInfo();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.systemInfo;
        },
        getRegistrationInfo : function(forceReload) {
            if (forceReload || this.rpc.registrationInfo === undefined) {
                try {
                    this.rpc.registrationInfo = rpc.adminManager.getRegistrationInfo();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                    
            }
            return this.rpc.registrationInfo;
        },
        buildVersion : function() {
            this.panelVersion = new Ext.Panel({
                name : 'Version',
                helpSource : 'version',
                parentId : this.getId(),
                title : this.i18n._('Version'),
                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                items : [{
                    title : this.i18n._('System Info'),
                    name : 'System Info',
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left',
                    items: [ new Ext.form.TextArea({
                        name : 'System Info',
                        hideLabel : true,
                        readOnly : true,
                        style : 'font-weight: bold;',
                        width : 600,
                        height : 400,
                        value : this.i18n._('Summary')+":\n"+
                            this.i18n._('UID')+": "+ this.getSystemInfo().serverUID + "\n" + 
                            this.i18n._('Build') + ": " + this.getSystemInfo().fullVersion + "\n" + 
                            this.i18n._('Java') + ": " + this.getSystemInfo().javaVersion 
                    })]
                }]
            });
        },
        buildRegistration : function() {
            var info = this.getRegistrationInfo();
            var environment = null;
            var other = "";

            environment = info.environment;
            if (( environment != null ) &&
                ( environment != "" ) &&
                ( environment != "my-business" ) &&
                ( environment != "clients-business" ) &&
                ( environment != "school" ) && 
                ( environment != "home" )) {
                other = environment;
                environment = "other";
            }

            this.panelRegistration = new Ext.FormPanel({
                parentId : this.getId(),
                title : this.i18n._( "Registration" ),
                helpSource : 'registration',
                defaultType : 'fieldset',
                defaults : { 
                    autoHeight : true,
                    labelWidth : 220,
                    cls : 'noborder'
                },
                listeners : {
                    activate : {
                        fn : function()
                        {
                            var g = this.panelRegistration.find( "inputValue", "other" )[0].getValue();
                            this.onSelectOther( null, g );
                        },
                        scope : this
                    }
                },
                    
                items : [{
                    title : this.i18n._( "Please provide administrator contact info." ),
                    defaultType : 'textfield',
                    defaults : {
                        validationEvent : "blur",
                        msgTarget : 'side'
                    },
                    items : [{
                        fieldLabel : this.i18n._("First Name"),
                        name : "firstName",
                        value : info.firstName,
                        listeners : {
                            "change" : {
                                fn : this.onFieldChange.createDelegate(this)
                            }
                        }
                    },{
                        fieldLabel : this.i18n._("Last Name/Surname"),
                        name : "lastName",
                        value : info.lastName,
                        listeners : {
                            "change" : {
                                fn : this.onFieldChange.createDelegate(this)
                            }
                        }
                    },{
                        fieldLabel : this.i18n._('Email'),
                        name : "emailAddr",
                        width : 200,
                        allowBlank : false,
                        value : info.emailAddr,
                        listeners : {
                            "change" : {
                                fn : this.onFieldChange.createDelegate(this)
                            }
                        }
                    },new Ung.form.TextField({
                        fieldLabel : i18n._('Organization Name'),
                        name : 'companyName',
                        value : info.companyName,
                        boxLabel : i18n._('(if applicable)'),
                        listeners : {
                            "change" : {
                                fn : this.onFieldChange.createDelegate(this)
                            }
                        }
                    }),new Ung.form.NumberField({
                        minValue : 0,
                        width : 60,
                        allowDecimals : false,
                        fieldLabel : this.i18n._('Number of PCs on your network'),
                        name : 'numSeats',
                        value : info.numSeats,
                        allowBlank : false,
                        boxLabel : i18n._('(approximate; include Windows, Linux and Mac)'),
                        listeners : {
                            "change" : {
                                fn : this.onFieldChange.createDelegate(this)
                            }
                        }
                    }),{
                        fieldLabel : String.format(i18n._("Where will you be using {0}"),main.getBrandingManager().getCompanyName()),
                        name : "environment",
                        xtype : 'radio',
                        inputValue : "my-business",
                        boxLabel : i18n._( "My Business" ),
                        checked : environment == "my-business"
                    },{
                        fieldLabel : "",
                        labelSeparator : "",
                        name : "environment",
                        xtype : "radio",
                        inputValue : "clients-business",
                        checked : environment == "clients-business",
                        boxLabel : i18n._( "A Client's Business" )
                        
                    },{
                        fieldLabel : "",
                        labelSeparator : "",
                        name : "environment",
                        xtype : "radio",
                        inputValue : "school",
                        checked : environment == "school",
                        boxLabel : i18n._( "School" )
                    },{
                        fieldLabel : "",
                        labelSeparator : "",
                        name : "environment",
                        xtype : "radio",
                        inputValue : "home",
                        checked : environment == "home",
                        boxLabel : i18n._( "Home" )
                    },{
                        fieldLabel : "",
                        labelSeparator : "",
                        name : "environment",
                        xtype : "radio",
                        listeners : {
                            check : {
                                fn : this.onSelectOther,
                                scope : this
                            }
                        },
                        inputValue : "other",
                        checked : environment == "other",
                        boxLabel : i18n._( "Other" )
                    },this.environmentOther = new Ext.form.TextField({
                        fieldLabel : i18n._("Please Describe"),
                        name : "environment-other",
                        value : other,
                        itemCls : "left-indent-5"
                    })]
                }]
            });
        },
        buildLicenseAgreement : function() {
            this.panelLicenseAgreement = new Ext.Panel({
                name : 'License Agreement',
                helpSource : 'license_agreement',
                parentId : this.getId(),
                title : this.i18n._('License Agreement'),
                cls: 'ung-panel',
                items: [{
                    xtype : "button",
                    text : this.i18n._("View License"),
                    name : "View License",
                    iconCls : "reboot-icon",
                    handler : function() {window.open("../library/launcher?action=license");}
                }]
                
           });
        },
        buildLicenses : function() {

            this.buildGridLicenses();

            this.panelLicenses = new Ext.Panel({
                name : 'Licenses',
                helpSource : 'licenses',
                parentId : this.getId(),
                title : this.i18n._('Licenses'),
                cls: 'ung-panel',
                items: [{
                    title : this.i18n._('Licenses'),
                    name : 'Status',
                    xtype : 'fieldset',
                    autoHeight : true,
                    items: [{
                        html : String.format(this.i18n._('Licenses determine entitlement to paid applications and services. Click Refresh to force reconciliation with the license server.'),'<b>','</b>'),
                        cls: 'description',
                        border : false}]
                 }]
           });

            this.panelLicenses.add( this.gridLicenses );

        },
        buildGridLicenses : function()
        {
            this.gridLicenses = new Ung.EditorGrid({
                name : "gridLicenses",
                settingsCmp : this,
                height : 350,
                parentId : this.getId(),
                hasAdd : false,
                configAdd : null,
                hasEdit : false,
                configEdit : null,
                hasDelete : false,
                configDelete : null,
                columnsDefaultSortable : true,
                title : this.i18n._("Licenses"),
                qtip : this.i18n._("The Current list of Licenses available on this Server."),
                paginated : false,
                bbar : new Ext.Toolbar({
                    items : [
                        '-',
                        {
                            xtype : 'tbbutton',
                            id: "refresh_"+this.getId(),
                            text : i18n._('Refresh'),
                            name : "Refresh",
                            tooltip : i18n._('Refresh'),
                            iconCls : 'icon-refresh',
                            handler : function() {
                                // XXX
                                // Also call refresh
                                this.gridLicenses.store.reload();
                            }.createDelegate(this)
                        }
                    ]
                }),
                recordJavaClass : "com.untangle.uvm.license.License",
                proxyRpcFn : main.getLicenseManager().getLicenses,
                plugins : [],
                fields : [{
                    name : "displayName"
                },{
                    name : "name"
                },{
                    name : "UID"
                },{
                    name : "start"
                },{
                    name : "end"
                },{
                    name : "valid"
                },{
                    name : "status"
                },{
                    name : "id"
                }],
                columns : [{
                    id : "displayName",
                    header : this.i18n._("Name"),
                    width : 150
                },{
                    id : "name",
                    header : this.i18n._("App"),
                    width : 150
                },{
                    id : "UID",
                    header : this.i18n._("UID"),
                    width : 150
                },{
                    id : "start",
                    header : this.i18n._("Start Date"),
                    width : 240,
                    renderer : function(value) { return new Date(value*1000); }
                },{
                    id : "end",
                    header : this.i18n._("End Date"),
                    width : 240,
                    renderer : function(value) { return new Date(value*1000); }
                },{
                    id : "valid",
                    header : this.i18n._("Valid"),
                    width : 50
                },{
                    id : "status",
                    header : this.i18n._("Status"),
                    width : 150
                }]
            });
        },
        onFieldChange : function() {
            this.dirty = true;
        },
        onSelectOther: function( checkbox, isChecked )
        {
            if ( isChecked ) {
                this.environmentOther.show();
                this.environmentOther.enable();
                this.environmentOther.getEl().up('.x-form-item').setDisplayed( true );
            } else {
                this.environmentOther.hide();
                this.environmentOther.disable();
                this.environmentOther.getEl().up('.x-form-item').setDisplayed( false );
            }
        },
        //validate Registration Info
        validateClient : function() {
            if ( !_validate( this.panelRegistration.items.items )) {
                this.tabs.activate(this.panelRegistration);

                Ext.MessageBox.alert( this.i18n._('Warning'),
                                      this.i18n._('You must complete all required fields.'));

                return false;
            }
            
            var info = this.getRegistrationInfo();
            info.firstName = this.panelRegistration.find( "name", "firstName" )[0].getValue();
            info.lastName = this.panelRegistration.find( "name", "lastName" )[0].getValue();
            info.emailAddr   = this.panelRegistration.find( "name", "emailAddr" )[0].getValue();
            info.numSeats    = this.panelRegistration.find( "name", "numSeats" )[0].getValue();
            info.companyName = this.panelRegistration.find( "name", "companyName" )[0].getValue();
            var value = this.panelRegistration.find( "name", "environment" )[0].getGroupValue();
            if ( value == "other" ) {
                value = this.panelRegistration.find( "name", "environment-other" )[0].getValue();
            }
            if ( value != null ) info.environment = value;

            return true;
        },
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        reloadSettings : function()
        {
            this.getRegistrationInfo(true);
            this.dirty = false;
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
            if (!this.validate()) {
                return;
            }
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            rpc.adminManager.setRegistrationInfo(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                callback();
            }.createDelegate(this), this.getRegistrationInfo());
        },
        isDirty : function()
        {
            return this.dirty;
        }
    });

}
