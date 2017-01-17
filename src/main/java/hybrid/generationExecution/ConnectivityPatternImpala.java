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

package hybrid.generationExecution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.sql.rowset.CachedRowSet;
import data.structures.QueryStruct;
import data.structures.ResultStruct;
import executor.ImpalaDaemon;

public class ConnectivityPatternImpala {
	public static String finalQuery;
	public static String createTableQuery;
	public static String baseQuery = "";
	static ResultSet results = null;
	public static String temporaryQuery;
	static int numberOfLines;
	static String whereExp = "";
	static CachedRowSet resultsCopy;
	static boolean newResultsL = true;
	static boolean newResultsR = true;
	static String secondSel;
	static long lStartTime = 0;

	/**
	 * Generator/ Executor of Impala queries formed by recursive E-TriAL-QL
	 * queries. This implementation materializes intermediary results for
	 * queries of Connectivity Pattern.
	 * @param oldTableName
	 * @param newTableName
	 * @param joinOnExpression
	 * @param kleeneType
	 * @param selectionPart
	 * @param sourceDest
	 * @throws SQLException
	 */
	public static void CreateQuery(String[] oldTableName, String newTableName, ArrayList<String> joinOnExpression,
			String kleeneType, String[] selectionPart, String[] sourceDest) throws SQLException {

		String tableShortForm = oldTableName[0].substring(0, 2);
		String currentTableName = oldTableName[0];
		String join;

		int stepCounter = 0;
		int templCounter = 0;
		int temprCounter = 0;
		int hops = 0;

		int res = 0;

		hops++;
		boolean considerImmidiateConnection = false;
		if (considerImmidiateConnection) {
			String directCon = "SELECT COUNT(*) from (SELECT '1' FROM " + currentTableName + " WHERE " + " subject="
					+ sourceDest[0] + " AND object=" + sourceDest[1] + " LIMIT 1) c";
			results = ImpalaDaemon.main(directCon);
			baseQuery = directCon + "\n";

			results.next();
			res = results.getInt(1);

			System.out.println("#1 res = " + res);
		}

		if (res == 0) {
			tableInitialization(oldTableName, sourceDest);

			hops++;

			String oneHop = "SELECT COUNT(*) FROM (SELECT '1' FROM deltaPl " + tableShortForm + 1 + " JOIN "
					+ "(SELECT * FROM " + oldTableName[0] + " WHERE " + " object =" + sourceDest[1] + " ) "
					+ tableShortForm + 2 + " ON " + joinOnExpression.get(0) + joinOnExpression.get(1)
					+ joinOnExpression.get(2) + " LIMIT 1) c";
			results = ImpalaDaemon.main(oneHop);

			results.next();
			res = results.getInt(1);
			System.out.println("#2 res = " + res);
		}

		while (newResultsL && newResultsR && res == 0) {
			stepCounter++;
			hops++;

			// If odd
			if (stepCounter % 2 == 1 && newResultsL) {
				if (selectionPart[2].equals("1")) {
					secondSel = "d.";
				} else {
					secondSel = tableShortForm + 2 + ".";
				}

				join = "SELECT DISTINCT d." + selectionPart[1] + ", " + secondSel + selectionPart[3] + ", "
						+ tableShortForm + 2 + "." + selectionPart[5] + " FROM deltaPl d " + "JOIN " + oldTableName[0]
						+ " " + tableShortForm + 2 + " ON d" + joinOnExpression.get(0).toString().substring(3) + " "
						+ joinOnExpression.get(1) + " " + joinOnExpression.get(2) + " WHERE d.iter='" + templCounter
						+ "'";

				String Tmpl = "WITH tmpl AS ( " + join + " ) ";

				temporaryQuery = Tmpl
						+ "SELECT tmpl.subject, tmpl.predicate, tmpl.object FROM tmpl LEFT OUTER JOIN deltaPl"
						+ " ON tmpl.subject = deltaPl.subject AND tmpl.predicate = deltaPl.predicate"
						+ " AND tmpl.object = deltaPl.object " + " WHERE deltaPl.predicate IS NULL";

				String insertDeltaPl = "INSERT INTO deltaPl partition(iter='" + Integer.toString(templCounter + 1)
						+ "') " + temporaryQuery;
				ImpalaDaemon.noReturn(insertDeltaPl);

				baseQuery = baseQuery + insertDeltaPl + "\n";

				results = ImpalaDaemon.main("SELECT COUNT(*) AS count FROM deltaPl " + " WHERE iter='"
						+ Integer.toString(templCounter + 1) + "'");

				int newItems = -1;
				results.next();
				newItems = results.getInt(1);

				if (newItems == 0) {
					newResultsL = false;
				}

				templCounter++;

				// If even
			} else if (newResultsR) {
				if (selectionPart[2].equals("1")) {
					secondSel = tableShortForm + 1 + ".";
				} else {
					secondSel = "d.";
				}

				join = "SELECT DISTINCT " + tableShortForm + 1 + "." + selectionPart[1] + ", " + secondSel
						+ selectionPart[3] + ", d." + selectionPart[5] + " FROM " + oldTableName[0] + " "
						+ tableShortForm + 1 + " JOIN deltaPr d ON " + " " + joinOnExpression.get(0) + " "
						+ joinOnExpression.get(1) + " d" + joinOnExpression.get(2).toString().substring(3)
						+ " WHERE d.iter='" + temprCounter + "'";

				if (temprCounter == 0) {
					join = "SELECT DISTINCT " + tableShortForm + 1 + "." + selectionPart[1] + ", " + secondSel
							+ selectionPart[3] + ", d." + selectionPart[5] + " FROM " + oldTableName[0] + " "
							+ tableShortForm + 1 + " JOIN (SELECT * FROM " + oldTableName[0] + " WHERE " + " object ="
							+ sourceDest[1] + " ) d" + " ON " + " " + joinOnExpression.get(0) + " "
							+ joinOnExpression.get(1) + " d" + joinOnExpression.get(2).toString().substring(3);
				}

				String Tmpr = "WITH tmpr AS ( " + join + " ) ";

				temporaryQuery = Tmpr + "SELECT tmpr.subject, tmpr.predicate, tmpr.object FROM tmpr LEFT OUTER "
						+ " JOIN deltaPr" + " ON tmpr.subject = deltaPr.subject AND tmpr.predicate = deltaPr.predicate"
						+ " AND tmpr.object = deltaPr.object " + " WHERE deltaPr.predicate IS NULL";

				String insertDeltaPr = "INSERT INTO deltaPr partition(iter='" + Integer.toString(temprCounter + 1)
						+ "') " + temporaryQuery;

				ImpalaDaemon.noReturn(insertDeltaPr);

				baseQuery = baseQuery + insertDeltaPr + "\n";

				results = ImpalaDaemon.main("SELECT COUNT(*) AS count FROM deltaPr " + " WHERE iter='"
						+ Integer.toString(temprCounter + 1) + "'");

				int newItems = -1;
				results.next();
				newItems = results.getInt(1);

				if (newItems == 0) {
					newResultsR = false;
				}

				temprCounter++;
			}

			String resultsChecking = "SELECT COUNT(*) AS COUNT FROM " + "(select '1' FROM deltapl " + tableShortForm + 1
					+ " JOIN deltapr " + tableShortForm + 2 + " ON " + joinOnExpression.get(0) + joinOnExpression.get(1)
					+ joinOnExpression.get(2) + " WHERE " + tableShortForm + 1 + ".iter = '" + templCounter + "' AND "
					+ tableShortForm + 2 + ".iter = '" + temprCounter + "' LIMIT 1 ) MyTable1";

			if (temprCounter == 0) {
				resultsChecking = "SELECT COUNT(*) AS COUNT FROM " + "(select '1' FROM deltapl " + tableShortForm + 1
						+ " JOIN " + "(SELECT * FROM " + oldTableName[0] + " WHERE " + " object =" + sourceDest[1]
						+ " ) " + tableShortForm + 2 + " ON " + joinOnExpression.get(0) + joinOnExpression.get(1)
						+ joinOnExpression.get(2) + " WHERE " + tableShortForm + 1 + ".iter = '" + templCounter
						+ "' LIMIT 1 ) MyTable1";
				;
			}

			results = ImpalaDaemon.main(resultsChecking);

			baseQuery = baseQuery + resultsChecking + "\n";

			results.next();
			res = results.getInt(1);

			System.out.println("#" + (stepCounter + 2) + " res = " + res);

		}
		System.out.println("Loop finished");
		long lEndTime = System.nanoTime();
		long difference = lEndTime - lStartTime;
		System.out.println("Conn time in ms: " + difference / 1000000);

		QueryStruct.fillStructure(oldTableName, newTableName, baseQuery, "none", "none");
		ResultStruct.fillStructure(results);

	}

	static void tableInitialization(String[] oldTableName, String[] sourceDest) {

		String createDeltaPl = "CREATE TABLE deltaPl (subject string, predicate string, object string) "
				+ "PARTITIONED BY (iter string) STORED AS PARQUET;";
		ImpalaDaemon.noReturn(createDeltaPl);

		baseQuery = baseQuery + createDeltaPl + "\n";

		String createDeltaPr = "CREATE TABLE deltaPr (subject string, predicate string, object string) "
				+ "PARTITIONED BY (iter string) STORED AS PARQUET;";
		ImpalaDaemon.noReturn(createDeltaPr);

		baseQuery = baseQuery + createDeltaPr + "\n";

		lStartTime = System.nanoTime();

		String insertDeltaPl = "INSERT INTO deltaPl partition(iter='0') " + " SELECT subject, predicate, object FROM "
				+ oldTableName[0] + " WHERE " + " subject =" + sourceDest[0];
		ImpalaDaemon.noReturn(insertDeltaPl);

		baseQuery = baseQuery + insertDeltaPl + "\n";

	}
}
