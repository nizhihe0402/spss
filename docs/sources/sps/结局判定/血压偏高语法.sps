* Encoding: GBK.
*定义身高逻辑界值.
COMPUTE BPC=Q81 - Q82. 
VARIABLE LABELS  BPC '脉压差'. 
EXECUTE.
RECODE BPC (Lowest thru 10=1) (300 thru Highest=1) INTO 血压异常.
RECODE BPC (Lowest thru 10=1) (300 thru Highest=1) INTO 血压异常3.
EXECUTE.
*收缩压&舒张压.
RECODE Q81 (MISSING=1) (Lowest thru 59=1) (271 thru Highest=1) INTO 血压异常3.
EXECUTE.
RECODE Q82 (MISSING=1) (151 thru Highest=1) INTO 血压异常3.
EXECUTE.


*血压偏高计算.
*heightgroup:0= <P5; 1=P5-P10; 2=P10-P25; 3=P25-P50; 4=P50-P75; 5=P75-P90; 6=P90-P95; 7=>P95.
*男生身高分类*

DO IF  (gender = 1 & age = 7).
RECODE Q6 (Lowest thru 115.6999999999999=0) (115.7 thru 117.8999999999999=1) (117.9 thru 121.499999999=2) 
    (121.5 thru 125.49999999=3) (125.5 thru 129.499999999=4) (129.5 thru 133.299999999=5) (133.3 thru 
    135.3999999999=6) (135.4 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8).
RECODE Q6 (Lowest thru 120.5999999999999=0) (120.6 thru 122.8999999999999=1) (122.9 thru 126.499999999=2) 
    (126.5 thru 130.69999999=3) (130.7 thru 134.899999999=4) (134.9 thru 138.699999999=5) (138.7 thru 
    140.9999999999=6) (141.0 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9).
RECODE Q6 (Lowest thru 124.9999999999999=0) (125.0 thru 127.3999999999999=1) (127.4 thru 131.399999999=2) 
    (131.4 thru 135.79999999=3) (135.8 thru 140.299999999=4) (140.3 thru 144.199999999=5) (144.2 thru 
    146.5999999999=6) (146.6 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10).
RECODE Q6 (Lowest thru 129.999999999999=0) (130.0 thru 132.0999999999999=1) (132.1 thru 136.099999999=2) 
    (136.1 thru 140.79999999=3) (140.8 thru 145.399999999=4) (145.4 thru 149.799999999=5) (149.8 thru 
    152.3999999999=6) (152.4 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11).
RECODE Q6 (Lowest thru 133.6999999999999=0) (133.7 thru 136.3999999999999=1) (136.4 thru 140.99999999=2) 
    (141.0 thru 145.99999999=3) (146.0 thru 151.299999999=4) (151.3 thru 156.499999999=5) (156.5 thru 
    159.6999999999=6) (159.7 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12).
RECODE Q6 (Lowest thru 138.3999999999999=0) (138.4 thru 141.1999999999999=1) (141.2 thru 145.99999999=2) 
    (146.0 thru 151.99999999=3) (152.0 thru 158.599999999=4) (158.6 thru 164.099999999=5) (164.1 thru 
    167.2999999999=6) (167.3 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13).
RECODE Q6 (Lowest thru 145.0999999999999=0) (145.1 thru 148.1999999999999=1) (148.2 thru 153.99999999=2) 
    (154.0 thru 160.19999999=3) (160.2 thru 166.199999999=4) (166.2 thru 170.699999999=5) (170.7 thru 
    173.3999999999=6) (173.4 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14).
RECODE Q6 (Lowest thru 151.5999999999999=0) (151.6 thru 154.9999999999999=1) (155.0 thru 160.39999999=2) 
    (160.4 thru 165.69999999=3) (165.7 thru 170.499999999=4) (170.5 thru 174.999999999=5) (175.0 thru 
    177.4999999999=6) (177.5 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15).
RECODE Q6 (Lowest thru 157.1999999999999=0) (157.2 thru 159.9999999999999=1) (160.0 thru 164.39999999=2) 
    (164.4 thru 168.99999999=3) (169.0 thru 173.399999999=4) (173.4 thru 177.399999999=5) (177.4 thru 
    179.9999999999=6) (180.0 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16).
