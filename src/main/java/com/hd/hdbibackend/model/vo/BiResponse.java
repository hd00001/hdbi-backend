package com.hd.hdbibackend.model.vo;

import lombok.Data;

/**
 * @auther hd
 * @Description BI 方法返回
 */
@Data
public class BiResponse {

    private String genChart;

    private String genResult;

    private Long chartId;
}
