Ext.define('Ung.Setup.Main', {
    extend: 'Ext.container.Viewport',
    // controller: 'main',

    viewModel: {
        data: {
            resuming: false
        }
    },
    layout: 'center',
    items: [{
        xtype: 'container',
        baseCls: 'intro',
        padding: '0 0 300 0',
        layout: {
            type: 'vbox',
            align: 'center'
        },
        items: [{
            xtype: 'component',
            margin: '0 0 20 0',
            style: { textAlign: 'center' },
            html: '<img src="images/BrandingLogo.png" height=96/><h1>' + Ext.String.format('Thanks for choosing {0}!'.t(), rpc.oemName) + '</h1>' +
                '<p>' + Ext.String.format('A wizard will guide you through the initial setup and configuration of the {0} Server.'.t(), rpc.oemName) + '</p>'
        }, {
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
        }]
    }],
    listeners: {
        afterrender: 'onAfterRender'
    },
    controller: {

        onAfterRender: function () {
            var me = this, vm = me.getViewModel();

            // fadein
            Ext.defer(function () {
                me.getView().down('container').addCls('fadein');
            }, 100);

            // check if resuming
            if (!rpc.wizardSettings.wizardComplete && rpc.wizardSettings.completedStep != null) {
                var steps = rpc.wizardSettings.steps;

                vm.set('resuming', true);
                console.log('here');
                // vm.set({
                //     resuming: true,
                //     completedStep: completedStepCmp.getTitle(),
                //     nextStep: nextStepCmp.getTitle(),
                //     nextStepIndex: nextStepIndex
                // });

                // Ext.defer(function () {
                //     Ung.app.getMainView().lookup('nextBtn').setText('<strong>' + nextStepCmp.getTitle() + '</strong>&nbsp;&nbsp;<i class="fa fa-arrow-circle-right fa-lg"></i>');
                // }, 100);
            }
        },

        resetWizard: function() {
            var me = this, vm = me.getViewModel();

            vm.set('resuming', false);

            rpc.wizardSettings.completedStep = null;
            rpc.wizardSettings.steps = [];
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
            var wizard = me.getView().add({
                xtype: 'setupwizard',
                // activeItem: rpc.wizardSettings.completedStep || 1
                // steps: rpc.wizardSettings.steps
            });
            wizard.show();
        }
    }

});
