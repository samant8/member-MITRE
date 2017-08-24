package org.mitre.tangerine.reasoner;
import java.util.ArrayList;

public class ChoicePoint {

	private ArrayList<Integer> choicePoints = new ArrayList<Integer>();
	private boolean calcChoicePoints = false;

	public ChoicePoint() {}

	public void release() {choicePoints.clear(); choicePoints=null;}

	/**
	 * @return the choicePoints
	 */
	public ArrayList<Integer> getChoicePoints() {
		return choicePoints;
	}

	/**
	 * @param choicePoints the choicePoints to set
	 */
	public void setChoicePoints(ArrayList<Integer> choicePoints) {
		this.choicePoints = choicePoints;
	}

	/**
	 * @return the calcChoicePoints
	 */
	public boolean isCalcChoicePoints() {
		return calcChoicePoints;
	}

	/**
	 * @param calcChoicePoints the calcChoicePoints to set
	 */
	public void setCalcChoicePoints(boolean calcChoicePoints) {
		this.calcChoicePoints = calcChoicePoints;
	}
}
