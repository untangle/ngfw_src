Ext.define('Ung.config.events.view.EmailTemplate', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config-events-email-template',
    itemId: 'emailtemplate',
    title: 'Email Template'.t(),
    scrollable: true,

    bodyPadding: 10,
    withValidation: true,

    items:[{
        xtype:'fieldset',
        title: 'Emailed Alert Template Settings'.t(),
        collapsible: false,
        items:[{
            xtype:'fieldset',
            title: 'Customization Parameters'.t(),
            collapsible: true,
            collapsed: true,
            items:[{
                xtype: 'grid',
                columns:[{
                    text: 'Name'.t(),
                    dataIndex: 'name',
                    flex: 1
                },{
                    text: 'Description'.t(),
                    dataIndex: 'description',
                    flex: 2
                }],
                bind:{
                    store: '{templateParametersStore}'
                }
            }],
        },{
            xtype: 'textfield',
            fieldLabel: 'Subject'.t(),
            itemId: 'emailSubject',
            labelWidth: 150,
            width: 500,
            bind: '{settings.emailSubject}',
            allowBlank: false,
            blankText: 'Subject must be specified.'.t(),
            listeners: {
                change: 'templateChange'
            }
        },{
            xtype: "textarea",
            fieldLabel: "Body".t(),
            itemId: 'emailBody',
            blankText: 'Body must be specified.'.t(),
            allowBlank: false,
            labelWidth: 150,
            width: 500,
            height: 200,
            bind: '{settings.emailBody}',
            listeners: {
                change: 'templateChange'
            }
        },{
            xtype: "displayfield",
            margin: '0 0 0 155',
            bind: {
                hidden: '{templateUnmatched == ""}',
                value: '{templateUnmatched}'
            }
        },{
            xtype: "checkbox",
            itemId: 'emailConvert',
            allowBlank: false,
            fieldLabel: "Human-readable values".t(),
            labelWidth: 150,
            bind: '{settings.emailConvert}',
            listeners: {
                change: 'templateChange'
            }
        },{
            xtype: 'container',
            layout: 'column',
            items:[{
                xtype: 'button',
                text: 'Reset to defaults'.t(),
                iconCls: 'fa fa-recycle',
                handler: 'templateDefaults',
                width: 150
            },{
                xtype: 'component',
                margin: '4 0 10 10',
                html: Ext.String.format( 'Reset template to defaults.'.t(),'<b>','</b>')
            }]
        }]
    },{
        xtype:'fieldset',
        title: 'Preview'.t(),
        collapsible: false,
        items:[{
            xtype: 'displayfield',
            fieldLabel: 'Subject'.t(),
            fieldStyle: {
                'font-family': 'monospace, monospace'
            },
            bind: {
                value: '{previewSubject}'
            }
        },{
            xtype: 'displayfield',
            fieldLabel: 'Body'.t(),
            fieldStyle: {
                'font-family': 'monospace, monospace'
            },
            bind: {
                value: '{previewBody}'
            }
        }]
    }]
});
