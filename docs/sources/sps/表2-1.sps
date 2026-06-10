* Encoding: UTF-8.

****表2-1 学生重点常见病监测表（中小学生版）
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\01-表2-1-ID不一致-中小学.sav' 
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\02-表2-1-ID重复-中小学.sav' 
  /COMPRESSED.

***手动在核查库中删除ID重复的.


*5.查看基本信息是否有缺失、取值是否有异常。*
*若省编码取值有异常，一般为录入手误，可直接修正为正确的省编码*

FREQUENCIES VARIABLES=province city county point school grade gender birth examine age2 
  /FORMAT=NOTABLE 
  /STATISTICS=MINIMUM MAXIMUM MEAN MEDIAN 
  /ORDER=ANALYSIS.

*6.标记基本信息缺失和异常样本。0为正常，1为缺失，2为取值异常。将缺失或取值异常样本剔出并另存*————————修
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


*2025年新增---证件类型.
RECODE zjtype (1 thru 4=0)(SYSMIS=1)(ELSE=1) INTO 证件类型缺失或异常.
EXECUTE.


*表2-1中小学库.
RECODE grade (1 thru 6=0)(11 thru 14=0)(21 thru 23=0)(31 thru 33=0)(SYSMIS=1)(ELSE=1) INTO 年级编码缺失或异常.
EXECUTE.

VALUE LABELS 地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 出生日期缺失 检测日期缺失 证件类型缺失或异常 1 '缺失/异常' 0 '不缺失'.

FREQUENCIES VARIABLES=地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 出生日期缺失 检测日期缺失  证件类型缺失或异常
  /ORDER=ANALYSIS.

*拷贝基本信息缺失数据.
DATASET COPY  infoxx. 
DATASET ACTIVATE  infoxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 1 OR 县区编码缺失=1 OR 城乡编码缺失或异常=1 OR 学校编码缺失=1 OR 年级编码缺失或异常=1 OR 性别编码缺失或异常=1 OR 出生日期缺失=1 OR 
                 检测日期缺失=1 OR 证件类型缺失或异常=1). 
EXECUTE.
DATASET ACTIVATE  infoxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\03-表2-1-基本信息缺失-中小学.sav' 
  /COMPRESSED.


**新增-当证件类型（zjtype）不存在缺失值时，检查身份证、港澳居民来往内地通行证，台湾居民来往大陆通行证，护照相关证件号码是否缺失。0为正常，1为缺失*
*
*trpmt需要提前改为字符串类型！！！！！！！
*
* 测试身份证类型的检查
* 首先检查证件类型字段的情况

FREQUENCIES VARIABLES=zjtype
  /ORDER=ANALYSIS.

* 初始化证件号码缺失变量.
COMPUTE 证件号码缺失 = 0.
EXECUTE.

* 只有当证件类型存在且为1（身份证）时，检查身份证缺失.
DO IF (NOT MISSING(zjtype) AND zjtype = 1).
  * 检查sfz是否为空或缺失值
  
  IF (MISSING(sfz) OR LTRIM(RTRIM(sfz)) = "" OR LTRIM(RTRIM(sfz)) = " ") 证件号码缺失 = 1.
END IF.

* 只有当证件类型存在且为2（港澳居民来往内地通行证）时，检查港澳居民来往内地通行证缺失.
DO IF (NOT MISSING(zjtype) AND zjtype = 2).
  * 检查mtp是否为空或缺失值
  
  IF (MISSING(mtp) OR LTRIM(RTRIM(mtp)) = "" OR LTRIM(RTRIM(mtp)) = " ") 证件号码缺失 = 1.
END IF.


* 只有当证件类型存在且为3（台湾居民来往大陆通行证）时，检查台湾居民来往大陆通行证缺失.
DO IF (NOT MISSING(zjtype) AND zjtype = 3).
  * 检查trpmt是否为空或缺失值
  
  IF (MISSING(trpmt) OR LTRIM(RTRIM(trpmt)) = "" OR LTRIM(RTRIM(trpmt)) = " ") 证件号码缺失 = 1.
END IF.


* 只有当证件类型存在且为4（护照）时，检查护照缺失.
DO IF (NOT MISSING(zjtype) AND zjtype = 4).
  * 检查hz是否为空或缺失值
  
  IF (MISSING(hz) OR LTRIM(RTRIM(hz)) = "" OR LTRIM(RTRIM(hz)) = " ") 证件号码缺失 = 1.
