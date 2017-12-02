package com.strider.datadefender.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class BackendDBConnection {
    // init database constants
    private static final String DATABASE_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/redatasense";
    private static final String USERNAME = "redatasense";
    private static final String PASSWORD = "redatasense123!";
    private static final String MAX_POOL = "250";

    // init connection object
    private Connection connection;
    // init properties object
    private Properties properties;

    // create properties
    private Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            properties.setProperty("user", USERNAME);
            properties.setProperty("password", PASSWORD);
            properties.setProperty("MaxPooledStatements", MAX_POOL);
        }
        return properties;
    }

    // connect database
    public Connection connect() {
        if (connection == null) {
            try {
                Class.forName(DATABASE_DRIVER);
                connection = DriverManager.getConnection(DATABASE_URL, getProperties());
                
            } catch (ClassNotFoundException | SQLException e) {
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
        String SelectTableSQL = "SELECT VENDOR,DRIVER,USERNAME,PASSWORD,SCHEMA,URL FROM REDATASENSE.DB_PROPERTIES";

    dbConnection.setAutoCommit(false);
    PreparedStatement preparedStatement = dbConnection.prepareStatement(SelectTableSQL);    
    rs = preparedStatement.executeQuery(SelectTableSQL);
    
    
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return rs;
    }




    public void insertFileResultRow (Connection dbConnection, String RunID, Timestamp now, String Directory, String Filename, String Probability, String Model, String Dictionary)
    {
         
        
        try {
        String insertTableSQL = "INSERT INTO REDATASENSE.FILE_RESULTS"
		+ "(RUN_ID, RUN_TIMESTAMP, DIRECTORY, FILENAME, PROBABILITY, MODEL, DICTIONARY) VALUES"
        + "(?,?,?,?,?,?,?)";

    dbConnection.setAutoCommit(false);
    PreparedStatement preparedStatement = dbConnection.prepareStatement(insertTableSQL);
    preparedStatement.setString(1, RunID);
    preparedStatement.setTimestamp(2, now);
    preparedStatement.setString(3, Directory);
    preparedStatement.setString(4, Filename);
    preparedStatement.setString(5, Probability);
    preparedStatement.setString(6, Model);
    preparedStatement.setString(7, Dictionary);
    
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