/*global
 Ext, Ung, Webui, rpc:true, i18n:true
 */
Ext.define('Ung.Wizard', {
    extend: 'Ext.container.Viewport',
    controller: 'wizard',
    layout: 'auto',
    name: 'wizard',
    maxWidth: 'auto',
    minWidth: 'auto',
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
        header: {
            xtype: 'header',
            padding: '10',
            margin: '0',
            itemId: 'progress'
        },
        items: []
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        items: [{
            xtype: 'button',
            itemId: 'prevBtn',
            hidden: true,
            scale: 'medium',
            padding: '3 10 3 0',
            listeners: {
                click: 'onPrev'
            }
        }, '->', {
            xtype: 'button',
            itemId: 'cancelBtn',
            scale: 'medium',
            text: i18n._('Cancel'),
            hidden: true,
            listeners: {
                click: 'onCancel'
            }
        }, {
            xtype: 'button',
            itemId: 'nextBtn',
            scale: 'medium',
            padding: '3 0 3 10',
            listeners: {
                click: 'onNext'
            }
        }]
    }]
});

Ext.define('Ung.WizardController', {
    extend : 'Ext.app.ViewController',
    alias: 'controller.wizard',
    init: function () {
        Ext.getWin().addKeyListener(13, function () {
            this.onNext();
        }, this);

        this.callParent();
        this.view.setStyle({
            maxWidth: this.view.maxWidth + 'px',
            minWidth: this.view.minWidth + 'px',
            margin: '0 auto'
        });

        this.prevBtn = this.view.down('#prevBtn');
        this.nextBtn = this.view.down('#nextBtn');
        this.cancelBtn = this.view.down('#cancelBtn');
        this.content = this.view.down('#content');

        var items = [], i;
        for (i = 0; i < this.view.cards.length; i += 1) {
            items.push(this.view.cards[i].panel);
        }
        this.content.add(items);
    },

    afterRender: function () {
        this.progress = this.view.down('#progress');
        var i;
        // remove welcome and finish pages from progress
        for (i = 0; i < this.view.cards.length - 2; i += 1) {
            this.progress.add({
                xtype: 'component',
                cls: 'progress-item',
                html: '<i class="material-icons" style="color: #999;">check_box_outline_blank</i>'
            });
        }
        this.loadPage(0);
    },

    onPrev: function () {
        this.goToPage(this.view.currentPage - 1);
    },

    onNext: function () {
        this.goToPage(this.view.currentPage + 1);
    },

    onCancel: function () {
        Ext.emptyFn();
    },

    goToPage: function (index) {
        //this.content.setActiveItem(this.currentIndex);
        var handler = null, pageNo = this.view.currentPage;
        if (pageNo <= index) {
            if (Ext.isFunction(this.view.cards[pageNo].onValidate)) {
                if (!this.view.cards[pageNo].onValidate()) {
                    return;
                }
            }
            handler = this.view.cards[pageNo].onNext;
        } else if (pageNo > index) {
            handler = this.view.cards[pageNo].onPrevious;
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
        this.view.currentPage = index;
        var card = this.view.cards[this.view.currentPage];

        this.content.setTitle('<h2 class="wizard-title">' + card.title + '</h2>');

        this.setProgress();

        if (Ext.isFunction(card.onLoad)) {
            card.onLoad(Ext.bind(this.syncWizard, this));
        } else {
            this.syncWizard();
        }
    },

    syncWizard : function () {
        var pageNo = this.view.currentPage;
        this.content.setActiveItem(pageNo);

        if (pageNo === 0) {
            this.prevBtn.hide();
        } else {
            this.prevBtn.show();
            this.prevBtn.setText('<i class="material-icons" style="vertical-align: middle;">navigate_before</i><span style="vertical-align: middle;"><strong>' + i18n._('Previous') + '</strong>: ' + this.view.cards[pageNo - 1].title + '</span>');
        }

        if (pageNo == (this.view.cards.length - 1)) {
            if (this.view.modalFinish) {
                this.nextBtn.setText(i18n._('Close'));
                if (this.view.hasCancel) {
                    this.cancelBtn.hide();
                }
                this.view.finished = true;
            } else {
                if (!this.view.languageSetup) {
                    this.nextBtn.setText('<span style="vertical-align: middle;">' + i18n._('Finish') + '</span> <i class="material-icons" style="vertical-align: middle;">check</i>');
                } else {
                    this.nextBtn.setText('<span style="vertical-align: middle;"><strong>' + i18n._('Continue') + '</strong> <i class="material-icons" style="vertical-align: middle;">navigate_next</i>');
                }
            }
        } else {
            this.nextBtn.setText('<span style="vertical-align: middle;"><strong>' + i18n._('Next') + '</strong>: ' + this.view.cards[pageNo + 1].title + '</span><i class="material-icons" style="vertical-align: middle;">navigate_next</i>');
            if (this.view.hasCancel) {
                this.cancelBtn.show();
            }
        }
    },

    setProgress: function () {
        var progressItems = this.progress.query('component[cls="progress-item"]'), i;
        for (i = 0; i < progressItems.length; i += 1) {
            if (this.view.currentPage !== 0 && this.view.currentPage !== this.view.cards.length) {
                if (i < this.view.currentPage - 1) {
                    progressItems[i].setHtml('<i class="material-icons" style="color: green;">check_box</i>');
                }
                if (i === this.view.currentPage - 1) {
                    progressItems[i].setHtml('<i class="material-icons" style="color: green;">check_box_outline_blank</i>');
                }
                if (i > this.view.currentPage - 1) {
                    progressItems[i].setHtml('<i class="material-icons" style="color: #999;">check_box_outline_blank</i>');
                }
            }
        }
    }
});