END IF.
EXECUTE.

* 设置变量标签和价值标签.
VARIABLE LABELS 证件号码缺失 '证件号码是否缺失'.
VALUE LABELS 证件号码缺失 1 '缺失' 0 '正常'.

* 显示最终结果.
FREQUENCIES VARIABLES=证件号码缺失
  /ORDER=ANALYSIS.



* 拷贝证件号码缺失数据.
DATASET COPY zjmiss. 
DATASET ACTIVATE zjmiss. 
FILTER OFF. 
USE ALL. 
SELECT IF (证件号码缺失 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\04-表2-1--证件号码缺失-中小学.sav' 
  /COMPRESSED.




***手动在核查库中基本信息缺失、证件号码缺失的.


**2025年新增-当证件类型选择为身份证时，标记身份证可疑样本。0为正常，1为可疑* 
对于身份证可疑样本，暂时不予剔出，但是拷贝另存，请当地核实*
*身份证满足18位正常，身份证不满足18位视为可疑。*
* 初始化身份证可疑变量.
COMPUTE 证件位数异常 = $SYSMIS.
DO IF (NOT MISSING(zjtype)).
   COMPUTE 证件位数异常 = 0.
   IF  ((STRING(zjtype, F1)="1" AND CHAR.LENGTH(SFZ) <> 18) OR
        (STRING(zjtype, F1)="2" AND CHAR.LENGTH(MTP) <> 9) OR
        (STRING(zjtype, F1)="3" AND CHAR.LENGTH(TRPMT) <> 8) OR
        (STRING(zjtype, F1)="4" AND CHAR.LENGTH(HZ) < 8)) 证件位数异常=1.
END IF.
EXECUTE.
VALUE LABELS 证件位数异常 0'正常' 1'异常'.

* 显示结果.
FREQUENCIES VARIABLES=证件位数异常
  /ORDER=ANALYSIS.
 

* 拷贝证件可疑数据.
DATASET COPY sfzxx. 
DATASET ACTIVATE sfzxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (证件位数异常 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\\第一次反馈库\表2-1\05-表2-1--证件位数异常-中小学.sav' 
  /COMPRESSED.




*新增----查看标记身份证日期与出生日期不一致可疑的样本。0为正常，1为异常。
*身份证日期与出生日期不一致可疑的样本，暂时不予剔出，但是拷贝另存，请当地核实*
*对于BIRTH变量缺失的请当地核实                .
*==============================================.              .
COMPUTE 身份证出生日期异常 = $SYSMIS.

DO IF ( ZJTYPE = 1).
    DO IF (MISSING(SFZ) OR MISSING(BIRTH) OR LENGTH(RTRIM(SFZ)) < 14).
        COMPUTE 身份证出生日期异常 = 1.
    ELSE.
        COMPUTE 身份证出生日期异常 = (NUMBER(CHAR.SUBSTR(SFZ, 7, 8), F8.0) <>
                                 (XDATE.YEAR(BIRTH)*10000 +
                                  XDATE.MONTH(BIRTH)*100 +
                                  XDATE.MDAY(BIRTH))).
    END IF.
END IF.

VALUE LABELS 身份证出生日期异常 0'正常' 1'异常'.

FREQUENCIES VARIABLES=身份证出生日期异常
/ORDER=ANALYSIS.




COMPUTE 身份证出生日期异常 = $SYSMIS.

DO IF (ZJTYPE = 1).
    * 基础检查：数据是否完整;.
    IF (MISSING(SFZ) OR MISSING(BIRTH) OR LENGTH(RTRIM(SFZ)) < 14) 
        身份证出生日期异常 = 1.
    
    ELSE.
        * 提取并转换身份证日期;.
        STRING SFZ_DATE (A8).
        COMPUTE SFZ_DATE = CHAR.SUBSTR(SFZ, 7, 8).
        
        * 使用CONVERT函数可能更稳健;.
        NUMERIC ID_DATE (F8.0).
        COMPUTE ID_DATE = NUMBER(SFZ_DATE, F8.0).
        
        * 计算出生日期数字;.
        COMPUTE BIRTH_DATE = (XDATE.YEAR(BIRTH)*10000 +
                             XDATE.MONTH(BIRTH)*100 +
                             XDATE.MDAY(BIRTH)).
        
        * 比较;.
        IF (NOT MISSING(ID_DATE)) 
            身份证出生日期异常 = (ID_DATE <> BIRTH_DATE).
        ELSE 
            身份证出生日期异常 = 1.  * 转换失败;
    END IF.
END IF.



FREQUENCIES 身份证出生日期异常.










* 拷贝出生日期不一致的数据.
DATASET COPY birthx. 
DATASET ACTIVATE birthx. 
FILTER OFF. 
USE ALL. 
SELECT IF (身份证出生日期异常 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\06-表2-1-身份证与出生日期不一致.sav' 
  /COMPRESSED.

*==============================================.

COMPUTE 身份证性别异常 = $SYSMIS.

DO IF (ZJTYPE = 1).
    * 首先检查基本条件是否满足
    
    DO IF (LENGTH(RTRIM(SFZ)) = 18 AND NOT MISSING(gender)).
        * 条件满足：进行正常的性别校验
        
        COMPUTE #gender_bit = NUMBER(CHAR.SUBSTR(SFZ, 17, 1), F8.0).
        DO IF (NOT MISSING(#gender_bit)).
            * 核心逻辑：当性别不一致时标记为异常(1)
            
            COMPUTE 身份证性别异常 = ((MOD(#gender_bit, 2) = 1 AND gender = 2) OR
                                 (MOD(#gender_bit, 2) = 0 AND gender = 1)).
        ELSE.
            * 身份证第17位不是数字，标记为异常
            
            COMPUTE 身份证性别异常 = 1.
        END IF.
    ELSE.
        * 条件不满足：身份证不是18位或gender为缺失值，标记为异常
        
        COMPUTE 身份证性别异常 = 1.
    END IF.
END IF.



* 添加值标签

VARIABLE LABELS 身份证性别异常 '身份证性别检验结果'.
VALUE LABELS 身份证性别异常 0 '正常' 1 '异常'.
FORMATS 身份证性别异常 (F1.0).
FREQUENCIES VARIABLES=身份证性别异常
  /ORDER=ANALYSIS.

* 拷贝性别不一致的数据.
DATASET COPY sexx. 
DATASET ACTIVATE sexx. 
FILTER OFF. 
USE ALL. 
SELECT IF (身份证性别异常 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\07-表2-1-身份证性别异常-中小学.sav' 
  /COMPRESSED.





***手动在核查库中身份证与出生日期不一致.
*若身份或与出生日期一般为录入手误，可直接修正为正确的出生日期*




*7.标记年龄异常样本。0为正常，1为异常*
*需要根据每个数据库中年龄取值范围进行具体分析，年龄为负值或者年龄过小（比如非幼儿园样本而年龄小于3岁），视为年龄异常样本。*
*以天津中小学生库为例，该数据库中有4人计算年龄为负值，2人计算年龄小于1岁，其余样本年龄均在5岁以上，因此设定年龄小于1岁标记为年龄异常，并从数据库中剔出另存*

RECODE age2 (Lowest thru 5=1) (27 thru Highest=1) (ELSE=0) INTO 年龄异常.
EXECUTE.
VALUE LABELS 年龄异常 1'异常' 0'正常'.
FREQUENCIES VARIABLES=年龄异常 
  /ORDER=ANALYSIS.

*拷贝年龄异常数据.
DATASET COPY  agexx. 
DATASET ACTIVATE  agexx. 
FILTER OFF. 
USE ALL. 
SELECT IF (年龄异常=1 ). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\08-表2-1-年龄异常-中小学.sav' 
  /COMPRESSED.





*拷贝删除前几项应删除的数据.——————修.
DATASET COPY  bbb. 
DATASET ACTIVATE  bbb. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 0 AND 县区编码缺失=0 AND 城乡编码缺失或异常=0 AND 学校编码缺失=0 AND 年级编码缺失或异常=0 AND 性别编码缺失或异常=0 AND 出生日期缺失=0 AND 检测日期缺失=0 AND 
                    ID是否一致=0 AND PrimaryFirst1=1 AND 证件类型缺失或异常 = 0 AND 年龄异常=0 ). 
EXECUTE. 
DATASET ACTIVATE  bbb. 

SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\表2-1-清理后-中小学.sav' 
  /COMPRESSED.





*8.标记年龄可疑样本。0为正常，1为可疑。对于年龄可疑样本，暂时不予剔出，但是拷贝另存，请当地核实*
*年龄是否可疑，一方面根据相应的年级和常理推测，另一方面参照体质调研清理连续变量的方法，首先按年级分组列出年龄頻数表，
   对于位于两端的数值，如果与主体不连续，则以该数值为界点，超出界点范围以外者视为年龄可疑样本*.
*grade不能为标度

SORT CASES  BY grade.
SPLIT FILE LAYERED BY grade.



DO IF  (grade = 1).
RECODE age2 (5 thru 9.5=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 2).
RECODE age2 (6 thru 10.5=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 3).
RECODE age2 (7 thru 12=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 4).
RECODE age2 (8 thru 13=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 5).
RECODE age2 (9 thru 14=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 6).
RECODE age2 (9  thru 14.5=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 11).
RECODE age2 (10 thru 16=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 12).
RECODE age2 (10 thru 17=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 13).
RECODE age2 (11 thru 18=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 14).
RECODE age2 (11 thru 19=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 21).
RECODE age2 (13  thru 19.5=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 22).
RECODE age2 (14  thru 20.5 =0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 23).
RECODE age2 (15  thru 21.5=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 31).
RECODE age2 (13 thru 19.5=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 32).
RECODE age2 (14 thru 20.5=0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

DO IF  (grade = 33).
RECODE age2 (15 thru 21.5 =0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

*大学.
DO IF  (grade = 41 | grade=42 | grade=43).
RECODE age2 (17 thru 24 =0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

VALUE LABELS 年龄可疑 1'可疑' 0'正常'.

* 在运行CTABLES之前先关闭SPLIT FILE

SPLIT FILE OFF.

* Custom Tables - 修改：将grade作为最外层的行变量

CTABLES 
  /VLABELS VARIABLES=grade 年龄可疑 age2 DISPLAY=LABEL 
  /TABLE grade > 年龄可疑 BY age2 [COUNT F40.0, MEAN, MINIMUM, MAXIMUM] 
  /CATEGORIES VARIABLES=grade ORDER=A KEY=VALUE EMPTY=INCLUDE 
  /CATEGORIES VARIABLES=年龄可疑 ORDER=A KEY=VALUE EMPTY=EXCLUDE.

* 如果需要重新启用SPLIT FILE进行后续分析，可以取消下面的注释

*SORT CASES BY grade.
*SPLIT FILE LAYERED BY grade.

FREQUENCIES VARIABLES=年龄可疑 
  /STATISTICS=STDDEV MINIMUM MAXIMUM MEAN 
  /ORDER=ANALYSIS.

*拷贝 年龄可疑数据 到新数据库.
DATASET COPY  agex. 
DATASET ACTIVATE  agex. 
FILTER OFF. 
USE ALL. 
SELECT IF (年龄可疑 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\08-表2-1-年龄可疑-中小学.sav' 
  /COMPRESSED.







* Encoding: GBK.
*整岁年龄: age.
COMPUTE  age=DATEDIF(EXAMINE, BIRTH, "years").
VARIABLE LABELS  age "年龄".
VARIABLE LEVEL  age (SCALE).
FORMATS  age (F5.0).
VARIABLE WIDTH  age(5).
EXECUTE.

***1.身高体重可疑.
*身高,分年龄、性别，1=身高异常或缺失，0=不缺失.

RECODE Q6 (0=1)(MISSING=1)(999=1) INTO 身高异常或缺失.
****男生.
DO IF (gender = 1 & age = 6).
RECODE Q6 (Lowest thru 103.4=1)(103.5 thru 141.0 =0)(141.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 7).
RECODE Q6 (Lowest thru 104.4=1)(104.5 thru 147.4 =0)(147.5 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 8).
RECODE Q6 (Lowest thru 109.9=1)(110.0 thru 152 =0)(152.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 9).
RECODE Q6 (Lowest thru 116.1=1)(116.2 thru 165.0 =0)(165.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 10).
RECODE Q6 (Lowest thru 117.9=1)(118 thru 168.7=0)(168.8 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 11).
RECODE Q6 (Lowest thru 119.9=1)(120 thru 177.0=0)(177.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 12).
RECODE Q6 (Lowest thru 125.9=1)(126 thru 185=0)(185.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 13).
RECODE Q6 (Lowest thru 131.1=1)(131.2 thru 187.5=0)(187.6 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 14).
RECODE Q6 (Lowest thru 137.4=1)(137.5 thru 191=0)(191.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 15).
RECODE Q6 (Lowest thru 142.9=1)(143 thru 193=0)(193.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 16).
RECODE Q6 (Lowest thru 149.4=1)(149.5 thru 195=0)(195.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 17).
RECODE Q6 (Lowest thru 149.9=1)(150 thru 195.1=0)(195.2 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 18).
RECODE Q6 (Lowest thru 150.7=1)(150.8 thru 196=0)(196.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 19).
RECODE Q6 (Lowest thru 151.5=1)(151.6 thru 196=0)(196.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 20).
RECODE Q6 (Lowest thru 151.9=1)(152 thru 196=0)(196.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 21).
RECODE Q6 (Lowest thru 151.9=1)(152 thru 196=0)(196.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 22).
RECODE Q6 (Lowest thru 151.9=1)(152 thru 198.5=0)(198.6 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 23).
RECODE Q6 (Lowest thru 151.9=1)(152 thru 198.5=0)(198.6 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 24).
RECODE Q6 (Lowest thru 151.9=1)(152 thru 198.5=0)(198.6 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

*********女生.
DO IF (gender = 2 & age = 6).
RECODE Q6 (Lowest thru 100.9=1)(101 thru 139.5 =0)(139.6 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 7).
RECODE Q6 (Lowest thru 105.5=1)(105.6 thru 147 =0)(147.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 8).
RECODE Q6 (Lowest thru 109.6=1)(109.7 thru 152 =0)(152.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 9).
RECODE Q6 (Lowest thru 115.9=1)(116 thru 164=0)(164.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 10).
RECODE Q6 (Lowest thru 116.9=1)(117 thru 168 =0)(168.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 11).
RECODE Q6 (Lowest thru 122.4=1)(122.5 thru 174=0)(174.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 12).
RECODE Q6 (Lowest thru 124.9=0)(125 thru 176 =0)(176.1 thru Highest=0) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 13).
RECODE Q6 (Lowest thru 131.4=1)(131.5 thru 178=0)(178.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 14).
RECODE Q6 (Lowest thru 136.9=1)(137 thru 179.5=0) (179.6 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 15).
RECODE Q6 (Lowest thru 138.9=1)(139 thru 182=0) (182.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 16).
RECODE Q6 (Lowest thru 140.4=1)(140.5 thru 183=0) (183.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 17).
RECODE Q6 (Lowest thru 140.9=1)(141 thru 184=0) (184.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 18).
RECODE Q6 (Lowest thru 140.9=1)(141 thru 184=0) (184.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 19).
RECODE Q6 (Lowest thru 142.9=1)(143 thru 184=0) (184.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 20).
RECODE Q6 (Lowest thru 142.9=1)(143 thru 184=0) (184.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 21).
RECODE Q6 (Lowest thru 142.9=1)(143 thru 187=0) (187.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 22).
RECODE Q6 (Lowest thru 142.9=1)(143 thru 188=0) (188.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 23).
RECODE Q6 (Lowest thru 142.9=1)(143 thru 188=0) (188.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 24).
RECODE Q6 (Lowest thru 142.9=1)(143 thru 188=0) (188.1 thru Highest=1) INTO 身高异常或缺失.
END IF.
EXECUTE.

VALUE LABELS 身高异常或缺失 1'异常' 0'正常'.

FREQUENCIES VARIABLES=身高异常或缺失
  /ORDER=ANALYSIS.

*体重,分年龄、性别,1=体重异常或缺失，0=不缺失.

RECODE Q7 (0=1)(MISSING=1)(999=1) INTO 体重异常或缺失.

****男生.
DO IF (gender = 1 & age = 6).
RECODE Q7 (Lowest thru 13.9=1)(14.0 thru 60.0 =0)(60.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 7).
RECODE Q7 (Lowest thru 14.9=1)(15.0 thru 65.0 =0)(65.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 8).
RECODE Q7 (Lowest thru 15.9=1)(16.0 thru 68.0 =0)(68.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 9).
RECODE Q7 (Lowest thru 16.9=1)(17.0 thru 82.0 =0)(82.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 10).
RECODE Q7 (Lowest thru 18.8=1)(18.9 thru 93.0=0)(93.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 11).
RECODE Q7 (Lowest thru 18.9=1)(19.0 thru 102.0=0)(102.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 12).
RECODE Q7 (Lowest thru 20.6=1)(20.7 thru 120.1=0)(120.2 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 13).
RECODE Q7 (Lowest thru 22.9=1)(23.0 thru 125.1=0)(125.2 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 14).
RECODE Q7 (Lowest thru 24.9=1)(25.0 thru 125.5=0)(125.6 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 15).
RECODE Q7 (Lowest thru 28.8=1)(28.9 thru 130=0)(130.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 16).
RECODE Q7 (Lowest thru 29.9=1)(30.0 thru 133.4=0)(133.5 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 17).
RECODE Q7 (Lowest thru 31.9=1)(32 thru 140=0)(140.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 18).
RECODE Q7 (Lowest thru 33.0=1)(33.1 thru 145.0=0)(145.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 19).
RECODE Q7 (Lowest thru 33.5=1)(33.6 thru 145.0=0)(145.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 20).
RECODE Q7 (Lowest thru 34.3=1)(34.4 thru 145.0=0)(145.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 21).
RECODE Q7 (Lowest thru 37.5=1)(37.6 thru 145.0=0)(145.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 22).
RECODE Q7 (Lowest thru 38.7=1)(38.8 thru 145.0=0)(145.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 23).
RECODE Q7 (Lowest thru 41.9=1)(42 thru 145.0=0)(145.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 1 & age = 24).
RECODE Q7 (Lowest thru 45.5=1)(45.6 thru 145.0=0)(145.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

*********女生.
DO IF (gender = 2 & age = 6).
RECODE Q7 (Lowest thru 13.9=1)(14.0 thru 52.6 =0)(52.7 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 7).
RECODE Q7 (Lowest thru 13.9=1)(14.0 thru 60.3 =0)(60.4 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 8).
RECODE Q7 (Lowest thru 15.9=1)(16.0 thru 65.4 =0)(65.5 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 9).
RECODE Q7 (Lowest thru 16.5=1)(16.6 thru 82.0=0)(82.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 10).
RECODE Q7 (Lowest thru 17.9=1)(18.0 thru 90 =0)(90.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 11).
RECODE Q7 (Lowest thru 19.5=1)(19.6 thru 96.8=0)(96.9 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 12).
RECODE Q7 (Lowest thru 20.1=0)(20.2 thru 110.0 =0)(110.1 thru Highest=0) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 13).
RECODE Q7 (Lowest thru 20.9=1)(21.0 thru 120=0)(120.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 14).
RECODE Q7 (Lowest thru 26.9=1)(27 thru 120=0) (120.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 15).
RECODE Q7 (Lowest thru 28.9=1)(29.0 thru 125.9=0) (126 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 16).
RECODE Q7 (Lowest thru 30.9=1)(31 thru 126=0) (126.1 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 17).
RECODE Q7 (Lowest thru 30.9=1)(31 thru 126.3=0) (126.4 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 18).
RECODE Q7 (Lowest thru 30.7=1)(30.8 thru 126.3=0) (126.4 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 19).
RECODE Q7 (Lowest thru 30.9=1)(31 thru 126.3=0) (126.4 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 20).
RECODE Q7 (Lowest thru 34.0=1)(34.1 thru 127.5=0) (127.6 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 21).
RECODE Q7 (Lowest thru 33.9=1)(34 thru 127.5=0) (127.6 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 22).
RECODE Q7 (Lowest thru 33.9=1)(34 thru 127.5=0) (127.6 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 23).
RECODE Q7 (Lowest thru 36.9=1)(37.0 thru 127.5=0) (127.6 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

DO IF (gender = 2 & age = 24).
RECODE Q7 (Lowest thru 38.1=1)(38.2 thru 127.5=0) (127.6 thru Highest=1) INTO 体重异常或缺失.
END IF.
EXECUTE.

VALUE LABELS 体重异常或缺失 1'异常' 0'正常'.

FREQUENCIES VARIABLES=体重异常或缺失
  /ORDER=ANALYSIS.

*BMI.

COMPUTE BMI=RND(Q7 * 10000 / (Q6 * Q6),0.1).
EXECUTE.

RECODE BMI (MISSING=1) (Lowest thru 9.9999=1) (ELSE=0) INTO BMI异常或缺失.
EXECUTE.

VALUE LABELS BMI异常或缺失 1'异常' 0'正常'.

FREQUENCIES VARIABLES=BMI异常或缺失
  /ORDER=ANALYSIS.

*拷贝身高、体重、BMI异常/缺失的数据.
DATASET COPY  xxx. 
DATASET ACTIVATE  xxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (身高异常或缺失 = 1 OR 体重异常或缺失=1 OR BMI异常或缺失=1). 
EXECUTE.
DATASET ACTIVATE  xxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\09-表2-1-身高体重可疑-中小学.sav' 
  /COMPRESSED.



****新增————腰围异常或缺失


RECODE Q09 (0=1) (SYSMIS=1) (999=1) (Lowest thru 39=1) (151  thru Highest=1) (40 thru 150=0) INTO 腰围异常或缺失.
EXECUTE.


VALUE LABELS 腰围异常或缺失 1 '异常/缺失' 0 '正常'.
EXECUTE.
FREQUENCIES VARIABLES=腰围异常或缺失
  /ORDER=ANALYSIS.

* 拷贝腰围异常或缺失的数据。

DATASET COPY  yw.
DATASET ACTIVATE  yw.
FILTER OFF.
USE ALL.
SELECT IF (腰围异常或缺失=1).
EXECUTE.

* 保存筛选后的数据集。

SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\10-表2-1-腰围可疑-中小学.sav'
  /COMPRESSED.

* 激活原始数据集。

DATASET ACTIVATE  xxx.






****2.血压可疑.
*收缩压&舒张压.
COMPUTE BPC=Q81 - Q82.
VARIABLE LABELS  BPC '脉压差'.
EXECUTE.
DO IF(Q81~=999 and Q82~=999).
RECODE Q81 (Lowest thru 59=1) (271 thru Highest=1) (ELSE=0) INTO 收缩压可疑.
RECODE Q82 (151 thru Highest=1) (ELSE=0) INTO 舒张压可疑.
RECODE BPC (Lowest thru 10=1) (300 thru Highest=1) (ELSE=0) INTO 压差可疑.
END IF.
EXECUTE.
IF(Q81=999 AND Q82=999) 收缩压可疑=0.
IF(Q81=999 AND Q82=999) 舒张压可疑=0.
IF(Q81=999 AND Q82=999) 压差可疑=0.
EXECUTE.
RECODE 收缩压可疑 (1=1) (0=0) (MISSING=1).
RECODE 舒张压可疑 (1=1) (0=0) (MISSING=1).
RECODE 压差可疑 (1=1) (0=0) (MISSING=1).
VALUE LABELS 收缩压可疑 舒张压可疑 压差可疑 1'异常' 0'正常'.

FREQUENCIES VARIABLES=收缩压可疑 舒张压可疑 压差可疑
  /ORDER=ANALYSIS.

*拷贝血压可疑的数据.
DATASET COPY  hhh. 
DATASET ACTIVATE  hhh. 
FILTER OFF. 
USE ALL. 
SELECT IF (收缩压可疑 = 1 OR 舒张压可疑=1 OR 压差可疑=1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\11-表2-1-血压可疑-中小学.sav' 
  /COMPRESSED.


****3.龋齿可疑.
*龋齿可疑，乳牙龋失补＞20，恒牙龋失补＞32.
COMPUTE 乳龋失补=Q51+Q52+Q53.
RECODE 乳龋失补(21 thru Highest=1) INTO 乳龋可疑.
EXECUTE.
RECODE Q51(MISSING=1)(21 thru Highest=1) INTO 乳龋可疑.
EXECUTE.
RECODE Q52(MISSING=1)(21 thru Highest=1) INTO 乳龋可疑.
EXECUTE.
RECODE Q53(MISSING=1)(21 thru Highest=1) INTO 乳龋可疑.
EXECUTE.
IF(Q51=99 AND Q52=99 AND Q53=99) 乳龋可疑=0.
EXECUTE.
RECODE 乳龋可疑(1=1)(ELSE=0).
EXECUTE.


COMPUTE 恒龋失补=Q54+Q55+Q56.
RECODE 恒龋失补 (32 thru Highest=1) INTO 恒龋可疑.
EXECUTE.
RECODE Q54 (MISSING=1)(32 thru Highest=1) INTO 恒龋可疑.
EXECUTE.
RECODE Q55 (MISSING=1)(32 thru Highest=1) INTO 恒龋可疑.
EXECUTE.
RECODE Q56 (MISSING=1)(32 thru Highest=1) INTO 恒龋可疑.
EXECUTE.
IF(Q54=99 AND Q55=99 AND Q56=99) 恒龋可疑=0.
EXECUTE.
RECODE 恒龋可疑(1=1)(ELSE=0).
EXECUTE.

VALUE LABELS 乳龋可疑 恒龋可疑 1'异常' 0'正常'.
FREQUENCIES VARIABLES=乳龋可疑 恒龋可疑
  /ORDER=ANALYSIS.

*拷贝龋齿可疑的数据.
DATASET COPY  ttt. 
DATASET ACTIVATE  ttt. 
FILTER OFF. 
USE ALL. 
SELECT IF (乳龋可疑 =1 OR 恒龋可疑=1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\12-表2-1-龋齿可疑-中小学.sav' 
  /COMPRESSED.







****4.脊柱弯曲异常可疑.
****新增-4.1脊柱筛查填写异常*.
COMPUTE 脊柱异常或缺失=0.
IF (MISSING(QX51) OR  QX51>=3)脊柱异常或缺失=1.
IF (MISSING(QX091) OR QX091 >= 3) 脊柱异常或缺失 = 1.
IF (MISSING(QX6) OR QX6 >= 3) 脊柱异常或缺失 = 1.
IF (MISSING(QX7) OR QX7 >= 4) 脊柱异常或缺失 = 1.
EXECUTE.

**** QX52至QX56中有意向选择是，QX51一般检查正常都不能选择是 *

DO IF (QX51 = 1).
    COMPUTE 脊柱异常或缺失 = 0.
    IF (QX52 = 1 OR QX53 = 1 OR QX54 = 1 OR QX55 = 1 OR QX56 = 1) 脊柱异常或缺失 = 1.
END IF.
EXECUTE.

**** QX092至QX094中有意向选择是，QX091前屈检查正常都不能选择是 *

DO IF (QX091 = 1).
    IF (QX092 = 1 OR QX093 = 1 OR QX094 = 1)脊柱异常或缺失 = 1.
END IF.
EXECUTE.

* 脊柱侧弯可疑 -

COMPUTE 脊柱侧弯可疑 = 0.
EXECUTE.

IF (MISSING(QX2) OR QX2 >= 4) 脊柱侧弯可疑 = 1.
IF (MISSING(QX3) OR QX3 >= 4) 脊柱侧弯可疑 = 1.
IF (MISSING(QX4) OR QX4 >= 4) 脊柱侧弯可疑 = 1.
IF (MISSING(QX6) OR QX6 >= 3) 脊柱侧弯可疑 = 1.
IF (MISSING(QX7) OR QX7 >= 4) 脊柱侧弯可疑 = 1.
EXECUTE.

VALUE LABELS 脊柱异常或缺失 脊柱侧弯可疑 1'异常' 0'正常'.
FREQUENCIES VARIABLES=脊柱异常或缺失 脊柱侧弯可疑 
  /ORDER=ANALYSIS.

*拷贝脊柱异常或缺失以及脊柱侧弯可疑的数据.
DATASET COPY  qx. 
DATASET ACTIVATE  qx. 
FILTER OFF. 
USE ALL. 
SELECT IF (脊柱侧弯可疑 = 1 OR 脊柱异常或缺失 = 1  ). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\表2-1\13-表2-1-脊柱异常或缺失或脊柱侧弯可疑-中小学.sav' 
  /COMPRESSED.






*学校类型赋值

IF  (GRADE >= 1 & GRADE <= 6) 学校类型=2.
EXECUTE.
IF  (GRADE >= 11 & GRADE <= 14) 学校类型=3.
EXECUTE.
IF  (GRADE >= 21 & GRADE <= 23) 学校类型=4.
EXECUTE.
IF  (GRADE >= 31 & GRADE <= 33) 学校类型=5.
EXECUTE.

IF  (GRADE = 53) 学校类型=1.
EXECUTE.

VARIABLE LABELS 学校类型 '学校类型'.
VALUE LABELS 学校类型 1'幼儿园' 2'小学' 3'初中' 4'高中' 5'职高'.

FREQUENCIES VARIABLES=学校类型 
  /STATISTICS=STDDEV MINIMUM MAXIMUM MEAN 
  /ORDER=ANALYSIS.

RECODE 学校类型 (4 thru 5=4) (ELSE=Copy) INTO 学段.
EXECUTE.

FREQUENCIES VARIABLES=学段 
  /STATISTICS=STDDEV MINIMUM MAXIMUM MEAN 
  /ORDER=ANALYSIS.




