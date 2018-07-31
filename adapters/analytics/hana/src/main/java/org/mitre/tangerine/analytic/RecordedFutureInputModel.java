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
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class RecordedFutureInputModel {
    @SerializedName("reference")
    private Reference reference;

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public class Reference {
        @SerializedName("time_range")
        private String time_range;
        @SerializedName("function")
        private List<String> function;
        @SerializedName("limit")
        private String limit;
        @SerializedName("attributes")
        private List<Attributes> attributes;
        @SerializedName("type")
        private List<String> type;

        public String getTime_range() {
            return time_range;
        }

        public void setTime_range(String time_range) {
            this.time_range = time_range;
        }

        public List<String> getFunction() {
            return function;
        }

        public void setFunction(List<String> function) {
            this.function = function;
        }

        public String getLimit() {
            return limit;
        }

        public void setLimit(String limit) {
            this.limit = limit;
        }

        public List<Attributes> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<Attributes> attributes) {
            this.attributes = attributes;
        }

        public List<String> getType() {
            return type;
        }

        public void setType(List<String> type) {
            this.type = type;
        }

        public class Attributes {
            @SerializedName("entity")
            private Map<String, List<String>> entity;
            @SerializedName("exists")
            private String exists;

            public Map<String, List<String>> getEntity() {
                return entity;
            }

            public void setEntity(Map<String, List<String>> entity) {
                this.entity = entity;
            }

            public String getExists() {
                return exists;
            }

            public void setExists(String exists) {
                this.exists = exists;
            }
        }
    }
}
