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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

public class Literal {

    private Atom predicate;
    private Atom[] args = new Atom[FLogicConstants.MAX_ARITY];
    public  HashMap<String, Atom> atts = new HashMap<String, Atom>();
    private Atom graph;
    private boolean negated = false;
    private int arity;
    private int negBTGoal;
    private int type = 0; // 0=normal 1=disjunct 2=aggregate
    private ArrayList<Literal> disjuncts = new ArrayList<Literal>();

    // for aggregate literals
    public String aggregateVariable;

    public Literal() {}

    public Literal(String predLabel, ArrayList<String> argLabels, String graphLabel) {
        int argsCnt = 0;
        negated     = false;
        type        = 0;
        predicate   = new Atom(predLabel);
        for (int i = 0; i < argLabels.size() && i < FLogicConstants.MAX_ARITY; i++) {
            String albl = argLabels.get(i);
            if(albl.startsWith("$")) {
                String tok[] = albl.split("->");
                if(tok.length==2) atts.put(tok[0].replace("$",""), new Atom(tok[1]));
            } else {
                args[i] = new Atom(argLabels.get(i));
                argsCnt++;
            }
        }
        arity = argsCnt;
        graph = new Atom(graphLabel);
    }

    public Literal(String predLabel, String subLabel, String objLabel, String graphLabel) {
        negated   = false;
        type      = 0;
        predicate = new Atom(predLabel);
        args[0]   = new Atom(subLabel);
        args[1]   = new Atom(objLabel);
        arity     = 2;
        graph     = new Atom(graphLabel);
    }

    public Literal(Literal literal) {
        Literal disj, ldisj;

        type = 0;

        predicate = new Atom(literal.getPredicate().getLabel());
        for (int a = 0; a < literal.arity; a++) {
            args[a] = new Atom(literal.getArgs()[a].getLabel());
        }
        for(Map.Entry me : literal.atts.entrySet()) {
            Atom atm = (Atom) me.getValue();
            atts.put((String) me.getKey(), new Atom(atm.getLabel()));
        }
        negated   = literal.negated;
        negBTGoal = literal.negBTGoal;
        arity     = literal.arity;
        type      = literal.type;
        graph     = new Atom(literal.getGraph().getLabel());
        aggregateVariable = literal.aggregateVariable;

        if (literal.type == 1) {
            for (int i = 0; i < literal.disjuncts.size(); i++) {
                ldisj = literal.disjuncts.get(i);
                disj = new Literal();
                disj.predicate = new Atom(ldisj.getPredicate().getLabel());
                for (int a = 0; a < ldisj.arity; a++) {
                    disj.args[a] = new Atom(ldisj.getArgs()[a].getLabel());
                }
                disj.arity   = ldisj.arity;
                disj.negated = ldisj.negated;
                disj.type    = ldisj.type;
                disj.graph   = new Atom(ldisj.getGraph().getLabel());
                disjuncts.add(disj);
            }
        }

        if (literal.type == 2) {
            for(int i=0; i<literal.disjuncts.size(); i++) {
                ldisj = literal.disjuncts.get(i);
                disj  = new Literal(ldisj);
                disjuncts.add(disj);
            }
        }
    }

    public void release() {
        for(int i=0; i<FLogicConstants.MAX_ARITY; i++) args[i]=null;
        args=null;
        for(Map.Entry me : atts.entrySet()) {
            Atom atm = (Atom) me.getValue();
            atm = null;
        }
        atts.clear();
        atts=null;
        for(Literal lit : disjuncts) {
            lit.release();
            lit=null;
        }
        disjuncts.clear();
        disjuncts=null;
    }

    @Override
    public String toString() {
        String ss;

        ss = "";
        if (isNegated()) {
            ss += "not ";
        }
        if (getType() == 0) {
            ss += getArgs()[0].getLabel() + "[" + getPredicate().getLabel();
            for(Map.Entry me : atts.entrySet()) {
                Atom atm = (Atom) me.getValue();
                ss += "$"+(String) me.getKey() + "->" + atm.getLabel();
            }
            if (getArity()>2) {
                ss += "(";
                for(int a=2; a<getArity(); a++) {
                    if(a>2) ss += ",";
                    ss += getArgs()[a].toFLogic();
                }
                ss += ")";
            }
            ss += "->" + getArgs()[1].getLabel() + "]";
        } else {
            ss += " (";
            for (int i = 0; i < getDisjuncts().size(); i++) {
                if (i > 0) {
                    ss += " ; ";
                }
                ss += getDisjuncts().get(i).toString();
            }
            ss += ") ";
        }
        if (!getGraph().getLabel().equals(FLogicConstants.DEFAULT_GRAPH)) {
            ss += "@" + getGraph().getLabel();
        }
        return ss;
    }

