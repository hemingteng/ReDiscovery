
RedataSense - Data Discovery with Machine Learning
========================================

Disclaimer
-------
* This is a fork from original work called DataDefender (https://github.com/armenak/DataDefender). The reasons behind fork are:
* Remove the Anonymizer part of the original project, as commercial and open source RDBMS have different ways to anonymize data. Data masking is a vast topic on databases and most of the time a very complex issue and can't be solved using this tool;
* Remove of Data Generator, as this tools should focus on scanning real data that can be in any environment;
* Add of other OpenNLP techniques based on Dictionary and Regex search to improve performance and accuracy of "sometimes" not very good Max Entropy NLP models.
* This tool can help you on GDPR.

The complete source code is available. The license is the same as the original project.

This implementation of is done using [Apache OpenNLP](https://opennlp.apache.org/)


Features
--------
1. Identifies and sensitive personal data using Named Entity Recognition: Max Entropy models, Dictionary and REGEX (all provided by OpenNLP) inside documents and databases.
4. Platform-independent
5. Supports Oracle, MS SQL Server, DB2, MySQL and PostgreSQL

Prerequisites
----------------
1. JDK 1.8+
2. Maven 3+

Build from source
-----------------
1. Download ZIP file and unzip in a directory of your choice, or clone repo
2. mvn install
3. RedataSense.jar will be located in "target" directory target/

Download Binaries
-----------------
We are now providing compiled binaries for you to download and here:
https://github.com/redglue/redsense/tree/3.0phoenix/releases/

Please note, that we strongly recommend using version 3.0 and later.

How to install (v3.0 and later)
----------

Note: Version 3.0 and later came with a new features and a lot of code rework. One thing that really changed from version 2.0 was that Redatasense will use an MySQL database as backend to save and store the results from Data Discovery.

1) Install software

- Windows, GNU/Linux or MacOSX are supported
- MySQL server (Recommended version is 5.5 and later)
- Java 8 JDK 64bits


2) Create a database and deploy the datamodel.
- Create schema and datamodel: (https://github.com/redglue/redsense/blob/3.0phoenix/src/main/resources/datamodel.sql)
- Create application configuration:
(https://github.com/redglue/redsense/blob/3.0phoenix/src/main/resources/user_datamodel.sql)

3) Edit file backend.properties with correct MySQL connection string.

4) *Any changes to configuration, need an update to database parameters*

5) Done


How to run (version 3.0 and later)
----------
The toolkit is implemented as a command line program. To run it first build the application as above (mvn install). This
will generate an executeable jar file in the "target" directory. Once this has been done you can get help by typing:

    java -jar RedataSense.jar --help

All modes support an optional list of tables at the end to use for either discover, or anonymization of a specific table or list of tables.

File Discovery (version 3.0 and later)
--------------
File discovery will attempt to find a sensitive personal information in binary and text files located on the file system.

Note: Remember that on version 3.0, all configuration for File Discovery is stored on database and filediscovery.properties file is depreceated.

    java -jar RedataSense.jar file-discovery

*Note: Regex mode scan still need file regex.properties*


Column Discovery (version 3.0 and later)
----------------
In this mode the tool attempts to query your database and identified columns that should be anonymized based on their names.

    java -jar RedataSense.jar database-discovery -c

*Note: Column discovery mode scan still need file columndiscovery.properties*


Data Discovery (version 3.0 and later)
------------------
Data Discovery will perform an NLP scan, an REGEX scan or dictionary scan of data in the database and return columns that have a match score greater than the value of probability_threshold specified in parameters. 

    java -jar RedataSense.jar database-discovery -d 

Note: Remember that on version 3.0, all configuration for Data Discovery is stored on database.

*Note: Regex mode scan still need file regex.properties*



Build it - Using 3rd-Party JDBC Drivers with Maven
------------------
Unfortunately, not all JDBC drivers are downloadable via a publicly available maven repostitory and must be downloaded individually.  For example:

- http://www.oracle.com/technetwork/apps-tech/jdbc-112010-090769.html
- http://www.microsoft.com/en-us/download/details.aspx?displaylang=en&id=11774

In order to use these drivers via maven you can add the driver jar to your private maven repository if you have one or install locally:

<ol>
<li>download package</li>
<li>unzip/extract jdbc jar file from package</li>
<li>add driver to your local maven repository by executing:  
<pre>
mvn install:install-file -Dfile=${path to jdbc driver jar file} -DgroupId=${groupId} -DartifactId=${artifactId} -Dversion=${version} -Dpackaging=jar
</pre>
</li>
<li>add dependency to pom.xml:
<pre>
    &lt;dependency&gt;
        &lt;groupId&gt;${groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;${artifactId}&lt;/artifactId&gt;
        &lt;version&gt;${version}&lt;/version&gt;
    &lt;/dependency&gt;
</pre>
</li>
</ol>

Important Note
----------
- Redatasense Analytics Portal (RAP) is not opensource.
- Redatasense NLP models or dictionaries are not opensource.
