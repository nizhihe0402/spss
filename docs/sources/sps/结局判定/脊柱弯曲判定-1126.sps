* Encoding: UTF-8.
*===============================================================脊柱弯曲异常. 
 


****脊柱筛查填写异常*.新增
*1异常或缺失值判定
*1.1一般检查填写异常或缺失.
COMPUTE 脊柱异常或缺失11=0.
IF (MISSING(QX51) | QX51>=3) 脊柱异常或缺失11=1.
IF (QX51=1 & (QX52=1 | QX53=1 | QX54=1 | QX55=1 | QX56=1)) 脊柱异常或缺失11= 1.
IF (QX51=2 & (QX52=2 | MISSING(QX52) | QX52=9) & (QX53=2 | MISSING(QX53) |QX53=9) & (QX54=2 | MISSING(QX54) |QX54=9) & 
(QX55=2 | MISSING(QX55) |QX55=9) & (QX56=2 | MISSING(QX56) |QX56=9)) 脊柱异常或缺失11= 1.
EXECUTE.

*1.2前屈试验填写异常或缺失.
COMPUTE 脊柱异常或缺失21=0.
IF (MISSING(QX091) | QX091>=3) 脊柱异常或缺失21=1.
IF (QX091=1 & (QX092=1 & QX093=1 & QX094=1) ) 脊柱异常或缺失21= 1.
IF (QX091=2 & (QX092=2 | MISSING(QX092) | QX092=9) & (QX093=2 | MISSING(QX093) |QX093=9) & 
(QX094=2 | MISSING(QX094) |QX094=9) ) 脊柱异常或缺失21= 1.
EXECUTE.

*1.3躯干旋转测量仪检查填写异常或缺失.
COMPUTE 脊柱异常或缺失31=0.
IF (((MISSING(QX2) | QX2>=4) & MISSING(QX2ATR)) & ((MISSING(QX3) | QX3>=4) & MISSING(QX3ATR)) &
((MISSING(QX4) | QX4>=4) & MISSING(QX4ATR))) 脊柱异常或缺失31=1.
IF ((QX2=1 & QX2ATR>=5) & (QX3=1 & QX3ATR>=5) & (QX4=1 & QX4ATR>=5)) 脊柱异常或缺失31= 1.
IF ((QX2>=2 & (MISSING(QX2ATR) | QX2ATR<5)) & (QX3>=2 & (MISSING(QX3ATR) | QX3ATR<5)) & 
(QX4>=2 & (MISSING(QX4ATR) | QX4ATR<5))) 脊柱异常或缺失31= 1.
EXECUTE.

**1.4一般检查异常或前屈试验阳性或ATR>=5，但未做脊柱运动试验.
COMPUTE 脊柱异常或缺失41=0.
IF (((QX51=2 | (QX091=2) | (QX2ATR >= 5 | QX3ATR >= 5 | QX4ATR >= 5)) & QX6=2))脊柱异常或缺失41=1.
EXECUTE.

**1.5脊柱前后弯曲填写异常.
COMPUTE 脊柱异常或缺失51=0.
IF (MISSING(QX7) |  QX7>=4) 脊柱异常或缺失51=1.
IF (QX7=1 & QX71>=1) 脊柱异常或缺失51= 1.
IF (QX7>=2 & QX71>=4) 脊柱异常或缺失51= 1.
EXECUTE.

*异常或缺失值汇总.
RECODE 脊柱异常或缺失11 (1=1)  INTO 脊柱异常或缺失.
EXECUTE.
RECODE 脊柱异常或缺失21 (1=1)  INTO 脊柱异常或缺失.  
EXECUTE.
RECODE 脊柱异常或缺失31  (1=1) INTO 脊柱异常或缺失. 
EXECUTE. 
RECODE 脊柱异常或缺失41  (1=1) INTO 脊柱异常或缺失. 
EXECUTE. 
RECODE 脊柱异常或缺失51  (1=1) INTO 脊柱异常或缺失. 
EXECUTE. 

RECODE 脊柱异常或缺失(1=1)(ELSE=0). 
EXECUTE. 
VALUE LABELS 脊柱异常或缺失 1'异常或缺失' 0'正常'. 
FREQUENCIES VARIABLES=脊柱异常或缺失 
  /ORDER=ANALYSIS.


***拷贝脊柱缺失或异常数据.
SAVE后的路径根据自己实际进行修改.

