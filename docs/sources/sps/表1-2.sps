* Encoding: UTF-8.


****表1-2 中小学校开展学校卫生工作情况调查表.
*1.查看并记录数据库总样本量.
*若省编码取值有异常，一般为录入手误，可直接修正为正确的省编码*

DESCRIPTIVES VARIABLES=ID1 PROVINCE CITY COUNTY 
  /STATISTICS=MEAN STDDEV MIN MAX.

*每个地级市所调查的城区学校数和郊县学校数.
CTABLES
  /VLABELS VARIABLES=CITY POINT DISPLAY=LABEL
  /TABLE CITY [C] > POINT [C][COUNT F40.0]
  /CATEGORIES VARIABLES=CITY POINT ORDER=A KEY=VALUE EMPTY=EXCLUDE.


*2.查看并标记ID1与各分项编码不一致的样本。0为一致，1为不一致。将不一致样本剔出，另存为一个数据库*

COMPUTE ID3=((10**7)*province)+((10**5)*city)+(county*(10**3))+(point*100)+school.
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-2\01-表1-2-ID不一致.sav' 
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-2\02-表1-2-ID重复.sav' 
  /COMPRESSED.

*4.查看基本信息是否有缺失、取值是否有异常.
FREQUENCIES VARIABLES=province city county point school
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
RECODE school (SYSMIS=1)(ELSE=0) INTO 学校编码缺失.
EXECUTE.

VALUE LABELS 地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 1 '缺失' 0 '不缺失'.

FREQUENCIES VARIABLES=地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失
  /ORDER=ANALYSIS.

*拷贝基本信息缺失数据.
DATASET COPY  infoxx. 
DATASET ACTIVATE  infoxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 1 OR 县区编码缺失=1 OR 城乡编码缺失或异常=1 OR 学校编码缺失 =1). 
EXECUTE.
DATASET ACTIVATE  infoxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-2\03-表1-2-基本信息缺失.sav' 
  /COMPRESSED.

*5.拷贝清理后数据库，删除前几项应删除的数据.
DATASET COPY  bbb. 
DATASET ACTIVATE  bbb. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 0 AND 县区编码缺失=0 AND 城乡编码缺失或异常=0 AND 学校编码缺失=0 AND ID是否一致=0 AND PrimaryFirst1=1). 
EXECUTE. 
DATASET ACTIVATE  bbb. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-2\表1-2-清理后.sav' 
  /COMPRESSED.

*6.学校基本信息缺失.
RECODE A1 (1 thru 3=0) (SYSMIS=1) (ELSE=1) INTO 学校性质缺失.
EXECUTE.
RECODE A2 (1 thru 3=0) (SYSMIS=1) (ELSE=1) INTO 寄宿学校缺失.
EXECUTE.
RECODE A3 (1 thru 8=0) (SYSMIS=1) (ELSE=1) INTO 学校类型缺失.
EXECUTE.
VALUE LABELS 学校性质缺失 寄宿学校缺失 学校类型缺失 1'缺失/异常' 0'不缺失'.

FREQUENCIES VARIABLES=学校性质缺失 寄宿学校缺失 学校类型缺失
  /ORDER=ANALYSIS.

*拷贝学校信息缺失的数据.
DATASET COPY  ixxx. 
DATASET ACTIVATE  ixxx. 
FILTER OFF. 
USE ALL. 
SELECT IF ( 学校性质缺失=1 OR 寄宿学校缺失=1 OR 学校类型缺失 = 1). 
EXECUTE.
DATASET ACTIVATE  ixxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-2\04-表1-2-学校信息缺失.sav' 
  /COMPRESSED.

*7.数据逻辑检验-学生情况.
*题一4.学生情况，在校学生人数应等于男生数+女生数，住校学生人数应等于住校男生数+住校女生数.
COMPUTE 学生情况1=A41-A42-A43.
EXECUTE.
RECODE 学生情况1 (0=0) (SYSMIS=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS 学生情况1 '在校学生人数'.
VALUE LABELS 学生情况1 1'不一致' 0'一致'.

COMPUTE 学生情况2=A44-A45-A46.
EXECUTE.
RECODE 学生情况2 (0=0) (SYSMIS=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS 学生情况2 '住校学生人数'.
VALUE LABELS 学生情况2 1'不一致' 0'一致'.


FREQUENCIES VARIABLES=学生情况1 学生情况2 
  /ORDER=ANALYSIS.

*拷贝学生数量不一致的数据.
DATASET COPY  nxxx. 
DATASET ACTIVATE  nxxx. 
FILTER OFF. 
USE ALL. 
SELECT IF ( 学生情况1=1 OR 学生情况2=1 ). 
EXECUTE.
DATASET ACTIVATE nxxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表1-2\05表1-2-学生情况异常.sav' 
  /COMPRESSED.
