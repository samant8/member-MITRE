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
	private String errorMessage;
	private int numberOfAssertions;
	private List<String> elements;
	private List<String> collections;
	// TODO change type to AssertionModel
	private List<AssertionModel> assertions;
	// predefine the adapter call names (use enum)
	

	public ResponseModel(){
		setError(false);
		message = "";
		dataType = "";
		collection = "";
		errorMessage = "";
		numberOfAssertions = 0;
		
		elements = null;
		collections = null;
		assertions = null;
		
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

	public boolean isError() {
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
		if(this.collections == null)
			this.collections = new ArrayList<String>();
		this.collections.add(collection);
	}
	
	public void addElements(String element) {
		if(this.elements == null)
			this.elements = new ArrayList<String>();
		this.elements.add(element);
	}
	
	public void addAssertion(AssertionModel assertion) {
		if(this.assertions == null)
			this.assertions = new ArrayList<AssertionModel>();
		this.assertions.add(assertion);
	}
	
}