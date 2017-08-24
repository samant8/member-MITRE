package org.mitre.tangerine.reasoner;
public class Tuple {

    private String[] col = new String[FLogicConstants.MAX_QUERY_VARS];

    public Tuple() {
    }

    /**
     * @return the col
     */
    public String[] getCol() {
        return col;
    }

    /**
     * @param col the col to set
     */
    public void setCol(String[] col) {
        this.col = col;
    }
}
