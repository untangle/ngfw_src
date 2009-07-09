#!/usr/bin/python

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

import sys

if PREFIX != '':
    sys.path.insert(0, REPORTS_PYTHON_DIR)

import gettext
import mx
import os
import smtplib
import sql_helper

from email import Encoders
from email.MIMEBase import MIMEBase
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText

_ = gettext.gettext

def mail_reports(date, file):
    receivers, sender = __get_mail_info()
    for receiver in receivers:
        mail(file, sender, receiver, date)

def mail(file, sender, receiver, date):
    msgRoot = MIMEMultipart('related')
    msgRoot['Subject'] = _('New Untangle Reports Available')
    msgRoot['From'] = sender
    msgRoot['To'] = receiver

    msgRoot.attach(MIMEText(_("""\
Your Report for %s is attached.
""") % date.strftime("%c")))

    part = MIMEBase('application', "pdf")
    part.set_payload(open(file, 'rb').read())
    Encoders.encode_base64(part)
    part.add_header('Content-Disposition', 'attachment; filename="%s"'
                    % os.path.basename(file))
    msgRoot.attach(part)

    smtp = smtplib.SMTP()
    smtp.connect('localhost')
    print "SENDER: '%s' RECEIVER: '%s'" % (sender, receiver)
    smtp.sendmail(sender, receiver, msgRoot.as_string())
    smtp.quit()

def __get_mail_info():
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT report_email, from_address FROM settings.u_mail_settings""")

        row = curs.fetchone()

        if row:
            rv = (row[0].split(','), row[1])
        else:
            rv = None
    finally:
        conn.commit()

    return rv
