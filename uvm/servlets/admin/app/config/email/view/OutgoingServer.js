Ext.define('Ung.config.email.view.OutgoingServer', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config.email.outgoingserver',
    itemId: 'outgoingserver',

    viewModel: true,
    title: 'Outgoing Server'.t(),

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Outgoing Email Server'.t(),
        items: [{
            xtype: 'component',
            padding: 10,
            html: Ext.String.format('The Outgoing Email Server settings determine how the {0} Server sends emails such as reports, quarantine digests, etc. In most cases the cloud hosted mail relay server is preferred. Alternatively, you can configure mail to be sent directly to mail account servers. You can also specify a valid SMTP server that will relay mail for the {0} Server.'.t(), rpc.companyName)
        }, {
            xtype: 'radiogroup',
            margin: '0 0 10 10',
            simpleValue: true,
            bind: '{mailSender.sendMethod}',
            columns: 1,
            vertical: true,
            items: [
                { boxLabel: 'Send email using the cloud hosted mail relay server'.t(), inputValue: 'RELAY' },
                { boxLabel: 'Send email directly'.t(), inputValue: 'DIRECT' },
                { boxLabel: 'Send email using the specified SMTP Server'.t(), inputValue: 'CUSTOM' }
            ]
        }, {
            xtype: 'fieldset',
            border: false,
            defaults: {
                labelWidth: 200,
                labelAlign: 'right'
            },
            disabled: true,
            bind: {
                disabled: '{mailSender.sendMethod !== "CUSTOM"}'
            },
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Server Address or Hostname'.t(),
            }, {
                xtype: 'textfield',
                width: 260,
                fieldLabel: 'Server Port'.t(),
                bind: '{mailSender.smtpPort}',
                vtype: 'port'
            }, {
                xtype: 'component',
                margin: '0 0 0 200',
                padding: 5,
                html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' + 'Warning:'.t() + ' ' + 'SMTPS (465) is deprecated and not supported. Use STARTTLS (587).'.t(),
                hidden: true,
                bind: {
                    hidden: '{mailSender.smtpPort !== "465"}'
                }
            }, {
                xtype: 'checkbox',
                fieldLabel: 'Use Authentication'.t(),
                reference: 'cb',
                bind: '{mailSender.authUser !== null}'
            }, {
                xtype: 'textfield',
                fieldLabel: 'Login'.t(),
                hidden: true,
                bind: {
                    value: '{mailSender.authUser}',
                    hidden: '{!cb.checked}'
                }
            }, {
                xtype: 'textfield',
                inputType: 'password',
                fieldLabel: 'Password'.t(),
                hidden: true,
                bind: {
                    value: '{mailSender.authPass}',
                    hidden: '{!cb.checked}'
                }
            }]
        } ]
    }, {
        xtype: 'fieldset',
        title: 'Email From Address'.t(),
        padding: 10,
        items: [{
            xtype: 'textfield',
            fieldLabel: Ext.String.format('The {0} Server will send email from this address.'.t(), rpc.companyName),
            labelAlign: 'top',
            emptyText: '[enter email]'.t(),
            vtype: 'email',
            allowBlank: false,
            bind: '{mailSender.fromAddress}'
        }]
    }, {
        xtype: 'fieldset',
        title: 'Email Test'.t(),
        padding: 10,
        items: [{
            xtype: 'component',
            html: 'The Email Test will send an email to a specified address with the current configuration. If the test email is not received your settings may be incorrect.'.t()
        }, {
            xtype: 'button',
            margin: '10 0 0 0',
            text: 'Email Test'.t(),
            handler: 'testEmail'
        }]
    }]

});