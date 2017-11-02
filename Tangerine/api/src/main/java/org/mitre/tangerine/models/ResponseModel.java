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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ResponseModel {
    private boolean error;
    private String message;
    private String dataType;
    private String collection;
    private String datamap;
    private String errorMessage;
    private int numberOfAssertions;
    private List<String> elements;
    private List<String> collections;
    // TODO change type to AssertionModel
    private List<String> outputs;
    private List<String> analytics;
    private List<AssertionModel> assertions;
    // predefine the adapter call names (use enum)


    public ResponseModel() {
        setError(false);
        message = "";
        dataType = "";
        collection = "";
        errorMessage = "";
        setDatamap("");
        numberOfAssertions = 0;

        analytics = new ArrayList<String>();
        outputs = new ArrayList<String>();
        elements = new ArrayList<String>();
        collections = new ArrayList<String>();
        assertions = new ArrayList<AssertionModel>();

    }

    public List<AssertionModel> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<AssertionModel> asserts) {
        this.assertions = asserts;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public boolean hasError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getNumberOfElements() {
        return numberOfAssertions;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfAssertions = numberOfElements;
    }

    public List<String> getElements() {
        return elements;
    }

    public void setElements(List<String> elements) {
        this.elements = elements;
    }

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> list) {
        this.collections = list;
    }

    public void addCollection(String collection) {
        this.collections.add(collection);
    }

    public void addElements(String element) {
        this.elements.add(element);
    }

    public void addAssertion(AssertionModel assertion) {
        this.assertions.add(assertion);
    }

    public List<String> getAnalytics() {
        return analytics;
    }

    public void setAnalytics(List<String> analytics) {
        this.analytics = analytics;
    }
    public void addAnalytics(String analytic) {
        this.analytics.add(analytic);
    }
    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }
    public void addOutputs(String analytic) {
        this.outputs.add(analytic);
    }


    public String getDatamap() {
        return datamap;
    }

    public void setDatamap(String datamap) {
        this.datamap = datamap;
    }

}
