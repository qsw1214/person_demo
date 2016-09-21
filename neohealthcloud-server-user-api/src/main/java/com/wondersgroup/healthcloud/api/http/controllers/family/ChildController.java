package com.wondersgroup.healthcloud.api.http.controllers.family;

import com.wondersgroup.healthcloud.common.http.dto.JsonResponseEntity;
import com.wondersgroup.healthcloud.common.http.support.misc.JsonKeyReader;
import com.wondersgroup.healthcloud.common.http.support.version.VersionRange;
import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;
import com.wondersgroup.healthcloud.services.user.UserAccountService;
import com.wondersgroup.healthcloud.services.user.UserService;
import com.wondersgroup.healthcloud.services.user.exception.ErrorChildVerificationException;
import com.wondersgroup.healthcloud.utils.IdcardUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by longshasha on 16/9/18.
 */
@RestController
@RequestMapping("/api/child")
public class ChildController {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserService userService;

    /**
     * 提交实名认证信息
     *
     * @return
     */
    @VersionRange
    @PostMapping(path = "/verification/submit")
    public JsonResponseEntity<String> verificationSubmit(@RequestBody String request) {
        JsonKeyReader reader = new JsonKeyReader(request);
        String id = reader.readString("uid", false);//监护人Id
        String childId = reader.readString("childId", false);//儿童匿名Id
        String name = reader.readString("name", false);//儿童的真实姓名
        String idCard = reader.readString("idcard", false);//儿童的身份证号
        String idCardFile = reader.readString("idCardFile", false);//户口本(儿童身份信息页照片)
        String birthCertFile = reader.readString("birthCertFile", false);//出生证明(照片)
        JsonResponseEntity<String> body = new JsonResponseEntity<>();
        name = name.trim();//去除空字符串
        idCard = idCard.trim();
        int age = IdcardUtils.getAgeByIdCard(idCard);
        if(age>18){
            throw new ErrorChildVerificationException("年龄大于18岁的不能使用儿童实名认证");
        }
        RegisterInfo registerInfo = userService.getOneNotNull(id);
        if(!registerInfo.verified()){
            throw new ErrorChildVerificationException("您还未实名认证,请先去实名认证");
        }
        if(StringUtils.isBlank(registerInfo.getRegmobilephone())){
            throw new ErrorChildVerificationException("您未绑定手机号,请先绑定手机号");
        }
        userAccountService.childVerificationSubmit(id, childId,name, idCard, idCardFile, birthCertFile);
        body.setMsg("提交成功");
        return body;
    }


}
