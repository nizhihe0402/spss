* Encoding: UTF-8.
**合并幼儿园和中小学
****近视数据库清理及近视率情况
*1.查看并记录数据库总样本量*

FILTER OFF.
USE ALL.
SELECT IF (grade ~= 41 &  grade ~= 42 & grade ~= 43).
EXECUTE.

DESCRIPTIVES VARIABLES=ID1 PROVINCE CITY COUNTY 
  /STATISTICS=MEAN STDDEV MIN MAX.

*2.计算年龄，生成指标age2*

COMPUTE age2=DATEDIFF(EXAMINE,BIRTH,"days")/365.25.
EXECUTE.


*整岁年龄: age.
COMPUTE  age=DATEDIF(EXAMINE, BIRTH, "years").
VARIABLE LABELS  age "年龄".
VARIABLE LEVEL  age (SCALE).
FORMATS  age (F5.0).
VARIABLE WIDTH  age(5).
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\01-近视数据库-ID不一致.sav' 
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\02-近视数据库-ID重复.sav' 
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

*2025年新增证件类型.完善

COMPUTE 证件类型缺失或异常 = $SYSMIS.
DO IF (grade <> 53).
  RECODE zjtype (1 thru 4 = 0)(SYSMIS = 1)(ELSE = 1) INTO 证件类型缺失或异常.
END IF.
EXECUTE.



*————————————————————————————————
*2025年修原表2-1中小学库重新修改针对为样本人群为幼儿园中小学
* 修改RECODE规则，将grade=53也视为正常年级

RECODE grade (1 thru 6=0)(11 thru 14=0)(21 thru 23=0)(31 thru 33=0)(53=0)(SYSMIS=1)(ELSE=1) 
  INTO 年级编码缺失或异常.
EXECUTE.
VALUE LABELS 地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 出生日期缺失 检测日期缺失 证件类型缺失或异常 1 '缺失/异常' 0 '不缺失'.
FREQUENCIES VARIABLES=地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 出生日期缺失 检测日期缺失  证件类型缺失或异常
  /ORDER=ANALYSIS.

*拷贝基本信息缺失数据.
DATASET COPY  infoxx. 
DATASET ACTIVATE  infoxx. 
FILTER OFF. 
USE ALL. 
SELECT IF (地市编码缺失 = 1 OR 县区编码缺失=1 OR 城乡编码缺失或异常=1 OR 学校编码缺失=1 OR 年级编码缺失或异常=1 
                   OR 性别编码缺失或异常=1 OR 出生日期缺失=1 OR 检测日期缺失=1 OR 证件类型缺失或异常=1). 
EXECUTE.
DATASET ACTIVATE  infoxx. 
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\03-近视数据库-基本信息缺失-幼儿园中小学.sav' 
  /COMPRESSED.



**2025年新增-当证件类型（zjtype）不存在缺失值时，检查身份证、港澳居民来往内地通行证，台湾居民来往大陆通行证，护照相关证件号码是否缺失。0为正常，1为缺失*
*
*
*trpmt需要提前改为字符串类型！！！！！！！
*
* 测试身份证类型的检查
* 首先检查证件类型字段的情况
*===  统一加 grade≠53 限制  ===.

FREQUENCIES VARIABLES=zjtype
  /ORDER=ANALYSIS.


*1 证件号码是否缺失（0=正常 1=缺失）*.
       .
*    仅对 grade≠53 的个案生效                .
*==============================================.
COMPUTE 证件号码缺失 = $SYSMIS.
EXECUTE.

DO IF (grade <> 53).
   COMPUTE 证件号码缺失 = 0.
   IF (zjtype=1 AND (MISSING(sfz) OR LTRIM(RTRIM(sfz))="")) 证件号码缺失=1.
   IF (zjtype=2 AND (MISSING(mtp) OR LTRIM(RTRIM(mtp))="")) 证件号码缺失=1.
   IF (zjtype=3 AND (MISSING(trpmt) OR LTRIM(RTRIM(trpmt))="")) 证件号码缺失=1.
   IF (zjtype=4 AND (MISSING(hz) OR LTRIM(RTRIM(hz))="")) 证件号码缺失=1.
END IF.
EXECUTE.



