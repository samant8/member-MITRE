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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class FLogicDriver {

    private static final Logger logger = Logger.getLogger(FLogicDriver.class);

    private FLogicEngine flengine;
    private FLogicEnvironment oenv;
    private String[] args = new String[1000];
    private int valueCount;

    public FLogicDriver() {}

    public void release() {
        args=null;
    }

    public void parse(String in) throws Exception {
        parse(new StringReader(in));
    }

    public void parse(InputStream in) throws Exception {
        parse(new InputStreamReader(in));
    }

    public void parse(Reader in) throws Exception {
        setArgs(new String[FLogicConstants.MAX_DISJUNCTS]);
        FLogicLexer lexer = new FLogicLexer(in);
        FLogicParser parser = new FLogicParser(lexer, this);
        Object result = parser.parse();
        lexer = null;
    }

    public void initArgs(String str) {
        getArgs()[0] = str;
        setValueCount(1);
    }

    public void addArg(String str) {
        getArgs()[getValueCount()] = str;
        setValueCount(getValueCount() + 1);
    }

    public void addExpression(Expression ex1, Expression ex2) {
        Expression ex = ex1;
        while (ex.getNext() != null) {
            ex = ex.getNext();
        }
        ex.setNext(ex2);
    }

    public void addConjunct(PredValue pv1, PredValue pv2) {
        PredValue pv = pv1;
        while (pv.getConj_next() != null) {
            pv = pv.getConj_next();
        }
        pv.setConj_next(pv2);
    }

    public void addDisjunct(PredValue pv1, PredValue pv2) {
        PredValue pv = pv1;
        while (pv.getDisj_next() != null) {
            pv = pv.getDisj_next();
        }
        pv.setDisj_next(pv2);
    }

    public void addAssertion(Expression assertion) {
        PredValue pv;

        if (assertion.getGraph().length() == 0) {
            assertion.setGraph(FLogicConstants.DEFAULT_GRAPH);
        }
        pv = assertion.getPreds();

        while (pv != null) {
            for (int i = 0; i < pv.getDisjuncts(); i++) {
                getFlengine().addToOntology(assertion.getSub(), pv.getPredicate(), pv.getArgs()[i], pv.getType(), assertion.getGraph(), getOenv());
            }
            pv = pv.getConj_next();
        }
    }

    public void addPredicate(Expression preddef) {
        PredValue pv = preddef.getPreds();
        ArrayList<String> range = new ArrayList<String>();

        // test for pred[symmetric=>true]
        if(pv.getPredicate().equals("symmetric") || pv.getPredicate().equals("transitive"))
            getFlengine().addPredicateSemantics(preddef.getSub(), pv.getPredicate(), "");
        else if(pv.getPredicate().equals("inverseOf") || pv.getPredicate().equals("subPropertyOf"))
            getFlengine().addPredicateSemantics(preddef.getSub(), pv.getPredicate(), pv.getArgs()[0]);
        else { // domain[pred=>{range}]
            for (int i = 0; i < pv.getDisjuncts(); i++) {
                range.add(pv.getArgs()[i]);
            }
            //getFlengine().addPredicateToOntology(pv.getPredicate(), pv.getType(), preddef.getSub(), range, getOenv());
            if(pv.getPredicate().equals("domain"))
                getFlengine().addPredicateToOntology(preddef.getSub(), pv.getType(), range.get(0), null, getOenv());
            else if(pv.getPredicate().equals("range"))
                getFlengine().addPredicateToOntology(preddef.getSub(), pv.getType(), null, range, getOenv());
        }
        // clean up
        range.clear();
        range=null;
        preddef.release();
        preddef=null;
    }

    public void delAssertion(Expression assertion) {
        for (int i = 0; i < assertion.getPreds().getDisjuncts(); i++) {
            getFlengine().delFromOntology(assertion.getSub(), assertion.getPreds().getPredicate(),
                                          assertion.getPreds().getArgs()[i], FLogicConstants.DEFAULT_GRAPH, getOenv());
        }
    }

    public void createNewRule(Expression head, Expression body) {
        getFlengine().loadFLogicRule(head, body);
        head = null;
        body = null;
    }

    public void createNewGoal(Expression clauses) {
        getFlengine().addGoal(clauses, getOenv());
        clauses = null;
    }

    public Expression createNewExpression(String sub, String pred, String obj) {
        Expression ex;

        ex = new Expression();
        ex.setPreds(new PredValue());
        ex.setSub(sub);
        ex.setNext(null);
        ex.setGraph("");
        ex.getPreds().setType("->");
        ex.getPreds().setPredicate(pred);
        ex.getPreds().getArgs()[0] = obj;
        ex.getPreds().setDisjuncts(1);
        ex.getPreds().setConj_next(null);
        ex.getPreds().setDisj_next(null);

        return ex;
    }

    public Expression createNewExpression(String sub, PredValue preds) {
        Expression ex;

        ex = new Expression();
        ex.setPreds(preds);
        ex.setSub(sub);
        ex.setNext(null);
        ex.setGraph("");

        return ex;
    }

    public Expression createNewExpression(String sub, Aggregate aggr) {
        Expression ex;

        ex = new Expression();
        ex.setPreds(null);
        ex.setSub(sub);
        ex.setNext(null);
        ex.setGraph("");
        ex.setAggregate(aggr);

        return ex;
    }

    public PredValue createNewPredValue(String predExpr, String type) {
        PredValue pv;

        pv = new PredValue();
        pv.setPredicate(predExpr);
        pv.setType(type);
        for (int i = 0; i < getValueCount(); i++) {
            pv.getArgs()[i] = getArgs()[i];
        }
        pv.setDisjuncts(getValueCount());
        pv.setConj_next(null);
        pv.setDisj_next(null);

        return pv;
    }

    public String createPredExpr(String pred) {
        String predExpr;

        predExpr = pred + "(";
        for (int i = 0; i < getValueCount(); i++) {
            if (i > 0) {
                predExpr = predExpr.concat(",");
            }
            predExpr = predExpr.concat(getArgs()[i]);
        }
        predExpr = predExpr.concat(")");
        return predExpr;
    }

    public Aggregate createNewAggregate(String aggregateType, String var, Expression ex) {
        Aggregate ag;

        ag = new Aggregate();
        ag.setExpressions(ex);
        ag.setVariable(var);
        ag.setMethod(aggregateType);

        return ag;
    }

    /**
     * @return the flengine
     */
    public FLogicEngine getFlengine() {
        return flengine;
    }

    /**
     * @param flengine the flengine to set
     */
    public void setFlengine(FLogicEngine flengine) {
        this.flengine = flengine;
    }

    /**
     * @return the oenv
     */
    public FLogicEnvironment getOenv() {
        return oenv;
    }

    /**
     * @param oenv the oenv to set
     */
    public void setOenv(FLogicEnvironment oenv) {
        this.oenv = oenv;
    }

    /**
     * @return the args
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * @param args the args to set
     */
    public void setArgs(String[] args) {
        this.args = args;
    }

    /**
     * @return the valueCount
     */
    public int getValueCount() {
        return valueCount;
    }

    /**
     * @param valueCount the valueCount to set
     */
    public void setValueCount(int valueCount) {
        this.valueCount = valueCount;
    }
}

// July 30, 2015
// java -jar ../jars/jflex-1.4.3.jar f-logic.flex
// java -jar ../jars/cup-0.11a.jar -parser FLogicParser f-logic.cup
