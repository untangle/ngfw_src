Ext.define('Ung.apps.captive-portal.view.CaptivePage', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-captive-portal-captivepage',
    itemId: 'captive-page',
    title: 'Captive Page'.t(),
    withValidation: true,
    viewModel: {
        formulas: {
            _redirectUrl: {
                get: function (get) {
                    return get('settings.redirectUrl');
                },
                set: function (value) {
                    var useValue = value;
                    if ((value.length > 0) && (value.indexOf('http://') !== 0) && (value.indexOf('https://') !== 0)) {
                        useValue = ('http://' + value);
                    }
                    this.set('settings.redirectUrl', useValue);
                }
            }
        }
    },

    bodyPadding: 10,
    scrollable: 'y',

    layout: {
        type: 'vbox'
    },

    // tbar can be removed when all users have upgraded to v17.2 and no user is having page type - custom
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        hidden: true,
        bind: {
            hidden: '{settings.pageType !== "CUSTOM"}',
        },
        html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' + 'Custom captive portal has been deprecated. Switch to Basic Message or Basic Logic Configuration.'.t()
    }],

    items: [{
        xtype: 'radiogroup',
        margin: '0 0 20 0',
        bind: '{settings.pageType}',
        simpleValue: true,
        columns: 2,
        items: [
            { boxLabel: '<strong>' + 'Basic Message'.t() + '</strong>', inputValue: 'BASIC_MESSAGE', width: 150 },
            { boxLabel: '<strong>' + 'Basic Login'.t() + '</strong>', inputValue: 'BASIC_LOGIN', width: 150 },
            { xtype: 'button', iconCls: 'fa fa-eye', text: 'Preview Captive Portal Page'.t(), margin: '10 0 0 0', handler: 'previewCaptivePage' }
        ]
    }, {
        xtype: 'fieldset',
        width: '100%',
        title: 'Captive Portal Page Configuration'.t(),
        padding: 10,
        hidden: true,
        bind: {
            hidden: '{settings.pageType !== "BASIC_MESSAGE"}'
        },
        defaults: {
            xtype: 'textfield',
            allowBlank: false,
            labelWidth: 120,
            labelAlign: 'right'
        },
        items: [{
            fieldLabel: 'Page Title'.t(),
            bind: '{settings.basicMessagePageTitle}'
        }, {
            fieldLabel: 'Welcome Text'.t(),
            width: 400,
            bind: '{settings.basicMessagePageWelcome}'
        }, {
            xtype: 'textarea',
            width: 400,
            height: 250,
            fieldLabel: 'Message Text'.t(),
            bind: '{settings.basicMessageMessageText}'
        }, {
            xtype: 'checkbox',
            fieldLabel: 'Agree Checkbox'.t(),
            bind: '{settings.basicMessageAgreeBox}'
        }, {
            fieldLabel: 'Agree Text'.t(),
            width: 400,
            bind: '{settings.basicMessageAgreeText}'
        }, {
            fieldLabel: 'Lower Text'.t(),
            width: 400,
            bind: '{settings.basicMessageFooter}'
        }]
    }, {
        xtype: 'fieldset',
        width: '100%',
        title: 'Captive Portal Page Configuration'.t(),
        padding: 10,
        hidden: true,
        bind: {
            hidden: '{settings.pageType !== "BASIC_LOGIN"}'
        },
        defaults: {
            xtype: 'textfield',
            allowBlank: false,
            labelWidth: 120,
            labelAlign: 'right'
        },
        items: [{
            fieldLabel: 'Page Title'.t(),
            bind: '{settings.basicLoginPageTitle}'
        }, {
            fieldLabel: 'Welcome Text'.t(),
            width: 400,
            bind: '{settings.basicLoginPageWelcome}'
        }, {
            fieldLabel: 'Username Text'.t(),
            bind: '{settings.basicLoginUsername}'
        }, {
            fieldLabel: 'Password Text'.t(),
            bind: '{settings.basicLoginPassword}'
        }, {
            xtype: 'textarea',
            allowBlank: true,
            width: 600,
            height: 200,
            fieldLabel: 'Message Text'.t(),
            bind: '{settings.basicLoginMessageText}'
        }, {
            fieldLabel: 'Lower Text'.t(),
            width: 600,
            bind: '{settings.basicLoginFooter}'
        }]
    }, {
        xtype: 'fieldset',
        width: '100%',
        title: 'HTTPS/SSL Root Certificate Detection'.t(),
        padding: 10,
        hidden: true,
        bind: {
            hidden: '{settings.pageType === "CUSTOM"}'
        },
        items: [{
            xtype: 'radiogroup',
            bind: '{settings.certificateDetection}',
            simpleValue: true,
            columns: 1,
            vertical: true,
            items: [
                { boxLabel: 'Disable certificate detection.'.t(), inputValue: 'DISABLE_DETECTION' },
                { boxLabel: 'Check certificate. Show warning when not detected.'.t(), inputValue: 'CHECK_CERTIFICATE' },
                { boxLabel: 'Require certificate. Prohibit login when not detected.'.t(), inputValue: 'REQUIRE_CERTIFICATE' }
            ]
        }]
    }, {
        xtype: 'fieldset',
        width: '100%',
        title: 'Session Redirect'.t(),
        padding: 10,
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Block instead of capture and redirect unauthenticated HTTPS connections'.t(),
            bind: '{settings.disableSecureRedirect}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Use hostname instead of IP address for the capture page redirect'.t(),
            bind: '{settings.redirectUsingHostname}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Always use HTTPS for the capture page redirect'.t(),
            bind: '{settings.alwaysUseSecureCapture}'
        }, {
            xtype: 'textfield',
            width: 600,
            fieldLabel: 'Redirect URL'.t(),
            bind: '{_redirectUrl}',
            vtype: 'url'
        }, {
            xtype: 'component',
            margin: '10 0 0 0',
            html: '<B>NOTE:</B> The Redirect URL field must start with http:// or https:// and allows you to specify a page to display immediately after user authentication.  If you leave this field blank, users will instead be forwarded to their original destination.'.t()
        }]
    }]
});
