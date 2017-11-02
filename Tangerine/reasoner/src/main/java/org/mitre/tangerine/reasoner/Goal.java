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
import java.util.UUID;

public class Goal {

    private ArrayList<Literal> clauses = new ArrayList<Literal>();
    private ArrayList<String> history  = new ArrayList<String>(); // derivation history
    private BindingSet bindSet  = new BindingSet();
    private boolean saveResults = false;
    private boolean tabledGoal; // is this a tabled goal
    public  boolean connected = true; // is this a valid connected query
    private int id;
    private String uuid;  // used for saving goals
    private char ruleMode = 0;
    private Goal par; // to navigate up to the tree
    private ChoicePoint dataChoicePoints = new ChoicePoint(); // id for data
    private ChoicePoint ruleChoicePoints = new ChoicePoint(); // id for rules

    public Goal() {
        uuid = null;
    }

    public Goal(Expression clauses_expr) {
        clauses = new ArrayList<Literal>();
        history = new ArrayList<String>();
        bindSet = new BindingSet();
        dataChoicePoints = new ChoicePoint();
        ruleChoicePoints = new ChoicePoint();
        saveResults = false;
        ruleMode = 0;
        par = null;
        uuid = null;
        connected = true;

        HornClause.buildClauses(clauses_expr, clauses);
        optimize();
    }

    public void release() {
        history.clear();
        history=null;
        bindSet.release();
        bindSet=null;
        for(Literal lit : clauses) {
            lit.release();
            lit=null;
        }
        clauses.clear();
        clauses=null;
        dataChoicePoints.release();
        dataChoicePoints=null;
        ruleChoicePoints.release();
        ruleChoicePoints=null;
    }

