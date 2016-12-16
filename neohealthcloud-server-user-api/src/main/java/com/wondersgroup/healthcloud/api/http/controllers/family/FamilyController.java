package com.wondersgroup.healthcloud.api.http.controllers.family;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.wondersgroup.common.http.HttpRequestExecutorManager;
import com.wondersgroup.common.http.builder.RequestBuilder;
import com.wondersgroup.common.http.entity.JsonNodeResponseWrapper;
import com.wondersgroup.common.image.utils.ImagePath;
import com.wondersgroup.healthcloud.api.http.dto.family.FamilyInfoDTO;
import com.wondersgroup.healthcloud.api.http.dto.family.FamilyMemberDTO;
import com.wondersgroup.healthcloud.api.http.dto.family.FamilyMemberDTO.MemberInfo;
import com.wondersgroup.healthcloud.api.http.dto.family.FamilyMemberInfoDTO.Info;
import com.wondersgroup.healthcloud.api.http.dto.family.FamilyMemberInfoDTO.InfoTemplet;
import com.wondersgroup.healthcloud.api.http.dto.family.FamilyMemberInfoDTO;
import com.wondersgroup.healthcloud.api.http.dto.family.FamilyMemberInfoDTO.MemberInfoTemplet;
import com.wondersgroup.healthcloud.api.http.dto.measure.MeasureInfoDTO;
import com.wondersgroup.healthcloud.api.http.dto.measure.SimpleMeasure;
import com.wondersgroup.healthcloud.api.utils.CommonUtils;
import com.wondersgroup.healthcloud.common.converter.gender.GenderConverter;
import com.wondersgroup.healthcloud.common.http.dto.JsonListResponseEntity;
import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.common.http.support.misc.JsonKeyReader;
import com.wondersgroup.healthcloud.common.http.support.session.AccessToken;
import com.wondersgroup.healthcloud.common.http.support.version.VersionRange;
import com.wondersgroup.healthcloud.common.utils.AppUrlH5Utils;
import com.wondersgroup.healthcloud.common.utils.IdGen;
import com.wondersgroup.healthcloud.exceptions.CommonException;
import com.wondersgroup.healthcloud.helper.family.FamilyMemberAccess;
import com.wondersgroup.healthcloud.helper.family.FamilyMemberRelation;
import com.wondersgroup.healthcloud.jpa.entity.identify.HealthQuestion;
import com.wondersgroup.healthcloud.jpa.entity.user.AnonymousAccount;
import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;
import com.wondersgroup.healthcloud.jpa.entity.user.member.FamilyMember;
import com.wondersgroup.healthcloud.jpa.entity.user.member.FamilyMemberInvitation;
import com.wondersgroup.healthcloud.jpa.repository.user.AnonymousAccountRepository;
import com.wondersgroup.healthcloud.jpa.repository.user.RegisterInfoRepository;
import com.wondersgroup.healthcloud.jpa.repository.user.member.FamilyMemberInvitationRepository;
import com.wondersgroup.healthcloud.jpa.repository.user.member.FamilyMemberRepository;
import com.wondersgroup.healthcloud.services.assessment.AssessmentService;
import com.wondersgroup.healthcloud.services.identify.PhysicalIdentifyService;
import com.wondersgroup.healthcloud.services.step.StepCountService;
import com.wondersgroup.healthcloud.services.user.AnonymousAccountService;
import com.wondersgroup.healthcloud.services.user.FamilyService;
import com.wondersgroup.healthcloud.services.user.UserAccountService;
import com.wondersgroup.healthcloud.services.user.UserService;
import com.wondersgroup.healthcloud.services.user.dto.Session;
import com.wondersgroup.healthcloud.services.user.dto.member.FamilyMemberAPIEntity;
import com.wondersgroup.healthcloud.services.user.dto.member.FamilyMemberInvitationAPIEntity;
import com.wondersgroup.healthcloud.services.user.exception.ErrorChangeMobileException;
import com.wondersgroup.healthcloud.services.user.exception.ErrorChildVerificationException;
import com.wondersgroup.healthcloud.utils.IdcardUtils;

