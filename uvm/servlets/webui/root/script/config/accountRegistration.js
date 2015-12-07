Ext.define('Webui.config.accountRegistration', {
    extend: 'Ung.StatusWin',
    helpSource: 'account_registration',
    doSize: function() {
        var objSize = Ung.Main.viewport.getSize();
        var width = Math.min(770, objSize.width - Ung.Main.contentLeftWidth);
        var height = Math.min(650, objSize.height);
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

        this.expirationMonths = Ext.create('Ext.data.ArrayStore', {
            fields: ['id', 'value'],
            data: [ 
                ["01", "01 - " + i18n._("January")],
                ["02", "02 - " + i18n._("February")],
                ["03", "03 - " + i18n._("March")],
                ["04", "04 - " + i18n._("April")],
                ["05", "05 - " + i18n._("May")],
                ["06", "06 - " + i18n._("June")],
                ["07", "07 - " + i18n._("July")],
                ["08", "08 - " + i18n._("August")],
                ["09", "09 - " + i18n._("September")],
                ["10", "10 - " + i18n._("October)")],
                ["11", "11 - " + i18n._("November")],
                ["12", "12 - " + i18n._("December")]
            ]
        });
        this.cardTypes = Ext.create('Ext.data.ArrayStore', {
            fields: ['id', 'value'],
            data: [ 
                ["MC", i18n._("Mastercard")],
                ["VISA", i18n._("VISA")],
                ["DISC", i18n._("Discover")],
                ["AMEX", i18n._("American Express")]
            ]
        });
        this.defaultFocusButton = function(buttonName){
            return function(me, event, eOpts) {
                    var scope = this.up("container");
                    this.keyNav = Ext.create('Ext.util.KeyNav', this.el, {                    
                        enter: scope.down("button[name="+buttonName+"]"),
                        scope: scope
                    });
                };
        };
        this.renewDefaultChecked = true;
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
                            fieldLabel: i18n._("Email Address")+ " *",
                            listeners: {
                                focus: this.defaultFocusButton("loginButton")
                            }                            
                        }, {
                            xtype:'textfield',
                            width: 300,
                            margin: '10 40 0 20',
                            labelAlign: 'top',
                            inputType: 'password',
                            name: 'password',
                            fieldLabel: i18n._("Password")+ " *",
                            listeners: {
                                focus: this.defaultFocusButton("loginButton")
                            }
                        }, {
                            xtype: 'container',
                            layout: 'center',
                            height: 70,
                            items: {
                                xtype: 'button',
                                name: "loginButton",
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
                                fieldLabel: i18n._("First Name")+ " *",
                                listeners: {
                                    focus: this.defaultFocusButton("registerButton")
                                }
                            }, {
                                xtype:'textfield',
                                columnWidth: 0.5,
                                margin: '0 20 0 10',
                                labelAlign: 'top',
                                name: 'lastName',
                                fieldLabel: i18n._("Last Name")+ " *",
                                listeners: {
                                    focus: this.defaultFocusButton("registerButton")
                                }
                            }]
                        }, {
                            xtype:'textfield',
                            margin: '0 50 0 20',
                            labelAlign: 'top',
                            name: 'companyName',
                            fieldLabel: i18n._("Company Name"),
                            listeners: {
                                focus: this.defaultFocusButton("registerButton")
                            }
                        }, {
                            xtype:'textfield',
                            vtype: 'email',
                            margin: '0 50 0 20',
                            labelAlign: 'top',
                            name: 'emailAddress',
                            fieldLabel: i18n._("Email Address")+ " *",
                            listeners: {
                                focus: this.defaultFocusButton("registerButton")
                            }
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
                                fieldLabel: i18n._("Password")+ " *",
                                listeners: {
                                    focus: this.defaultFocusButton("registerButton")
                                }
                            }, {
                                xtype:'textfield',
                                inputType: 'password',
                                margin: '0 20 0 10',
                                labelAlign: 'top',
                                columnWidth: 0.5,
                                name: 'confirmPassword',
                                fieldLabel: i18n._("Confirm Password")+ " *",
                                listeners: {
                                    focus: this.defaultFocusButton("registerButton")
                                }
                            }]
                        }, {
                            xtype: 'container',
                            layout: 'center',
                            margin: '10 0 0 0',
                            height: 40,
                            items: {
                                xtype: 'button',
                                name: "registerButton",
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
                                    this.checkActivationCode();
                                },
                                scope: this
                            }]
                        }
                    }]
                },
                //Activation Code step
                {
                    xtype: 'container',
                    itemId: 'step4',
                    autoScroll: true,
                    items: [{
                        xtype: 'component',
                        margin: '0 120 0 150',
                        html: Ext.String.format(i18n._("If your {0} appliance came with an activation code to redem your subscription please enter it now. Otherwise simply skip this step."), rpc.companyName)
                    }, {
                        xtype:'textfield',
                        margin: '0 0 0 150',
                        width: 350,
                        name: "activationCode",
                        labelAlign: 'top',
                        fieldLabel: i18n._("Activation Code")+ " *"
                    }, {
                        xtype:'checkbox',
                        margin: '0 0 0 150',
                        name: "renew",
                        width: 350,
                        checked: this.renewDefaultChecked,
                        boxLabel: i18n._("Automatically renew my subscription"),
                        listeners: {
                            change: {
                                fn: function(elem, newValue, oldValue, eOpts) {
                                    this.down("container[name=creditContainer]").setVisible(newValue);
                                    this.down("container[name=creditNoContainer]").setVisible(!newValue);
                                },
                                scope: this
                            }
                        }
                    },{
                        // Credit card information.  If we determine the customer already has an associated
                        // credit card, we will hide these sections.
                        xtype: 'container',
                        margin: '0 120 0 150',
                        name: 'creditNoContainer',
                        hidden: this.renewDefaultChecked == true,
                        html: i18n._("If you opt out of automatic renewal, services and protection will lapse at the end of the subscription period.") +
                        '  <i>'+i18n._("It is highly recommended that you enable automatic renewal.")+'</i>'
                    },{
                        xtype: 'container',
                        name: 'creditContainer',
                        hidden: this.renewDefaultChecked == false,
                        items:[{
                            xtype:'textfield',
                            margin: '0 0 0 150',
                            width: 350,
                            name: "creditAddress1",
                            labelAlign: 'top',
                            fieldLabel: i18n._("Billing Address 1")+ " *"
                        }, {
                            xtype:'textfield',
                            margin: '0 0 0 150',
                            width: 350,
                            name: "creditAddress2",
                            labelAlign: 'top',
                            fieldLabel: i18n._("Billing Address 2")
                        }, {
                            xtype: 'container',
                            margin: '0 0 0 150',
                            width: 400,
                            layout: 'column',
                            items:[{
                                xtype:'textfield',
                                name: "creditCity",
                                labelAlign: 'top',
                                fieldLabel: i18n._("City")+ " *"
                            },{
                                xtype:'textfield',
                                name: "creditState",
                                labelAlign: 'top',
                                margin: "0 0 0 20",
                                fieldLabel: i18n._("State or Province")+ " *"
                            }]
                        }, {
                            xtype: 'container',
                            margin: '0 0 0 150',
                            width: 400,
                            layout: 'column',
                            items:[{
                                xtype:'textfield',
                                name: "creditPostal",
                                labelAlign: 'top',
                                fieldLabel: i18n._("Postal Code")+ " *"
                            },{
                                xtype:'textfield',
                                name: "creditCountry",
                                labelAlign: 'top',
                                margin: "0 0 0 20",
                                fieldLabel: i18n._("Country")+ " *"
                            }]
                        }, {
                            xtype:'textfield',
                            margin: '0 0 0 150',
                            width: 350,
                            name: "creditNumber",
                            labelAlign: 'top',
                            fieldLabel: i18n._("Credit Card Number")+ " *",
                            listeners: {
                                change: {
                                    fn: function(elem, creditNumber, oldValue, eOpts) {
                                        // Determine credit card type for those supported (AMEX, MC, DISC, VISA).
                                        // In theory we could do this silently but we would risk
                                        // future changes to credit card algorithms.
                                        // To be safe, we provide a combobox to select the type but 
                                        // aid in changing that value automatically when the number has changed
                                        // using the current card algoritm.
                                        creditNumber = creditNumber.replace(/\-/g, '');
                                        if( (creditNumber.length < 14) || !/^\d+$/.test(creditNumber)){
                                            return;
                                        }
                                        var creditType = "MC";
                                        var creditPrefix = creditNumber.substring(0,2);
                                        switch(creditPrefix){
                                            case "34":
                                            case "37":
                                                creditType = "AMEX";
                                                break;
                                            case "51":
                                            case "52":
                                            case "53":
                                            case "54":
                                            case "55":
                                                creditType = "MC";
                                                break;
                                            default:
                                                creditPrefix = creditNumber.substring(0,4);
                                                switch(creditPrefix){
                                                    case "6011":
                                                        creditType = "DISC";
                                                        break;
                                                    default:
                                                        creditType ="VISA";
                                                }
                                        }
                                        this.down("combo[name=creditType]").setValue(creditType);
                                    },
                                    scope: this,
                                    buffer: 400
                                }
                            }
                        }, {
                            xtype: 'container',
                            margin: '0 0 0 150',
                            width: 400,
                            layout: 'column',
                            items:[{
                                xtype:'combo',
                                name: "creditType",
                                labelAlign: 'top',
                                width: 130,
                                fieldLabel: i18n._("Card Type")+ " *",
                                queryMode: "local",
                                valueField: "id",
                                displayField: "value",
                                store: this.cardTypes,
                                value: "MC"
                            },{
                                xtype:'combo',
                                name: "creditMonth",
                                labelAlign: 'top',
                                width: 110,
                                margin: "0 0 0 20",
                                fieldLabel: i18n._("Expiration Month")+ " *",
                                queryMode: "local",
                                valueField: "id",
                                displayField: "value",
                                store: this.expirationMonths,
                                value: "01"
                            }, {
                                xtype:'textfield',
                                name: "creditYear",
                                labelAlign: 'top',
                                width: 100,
                                margin: "0 0 0 20",
                                maxLength: 4,
                                enforceMaxLength: true,
                                fieldLabel: i18n._("Expiration Year")+ " *"
                            }]
                        }]
                    }, {
                        xtype: 'container',
                        layout: 'center',
                        height: 70,
                        items: {
                            xtype: 'container',
                            layout: 'hbox',
                            items: [{
                                xtype: 'button',
                                text: i18n._('Redeem Activation Code'),
                                padding: '7 10 7 10',
                                width: 150,
                                handler: function() {
                                    if( this.updatePaymentMethod() == true ){
                                        this.redeemActivationCode();
                                    }
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
        this.setLoading(i18n._("Logging in..."));
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/account/login",
            scope: this,
            params: {
                email: emailAddress.getValue(),
                password: password.getValue(),
                uid: rpc.serverUID,
                majorVersion: rpc.version
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
                        this.checkAccount();
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
        this.setLoading(i18n._("Creating account..."));
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/account/create",
            scope: this,
            params: {
                email: emailAddress.getValue(), 
                password: password.getValue(),
                fname: firstName,
                lname: lastName,
                cname: companyName,
                uid: rpc.serverUID,
                majorVersion: rpc.version
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
                        this.checkAccount();
                    }
                }
            },
            failure: function(response, opts) {
                this.setLoading(false);
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("Failed to access the store to create the account"));
            }
            
        });
    },
    checkAccount: function() {
        this.setLoading(true);
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/account",
            params: {
                email: this.email,
                token: this.token,
                majorVersion: rpc.version,
                uid: rpc.serverUID,
                majorVersion: rpc.version
            },
            success: function(response, opts) {
                this.setLoading(false);
                if( response!=null) {
                    if(!response.success) {
                        Ext.MessageBox.alert(i18n._("Warning"), response.customerMessage);
                        return;
                    }
                    var paymentMethodDefined = typeof(response.paymentMethod.length) == 'undefined' ? true: false;
                    // If credit card information is associated with account, disable credit card fields
                    this.down("container[itemId=step4]").down("checkbox[name=renew]").setVisible(paymentMethodDefined == false);
                    if(paymentMethodDefined){
                        this.down("container[itemId=step4]").down("container[name=creditNoContainer]").setVisible(false);
                        this.down("container[itemId=step4]").down("container[name=creditContainer]").setVisible(false);
                    }
                    this.checkSubscriptions();
                }
                
            },
            failure: function(response, opts) {
                this.setLoading(false);
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("Failed to access the store to check the subscriptions"));
            },
            scope: this
        });
    },
    checkSubscriptions: function() {
        this.setLoading(true);
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/subscriptions",
            params: {
                email: this.email,
                token: this.token,
                majorVersion: rpc.version,
                uid: rpc.serverUID,
                majorVersion: rpc.version
            },
            success: function(response, opts) {
                this.setLoading(false);
                if( response!=null) {
                    if(!response.success) {
                        Ext.MessageBox.alert(i18n._("Warning"), response.customerMessage);
                        return;
                    }
                    var unbound = false;
                    if( response.subscriptions && response.subscriptions.length>0) {
                        for( var i = 0; i < response.subscriptions.length; i++){
                            if(response.subscriptions[i].uid == ""){
                                unbound = true;
                                break;
                            }
                        }
                    }
                    if(unbound == true){
                        this.down("container[name=cardsContainer]").setActiveItem("step3");
                    }else{                            
                        this.checkActivationCode();
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
    checkActivationCode: function() {
        this.setLoading(true);
        rpc.jsonrpc.UvmContext.isActivationCode(Ext.bind(function(result, exception) {
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
    updatePaymentMethod: function(){

        var renew = this.down("container[itemId=step4]").down("checkbox[name=renew]");
        if((renew.isVisible() == false) ||
           (renew.getValue() == false)){
            return true;
        }

        var form = this.down("container[itemId=step4]");
        var creditAddress1 = form.down("textfield[name=creditAddress1]").getValue().trim();
        var creditAddress2 = form.down("textfield[name=creditAddress2]").getValue().trim();
        var creditCity = form.down("textfield[name=creditCity]").getValue().trim();
        var creditState = form.down("textfield[name=creditState]").getValue().trim();
        var creditPostal = form.down("textfield[name=creditPostal]").getValue().trim();
        var creditCountry = form.down("textfield[name=creditCountry]").getValue().trim();
        var creditNumber = form.down("textfield[name=creditNumber]").getValue().trim();
        var creditType = form.down("textfield[name=creditType]").getValue().trim();
        var creditMonth = form.down("textfield[name=creditMonth]").getValue().trim();
        var creditYear = form.down("textfield[name=creditYear]").getValue().trim();

        var errors = [];
        if(Ext.isEmpty(creditAddress1)) {
            errors.push(Ext.String.format(i18n._("{0} is required."), i18n._("Address 1")));
        }
        if(Ext.isEmpty(creditCity)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("City")));
        }
        if(Ext.isEmpty(creditState)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("State")));
        }
        if(Ext.isEmpty(creditPostal)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Postal Code")));
        }
        if(Ext.isEmpty(creditCountry)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Country")));
        }
        if(Ext.isEmpty(creditNumber)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Credit Card Number")));
        }else{
            creditNumber = creditNumber.replace(/\-/g, '');
            if( (creditNumber.length < 14) || !/^\d+$/.test(creditNumber)){
                errors.push(Ext.String.format(i18n._("The {0} is invalid."), i18n._("Credit Card Number")));
            }
        }
        if(Ext.isEmpty(creditYear)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Expiration Year")));
        }else{
            if( (creditYear.length < 4) || !/^\d+$/.test(creditYear)){
                errors.push(Ext.String.format(i18n._("The {0} is invalid."), i18n._("Credit Year")));
            }            
        }
        if(errors.length>0) {
            Ext.MessageBox.alert(i18n._("Warning"), errors.join("<br/>"));
            return false;
        }

        this.setLoading(i18n._("Updating payment..."));
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/account/updatePaymentMethod",
            params: {
                email: this.email,
                token: this.token,
                majorVersion: rpc.version,
                ccType: creditType,
                ccNumber: creditNumber,
                ccExpMonth: creditMonth,
                ccExpYear: creditYear,
                address1: creditAddress1,
                address2: creditAddress2,
                city: creditCity,
                state: creditState,
                postCode: creditPostal,
                country: creditCountry,
                uid: rpc.serverUID,
                majorVersion: rpc.version
            },
            success: function(response, opts) {
                this.setLoading(false);
                if( response!=null) {
                    if(!response.success) {
                        Ext.MessageBox.alert(i18n._("Warning"), response.customerMessage);
                        return;
                    }
                }
                
            },
            failure: function(response, opts) {
                this.setLoading(false);
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("Failed to access the store to update the payment method"));
            },
            scope: this
        });
        return true;
    },
    redeemActivationCode: function() {
        var form = this.down("container[itemId=step4]");
        var activationCode = form.down("textfield[name=activationCode]").getValue().trim();
        var errors = [];
        if(Ext.isEmpty(activationCode)) {
            errors.push(Ext.String.format(i18n._("The {0} is required."), i18n._("Activation Code")));
        }
        if(errors.length>0) {
            Ext.MessageBox.alert(i18n._("Warning"), errors.join("<br/>"));
            return false;
        }

        this.setLoading(i18n._("Redeeming activation code..."));
        Ext.data.JsonP.request({
            url: this.storeApiUrl+"/appliance/create",
            params: {
                email: this.email,
                token: this.token,
                majorVersion: rpc.version,
                version: rpc.version,
                activationCode: activationCode,
                uid: rpc.serverUID,
                uidName: rpc.jsonrpc.UvmContext.networkManager().getNetworkSettings().hostName + "." + rpc.jsonrpc.UvmContext.networkManager().getNetworkSettings().domainName,
                majorVersion: rpc.version
            },
            success: function(response, opts) {
                this.setLoading(false);
                if( response!=null) {
                    if(!response.success) {
                        Ext.MessageBox.alert(i18n._("Warning"), response.customerMessage);
                        return;
                    }
                    this.finished = true;
                    this.down("container[name=cardsContainer]").setActiveItem("step5");
                }
            },
            failure: function(response, opts) {
                this.setLoading(false);
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("Failed to access the store to update the payment method"));
            },
            scope: this
        });
        return true;
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