RECODE Q6 (Lowest thru 159.9999999999999=0) (160.0 thru 162.3999999999999=1) (162.4 thru 166.29999999=2) 
    (166.3 thru 170.499999999=3) (170.5 thru 174.899999999=4) (174.9 thru 178.799999999=5) (178.8 thru 
    180.9999999999=6) (181.0 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17).
RECODE Q6 (Lowest thru 161.1999999999999=0) (161.2 thru 163.2999999999999=1) (163.3 thru 167.09999999=2) 
    (167.1 thru 171.399999999=3) (171.4 thru 175.599999999=4) (175.6 thru 179.499999999=5) (179.5 thru 
    181.8999999999=6) (181.9 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.

*女生身高分类*

DO IF  (gender = 2 & age = 7).
RECODE Q6 (Lowest thru 114.2999999999999=0) (114.3 thru 116.4999999999999=1) (116.5 thru 120.099999999=2) 
    (120.1 thru 124.09999999=3) (124.1 thru 128.099999999=4) (128.1 thru 131.899999999=5) (131.9 thru 
    133.9999999999=6) (134.0 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8).
RECODE Q6 (Lowest thru 119.1999999999999=0) (119.2 thru 121.4999999999999=1) (121.5 thru 125.199999999=2) 
    (125.2 thru 129.29999999=3) (129.3 thru 133.599999999=4) (133.6 thru 137.199999999=5) (137.2 thru 
    139.5999999999=6) (139.6 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9).
RECODE Q6 (Lowest thru 123.9999999999999=0) (124.0 thru 126.3999999999999=1) (126.4 thru 130.299999999=2) 
    (130.3 thru 134.99999999=3) (135.0 thru 139.599999999=4) (139.6 thru 143.899999999=5) (143.9 thru 
    146.4999999999=6) (146.5 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10).
RECODE Q6 (Lowest thru 129.0999999999999=0) (129.1 thru 131.7999999999999=1) (131.8 thru 135.999999999=2) 
    (136.0 thru 141.19999999=3) (141.2 thru 146.299999999=4) (146.3 thru 150.699999999=5) (150.7 thru 
    153.2999999999=6) (153.3 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11).
RECODE Q6 (Lowest thru 134.1999999999999=0) (134.2 thru 137.0999999999999=1) (137.1 thru 141.999999999=2) 
    (142.0 thru 147.29999999=3) (147.3 thru 152.699999999=4) (152.7 thru 157.099999999=5) (157.1 thru 
    159.5999999999=6) (159.6 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12).
RECODE Q6 (Lowest thru 139.6999999999999=0) (139.7 thru 142.5999999999999=1) (142.6 thru 147.499999999=2) 
    (147.5 thru 152.49999999=3) (152.5 thru 157.099999999=4) (157.1 thru 160.899999999=5) (160.9 thru 
    163.4999999999=6) (163.5 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13).
RECODE Q6 (Lowest thru 145.5999999999999=0) (145.6 thru 147.9999999999999=1) (148.0 thru 151.999999999=2) 
    (152.0 thru 156.09999999=3) (156.1 thru 159.999999999=4) (160.0 thru 163.799999999=5) (163.8 thru 
    165.9999999999=6) (166.0 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14).
RECODE Q6 (Lowest thru 148.1999999999999=0) (148.2 thru 150.3999999999999=1) (150.4 thru 153.999999999=2) 
    (154.0 thru 157.79999999=3) (157.8 thru 161.599999999=4) (161.6 thru 165.099999999=5) (165.1 thru 
    167.1999999999=6) (167.2 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15).
RECODE Q6 (Lowest thru 149.1999999999999=0) (149.2 thru 151.3999999999999=1) (151.4 thru 154.799999999=2) 
    (154.8 thru 158.39999999=3) (158.4 thru 162.199999999=4) (162.2 thru 165.999999999=5) (166.0 thru 
    167.9999999999=6) (168.0 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16).
RECODE Q6 (Lowest thru 149.9999999999999=0) (150.0 thru 151.6999999999999=1) (151.7 thru 155.199999999=2) 
    (155.2 thru 158.99999999=3) (159.0 thru 162.799999999=4) (162.8 thru 166.299999999=5) (166.3 thru 
    168.2999999999=6) (168.3 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17).
RECODE Q6 (Lowest thru 150.0999999999999=0) (150.1 thru 151.9999999999999=1) (152.0 thru 155.299999999=2) 
    (155.3 thru 159.19999999=3) (159.2 thru 163.099999999=4) (163.1 thru 166.599999999=5) (166.6 thru 
    168.7999999999=6) (168.8 thru Highest=7) INTO heightgroup.
END IF.
EXECUTE.


*男生收缩压*

DO IF  (gender = 1 & age = 7 & heightgroup = 0).
RECODE Q81 (108 thru Highest=1) (Lowest thru 107.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 1).
RECODE Q81 (110 thru Highest=1) (Lowest thru 109.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 2).
RECODE Q81 (112 thru Highest=1) (Lowest thru 111.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 3).
RECODE Q81 (113 thru Highest=1) (Lowest thru 112.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 4).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 5).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 6).
RECODE Q81 (118 thru Highest=1) (Lowest thru 117.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 7).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 0).
RECODE Q81 (110 thru Highest=1) (Lowest thru 109.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 1).
RECODE Q81 (112 thru Highest=1) (Lowest thru 111.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 2).
RECODE Q81 (113 thru Highest=1) (Lowest thru 112.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 3).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 4).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 5).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 6).
RECODE Q81 (120 thru Highest=1) (Lowest thru 119.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 7).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 9 & heightgroup = 0).
RECODE Q81 (112 thru Highest=1) (Lowest thru 111.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 1).
RECODE Q81 (114 thru Highest=1) (Lowest thru 113.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 2).
RECODE Q81 (114 thru Highest=1) (Lowest thru 113.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 3).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 4).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 5).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 6).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 10 & heightgroup = 0).
RECODE Q81 (113 thru Highest=1) (Lowest thru 112.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 1).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 2).
RECODE Q81 (116 thru Highest=1) (Lowest thru 115.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 3).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 4).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 5).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 6).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 7).
RECODE Q81 (129 thru Highest=1) (Lowest thru 128.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 11 & heightgroup = 0).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 1).
RECODE Q81 (116 thru Highest=1) (Lowest thru 115.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 2).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 3).
RECODE Q81 (120 thru Highest=1) (Lowest thru 119.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 4).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 5).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 6).
RECODE Q81 (128 thru Highest=1) (Lowest thru 127.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age =11 & heightgroup = 7).
RECODE Q81 (131 thru Highest=1) (Lowest thru 130.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 12 & heightgroup = 0).
RECODE Q81 (116 thru Highest=1) (Lowest thru 115.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 1).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 2).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 3).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 4).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 5).
RECODE Q81 (128 thru Highest=1) (Lowest thru 127.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 6).
RECODE Q81 (130 thru Highest=1) (Lowest thru 129.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 7).
RECODE Q81 (133 thru Highest=1) (Lowest thru 132.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 13 & heightgroup = 0).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 1).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 2).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 3).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 4).
RECODE Q81 (127 thru Highest=1) (Lowest thru 126.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age =13 & heightgroup = 5).
RECODE Q81 (130 thru Highest=1) (Lowest thru 129.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 6).
RECODE Q81 (132 thru Highest=1) (Lowest thru 131.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age =13 & heightgroup = 7).
RECODE Q81 (134 thru Highest=1) (Lowest thru 133.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 14 & heightgroup = 0).
RECODE Q81 (120 thru Highest=1) (Lowest thru 119.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age =14 & heightgroup = 1).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 2).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 3).
RECODE Q81 (128 thru Highest=1) (Lowest thru 127.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 4).
RECODE Q81 (129 thru Highest=1) (Lowest thru 128.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 5).
RECODE Q81 (131 thru Highest=1) (Lowest thru 130.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 6).
RECODE Q81 (133 thru Highest=1) (Lowest thru 132.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 7).
RECODE Q81 (135 thru Highest=1) (Lowest thru 134.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 15 & heightgroup = 0).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 1).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 2).
RECODE Q81 (128 thru Highest=1) (Lowest thru 127.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 3).
RECODE Q81 (130 thru Highest=1) (Lowest thru 129.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 4).
RECODE Q81 (131 thru Highest=1) (Lowest thru 130.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 5).
RECODE Q81 (132 thru Highest=1) (Lowest thru 131.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 6).
RECODE Q81 (133 thru Highest=1) (Lowest thru 132.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 7).
RECODE Q81 (136 thru Highest=1) (Lowest thru 135.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 16 & heightgroup = 0).
RECODE Q81 (127 thru Highest=1) (Lowest thru 126.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 1).
RECODE Q81 (129 thru Highest=1) (Lowest thru 128.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 2).
RECODE Q81 (130 thru Highest=1) (Lowest thru 129.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 3).
RECODE Q81 (131 thru Highest=1) (Lowest thru 130.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 4).
RECODE Q81 (132 thru Highest=1) (Lowest thru 131.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 5).
RECODE Q81 (133 thru Highest=1) (Lowest thru 132.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 6).
RECODE Q81 (134 thru Highest=1) (Lowest thru 133.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 7).
RECODE Q81 (136 thru Highest=1) (Lowest thru 135.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 17 & heightgroup = 0).
RECODE Q81 (129 thru Highest=1) (Lowest thru 128.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 1).
RECODE Q81 (131 thru Highest=1) (Lowest thru 130.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 2).
RECODE Q81 (131 thru Highest=1) (Lowest thru 130.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 3).
RECODE Q81 (132 thru Highest=1) (Lowest thru 131.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 4).
RECODE Q81 (133 thru Highest=1) (Lowest thru 132.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 5).
RECODE Q81 (134 thru Highest=1) (Lowest thru 133.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 6).
RECODE Q81 (135 thru Highest=1) (Lowest thru 134.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 7).
RECODE Q81 (136 thru Highest=1) (Lowest thru 135.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.


*男生舒张压*

DO IF  (gender = 1 & age = 7 & heightgroup = 0).
RECODE Q82 (72 thru Highest=1) (Lowest thru 71.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 1).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 2).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 3).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 4).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 5).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 6).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 7 & heightgroup = 7).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 8 & heightgroup = 0).
RECODE Q82 (73 thru Highest=1) (Lowest thru 72.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 1).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 2).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 3).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 4).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 5).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age =8 & heightgroup = 6).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 8 & heightgroup = 7).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 9 & heightgroup = 0).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 1).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 2).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 3).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 4).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 5).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 6).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 9 & heightgroup = 7).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 10 & heightgroup = 0).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 1).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 2).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 3).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 4).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 5).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 6).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 10 & heightgroup = 7).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 1 & age = 11 & heightgroup = 0).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 1).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 2).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 3).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 4).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 5).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 6).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 11 & heightgroup = 7).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 1 & age = 12 & heightgroup = 0).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 1).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 2).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 3).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 4).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 5).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 6).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 12 & heightgroup = 7).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 1 & age = 13 & heightgroup = 0).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 1).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 2).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 3).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 4).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 5).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 6).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 13 & heightgroup = 7).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 1 & age = 14 & heightgroup = 0).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 1).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 2).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 3).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 4).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 5).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 6).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 14 & heightgroup = 7).
RECODE Q82 (84 thru Highest=1) (Lowest thru 83.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 1 & age = 15 & heightgroup = 0).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 1).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 2).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 3).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 4).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 5).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 6).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 15 & heightgroup = 7).
RECODE Q82 (84 thru Highest=1) (Lowest thru 83.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 1 & age = 16 & heightgroup = 0).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 1).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 2).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 3).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 4).
RECODE Q82 (84 thru Highest=1) (Lowest thru 83.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 5).
RECODE Q82 (84 thru Highest=1) (Lowest thru 83.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 6).
RECODE Q82 (84 thru Highest=1) (Lowest thru 83.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 16 & heightgroup = 7).
RECODE Q82 (85 thru Highest=1) (Lowest thru 84.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 1 & age = 17 & heightgroup = 0).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 1).
RECODE Q82 (83 thru Highest=1) (Lowest thru 82.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 2).
RECODE Q82 (84 thru Highest=1) (Lowest thru 83.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 3).
RECODE Q82 (84 thru Highest=1) (Lowest thru 83.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 4).
RECODE Q82 (85 thru Highest=1) (Lowest thru 84.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 5).
RECODE Q82 (85 thru Highest=1) (Lowest thru 84.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 6).
RECODE Q82 (85 thru Highest=1) (Lowest thru 84.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 1 & age = 17 & heightgroup = 7).
RECODE Q82 (86 thru Highest=1) (Lowest thru 85.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.



*女生收缩压*

DO IF  (gender = 2 & age = 7 & heightgroup = 0).
RECODE Q81 (109 thru Highest=1) (Lowest thru 108.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 1).
RECODE Q81 (109 thru Highest=1) (Lowest thru 108.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 2).
RECODE Q81 (111 thru Highest=1) (Lowest thru 110.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 3).
RECODE Q81 (111 thru Highest=1) (Lowest thru 110.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 4).
RECODE Q81 (113 thru Highest=1) (Lowest thru 112.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 5).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 6).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 7).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 8 & heightgroup = 0).
RECODE Q81 (110 thru Highest=1) (Lowest thru 109.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 1).
RECODE Q81 (110 thru Highest=1) (Lowest thru 109.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 2).
RECODE Q81 (113 thru Highest=1) (Lowest thru 112.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 3).
RECODE Q81 (113 thru Highest=1) (Lowest thru 112.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 4).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 5).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 6).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 7).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 9 & heightgroup = 0).
RECODE Q81 (112 thru Highest=1) (Lowest thru 111.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 1).
RECODE Q81 (112 thru Highest=1) (Lowest thru 111.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 2).
RECODE Q81 (114 thru Highest=1) (Lowest thru 113.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 3).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 4).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 5).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 6).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 7).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 10 & heightgroup = 0).
RECODE Q81 (113 thru Highest=1) (Lowest thru 112.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 1).
RECODE Q81 (114 thru Highest=1) (Lowest thru 113.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 2).
RECODE Q81 (116 thru Highest=1) (Lowest thru 115.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 3).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 4).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 5).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 6).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 7).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 0).
RECODE Q81 (115 thru Highest=1) (Lowest thru 114.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 1).
RECODE Q81 (116 thru Highest=1) (Lowest thru 115.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 2).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 3).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 4).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 5).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 6).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 12 & heightgroup = 0).
RECODE Q81 (116 thru Highest=1) (Lowest thru 115.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 1).
RECODE Q81 (117 thru Highest=1) (Lowest thru 116.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 2).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 3).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 4).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 5).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 6).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 13 & heightgroup = 0).
RECODE Q81 (118 thru Highest=1) (Lowest thru 117.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 1).
RECODE Q81 (119 thru Highest=1) (Lowest thru 118.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 2).
RECODE Q81 (120 thru Highest=1) (Lowest thru 119.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 3).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 4).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 5).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 6).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 14 & heightgroup = 0).
RECODE Q81 (120 thru Highest=1) (Lowest thru 119.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 1).
RECODE Q81 (120 thru Highest=1) (Lowest thru 119.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 2).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 3).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 4).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 5).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 6).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 2 & age = 15 & heightgroup = 0).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 1).
RECODE Q81 (121 thru Highest=1) (Lowest thru 120.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 2).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 3).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 4).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 5).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 6).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 2 & age = 16 & heightgroup = 0).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 1).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 2).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 3).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 4).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 5).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 6).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.


