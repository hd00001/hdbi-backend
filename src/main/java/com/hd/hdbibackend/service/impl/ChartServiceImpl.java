package com.hd.hdbibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.hd.hdbibackend.mapper.ChartMapper;
import com.hd.hdbibackend.model.entity.Chart;
import com.hd.hdbibackend.service.ChartService;
import org.springframework.stereotype.Service;

/**
* @author 29077
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-04-16 20:19:54
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {

}




