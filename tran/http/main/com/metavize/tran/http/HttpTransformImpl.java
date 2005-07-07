/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.http;


import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.TransformContextFactory;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class HttpTransformImpl extends AbstractTransform implements HttpTransform
{
    private final Logger logger = Logger.getLogger(HttpTransformImpl.class);

    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new CasingPipeSpec("http", this, HttpCasingFactory.factory(),
                           Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS)
    };

    //    private HttpSettingsPriv settingsPriv;

    // constructors -----------------------------------------------------------

    public HttpTransformImpl() { }

    // HttpTransform methods --------------------------------------------------

//     public void enable()
//     {
// //         settingsPriv.setEnabled(true);
// //         syncSettingsPriv(settingsPriv);

// //         TransformState ts = getTransformState();
// //         if (ts == TransformState.RUNNING) {
// //             connectMPipe();
// //         }
//     }

//     public void disable()
//     {
// //         settingsPriv.setEnabled(false);
// //         syncSettingsPriv(settingsPriv);

// //         TransformState ts = getTransformState();
// //         if (ts == TransformState.RUNNING) {
// //             diconnectMPipe();
// //         }
//     }

    // MultiTransform methods -------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // lifecycle methods ------------------------------------------------------

    protected void postInit(String[] args)
    {
//         Session s = TransformContextFactory.context().openSession();
//         try {
//             Transaction tx = s.beginTransaction();

//             Query q = s.createQuery
//                 ("from HttpSettingsPriv hsp where hsp.tid = :tid");
//             q.setParameter("tid", getTid());
//             settingsPriv = (HttpSettingsPriv)q.uniqueResult();

//             tx.commit();
//         } catch (HibernateException exn) {
//             logger.warn("Could not get HttpBlockerSettings", exn);
//         } finally {
//             try {
//                 s.close();
//             } catch (HibernateException exn) {
//                 logger.warn("could not close Hibernate session", exn);
//             }
//         }
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        throw new UnsupportedOperationException("bad move");
    }

    public void setSettings(Object settings)
    {
        throw new UnsupportedOperationException("bad move");
    }

    // private methods --------------------------------------------------------

//     private void syncSettingsPriv()
//     {
//         Session s = TransformContextFactory.context().openSession();
//         try {
//             Transaction tx = s.beginTransaction();

//             s.saveOrUpdate(settingsPriv);

//             tx.commit();
//         } catch (HibernateException exn) {
//             logger.warn("could not get HttpBlockerSettings", exn);
//         } finally {
//             try {
//                 s.close();
//             } catch (HibernateException exn) {
//                 logger.warn("could not close hibernate session", exn);
//             }
//         }
//     }
}
