Ext.define('Ung.apps.webfilter.view.SiteLookup', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-web-filter-sitelookup',
    itemId: 'site_lookup',
    title: 'Site Lookup'.t(),
    viewModel: true,
    bodyPadding: 10,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'Lookup the category for websites.'.t()
    }],

    items: [{
        xtype: 'fieldset',
        layout: 'column',
        border: false,
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Site URL'.t(),
            fieldIndex: 'siteLookupInput',
            bind: '{siteLookupInput}',
            width: 400
        }, {
            xtype: 'button',
            text: 'Search'.t(),
            iconCls: 'fa fa-search',
            margin: '0 0 0 10',
            handler: 'handleSiteLookup'
        }],
    }, {
        xtype: 'displayfield',
        labelWidth: 160,
        fieldLabel: 'Last Search URL'.t(),
        fieldIndex: 'siteLookupAddress',
        bind: '{siteLookupAddress}'
    }, {
        xtype: 'displayfield',
        labelWidth: 160,
        fieldLabel: 'Last Search Category'.t(),
        fieldIndex: 'siteLookupCategory',
        bind: '{siteLookupCategory}'
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Suggest a different category'.t(),
        fieldIndex: 'siteLookupCheckbox',
        labelWidth: 200,
        bind: {
            value: '{siteLookupCheckbox}',
            disabled: '{siteLookupAddress.length === 0}'
        }
    }, {
        xtype: 'fieldset',
        border: false,
        bind: {
            hidden: '{!siteLookupCheckbox}'
        },
        items: [{
            xtype: 'component',
            margin: '10 0 0 20',
            html: 'NOTE: This is only a suggestion and may not be accepted. If accepted it may take a few days to become active.'.t()
        }, {
            xtype: 'combobox',
            margin: '10 0 0 20',
            width: 400,
            fieldIndex: 'siteLookupSuggest',
            displayField: 'name',
            valueField: 'string',
            editable: false,
            bind: {
                store: '{categories}',
                value: '{siteLookupSuggest}'
            }
        }, {
            xtype: 'button',
            text: 'Suggest'.t(),
            margin: '10 0 0 20',
            iconCls: 'fa fa-share-square',
            handler: 'handleCategorySuggest'
        }]
    }]

});
