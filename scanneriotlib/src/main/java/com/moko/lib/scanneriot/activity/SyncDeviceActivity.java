package com.moko.lib.scanneriot.activity;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

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
import com.moko.lib.scannerui.dialog.LoadingDialog;
import com.moko.lib.scannerui.utils.ToastUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import okhttp3.RequestBody;

public class SyncDeviceActivity extends FragmentActivity implements BaseQuickAdapter.OnItemClickListener {
    private ActivityDevicesBinding mBind;
    private ArrayList<SyncDevice> devices;
    private SyncDeviceAdapter adapter;
    public Handler mHandler;
    private int mDeviceModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            // 透明导航栏
            getWindow().setNavigationBarColor(Color.TRANSPARENT);

            // Android P及以上支持刘海屏
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);

            // 设置WindowInsets监听
            getWindow().getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                private int lastOrientation = -1;

                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        // 获取当前方向
                        int currentOrientation = getResources().getConfiguration().orientation;

                        // 只有当方向改变时才重新设置padding
                        if (currentOrientation != lastOrientation) {
                            lastOrientation = currentOrientation;

                            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                                // 横屏：只考虑左右安全区域
                                v.setPadding(cutout.getSafeInsetLeft(), 0,
                                        cutout.getSafeInsetRight(), 0);
                            } else {
                                int bottomInset = 0;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                                }
                                // 竖屏：使用全部安全区域
                                v.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
                                        cutout.getSafeInsetRight(), cutout.getSafeInsetBottom() + bottomInset);
                            }

                            // 请求重新布局
                            v.requestLayout();
                        }
                    } else {
                        int bottomInset = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                        }
                        // 没有刘海屏时重置padding
                        v.setPadding(0, 0, 0, bottomInset);
                        lastOrientation = -1;
                    }

                    return insets;
                }
            });
        }
        mBind = ActivityDevicesBinding.inflate(getLayoutInflater());
        devices = getIntent().getParcelableArrayListExtra(IoTDMConstants.EXTRA_KEY_SYNC_DEVICES);
        mDeviceModel = getIntent().getIntExtra(IoTDMConstants.EXTRA_KEY_DEVICE_MODEL, 0);
        adapter = new SyncDeviceAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        mBind.rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDeviceList.setAdapter(adapter);
        mHandler = new Handler(Looper.getMainLooper());
        setContentView(mBind.getRoot());
    }


    public void onSyncDevices(View view) {
        if (isWindowLocked()) return;
        if (devices.isEmpty()) {
            ToastUtils.showToast(this, "Add devices first");
            return;
        }
        List<SyncDevice> syncDevices = adapter.getData();
        List<SyncDevice> selectedDevices = new ArrayList<>(syncDevices);
        selectedDevices.removeIf(device -> !device.isSelected);
        syncDevices(selectedDevices);
    }

    private void syncDevices(List<SyncDevice> syncDevices) {
        RequestBody body = RequestBody.create(Urls.JSON, new Gson().toJson(syncDevices));
        String url = Urls.syncGatewayApi(getApplicationContext());
        if (mDeviceModel >= 200)
            url = Urls.syncCellularGatewayApi(getApplicationContext());
        OkGo.<String>post(url)
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
                            ToastUtils.showToast(SyncDeviceActivity.this, commonResp.msg);
                            return;
                        }
                        ToastUtils.showToast(SyncDeviceActivity.this, "Sync Success");
                    }

                    @Override
                    public void onError(Response<String> response) {
                        ToastUtils.showToast(SyncDeviceActivity.this, R.string.request_error);
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
