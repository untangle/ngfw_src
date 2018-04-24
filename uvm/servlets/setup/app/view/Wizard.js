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
            cls: 'steps',
            width: 200,
            // padding: 12,
            html: '<i></i><i></i><i></i><i></i>',
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
        { xtype: 'Internet' },
        { xtype: 'InternalNetwork' },
        { xtype: 'Wireless' },
        { xtype: 'AutoUpgrades' },
        { xtype: 'Complete' }
    ],

    listeners: {
        afterrender: 'onAfterRender'
    },

    controller: {
        onAfterRender: function () {
            this.updateNav();
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
                nextStep = layout.getNext();

            vm.set({
                'activeStep': layout.getActiveItem().getXType(),
                'activeStepDesc': layout.getActiveItem().description
            });

            // console.log(vm.get('activeStep'));

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
        }
    }

});
