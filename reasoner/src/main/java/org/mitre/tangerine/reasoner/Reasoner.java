package org.mitre.tangerine.reasoner;

import java.util.ArrayList;

import org.mitre.tangerine.reasoner.ReasonerModel.Data;
import org.mitre.tangerine.reasoner.ReasonerModel.Data.Results;
import org.mitre.tangerine.reasoner.ReasonerModel.Data.Results.Result;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Reasoner {

	public Reasoner() {
	}

	public String Reason(String configuration, String ontologies, String queries, String type) {
		FLogicEngine flengine = new FLogicEngine("", configuration);
		FLogicEnvironment flenv = new FLogicEnvironment();
		ReasonerModel reasonerModel = new ReasonerModel();
		ResultSet RS = new ResultSet();
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();

		RS.displayOnAdd = true;
		ReasonerModel.Data data;
		ReasonerModel.Data.Results results;
		ReasonerModel.Data.Results.Result result;

		ArrayList<Data> dataList = reasonerModel.getDataList();
		ArrayList<Results> resultsList;
		ArrayList<Result> resultList;

		// ###output data type will go here###
		flengine.loadOntology(ontologies);
		String[] input = queries.split("\n");
		for (String goal : input) {
			if (!goal.trim().equals("") && goal.length() > 2 && !goal.startsWith("//")) {
				data = reasonerModel.new Data();
				data.setQuery(goal);
				resultsList = data.getResults();
				flengine.evaluate(goal, RS, flenv);
				for (int i = 0; i < RS.getTuples().size(); i++) {
					Tuple tpl = RS.getTuples().get(i);
					results = data.new Results();
					resultList = results.getResult();
					for (int j = 0; j < RS.getVariables().size(); j++) {
						result = results.new Result();
						result.setKey(RS.getVariables().get(j));
						result.setValue(tpl.getCol()[j]);
						resultList.add(result);
					}
					results.setResult(resultList);
					resultsList.add(results);
				}
				data.setResults(resultsList);
				dataList.add(data);
			}
		}
		reasonerModel.setDataList(dataList);

		flengine.close();
		return gson.toJson(reasonerModel);
	}
}
