DROP TABLE IF EXISTS CONFIGURATION;
DROP TABLE IF EXISTS LOCATION;
DROP TABLE IF EXISTS CLAIM;
DROP TABLE IF EXISTS PERSON;
DROP TABLE IF EXISTS ATTACHMENT;
DROP TABLE IF EXISTS ADDITIONAL_INFO;
DROP TABLE IF EXISTS VERTEX;
DROP TABLE IF EXISTS CLAIM_STATUS;
DROP TABLE IF EXISTS ATTACHMENT_STATUS;
DROP TABLE IF EXISTS ADJACENCY;
DROP TABLE IF EXISTS OWNER;
DROP TABLE IF EXISTS CARDINAL_DIRECTION;
DROP TABLE IF EXISTS CLAIM_TYPE;
DROP TABLE IF EXISTS DOCUMENT_TYPE;
	
CREATE TABLE CONFIGURATION
(CONFIGURATION_ID VARCHAR(255) PRIMARY KEY,
NAME VARCHAR(255) NOT NULL,
VALUE VARCHAR(255));

CREATE UNIQUE INDEX CONFIGURATION_NAME_IDX ON CONFIGURATION(NAME);

MERGE INTO CONFIGURATION(CONFIGURATION_ID, NAME, VALUE) KEY (CONFIGURATION_ID) SELECT '1', 'DBVERSION', '1.0.0' FROM DUAL;

CREATE TABLE PERSON
(PERSON_ID VARCHAR(255) PRIMARY KEY,
FIRST_NAME VARCHAR(255) NOT NULL,
LAST_NAME VARCHAR(255) NOT NULL,
DATE_OF_BIRTH DATE,
PLACE_OF_BIRTH VARCHAR(255),
EMAIL_ADDRESS VARCHAR(255),
POSTAL_ADDRESS VARCHAR(255),
MOBILE_PHONE_NUMBER VARCHAR(255),
CONTACT_PHONE_NUMBER VARCHAR(255),
GENDER CHAR(1));

CREATE TABLE LOCATION
(LOCATION_ID VARCHAR(255) PRIMARY KEY, 
NAME VARCHAR(255) NOT NULL, 
LAT DECIMAL(15,10) NOT NULL, 
LON DECIMAL(15,10) NOT NULL);

CREATE UNIQUE INDEX LOCATION_NAME_IDX ON LOCATION(NAME);

CREATE TABLE CLAIM_STATUS
(ID INT auto_increment PRIMARY KEY,
 STATUS VARCHAR(255) NOT NULL);
 
CREATE TABLE ATTACHMENT_STATUS
(ID INT auto_increment PRIMARY KEY,
 STATUS VARCHAR(255) NOT NULL);
 
CREATE TABLE CLAIM_TYPE
(ID INT auto_increment PRIMARY KEY,
 TYPE VARCHAR(255) NOT NULL,
 DISPLAY_VALUE VARCHAR(255) NOT NULL,
 DESCRIPTION VARCHAR(255)); 
 
CREATE UNIQUE INDEX CLAIM_TYPE_IDX ON CLAIM_TYPE(TYPE); 
 
 CREATE TABLE DOCUMENT_TYPE
(ID INT auto_increment PRIMARY KEY,
 CODE VARCHAR(255) NOT NULL,
 DISPLAY_VALUE VARCHAR(255) NOT NULL,
 DESCRIPTION VARCHAR(255)); 
 
CREATE UNIQUE INDEX DOCUMENT_TYPE_IDX ON DOCUMENT_TYPE(CODE);
 
CREATE TABLE CLAIM
(CLAIM_ID VARCHAR(255) PRIMARY KEY, 
NAME VARCHAR(255) NOT NULL, 
PERSON_ID VARCHAR(255) NOT NULL, 
CHALLENGED_CLAIM_ID VARCHAR(255),
CHALLANGE_EXPIRY_DATE DATE,
STATUS VARCHAR(255),
TYPE VARCHAR(255),
FOREIGN KEY (CHALLENGED_CLAIM_ID)
REFERENCES CLAIM(CLAIM_ID),
FOREIGN KEY (PERSON_ID)
REFERENCES PERSON(PERSON_ID),
FOREIGN KEY (TYPE)
REFERENCES CLAIM_TYPE(TYPE),
FOREIGN KEY (STATUS)
REFERENCES CLAIM_STATUS(STATUS));

CREATE UNIQUE INDEX CLAIM_NAME_IDX ON CLAIM(NAME);

CREATE TABLE CARDINAL_DIRECTION
(ID INT auto_increment PRIMARY KEY,
 DIRECTION VARCHAR(255) NOT NULL);
 
