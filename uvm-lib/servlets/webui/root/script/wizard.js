Ext.namespace('Ung');
// The location of the blank pixel image
Ung.Wizard = Ext.extend(Ext.Panel, {
    currentPage : 0,

    constructor : function( config )
    {
        this.cards = config.cards;
        
        /* Build a panel to hold the headers on the left */
        this.headerPanel = new Ext.Panel( {
            cls : 'wizard-steps',
            items : this.buildHeaders( this.cards ),
            defaults : { border : false },
            layout : 'table',
            layoutConfig : { columns : 1 },
            region : "west",
            width : 100
        } );

        var panels = [];

        var length = this.cards.length;
        for ( c = 0 ;c < length ; c++ ) panels.push(this.cards[c].panel );

        this.previousButton = new Ext.Button({
            id : 'card-prev',
            text : '&laquo; Previous',
            handler : this.goPrevious,
            scope : this
        });

        this.nextButton = new Ext.Button({
            id : 'card-next',
            text : 'Next &raquo;',
            handler : this.goNext,
            scope : this
        });

        /* Build a card to hold the wizard */
        this.contentPanel = new Ext.Panel({
            layout : "card",
            items : panels,
            activeItem : 0,
            region : "center",
            defaults : { 
                autoHeight : true,
                border : false
            },
            bbar : [ '->', this.previousButton, this.nextButton ]
        });

        config.layout = "border";

        config.items = [ this.headerPanel, this.contentPanel ];

        Ung.Wizard.superclass.constructor.apply(this, arguments);
    },
    
    buildHeaders : function( cards )
    {
        var items = [];
        
        var length = cards.length;
        for ( var c = 0 ; c < length ; c++ ) {
            var card = cards[c];
            var id = this.getStepId( c );
            items.push({ 
                html : '<p>' + card.title + '</p>',
                cls : 'step'
            });
        }
        
        return items;
    },
    
    getStepId : function( index )
    {
        return "wizard-step-" + index;
    },

    goPrevious : function()
    {
        this.goToPage( this.currentPage - 1 );
    },

    goNext : function()
    {
        this.goToPage( this.currentPage + 1 );
    },

    goToPage : function( index )
    {
        if ( index >= this.cards.length ) index = this.cards.length - 1;
        if ( index < 0 ) index = 0;

        var hasChanged = false;
        var handler = null;

        if ( this.currentPage < index ) {
            /* moving forward, call the forward handler */
            hasChanged = true;
            handler = this.cards[this.currentPage].onNext;
        } else if ( this.currentPage > index ) {
            hasChanged = true;
            handler = this.cards[this.currentPage].onPrevious;
        }

        /* If the page has changed and it is defined, then call the handler */
        if ( handler ) handler();

        this.currentPage = index;
        handler = this.cards[this.currentPage].onLoad;
        
        if ( hasChanged && ( handler )) handler();

        this.contentPanel.getLayout().setActiveItem( this.currentPage );
        
        /* retrieve all of the items */
        var items = this.headerPanel.find();
        var length = items.length;
        var isComplete = true;
        for ( var c = 0 ; c < length ; c++ ) {
            var item = items[c];
            if ( c == this.currentPage ) {
                item.removeClass( "incomplete" );
                item.removeClass( "completed" );
                item.addClass( "current" );
                isComplete = false;
            } else {
                item.removeClass( "current" );
                if ( isComplete ) {
                    item.removeClass( "incomplete" );
                    item.addClass( "completed" );
                } else {
                    item.removeClass( "completed" );
                    item.addClass( "incomplete" );
                }
            }
        }
        
        if ( this.currentPage == 0 ) {
            this.previousButton.hide();
        } else {
            this.previousButton.show();
        }

        if ( this.currentPage == ( length - 1 )) {
            this.nextButton.setText( "Finish" );
        } else {
            this.nextButton.setText( "Next &raquo;" );
        }
    }
});
