Ext.define('Webui.config.accountRegistration', {
    extend: 'Ext.window.Window',
    name: 'accountRegistration',
    helpSource: 'account_registration',

    controller: 'registration',
    viewModel: {
        data: {
            error: null
        }
    },

    modal: true,
    monitorResize: true,
    width: '100%',
    height: '100%',
    constrain: true,
    frame: false,
    frameHeader: false,
    header: false,
    bodyBorder: false,
    border: false,
    shadow: false,
    resizable: false,
    draggable: false,
    collapsible: false,
    baseCls: 'reg',
    bodyCls: 'reg-body',

    bodyStyle: {
        borderRadius: '5px',
        boxShadow: '0 0 30px rgba(0, 0, 0, 0.8)'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    defaults: {
        border: false
    },

    plugins: 'responsive',
    responsiveConfig: {
        'width < 768': {
            padding: 0
        },
        'width >= 768': {
            padding: 30
        }
    },

    initComponent: function () {
        this.cloudManager = rpc.jsonrpc.UvmContext.cloudManager();
        this.forgotPasswordUrl = Ung.Main.storeUrl + '?action=forgot_password&' + Ung.Main.about();
        this.items = [{
            xtype: 'container',
            defaults: {
                border: false
            },
            items: [{
                height: 100,
                bodyCls: 'branding',
                html: '<img src="/images/BrandingLogo.png?' + (new Date()).getTime() + '" border=0/>'
            }, {
                xtype: 'button',
                baseCls: 'reg-close',
                html: '<span style="vertical-align: middle;">' + i18n._('Skip') + '</span> <i class="material-icons">close</i>',
                handler: 'skipReg'
            }]
        }, {
            layout: 'card',
            itemId: 'cards',
            items: [{
                xtype: 'container',
                itemId: 'congrats',
                cls: 'card',
                border: false,
                padding: 20,
                maxWidth: 650,
                layout: {
                    type: 'vbox',
                    align: 'stretch',
                    pack: 'middle'
                },
                items: [{
                    xtype: 'component',
                    cls: 'welcome',
                    bind: {
                        html: '<h1>' + Ext.String.format(i18n._('Congratulations! {0} is ready to be configured.'), rpc.companyName) + '</h1>' +
                            '<p>' + i18n._('Please register with an untangle.com account before continuing.') + '<br/>' + i18n._('Registering gets you the following benefits:') + '</p>' +
                            '<ul>' +
                                '<li>' + i18n._('Access to your account on untangle.com') + '</li>' +
                                '<li>' + i18n._('Manage your licences, renewals, servers, and contact info all from one dashboard.') + '</li>' +
                                '<li>' + i18n._('Easily transfer licences between servers.') + '</li>' +
                            '</ul>' +
                            '<p>' + i18n._('Registration only takes a second and it is required before installing applications.') + '<br/>' +
                            i18n._('Rest assured, we will never spam you or share your contact information with anyone.') + '</p>'
                    }
                }, {
                    xtype: 'button',
                    text: i18n._('Continue'),
                    maxWidth: 200,
                    baseCls: 'reg-btn',
                    margin: '20 0 0 0',
                    handler: 'showLogin'
                }]
            }, {
                cls: 'card',
                itemId: 'auth',
                layout: 'column',
                plugins: 'responsive',
                responsiveConfig: {
                    'width < 960': {
                        maxWidth: 480
                    },
                    'width >= 960': {
                        maxWidth: 960
                    }
                },
                border: false,
                defaults: {
                    border: false
                },
                items: [{
                    xtype: 'component',
                    columnWidth: 1,
                    cls: 'title',
                    html: i18n._('Login or Create an Account')
                }, {
                    xtype: 'component',
                    columnWidth: 1,
                    cls: 'login-error',
                    hidden: true,
                    border: true,
                    bind: {
                        html: '<i class="material-icons" style="font-size: 20px;">warning</i> <span style="vertical-align: middle;">{error}</span>',
                        hidden: '{!error}'
                    }
                }, {
                    xtype: 'panel',
                    margin: 10,
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width < 960': {
                            columnWidth: 1,
                            minHeight: 'auto'
                        },
                        'width >= 960': {
                            columnWidth: 0.5,
                            minHeight: 250
                        }
                    },
                    cls: 'reg-panel',
                    title: 'NEW CUSTOMERS',
                    layout: 'column',
                    items: [{
                        xtype: 'component',
                        padding: 10,
                        html: '<img src="/skins/modern-rack/images/admin/icons/icon_new_customer.png"/>',
                        style: {
                            textAlign: 'center'
                        },
                        plugins: 'responsive',
                        responsiveConfig: {
                            'width < 450': {
                                columnWidth: 1,
                                width: 'auto',
                                hidden: true
                            },
                            'width >= 450': {
                                columnWidth: 0,
                                width: 140,
                                hidden: false
                            }
                        },
                    }, {
                        xtype: 'container',
                        columnWidth: 1,
                        padding: 10,
                        layout: {
                            type: 'vbox',
                            align: 'stretch'
                        },
                        items: [{
                            xtype: 'component',
                            flex: 1,
                            html: i18n._("If you are a new Untangle Customer, simply click Continue With Checkout below. We'll create an account for you as you checkout.")
                        }, {
                            xtype: 'button',
                            baseCls: 'reg-btn',
                            margin: '20 0 0 0',
                            text: i18n._('Create an Account'),
                            handler: 'showRegister'
                        }]
                    }]
                }, {
                    xtype: 'panel',
                    margin: 10,
                    minHeight: 250,
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width < 960': {
                            columnWidth: 1,
                        },
                        'width >= 960': {
                            columnWidth: 0.5,
                        }
                    },
                    cls: 'reg-panel',
                    title: 'REGISTERED CUSTOMERS',
                    layout: 'column',
                    items: [{
                        xtype: 'component',
                        padding: 10,
                        html: '<img src="/skins/modern-rack/images/admin/icons/icon_registred_customer.png"/>',
                        style: {
                            textAlign: 'center'
                        },
                        plugins: 'responsive',
                        responsiveConfig: {
                            'width < 450': {
                                columnWidth: 1,
                                width: 'auto',
                                hidden: true
                            },
                            'width >= 450': {
                                columnWidth: 0,
                                width: 140,
                                hidden: false
                            }
                        },
                    }, {
                        columnWidth: 1,
                        xtype: 'form',
                        itemId: 'loginForm',
                        name: 'loginForm',
                        border: false,
                        layout: {
                            type: 'vbox',
                            align: 'stretch'
                        },
                        defaults: {
                            xtype: 'textfield',
                            baseCls: 'reg-field',
                            labelAlign: 'top',
                            msgTarget: 'under',
                            errorMsgCls: 'reg-field-error',
                            enableKeyEvents: true
                        },
                        items: [{
                            xtype: 'component',
                            padding: 10,
                            html: i18n._("If you have an account with us, please log in.")
                        }, {
                            name: 'email',
                            fieldLabel: i18n._('Email Address') + ' *',
                            validator: function (email) {
                                if (!/^(")?(?:[^\."\s])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.){1,5}([A-Za-z]){2,6}$/.test(email)) {
                                    return i18n._('You must provide a valid login name or email address.');
                                }
                                return true;
                            }
                        }, {
                            name: 'password',
                            fieldLabel: i18n._('Password') + ' *',
                            inputType: 'password',
                            validator: function (pass) {
                                return !pass ? 'You must provide a password.' : true;
                            }
                        }],
                        buttons: [{
                            baseCls: 'html-btn',
                            text: i18n._('Forgot your password?'),
                            href: this.forgotPasswordUrl
                        }, '->', {
                            baseCls: 'reg-btn',
                            itemId: 'loginBtn',
                            formBind: true,
                            margin: '0 10 0 0',
                            text: i18n._('Login'),
                            handler: 'login'
                        }]
                    }]
                }]
            }, {
                xtype: 'form',
                itemId: 'register',
                name: 'registerForm',
                cls: 'card',
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                // scrollable: true,
                plugins: 'responsive',
                responsiveConfig: {
                    'width < 960': {
                        maxWidth: 480
                    },
                    'width >= 960': {
                        maxWidth: 960
                    }
                },
                border: false,
                items: [{
                    xtype: 'component',
                    cls: 'title',
                    html: i18n._('Create an Account')
                }, {
                    xtype: 'component',
                    cls: 'login-error',
                    hidden: true,
                    bind: {
                        html: '<i class="material-icons" style="font-size: 20px;">warning</i> <span style="vertical-align: middle;">{error}</span>',
                        hidden: '{!error}'
                    }
                }, {
                    title: 'CONTACT INFORMATION',
                    border: false,
                    cls: 'reg-panel small',
                    padding: 10,
                    layout: 'column',
                    items: [{
                        xtype: 'component',
                        padding: 10,
                        style: {
                            textAlign: 'center'
                        },
                        html: '<img src="/skins/modern-rack/images/admin/icons/icon_new_customer.png"/>',
                        plugins: 'responsive',
                        responsiveConfig: {
                            'width < 450': {
                                columnWidth: 1,
                                width: 'auto',
                                hidden: true
                            },
                            'width >= 450': {
                                columnWidth: 0,
                                width: 140,
                                hidden: false
                            }
                        }
                    }, {
                        columnWidth: 1,
                        padding: '10 0',
                        border: false,
                        layout: 'column',
                        defaults: {
                            xtype: 'textfield',
                            baseCls: 'reg-field',
                            labelAlign: 'top',
                            labelSeparator: '',
                            msgTarget: 'under',
                            errorMsgCls: 'reg-field-error',
                            enableKeyEvents: true,
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width < 960': {
                                    columnWidth: 1
                                },
                                'width >= 960': {
                                    columnWidth: 0.5
                                }
                            }
                        },
                        items: [{
                            name: 'firstName',
                            columnWidth: 0.5,
                            fieldLabel: i18n._('First Name') + ' *',
                            validator: function (fname) {
                                if (!fname) {
                                    return i18n._('Please specify your first name.');
                                }
                                return true;
                            }
                        }, {
                            name: 'lastName',
                            columnWidth: 0.5,
                            fieldLabel: i18n._('Last Name') + ' *',
                            validator: function (lname) {
                                if (!lname) {
                                    return i18n._('Please specify your last name.');
                                }
                                return true;
                            }
                        }, {
                            name: 'companyName',
                            columnWidth: 0.5,
                            fieldLabel: i18n._('Company Name')
                        }, {
                            name: 'email',
                            columnWidth: 0.5,
                            fieldLabel: i18n._('Email Address') + ' *',
                            validator: function (email) {
                                if (!email) {
                                    return i18n._('We need your email address to contact you.');
                                }
                                if (!/^(")?(?:[^\."\s])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.){1,5}([A-Za-z]){2,6}$/.test(email)) {
                                    return i18n._('Your email address must be in the format of name@domain.com.');
                                }
                                return true;
                            }
                        }]
                    }]
                }, {
                    title: 'LOGIN INFORMATION',
                    border: false,
                    cls: 'reg-panel small',
                    padding: 10,
                    layout: 'column',
                    items: [{
                        xtype: 'component',
                        padding: 10,
                        html: '<img src="/skins/modern-rack/images/admin/icons/icon_lock.png"/>',
                        style: {
                            textAlign: 'center'
                        },
                        plugins: 'responsive',
                        responsiveConfig: {
                            'width < 450': {
                                columnWidth: 1,
                                width: 'auto',
                                hidden: true
                            },
                            'width >= 450': {
                                columnWidth: 0,
                                width: 140,
                                hidden: false
                            }
                        }
                    }, {
                        columnWidth: 1,
                        padding: '10 0',
                        border: false,
                        layout: 'column',
                        defaults: {
                            xtype: 'textfield',
                            baseCls: 'reg-field',
                            labelAlign: 'top',
                            labelSeparator: '',
                            inputType: 'password',
                            msgTarget: 'under',
                            errorMsgCls: 'reg-field-error',
                            enableKeyEvents: true,
                            plugins: 'responsive',
                            responsiveConfig: {
                                'width < 960': {
                                    columnWidth: 1
                                },
                                'width >= 960': {
                                    columnWidth: 0.5
                                }
                            }
                        },
                        items: [{
                            name: 'password',
                            columnWidth: 0.5,
                            fieldLabel: i18n._('Password') + ' *',
                            validator: function (pass) {
                                if (!pass) {
                                    return i18n._('Please specify your password.');
                                }
                                if (pass.length < 6) {
                                    return i18n._('Your password is too short.');
                                }
                                return true;
                            }
                        }, {
                            name: 'passwordConfirm',
                            columnWidth: 0.5,
                            fieldLabel: i18n._('Confirm Password') + ' *',
                            validator: function (pass) {
                                if (!pass) {
                                    return i18n._('Please retype your password.');
                                }
                                if (pass.length < 6) {
                                    return i18n._('Your password is too short.');
                                }
                                return true;
                            }
                        }]
                    }]
                }],
                buttons: [{
                    baseCls: 'html-btn',
                    text: '<i class="material-icons">keyboard_arrow_left</i><span style="vertical-align: middle;">' + i18n._('Back') + '</span>',
                    handler: 'showLogin'
                }, '->', {
                    baseCls: 'reg-btn',
                    itemId: 'registerBtn',
                    formBind: true,
                    text: i18n._('Register'),
                    handler: 'createAccount'
                }]
            }, {
                xtype: 'container',
                itemId: 'finish',
                cls: 'card',
                border: false,
                layout: {
                    type: 'vbox',
                    align: 'middle',
                    pack: 'center'
                },
                items: [{
                    xtype: 'component',
                    cls: 'title',
                    html: i18n._('Done!')
                }, {
                    xtype: 'component',
                    margin: 20,
                    html: '<p style="font-size: 14px;">' + Ext.String.format(i18n._("Your account is configured and {0} is ready to be configured."), rpc.companyName) + '</p>'
                }, {
                    xtype: 'button',
                    text: i18n._('Continue'),
                    baseCls: 'reg-btn',
                    margin: '20 0 0 0',
                    handler: 'finishReg'
                }]
            }]
        }];
        this.callParent(arguments);
    }

});

