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
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

public class GoalStack {

    private LinkedHashMap<String, Boolean> previousGoals = new LinkedHashMap<String, Boolean>();
    private Deque<Goal> goalsToEval = new LinkedList<Goal>();
    private BitSet invalidGoals = new BitSet();

    public String tabPred;
    public int tabPredArity;

    // Linear Tabling
    public ArrayList<Goal> LT_Goals   = new ArrayList<Goal>();
    public ArrayList<ResultSet> LT_RS = new ArrayList<ResultSet>();
    public int curGoalIdx; // which goal in LT_Goals to process
    public boolean isLT;

    public GoalStack() {
        curGoalIdx=0;
        isLT=false;
    }

    public boolean addNewGoal(Goal g) {
        String st;

        if (g == null) {
            return false;
        }

        st = g.state(false);

        if (getPreviousGoals().containsKey(st)) {
            g = null;
            return false;
        } else {
            getGoalsToEval().addFirst(g);
            getPreviousGoals().put(st, true);
            return true;
        }
    }

    public Goal getTopGoal() {
        return getGoalsToEval().peekFirst();
    }

    public void removeTopGoal() {
        Goal g = getGoalsToEval().removeFirst();
        g = null;
    }

    public void clearHistory() {
        getInvalidGoals().clear();
        getPreviousGoals().clear();
    }

    public void invalidateGoal(int gid) {
        getInvalidGoals().set(gid);
    }

    public void invalidateGoal(Goal g) {
        getInvalidGoals().set(g.getId());
    }

    /**
     * @return the previousGoals
     */
    public HashMap<String, Boolean> getPreviousGoals() {
        return previousGoals;
    }

    /**
     * @param previousGoals the previousGoals to set
     */
    public void setPreviousGoals(LinkedHashMap<String, Boolean> previousGoals) {
        this.previousGoals = previousGoals;
    }

    /**
     * @return the goalsToEval
     */
    public Deque<Goal> getGoalsToEval() {
        return goalsToEval;
    }

    /**
     * @param goalsToEval the goalsToEval to set
     */
    public void setGoalsToEval(Deque<Goal> goalsToEval) {
        this.goalsToEval = goalsToEval;
    }

    /**
     * @return the invalidGoals
     */
    public BitSet getInvalidGoals() {
        return invalidGoals;
    }

    /**
     * @param invalidGoals the invalidGoals to set
     */
    public void setInvalidGoals(BitSet invalidGoals) {
        this.invalidGoals = invalidGoals;
    }

    // Linear Tabling

    public void addNewTabledGoal(Literal lit) {
        if(lit == null) return;
        for(Goal lt_goal : LT_Goals) if(lit == lt_goal.getClauses().get(0)) return;

        // create the goal
        Literal newlit = new Literal(lit);
        Goal newgoal   = new Goal();
        newgoal.getClauses().add(newlit);
        newgoal.setId(0);
        LT_Goals.add(newgoal);

        // create the result set
        ResultSet newrs   = new ResultSet();
        newrs.setIterations(0);
        newrs.setGoalCount(0);
        LT_RS.add(newrs); // goal and result should have same index in vectors

        // create variables
        StringBuffer SB        = new StringBuffer();
        ArrayList<String> args = new ArrayList<String>();
        for(int i=0; i<newlit.getArity(); i++) {
            if(newlit.getArgs()[i].getType() == FLogicConstants.VAR &&
                    newlit.getArgs()[i].getLabel().charAt(1) != '_')
                newrs.addVariable( newlit.getArgs()[i].getLabel() );
        }
    }

    public Goal getNextTabledGoal() {
        curGoalIdx++;
        if(curGoalIdx >= LT_Goals.size()) curGoalIdx=0;
        clearHistory();
        Goal goal = LT_Goals.get(curGoalIdx);
        addNewGoal(goal);

        return goal;
    }

    public ResultSet getNextTabledGoalRS() {
        ResultSet res = LT_RS.get(curGoalIdx);

        return res;
    }

    public int getTabledGoalResultsCnt() {
        int res_cnt = 0;
        for(int r=0; r<LT_RS.size(); r++) res_cnt += LT_RS.get(r).getTuples().size();
        return res_cnt;
    }

    public void clearTabledGoals() {
        LT_Goals.clear();
        LT_RS.clear();
    }

    public boolean isRecursiveCall(Goal curGoal) {
        Literal lit = curGoal.getClauses().get(0);
        boolean recursive = false;

        for(int i=0; i<LT_Goals.size() && !recursive; i++) {
            Literal origLit = LT_Goals.get(i).getClauses().get(0);
            if(lit != origLit &&
                    lit.getPredicate().getLabel().equals(origLit.getPredicate().getLabel()) &&
                    lit.getArity() == origLit.getArity()) {
                // Are the arguments subsumed
                boolean subSumed = true;
                for(int a=0; a<origLit.getArity(); a++) {
                    if(origLit.getArgs()[a].getType() == FLogicConstants.VAR ||
                            (origLit.getArgs()[a].getType() == FLogicConstants.VALUE &&
                             origLit.getArgs()[a].getLabel().equals(lit.getArgs()[a].getLabel())));
                    else subSumed = false;
                }
                recursive = subSumed;
            }
        }
        return recursive;
    }
}
