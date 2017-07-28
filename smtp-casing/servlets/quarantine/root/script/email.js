Ext.define('Ung.view.Main', {
    extend: 'Ext.panel.Panel',
    layout: {
        type: 'hbox',
        pack: 'center'
    },

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        height: 52,
        style: {
            background: '#1b1e26'
        },
        items: [{
            xtype: 'component',
            width: 80,
            html: '<img src="' + '/images/BrandingLogo.png" style="display: block; height: 40px; margin: 0 auto;"/>',
        }, {
            xtype: 'component',
            html: 'Request Quarantine Digest Emails'.t(),
            style: { color: '#CCC', fontSize: '16px' }
        }]
    }],
    items: [{
        xtype: 'form',
        width: 350,
        height: 'auto',
        padding: 20,
        margin: '50 0 0 0',
        frame: true,
        layout: { type: 'anchor' },
        items: [{
            xtype: 'component',
            // style: { textAlign: 'center' },
            html: '<i class="fa fa-envelope fa-3x" style="color: #508d3e"></i>'
        }, {
            xtype: 'component',
            margin: '10 0',
            html: '<strong>' + 'Enter the email address for which you would like the Quarantine Digest'.t() + '</strong>'
        }, {
            xtype: 'textfield',
            name: 'email',
            anchor: '100%',
            fieldLabel: 'Email Address'.t(),
            labelAlign: 'top',
            allowBlank: false,
            blankText: 'Please enter a valid email address'.t(),
            // emptyText: 'user@example.com',
            enableKeyEvents: true,
            vtype: 'email',
            validateOnChange: false,
            validateOnBlur: false,
            msgTarget: 'under',
            listeners: {
                specialkey: 'onEnter'
            }
        }],
        buttons: [{
            text: 'Request'.t(),
            iconCls: 'fa fa-arrow-circle-right',
            scale: 'medium',
            handler: 'requestEmail',
            margin: 0
        }]
    }],

    controller: {
        onEnter: function (field, e) {
            if (e.getKey() === e.ENTER) {
                this.requestEmail();
            }
        },
        requestEmail: function () {
            var form = this.getView().down('form'), emailField = form.down('textfield[name="email"]');
            if (!form.isValid()) { return; }

            form.setLoading('Please Wait'.t());

            rpc.requestDigest(function(result, ex) {
                form.setLoading(false);
                if (ex) { Util.handleException(ex); return; }
                var message;
                if (result) {
                    message = Ext.String.format('Successfully sent digest to {0}'.t(),  emailField.getValue());
                    emailField.setValue('');
                }  else {
                    message = Ext.String.format('A quarantine does not exist for {0}'.t(), emailField.getValue());
                }
                Ext.MessageBox.show({
                    title : 'Quarantine Request'.t(),
                    msg : message,
                    buttons : Ext.MessageBox.OK,
                    icon : Ext.MessageBox.INFO
                });
            }, emailField.getValue());
        }
    }
});

Ext.define('Ung.Application', {
    extend: 'Ext.app.Application',
    name: 'Ung',
    namespace: 'Ung',
    defaultToken : '',
    mainView: 'Ung.view.Main',
    launch: function () {
        document.title = Ext.String.format('{0} | Request Quarantine Digest'.t(), this.companyName);
    }
});
