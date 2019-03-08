Ext.define('Ung.view.main.Registration', {
    extend: 'Ext.window.Window',
    alias: 'widget.registration',

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
        this.cloudManager = rpc.UvmContext.cloudManager();
        this.forgotPasswordUrl = Util.getStoreUrl() + '?action=forgot_password&' + Util.getAbout();
        this.items = [{
            xtype: 'container',
            defaults: {
                border: false
            },
            items: [{
                height: 100,
                bodyCls: 'branding',
                html: Ext.String.format('<img src="/images/BrandingLogo.png?%s" border=0 style="max-width: 150px; max-height: 140px;"/>',(new Date()).getTime())
            }, {
                xtype: 'button',
                baseCls: 'reg-close',
                html: '<span style="vertical-align: middle;">' + 'Skip'.t() + '</span> <i class="fa fa-close fa-lg"></i>',
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
                        html: '<h1>' + Ext.String.format('Congratulations! {0} is ready to be configured.'.t(), rpc.companyName) + '</h1>' +
                            '<p>' + 'Please register with an untangle.com account before continuing.'.t() + '<br/>' + 'Registering gets you the following benefits:'.t() + '</p>' +
                            '<ul>' +
                                '<li>' + 'Access to your account on untangle.com'.t() + '</li>' +
                                '<li>' + 'Manage your licences, renewals, servers, and contact info all from one dashboard.'.t() + '</li>' +
                                '<li>' + 'Easily transfer licences between servers.'.t() + '</li>' +
                            '</ul>' +
                            '<p>' + 'Registration only takes a second and it is required before installing applications.'.t() + '<br/>' +
                            'Rest assured, we will never spam you or share your contact information with anyone.'.t() + '</p>'
                    }
                }, {
                    xtype: 'button',
                    text: 'Continue'.t(),
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
                    html: 'Login or Create an Account'.t()
                }, {
                    xtype: 'component',
                    columnWidth: 1,
                    cls: 'login-error',
                    hidden: true,
                    border: true,
                    bind: {
                        html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="vertical-align: middle;">{error}</span>',
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
                            html: 'If you are a new Untangle Customer, please click Create New Account below.'.t()
                        }, {
                            xtype: 'button',
                            baseCls: 'reg-btn',
                            margin: '20 0 0 0',
                            text: 'Create an Account'.t(),
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
                            html: 'If you have an account with us, please log in.'.t()
                        }, {
                            name: 'email',
                            fieldLabel: 'Email Address'.t() + ' *',
                            vtype: 'email'
                        }, {
                            name: 'password',
                            fieldLabel: 'Password'.t() + ' *',
                            inputType: 'password',
                            validator: function (pass) {
                                return !pass ? 'You must provide a password.' : true;
                            }
                        }],
                        buttons: [{
                            baseCls: 'html-btn',
                            text: 'Forgot your password?'.t(),
                            href: this.forgotPasswordUrl
                        }, '->', {
                            baseCls: 'reg-btn',
                            itemId: 'loginBtn',
                            formBind: true,
                            margin: '0 10 0 0',
                            text: 'Login'.t(),
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
                    html: 'Create an Account'.t()
                }, {
                    xtype: 'component',
                    cls: 'login-error',
                    hidden: true,
                    bind: {
                        html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="vertical-align: middle;">{error}</span>',
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
                            fieldLabel: 'First Name'.t() + ' *',
                            validator: function (fname) {
                                if (!fname) {
                                    return 'Please specify your first name.'.t();
                                }
                                if (fname.length < 2) {
                                    return 'First name must be at least 2 characters.'.t();
                                }
                                if (fname.length > 24) {
                                    return 'First name must be less than 24 characters.'.t();
                                }
                                return true;
                            }
                        }, {
                            name: 'lastName',
                            columnWidth: 0.5,
                            fieldLabel: 'Last Name'.t() + ' *',
                            validator: function (lname) {
                                if (!lname) {
                                    return 'Please specify your last name.'.t();
                                }
                                if (lname.length < 2) {
                                    return 'Last name must be at least 2 characters.'.t();
                                }
                                if (lname.length > 24) {
                                    return 'Last name must be less than 24 characters.'.t();
                                }
                                return true;
                            }
                        }, {
                            name: 'companyName',
                            columnWidth: 0.5,
                            fieldLabel: 'Company Name'.t()
                        }, {
                            name: 'email',
                            columnWidth: 0.5,
                            fieldLabel: 'Email Address'.t() + ' *',
                            vtype: 'email'
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
                            fieldLabel: 'Password'.t() + ' *',
                            validator: function (pass) {
                                if (!pass) {
                                    return 'Please specify your password.'.t();
                                }
                                if (pass.length < 6) {
                                    return 'Your password is too short.'.t();
                                }
                                return true;
                            }
                        }, {
                            name: 'passwordConfirm',
                            columnWidth: 0.5,
                            fieldLabel: 'Confirm Password'.t() + ' *',
                            validator: function (pass) {
                                if (!pass) {
                                    return 'Please retype your password.'.t();
                                }
                                if (pass.length < 6) {
                                    return 'Your password is too short.'.t();
                                }
                                return true;
                            }
                        }]
                    }]
                }],
                buttons: [{
                    baseCls: 'html-btn',
                    text: '<i class="fa fa-angle-left fa-lg"></i> <span style="vertical-align: middle;">' + 'Back'.t() + '</span>',
                    handler: 'showLogin'
                }, '->', {
                    baseCls: 'reg-btn',
                    itemId: 'registerBtn',
                    formBind: true,
                    text: 'Register'.t(),
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
                    html: 'Done!'.t()
                }, {
                    xtype: 'component',
                    margin: 20,
                    html: '<p style="font-size: 14px;">' + Ext.String.format('Your account is configured and {0} is ready to be configured.'.t(), rpc.companyName) + '</p>'
                }, {
                    xtype: 'button',
                    text: 'Continue'.t(),
                    baseCls: 'reg-btn',
                    margin: '20 0 0 0',
                    handler: 'finishReg'
                }]
            }]
        }];
        this.callParent(arguments);
    }

});
