import gettext
import reports.node.untangle_base_spam
import uvm.i18n_helper

_ = uvm.i18n_helper.get_translation('untangle-node-phish-blocker').lgettext

reports.engine.register_node(reports.node.untangle_base_spam.SpamBaseNode('untangle-node-phish-blocker', 'Phish Blocker', 'phish_blocker', 'Clam', _('Phish'), _('Clean'), _('Hourly Phish Rate'), _('Daily Phish Rate'), _('Top Ten Phishing Victims')))
