package com.wondersgroup.healthcloud.api.http.controllers.imagetext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.wondersgroup.healthcloud.api.utils.Pager;
import com.wondersgroup.healthcloud.common.appenum.ImageTextEnum;
import com.wondersgroup.healthcloud.common.http.annotations.Admin;
import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.jpa.entity.imagetext.GImageText;
import com.wondersgroup.healthcloud.jpa.entity.imagetext.ImageText;
import com.wondersgroup.healthcloud.services.imagetext.ImageTextService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by zhaozhenxing on 2016/8/17.
 */
@RestController
@RequestMapping("/api/imagetext")
public class ImageTextController {

    private static final Logger log = Logger.getLogger(ImageTextController.class);

    @Autowired
    private ImageTextService imageTextService;

    @Admin
    @GetMapping("/findGImageTextVersions")
    public JsonResponseEntity<List<String>> findGImageTextVersions(@RequestHeader(name = "main-area", required = true) String mainArea,
                                                                   @RequestHeader(name = "spec-area", required = false) String specArea,
                                                                   @RequestParam(required = true) Integer gadcode) {
        JsonResponseEntity<List<String>> result = new JsonResponseEntity<>();
        List<String> versions = imageTextService.findGImageTextVersions(mainArea, specArea, gadcode);
        if (versions != null && versions.size() > 0) {
            result.setData(versions);
        } else {
            result.setCode(1000);
            result.setMsg("未查询到相关数据！");
        }
        return result;
    }

    @Admin
    @PostMapping("/findGImageTextList")
    public Pager findGImageTextList(@RequestHeader(name = "main-area", required = true) String main_area,
                                    @RequestHeader(name = "spec-area", required = false) String spec_area,
                                    @RequestBody Pager pager) {
        int pageNum = 1;
        if (pager.getNumber() != 0) {
            pageNum = pager.getNumber();
        }
        pager.getParameter().put("main_area", main_area);
        pager.getParameter().put("spec_area", spec_area);
        List<GImageText> gImageTexts = imageTextService.findGImageTextList(pageNum, pager.getSize(), pager.getParameter());
        if (gImageTexts != null && gImageTexts.size() > 0) {
            int totalSize = imageTextService.countGImageTextList(pager.getParameter());
            pager.setTotalElements(totalSize);
            pager.setData(gImageTexts);
        }
        return pager;
    }

    @Admin
    @GetMapping("/findGImageTextById")
    public JsonResponseEntity<GImageText> findGImageTextById(@RequestParam String gid) {
        JsonResponseEntity<GImageText> result = new JsonResponseEntity<>();
        GImageText gImageText = imageTextService.findGImageTextById(gid);
        if (gImageText != null) {
            result.setData(gImageText);
        } else {
            result.setCode(1000);
            result.setMsg("未查询到相关数据！");
        }
        return result;
    }

    @Admin
    @PostMapping("/saveGImageText")
    public JsonResponseEntity saveGImageText(@RequestHeader(required = true) String source, @RequestBody GImageText gImageText) {
        JsonResponseEntity result = new JsonResponseEntity();
        gImageText.setSource(source);
        if (imageTextService.saveGImageText(gImageText)) {
            result.setMsg("数据保存成功");
        } else {
            result.setCode(1000);
            result.setMsg("数据保存失败！");
        }
        return result;
    }

    @Admin
    @GetMapping(value = "/findImageTextById")
    public JsonResponseEntity<ImageText> findImageTextById(@RequestParam(required = true) String id) {
        JsonResponseEntity<ImageText> result = new JsonResponseEntity<>();
        ImageText imageText = imageTextService.findImageTextById(id);
        if (imageText != null) {
            result.setData(imageText);
        } else {
            result.setCode(1000);
            result.setMsg("未查询到相关配置数据！");
        }
        return result;
    }

    @Admin
    @RequestMapping(value = "/findImageTextByAdcode", method = RequestMethod.POST)
    public Pager findImageTextByAdcode(@RequestHeader(name = "main-area", required = true) String mainArea,
                                       @RequestHeader(name = "spec-area", required = false) String specArea,
                                       @RequestBody(required = true) Pager pager) {
        JsonResponseEntity result = new JsonResponseEntity();
        int pageNum = 1;
        if (pager.getNumber() != 0) {
            pageNum = pager.getNumber();
        }
        pager.getParameter().put("mainArea", mainArea);
        pager.getParameter().put("specArea", specArea);

        List<ImageText> imageTextList = imageTextService.findImageTextByAdcode(pageNum, pager.getSize(), pager.getParameter());
        if (imageTextList != null && imageTextList.size() > 0) {
            int totalSize = imageTextService.countImageTextByAdcode(pager.getParameter());
            pager.setTotalElements(totalSize);
            pager.setData(imageTextList);
        }
        return pager;
    }

    @Admin
    @PostMapping(value = "/saveBatchImageText")
    public JsonResponseEntity saveBatchImageText(@RequestBody String imageTexts) {
        JsonResponseEntity result = new JsonResponseEntity();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(List.class, ImageText.class);
            List<ImageText> imageTextList = objectMapper.readValue(imageTexts, javaType);
            if (imageTextList != null && imageTextList.size() > 0) {
                int flag = imageTextService.saveBatchImageText(imageTextList);
                if (flag == imageTextList.size()) {
                    result.setMsg("数据保存成功！");
                    return result;
                }
            }
        } catch (Exception ex) {
            log.error("ImageTextController.saveBatchImageText error --> " + ex.getLocalizedMessage());
        }
        result.setCode(1000);
        result.setMsg("数据保存失败！");
        return result;
    }

    @Admin
    @RequestMapping(value = "/saveImageText", method = RequestMethod.POST)
    public JsonResponseEntity saveImageText(@RequestHeader(required = true) String source, @RequestBody ImageText imageText) {
        JsonResponseEntity result = new JsonResponseEntity();
        ImageTextEnum imageTextEnum = ImageTextEnum.fromValue(imageText.getAdcode());
        if (imageTextEnum != null) {
            imageText.setSource(source);
            ImageText advertisement = imageTextService.saveImageText(imageText);
            if (advertisement != null) {
                result.setMsg("广告信息保存成功！");
            } else {
                result.setCode(1001);
                result.setMsg("广告信息保存失败！");
            }
        } else {
            result.setCode(1000);
            result.setMsg("广告类型参数异常");
        }
        return result;
    }

}
