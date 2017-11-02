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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HornClause {

    public int id;
    public Literal head;
    public ArrayList<Literal> body = new ArrayList<Literal>();

    public HornClause(Expression headExpr, Expression bodyExpr) {
        ArrayList<String> args;
        String[] tmp_args;
        String predLabel, val, graphLabel, subLabel, pred_args;
        boolean negated;
        Pattern func_expr = Pattern.compile("(.+)\\((.+)\\)");
        Matcher matches;
        PredValue pv, dpv;

        // init
        args = new ArrayList<String>();

        // Create the head of the rule
        val = headExpr.getSub();
        args.add(val);
        val = headExpr.getPreds().getArgs()[0];
        args.add(val);
        if (headExpr.getGraph().length() > 0) {
            graphLabel = headExpr.getGraph();
        } else {
            graphLabel = FLogicConstants.DEFAULT_GRAPH;
        }
        // function
        val = headExpr.getPreds().getPredicate();
        matches = func_expr.matcher(val);
        if (matches.find()) {
            predLabel = matches.group(1);
            pred_args = matches.group(2);
            tmp_args = pred_args.split(",");
            args.addAll(Arrays.asList(tmp_args));
        } else {
            predLabel = headExpr.getPreds().getPredicate();
        }
        String negSub = args.get(0);
        if (negSub.startsWith("~")) {
            negSub = negSub.substring(1);
            args.set(0, negSub);
        } // not allowed
        head = new Literal(predLabel, args, graphLabel);
        args.clear();

        // Create the body of the rule
        HornClause.buildClauses(bodyExpr, body);
    }

    public static void buildClauses(Expression exprs, ArrayList<Literal> Clauses) {
        Literal lit;
        Aggregate agg;
        DisjNo disjNo;
        PredValue pv;

        disjNo = new DisjNo();
        disjNo.value = 0;
        while (exprs != null) {
            pv = exprs.getPreds(); // pv is null if exprs is an aggregate expression
            if(pv != null) {
                lit = generateLiteral(exprs, disjNo);
                Clauses.add(lit);
            } else {
                agg = exprs.getAggregate();
                if(agg != null) {
                    Expression agg_exprs = agg.getExpression();
                    lit = new Literal();
                    lit.setPredicate(new Atom(agg.getMethod()));
                    lit.setArgs(new Atom[FLogicConstants.MAX_ARITY]);
                    lit.getArgs()[0] = new Atom(exprs.getSub());
                    lit.setArity(1); // ??
                    lit.setType(2); // aggregate
                    lit.setGraph(new Atom(FLogicConstants.DEFAULT_GRAPH));
                    lit.aggregateVariable = agg.getVariable();
                    while(agg_exprs != null) {
                        pv = agg_exprs.getPreds();
                        if(pv != null)
                            lit.getDisjuncts().add( generateLiteral(agg_exprs, disjNo) );
                        agg_exprs = agg_exprs.getNext();
                    }
                    Clauses.add(lit);
                }
            }
            exprs = exprs.getNext();
        }
        disjNo=null;
    }

    public static Literal generateLiteral(Expression expr, DisjNo disjNo) {
        Literal lit, dlit;
        String[] tmp_args;
        String predLabel, graphLabel, subLabel, pred_args, val;
        boolean negated;
        Pattern func_expr = Pattern.compile("(.+)\\((.+)\\)");
        Matcher matches;
        PredValue pv, dpv;

        ArrayList<Literal> disj_lit;
        ArrayList<String> args, disj_args;

        // init
        disj_lit = new ArrayList<Literal>();
        args = new ArrayList<String>();
        disj_args = new ArrayList<String>();

        disj_args.add("?_d1");
        disj_args.add("?_d2");

        subLabel = expr.getSub();
        if (expr.getGraph().length() > 0) {
            graphLabel = expr.getGraph();
        } else {
            graphLabel = FLogicConstants.DEFAULT_GRAPH;
        }
        if (subLabel.startsWith("~")) {
            subLabel = subLabel.substring(1);
            negated = true;
        } else {
            negated = false;
        }
        lit = null; // default
        pv = expr.getPreds();
        while (pv != null) {
            dpv = pv;
            while (dpv != null) {
                args.clear();
                args.add(subLabel);
                args.add(""); // dummy object
                val = dpv.getPredicate();
                matches = func_expr.matcher(val);
                if (matches.find()) {
                    predLabel = matches.group(1);
                    pred_args = matches.group(2);
                    tmp_args = pred_args.split(",");
                    args.addAll(Arrays.asList(tmp_args));
                } else {
                    predLabel = dpv.getPredicate();
                }
                for (int i = 0; i < dpv.getDisjuncts(); i++) {
                    val = dpv.getArgs()[i];
                    args.set(1, val);
                    dlit = new Literal(predLabel, args, graphLabel);
                    disj_lit.add(dlit);
                }
                dpv = dpv.getDisj_next();
            }
            if (disj_lit.size() > 1) {
                if (disjNo.value > 0) {
                    String ss;
                    ss = "" + disjNo.value;
                    String tmp_arg;
                    tmp_arg = disj_args.get(0).concat(ss);
                    disj_args.set(0, tmp_arg);
                    tmp_arg = disj_args.get(1).concat(ss);
                    disj_args.set(1, tmp_arg);
                }
                lit = new Literal("disj", disj_args, graphLabel);
                lit.setType(1); // disjunct
                for (int i = 0; i < disj_lit.size(); i++) {
                    lit.getDisjuncts().add(disj_lit.get(i));
                }
                disjNo.value++;
            } else {
                lit = disj_lit.get(0);
            }
            disj_lit.clear();
            lit.setNegated(negated);
            pv = pv.getConj_next();
        }
        return lit;
    }

    public void updateVariableNames() {
        String idstr;
        Literal lit, dlit;
        int i, j, l;

        idstr = "" + id;
        head.updateVariableName(idstr);
        for (j = 0; j < body.size(); j++) {
            lit = body.get(j);
            lit.updateVariableName(idstr);
            if (lit.getType() == 1 || lit.getType() == 2) {
                for (l = 0; l < lit.getDisjuncts().size(); l++) {
                    dlit = lit.getDisjuncts().get(l);
                    dlit.updateVariableName(idstr);
                }
            }
        }
    }

    @Override
    public String toString() {
        String ss;

        ss = head.toFLogic() + ":-";
        for(int b=0; b<body.size(); b++) {
            if(b>0) ss += " ";
            ss += body.get(b).toFLogic();
        }

        return ss;
    }
}
