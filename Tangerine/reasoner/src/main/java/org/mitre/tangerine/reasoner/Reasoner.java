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

package org.mitre.tangerine.reasoner;

import java.util.List;

import org.mitre.tangerine.models.ReasonerModel;
import org.mitre.tangerine.models.ReasonerModel.Data;
import org.mitre.tangerine.models.ReasonerModel.Data.Results;
import org.mitre.tangerine.models.ReasonerModel.Data.Results.Result;

public class Reasoner {

    public ReasonerModel Reason(String configuration, String ontologies, String queries) {
        FLogicEngine flengine = new FLogicEngine("", configuration);
        FLogicEnvironment flenv = new FLogicEnvironment();
        ReasonerModel reasonerModel = new ReasonerModel();
        ResultSet RS = new ResultSet();
        RS.displayOnAdd = true;

        ReasonerModel.Data data;
        ReasonerModel.Data.Results results;
        ReasonerModel.Data.Results.Result result;
        List<Result> resultList;
        List<Results> resultsList;
        List<Data> dataList = reasonerModel.getDataList();

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
        return reasonerModel;
    }

}
