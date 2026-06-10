* Encoding: UTF-8.


USE ALL.


* Date and Time Wizard: age_2.
COMPUTE  age_2=DATEDIF(EXAMINE, BIRTH, "months").
VARIABLE LABELS  age_2 "月龄".
VARIABLE LEVEL  age_2 (SCALE).
FORMATS  age_2 (F5.0).
VARIABLE WIDTH  age_2(5).
EXECUTE.


*月龄age_2.
*半岁年龄age_1.
IF  (age_2 >= 72 and age_2 < 78) age_1=6.0. 
VARIABLE LABELS  age_1 '半岁年龄'. 
EXECUTE.
IF  (age_2 >= 78 and age_2 < 84) age_1=6.5. 
EXECUTE.
IF  (age_2 >= 84 and age_2 < 90) age_1=7.0. 
EXECUTE.
IF  (age_2 >= 90 and age_2 < 96) age_1=7.5. 
EXECUTE.
IF  (age_2 >= 96 and age_2 < 102) age_1=8. 
EXECUTE.
IF  (age_2 >= 102 and age_2 < 108) age_1=8.5. 
EXECUTE.
IF  (age_2 >= 108 and age_2 < 114) age_1=9.0. 
EXECUTE.
IF  (age_2 >= 114 and age_2 < 120) age_1=9.5. 
EXECUTE.
IF  (age_2 >= 120 and age_2 < 126) age_1=10.0. 
EXECUTE.
IF  (age_2 >= 126 and age_2 < 132) age_1=10.5. 
EXECUTE.
IF  (age_2 >= 132 and age_2 < 138) age_1=11.0. 
EXECUTE.
IF  (age_2 >= 138 and age_2 < 144) age_1=11.5. 
EXECUTE.
IF  (age_2 >= 144 and age_2 < 150) age_1=12.0. 
EXECUTE.
IF  (age_2 >= 150 and age_2 < 156) age_1=12.5. 
EXECUTE.
IF  (age_2 >= 156 and age_2 < 162) age_1=13.0. 
EXECUTE.
IF  (age_2 >= 162 and age_2 < 168) age_1=13.5. 
EXECUTE.
IF  (age_2 >= 168 and age_2 < 174) age_1=14.0. 
EXECUTE.
IF  (age_2 >= 174 and age_2 < 180) age_1=14.5. 
EXECUTE.
IF  (age_2 >= 180 and age_2 < 186) age_1=15.0. 
EXECUTE.
IF  (age_2 >= 186 and age_2 < 192) age_1=15.5. 
EXECUTE.
IF  (age_2 >= 192 and age_2 < 198) age_1=16.0. 
EXECUTE.
IF  (age_2 >= 198 and age_2 < 204) age_1=16.5. 
EXECUTE.
IF  (age_2 >= 204 and age_2 < 210) age_1=17.0. 
EXECUTE.
IF  (age_2 >= 210 and age_2 < 216) age_1=17.5. 
EXECUTE.
IF  (age_2 >= 216 and age_2 < 222) age_1=18.0. 
EXECUTE.
IF  (age_2 >= 222 and age_2 < 228) age_1=18.5. 
EXECUTE.
IF  (age_2 >= 228 and age_2 < 234) age_1=19.0. 
EXECUTE.
IF  (age_2 >= 234 and age_2 < 240) age_1=19.5. 
EXECUTE.
IF  (age_2 >= 240 and age_2 < 246) age_1=20.0. 
EXECUTE.
IF  (age_2 >= 246 and age_2 < 252) age_1=20.5. 
EXECUTE.
IF  (age_2 >= 252 and age_2 < 258) age_1=21.0. 
EXECUTE.
IF  (age_2 >= 258 and age_2 < 264) age_1=21.5. 
EXECUTE.
IF  (age_2 >= 264 and age_2 < 270) age_1=22.0. 
EXECUTE.
IF  (age_2 >= 270 and age_2 < 276) age_1=22.5. 
EXECUTE.
IF  (age_2 >= 276 and age_2 < 282) age_1=23.0. 
EXECUTE.
IF  (age_2 >= 282 and age_2 < 288) age_1=23.5. 
EXECUTE.
IF  (age_2 >= 288 and age_2 < 294) age_1=24.0. 
EXECUTE.
IF  (age_2 >= 294 and age_2 < 300) age_1=24.5. 
EXECUTE.
IF  (age_2 >= 300 and age_2 < 306) age_1=25.0. 
EXECUTE.


