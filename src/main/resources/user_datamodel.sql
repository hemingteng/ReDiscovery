CREATE USER 'redatasense'@'localhost' IDENTIFIED BY 'redatasense123!';
GRANT ALL ON redatasense.* TO 'redatasense'@'localhost' IDENTIFIED BY 'redatasense123!';

INSERT INTO redatasense.DB_PROPERTIES (`VENDOR`, `DRIVER`,`USERNAME`,`PASSWORD`,`SCHEMA`,`URL`) VALUES ('oracle', 'oracle.jdbc.driver.OracleDriver', 'oe', 'oe', 'OE', 'jdbc:oracle:thin:@127.0.0.1:1521/p12');
COMMIT;

INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('probability_threshold','0.6');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('model_generic','pt-ner-multi.bin');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('tokens','en-token.bin');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('models','generic');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('directories','/Users/luismarques/github-redglue/redatasense/releases/3.0/sensitive');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('files_excluded','excluded_file.txt');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('dictionary_path','nomes.xml');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('inclusions','txt,TXT,doc,DOCX');
INSERT INTO redatasense.FILE_PROPERTIES (`KEY`, `VALUE`) VALUES ('NERmodel','NERDictionary');

COMMIT;