/**
 * 孫海迪
 * Created by sunhaidi on 2016.8.10
 */

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    private static final Logger logger = LoggerFactory.getLogger(FamilyController.class);
    @Autowired
    private UserAccountService      accountService;
    @Autowired
    private UserService             userService;
    @Autowired
    private RegisterInfoRepository registerInfoRepository;
    @Autowired
    private FamilyService           familyService;
    @Autowired
    private FamilyMemberInvitationRepository invitationRepository;
    @Autowired
    private FamilyMemberRepository familyMemberRepository;
    @Autowired
    private AnonymousAccountService anonymousAccountService;
    @Autowired
    private AnonymousAccountRepository anonymousAccountRepository;
    @Autowired
    private PhysicalIdentifyService physicalIdentifyService;
    @Autowired
    private AssessmentService assessmentService;
    @Autowired
    StepCountService stepCountService;
    @Autowired
    private Environment environment;
    @Autowired
    private AppUrlH5Utils h5Utils;
    RestTemplate restTemplate = new RestTemplate();
    @Value("${internal.api.service.measure.url}")
    private String host;
    private static final String requestAbnormalHistories = "%s/api/measure/3.0/historyMeasureAbnormal?%s";
    private static final String requestHistoryMeasureNew = "%s/api/measure/3.0/historyMeasureNew?%s";
    
    /**
     * 申请添加为亲情账户
     * @param request
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/member", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<String> inviteMember(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String id = reader.readString("uid", false);
        String mobile = reader.readString("mobile", true);
        String memberId = reader.readString("member_id", true);
        String relation = reader.readString("relation", false);
        if(Integer.parseInt(relation) > 38){
            relation = "0";
        }
        String relationName = reader.readString("relation_name", true);
        String memo = StringUtils.defaultString(reader.readString("memo", true));
        Boolean recordReadable = reader.readDefaultBoolean("record_readable", false);

        JsonResponseEntity<String> body = new JsonResponseEntity<>();

        if (mobile == null && memberId == null) {
            body.setCode(1000);
            body.setMsg("[mobile]与[member_id]字段不能都为空");
            return body;
        }

        if (mobile != null && mobile.length() != 11) {
            body.setCode(1000);
            body.setMsg("请输入11位的手机号");
            return body;
        }

        if (memberId == null) {
            JsonResponseEntity<Map<String, String>> info = tinyInfo(mobile);
            if (info.getCode() != 0) {
                body.setCode(1004);
                body.setMsg("无相关账户");
                return body;
            }
        }
        familyService.inviteMember(id, memberId, mobile, memo, relation, relationName, recordReadable);
        body.setMsg("申请提交成功");
        return body;
    }

    /**
     * 账户简讯查询
     * @param mobile
     * @return JsonResponseEntity<Map<String,String>>
     */
    @RequestMapping(value = "/member/info", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<Map<String, String>> tinyInfo(@RequestParam String mobile) {
        JsonResponseEntity<Map<String, String>> body = new JsonResponseEntity<>();
        Map<String, String> data = Maps.newHashMap();
        try {
            RegisterInfo register = userService.findRegisterInfoByMobile(mobile);
            if (register.getHeadphoto() != null) {
                data.put("avatar", register.getHeadphoto() + ImagePath.avatarPostfix());
            }
            data.put("nickname", register.getNickname());
        } catch (Exception e) {
            if (accountService.checkAccount(mobile)) {
                RegisterInfo register = accountService.fetchInfo(mobile);
                data.put("avatar", register.getHeadphoto() + ImagePath.avatarPostfix());
                data.put("nickname", register.getNickname());
            } else {
                throw new ErrorChangeMobileException(1004, "无相关账户");
            }
        }
        body.setData(data);
        return body;
    }

    /**
     * 帮助对方注册
     * @param uid
     * @param mobile
     * @param verifyCode
     * @param password
     * @param relation
     * @param relationName
     * @param recordReadable
     * @param avatar
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/member/registration", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<String> helpRegistration(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String uid = reader.readString("uid", false);
        String mobile = reader.readString("mobile", false);
        String verifyCode = reader.readString("verify_code", false);
        String password = reader.readString("password", false);
        String relation = reader.readString("relation", false);
        String relationName = reader.readString("relation_name", true);
        String path = reader.readString("avatar", true);
        Boolean recordReadable = reader.readDefaultBoolean("record_readable", true);
        familyService.helpRegistration(uid, mobile, password, verifyCode, path, null, relation, relationName,
                recordReadable);
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        body.setMsg("注册成功, 并已添加到亲情账户");
        return body;
    }

    /**
     * 发送注册短信验证码
     * @param uid
     * @param mobile
     * @param relation
     * @param relationName
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/member/registration/code", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<String> registrationCode(
            @RequestHeader(name = "main-area",defaultValue = "") String area, 
            @RequestParam String uid, 
            @RequestParam String mobile,
            @RequestParam String relation, 
            @RequestParam("relation_name") String relationName) {
        familyService.sendRegistrationCode(uid, relation, relationName, mobile,area);
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        body.setMsg("发送成功");
        return body;
    }

    /**
     * 申请列表
     * @param uid
     * @return JsonListResponseEntity<FamilyMemberInvitationAPIEntity>
     */
    @RequestMapping(value = "/member/todo", method = RequestMethod.GET)
    @VersionRange
    public JsonListResponseEntity<FamilyMemberInvitationAPIEntity> invitationList(@RequestParam String uid) {
        List<FamilyMemberInvitation> invitations = familyService.getInvitationList(uid);
        List<FamilyMemberInvitationAPIEntity> data = Lists.newLinkedList();
        for (FamilyMemberInvitation invitation : invitations) {
            RegisterInfo register = uid.equals(invitation.getUid()) ? userService.getOneNotNull(invitation
                    .getMemberId()) : userService.getOneNotNull(invitation.getUid());
            FamilyMember member = familyService.getFamilyMemberWithOrder(uid, invitation.getMemberId());
            int isAnonymous = null == member ? 0 : member.getIsAnonymous();
            if (uid.equals(invitation.getUid()) && null != member && null != member.getMemo()) {
                invitation.setRelationName(member.getMemo());
            }
            FamilyMemberInvitationAPIEntity entity = new FamilyMemberInvitationAPIEntity(invitation, uid, register,
                    isAnonymous);
            data.add(entity);
        }
        JsonListResponseEntity<FamilyMemberInvitationAPIEntity> body = new JsonListResponseEntity<>();
        body.setContent(data, false, null, null);
        return body;
    }

    /**
     * 同意/拒绝
     * @param request
     * @param token
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/member/invitation", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<String> agreeInvitation(@RequestBody String request, @AccessToken Session session) {
        String uid = session.getUserId();
        JsonKeyReader reader = new JsonKeyReader(request);
        //        String uid = reader.readString("uid", false);
        String id = reader.readString("invitation_id", false);
        Boolean agree = reader.readBoolean("agree", false);
        String relationName = reader.readString("relation_name", true);
        Boolean recordReadable = reader.readDefaultBoolean("record_readable", true);

        familyService.dealWithMemberInvitation(id, uid, agree, relationName, recordReadable);
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        body.setMsg(agree ? "添加成功" : "拒绝成功");
        return body;
    }

    /**
     * 成员列表
     * @param uid
     * @param memberId
     * @return JsonListResponseEntity<FamilyMemberAPIEntity>
     */
    @RequestMapping(value = "/member", method = RequestMethod.GET)
    @VersionRange
    public JsonListResponseEntity<FamilyMemberAPIEntity> memberList(
            @RequestParam String uid,
            @RequestParam(value = "member_id", required = false) String memberId,
            @RequestHeader(value = "app-version") String appVersion
            ) {
        Boolean hasUpdate = CommonUtils.compareVersion(appVersion, "3.1");
        List<FamilyMember> familyMembers;
        if (memberId == null) {
            familyMembers = familyService.getFamilyMembers(uid);
        } else {
            FamilyMember familyMember = familyService.getFamilyMemberWithOrder(uid, memberId);
            if (familyMember == null) {
                familyMembers = new ArrayList<>(0);
            } else {
                familyMembers = Lists.newArrayList(familyMember);
            }
        }
        if(hasUpdate){//小于3.1版本 去掉匿名账户
            familyMembers = deleteAnonymousByVersion(familyMembers);
        }
        List<FamilyMemberAPIEntity> data = Lists.newLinkedList();
        for (FamilyMember familyMember : familyMembers) {
            FamilyMemberAPIEntity entity;
            if (familyMember.getIsAnonymous() == 0) {
                RegisterInfo register = userService.getOneNotNull(familyMember.getMemberId());
                entity = new FamilyMemberAPIEntity(familyMember, register);
                FamilyMember familyMemb = familyService.getFamilyMemberWithOrder(familyMember.getMemberId(),
                        familyMember.getUid());
                entity.setRecordReadable(FamilyMemberAccess.recordReadable(familyMemb.getAccess()));
                entity.setRedirectFlag(0);
                entity.setHealthWarning(false);
            } else {
                AnonymousAccount anonymousAccount = anonymousAccountService.getAnonymousAccount(
                        familyMember.getMemberId(), false);
                entity = new FamilyMemberAPIEntity(familyMember, anonymousAccount);
                entity.setRecordReadable(true);
                entity.setRedirectFlag(4);
//                if(anonymousAccount.getBirthDate()){
//                    
//                }
                JsonNode submitInfo = accountService.verficationSubmitInfo(anonymousAccount.getId(), true);
                if(submitInfo != null){
                    Integer status = submitInfo.get("status").asInt();//1 成功 2 审核中 3失败
                    String  name =  submitInfo.get("name").asText();
                    String  idcard = submitInfo.get("idcard").asText();
                    if(status == 1){ 
                        status = 3;
                    }else if(status == 3){
                        status = 4;
                    }
                    if (!anonymousAccount.getIsChild() && status == 2){
                        status = 1;
                    }
                    entity.setRedirectFlag(status);
                    entity.setName(IdcardUtils.cardNameYard(name));
                    entity.setIdCard(IdcardUtils.cardYard(idcard));
                }
            }
            entity.setLabelColor("#666666");
            entity.setHealthWarning(false);
            switch (entity.getRedirectFlag()) {
            case 0:
               if (haveMeasureException(entity.getUid(), entity.getIdCard(), entity.getGender(), new DateTime(new Date()).plusMonths(-6).toString("yyyy-MM-dd"), 1)) {
                    entity.setLabel("最近有异常指标");
                    entity.setHealthWarning(true);
                    entity.setLabelColor("#CC0000");
                } else {
                    entity.setLabel("最近无异常指标");
                }
                break;
            case 1:
                entity.setLabel("申请身份核实中");
                break;
            case 2:
                entity.setLabel("实名制认证审核中");
                break;
            case 3:
                entity.setLabel("已通过实名制认证");
                break;
            case 4:
                entity.setLabel("实名制认证失败");
                entity.setLabelColor("#CC0000");
                break;
            }

            data.add(entity);
        }
        Integer todo = familyService.countTodo(uid);
        JsonListResponseEntity<FamilyMemberAPIEntity> body = new JsonListResponseEntity<>();
        body.setContent(data, false, null, null);
        if (memberId == null) {
            body.addExtra("invitation_count", String.valueOf(todo));
            body.addExtra("empty",
                    String.valueOf(familyMembers.size() == 0 && todo == 0 && familyService.countSent(uid) == 0));
        }
        return body;
    }

    /**
     * 解除情亲账户关系
     * @param uid
     * @param memberId
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/member", method = RequestMethod.DELETE)
    @VersionRange
    public JsonResponseEntity<String> unbindRelation(@RequestParam String uid,
            @RequestParam("member_id") String memberId) {
        familyService.unbindFamilyRelation(uid, memberId);
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        body.setMsg("解除成功");
        
        return body;
    }

    /**
     * 实名认证，对方无手机号 点此为其认证
     * @param uid
     * @param relation
     * @param relationName
     * @param name
     * @param idcard
     * @param photo
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/member/registration/anonym", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<String> anonymousRegistration(
            @RequestBody String request) {
        
        JsonKeyReader reader = new JsonKeyReader(request);
        String uid = reader.readString("uid", false);
        String relation = reader.readString("relation", false);
        String relationName = reader.readString("relation_name",true);
        String name = reader.readString("name", false);
        String idcard = reader.readString("idcard", false).toUpperCase();
        String photo = reader.readString("photo", false);
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        familyService.anonymousRegistration(uid, relation, relationName, name, idcard, photo);
        body.setMsg("添加成功, 正在进行实名认证");
        return body;
    }

    /**
     * 修改备注名
     * @param request
     * @return JsonResponseEntity<Map<String,String>>
     */
    @RequestMapping(value = "/member/memo", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<Map<String, String>> updateMemo(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String uid = reader.readString("uid", false);
        String memberId = reader.readString("member_id", false);
        String memo = reader.readDefaultString("memo", "");

        JsonResponseEntity<Map<String, String>> body = new JsonResponseEntity<>();
        Map<String, String> data = ImmutableMap.of("memo", memo);
        FamilyMember familyMember = familyService.getFamilyMemberWithOrder(uid, memberId);
        if(familyMember == null || familyMember.getId() == null){
            body.setCode(1695);
            body.setData(data);
            body.setMsg("用户已解绑，修改失败");
            return body;
        }
        familyService.updateMemoName(uid, memberId, memo);
        body.setData(data);
        body.setMsg("修改成功");
        return body;
    }

    /**
     * 档案阅读权限开关
     * @param request
     * @return JsonResponseEntity<Map<String,Boolean>>
     */
    @RequestMapping(value = "/member/access/record", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<Map<String, Boolean>> updateRecordReadAccess(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String uid = reader.readString("uid", false);
        String memberId = reader.readString("member_id", false);
        Boolean recordReadable = reader.readDefaultBoolean("record_readable", false);

        familyService.switchRecordReadAccess(uid, memberId, recordReadable);
        JsonResponseEntity<Map<String, Boolean>> body = new JsonResponseEntity<>();
        Map<String, Boolean> data = ImmutableMap.of("record_readable", recordReadable);
        body.setData(data);
        body.setMsg("修改成功");
        return body;
    }

    /**
     * 是否有异常指标
     * @param registerId
     * @param date
     * @param isNew  1 最近是否有，2 是否有新的
     * @return boolean
     */
    public boolean haveMeasureException(String registerId, String personCard, String sex, String date, int isNew) {
        boolean res = false;
        try {
            String url = environment.getProperty("internal.api.service.measure.url");
            url += isNew == 1 ? "/api/measure/abnormal/afterDate" : "/api/measure/abnormal/byDate";
            String[] header = new String[] { "version", "3.0" };
            String[] form = new String[] { "registerId", registerId, "date", date, "personCard", personCard, "sex", sex};
            OkHttpClient client = new OkHttpClient();
            HttpRequestExecutorManager httpRequestExecutorManager = new HttpRequestExecutorManager(client);
            Request request = new RequestBuilder().get().url(url).params(form).headers(header).build();
            JsonNodeResponseWrapper response = (JsonNodeResponseWrapper) httpRequestExecutorManager.newCall(request).run()
                    .as(JsonNodeResponseWrapper.class);
            JsonNode result = response.convertBody();

            if (result.get("code").asInt() == 0 && result.get("data").get("hashAbnormal").asBoolean()) {
                res = true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        return res;
    }
    
    /**
     * 提交儿童实名认证信息
     * @return
     */
    @VersionRange
    @PostMapping(path = "/childVerification/submit")
    public JsonResponseEntity<String> verificationSubmit(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String id = reader.readString("uid", false);//监护人Id
        String name = reader.readString("name", false);//儿童的真实姓名
        String idCard = reader.readString("idcard", false);//儿童的身份证号
        String idCardFile = reader.readString("idCardFile", false);//户口本(儿童身份信息页照片)
        String birthCertFile = reader.readString("birthCertFile", false);//出生证明(照片)
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        name = name.trim();
        idCard = idCard.trim().toUpperCase();
        int age = IdcardUtils.getAgeByIdCard(idCard);
        if(!containsChinese(name)){
            throw new ErrorChildVerificationException("姓名必须是中文");
        }
        if(age >= 18){
            throw new ErrorChildVerificationException("年龄大于等于18岁的不能使用儿童实名认证");
        }
        
        Boolean result = familyService.childVerificationRegistration(id, name, idCard, idCardFile, birthCertFile);
        if(!result){
            body.setCode(1001);
            body.setMsg("提交失败");
            return body;
        }
        body.setMsg("实名认证已提交，请耐心等待");
        return body;
    }
    
    /**
     * 亲情账户儿童实名认证是否打开
     * @return JsonResponseEntity<String>
     */
    @VersionRange
    @GetMapping(path = "/isOpenVerification")
    public JsonResponseEntity<Map<String, String>> openVerification(@RequestParam() String uid){
        RegisterInfo register = userService.getOneNotNull(uid);
        JsonResponseEntity<Map<String, String>> result = new JsonResponseEntity<Map<String, String>>();
        Map<String, String> map = new HashMap<String, String>();
        String identifytype = register.getIdentifytype();
        map.put("identifyType", identifytype);
        result.setData(map);
        result.setMsg("查询成功");
        return result;
    }

    private boolean containsChinese(String s){
       if (null == s || "".equals(s.trim())) return false;
       for (int i = 0; i < s.length(); i++) {
         int v = s.charAt(i);
         if(!(v >=19968 && v <= 171941)){
             return false;
         }
       }
       return true;
   }

    public List<FamilyMember> deleteAnonymousByVersion(List<FamilyMember> familyMembers){
        List<FamilyMember> list = new ArrayList<FamilyMember>();
        for (FamilyMember familyMember : familyMembers) {
            if(familyMember.getIsAnonymous().intValue() == 0){
                list.add(familyMember);
            }
        }
        return list;
    }
    
    /**
     * 查看手机号是否注册过健康云
     * @param mobile
     * @return Object
     */
    @RequestMapping("/isExist")
    @VersionRange
    public Object isVerification(@RequestParam String mobile){
        JsonResponseEntity<Map<String, Object>> result = new JsonResponseEntity<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        Boolean exist = accountService.checkAccount(mobile);
        map.put("isExist", exist);
        if(exist){
            RegisterInfo info = userService.findRegisterInfoByMobile(mobile);
            if(info != null){
                map.put("avatar", info.getHeadphoto());
            }
        }
        result.setData(map);
        result.setMsg("查询成功");
        return result; 
    }
    
    /**
     * 添加非健康云为亲情账户
     * @param request
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/addMember", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<String> addMember(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String id = reader.readString("uid", false);
        String mobile = reader.readString("mobile", true);
        String relation = reader.readString("relation", false);
        if(Integer.parseInt(relation) > 38){
            relation = "0";
        }
        String relationName = reader.readString("relation_name", true);
        String memo = StringUtils.defaultString(reader.readString("memo", true));
        Boolean recordReadable = reader.readDefaultBoolean("record_readable", true);
        String birthDate = reader.readString("birthDate", false);
        String headphoto = reader.readString("headphoto", true);
        
        JsonResponseEntity<String> body = new JsonResponseEntity<>();

        if (mobile != null && mobile.length() != 11) {
            body.setCode(1000);
            body.setMsg("请输入11位的手机号");
            return body;
        }
        Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(birthDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        familyService.anonymousRegistration(id, relation, relationName, null, headphoto, mobile,date , false);
        body.setMsg("添加成功");
        return body;
    }
    
    /**
     * 家庭首页接口
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/memberTop", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<FamilyMemberDTO> memberTop(@RequestParam String uid){
        JsonResponseEntity<FamilyMemberDTO> response = new JsonResponseEntity<FamilyMemberDTO>();
        FamilyMemberDTO dto = new FamilyMemberDTO();
        dto.setInvitsations(new ArrayList<FamilyMemberInvitationAPIEntity>());
        
        List<FamilyMemberInvitation> invitations = invitationRepository.invitationList(uid,3);
        for (FamilyMemberInvitation invitation : invitations) {
            dto.getInvitsations().add(new FamilyMemberInvitationAPIEntity(invitation, uid));
        }
        
        dto.setMemberInfos(new ArrayList<FamilyMemberDTO.MemberInfo>());
        Map<String, String> memberMap = getFamilyMemberByUid(uid);
       
        for (String regId : memberMap.keySet()) {
            RegisterInfo reg = registerInfoRepository.findOne(regId);
            MemberInfo info = new MemberInfo(getRelationName(memberMap.get(regId)), null, historyMeasureAbnormal(regId));
            info.setId(regId);
            info.setAvatar(reg == null ? null : reg.getHeadphoto());
            if(info.getMeasures() == null || info.getMeasures().isEmpty()){
                continue;   
            }
            dto.getMemberInfos().add(info);
            if(dto.getMemberInfos().size() >= 3){
                break;
            }
        }
        response.setData(dto);
        response.setMsg("查询成功");
        return response;
    }
    
    /**
     * 家庭首页个人信息
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/memberInfo", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<FamilyMemberInfoDTO>  memberInfo(@RequestParam String uid, @RequestParam String memberId){
        JsonResponseEntity<FamilyMemberInfoDTO> response = new JsonResponseEntity<FamilyMemberInfoDTO>();
        List<InfoTemplet>  tems = new ArrayList<FamilyMemberInfoDTO.InfoTemplet>();
        FamilyMemberInfoDTO infoDto = new FamilyMemberInfoDTO();
        String registerId = null;
        String sex = null;
        Info info = new Info();
        info.setIsStandalone(false);
        info.setIsVerification(false);
        RegisterInfo regInfo = userService.findOne(memberId);
        FamilyMember familyMember = familyService.getFamilyMemberWithOrder(uid, memberId);
        if(regInfo == null){
            AnonymousAccount ano = anonymousAccountRepository.findOne(memberId);
            if(ano != null && ano.getIsStandalone()){
                info.setIsStandalone(true);
            }
            info.setNikcName(ano.getNickname());
            info.setAge(getAge(ano.getBirthDate()));
            info.setMobile(ano.getMobile());
            registerId = ano.getId();
            sex = ano.getSex();
        }else{
            registerId = regInfo.getRegisterid();
            sex = regInfo.getGender();
            info.setIsVerification(regInfo.verified());
            info.setNikcName(regInfo.getNickname());
            info.setAge(getAge(regInfo.getBirthday()));
            info.setMobile(regInfo.getRegmobilephone());
        }
        info.setId(memberId);
        if(uid.equals(memberId)){
            info.setAccess(true);
            info.setNikcName("我");
        }else{
            info.setAccess(FamilyMemberAccess.recordReadable(familyMember.getAccess()));
            info.setRelation_name(FamilyMemberRelation.getName(familyMember.getRelation()));
        }       
        List<SimpleMeasure> measures = historyMeasureNew(registerId, sex);
        for (Integer id : MemberInfoTemplet.map.keySet()) {
            InfoTemplet templet = new InfoTemplet(id, MemberInfoTemplet.map.get(id), null);
//            if(id == 1 && !info.getIsVerification()){
//                templet.setValues(Arrays.asList(new MeasureInfoDTO("", getDateStr(), null)));
//            }else 
            if(id == 2){
                templet.setDesc("就医历史 一查便知");
            }
            if(info.getAge() != null && info.getAge() < 6){
                templet.setType(10);
                templet.setDesc("家有宝贝初养成");
                templet.setTitle("N天后可接种疫苗");
                break;
            }
            if(id == 3){
                
            }else if(id == 4){
                JsonNode node = stepCountService.findStepByUserIdAndDate(memberId, new Date());
                if(node == null && node.get("stepCount") != null){
                    templet.setValues(Arrays.asList(new MeasureInfoDTO("今日", getDateStr(), node.get("stepCount").textValue() + "步")));
                }else{
                    templet.setDesc("健康计步，领取金币");
                }
            }else if(id == 5){
                List<MeasureInfoDTO> m = getMeasure(measures, id);
                if(!m.isEmpty()){
                    templet.setValues(m);
                }else{
                    templet.setDesc("您的BMI指数是多少?");
                }
                
            }else if(id == 6){
                List<MeasureInfoDTO> m = getMeasure(measures, id);
                if(!m.isEmpty()){
                    templet.setValues(m);
                }else{
                    templet.setDesc("开启科学控压之路");
                }
            }else if(id == 7){
                List<MeasureInfoDTO> m = getMeasure(measures, id);
                if(!m.isEmpty()){
                    templet.setValues(m);
                }else{
                    templet.setDesc("开启科学控糖之路");
                }
            }else if(id == 8){
                Map<String,Object> result = assessmentService.getRecentAssessIsNormal(memberId);
                if(result != null && result.containsKey("state")){
                    String date = result.get("date").toString();
                    Boolean state = Boolean.valueOf(result.get("state").toString());
                    templet.setValues(Arrays.asList(new MeasureInfoDTO("评估结果",date , state ? "正常人群" : "风险人群")));
                }
            }else if(id == 9){
                HealthQuestion result = physicalIdentifyService.getRecentPhysicalIdentify(memberId);
                if(result != null){
                    String date = new SimpleDateFormat("yyyy-MM-dd").format(result.getTesttime());
                    templet.setValues(Arrays.asList(new MeasureInfoDTO(null,date , result.getResult())));
                }
            }
            tems.add(templet);
        }
        infoDto.setInfo(info);
        infoDto.setInfoTemplets(tems);
        response.setData(infoDto);
        response.setMsg("查询成功");
        return response;
    }
    
    public List<MeasureInfoDTO> getMeasure(List<SimpleMeasure> measures, int type){
        List<MeasureInfoDTO> list = new ArrayList<MeasureInfoDTO>();
        for (SimpleMeasure measure : measures) {
            MeasureInfoDTO info = new MeasureInfoDTO();
            if(type == 5 && measure.getType() == 0){
                info.setValue(measure.getValue());
                info.setFlag(measure.getFlag());
                info.setDate(measure.getTestTime());
            }else if(type == 6 && measure.getType() == 3){
                info.setName(measure.getName());
                info.setValue(measure.getValue());
                info.setFlag(measure.getFlag());
                info.setDate(measure.getTestTime());
            }else if(type == 7 && measure.getType() == 2){
                info.setName(measure.getName());
                info.setValue(measure.getValue());
                info.setFlag(measure.getFlag());
                info.setDate(measure.getTestTime());
            }
            if(info.getValue() != null){
                list.add(info);
            }
        }
        return list;
    }
    
    /**
     * 家人信息
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/familyInfo", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<FamilyInfoDTO>  familyInfo(@RequestParam String uid, @RequestParam String memberId){
        JsonResponseEntity<FamilyInfoDTO> response = new JsonResponseEntity<FamilyInfoDTO>();
        FamilyInfoDTO info = new FamilyInfoDTO();
        info.setIsStandalone(false);
        RegisterInfo regInfo = userService.findOne(memberId);
        FamilyMember familyMember = familyService.getFamilyMemberWithOrder(uid, memberId);
        if(familyMember == null){
            throw new CommonException(1000, "不是您的家庭成员");
        }
        if(regInfo == null){
            AnonymousAccount ano = anonymousAccountRepository.findOne(memberId);
            if(ano != null && ano.getIsStandalone()){
                info.setIsStandalone(true);
            }
            info.setId(ano.getId());
            info.setNickname(ano.getNickname());
            info.setMobile(ano.getMobile());
            info.setSex(GenderConverter.toChinese(ano.getSex()));
            info.setAvatar(ano.getHeadphoto());
            info.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").format(ano.getBirthDate()));
        }else{
            info.setSex(GenderConverter.toChinese(regInfo.getGender()));
            info.setId(regInfo.getRegisterid());
            info.setIsVerification(regInfo.verified());
            info.setNickname(regInfo.getNickname());
            info.setMobile(regInfo.getRegmobilephone());
            info.setAvatar(regInfo.getHeadphoto());
            info.setAge(getAge(regInfo.getBirthday()));
            info.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").format(regInfo.getBirthday()));
        }
        info.setRelation_name(FamilyMemberRelation.getName(familyMember.getRelation()));
        info.setIsAccess(FamilyMemberAccess.recordReadable(familyMember.getAccess()));
        response.setData(info);
        response.setMsg("查询成功");
        return response;
    }
    
    /**
     * 家庭首页人员排序
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/memberOrder", method = RequestMethod.GET)
    @VersionRange
    public JsonListResponseEntity<FamilyMemberInvitationAPIEntity> memberOrder(@RequestParam String uid){
        JsonListResponseEntity<FamilyMemberInvitationAPIEntity> response = new JsonListResponseEntity<FamilyMemberInvitationAPIEntity>();
        List<FamilyMember> familyMembers = familyService.getFamilyMembers(uid);
        List<FamilyMemberInvitationAPIEntity> list = new ArrayList<FamilyMemberInvitationAPIEntity>();
        for (FamilyMember familyMember : familyMembers) {
            RegisterInfo info =  registerInfoRepository.findByRegisterid(familyMember.getMemberId());
            FamilyMemberInvitationAPIEntity entity = new FamilyMemberInvitationAPIEntity();
            entity.setId(familyMember.getMemberId());
            entity.setAvatar((info != null && info.getRegisterid() != null) ? info.getHeadphoto() : null);
            entity.setRelationName(FamilyMemberRelation.getName(familyMember.getRelation()));
            entity.setIsStandalone(false);
            if(info == null){
                AnonymousAccount ano = anonymousAccountRepository.findOne(familyMember.getMemberId());
                if(ano != null && ano.getIsStandalone()){
                    entity.setIsStandalone(true);
                }
                entity.setAvatar(ano.getHeadphoto());
            }
            list.add(entity);
        }
        response.setContent(list);
        response.setMsg("查询成功");
        return response;
    }
    
    /**
     * 家庭首页人员排序修改
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/memberOrderUpdate", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<String> memberOrderUpdate(@RequestParam String uid, @RequestParam String orderUids){
        JsonResponseEntity<String> response = new JsonResponseEntity<String>();
        if(!StringUtils.isBlank(orderUids)){
            String[] orderUid = orderUids.split(",");
            for (int i = 0; i < orderUid.length; i++) {
                String id = orderUid[i];
                if(!StringUtils.isBlank(id)){
                    familyMemberRepository.updateOrder(uid, id, i);
                }
            }
        }
        response.setMsg("修改成功");
        return response;
    }
    
    /**
     * 查看非JKY家人信息
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/memberFamilyInfo", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<AnonymousAccount> memberFamilyInfo(@RequestParam String uid, @RequestParam String memberId){
        JsonResponseEntity<AnonymousAccount> response = new JsonResponseEntity<AnonymousAccount>();
        AnonymousAccount ano =  anonymousAccountRepository.findOne(memberId);
        response.setData(ano);
        response.setMsg("查询成功");
        return response;
    }
    
    /**
     * 修改非JKY家人信息
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/memberFamilyInfoUpdate", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<String> memberFamilyInfoUpdate(@RequestBody String body){
        JsonResponseEntity<String> response = new JsonResponseEntity<String>();
        JsonKeyReader reader = new JsonKeyReader(body);
        String id = reader.readString("uid", false);
        String memberId = reader.readString("memberId", false);
        String relation = reader.readString("relation", true);
        String mobile = reader.readString("mobile", true);
        String appellation = reader.readString("appellation", true);
        String height = reader.readString("height", true);
        String weight = reader.readString("weight", true);
        String birthDate = reader.readString("birthDate", true);
        String sex = reader.readString("sex", true);
        String nickname = reader.readString("nickname", true);
        String avatar = reader.readString("avatar", true);
        
        AnonymousAccount ano =  anonymousAccountRepository.findOne(memberId);
        if(!StringUtils.isBlank(mobile)){
            ano.setMobile(mobile);
        }
        if(!StringUtils.isBlank(appellation)){
            ano.setAppellation(appellation);
        }
        if(!StringUtils.isBlank(height)){
            ano.setHeight(height);
        }
        if(!StringUtils.isBlank(mobile)){
            ano.setMobile(mobile);
        }
        if(!StringUtils.isBlank(weight)){
            ano.setWeight(weight);
        }
        if(!StringUtils.isBlank(avatar)){
            ano.setHeadphoto(avatar);
        }
        if(!StringUtils.isBlank(birthDate)){
            try {
                ano.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse(birthDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if(!StringUtils.isBlank(sex)){
            ano.setSex(sex);
        }
        if(!StringUtils.isBlank(nickname)){
            ano.setNickname(nickname);
        }
        anonymousAccountRepository.saveAndFlush(ano);
        
        if(!StringUtils.isBlank(relation)){
            FamilyMember memb = familyService.getFamilyMemberWithOrder(id, memberId);
            if(memb != null){
                memb.setRelation(relation);
                memb.setRelationName(FamilyMemberRelation.getName(relation));
                familyMemberRepository.saveAndFlush(memb);
            }
        }
        
        response.setMsg("修改成功");
        return response;
    }
    
    /**
     * 咨询Ta-发送家庭消息
     * @param uid
     * @return Object
     */
    @RequestMapping(value = "/memberSendMessage", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<String> memberSendMessage(@RequestParam String uid, @RequestParam String memberId, @RequestParam int type){
        JsonResponseEntity<String> response = new JsonResponseEntity<String>();
        
        boolean result = familyService.pushMessage(uid, memberId, type);
        if(result ){
            response.setMsg("发送成功");
        }else{
            response.setCode(1001);
            response.setMsg("发送失败");
        }
        return response;
    }
    
    /**
     * 所有家庭关系
     */
    @RequestMapping(value = "/memberFooting", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<Object> memberFooting(){
        JsonResponseEntity<Object> response = new JsonResponseEntity<Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("memberFooting", FamilyMemberRelation.getMemberFooting());
        response.setData(map);
        response.setMsg("查询成功");
        return response;
    }
    
    /**
     * 所有家庭关系
     */
    @RequestMapping(value = "/memberFootings", method = RequestMethod.GET)
    @VersionRange
    public JsonResponseEntity<Object> memberFootings(){
        JsonResponseEntity<Object> response = new JsonResponseEntity<Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("memberFooting", FamilyMemberRelation.getMemberFootings());
        response.setData(map);
        response.setMsg("查询成功");
        return response;
    }
    
    /**
     * 单机版实名认证
     * @param request
     * @return JsonResponseEntity<String>
     */
    @RequestMapping(value = "/standaloneVerification", method = RequestMethod.POST)
    @VersionRange
    public JsonResponseEntity<String> standaloneVerificationSubmit(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String uid = reader.readString("uid", false);
        String name = reader.readString("name", false);
        String idcard = reader.readString("idcard", true).toUpperCase();
        String photo = reader.readString("photo", true);
        
        String memberId = reader.readString("memberId", true);
        String idCardFile = reader.readString("idCardFile", true);//户口本(儿童身份信息页照片)
        String birthCertFile = reader.readString("birthCertFile", true);//出生证明(照片)
        
        if(!StringUtils.isBlank(memberId) ){
            accountService.childVerificationSubmit(uid, memberId, name, idcard, idCardFile,
                    birthCertFile);
        }else{
            accountService.verificationSubmit(uid, name, idcard, photo);
        }
        
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        body.setMsg("正在进行实名认证");
        return body;
    }
    
    /**
     * 获取用户异常记录
     * @param registerId
     * @return List<SimpleMeasure>
     */
    public List<SimpleMeasure> historyMeasureAbnormal(String registerId){
        Map<String, Object> result = new HashMap<>();
        String personCard = "";
        String gender = null;
        List<SimpleMeasure> list = new ArrayList<SimpleMeasure>();
        try {
            RegisterInfo info = userService.findOne(registerId);
            if(info == null){
                AnonymousAccount account = anonymousAccountService.getAnonymousAccount(registerId, false);
                personCard = account.getIdcard();
                gender = account.getSex();
            }else{
                personCard = info.getPersoncard();
                gender = info.getGender();
            }
            if(StringUtils.isEmpty(personCard)){
                result.put("h5Url", Collections.EMPTY_MAP );
            }else{
                result.put("h5Url", h5Utils.generateLinks(personCard)) ;
            }
            String param = "registerId=".concat(registerId).concat("&sex=").concat(getGender(gender))
                    .concat("&personCard=").concat(getPersonCard(personCard));
            String url = String.format(requestAbnormalHistories, host, param);
            ResponseEntity<JsonResponseEntity> response = buildGetEntity(url, JsonResponseEntity.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                JsonResponseEntity entity = response.getBody();
                if (entity.getCode() == 0) {
                    List<Map> content = (List<Map>)entity.getData();
                    for (Map map : content) {
                        SimpleMeasure sims = new SimpleMeasure();
                        BeanUtils.populate(sims, map);
                        list.add(sims);
                    }
                    return list;
                }
            }
        } catch (Exception e) {
            logger.info("近期异常数据获取失败", e);
        }
        return list;
    }
    
    /**
     * 获取用户最新一条 bmi 血压 血氧数据
     * @param registerId
     * @return List<SimpleMeasure>
     */
    public List<SimpleMeasure> historyMeasureNew(String registerId, String gender){
        Map<String, Object> result = new HashMap<>();
        String personCard = "";
//        String gender = null;
        List<SimpleMeasure> list = new ArrayList<SimpleMeasure>();
        try {
//            RegisterInfo info = userService.findOne(registerId);
//            if(info == null){
//                AnonymousAccount account = anonymousAccountService.getAnonymousAccount(registerId, false);
//                personCard = account.getIdcard();
//                gender = account.getSex();
//            }else{
//                personCard = info.getPersoncard();
//                gender = info.getGender();
//            }
            if(StringUtils.isEmpty(personCard)){
                result.put("h5Url", Collections.EMPTY_MAP );
            }else{
                result.put("h5Url", h5Utils.generateLinks(personCard)) ;
            }
            String param = "registerId=".concat(registerId).concat("&sex=").concat(getGender(gender))
                    .concat("&personCard=").concat(getPersonCard(personCard));
            String url = String.format(requestHistoryMeasureNew, host, param);
            ResponseEntity<JsonResponseEntity> response = buildGetEntity(url, JsonResponseEntity.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                JsonResponseEntity entity = response.getBody();
                if (entity.getCode() == 0) {
                    List<Map> content = (List<Map>)entity.getData();
                    for (Map map : content) {
                        SimpleMeasure sims = new SimpleMeasure();
                        BeanUtils.populate(sims, map);
                        list.add(sims);
                    }
                    return list;
                }
            }
        } catch (Exception e) {
            logger.info("近期异常数据获取失败", e);
        }
        return list;
    }
    
    public Map<String, String> getFamilyMemberByUid(String uid){
        Map<String, String> map = new TreeMap<String, String>();
        List<FamilyMember> familyMembers = familyService.getFamilyMembers(uid);
        map.put(uid, "-1");
        if(familyMembers != null){
            for (FamilyMember familyMember : familyMembers) {
                map.put(familyMember.getMemberId(), familyMember.getRelation());
            }
        }
        return map;
    }
    
    public String getPersonCard(String personcard){
        return StringUtils.isEmpty(personcard) ? "" : personcard;
    }
    
    public String getGender(String gender){
        return StringUtils.isEmpty(gender) ? "1" : gender;
    }

    private HttpHeaders buildHeader(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String version = request.getHeader("version");
        boolean isStandard =  CommonUtils.compareVersion(version, "3.1");
        HttpHeaders headers = new HttpHeaders();
        headers.add("isStandard", String.valueOf(isStandard));
        return headers;
    }
    
    private <T> ResponseEntity<T> buildGetEntity(String url, Class<T> responseType, Object... urlVariables){
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(buildHeader()), responseType, urlVariables);
    }
    
    public String getRelationName(String relation){
        return "-1".equals(relation) ? "我的" : FamilyMemberRelation.getName(relation, "");
    }
    
    public Integer getAge(Date birthDay) {
        if(birthDay == null){
            return null;
        }
        Calendar cal = Calendar.getInstance();  
      
        if (cal.before(birthDay)) {  
            return 0;  
        }  
        int yearNow = cal.get(Calendar.YEAR);  
        int monthNow = cal.get(Calendar.MONTH);  
        int dayOfMonthNow = cal.get(Calendar.DAY_OF_MONTH);  
        cal.setTime(birthDay);  
      
        int yearBirth = cal.get(Calendar.YEAR);  
        int monthBirth = cal.get(Calendar.MONTH);  
        int dayOfMonthBirth = cal.get(Calendar.DAY_OF_MONTH);  
      
        int age = yearNow - yearBirth;  
      
        if (monthNow <= monthBirth) {
            if (monthNow == monthBirth) {  
                if (dayOfMonthNow < dayOfMonthBirth) {  
                    age--;  
                }  
            } else {  
                age--;  
            }  
        }  
        return age;  
    }  
    
    public String getDateStr(){
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}
