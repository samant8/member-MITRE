package org.mitre.tangerine.reasoner;
public class InheritedAssertion {

    private String instanceLbl;
    private String classLbl;
    private int distFromSrcClass;

    public InheritedAssertion() {
    }

    /**
     * @return the instanceLbl
     */
    public String getInstanceLbl() {
        return instanceLbl;
    }

    /**
     * @param instanceLbl the instanceLbl to set
     */
    public void setInstanceLbl(String instanceLbl) {
        this.instanceLbl = instanceLbl;
    }

    /**
     * @return the classLbl
     */
    public String getClassLbl() {
        return classLbl;
    }

    /**
     * @param classLbl the classLbl to set
     */
    public void setClassLbl(String classLbl) {
        this.classLbl = classLbl;
    }

    /**
     * @return the distFromSrcClass
     */
    public int getDistFromSrcClass() {
        return distFromSrcClass;
    }

    /**
     * @param distFromSrcClass the distFromSrcClass to set
     */
    public void setDistFromSrcClass(int distFromSrcClass) {
        this.distFromSrcClass = distFromSrcClass;
    }
}