    // March 2015: Rewrite the goal to use the most selective clause 1st
    //             then add additional clauses that refer to variables that have
    //             already been added
    // July 30 2015: For aggregate literals, they can be added once all referenced variables that are outside of the aggregation
    //               have been bound.
    public void optimize() {
        ArrayList<Literal> oldClauses = new ArrayList<Literal>();
        ArrayList<Literal> LbLits     = new ArrayList<Literal>();
        HashSet<String> vars          = new HashSet<String>();
        HashSet<String> litVars       = new HashSet<String>();
        HashSet<String> vals          = new HashSet<String>();
        HashSet<String> litVals       = new HashSet<String>();
        HashSet<String> negVars       = new HashSet<String>();

        HashSet<String> systemPreds   = new HashSet<String>();
        for(String pred: FLogicConstants.BuiltInPreds.split(" ")) systemPreds.add(pred);

        HashSet<String> hiPriorityPreds = new HashSet<String>();
        for(String pred: FLogicConstants.HiPriorityPreds.split(" ")) hiPriorityPreds.add(pred);

        HashSet<String> labelPreds = new HashSet<String>();
        for(String pred: FLogicConstants.LabelPreds.split(" ")) labelPreds.add(pred);

        // find variables that are used in negated literals
        for(Literal lit : clauses)
            if(lit.isNegated()) {
                litVars.clear();
                litVals.clear();
                lit.getUniqueVariables(litVars, litVals, false);
                for(String lvar : litVars) negVars.add(lvar);
            }

        // copy original list of clauses
        // search for literal with the fewest variables;
        Literal seedLit        = null;
        int minVars            = 100;
        boolean negLitSelected = false;
        for(Literal lit : clauses) {
            if(lit.getType()==2) return; // not sure how to optimize with aggregates July 30, 2015
            if(labelPreds.contains(lit.getPredicate().getLabel()) &&
                    lit.getArgs()[1].getType() == FLogicConstants.VALUE) LbLits.add(lit);
            else oldClauses.add(lit);
            litVars.clear();
            litVals.clear();
            lit.getUniqueVariables(litVars, litVals, false);
            if(!lit.isNegated()) {
                if(litVars.size() < minVars && !negLitSelected &&
                        !systemPreds.contains(lit.getPredicate().getLabel())) {
                    seedLit = lit;
                    minVars = litVars.size();
                    for(String lvar : litVars)
                        if(negVars.contains(lvar)) {
                            negLitSelected=true;
                        }
                }
            }
            //else return; // not sure how to optimize with negated literals Dec 3, 2015
        }

        // clear original list
        clauses.clear();

        // add labels
        if(LbLits.size()>0) {
            for(Literal lit : LbLits) {
                clauses.add(lit);
                // update variables
                litVars.clear();
                litVals.clear();
                lit.getUniqueVariables(litVars, litVals, true);
                for(String lval : litVals) vals.add(lval);
                if(!lit.isNegated())
                    for(String lvar : litVars) vars.add(lvar);
            }
        }

        Literal litToAdd=null;
        while(oldClauses.size()>0) {
            litToAdd = null;
            if(clauses.size()==0) {
                litToAdd = seedLit;
            } else { // find the next best literal to add
                int maxBoundVars = 0;
                int maxBoundVals = 0;
                for(Literal lit : oldClauses) {
                    int boundVars = 0;
                    int boundVals = 0;
                    litVars.clear();
                    litVals.clear();
                    lit.getUniqueVariables(litVars, litVals, true);
                    for(String lval : litVals) if(vals.contains(lval)) boundVals++;
                    for(String lvar : litVars) if(vars.contains(lvar)) boundVars++;
                    if(boundVals > 0) {
                        if(systemPreds.contains(lit.getPredicate().getLabel())) {
                            if(boundVars == litVars.size()) {
                                litToAdd=lit;
                                break;
                            }
                        } else {
                            litToAdd=lit;
                            break;
                        }
                    }
                    if(boundVars > maxBoundVars) {
                        if(systemPreds.contains(lit.getPredicate().getLabel())) {
                            // for system predicates, all variables have to be bound
                            if(boundVars == litVars.size()) {
                                maxBoundVars=boundVars;
                                litToAdd=lit;
                            }
                        } else {
                            maxBoundVars=boundVars;
                            litToAdd=lit;
                        }
                    } else if(maxBoundVars > 0 && boundVars == maxBoundVars && hiPriorityPreds.contains(lit.getPredicate().getLabel())) {
                        maxBoundVars=boundVars;
                        litToAdd=lit;
                        break;
                    }
                }
            }
            // remove the literal from old list
            int idx = oldClauses.indexOf(litToAdd);
            if(idx == -1) {
                // not a connected graph
                connected = false;
                break;
            }
            oldClauses.remove(idx);

            // add the literal
            clauses.add(litToAdd);

            // update variables
            litVars.clear();
            litVals.clear();
            litToAdd.getUniqueVariables(litVars, litVals, true);
            for(String lval : litVals) vals.add(lval);
            if(!litToAdd.isNegated())
                for(String lvar : litVars) vars.add(lvar);
        }

        // clean up
        oldClauses.clear();
        oldClauses=null;
        vars.clear();
        vars=null;
        litVars.clear();
        litVars=null;
        vals.clear();
        vals=null;
        litVals.clear();
        litVals=null;
    }

    public void removeDuplicates() {
        int i, j, a;
        Literal lit, dlit;
        boolean match, dupl;

        for (i = 0; i < getClauses().size(); i++) {
            lit = getClauses().get(i);
            dupl = false;
            for (j = i + 1; j < getClauses().size() && !dupl; j++) {
                dlit = getClauses().get(j);
                match = true;
                if (!lit.getPredicate().getLabel().equals(dlit.getPredicate().getLabel())) {
                    match = false;
                }
                for (a = 0; a < lit.getArity() && match; a++) {
                    if (!lit.getArgs()[a].getLabel().equals(dlit.getArgs()[a].getLabel())) {
                        match = false;
                    }
                }
                if (match) {
                    dupl = true;
                }
            }
            if (dupl) {
                getClauses().remove(lit);
                i--;
                lit = null;
            }
        }
    }

    public boolean exhaustedChoicePoints() {
        if (getDataChoicePoints().isCalcChoicePoints()
                && getRuleChoicePoints().isCalcChoicePoints()
                && getDataChoicePoints().getChoicePoints().isEmpty()
                && getRuleChoicePoints().getChoicePoints().isEmpty()) {
            return true;
        }

        return false;
    }


