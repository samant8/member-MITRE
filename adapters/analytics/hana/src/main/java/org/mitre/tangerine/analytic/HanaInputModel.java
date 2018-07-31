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



package org.mitre.tangerine.analytic;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class HanaInputModel {
    @SerializedName("d")
    private Results d;

    public Results getD() {
        return d;
    }

    public void setD(Results d) {
        this.d = d;
    }

    public class Results {
        @SerializedName("results")
        private List<Result> results;

        public List<Result> getResults() {
            return results;
        }

        public void setResults(List<Result> results) {
            this.results = results;
        }

        public class Result {
            @SerializedName("ID")
            private String ID;
            @SerializedName("GWNO")
            private String GWNO;
            @SerializedName("EVENT_ID_CNTY")
            private String EVENT_ID_CNTY;
            @SerializedName("EVENT_ID_NO_CNTY")
            private String EVENT_ID_NO_CNTY;
            @SerializedName("EVENT_DATE")
            private String EVENT_DATE;
            @SerializedName("YEAR")
            private String YEAR;
            @SerializedName("TIME_PRECISION")
            private String TIME_PRECISION;
            @SerializedName("EVENT_TYPE")
            private String EVENT_TYPE;
            @SerializedName("ACTOR1")
            private String ACTOR1;
            @SerializedName("ALLY_ACTOR_1")
            private String ALLY_ACTOR_1;
            @SerializedName("INTER1")
            private String INTER1;
            @SerializedName("ACTOR2")
            private String ACTOR2;
            @SerializedName("ALLY_ACTOR_2")
            private String ALLY_ACTOR_2;
            @SerializedName("INTER2")
            private String INTER2;
            @SerializedName("INTERACTION")
            private String INTERACTION;
            @SerializedName("COUNTRY")
            private String COUNTRY;
            @SerializedName("ADMIN1")
            private String ADMIN1;
            @SerializedName("ADMIN2")
            private String ADMIN2;
            @SerializedName("ADMIN3")
            private String ADMIN3;
            @SerializedName("LOCATION")
            private String LOCATION;
            @SerializedName("LATITUDE")
            private String LATITUDE;
            @SerializedName("LONGITUDE")
            private String LONGITUDE;
            @SerializedName("GEO_PRECISION")
            private String GEO_PRECISION;
            @SerializedName("SOURCE")
            private String SOURCE;
            @SerializedName("NOTES")
            private String NOTES;
            @SerializedName("FATALITIES")
            private String FATALITIES;
            @SerializedName("GEO_URL")
            private String GEO_URL;
            @SerializedName("RC_JSON")
            private String RC_JSON;

            public String getID() {
                return ID;
            }

            public void setID(String iD) {
                ID = iD;
            }

            public String getGWNO() {
                return GWNO;
            }

            public void setGWNO(String gWNO) {
                GWNO = gWNO;
            }

            public String getEVENT_ID_CNTY() {
                return EVENT_ID_CNTY;
            }

            public void setEVENT_ID_CNTY(String eVENT_ID_CNTY) {
                EVENT_ID_CNTY = eVENT_ID_CNTY;
            }

            public String getEVENT_ID_NO_CNTY() {
                return EVENT_ID_NO_CNTY;
            }

            public void setEVENT_ID_NO_CNTY(String eVENT_ID_NO_CNTY) {
                EVENT_ID_NO_CNTY = eVENT_ID_NO_CNTY;
            }

            public String getEVENT_DATE() {
                return EVENT_DATE;
            }

            public void setEVENT_DATE(String eVENT_DATE) {
                EVENT_DATE = eVENT_DATE;
            }

            public String getYEAR() {
                return YEAR;
            }

            public void setYEAR(String yEAR) {
                YEAR = yEAR;
            }

            public String getTIME_PRECISION() {
                return TIME_PRECISION;
            }

            public void setTIME_PRECISION(String tIME_PRECISION) {
                TIME_PRECISION = tIME_PRECISION;
            }

            public String getEVENT_TYPE() {
                return EVENT_TYPE;
            }

            public void setEVENT_TYPE(String eVENT_TYPE) {
                EVENT_TYPE = eVENT_TYPE;
            }

            public String getACTOR1() {
                return ACTOR1;
            }

            public void setACTOR1(String aCTOR1) {
                ACTOR1 = aCTOR1;
            }

            public String getALLY_ACTOR_1() {
                return ALLY_ACTOR_1;
            }

            public void setALLY_ACTOR_1(String aLLY_ACTOR_1) {
                ALLY_ACTOR_1 = aLLY_ACTOR_1;
            }

            public String getINTER1() {
                return INTER1;
            }

            public void setINTER1(String iNTER1) {
                INTER1 = iNTER1;
            }

            public String getACTOR2() {
                return ACTOR2;
            }

            public void setACTOR2(String aCTOR2) {
                ACTOR2 = aCTOR2;
            }

            public String getALLY_ACTOR_2() {
                return ALLY_ACTOR_2;
            }

            public void setALLY_ACTOR_2(String aLLY_ACTOR_2) {
                ALLY_ACTOR_2 = aLLY_ACTOR_2;
            }

            public String getINTER2() {
                return INTER2;
            }

            public void setINTER2(String iNTER2) {
                INTER2 = iNTER2;
            }

            public String getINTERACTION() {
                return INTERACTION;
            }

            public void setINTERACTION(String iNTERACTION) {
                INTERACTION = iNTERACTION;
            }

            public String getCOUNTRY() {
                return COUNTRY;
            }

            public void setCOUNTRY(String cOUNTRY) {
                COUNTRY = cOUNTRY;
            }

            public String getADMIN1() {
                return ADMIN1;
            }

            public void setADMIN1(String aDMIN1) {
                ADMIN1 = aDMIN1;
            }

            public String getADMIN2() {
                return ADMIN2;
            }

            public void setADMIN2(String aDMIN2) {
                ADMIN2 = aDMIN2;
            }

            public String getADMIN3() {
                return ADMIN3;
            }

            public void setADMIN3(String aDMIN3) {
                ADMIN3 = aDMIN3;
            }

            public String getLOCATION() {
                return LOCATION;
            }

            public void setLOCATION(String lOCATION) {
                LOCATION = lOCATION;
            }

            public String getLATITUDE() {
                return LATITUDE;
            }

            public void setLATITUDE(String lATITUDE) {
                LATITUDE = lATITUDE;
            }

            public String getLONGITUDE() {
                return LONGITUDE;
            }

            public void setLONGITUDE(String lONGITUDE) {
                LONGITUDE = lONGITUDE;
            }

            public String getGEO_PRECISION() {
                return GEO_PRECISION;
            }

            public void setGEO_PRECISION(String gEO_PRECISION) {
                GEO_PRECISION = gEO_PRECISION;
            }

            public String getSOURCE() {
                return SOURCE;
            }

            public void setSOURCE(String sOURCE) {
                SOURCE = sOURCE;
            }

            public String getNOTES() {
                return NOTES;
            }

            public void setNOTES(String nOTES) {
                NOTES = nOTES;
            }

            public String getFATALITIES() {
                return FATALITIES;
            }

            public void setFATALITIES(String fATALITIES) {
                FATALITIES = fATALITIES;
            }

            public String getGEO_URL() {
                return GEO_URL;
            }

            public void setGEO_URL(String gEO_URL) {
                GEO_URL = gEO_URL;
            }

            public String getRC_JSON() {
                return RC_JSON;
            }

            public void setRC_JSON(String rC_JSON) {
                RC_JSON = rC_JSON;
            }

            public class __metadata {
                @SerializedName("type")
                private String type;
                @SerializedName("uri")
                private String uri;

                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public String getUri() {
                    return uri;
                }

                public void setUri(String uri) {
                    this.uri = uri;
                }
            }
        }
    }
}
