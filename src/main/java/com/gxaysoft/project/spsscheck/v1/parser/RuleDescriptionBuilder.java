package com.gxaysoft.project.spsscheck.v1.parser;

import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.model.RowContext;

import java.util.List;

public final class RuleDescriptionBuilder {

    private RuleDescriptionBuilder() {}

    public static String build(SpssCheckRule rule) {
        if (rule.isCheckRule()) return buildCheckDesc(rule);
        return buildComputeDesc(rule);
    }

    private static String buildComputeDesc(SpssCheckRule rule) {
        String target = rule.getTarget();
        if (rule.getSteps().isEmpty()) {
            // Single COMPUTE
            String expr = rule.getExpression();
            if (target.contains("ID3") || target.equals("ID3"))
                return "根据省份(10^5)+地市(10^3)+区县(10^1)+监测点编码，生成复合标识" + target;
            if (target.contains("是否一致") || target.equals("ID是否一致"))
                return "计算" + target + "：检验ID编码与分项编码是否匹配";
            if (target.contains("age"))
                return "根据检查日期和出生日期计算" + target + "（年龄）";
            if (target.equals("BMI"))
                return "根据身高(Q6)和体重(Q7)计算BMI指数：体重(kg)×10000/(身高(cm))^2";
            if (target.equals("BPC"))
                return "计算脉压差：收缩压(Q81)-舒张压(Q82)";
            return "计算中间变量" + target + " = " + expr;
        }
        // COMPUTE with RECODE steps
        return "计算" + target + "并进行重编码校验";
    }

    private static String buildCheckDesc(SpssCheckRule rule) {
        String target = rule.getTarget();
        String name = rule.getLabel();

        if (target.contains("ID是否一致"))
            return "校验ID是否一致：编码不匹配标记为1，一致标记为0";
        if (target.contains("重复") || target.contains("PrimaryFirst"))
            return "去重标记：" + target + "：每组首条标记为1（不重复），重复记录为0";
        if (target.contains("缺失") || target.contains("缺失或异常"))
            return "检查" + (name != null ? name : target) + "：存在缺失或取值异常标记为1，正常为0";
        if (target.contains("人员配备"))
            return "校验" + name + "：人员总数应等于专职+兼职人数，不等则标记为1";
        if (target.contains("经费"))
            return "校验" + name + "：总经费应≥到账经费且≥支出经费，不一致标记为1";
        if (target.contains("学生情况"))
            return "校验" + name + "：在校人数应等于男生+女生，不一致标记为1";
        if (target.contains("学校情况"))
            return "校验" + name + "：辖区内学校数量应≥中小学数+大学数，不一致标记为1";
        if (target.contains("身高") || target.contains("体重"))
            return "检查" + name + "：根据年龄/性别对应的正常范围，超出范围或缺失标记为1";
        if (target.contains("龋齿"))
            return "检查" + name + "：龋齿数或缺失数超出正常范围标记为1";
        if (target.contains("血压") || target.contains("压差") || target.contains("收缩压") || target.contains("舒张压"))
            return "检查" + name + "：血压值超出正常范围或压差异常标记为1";
        if (target.contains("脊柱"))
            return "检查" + name + "：脊柱筛查结果异常或缺失标记为1";
        if (target.contains("证件"))
            return "检查" + name + "：证件号码缺失、位数异常或与基本信息不符标记为1";
        if (target.contains("年龄"))
            return "检查" + name + "：年龄超出正常范围或与年级不匹配标记为1";
        if (target.contains("眼镜") || target.contains("视力") || target.contains("近视"))
            return "检查" + name + "：视力筛查数据异常或缺失标记为1";
        if (target.contains("教室") || target.contains("年级"))
            return "检查" + name + "：教室或年级代码缺失标记为1";
        if (target.contains("BMI"))
            return "BMI判定" + name + "：根据年龄/性别标准判定超重、肥胖或营养不良";
        if (target.contains("heightgroup") || target.equalsIgnoreCase("heightgroup"))
            return "身高分组：根据年龄/性别将身高映射到百分位区间(0~7)";
        if (target.contains("SBP_GROUP") || target.contains("DBP_GROUP"))
            return "血压偏高判定" + name + "：根据身高分组和年龄判定收缩压/舒张压是否偏高";
        if (target.contains("HBP"))
            return "高血压综合判定" + name + "：综合SBP和DBP判定是否血压偏高";
        if (target.contains("异常") || target.contains("可疑"))
            return "检查" + name + "：数据存在异常或可疑标记为1";

        // Generic fallback
        String sources = String.join("、", rule.getSourceVariables());
        if (sources.isEmpty()) return "校验规则：" + (name != null ? name : target);
        return "校验" + (name != null ? name : target) + "：基于" + sources + "判定";
    }

    public static String buildForOutput(SpssOutputRule rule) {
        String name = rule.getSheetName();
        if (name.contains("清理后"))
            return "清洗后数据：筛除所有异常记录后保留的正常数据";
        if (name.contains("ID不一致"))
            return "异常分组：ID编码与分项编码不一致的记录";
        if (name.contains("ID重复"))
            return "异常分组：ID重复的记录（非首次出现）";
        if (name.contains("基本信息缺失"))
            return "异常分组：基本信息（地市/县区/城乡编码等）存在缺失或异常";
        if (name.contains("人员配备"))
            return "异常分组：人员配备数据不一致的记录";
        if (name.contains("经费"))
            return "异常分组：经费数据不一致的记录";
        if (name.contains("学校信息"))
            return "异常分组：学校基本信息缺失的记录";
        if (name.contains("学校情况"))
            return "异常分组：学校数量情况数据不一致的记录";
        if (name.contains("学生情况"))
            return "异常分组：学生人数数据不一致的记录";
        if (name.contains("年龄异常"))
            return "异常分组：年龄超出正常范围的记录";
        if (name.contains("年龄可疑"))
            return "异常分组：年龄与年级不匹配的记录";
        if (name.contains("身高体重") || name.contains("身高"))
            return "异常分组：身高或体重超出正常范围或缺失的记录";
        if (name.contains("腰围"))
            return "异常分组：腰围超出正常范围或缺失的记录";
        if (name.contains("血压"))
            return "异常分组：血压值异常或压差异常的记录";
        if (name.contains("龋齿"))
            return "异常分组：龋齿数据异常的记录";
        if (name.contains("脊柱"))
            return "异常分组：脊柱筛查数据异常或缺失的记录";
        if (name.contains("证件"))
            return "异常分组：证件号码缺失或异常的记录";
        if (name.contains("身份证"))
            return "异常分组：身份证信息与出生日期或性别不符的记录";
        if (name.contains("教室") || name.contains("年级"))
            return "异常分组：教室或年级代码缺失的记录";
        if (name.contains("近视"))
            return "异常分组：近视筛查数据异常或缺失的记录";
        return "输出分组：" + name;
    }
}
