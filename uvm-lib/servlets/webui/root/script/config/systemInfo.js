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
            if (this.hasPremiumLicense()) {
                this.buildLicenseAgreement();
            }
            // builds the tab panel with the tabs
            var pageTabs = [this.panelVersion, this.panelRegistration];
            if (this.hasPremiumLicense()) {
                pageTabs.push(this.panelLicenseAgreement);
            }
            this.buildTabPanel(pageTabs);
            this.tabs.activate(this.panelVersion);
            Ung.SystemInfo.superclass.initComponent.call(this);
        },
        getSystemInfo : function(forceReload) {
            if (forceReload || this.rpc.systemInfo === undefined) {
                this.rpc.systemInfo = rpc.adminManager.getSystemInfo();
            }
            return this.rpc.systemInfo;
        },
        getRegistrationInfo : function(forceReload) {
            if (forceReload || this.rpc.registrationInfo === undefined) {
                this.rpc.registrationInfo = rpc.adminManager.getRegistrationInfo();
            }
            return this.rpc.registrationInfo;
        },
        hasPremiumLicense : function(forceReload) {
            if (forceReload || this.rpc.hasPremiumLicense === undefined) {
                this.rpc.hasPremiumLicense = main.getLicenseManager().hasPremiumLicense();
            }
            return this.rpc.hasPremiumLicense;
        },
        getLicenseAgreement : function(forceReload) {
            if (forceReload || this.rpc.getLicenseAgreement === undefined) {
                this.rpc.getLicenseAgreement = main.getLicenseManager().getLicenseAgreement();
            }
            return this.rpc.getLicenseAgreement;
        },
        // get branding settings
        getBrandingBaseSettings : function(forceReload) {
            if (forceReload || this.rpc.brandingBaseSettings === undefined) {
                this.rpc.brandingBaseSettings = main.getBrandingManager().getBaseSettings();
            }
            return this.rpc.brandingBaseSettings;
        },        
        buildVersion : function() {
            this.panelVersion = new Ext.Panel({
                name : 'Version',
                helpSource : 'version',
                parentId : this.getId(),
                title : this.i18n._('Version'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
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
            var requiredLabel = {
                html : '(required)',
                border : false
            };
            this.panelRegistration = new Ext.Panel({
                name : 'Registration',
                helpSource : 'registration',
                parentId : this.getId(),
                title : this.i18n._('Registration'),
                layout: 'anchor',
                defaults: {
                    anchor: '98%',
                    autoWidth: true,
                    autoScroll: true
                },
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    title : this.i18n._('Registration'),
                    name : 'Registration',
                    xtype : 'fieldset',
                    autoHeight : true,
                    layout : 'form',
                    labelWidth: 160,
                    defaults : {
                        width : 200
                    },
                    items : [ new Ext.form.TextField({
                        name : "Company Name",
                        fieldLabel : this.i18n._("Company Name"),
                        id : 'companyName',
                        allowBlank : false,
                        value : this.getRegistrationInfo().companyName
                    }), new Ext.form.TextField({
                        name : "First Name",
                        fieldLabel : this.i18n._("First Name"),
                        id : 'firstName',
                        allowBlank : false,
                        value : this.getRegistrationInfo().firstName
                    }), new Ext.form.TextField({
                        name : "Last Name",
                        fieldLabel : this.i18n._("Last Name"),
                        id : 'lastName',
                        allowBlank : false,
                        value : this.getRegistrationInfo().lastName
                    }), new Ext.form.TextField({
                        name : "Address 1",
                        id : 'address1',
                        fieldLabel : this.i18n._("Address 1"),
                        value : this.getRegistrationInfo().address1
                    }), new Ext.form.TextField({
                        name : "Address 2",
                        id : 'address2',
                        fieldLabel : this.i18n._("Address 2"),
                        value : this.getRegistrationInfo().address2
                    }), new Ext.form.TextField({
                        name : "City",
                        id : 'city',
                        fieldLabel : this.i18n._("City"),
                        value : this.getRegistrationInfo().city
                    }), new Ext.form.TextField({
                        name : "State",
                        id : 'state',
                        fieldLabel : this.i18n._("State"),
                        value : this.getRegistrationInfo().state
                    }), new Ext.form.TextField({
                        name : "Zipcode",
                        id : 'zipcode',
                        fieldLabel : this.i18n._("Zipcode"),
                        value : this.getRegistrationInfo().zipcode
                    }), new Ext.form.TextField({
                        name : "Phone #",
                        id : 'phone',
                        fieldLabel : this.i18n._("Phone #"),
                        value : this.getRegistrationInfo().phone
                    }), new Ext.form.TextField({
                        name : "Email",
                        id : 'emailAddr',
                        fieldLabel : this.i18n._("Email"),
                        allowBlank : false,
                        value : this.getRegistrationInfo().emailAddr,
                        vtype : 'email'
                    }), new Ext.form.TextField({
                        name : "Number of computers protected by Untangle",
                        id : 'numSeats',
                        fieldLabel : String.format(this.i18n._('Number of computers{0}protected by {1}'),
                                        '<br>',this.getBrandingBaseSettings().companyName),
                        allowBlank : false,
                        value : this.getRegistrationInfo().numSeats
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
                bodyStyle : 'padding:5px 5px 0px 5px;',
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
            var companyNameCmp = Ext.getCmp('companyName');
            if (!companyNameCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the company name.'),
                    function () {
                        this.tabs.activate(this.panelRegistration);
                        companyNameCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            this.getRegistrationInfo().companyName = companyNameCmp.getValue();

            var firstNameCmp = Ext.getCmp('firstName');
            if (!firstNameCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out your first name.'),
                    function () {
                        this.tabs.activate(this.panelRegistration);
                        firstNameCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            this.getRegistrationInfo().firstName = firstNameCmp.getValue();

            var lastNameCmp = Ext.getCmp('lastName');
            if (!lastNameCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the last name.'),
                    function () {
                        this.tabs.activate(this.panelRegistration);
                        lastNameCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            this.getRegistrationInfo().lastName = lastNameCmp.getValue();

            var emailAddrCmp = Ext.getCmp('emailAddr');
            if (!emailAddrCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the email address in the format "user@domain.com".'),
                    function () {
                        this.tabs.activate(this.panelRegistration);
                        emailAddrCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            this.getRegistrationInfo().emailAddr = emailAddrCmp.getValue();

            var numSeatsCmp = Ext.getCmp('numSeats');
            if (!numSeatsCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._('You must fill out the number of computers protected by {0}.'), this.getBrandingBaseSettings().companyName),
                    function () {
                        this.tabs.activate(this.panelRegistration);
                        numSeatsCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            this.getRegistrationInfo().numSeats = numSeatsCmp.getValue();

            var address1Cmp = Ext.getCmp('address1');
            this.getRegistrationInfo().address1 = address1Cmp.getValue();
            var address2Cmp = Ext.getCmp('address2');
            this.getRegistrationInfo().address2 = address2Cmp.getValue();
            var cityCmp = Ext.getCmp('city');
            this.getRegistrationInfo().city = cityCmp.getValue();
            var stateCmp = Ext.getCmp('state');
            this.getRegistrationInfo().state = stateCmp.getValue();
            var zipcodeCmp = Ext.getCmp('zipcode');
            this.getRegistrationInfo().zipcode = zipcodeCmp.getValue();
            var phoneCmp = Ext.getCmp('phone');
            this.getRegistrationInfo().phone = phoneCmp.getValue();
            
            return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                rpc.adminManager.setRegistrationInfo(function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.cancelAction();
                }.createDelegate(this), this.getRegistrationInfo());
            }
        }

    });

}