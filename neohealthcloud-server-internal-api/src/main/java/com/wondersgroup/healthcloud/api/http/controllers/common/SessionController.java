package com.wondersgroup.healthcloud.api.http.controllers.common;

import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.services.user.SessionUtil;
import com.wondersgroup.healthcloud.services.user.dto.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by longshasha on 16/5/31.
 */

@RestController
public class SessionController {

    @Autowired
    private SessionUtil sessionUtil;

    @RequestMapping(value = "/internal/session", method = RequestMethod.GET)
    public JsonResponseEntity<Session> get(@RequestParam String token) {
        JsonResponseEntity<Session> response = new JsonResponseEntity<>();
        Session session = sessionUtil.get(token);
        if (session == null) {
            response.setCode(1001);

        } else {
            response.setData(sessionUtil.get(token));
        }
        return response;
    }


}
