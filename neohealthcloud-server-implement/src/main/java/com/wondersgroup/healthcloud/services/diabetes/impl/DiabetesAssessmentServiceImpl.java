package com.wondersgroup.healthcloud.services.diabetes.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.okhttp.Request;
import com.wondersgroup.common.http.HttpRequestExecutorManager;
import com.wondersgroup.common.http.builder.RequestBuilder;
import com.wondersgroup.common.http.entity.JsonNodeResponseWrapper;
import com.wondersgroup.healthcloud.common.utils.IdGen;
import com.wondersgroup.healthcloud.exceptions.Exceptions;
import com.wondersgroup.healthcloud.jpa.entity.diabetes.DiabetesAssessment;
import com.wondersgroup.healthcloud.jpa.entity.diabetes.DiabetesAssessmentRemind;
import com.wondersgroup.healthcloud.jpa.repository.diabetes.DiabetesAssessmentRemindRepository;
import com.wondersgroup.healthcloud.jpa.repository.diabetes.DiabetesAssessmentRepository;
import com.wondersgroup.healthcloud.services.diabetes.DiabetesAssessmentService;
import com.wondersgroup.healthcloud.services.diabetes.dto.DiabetesAssessmentDTO;
import com.wondersgroup.healthcloud.utils.PageFactory;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhuchunliu on 2016/12/6.
 */
@Service("diabetesAssessmentService")
public class DiabetesAssessmentServiceImpl implements DiabetesAssessmentService{

    private static final Logger logger = LoggerFactory.getLogger("exlog");

    @Autowired
    private DiabetesAssessmentRepository assessmentRepo;

    @Autowired
    private TubeRelationServiceImpl tubeRelationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DiabetesAssessmentRemindRepository remindRepo;

    @Autowired
    private HttpRequestExecutorManager httpRequestExecutorManager;

    @Value("${JOB_CONNECTION_URL}")
    private String jobClientUrl;


    /**
     * 肾病风险评估
     * @param assessment
     * @return
     */
    @Override
    public Integer kidney(DiabetesAssessment assessment) {

        assessment.setId(IdGen.uuid());
        assessment.setType(2);
        assessment.setCreateDate(new Date());
        assessment.setUpdateDate(new Date());
        assessment.setDelFlag("0");


        int total = assessment.getIsHistory() + assessment.getIsEyeHistory() + assessment.getIsPressureHistory()
                + assessment.getIsUrine() + assessment.getIsEdema() + assessment.getIsTired() + assessment.getIsCramp();
        assessment.setResult(total == 0 ?0 :1);


        assessmentRepo.save(assessment);
        return assessment.getResult();
    }

    /**
     * 眼病风险评估
     * @param assessment
     * @return
     */
    @Override
    public Integer eye(DiabetesAssessment assessment) {
        assessment.setId(IdGen.uuid());
        assessment.setType(3);
        assessment.setCreateDate(new Date());
        assessment.setUpdateDate(new Date());
        assessment.setDelFlag("0");


        int total = assessment.getIsEyeSight() + assessment.getIsEyeFuzzy() + assessment.getIsEyeShadow()
                + assessment.getIsEyeGhosting() + assessment.getIsEyeFlash();
        assessment.setResult(total == 0 ?0 :1);


        assessmentRepo.save(assessment);
        return assessment.getResult();
    }

    /**
     * 足部风险评估
     * @param assessment
     * @return
     */
    @Override
    public Integer foot(DiabetesAssessment assessment) {
        assessment.setId(IdGen.uuid());
        assessment.setType(4);
        assessment.setCreateDate(new Date());
        assessment.setUpdateDate(new Date());
        assessment.setDelFlag("0");


        int total = assessment.getIsHbac()+assessment.getIsSmoking() + assessment.getIsEyeProblem() + assessment.getIsKidney()
                + assessment.getIsCardiovascular() + assessment.getIsLimbsEdema() + assessment.getIsLimbsTemp()
                + assessment.getIsDeformity() + assessment.getIsFootBeat() + assessment.getIsShinBeat();
        assessment.setResult(total == 0 ?0 :1);

        assessmentRepo.save(assessment);
        return assessment.getResult();
    }

