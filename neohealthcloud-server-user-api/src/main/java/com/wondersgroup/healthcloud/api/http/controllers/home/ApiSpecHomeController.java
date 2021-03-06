package com.wondersgroup.healthcloud.api.http.controllers.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wondersgroup.healthcloud.common.http.annotations.WithoutToken;
import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.common.http.support.session.AccessToken;
import com.wondersgroup.healthcloud.common.http.support.version.VersionRange;
import com.wondersgroup.healthcloud.jpa.entity.config.AppConfig;
import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;
import com.wondersgroup.healthcloud.jpa.enums.FamilyHealthStatusEnum;
import com.wondersgroup.healthcloud.jpa.enums.ServiceTypeEnum;
import com.wondersgroup.healthcloud.jpa.enums.UserHealthStatusEnum;
import com.wondersgroup.healthcloud.jpa.repository.user.RegisterInfoRepository;
import com.wondersgroup.healthcloud.services.article.ManageNewsArticleService;
import com.wondersgroup.healthcloud.services.article.dto.NewsArticleListAPIEntity;
import com.wondersgroup.healthcloud.services.bbs.dto.topic.TopicListDto;
import com.wondersgroup.healthcloud.services.config.AppConfigService;
import com.wondersgroup.healthcloud.services.diabetes.DiabetesService;
import com.wondersgroup.healthcloud.services.home.HomeService;
import com.wondersgroup.healthcloud.services.home.dto.advertisements.CenterAdDTO;
import com.wondersgroup.healthcloud.services.home.dto.advertisements.SideAdDTO;
import com.wondersgroup.healthcloud.services.home.dto.cloudTopLine.CloudTopLineDTO;
import com.wondersgroup.healthcloud.services.home.dto.familyHealth.FamilyHealthDTO;
import com.wondersgroup.healthcloud.services.home.dto.familyHealth.FamilyHealthJKGLDTO;
import com.wondersgroup.healthcloud.services.home.dto.familyHealth.FamilyMemberDTO;
import com.wondersgroup.healthcloud.services.home.dto.familyHealth.FamilyMemberJKGLDTO;
import com.wondersgroup.healthcloud.services.home.dto.familyHealth.UserHealthDTO;
import com.wondersgroup.healthcloud.services.home.dto.familyHealth.UserHealthJKGLDTO;
import com.wondersgroup.healthcloud.services.home.dto.functionIcons.FunctionIconsDTO;
import com.wondersgroup.healthcloud.services.home.dto.modulePortal.ModulePortalDTO;
import com.wondersgroup.healthcloud.services.home.dto.specialService.SpecialServiceDTO;
import com.wondersgroup.healthcloud.services.home.impl.TopicManageServiceImpl;
import com.wondersgroup.healthcloud.services.homeservice.dto.HomeServiceDTO;
import com.wondersgroup.healthcloud.services.remind.RemindService;
import com.wondersgroup.healthcloud.services.remind.dto.RemindForHomeDTO;
import com.wondersgroup.healthcloud.services.user.dto.Session;

/**
 * Created by xianglinhai on 2016/12/14.
 */
@RestController
@RequestMapping("/api/spec")
public class ApiSpecHomeController {
    private static final Logger logger = LoggerFactory.getLogger(ApiSpecHomeController.class);
    @Autowired
    private HomeService homeService;

    @Autowired
    private TopicManageServiceImpl topicManageService;

    @Autowired
    RegisterInfoRepository registerInfoRepo;

    @Autowired
    private AppConfigService appConfigService;

    @Autowired
    private ManageNewsArticleService manageNewsArticleServiceImpl;
    
    @Autowired
    private RemindService remindService;

    @Value("${internal.api.service.measure.url}")
    private String API_MEASURE_URL;

    @Value("${internal.api.service.healthrecord.url}")
    private String API_USERHEALTH_RECORD_URL;

    @Value("${api.vaccine.url}")
    private String API_VACCINE_URL;
    
    

