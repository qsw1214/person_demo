package com.wondersgroup.healthcloud.api.http.controllers.user;

import com.wondersgroup.healthcloud.common.http.annotations.WithoutToken;
import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.common.http.support.misc.JsonKeyReader;
import com.wondersgroup.healthcloud.common.http.support.version.VersionRange;
import com.wondersgroup.healthcloud.services.user.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 意见反馈
     */
    @PostMapping(path = "/user/feedback")
    @WithoutToken
    @VersionRange
    public JsonResponseEntity<String> saveFeedback(@RequestHeader("main-area") String mainArea, @RequestBody String request) {
        JsonResponseEntity<String> response = new JsonResponseEntity<>();
        JsonKeyReader reader = new JsonKeyReader(request);
        String uid = reader.readString("uid", true);
        String comments = reader.readString("comments", false);
        String contact = reader.readString("contact", true);

        feedbackService.saveFeedback(uid, comments, contact, mainArea, "0");
        response.setData("反馈成功");
        return response;
    }
}
