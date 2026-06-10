* Encoding: UTF-8.

******表3-2 学生健康状况及影响因素调查表（中学版）.
*1.查看并记录数据库总样本量.
DESCRIPTIVES VARIABLES=ID1 PROVINCE CITY COUNTY 
  /STATISTICS=MEAN STDDEV MIN MAX.


*2.查看并标记id1与各分项编码不一致的样本。0为一致，1为不一致。将不一致样本剔出，另存为一个数据库*

COMPUTE ID3=((10**13)*province)+((10**11)*city)+(county*(10**9))+(point*(10**8))+(school*(10**6))+(a01*(10**4))+a011.
EXECUTE.
COMPUTE ID是否一致=id1 - ID3.
EXECUTE .
RECODE ID是否一致 (0=0) (SYSMIS=1) (ELSE=1).
EXECUTE.
VARIABLE LABELS  ID是否一致 'ID是否一致'.
VALUE LABELS ID是否一致 1 '不一致' 0 '一致'.

FREQUENCIES VARIABLES=ID是否一致
  /ORDER=ANALYSIS.

*拷贝ID不一致的数据.
DATASET COPY  idx. 
DATASET ACTIVATE  idx. 
FILTER OFF. 
USE ALL. 
SELECT IF ( ID是否一致 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表3-2\01-表3-2-ID不一致.sav' 
  /COMPRESSED.

*3.查看和标记重复样本。PrimaryFirst1=0者为重复样本，将重复样本剔出，另存为一个数据库*
* Identify Duplicate Cases.
SORT CASES BY ID1(A) A02 A04 A05.
MATCH FILES
  /FILE=*
  /BY ID1 A02 A04 A05
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
VARIABLE LABELS  PrimaryFirst1 'Indicator of each first matching case as Primary'.
VALUE LABELS  PrimaryFirst1 0 'Duplicate Case' 1 'Primary Case'.
VARIABLE LEVEL  PrimaryFirst1 (ORDINAL).
FREQUENCIES VARIABLES=PrimaryFirst1.
EXECUTE.
VARIABLE LABELS  PrimaryFirst1 '重复样本'.
VALUE LABELS PrimaryFirst1 1  '不重复' 0 '重复'.

*拷贝ID重复的数据.
DATASET COPY  idxx. 
DATASET ACTIVATE  idxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (PrimaryFirst1 = 0). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表3-2\02-表3-2-ID重复.sav' 
  /COMPRESSED.


*5.查看基本信息是否有缺失、取值是否有异常。*
*若省编码取值有异常，一般为录入手误，可直接修正为正确的省编码*

FREQUENCIES VARIABLES=province city county point school A01 A02 A04 A05
  /FORMAT=NOTABLE 
  /STATISTICS=MINIMUM MAXIMUM MEAN MEDIAN 
  /ORDER=ANALYSIS.

*标记基本信息缺失和异常样本。0为正常，1为缺失/异常。将缺失或取值异常样本剔出并另存*

RECODE city (SYSMIS=1)(ELSE=0) INTO 地市编码缺失.
EXECUTE.
RECODE county (SYSMIS=1)(ELSE=0) INTO 县区编码缺失.
EXECUTE.
RECODE point (1 thru 2=0)(SYSMIS=1)(ELSE=1) INTO 城乡编码缺失或异常.
EXECUTE.
RECODE school (SYSMIS=1)(ELSE=0) INTO 学校编码缺失.
EXECUTE.
RECODE A01 (4 thru 6 =0)(11 thru 14=0) (21 thru 23=0)  (31 thru 33=0) (SYSMIS=1) (ELSE=1) INTO 年级编码缺失或异常.
EXECUTE.
RECODE A02 (1 thru 2=0)(SYSMIS=1)(ELSE=1) INTO 性别编码缺失或异常.
EXECUTE.
RECODE A04 (1 thru 2=0)(SYSMIS=1)(ELSE=1) INTO 住校编码缺失或异常.
EXECUTE.
RECODE A05 (1 thru 8=0)(SYSMIS=1)(ELSE=1) INTO 民族编码缺失或异常.
EXECUTE.

VALUE LABELS 地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 住校编码缺失或异常 民族编码缺失或异常 1 '缺失/异常' 0 '不缺失'.

FREQUENCIES VARIABLES=地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 住校编码缺失或异常 民族编码缺失或异常 
  /ORDER=ANALYSIS.

*拷贝基本信息缺失数据.
DATASET COPY  infoxx. 
DATASET ACTIVATE  infoxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 1  OR 县区编码缺失=1 OR 城乡编码缺失或异常=1 OR 学校编码缺失=1 OR 年级编码缺失或异常=1 OR 性别编码缺失或异常=1 OR 住校编码缺失或异常=1 OR 民族编码缺失或异常=1 ). 
EXECUTE.
DATASET ACTIVATE  infoxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表3-2\03-表3-2-基本信息缺失.sav' 
  /COMPRESSED.

*6.拷贝清理后数据库，删除前几项应删除的数据.
DATASET COPY  bbb. 
DATASET ACTIVATE  bbb. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 0 AND 县区编码缺失=0 AND 城乡编码缺失或异常=0 AND 学校编码缺失=0 AND 年级编码缺失或异常=0 AND 性别编码缺失或异常=0 AND 住校编码缺失或异常=0 AND 民族编码缺失或异常=0 AND ID是否一致=0 AND PrimaryFirst1=1). 
EXECUTE. 
DATASET ACTIVATE  bbb. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表3-2\表3-2-清理后.sav' 
  /COMPRESSED.

