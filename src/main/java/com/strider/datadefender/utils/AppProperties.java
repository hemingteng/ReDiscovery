/*
 * 
 * Copyright 2014, Armenak Grigoryan, and individual contributors as indicated
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

package com.strider.datadefender.utils;

import static org.apache.log4j.Logger.getLogger;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import com.strider.datadefender.DataDefenderException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;


import com.strider.datadefender.database.BackendDBConnection;
import com.strider.datadefender.database.DBConnection;

/**
 *
 * @author Armenak Grigoryan
 */
public final class AppProperties {
    
    private static final Logger log = getLogger(AppProperties.class);

    /**
     * Load property file
     * @param fileName
     * @return Properties
     */
    public static Properties loadPropertiesFromClassPath(final String fileName) {
        final Properties props = new Properties();
        InputStream input = null;
 
    	try {
            input = AppProperties.class.getClassLoader().getResourceAsStream(fileName);
            if(input==null){
    	        log.warn("Unable to find " + fileName);
                return null;
            }
            //load a properties file from class path, inside static method
            props.load(input);
    	} catch (IOException ex) {
            log.error(ex.toString());
        } finally{
            if(input!=null){
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e.toString());
                }
            }
        }
 
        return props;
    }
 
    public static Properties loadProperties(final String fileName) throws DataDefenderException {
        final Properties properties = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(fileName), "UTF-8")) {
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new DataDefenderException("ERROR: Unable to load " + fileName, e);
        }
    }

    public static Properties loadPropertiesFromDB(String PropertiesTablename) throws DataDefenderException {
        BackendDBConnection backendDB = new BackendDBConnection();
        Connection dbc = backendDB.connect();
        final Properties properties = new Properties();
        

        ResultSet rs = null;
        try {
        String SelectTableSQL = "SELECT RKEY,RVALUE FROM REDATASENSE."+ PropertiesTablename;

        dbc.setAutoCommit(false);
        PreparedStatement preparedStatement = dbc.prepareStatement(SelectTableSQL);    
        rs = preparedStatement.executeQuery(SelectTableSQL);
        
        while(rs.next()) {
            // first column is the key , second is the value itself
            properties.put(rs.getString(1), rs.getString(2));
            }
        dbc.close();
        return properties;
        } 
        catch (SQLException e) {
            throw new DataDefenderException("ERROR: Unable to load properties from Database", e);
        }
    }

    public static Properties loadPropertiesDBRepository() throws DataDefenderException {
        BackendDBConnection backendDB = new BackendDBConnection();
        Connection dbc = backendDB.connect();
        final Properties pp = new Properties();
        

        ResultSet rs = null;
        try {
            String SelectTableSQL = "SELECT VENDOR,DRIVER,USERNAME,PASSWORD,DBSCHEMA,URL,ISACTIVE from REDATASENSE.DB_PROPERTIES";

        dbc.setAutoCommit(false);
        PreparedStatement preparedStatement = dbc.prepareStatement(SelectTableSQL);    
        rs = preparedStatement.executeQuery(SelectTableSQL);
        
        while(rs.next()) {

            pp.put("vendor", rs.getString("VENDOR"));
            pp.put("driver", rs.getString("DRIVER"));
            pp.put("username", rs.getString("USERNAME"));
            pp.put("password", rs.getString("PASSWORD"));
            pp.put("schema", rs.getString("DBSCHEMA"));
            pp.put("url", rs.getString("URL"));
            pp.put("isactive", rs.getBoolean("ISACTIVE"));
            }

        dbc.close();
        return pp;
        } 
        catch (SQLException e) {
            throw new DataDefenderException("ERROR: Unable to load properties from Database", e);
        }
    }



}