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

package com.markallenjohnson.assurance.model.compare.file;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface IFileComparor
{

	// NOTE:  This bleed of configuration into the functional signatures isn't ideal.  
	// Would like to see a better implementation.
	boolean compare(File file1, File file2, boolean includeTimestamps, boolean includeAdvancedAttributes) throws NoSuchAlgorithmException, IOException;

	byte[] calculateHashForFile(File file) throws NoSuchAlgorithmException, IOException;
}
