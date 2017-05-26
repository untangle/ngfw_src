Ext.define('Ung.cmp.TagPicker', {
    extend: 'Ext.form.field.Picker',
    // extend: 'Ext.form.field.Tag',
    // extend: 'Ext.form.field.ComboBox',
    alias: 'widget.tagpicker',

    twoWayBindable: ['tags', 'value'],
    publishes: ['tags', 'value'],

    fields: ['name', 'expirationTime'],

    displayField: false,
    valueField: false,
    delimiter: ", ",

    editable: false,

    tags: [],

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
            // maxHeight: 300,
            minWidth: 520,
            // height: 200,
            trackMouseOver: false,
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
                fields: ['name', 'expirationTime'],
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
                focusable: false,
                header: '<i class="fa fa-clock-o"></i> ' + 'Expiration Time'.t(),
                widget: {
                    xtype: 'tagtime',
                    bind: {
                        value: '{record.expirationTime}'
                    }
                }
            }, {
                // xtype: 'widgetcolumn',
                // header: '<i class="fa fa-trash-o"></i>',
                // resizable: false,
                // sortable: false,
                // menuDisabled: true,
                // align: 'center',
                // width: 30,
                // widget: {
                //     xtype: 'button',
                //     focusable: false,
                //     iconCls: 'fa fa-times',
                //     handler: me.removeTag,
                //     scope: me
                // }
                xtype: 'actioncolumn',
                header: '<i class="fa fa-trash-o"></i>',
                width: 30,
                resizable: false,
                sortable: false,
                menuDisabled: true,
                align: 'center',
                iconCls: 'fa fa-times',
                handler: me.removeTag,
                focusable: false
            }],
            bbar: [{
                xtype: 'button',
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

        return me.grid;
    },

    addTag: function () {
        var me = this;
        me.grid.getStore().add({
            expirationTime: -1,
            expired: false,
            javaClass: 'com.untangle.uvm.Tag',
            name: 'Tag',
            valid: true
        });
    },

    removeTag: function (view, rowIndex, colIndex, item, e, record) {
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
        if (this.grid) {
            this.grid.getStore().loadData(tags.list);
        }
        this.tags = tags.list;
        this.setValue(tags.list);
    },

    setValue: function (tags) {
        var value = [];
        Ext.Array.each(tags, function(tag) {
            value.push(tag.name);
        });
        this.setRawValue(value.join(', '));
    },

    publishValue: function () {
        var me = this;
        // console.log(Ext.Array.pluck(me.grid.getStore().getRange(), 'data'));
        if (me.rendered) {
            me.publishState('tags', {
                javaClass: 'java.util.LinkedList',
                list: Ext.Array.pluck(me.grid.getStore().getRange(), 'data')
            });
        }
    }
});
