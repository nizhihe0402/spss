* Encoding: UTF-8.

******表1-1 省、地市及区县管理部门学校卫生工作调查表.
*1.查看并记录数据库总样本量.
*若省编码取值有异常，一般为录入手误，可直接修正为正确的省编码*

DESCRIPTIVES VARIABLES=ID1 PROVINCE CITY COUNTY 
  /STATISTICS=MEAN STDDEV MIN MAX.

**地市为0且区县为0的是省级数据，地市不为0且区县为0的是地市级数据，地市和区县均不为0为区县级数据，每一条地市级数据下均应对应至少2条区县级数据.
CTABLES
  /VLABELS VARIABLES=PROVINCE CITY COUNTY DISPLAY=LABEL
  /TABLE PROVINCE [S][COUNT F40.0] > CITY [C] > COUNTY [C]
  /CATEGORIES VARIABLES=CITY COUNTY ORDER=A KEY=VALUE EMPTY=EXCLUDE.

*2.查看并标记ID1与各分项编码不一致的样本。0为一致，1为不一致。将不一致样本剔出，另存为一个数据库*

COMPUTE ID3=((10**5)*province)+((10**3)*city)+(county*(10**1))+point.
EXECUTE.

COMPUTE ID是否一致=ID1 - ID3.
EXECUTE .

RECODE ID是否一致 (0=0) (SYSMIS=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS  ID是否一致 'ID是否一致'.
VALUE LABELS ID是否一致 1 '不一致' 0 '一致'.

FREQUENCIES VARIABLES=ID是否一致
  /ORDER=ANALYSIS.

*拷贝ID不一致的数据，SAVE后的路径根据自己实际存储路径进行修改.
DATASET COPY  idx. 
DATASET ACTIVATE  idx. 
FILTER OFF. 
USE ALL. 
SELECT IF ( ID是否一致 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-1\表1-1\01-表1-1-ID不一致.sav' 
  /COMPRESSED.


*3.查看和标记重复样本。PrimaryFirst1=0者为重复样本，将重复样本剔出，另存为一个数据库*
* Identify Duplicate Cases.
SORT CASES BY ID1(A).
MATCH FILES
  /FILE=*
  /BY ID1
  /FIRST=PrimaryFirst1
  /LAST=PrimaryLast.
DO IF (PrimaryFirst1).
COMPUTE  MatchSequence=1-PrimaryLast.
ELSE.
COMPUTE  MatchSequence=MatchSequence+1.
END IF.
LEAVE  MatchSequence.
FORMAT  MatchSequence (f7).
COMPUTE  InDupGrp=MatchSequence>0.
SORT CASES InDupGrp(D).
MATCH FILES
  /FILE=*
  /DROP=PrimaryLast InDupGrp MatchSequence.

VARIABLE LABELS  PrimaryFirst1 '重复样本'.
VALUE LABELS PrimaryFirst1 1  '不重复' 0 '重复'.
VARIABLE LEVEL  PrimaryFirst1 (ORDINAL).
FREQUENCIES VARIABLES=PrimaryFirst1.
EXECUTE.

*拷贝ID重复的数据，SAVE后的路径根据自己实际存储路径进行修改.
DATASET COPY  idxx. 
DATASET ACTIVATE  idxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (PrimaryFirst1 = 0). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-1\02-表1-1-ID重复.sav' 
  /COMPRESSED.

*4.查看基本信息是否有缺失、取值是否有异常*

FREQUENCIES VARIABLES=province city county point 
  /FORMAT=NOTABLE 
  /STATISTICS=MINIMUM MAXIMUM MEAN MEDIAN 
  /ORDER=ANALYSIS.

*标记基本信息缺失和异常样本。0为正常，1为缺失，2为取值异常。将缺失或取值异常样本剔出并另存*
*直辖市有可能county（县区）指标是缺失或者0值*

RECODE city (SYSMIS=1)(ELSE=0) INTO 地市编码缺失.
EXECUTE.
RECODE county (SYSMIS=1)(ELSE=0) INTO 县区编码缺失.
EXECUTE.
RECODE point (0 thru 2=0)(SYSMIS=1)(ELSE=1) INTO 城乡编码缺失或异常.
EXECUTE.

VALUE LABELS 地市编码缺失 县区编码缺失 城乡编码缺失或异常 1 '缺失' 0 '不缺失' 2 '异常'.

FREQUENCIES VARIABLES=地市编码缺失 县区编码缺失 城乡编码缺失或异常 
  /ORDER=ANALYSIS.

*拷贝基本信息缺失数据.
DATASET COPY  infoxx. 
DATASET ACTIVATE  infoxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 =1 OR 县区编码缺失=1 OR 城乡编码缺失或异常 =1). 
EXECUTE.
DATASET ACTIVATE  infoxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-1\03-表1-1-基本信息缺失.sav' 
  /COMPRESSED.



*5.拷贝清理后数据库，删除前几项应删除的数据.
DATASET COPY  bbb. 
DATASET ACTIVATE  bbb. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 0 AND 县区编码缺失=0 AND 城乡编码缺失或异常=0 AND ID是否一致=0 AND PrimaryFirst1=1). 
EXECUTE. 
DATASET ACTIVATE  bbb. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-1\表1-1-清理后.sav' 
  /COMPRESSED.



