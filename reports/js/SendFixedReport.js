Ext.define('Ung.apps.reports.view.SendFixedReport', {
    extend: 'Ext.window.Window',
    alias: 'widget.app-reports-sendfixedreport',
    renderTo: Ext.getBody(),
    constrain: true,

    scrollable: true,

    onEsc: Ext.emptyFn,
    closable: false,
    modal: true,

    controller: 'app-reports-sendfixedreport',

    viewModel:{
        data: {
            minDate: null,
            maxDate: null
        }
    },
    
    defaults: {
        bodyStyle: {
            background: 'transparent'
        },
        border: false,
    },

    items: [{
        xtype: 'panel',
        bodyStyle: {
            background: 'transparent'
        },
        border: false,
        items:[{
            xtype: 'container',
            layout: { 
                type: 'hbox', 
                align: 'stretch', 
                pack: 'end' 
            },
            border: false,
            items:[{
                xtype: 'container',
                border: false,
                items: [{               
                    xtype: 'toolbar',
                    docked: 'top',
                    height: 24,
                    style: { background: 'transparent' },
                    border: false,
                    items: [{
                        xtype: 'component',
                        html: '<strong>' + 'Since'.t() + '</strong>'
                    }]
                },{
                    xtype: 'datepicker',
                    itemId: 'startDate',
                    showToday: false,
                    listeners: {
                        select: 'datepickerselect',
                    },
                    bind:{
                        minDate: '{minDate}',
                        maxDate: '{maxDate}',
                        // disabledDates: '{startDisabledDates}',
                        // value: '{startDate}'
                    }
                }]
            },{
                xtype: 'container',
                border: false,
                disabled: true,
                hidden: true,
                bind:{
                    disabled: '{disableStopDate}',
                    hidden: '{disableStopDate}',
                },
                items: [{               
                    xtype: 'toolbar',
                    docked: 'top',
                    height: 24,
                    style: { background: 'transparent' },
                    border: false,
                    items: [{
                        xtype: 'component',
                        html: '<strong>' + 'Until'.t() + '</strong>'
                    }]
                },{     
                    xtype: 'datepicker',
                    itemId: 'stopDate',
                    showToday: false,
                    bind:{
                        minDate: '{minDate}',
                        maxDate: '{maxDate}'                    }
                }]
            }]
        }]
    }],

    buttons: [{
        text: 'Cancel'.t(),
        iconCls: 'fa fa-ban',
        handler: 'closeWindow'
    },{
        text: 'Send'.t(),
        iconCls: 'fa fa-envelope',
        handler: 'send'
    }],

    listeners: {
        afterrender: function(el){
            var vm = el.getViewModel();
            var record = el.record;

            vm.set( 'title', record.get('title') );
            vm.set( 'interval', Ung.apps.reports.cmp.EmailTemplatesGridController.intervalRender( record.get('interval'), null, record ) );
        }
    }

});