* 拷贝证件号码缺失数据.
DATASET COPY zjmiss. 
DATASET ACTIVATE zjmiss. 
FILTER OFF. 
USE ALL. 
SELECT IF (证件号码缺失 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\04-近视数据库-证件号码缺失.sav' 
  /COMPRESSED.


***手动在核查库中基本信息缺失、证件号码缺失的.

**2025年新增-当证件类型选择为身份证时，标记身份证可疑样本。0为正常，1为可疑* 
对于身份证可疑样本，暂时不予剔出，但是拷贝另存，请当地核实*
*身份证满足18位正常，身份证不满足18位视为可疑。*
* 初始化身份证可疑变量.
*    仅对 grade≠53 的个案生效                .
*==============================================.
COMPUTE 证件位数异常=0.
IF  ((STRING(zjtype, F1)="1" AND CHAR.LENGTH(SFZ) <> 18) OR
     (STRING(zjtype, F1)="2" AND CHAR.LENGTH(MTP) <> 9) OR
     (STRING(zjtype, F1)="3" AND CHAR.LENGTH(TRPMT) <> 8) OR
     (STRING(zjtype, F1)="4" AND CHAR.LENGTH(HZ) < 8)) 证件位数异常=1.
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\05-近视数据库-身份证可疑.sav' 
  /COMPRESSED.




*2025年新增----查看标记身份证日期与出生日期不一致的样本。0为一致，1为不一致。对于出生日期不一致样本，暂时不予剔出，但是拷贝另存，请当地核实**
*    仅对 grade≠53 的个案生效
*对于BIRTH变量缺失的请当地核实                .
*==============================================.
COMPUTE 身份证出生日期异常 = $SYSMIS.

DO IF (grade <> 53 AND ZJTYPE = 1).
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
FREQUENCIES VARIABLES=身份证出生日期异常.




* 拷贝出生日期不一致的数据.
DATASET COPY birthx. 
DATASET ACTIVATE birthx. 
FILTER OFF. 
USE ALL. 
SELECT IF (身份证出生日期异常 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\06-近视数据库-身份证与出生日期不一致.sav' 
  /COMPRESSED.



* 新增提取身份证第17位（性别位）并计算其奇偶性
* 身份证性别一致性检查             .
*==============================================.
COMPUTE 身份证性别异常 = $SYSMIS.

DO IF (grade <> 53 AND ZJTYPE = 1).
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

VALUE LABELS 身份证性别异常 0 '正常' 1 '异常'.
FREQUENCIES 身份证性别异常.



* 拷贝性别不一致的数据.
DATASET COPY sexx. 
DATASET ACTIVATE sexx. 
FILTER OFF. 
USE ALL. 
SELECT IF (身份证性别异常 = 1). 
EXECUTE.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\07-近视数据库-身份证性别异常-中小学.sav' 
  /COMPRESSED.







***手动在核查库中身份证与出生日期不一致.
*若身份或与出生日期一般为录入手误，可直接修正为正确的出生日期*




*7.标记年龄异常样本。0为正常，1为异常*
*需要根据每个数据库中年龄取值范围进行具体分析，年龄为负值或者年龄过小（比如非幼儿园样本而年龄小于3岁），视为年龄异常样本。*
*以天津中小学生库为例，该数据库中有4人计算年龄为负值，2人计算年龄小于1岁，其余样本年龄均在5岁以上，因此设定年龄小于1岁标记为年龄异常，并从数据库中剔出另存*

RECODE age2 (Lowest thru 5=1) (27 thru Highest=1) (ELSE=0) INTO 年龄异常.
EXECUTE.
DO IF  (grade = 53).
RECODE age2 (5.5 thru 6.5=0)(6.5=0)(ELSE=1) INTO 年龄异常.
END IF.
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\08-近视数据库-年龄异常.sav' 
  /COMPRESSED.



*拷贝删除前几项应删除的数据.——————##修*###修
*从原先仅针对中小学修改为幼儿园和中小学

FREQUENCIES VARIABLES=PrimaryFirst1 地市编码缺失 县区编码缺失 城乡编码缺失或异常 学校编码缺失 年级编码缺失或异常 性别编码缺失或异常 出生日期缺失 检测日期缺失 ID是否一致 证件类型缺失或异常 证件号码缺失 年龄异常
  /ORDER=ANALYSIS.


*拷贝基本信息缺失数据.* 情况1：grade=53的特殊规则* 情况2：其他年级的统一规则

DATASET COPY combined.
DATASET ACTIVATE combined.
FILTER OFF.
USE ALL.
SELECT IF ((grade = 53 AND 
   地市编码缺失 = 0 AND 
   县区编码缺失 = 0 AND 
   城乡编码缺失或异常 = 0 AND 
   学校编码缺失 = 0 AND
   年级编码缺失或异常 = 0 AND 
   性别编码缺失或异常 = 0 AND 
   出生日期缺失 = 0 AND 
   检测日期缺失 = 0 AND 
   ID是否一致 = 0 AND 
   PrimaryFirst1 = 1 AND 
   年龄异常 = 0)
  OR
  ((grade >= 1 AND grade <= 6) OR 
   (grade >= 11 AND grade <= 14) OR 
   (grade >= 21 AND grade <= 23) OR 
   (grade >= 31 AND grade <= 33)) AND
   地市编码缺失 = 0 AND 
   县区编码缺失 = 0 AND 
   城乡编码缺失或异常 = 0 AND 
   学校编码缺失 = 0 AND 
   年级编码缺失或异常 = 0 AND 
   性别编码缺失或异常 = 0 AND 
   出生日期缺失 = 0 AND 
   检测日期缺失 = 0 AND 
   ID是否一致 = 0 AND 
   PrimaryFirst1 = 1 AND 
   证件类型缺失或异常 = 0 AND 
   年龄异常 = 0
).
EXECUTE.
DATASET ACTIVATE combined.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\近视数据库--清理后-幼儿园中小学.sav' 
  /COMPRESSED.











*8.标记年龄可疑样本。0为正常，1为可疑。对于年龄可疑样本，暂时不予剔出，但是拷贝另存，请当地核实*
*年龄是否可疑，一方面根据相应的年级和常理推测，另一方面参照体质调研清理连续变量的方法，首先按年级分组列出年龄頻数表，
   对于位于两端的数值，如果与主体不连续，则以该数值为界点，超出界点范围以外者视为年龄可疑样本*.
SORT CASES  BY grade.
SPLIT FILE LAYERED BY grade.

*幼儿园.
DO IF  (grade = 53 ).
RECODE age2 (5.5 thru 6.5 =0) (ELSE=1) INTO 年龄可疑.
END IF.
EXECUTE.

*中小学.
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

* Custom Tables. 
CTABLES 
  /VLABELS VARIABLES=GRADE 年龄可疑 age2 DISPLAY=LABEL 
  /TABLE GRADE > 年龄可疑 BY age2 [COUNT F40.0, MEAN, MINIMUM, MAXIMUM] 
  /CATEGORIES VARIABLES=GRADE ORDER=A KEY=VALUE EMPTY=INCLUDE 
  /CATEGORIES VARIABLES=年龄可疑 ORDER=A KEY=VALUE EMPTY=EXCLUDE.

SPLIT FILE OFF.  
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
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\09-近视数据库-年龄可疑.sav' 
  /COMPRESSED.



*9.进行近视状况判定，计算近视率*

*定义球镜和柱镜缺失值以及剔除不合理的数据

RECODE SPHERR (999=SYSMIS) (-30 thru 30=Copy) (ELSE=SYSMIS) INTO 球镜右修改.
EXECUTE.
RECODE SPHERL (999=SYSMIS) (-30 thru 30=Copy) (ELSE=SYSMIS) INTO 球镜左修改.
EXECUTE.

RECODE CYLINR (999=SYSMIS) (-15 thru 15=Copy) (ELSE=SYSMIS) INTO 柱镜右修改.
EXECUTE.
RECODE CYLINL (999=SYSMIS) (-15 thru 15=Copy) (ELSE=SYSMIS) INTO 柱镜左修改.
EXECUTE.

FREQUENCIES VARIABLES=球镜右修改 球镜左修改 柱镜右修改 柱镜左修改 
  /STATISTICS=STDDEV MINIMUM MAXIMUM MEAN 
  /ORDER=ANALYSIS.

*计算等效球镜度数

COMPUTE SER修=球镜右修改 + (柱镜右修改 /2).
EXECUTE.
COMPUTE SEL修=球镜左修改 + (柱镜左修改 /2).
EXECUTE.

DESCRIPTIVES VARIABLES=SER修 SEL修 
  /STATISTICS=MEAN STDDEV MIN MAX.

*剔除裸眼视力不合理的数据

RECODE VISIONR (0=Copy) (3.3 thru 5.6=Copy) (0.1 thru 3.2=SYSMIS) (5.7 thru Highest=SYSMIS) INTO 
    visionR修改.
EXECUTE.
RECODE VISIONL (0=Copy) (3.3 thru 5.6=Copy) (0.1 thru 3.2=SYSMIS) (5.7 thru Highest=SYSMIS) INTO 
    visionL修改.
EXECUTE .

FREQUENCIES VARIABLES=visionR修改 visionL修改 
  /STATISTICS=STDDEV MINIMUM MAXIMUM MEAN 
  /ORDER=ANALYSIS.


*判定是否近视
*裸眼视力<5.0同时等效球镜度数<-0.50就判定为近视，只要有一个眼睛判定为近视，计入近视样本；如果戴镜类型glasstype为3，即夜戴角膜塑形镜，直接计入近视样本
*这段语法中myopiaR和myopiaL是分别对右眼和左眼进行近视判定，1是近视，2是正常；myopia是根据左右眼有一个眼睛近视，或者glasstype为3，就将这个人计入近视样本

IF  (VISIONR修改 < 5.0 & SER修 < -0.50) myopiaR=1.
EXECUTE.
IF  (VISIONR修改 >= 5.0 | (VISIONR修改 < 5.0 & SER修 >= -0.50)) myopiaR=2.
EXECUTE.  
IF  (VISIONL修改 < 5.0 & SEL修 < -0.50) myopiaL=1.
EXECUTE.
IF  (VISIONL修改 >= 5.0 | (VISIONL修改 < 5.0 & SEL修 >= -0.50)) myopiaL=2.
EXECUTE.

IF  (myopiaR > 0 & myopiaL > 0 & (myopiaR =1 |  myopiaL=1)) myopia=1.
EXECUTE.
IF  ( myopiaR =2 & myopiaL=2) myopia=2.
EXECUTE.
IF  (GLASSTYPE = 3) myopia=1.
EXECUTE.

*注意GLASSTYPE变量名称是否正确.

VARIABLE LABELS myopia '近视'.
VALUE LABELS myopia 1'近视' 2'非近视'.

FREQUENCIES VARIABLES=myopiaR myopiaL myopia 
  /STATISTICS=STDDEV MINIMUM MAXIMUM MEAN 
  /ORDER=ANALYSIS.

* 被剔除样本另存为.
DATASET COPY  aaa. 
DATASET ACTIVATE  aaa. 
FILTER OFF. 
USE ALL. 
SELECT IF (MISSING(myopia)). 
EXECUTE. 
DATASET ACTIVATE aaa.  
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\10-近视数据库-近视判定缺失.sav' 
  /COMPRESSED.

*在"近视数据库-清理后-幼儿园中小学"这个库里清理“近视判定缺失”样本，形成最终近视数据库。.
DATASET COPY  ccc.
DATASET ACTIVATE  ccc.
FILTER OFF.
USE ALL.
SELECT IF (myopia=1 or myopia=2).
EXECUTE.
DATASET ACTIVATE  ccc.
SAVE OUTFILE='D:\2025常见病\数据审核\XX省份\第一次反馈库\近视数据库\近视数据库-最终近视数据库.sav'
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



* Encoding: GBK.

*1.地市数，直辖市无地市，可以直接看区县数.
FREQUENCIES VARIABLES=CITY
  /ORDER=ANALYSIS.

*2.区县数，查看是否有区县样本量过小，如果有可在word中备注.
CTABLES
  /VLABELS VARIABLES=CITY COUNTY DISPLAY=LABEL
  /TABLE CITY [C] > COUNTY [C] [COUNT F40.0]
  /CATEGORIES VARIABLES=CITY COUNTY ORDER=A KEY=VALUE EMPTY=EXCLUDE.

* 学校数定制表.
CTABLES
  /VLABELS VARIABLES=学校类型 CITY COUNTY SCHOOL DISPLAY=LABEL
  /TABLE 学校类型 [C] > CITY [C] > COUNTY [C] > SCHOOL [C][COUNT F40.0]
  /CATEGORIES VARIABLES=学校类型 CITY COUNTY SCHOOL ORDER=A KEY=VALUE EMPTY=EXCLUDE
  /CRITERIA CILEVEL=95.

*5.近视率.
CTABLES
  /VLABELS VARIABLES=学校类型 myopia DISPLAY=LABEL
  /TABLE 学校类型 [C] BY myopia [C][COUNT F40.0, ROWPCT.COUNT PCT40.2]
  /CATEGORIES VARIABLES=学校类型 myopia ORDER=A KEY=VALUE EMPTY=INCLUDE TOTAL=YES POSITION=AFTER.

CTABLES
  /VLABELS VARIABLES=学段 myopia DISPLAY=LABEL
  /TABLE 学段 [C] BY myopia [C][COUNT F40.0, ROWPCT.COUNT PCT40.2]
  /CATEGORIES VARIABLES=学段 myopia ORDER=A KEY=VALUE EMPTY=INCLUDE TOTAL=YES POSITION=AFTER.

* 附件表.
CTABLES
    /VLABELS VARIABLES=CITY COUNTY 学校类型 myopia DISPLAY=LABEL
    /TABLE CITY [C] > COUNTY [C] BY 学校类型 [C] > myopia [C][COUNT F40.0, ROWPCT.COUNT PCT40.2]
    /CATEGORIES VARIABLES=CITY COUNTY ORDER=A KEY=VALUE EMPTY=EXCLUDE
    /CATEGORIES VARIABLES=学校类型 ORDER=A KEY=VALUE EMPTY=INCLUDE
    /CATEGORIES VARIABLES=myopia [1.00, 2.00, OTHERNM] EMPTY=INCLUDE
    /CRITERIA CILEVEL=95.