    public String toFLogic() {
        String ss;

        ss = "";
        if(isNegated()) ss += "~";

        // Normal statement
        if(getType() == 0) {
            String pred = getPredicate().getLabel();
            if(pred.equals(":") || pred.equals("<:") || pred.equals("!="))
                ss += getArgs()[0].toFLogic() + " " + pred + " " + getArgs()[1].toFLogic();
            else {
                ss += getArgs()[0].toFLogic() + "[" + pred;
                for (Map.Entry me : atts.entrySet()) {
                    Atom atm = (Atom) me.getValue();
                    ss += "$"+(String) me.getKey() + "->"+atm.getLabel();
                }
                if(getArity()>2) {
                    ss += "(";
                    for(int a=2; a<getArity(); a++) {
                        if(a>2) ss += ",";
                        ss += getArgs()[a].toFLogic();
                    }
                    ss += ")";
                }
                ss += "->" + getArgs()[1].toFLogic() + "]";
            }
        }

        // Disjunct statement
        else if(getType() == 1) {
            Literal dlit = getDisjuncts().get(0); // all disjuncts share the same subject
            ss += dlit.getArgs()[0].toFLogic() + "[";
            for(int d=0; d<getDisjuncts().size(); d++) {
                if(d>0) ss += "; ";
                dlit = getDisjuncts().get(d);
                ss += dlit.getPredicate().getLabel() + "->" + dlit.getArgs()[1].toFLogic();
            }
            ss += "]";
        }

        // Aggregate statement
        else if(getType() == 2) {
            ss += getArgs()[0].toFLogic() + "[" + getPredicate().getLabel() + "->{";
            ss += aggregateVariable + " | ";
            for(int d=0; d<getDisjuncts().size(); d++) {
                Literal dlit = getDisjuncts().get(d);
                if(d>0) ss += ", ";
                ss += dlit.toFLogic();
            }
            ss += "}]";
        }

        return ss;
    }

    public String toNormalizedString() {
        StringBuffer sb = new StringBuffer();
        sb.append(predicate.getLabel());
        for(int a=0; a<getArity(); a++)
            if(getArgs()[a].getType() == FLogicConstants.VAR) sb.append("_?V");
            else {
                sb.append("_");
                sb.append(getArgs()[a].getLabel());
            }
        return sb.toString();
    }

    public void getUniqueVariables(HashSet<String> vars, HashSet<String> vals, boolean includeAtts) {
        if(type == 1) {
            for(Literal lit: disjuncts)
                lit.getUniqueVariables(vars, vals, includeAtts);
            return;
        }

        for(int a=0; a<arity; a++)
            if(args[a].getType() == FLogicConstants.VAR)
                vars.add(args[a].getLabel());
            else if(a<2) // Dec. 10, 2015 : limit to sub & obj
                vals.add(args[a].getLabel());

        if(includeAtts)
            for(Map.Entry me: atts.entrySet()) {
                Atom atm = (Atom) me.getValue();
                if(atm.getType()  == FLogicConstants.VAR)
                    vars.add(atm.getLabel());
                //else // Dec. 10, 2015 : Don't include these values in the optimizer
                //	vals.add(atm.getLabel());
            }
    }

    // Apply bindings
    public void updateAtom(Atom atm, BindingSet bindSet) {
        int j;
        boolean changed;

        changed = false;
        for (j = 0; j < bindSet.getSize(); j++) {
            if (atm.getLabel().equals(bindSet.getSource()[j])) {
                atm.update(bindSet.getDestination()[j]);
                changed = true;
                break;
            } else if (atm.getLabel().equals(bindSet.getDestination()[j])) {
                if (!bindSet.getSource()[j].startsWith("?")) {
                    atm.update(bindSet.getSource()[j]);
                    changed = true;
                    break;
                }
            }
        }
    }

