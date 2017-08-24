package org.mitre.tangerine.esri.adapter;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class EsriDataMapModel {
	List<OntologyMappings> OntologyMappings;

	public List<OntologyMappings> getOntologyMappings() {
		return OntologyMappings;
	}

	public void setOntologyMappings(List<OntologyMappings> ontologyMappings) {
		OntologyMappings = ontologyMappings;
	}

	public class OntologyMappings {

		@SerializedName("key")
		private String key;
		@SerializedName("mappings")
		private EsriMapping mapping;

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
		public EsriMapping getMapping() {
			return mapping;
		}

		/**
		 * @param mapping
		 *            the mapping to set
		 */
		public void setMapping(EsriMapping mapping) {
			this.mapping = mapping;
		}

		public class EsriMapping {
			@SerializedName("asserts")
			private List<Map<String, String>> asserts;
			@SerializedName("instances")
			private Map<String, String> instances;

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