*6.数据逻辑检验-人员配备情况.
*题一、（一）2.人员配备，从事学校卫生工作人员数应=专职+兼职.
COMPUTE 人员配备1=A121-A122-A123.
EXECUTE.
RECODE 人员配备1 (0=0) (SYSMIS=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS 人员配备1 '疾控部门人员配备'.
VALUE LABELS 人员配备1 1'不一致' 0'一致'.

*题一、（二）2.人员配备，从事学校卫生工作人员数应=专职+兼职.
COMPUTE 人员配备2=A21-A22-A23.
EXECUTE.
RECODE 人员配备2 (0=0) (SYSMIS=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS 人员配备2 '疾控中心人员配备'.
VALUE LABELS 人员配备2 1'不一致' 0'一致'.

*题一、（三）2.人员配备，从事学校卫生工作人员数应=专职+兼职.
COMPUTE 人员配备3=A221-A222-A223.
EXECUTE.
RECODE 人员配备3 (0=0) (SYSMIS=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS 人员配备3 '教育部门人员配备'.
VALUE LABELS 人员配备3 1'不一致' 0'一致'.

FREQUENCIES VARIABLES=人员配备1 人员配备2 人员配备3
  /ORDER=ANALYSIS.

*拷贝人员配备不一致的数据.
DATASET COPY  idx. 
DATASET ACTIVATE  idx. 
FILTER OFF. 
USE ALL. 
SELECT IF ( 人员配备1=1 OR 人员配备2=1 OR 人员配备3 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-1\04-表1-1-人员配备不一致.sav' 
  /COMPRESSED.

*7.数据逻辑检验-学生常见病项目经费.----------------修
*题一、（一）3.学生常见病项目经费，总学生常见病项目经费应大于等于到账经费且总学生常见病项目经费大于等于支出经费.
RECODE A131 (SYSMIS=1) INTO 学生常见病项目经费1.
RECODE A132 (SYSMIS=1) INTO 学生常见病项目经费1.
RECODE A133 (SYSMIS=1) INTO 学生常见病项目经费1.
EXECUTE.

IF(A131 >= A132 AND A131 >= A133) 学生常见病项目经费1=0.
EXECUTE.
IF(A131 < A132 OR A131 < A133) 学生常见病项目经费1=1.
EXECUTE.
RECODE 学生常见病项目经费1 (0=0) (1=1) (SYSMIS=1).
EXECUTE.
VARIABLE LABELS 学生常见病项目经费1 '疾控部门学生常见病项目经费'.
VALUE LABELS 学生常见病项目经费1 1'不一致' 0'一致'.

*题一、（一）3.省（市、县）级配套经费，省（市、县）级配套经费应大于等于近视防控专项配套经费.
RECODE A134 (SYSMIS=1) INTO 配套经费1.
RECODE A135 (SYSMIS=1) INTO 配套经费1.
EXECUTE.

IF(A134 >= A135) 配套经费1=0.
EXECUTE.
IF(A134 < A135) 配套经费1=1.
EXECUTE.
RECODE 配套经费1 (0=0) (1=1) (SYSMIS=1).
EXECUTE.
VARIABLE LABELS 配套经费1 '疾控部门配套经费'.
VALUE LABELS 配套经费1 1'不一致' 0'一致'.


*题一、（二）3.学生常见病项目经费，总学生常见病项目经费应大于等于到账经费且总学生常见病项目经费大于等于支出经费.
RECODE A311 (SYSMIS=1) INTO 学生常见病项目经费2.
RECODE A312 (SYSMIS=1) INTO 学生常见病项目经费2.
RECODE A313 (SYSMIS=1) INTO 学生常见病项目经费2.
EXECUTE.

IF(A311 >= A312 AND A311 >= A313) 学生常见病项目经费2=0.
EXECUTE.
IF(A311 < A312 OR A311 < A313) 学生常见病项目经费2=1.
EXECUTE.
RECODE 学生常见病项目经费2 (0=0) (1=1) (SYSMIS=1).
EXECUTE.
VARIABLE LABELS 学生常见病项目经费2 '疾控中心学生常见病项目经费'.
VALUE LABELS 学生常见病项目经费2 1'不一致' 0'一致'.



*题一、（三）3.业务经费，总业务专项经费应大于等于学校卫生业务专项经费且总业务专项经费大于等于近视防控专项经费..
RECODE A231 (SYSMIS=1) INTO 业务经费3.
RECODE A232 (SYSMIS=1) INTO 业务经费3.
RECODE A233 (SYSMIS=1) INTO 业务经费3.
EXECUTE.
IF(A231 >= A232 AND A231 >= A233) 业务经费3 = 0.
EXECUTE.
IF(A231< A232 AND A231 < A233) 业务经费3 = 1.
EXECUTE.
RECODE 业务经费3 (0=0) (1=1) (SYSMIS=1).
EXECUTE.
VARIABLE LABELS 业务经费3 '教育部门业务经费业务经费'.
VALUE LABELS 业务经费3 1'不一致' 0'一致'.

FREQUENCIES VARIABLES= 学生常见病项目经费1 配套经费1 学生常见病项目经费2 业务经费3
  /ORDER=ANALYSIS.

*拷贝经费不一致的数据.
DATASET COPY  jfx. 
DATASET ACTIVATE jfx. 
FILTER OFF. 
USE ALL. 
SELECT IF ( 学生常见病项目经费1=1 OR 配套经费1=1 OR 学生常见病项目经费2=1 OR 业务经费3=1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-1\05-表1-1-经费不一致.sav' 
  /COMPRESSED.


*8.数据逻辑检验-学校情况.--------修改
*题一、（四）1.学校情况，辖区内学校数量应大于等于中小学数+大学数；
中小学数应大于等于有卫生室或保健室的中小学数，中小学数应大于等于按照《学校卫生工作条例》600：1的比例配备专职卫生技术人员的中小学校
，大学数应大于等于有卫生室或保健室的大学数.

RECODE A411 (SYSMIS=1) INTO 学校情况.
RECODE A4111 (SYSMIS=1) INTO 学校情况.
RECODE A4112 (SYSMIS=1) INTO 学校情况.
RECODE A4121 (SYSMIS=1) INTO 学校情况.
RECODE A4122 (SYSMIS=1) INTO 学校情况.
RECODE A4123 (SYSMIS=1) INTO 学校情况.
EXECUTE.

IF(A411>= A4111 + A4112 AND A4111 >= A4121 AND A4112 >= A4123 AND A4111 >= A4122) 学校情况=0.
EXECUTE.
IF(A411 < A4111 + A4112 OR A4111 < A4121 OR A4112 < A4123 OR A4111 <  A4122) 学校情况=1.
EXECUTE.
RECODE 学校情况 (0=0) (1=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS 学校情况 '区县学校情况'.
VALUE LABELS 学校情况 1'不一致' 0'一致'.

IF(POINT=0) 学校情况=0.
EXECUTE.

FREQUENCIES VARIABLES=学校情况
  /ORDER=ANALYSIS.

*拷贝学校情况不一致的数据.
DATASET COPY  xxqx. 
DATASET ACTIVATE  xxqx. 
FILTER OFF. 
USE ALL. 
SELECT IF (学校情况=1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-1\06-表1-1-学校情况不一致.sav' 
  /COMPRESSED.


