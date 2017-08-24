package org.mitre.tangerine.reasoner;
public class Expression {

    private String sub;
    private String graph;
    private PredValue preds;
    private Expression next;
	  private Aggregate aggr; // aggregate processing

    public Expression() {
			aggr = null; // default
    }

	public void release() {
		preds.release(); preds=null;
		if(next != null) {next.release(); next=null;}
	}

    /**
     * @return the sub
     */
    public String getSub() {
        return sub;
    }

    /**
     * @param sub the sub to set
     */
    public void setSub(String sub) {
        this.sub = sub;
    }

    /**
     * @return the graph
     */
    public String getGraph() {
        return graph;
    }

    /**
     * @param graph the graph to set
     */
    public void setGraph(String graph) {
        this.graph = graph;
    }

    /**
     * @return the preds
     */
    public PredValue getPreds() {
        return preds;
    }

    /**
     * @param preds the preds to set
     */
    public void setPreds(PredValue preds) {
        this.preds = preds;
    }

    /**
     * @return the next
     */
    public Expression getNext() {
        return next;
    }

    /**
     * @param next the next to set
     */
    public void setNext(Expression next) {
        this.next = next;
    }

	public Aggregate getAggregate() {
		return aggr;
	}

  public void setAggregate(Aggregate aggr) {
		this.aggr = aggr;
	}
}
