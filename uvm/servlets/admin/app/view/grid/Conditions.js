Ext.define('Ung.view.grid.Conditions', {
    extend: 'Ext.grid.Panel',
    xtype: 'ung.gridconditions',
    //viewType: 'tableview',

    layout: 'column',

    requires: [
        'Ung.view.grid.ConditionsController',
        'Ung.store.Conditions',
        'Ung.store.Countries',
        'Ung.view.grid.ConditionEditor'
    ],

    //height: 200,

    controller: 'ung.gridconditions',
    viewModel: true,

    config: {
        conditions: null
    },


    bind: {
        store: '{store}'
    },


    /*
    plugins: [{
        ptype: 'rowediting',
        clicksToEdit: 1,
        clicksToMoveEditor: 1
    }],
    */

    resizable: false,
    //border: false,
    bodyBorder: false,
    //title: 'Conditions'.t(),
    columns: [{
        text: 'Type'.t(),
        dataIndex: 'conditionType',
        width: 250,
        menuDisabled: true,
        hideable: false,
        sortable: false,
        renderer: 'typeRenderer',
        editor: {
            xtype: 'combo',
            store: 'conditions',
            valueField: 'name',
            displayField: 'displayName',
            editable: false,
            listeners: {
                change: 'onChange'
            }
        }
    }, {
        //text: 'Value'.t(),
        width: 80,
        menuDisabled: true,
        hideable: false,
        sortable: false,
        resizable: false,
        dataIndex: 'invert',
        renderer: function (value) {
            return value ? 'is NOT'.t() : 'is'.t();
        }
    }, {
        text: 'Value'.t(),
        flex: 1,
        menuDisabled: true,
        hideable: false,
        sortable: false,
        dataIndex: 'value',
        renderer: 'valueRenderer'

    }, {
        xtype: 'ung.actioncolumn',
        text: 'Delete'.t(),
        align: 'center',
        width: 50,
        sortable: false,
        hideable: false,
        resizable: false,
        menuDisabled: true,
        materialIcon: 'delete'
    }],
    tbar: [{
        text: Ung.Util.iconTitle('Add Condition', 'add-16'),
        handler: 'onAddCondition'
    }],
    listeners: {
        select: 'onConditionSelect',
        beforerender: 'onBeforeRender'
    }

    /*
    initComponent: function () {
        var str = Ext.create('Ung.store.Conditions');
        this.columns.push({
            header: 'Type'.t(),
            width: 200,
            dataIndex: 'conditionType',
            editor: {
                xtype: 'combo',
                store: Ext.create('Ung.store.Conditions'),
                displayField: 'displayName',
                valueField: 'name'
            },
            renderer: function(value) {
                return str.findRecord('name', value).get('displayName');
            }
        });

        this.columns.push({
            header: 'Value'.t(),
            flex: 1,
            dataIndex: 'conditionType',
            editor: {
                xtype: 'textfield'
            },
            renderer: function(value) {
                return str.findRecord('name', value).get('displayName');
            }
        });
        this.callParent(arguments);
    }
    */

});