CREATE TABLE ADJACENCY
(SOURCE_CLAIM_ID VARCHAR(255) NOT NULL, 
DEST_CLAIM_ID VARCHAR(255) NOT NULL, 
CARDINAL_DIRECTION VARCHAR(255) NOT NULL,
PRIMARY KEY (SOURCE_CLAIM_ID, DEST_CLAIM_ID),
FOREIGN KEY (SOURCE_CLAIM_ID)
REFERENCES CLAIM(CLAIM_ID),
FOREIGN KEY (CARDINAL_DIRECTION)
REFERENCES CARDINAL_DIRECTION(DIRECTION),
FOREIGN KEY (DEST_CLAIM_ID)
REFERENCES CLAIM(CLAIM_ID));

CREATE TABLE OWNER
(ID VARCHAR(255) PRIMARY KEY,
CLAIM_ID VARCHAR(255) NOT NULL, 
PERSON_ID VARCHAR(255) NOT NULL,
OWNER_ID VARCHAR(255) NOT NULL,
SHARES DECIMAL(4,0) NOT NULL DEFAULT 1,
FOREIGN KEY (CLAIM_ID)
REFERENCES CLAIM(CLAIM_ID),
FOREIGN KEY (PERSON_ID)
REFERENCES PERSON(PERSON_ID));

CREATE TABLE VERTEX
(VERTEX_ID VARCHAR(255) PRIMARY KEY, 
CLAIM_ID VARCHAR(255) NOT NULL,
SEQUENCE_NUMBER INT NOT NULL,
GPS_LAT DECIMAL(15,10),
GPS_LON DECIMAL(15,10),
MAP_LAT DECIMAL(15,10) NOT NULL,
MAP_LON DECIMAL(15,10) NOT NULL,
FOREIGN KEY (CLAIM_ID)
REFERENCES CLAIM(CLAIM_ID));

CREATE UNIQUE INDEX CLAIM_VERTEX_IDX ON VERTEX(CLAIM_ID,SEQUENCE_NUMBER);

CREATE TABLE ADDITIONAL_INFO
(ADDITIONAL_INFO_ID VARCHAR(255) PRIMARY KEY,
CLAIM_ID VARCHAR(255) NOT NULL, 
NAME VARCHAR(255) NOT NULL, 
VALUE VARCHAR(255), 
FOREIGN KEY (CLAIM_ID)
REFERENCES CLAIM(CLAIM_ID));

CREATE UNIQUE INDEX CLAIM_ADDITIONAL_INFO_IDX ON ADDITIONAL_INFO(CLAIM_ID,NAME);

CREATE TABLE ATTACHMENT
(ATTACHMENT_ID VARCHAR(255) PRIMARY KEY, 
STATUS VARCHAR(255),
CLAIM_ID VARCHAR(255) NOT NULL, 
DESCRIPTION VARCHAR(255) NOT NULL, 
FILE_NAME VARCHAR(255) NOT NULL, 
FILE_TYPE VARCHAR(255) NOT NULL, 
MIME_TYPE VARCHAR(255) NOT NULL, 
MD5SUM VARCHAR(255), 
PATH VARCHAR(255) NOT NULL,
SIZE BIGINT,
FOREIGN KEY (CLAIM_ID)
REFERENCES CLAIM(CLAIM_ID),
FOREIGN KEY (STATUS)
REFERENCES ATTACHMENT_STATUS(STATUS));

MERGE INTO LOCATION(LOCATION_ID, NAME, LAT, LON) KEY (LOCATION_ID) SELECT '1', 'CURRENT', 0.0, 0.0 FROM DUAL;
MERGE INTO LOCATION(LOCATION_ID, NAME, LAT, LON) KEY (LOCATION_ID) SELECT '2', 'HOME', 0.0, 0.0 FROM DUAL;

INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('NORTH');
INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('SOUTH');
INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('EAST');
INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('WEST');
INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('NORTHEAST');
INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('NORTHWEST');
INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('SOUTHEAST');
INSERT INTO CARDINAL_DIRECTION (DIRECTION) VALUES ('SOUTHWEST');

INSERT INTO CLAIM_STATUS (STATUS) VALUES ('created');
INSERT INTO CLAIM_STATUS (STATUS) VALUES ('uploading');
INSERT INTO CLAIM_STATUS (STATUS) VALUES ('unmoderated');
INSERT INTO CLAIM_STATUS (STATUS) VALUES ('moderated');
INSERT INTO CLAIM_STATUS (STATUS) VALUES ('challenged');
INSERT INTO CLAIM_STATUS (STATUS) VALUES ('upload_incomplete');
INSERT INTO CLAIM_STATUS (STATUS) VALUES ('upload_error');

INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('created');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('uploading');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('uploaded');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('deleted');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('upload_incomplete');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('upload_error');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('download_incomplete');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('download_failed');
INSERT INTO ATTACHMENT_STATUS (STATUS) VALUES ('downloading');




 
 

