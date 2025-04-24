package com.moko.lib.scanneriot.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.moko.lib.scanneriot.IoTDMConstants;
import com.moko.lib.scanneriot.R;
import com.moko.lib.scanneriot.Urls;
import com.moko.lib.scanneriot.adapter.SyncDeviceAdapter;
import com.moko.lib.scanneriot.databinding.ActivityDevicesBinding;
import com.moko.lib.scanneriot.dialog.LogoutDialog;
import com.moko.lib.scanneriot.entity.CommonResp;
import com.moko.lib.scanneriot.entity.SyncDevice;
import com.moko.lib.scanneriot.utils.IoTDMToastUtils;
import com.moko.lib.scannerui.dialog.LoadingDialog;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import okhttp3.RequestBody;

public class SyncDeviceActivity extends FragmentActivity implements BaseQuickAdapter.OnItemClickListener {
    private ActivityDevicesBinding mBind;
    private ArrayList<SyncDevice> devices;
    private SyncDeviceAdapter adapter;
    public Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityDevicesBinding.inflate(getLayoutInflater());
        devices = getIntent().getParcelableArrayListExtra(IoTDMConstants.EXTRA_KEY_SYNC_DEVICES);
        adapter = new SyncDeviceAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        mBind.rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDeviceList.setAdapter(adapter);
        mHandler = new Handler(Looper.getMainLooper());
    }


    public void onSyncDevices(View view) {
        if (isWindowLocked()) return;
        if (devices.isEmpty()) {
            IoTDMToastUtils.showToast(this, "Add devices first");
            return;
        }
        List<SyncDevice> syncDevices = adapter.getData();
        List<SyncDevice> selectedDevices = new ArrayList<>(syncDevices);
        selectedDevices.removeIf(device -> !device.isSelected);
        syncDevices(selectedDevices);
    }

    private void syncDevices(List<SyncDevice> syncDevices) {
        RequestBody body = RequestBody.create(Urls.JSON, new Gson().toJson(syncDevices));
        OkGo.<String>post(Urls.syncGatewayApi(getApplicationContext()))
                .upRequestBody(body)
                .execute(new StringCallback() {

                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        showLoadingProgressDialog();
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        Type type = new TypeToken<CommonResp<JsonObject>>() {
                        }.getType();
                        CommonResp<JsonObject> commonResp = new Gson().fromJson(response.body(), type);
                        if (commonResp.code != 200) {
                            IoTDMToastUtils.showToast(SyncDeviceActivity.this, commonResp.msg);
                            return;
                        }
                        IoTDMToastUtils.showToast(SyncDeviceActivity.this, "Sync Success");
                    }

                    @Override
                    public void onError(Response<String> response) {
                        IoTDMToastUtils.showToast(SyncDeviceActivity.this, R.string.request_error);
                    }

                    @Override
                    public void onFinish() {
                        dismissLoadingProgressDialog();
                    }
                });
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        SyncDevice syncDevice = (SyncDevice) adapter.getItem(position);
        if (syncDevice == null)
            return;
        boolean isSelected = syncDevice.isSelected;
        syncDevice.isSelected = !isSelected;
        adapter.notifyItemChanged(position);
    }


    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        finish();
    }

    public void onAccount(View view) {
        if (isWindowLocked()) return;
        LogoutDialog dialog = new LogoutDialog();
        dialog.setOnLogoutClicked(() -> {
            back();
        });
        dialog.show(getSupportFragmentManager());
    }

    // 记录上次页面控件点击时间,屏蔽无效点击事件
    protected long mLastOnClickTime = 0;

    public boolean isWindowLocked() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastOnClickTime > 500) {
            mLastOnClickTime = current;
            return false;
        } else {
            return true;
        }
    }

    private LoadingDialog mLoadingDialog;

    public void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    public void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }
}