    @Override
    public List<DiabetesAssessmentDTO> findAssessment(Integer pageNo, Integer pageSize, String name) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("select t1.id, t1.age, t1.gender, t1.height, " +
                " t1.weight, t1.waist, t1.isIGR, t1.isSit, t1.isFamily, t1.isLargeBaby, t1.isHighPressure, t1.isBloodFat, " +
                " t1.isArteriesHarden, t1.isSterol, t1.isPCOS, t1.isMedicineTreat," +
                " t1.create_date,t2.name " +
                " from (select * from app_tb_diabetes_assessment order by create_date desc) t1 ");
        buffer.append(" join app_tb_register_info t2 on t1.registerid = t2.registerid");
        buffer.append(" where  t1.type = 1 and t1.result = 1 " );
        if(!StringUtils.isEmpty(name)){
            buffer.append(" and t2.name like '%"+ name+"%'");
        }
        buffer.append(" and NOT EXISTS (select * from app_tb_diabetes_tube_relation where registerid = t1.registerid and del_flag = '0')");
        buffer.append(" and NOT EXISTS (select * from app_tb_diabetes_assessment_remind where \n" +
                " registerid = t1.registerid and  DATEDIFF(create_date,t1.create_date) >= 0 and del_flag = '0')");
        buffer.append(" and t1.del_flag = '0' and t2.identifytype != '0' ");
        buffer.append(" group by t1.registerid ");
        buffer.append(" order by t1.create_date desc ");
        buffer.append(" limit "+(pageNo-1)*pageSize+","+pageSize);
//        return jdbcTemplate.queryForList(buffer.toString(),DiabetesAssessmentDTO.class);
        return jdbcTemplate.query(buffer.toString(), new RowMapper<DiabetesAssessmentDTO>() {
            @Override
            public DiabetesAssessmentDTO mapRow(ResultSet resultSet, int i) throws SQLException {
                DiabetesAssessmentDTO dto = new DiabetesAssessmentDTO();
                dto.setId(resultSet.getString("id"));
                dto.setAge(resultSet.getInt("age"));
                dto.setGender(resultSet.getInt("gender"));
                dto.setHeight(resultSet.getDouble("height"));
                dto.setWeight(resultSet.getDouble("weight"));
                dto.setWaist(resultSet.getDouble("waist"));
                dto.setIsIGR(resultSet.getInt("isIGR"));
                dto.setIsSit(resultSet.getInt("isSit"));
                dto.setIsFamily(resultSet.getInt("isFamily"));
                dto.setIsLargeBaby(resultSet.getInt("isLargeBaby"));
                dto.setIsHighPressure(resultSet.getInt("isHighPressure"));
                dto.setIsBloodFat(resultSet.getInt("isBloodFat"));
                dto.setIsArteriesHarden(resultSet.getInt("isArteriesHarden"));
                dto.setIsSterol(resultSet.getInt("isSterol"));
                dto.setIsPCOS(resultSet.getInt("isPCOS"));
                dto.setIsMedicineTreat(resultSet.getInt("isMedicineTreat"));
                dto.setCreate_date(resultSet.getDate("create_date"));
                dto.setName(resultSet.getString("name"));
                return dto;
            }
        });
    }

    @Override
    public Integer findAssessmentTotal(String name) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("select t1.registerid from " +
                "   (select * from app_tb_diabetes_assessment order by create_date desc) t1 ");
        buffer.append(" join app_tb_register_info t2 on t1.registerid = t2.registerid");

        buffer.append(" where  t1.type = 1 and t1.result = 1 " );
        if(!StringUtils.isEmpty(name)){
            buffer.append(" and t2.name like '%"+ name+"%'");
        }
        buffer.append(" and not exists (select * from app_tb_diabetes_tube_relation where registerid = t1.registerid and del_flag = '0')");
        buffer.append(" and NOT EXISTS (select * from app_tb_diabetes_assessment_remind where \n" +
                " registerid = t1.registerid and  DATEDIFF(create_date,t1.create_date) >= 0 and del_flag = '0')");
        buffer.append(" and t1.del_flag = '0' and t2.identifytype != '0' ");
        buffer.append(" GROUP BY t1.registerid");

        return jdbcTemplate.queryForList(buffer.toString()).size();
    }

    @Override
    public Boolean  remind(List<String> registerIds , String doctorId) {

        for(String registerid : registerIds){
            DiabetesAssessmentRemind remind = new DiabetesAssessmentRemind();
            remind.setId(IdGen.uuid());
            remind.setRegisterid(registerid);
            remind.setDoctorId(doctorId);
            remind.setCreateDate(new Date());
            remind.setUpdateDate(new Date());
            remind.setDelFlag("0");
            remindRepo.save(remind);

            String param = "{\"notifierUID\":\""+doctorId+"\",\"receiverUID\":\""+registerid+"\",\"msgType\":\"1\",\"msgTitle\":\"筛查提醒\",\"msgContent\":\"您的风险评估结果存在异常，建议您到所属社区卫生服务中心进行糖尿病高危筛查。\"}";
            Request build= new RequestBuilder().post().url(jobClientUrl+"/api/disease/message").body(param).build();
            JsonNodeResponseWrapper response = (JsonNodeResponseWrapper) httpRequestExecutorManager.newCall(build).run().as(JsonNodeResponseWrapper.class);
            JsonNode result = response.convertBody();

            if(0 != result.get("code").asInt()){
                logger.error("高危筛查提醒失败 "+registerid+" "+doctorId);
                logger.error(result.toString());
            }

        }
        return true;
    }




    @Override
    public Map<String, Object> getLastAssessmentResult(String uid) {
        Map<String, Object> rtnMap = new HashMap<>();
        String sql = "SELECT type, result FROM app_tb_diabetes_assessment WHERE del_flag = '0' AND registerid = '" + uid + "' ORDER BY update_date DESC LIMIT 0, 1";
        try {
            AssessmentResult ar = jdbcTemplate.queryForObject(sql.toString(), new BeanPropertyRowMapper<>(AssessmentResult.class));
            rtnMap.put("code", ar.getResult());
            if (ar.getType() == 1) {// 患病风险评估
                switch (ar.getResult()) {
                    case 0:// 正常
                        rtnMap.put("message", "您的评估结果无糖尿病风险，请继续保持");
                        break;
                    case 1:// 高危
                        rtnMap.put("message", "您属于糖尿病高危风险人群，请到医院进一步确诊");
                        break;
                }
            } else if (ar.getType() == 2) {// 肾病症状评估
                switch (ar.getResult()) {
                    case 0:// 正常
                        rtnMap.put("message", "您目前尚未出现肾脏病变症状，继续保持");
                        break;
                    case 1:// 满足1-2项
                        rtnMap.put("message", "您目前存在一些类似糖尿病肾脏病变的症状或危险因素，请您在日常的生活中多多注意");
                        break;
                    case 2:// 满足3项及以上
                        rtnMap.put("message", "您很有可能已经患有糖尿病肾病了，请及时咨询医生获得专业建议");
                        break;
                }
            } else if (ar.getType() == 3) {// 眼病症状评估
                switch (ar.getResult()) {
                    case 0:// 正常
                        rtnMap.put("message", "恭喜您，暂未出现糖尿病眼病的症状，请您继续保持");
                        break;
                    case 1:// 出现症状
                        rtnMap.put("message", "您目前出现一部分糖尿病眼部症状，请及时咨询医生获得专业建议");
                        break;
                }
            } else if (ar.getType() == 4) {// 足部风险评估
                switch (ar.getResult()) {
                    case 0:// 正常
                        rtnMap.put("message", "您不属于糖尿病足的高危人群，恭喜您，请继续保持");
                        break;
                    case 1:// 轻度
                        rtnMap.put("message", "您属于轻度糖尿病足的高危人群，糖尿病足并非微不“足”道，请及时咨询医生获得专业建议");
                        break;
                    case 2:// 属于高度
                        rtnMap.put("message", "您属于高度糖尿病足的高危人群，糖尿病足并非微不“足”道，请及时咨询医生获得专业意见");
                        break;
                }
            }
            return rtnMap;
        } catch (EmptyResultDataAccessException ex) {
            // ignore
        } catch (Exception ex) {
            logger.error(Exceptions.getStackTraceAsString(ex));
        }
        return null;
    }

    @Override
    public List<DiabetesAssessment> getAssessmentList(String registerid, Integer type , Integer pageNum, int pageSize) {
        Pageable pageable  = PageFactory.create(pageNum,pageSize,"createDate:desc");
        Page page = assessmentRepo.getAssessmentList(registerid,type,pageable);
        return page.getContent();
    }

    /**
     * 统计用户每种类型评估数
     * @param registerid
     * @return
     */
    @Override
    public List<Map<String, Object>> getNumByTypeAndRegisterid(String registerid) {
        String sql = String.format(
                "select count(1) as num,type from app_tb_diabetes_assessment " +
                        " where registerid = '%s' and del_flag = 0 GROUP BY registerid,type",registerid);


        return jdbcTemplate.queryForList(sql);
    }



    @Data
    public static class AssessmentResult {
        Integer type;
        Integer result;
    }


}
