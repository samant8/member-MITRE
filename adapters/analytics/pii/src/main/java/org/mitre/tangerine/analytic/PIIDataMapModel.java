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

public class PIIDataMapModel {
    List<OntologyMappings> OntologyMappings;
    List<ValueMappings> ValueMappings;

    public List<OntologyMappings> getOntologyMappings() {
        return OntologyMappings;
    }

    public void setOntologyMappings(List<OntologyMappings> ontologyMappings) {
        OntologyMappings = ontologyMappings;
    }

    public List<ValueMappings> getValueMappings() {
        return ValueMappings;
    }

    public void setValueMappings(List<ValueMappings> valueMappings) {
        ValueMappings = valueMappings;
    }

    public class OntologyMappings {

        @SerializedName("key")
        private String key;
        @SerializedName("mappings")
        private PIIMapping mapping;

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @param key
         *            the key to set
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * @return the mapping
         */
        public PIIMapping getMapping() {
            return mapping;
        }

        /**
         * @param mapping
         *            the mapping to set
         */
        public void setMapping(PIIMapping mapping) {
            this.mapping = mapping;
        }

        public class PIIMapping {
            @SerializedName("asserts")
            private List<Map<String, String>> asserts;

            public List<Map<String, String>> getAsserts() {
                return asserts;
            }

            public void setAsserts(List<Map<String, String>> asserts) {
                this.asserts = asserts;
            }

        }
    }

    public class ValueMappings {

        @SerializedName("key")
        private String key;
        @SerializedName("mappings")
        private Map<String, String> mappings;

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @param key
         *            the key to set
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * @return the value
         */
        public Map<String, String> getMapping() {
            return mappings;
        }

        /**
         * @param value
         *            the value to set
         */
        public void setMapping(Map<String, String> mappings) {
            this.mappings = mappings;
        }
    }
}
