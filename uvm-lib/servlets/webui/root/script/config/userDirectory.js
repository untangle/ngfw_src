if (!Ung.hasResource["Ung.UserDirectory"]) {
    Ung.hasResource["Ung.UserDirectory"] = true;

    Ung.UserDirectory = Ext.extend(Ung.ConfigWin, {
        panelActiveDirectoryConnector : null,
        panelLocalDirectory : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('User Directory')
            }];
            Ung.UserDirectory.superclass.initComponent.call(this);
        },
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.UserDirectory.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 2 tabs
        },
        initSubCmps : function() {
            this.buildActiveDirectoryConnector();
            this.buildLocalDirectory();
            // builds the tab panel with the tabs
            var pageTabs = [this.panelActiveDirectoryConnector, this.panelLocalDirectory];
            this.buildTabPanel(pageTabs);
            this.tabs.activate(this.panelActiveDirectoryConnector);
        },
//        getSystemInfo : function(forceReload) {
//            if (forceReload || this.rpc.systemInfo === undefined) {
//                this.rpc.systemInfo = rpc.adminManager.getSystemInfo();
//            }
//            return this.rpc.systemInfo;
//        },
//        getRegistrationInfo : function(forceReload) {
//            if (forceReload || this.rpc.registrationInfo === undefined) {
//                this.rpc.registrationInfo = rpc.adminManager.getRegistrationInfo();
//            }
//            return this.rpc.registrationInfo;
//        },
//        hasPremiumLicense : function(forceReload) {
//            if (forceReload || this.rpc.hasPremiumLicense === undefined) {
//                this.rpc.hasPremiumLicense = main.getLicenseManager().hasPremiumLicense();
//            }
//            return this.rpc.hasPremiumLicense;
//        },
//        getLicenseAgreement : function(forceReload) {
//            if (forceReload || this.rpc.getLicenseAgreement === undefined) {
//                this.rpc.getLicenseAgreement = main.getLicenseManager().getLicenseAgreement();
//            }
//            return this.rpc.getLicenseAgreement;
//        },
        buildActiveDirectoryConnector : function() {
            this.panelActiveDirectoryConnector = new Ext.Panel({
                name : 'Active Directory (AD) Connector',
                parentId : this.getId(),
                title : this.i18n._('Active Directory (AD) Connector'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{html : 'TODO'}]
            });
        },
        buildLocalDirectory : function() {
            this.panelLocalDirectory = new Ext.Panel({
                name : 'Local Directory',
                parentId : this.getId(),
                title : this.i18n._('Local Directory'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{html : 'TODO'}]
           });
        },
        validateClient : function() {
//            var companyNameCmp = Ext.getCmp('companyName');
//            if (!companyNameCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the company name.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        companyNameCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var firstNameCmp = Ext.getCmp('firstName');
//            if (!firstNameCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out your first name.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        firstNameCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var lastNameCmp = Ext.getCmp('lastName');
//            if (!lastNameCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the last name.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        lastNameCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var emailAddrCmp = Ext.getCmp('emailAddr');
//            if (!emailAddrCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the email address in the format "user@domain.com".'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        emailAddrCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var numSeatsCmp = Ext.getCmp('numSeats');
//            if (!numSeatsCmp.isValid()) {
//                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('You must fill out the number of computers protected by Untangle.'),
//                    function () {
//                        this.tabs.activate(this.panelRegistration);
//                        numSeatsCmp.focus(true);
//                    }.createDelegate(this) 
//                );
//                return false;
//            }
//            this.getRegistrationInfo().companyName = companyNameCmp.getValue();
//
//            var address1Cmp = Ext.getCmp('address1');
//            this.getRegistrationInfo().address1 = address1Cmp.getValue();
//            var address2Cmp = Ext.getCmp('address2');
//            this.getRegistrationInfo().address2 = address2Cmp.getValue();
//            var cityCmp = Ext.getCmp('city');
//            this.getRegistrationInfo().city = cityCmp.getValue();
//            var stateCmp = Ext.getCmp('state');
//            this.getRegistrationInfo().state = stateCmp.getValue();
//            var zipcodeCmp = Ext.getCmp('zipcode');
//            this.getRegistrationInfo().zipcode = zipcodeCmp.getValue();
//            var phoneCmp = Ext.getCmp('phone');
//            this.getRegistrationInfo().phone = phoneCmp.getValue();
            
            return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
//                rpc.adminManager.setRegistrationInfo(function(result, exception) {
//                    Ext.MessageBox.hide();
//                    if (exception) {
//                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
//                        return;
//                    }
//                    this.cancelAction();
//                }.createDelegate(this), this.getRegistrationInfo());
            }
        }

    });

}