CREATE USER 'redatasense'@'localhost' IDENTIFIED BY 'redatasense123!';
GRANT ALL ON redatasense.* TO 'redatasense'@'localhost' IDENTIFIED BY 'redatasense123!';

INSERT INTO redatasense.DB_PROPERTIES (`VENDOR`, `DRIVER`,`USERNAME`,`PASSWORD`,`SCHEMA`,`URL`) VALUES ('oracle', 'oracle.jdbc.driver.OracleDriver', 'oe', 'oe', 'OE', 'jdbc:oracle:thin:@127.0.0.1:1521/p12');
COMMIT;