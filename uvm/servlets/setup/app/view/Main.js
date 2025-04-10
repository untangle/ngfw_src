Ext.define('Ung.Setup.Main', {
    extend: 'Ext.container.Viewport',

    viewModel: {
        data: {
            resuming: false,
            remoteReachable: null
        }
    },
    layout: 'center',
    padding: 20,
    items: [{
        xtype: 'container',
        itemId: 'intro',
        baseCls: 'intro',
        padding: '0 0 300 0',
        layout: {
            type: 'vbox',
            align: 'center'
        },
        items: [{
            xtype: 'component',
            style: { textAlign: 'center' },
            html: '<img src="images/BrandingLogo.png?' + (new Date()).getTime() + '" height=48/><h1>' + Ext.String.format('Thanks for choosing {0}!'.t(), rpc.oemShortName) + '</h1>'
        }]
    }],
    listeners: {
        afterrender: 'onAfterRender'
    },
    controller: {

        onAfterRender: function () {
            var me = this,
                view = me.getView(),
                vm = me.getViewModel(),
                items = [];

            // Configure main based on remote.
            if(!rpc.remote){
                // Local Setup Wizard configuration
                items.push({
                    xtype: 'component',
                    margin: '0 0 20 0',
                    style: { textAlign: 'center' },
                    html: Ext.String.format('A wizard will guide you through the initial setup and configuration of the {0}.'.t(), rpc.oemProductName)
                });
                items.push({
                    xtype: 'container',
                    layout: {
                        type: 'hbox',
                        align: 'center'
                    },
                    defaults: {
                        xtype: 'button',
                        scale: 'medium',
                        focusable: false,
                        margin: 5
                    },
                    items: [{
                        iconCls: 'fa fa-play fa-lg',
                        text: 'Resume Setup Wizard'.t(),
                        hidden: true,
                        bind: {
                            hidden: '{!resuming}'
                        },
                        handler: 'resumeWizard'
                    }, {
                        bind: {
                            iconCls: 'fa {!resuming ? "fa-play" : "fa-refresh" } fa-lg',
                            text: '{!resuming ? "Run Setup Wizard" : "Restart"}'.t(),
                        },
                        handler: 'resetWizard'
                    }]
                });
            }else{
                items.push({
                    xtype: 'component',
                    margin: '0 0 20 0',
                    html: '<i class="fa fa-spinner fa-spin fa-lg fa-fw"></i>' + 'Checking Internet connectivity'.t(),
                    hidden: false,
                    bind: {
                        hidden: '{remoteReachable != null}'
                    }
                },{
                    xtype: 'component',
                    margin: '0 0 20 0',
                    style: { textAlign: 'center' },
                    html: 'To continue, you must log in using your ETM Dashboard account. If you do not have one, you can create a free account.'.t(),
                    hidden: true,
                    bind: {
                        hidden: '{remoteReachable == false || remoteReachable == null}'
                    }
                },{
                    xtype: 'container',
                    hidden: true,
                    bind: {
                        hidden: '{remoteReachable == false || remoteReachable == null}'
                    },
                    items: [{
                        xtype: 'button',
                        width: 332,
                        margin: '0 0 10 0',
                        text: '<div style="color:white;">' + 'Log In'.t() + '</div>',
                        baseCls: 'command-center-login-button',
                        handler: function(){
                            window.location = rpc.remoteUrl + "appliances/add/" + rpc.serverUID;
                        }
                    },{
                        xtype: 'button',
                        width: 332,
                        text: '<div style="color:white;">' + 'Create Account'.t() + '</div>',
                        baseCls: 'command-center-create-button',
                        handler: function(){
                            window.location = rpc.remoteUrl + "login/create-account/add-appliance/" + rpc.serverUID;
                        }
                    }]
                },{
                    xtype: 'component',
                    margin: '0 0 20 0',
                    style: { textAlign: 'center' },
                    html: '<p>' + 'To continue, you must connect to ETM Dashboard which is currently unreachable from this device.'.t() + '<br/>' +
                            '<p>' + 'You must configure Internet connectivity to ETM Dashboard to continue.'.t() + '<br/>',
                    hidden: true,
                    bind: {
                        hidden: '{remoteReachable == true || remoteReachable == null}'
                    }
                },{
                    xtype: 'container',
                    layout: {
                        type: 'hbox',
                        align: 'center'
                    },
                    defaults: {
                        xtype: 'button',
                        scale: 'medium',
                        focusable: false,
                        margin: 5
                    },
                    hidden: true,
                    bind: {
                        hidden: '{remoteReachable == true || remoteReachable == null}'
                    },
                    items: [{
                        iconCls: 'fa fa-play fa-lg',
                        text: 'Configure Internet'.t(),
                        handler: 'resetWizard'
                    }]
                });
            }
            view.down('[itemId=intro]').add(items);

            // fadein
            Ext.defer(function () {
                me.getView().down('container').addCls('fadein');
            }, 100);

            Ext.defer(function () {
                var remoteReachable = rpc.setup.getRemoteReachable();
                vm.set("remoteReachable", remoteReachable);
            }, 500);

            // check if resuming
            if (!rpc.wizardSettings.wizardComplete && rpc.wizardSettings.completedStep != null) {
                vm.set('resuming', true);
            }
        },

        resetWizard: function() {
            var me = this;
            if(rpc.remote && !rpc.remoteReachable){
                if(Util.setRpcJsonrpc("admin") == true){
                    me.resetWizardContinue();
                }else{
                    Util.authenticate("passwd", function (isNonDefaultPassword) {
                        if (isNonDefaultPassword) {
                            var msg = Ext.create('Ext.window.MessageBox');
                            msg.textField.inputType = 'password';
                            msg.prompt(
                                'Authentication required'.t(),
                                'Please enter admin password'.t(), function(btn, p) {
                                    if (btn === 'ok') {
                                        Util.authenticate(p, function (flag) {
                                            me.resetWizardContinue();
                                        });
                                    }
                                }
                            );
                        } else {
                            me.resetWizardContinue();
                        }
                    });
                }
            }else{
                me.resetWizardContinue();
            }
        },

        resetWizardContinue: function(){
            var me = this, vm = me.getViewModel();

            vm.set('resuming', false);

            rpc.wizardSettings.completedStep = null;
            rpc.wizardSettings.wizardComplete = false;
            me.openSetup();

        },

        resumeWizard: function () {
            var me = this, msg = Ext.create('Ext.window.MessageBox');
            msg.textField.inputType = 'password';

            msg.prompt(
                'Authentication required'.t(),
                'Please enter admin password'.t(), function(btn, p) {
                    if (btn === 'ok') {
                        Util.authenticate(p, function () {
                            me.openSetup();
                        });
                    }
                }
            );
        },

        openSetup: function () {
            var me = this;
            me.getView().removeAll();
            me.getView().setStyle({
                background: '#F5F5F5'
            });
            me.getView().add({
                xtype: 'container',
                width: '100%',
                layout: {
                    type: 'hbox',
                    align: 'stretch'
                },
                items: [{
                    xtype: 'component',
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width >= 840': { flex: 1 },
                        'width < 840': { flex: 0 }
                    }
                }, {
                    xtype: 'setupwizard',
                    height: 600,
                    flex: 1,
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width >= 840': { width: 800, flex: 0 },
                        'width < 840': { flex: 1 }
                    },
                }, {
                    xtype: 'component',
                    plugins: 'responsive',
                    responsiveConfig: {
                        'width >= 840': { flex: 1 },
                        'width < 840': { flex: 0 }
                    }
                }]

            });
        }
    }

});
