/*
 *
 * Copyright 2014-2015, Armenak Grigoryan, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package com.strider.datadefender;

import static java.lang.Double.parseDouble;
import static java.util.regex.Pattern.compile;
import static org.apache.log4j.Logger.getLogger;
import org.apache.commons.collections.ListUtils;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.util.StringList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


import opennlp.tools.util.Span;

import org.apache.log4j.Logger;

import com.strider.datadefender.database.IDBFactory;
import com.strider.datadefender.database.metadata.IMetaData;
import com.strider.datadefender.database.metadata.MatchMetaData;
import com.strider.datadefender.database.sqlbuilder.ISQLBuilder;
import com.strider.datadefender.functions.Utils;
import com.strider.datadefender.report.ReportUtil;
import com.strider.datadefender.specialcase.SpecialCase;
import com.strider.datadefender.utils.CommonUtils;
import com.strider.datadefender.utils.Score;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.Enumeration;
import static com.strider.datadefender.utils.AppProperties.loadProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.strider.datadefender.database.BackendDBConnection;
import com.strider.datadefender.database.DBConnection;
import java.util.UUID;

import static com.strider.datadefender.utils.AppProperties.loadPropertiesFromDB;
import static com.strider.datadefender.utils.AppProperties.loadProperties;



/**
 *
 * @author Armenak Grigoryan
 * @author Redglue
 */
public class DatabaseDiscoverer extends Discoverer {

    private static final Logger log = getLogger(DatabaseDiscoverer.class);

    private static String[] modelList;

    @SuppressWarnings("unchecked")
    public List<MatchMetaData> discover(final IDBFactory factory, final Properties dataDiscoveryProperties, final Set<String> tables)
    throws AnonymizerException, ParseException, IOException, DataDefenderException {
        log.info("Data discovery in process");

        // Get the probability threshold from property file
        final double probabilityThreshold = parseDouble(dataDiscoveryProperties.getProperty("probability_threshold"));
        final String calculate_score = dataDiscoveryProperties.getProperty("score_calculation");
        final String NERModel = dataDiscoveryProperties.getProperty("NERmodel");

        // Initialize Data Connection to Store the results
        // Version 3.0 - Redatasense
        BackendDBConnection backendDB = new BackendDBConnection();
        Connection dbc = backendDB.connect();
        //generate UUID unique for identify the RUN
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        java.sql.Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());


        log.info("Probability threshold [" + probabilityThreshold + "]");

        // Get list of models used in data discovery
        final String models = dataDiscoveryProperties.getProperty("models");
        List<MatchMetaData> finalList = new ArrayList<>();
        log.info("Processing NER Model Type:" + NERModel);

        final Model modelPerson = createModel(dataDiscoveryProperties, models);
        matches = discoverAgainstSingleModel(factory, dataDiscoveryProperties, tables, modelPerson, probabilityThreshold, NERModel);
        finalList = ListUtils.union(finalList, matches);

