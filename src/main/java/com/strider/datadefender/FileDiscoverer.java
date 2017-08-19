/*
 *
 * Copyright 2014-2017, Armenak Grigoryan, and individual contributors as indicated
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
 * 2017 - Forked as redsense by Redglue to implement a different direction to the project
 * Distributed as free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;

 */

 /**
  * Check for properties required for execution of data defender and data anonymizer
  * and stop the execution if not all required properties are defined
  * @author Armenak Grigoryan (original work and author)
  * @author Redglue (www.redglue.eu)
  */

package com.strider.datadefender;

import static java.lang.Double.parseDouble;
import static org.apache.log4j.Logger.getLogger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Arrays;
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
import com.strider.datadefender.utils.CommonUtils;
import java.util.Collections;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.StringList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.LinkedHashSet;
import static com.strider.datadefender.utils.AppProperties.loadProperties;


public class FileDiscoverer extends Discoverer {

    private static final Logger log = getLogger(FileDiscoverer.class);

    private static String[] modelList;
    protected List<FileMatchMetaData> fileMatches;

    @SuppressWarnings("unchecked")
    public List<FileMatchMetaData> discover(final Properties dataDiscoveryProperties)
    throws AnonymizerException, IOException, SAXException, TikaException, NullPointerException, Exception {
        log.info("Data discovery in process");

        // Get the probability threshold from property file
        final double probabilityThreshold = parseDouble(dataDiscoveryProperties.getProperty("probability_threshold"));
        log.info("Probability threshold [" + probabilityThreshold + "]");

        // Get list of models used in data discovery
        final String models = dataDiscoveryProperties.getProperty("models");
        modelList = models.split(",");
        log.info("Model list [" + Arrays.toString(modelList) + "]");

        List<FileMatchMetaData> finalList = new ArrayList<>();
        for (String model: modelList) {
            log.info("********************************");
            log.info("Processing model " + model);
            log.info("********************************");
            final Model modelPerson = createModel(dataDiscoveryProperties, model);
            fileMatches = discoverAgainstSingleModel(dataDiscoveryProperties, modelPerson, probabilityThreshold);
            finalList = ListUtils.union(finalList, fileMatches);
        }

        final DecimalFormat decimalFormat = new DecimalFormat("#.##");
        log.info("List of suspects:");
        log.info(String.format("%s,%s,%s,%s", "Directory", "Filename", "Probability", "Model"));
        for(final FileMatchMetaData data: finalList) {
            final String probability = decimalFormat.format(data.getAverageProbability());
            final String result = String.format("%s,%s,%s,%s", data.getDirectory(), data.getFileName(), probability, data.getModel());
            log.info(result);
        }

        return Collections.unmodifiableList(fileMatches);
    }


