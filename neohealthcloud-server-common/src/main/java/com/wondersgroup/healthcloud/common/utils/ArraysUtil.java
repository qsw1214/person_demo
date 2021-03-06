package com.wondersgroup.healthcloud.common.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by ys on 16/08/11.
 *
 */
public class ArraysUtil {


    /**
     * 分割int数组 list to string
     */
    public static String splitInts2Sting(Iterable<Integer> list, String separator){
        if (list == null) {
            return "";
        }
        String rt = "";
        for (Integer str : list) {
            rt += separator + str.toString();
        }
        return rt.length() > 0 ? rt.substring(separator.length()) : "";
    }

    /**
     * 分割数组 list to string
     * list value为null or "" 则抛弃这个value
     */
    public static String split2Sting(Iterable<String> list, String separator){
        if (list == null) {
            return "";
        }
        String rt = "";
        for (String str : list) {
            if (StringUtils.isNotEmpty(str)){
                rt += separator + str;
            }
        }
        return rt.length() > 0 ? rt.substring(separator.length()) : "";
    }

}
