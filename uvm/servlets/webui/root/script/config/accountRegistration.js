Ext.define('Webui.config.accountRegistration', {
    extend: 'Ung.StatusWin',
    helpSource: 'account_registration',
    doSize: function() {
        var objSize = Ung.Main.viewport.getSize();
        var width = Math.min(770, objSize.width - Ung.Main.contentLeftWidth);
        var height = Math.min(470, objSize.height);
        var x = Ung.Main.contentLeftWidth + Math.round((objSize.width - Ung.Main.contentLeftWidth-width)/2); 
        var y = Math.round((objSize.height-height)/2);
        this.setPosition(x, y);
        this.setSize({width:width, height: height});
    },
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Account Registration')
        }];
        rpc.jsonrpc.UvmContext.getServerUID(Ext.bind(function (result, exception) {
            if(Ung.Util.handleException(exception)) return;
            rpc.serverUID = result;
        }, this));
        this.storeApiUrl = rpc.storeUrl.replace("/store/open.php","/api/v1");
        this.items = {
            xtype: 'panel',
            layout: { type: 'vbox', align: 'stretch'},
            items: [{
                xtype: 'container',
                layout: 'center',
                items: {
                    xtype: 'component',
                    html: '<img src="/images/BrandingLogo.png?'+(new Date()).getTime()+'" border="0"/>',
                    width: 166,
                    height: 100
                },
                height: 118,
                flex: 0
            }, {
                xtype: 'container',
                name: 'cardsContainer',
                layout: 'card',
                flex: 1,
                items: [
                //Welcome Step
                {
                    xtype: 'container',
                    itemId: 'step1',
                    autoScroll: true,
                    items: [{
                        xtype: 'component',
                        html: Ext.String.format(i18n._("Congratulations! {0} is ready to be configured."), rpc.companyName),
                        margin: '10 20 0 150'
                    }, {
                        xtype: 'component',
                        html: i18n._("Please register with an untangle.com account before continuing."),
                        margin: '10 20 0 150'
                    }, {
                        xtype: 'component',
                        html: i18n._("Registering gets you the following benefits:"),
                        margin: '10 20 0 150'
                    }, {
                        xtype: 'component',
                        html: "<li>" + i18n._("Access to your account on untangle.com") + "</li>",
                        margin: '0 20 0 180'
                    }, {
                        xtype: 'component',
                        html: "<li>" + i18n._("Manage your licences, renewals, servers, and contact info all from one dashboard.") + "</li>",
                        margin: '0 20 0 180'
                    }, {
                        xtype: 'component',
                        html: "<li>" + i18n._("Easily transfer licences between servers.") + "</li>",
                        margin: '0 20 0 180'
                    }, {
                        xtype: 'component',
                        html: i18n._("Registration only takes a second and it is required before installing applications."),
                        margin: '10 20 0 150'
                    }, {
                        xtype: 'component',
                        html: i18n._("Rest assured, we will never spam you or share your contact information with anyone."),
                        margin: '10 20 0 150'
                    }, {
                        xtype: 'container',
                        layout: 'center',
                        height: 70,
                        items: {
                            xtype: 'button',
                            text: i18n._('Continue'),
                            padding: '7 30 7 30',
                            handler: function() {
                                this.down("container[name=cardsContainer]").setActiveItem("step2");
                            },
                            scope: this
                        }
                    }]
                }, 
                {
                    xtype: 'container',
                    itemId: 'step2',
                    layout: { type: 'hbox', align: 'stretch'},
                    style: {borderTop: '1px solid #D0D0D0'},
                    items: [{
                        xtype: 'container',
                        flex: 7,
                        autoScroll: true,
                        name: 'loginForm',
                        layout: { type: 'vbox', align: 'stretch'},
                        items: [{
                            xtype: 'container',
                            layout: 'center',
                            width: '100%',
                            height: 36,
                            items: {
                                xtype: 'component',
                                style: {fontSize: '16px'},
                                html: i18n._('Login')
                            }
                        }, {
                            xtype:'textfield',
                            width: 300,
                            margin: '0 40 0 20',
                            name: 'emailAddress',
                            vtype: 'email',
                            labelAlign: 'top',
                            fieldLabel: i18n._("Email Address")+ " *"
                            
                        }, {
                            xtype:'textfield',
                            width: 300,
                            margin: '10 40 0 20',
                            labelAlign: 'top',
                            inputType: 'password',
                            name: 'password',
                            fieldLabel: i18n._("Password")+ " *"
                        }, {
                            xtype: 'container',
                            layout: 'center',
                            height: 70,
                            items: {
                                xtype: 'button',
                                text: i18n._('Login'),
                                padding: '7 30 7 30',
                                handler: function() {
                                    this.login();
                                },
                                scope: this
                            }
                        }]
                    }, {
                        xtype: 'container',
                        flex: 10,
                        autoScroll: true,
                        style: {borderLeft: '1px solid #D0D0D0'},
                        layout: { type: 'vbox', align: 'stretch'},
                        name: 'registerForm',
                        items: [{
                            xtype: 'container',
                            layout: 'center',
                            width: '100%',
                            height: 36,
                            items: {
                                xtype: 'component',
                                style: {fontSize: '16px'},
                                html: i18n._('Create an Account')
                            }
                        }, {
                            xtype: 'container',
                            layout: 'column',
                            items:[{
                                xtype:'textfield',
                                columnWidth: 0.5,
                                margin: '0 10 0 20',
                                labelAlign: 'top',
                                name: 'firstName',
                                fieldLabel: i18n._("First Name")+ " *"
                            }, {
                                xtype:'textfield',
                                columnWidth: 0.5,
                                margin: '0 20 0 10',
                                labelAlign: 'top',
                                name: 'lastName',
                                fieldLabel: i18n._("Last Name")+ " *"
                            }]
                        }, {
                            xtype:'textfield',
                            margin: '0 50 0 20',
                            labelAlign: 'top',
                            name: 'companyName',
                            fieldLabel: i18n._("Company Name")
                        }, {
                            xtype:'textfield',
                            vtype: 'email',
                            margin: '0 50 0 20',
                            labelAlign: 'top',
                            name: 'emailAddress',
                            fieldLabel: i18n._("Email Address")+ " *"
                            
                        }, {
                            xtype: 'container',
                            layout: 'column',
                            items:[{
                                xtype:'textfield',
                                inputType: 'password',
                                columnWidth: 0.5,
                                margin: '0 10 0 20',
                                labelAlign: 'top',
                                name: 'password',
                                fieldLabel: i18n._("Password")+ " *"
                            }, {
                                xtype:'textfield',
                                inputType: 'password',
                                margin: '0 20 0 10',
                                labelAlign: 'top',
                                columnWidth: 0.5,
                                name: 'confirmPassword',
                                fieldLabel: i18n._("Confirm Password")+ " *"
                            }]
                        }, {
                            xtype: 'container',
                            layout: 'center',
                            margin: '10 0 0 0',
                            height: 40,
                            items: {
                                xtype: 'button',
                                text: i18n._('Register'),
                                padding: '7 30 7 30',
                                handler: function() {
                                    this.register();
                                },
                                scope: this
                            }
                        }]
                    }]
                }, 
                //Subscriptions step
                {
                    xtype: 'container',
                    itemId: 'step3',
                    autoScroll: true,
                    items: [{
                        xtype: 'component',
                        html: i18n._("Your untangle.com account has subscriptions that are not assigned to any server."),
                        margin: '10 120 0 150'
                    }, {
                        xtype: 'component',
                        html: i18n._("If you wish to assign a subscription to this server visit your account and transfer the subscription to this server."),
                        margin: '10 120 0 150'
                    }, {
                        xtype: 'component',
                        html: i18n._("If you do not wish to do this, you can skip this and do this at any time in the future by visiting your account at untangle.com"),
                        margin: '10 120 0 150'
                    }, {
                        xtype: 'container',
                        layout: 'center',
                        height: 70,
                        items: {
                            xtype: 'container',
                            layout: 'hbox',
                            items: [{
                                xtype: 'button',
                                text: i18n._('Open My Account'),
                                padding: '7 10 7 10',
                                width: 150,
                                handler: function() {
                                    Ung.Main.openMyAccountScreen();
                                },
                                scope: this
                            }, {
                                xtype: 'button',
                                text: i18n._('Skip'),
                                style: {marginLeft: '150px'},
                                padding: '7 10 7 10',
                                width: 150,
                                handler: function() {
                                    this.checkVoucher();
                                },
                                scope: this
                            }]
                        }
                    }]
                },
                //Voucher step
                {
                    xtype: 'container',
                    itemId: 'step4',
                    autoScroll: true,
                    items: [{
                        xtype: 'component',
                        margin: '10 120 0 150',
                        html: Ext.String.format(i18n._("If your {0} appliance came with a voucher key to redem your subscription please enter it now. Otherwise simply skip this step."), rpc.companyName)
                    }, {
                        xtype:'textfield',
                        margin: '20 0 0 150',
                        width: 350,
                        name: "voucher",
                        labelAlign: 'top',
                        fieldLabel: i18n._("Voucher")+ " *"
                    }, {
                        xtype: 'container',
                        layout: 'center',
                        height: 70,
                        items: {
                            xtype: 'container',
                            layout: 'hbox',
                            items: [{
                                xtype: 'button',
                                text: i18n._('Redeem Voucher'),
                                padding: '7 10 7 10',
                                width: 150,
                                handler: function() {
                                    this.redeemVoucher();
                                },
                                scope: this
                            }, {
                                xtype: 'button',
                                text: i18n._('Skip'),
                                style: {marginLeft: '150px'},
                                padding: '7 10 7 10',
                                width: 150,
                                handler: function() {
                                    this.finished = true;
                                    this.down("container[name=cardsContainer]").setActiveItem("step5");
                                },
                                scope: this
                            }]
                        }
                    }]
                },
                //Finish step
                {
                    xtype: 'container',
                    itemId: 'step5',
                    autoScroll: true,
                    items: [{
                        xtype: 'component',
                        html: i18n._("All done!"),
                        margin: '10 20 0 150'
                    }, {
                        xtype: 'component',
                        html: Ext.String.format(i18n._("Your account is configured and {0} is ready to be configured."), rpc.companyName),
                        margin: '10 20 0 150'
                    }, {
                        xtype: 'container',
                        layout: 'center',
                        height: 70,
                        items: {
                            xtype: 'button',
                            text: i18n._('Continue'),
                            padding: '7 30 7 30',
                            handler: function() {
                                this.finished = true;
                                this.closeWindow();
                            },
                            scope: this
                        }
                    }]
                }
                ]
            }]
        };
        this.callParent(arguments);
    },
    login: function() {
        var form = this.down("container[name=loginForm]");
        var emailAddress = form.down("textfield[name=emailAddress]");
        var password = form.down("textfield[name=password]");
        var errors = [];
        if(!emailAddress.isValid()) {
            errors.push(i18n._("The Email Address is not valid."));
        } else if(Ext.isEmpty(emailAddress.getValue())) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Email Address")));
        } 
        if(Ext.isEmpty(password.getValue())) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Password")));
        }
        if(errors.length>0) {
            Ext.MessageBox.alert(i18n._("Warning"), errors.join("<br/>"));
            return;
        }
        this.setLoading(true);
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/account/login",
            scope: this,
            params: {
                email: emailAddress.getValue(),
                password: password.getValue(),
                uid: rpc.serverUID
            },
            success: function(response, opts) {
                this.setLoading(false);
                if( response!=null) {
                    if(!response.success) {
                        Ext.MessageBox.alert(i18n._("Warning"), response.customerMessage);
                        return;
                    }
                    if(response.token) {
                        this.email = emailAddress.getValue();
                        this.token = response.token;
                        this.checkSubscriptions();
                    }
                }
            },
            failure: function(response, opts) {
                this.setLoading(false);
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("Failed to access the store to login"));
            }
        });
    },
    register: function() {
        var form = this.down("container[name=registerForm]");
        var firstName = form.down("textfield[name=firstName]").getValue().trim();
        var lastName = form.down("textfield[name=lastName]").getValue().trim();
        var companyName = form.down("textfield[name=companyName]").getValue().trim();
        var emailAddress = form.down("textfield[name=emailAddress]");
        var password = form.down("textfield[name=password]");
        var confirmPassword = form.down("textfield[name=confirmPassword]");
        
        var errors = [];
        if(Ext.isEmpty(firstName)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("First Name")));
        }
        if(Ext.isEmpty(lastName)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Last Name")));
        }
        if(!emailAddress.isValid()) {
            errors.push(i18n._("The Email Address is not valid."));
        } else if(Ext.isEmpty(emailAddress.getValue())) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Email Address")));
        } 
        if(Ext.isEmpty(password.getValue())) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Password")));
        }
        if(password.getValue() != confirmPassword.getValue()) {
            errors.push(i18n._("Passwords do not match."));
        }
        if(errors.length>0) {
            Ext.MessageBox.alert(i18n._("Warning"), errors.join("<br/>"));
            return;
        }
        this.setLoading(true);
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/account/create",
            scope: this,
            params: {
                email: emailAddress.getValue(), 
                password: password.getValue(),
                fname: firstName,
                lname: lastName,
                cname: companyName,
                uid: rpc.serverUID
            },
            success: function(response, opts) {
                this.setLoading(false);
                if( response!=null) {
                    if(!response.success) {
                        Ext.MessageBox.alert(i18n._("Warning"), response.customerMessage);
                        return;
                    }
                    if(response.token) {
                        this.email = emailAddress.getValue();
                        this.token = response.token;
                        this.checkSubscriptions();
                    }
                }
            },
            failure: function(response, opts) {
                this.setLoading(false);
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("Failed to access the store to create the account"));
            }
            
        });
    },
    checkSubscriptions: function() {
        this.setLoading(true);
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/subscriptions",
            params: {
                email: this.email,
                token: this.token
            },
            success: function(response, opts) {
                this.setLoading(false);
                if( response!=null) {
                    if(!response.success) {
                        Ext.MessageBox.alert(i18n._("Warning"), response.customerMessage);
                        return;
                    }
                    if( response.subscriptions && response.subscriptions.length>0) {
                        this.down("container[name=cardsContainer]").setActiveItem("step3");
                    } else {
                        this.checkVoucher();
                    }
                }
                
            },
            failure: function(response, opts) {
                this.setLoading(false);
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("Failed to access the store to check the subscriptions"));
            },
            scope: this
        });
    },
    checkVoucher: function() {
        this.setLoading(true);
        rpc.jsonrpc.UvmContext.isVoucher(Ext.bind(function(result, exception) {
            this.setLoading(false);
            if(Ung.Util.handleException(exception)) return;
            if(result) {
                this.down("container[name=cardsContainer]").setActiveItem("step4");
            } else {
                this.finished = true;
                this.down("container[name=cardsContainer]").setActiveItem("step5");
            }
        }, this));
    },
    redeemVoucher: function() {
        var voucher = form.down("textfield[name=voucher]").getValue().trim();
        var errors = [];
        if(Ext.isEmpty(voucher.getValue())) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Voucher")));
        }
        if(errors.length>0) {
            Ext.MessageBox.alert(i18n._("Warning"), errors.join("<br/>"));
            return;
        }
        //TODO: call redeemVoucher api if this will be implemented
    },
    closeWindow: function() {
        if(this.finished) {
            rpc.jsonrpc.UvmContext.setRegistered(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                rpc.isRegistered = true;
                Ung.Main.showPostRegistrationPopup();
            });
        }
        this.hide();
        Ext.destroy(this);
    }
});
//# sourceURL=accountRegistration.js