    public String toFLogic() {
        String ss = "?- ";
        for(int i=0; i<getClauses().size(); i++) {
            if(i>0) ss += ", ";
            ss += getClauses().get(i).toFLogic();
        }
        ss += ".";
        return ss;
    }

    public String state(boolean md) { // default md=true
        String ss;
        int i;

        ss = "";
        if (md) {
            ss = "G" + getId() + ": ";
        }
        for (i = 0; i < getClauses().size(); i++) {
            ss += getClauses().get(i).toString() + " ";
        }
        if (md) {
            for (i = 0; i < getHistory().size(); i++) {
                ss += getHistory().get(i) + " ";
            }
        }
        ss += "{";
        for (i = 0; i < getBindSet().getSize(); i++) {
            ss += getBindSet().getSource()[i] + "/" + getBindSet().getDestination()[i] + " ";
        }
        ss += "}";
        if (getDataChoicePoints().getChoicePoints().size() > 0) {
            ss += "d" + getDataChoicePoints().getChoicePoints().get(0);
        }
        if (getRuleChoicePoints().getChoicePoints().size() > 0) {
            ss += "r" + getRuleChoicePoints().getChoicePoints().get(0);
        }
        if (md && getPar() != null) {
            ss += " ^ G" + getPar().getId();
        }
        return ss;
    }

    public Goal deriveNewGoalForNegation(Goal goal_to_stop, FLogicEnvironment oenv) {
        Goal newGoal;
        Literal curlit, newlit;

        curlit = this.getClauses().get(0);
        // create 2nd goal
        newGoal = new Goal();
        newGoal.setId(oenv.getAndIncrementGoal_id());
        newGoal.setPar(this);
        newlit = new Literal(curlit);
        newlit.setNegated(false);
        newGoal.getClauses().add(newlit);
        newlit = new Literal("fail", "?faila", "?failb", curlit.getGraph().getLabel());
        newGoal.getClauses().add(newlit);
        newlit.setNegBTGoal(goal_to_stop.getId());
        newGoal.getBindSet().append(this.getBindSet());
        return newGoal;
    }

    public Goal deriveNewGoalForDisjunction(Literal lit, FLogicEnvironment oenv) {
        Goal newGoal;
        Literal newLit;
        int i;

        newGoal = new Goal();
        newGoal.setPar(this);
        newGoal.setId(oenv.getAndIncrementGoal_id());

        // copy all bindings
        newGoal.getBindSet().append(this.getBindSet());
        // copy goal derivation history
        newGoal.setHistory(this.getHistory());

        newLit = new Literal(lit);
        newGoal.getClauses().add(newLit);

        // copy the clauses - 1st clause
        for (i = 1; i < this.getClauses().size(); i++) {
            newLit = new Literal(this.getClauses().get(i));
            newGoal.getClauses().add(newLit);
        }

        return newGoal;
    }

