#!/usr/bin/python

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

import sys

if PREFIX != '':
    sys.path.insert(0, REPORTS_PYTHON_DIR)

import gettext
import locale
import mx
import os
import smtplib
import reports.sql_helper as sql_helper

from email import Encoders
from email.MIMEBase import MIMEBase
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText

_ = gettext.gettext

def mail_reports(date, file):
    receivers, sender = __get_mail_info()
    company_name = __get_branding_info()
    for receiver in receivers:
        mail(file, sender, receiver, date, company_name)

def mail(file, sender, receiver, date, company_name):
    msgRoot = MIMEMultipart('related')
    msgRoot['Subject'] = _('New %s Reports Available') % company_name
    msgRoot['From'] = sender
    msgRoot['To'] = receiver

    msgRoot.attach(MIMEText(_("""\
%s Reports are attached for %s is attached. The pdf file requires Adobe Acrobat Reader to view.\
""") % (company_name, date.strftime(locale.nl_langinfo(locale.D_FMT)))))

    part = MIMEBase('application', "pdf")
    part.set_payload(open(file, 'rb').read())
    Encoders.encode_base64(part)
    part.add_header('Content-Disposition', 'attachment; filename="reports-%d%02d%02d.pdf"'
                    % (date.year, date.month, date.day))
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

def __get_branding_info():
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT company_name FROM settings.uvm_branding_settings""")

        row = curs.fetchone()

        if row:
            rv = row[0]
        else:
            rv = 'Untangle'
    except Exception, e:
        rv = 'Untangle'
    finally:
        conn.commit()

    return rv
