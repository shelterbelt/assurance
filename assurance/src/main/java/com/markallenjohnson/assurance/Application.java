/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 Mark Johnson
 * 
 */
/*
 * Copyright 2015 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.markallenjohnson.assurance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.markallenjohnson.assurance.model.ScriptRunner;
import com.markallenjohnson.assurance.ui.IApplicationUI;

public class Application
{
	private static String verificationTableName = "COMPARISON_RESULT";

	public static String applicationShortName = "Assurance";
	public static String applicationName = "Assurance Backup Manager";
	public static String applicationVersion = "0.0.0";
	public static String applicationBuildNumber = "DEVBUILD";

	public static void main(String[] args)
	{
		Logger logger = Logger.getLogger(Application.class);

		logger.info("App is starting.");

		Properties applicationProperties = new Properties();
		String applicationInfoFileName = "/version.txt";
		InputStream inputStream = Application.class.getResourceAsStream(applicationInfoFileName);
		applicationInfoFileName = null;

		try
		{
			if (inputStream != null)
			{
				applicationProperties.load(inputStream);

				Application.applicationShortName = applicationProperties.getProperty("name");
				Application.applicationName = applicationProperties.getProperty("applicationName");
				Application.applicationVersion = applicationProperties.getProperty("version");
				Application.applicationBuildNumber = applicationProperties.getProperty("buildNumber");
				
				applicationProperties = null;
			}
		}
		catch (IOException e)
		{
			logger.warn("Could not load application version information.", e);
		}
		finally
		{
			try 
			{
				inputStream.close();
			}
			catch (IOException e) 
			{
				logger.error("Couldn't close the application version input stream.");
			}
			inputStream = null;
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			private Logger logger = Logger.getLogger(Application.class);

			public void run()
			{
				logger.info("Starting the Swing run thread.");

				try
				{
					Application.installDb();
				}
				catch (IOException e)
				{
					logger.fatal("Unable to install the application database.", e);
					System.exit(1);
				}
				catch (SQLException e)
				{
					logger.fatal("Unable to install the application database.", e);
					System.exit(1);
				}

				IApplicationUI window = null;
				ClassPathXmlApplicationContext springContext = null;
				try
				{
					springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
					StringBuffer message = new StringBuffer(256);
					logger.info(message.append("Spring Context: ").append(springContext));
					message.setLength(0);
					window = (IApplicationUI) springContext.getBean("ApplicationUI");
				}
				finally
				{
					if (springContext != null)
					{
						springContext.close();
					}
					springContext = null;
				}
				
				if (window != null)
				{
					logger.info("Launching the window.");
					window.display();
				}
				else
				{
					logger.fatal("The main application window object is null.");
				}
				
				logger = null;
			}
		});
	}

	private static void installDb() throws IOException, SQLException
	{
		InputStream propertiesFileStream = null;
		InputStream dbScriptStream = null;
		try
		{
			String propertiesFileName = "properties/database.properties";
			String databaseScriptFileName = "database/assurance.sql";
			propertiesFileStream = Application.class.getClassLoader().getResourceAsStream(propertiesFileName);
			propertiesFileName = null;
			dbScriptStream = Application.class.getClassLoader().getResourceAsStream(databaseScriptFileName);
			databaseScriptFileName = null;
			
			Application.installDb(propertiesFileStream, dbScriptStream);
		}
		finally
		{
			if (propertiesFileStream != null)
			{
				propertiesFileStream.close();
				propertiesFileStream = null;
			}
			if (dbScriptStream != null)
			{
				dbScriptStream.close();
				dbScriptStream = null;
			}
		}
	}

	static void installDb(InputStream propertiesFileStream, InputStream dbScriptStream) throws IOException, SQLException
	{
		Logger logger = Logger.getLogger(Application.class);

		Connection dbConnection = null;
		ResultSet rs = null;
		try
		{
			Properties properties = new Properties();

			if (propertiesFileStream != null)
			{
				properties.load(propertiesFileStream);
			}
			else
			{
				throw new FileNotFoundException("The database properties file could not be loaded.");
			}
			String dbUrl = (String) properties.get("jdbc.url");
			String dbUser = (String) properties.get("jdbc.username");
			String dbPassword = (String) properties.get("jdbc.password");

			dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

			ArrayList<String> listOfDatabases = new ArrayList<String>();
			DatabaseMetaData meta = dbConnection.getMetaData();
			String[] tableTypes = { "TABLE" };
			rs = meta.getTables(null, null, Application.verificationTableName, tableTypes);
			while (rs.next())
			{
				String databaseName = rs.getString("TABLE_NAME");
				listOfDatabases.add(databaseName.toUpperCase());
			}
			if (listOfDatabases.contains(Application.verificationTableName))
			{
				logger.info("Database already exists");
			}
			else
			{
				ScriptRunner runner = new ScriptRunner(dbConnection, true, true);

				Reader dbScript = new InputStreamReader(dbScriptStream);
				runner.runScript(dbScript);

				logger.info("Database is created");
			}
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
					rs = null;
				}
				catch (SQLException e)
				{
					// The ship is going down. Not much we can do.
					logger.fatal(e);
				}
			}
			if (dbConnection != null)
			{
				dbConnection.close();
				dbConnection = null;
			}
		}
		
		logger = null;
	}
}