    // goal contains previous bindings
    // new bindings have to be consistent with previous bindings
    // add body of horn clause to new goal
    public Goal deriveNewGoal(HornClause hc, String extSrc, BindingSet bindSet_in,
                              ResultSet RS, HashSet<String> systemPreds, FLogicEnvironment oenv) {
        Goal newGoal;
        Literal literal, newlit, oldlit;
        int i, j;
        boolean dataBranchFnd, dupRuleFnd, destVarFnd, srcVarFnd;

        dataBranchFnd = false;
        dupRuleFnd = false;
        for (i = getHistory().size() - 1; i >= 0; i--) {
            // check for a data branch before seeing extSrc
            if (getHistory().get(i).startsWith("D")) {
                dataBranchFnd = true;
                break;
            }
            if (getHistory().get(i).equals(extSrc)) {
                dupRuleFnd = true;
                break;
            }
        }
        //if (dupRuleFnd && !dataBranchFnd) // infinite loop ?
        //{
        //    return null;
        //}

        newGoal = new Goal();
        newGoal.setPar(this);
        newGoal.setId(oenv.getAndIncrementGoal_id());

        // copy all bindings
        newGoal.getBindSet().append(getBindSet());

        newGoal.getHistory().addAll(getHistory()); // copy goal derivation history
        newGoal.getHistory().add(extSrc); // add reason for extending goal

        // insert new bindings
        for (i = 0; i < bindSet_in.getSize(); i++) {
            destVarFnd = false;
            srcVarFnd = false;
            for (j = 0; j < RS.getVariables().size(); j++) {
                if (bindSet_in.getDestination()[i].equals(RS.getVariables().get(j))) {
                    destVarFnd = true;
                }
                if (bindSet_in.getSource()[i].equals(RS.getVariables().get(j))) {
                    srcVarFnd = true;
                }
            }
            if (destVarFnd && srcVarFnd) {
                newGoal.getBindSet().addBinding(bindSet_in.getSource()[i], bindSet_in.getDestination()[i]);
            } else if (destVarFnd && !bindSet_in.getSource()[i].startsWith("?")) {
                newGoal.getBindSet().addBinding(bindSet_in.getSource()[i], bindSet_in.getDestination()[i]);
            } else if (srcVarFnd && !bindSet_in.getDestination()[i].startsWith("?")) {
                newGoal.getBindSet().addBinding(bindSet_in.getDestination()[i], bindSet_in.getSource()[i]);
            }
        }

        // copy the body of the horn clause
        for (i = 0; i < hc.body.size(); i++) {
            newlit = new Literal(hc.body.get(i));
            newGoal.getClauses().add(newlit);
        }

        // copy the clauses - 1st clause
        for (i = 1; i < getClauses().size(); i++) {
            newGoal.getClauses().add(new Literal(getClauses().get(i)));
        }

        // substitute variables with bindings from bindSet
        for (i = 0; i < newGoal.getClauses().size(); i++) {
            literal = newGoal.getClauses().get(i);
            literal.substituteBindings(bindSet_in);
        }

        // is there a solution for this goal ; all query variables bound
        if (RS.solutionExists(newGoal)) {
            newGoal = null;
            oenv.setGoal_id(oenv.getGoal_id() - 1);
            return null;
        }

        // don't move a literal in front of a "fail", "!=", "=="
        literal = newGoal.getClauses().get(0);
        if (    literal.getArgs()[0].getType() != FLogicConstants.VALUE && literal.getArgs()[1].getType() != FLogicConstants.VALUE) {
            for (i = 1; i < newGoal.getClauses().size(); i++) {
                if (!systemPreds.contains(literal.getPredicate().getLabel())) {
                    if (newGoal.getClauses().get(i).getPredicate().getLabel().equals(literal.getPredicate().getLabel())) {
                        if (newGoal.getClauses().get(i).getArgs()[0].getType() == FLogicConstants.VALUE
                                || newGoal.getClauses().get(i).getArgs()[1].getType() == FLogicConstants.VALUE) {
                            // query optimization only true for Conjunctive Queries
                            // select a clause that has at least one bound variable
                            literal = newGoal.getClauses().get(i);
                            newGoal.getClauses().remove(i);
                            newGoal.getClauses().add(0, literal);
                            break;
                        }
                    }
                }
            }
        }

        if (getClauses().size() > 0) {
            oldlit = getClauses().get(0);
            literal = newGoal.getClauses().get(0);
            if (literal.getPredicate().getLabel().equals(oldlit.getPredicate().getLabel())
                    && literal.getArity() == oldlit.getArity()
                    && literal.getArgs()[0].getLabel().equals(oldlit.getArgs()[0].getLabel())
                    && literal.getArgs()[1].getLabel().equals(oldlit.getArgs()[1].getLabel())) {
                // infinite loop
                newGoal = null;
                oenv.setGoal_id(oenv.getGoal_id() - 1);
                return null;
            }
            newGoal.removeDuplicates();
        }

        // look for ?x / ?c && value / ?x
        newGoal.getBindSet().reduce();

        return newGoal;
    }

