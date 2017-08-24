package org.mitre.tangerine.reasoner;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;

/// To run
/// setenv CLASSPATH ./FLEngine.jar:./log4j-1.2.15.jar:./cup-0.11a.jar:./
/// java QueryEngine -o <ontology.flr> -d <kb.flr> -q <qry.flr>

public class QueryEngine {

	static FLogicEngine flengine;
	static FLogicEnvironment flenv = null;
	
	public static void main(String args[]) {
		String FLConfigFile = "flquery-config.json";
		ArrayList<String> Onts = new ArrayList<String>();
		ArrayList<String> Data = new ArrayList<String>();

		String QryFile=null;
		boolean streamMode = false;
		int i=0;
		
		while(i < args.length) {
			if(args[i].startsWith("-")) {
				String arg = args[i];
				if(arg.equals("-o")) Onts.add(args[i+1]);
				else if(arg.equals("-d")) Data.add(args[i+1]);
				else if(arg.equals("-q")) QryFile = args[i+1];
				else if(arg.equals("-s")) {
					if(args[i+1].equals("true")) streamMode = true;
				}
				else if(arg.equals("-f")) FLConfigFile = args[i+1];
				i += 2;
			}
		}

		// configure F-Logic environment
		Map<String, String> env = System.getenv();
		if(env.containsKey("DB")) {
			flengine = new FLogicEngine(env.get("DB"), FLConfigFile);
		}

		// load ontology files
		for(String filename : Onts) flengine.loadOntology(new File(filename));

		// Prepare the knowledge base
		flenv = new FLogicEnvironment();

		// load kb
		for(String filename : Data) flengine.loadKB(new File(filename), flenv);

		flengine.setStreamMode(streamMode);

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(QryFile))));
			String goal;
			ResultSet RS = new ResultSet(); RS.displayOnAdd = true;
			while ((goal = br.readLine()) != null) {
				if (goal.length() > 2 && !goal.startsWith("//")) {
					System.out.println(goal);
					flengine.evaluate(goal, RS, flenv);
					//Goal g = flengine.parseGoal(goal, flenv);
					//flengine.evaluate(g, RS, flenv);
					//System.out.println(RS.toJSONString(goal, flenv));
					RS.displayPerformance();
					System.out.println(" MGTime:" + flenv.mgTim + " ESTime:" + flenv.esTim + " PYTime:" + flenv.pyTim + " KB Size:" + flenv.getKB().kb.size());
					System.out.println();
				}
			}
			br.close();
		}
		catch (Exception e) {e.printStackTrace();}

		flengine.close();
	}
}
