package com.wondersgroup.healthcloud.services.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.wondersgroup.healthcloud.jpa.entity.user.Address;
import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;
import com.wondersgroup.healthcloud.jpa.entity.user.UserInfo;
import com.wondersgroup.healthcloud.services.user.dto.UserInfoForm;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by longshasha on 16/8/4.
 */
public interface UserService {
    Map<String, Object> findUserInfoByUid(String uid);

    RegisterInfo getOneNotNull(String id);

    List<RegisterInfo> findRegisterInfoByIdcard(String idcard);

    Boolean updateNickname(String id, String nickname);

    Boolean updateNicknameAndAvatar(String id, String nickname, String avatar);

    Boolean updateGender(String id, String gender);

    RegisterInfo findRegisterInfoByMobile(String mobile);

    void updateUserInfo(UserInfoForm form);

    Boolean updateUserHeightAndWeight(UserInfoForm form);

    UserInfo getUserInfo(String uid);

    void updateAvatar(String id, String avatar);

    Address updateAddress(String id, String province, String city, String county, String town, String committee, String other);

    Address getAddress(String uid);

    List<Map<String,Object>> findUserListByPager(int pageNum, int size, Map parameter);

    int countUserByParameter(Map parameter);

    Map<String,Object> findUserDetailByUid(String registerid);

    JsonNode getFamilyDoctorByUserPersoncard(String personcard);

    Map<String, Object>  findSignDoctorByUid(String uid);

    Boolean updateMedicarecard(String uid, String medicareCard);

    void activeInvitation(String uid, String code);

    String findFirstTagName();

    RegisterInfo findOne(String id);

    /**
     * 现在有单机版用户，查的时候如果registerInfo表查不到就差匿名表
     * @param registerId
     * @return RegisterInfo
     */
    RegisterInfo findRegOrAnonymous(String registerId);

    Map<String, RegisterInfo> findByUids(Iterable<String> uids);
}
