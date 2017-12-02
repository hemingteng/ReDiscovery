CREATE USER 'redatasense'@'localhost' IDENTIFIED BY 'redatasense123!';
GRANT ALL ON redatasense.* TO 'redatasense'@'localhost' IDENTIFIED BY 'redatasense123!';

INSERT INTO redatasense.DB_PROPERTIES (`VENDOR`, `DRIVER`,`USERNAME`,`PASSWORD`,`DBSCHEMA`,`URL`, `ISACTIVE`) VALUES ('oracle', 'oracle.jdbc.driver.OracleDriver', 'oe', 'oe', 'OE', 'jdbc:oracle:thin:@127.0.0.1:1521/p12', '1');
INSERT INTO redatasense.DB_PROPERTIES (`VENDOR`, `DRIVER`,`USERNAME`,`PASSWORD`,`DBSCHEMA`,`URL`, `ISACTIVE`) VALUES ('mssql', 'com.microsoft.sqlserver.jdbc.SQLServerDriver', 'Person', 'antonio', 'UmaBoaPassword1+', 'jdbc:sqlserver://redsqltest01.westeurope.cloudapp.azure.com:1433;database=AdventureWorks', '0');
INSERT INTO redatasense.DB_PROPERTIES (`VENDOR`, `DRIVER`,`USERNAME`,`PASSWORD`,`DBSCHEMA`,`URL`, `ISACTIVE`) VALUES ('mssql', 'com.microsoft.sqlserver.jdbc.SQLServerDriver', 'redglue@redatasense;', 'Slackware3797', 'AdventureWorksLT', 'jdbc:sqlserver://redatasense.database.windows.net:1433;database=rdsexamples', '0');


COMMIT;

INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('probability_threshold','0.6');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('model_generic','pt-ner-multi.bin');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('tokens','en-token.bin');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('models','generic');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('directories','/Users/luismarques/github-redglue/redatasense/releases/3.0/sensitive');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('files_excluded','excluded_file.txt');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('dictionary_path','nomes.xml');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('inclusions','txt,TXT,doc,DOCX');
INSERT INTO redatasense.FILE_PROPERTIES (`RKEY`, `RVALUE`) VALUES ('NERmodel','NERDictionary');

COMMIT;
