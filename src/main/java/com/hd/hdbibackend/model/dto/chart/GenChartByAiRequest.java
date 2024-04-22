package com.hd.hdbibackend.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * @auther hd
 * @Description
 */
@Data
public class GenChartByAiRequest implements Serializable {

    private static final long serialVersionUID = 3396796108990566405L;

    /**
     * 名称
     */
    private String goal;

    /**
     * 图标类型
     */
    private String chartType;

    private String name;

}
