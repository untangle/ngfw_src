Ext.define('Ung.apps.reports.view.EmailTemplates', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-reports-emailtemplates',
    itemId: 'email-templates',
    title: 'Email Templates'.t(),

    controller: 'unreportsemailtemplatesgrid',

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'copy', 'delete'],

    copyAppendField: 'title',

    emptyRow: {
        javaClass: 'com.untangle.app.reports.EmailTemplate',
        title: '',
        description: '',
        interval: 86400,
        intervalWeekStart: 1,
        mobile: false,
        enabledConfigIds : {
            "javaClass": "java.util.LinkedList",
            "list": ['_recommended']
        },
        enabledAppIds : {
            "javaClass": "java.util.LinkedList",
            "list": ['_recommended']
        },
    },

    bind: '{emailTemplates}',

    listProperty: 'settings.emailTemplates.list',
    columns: [{
        header: 'Id'.t(),
        width: 50,
        dataIndex: 'templateId',
        renderer: function(value) {
            return value < 0 ? 'new'.t() : value;
        }
    }, {
        header: 'Title'.t(),
        dataIndex: 'title',
        flex: 1,
        editor: {
            xtype: 'textfield',
            emptyText: '[no title]'.t(),
            allowBlank: false,
            blankText: 'The title cannot be blank.'.t(),
            listeners: {
                change: 'editorTitleChange'
            }
        }
    }, {
        header: 'Description'.t(),
        width: 200,
        dataIndex: 'description',
        flex: 1,
        editor: {
            xtype:'textfield',
            emptyText: '[no description]'.t(),
            allowBlank: false,
            blankText: 'The description cannot be blank.'.t()
        },
    }, {
        header: 'Interval'.t(),
        width: 80,
        dataIndex: 'interval',
        rtype: 'timeInterval',
        editor: {
            xtype: 'combo',
            editable: false,
            queryMode: 'local',
            bind: {
                store: '{emailIntervals}'
            }
        }
    }, {
        header: 'Mobile'.t(),
        width: 60,
        dataIndex: 'mobile',
        rtype: 'boolean',
        editor: {
            xtype: 'checkbox',
            bind: '{record.mobile}'
        }
    }, {
        header: 'Config'.t(),
        flex: 1,
        dataIndex: 'enabledConfigIds',
        renderer: 'reportRenderer'
    }, {
        header: 'Apps'.t(),
        flex: 1,
        dataIndex: 'enabledAppIds',
        renderer: 'reportRenderer'
    }],

    editorXtype: 'ung.cmp.unemailtemplatesrecordeditor',
    editorFields: [{
        xtype: 'textfield',
        bind: '{record.title}',
        fieldLabel: 'Title'.t(),
        emptyText: '[no title]'.t(),
        width: 500,
        listeners: {
            change: 'editorTitleChange'
        }
    },{
        xtype: 'textfield',
        bind: '{record.description}',
        fieldLabel: 'Description'.t(),
        emptyText: '[no description]'.t(),
        width: 500
    },{
        xtype: 'container',
        layout: 'hbox',
        defaults: {
            anchor: '100%',
            labelWidth: 180,
            labelAlign : 'right',
        },
        items: [{
            xtype: 'combo',
            editable: false,
            fieldLabel: 'Interval'.t(),
            queryMode: 'local',
            bind: {
                value: '{record.interval}',
                store: '{emailIntervals}'
            }
        },{
            xtype: 'combo',
            editable: false,
            fieldLabel: 'Start of week'.t(),
            queryMode: 'local',
            hidden: true,
            disabled: true,
            bind: {
                value: '{record.intervalWeekStart}',
                store: '{dayOfWeekList}',
                hidden: '{record.interval != 604800}',
                disabled: '{record.interval != 604800}'
            }
        }]
    },{
        xtype: 'checkbox',
        editable: false,
        fieldLabel: 'Mobile'.t(),
        bind: '{record.mobile}'
    },{
        xtype: 'unreporttemplateselect',
        title: 'Config'.t(),
        group: 'config',
        bind: '{record.enabledConfigIds.list}'
    },{
        xtype: 'unreporttemplateselect',
        title: 'Apps'.t(),
        group: 'app',
        bind: '{record.enabledAppIds.list}'
    }]
});
