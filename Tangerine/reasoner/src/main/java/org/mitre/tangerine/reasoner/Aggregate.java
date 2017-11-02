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

package org.mitre.tangerine.reasoner;
public class Aggregate {

    private String method; // aggregate method
    private Expression expr; // query to evaluate
    private String variable; // variable to use as basis for method

    public Aggregate() {
    }

    public void setMethod(String m) {
        this.method = m;
    }
    public String getMethod() {
        return method;
    }

    public void setExpressions(Expression e) {
        this.expr = e;
    }
    public Expression getExpression() {
        return expr;
    }

    public void setVariable(String v) {
        this.variable = v;
    }
    public String getVariable() {
        return variable;
    }
}