DO IF  (gender = 2 & age = 17 & heightgroup = 0).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 1).
RECODE Q81 (122 thru Highest=1) (Lowest thru 121.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 2).
RECODE Q81 (123 thru Highest=1) (Lowest thru 122.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 3).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 4).
RECODE Q81 (124 thru Highest=1) (Lowest thru 123.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 5).
RECODE Q81 (125 thru Highest=1) (Lowest thru 124.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 6).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 7).
RECODE Q81 (126 thru Highest=1) (Lowest thru 125.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.

*女生舒张压*

DO IF  (gender = 2 & age = 7 & heightgroup = 0).
RECODE Q82 (73 thru Highest=1) (Lowest thru 72.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 1).
RECODE Q82 (73 thru Highest=1) (Lowest thru 72.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 2).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 3).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 7 & heightgroup = 4).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 5).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 7 & heightgroup = 6).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 7 & heightgroup = 7).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 8 & heightgroup = 0).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 1).
RECODE Q82 (74 thru Highest=1) (Lowest thru 73.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 2).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age =8 & heightgroup = 3).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 8 & heightgroup = 4).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 5).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 8 & heightgroup = 6).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 8 & heightgroup = 7).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 9 & heightgroup = 0).
RECODE Q82 (75 thru Highest=1) (Lowest thru 74.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 1).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 2).
RECODE Q82 (76 thru Highest=1) (Lowest thru 75.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 3).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 9 & heightgroup = 4).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 5).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 9 & heightgroup = 6).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 9 & heightgroup = 7).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 10 & heightgroup = 0).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 1).
RECODE Q82 (77 thru Highest=1) (Lowest thru 76.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 2).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 3).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 10 & heightgroup = 4).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 5).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 10 & heightgroup = 6).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 10 & heightgroup = 7).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 11 & heightgroup = 0).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 1).
RECODE Q82 (78 thru Highest=1) (Lowest thru 77.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 2).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 3).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 11 & heightgroup = 4).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 5).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 11 & heightgroup = 6).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 11 & heightgroup = 7).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 12 & heightgroup = 0).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 1).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 2).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 3).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 12 & heightgroup = 4).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 5).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 12 & heightgroup = 6).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 12 & heightgroup = 7).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 13 & heightgroup = 0).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 1).
RECODE Q82 (79 thru Highest=1) (Lowest thru 78.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 2).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 3).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 13 & heightgroup = 4).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 5).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 13 & heightgroup = 6).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 13 & heightgroup = 7).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 14 & heightgroup = 0).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 1).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 2).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 3).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 14 & heightgroup = 4).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 5).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 14 & heightgroup = 6).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 14 & heightgroup = 7).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 15 & heightgroup = 0).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 1).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 2).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 3).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 15 & heightgroup = 4).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 5).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 15 & heightgroup = 6).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 15 & heightgroup = 7).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 16 & heightgroup = 0).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 1).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 2).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 3).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 16 & heightgroup = 4).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 5).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 16 & heightgroup = 6).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 16 & heightgroup = 7).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

