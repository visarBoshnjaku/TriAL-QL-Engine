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

package data.structures;

/**
 * Configuration variables for E-TriAL-QL. Use them to switch to different
 * modes. Note: Not all combinations work together, for example not more than
 * one TC should be chosen.
 * 
 */
public class Configuration {

	public static String initialTableName = "table_name";

	public static Boolean compositeJoin = false;

	public static Boolean noFixedPoint = false;

	public static Boolean SemNaive = true;

	public static Boolean Smart = false;

	public static Boolean Heuristics = false;

	public static Boolean compositionalConnectivity = false;

	public static Boolean longChainRecursion = false;

	public static Boolean saveToFiles = false;

}
