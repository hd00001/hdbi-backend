package com.hd.hdbibackend.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @auther hd
 * @Description excel 相关工具类 (excel 转 csv 进行原始数据压缩)
 */
@Slf4j
public class ExcelUtils {

    public static String excelToCsv(MultipartFile multipartFile){
//        File file = null;
//        try {
//            file = ResourceUtils.getFile("classpath:网站.xlsx");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        读取数据
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误");
        }
        if (CollUtil.isEmpty(list)){
            return "";
        }
        //        转换为csv
        StringBuilder stringBuilder = new StringBuilder();
//        读取表头
        LinkedHashMap<Integer, String> stringMap = (LinkedHashMap<Integer, String>) list.get(0);
        List<String> stringList = stringMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(stringList,",")).append("\n");
//        读取数据
        for (int i = 1; i < list.size() ; i++) {
            LinkedHashMap<Integer, String> dataMap =(LinkedHashMap<Integer, String>) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList,",")).append("\n");
        }
        return stringBuilder.toString();
    }

    public static void main(String[] args) {
        excelToCsv(null);
    }
}
