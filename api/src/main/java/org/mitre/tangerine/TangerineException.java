/*
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *                   NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. FA8702-17-C-0001, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (MAY 2013)
 *
 * (c)2016-2017 The MITRE Corporation. All Rights Reserved.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */


package org.mitre.tangerine;

public class TangerineException extends Exception {

    /**
     * A basic exception to allow for reporting Exceptions that are distinctly Tangerine or Analysis Exchange in nature.
     */
    private static final long serialVersionUID = 1L;
    public TangerineException(String msg) {
        super(msg);
    }
    public TangerineException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
