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

import java.util.ArrayList;
import java.util.List;

public class ReasonerModel {

    private List<Data> dataList;

    public ReasonerModel() {
        dataList = new ArrayList<Data>();
    }

    public List<Data> getDataList() {
        return dataList;
    }

    public void setDataList(List<Data> dataList) {
        this.dataList = dataList;
    }

    public class Data {

        private String query;
        private List<Results> resultList;

        public Data() {
            resultList = new ArrayList<Results>();
        }

        public String getQuery() {
            return query;
        }

        public List<Results> getResults() {
            return resultList;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public void setResults(List<Results> resultsList) {
            this.resultList = resultsList;
        }

        public class Results {

            private List<Result> result;

            public Results() {
                result = new ArrayList<Result>();
            }

            public List<Result> getResult() {
                return result;
            }

            public void setResult(List<Result> result) {
                this.result = result;
            }

            public class Result {

                private String key;
                private String val;

                public String getKey() {
                    return key;
                }

                public String getValue() {
                    return val;
                }

                public void setKey(String key) {
                    this.key = key;
                }

                public void setValue(String value) {
                    this.val = value;
                }
            }

        }

    }
}