    public void substituteBindings(BindingSet bindSet) {
        Literal dlit;
        int a, j, l;

        if (getType() == 0) {
            for (a = 0; a < getArity(); a++) {// check bindings for this variable
                if (getArgs()[a].getType() == FLogicConstants.VAR)
                    updateAtom(getArgs()[a], bindSet);
            }
            for(Map.Entry me: atts.entrySet()) {
                Atom atm = (Atom) me.getValue();
                if(atm.getType() == FLogicConstants.VAR)
                    updateAtom(atm, bindSet);
            }
            if (getGraph().getType() == FLogicConstants.VAR)
                updateAtom(getGraph(), bindSet);
            if (getPredicate().getType() == FLogicConstants.VAR)
                updateAtom(getPredicate(), bindSet);
        } else if (getType() == 1) {
            for (l = 0; l < getDisjuncts().size(); l++) {
                dlit = getDisjuncts().get(l);
                for (a = 0; a < dlit.getArity(); a++) {
                    if (dlit.getArgs()[a].getType() == FLogicConstants.VAR)
                        updateAtom(dlit.getArgs()[a], bindSet);
                }
                if(dlit.getGraph().getType() == FLogicConstants.VAR)
                    updateAtom(dlit.getGraph(), bindSet);
                if(dlit.getPredicate().getType() == FLogicConstants.VAR)
                    updateAtom(dlit.getPredicate(), bindSet);
            }
        } else if (getType() == 2) {
            for (a = 0; a < getArity(); a++) {// check bindings for this variable
                if (getArgs()[a].getType() == FLogicConstants.VAR)
                    updateAtom(getArgs()[a], bindSet);
            }
            for (l = 0; l < getDisjuncts().size(); l++) {
                dlit = getDisjuncts().get(l);
                dlit.substituteBindings(bindSet);
            }
        }
    }

    public void updateVariableName(String idstr) {
        int i;

        if (getPredicate().getType() == FLogicConstants.VAR) {
            getPredicate().setLabel(getPredicate().getLabel().concat(idstr));
        }
        for (i = 0; i < getArity(); i++) {
            if (getArgs()[i].getType() == FLogicConstants.VAR) {
                getArgs()[i].setLabel(getArgs()[i].getLabel().concat(idstr));
            }
        }
        for(Map.Entry me : atts.entrySet()) {
            Atom atm = (Atom) me.getValue();
            atm.setLabel(atm.getLabel().concat(idstr));
        }
        if (getGraph().getType() == FLogicConstants.VAR) {
            getGraph().setLabel(getGraph().getLabel().concat(idstr));
        }
        if(type==2) aggregateVariable = aggregateVariable.concat(idstr);
    }

    /**
     * @return the predicate
     */
    public Atom getPredicate() {
        return predicate;
    }

    /**
     * @param predicate the predicate to set
     */
    public void setPredicate(Atom predicate) {
        this.predicate = predicate;
    }

    /**
     * @return the args
     */
    public Atom[] getArgs() {
        return args;
    }

    /**
     * @param args the args to set
     */
    public void setArgs(Atom[] args) {
        this.args = args;
    }

    /**
     * @return the graph
     */
    public Atom getGraph() {
        return graph;
    }

    /**
     * @param graph the graph to set
     */
    public void setGraph(Atom graph) {
        this.graph = graph;
    }

    /**
     * @return the negated
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * @param negated the negated to set
     */
    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * @return the arity
     */
    public int getArity() {
        return arity;
    }

    /**
     * @param arity the arity to set
     */
    public void setArity(int arity) {
        this.arity = arity;
    }

    /**
     * @return the negBTGoal
     */
    public int getNegBTGoal() {
        return negBTGoal;
    }

    /**
     * @param negBTGoal the negBTGoal to set
     */
    public void setNegBTGoal(int negBTGoal) {
        this.negBTGoal = negBTGoal;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return the disjuncts
     */
    public ArrayList<Literal> getDisjuncts() {
        return disjuncts;
    }

    /**
     * @param disjuncts the disjuncts to set
     */
    public void setDisjuncts(ArrayList<Literal> disjuncts) {
        this.disjuncts = disjuncts;
    }
}
