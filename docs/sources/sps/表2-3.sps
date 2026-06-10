* Encoding: UTF-8.
**** 表2-3 学生重点常见病监测表（幼儿园版）

*1.查看并记录数据库总样本量*

DESCRIPTIVES VARIABLES=ID1 PROVINCE CITY COUNTY 
  /STATISTICS=MEAN STDDEV MIN MAX.

*2.计算年龄，生成指标age2*

COMPUTE age2=DATEDIFF(EXAMINE,BIRTH,"days")/365.25.
EXECUTE.


*3.查看并标记id1与各分项编码不一致的样本。0为一致，1为不一致。将不一致样本剔出，另存为一个数据库*

COMPUTE ID3=((10**13)*province)+((10**11)*city)+(county*(10**9))+(point*(10**8))+(school*(10**6))+(grade*(10**4))+num.
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-3\01-表2-3-ID不一致-幼儿园.sav' 
  /COMPRESSED.


*4.查看和标记重复样本。PrimaryFirst1=0者为重复样本，将重复样本剔出，另存为一个数据库*

* Identify Duplicate Cases.
SORT CASES BY id1(A).
MATCH FILES
  /FILE=*
  /BY id1
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-3\02-表2-3-ID重复-幼儿园.sav' 
  /COMPRESSED.

***手动在核查库中删除ID重复的.


*5.查看基本信息是否有缺失、取值是否有异常。*
*若省编码取值有异常，一般为录入手误，可直接修正为正确的省编码*

FREQUENCIES VARIABLES=province city county point school grade gender birth examine age2 
  /FORMAT=NOTABLE 
  /STATISTICS=MINIMUM MAXIMUM MEAN MEDIAN 
  /ORDER=ANALYSIS.

*6.标记基本信息缺失和异常样本。0为正常，1为缺失，2为取值异常。将缺失或取值异常样本剔出并另存*
*直辖市有可能county（县区）指标是缺失或者0值*

RECODE city (SYSMIS=1)(ELSE=0) INTO 地市编码缺失.
EXECUTE.
RECODE county (SYSMIS=1)(ELSE=0) INTO 县区编码缺失.
EXECUTE.
RECODE point (1 thru 2=0)(SYSMIS=1)(ELSE=1) INTO 城乡编码缺失或异常.
EXECUTE.
RECODE school (SYSMIS=1)(ELSE=0) INTO 学校编码缺失.
EXECUTE.

RECODE gender (1 thru 2=0)(SYSMIS=1)(ELSE=1) INTO 性别编码缺失或异常.
EXECUTE.
RECODE birth (SYSMIS=1)(ELSE=0) INTO 出生日期缺失.
EXECUTE.
RECODE examine (SYSMIS=1)(ELSE=0) INTO 检测日期缺失.
EXECUTE.


*表2-3幼儿园库.
RECODE grade (53=0)(SYSMIS=1)(ELSE=1) INTO 年级编码缺失或异常.
EXECUTE.

VALUE LABELS 地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 出生日期缺失 检测日期缺失 1 '缺失/异常' 0 '不缺失'.

FREQUENCIES VARIABLES=地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 出生日期缺失 检测日期缺失 
  /ORDER=ANALYSIS.

*拷贝基本信息缺失数据.
DATASET COPY  infoxx. 
DATASET ACTIVATE  infoxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 1 OR 县区编码缺失=1 OR 城乡编码缺失或异常=1 OR 学校编码缺失=1 OR 年级编码缺失或异常=1 OR 性别编码缺失或异常=1 OR 出生日期缺失=1 OR 检测日期缺失=1 ). 
EXECUTE.
DATASET ACTIVATE  infoxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-3\03-表2-3-基本信息缺失-幼儿园.sav' 
  /COMPRESSED.

***手动在核查库中删除基本信息缺失的.





*7.标记幼儿园大班年龄超范围样本。幼儿园大班按照方案要求年龄应为5岁半至6岁半*
*0为正常，1为年龄超范围。将超范围样本剔出另存*

DESCRIPTIVES VARIABLES=age2 
  /STATISTICS=MEAN STDDEV MIN MAX.

DO IF  (grade = 53).
RECODE age2 (5.5 thru 6.5=0)(6.5=0)(ELSE=1) INTO 年龄异常.
END IF.
EXECUTE.

FREQUENCIES VARIABLES=年龄异常 
  /STATISTICS=STDDEV MINIMUM MAXIMUM MEAN 
  /ORDER=ANALYSIS.

*拷贝年龄异常数据.
DATASET COPY  agexx. 
DATASET ACTIVATE  agexx. 
FILTER OFF. 
USE ALL. 
SELECT IF (年龄异常=1 ). 
EXECUTE.
DATASET ACTIVATE  agexx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-3\04-表2-3-年龄异常-幼儿园.sav' 
  /COMPRESSED.


*拷贝删除前几项应删除的数据.
DATASET COPY  bbb. 
DATASET ACTIVATE  bbb. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 0 AND 县区编码缺失=0 AND 城乡编码缺失或异常=0 AND 学校编码缺失=0 AND 年级编码缺失或异常=0 AND 性别编码缺失或异常=0 AND 出生日期缺失=0 AND 检测日期缺失=0 AND ID是否一致=0 AND PrimaryFirst1=1 AND 年龄异常=0). 
EXECUTE. 
DATASET ACTIVATE  bbb. 

SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-3\表2-3-清理后-幼儿园.sav' 
  /COMPRESSED.






