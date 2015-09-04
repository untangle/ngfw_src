import uvm.i18n_helper
import gettext
import reports.node.untangle_base_spam

_ = uvm.i18n_helper.get_translation('untangle-node-spam-blocker-lite').lgettext

reports.engine.register_node(reports.node.untangle_base_spam.SpamBaseNode('untangle-node-spam-blocker-lite', 'Spam Blocker Lite', 'spam_blocker_lite', 'SpamBlockerLite', _('Spam'), _('Clean'), _("Hourly Spam Rate"), _("Spam Rate"), _("Top Ten Spammed")))
