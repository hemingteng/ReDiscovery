package com.strider.datadefender.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.strider.datadefender.DataDefenderException;

import static com.strider.datadefender.utils.AppProperties.loadProperties;



import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class BackendDBConnection {
    // init database constants
    //private static final String DATABASE_DRIVER = "com.mysql.jdbc.Driver";
    //private static final String DATABASE_URL = "jdbc:mysql://redbackend.mysql.database.azure.com:3306/redatasense";
    //private static final String USERNAME = "redglue@redbackend";
    //private static final String PASSWORD = "Slackware3797";
    //private static final String MAX_POOL = "250";

    // init connection object
    private Connection connection;
    // init properties object
    private Properties properties;

    // create properties
    private Properties getProperties() throws DataDefenderException {

        final Properties dbProperties = loadProperties("backend.properties");

        if (properties == null) {
            properties = new Properties();
            properties.setProperty("user", dbProperties.getProperty("username"));
            properties.setProperty("password", dbProperties.getProperty("password"));
            properties.setProperty("driver", dbProperties.getProperty("driver"));
            properties.setProperty("url", dbProperties.getProperty("url"));
            properties.setProperty("useSSL", dbProperties.getProperty("useSSL"));
            properties.setProperty("autoReconnect", dbProperties.getProperty("autoReconnect"));
            properties.setProperty("autoReconnect", dbProperties.getProperty("autoReconnect"));


           // properties.setProperty("MaxPooledStatements", "250");
            //properties.setProperty("useSSL", "false");
            //properties.setProperty("autoReconnect", "true");

        }
        return properties;
    }

    // connect database
    public Connection connect() {
        if (connection == null) {
            try {
                final Properties backEndProperties = getProperties();
                //log.info("Connecting to backend repository : "+ DATABASE_URL);
                String DATABASE_DRIVER = backEndProperties.getProperty("driver");
                String DATABASE_URL = backEndProperties.getProperty("url");
                Class.forName(DATABASE_DRIVER);
                connection = DriverManager.getConnection(DATABASE_URL, getProperties());
                connection.setAutoCommit(false);
                
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            } catch (DataDefenderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return connection;
    }

    // Return the resultset of database properties
    public ResultSet getDatabaseDiscoveryProperties (Connection dbConnection)
    {
         
        ResultSet rs = null;
        try {
        String SelectTableSQL = "SELECT VENDOR,DRIVER,USERNAME,PASSWORD,DBSCHEMA,URL,ISACTIVE from REDATASENSE.DB_PROPERTIES WHERE ISACTIVE=1";

    dbConnection.setAutoCommit(false);
    PreparedStatement preparedStatement = dbConnection.prepareStatement(SelectTableSQL);    
    rs = preparedStatement.executeQuery(SelectTableSQL);
    
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return rs;
    }




    public void insertFileResultRow (Connection dbConnection, String RunID, Timestamp now, String Directory, String Filename, String Probability, String Model, String ModelMode, String Dictionary)
    {
         
        
        try {
        String insertTableSQL = "INSERT INTO REDATASENSE.FILE_RESULTS"
		+ "(RUN_ID, RUN_TIMESTAMP, DIRECTORY, FILENAME, PROBABILITY, MODEL, MODELMODE, DICTIONARY) VALUES"
        + "(?,?,?,?,?,?,?,?)";

    dbConnection.setAutoCommit(false);
    PreparedStatement preparedStatement = dbConnection.prepareStatement(insertTableSQL);
    preparedStatement.setString(1, RunID);
    preparedStatement.setTimestamp(2, now);
    preparedStatement.setString(3, Directory);
    preparedStatement.setString(4, Filename);
    preparedStatement.setString(5, Probability);
    preparedStatement.setString(6, Model);
    preparedStatement.setString(7, ModelMode);
    preparedStatement.setString(8, Dictionary);
    
    // execute insert SQL stetement
    preparedStatement.executeUpdate();
    
    } catch (SQLException e) {
        e.printStackTrace();
    }
    //return dbConnection;
    
    }


    public void insertDataDiscoveryRow (Connection dbConnection, String RunID, Timestamp now, String columnName, Double Probability, String Model, String ModelMode, String Dictionary, Integer numRows, Double score, String sampleData)
    {
         
       
        
        try {
        String insertTableSQL = "INSERT INTO REDATASENSE.DATA_RESULTS"
		+ "(`ID_DB`, `RUN_ID`, `RUN_TIMESTAMP`, `COLUMN_NAME`, `PROBABILITY`, `MODEL`, `MODELMODE`, `DICTIONARY`, `NUM_ROWS`, `SCORE`, `SAMPLE_DATA`) VALUES "
        + "((select max(ID_DB) from REDATASENSE.DB_PROPERTIES where ISACTIVE=1),?,?,?,?,?,?,?,?,?,?)";

    dbConnection.setAutoCommit(false);
    PreparedStatement preparedStatement = dbConnection.prepareStatement(insertTableSQL);
    preparedStatement.setString(1, RunID);
    preparedStatement.setTimestamp(2, now);
    preparedStatement.setString(3, columnName);
    preparedStatement.setDouble(4, Probability);
    preparedStatement.setString(5, Model);
    preparedStatement.setString(6, ModelMode);
    preparedStatement.setString(7, Dictionary);
    preparedStatement.setInt(8, numRows);
    preparedStatement.setDouble(9, score);
    preparedStatement.setString(10, sampleData);

    
    // execute insert SQL stetement
    preparedStatement.executeUpdate();
    
    } catch (SQLException e) {
        e.printStackTrace();
    }
    //return dbConnection;
    
    }


    public void insertColumnDiscoveryRow (Connection dbConnection, String RunID, Timestamp now, String Tablename, String columnName)
    {
         
       
        
        try {
        String insertTableSQL = "INSERT INTO REDATASENSE.COLUMN_RESULTS"
		+ "(`ID_DB`, `RUN_ID`, `RUN_TIMESTAMP`, `TABLE_NAME`, `COLUMN_NAME`) VALUES "
        + "((select max(ID_DB) from REDATASENSE.DB_PROPERTIES where ISACTIVE=1),?,?,?,?)";

    dbConnection.setAutoCommit(false);
    PreparedStatement preparedStatement = dbConnection.prepareStatement(insertTableSQL);
    preparedStatement.setString(1, RunID);
    preparedStatement.setTimestamp(2, now);
    preparedStatement.setString(3, Tablename);
    preparedStatement.setString(4, columnName);

    
    // execute insert SQL stetement
    preparedStatement.executeUpdate();
    
    } catch (SQLException e) {
        e.printStackTrace();
    }
    //return dbConnection;
    
    }


    // disconnect database
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}