package org.mitre.tangerine.reasoner;
public final class FLogicConstants {
	public static final int MAX_ITERATIONS = 2000000;
	public static final int MAX_GOALS = 2000000;
	public static final int MAX_ARITY = 10;
	public static final int MAX_QUERY_VARS = 10;
	public static final int MAX_DISJUNCTS = 10;
	public static final int MAX_ISA_CACHE = 32;
  public static final int MAX_KB_SIZE = 2000000;  // Assertions to retain in memory before flushing

	public static final int MAX_ONTOLOGY_CLASS_CNT = 40000;
	public static final int MAX_ONTOLOGY_DISTANCE = 100;

	public static final int FULL_MODE = 1;
	public static final int LITE_MODE = 0;

	public static final String DEFAULT_GRAPH = "NULL";
	public static final String DEFAULT_QUERY_GRAPH = "?_NULL";

	public static final byte DIRECT_ASSERTION = 0;
	public static final byte INFERRED_ASSERTION = 1;
	public static final byte RULE_INFERENCE = 2;
	public static final byte DELETED_ASSERTION = -1;

	public static final byte NOT_INHERITABLE = 0;
	public static final byte MONOTONIC = 1;
	public static final byte NON_MONOTONIC = 2;

	public static final int MAX_BINDINGS = 10;

	public static final char VALUE = 0;
	public static final char VAR = 1;

	public static final char RULE_MODE_RULES_ONLY = 1;

	public static final String BuiltInPreds    = "fail != == < > ontoMap table str_find str_remove temp_gt temp_lt temp_eq";
	public static final String BuildInStrPreds = "str_remove str_append str_split";
	public static final String AggregatePreds  = "setof lengthof";
	public static final String InstanceOfPreds = ": <:";
	public static final String HiPriorityPreds = ": <: ontoMap";
	public static final String LabelPreds      = "label";

	public static final String firstDate = "1400-01-01T00:00:00";
	public static final String lastDate  = "9999-12-31T59:59:59";
}
