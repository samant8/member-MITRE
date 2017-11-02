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

public class HanaDataMapModel {
    @SerializedName("OntologyMappings")
    List<OntologyMappings> OntologyMappings;
    @SerializedName("EvaluationOrder")
    List<String> EvaluationOrder;
    @SerializedName("Events")
    private Map<String, String> Events;
    @SerializedName("Actor")
    private Map<String, String> Actor;
    @SerializedName("InteractionCode")
    private Map<String, String> InteractionCode;

    public List<OntologyMappings> getOntologyMappings() {
        return OntologyMappings;
    }

    public void setOntologyMappings(List<OntologyMappings> ontologyMappings) {
        OntologyMappings = ontologyMappings;
    }

    public List<String> getEvaluationOrder() {
        return EvaluationOrder;
    }

    public void setEvaluationOrder(List<String> evaluationOrder) {
        EvaluationOrder = evaluationOrder;
    }

    public Map<String, String> getEvents() {
        return Events;
    }

    public void setEvents(Map<String, String> events) {
        Events = events;
    }

    public Map<String, String> getActor() {
        return Actor;
    }

    public void setActor(Map<String, String> actor) {
        Actor = actor;
    }

    public Map<String, String> getInteractionCode() {
        return InteractionCode;
    }

    public void setInteractionCode(Map<String, String> interactionCode) {
        InteractionCode = interactionCode;
    }

    public class OntologyMappings {
        @SerializedName("key")
        private String key;
        @SerializedName("mappings")
        private HanaMapping mapping;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public HanaMapping getMapping() {
            return mapping;
        }

        public void setMapping(HanaMapping mapping) {
            this.mapping = mapping;
        }

        public class HanaMapping {
            @SerializedName("assertions")
            private List<Map<String, String>> assertions;
            @SerializedName("instances")
            private Map<String, String> instances;
            @SerializedName("instanceID")
            private Map<String, String> instanceID;
            @SerializedName("variables")
            private Map<String, String> variables;

            public List<Map<String, String>> getAssertions() {
                return assertions;
            }

            public void setAssertions(List<Map<String, String>> assertions) {
                this.assertions = assertions;
            }

            public Map<String, String> getInstances() {
                return instances;
            }

            public void setInstances(Map<String, String> instances) {
                this.instances = instances;
            }

            public Map<String, String> getInstanceID() {
                return instanceID;
            }

            public void setInstanceID(Map<String, String> instanceID) {
                this.instanceID = instanceID;
            }

            public Map<String, String> getVariables() {
                return variables;
            }

            public void setVariables(Map<String, String> variables) {
                this.variables = variables;
            }
        }
    }
}