Ext.define('Webui.config.accountRegistrationController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.registration',

    control: {
        '#auth': {
            beforeactivate: 'onBeforeActivateAuth'
        },
        '#register': {
            beforeactivate: 'onBeforeActivateRegister'
        },
        'textfield': {
            keyup: 'submitForm'
        }
    },

    onBeforeActivateAuth: function (auth) {
        this.getViewModel().set('error', null);
        auth.down('#loginForm').reset();
    },

    onBeforeActivateRegister: function (form) {
        this.getViewModel().set('error', null);
        form.reset();
    },

    submitForm: function (field, e) {
        var me = this;
        if (e.getCharCode() === 13) {
            var formCmp = field.up('form');
            if (formCmp.getForm().isValid()) {
                if (formCmp.name === 'loginForm') {
                    me.login(formCmp.down('#loginBtn'));
                }
                if (formCmp.name === 'registerForm') {
                    me.createAccount(formCmp.down('#registerBtn'));
                }
            }
        }
    },

    skipReg: function () {
        this.getView().close();
    },

    finishReg: function() {
        rpc.jsonrpc.UvmContext.setRegistered(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.isRegistered = true;
            Ung.Main.showPostRegistrationPopup();
        });
        this.getView().close();
        Ext.destroy(this.getView());
    },

    showLogin: function () {
        this.getView().down('#cards').setActiveItem(1);
    },

    showRegister: function () {
        this.getView().down('#cards').setActiveItem(2);
    },

    login: function (btn) {
        var me = this,
            vm = me.getViewModel(),
            form = this.getView().down('#loginForm').getForm();

        vm.set('error', null);
        btn.setDisabled(true);

        if (form.isValid()) {
            me.getView().cloudManager.accountLogin(function(response, exception) {
                    btn.setDisabled(false);
                    if (exception) {
                        console.log(ex);
                        vm.set('error', ex);
                        return;
                    }
                    if (!response.success) {
                        vm.set('error', i18n._(response.customerMessage));
                        return;
                    }
                    if (response.token) {
                        me.getView().email = form.findField('email').getValue();
                        me.getView().token = response.token;
                        me.getView().down('#cards').setActiveItem(3);
                    }
                },
                form.findField('email').getValue(),
                form.findField('password').getValue()
            );
        }
    },

    createAccount: function (btn) {
        var me = this,
            vm = me.getViewModel(),
            form = me.getView().down('#register').getForm();

        vm.set('error', null);

        if (form.isValid()) {
            if (form.findField('password').getValue() !== form.findField('passwordConfirm').getValue()) {
                vm.set('error', i18n._('"Confirm password" must match the "Password" field.'));
                return;
            }
            btn.setDisabled(true);
            me.getView().cloudManager.accountCreate(function(response, exception) {
                    btn.setDisabled(false);
                    if (exception) {
                        console.log(ex);
                        vm.set('error', ex);
                        return;
                    }
                    if (!response.success) {
                        vm.set('error', i18n._(response.customerMessage));
                        return;
                    }
                    if (response.token) {
                        me.getView().email = form.findField('email').getValue();
                        me.getView().token = response.token;
                        me.getView().down('#cards').setActiveItem(3);
                    }
                },
                form.findField('email').getValue(),
                form.findField('password').getValue(),
                form.findField('firstName').getValue(),
                form.findField('lastName').getValue(),
                form.findField('companyName').getValue(),
                rpc.serverUID,
                rpc.applianceModel,
                rpc.version
            );
        }
    }
});