FILTER OFF.
USE ALL.
SELECT IF (脊柱异常或缺失1 = 0).
EXECUTE.
SAVE OUTFILE="D:\2025常见病数据审核\***市\脊柱弯曲计算库.sav"
/COMPRESSED.

 
*筛选不可疑数据后生成脊柱弯曲、脊柱侧弯、侧弯度数、脊柱前凸异常、脊柱后凸异常5个变量. 
USE ALL. 
COMPUTE filter_$=(grade < 40 & 脊柱异常或缺失 = 0). 
VARIABLE LABELS filter_$ 'grade < 40 & 脊柱异常或缺失 = 0 (FILTER)'. 
VALUE LABELS filter_$ 0 'Not Selected' 1 'Selected'. 
FORMATS filter_$ (f1.0). 
FILTER BY filter_$. 
EXECUTE. 
 


*脊柱侧弯判定.
*一般检查和前屈试验均无异常且ATR<5. 
IF (QX51 = 1 & QX091 = 1 ) & ((QX2=1|QX2ATR < 5) & (QX3=1|QX3ATR<5) & (QX4=1|QX4ATR<5)) 脊柱侧弯=0.
EXECUTE.
*一般检查异常或前屈试验阳性或ATR>=5，但脊柱运动试验后进行躯干旋转测量仪检查ATR<5.
IF ((QX51 = 2 | (QX091 = 2 | (QX092=1 | QX093=1 | QX094=1)) | (QX2ATR >= 5 | QX3ATR >= 5 | QX4ATR >= 5)) &  (QX62ATR<5 & QX63ATR<5 & QX64ATR<5)) 脊柱侧弯=1.
EXECUTE.
*一般检查异常或前屈试验阳性或ATR>=5，脊柱运动试验后进行躯干旋转测量仪检查ATR>=5.
IF ((QX51 = 2 | (QX091 = 2 | (QX092=1 | QX093=1 | QX094=1)) | (QX2ATR >= 5 | QX3ATR >= 5 | QX4ATR >= 5)) &  
   ((QX62ATR >= 5 & QX62ATR < 7) | (QX63ATR >= 5 & QX63ATR < 7) | (QX64ATR >= 5 & QX64ATR < 7))) 脊柱侧弯=2.
EXECUTE.
IF ((QX51 = 2 | (QX091 = 2 | (QX092=1 | QX093=1 | QX094=1)) | (QX2ATR >= 5 | QX3ATR >= 5 | QX4ATR >= 5)) &  
   ((QX62ATR >= 7 & QX62ATR < 10) | (QX63ATR >= 7 & QX63ATR < 10) | (QX64ATR >= 7 & QX64ATR < 10)))脊柱侧弯=3.
EXECUTE.
 IF ((QX51 = 2 | (QX091 = 2 | (QX092=1 | QX093=1 | QX094=1)) | (QX2ATR >= 5 | QX3ATR >= 5 | QX4ATR >= 5)) &  
   (QX62ATR >= 10 | QX63ATR >= 10 | QX64ATR >= 10))脊柱侧弯=4.
EXECUTE.
VALUE LABELS 脊柱侧弯  0'否' 1'姿态不良' 2'侧弯1度' 3'侧弯2度' 4'侧弯3度' . 
FREQUENCIES VARIABLES=脊柱侧弯
  /ORDER=ANALYSIS.




IF  (QX7 = 1 ) 脊柱前后弯曲异常=0. 
EXECUTE. 
IF  (QX7 >=2 & QX71 = 1) 脊柱前后弯曲异常=1. 
EXECUTE. 
IF  (QX7 >=2 & QX71 = 2) 脊柱前后弯曲异常=2. 
EXECUTE.
IF  (QX7 >=2 & QX71 = 3) 脊柱前后弯曲异常=3. 
EXECUTE.  
VALUE LABELS 脊柱前后弯曲异常 0'否' 1'姿态不良' 2'脊柱前凸异常' 3'脊柱后凸异常' .  
 FREQUENCIES VARIABLES=脊柱前后弯曲异常
  /ORDER=ANALYSIS.


*脊柱弯曲异常判定—综合脊柱侧弯和脊柱前后弯曲异常结果.
IF (脊柱侧弯 = 0 & 脊柱前后弯曲异常=0) 脊柱弯曲 =0.
EXECUTE.
IF ((脊柱侧弯 = 1 & 脊柱前后弯曲异常= 0) | (脊柱侧弯 = 0 & 脊柱前后弯曲异常= 1) |(脊柱侧弯 = 1 & 脊柱前后弯曲异常= 1))脊柱弯曲 = 1.
EXECUTE.
IF (脊柱侧弯 >= 2 | 脊柱前后弯曲异常>= 2) 脊柱弯曲=2.
EXECUTE.
VALUE LABELS 脊柱弯曲 0'正常' 1'姿势不良'  2'侧弯或前凸后凸异常'. 
 FREQUENCIES VARIABLES=脊柱弯曲
  /ORDER=ANALYSIS.




FILTER OFF. 
USE ALL. 
EXECUTE. 
 
