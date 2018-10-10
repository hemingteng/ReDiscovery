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
import static org.apache.log4j.Logger.getLogger;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;

import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import opennlp.tools.util.Span;

import com.strider.datadefender.file.metadata.FileMatchMetaData;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.namefind.TokenNameFinder;

import java.util.regex.Pattern;

import com.strider.datadefender.database.BackendDBConnection;

import static com.strider.datadefender.utils.AppProperties.loadProperties;

/**
 *
 * @author Armenak Grigoryan
 */
public class FileDiscoverer extends Discoverer {

    private static final Logger log = getLogger(FileDiscoverer.class);

    private static String[] modelList;
    protected List<FileMatchMetaData> fileMatches;

    @SuppressWarnings("unchecked")
    public List<FileMatchMetaData> discover(final Properties dataDiscoveryProperties)
            throws AnonymizerException, IOException, SAXException, TikaException, NullPointerException, Exception {
        log.info("Data discovery in process");

        // Initialize Data Connection to Store the results
        // Version 3.0 - Redatasense
        BackendDBConnection backendDB = new BackendDBConnection();
        Connection dbc = backendDB.connect();

        //generate UUID unique for identify the RUN
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        java.sql.Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());

        // Get the probability threshold from property file
        final double probabilityThreshold = parseDouble(dataDiscoveryProperties.getProperty("probability_threshold"));
        log.info("Probability threshold [" + probabilityThreshold + "]");

        // Get list of models used in data discovery
        final String models = dataDiscoveryProperties.getProperty("models");
        modelList = models.split(",");
        log.info("Model list [" + Arrays.toString(modelList) + "]");

        List<FileMatchMetaData> finalList = new ArrayList<>();
        for (String model: modelList) {
            log.info("Processing model: " + model);
            final Model modelPerson = createModel(dataDiscoveryProperties, model);
            fileMatches = discoverAgainstSingleModel(dataDiscoveryProperties, modelPerson, probabilityThreshold);
            finalList = ListUtils.union(finalList, fileMatches);
        }

        final DecimalFormat decimalFormat = new DecimalFormat("#.##");
        //log.info("List of suspects:");
        //log.info(String.format("%s,%s,%s,%s,%s", "Directory", "Filename", "Probability", "Model", "[Dictionaries]"));

