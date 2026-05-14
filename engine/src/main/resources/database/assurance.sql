-- Assurance
-- 
-- Created by Mark Johnson
-- 
-- Copyright (c) 2015 - 2023 Mark Johnson
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

-- Column-level deltas from 1.x to 2.0. These ALTERs are idempotent in H2 2.x
-- and let a migrated 1.x DB pick up the 2.0 schema additions without losing data.
ALTER TABLE FILE_REFERENCE ADD COLUMN IF NOT EXISTS SCAN_MAPPING_DEF_ID bigint;
ALTER TABLE FILE_ATTRIBUTES ADD COLUMN IF NOT EXISTS CONTENTS_HASH varchar(512);

-- H2 2.x's RUNSCRIPT promotes 1.4.x TINYINT to INTEGER in the dump round-trip,
-- which then fails Hibernate's strict schema-validate. Coerce the enum-ordinal
-- columns back to TINYINT. On fresh 2.0 DBs these are already TINYINT — the
-- ALTERs are no-ops there.
ALTER TABLE COMPARISON_RESULT ALTER COLUMN REASON SET DATA TYPE TINYINT;
ALTER TABLE COMPARISON_RESULT ALTER COLUMN RESOLUTION SET DATA TYPE TINYINT;
ALTER TABLE SCAN_DEF ALTER COLUMN STRATEGY SET DATA TYPE TINYINT;

ALTER TABLE FILE_REFERENCE ADD CONSTRAINT IF NOT EXISTS FK_FILE_REFERENCE_ATTRIBUTES FOREIGN KEY (ATTRIBUTES) REFERENCES FILE_ATTRIBUTES (ID);
ALTER TABLE FILE_REFERENCE ADD CONSTRAINT IF NOT EXISTS FK_FILE_REFERENCE_SCAN_MAPPING_DEF FOREIGN KEY (SCAN_MAPPING_DEF_ID) REFERENCES SCAN_MAPPING_DEF (ID);

CREATE UNIQUE INDEX IF NOT EXISTS scan_id ON SCAN (ID);
CREATE UNIQUE INDEX IF NOT EXISTS comparison_id ON COMPARISON_RESULT (ID);
CREATE UNIQUE INDEX IF NOT EXISTS file_reference_id ON FILE_REFERENCE (ID);
CREATE UNIQUE INDEX IF NOT EXISTS file_attributes_id ON FILE_ATTRIBUTES (ID);
CREATE UNIQUE INDEX IF NOT EXISTS scan_def_id ON SCAN_DEF (ID);
CREATE UNIQUE INDEX IF NOT EXISTS scan_mapping_id ON SCAN_MAPPING_DEF (ID);

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence;

-- Conditional default-config insert: only fires when no APP_CONFIGURATION row
-- exists yet (fresh install or post-1.x-migration where the legacy DB never
-- had the table). Re-runs against a populated 2.0 DB are no-ops.
INSERT INTO APP_CONFIGURATION (IGNORED_FILES)
  SELECT '.DS_Store, Thumbs.db, desktop.ini'
  WHERE NOT EXISTS (SELECT 1 FROM APP_CONFIGURATION);
