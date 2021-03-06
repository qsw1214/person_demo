package com.wondersgroup.healthcloud.jpa.repository.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.wondersgroup.healthcloud.jpa.entity.user.RegisterInfo;

/**
 *
 * Created by longshasha on 16/8/4.
 */
public interface RegisterInfoRepository extends JpaRepository<RegisterInfo,String> {

    @Query("select r from RegisterInfo r where r.talkid =?1 and r.delFlag='0'")
    RegisterInfo findByTalkid(String talkid);

    @Query(nativeQuery = true,
            value = "select * from app_tb_register_info r where r.regmobilephone = ?1 and r.del_flag='0' limit 1")
    RegisterInfo findByMobile(String mobile);

    /**
     * 根据身份证号获取已实名认证用户列表，按登录时间(updateDate)倒序排列
     * @param personcard
     * @return
     */
    @Query("select r from RegisterInfo r where r.personcard =?1 and r.identifytype!='0' and r.delFlag='0' order by r.updateDate desc")
    List<RegisterInfo> findByPersoncard(String personcard);

    /**
     * 检查昵称是否被用 (uid!=""除去当前uid)
     */
    @Query("select count(r) > 0 as c from RegisterInfo r where r.nickname =?1 and r.registerid <> ?2")
    Boolean checkNickNameisUsedIgnoreAppointUid(String nickname, String uid);

    @Query("select r from RegisterInfo r where r.registerid =?1 and r.delFlag='0'")
    RegisterInfo findByRegisterid(String registerId);

    @Query("select r from RegisterInfo r where (r.personcard =?1 or r.regmobilephone = ?1) and r.delFlag='0'")
    List<RegisterInfo> getByCardOrPhone(String info);
    
    @Transactional
    @Modifying
    @Query("update RegisterInfo set bindPersoncard=?1 where registerId =?2")
    int updateByRegister(String bindPersoncard, String registerId);

    @Query("select r from RegisterInfo r where r.isBBsAdmin=1 and r.delFlag='0'")
    List<RegisterInfo> queryAllBBsAdmins();

    @Query(nativeQuery = true,value="SELECT COUNT(1) FROM app_tb_register_info WHERE length(nickname)>=16 AND substring(nickname, 5, 4)=?1")
    int countNickname(String nick);

    /**
     * 仅查询已认证用户
     * @param registerIds
     * @return
     */
    @Query("select a from RegisterInfo a where a.identifytype <> '0' and a.registerid in ?1")
    List<RegisterInfo> findByRegisterIds(List<String> registerIds);
}
