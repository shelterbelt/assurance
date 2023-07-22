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

package com.markallenjohnson.assurance.model;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.dialect.H2Dialect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class HibernateConfiguration
{
	@Value("#{dataSource}")
	private DataSource dataSource;

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory()
	{
		Properties props = this.hibernateProperties();

		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setPersistenceUnitName("assurancePersistenceUnit");
		em.setDataSource(this.dataSource);
		em.setPackagesToScan(new String[] { "com.markallenjohnson.assurance.model.entities" });

		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		vendorAdapter = null;
		em.setJpaProperties(props);
		props = null;
		
		return em;
	}

	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory emf)
	{
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(emf);

		return transactionManager;
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor exceptionTranslation()
	{
		return new PersistenceExceptionTranslationPostProcessor();
	}

	// The default configuration of this appears to be working.
	//
	// @Bean
	// public LocalSessionFactoryBean sessionFactory()
	// {
	//	 LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
	//	 sessionFactory.setDataSource(this.dataSource);
	//	 sessionFactory.setPackagesToScan(new String[] {
	//	 "com.markallenjohnson.assurance" });
	//	 sessionFactory.setHibernateProperties(this.hibernateProperties());
	//	 
	//	 return sessionFactory;
	// }

	private Properties hibernateProperties()
	{
		return new Properties()
		{
			private static final long serialVersionUID = 1L;
			{
				setProperty("hibernate.dialect", H2Dialect.class.getName());
				setProperty("hibernate.hbm2ddl.auto", "validate");
				
				// NOTE: Enable these properties in a development release.
				setProperty("hibernate.show_sql", "false");
				setProperty("hibernate.format_sql", "false");
			}
		};
	}
}
