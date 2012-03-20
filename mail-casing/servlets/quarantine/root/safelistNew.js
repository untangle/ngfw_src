Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

Ext.define('Ung.SafelistSelectionModel', {
    extend:'Ext.selection.CheckboxModel',
    onRowSelect : function( sm, rowIndex, record ) {
        this.safelist.updateActionItems();
    },

    onRowDeselect : function( sm, rowIndex, record ) {
        this.safelist.updateActionItems();
    },

    constructor : function( config ) {
        Ung.SafelistSelectionModel.superclass.constructor.apply(this, arguments);

        this.safelist = config.safelist;

        this.addListener('rowselect',this.onRowSelect, this );
        this.addListener('rowdeselect',this.onRowDeselect, this );
    }
});


Ung.Safelist = function() {
    this.cm = null;
}

Ung.Safelist.prototype = {
    init : function()
    {
        this.addWindow = null;

        this.selectionModel = Ext.create('Ung.SafelistSelectionModel',{ safelist : this } );

        // the column model has information about grid columns
        // dataIndex maps the column to the specific data field in
        // the data store
        this.columns = [
            {
                header: i18n._( "Email Address" ),
                dataIndex: 'emailAddress',
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
                window.show( this );
            },
            "scope" : this
        });

        this.deleteButton = Ext.create('Ext.button.Button',{
            text : i18n._( "Delete Addresses" ),
            disabled : true,
            iconCls:'icon-delete-row',
            handler : function() {
                var addresses = [];
                this.selectionModel.each( function( record ) {
                    addresses.push( record.data.emailAddress );
                    return true;
                });

                this.grid.setDisabled( true );
                this.selectionModel.clearSelections();
                quarantine.selectionModel.clearSelections();
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
            autoExpandColumn : 1,
            autoExpandMax: 1700,
            tbar : [ this.addButton, this.deleteButton ]
        });

        safelist.store.load();
    },

    getAddWindow : function() {
        if ( this.addWindow == null ) {
            var panel = Ext.create('Ext.form.Panel', {
                defaultType : 'textfield',
                items: [{
                    fieldLabel : i18n._( "Email Address" ),
                    name : "email_address"
                }],
                frame :false,
                height:'100%'
            } );
            this.addWindow = Ext.create('Ext.Window',{

                width:500,
                height:300,
                closeAction:'hide',
                plain: true,
                items  :[ panel ],
                title:i18n._('Add an Email Address to Safelist'),

                buttons: [{
                    text : i18n._( 'Save' ),
                    handler: function() {
                        var field = this.addWindow.query('textfield[name="email_address"]')[0];
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

    deleteAddresses : function( result, exception, foo )
    {
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

    updateActionItems : function()
    {
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
