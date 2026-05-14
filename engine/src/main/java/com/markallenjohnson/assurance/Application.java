/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 - 2023 Mark Johnson
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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.markallenjohnson.assurance.ipc.EngineFacade;
import com.markallenjohnson.assurance.ipc.WebSocketIpcNotifier;
import com.markallenjohnson.assurance.ipc.WebSocketIpcServer;
import com.markallenjohnson.assurance.model.ScriptRunner;
import com.markallenjohnson.assurance.model.migration.HibernateSequenceRealigner;
import com.markallenjohnson.assurance.ui.IApplicationUI;

public class Application
{
	public static String applicationShortName = "Assurance";
	public static String applicationName = "Assurance Backup Manager";
	public static String applicationVersion = "0.0.0";
	public static String applicationBuildNumber = "DEVBUILD";

	static final int IPC_PROTOCOL_VERSION = 1;
	static final String HEADLESS_FLAG = "--headless";

	public static void main(String[] args)
	{
		Logger logger = LogManager.getLogger(Application.class);

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
				if (inputStream != null) {
					inputStream.close();
				}
			}
			catch (IOException e) 
			{
				logger.error("Couldn't close the application version input stream.");
			}
			inputStream = null;
		}

		if (isHeadless(args))
		{
			runHeadless(logger);
			return;
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			private Logger logger = LogManager.getLogger(".class");

			public void run()
			{
				logger.info("Starting the Swing run thread.");

				try
				{
					Application.installDb();
				}
				catch (IOException | SQLException e)
				{
					logger.fatal("Unable to install the application database.", e);
					System.exit(1);
				}
				
				IApplicationUI window = null;
				ClassPathXmlApplicationContext springContext = null;
				try
				{
					springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
					StringBuilder message = new StringBuilder(256);
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

	private static boolean isHeadless(String[] args)
	{
		if (args == null)
		{
			return false;
		}
		for (String arg : args)
		{
			if (HEADLESS_FLAG.equals(arg))
			{
				return true;
			}
		}
		return false;
	}

	private static void runHeadless(Logger logger)
	{
		try
		{
			Application.installDb();
		}
		catch (IOException | SQLException e)
		{
			logger.fatal("Unable to install the application database in headless mode.", e);
			System.exit(1);
			return;
		}

		ClassPathXmlApplicationContext springContext;
		try
		{
			springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
		}
		catch (RuntimeException e)
		{
			logger.fatal("Unable to start the Spring context in headless mode.", e);
			System.exit(1);
			return;
		}

		final EngineFacade engineFacade;
		try
		{
			engineFacade = springContext.getBean(EngineFacade.class);
		}
		catch (RuntimeException e)
		{
			logger.fatal("Unable to resolve EngineFacade bean.", e);
			springContext.close();
			System.exit(1);
			return;
		}

		WebSocketIpcServer ipcServer = new WebSocketIpcServer(
			IPC_PROTOCOL_VERSION, Application.applicationVersion, engineFacade);
		final int port;
		try
		{
			ipcServer.start();
			port = ipcServer.awaitStart(5, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			logger.fatal("Unable to start the IPC WebSocket server.", e);
			springContext.close();
			System.exit(1);
			return;
		}

		// Wire the IPC notifier into the engine facade so async operations
		// (performScan etc.) can publish server-initiated notifications to
		// connected renderers.
		engineFacade.setNotifier(new WebSocketIpcNotifier(ipcServer));

		Runtime.getRuntime().addShutdownHook(new Thread(() ->
		{
			logger.info("Engine shutdown signal received.");
			try
			{
				engineFacade.shutdown();
			}
			catch (RuntimeException ex)
			{
				logger.warn("Failed to shut down the scan executor cleanly.", ex);
			}
			try
			{
				ipcServer.stop();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			finally
			{
				try
				{
					springContext.close();
				}
				catch (RuntimeException ex)
				{
					logger.warn("Failed to close Spring context cleanly during shutdown.", ex);
				}
			}
		}, "assurance-engine-shutdown-hook"));

		System.out.println("ASSURANCE_READY ws://127.0.0.1:" + port + " v" + IPC_PROTOCOL_VERSION);
		System.out.flush();

		logger.info("Engine running in headless mode on port {}; awaiting shutdown.", port);

		try
		{
			new CountDownLatch(1).await();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
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
		Logger logger = LogManager.getLogger(Application.class);

		Connection dbConnection = null;
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

			try
			{
				dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
			}
			catch (SQLException openEx)
			{
				if (com.markallenjohnson.assurance.model.migration.H2Migrator.isWriteFormat1Error(openEx))
				{
					// Legacy 1.x DB written by H2 1.4.x — auto-migrate once,
					// then retry. The migrator preserves the original as
					// <path>.1.4-backup-<iso8601-timestamp> so the upgrade is
					// reversible if anything goes wrong downstream.
					com.markallenjohnson.assurance.model.migration.H2Migrator.migrate(dbUrl, dbUser, dbPassword);
					dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
				}
				else
				{
					throw openEx;
				}
			}

			// The schema script is idempotent, but re-running it on every launch
			// still parses the whole file and executes dozens of statements
			// against H2 — noticeable on large on-disk databases. When core
			// tables already exist, skip the script and rely on Hibernate's
			// schema validate at EMF startup to catch drift. (Set JVM system
			// property {@code assurance.logSchemaScript=true} to echo the
			// script to stdout when debugging bootstrap.)
			if (dbScriptStream != null)
			{
				if (assuranceSchemaTablesExist(dbConnection))
				{
					logger.info("Assurance schema already present; skipping assurance.sql bootstrap.");
				}
				else
				{
					ScriptRunner runner = new ScriptRunner(dbConnection, true, true);
					if (!Boolean.getBoolean("assurance.logSchemaScript"))
					{
						runner.setLogWriter(null);
					}
					Reader dbScript = new InputStreamReader(dbScriptStream);
					runner.runScript(dbScript);
					logger.info("Database schema script executed (idempotent — applies only what's missing).");
				}
				HibernateSequenceRealigner.realignHibernateSequence(dbConnection);
			}
		}
		finally
		{
			if (dbConnection != null)
			{
				dbConnection.close();
				dbConnection = null;
			}
		}
		
		logger = null;
	}

	/**
	 * Detects whether the Assurance core schema has already been installed.
	 * Used to skip {@code assurance.sql} on warm starts; first boot on an empty
	 * H2 file still runs the script.
	 */
	private static boolean assuranceSchemaTablesExist(Connection connection) throws SQLException
	{
		try (Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(
				"SELECT 1 FROM INFORMATION_SCHEMA.TABLES "
					+ "WHERE UPPER(TABLE_SCHEMA) = 'PUBLIC' AND UPPER(TABLE_NAME) = 'SCAN_DEF'"))
		{
			return rs.next();
		}
	}
}
