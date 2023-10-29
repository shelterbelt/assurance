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

package com.markallenjohnson.assurance.model;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.hibernate.Hibernate;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.markallenjohnson.assurance.model.compare.file.IFileComparer;

public class ModelUtils 
{
	private ModelUtils() {
		throw new IllegalStateException("Utility class");
	  }
	
	public static IInitializableEntity initializeEntity(IInitializableEntity entity, String propertyKey) 
	{

		IInitializableEntity result = null;
		
		if (entity != null)
		{
			if (!Hibernate.isInitialized(entity.getPropertyToInitialize(propertyKey)))
			{
				// Because we are operating in a Swing application, the session context
				// closes after the initial bootstrap run. There appears to be an
				// "application"-level session configuration that should enable the
				// behavior we want for a desktop application, but there is no documentation
				// for it that I can find. 
				// So, Assurance mimics a web app request/response
				// cycle for calls into the model delegate through the Swing
				// application. All calls to the model initiated directly from the UI
				// essentially operate as a new "request" and rebuild the Hibernate and
				// Spring session contexts for use within that operation.
				ClassPathXmlApplicationContext springContext = null;
				try
				{
					springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
									IModelDelegate modelDelegate = (IModelDelegate) springContext.getBean("ModelDelegate");

					result = modelDelegate.initializeEntity(entity, propertyKey);
					
					modelDelegate = null;
				}
				finally
				{
					if (springContext != null)
					{
						springContext.close();
					}
					springContext = null;
				}
			}
			else
			{
				result = entity;
			}
		}
		
		return result;
	}
	
	public static String calculateHashForFile(File file) 
	{
		Logger logger = LogManager.getLogger(ModelUtils.class);

		String result = null;
		
		if (file != null)
		{
			// Because we are operating in a Swing application, the session context
			// closes after the initial bootstrap run. There appears to be an
			// "application"-level session configuration that should enable the
			// behavior we want for a desktop application, but there is no documentation
			// for it that I can find. 
			// So, Assurance mimics a web app request/response
			// cycle for calls into the model delegate through the Swing
			// application. All calls to the model initiated directly from the UI
			// essentially operate as a new "request" and rebuild the Hibernate and
			// Spring session contexts for use within that operation.
			ClassPathXmlApplicationContext springContext = null;
			try
			{
				springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
				IFileComparer comparer = (IFileComparer) springContext.getBean("FileCompareValidator");

				byte[] hash = comparer.calculateHashForFile(file);
				if (hash != null)
				{
					result = Arrays.toString(hash);
				}
				else
				{
					result = "";
				}
				if (result.length() > 512)
				{
					result = result.substring(0, 511);
				}
				
				comparer = null;
			}
			catch (NoSuchAlgorithmException | IOException e)
			{
				result = "";
				logger.warn(e);
			}
			finally
			{
				if (springContext != null)
				{
					springContext.close();
				}
				springContext = null;
			}
		}
		
		return result;
	}
}
