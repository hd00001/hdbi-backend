package com.hd.hdbibackend.bizmq;

import com.hd.hdbibackend.common.ErrorCode;
import com.hd.hdbibackend.exception.BusinessException;
import com.hd.hdbibackend.manager.AiManager;
import com.hd.hdbibackend.model.entity.Chart;
import com.hd.hdbibackend.service.ChartService;
import com.hd.hdbibackend.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @auther hd
 * @Description
 */
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;
    //指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    public void receiverMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.info("receiveMessage message = {}", message);
        if (StringUtils.isBlank(message)) {
//            更新失败,拒绝当前消息,让消息重新进入队列
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null){
//            图表为空，拒绝消息并抛出业务异常
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }

        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            handleChartUpdateError(chart.getId(),"更新图表执行中失败");
            return;
        }
        String res = aiManager.doChat(buildUserInput(chart));
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
//            如果更新图标成功状态失败，拒绝消息并处理图标更新错误
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"更新图表成功，状态失败");
        }

        log.info("receiveMessage message={}",message);
        channel.basicAck(deliveryTag,false);

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
    private String buildUserInput(Chart chart) {

        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

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
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }
}
