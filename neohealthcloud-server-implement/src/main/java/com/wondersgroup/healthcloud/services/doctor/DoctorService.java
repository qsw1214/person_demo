package com.wondersgroup.healthcloud.services.doctor;

import com.wondersgroup.healthcloud.jpa.entity.doctor.DoctorAccount;
import com.wondersgroup.healthcloud.jpa.entity.doctor.DoctorInfo;
import com.wondersgroup.healthcloud.services.doctor.dto.DoctorAreaResidentDto;
import com.wondersgroup.healthcloud.services.doctor.entity.Doctor;

import java.util.List;
import java.util.Map;

/**
 * Created by longshasha on 16/8/1.
 */
public interface DoctorService {

    Map<String, Object> findDoctorInfoByUidAndDoctorId(String uid, String doctorId);

    Map<String, Object> findDoctorInfoByUid(String uid);

    Doctor findDoctorByUid(String uid);

    List<Doctor> findDoctorByIds(String ids);

    Doctor findDoctorInfoByActcode(String actcode);

    DoctorInfo updateExpertin(String uid, String expertin);

    DoctorAccount updateDoctorAvatar(String uid, String avatar);

    DoctorInfo updateIntro(String uid, String intro);

    List<Map<String, Object>> findAllFaqDoctors(String kw, String rootQid, String doctorAnswerId);

    Map<String, Object> findDoctorInfoByIdcard(String doctorIdcard);

    List<Map<String, Object>> findDoctorServicesById(String uid);

    Boolean checkDoctorHasService(String doctorId, String keyword);

    List<Doctor> findDoctorListByPager(int pageNum, int size, Map parameter);

    int countFaqByParameter(Map parameter);

    List<Map<String, Object>> findDoctorServicesByIdWithoutDel(String uid);

    DoctorInfo getDoctorInfoByUid(String uid);

    DoctorAccount getDoctorAccountByUid(String uid);

    /**
     * 获取医生管辖区域内的用户
     *
     * @param doctorId
     * @return
     */
    List<DoctorAreaResidentDto> getResidentByDoctorArea(String doctorId);

    /**
     * 获取医生管辖区域内的用户
     *
     * @param doctorId
     * @return 身份证列表 List<String> String为空的已做了过滤
     */
    List<String> getResidentListByArea(String doctorId);
}
