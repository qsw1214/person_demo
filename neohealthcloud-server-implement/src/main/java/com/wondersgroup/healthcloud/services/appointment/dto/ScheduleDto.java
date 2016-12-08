package com.wondersgroup.healthcloud.services.appointment.dto;


import lombok.Data;

import java.util.Date;

/**
 * Created by longshasha on 16/12/7.
 */
@Data
public class ScheduleDto {

    private String id;

    private String name;

    /**
     * 医生头像
     */
    private String avatar;

    /**
     *医生职称
     */
    private String dutyName;

    /**
     * 医生简介
     */
    private String specialty;

    /**
     * 预约数
     */
    private String reservationNum;


    /**
     * 1:医生 2:科室
     */
    private String type;

    /**
     * 就诊时间
     */
    private Date scheduleDate;

    private Date startTime;

    private Date endTime;

    /**
     * 出诊费用
     */
    private String visitCost;

    private Integer reserveOrderNum;




}
