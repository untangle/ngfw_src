/**
 * Used in Policy Overview widget
 */
Ext.define('Ung.cmp.AppsPicker', {
    extend: 'Ext.form.field.Picker',
    alias: 'widget.appspicker',

    baseCls: 'tagpicker',

    twoWayBindable: ['apps', 'value'],

    displayField: false,
    valueField: false,
    delimiter: ', ',

    editable: false,
    hideTrigger: true,

    apps: [],

    fieldSubTpl: [
        // listWrapper div is tabbable in Firefox, for some unfathomable reason
        '<div id="{cmpId}-listWrapper" data-ref="listWrapper"' + (Ext.isGecko ? ' tabindex="-1"' : ''),
            '<tpl foreach="ariaElAttributes"> {$}="{.}"</tpl>',
            ' class="' + Ext.baseCSSPrefix + 'tagfield {fieldCls} {typeCls} {typeCls}-{ui}"<tpl if="wrapperStyle"> style="{wrapperStyle}"</tpl>>',
            '<span id="{cmpId}-selectedText" data-ref="selectedText" aria-hidden="true" class="' + Ext.baseCSSPrefix + 'hidden-clip"></span>',
            '<ul id="{cmpId}-itemList" data-ref="itemList" role="presentation" class="' + Ext.baseCSSPrefix + 'tagfield-list{itemListCls}">',
                '<li id="{cmpId}-inputElCt" data-ref="inputElCt" role="presentation" class="' + Ext.baseCSSPrefix + 'tagfield-input">',
                    '<input id="{cmpId}-inputEl" data-ref="inputEl" type="{type}" style="cursor: pointer;"',
                    '<tpl if="name">name="{name}" </tpl>',
                    '<tpl if="value"> value="{[Ext.util.Format.htmlEncode(values.value)]}"</tpl>',
                    '<tpl if="size">size="{size}" </tpl>',
                    '<tpl if="tabIdx != null">tabindex="{tabIdx}" </tpl>',
                    '<tpl if="disabled"> disabled="disabled"</tpl>',
                    '<tpl foreach="inputElAriaAttributes"> {$}="{.}"</tpl>',
                    'class="' + Ext.baseCSSPrefix + 'tagfield-input-field {inputElCls} {emptyCls}" autocomplete="off">',
                '</li>',
            '</ul>',
            '<ul id="{cmpId}-ariaList" data-ref="ariaList" role="listbox"',
                '<tpl if="ariaSelectedListLabel"> aria-label="{ariaSelectedListLabel}"</tpl>',
                '<tpl if="multiSelect"> aria-multiselectable="true"</tpl>',
                ' class="' + Ext.baseCSSPrefix + 'tagfield-arialist">',
            '</ul>',
          '</div>',
        {
            disableFormats: true
        }
    ],

    childEls: [
        'listWrapper', 'itemList', 'inputEl', 'inputElCt', 'selectedText', 'ariaList'
    ],

    initComponent: function() {
        this.callParent(arguments);
    },

    createPicker: function() {
        var me = this;
        me.grid = Ext.create('Ext.grid.Panel', {
            renderTo: Ext.getBody(),
            width: this.bodyEl.getWidth(),
            scrollable: true,
            floating: true,
            // maxHeight: 150,
            minWidth: 300,
            disableSelection: true,
            enableColumnHide: false,
            header: false,
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Apps!</p>',
            viewConfig: {
                stripeRows: false,
                // trackOver: false
            },
            store: {
                data: me.getApps(),
                sorters: ['inherited']
                // fields: ['name']
            },
            columns: [{
                dataIndex: 'state',
                align: 'right',
                width: 24,
                renderer: function (state) {
                    return '<i class="fa fa-circle ' + (state.get('on') ? 'fa-green' : 'fa-gray') + '"></i>';
                }
            }, {
                dataIndex: 'displayName',
                flex: 1,
                renderer: function (val, meta, record) {
                    var str = '<img src="/icons/apps/' + record.get('name') + '.svg" style="width: 12px; height: 12px; vertical-align: middle;"/> ';
                    if (!record.get('parentPolicy')) {
                        str += '<strong>' + val + '</strong>';
                    } else {
                        str += val + ' <span style="color: #999;">(<em>' + record.get('parentPolicy') + '</em>)</span>';
                    }
                    return str;
                }
            }, {
                xtype: 'actioncolumn',
                iconCls: 'fa fa-external-link-square',
                align: 'center',
                tooltip: 'Go to App'.t(),
                width: 30,
                handler: function (view, rowIndex, colIndex, item, e, record) {
                    Ung.app.redirectTo('#apps/' + record.get('policyId') + '/' + record.get('name'));
                }
            }]
        });

        return me.grid;
    },

    getApps: function () {
        return this.apps;
    },

    setApps: function (apps) {
        this.apps = apps;
        this.setValue(apps);
    },

    setValue: function (apps) {
        var me = this;
        if (!apps) { return; }
        var total = apps.length, running = 0, inherited = 0;
        Ext.Array.each(apps, function (app) {
            if (app.state.get('on')) {
                running++;
            }
            if (app.inherited) {
                inherited++;
            }
        });

        var str = '<li class="_apps" style="font-size: 11px;"><strong>' + total + '</strong>' +
                  '<span style="color: #999"> &nbsp; ' + running + '</span> <i class="fa fa-circle fa-green" style="font-size: 10px;"></i>' +
                  '<span style="color: #999"> &nbsp; ' + inherited + '</span> <i class="fa fa-level-up fa-gray" style="font-size: 10px;"></i>' +
                  '&nbsp;&nbsp;&nbsp;<i class="fa fa-angle-down fa-lg fa-gray"></i></li>';
        me.itemList.select('._apps').destroy();
        me.inputElCt.insertHtml('beforeBegin', str);
    }
});
