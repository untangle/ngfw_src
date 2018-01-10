Ext.define('Ung.reports.cmp.ColorsPicker', {
    extend: 'Ext.form.field.Picker',
    alias: 'widget.colorspicker',

    baseCls: 'colorpicker',

    twoWayBindable: ['value'],
    publishes: ['value'],

    fields: ['color'],

    displayField: false,
    valueField: false,
    // matchFieldWidth: false,

    editable: false,
    hideTrigger: true,

    colorsStore: [],

    colorPalette: [
        // red
        'B71C1C', 'C62828', 'D32F2F', 'E53935', 'F44336', 'EF5350', 'E57373', 'EF9A9A',
        // pink
        '880E4F', 'AD1547', 'C2185B', 'D81B60', 'E91E63', 'EC407A', 'F06292', 'F48FB1',
        // purple
        '4A148C', '6A1B9A', '7B1FA2', '8E24AA', '9C27B0', 'AB47BC', 'BA68C8', 'CE93D8',
        // blue
        '0D47A1', '1565C0', '1976D2', '1E88E5', '2196F3', '42A5F5', '64B5F6', '90CAF9',
        // teal`
        '004D40', '00695C', '00796B', '00897B', '009688', '26A69A', '4DB6AC', '80CBC4',
        // green
        '1B5E20', '2E7D32', '388E3C', '43A047', '4CAF50', '66BB6A', '81C784', 'A5D6A7',
        // limE
        '827717', '9E9D24', 'AFB42B', 'C0CA33', 'CDDC39', 'D4E157', 'DCE775', 'E6EE9C',
        // yellow
        'F57F17', 'F9A825', 'FBC02D', 'FDD835', 'FFEB3B', 'FFEE58', 'FFF176', 'FFF59D',
        // orange
        'E65100', 'EF6C00', 'F57C00', 'FB8C00', 'FF9800', 'FFA726', 'FFB74D', 'FFCC80',
        // brown
        '3E2723', '4E342E', '5D4037', '6D4C41', '795548', '8D6E63', 'A1887F', 'BCAAA4',
        // grey
        '212121', '424242', '616161', '757575', '9E9E9E', 'BDBDBD', 'E0E0E0', 'EEEEEE',
    ],

    fieldSubTpl: [
        // listWrapper div is tabbable in Firefox, for some unfathomable reason
        '<div id="{cmpId}-listWrapper" data-ref="listWrapper"' + (Ext.isGecko ? ' tabindex="-1"' : ''),
        '<tpl foreach="ariaElAttributes"> {$}="{.}"</tpl>',
        ' class="' + Ext.baseCSSPrefix + 'tagfield {fieldCls} {typeCls} {typeCls}-{ui}"<tpl if="wrapperStyle"> style="{wrapperStyle}"</tpl>>',
        '<span id="{cmpId}-selectedText" data-ref="selectedText" aria-hidden="true" class="' + Ext.baseCSSPrefix + 'hidden-clip"></span>',
        '<ul id="{cmpId}-itemList" data-ref="itemList" role="presentation" class="' + Ext.baseCSSPrefix + 'tagfield-list{itemListCls}">',
        '<li id="{cmpId}-inputElCt" data-ref="inputElCt" role="presentation" class="' + Ext.baseCSSPrefix + 'tagfield-input">',
        '<input id="{cmpId}-inputEl" data-ref="inputEl" type="{type}" ',
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
            // width: this.bodyEl.getWidth(),
            scrollable: true,
            floating: true,
            minHeight: 200,
            width: 150,
            disableSelection: true,
            enableColumnHide: false,
            hideHeaders: true,
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Colors!</p>',
            viewConfig: {
                stripeRows: false,
                trackOver: false
            },
            // plugins: [{
            //     ptype: 'cellediting',
            //     clicksToEdit: 1
            // }],
            store: {
                data: me.colorsStore,
                fields: ['color'],
                listeners: {
                    datachanged: function (store) {
                        var colors = [];
                        store.each(function (rec) {
                            colors.push(rec.get('color'));
                        });
                        me.setValue(colors);
                    },
                    update: function (store) {
                        var colors = [];
                        store.each(function (rec) {
                            colors.push(rec.get('color'));
                        });
                        me.setValue(colors);
                    }
                }
            },
            columns: [{
                xtype: 'widgetcolumn',
                width: 120,
                widget: {
                    xtype: 'button',
                    bind: {
                        text: '<i class="fa fa-square" style="color: {record.color}"></i> {record.color}',
                    },
                    style: {
                        textAlign: 'left'
                    },
                    menu: {
                        xtype: 'colormenu',
                        colors: me.colorPalette,
                        minHeight: 200,
                        listeners: {
                            select: function (menu, color) {
                                menu.up('button').getViewModel().set('record.color', '#' + color);
                            }
                        }
                    }
                }
            }, {
                xtype: 'actioncolumn',
                width: 30,
                resizable: false,
                sortable: false,
                menuDisabled: true,
                align: 'center',
                iconCls: 'fa fa-times',
                handler: me.removeColor,
                scope: me
            }, {
                flex: 1
            }],
            bbar: [{
                xtype: 'button',
                itemId: 'addbtn',
                text: 'Add'.t(),
                iconCls: 'fa fa-plus-circle',
                handler: me.addColor,
                scope: me
            }, '->', {
                xtype: 'button',
                text: 'Remove All (Use Default)'.t(),
                handler: me.removeAll,
                scope: me
            }, {
                xtype: 'button',
                text: 'Done'.t(),
                iconCls: 'fa fa-check',
                handler: me.finish,
                scope: me
            }],
            listeners: {
                afterrender: function (grid) {
                    grid.getStore().loadData(Ext.Array.map(me.value, function (color) { return { color: color }; }));
                },
                show: function (grid) {
                    grid.getStore().loadData(Ext.Array.map(me.value, function (color) { return { color: color }; }));
                }
            }
        });

        return me.grid;
    },

    addColor: function (btn) {
        this.grid.getStore().add({
            color: '#' + this.colorPalette[Ext.Number.randomInt(0, this.colorPalette.length - 1)]
        });
        // limit amount of colors to max 8
        if (this.grid.getStore().count() >= 8 ) {
            btn.setDisabled(true);
        }
    },

    removeColor: function (view, rowIndex, colIndex, item, e, record) {
        view.focus(); // important to move the focus on the grid so the picker does not collapse
        record.drop();
        if (this.grid.getStore().count() < 8 ) {
            this.grid.down('#addbtn').setDisabled(false);
        }
    },

    removeAll: function () {
        this.grid.getStore().loadData([]);
        this.grid.down('#addbtn').setDisabled(false);
        this.finish();
    },

    finish: function () {
        this.collapse();
    },

    listeners: {
        collapse: function () {
            this.publishValue();
            this.grid.getStore().commitChanges();
        }
    },

    getValue: function () {
        return this.value;
    },

    setValue: function (value) {
        var me = this, render = [];
        me.value = value || [];

        // if (me.grid) {
        //     me.grid.getStore().loadData(Ext.Array.map(me.value, function (color) { return { color: color }; }));
        // }
        me.colorsStore = Ext.Array.map(me.value, function (color) { return { color: color }; });

        if (me.value.length === 0) {
            render.push('<li class="no-colors" style="color: #999;">Using the default colors!</li>');
        }

        Ext.Array.each(me.value, function(color) {
            render.push('<li class="color-item" style="background-color: ' + color + '"></li>');
        });

        me.itemList.select('.color-item').destroy();
        me.itemList.select('.no-colors').destroy();
        me.inputElCt.insertHtml('beforeBegin', render.join(''));

        if (render.length === 0) {
            me.inputElCt.insertHtml('beforeBegin', '<li class="no-colors"><em>' + '</em></li>');
        }
    }
});