*超重、肥胖----WST 586-2018《6-18岁学龄儿童青少年超重与肥胖筛查》.
*BMI_grade: 1=正常；2=超重；3=肥胖.
*男生.

DO IF  (gender = 1 & age_1 = 6 ).  
RECODE BMI (16.40 thru 17.6999=2) (17.70 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.
VALUE LABELS BMI_grade 1'不超重肥胖' 2'超重' 3'肥胖'.
VARIABLE LABELS BMI_grade '超重肥胖'.

DO IF  (gender = 1 & age_1 = 6.5 ).  
RECODE BMI (16.70 thru 18.0999=2) (18.10 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 7 ).  
RECODE BMI (17.0 thru 18.6999=2) (18.70 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 7.5 ).
RECODE BMI (17.4 thru 19.1999=2) (19.20 thru Highest=3) (ELSE=1)
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 8 ).
RECODE BMI (17.8 thru 19.6999=2) (19.70 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 8.5 ).
RECODE BMI (18.1 thru 20.2999=2) (20.3 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 9 ).
RECODE BMI (18.5 thru 20.7999=2) (20.8 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 9.5 ).
RECODE BMI (18.9 thru 21.3999=2) (21.4 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 10 ).
RECODE BMI (19.2 thru 21.8999=2) (21.90 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 10.5 ).
RECODE BMI (19.6 thru 22.4999=2) (22.5 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 11 ).
RECODE BMI (19.9 thru 22.9999=2) (23.0 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 11.5).
RECODE BMI (20.3 thru 23.5999=2) (23.6 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 12 ).
RECODE BMI (20.7 thru 24.0999=2) (24.1 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 12.5 ).
RECODE BMI (21.0 thru 24.6999=2) (24.7 thru Highest=3) (ELSE=1)
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 13 ).
RECODE BMI (21.4 thru 25.1999=2) (25.2 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 13.5 ).
RECODE BMI (21.9 thru 25.6999=2) (25.7 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 14 ).
RECODE BMI (22.3 thru 26.0999=2) (26.1 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 14.5 ).
RECODE BMI (22.6 thru 26.3999=2) (26.4 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 15 ).
RECODE BMI (22.9 thru 26.5999=2) (26.6 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 15.5 ).
RECODE BMI (23.1 thru 26.8999=2) (26.9 thru Highest=3) (ELSE=1)
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 16 ).
RECODE BMI (23.3 thru 27.0999=2) (27.1 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 16.5 ).
RECODE BMI (23.5 thru 27.3999=2) (27.4 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 17 ).
RECODE BMI (23.7 thru 27.5999=2) (27.6 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 17.5 ).
RECODE BMI (23.8 thru 27.7999=2) (27.8 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 >= 18 ).
RECODE BMI (24.0 thru 27.9999=2) (28.0 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

*女生.

DO IF  (gender = 2 & age_1 = 6 ).
RECODE BMI (16.2 thru 17.4999=2) (17.5 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 6.5 ).
RECODE BMI (16.5 thru 17.9999=2) (18.0 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 7 ).
RECODE BMI (16.8 thru 18.4999=2) (18.5 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 7.5 ).
RECODE BMI (17.2 thru 18.9999=2) (19.0 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 8 ).
RECODE BMI (17.6 thru 19.3999=2) (19.4 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 8.5 ).
RECODE BMI (18.1 thru 19.8999=2) (19.9 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 9 ).
RECODE BMI (18.5 thru 20.3999=2) (20.4 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 9.5 ).
RECODE BMI (19.0 thru 20.9999=2) (21.0 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 10 ).
RECODE BMI (19.5 thru 21.4999=2) (21.5 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 10.5 ).
RECODE BMI (20.0 thru 22.0999=2) (22.1 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 11 ).
RECODE BMI (20.5 thru 22.6999=2) (22.7 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 11.5 ).
RECODE BMI (21.1 thru 23.2999=2) (23.3 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 12 ).
RECODE BMI (21.5 thru 23.8999=2) (23.9 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 12.5 ).
RECODE BMI (21.9 thru 24.4999=2) (24.5 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 13 ).
RECODE BMI (22.2 thru 24.9999=2) (25.0 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 13.5 ).
RECODE BMI (22.6 thru 25.5999=2) (25.6 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 14 ).
RECODE BMI (22.8 thru 25.8999=2) (25.9 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 14.5 ).
RECODE BMI (23.0 thru 26.2999=2) (26.3 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 15 ).
RECODE BMI (23.2 thru 26.5999=2) (26.6 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 15.5 ).
RECODE BMI (23.4 thru 26.8999=2) (26.9 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 16 ).
RECODE BMI (23.6 thru 27.0999=2) (27.1 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 16.5 ).
RECODE BMI (23.7 thru 27.3999=2) (27.4 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 17 ).
RECODE BMI (23.8 thru 27.5999=2) (27.6 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 17.5 ).
RECODE BMI (23.9 thru 27.7999=2) (27.8 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 >= 18 ).
RECODE BMI (24.0 thru 27.9999=2) (28.0 thru Highest=3) (ELSE=1) 
    INTO BMI_grade.
END IF.
EXECUTE.


IF  (BMI异常或缺失 = 1 or 身高异常或缺失=1 or 体重异常或缺失=1) BMI_grade=9.
EXECUTE.



*中重度消瘦、消瘦.
*1=不消瘦；3=中重度消瘦；2=轻度消瘦
*男生.

DO IF  (gender = 1 & age_1 = 6.0).
RECODE BMI (Lowest thru 13.24999=3) (13.25000 thru 13.4=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.
VARIABLE LABELS BMI_thin '消瘦'.
VALUE LABELS BMI_thin 1'不消瘦' 2'轻度消瘦' 3'中重度消瘦'. 

DO IF  (gender = 1 & age_1 = 6.5).
RECODE BMI (Lowest thru 13.44999=3) (13.45000 thru 13.8=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 7.0).
RECODE BMI (Lowest thru 13.54999=3) (13.55000 thru 13.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 7.5).
RECODE BMI (Lowest thru 13.54999=3) (13.55000 thru 13.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 8.0).
RECODE BMI (Lowest thru 13.64999=3) (13.65000 thru 14.0=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 8.5).
RECODE BMI (Lowest thru 13.64999=3) (13.65000 thru 14.0=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 9.0 ).
RECODE BMI (Lowest thru 13.74999=3) (13.75000 thru 14.1=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 9.5 ).
RECODE BMI (Lowest thru 13.84999=3) (13.85000 thru 14.2=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 10.0 ).
RECODE BMI (Lowest thru 13.94999=3) (13.95000 thru 14.4=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 10.5 ).
RECODE BMI (Lowest thru 14.04999=3) (14.05000 thru 14.6=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 11.0 ).
RECODE BMI (Lowest thru 14.24999=3) (14.25000 thru 14.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 11.5 ).
RECODE BMI (Lowest thru 14.34999=3) (14.35000 thru 15.1=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 12.0 ).
RECODE BMI (Lowest thru 14.44999=3) (14.45000 thru 15.4=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 12.5 ).
RECODE BMI (Lowest thru 14.54999=3) (14.55000 thru 15.6=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 13.0 ).
RECODE BMI (Lowest thru 14.84999=3) (14.85000 thru 15.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 13.5 ).
RECODE BMI (Lowest thru 15.04999=3) (15.05000 thru 16.1=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 14.0 ).
RECODE BMI (Lowest thru 15.34999=3) (15.35000 thru 16.4=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 14.5 ).
RECODE BMI (Lowest thru 15.54999=3) (15.55000 thru 16.7=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 15.0 ).
RECODE BMI (Lowest thru 15.84999=3) (15.85000 thru 16.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 15.5 ).
RECODE BMI (Lowest thru 16.04999=3) (16.05000 thru 17.0=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 16.0 ).
RECODE BMI (Lowest thru 16.24999=3) (16.25000 thru 17.3=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 16.5 ).
RECODE BMI (Lowest thru 16.44999=3) (16.45000 thru 17.5=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 1 & age_1 = 17.0 ).
RECODE BMI (Lowest thru 16.64999=3) (16.65000 thru 17.7=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.



DO IF (gender = 1 & age_1 >= 17.5 & age_1 < 18.0).
RECODE BMI (Lowest thru 16.84999=3) (16.85000 thru 17.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.





*消瘦_女生.

DO IF  (gender = 2 & age_1 = 6.0 ).
RECODE BMI (Lowest thru 12.84999=3) (12.85000 thru 13.1=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 6.5 ).
RECODE BMI (Lowest thru 12.94999=3) (12.95000 thru 13.3=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 7.0 ).
RECODE BMI (Lowest thru 13.04999=3) (13.05000 thru 13.4=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 7.5 ).
RECODE BMI (Lowest thru 13.04999=3) (13.05000 thru 13.5=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 8.0 ).
RECODE BMI (Lowest thru 13.14999=3) (13.15000 thru 13.6=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 8.5 ).
RECODE BMI (Lowest thru 13.14999=3) (13.15000 thru 13.7=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 9.0 ).
RECODE BMI (Lowest thru 13.24999=3) (13.25000 thru 13.8=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 9.5 ).
RECODE BMI (Lowest thru 13.24999=3) (13.25000 thru 13.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 10.0 ).
RECODE BMI (Lowest thru 13.34999=3) (13.35000 thru 14.0=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 10.5 ).
RECODE BMI (Lowest thru 13.44999=3) (13.45000 thru 14.1=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 11.0 ).
RECODE BMI (Lowest thru 13.74999=3) (13.75000 thru 14.3=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 11.5 ).
RECODE BMI (Lowest thru 13.94999=3) (13.95000 thru 14.5=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.


DO IF  (gender = 2 & age_1 = 12.0 ).
RECODE BMI (Lowest thru 14.14999=3) (14.15000 thru 14.7=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 12.5 ).
RECODE BMI (Lowest thru 14.34999=3) (14.35000 thru 14.9=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 13.0 ).
RECODE BMI (Lowest thru 14.64999=3) (14.65000 thru 15.3=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 13.5 ).
RECODE BMI (Lowest thru 14.94999=3) (14.95000 thru 15.6=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 14.0 ).
RECODE BMI (Lowest thru 15.34999=3) (15.35000 thru 16.0=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 14.5 ).
RECODE BMI (Lowest thru 15.74999=3) (15.75000 thru 16.3=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 15.0 ).
RECODE BMI (Lowest thru 16.04999=3) (16.05000 thru 16.6=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 15.5 ).
RECODE BMI (Lowest thru 16.24999=3) (16.25000 thru 16.8=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 16.0 ).
RECODE BMI (Lowest thru 16.44999=3) (16.45000 thru 17.0=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 16.5 ).
RECODE BMI (Lowest thru 16.54999=3) (16.55000 thru 17.1=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 = 17.0 ).
RECODE BMI (Lowest thru 16.64999=3) (16.65000 thru 17.2=2) (ELSE=1)
    INTO BMI_thin.
END IF.
EXECUTE.

DO IF  (gender = 2 & age_1 >= 17.5  & age_1 < 18.0).
RECODE BMI (Lowest thru 16.74999=3) (16.75000 thru 17.3=2) (ELSE=1) 
    INTO BMI_thin.
END IF.
EXECUTE.


IF  (BMI异常或缺失 = 1 or 身高异常或缺失=1 or 体重异常或缺失=1) BMI_thin=9.
EXECUTE.


*生长迟缓_男生
*BMI_stunting 1=是，2=否.

DO IF  (gender = 1 & age_1 = 6.0 ).
RECODE Q6 (Lowest thru 106.3=1) (106.4 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
VARIABLE LABELS BMI_stunting '生长迟缓'.
VALUE LABELS BMI_stunting 1'是' 2'否'.

DO IF  (gender = 1 & age_1 = 6.5 ).
RECODE Q6 (Lowest thru 109.5=1)  (109.6 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 7.0 ).
RECODE Q6 (Lowest thru 111.3=1)  (111.4 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 7.5 ).
RECODE Q6 (Lowest thru 112.8=1)  (112.9 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 8.0 ).
RECODE Q6 (Lowest thru 115.4=1)  (115.5 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 8.5 ).
RECODE Q6 (Lowest thru 117.6=1)  (117.7 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 9.0 ).
RECODE Q6 (Lowest thru 120.6=1) (120.7 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 9.5 ).
RECODE Q6 (Lowest thru 123.0=1) (123.1 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 10.0 ).
RECODE Q6 (Lowest thru 125.2=1) (125.3 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 10.5 ).
RECODE Q6 (Lowest thru 127.0=1) (127.1 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 11.0 ).
RECODE Q6 (Lowest thru 129.1=1) (129.2 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 11.5 ).
RECODE Q6 (Lowest thru 130.8=1) (130.9 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 12.0 ).
RECODE Q6 (Lowest thru 133.1=1) (133.2 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 12.5 ).
RECODE Q6 (Lowest thru 134.9=1) (135.0 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 13.0 ).
RECODE Q6 (Lowest thru 136.9=1) (137.0 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 13.5 ).
RECODE Q6 (Lowest thru 138.6=1) (138.7 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 14.0 ).
RECODE Q6 (Lowest thru 141.9=1)  (142 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 14.5 ).
RECODE Q6 (Lowest thru 144.7=1)  (144.8 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 15.0 ).
RECODE Q6 (Lowest thru 149.6=1)  (149.7 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 15.5 ).
RECODE Q6 (Lowest thru 153.6=1)  (153.7 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 16.0 ).
RECODE Q6 (Lowest thru 155.1=1)  (155.2 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 16.5 ).
RECODE Q6 (Lowest thru 156.4=1)  (156.5 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 = 17.0 ).
RECODE Q6 (Lowest thru 156.8=1)  (156.9 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 1 & age_1 >= 17.5 & age_1 < 18.0 ).
RECODE Q6 (Lowest thru 157.1=1)  (157.2 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.

*生长迟缓_女生.

DO IF  (gender = 2 & age_1 = 6.0 ).
RECODE Q6 (Lowest thru 105.7=1)  (105.8 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 6.5 ).
RECODE Q6 (Lowest thru 108.0=1)  (108.1 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 7.0 ).
RECODE Q6 (Lowest thru 110.2=1)  (110.3 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 7.5 ).
RECODE Q6 (Lowest thru 111.8=1)  (111.9 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 8.0 ).
RECODE Q6 (Lowest thru 114.5=1)  (114.6 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 8.5 ).
RECODE Q6 (Lowest thru 116.8=1)  (116.9 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 9.0 ).
RECODE Q6 (Lowest thru 119.5=1)  (119.6 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 9.5 ).
RECODE Q6 (Lowest thru 121.7=1)  (121.8 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 10.0 ).
RECODE Q6 (Lowest thru 123.9=1)  (124.0 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 10.5 ).
RECODE Q6 (Lowest thru 125.7=1)  (125.8 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 11.0 ).
RECODE Q6 (Lowest thru 128.6=1)  (128.7 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 11.5 ).
RECODE Q6 (Lowest thru 131.0=1) (131.1 thru Highest=2) 
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 12.0 ).
RECODE Q6 (Lowest thru 133.6=1) (133.7 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 12.5 ).
RECODE Q6 (Lowest thru 135.7=1) (135.8 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 13.0 ).
RECODE Q6 (Lowest thru 138.8=1) (138.9 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 13.5 ).
RECODE Q6 (Lowest thru 141.4=1) (141.5 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 14.0 ).
RECODE Q6 (Lowest thru 142.9=1) (143.0 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 14.5 ).
RECODE Q6 (Lowest thru 144.1=1) (144.2 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 15.0 ).
RECODE Q6 (Lowest thru 145.4=1) (145.5 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 15.5 ).
RECODE Q6 (Lowest thru 146.5=1) (146.6 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 16.0 ).
RECODE Q6 (Lowest thru 146.8=1) (146.9 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 16.5 ).
RECODE Q6 (Lowest thru 147.0=1) (147.1 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 = 17.0 ).
RECODE Q6 (Lowest thru 147.3=1) (147.4 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.
DO IF  (gender = 2 & age_1 >= 17.5  & age_1 < 18.0).
RECODE Q6 (Lowest thru 147.5=1) (147.6 thru Highest=2)
    INTO BMI_stunting.
END IF.
EXECUTE.

IF  (BMI异常或缺失 = 1 or 身高异常或缺失=1 or 体重异常或缺失=1) BMI_stunting=9.
EXECUTE.

