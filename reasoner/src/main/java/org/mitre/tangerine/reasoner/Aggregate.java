package org.mitre.tangerine.reasoner;
public class Aggregate {

	private String method; // aggregate method
	private Expression expr; // query to evaluate
	private String variable; // variable to use as basis for method

	public Aggregate() {
	}

	public void setMethod(String m) {this.method = m;}
	public String getMethod() {return method;}

	public void setExpressions(Expression e) {this.expr = e;}
	public Expression getExpression() {return expr;}

	public void setVariable(String v) {this.variable = v;}
	public String getVariable() {return variable;}
}
