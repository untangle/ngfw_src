Ext.define('Ung.config.email.EmailTest', {
    extend: 'Ext.window.Window',
    width: 500,
    height: 300,
    modal: true,

    alias: 'widget.config.email.test',

    requires: [
        'Ung.config.email.EmailTestController'
    ],

    controller: 'config.email.test',

    title: 'Email Test'.t(),
    autoShow: true,

    layout: 'fit',
    plain: false,
    bodyBorder: false,
    border: false,

    items: [{
        xtype: 'form',
        // border: false,
        bodyPadding: 10,
        layout: 'anchor',
        items: [{
            xtype: 'component',
            html: '<strong style="margin-bottom:20px;font-size:11px;display:block;">' + 'Enter an email address to send a test message and then press Send. That email account should receive an email shortly after running the test. If not, the email settings may not be correct.<br/><br/>It is recommended to verify that the email settings work for sending to both internal (your domain) and external email addresses.'.t() + '</strong>'
        }, {
            xtype: 'textfield',
            anchor: '100%',
            vtype: 'email',
            validateOnBlur: true,
            allowBlank: false,
            fieldLabel: 'Email Address'.t(),
            emptyText: '[enter email]'.t(),
            bind: {
                disabled: '{processing === true}'
            }
        }, {
            xtype: 'component',
            bind: {
                html: '{emailRef}'
            }
        }, {
            xtype: 'component',
            padding: 10,
            style: {
                textAlign: 'center',
            },
            hidden: true,
            bind: {
                html: '{processingIcon}',
                hidden: '{processing === false}'
            }
        }],

        buttons: [{
            text: 'Cancel'.t(),
            handler: 'cancel'
        }, {
            text: 'Send'.t(),
            formBind: true,
            handler: 'sendMail'
        }]
    }],

    viewModel: {
        data: {
            processingIcon: null,
            processing: false
        },
    }

});