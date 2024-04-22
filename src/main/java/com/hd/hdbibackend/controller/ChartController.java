package com.hd.hdbibackend.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.hdbibackend.annotation.AuthCheck;
import com.hd.hdbibackend.bizmq.BiMessageProducer;
import com.hd.hdbibackend.common.BaseResponse;
import com.hd.hdbibackend.common.DeleteRequest;
import com.hd.hdbibackend.common.ErrorCode;
import com.hd.hdbibackend.common.ResultUtils;
import com.hd.hdbibackend.constant.CommonConstant;
import com.hd.hdbibackend.constant.FileConstant;
import com.hd.hdbibackend.constant.UserConstant;
import com.hd.hdbibackend.exception.BusinessException;
import com.hd.hdbibackend.exception.ThrowUtils;
import com.hd.hdbibackend.manager.AiManager;
import com.hd.hdbibackend.manager.RedisLimiterManager;
import com.hd.hdbibackend.model.dto.chart.*;
import com.hd.hdbibackend.model.dto.file.UploadFileRequest;
import com.hd.hdbibackend.model.entity.Chart;
import com.hd.hdbibackend.model.entity.User;
import com.hd.hdbibackend.model.enums.FileUploadBizEnum;
import com.hd.hdbibackend.model.vo.BiResponse;
import com.hd.hdbibackend.service.ChartService;
import com.hd.hdbibackend.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.hd.hdbibackend.utils.ExcelUtils;
import com.hd.hdbibackend.utils.SqlUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
 * @author hd
 * @from hd
 */
@RestController
@RequestMapping("/chart")
@Slf4j
@CrossOrigin(origins = {"http://localhost:8000"},allowCredentials = "true")
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private UserService userService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
               getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

//    /**
//     * 分页搜索（从 ES 查询，封装类）
//     *
//     * @param chartQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/search/page/vo")
//    public BaseResponse<Page<Chart>> searchChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
//            HttpServletRequest request) {
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> chartPage = chartService.searchFromEs(chartQueryRequest);
//        return ResultUtils.success(chartService.getChartVOPage(chartPage, request));
//    }

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(chartType),"chartType",chartType);
        queryWrapper.like(StringUtils.isNotBlank(name),"name",name);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq(StringUtils.isNotBlank(goal),"goal",goal);
        queryWrapper.eq(StringUtils.isNotBlank(sortField),"sortField",sortField);
        queryWrapper.eq("isDelete",false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     *
     * 智能分析(同步)
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
//        校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
//      校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");
//        校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

//        限流判断(根据方法(每个方法不影响)，每个用户一个限流器)
        redisLimiterManager.doRateLimit("genChartByAi" + String.valueOf(loginUser.getId()));

        //  分析需求:
        //  原始数据:

//        用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ", 请使用" + chartType;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据").append("\n");
//        压缩后的数据
        String result = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(result).append("\n");
        String res = aiManager.doChat(userInput.toString());
        String[] split = res.split("#####");
        if (split.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
//        插入到数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(result);
        chart.setStatus("succeed");
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setName(name);
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR,"图标保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析(异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
//        校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"参数为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,ErrorCode.PARAMS_ERROR,"名称过长");
//      校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR,"文件超过1M");
//        校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR,"文件后缀非法");

//        限流判断(根据方法(每个方法不影响)，每个用户一个限流器)
        redisLimiterManager.doRateLimit("genChartByAi" + String.valueOf(loginUser.getId()));

        //  分析需求:
        //  原始数据:

//        用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ", 请使用" + chartType;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据").append("\n");
//        压缩后的数据
        String result = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(result).append("\n");

//        先把图表保存到数据库里
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        chart.setName(name);
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR,"图标保存失败");

//      返回最终结果之前提交任务
//        todo 处理任务队列满了后,抛异常(提交任务报错,前端返回异常)
        CompletableFuture.runAsync(() -> {
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(),"更新图表执行中失败");
                return;
            }
            String res = aiManager.doChat(userInput.toString());
            String[] split = res.split("#####");
            if (split.length < 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
            }
            String genChart = split[1].trim();
            String genResult = split[2].trim();
//            调用AI得到结果后.再更新一次
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean b1 = chartService.updateById(updateChartResult);
            if (!b1) {
                handleChartUpdateError(chart.getId(),"更新图表失败");
            }
        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
//        biResponse.setGenChart(genChart);
//        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }
    /**
     * 智能分析(异步消息队列)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
//        校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"参数为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,ErrorCode.PARAMS_ERROR,"名称过长");
//      校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR,"文件超过1M");
//        校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR,"文件后缀非法");

//        限流判断(根据方法(每个方法不影响)，每个用户一个限流器)
        redisLimiterManager.doRateLimit("genChartByAi" + String.valueOf(loginUser.getId()));

        //  分析需求:
        //  原始数据:

//        用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ", 请使用" + chartType;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据").append("\n");
//        压缩后的数据
        String result = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(result).append("\n");

//        先把图表保存到数据库里
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        chart.setName(name);
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR,"图标保存失败");
        long newChartId = chart.getId();

//      返回最终结果之前提交任务
//        todo 处理任务队列满了后,抛异常(提交任务报错,前端返回异常)
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        BiResponse biResponse = new BiResponse();
//        biResponse.setGenChart(genChart);
//        biResponse.setGenResult(genResult);
        biResponse.setChartId(newChartId);

        return ResultUtils.success(biResponse);
    }

//    异常处理工具类
    private void handleChartUpdateError(long chartId,String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图标失败" + chartId + "," + execMessage);
        }
    }
}


