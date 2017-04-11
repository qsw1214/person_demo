package com.wondersgroup.healthcloud.api.http.controllers.user;

import com.google.common.collect.Maps;
import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.common.http.support.misc.JsonKeyReader;
import com.wondersgroup.healthcloud.jpa.entity.user.Address;
import com.wondersgroup.healthcloud.jpa.entity.user.AnonymousAccount;
import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;
import com.wondersgroup.healthcloud.services.user.AnonymousAccountService;
import com.wondersgroup.healthcloud.services.user.UserAccountService;
import com.wondersgroup.healthcloud.services.user.UserService;
import com.wondersgroup.healthcloud.services.user.dto.UserInfoForm;
import com.wondersgroup.healthcloud.services.user.exception.ErrorUserAccountException;
import com.wondersgroup.healthcloud.utils.wonderCloud.AccessToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * Created by longshasha on 16/9/1.
 */
@RestController
@RequestMapping(value = "/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AnonymousAccountService anonymousAccountService;

    @Autowired
    private UserAccountService userAccountService;


    @PostMapping(path = "/userInfo/update")
    public JsonResponseEntity updateUserInfo(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        JsonResponseEntity body = new JsonResponseEntity<>();
        UserInfoForm form = new UserInfoForm();

        form.registerId = reader.readString("uid", false);

        form.height = reader.readObject("height", true, Integer.class);
        form.weight = reader.readObject("weight", true, Float.class);
        form.waist = reader.readObject("waist", true, Float.class);
        try {
            userService.updateUserInfo(form);
        } catch (Exception e) {
            body.setData(3101);
            body.setMsg("信息修改失败");
            return body;
        }

        body.setMsg("信息修改成功");
        return body;
    }

    /**
     * 获取用户信息
     *
     * @param uid
     * @return
     */
    @GetMapping(path = "/userInfo")
    public JsonResponseEntity<Map<String, String>> getUserInfo(@RequestParam String uid) {
        JsonResponseEntity<Map<String, String>> response = new JsonResponseEntity<>();
        Map<String, String> map = Maps.newHashMap();
        try {
            RegisterInfo registerInfo = userService.getOneNotNull(uid);
            map.put("personcard", registerInfo.getPersoncard());
            response.setData(map);
            return response;
        } catch (ErrorUserAccountException ex) {
            AnonymousAccount anonymousAccount = anonymousAccountService.getAnonymousAccount(uid, true);
            if (anonymousAccount != null && StringUtils.isNotBlank(anonymousAccount.getIdcard())) {
                map.put("personcard", anonymousAccount.getIdcard());
                response.setData(map);
            }
            return response;
        }
    }

    /**
     * 根据用户登录token判断地址是否完整(用于慢病H5页面判断)
     * @param token
     * @return
     */
    @GetMapping(path = "/judgeAddress")
    public JsonResponseEntity<Map<String, String>> judgeAddressIsComplete(@RequestParam(required = true) String token){
        JsonResponseEntity<Map<String, String>> response = new JsonResponseEntity<>();
        Map<String, String> map = Maps.newHashMap();
        Boolean judgeAddressIsComplete = false;
        try {
            AccessToken accessToken = userAccountService.getAccessToken(token);
            Address address = userService.getAddress(accessToken.getUid());
            if(address !=null &&
                    StringUtils.isNotBlank(address.getProvince()) &&
                    StringUtils.isNotBlank(address.getCity()) &&
                    StringUtils.isNotBlank(address.getCounty()) &&
                    StringUtils.isNotBlank(address.getTown()) &&
                    StringUtils.isNotBlank(address.getCommittee())){
                judgeAddressIsComplete = true;
            }
        }catch (Exception e){
            response.setCode(3104);
            response.setMsg("用户信息获取失败");
            return response;
        }
        map.put("addIsComplete",judgeAddressIsComplete.toString());
        response.setData(map);
        return response;
    }
}
