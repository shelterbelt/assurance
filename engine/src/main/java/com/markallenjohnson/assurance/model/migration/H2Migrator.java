/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.model.migration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * One-shot migrator from H2 1.4.x ("MV-store write format 1") to H2 2.x
 * ("MV-store write format 2").
 *
 * <p>The legacy 1.x Assurance Swing app shipped with H2 1.4.184 and stored its
 * database at {@code ~/.assurance/assurance.mv.db}. The 2.0 engine packs
 * H2 2.x, which throws {@link SQLException} (cause:
 * {@code MVStoreException: The write format 1 is smaller than the supported
 * format 2}) when asked to open a 1.4-format file. There is no in-place
 * upgrade path — H2's documented procedure is "open with the old jar, SCRIPT
 * to SQL, open with the new jar, RUNSCRIPT".
 *
 * <p>This migrator implements that procedure inside a single JVM by isolating
 * the legacy driver inside a {@link URLClassLoader}. The bundled
 * {@code h2-1.4.200.jar} resource (under {@code /migrations/}) is extracted to
 * a temporary location, loaded by a child classloader rooted at the
 * <em>platform</em> classloader (so the application's H2 2.x driver is not
 * visible to the legacy code), used to dump the legacy DB to a SQL file via
 * the {@code SCRIPT TO} statement, and then unloaded. The original
 * {@code .mv.db} is renamed with a {@code .1.4-backup-<timestamp>} suffix and
 * a fresh 2.x database is created at the original path by running
 * {@code RUNSCRIPT FROM} against the 2.x driver in the application's
 * classpath.
 *
 * <p>The migrator is idempotent at the level of "if no migration is needed,
 * do nothing": callers attempt a normal 2.x connection first, and only invoke
 * {@link #migrate} after catching the format-1 error.
 */
public final class H2Migrator
{
	private static final Logger logger = LogManager.getLogger(H2Migrator.class);

	// 1.4.197 (and not the final 1.4.200) is intentional: the bundled 1.x
	// Assurance shipped with H2 1.4.184, and DB files written across the
	// 1.4.184–1.4.197 lineage carry MV-store internal types that the very last
	// 1.4.x release (1.4.200) refuses to read. 1.4.197 reads the full lineage
	// while still being a release H2 version we can pull from Maven Central.
	private static final String LEGACY_JAR_RESOURCE = "/migrations/h2-1.4.197.jar";
	private static final String LEGACY_DRIVER_CLASS = "org.h2.Driver";
	private static final String FORMAT_1_MARKER = "write format 1";

	private H2Migrator()
	{
	}

	/**
	 * Returns {@code true} when the supplied exception (or any of its causes)
	 * carries the H2 "write format 1" marker — i.e. this is a 1.4.x DB being
	 * opened by a 2.x driver.
	 */
	public static boolean isWriteFormat1Error(Throwable ex)
	{
		Throwable t = ex;
		while (t != null)
		{
			String message = t.getMessage();
			if (message != null && message.contains(FORMAT_1_MARKER))
			{
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Runs the 1.4 → 2.x migration end-to-end. The caller has already attempted
	 * to open the database with the application's H2 2.x driver and caught the
	 * format-1 {@link SQLException}.
	 *
	 * <p>On success the original {@code .mv.db} file is preserved as
	 * {@code <original>.1.4-backup-<iso8601-timestamp>} and the path the URL
	 * resolves to now contains a fresh H2 2.x database with the same schema +
	 * data as the 1.4 original. The caller can re-issue
	 * {@code DriverManager.getConnection} against the same URL.
	 *
	 * <p>On failure the original {@code .mv.db} is left untouched (the rename
	 * happens only after the SCRIPT dump succeeds, and a failed RUNSCRIPT
	 * leaves the empty 2.x file but the {@code .1.4-backup-*} carries the
	 * original).
	 *
	 * @param dbUrl JDBC URL of the form {@code jdbc:h2:[file:][/abs|./rel]/path}
	 *   (the H2 driver appends {@code .mv.db} when locating the on-disk file).
	 * @param dbUser the JDBC username (matches what the application uses).
	 * @param dbPassword the JDBC password (matches what the application uses).
	 */
	public static void migrate(String dbUrl, String dbUser, String dbPassword) throws IOException, SQLException
	{
		logger.warn("Detected H2 1.4-format database; running one-shot migration to H2 2.x at URL {}.", dbUrl);
		Path dbFile = resolveDbFile(dbUrl);
		Path backupFile = dbFile.resolveSibling(
			dbFile.getFileName().toString()
				+ ".1.4-backup-"
				+ DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-'));
		Path scriptFile = Files.createTempFile("assurance-h2-migration-", ".sql");
		Path legacyJar = extractLegacyJar();
		try
		{
			dumpLegacyToScript(legacyJar, dbUrl, dbUser, dbPassword, scriptFile);
			Files.move(dbFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
			logger.info("H2 migration: original DB backed up to {}.", backupFile);
			runScriptOnFreshDb(dbUrl, dbUser, dbPassword, scriptFile);
			logger.info("H2 migration: complete — fresh 2.x database at {}.", dbFile);
		}
		finally
		{
			try
			{
				Files.deleteIfExists(scriptFile);
			}
			catch (IOException cleanupEx)
			{
				logger.warn("H2 migration: could not delete temp script {}: {}.", scriptFile, cleanupEx.getMessage());
			}
			try
			{
				Files.deleteIfExists(legacyJar);
			}
			catch (IOException cleanupEx)
			{
				logger.warn("H2 migration: could not delete temp legacy driver jar {}: {}.", legacyJar, cleanupEx.getMessage());
			}
		}
	}

	private static Path extractLegacyJar() throws IOException
	{
		Path tempJar = Files.createTempFile("assurance-h2-legacy-", ".jar");
		try (InputStream in = H2Migrator.class.getResourceAsStream(LEGACY_JAR_RESOURCE))
		{
			if (in == null)
			{
				throw new IOException(
					"Bundled legacy H2 driver missing on classpath at " + LEGACY_JAR_RESOURCE
						+ ". Backward-compat with 1.x H2 databases requires the migration resource.");
			}
			Files.copy(in, tempJar, StandardCopyOption.REPLACE_EXISTING);
		}
		return tempJar;
	}

	private static void dumpLegacyToScript(Path legacyJar, String dbUrl, String dbUser, String dbPassword, Path scriptFile)
		throws IOException, SQLException
	{
		// Parent the loader at the platform classloader so the application's
		// H2 2.x classes (loaded by the system classloader) are NOT visible to
		// the legacy driver. DriverManager is shared globally; we deliberately
		// avoid it here for the same reason and call Driver.connect directly.
		URL[] urls = new URL[] { legacyJar.toUri().toURL() };
		try (URLClassLoader legacy = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader()))
		{
			Driver legacyDriver;
			try
			{
				Class<?> driverClass = Class.forName(LEGACY_DRIVER_CLASS, true, legacy);
				legacyDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();
			}
			catch (ReflectiveOperationException ex)
			{
				throw new SQLException("Failed to instantiate legacy H2 driver from " + legacyJar, ex);
			}

			Properties props = new Properties();
			props.setProperty("user", dbUser);
			props.setProperty("password", dbPassword);
			try (Connection legacyConn = legacyDriver.connect(dbUrl, props);
				 Statement stmt = legacyConn.createStatement())
			{
				if (legacyConn == null)
				{
					throw new SQLException(
						"Legacy H2 driver returned null for URL " + dbUrl
							+ "; the URL did not match any registered driver.");
				}
				stmt.execute("SCRIPT TO '" + asH2StringLiteral(scriptFile) + "'");
				logger.info("H2 migration: dumped legacy DB to script file {} ({} bytes).",
					scriptFile, Files.size(scriptFile));
			}
		}
	}

	private static void runScriptOnFreshDb(String dbUrl, String dbUser, String dbPassword, Path scriptFile)
		throws SQLException
	{
		// At this point the original .mv.db has been moved aside; opening the
		// URL with the application's H2 2.x driver creates an empty file at
		// the same path. RUNSCRIPT loads the legacy schema + data into it.
		try (Connection conn = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword);
			 Statement stmt = conn.createStatement())
		{
			stmt.execute("RUNSCRIPT FROM '" + asH2StringLiteral(scriptFile) + "'");
		}
	}

	/**
	 * Resolves the on-disk {@code .mv.db} file path corresponding to a JDBC URL
	 * of the shape {@code jdbc:h2:[file:][./|/]<path>[;<options>]}.
	 *
	 * <p>The H2 driver appends {@code .mv.db} when locating the MV-store file,
	 * so the URL's path component plus that suffix is the on-disk file. The
	 * relative form ({@code ./...}) is resolved against the current JVM cwd —
	 * in the packaged 2.0 app, that's the user's home directory (per the
	 * launcher's {@code bundledCwd}), so the path lands at
	 * {@code ~/.assurance/assurance.mv.db}, matching the 1.x location.
	 */
	static Path resolveDbFile(String dbUrl)
	{
		String url = dbUrl;
		int semi = url.indexOf(';');
		if (semi >= 0)
		{
			url = url.substring(0, semi);
		}
		String prefix = "jdbc:h2:";
		if (!url.startsWith(prefix))
		{
			throw new IllegalArgumentException("Not an H2 JDBC URL: " + dbUrl);
		}
		String body = url.substring(prefix.length());
		// Optional 'file:' prefix is documented; default mode is also file.
		if (body.startsWith("file:"))
		{
			body = body.substring("file:".length());
		}
		Path path = Paths.get(body + ".mv.db");
		if (!path.isAbsolute())
		{
			path = Paths.get(System.getProperty("user.dir")).resolve(path);
		}
		return path.normalize();
	}

	/**
	 * H2 string literals use single quotes with embedded quotes doubled.
	 * Backslashes (Windows paths) are passed through verbatim.
	 */
	private static String asH2StringLiteral(Path path)
	{
		return path.toAbsolutePath().toString().replace("'", "''");
	}
}
