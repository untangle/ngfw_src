Ext.define('Ung.cmp.TagPicker', {
    extend: 'Ext.form.field.Picker',
    // extend: 'Ext.form.field.Tag',
    // extend: 'Ext.form.field.ComboBox',
    alias: 'widget.tagpicker',

    baseCls: 'tagpicker',

    twoWayBindable: ['tags', 'value'],
    publishes: ['tags', 'value'],

    fields: ['name', 'expirationTime'],

    displayField: false,
    valueField: false,
    delimiter: ", ",

    editable: false,
    hideTrigger: true,

    tags: [],

    fieldSubTpl: [
        // listWrapper div is tabbable in Firefox, for some unfathomable reason
        '<div id="{cmpId}-listWrapper" data-ref="listWrapper"' + (Ext.isGecko ? ' tabindex="-1"' : ''),
            '<tpl foreach="ariaElAttributes"> {$}="{.}"</tpl>',
            ' class="' + Ext.baseCSSPrefix + 'tagfield {fieldCls} {typeCls} {typeCls}-{ui}"<tpl if="wrapperStyle"> style="{wrapperStyle}"</tpl>>',
            '<span id="{cmpId}-selectedText" data-ref="selectedText" aria-hidden="true" class="' + Ext.baseCSSPrefix + 'hidden-clip"></span>',
            '<ul id="{cmpId}-itemList" data-ref="itemList" role="presentation" class="' + Ext.baseCSSPrefix + 'tagfield-list{itemListCls}">',
                '<li id="{cmpId}-inputElCt" data-ref="inputElCt" role="presentation" class="' + Ext.baseCSSPrefix + 'tagfield-input">',
                    '<input id="{cmpId}-inputEl" data-ref="inputEl" type="{type}" ',
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
            minHeight: 150,
            minWidth: 520,
            disableSelection: true,
            enableColumnHide: false,
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Tags!</p>',
            viewConfig: {
                stripeRows: false,
                listeners: {
                    // to avoid some focusing issues
                    cellclick: function (view, cell, cellIndex, record) {
                        if (cellIndex > 0) {
                            return false;
                        }
                    }
                }
            },
            plugins: [{
                ptype: 'cellediting',
                clicksToEdit: 1
            }],
            store: {
                data: me.getTags(),
                fields: ['name', 'expirationTime']
                // sorters: ['expirationTime']
            },
            columns: [{
                header: '<i class="fa fa-tag"></i> ' + 'Name'.t(),
                menuDisabled: true,
                dataIndex: 'name',
                flex: 1,
                editor: {
                    xtype: 'textfield',
                    allowBlank: false
                }
            }, {
                xtype: 'widgetcolumn',
                width: 380,
                menuDisabled: true,
                header: '<i class="fa fa-clock-o"></i> ' + 'Expiration Time'.t(),
                widget: {
                    xtype: 'tagtime',
                    bind: {
                        value: '{record.expirationTime}'
                    }
                }
            }, {
                xtype: 'actioncolumn',
                header: '<i class="fa fa-trash-o"></i>',
                width: 30,
                resizable: false,
                sortable: false,
                menuDisabled: true,
                align: 'center',
                iconCls: 'fa fa-times',
                handler: me.removeTag,
                scope: me
            }],
            bbar: [{
                xtype: 'button',
                itemId: 'addbtn',
                text: 'Add'.t(),
                iconCls: 'fa fa-plus-circle',
                handler: me.addTag,
                scope: me
            }, '->', {
                xtype: 'button',
                text: 'Done'.t(),
                iconCls: 'fa fa-check',
                handler: me.finish,
                scope: me
            }],
        });

        // focus on tag name editing when adding new
        me.grid.getStore().on('add', function (store, records, index) {
            me.grid.getPlugins()[0].startEditByPosition({ row: index, column: 0 });
        });

        return me.grid;
    },

    addTag: function () {
        var me = this;
        me.grid.getStore().add({
            expirationTime: 0,
            expired: false,
            javaClass: 'com.untangle.uvm.Tag',
            name: '',
            valid: true
        });
    },

    removeTag: function (view, rowIndex, colIndex, item, e, record) {
        view.focus(); // important to move the focus on the grid so the picker does not collapse
        record.drop();
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

    getTags: function () {
        return this.tags;
    },
    setTags: function (tags) {
        var _tags = [];
        if (tags && Ext.isArray(tags.list)) {
            _tags = tags.list;
        }
        if (this.grid) {
            this.grid.getStore().loadData(_tags);
        }
        this.tags = _tags;
        this.setValue(_tags);
    },

    setValue: function (tags) {
        var me = this, value = [];
        Ext.Array.each(tags, function(tag) {
            // value.push(tag.name);
            value.push('<li class="tag-item"><i class="fa fa-tag"></i> ' + tag.name + '</li>');
        });
        me.itemList.select('.tag-item').destroy();
        me.itemList.select('.no-tags').destroy();
        me.inputElCt.insertHtml('beforeBegin', value.join(''));

        if (tags.length === 0) {
            me.inputElCt.insertHtml('beforeBegin', '<li class="no-tags"><em>' + '</em></li>');
        }
    },

    publishValue: function () {
        var me = this;
        if (me.rendered) {
            me.publishState('tags', {
                javaClass: 'java.util.LinkedList',
                list: Ext.Array.pluck(me.grid.getStore().getRange(), 'data')
            });
        }
    }
});