    private List<FileMatchMetaData> discoverAgainstSingleModel(final Properties fileDiscoveryProperties, final Model model, final double probabilityThreshold)
    throws AnonymizerException, IOException, SAXException, TikaException, NullPointerException, Exception {
        // Start running NLP algorithms for each column and collect percentage
        fileMatches = new ArrayList<>();
        String[] directoryList = null;
        String[] inclusionList = null;
        String[] files_excludedList = null;

        final String directories = fileDiscoveryProperties.getProperty("directories");
        final String inclusions = fileDiscoveryProperties.getProperty("inclusions");
        final String files_excluded = fileDiscoveryProperties.getProperty("files_excluded");
        final String dictionaryPath = fileDiscoveryProperties.getProperty("dictionary_path");

        // possible values for this:
        // NEREntropy - Uses only NER MaxEntropy OpenNLP trained models
        // NERDictionary - Uses only Dictionary XML OPenNLP implementation
        // NERRegex - Uses only Regex OpenNLP models
        // NEREntropyDictionary - Uses MaxEntropy and compares to Dictionary - Returns only the ones present in the dictionary
        final String NERModel = fileDiscoveryProperties.getProperty("NERmodel");


        directoryList = directories.split(",");
        inclusionList = inclusions.split(",");
        files_excludedList = files_excluded.split(",");

        // Let's iterate over directories
        File node;
        Metadata metadata;
        List<Probability> probabilityList;
        List<Probability> probabilityListRegex;
        List<Probability> probabilityListDict;

        double averageProbability = 0;



      final InputStream Dictstream = new FileInputStream(dictionaryPath);
      log.info("Dictionary considered for analysis: " + dictionaryPath);

      Dictionary rawdict = new Dictionary(Dictstream);
      DictionaryNameFinder dictionaryNER = new DictionaryNameFinder(rawdict, "NER");


        log.info("File types considered for analysis: " + inclusions);
        log.info("Directories for analysis: " + directories);

        for (final String directory: directoryList) {
            log.info("Listing files. Please wait...");
            node = new File(directory);
            final List<File> files = (List<File>) FileUtils.listFiles(node, inclusionList, true);

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
                    try  {
                    final InputStream stream = new FileInputStream(fich.getCanonicalPath());
                        if (stream != null) {
                            parser.parse(stream, handler, metadata);
                            handlerString =  handler.toString();

                        }
                    }
                    catch (IOException e) {
                      log.info("Unable to read " + fich.getCanonicalPath() +".Ignoring...");
                      }
                    catch (Throwable npe) {
                        log.info("File error or not supported " + fich.getCanonicalPath() +".Ignoring...");
                        continue;
                        }

                    try {

                    switch (NERModel) {

                      case "NERDictionary":
                        log.info("Tokenizing for file is starting...");
                        log.debug("Content: " + handlerString);
                        probabilityListDict = new ArrayList<>();
                        final String tokensDict[] = model.getTokenizer().tokenize(handler.toString());
                        log.info("Applying Dictionary model...");
                        final Span DictSpansOnly[] = dictionaryNER.find(tokensDict);
                        for( int i = 0; i < DictSpansOnly.length; i++)
                        {
                            log.info("Dictionary text is: " + tokensDict[DictSpansOnly[i].getStart()]);
                            probabilityListDict.add(new Probability(tokensDict[DictSpansOnly[i].getStart()], 0.99));
                        }
                      dictionaryNER.clearAdaptiveData();
                      averageProbability = calculateAverage(probabilityListDict);
                      break;


                      case "NEREntropyDictionary":

                    log.info("Tokenizing for file is starting...");
                    log.debug("Content: " + handlerString);
                    final String tokens[] = model.getTokenizer().tokenize(handler.toString());
                    log.info("Applying NER and Dictionary Models...");

                    final Span nameSpans[] = model.getNameFinder().find(tokens);
                    final Span DictSpans[] = dictionaryNER.find(tokens);
                    final double[] spanProbs = model.getNameFinder().probs(nameSpans);

                    //display names
                    probabilityList = new ArrayList<>();
                    final String nameSpansString = Arrays.toString(Span.spansToStrings(nameSpans, tokens));
                    final String nameSpansDictString = Arrays.toString(Span.spansToStrings(DictSpans, tokens));


                    System.out.println("Found NER Maxentropy: " + nameSpansString);
                    System.out.println("Found Dictionary: " + nameSpansDictString);

                    log.info("Comparing results...");
                    for( int i = 0; i < nameSpans.length; i++) {
                      for (int j = 0; j < DictSpans.length; j++) {
                          if (nameSpans[i].contains(DictSpans[j]))
                          {
                          log.info("Probability is: "+spanProbs[i] +" for text: " + tokens[nameSpans[i].getStart()]);
                          probabilityList.add(new Probability(tokens[nameSpans[i].getStart()], spanProbs[i]));
                          }

                      }
                    }
                    model.getNameFinder().clearAdaptiveData();
                    dictionaryNER.clearAdaptiveData();
                    averageProbability = calculateAverage(probabilityList);
                    break;

                    // REGEX OpenNLP implementation;
                    case "NERRegex":
                      final Properties RegexProperties = loadProperties("regex.properties");
                      log.info("Tokenizing for file is starting...");
                      log.debug("Content: " + handlerString);
                      probabilityListRegex = new ArrayList<>();
                      final String tokensRegex[] = model.getTokenizer().tokenize(handler.toString());
                      final List<String> suspList = new ArrayList(RegexProperties.keySet());
                      //matches = new ArrayList<>();
                      //Pattern[] patterns = new Pattern[];
                      log.info("Applying REGEX model for sensitive data...");
                      Pattern[] patterns = suspList.stream().map(Pattern::compile).toArray(Pattern[]::new);
                      Map<String, Pattern[]> regexMap = new HashMap<>();


                      regexMap.put("ALL_LANGS", patterns);
                      RegexNameFinder finder = new RegexNameFinder(regexMap);

                      Span[] resultRegex = finder.find(tokensRegex);

                      log.info("Evaluating Regex results...");
                      final String RegexSpam = Arrays.toString(Span.spansToStrings(resultRegex, tokensRegex));
                      System.out.println("Found Regex: " + RegexSpam);


                      for( int i = 0; i < resultRegex.length; i++) {
                            // default regex probability is 99% always
                            //log.info("Regex for text: " + tokensRegex[resultRegex[i].getStart()]);
                            log.info("Found identifier text: " + tokensRegex[resultRegex[i].getStart()]);
                            probabilityListRegex.add(new Probability(tokensRegex[resultRegex[i].getStart()], 0.99));
                            }

                      finder.clearAdaptiveData();
                      averageProbability = calculateAverage(probabilityListRegex);
                      break;

                  }



                    if ((averageProbability >= probabilityThreshold)) {
                        final FileMatchMetaData result = new FileMatchMetaData(recursivedir, file);
                        result.setAverageProbability(averageProbability);
                        result.setModel(model.getName());
                        fileMatches.add(result);
                    }
                  }
                  catch (NullPointerException npe) {
                      log.info("NameFinder Model can't be applied to " + fich.getCanonicalPath() +".Ignoring...");
                      }

                }
            }


        return fileMatches;
      }

}
