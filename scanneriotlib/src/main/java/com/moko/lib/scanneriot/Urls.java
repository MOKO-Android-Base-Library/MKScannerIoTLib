package com.moko.lib.scanneriot;

import android.content.Context;

import com.moko.lib.scanneriot.utils.IoTDMSPUtils;

import okhttp3.MediaType;

public class Urls {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 测试环境
     */
    private static final String HOST_URL_TEST = "https://testiotdm.mokotechnology.com/prod-api/";
    /**
     * 正式环境
     */
    private static final String HOST_URL_CLOUD = "https://cloud.mokotechnology.com/stage-api/";

    /**
     * 生产测试
     */
    // 用户登录
    private static final String URL_LOGIN = "auth/login";
    // req:
    // {"username":"lwz","password":"123456"}
    // resp:
    // {
    //    "code": 200,
    //    "msg": null,
    //    "data": {
    //        "access_token": "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjoxMDYsInVzZXJfa2V5IjoiNGZkMWQzZWEtOWUyMy00YTMxLWE0YmYtNzAyZDY0MjU3MjY3IiwidXNlcm5hbWUiOiJsd3oifQ.Vd8oWDpJpnX__SaEpq65VKkyQdHCHVdWY-RurdZowroBHmean7pDBqgFOFeuW4eA84Asw8f-cBcKXs7RS382Lg",
    //        "expires_in": 720
    //    }
    //}

    // 同步网关
    private static final String URL_SYNC_GATEWAY = "mqtt/mqttGateway/batchAdd";
    // 同步网管（MKGW4）
    private static final String URL_SYNC_CELLULAR_GATEWAY = "mqtt/mqttGateway/cellular/batchAdd";

    // req:
    // [{"macName":"","mac":"","model":"","publishTopic":"","subscribeTopic":"","lastWill":""}]
    // resp:
    // {
    //    "code": 200,
    //    "msg": null,
    //    "data": {
    //
    //    }
    //}

    public static void setTestEnv(Context context) {
        IoTDMSPUtils.setStringValue(context, IoTDMConstants.SP_URL, HOST_URL_TEST);
    }

    public static void setCloudEnv(Context context) {
        IoTDMSPUtils.setStringValue(context, IoTDMConstants.SP_URL, HOST_URL_CLOUD);
    }

    private static String getUrl(Context context) {
        return IoTDMSPUtils.getStringValue(context, IoTDMConstants.SP_URL, HOST_URL_CLOUD);
    }

    public static String loginApi(Context context) {
        return getUrl(context) + URL_LOGIN;
    }

    public static String syncGatewayApi(Context context) {
        return getUrl(context) + URL_SYNC_GATEWAY;
    }

    public static String syncCellularGatewayApi(Context context) {
        return getUrl(context) + URL_SYNC_CELLULAR_GATEWAY;
    }
}
