package org.mitre.tangerine.reasoner;
public class PredValue {

    private String type;
    private String predicate;
    private String[] args = new String[FLogicConstants.MAX_ARITY];;
    private int disjuncts;
    private PredValue conj_next;
    private PredValue disj_next;

    public PredValue() {}

	public void release() {
		args=null;
		if(conj_next != null) {conj_next.release(); conj_next=null;}
		if(disj_next != null) {disj_next.release(); disj_next=null;}
	}

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the predicate
     */
    public String getPredicate() {
        return predicate;
    }

    /**
     * @param predicate the predicate to set
     */
    public void setPredicate(String predicate) {
        this.predicate = predicate;
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
     * @return the disjuncts
     */
    public int getDisjuncts() {
        return disjuncts;
    }

    /**
     * @param disjuncts the disjuncts to set
     */
    public void setDisjuncts(int disjuncts) {
        this.disjuncts = disjuncts;
    }

    /**
     * @return the conj_next
     */
    public PredValue getConj_next() {
        return conj_next;
    }

    /**
     * @param conj_next the conj_next to set
     */
    public void setConj_next(PredValue conj_next) {
        this.conj_next = conj_next;
    }

    /**
     * @return the disj_next
     */
    public PredValue getDisj_next() {
        return disj_next;
    }

    /**
     * @param disj_next the disj_next to set
     */
    public void setDisj_next(PredValue disj_next) {
        this.disj_next = disj_next;
    }
}
