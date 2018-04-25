Ext.define('Ung.Setup.Wizard', {
    extend: 'Ext.window.Window',
    alias: 'widget.setupwizard',

    viewModel: {
        data: {
            activeStepDesc: '',
            activeStep: null,
            intfListLength: null, // used for next button disable/enable
        }
    },

    modal: true,

    steps: [],

    resizable: false,
    draggable: false,

    width: 800,
    height: 600,
    frame: false,
    frameHeader: false,
    header: false,
    border: false,
    bodyBorder: false,
    bodyPadding: 0,
    layout: 'card',
    padding: 0,

    bodyStyle: {
        background: '#FFF',
        border: 0
    },

    defaults: {
        border: false,
        bodyBorder: false,
        bodyPadding: 0,
        padding: 20,
        cls: 'step',
        header: false
    },

    dockedItems: [{
        xtype: 'component',
        cls: 'step-title',
        padding: '20 20 0 20',
        dock: 'top',
        html: '&nbsp;',
        bind: {
            html: '{activeStepDesc}'
        }
    }, {
        xtype: 'toolbar',
        dock: 'top',
        hidden: true,
        defaults: {
            xtype: 'component'
        },
        items: [{
            bind: {
                html: '{activeStep}'
            }
        }]
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        background: '#FFF',
        layout: {
            type: 'hbox',
            align: 'stretch'
        },
        defaults: {
            scale: 'medium',
            focusable: false
        },
        items: [{
            itemId: 'prevBtn',
            iconCls: 'fa fa-chevron-circle-left fa-lg',
            hidden: true,
            handler: 'onPrev'
        }, {
            xtype: 'component',
            itemId: 'stepIndicator',
            cls: 'steps',
            width: 200,
            padding: '7 0 0 0',
            html: ''
        }, '->', {
            itemId: 'nextBtn',
            iconCls: 'fa fa-chevron-circle-right fa-lg',
            iconAlign: 'right',
            handler: 'onNext',
            hidden: true,
            disabled: true,
            bind: {
                disabled: '{activeStep === "Interfaces" && intfListLength < 2 && !forcecontinue.checked}'
            }
        }]
    }],

    items: [
        { xtype: 'ServerSettings' },
        { xtype: 'Interfaces' },
        // the rest of steps are added after network settings are fetched
    ],

    listeners: {
        afterrender: 'onAfterRender',
        syncsteps: 'onSyncSteps'
    },

    controller: {
        onAfterRender: function (view) {
            var me = this, vm = me.getViewModel(), cardIndex;
            if (!rpc.wizardSettings.wizardComplete && rpc.wizardSettings.completedStep !== null) {
                cardIndex = Ext.Array.indexOf(rpc.wizardSettings.steps, rpc.wizardSettings.completedStep);

                // if resuming from a step after Network Cards settings, need to fetch network settings
                if (cardIndex > 1) {
                    Ung.app.loading('Loading interfaces...'.t());
                    rpc.networkManager.getNetworkSettings(function (result, ex) {
                        Ung.app.loading(false);
                        if (ex) { Util.handleException('Unable to load interfaces.'.t()); return; }

                        vm.set({
                            networkSettings: result,
                            intfListLength: result.interfaces.list.length
                        });

                        // update the steps based on interfaces
                        me.onSyncSteps(cardIndex);
                    });
                } else {
                    view.setActiveItem(cardIndex + 1);
                    me.updateNav();
                }
            } else {
                me.updateNav();
            }
        },

        onPrev: function () {
            var me = this;
            me.getView().getLayout().prev();
            me.updateNav();
        },

        onNext: function () {
            var me = this, layout = me.getView().getLayout();

            layout.getActiveItem().fireEvent('save', function () {
                if (!rpc.wizardSettings.wizardComplete) {
                    rpc.wizardSettings.completedStep = layout.getActiveItem().getXType();
                    rpc.jsonrpc.UvmContext.setWizardSettings(function (result, ex) {
                        if (ex) { Util.handleException(ex); return; }
                    }, rpc.wizardSettings);
                }
                // move to next step
                layout.next();
                me.updateNav();
            });
        },

        updateNav: function () {
            var me = this, view = me.getView(), vm = me.getViewModel(),

                prevBtn = view.down('#prevBtn'),
                nextBtn = view.down('#nextBtn'),
                layout = me.getView().getLayout(),
                prevStep = layout.getPrev(),
                nextStep = layout.getNext(),

                stepInd = view.down('#stepIndicator'),
                stepIndHtml = '';

            vm.set({
                'activeStep': layout.getActiveItem().getXType(),
                'activeStepDesc': layout.getActiveItem().description
            });

            if (prevStep) {
                prevBtn.show().setText(prevStep.getTitle());
            } else {
                prevBtn.hide().setText('');
            }

            if (nextStep) {
                nextBtn.show().setText(nextStep.getTitle());
            } else {
                nextBtn.hide().setText('');
            }

            var activeIndex = Ext.Array.indexOf(view.steps, layout.getActiveItem().getXType());

            Ext.Array.each(view.steps, function (step, idx) {
                if (idx < activeIndex) {
                    stepIndHtml += '<i class="done"></i>';
                    return;
                }
                if (idx === activeIndex) {
                    stepIndHtml += '<i class="active"></i>';
                    return;
                }
                stepIndHtml += '<i></i>';
            });
            stepInd.setHtml(stepIndHtml);

            if (rpc.jsonrpc) {
                rpc.wizardSettings.steps = view.steps;
                rpc.wizardSettings.completedStep = prevStep ? prevStep.getXType() : null;
                rpc.wizardSettings.wizardComplete = nextStep ? false : true;

                rpc.jsonrpc.UvmContext.setWizardSettings(function (result, ex) {
                    if (ex) { Util.handleException(ex); return; }
                }, rpc.wizardSettings);
            }
        },

        /**
         * called after the netwark settings were fetched,
         * sets the wizard steps
         */
        onSyncSteps: function (activeItemIdx) {
            var me = this, vm = me.getViewModel(),
                wizard = me.getView(),

                interfaces = vm.get('networkSettings.interfaces.list'),
                firstWan = Ext.Array.findBy(interfaces, function (intf) {
                    return (intf.isWan && intf.configType !== 'DISABLED');
                }),
                firstNonWan = Ext.Array.findBy(interfaces, function (intf) {
                    return !intf.isWan;
                }),
                firstWireless = Ext.Array.findBy(interfaces, function (intf) {
                    return intf.isWirelessInterface;
                });

            wizard.steps = ['ServerSettings', 'Interfaces'];

            if (firstWan) {
                wizard.steps.push('Internet');
            }
            if (firstNonWan) {
                wizard.steps.push('InternalNetwork');
            }
            if (firstWireless) {
                wizard.steps.push('Wireless');
            }

            wizard.steps.push('AutoUpgrades');
            wizard.steps.push('Complete');

            // remove any nonwanted step if exists
            Ext.Array.each(wizard.items.items, function (card) {
                if (!Ext.Array.contains(wizard.steps, card.getXType())) {
                    wizard.remove(card);
                }
            });


            Ext.Array.each(wizard.steps, function (step) {
                if (!wizard.down(step)) {
                    wizard.add( { xtype: step } );
                }
            });

            if (Ext.isNumber(activeItemIdx)) {
                wizard.setActiveItem(activeItemIdx + 1);
            }

            me.updateNav();
        }
    }

});
