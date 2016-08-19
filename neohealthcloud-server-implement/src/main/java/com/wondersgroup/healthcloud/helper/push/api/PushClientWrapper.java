package com.wondersgroup.healthcloud.helper.push.api;

import com.squareup.okhttp.*;
import com.wondersgroup.common.http.HttpRequestExecutorManager;
import com.wondersgroup.common.http.entity.JsonNodeResponseWrapper;
import com.wondersgroup.common.http.utils.JsonConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ░░░░░▄█▌▀▄▓▓▄▄▄▄▀▀▀▄▓▓▓▓▓▌█
 * ░░░▄█▀▀▄▓█▓▓▓▓▓▓▓▓▓▓▓▓▀░▓▌█
 * ░░█▀▄▓▓▓███▓▓▓███▓▓▓▄░░▄▓▐█▌
 * ░█▌▓▓▓▀▀▓▓▓▓███▓▓▓▓▓▓▓▄▀▓▓▐█
 * ▐█▐██▐░▄▓▓▓▓▓▀▄░▀▓▓▓▓▓▓▓▓▓▌█▌
 * █▌███▓▓▓▓▓▓▓▓▐░░▄▓▓███▓▓▓▄▀▐█
 * █▐█▓▀░░▀▓▓▓▓▓▓▓▓▓██████▓▓▓▓▐█
 * ▌▓▄▌▀░▀░▐▀█▄▓▓██████████▓▓▓▌█▌
 * ▌▓▓▓▄▄▀▀▓▓▓▀▓▓▓▓▓▓▓▓█▓█▓█▓▓▌█▌
 * █▐▓▓▓▓▓▓▄▄▄▓▓▓▓▓▓█▓█▓█▓█▓▓▓▐█
 * <p>
 * Created by zhangzhixiu on 8/17/16.
 */
@Component
public class PushClientWrapper {

    @Value("${internal.api.service.message.url}")
    private String baseUrl;

    @Autowired
    private HttpRequestExecutorManager httpRequestExecutorManager;

    public Boolean pushToAll(AppMessage message, String area) {
        Request.Builder builder = new Request.Builder();
        builder.url(HttpUrl.parse(baseUrl + "/push/all").newBuilder().addQueryParameter("area", area).build()).post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonConverter.toJson(message)));
        JsonNodeResponseWrapper wrapper = (JsonNodeResponseWrapper) httpRequestExecutorManager.newCall(builder.build()).run().as(JsonNodeResponseWrapper.class);
        return wrapper.convertBody().get("code").asInt() == 0;
    }

    public Boolean pushToAlias(AppMessage message, String alias) {
        Request.Builder builder = new Request.Builder();
        builder.url(HttpUrl.parse(baseUrl + "/push/single").newBuilder().addQueryParameter("alias", alias).build()).post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonConverter.toJson(message)));
        JsonNodeResponseWrapper wrapper = (JsonNodeResponseWrapper) httpRequestExecutorManager.newCall(builder.build()).run().as(JsonNodeResponseWrapper.class);
        return wrapper.convertBody().get("code").asInt() == 0;
    }

    public Boolean pushToTags(AppMessage message, String area, List<String> tags) {
        Request.Builder builder = new Request.Builder();
        builder.url(HttpUrl.parse(baseUrl + "/push/tags").newBuilder().addQueryParameter("area", area).addQueryParameter("tags", StringUtils.join(tags, ",")).build()).post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonConverter.toJson(message)));
        JsonNodeResponseWrapper wrapper = (JsonNodeResponseWrapper) httpRequestExecutorManager.newCall(builder.build()).run().as(JsonNodeResponseWrapper.class);
        return wrapper.convertBody().get("code").asInt() == 0;
    }

    public static void main(String... args) {
        PushClientWrapper wrapper = new PushClientWrapper();
        wrapper.httpRequestExecutorManager = new HttpRequestExecutorManager(new OkHttpClient());
        AppMessage message = AppMessage.Builder.init().title("tttt").isDoctor(false).content("neirong").param("aaa", "bbb").urlFragment("/aaaa").build();
        System.out.println(message.toPushMessage().url);
        wrapper.pushToAlias(message, "8a81c1fb555cab53015672342b410133");
//        wrapper.pushToAll(message, "4401");
    }
}