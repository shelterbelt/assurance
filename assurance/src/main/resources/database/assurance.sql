-- Assurance
-- 
-- Created by Mark Johnson
-- 
-- Copyright (c) 2015 Mark Johnson
--
-- Copyright 2015 Mark Johnson
-- 
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--    http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

SET AUTOCOMMIT = 0;

--
-- Table structure for table APP_CONFIGURATION
--

DROP TABLE IF EXISTS APP_CONFIGURATION;
CREATE MEMORY TABLE IF NOT EXISTS APP_CONFIGURATION (
  ID IDENTITY NOT NULL,
  IGNORED_FILES varchar(255) NOT NULL DEFAULT '',
  IGNORED_EXT varchar(255) NOT NULL DEFAULT '',
  NUM_THREADS int NOT NULL DEFAULT 4,
  PRIMARY KEY (ID)
);

-- --------------------------------------------------------

--
-- Table structure for table SCAN_DEF
--

DROP TABLE IF EXISTS SCAN_DEF;
CREATE MEMORY TABLE IF NOT EXISTS SCAN_DEF (
  ID IDENTITY NOT NULL,
  NAME varchar(255) NOT NULL,
  STRATEGY tinyint NOT NULL,
  AUTO_RESOLVE bit NOT NULL DEFAULT 0,
  INCLUDE_TIMESTAMPS bit NOT NULL DEFAULT 0,
  INCLUDE_ADVANCED_ATTRIBUTES bit NOT NULL DEFAULT 0,
  PRIMARY KEY (ID)
);

-- --------------------------------------------------------

--
-- Table structure for table SCAN
--

DROP TABLE IF EXISTS SCAN;
CREATE MEMORY TABLE IF NOT EXISTS SCAN (
  ID IDENTITY NOT NULL,
  SCAN_DEF_ID bigint,
  WHEN_STARTED timestamp NOT NULL,
  WHEN_COMPLETED timestamp,
  PRIMARY KEY (ID),
  FOREIGN KEY (SCAN_DEF_ID) REFERENCES SCAN_DEF (ID) ON DELETE SET NULL
);

-- --------------------------------------------------------

--
-- Table structure for table FILE_REFERENCE
--

DROP TABLE IF EXISTS FILE_REFERENCE;
CREATE MEMORY TABLE IF NOT EXISTS FILE_REFERENCE (
  ID IDENTITY NOT NULL,
  LOCATION varchar(8192) NOT NULL DEFAULT '',
  ATTRIBUTES bigint,
  SCAN_MAPPING_DEF_ID bigint,
  PRIMARY KEY (ID)
);

-- --------------------------------------------------------

--
-- Table structure for table FILE_ATTRIBUTES
--

DROP TABLE IF EXISTS FILE_ATTRIBUTES;
CREATE MEMORY TABLE IF NOT EXISTS FILE_ATTRIBUTES (
  ID IDENTITY NOT NULL,
  FILE_REFERENCE bigint,
  CONTENTS_HASH varchar(512),
  -- Basic File Attributes
  CREATION_TIME timestamp,
  IS_DIRECTORY bit,
  IS_OTHER bit,
  IS_REGULAR_FILE bit,
  IS_SYMBOLIC_LINK bit,
  LAST_ACCESS_TIME timestamp,
  LAST_MODIFIED_TIME timestamp,
  FILE_SIZE bigint,
  -- DOS Attributes
  IS_ARCHIVE bit,
  IS_HIDDEN bit,
  IS_READ_ONLY bit,
  IS_SYSTEM bit,
  -- POSIX Attributes
  GROUP_NAME varchar(256),
  OWNER varchar(256),
  PERMISSIONS varchar(256),
  -- File Owner Attributes
  FILE_OWNER varchar(256),
  -- ACL Attributes
  ACL_DESCRIPTION varchar(256),
  -- User-defined Attributes
  USER_DEFINED_ATTRIBUTES_HASH varchar(256),
  PRIMARY KEY (ID),
  FOREIGN KEY (FILE_REFERENCE) REFERENCES FILE_REFERENCE (ID)
);

-- --------------------------------------------------------

--
-- Table structure for table COMPARISON_RESULT
--

DROP TABLE IF EXISTS COMPARISON_RESULT;
CREATE MEMORY TABLE IF NOT EXISTS COMPARISON_RESULT (
  ID IDENTITY NOT NULL,
  SCAN_ID bigint NOT NULL,
  TARGET_REFERENCE bigint NOT NULL,
  SOURCE_REFERENCE bigint NOT NULL,
  REASON tinyint NOT NULL DEFAULT 0,
  RESOLUTION tinyint NOT NULL DEFAULT 0,
  RESOLUTION_ERROR varchar(256),
  PRIMARY KEY (ID),
  FOREIGN KEY (SCAN_ID) REFERENCES SCAN (ID),
  FOREIGN KEY (TARGET_REFERENCE) REFERENCES FILE_REFERENCE (ID),
  FOREIGN KEY (SOURCE_REFERENCE) REFERENCES FILE_REFERENCE (ID)
);

-- --------------------------------------------------------

--
-- Table structure for table SCAN_MAPPING_DEF
--

DROP TABLE IF EXISTS SCAN_MAPPING_DEF;
CREATE MEMORY TABLE IF NOT EXISTS SCAN_MAPPING_DEF (
  ID IDENTITY NOT NULL,
  SCAN_DEF_ID bigint NOT NULL,
  TARGET_REFERENCE bigint NOT NULL,
  SOURCE_REFERENCE bigint NOT NULL,
  PRIMARY KEY (ID),
  FOREIGN KEY (SCAN_DEF_ID) REFERENCES SCAN_DEF (ID),
  FOREIGN KEY (TARGET_REFERENCE) REFERENCES FILE_REFERENCE (ID),
  FOREIGN KEY (SOURCE_REFERENCE) REFERENCES FILE_REFERENCE (ID)
);

-- --------------------------------------------------------

ALTER TABLE FILE_REFERENCE ADD FOREIGN KEY (ATTRIBUTES) REFERENCES FILE_ATTRIBUTES (ID);
ALTER TABLE FILE_REFERENCE ADD FOREIGN KEY (SCAN_MAPPING_DEF_ID) REFERENCES SCAN_MAPPING_DEF (ID);

CREATE UNIQUE INDEX scan_id ON SCAN (ID);
CREATE UNIQUE INDEX comparison_id ON COMPARISON_RESULT (ID);
CREATE UNIQUE INDEX file_reference_id ON FILE_REFERENCE (ID);
CREATE UNIQUE INDEX file_attributes_id ON FILE_ATTRIBUTES (ID);
CREATE UNIQUE INDEX scan_def_id ON SCAN_DEF (ID);
CREATE UNIQUE INDEX scan_mapping_id ON SCAN_MAPPING_DEF (ID);

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence; 

INSERT INTO APP_CONFIGURATION (IGNORED_FILES) VALUES ('.DS_Store, Thumbs.db, desktop.ini');