        final DecimalFormat decimalFormat = new DecimalFormat("#.##");
        log.info("List of suspects:");
        final Score score = new Score();
        int highRiskColumns = 0;
        int rowCount=0;
        for(final MatchMetaData data: finalList) {
            // Row count
            if (calculate_score.equals("yes")) {
              log.debug("Skipping table rowcount...");
              rowCount = ReportUtil.rowCount(factory, data.getTableName());
            }
            // Getting 5 sample values
            // final List<String> sampleDataList = ReportUtil.sampleData(factory, data.getTableName(), data.getColumnName());

            // Output
            log.info("Column                      : " + data.toString());
            log.info( CommonUtils.fixedLengthString('=', data.toString().length() + 30));

            log.info("Probability                 : " + decimalFormat.format(data.getAverageProbability()));
            log.info("Model                       : " + data.getModel());
            log.info("Dictionaries/Model          : " + data.getDictionariesFound());
            if (calculate_score.equals("yes")) {
            log.info("Number of rows in the table : " + rowCount);
            log.info("Score                       : " + score.columnScore(rowCount));
            } else {
            log.info("Number of rows in the table : N/A");
            log.info("Score                       : N/A");
            }

            log.info("Sample data");
            log.info( CommonUtils.fixedLengthString('-', 11));


            final List<Probability> probabilityList = data.getProbabilityList();
            final ArrayList<String> SampleData = new ArrayList();

            Collections.sort(probabilityList,
                Comparator.comparingDouble(Probability::getProbabilityValue).reversed());

            int y=0;
            if (data.getProbabilityList().size() >= 5) {
                y = 5;
            } else {
                y = data.getProbabilityList().size();
            }

            for (int i=0; i<y; i++) {
                final Probability p = data.getProbabilityList().get(i);
                log.info(p.getSentence() + ":" + p.getProbabilityValue());
                SampleData.add(p.getSentence());
            }

            log.info("" );
            // Score calculation is evaluated with calculate_score parameter
          if (calculate_score.equals("yes")) {
            if (score.columnScore(rowCount).equals("High")) {
                highRiskColumns++;
            }
          }

          log.info(String.format("Summary: %s,%s,%s,%s,%s,%s", "Schema", "Table", "Column", "Probability", "Model", "[Dictionary/Model]"));
          final String result = String.format("Summary: %s,%s,%s,%s,%s,[%s]", data.getSchemaName(), data.getTableName(), data.getColumnName(), decimalFormat.format(data.getAverageProbability()), data.getModel(), data.getDictionariesFound());
          
          // writing to database;
      
            log.info("Writing to database the list of suspects...");
            if (calculate_score.equals("yes")) { 
            backendDB.insertDataDiscoveryRow(dbc, randomUUIDString, now, data.getColumnName(), data.getAverageProbability(), data.getModel(), data.getDictionariesFound(),rowCount, Double.parseDouble(score.columnScore(rowCount)), String.join(";", SampleData));
            }
            else{
            backendDB.insertDataDiscoveryRow(dbc, randomUUIDString, now, data.getColumnName(), data.getAverageProbability(), data.getModel(), data.getDictionariesFound(),0, 0.0, String.join(";", SampleData));                    

            }

          log.info(result);
          log.info("\n");

        }

