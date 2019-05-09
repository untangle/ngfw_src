Ext.define('Ung.cmp.GridFilter', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.ungridfilter',

    viewModel: {
        data: {
            filterSearchDisabled: false,
            searchValue: ''
        }
    },

    controller: 'ungridfilter',

    layout: 'hbox',
    border: 0,

    items: [{
        xtype: 'displayfield',
        labelCls: 'fa fa-search',
        labelWidth: 'auto',
        padding: '6 -10 -6 0'
    },{
        xtype: 'displayfield',
        cls: 'x-btn-inner-default-toolbar-small',
        name: 'filterLabel',
        value: 'Filter'.t(),
        labelWidth: 'auto',
        padding: '2 0 0 5'
    },{
        xtype: 'textfield',
        name: 'filterSearch',
        _neverDirty: true,
        padding: '2 0 0 0',
        emptyText: 'Search ...'.t(),
        labelWidth: 'auto',
        enableKeyEvents: true,
        triggers: {
            clear: {
                cls: 'x-form-clear-trigger',
                hidden: true,
                handler: function (field) {
                    field.setValue('');
                }
            }
        },
        disabled: true,
        hidden: true,
        bind:{
            'value': '{searchValue}',
            'disabled': '{filterSearchDisabled}',
            'hidden': '{filterSearchDisabled}'
        },
        listeners: {
            change: 'changeFilterSearch',
            buffer: 500
        }
    }]
});