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
            Ung.SystemInfo.superclass.initComponent.call(this);
        },
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.SystemInfo.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 2 tabs
        },
        initSubCmps : function() {
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
        buildVersion : function() {
            this.panelVersion = new Ext.Panel({
                name : 'Version',
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
                parentId : this.getId(),
                title : this.i18n._('Registration'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    title : this.i18n._('Registration'),
                    name : 'Registration',
                    xtype : 'fieldset',
                    autoHeight : true,
                    layout : 'column',
                    items: [{
	                    columnWidth : .6,
	                    layout : "form",
                        border : false,
                        style : 'text-align: right;',
		                defaults : {
		                    labelStyle : 'text-align: right; width: 150px;',
                            style : 'text-align: left;',
                            width : 200
		                },
	                    items : [ new Ext.form.TextField({
	                        name : "Company Name",
	                        fieldLabel : this.i18n._("Company Name"),
                            allowBlank : false,
                            value : this.getRegistrationInfo().companyName
	                    }), new Ext.form.TextField({
                            name : "First Name",
                            fieldLabel : this.i18n._("First Name"),
                            allowBlank : false,
                            value : this.getRegistrationInfo().firstName
                        }), new Ext.form.TextField({
                            name : "Last Name",
                            fieldLabel : this.i18n._("Last Name"),
                            allowBlank : false,
                            value : this.getRegistrationInfo().lastName
                        }), new Ext.form.TextField({
                            name : "Address 1",
                            fieldLabel : this.i18n._("Address 1"),
                            value : this.getRegistrationInfo().address1
                        }), new Ext.form.TextField({
                            name : "Address 2",
                            fieldLabel : this.i18n._("Address 2"),
                            value : this.getRegistrationInfo().address2
                        }), new Ext.form.TextField({
                            name : "City",
                            fieldLabel : this.i18n._("City"),
                            value : this.getRegistrationInfo().city
                        }), new Ext.form.TextField({
                            name : "State",
                            fieldLabel : this.i18n._("State"),
                            value : this.getRegistrationInfo().state
                        }), new Ext.form.TextField({
                            name : "Zipcode",
                            fieldLabel : this.i18n._("Zipcode"),
                            value : this.getRegistrationInfo().zipcode
                        }), new Ext.form.TextField({
                            name : "Phone #",
                            fieldLabel : this.i18n._("Phone #"),
                            value : this.getRegistrationInfo().phone
                        }), new Ext.form.TextField({
                            name : "Email",
                            fieldLabel : this.i18n._("Email"),
                            allowBlank : false,
                            value : this.getRegistrationInfo().emailAddr
                        }), new Ext.form.TextField({
                            name : "Number of computers%sprotected by Untangle",
                            fieldLabel : i18n.sprintf(this.i18n._('Number of computers%sprotected by Untangle'),'<br>'),
                            allowBlank : false,
                            value : this.getRegistrationInfo().numSeats
                        })]
	                },{
	                    columnWidth : .4,
                        layout : "form",
                        border : false
//                        items : [ requiredLabel, requiredLabel, requiredLabel]
	                }]
                }]
           });
        },
        buildLicenseAgreement : function() {
            this.panelLicenseAgreement = new Ext.Panel({
                name : 'License Agreement',
                parentId : this.getId(),
                title : this.i18n._('License Agreement'),
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
        // save function
        saveAction : function() {
            if (this.validate()) {
//                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
//                rpc.brandingManager.setBaseSettings(function(result, exception) {
//                    Ext.MessageBox.hide();
//                    if (exception) {
//                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
//                        return;
//                    }
//                    this.cancelAction();
//                }.createDelegate(this), this.getBrandingBaseSettings());
            }
        }

    });

}