package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.v1.model.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class RegexDebug {
    public static void main(String[] args) throws Exception {
        String text = PrototypeFileReaders.readSpssText(Paths.get("docs/sources/sps/结局判定/血压偏高语法.sps"));

        // Test the old vs new regex
        Pattern old = Pattern.compile("(?im)^[ \\t]*RECODE\\s+([^\\r\\n]+?)\\s+INTO\\s+([^\\.\\r\\n]+)\\.");
        Pattern neu = Pattern.compile("(?ims)^[ \\t]*RECODE\\s+(.+?)\\s+INTO\\s+([^\\.]+?)\\.");

        System.out.println("=== Old regex ===");
        Matcher mo = old.matcher(text);
        int oldCount = 0;
        while (mo.find()) {
            oldCount++;
            String t = mo.group(2).trim();
            if (t.equalsIgnoreCase("heightgroup")) {
                System.out.println("  heightgroup at " + mo.start());
            }
        }
        System.out.println("  Total: " + oldCount);

        System.out.println("=== New regex ===");
        Matcher mn = neu.matcher(text);
        int newCount = 0;
        while (mn.find()) {
            newCount++;
            String t = mn.group(2).trim();
            if (t.equalsIgnoreCase("heightgroup")) {
                System.out.println("  heightgroup at " + mn.start() + " (len=" + mn.group().length() + ")");
            }
        }
        System.out.println("  Total: " + newCount);

        // How many unique targets?
        Set<String> targets = new LinkedHashSet<>();
        mn.reset();
        while (mn.find()) targets.add(mn.group(2).trim().toLowerCase());
        System.out.println("  Targets: " + targets);
    }
}