    public Goal deriveNewGoal(String extSrc, BindingSet bindSet_in, ResultSet RS, FLogicEnvironment oenv) {
        Goal newGoal;
        Literal literal;
        int i, j;

        newGoal = new Goal();
        newGoal.setPar(this);
        newGoal.setId(oenv.getAndIncrementGoal_id());

        // copy all bindings
        newGoal.getBindSet().append(getBindSet());
        // copy goal derivation history
        newGoal.getHistory().addAll(getHistory());
        newGoal.getHistory().add(extSrc); // reason for extending goal

        // insert new bindings
        for (i = 0; i < bindSet_in.getSize(); i++) {
            for (j = 0; j < RS.getVariables().size(); j++) {
                if (!bindSet_in.getSource()[i].startsWith("?")
                        && bindSet_in.getDestination()[i].equals(RS.getVariables().get(j))) { // only save if on the variable list
                    newGoal.getBindSet().addBinding(bindSet_in.getSource()[i], bindSet_in.getDestination()[i]);
                }
            }
        }

        // copy the clauses - 1st clause
        for (i = 1; i < getClauses().size(); i++) {
            literal = new Literal(getClauses().get(i));
            literal.substituteBindings(bindSet_in);
            newGoal.getClauses().add(literal);
        }

        // is there a solution for this goal ; all query variables bound
        if (RS.solutionExists(newGoal)) {
            newGoal = null;
            oenv.setGoal_id(oenv.getGoal_id() - 1);
            return null;
        }

        newGoal.removeDuplicates();

        // look for ?x / ?c && value / ?x
        newGoal.getBindSet().reduce();

        return newGoal;
    }

    /**
     * @return the clauses
     */
    public ArrayList<Literal> getClauses() {
        return clauses;
    }

    /**
     * @param clauses the clauses to set
     */
    public void setClauses(ArrayList<Literal> clauses) {
        this.clauses = clauses;
    }

    /**
     * @return the history
     */
    public ArrayList<String> getHistory() {
        return history;
    }

    /**
     * @param history the history to set
     */
    public void setHistory(ArrayList<String> history) {
        this.history = history;
    }

    /**
     * @return the bindSet
     */
    public BindingSet getBindSet() {
        return bindSet;
    }

    /**
     * @param bindSet the bindSet to set
     */
    public void setBindSet(BindingSet bindSet) {
        this.bindSet = bindSet;
    }

    /**
     * @return the saveResults
     */
    public boolean isSaveResults() {
        return saveResults;
    }

    /**
     * @param saveResults the saveResults to set
     */
    public void setSaveResults(boolean saveResults) {
        this.saveResults = saveResults;
    }

    /**
     * @return the tabledGoal
     */
    public boolean isTabledGoal() {
        return tabledGoal;
    }

    /**
     * @param tabledGoal the tabledGoal to set
     */
    public void setTabledGoal(boolean tabledGoal) {
        this.tabledGoal = tabledGoal;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    public String getUUID() {
        if(uuid == null) uuid = UUID.randomUUID().toString();
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the ruleMode
     */
    public char getRuleMode() {
        return ruleMode;
    }

    /**
     * @param ruleMode the ruleMode to set
     */
    public void setRuleMode(char ruleMode) {
        this.ruleMode = ruleMode;
    }

    /**
     * @return the par
     */
    public Goal getPar() {
        return par;
    }

    /**
     * @param par the par to set
     */
    public void setPar(Goal par) {
        this.par = par;
    }

    /**
     * @return the dataChoicePoints
     */
    public ChoicePoint getDataChoicePoints() {
        return dataChoicePoints;
    }

    /**
     * @param dataChoicePoints the dataChoicePoints to set
     */
    public void setDataChoicePoints(ChoicePoint dataChoicePoints) {
        this.dataChoicePoints = dataChoicePoints;
    }

    /**
     * @return the ruleChoicePoints
     */
    public ChoicePoint getRuleChoicePoints() {
        return ruleChoicePoints;
    }

    /**
     * @param ruleChoicePoints the ruleChoicePoints to set
     */
    public void setRuleChoicePoints(ChoicePoint ruleChoicePoints) {
        this.ruleChoicePoints = ruleChoicePoints;
    }
}
