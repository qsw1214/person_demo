package com.wondersgroup.healthcloud.jpa.entity.doctor;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by longshasha on 16/8/1.
 */

@Data
@Entity
@Table(name = "doctor_service_dic")
public class DoctorServiceDic {

    @Id
    private String id;

    private String name;

    //服务图标
    private String icon;

    //副标题
    private String subtitle;

    //关键字
    private String keyword;

    //跳转地址
    private String url;

    @Column(name = "is_available")
    private String isAvailable;

    @Column(name = "del_flag")
    private String delFlag = "0";

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "create_by")
    private String createBy;

    @Column(name = "create_date")
    private Date createDate;

    @Column(name = "update_by")
    private String updateBy;

    @Column(name = "update_date")
    private Date updateDate;


}
