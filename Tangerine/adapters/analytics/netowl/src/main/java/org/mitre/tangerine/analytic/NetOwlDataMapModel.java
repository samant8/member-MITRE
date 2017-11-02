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

import java.util.ArrayList;
import java.util.HashMap;

public class NetOwlDataMapModel {
    //	{OntologyMappings:[
    //		{key:"", mapping:
    //			{onto:"",eventType:"", source:"", target:"", buyer:"", company:"",
    //			seller:"", money:"", time:"", place:"", entity:"", organization:""}
    //		}
    //	],ArgumentMappings:[
    //		{key: "", mapping:{pred:"", use:""}}
    //	]}
    private ArrayList<Mappings> OntologyMappings;
    private ArrayList<Mappings> ArgumentMappings;

    /**
     * @return the ontologyMappings
     */
    public ArrayList<Mappings> getOntologyMappings() {
        return OntologyMappings;
    }

    /**
     * @param ontologyMappings the ontologyMappings to set
     */
    public void setOntologyMappings(ArrayList<Mappings> ontologyMappings) {
        OntologyMappings = ontologyMappings;
    }

    /**
     * @return the argumentMappings
     */
    public ArrayList<Mappings> getArgumentMappings() {
        return ArgumentMappings;
    }

    /**
     * @param argumentMappings the argumentMappings to set
     */
    public void setArgumentMappings(ArrayList<Mappings> argumentMappings) {
        ArgumentMappings = argumentMappings;
    }

    public class Mappings {
        private String key;
        private Mapping mapping;


        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }
        /**
         * @param key the key to set
         */
        public void setKey(String key) {
            this.key = key;
        }
        /**
         * @return the mapping
         */
        public Mapping getMapping() {
            return mapping;
        }
        /**
         * @param mapping the mapping to set
         */
        public void setMapping(Mapping mapping) {
            this.mapping = mapping;
        }
    }

    public class Mapping {
        private String onto;
        private String eventType;
        private String source;
        private String target;
        private String buyer;
        private String company;
        private String seller;
        private String money;
        private String time;
        private String place;
        private String entity;
        private String organization;
        private String pred;
        private String use;
        private String subject;

        /**
         * @return all the values inside a HashMap
         */
        public HashMap<String, String> getAll() {
            HashMap<String, String> values = new HashMap<String, String>();
            if(onto != null && !onto.isEmpty()) values.put("onto", onto);
            if(eventType != null && !eventType.isEmpty()) values.put("eventType", eventType);
            if(source != null && !source.isEmpty()) values.put("source", source);
            if(target != null && !target.isEmpty()) values.put("target", target);
            if(buyer != null && !buyer.isEmpty()) values.put("buyer", buyer);
            if(company != null && !company.isEmpty()) values.put("company", company);
            if(seller != null && !seller.isEmpty()) values.put("seller", seller);
            if(money != null && !money.isEmpty()) values.put("money", money);
            if(time != null && !time.isEmpty()) values.put("time", time);
            if(place != null && !place.isEmpty()) values.put("place", place);
            if(entity != null && !entity.isEmpty()) values.put("entity", entity);
            if(organization != null && !organization.isEmpty()) values.put("organization", organization);
            if(pred != null && !pred.isEmpty()) values.put("pred", pred);
            if(use != null && !use.isEmpty()) values.put("use", use);
            if(subject != null && !subject.isEmpty()) values.put("subject", subject);
            return values;
        }
        /**
         * @return the onto
         */
        public String getOnto() {
            return onto;
        }
        /**
         * @param onto the onto to set
         */
        public void setOnto(String onto) {
            this.onto = onto;
        }
        /**
         * @return the envetType
         */
        public String getEventType() {
            return eventType;
        }
        /**
         * @param envetType the envetType to set
         */
        public void setEventType(String envetType) {
            this.eventType = envetType;
        }
        /**
         * @return the source
         */
        public String getSource() {
            return source;
        }
        /**
         * @param source the source to set
         */
        public void setSource(String source) {
            this.source = source;
        }
        /**
         * @return the target
         */
        public String getTarget() {
            return target;
        }
        /**
         * @param target the target to set
         */
        public void setTarget(String target) {
            this.target = target;
        }
        /**
         * @return the buyer
         */
        public String getBuyer() {
            return buyer;
        }
        /**
         * @param buyer the buyer to set
         */
        public void setBuyer(String buyer) {
            this.buyer = buyer;
        }
        /**
         * @return the company
         */
        public String getCompany() {
            return company;
        }
        /**
         * @param company the company to set
         */
        public void setCompany(String company) {
            this.company = company;
        }
        /**
         * @return the seller
         */
        public String getSeller() {
            return seller;
        }
        /**
         * @param seller the seller to set
         */
        public void setSeller(String seller) {
            this.seller = seller;
        }
        /**
         * @return the money
         */
        public String getMoney() {
            return money;
        }
        /**
         * @param money the money to set
         */
        public void setMoney(String money) {
            this.money = money;
        }
        /**
         * @return the time
         */
        public String getTime() {
            return time;
        }
        /**
         * @param time the time to set
         */
        public void setTime(String time) {
            this.time = time;
        }
        /**
         * @return the place
         */
        public String getPlace() {
            return place;
        }
        /**
         * @param place the place to set
         */
        public void setPlace(String place) {
            this.place = place;
        }
        /**
         * @return the entity
         */
        public String getEntity() {
            return entity;
        }
        /**
         * @param entity the entity to set
         */
        public void setEntity(String entity) {
            this.entity = entity;
        }
        /**
         * @return the organization
         */
        public String getOrganization() {
            return organization;
        }
        /**
         * @param organization the organization to set
         */
        public void setOrganization(String organization) {
            this.organization = organization;
        }
        /**
         * @return the pred
         */
        public String getPred() {
            return pred;
        }
        /**
         * @param pred the pred to set
         */
        public void setPred(String pred) {
            this.pred = pred;
        }
        /**
         * @return the use
         */
        public String getUse() {
            return use;
        }
        /**
         * @param use the use to set
         */
        public void setUse(String use) {
            this.use = use;
        }
        public String getSubject() {
            return subject;
        }
        public void setSubject(String subject) {
            this.subject = subject;
        }
    }
}


