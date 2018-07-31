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


package org.mitre.tangerine.models;

public class AssertionModel {

    private A A;
    private String geoloc[];
    private String V[];

    public enum ASSERT_TYPE {
        DATA, OBJECT
    }

    public AssertionModel() {
        this.A = new A();
        this.geoloc = this.V = null;
    }

    public AssertionModel(String s, String p, String od, ASSERT_TYPE typ) {
        this.A = new A();
        this.geoloc = this.V = null;
        switch (typ) {
        case DATA:
            this.A.setSPD(s, p, od);
            break;
        case OBJECT:
            this.A.setSPO(s, p, od);
            break;
        }
    }

    public AssertionModel(String s, String p, String od, String lat, String lon) {
        this.A = new A();
        this.V = null;
        this.A.setSPO(s, p, od);
        geoloc = new String[2];
        geoloc[0] = lat;
        geoloc[1] = lon;
    }

    public void setSPO(String s, String p, String o) {
        this.A.setSPO(s, p, o);

    }

    public void setSPD(String s, String p, String d) {
        this.A.setSPD(s, p, d);
    }

    public void setGeoloc(String lat, String lon) {
        geoloc = new String[2];
        geoloc[0] = lat;
        geoloc[1] = lon;
    }

    public void setV(String rel_id, String file) {
        V = new String[2];
        V[0] = rel_id;
        V[1] = file;
    }

    public String[] getGeoloc() {
        return geoloc;
    }

    public String[] getV() {
        return V;
    }

    public A getA() {
        return A;
    }
    public void setA(A A) {
        this.A = A;
    }

    public static class A {

        private String S;
        private String P;
        private String O;
        private String D;

        public A() {
        }

        public void setSPO(String s, String p, String o) {
            this.setS(s);
            this.setP(p);
            this.setO(o);
        }

        public void setSPD(String s, String p, String d) {
            this.setS(s);
            this.setP(p);
            this.setD(d);
        }

        public void setP(String p) {
            this.P = p;
        }

        public void setS(String s) {
            this.S = s;
        }

        public void setO(String o) {
            this.O = o;
        }

        public void setD(String d) {
            this.D = d;
        }

        public String getS() {
            return S;
        }

        public String getP() {
            return P;
        }

        public String getO() {
            return O;
        }

        public String getD() {
            return D;
        }

    }
}
