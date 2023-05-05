Ext.define('Ung.Setup.ServerSettings', {
    extend: 'Ext.form.Panel',
    alias: 'widget.ServerSettings',

    title: 'Server Settings'.t(),
    description: 'Configure the Server'.t(),

    layout: {
        type: 'hbox',
        align: 'begin',
        pack: 'center'
    },

    defaults: {
        layout: {
            type: 'vbox',
            align: 'stretch',
        }
    },

    items: [{
        xtype: 'container',
        margin: '100 20 0 0',
        width: 200,
        defaults: {
            labelAlign: 'top'
        },
        items: [{
            xtype: 'component',
            cls: 'sectionheader',
            // margin: '30 0 0 0',
            html: 'Admin account'.t()
        }, {
            xtype: 'label',
            margin: '5 0',
            style: { color: '#999' },
            html: 'Choose a password for the <strong>admin</strong> account'.t()
        }, {
            xtype: 'textfield',
            inputType: 'password',
            fieldLabel: 'Password'.t(),
            name: 'password',
            id: 'settings_password',
            allowBlank: false,
            minLength: 3,
            minLengthText: Ext.String.format('The password is shorter than the minimum {0} characters.'.t(), 3)
        }, {
            xtype: 'textfield',
            inputType: 'password',
            fieldLabel: 'Confirm Password'.t(),
            name: 'confirmPassword',
            allowBlank: false,
            comparePasswordField: 'settings_password',
            vtype: 'passwordConfirmCheck'
        }, {
            xtype: 'textfield',
            inputType: 'text',
            name: 'adminEmail',
            fieldLabel: 'Admin Email'.t(),
            allowBlank: true,
            vtype: 'email',
            value: rpc.adminEmail
        }, {
            xtype: 'label',
            style: { color: '#999', fontSize: '11px' },
            html: 'Administrators receive email alerts and report summaries.'.t()
        }]
    }, {
        xtype: 'container',
        width: 300,
        margin: '100 0 0 20',
        defaults: {
            labelAlign: 'top'
        },
        items: [{
            xtype: 'component',
            cls: 'sectionheader',
            html: 'Install Type'.t()
        }, {
            xtype: 'label',
            margin: '5 0',
            style: { color: '#999' },
            html: 'Install type determines the optimal default settings for this deployment.'.t()
        }, {
            xtype: 'combo',
            name: 'installType',
            value: '',
            allowBlank: false,
            fieldLabel: 'Choose Type'.t(),
            emptyText: 'Select'.t(),
            editable: false,
            queryMode: 'local',
            store: [
                ['school', 'School'.t()],
                ['college', 'Higher Education'.t()],
                ['government', 'State & Local Government'.t()],
                ['fedgovernment', 'Federal Government'.t()],
                ['nonprofit', 'Nonprofit'.t()],
                ['retail', 'Hospitality & Retail'.t()],
                ['healthcare', 'Healthcare'.t()],
                ['financial', 'Banking & Financial'.t()],
                ['home', 'Home'.t()],
                ['student', 'Student'.t()],
                ['other', 'Other'.t()]
            ]
        }, {
            xtype: 'component',
            cls: 'sectionheader',
            margin: '30 0 0 0',
            html: 'Timezone'.t()
        }, {
            xtype: 'combo',
            name: 'timezone',
            editable: false,
            store: rpc.timezones,
            margin: '5 0',
            queryMode: 'local',
            value: rpc.timezoneID
        }]
    }],

    listeners: {
        save: 'onSave'
    },

    controller: {
        onSave: function (cb) {
            var me = this, form = me.getView(), values = form.getValues();

            if (!form.isValid()) { return; }

            Ung.app.loading('loading');
            rpc.setup = new JSONRpcClient('/setup/JSON-RPC').SetupContext; // to avoid invalid security nonce

            // set timezone
            if (rpc.timezoneID !== values.timezone) {
                rpc.setup.setTimeZone(function (result, ex) {
                    if (ex) { Ung.app.loading(false); Util.handleException(ex); return; }
                    rpc.timezoneID = values.timezone;
                    me.saveAdminPassword(function () { cb(); });
                }, values.timezone);
            } else {
                me.saveAdminPassword(function () { cb(); });
            }
        },

        saveAdminPassword: function (cb) {
            var values = this.getView().getValues();
            rpc.setup.setAdminPassword(function (result, ex) {
                if (ex) { Util.handleException(ex); return; } // Unable to save the admin password
                Util.authenticate(values.password, function () { cb(); });
            }, values.password, values.adminEmail, values.installType);
        }
    }





});
