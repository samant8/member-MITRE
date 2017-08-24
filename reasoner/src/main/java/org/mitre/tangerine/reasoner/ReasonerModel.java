package org.mitre.tangerine.reasoner;

import java.util.ArrayList;

public class ReasonerModel {

	public class Data {

		public class Results {

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

			/////////////////////////////////
			private ArrayList<Result> result;

			public Results() {
				result = new ArrayList<Result>();
			}

			public ArrayList<Result> getResult() {
				return result;
			}

			public void setResult(ArrayList<Result> result) {
				this.result = result;
			}
		}

		/////////////////////////////////
		private String query;
		private ArrayList<Results> resultList;

		public Data() {
			resultList = new ArrayList<Results>();
		}

		public String getQuery() {
			return query;
		}

		public ArrayList<Results> getResults() {
			return resultList;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public void setResults(ArrayList<Results> resultsList) {
			this.resultList = resultsList;
		}
	}

	/////////////////////////////////
	private ArrayList<Data> dataList;

	public ReasonerModel() {
		dataList = new ArrayList<Data>();
	}

	public ArrayList<Data> getDataList() {
		return dataList;
	}

	public void setDataList(ArrayList<Data> dataList) {
		this.dataList = dataList;
	}
}