    @Autowired
    private DiabetesService diabetesService;

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    @VersionRange(to = "4.0.2")
    @WithoutToken
    public JsonResponseEntity index(@RequestHeader(value = "main-area", required = true) String mainArea,
                                    @RequestHeader(value = "spec-area", required = false) String specArea,
                                    @RequestHeader(value = "app-version", required = true) String version,
                                    @RequestParam(value = "uid", required = false) String uid,
                                    @AccessToken(required = false, guestEnabled = true) Session session) {
        JsonResponseEntity result = new JsonResponseEntity();
        Map data = new HashMap();

        RegisterInfo registerInfo = null;
        if (StringUtils.isNotBlank(uid)) {
            registerInfo = registerInfoRepo.findOne(uid);
        }


        //主要功能区
        List<FunctionIconsDTO> functionIcons = null;
        try {
            functionIcons = homeService.findFunctionIconsDTO(session, version, mainArea, specArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        functionIcons = CollectionUtils.isEmpty(functionIcons) ? new ArrayList<FunctionIconsDTO>(0) : functionIcons;
        data.put("functionIcons", functionIcons);

        //特色服务
        List<SpecialServiceDTO> specialService = null;
        try {
            specialService = homeService.findSpecialServiceDTO(session, version, mainArea, specArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        specialService = CollectionUtils.isEmpty(specialService) ? new ArrayList<SpecialServiceDTO>(0) : specialService;
        data.put("specialService", specialService);

        //中央区广告
        List<CenterAdDTO> advertisements = null;
        try {
            advertisements = homeService.findCenterAdDTO(mainArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        advertisements = CollectionUtils.isEmpty(advertisements) ? new ArrayList<CenterAdDTO>(0) : advertisements;
        data.put("advertisements", advertisements);

        //侧边浮层广告
        SideAdDTO sideAd = null;
        try {
            sideAd = homeService.findSideAdDTO(mainArea, null);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        sideAd = sideAd == null ? new SideAdDTO() : sideAd;
        data.put("sideAd", sideAd);


        FamilyHealthDTO familyHealth = null;

        if (null != registerInfo) {
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put("apiMeasureUrl", API_MEASURE_URL);
            paramMap.put("apiUserhealthRecordUrl", API_USERHEALTH_RECORD_URL);
            paramMap.put("apiVaccineUrl", API_VACCINE_URL);
            paramMap.put("vaccineLessThanDays", "30");
            paramMap.put("familyLessThanDays", "30");
            paramMap.put("userLessThanDays", "7");

            try {
                familyHealth = homeService.findfamilyHealth(registerInfo, paramMap);
            } catch (Exception e) {
                logger.error(" msg " + e.getMessage());
            }
        }

        if (null == familyHealth) {

            UserHealthDTO userHealth = new UserHealthDTO();
            userHealth.setMainTitle("请录入您的健康数据");
            userHealth.setSubTitle("添加您的健康数据>>");
            userHealth.setHealthStatus(UserHealthStatusEnum.HAVE_NO_DATA.getId());

            FamilyMemberDTO familyMember = new FamilyMemberDTO();
            familyMember.setHealthStatus(FamilyHealthStatusEnum.HAVE_FAMILY_WITHOUT_DATA.getId());
            familyMember.setMainTitle("设置您的家庭成员数据");
            familyMember.setSubTitle("添加您家人的健康数据吧>>");

            familyHealth = new FamilyHealthDTO(userHealth, familyMember);
        }

        //家庭健康栏目
        data.put("familyHealth", familyHealth);

        List<TopicListDto> hotTopic = null;
        if (null != registerInfo) {
            try {
                hotTopic = topicManageService.getHotTopicList(registerInfo.getRegisterid(), mainArea);
            } catch (Exception e) {
                logger.error(" msg " + e.getMessage());
            }
        }

        hotTopic = CollectionUtils.isEmpty(hotTopic) ? new ArrayList<TopicListDto>(0) : hotTopic.size() > 5 ? hotTopic.subList(0, 5) : hotTopic;
        //热门话题
        data.put("hotTopic", hotTopic);

        //云头条
        CloudTopLineDTO cloudTopLine = null;
        try {
            cloudTopLine = homeService.findCloudTopLine();
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        cloudTopLine = cloudTopLine == null ? new CloudTopLineDTO() : cloudTopLine;
        data.put("cloudTopLine", cloudTopLine);

        //慢病模块
        List<ModulePortalDTO> modulePortal = null;
        try {
            modulePortal = homeService.findModulePortal();
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        modulePortal = CollectionUtils.isEmpty(modulePortal) ? new ArrayList<ModulePortalDTO>(0) : modulePortal;
        data.put("modulePortal", modulePortal);

        result.setCode(0);
        result.setData(data);
        result.setMsg("获取数据成功");
        return result;
    }

    @RequestMapping(value = "/telephoneAd", method = RequestMethod.GET)
    @VersionRange
    @WithoutToken
    public JsonResponseEntity telephoneAd(@RequestHeader(value = "main-area", required = true) String mainArea) {
        JsonResponseEntity result = new JsonResponseEntity();
        JsonNode jsonNode = null;

        AppConfig appConfig = appConfigService.findSingleAppConfigByKeyWord(mainArea, null, "app.common.floatTelephone");
        if (appConfig != null) {
            try {
                String telephoneAd = appConfig.getData();
                ObjectMapper om = new ObjectMapper();
                jsonNode = om.readTree(telephoneAd);
            } catch (Exception ex) {
                logger.error("telephoneAd " + ex.getMessage());
            }
        }

        if (null != jsonNode) {
            result.setCode(0);
            result.setData(jsonNode);
            result.setMsg("获取数据成功");
        } else {
            result.setCode(1000);
            result.setMsg("未查询到相关数据！");
        }

        return result;
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    @VersionRange(from = "4.1", to = "4.5")
    @WithoutToken
    public JsonResponseEntity indexForDisease(@RequestHeader(value = "main-area", required = true) String mainArea,
                                              @RequestHeader(value = "spec-area", required = false) String specArea,
                                              @RequestHeader(value = "app-version", required = true) String version,
                                              @RequestParam(value = "uid", required = false) String uid,
                                              @AccessToken(required = false, guestEnabled = true) Session session) {

        JsonResponseEntity result = new JsonResponseEntity();
        Map data = new HashMap();

        RegisterInfo registerInfo = null;
        if (StringUtils.isNotBlank(uid)) {
            registerInfo = registerInfoRepo.findOne(uid);
        }


        //主要功能区
        List<FunctionIconsDTO> functionIcons = null;
        try {
            functionIcons = homeService.findFunctionIconsDTO(session, version, mainArea, specArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        functionIcons = CollectionUtils.isEmpty(functionIcons) ? new ArrayList<FunctionIconsDTO>(0) : functionIcons;
        data.put("functionIcons", functionIcons);

        //特色服务
        List<SpecialServiceDTO> specialService = null;
        try {
            specialService = homeService.findSpecialServiceDTO(session, version, mainArea, specArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        specialService = CollectionUtils.isEmpty(specialService) ? new ArrayList<SpecialServiceDTO>(0) : specialService;
        data.put("specialService", specialService);

        //中央区广告
        List<CenterAdDTO> advertisements = null;
        try {
            advertisements = homeService.findCenterAdDTO(mainArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        advertisements = CollectionUtils.isEmpty(advertisements) ? new ArrayList<CenterAdDTO>(0) : advertisements;
        data.put("advertisements", advertisements);

        //侧边浮层广告
        SideAdDTO sideAd = null;
        try {
            sideAd = homeService.findSideAdDTO(mainArea, null);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        sideAd = sideAd == null ? new SideAdDTO() : sideAd;
        data.put("sideAd", sideAd);


        FamilyHealthDTO familyHealth = null;

        if (null != registerInfo) {
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put("apiMeasureUrl", API_MEASURE_URL);
            paramMap.put("apiUserhealthRecordUrl", API_USERHEALTH_RECORD_URL);
            paramMap.put("apiVaccineUrl", API_VACCINE_URL);
            paramMap.put("vaccineLessThanDays", "30");
            paramMap.put("familyLessThanDays", "30");
            paramMap.put("userLessThanDays", "7");

            try {
                familyHealth = homeService.findfamilyHealth(registerInfo, paramMap);
            } catch (Exception e) {
                logger.error(" msg " + e.getMessage());
            }
        }

        if (null == familyHealth) {
            UserHealthDTO userHealth = new UserHealthDTO();
            userHealth.setMainTitle("请录入您的健康数据");
            userHealth.setSubTitle("添加您的健康数据>>");
            userHealth.setHealthStatus(UserHealthStatusEnum.HAVE_NO_DATA.getId());

            FamilyMemberDTO familyMember = new FamilyMemberDTO();
            familyMember.setHealthStatus(FamilyHealthStatusEnum.HAVE_FAMILY_WITHOUT_DATA.getId());
            familyMember.setMainTitle("设置您的家庭成员数据");
            familyMember.setSubTitle("添加您家人的健康数据吧>>");

            familyHealth = new FamilyHealthDTO(userHealth, familyMember);
        }

        //家庭健康栏目
        data.put("familyHealth", familyHealth);

        //资讯
        List<NewsArticleListAPIEntity> articleForFirst = null;
        try {
            articleForFirst = manageNewsArticleServiceImpl.findArticleForFirst(mainArea, 0, 10);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        articleForFirst = CollectionUtils.isEmpty(articleForFirst) ? new ArrayList<NewsArticleListAPIEntity>(0) : articleForFirst;

        data.put("information", articleForFirst.size() > 3 ? articleForFirst.subList(0, 3) : articleForFirst);

        //云头条
        CloudTopLineDTO cloudTopLine = null;
        try {
            cloudTopLine = homeService.findCloudTopLine();
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        cloudTopLine = cloudTopLine == null ? new CloudTopLineDTO() : cloudTopLine;
        data.put("cloudTopLine", cloudTopLine);


        result.setCode(0);
        result.setData(data);
        result.setMsg("获取数据成功");


        try {//访问首页的时候 看是否需要进行血糖一周未测提示
            diabetesService.addDiabetesRemindMessage(uid);
        } catch (Exception e) {
            logger.error("HomeController 一周血糖 -->" + e.getLocalizedMessage());
        }
        return result;
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    @VersionRange(from = "4.3.0")
    @WithoutToken
    public JsonResponseEntity indexForHomeService(@RequestHeader(value = "main-area", required = true) String mainArea,
                                                  @RequestHeader(value = "spec-area", required = false) String specArea,
                                                  @RequestHeader(value = "app-version", required = true) String version,
                                                  @RequestParam(value = "uid", required = true) String uid,
                                                  @AccessToken(required = false, guestEnabled = true) Session session) {

        JsonResponseEntity result = new JsonResponseEntity();
        Map data = new HashMap();

        RegisterInfo registerInfo = null;
        if (StringUtils.isNotBlank(uid)) {
            registerInfo = registerInfoRepo.findOne(uid);
        }

        if(null == registerInfo){
            result.setCode(-1);
            result.setMsg("用户信息不存在");
            return result;
        }

        //首页服务
        List<HomeServiceDTO> functionIcons = null;
        try {
            Map paramMap = new HashMap();
            paramMap.put("version", version);
            paramMap.put("registerId", registerInfo.getRegisterid());
            paramMap.put("allowClose", 0);
            functionIcons = homeService.findMyHomeServices(paramMap);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        functionIcons = CollectionUtils.isEmpty(functionIcons) ? new ArrayList<HomeServiceDTO>(0) : functionIcons;
        checkService(functionIcons);
        data.put("functionIcons", functionIcons);


        List<SpecialServiceDTO> oldSpecialService = null;
        try {
            oldSpecialService = homeService.findSpecialServiceDTO(session, version, mainArea, specArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

         //是否有特色服务
        int hasSpecialService = 0;
        if(CollectionUtils.isNotEmpty(oldSpecialService)){
            hasSpecialService = 1;
        }
        data.put("hasSpecialService", hasSpecialService);

        //中央区广告
        List<CenterAdDTO> advertisements = null;
        try {
            advertisements = homeService.findCenterAdDTO(mainArea);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        advertisements = CollectionUtils.isEmpty(advertisements) ? new ArrayList<CenterAdDTO>(0) : advertisements;
        data.put("advertisements", advertisements);


        //新闻资讯
        List<NewsArticleListAPIEntity> articleForFirst = null;
        try {
            articleForFirst = manageNewsArticleServiceImpl.findArticleForFirst(mainArea, 0, 10);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        articleForFirst = CollectionUtils.isEmpty(articleForFirst) ? new ArrayList<NewsArticleListAPIEntity>(0) : articleForFirst;

        data.put("information", articleForFirst.size() > 3 ? articleForFirst.subList(0, 3) : articleForFirst);

        //健康头条
        CloudTopLineDTO cloudTopLine = null;
        try {
            cloudTopLine = homeService.findCloudTopLine();
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        cloudTopLine = cloudTopLine == null ? new CloudTopLineDTO() : cloudTopLine;
        data.put("cloudTopLine", cloudTopLine);

        //健康管理   
        // 用药提醒 &个人健康&家庭健康
        FamilyHealthJKGLDTO familyHealth = null;

        if (null != registerInfo) {
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put("apiMeasureUrl", API_MEASURE_URL);
            paramMap.put("apiUserhealthRecordUrl", API_USERHEALTH_RECORD_URL);
            paramMap.put("apiVaccineUrl", API_VACCINE_URL);
            paramMap.put("vaccineLessThanDays", "30");
            paramMap.put("familyLessThanDays", "30");
            paramMap.put("userLessThanDays", "30");

            try {
                familyHealth = homeService.findfamilyHealthForJKGL(registerInfo, paramMap);
                
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(" msg " + e.getMessage());
            }
        }
        if (null == familyHealth) {
            UserHealthJKGLDTO userHealth = new UserHealthJKGLDTO();
            userHealth.setMainTitle("您近一月无健康数据");
            userHealth.setSubTitle("");
            userHealth.setHealthStatus(UserHealthStatusEnum.HAVE_NO_DATA.getId());

            FamilyMemberJKGLDTO familyMember = new FamilyMemberJKGLDTO();
            familyMember.setHealthStatus(FamilyHealthStatusEnum.HAVE_NO_FAMILY.getId());
            familyMember.setMainTitle("请添加您的家庭成员");
            familyMember.setSubTitle("");

            familyHealth = new FamilyHealthJKGLDTO(userHealth, familyMember);
        }
        //用药提醒
        RemindForHomeDTO remindForHome=remindService.getRemindForHome(uid);
        familyHealth.setTakeDrugsRemind(remindForHome);
        
        data.put("familyHealth", familyHealth);
        
        
        try {//访问首页的时候 看是否需要进行血糖一周未测提示
            diabetesService.addDiabetesRemindMessage(uid);
        } catch (Exception e) {
            logger.error("HomeController 一周血糖 -->" + e.getLocalizedMessage());
        }
        
        result.setCode(0);
        result.setData(data);
        result.setMsg("获取数据成功");
        return result;
    }

    @RequestMapping(value = "/moreService", method = RequestMethod.GET)
    @WithoutToken
    public JsonResponseEntity moreService(@RequestHeader(value = "main-area", required = true) String mainArea,
                                          @RequestHeader(value = "spec-area", required = false) String specArea,
                                          @RequestHeader(value = "app-version", required = true) String version,
                                          @RequestParam(value = "uid", required = false) String uid,
                                          @AccessToken(required = false, guestEnabled = true) Session session) {
        JsonResponseEntity result = new JsonResponseEntity();
        Map data = new HashMap();
        RegisterInfo registerInfo = null;
        if (StringUtils.isNotBlank(uid)) {
            registerInfo = registerInfoRepo.findOne(uid);
        }

        if(null == registerInfo){
            result.setCode(-1);
            result.setMsg("用户信息不存在");
            return result;
        }

        List<HomeServiceDTO> myService = null;
        try {
            Map paramMap = new HashMap();
            paramMap.put("version", version);
            paramMap.put("registerId", registerInfo.getRegisterid());
            paramMap.put("allowClose", 0);
            myService = homeService.findMyHomeServices(paramMap);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        myService = CollectionUtils.isEmpty(myService) ? new ArrayList<HomeServiceDTO>(0) : myService;
        checkService(myService);
        data.put("myService", myService);

        List<HomeServiceDTO> baseService = null;
        try {
            Map paramMap = new HashMap();
            paramMap.put("serviceType", ServiceTypeEnum.BASE_SERVICE.getType());
            paramMap.put("version", version);
            paramMap.put("registerId", registerInfo.getRegisterid());
            paramMap.put("allowClose", 0);
            baseService = homeService.findBaseServices(paramMap);
        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }

        baseService = CollectionUtils.isEmpty(baseService) ? new ArrayList<HomeServiceDTO>(0) : baseService;
        checkService(baseService);
        handMyServiceAndBaseService(myService,baseService);
        data.put("baseService", baseService);


        //特色服务
        List<SpecialServiceDTO> oldSpecialService = null;
        try {
            oldSpecialService = homeService.findSpecialServiceDTO(session, version, mainArea, specArea);

        } catch (Exception e) {
            logger.error(" msg " + e.getMessage());
        }
        oldSpecialService = CollectionUtils.isEmpty(oldSpecialService) ? new ArrayList<SpecialServiceDTO>(0) : oldSpecialService;


        List<HomeServiceDTO> specialService = new ArrayList<HomeServiceDTO>();
        for(int index = 0;index < oldSpecialService.size();index++){
            SpecialServiceDTO oldDto =  oldSpecialService.get(index);
            HomeServiceDTO dto = new HomeServiceDTO();
            dto.setId(index+"");//设置虚拟id，兼容iso
            dto.setMainTitle(oldDto.getMainTitle());
            dto.setServiceType(ServiceTypeEnum.SPECIAL_SERVICE.getType());
            dto.setImgUrl(oldDto.getImgUrl());
            dto.setHoplink(oldDto.getHoplink());
            specialService.add(dto);
        }

        checkService(specialService);
        data.put("specialService", specialService);

        result.setCode(0);
        result.setData(data);
        result.setMsg("获取数据成功");
        return result;
    }

    /**
     * 检查我的服务里的基础服务的添加状态 （在我的服务里的基础服务的isAdd 设置为 1 （0:为添加，1: 已添加） ）
     * @param myServices
     * @param baseServices
     */
    void handMyServiceAndBaseService(List<HomeServiceDTO> myServices,List<HomeServiceDTO> baseServices){
       for(HomeServiceDTO myServiceDto:myServices){
           if (ServiceTypeEnum.BASE_SERVICE.getType().equals(myServiceDto.getServiceType()) ){
               for(HomeServiceDTO baseServiceDto:baseServices){
                   if(baseServiceDto.getId().equals(myServiceDto.getId()) && baseServiceDto.getIsAdd() == 0){
                       baseServiceDto.setIsAdd(1);
                   }

               }
           }



       }

    }

    /**
     * 添加判断
     * @param list
     */
    void checkService(List<HomeServiceDTO> list){
        for(HomeServiceDTO dto:list){
            if(dto.getMainTitle().contains("医养云")){//判断是否为医养云
                dto.setServiceType(ServiceTypeEnum.MEDICINE_CLOUD_SERVICE.getType());
            }

            if(dto.getHoplink().contains("=button_")){
                String[] arrayStr = dto.getHoplink().split("=button_");
                dto.setButtonKey(arrayStr[1]);
            }
        }
    }
    @RequestMapping(value = "/editMyService", method = RequestMethod.POST)
    @WithoutToken
    public JsonResponseEntity editMyService(
            @RequestBody(required = true) String mySerice,
            @AccessToken(required = false, guestEnabled = true) Session session) {
        JsonResponseEntity result = new JsonResponseEntity();
        if (StringUtils.isBlank(mySerice)) {
            result.setCode(-1);
            result.setMsg("修改失败");
            return result;
        }

        JSONObject jo = JSONObject.fromObject(mySerice);

        if(null == jo ){
            result.setCode(-1);
            result.setMsg("修改失败，数据格式转换异常");
            return result;
        }

        String uid = jo.getString("uid");

        RegisterInfo registerInfo = null;
        if (StringUtils.isNotBlank(uid)) {
            registerInfo = registerInfoRepo.findOne(uid);
        }
        if(null == registerInfo){
            result.setCode(-1);
            result.setMsg("用户信息不存在");
            return result;
        }

        JSONArray json = jo.getJSONArray("ids");
        List<String> editServiceIds = null;
        if (json.size() > 0) {
            editServiceIds = new ArrayList<String>();
            for (int i = 0; i < json.size(); i++) {
                JSONObject job = json.getJSONObject(i);
                editServiceIds.add(String.valueOf(job.get("id")));
            }
        }

        if (CollectionUtils.isNotEmpty(editServiceIds) && editServiceIds.size() > 11) {
            result.setCode(-1);
            result.setMsg("超过11条数据");
            return result;
        }
        // 编辑 (先删除，再添加)
        homeService.editHomeServices(registerInfo, editServiceIds);
        result.setCode(0);
        result.setMsg("操作成功");
        return result;
    }
}
