/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.model.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Aligns {@code HIBERNATE_SEQUENCE} with existing primary-key values after a
 * legacy H2 1.x → 2.x migration (SCRIPT / RUNSCRIPT) or any database where the
 * sequence was created fresh while rows already carry large {@code ID} values.
 *
 * <p>Hibernate is configured with {@code hibernate.id.db_structure_naming_strategy}
 * {@code single}, so every {@code @GeneratedValue(AUTO)} entity draws from the
 * same {@code HIBERNATE_SEQUENCE}. If that sequence lags behind the maximum
 * {@code ID} already present in any of those tables, the next insert re-uses an
 * existing primary key and surfaces as a unique-constraint violation when the
 * user runs a scan (new {@code Scan}, {@code FileReference}, …).
 */
public final class HibernateSequenceRealigner
{
	private static final Logger logger = LogManager.getLogger(HibernateSequenceRealigner.class);

	private HibernateSequenceRealigner()
	{
	}

	/**
	 * If {@code HIBERNATE_SEQUENCE} exists, restarts it so the next value is
	 * strictly greater than every {@code ID} in the tables that participate in
	 * the global Hibernate sequence. No-op when the sequence is missing.
	 */
	public static void realignHibernateSequence(Connection connection) throws SQLException
	{
		if (!hibernateSequenceExists(connection))
		{
			return;
		}
		long nextStart = computeNextSequenceStart(connection);
		try (Statement st = connection.createStatement())
		{
			st.execute("ALTER SEQUENCE IF EXISTS HIBERNATE_SEQUENCE RESTART WITH " + nextStart);
		}
		logger.debug(
			"Realigned HIBERNATE_SEQUENCE so the next generated id is at least {} (legacy / migrated DB safety).",
			Long.valueOf(nextStart));
	}

	static boolean hibernateSequenceExists(Connection connection) throws SQLException
	{
		String sql =
			"SELECT COUNT(*) FROM INFORMATION_SCHEMA.SEQUENCES "
				+ "WHERE UPPER(SEQUENCE_SCHEMA) = 'PUBLIC' AND UPPER(SEQUENCE_NAME) = 'HIBERNATE_SEQUENCE'";
		try (Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(sql))
		{
			return rs.next() && rs.getLong(1) > 0;
		}
	}

	/**
	 * One more than the greatest {@code ID} across all entity tables that share
	 * {@code HIBERNATE_SEQUENCE}. At least {@code 1}.
	 */
	static long computeNextSequenceStart(Connection connection) throws SQLException
	{
		String sql =
			"SELECT GREATEST("
				+ "COALESCE((SELECT MAX(ID) FROM APP_CONFIGURATION), 0), "
				+ "COALESCE((SELECT MAX(ID) FROM SCAN_DEF), 0), "
				+ "COALESCE((SELECT MAX(ID) FROM SCAN), 0), "
				+ "COALESCE((SELECT MAX(ID) FROM FILE_REFERENCE), 0), "
				+ "COALESCE((SELECT MAX(ID) FROM FILE_ATTRIBUTES), 0), "
				+ "COALESCE((SELECT MAX(ID) FROM COMPARISON_RESULT), 0), "
				+ "COALESCE((SELECT MAX(ID) FROM SCAN_MAPPING_DEF), 0)"
				+ ") + 1";
		try (Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(sql))
		{
			rs.next();
			long next = rs.getLong(1);
			return next < 1 ? 1 : next;
		}
	}
}
