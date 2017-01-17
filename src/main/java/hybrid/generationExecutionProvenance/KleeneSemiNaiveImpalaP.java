/*
* Copyright (C) 2016 University of Freiburg.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package hybrid.generationExecutionProvenance;

import java.sql.ResultSet;
import java.util.ArrayList;

import data.structures.QueryStruct;
import data.structures.ResultStruct;
import executor.AppImpala;
import executor.ImpalaDaemon;

public class KleeneSemiNaiveImpalaP {
	public static String finalQuery;
	public static String createTableQuery;
	public static String baseQuery = "";
	static ResultSet results = null;
	public static String temporaryQuery;
	static int numberOfLines;
	static String whereExp = "";

	/**
	 * Semi-naive Impala with Provenance implementation of Transitive closure.
	 * 
	 * @param oldTableName
	 * @param newTableName
	 * @param joinOnExpression
	 * @param kleeneType
	 * @param selectionPart
	 * @param kleeneDepth1
	 * @param provenanceAppenderList
	 */
	public static void CreateQuery(String[] oldTableName, String newTableName, ArrayList<String> joinOnExpression,
			String kleeneType, String[] selectionPart, int kleeneDepth1, ArrayList<Integer> provenanceAppenderList) {

		String tableShortForm = oldTableName[0].substring(0, 2);
		String currentTableName = oldTableName[0];
		String sel1, sel2, sel3, sel1l, sel2l, sel3l;
		String sel4 = "";
		String sel4l = "";
		String join = "";
		String topQueryPart = "";

		int stepCounter = 0;

		tableInitialization(oldTableName);

		numberOfLines = 1;
		while (numberOfLines != 0) {
			stepCounter++;
			String cTableShort = currentTableName;

			if (selectionPart[0].equals("1")) {
				sel1 = cTableShort + "." + selectionPart[1];
				sel1l = tableShortForm + "1" + "." + selectionPart[1];
			} else {
				sel1 = tableShortForm + "1" + "." + selectionPart[1];
				sel1l = cTableShort + "." + selectionPart[1];
			}

			if (selectionPart[2].equals("1")) {
				sel2 = cTableShort + "." + selectionPart[3];
				sel2l = tableShortForm + "1" + "." + selectionPart[3];
			} else {
				sel2 = tableShortForm + "1" + "." + selectionPart[3];
				sel2l = cTableShort + "." + selectionPart[3];
			}

			if (selectionPart[4].equals("1")) {
				sel3 = cTableShort + "." + selectionPart[5];
				sel3l = tableShortForm + "1" + "." + selectionPart[5];
			} else {
				sel3 = tableShortForm + "1" + "." + selectionPart[5];
				sel3l = cTableShort + "." + selectionPart[5];
			}

			String provenance = ", MIN(CONCAT(";
			if (provenanceAppenderList.get(0) == 1) {
				sel4 = provenance + cTableShort + ".provenance, '/'," + tableShortForm + "1.provenance))";
				sel4l = provenance + tableShortForm + "1.provenance, '/'," + cTableShort + ".provenance))";
			} else if (provenanceAppenderList.get(0) == 2) {
				sel4 = provenance + tableShortForm + "1.provenance, '/'," + cTableShort + ".provenance))";
				sel4l = provenance + cTableShort + ".provenance, '/'," + tableShortForm + "1.provenance))";
			}

			if (kleeneType.equals("right")) {
				topQueryPart = "SELECT " + sel1 + " AS subject, " + sel2 + " AS predicate, " + sel3 + " AS object"
						+ sel4 + " FROM " + currentTableName + " JOIN  " + oldTableName[0] + " " + tableShortForm + 1
						+ " ON ";

				if (currentTableName != oldTableName[0])
					whereExp = " WHERE deltaP.iter='" + Integer.toString(stepCounter - 1) + "' ";

				for (int k = 0; k < joinOnExpression.size(); k = k + 3) {
					if (k > 0)
						join = join + " AND ";

					if (joinOnExpression.get(k).toString().substring(2, 3).equals("1")) {
						join = join + " " + cTableShort + joinOnExpression.get(k).toString().substring(3);
					} else {
						join = join + " " + tableShortForm + 1 + joinOnExpression.get(k).toString().substring(3);
					}

					join = join + " " + joinOnExpression.get(k + 1) + " ";

					if (joinOnExpression.get(k + 2).toString().substring(2, 3).equals("1")) {
						join = join + " " + cTableShort + joinOnExpression.get(k + 2).toString().substring(3);
					} else {
						join = join + " " + tableShortForm + 1 + joinOnExpression.get(k + 2).toString().substring(3);
					}
				}
			} else if (kleeneType.equals("left")) {
				topQueryPart = "SELECT " + sel1l + " AS subject, " + sel2l + " AS predicate, " + sel3l + " AS object"
						+ sel4l + " FROM " + oldTableName[0] + " " + tableShortForm + 1 + " JOIN " + currentTableName
						+ " ON ";

				if (currentTableName != oldTableName[0]) {
					whereExp = " WHERE deltaP.iter='" + Integer.toString(stepCounter - 1) + "' ";
				}

				for (int k = 0; k < joinOnExpression.size(); k = k + 3) {
					if (k > 0)
						join = join + " AND ";

					if (joinOnExpression.get(k).toString().substring(2, 3).equals("1")) {
						join = join + " " + tableShortForm + 1 + joinOnExpression.get(k).toString().substring(3);
					} else {
						join = join + " " + cTableShort + joinOnExpression.get(k).toString().substring(3);
					}

					join = join + " " + joinOnExpression.get(k + 1) + " ";

					if (joinOnExpression.get(k + 2).toString().substring(2, 3).equals("1")) {
						join = join + " " + tableShortForm + 1 + joinOnExpression.get(k + 2).toString().substring(3);
					} else {
						join = join + " " + cTableShort + joinOnExpression.get(k + 2).toString().substring(3);
					}
				}
			}

			String insertTmp = "INSERT OVERWRITE tmp " + topQueryPart + join + whereExp + " GROUP BY"
					+ " subject, predicate, object";
			ImpalaDaemon.noReturn(insertTmp);
			ImpalaDaemon.noReturn("COMPUTE STATS tmp");
			baseQuery = baseQuery + insertTmp + "\n";

			temporaryQuery = "SELECT tmp.subject, tmp.predicate, tmp.object, tmp.provenance"
					+ " FROM tmp LEFT ANTI JOIN deltaP"
					+ " ON tmp.subject = deltaP.subject AND tmp.predicate = deltaP.predicate"
					+ " AND tmp.object = deltaP.object ";

			String insertDeltaP = "INSERT INTO deltaP partition(iter='" + Integer.toString(stepCounter) + "') "
					+ temporaryQuery;
			ImpalaDaemon.noReturn(insertDeltaP);
			ImpalaDaemon.noReturn("COMPUTE INCREMENTAL STATS deltaP");

			baseQuery = baseQuery + insertDeltaP + "\n";

			String resultsChecking = "SELECT COUNT(*) AS count FROM deltaP " + " WHERE iter='"
					+ Integer.toString(stepCounter) + "' ";
			results = ImpalaDaemon.main(resultsChecking);
			baseQuery = baseQuery + resultsChecking + "\n";

			try {
				results.next();
				numberOfLines = results.getInt(1);
			} catch (Exception e) {
			}

			currentTableName = "deltaP";
			System.out.println("# of new lines " + numberOfLines);
			join = "";
		}

		System.out.println("Loop Finished");

		temporaryQuery = "SELECT COUNT(*) FROM deltaP";
		// temporaryQuery = "SELECT subject AS subject, predicate as predicate,
		// object AS object, provenance as provenance from deltaP";

		if (kleeneDepth1 == -10) {
			temporaryQuery = temporaryQuery + " WHERE iter <> '0'";

		}
		if (AppImpala.theLastQuery) {
			results = ImpalaDaemon.main(temporaryQuery);
		} else {
			results = ImpalaDaemon.main("CREATE TABLE " + newTableName + " AS ( " + temporaryQuery + " )");
		}

		baseQuery = baseQuery + temporaryQuery + "\n";

		QueryStruct.fillStructure(oldTableName, newTableName, baseQuery, "none", "none");
		ResultStruct.fillStructure(results);

	}

	static void tableInitialization(String[] oldTableName) {
		String createDeltaP = "CREATE TABLE deltaP (subject string, predicate string, object string, provenance string) "
				+ "PARTITIONED BY (iter string) STORED AS PARQUET;";
		ImpalaDaemon.noReturn(createDeltaP);

		baseQuery = createDeltaP + "\n";

		String insertDeltaP0 = "INSERT INTO deltaP partition(iter='0') SELECT subject, predicate, object, provenance FROM "
				+ oldTableName[0];
		ImpalaDaemon.noReturn(insertDeltaP0);

		ImpalaDaemon.noReturn("COMPUTE STATS deltaP");

		baseQuery = baseQuery + insertDeltaP0 + "\n";

		String createTmpTable = "CREATE TABLE tmp (subject string, predicate string, object string, provenance string) "
				+ " STORED AS PARQUET;";
		ImpalaDaemon.noReturn(createTmpTable);
		baseQuery = baseQuery + createTmpTable + "\n";

	}
}
