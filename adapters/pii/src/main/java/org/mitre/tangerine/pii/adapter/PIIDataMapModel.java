package org.mitre.tangerine.pii.adapter;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class PIIDataMapModel {
	List<OntologyMappings> OntologyMappings;

	public List<OntologyMappings> getOntologyMappings() {
		return OntologyMappings;
	}

	public void setOntologyMappings(List<OntologyMappings> ontologyMappings) {
		OntologyMappings = ontologyMappings;
	}

	public class OntologyMappings {

		 @SerializedName("key") private String key;
		 @SerializedName("mappings") private PIIMapping mapping;

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
			 @SerializedName("asserts") private List<Map<String, String>> asserts;

			public List<Map<String, String>> getAsserts() {
				return asserts;
			}

			public void setAsserts(List<Map<String, String>> asserts) {
				this.asserts = asserts;
			}

		}
	}
}
