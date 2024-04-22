package com.hd.hdbibackend.model.dto.chart;

import com.hd.hdbibackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {


    private Long id;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    private Long userId;

    private String name;

    private static final long serialVersionUID = 1L;

}