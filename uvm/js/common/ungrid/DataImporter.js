Ext.define('Ung.cmp.DataImporter', {
    extend: 'Ext.window.Window',
    width: 500,
    height: 200,

    alias: 'widget.dataimporter',

    title: 'Import Settings'.t(),
    // controller: 'recordeditor',

    viewModel: {
        data: {
            importMode: 'replace'
        }
    },
    actions: {
        import: {
            text: 'Import'.t(),
            formBind: true,
            iconCls: 'fa fa-check',
            handler: function (btn) {
                console.log(btn.up('form'));
                btn.up('form').submit({
                    waitMsg: 'Please wait while the settings are uploaded...'.t(),
                    success: function () {
                        console.log('success');
                    },
                    failure: function () {
                        console.log('failure');
                    }
                });
            }
            // handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-check',
            // handler: 'onCancel'
        }
    },

    autoShow: true,
    modal: true,
    constrain: true,
    layout: 'fit',
    bodyStyle: {
        background: '#FFF'
    },

    items: [{
        xtype: 'form',
        bodyPadding: 10,
        bbar: ['->', '@cancel', '@import'],
        name: 'importSettingsForm',
        // url: 'gridSettings',
        url: 'http://localhost:8002/admin/gridSettings',
        border: false,
        items: [{
            xtype: 'radiogroup',
            // fieldLabel: 'Two Columns',
            // Arrange radio buttons into two columns, distributed vertically
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{importMode}',
            items: [
                { boxLabel: 'Replace current settings'.t(), name: 'importMode', inputValue: 'replace' },
                { boxLabel: 'Prepend to current settings'.t(), name: 'importMode', inputValue: 'prepend'},
                { boxLabel: 'Append to current settings'.t(), name: 'importMode', inputValue: 'append' }
            ]
        }, {
            xtype: 'filefield',
            width: '100%',
            fieldLabel: 'with settings from'.t(),
            labelAlign: 'top',
            // name: 'import_settings_textfield',
            // width: 450,
            // labelWidth: 50,
            allowBlank: false,
            validateOnBlur: false,
            // listeners: {
            //     afterrender: function(field){
            //         document.getElementById(field.getId()).addEventListener(
            //             'change',
            //             function(event){
            //                 Ext.getCmp(this.id).eventFiles = event.target.files;
            //             },
            //             false
            //         );
            //     }
            // }
        }, {
            xtype: 'hidden',
            name: 'type',
            value: 'import'
        }]
    }],
});