        for(final FileMatchMetaData data: finalList) {
            final String probability = decimalFormat.format(data.getAverageProbability());
            if (data.getDictionariesFoundList().isEmpty())
            {
                backendDB.insertFileResultRow(dbc, randomUUIDString, now, data.getDirectory(), data.getFileName(),
                        probability, data.getModel(), data.getModelMode(), data.getDictionariesFound());
            }
            else {
                // For each classification/dictionary write a line
                for (String classificator: data.getDictionariesFoundList()) {
                    //if classificator == null
                    backendDB.insertFileResultRow(dbc, randomUUIDString, now, data.getDirectory(), data.getFileName(),
                            probability, data.getModel(), data.getModelMode(), classificator);
                }
            }

            //final String result = String.format("%s,%s,%s,%s,[%s]", data.getDirectory(), data.getFileName(), probability, data.getModel(), data.getDictionariesFound());
            //log.info(result);

        }
        log.info("Writing to database the list of suspects...");
        dbc.commit();
        dbc.close();
        log.info("Done");
        return Collections.unmodifiableList(fileMatches);
    }

    // Task: function to getAllDictionaries and return a List of TokenNameFinders to feed the model

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

    private List<FileMatchMetaData> discoverAgainstSingleModel(final Properties fileDiscoveryProperties,
                                                               final Model model, final double probabilityThreshold)
            throws AnonymizerException, IOException, SAXException, TikaException, NullPointerException, Exception {
        // Start running NLP algorithms for each column and collect percentage
        fileMatches = new ArrayList<>();
        String[] directoryList = null;
        String[] dictionaryPathList = null;

        String[] inclusionList = null;
        String[] files_excludedList = null;

        final String directories = fileDiscoveryProperties.getProperty("directories");
        final String inclusions = fileDiscoveryProperties.getProperty("inclusions");
        final String files_excluded = fileDiscoveryProperties.getProperty("files_excluded");

        final String dictionaryPath = fileDiscoveryProperties.getProperty("dictionary_path");

        List<TokenNameFinder> findersDict = null;
        // possible values for this:
        // NEREntropy - Uses only NER MaxEntropy OpenNLP trained models
        // NERDictionary - Uses only Dictionary XML OPenNLP implementation
        // NERRegex - Uses only Regex OpenNLP models
        final String NERModel = fileDiscoveryProperties.getProperty("NERmodel");


        directoryList = directories.split(",");
        dictionaryPathList = dictionaryPath.split(",");
        inclusionList = inclusions.split(",");
        files_excludedList = files_excluded.split(",");

        // Let's iterate over directories
        File node;
        File nodeDict = null;
        Metadata metadata;
        //String DictionariesFound = "";
        ArrayList<String> DictionariesFound = null;
        List<Probability> probabilityList;
        List<Probability> probabilityListRegex;
        List<Probability> probabilityListDict;
        //ArrayList<String> DictionariesFoundRegex = new ArrayList<String>();

        double averageProbability = 0;




        //final InputStream Dictstream = new FileInputStream(dictionaryPath);
        //log.info("Dictionary considered for analysis: " + dictionaryPath);

        //Dictionary rawdict = new Dictionary(Dictstream);
        //log.info("Loading dictionaries...");

        //  List<TokenNameFinder> findersDict = new ArrayList<>();
        // add all dictionaries
        //  for (final String dictPath: dictionaryPathList) {
        //      nodeDict = new File(dictPath);
        //      final InputStream Dictstream = new FileInputStream(dictPath);

        //    log.info("Dictionary considered for analysis: " + dictPath);
        //    Dictionary rawdict = new Dictionary(Dictstream);
        //DictionaryNameFinder dictionaryNER = new DictionaryNameFinder(rawdict, nodeDict.getName().replaceFirst("[.][^.]+$", ""););

        //    findersDict.add(new DictionaryNameFinder(rawdict, nodeDict.getName().replaceAll("\\.\\w+", "")));

        //  }

        log.info("File types considered for analysis: " + inclusions);
        log.info("Directories for analysis: " + directories);

        for (final String directory: directoryList) {
            log.info("Listing files. Please wait...");
            node = new File(directory);
            final List<File> files = (List<File>) FileUtils.listFiles(node, inclusionList, true);
            log.info("Files to look for - >" + files);


            for (final File fich : files) {
                final String file = fich.getName().toString();
                final String recursivedir = fich.getParent().toString();

                if (Arrays.asList(files_excludedList).contains(file)) {
                    log.info("Ignoring [" + fich.getCanonicalPath() + "]");
                    continue;
                }

                log.info("Analyzing [" + fich.getCanonicalPath() + "]");

                final BodyContentHandler handler = new BodyContentHandler(-1);

                final AutoDetectParser parser = new AutoDetectParser();
                metadata = new Metadata();
                String handlerString = "";
                try {
                    final InputStream stream = new FileInputStream(fich.getCanonicalPath());
                    if (stream != null) {
                        parser.parse(stream, handler, metadata);
                        handlerString = handler.toString();

                    }
                } catch (IOException e) {
                    log.info("Unable to read " + fich.getCanonicalPath() + ".Ignoring...");
                } catch (Throwable npe) {
                    log.info("File error or not supported " + fich.getCanonicalPath() + ".Ignoring...");
                    continue;
                }

                try {
                    final FileMatchMetaData result = new FileMatchMetaData(recursivedir, file);

                    switch (NERModel.toLowerCase()) {

                        case "nerdictionary":
                            averageProbability = applyNERDictionary(dictionaryPathList, nodeDict, handlerString,
                                    handler, model);
                            break;

                        case "nerentropy":
                            averageProbability = applyNEREntropy(handlerString, model, handler);
                            break;


                        // REGEX OpenNLP implementation;
                        case "nerregex":
                            // averageProbability = applyNERRegex(handlerString, model, handler);
                            log.info ("**** NERRegex mode: on *** ");
                            DictionariesFound = new ArrayList<String>();
                            final Properties RegexProperties = loadProperties("regex.properties");
                            log.info("PROPERTIES ---------------->" + RegexProperties);
                            log.info("Tokenizing for file is starting...");
                            log.debug("Content: " + handlerString);
                            probabilityListRegex = new ArrayList<>();
                            final String tokensRegex[] = model.getTokenizer().tokenize(handler.toString());
                            //final List<String> suspList = new ArrayList(RegexProperties.keySet());
                            //matches = new ArrayList<>();
                            //Pattern[] patterns = new Pattern[];
                            log.info("Applying REGEX model for sensitive data...");
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

                            RegexNameFinder finder = new RegexNameFinder(regexMap);
                            Span[] resultRegex = finder.find(tokensRegex);

                            log.info("Evaluating Regex results...");
                            //final String RegexSpam = Arrays.toString(Span.spansToStrings(resultRegex, tokensRegex));
                            String getRegexType = "N/A";
                            for( int i = 0; i < resultRegex.length; i++) {
                                getRegexType = resultRegex[i].getType();
                                log.debug("Found Type text: " + getRegexType);
                                DictionariesFound.add(getRegexType);
                                // default regex probability is 99% always
                                //log.info("Regex for text: " + tokensRegex[resultRegex[i].getStart()]);
                                log.debug("Found identifier text: " + tokensRegex[resultRegex[i].getStart()]);
                                probabilityListRegex.add(new Probability(tokensRegex[resultRegex[i].getStart()], 0.99));
                            }

                            finder.clearAdaptiveData();
                            averageProbability = calculateAverage(probabilityListRegex);
                            //result.setAverageProbability(averageProbability);
                            //result.setDictionariesFound(DictionariesFoundRegex);
                            break;

                        case "all":
                            ArrayList<Double> allProbs = new ArrayList<>();

                            allProbs.add(applyNERDictionary(dictionaryPathList, nodeDict, handlerString, handler,
                                    model));
                            allProbs.add(applyNEREntropy(handlerString, model, handler));
                            //allProbs.add(applyNERRegex(handlerString, model, handler));

                            double sum = 0.0;
                            if(!allProbs.isEmpty()){
                                for(Double prob : allProbs){

                                }
                                ;
                            }


                            break;

                    }


                    if ((averageProbability >= probabilityThreshold)) {
                        //final FileMatchMetaData result = new FileMatchMetaData(recursivedir, file);
                        result.setAverageProbability(averageProbability);
                        result.setModel(model.getName());
                        result.setModelMode(NERModel);
                        result.setDictionariesFound(DictionariesFound);
                        fileMatches.add(result);
                    }

                } catch (NullPointerException npe) {
                    npe.printStackTrace(System.out);
                    log.info("NameFinder Model can't be applied to " + fich.getCanonicalPath() + ".Ignoring...");
                }

            }
        }
        return fileMatches;
    }

    private double applyNERDictionary(String[] dictionaryPathList, File nodeDict, String handlerString,
                                      BodyContentHandler handler, Model model) throws IOException{
        log.info ("**** NERDictionary mode: on *** ");
        ArrayList<String> DictionariesFound = new ArrayList<>();
        log.info("Loading Dictionaries..Please wait");
        List<TokenNameFinder> findersDict = getDictionariesFileForSearch(dictionaryPathList, nodeDict);

        log.debug("Content: " + handlerString);
        ArrayList probabilityListDict = new ArrayList<>();
        String tokensDict[] = model.getTokenizer().tokenize(handler.toString());
        tokensDict = ngrams(tokensDict, 6);
        log.info("Applying Dictionary model...");
        //final Span DictSpansOnly[] = dictionaryNER.find(tokensDict);
        for (TokenNameFinder dictionaryNER : findersDict) {
            final Span DictSpansOnly[] = dictionaryNER.find(tokensDict);
            for(Span dictSpan : DictSpansOnly)
            {
                DictionariesFound.add(dictSpan.getType());
                //DictionariesFound.add(DictSpansOnly[i].getType()); // not working - Ad all
                log.debug("Dictionary:" + dictSpan.getType() + "," + tokensDict[dictSpan.getStart()]);
                probabilityListDict.add(new Probability(tokensDict[dictSpan.getStart()], 0.99));
            }

            dictionaryNER.clearAdaptiveData();
        }
        return calculateAverage(probabilityListDict);
    }

    private double applyNEREntropy(String handlerString, Model model, BodyContentHandler handler){
        log.info ("**** NEREntropy mode: on *** ");
        ArrayList DictionariesFound = new ArrayList<String>();
        log.debug("Tokenizing for file is starting...");
        log.debug("Content: " + handlerString);
        String tokens[] = model.getTokenizer().tokenize(handler.toString());
        tokens = ngrams(tokens, 3);
        final Span nameSpans[] = model.getNameFinder().find(tokens);
        final double[] spanProbs = model.getNameFinder().probs(nameSpans);

        //display names
        ArrayList probabilityList = new ArrayList<Probability>();

        log.info("Comparing results...");
        for( int i = 0; i < nameSpans.length; i++) {
            log.info("Probability is: "+spanProbs[i] +" for text: " + tokens[nameSpans[i].getStart()]);
            probabilityList.add(new Probability(tokens[nameSpans[i].getStart()], spanProbs[i]));
        }
        //DictionariesFound.add(nameSpans[i].getType());
        model.getNameFinder().clearAdaptiveData();

        return calculateAverage(probabilityList);
    }

    private double applyNERRegex(String handlerString, Model model, BodyContentHandler handler) throws DataDefenderException {
        log.info ("**** NERRegex mode: on *** ");
        ArrayList DictionariesFound = new ArrayList<String>();
        final Properties RegexProperties = loadProperties("regex.properties");
        log.info("Tokenizing for file is starting...");
        log.debug("Content: " + handlerString);
        ArrayList probabilityListRegex = new ArrayList<>();
        final String tokensRegex[] = model.getTokenizer().tokenize(handler.toString());
        //final List<String> suspList = new ArrayList(RegexProperties.keySet());
        //matches = new ArrayList<>();
        //Pattern[] patterns = new Pattern[];
        log.info("Applying REGEX model for sensitive data...");
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

        RegexNameFinder finder = new RegexNameFinder(regexMap);
        Span[] resultRegex = finder.find(tokensRegex);

        log.info("Evaluating Regex results...");
        //final String RegexSpam = Arrays.toString(Span.spansToStrings(resultRegex, tokensRegex));
        String getRegexType = "N/A";
        for( int i = 0; i < resultRegex.length; i++) {
            getRegexType = resultRegex[i].getType();
            log.debug("Found Type text: " + getRegexType);
            DictionariesFound.add(getRegexType);
            // default regex probability is 99% always
            //log.info("Regex for text: " + tokensRegex[resultRegex[i].getStart()]);
            log.debug("Found identifier text: " + tokensRegex[resultRegex[i].getStart()]);
            probabilityListRegex.add(new Probability(tokensRegex[resultRegex[i].getStart()], 0.99));
        }

        finder.clearAdaptiveData();
        return calculateAverage(probabilityListRegex);
        //result.setAverageProbability(averageProbability);
        //result.setDictionariesFound(DictionariesFoundRegex);
    }

    public static String[] ngrams(String[] singleTokens, int n){
        /*
            String[] singleTokes: The String array of tokens
            int n: the size of the ngrams
            Function that from a set of Single Tokens (example: ["This", "is", "not", "sensitive"]) and a size n, which
            sprecifies the size of the ngram, creates and returns a new String array with the ngrams (example for size 3:
            ["This", "This is", "This is not", "is", "is not", .....]

         */
        ArrayList<String> ngrams = new ArrayList<String>();

        for(int i = 0; i < singleTokens.length; i++){
            for(int j = 1; j <= n; j++){
                // if((i + j) == singleTokens.length) continue;

                String range[] = Arrays.copyOfRange(singleTokens, i, i + j);
                StringJoiner ngram = new StringJoiner(" ");
                for (String s : range){
                    ngram.add(s);
                }
                ngrams.add(ngram.toString());
            }
        }
        return ngrams.stream().map(c -> c.toString()).toArray(String[]::new);
    }
}
