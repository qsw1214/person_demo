package com.wondersgroup.healthcloud.api.http.controllers.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wondersgroup.healthcloud.api.utls.CommonUtils;
import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.common.http.support.version.VersionRange;
import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;
import com.wondersgroup.healthcloud.services.user.UserService;
import com.wondersgroup.healthcloud.utils.DateFormatter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by zhaozhenxing on 2016/12/21.
 */
@RestController
@RequestMapping("/api/measure")
public class MeasureController {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MeasureController.class);

    @Autowired
    private UserService userService;

    @Value("${internal.api.service.measure.url}")
    private String host;
    private static final String requestUploadPath = "%s/api/measure/upload/%s";
    private static final String recentMeasureHistory = "%s/api/measure/3.0/recentHistory/%s?%s";
    private static final String recentMeasureHistoryByDate = "%s/api/measure/3.0/recentHistoryByDate/%s?%s";

    private RestTemplate template = new RestTemplate();

    @VersionRange
    @PostMapping("upload/{type}")
    public JsonResponseEntity<?> uploadMeasureIndexs(@PathVariable int type, @RequestBody Map<String, Object> paras) {
        try {
            String registerId = (String) paras.get("registerId");
            RegisterInfo info = userService.findRegOrAnonymous(registerId);

            String personCard = info.getPersoncard();
            if (personCard != null) {
                paras.put("personCard", personCard);
            }

            String url = String.format(requestUploadPath, host, type);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            headers.add("access-token", "version3.0");
            ResponseEntity<Map> response = template.postForEntity(url, new HttpEntity<>(paras, headers), Map.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                if (0 == (int) response.getBody().get("code")) {
                    return new JsonResponseEntity<>(0, "数据上传成功", response.getBody().get("data"));
                }
            }
        } catch (RestClientException e) {
            log.info("上传体征数据失败", e);
        }
        return new JsonResponseEntity<>(1000, "数据上传失败");
    }

    @RequestMapping(value = "/lastWeekHistory", method = RequestMethod.GET)
    public JsonResponseEntity measureHistory(@RequestParam(name = "uid") String registerId,
                                             @RequestParam(required = false) String personCard) {
        JsonResponseEntity result = new JsonResponseEntity();
        Map<String, Object> rtnMap = new HashMap<>();
        try {
            List<JsonNode> lastWeekData = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < 7; i++) {
                lastWeekData.add(((ObjectNode) mapper.readTree("{}")).put("date", DateFormatter.dateFormat(DateTime.now().plusDays(-i).toDate())));
            }
            RegisterInfo info = userService.getOneNotNull(registerId);
            String param = "registerId=".concat(registerId)
                    .concat("&sex=").concat(StringUtils.isEmpty(info.getGender()) ? "1" : info.getGender())
                    .concat("&personCard=").concat(StringUtils.isEmpty(info.getPersoncard()) ? "" : info.getPersoncard());
            String url = String.format(recentMeasureHistory, host, "3", param);
            ResponseEntity<Map> response = buildGetEntity(url, Map.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                if (0 == (int) response.getBody().get("code")) {
                    String jsonStr = mapper.writeValueAsString(response.getBody().get("data"));
                    if (StringUtils.isNotEmpty(jsonStr)) {
                        JsonNode resultJson = mapper.readTree(jsonStr);
                        Iterator<JsonNode> contentJson = resultJson.get("content").iterator();// 最近10天测量记录
                        DateTime today = DateTime.now();
                        while (contentJson.hasNext()) {
                            JsonNode jsonNode = contentJson.next();// 单日测量记录
                            String date = jsonNode.get("date").asText();
                            if (date != null
                                    && (new DateTime(date).isAfter(today.plusDays(-6).withTimeAtStartOfDay().getMillis())
                                    || new DateTime(date).isEqual(today.plusDays(-6).withTimeAtStartOfDay().getMillis()))
                                    && new DateTime(date).isBefore(today.plusDays(1).withTimeAtStartOfDay())) {
                                String[] dayDatas = {"", "", "", "", "", "", "", ""};
                                JsonNode jsonNodeData = jsonNode.get("data");
                                Iterator<JsonNode> itJsonNodeData = jsonNodeData.iterator();
                                while (itJsonNodeData.hasNext()) {
                                    JsonNode tmpJson = itJsonNodeData.next();
                                    int testPeriod = tmpJson.get("testPeriod").asInt();
                                    if (-1 <= testPeriod && testPeriod <= 6) {
                                        dayDatas[testPeriod + 1] = tmpJson.get("fpgValue").asText();
                                    }
                                }
                                StringBuffer strBuf = new StringBuffer();
                                for (int j = 0; j < dayDatas.length; j++) {
                                    if (j == 0) {
                                        strBuf.append(dayDatas[j]);
                                    } else {
                                        strBuf.append(",").append(dayDatas[j]);
                                    }
                                }
                                ((ObjectNode) jsonNode).put("data", strBuf.toString());
                                lastWeekData.set(today.getDayOfYear() - new DateTime(date).getDayOfYear(), jsonNode);
                            }
                        }
                    }
                }
            }
            Map<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("high", 3);
            tmpMap.put("normal", 7);
            tmpMap.put("low", 6);
            rtnMap.put("status", tmpMap);
            rtnMap.put("list", lastWeekData);
            result.setData(rtnMap);
        } catch (Exception e) {
            log.info("近期历史数据获取失败", e);
        }
        return result;
    }

    @RequestMapping(value = "/getMeasureHistoryByDate", method = RequestMethod.GET)
    public JsonResponseEntity measureHistoryByDate(@RequestParam(name = "uid") String registerId,
                                                   @RequestParam(required = false) String personCard,
                                                   @RequestParam(name = "beginDate", required = false) String begin_date) {
        JsonResponseEntity result = new JsonResponseEntity();
        Map<String, Object> rtnMap = new HashMap<>();
        String flagDate = StringUtils.isEmpty(begin_date) ? DateTime.now().toString("yyyy-MM-dd") : begin_date;
        DateTime beginDateTime = new DateTime(flagDate).plusMonths(-1).plusDays(1);
        DateTime endDateTime = new DateTime(flagDate);
        rtnMap.put("nextMonth", new DateTime(flagDate).plusMonths(1).toString("yyyy-MM-dd"));
        rtnMap.put("frontMonth", new DateTime(flagDate).plusMonths(-1).toString("yyyy-MM-dd"));
        try {
            List<JsonNode> monthDate = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            int days = -Days.daysBetween(endDateTime, beginDateTime).getDays() + 1;
            for (int i = 0; i < days; i++) {
                monthDate.add(((ObjectNode) mapper.readTree("{}")).put("date", DateFormatter.dateFormat(endDateTime.plusDays(-i).toDate())));
            }
            RegisterInfo info = userService.getOneNotNull(registerId);
            String param = "registerId=".concat(registerId)
                    .concat("&sex=").concat(StringUtils.isEmpty(info.getGender()) ? "1" : info.getGender())
                    .concat("&personCard=").concat(StringUtils.isEmpty(info.getPersoncard()) ? "" : info.getPersoncard())
                    .concat("&begin_date=").concat(beginDateTime.toString("yyyy-MM-dd"))
                    .concat("&end_date=").concat(endDateTime.toString("yyyy-MM-dd"));
            String url = String.format(recentMeasureHistoryByDate, host, "3", param);
            ResponseEntity<Map> response = buildGetEntity(url, Map.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                if (0 == (int) response.getBody().get("code")) {
                    String jsonStr = mapper.writeValueAsString(response.getBody().get("data"));
                    if (StringUtils.isNotEmpty(jsonStr)) {
                        JsonNode resultJson = mapper.readTree(jsonStr);
                        Iterator<JsonNode> contentJson = resultJson.get("content").iterator();// 最近10天测量记录
                        while (contentJson.hasNext()) {
                            JsonNode jsonNode = contentJson.next();// 单日测量记录
                            String date = jsonNode.get("date").asText();
                            if (date != null
                                    && (new DateTime(date).isAfter(beginDateTime.withTimeAtStartOfDay().getMillis())
                                    || new DateTime(date).isEqual(beginDateTime.withTimeAtStartOfDay().getMillis()))
                                    && new DateTime(date).isBefore(endDateTime.plusDays(1).withTimeAtStartOfDay())) {
                                String[] dayDatas = {"", "", "", "", "", "", "", ""};
                                JsonNode jsonNodeData = jsonNode.get("data");
                                Iterator<JsonNode> itJsonNodeData = jsonNodeData.iterator();
                                while (itJsonNodeData.hasNext()) {
                                    JsonNode tmpJson = itJsonNodeData.next();
                                    int testPeriod = tmpJson.get("testPeriod").asInt();
                                    if (-1 <= testPeriod && testPeriod <= 6) {
                                        dayDatas[testPeriod + 1] = tmpJson.get("fpgValue").asText();
                                    }
                                }
                                StringBuffer strBuf = new StringBuffer();
                                for (int j = 0; j < dayDatas.length; j++) {
                                    if (j == 0) {
                                        strBuf.append(dayDatas[j]);
                                    } else {
                                        strBuf.append(",").append(dayDatas[j]);
                                    }
                                }
                                ((ObjectNode) jsonNode).put("data", strBuf.toString());
                                monthDate.set(-Days.daysBetween(endDateTime, new DateTime(date)).getDays(), jsonNode);
                            }
                        }
                    }
                }
            }
            rtnMap.put("thisMonth", monthDate);
            result.setData(rtnMap);
        } catch (Exception e) {
            log.info("近期历史数据获取失败", e);
        }
        return result;
    }

    private <T> ResponseEntity<T> buildGetEntity(String url, Class<T> responseType, Object... urlVariables) {
        RestTemplate template = new RestTemplate();
        return template.exchange(url, HttpMethod.GET, new HttpEntity<>(buildHeader()), responseType, urlVariables);
    }

    private HttpHeaders buildHeader() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String version = request.getHeader("version");
        boolean isStandard = CommonUtils.compareVersion(version, "4.1");
        HttpHeaders headers = new HttpHeaders();
        headers.add("isStandard", String.valueOf(isStandard));
        return headers;
    }
}