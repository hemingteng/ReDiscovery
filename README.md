
RedataSense - Data Discovery Toolkit (DataDefender fork)
========================================

Table of content
----------------
- [Disclamer](#Disclamer)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Build from source](#build-from-source)
- [Contributing](#contributing)
- [How to run](#how-to-run-data)
- [File Discovery](#file-discovery)
- [Column Discovery](#column-discovery)
- [Data Discovery](#data-discovery)
- [Using 3rd-Party JDBC Drivers with Maven](#using-3rd-party-jdbc-drivers-with-maven)
- [Code quality](#code-quality)

Disclaimer
-------
* This is a fork from original work called DataDefender (https://github.com/armenak/DataDefender)
The reasons behind fork are:
* Remove the Anonymizer part of the original project, as commercial and open source RDBMS have different ways to anonymize data. Data masking is a vast topic on databases and most of the time a very complex issue and can't be solved using this tool;
* Remove of Data Generator, as this tools should focus on scanning real data that can be in any environment;
* Add of other OpenNLP techniques based on Dictionary and Regex search to improve performance and accuracy of "sometimes" not very good Max Entropy NLP models.

The complete source code is available. The license is the same as the original project.

This implementation of is done using [Apache OpenNLP](https://opennlp.apache.org/)



Features
--------
1. Identifies and sensitive personal data using Named Entity Recognition: Max Entropy models, Dictionary and REGEX (all provided by OpenNLP) inside documents and databases.
4. Platform-independent
5. Supports Oracle, MS SQL Server, DB2, MySQL, and Postgres

Prerequisites
----------------
1. JDK 1.8+
2. Maven 3+

Build from source
-----------------
1. Download ZIP file and unzip in a directory of your choice, or clone repo
2. cd {dir}/DataDefender/
3. mvn install
4. DataDefender.jar will be located in "target" directory {dir}/DataDefender/target/

Contributing
------------
You can contribute by clicking the star button and using this software!

Contributors are encouraged to fork this repository and issue pull requests. If you would like to contribute, please
1. Fork it
2. Create your feature branch (git checkout -b new-feature)
3. Commit your changes (git commit -am 'Add new feature')
4. Push to the branch (git push origin new-feature)
5. Create new Pull Request

How to run
----------
The toolkit is implemented as a command line program. To run it first build the application as above (mvn install). This
will generate an executeable jar file in the "target" directory. Once this has been done you can get help by typing:

    java -jar DataDefender.jar --help

The toolkit can be run in anonymizer mode, and three different discovery modes (file, column, and database discovery). For column and database discovery modes you need to provide
the database property file which defines which database to connect to and how to connect. The location of this property file it passed in using the -P or --data switch.

All modes support an optional list of tables at the end to use for either discover, or anonymization of a specific table or list of tables.

File Discovery
--------------
File discovery will attempt to find a sensitive personal information in binary and text files located on the file system.

Sample project can be found here: https://github.com/armenak/DataDefender/tree/master/sample_projects/file_discovery

In order to run File Discovery, please use filediscovery.properties file created in the sample project and adjust it for your needs. Specifically, probability threshold and the directory where files that needs to be scanned will need to be modified.

Column Discovery
----------------
In this mode the tool attempts to query your database and identified columns that should be anonymized based on their names.  When -r is provided a sample requirements file (which can be modified and used the anonymizer stage) will be created based on the columns discovered. To run in this mode type the following:

    java -jar DataDefender.jar database-discovery -c --data <db.properties> --column-discovery <columndiscovery.properties> [-r -R <requirement_output_file>]

Where:
    <db.properties>              - Path and file name of the file containing database connection properties
                                   (see src/main/resources/db.properties for an example)

    <columndiscovery.properties> - Path and file name of the file containing column discovery properties
                                   (see src/main/resources/columndiscovery.properties for an example)
    <requirement_output_file>    - Optional name of sample requirement file to be created (-r must also be specified)


Data Discovery
------------------
To run the tool in Data Discovery mode, pass '-d' to discover.  DA will perform an NLP scan of data in the database and return columns that have a match score greater than the value of probability_threshold specified in datadiscovery.properties file.  When -r is provided a sample requirements file (which can be modified and used the anonymizer stage) will be created based on the columns discovered by the DA.

    java -jar DataDefender.jar database-discovery -d --data <db.properties> --data-discovery <datadiscovery.properties> [-r -R <requirement_output_file>]

Where:
    <db.properties>            - Path and file name of the file containing database connection properties
                                (see src/main/resources/db.properties for an example)

    <datadiscovery.properties> - Path and file name of the file containing data discovery properties
                                (see src/main/resources/datadiscovery.properties for an example)
    <requirement_output_file>  - Optional name of sample requirement file to be created (-r must also be specified)


Using 3rd-Party JDBC Drivers with Maven
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



<h3>Code quality</h3>
Two amazing tools - Empear http://empear.com/ and SonarQube http://www.sonarqube.org/ help contributors of DataDefender maintain decent quality of code. Many thanks to their creators!
