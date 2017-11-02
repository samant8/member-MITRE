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

public class VoucherDataMapModel {
    List<OntologyMappings> OntologyMappings;

    public List<OntologyMappings> getOntologyMappings() {
        return OntologyMappings;
    }

    public void setOntologyMappings(List<OntologyMappings> ontologyMappings) {
        OntologyMappings = ontologyMappings;
    }

    public class OntologyMappings {

        @SerializedName("key") private String key;
        @SerializedName("mappings") private VoucherMapping mapping;

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
        public VoucherMapping getMapping() {
            return mapping;
        }

        /**
         * @param mapping
         *            the mapping to set
         */
        public void setMapping(VoucherMapping mapping) {
            this.mapping = mapping;
        }

        public class VoucherMapping {
            @SerializedName("asserts") private List<Map<String, String>> asserts;
            @SerializedName("instances") private Map<String, String> instances;

            public List<Map<String, String>> getAsserts() {
                return asserts;
            }

            public void setAsserts(List<Map<String, String>> asserts) {
                this.asserts = asserts;
            }

            public Map<String, String> getInstances() {
                return instances;
            }

            public void setInstances(Map<String, String> instances) {
                this.instances = instances;
            }

        }
    }
}