    try{
        dbc.commit();
        dbc.close();
     } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }


        // Only applicable when parameter table_rowcount=yes otherwise score calculation should not be done
        if (calculate_score.equals("yes")) {
        log.info("Overall score: " + score.dataStoreScore());
        log.info("");

        if (finalList != null && finalList.size() > 0) {
            log.info("============================================");
            final int threshold_count    = Integer.valueOf(dataDiscoveryProperties.getProperty("threshold_count"));
            if (finalList.size() > threshold_count) {
                log.info("Number of PI [" + finalList.size() + "] columns is higher than defined threashold [" + threshold_count + "]");
            } else {
                log.info("Number of PI [" + finalList.size() + "] columns is lower or equal than defined threashold [" + threshold_count + "]");
            }
            final int threshold_highrisk = Integer.valueOf(dataDiscoveryProperties.getProperty("threshold_highrisk"));
            if (highRiskColumns > threshold_highrisk) {
                log.info("Number of High risk PI [" + highRiskColumns + "] columns is higher than defined threashold [" + threshold_highrisk + "]");
            } else {
                log.info("Number of High risk PI [" + highRiskColumns + "] columns is lower or equal than defined threashold [" + threshold_highrisk + "]");
            }
        }

      }
      else {
      log.info("Overall score: N/A");
      }

        return matches;
    }

    private List<TokenNameFinder> getDictionariesFileForSearch(String[] dictionaryPathList, File nodeDict)
      throws IOException
    {
      List<TokenNameFinder> findersDict = new ArrayList<>();
    // add all dictionaries
      for (final String dictPath: dictionaryPathList) {
        nodeDict = new File(dictPath);
        final InputStream Dictstream = new FileInputStream(dictPath);

        log.info("Dictionary considered for analysis: " + dictPath);
        Dictionary rawdict = new Dictionary(Dictstream);
        //DictionaryNameFinder dictionaryNER = new DictionaryNameFinder(rawdict, nodeDict.getName().replaceFirst("[.][^.]+$", ""););

        findersDict.add(new DictionaryNameFinder(rawdict, nodeDict.getName().replaceAll("\\.\\w+", "")));

    }
    return findersDict;
  }


    private List<MatchMetaData> discoverAgainstSingleModel(final IDBFactory factory, final Properties dataDiscoveryProperties,
            final Set<String> tables, final Model model, final double probabilityThreshold, final String NERModel)
    throws AnonymizerException, ParseException, IOException, DataDefenderException {
        final IMetaData metaData = factory.fetchMetaData();
        final List<MatchMetaData> map = metaData.getMetaData();
        // Start running NLP algorithms for each column and collect percentage
        matches = new ArrayList<>();
        MatchMetaData specialCaseData = null;
        boolean specialCase = false;

        final String[] specialCaseFunctions = dataDiscoveryProperties.getProperty("extentions").split(",");
        //NERDictionary
        String dictionaryPath = null;
        File nodeDict = null;
        String[] dictionaryPathList = null;
        List<TokenNameFinder> findersDict = null;
        ArrayList<String> DictionariesFound = null;

        if (NERModel.equals("NERDictionary")) {
        dictionaryPath = dataDiscoveryProperties.getProperty("dictionary_path");
        dictionaryPathList = dictionaryPath.split(",");
        findersDict = getDictionariesFileForSearch(dictionaryPathList, nodeDict);

      }


        // possible values for this:
        // NEREntropy - Uses only NER MaxEntropy OpenNLP trained models
        // NERDictionary - Uses only Dictionary XML OPenNLP implementation
        // NERRegex - Uses only Regex OpenNLP models
        // NEREntropyDictionary - Uses MaxEntropy and compares to Dictionary - Returns only the ones present in the dictionary

        //final InputStream DictstreamDB = new FileInputStream(dictionaryPath);

        //Dictionary rawdictDB = new Dictionary(DictstreamDB);
        //DictionaryNameFinder dictionaryNERDB = new DictionaryNameFinder(rawdictDB, "NERDB");



        if (specialCaseFunctions != null && specialCaseFunctions.length > 0) {
            specialCase = true;
        }

        final ISQLBuilder sqlBuilder = factory.createSQLBuilder();
        List<Probability> probabilityList;
        List<Probability> probabilityListRegex;
        List<Probability> probabilityListDict;
        double averageProbability; // initization
        String getRegexType = "N/A";

        //findersDict = getDictionariesFileForSearch(dictionaryPathList, nodeDict);

        for(final MatchMetaData data: map) {
            final String tableName = data.getTableName();
            final String columnName = data.getColumnName();
            log.debug(data.getColumnType());
             // start with 0 as new column is analyzed
            probabilityList = new ArrayList<>();
            probabilityListRegex = new ArrayList<>();
            probabilityListDict = new ArrayList<>();
            averageProbability = 0;
            log.info("Analyzing table [" + tableName + "].["+ columnName+ "]");

            if (!tables.isEmpty() && !tables.contains(tableName.toLowerCase(Locale.ENGLISH))) {
                log.debug("Continue ...");
                continue;
            }

            final String tableNamePattern = dataDiscoveryProperties.getProperty("table_name_pattern");
            if (!CommonUtils.isEmptyString(tableNamePattern)) {
                final Pattern p = compile(tableNamePattern);
                if (!p.matcher(tableName).matches()) {
                    continue;
                }
            }

            final String table = sqlBuilder.prefixSchema(tableName);
            final int limit = Integer.parseInt(dataDiscoveryProperties.getProperty("limit"));
            final String query = sqlBuilder.buildSelectWithLimit(
                "SELECT " + columnName +
                " FROM " + table +
                " WHERE " + columnName  + " IS NOT NULL ", limit);
            log.debug("Executing query against database: " + query);

            try (Statement stmt = factory.getConnection().createStatement();
                ResultSet resultSet = stmt.executeQuery(query);) {

                while (resultSet.next()) {
                    if (data.getColumnType().equals("BLOB") || data.getColumnType().equals("GEOMETRY")) {
                        continue;
                    }

                    if (model.getName().equals("location") &&
                        data.getColumnType().contains("INT")) {
                        continue;
                    }

                    final String sentence = resultSet.getString(1);
                    if (specialCase) {
                        try {
                            for (int i=0; i<specialCaseFunctions.length; i++) {
                                if (sentence != null && !sentence.equals("")) {
                                    specialCaseData = (MatchMetaData)callExtention(specialCaseFunctions[i], data, sentence);
                                }
                            }
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException  e) {
                            log.error(e.toString());
                        }

                    }

                    if (sentence != null && !sentence.isEmpty()) {
                        String processingValue = "";
                        if (data.getColumnType().equals("DATE") ||
                            data.getColumnType().equals("TIMESTAMP") ||
                            data.getColumnType().equals("DATETIME")
                           ) {
                            final DateFormat originalFormat = new SimpleDateFormat(sentence, Locale.ENGLISH);
                            final DateFormat targetFormat = new SimpleDateFormat("MMM d, yy", Locale.ENGLISH);
                            final java.util.Date date = originalFormat.parse(sentence);
                            processingValue = targetFormat.format(date);
                        } else {
                            processingValue = sentence;
                        }

                        switch (NERModel) {


                          case "NERRegex":
                            final Properties RegexProperties = loadProperties("regex.properties");

                            log.debug("Tokenizing for column is starting...");
                            log.debug("Content: " + processingValue);

                            final String tokensRegex[] = model.getTokenizer().tokenize(processingValue);

                            final List<String> suspList = new ArrayList(RegexProperties.keySet());

                            //matches = new ArrayList<>();
                            //Pattern[] patterns = new Pattern[];
                            log.debug("Applying REGEX model for sensitive data...");

                            //Pattern[] patterns = suspList.stream().map(Pattern::compile).toArray(Pattern[]::new);
                            Enumeration<?> enumeration = RegexProperties.propertyNames();
                            Map<String, Pattern[]> regexMap = new HashMap<>();

                            while (enumeration.hasMoreElements()) {
                              String key = (String) enumeration.nextElement();
                              String value = RegexProperties.getProperty(key);
                              Pattern ptregex = Pattern.compile(value);
                              Pattern[] ptterns = new Pattern[]{ptregex};
                              //Map<String, Pattern[]> regexMap = new HashMap<>();
                              regexMap.put(key, ptterns);
                              }

                            //Pattern testEmail = Pattern.compile(regexEMAIL);

                            //Map<String, Pattern[]> regexMap = new HashMap<>();


                            RegexNameFinder finder = new RegexNameFinder(regexMap);

                            Span[] resultRegex = finder.find(tokensRegex);

                            log.debug("Evaluating Regex results...");
                            final String RegexSpam = Arrays.toString(Span.spansToStrings(resultRegex, tokensRegex));
                            //log.info("Found Regex: " + RegexSpam);
                            //String getRegexType = "N/A";
                            for( int i = 0; i < resultRegex.length; i++) {
                                  getRegexType = resultRegex[i].getType();
                                  log.debug("Found Type text: " + getRegexType);
                                  log.debug("Found identifier text: " + tokensRegex[resultRegex[i].getStart()]);
                                  probabilityListRegex.add(new Probability(tokensRegex[resultRegex[i].getStart()], 0.99));
                                  //averageProbability = calculateAverage(probabilityListRegex);
                                  //data.setAverageProbability(0.99);
                                  //data.setAverageProbability(averageProbabilityRegex);
                                  }

                            finder.clearAdaptiveData();
                            data.setProbabilityList(probabilityListRegex);
                            averageProbability = calculateAverage(probabilityListRegex);
                            data.setDictionariesFound(getRegexType);
                            data.setAverageProbability(averageProbability);
                            break;

                          case "NERDictionary":
                            data.setAverageProbability(0);
                            //List<TokenNameFinder> findersDict = getDictionariesFileForSearch(dictionaryPathList, nodeDict);
                            DictionariesFound = new ArrayList<String>();
                            log.debug("Loading Dictionaries..Please wait");
                            //findersDict = getDictionariesFileForSearch(dictionaryPathList, nodeDict);
                            log.debug("Dictionary considered for analysis: " + dictionaryPath);
                            log.debug("Tokenizing for column is starting...");


                            final String tokensDict[] = model.getTokenizer().tokenize(processingValue);
                            log.debug("Applying Dictionary model to column...");

                            for (TokenNameFinder dictionaryNERDB : findersDict) {
                              final Span DictSpansOnly[] = dictionaryNERDB.find(tokensDict);
                            for( int i = 0; i < DictSpansOnly.length; i++)
                            {

                              //DictionariesFound.add(DictSpansOnly[i].getType());
                              data.setDictionariesFound(DictSpansOnly[i].getType());
                              log.debug("Dictionary type is: " + DictSpansOnly[i].getType());
                              // dictionary finding always represent 99% of being correct.
                              log.debug("Dictionary text is: " + tokensDict[DictSpansOnly[i].getStart()]);

                              probabilityListDict.add(new Probability(tokensDict[DictSpansOnly[i].getStart()], 0.99));
                            }

                          dictionaryNERDB.clearAdaptiveData();
                          data.setProbabilityList(probabilityListDict);
                          averageProbability = calculateAverage(probabilityListDict);
                          data.setAverageProbability(averageProbability);

                        }

                          break;
                          // END OF DICTIONARY LOOKUP

                        case "NEREntropy":
                          final String tokens[] = model.getTokenizer().tokenize(processingValue);
                          // Find names
                          final Span nameSpans[] = model.getNameFinder().find(tokens);
                        //find probabilities for names
                          final double[] spanProbs = model.getNameFinder().probs(nameSpans);

                        // Collect top X tokens with highest probability
                        //display names
                        for( int i = 0; i<nameSpans.length; i++) {
                            log.debug("Span: "+nameSpans[i].toString());
                            log.debug("Covered text is: "+tokens[nameSpans[i].getStart()]);
                            log.debug("Probability is: "+spanProbs[i]);
                            probabilityList.add(new Probability(tokens[nameSpans[i].getStart()], spanProbs[i]));
                        }
                        // From OpenNLP documentation:
                        //  After every document clearAdaptiveData must be called to clear the adaptive data in the feature generators.
                        // Not calling clearAdaptiveData can lead to a sharp drop in the detection rate after a few documents.
                        data.setProbabilityList(probabilityList);
                        averageProbability = calculateAverage(probabilityList);
                        model.getNameFinder().clearAdaptiveData();
                        data.setDictionariesFound(model.getName());
                        data.setAverageProbability(averageProbability);
                        break;

                    }

                  }
                }

            } catch (SQLException sqle) {
                log.error(sqle.toString());
            }

            if ((averageProbability >= probabilityThreshold)) {

              data.setModel(NERModel);
              matches.add(data);
              }

        }


        return matches;
    }

    /**
     * Calls a function defined as an extention
     * @param function
     * @param data
     * @param text
     * @return
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private Object callExtention(final String function, MatchMetaData data, String text)
        throws SQLException,
               NoSuchMethodException,
               SecurityException,
               IllegalAccessException,
               IllegalArgumentException,
               InvocationTargetException {

        if (function == null || function.equals("")) {
            log.warn("Function " + function + " is not defined");
            return null;
        }

        Object value = null;

        try {
            final String className = Utils.getClassName(function);
            final String methodName = Utils.getMethodName(function);
            final Method method = Class.forName(className).getMethod(methodName, new Class[]{MatchMetaData.class, String.class});

            final SpecialCase instance = (SpecialCase) Class.forName(className).newInstance();

            final Map<String, Object> paramValues = new HashMap<>(2);
            paramValues.put("metadata", data);
            paramValues.put("text", text);

            value = method.invoke(instance, data, text);

        } catch (AnonymizerException | InstantiationException | ClassNotFoundException ex) {
            log.error(ex.toString());
        }

        return value;
    }
}
