DROP TABLE IF EXISTS tblYcValue;
create table tblYcValue (id int, CONTROLAREA int,CZH int,YCH int,YcValue double,RefreshTime TIMESTAMP,constraint PK_TBLYCVALUE primary key (id));
DROP TABLE IF EXISTS tblYxValue;
create table tblYxValue (id int, CONTROLAREA int,CZH int,YXH int,YxValue int,RefreshTime TIMESTAMP, sb_time TIMESTAMP,constraint PK_TBLYXVALUE primary key (id));
DROP TABLE IF EXISTS tblcommand; 
create table tblcommand (id int, 
CONTROLAREA int, 
schemeid int,
schemeindex int, 
cmddatetime TIMESTAMP,
dealdatetime TIMESTAMP, 
czh int, 
ykyth int,
ykytvalue int,
ytvalue float,
ykyttype int,
DEALTAG int);
