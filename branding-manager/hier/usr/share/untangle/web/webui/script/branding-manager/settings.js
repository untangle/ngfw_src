Ext.define('Webui.branding-manager.settings', {
    extend:'Ung.AppWin',
    hasReports: false,
    panelBranding: null,
    getAppSummary: function() {
        return i18n._("The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).");
    },
    initComponent: function(container, position) {
        this.buildBranding();
        this.buildTabPanel([this.panelBranding]);
        this.callParent(arguments);
    },
    buildBranding: function() {
        this.panelBranding = Ext.create('Ext.panel.Panel',{
            name: 'Branding',
            helpSource: 'branding_manager_settings',
            title: i18n._('Settings'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            listeners: {
                "afterrender": {
                    fn: Ext.bind(function(elem, checked) {
                        if (checked) {
                            this.panelBranding.enableFileUpload(!this.getSettings().defaultLogo);
                        }
                    }, this)
                }
            },
            enableFileUpload: function(enabled) {
                if (enabled) {
                    this.down('[name="upload_logo_file_textfield"]').enable();
                    this.down('button[name="upload_logo_file_button"]').enable();
                } else {
                    this.down('[name="upload_logo_file_textfield"]').disable();
                    this.down('button[name="upload_logo_file_button"]').disable();
                }
            },
            items: [{
                title: i18n._('Logo'),
                items: [{
                    xtype: 'radio',
                    name: 'Logo',
                    hideLabel: true,
                    boxLabel: i18n._('Use Default Logo'),
                    checked: this.getSettings().defaultLogo,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                if (checked) {
                                    this.getSettings().defaultLogo = true;
                                    this.panelBranding.enableFileUpload(false);
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'radio',
                    name: 'Logo',
                    hideLabel: true,
                    boxLabel: i18n._('Use Custom Logo. Maximum size is 166 x 100.'),
                    checked: !this.getSettings().defaultLogo,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                if (checked) {
                                    this.getSettings().defaultLogo = false;
                                    this.panelBranding.enableFileUpload(true);
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'form',
                    bodyStyle: 'padding:0px 0px 0px 25px',
                    name: 'upload_logo_form',
                    url: 'upload',
                    border: false,
                    isDirty: function () {
                        return false;
                    },
                    items: [{
                        xtype: 'filefield',
                        fieldLabel: i18n._('File'),
                        name: 'upload_logo_file_textfield',
                        width: 500,
                        labelWidth: 50,
                        allowBlank: false,
                        validateOnBlur: false
                    },{
                        xtype: 'button',
                        name: 'upload_logo_file_button',
                        text: i18n._("Upload"),
                        handler: Ext.bind(function() {
                            this.panelBranding.onUpload();
                        }, this),
                        disabled: this.getSettings().defaultLogo
                    },{
                        xtype: 'hidden',
                        name: 'type',
                        value: 'logo'
                    }]
                }]
            },{
                title: i18n._('Contact Information'),
                defaults: {
                    width: 300
                },
                items: [{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Company Name'),
                    name: 'Company Name',
                    allowBlank: true,
                    value: this.getSettings().companyName,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().companyName = newValue;
                            }, this)
                        }
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Company URL'),
                    name: 'Company URL',
                    allowBlank: true,
                    value: this.getSettings().companyUrl,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().companyUrl = newValue;
                            }, this)
                        }
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Contact Name'),
                    name: 'Contact Name',
                    allowBlank: true,
                    value: ("your network administrator" == this.getSettings().contactName)?i18n._("your network administrator"):this.getSettings().contactName,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().contactName = newValue;
                            }, this)
                        }
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Contact Email'),
                    name: 'contact_email',
                    allowBlank: true,
                    vtype: 'email',
                    value: this.getSettings().contactEmail,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().contactEmail = newValue;
                            }, this)
                        }
                    }
                }]
            },{
                title: i18n._('Banner Message'),
                items:[{
                    xtype: "textarea",
                    allowBlank: true,
                    name: "bannerMessage",
                    width: 500,
                    height: 100,
                    fieldLabel: i18n._("Message Text"),
                    value: this.settings.bannerMessage,
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            newValue = Ext.util.Format.stripTags(newValue);
                            newValue = newValue.replace(/<[^\s]+/g, "");
                            elem.setRawValue(newValue);
                            this.settings.bannerMessage = newValue;
                        }, this)
                    }
                }]
            }],
            onUpload: Ext.bind(function() {
                var formPanel = this.panelBranding.down('form[name="upload_logo_form"]');
                var fileField = formPanel.down('filefield');
                if (fileField.getValue().length === 0) {
                    Ext.MessageBox.alert(i18n._("Failed"), i18n._('Please select an image to upload.'));
                    return;
                }
                formPanel.getForm().submit({
                    waitMsg: i18n._('Please wait while your logo image is uploaded...'),
                    success: Ext.bind(function(form, action) {
                        this.needRackReload = true;
                        Ext.MessageBox.alert(i18n._("Succeeded"), i18n._("Upload Logo Succeeded."),
                            function() {
                                fileField.reset();
                            }
                        );
                    }, this),
                    failure: Ext.bind(function(form, action) {
                        Ext.MessageBox.alert(i18n._("Failed"), i18n._("Upload Logo Failed. The logo must be the correct dimensions and in GIF, PNG, or JPG format."));
                    }, this)
                });
            }, this)
        });
    },
    validate: function() {
        return this.panelBranding.down('textfield[name="contact_email"]').validate();
    },
    beforeSave: function(isApply, handler) {
        this.needRackReload = true;
        handler.call(this, isApply);
    },
    closeWindow: function() {
        this.callParent(arguments);
        if (this.needRackReload) {
            Ung.Util.goToStartPage();
        }
    }
});
//# sourceURL=branding-manager-settings.js
