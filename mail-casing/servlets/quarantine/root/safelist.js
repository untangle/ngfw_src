Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

Ext.define('Ung.SafelistSelectionModel', {
    extend:'Ext.selection.CheckboxModel',

    onSelectionChange:function(model, selected, options) {
        this.safelist.updateActionItems();
    },

    constructor : function( config ) {
        Ung.SafelistSelectionModel.superclass.constructor.apply(this, arguments);
        this.safelist = config.safelist;
        this.addListener('selectionchange', this.onSelectionChange, this );
    }
});


Ung.Safelist = function() {
    this.cm = null;
}

Ung.Safelist.prototype = {
    init : function() {
        this.addWindow = null;

        this.selectionModel = Ext.create('Ung.SafelistSelectionModel', { safelist : this } );

        // the column model has information about grid columns
        // dataIndex maps the column to the specific data field in
        // the data store
        this.columns = [{
            header: i18n._( "Email Address" ),
            dataIndex: 'emailAddress',
            flex: 1,
            menuDisabled: true,
            field: {
                xtype:'textfield'
            }
        }];
            
        this.store = Ext.create('Ext.data.ArrayStore',{
            fields:[{name:'emailAddress'}],
            data : inboxDetails.safelistData
        }); 


        this.addButton = Ext.create('Ext.button.Button',{
        iconCls:'icon-add-row',
            text : i18n._( "Add" ),
            handler : function() {
                var window = this.getAddWindow();
                window.show();
            },
            "scope" : this
        });

        this.deleteButton = Ext.create('Ext.button.Button',{
            text : i18n._( "Delete Addresses" ),
            disabled : true,
            iconCls:'icon-delete-row',
            handler : function() {
                var addresses = [];
                var selectedRecords = this.selectionModel.getSelection();
                for ( var i =0; i < selectedRecords.length;i++) {
                    addresses.push( selectedRecords[i].data.emailAddress );
                }
                this.grid.setDisabled( true );
                this.selectionModel.deselect(selectedRecords);;
                this.deleteButton.setText( i18n._( "Delete Addresses" ));
                this.deleteButton.setDisabled( true );

                quarantine.rpc.deleteAddressesFromSafelist( Ext.bind(this.deleteAddresses,this ),
                                                            inboxDetails.token, addresses );
            },
            "scope" : this
        });

        this.grid = Ext.create('Ext.grid.Panel',{
            store : safelist.store,
            region : "center",
            columns: safelist.columns,
            selModel : this.selectionModel,
            loadMask : true,
            frame : true,
            stripeRows : true,
            cls:'safelist-grid',
            enableColumnHide: false,
            enableColumnMove: false,
            tbar : [ this.addButton, this.deleteButton ]
        });

        safelist.store.load();
    },

    getAddWindow : function() {
        if ( this.addWindow == null ) {
            this.addWindow = Ext.create('Ext.window.Window', {
                width:500,
                height:250,
                title: i18n._('Add an Email Address to Safelist'),
                closeAction:'hide',
                modal: true,
                layout: 'fit',
                items: {
                    xtype: 'panel',
                    bodyPadding: 10,
                    items: [{
                        xtype: 'textfield',
                        width: 420,
                        fieldLabel : i18n._( "Email Address" ),
                        name : "email_address"
                    }]
                },
                buttons: [{
                    text : i18n._( 'Save' ),
                    handler: function() {
                        var field = this.addWindow.down('textfield[name="email_address"]');
                        var email = field.getValue();
                        field.setValue( "" );
                        quarantine.safelist( [ email ] );
                        this.addWindow.hide();
                    },
                    scope : this
                },{
                    text : i18n._( 'Cancel' ),
                    handler: function() {
                        this.addWindow.hide();
                    },
                    scope : this
                }]
            } );
        }

        return this.addWindow;
    },

    deleteAddresses : function( result, exception, foo ) {
        if ( exception ) {
            var message = exception.message;
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            
            Ext.MessageBox.alert("Failed",message);
            return;
        }

        this.grid.setDisabled( false );

        var count = result.safelistCount;
        count = -count;

        var message = i18n.pluralise( i18n._( "Deleted one address" ), Ext.String.format( i18n._( "Deleted {0} addresses" ), count ), count );

        quarantine.showMessage( message );

        var sl = result.safelist;
        /* Build a new set of data */
        for ( var c = 0 ; c < sl.length ; c++ ) sl[c] = [sl[c]];
        safelist.store.loadData( sl );
    },

    updateActionItems : function() {
        var count = this.selectionModel.getCount();
        var text = i18n.pluralise( i18n._( "Delete one Address" ), Ext.String.format( i18n._( "Delete {0} Addresses" ), count ), count );
        if ( count > 0 ) {
            this.deleteButton.setDisabled( false );
        } else {
            this.deleteButton.setDisabled( true );
            text = i18n._( "Delete Addresses" );
        }

        this.deleteButton.setText( text );
    }
};