DO IF  (gender = 2 & age = 17 & heightgroup = 0).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 1).
RECODE Q82 (80 thru Highest=1) (Lowest thru 79.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 2).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 3).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 17 & heightgroup = 4).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 5).
RECODE Q82 (81 thru Highest=1) (Lowest thru 80.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age = 17 & heightgroup = 6).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age = 17 & heightgroup = 7).
RECODE Q82 (82 thru Highest=1) (Lowest thru 81.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

*18岁男女生沿用成人标准*

DO IF  (gender = 1 & age >= 18).
RECODE Q81 (140 thru Highest=1) (Lowest thru 139.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender = 2 & age >= 18).
RECODE Q81 (140 thru Highest=1) (Lowest thru 139.99999999=0) INTO SBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =1 & age >= 18).
RECODE Q82 (90 thru Highest=1) (Lowest thru 89.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.
DO IF  (gender =2 & age >= 18).
RECODE Q82 (90 thru Highest=1) (Lowest thru 89.99999999=0) INTO DBP_GROUP.
END IF.
EXECUTE.

*定义高血压*


COMPUTE HBP=SBP_GROUP + DBP_GROUP.
EXECUTE.
VARIABLE LABELS HBP '+'.

RECODE HBP (0=0) (1 thru 2=1) INTO HBP_GROUP.
EXECUTE.

IF (血压异常3=1) HBP_GROUP=9.
EXECUTE.
IF (MISSING(heightgroup) and age<18) HBP_GROUP=9.
EXECUTE.

VARIABLE LABELS HBP_GROUP '血压偏高'.
VALUE LABELS HBP_GROUP 1'是' 0'否'.

IF (血压异常3=1) SBP_GROUP=9.
EXECUTE.
IF (MISSING(heightgroup) and age<18) SBP_GROUP=9.
EXECUTE.
IF (血压异常3=1) DBP_GROUP=9.
EXECUTE.
IF (MISSING(heightgroup) and age<18) DBP_GROUP=9.
EXECUTE.

VARIABLE LABELS SBP_GROUP '收缩压偏高'.
VALUE LABELS SBP_GROUP 1'是' 0'否'.
VARIABLE LABELS DBP_GROUP '舒张压偏高'.
VALUE LABELS DBP_GROUP 1'是' 0'否'.

