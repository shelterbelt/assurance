/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 Digital Generalists, LLC.
 * 
 */
/*
 * Copyright 2015 Digital Generalists, LLC.
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

package com.digitalgeneralists.assurance.model.compare.file.attributes;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.attribute.UserDefinedFileAttributeView;

public abstract class FileAttributeCompareValidator implements IFileAttributeComparor
{
	public abstract boolean compareFileAttributes(File file1, File file2, boolean includeTimestamps, boolean includeAdvancedAttributes) throws IOException;

	protected String readUserDefinedFileAttribute(UserDefinedFileAttributeView fileAttributes, String attribute) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(fileAttributes.size(attribute));
		fileAttributes.read(attribute, buffer);
		buffer.flip();
		
		String result = Charset.defaultCharset().decode(buffer).toString();
		buffer = null;
		
		return result;
	}
}
