/*global
 Ext, Ung, Webui, rpc:true, i18n:true
 */
Ext.define('Ung.Wizard', {
    extend: 'Ext.container.Viewport',
    controller: 'wizard',
    layout: 'auto',
    style: {
        maxWidth: '800px',
        margin: '0 auto'
    },
    name: 'wizard',
    border: 0,
    currentPage: null,
    hasCancel: false,
    modalFinish: false, //can not go back or cancel on finish step
    finished: false,
    showLogo: false,
    items: [{
        xtype: 'panel',
        layout: 'card',
        itemId: 'content',
        height: 500,
        items: []
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        items: [{
            xtype: 'button',
            itemId: 'prevBtn',
            hidden: true,
            scale: 'medium',
            listeners: {
                click: 'onPrev'
            }
        }, '->', {
            xtype: 'button',
            itemId: 'nextBtn',
            scale: 'medium',
            listeners: {
                click: 'onNext'
            }
        }]
    }]
});

Ext.define('Ung.WizardController', {
    extend : 'Ext.app.ViewController',
    alias: 'controller.wizard',
    currentPage: 0,
    init: function () {
        this.prevBtn = this.view.down('#prevBtn');
        this.nextBtn = this.view.down('#nextBtn');
        this.content = this.view.down('#content');
        var items = [], i;
        //console.log(this.view.cards);
        for (i = 0; i < this.view.cards.length; i += 1) {
            items.push(this.view.cards[i].panel);
        }
        this.content.add(items);
        this.loadPage(0);
    },

    onPrev: function () {
        this.goToPage(this.currentPage - 1);
    },

    onNext: function () {
        this.goToPage(this.currentPage + 1);
    },

    goToPage: function (index) {
        //this.content.setActiveItem(this.currentIndex);

        var handler = null;
        if (this.currentPage <= index) {
            if (Ext.isFunction(this.view.cards[this.currentPage].onValidate)) {
                if (!this.view.cards[this.currentPage].onValidate()) {
                    return;
                }
            }
            handler = this.view.cards[this.currentPage].onNext;
        } else if (this.currentPage > index) {
            handler = this.view.cards[this.currentPage].onPrevious;
        }

        // Call the handler if it is defined
        if (Ext.isFunction(handler)) {
            handler(Ext.bind(this.loadPage, this, [index]));
        } else {
            this.loadPage(index);
        }
    },

    loadPage: function (index) {
        if (index < 0 || index >= this.view.cards.length) {
            return;
        }
        this.currentPage = index;
        var card = this.view.cards[this.currentPage];
        if (Ext.isFunction(card.onLoad)) {
            card.onLoad(Ext.bind(this.syncWizard, this));
        } else {
            this.syncWizard();
        }
    },

    syncWizard : function () {
        this.content.setActiveItem(this.currentPage);

        if (this.currentPage === 0) {
            this.prevBtn.hide();
        } else {
            this.prevBtn.show();
            this.prevBtn.setText('&laquo; ' + this.view.cards[this.currentPage - 1].title);
        }

        if (this.currentPage == (this.view.cards.length - 1)) {
            if (this.modalFinish) {
                this.nextBtn.setText(i18n._('Close'));
                if (this.hasCancel) {
                    this.cancelButton.hide();
                }
                this.finished = true;
            } else {
                this.nextBtn.setText(i18n._('Finish'));
            }
        } else {
            this.nextBtn.setText(this.view.cards[this.currentPage + 1].title + ' &raquo;');
            if (this.hasCancel) {
                this.cancelButton.show();
            }
        }
    }
});