package com.wondersgroup.healthcloud.api.http.dto.activity;

import java.text.SimpleDateFormat;
import java.util.List;

import lombok.Data;

import com.google.common.collect.Lists;
import com.wondersgroup.healthcloud.jpa.entity.activity.HealthActivityInfo;

@Data
public class HealthActivityInfoDTO {

    private static final long serialVersionUID = -6988698188678786263L;
    
    private String            activityid;
    private String            host;                                    // '主办者',
    private String            type;                                    // '活动类型 1：糖尿病:2：高血压',
    private String            title;                                   // '标题',
    private String            releasetime;                             // '发布时间',
    private String            summary;                                 // '活动概述',
    private String            starttime;                               // '开始时间',
    private String            endtime;                                 // '结束时间',
    private String            province;                                // '地址 省 area字典表代码',
    private String            city;                                    // '地址 市 area字典表代码',
    private String            county;                                  // '地址 县或区 area字典表代码',
    private String            locate;                                  // '举办地点',
    private String            photo;                                   // '活动图片存入attach表',
    private String            speaker;                                 // '主讲人信息 姓名 科室 职务',
    private String            department;                              //科室
    private String            pftitle;                                 //职称
    private String            iscancel;                                // '是否取消 0：未取消 1：取消',
    private Integer           quota;                                   // '名额',
    private Integer           style;                                   //'活动形式1：讲座 2：表演',
    private String            thumbnail;                               //活动缩略图
    private String            online_status;                            //0 未上线 1已上线 2已下线',
    private String            online_time;                              //上线时间
    private String            offline_time;                             //下线时间
    private String            enroll_start_time;                         //活动报名开始时间'
    private String            enroll_end_time;                           //活动报名结束时间
    private String            update_date;
    
    public HealthActivityInfoDTO(){
        
    }
    
    public HealthActivityInfoDTO(HealthActivityInfo info) {
        if(info == null){
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        this.activityid = info.getActivityid();
        this.host = info.getHost();
        this.type = info.getType();
        this.title = info.getTitle();
        this.releasetime = info.getReleasetime() == null ? null : format.format(info.getReleasetime());
        this.summary = info.getSummary();
        this.starttime = info.getStarttime() == null ? null : format.format(info.getStarttime());
        this.endtime = info.getEndtime() == null ? null : format.format(info.getEndtime());
        this.province = info.getProvince();
        this.city = info.getCity();
        this.county = info.getCounty();
        this.locate = info.getLocate();
        this.photo = info.getPhoto();
        this.speaker = info.getSpeaker();
        this.department = info.getDepartment();
        this.pftitle = info.getPftitle();
        this.iscancel = info.getIscancel();
        this.quota = info.getQuota();
        this.style = info.getStyle();
        this.thumbnail = info.getThumbnail();
        this.online_status = info.getOnlineStatus();
        this.online_time = info.getOnlineTime() == null ? null : format.format(info.getOnlineTime());
        this.offline_time = info.getOfflineTime() == null ? null : format.format(info.getOfflineTime());
        this.enroll_start_time = info.getEnrollStartTime() == null ? null : format.format(info.getEnrollStartTime());
        this.enroll_end_time = info.getEnrollEndTime() == null ? null : format.format(info.getEnrollEndTime());
        this.update_date = info.getUpdateDate() == null ? null : format.format(info.getUpdateDate());
    }

    public static List<HealthActivityInfoDTO> infoDTO(List<HealthActivityInfo> infos){
        if(infos == null){
            return  Lists.newArrayList();
        }
        List<HealthActivityInfoDTO> infoDTO = Lists.newArrayList();
        for (HealthActivityInfo info : infos) {
            infoDTO.add(new HealthActivityInfoDTO(info));
        }
        return infoDTO;
    }
    
}
