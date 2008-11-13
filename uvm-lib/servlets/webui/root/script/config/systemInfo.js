if (!Ung.hasResource["Ung.SystemInfo"]) {
    Ung.hasResource["Ung.SystemInfo"] = true;

    Ung.SystemInfo = Ext.extend(Ung.ConfigWin, {
        panelVersion : null,
        panelRegistration : null,
        panelLicenseAgreement : null,
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
            this.buildLicenseAgreement();

            // builds the tab panel with the tabs
            var pageTabs = [this.panelVersion, this.panelRegistration, this.panelLicenseAgreement];
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
        hasPremiumLicense : function(forceReload) {
            if (forceReload || this.rpc.hasPremiumLicense === undefined) {
            	try {
                    this.rpc.hasPremiumLicense = main.getLicenseManager().hasPremiumLicense();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                    
            }
            return this.rpc.hasPremiumLicense;
        },
        getLicenseAgreement : function(forceReload) {
            if (forceReload || this.rpc.getLicenseAgreement === undefined) {
            	try {
                    this.rpc.getLicenseAgreement = main.getLicenseManager().getLicenseAgreement();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                    
            }
            return this.rpc.getLicenseAgreement;
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
                            this.i18n._('Key')+": "+ this.getSystemInfo().activationKey + "\n" + 
                            this.i18n._('Build') + ": " + this.getSystemInfo().fullVersion + "\n" + 
                            this.i18n._('Java') + ": " + this.getSystemInfo().javaVersion 
                    })]
                }]
            });
        },
        buildRegistration : function() {
            var info = this.getRegistrationInfo();
            var misc = null;
            if ( info.misc != null ) misc = info.misc.map;

            if ( misc == null ) misc = {};

            this.environmentStore = [
                this.i18n._( "My Business" ),
                this.i18n._( "A Client's Business" ),
                this.i18n._( "School" ),
                this.i18n._( "Home" )];

            this.panelRegistration = new Ext.FormPanel({
                parentId : this.getId(),
                title : this.i18n._( "Registration" ),
                helpSource : 'registration',
                defaultType : 'fieldset',
                defaults : { 
                    autoHeight : true,
                    labelWidth : 200,
                    cls : 'noborder'
                },
                    
                items : [{
                	title : this.i18n._( 'Please provide administrator contact info.' ),
                    defaultType : 'textfield',
                    defaults : {
                        validationEvent : 'blur',
                        msgTarget : 'side'
                    },
                    items : [{
                        fieldLabel : '<span class="required-star">*</span>'+this.i18n._('Email'),
                        name : 'emailAddr',
                        width : 200,
                        allowBlank : false,
                        value : info.emailAddr
                    },{
                        fieldLabel : this.i18n._('Name'),
                        name : 'name',
                        value : misc.name
                    },{
                        xtype : 'numberfield',
                        minValue : 0,
                        allowDecimals : false,
                        fieldLabel : '<span class="required-star">*</span>'+this.i18n._('Number of PCs on your network'),
                        name : 'numSeats',
                        value : info.numSeats,
                        allowBlank : false	
                    }]
                },{
                	title : this.i18n._( 'Answering these questions will help us build a better product - for you!' ),
                    defaultType : 'textfield',
                    items : [{
                        fieldLabel : this.i18n._('Where will you be using Untangle'),
                        name : "environment",
                        xtype : 'combo',
                        width : 200,
                        listWidth : 205,
                        store : this.environmentStore,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : misc.environment
                    },{
                        fieldLabel : this.i18n._('Country'),
                        name : "country",
                        xtype : 'combo',
                        width : 200,
                        listWidth : 205,
                        store : Ung.Country.getCountryStore(i18n),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : Ung.Country.getCountryCode(misc.country)
                    }]
                },{
                    xtype : 'label',
                    html : this.i18n._( '<span class="required-star">*</span> Required' ),
                    cls : 'required-info'
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
                items: [ new Ext.form.TextArea({
                    name : 'License Agreement',
                    hideLabel : true,
                    readOnly : true,
                    width : 600,
                    height : 400,
                    value : this.getLicenseAgreement()
                })]
           });
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
            var misc = {};
            misc.name        = this.panelRegistration.find( "name", "name" )[0].getValue();
            info.emailAddr   = this.panelRegistration.find( "name", "emailAddr" )[0].getValue();
            info.numSeats    = this.panelRegistration.find( "name", "numSeats" )[0].getValue();
            
            misc.environment = this.panelRegistration.find( "name", "environment" )[0].getRawValue();
            misc.country     = Ung.Country.getCountryName(this.panelRegistration.find( "name", "country" )[0].getValue());

            info.misc.map = misc;
            
            return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                rpc.adminManager.setRegistrationInfo(function(result, exception) {
                    Ext.MessageBox.hide();
                    if(Ung.Util.handleException(exception)) return;
                    this.cancelAction();
                }.createDelegate(this), this.getRegistrationInfo());
            }
        }

    });

}
