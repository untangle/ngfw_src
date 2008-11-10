Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

Ung.SafelistSelectionModel = Ext.extend(  Ext.grid.CheckboxSelectionModel, {    
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
} );


Ung.Safelist = function() {
    this.cm = null;
}

Ung.Safelist.prototype = {
    init : function()
    {
        this.addWindow = null;

        this.selectionModel = new Ung.SafelistSelectionModel( { safelist : this } );

        // the column model has information about grid columns
        // dataIndex maps the column to the specific data field in
        // the data store
        this.cm = new Ext.grid.ColumnModel([
            this.selectionModel,
            {
                header: i18n._( "Email Address" ),
                dataIndex: 'emailAddress',
                editor : new Ext.form.TextField()
            }]);

        this.reader = new Ext.data.ArrayReader(
        {},
        [ { name : 'emailAddress' } ]);

        this.store = new Ext.data.Store({
            reader : safelist.reader,
            data : inboxDetails.safelistData
        });

        this.addButton = new Ext.Button( {
			iconCls:'icon-add-row',
            text : i18n._( "Add" ),
            handler : function() {
                var window = this.getAddWindow();
                window.show( this );
            },
            "scope" : this
        });

        this.deleteButton = new Ext.Button( {
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
                
                quarantine.rpc.deleteAddressesFromSafelist( this.deleteAddresses.createDelegate( this ), 
                                                            inboxDetails.token, addresses );
            },
            "scope" : this
        });
                    
        this.grid = new Ext.grid.GridPanel({
            store : safelist.store,
            region : "center",
            cm : safelist.cm,
            sm : this.selectionModel,
            loadMask : true,
            frame : true,
            stripeRows : true,
			cls:'safelist-grid',
			autoExpandColumn : 1,	
			autoExpandMax: 1700,
            tbar : [ this.addButton, this.deleteButton ]
         });
    
    },
    
    getAddWindow : function() {
        if ( this.addWindow == null ) {
            var panel = new Ext.FormPanel( {
                defaultType : 'textfield',
                items: [{ 
                    fieldLabel : i18n._( "Email Address" ),
                    name : "email_address"
                }],
                frame :false,
                height:'100%'
            } );
            this.addWindow = new Ext.Window( {

                width:500,
                height:300,
                closeAction:'hide',
                plain: true,
                items  :[ panel ],
				title:i18n._('Add an Email Address to Safelist'),
                
                buttons: [{
                    text : i18n._( 'Save' ),
                    handler: function() {
                        var field = this.addWindow.find( "name", "email_address" )[0];
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
            Ext.MessageBox.alert("Failed",exception.message); 
            return;
        }
        
        this.grid.setDisabled( false );
        
        var count = result.safelistCount;
        count = -count;

        var message = i18n.pluralise( i18n._( "Deleted one address" ), String.format( i18n._( "Deleted {0} addresses" ), count ), count );
        
        quarantine.showMessage( message );

        var sl = result.safelist;
        /* Build a new set of data */
        for ( var c = 0 ; c < sl.length ; c++ ) sl[c] = [sl[c]];
        safelist.store.loadData( sl );
    },

    updateActionItems : function()
    {
        var count = this.selectionModel.getCount();
        var text = i18n.pluralise( i18n._( "Delete one Address" ), String.format( i18n._( "Delete {0} Addresses" ), count ), count );
        if ( count > 0 ) {
            this.deleteButton.setDisabled( false );
        } else {
            this.deleteButton.setDisabled( true );
            text = i18n._( "Delete Addresses" );
        }

        this.deleteButton.setText( text );
    }
};
