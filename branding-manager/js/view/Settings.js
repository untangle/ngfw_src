Ext.define('Ung.apps.brandingmanager.view.Settings', {
    extend: 'Ext.form.Panel',
    alias: 'widget.app-branding-manager-settings',
    itemId: 'settings',
    title: 'Settings'.t(),
    scrollable: true,

    bodyPadding: 10,
    withValidation: true,

    items:[{
        title: 'Logo'.t(),
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            simpleValue: true,
            bind: '{settings.defaultLogo}',
            items:[{
                xtype: 'radio',
                boxLabel: 'Use Default Logo'.t(),
                inputValue: true
            }, {
                xtype: 'radio',
                boxLabel: 'Use Custom Logo. Maximum size is 166 x 100.'.t(),
                inputValue: false
            }]
        },{
            xtype: 'form',
            bodyStyle: 'padding:0px 0px 0px 25px',
            name: 'upload_logo_form',
            url: 'upload',
            border: false,

            disabled: true,
            hidden: true,
            bind: {
                hidden: '{settings.defaultLogo == true}',
                disabled: '{settings.defaultLogo == true}'
            },

            items: [{
                xtype: 'filefield',
                label: 'Logo'.t(),
                name: 'logo',
                accept: 'image/*',
                buttonText: 'Select Image...'.t(),
                width: 300,
                listeners: {
                    change: 'onUpload'
                }
            },{
                xtype: 'hidden',
                name: 'type',
                value: 'logo'
            }]
        }]
    },{
        title: 'Contact Information'.t(),
        defaults: {
            labelWidth: 150,
            width: 500
        },
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Company Name'.t(),
            name: 'Company Name',
            allowBlank: true,
            bind: '{settings.companyName}',
        },{
            xtype: 'textfield',
            fieldLabel: 'Company URL'.t(),
            name: 'Company URL',
            allowBlank: true,
            bind: '{settings.companyUrl}',
        },{
            xtype: 'textfield',
            fieldLabel: 'Contact Name'.t(),
            name: 'Contact Name',
            allowBlank: true,
            bind: '{settings.contactName}'
        },{
            xtype: 'textfield',
            fieldLabel: 'Contact Email'.t(),
            name: 'contact_email',
            allowBlank: true,
            vtype: 'email',
            bind: '{settings.contactEmail}',
        }]
      },{
        title: 'Banner Message'.t(),
        defaults: {
            labelWidth: 150,
            width: 500
        },
        items:[{
            xtype: "textarea",
            allowBlank: true,
            name: "bannerMessage",
            height: 100,
            fieldLabel: 'Message Text'.t(),
            bind: '{settings.bannerMessage}'
        }]
    }]
});
