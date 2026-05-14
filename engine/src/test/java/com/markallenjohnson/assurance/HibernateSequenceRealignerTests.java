/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.junit.Test;

import com.markallenjohnson.assurance.model.migration.HibernateSequenceRealigner;

/**
 * {@link HibernateSequenceRealigner} prevents duplicate PK inserts when a
 * migrated 1.x database carries large {@code ID} values but
 * {@code HIBERNATE_SEQUENCE} was (re)created near zero.
 */
public class HibernateSequenceRealignerTests
{
	private static final String MEM_URL =
		"jdbc:h2:mem:hibernateSequenceRealignerTests;DB_CLOSE_DELAY=-1";

	@Test
	public void realignAfterLowSequenceWithHighExistingScanId() throws IOException, SQLException
	{
		Properties p = new Properties();
		p.setProperty("jdbc.driverClassName", "org.h2.Driver");
		p.setProperty("jdbc.url", MEM_URL);
		p.setProperty("jdbc.username", "assurance");
		p.setProperty("jdbc.password", "assurance");

		try (InputStream props = propsToStream(p);
			InputStream script = Application.class.getClassLoader()
				.getResourceAsStream("database/assurance.sql"))
		{
			Application.installDb(props, script);
		}

		try (Connection c = DriverManager.getConnection(MEM_URL, "assurance", "assurance");
			Statement st = c.createStatement())
		{
			st.execute(
				"INSERT INTO SCAN (ID, SCAN_DEF_ID, WHEN_STARTED) "
					+ "VALUES (50000, NULL, TIMESTAMP '2020-01-01 00:00:00')");
			st.execute("ALTER SEQUENCE HIBERNATE_SEQUENCE RESTART WITH 1");

			HibernateSequenceRealigner.realignHibernateSequence(c);

			try (ResultSet rs = st.executeQuery("SELECT NEXT VALUE FOR HIBERNATE_SEQUENCE"))
			{
				assertTrue(rs.next());
				assertEquals(50001L, rs.getLong(1));
			}
		}
	}

	private static InputStream propsToStream(Properties p) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		for (String name : p.stringPropertyNames())
		{
			sb.append(name).append('=').append(p.getProperty(name)).append('\n');
		}
		return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
	}
}
