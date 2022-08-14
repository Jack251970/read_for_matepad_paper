package com.jack.bookshelf.view.activity;

import static com.jack.bookshelf.utils.NetworkUtils.isNetWorkAvailable;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.hwangjr.rxbus.RxBus;
import com.jack.bookshelf.BuildConfig;
import com.jack.bookshelf.DbHelper;
import com.jack.bookshelf.MApplication;
import com.jack.bookshelf.R;
import com.jack.bookshelf.base.BaseViewPagerActivity;
import com.jack.bookshelf.constant.RxBusTag;
import com.jack.bookshelf.databinding.ActivityMainBinding;
import com.jack.bookshelf.help.FileHelp;
import com.jack.bookshelf.help.ProcessTextHelp;
import com.jack.bookshelf.help.permission.Permissions;
import com.jack.bookshelf.help.permission.PermissionsCompat;
import com.jack.bookshelf.help.storage.BackupRestoreUi;
import com.jack.bookshelf.model.UpLastChapterModel;
import com.jack.bookshelf.presenter.MainPresenter;
import com.jack.bookshelf.presenter.contract.MainContract;
import com.jack.bookshelf.service.WebService;
import com.jack.bookshelf.utils.StringUtils;
import com.jack.bookshelf.utils.theme.ThemeStore;
import com.jack.bookshelf.view.fragment.BookListFragment;
import com.jack.bookshelf.view.popupmenu.MoreSettingMenuMain;
import com.jack.bookshelf.view.popupmenu.SelectMenu;
import com.jack.bookshelf.widget.modialog.InputDialog;

import java.util.List;
import java.util.Objects;

import kotlin.Unit;

/**
 * Main Page
 * Adapt to Huawei MatePad Paper
 * Edited by Jack Ye
 */

public class MainActivity extends BaseViewPagerActivity<MainContract.Presenter> implements MainContract.View,
        BookListFragment.CallbackValue {
    private final int requestSource = 14;
    private ActivityMainBinding binding;
    private int group;
    private long exitTime = 0;
    private boolean resumed = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MoreSettingMenuMain moreSettingMenuMain;

    @Override
    protected MainContract.Presenter initInjector() {
        return new MainPresenter();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            resumed = savedInstanceState.getBoolean("resumed");
        }
        group = preferences.getInt("bookshelfGroup", 0);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("resumed", resumed);
    }

    @Override
    protected void onCreateActivity() {
        getWindow().getDecorView().setBackgroundColor(ThemeStore.backgroundColor(this));
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /**
     * 界面pause后恢复导入网络书籍
     */
    @Override
    public void onResume() {
        super.onResume();
        String shared_url = preferences.getString("shared_url", "");
        if (shared_url.length() > 1) {
            InputDialog.builder(this)
                    .setTitle(getString(R.string.add_book_url))
                    .setDefaultValue(shared_url)
                    .setCallback(new InputDialog.Callback() {
                        @Override
                        public void setInputText(String inputText) {
                            inputText = StringUtils.trim(inputText);
                            mPresenter.addBookUrl(inputText);
                        }

                        @Override
                        public void delete(String value) {

                        }
                    }).show();
            preferences.edit()
                    .putString("shared_url", "")
                    .apply();
        }
    }

    @Override
    protected void initData() {}

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) { return super.dispatchTouchEvent(ev); }

    @Override
    public void initImmersionBar() { super.initImmersionBar(); }

    @Override
    public void recreate() { super.recreate(); }

    @Override
    public boolean isRecreate() { return isRecreate; }

    @Override
    public int getGroup() { return group; }

    @Override
    public ViewPager getViewPager() { return mVp; }

    @Override
    protected List<Fragment> createTabFragments() {
        BookListFragment bookListFragment = null;
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof BookListFragment) {
                bookListFragment = (BookListFragment) fragment;
            }
        }
        if (bookListFragment == null)
            bookListFragment = new BookListFragment();
        return List.of(bookListFragment);
    }

    public BookListFragment getBookListFragment() {
        try {
            return (BookListFragment) mFragmentList.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void bindView() {
        super.bindView();
        // 初始化书架布局图标
        initLayoutIcon();
        // 初始化书籍类别图标
        initGroupIcon(group);
        // 更新书籍类别
        upGroup(group);
        // 初始化菜单
        moreSettingMenuMain = new MoreSettingMenuMain(this, new MoreSettingMenuMain.OnItemClickListener() {
            @Override
            public void downloadAll() {
                if (!isNetWorkAvailable()) {
                    toast(R.string.network_connection_unavailable);
                } else {
                    RxBus.get().post(RxBusTag.DOWNLOAD_ALL, 10000);
                }
            }

            @Override
            public void arrangeBookshelf() {
                if (getBookListFragment() != null) {
                    getBookListFragment().setArrange(true);
                }
            }

            @Override
            public void selectSequenceRule() {
                SelectMenu selectMenuMain = new SelectMenu(MainActivity.this,
                        "书籍排序",
                        "取消",
                        new String[]{"按阅读时间排序", "按更新时间排序", "手动排序"},
                        Integer.parseInt(preferences.getString("bookshelf_px", "0")),
                        new SelectMenu.OnItemClickListener() {
                            @Override
                            public void forBottomButton() {}

                            @Override
                            public void changeArrangeRule(int lastChoose, int position) {
                                if (position != lastChoose) {
                                    preferences.edit().putString("bookshelf_px",String.valueOf(position)).apply();
                                    RxBus.get().post(RxBusTag.RECREATE, true);
                                }
                            }
                        });
                if (!selectMenuMain.isShowing()) {
                    selectMenuMain.show(binding.mppLlContentMain);
                }
            }

            @Override
            public void startWebService() {
                boolean startedThisTime = WebService.startThis(MainActivity.this);
                if (!startedThisTime) {
                    toast(getString(R.string.web_service_already_started_hint));
                }
            }
        });
        // 左侧边栏事件
        binding.mppLlMineMain.setOnClickListener(view ->
                AboutActivity.startThis(this));
        binding.mppLlBookSourceMain.setOnClickListener(view ->
                BookSourceActivity.startThis(this, requestSource));
        binding.mppLlReplaceMain.setOnClickListener(view ->
                ReplaceRuleActivity.startThis(this, null));
        binding.mppLlDownloadMain.setOnClickListener(view ->
                DownloadActivity.startThis(this));
        binding.mppLlBackupMain.setOnClickListener(view ->
                BackupRestoreUi.INSTANCE.backup(this));
        binding.mppLlRestoreMain.setOnClickListener(view ->
                BackupRestoreUi.INSTANCE.restore(this));
        binding.mppIvSettingMain.setOnClickListener(view ->
                SettingActivity.startThis(this));
        // 主界面搜索栏事件
        binding.mppLlSearchMain.setOnClickListener(view -> MainActivity.this
                .startActivity(new Intent(MainActivity.this, SearchBookActivity.class)));
        // 主界面添加本地事件
        binding.mppIvAddLocalMain.setOnClickListener(view -> importLocalBooks());
        // 主界面导入网络事件
        binding.mppIvImportOnlineMain.setOnClickListener(view -> InputDialog.builder(this)
                .setTitle(getString(R.string.add_book_url))
                .setCallback(new InputDialog.Callback() {
                    @Override
                    public void setInputText(String inputText) {
                        inputText = StringUtils.trim(inputText);
                        mPresenter.addBookUrl(inputText);
                    }
                    @Override
                    public void delete(String value) {}
                }).show());
        // 主界面选择书架布局事件
        binding.mppIvSelectLayoutMain.setOnClickListener(view -> changeBookshelfLayout());
        // 主界面更多选项事件
        binding.mppIvMoreSettingsMain.setOnClickListener(view -> {
            if (!moreSettingMenuMain.isShowing()) {
                moreSettingMenuMain.show(binding.mppLlContentMain, binding.mppIvMoreSettingsMain);
            }
        });
        // 书籍类别切换事件
        binding.mppTvAllBooksMain.setOnClickListener(view -> upGroup(0));
        binding.mppTvChaseBookMain.setOnClickListener(view -> upGroup(1));
        binding.mppTvFattenBookMain.setOnClickListener(view -> upGroup(2));
        binding.mppTvEndBookMain.setOnClickListener(view -> upGroup(3));
        binding.mppTvLocalBookMain.setOnClickListener(view -> upGroup(4));
    }

    /**
     * 导入本地书籍
     */
    private void importLocalBooks() {
        new PermissionsCompat.Builder(this)
                .addPermissions(Permissions.READ_EXTERNAL_STORAGE, Permissions.WRITE_EXTERNAL_STORAGE)
                .rationale(R.string.import_per)
                .onGranted((requestCode) -> {
                    startActivity(new Intent(MainActivity.this, ImportBookActivity.class));
                    return Unit.INSTANCE;
                })
                .request();
    }

    /**
     * 更新书籍类别
     */
    private void upGroup(int group) {
        if (this.group != group) {
            upGroupIcon(this.group,group);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("bookshelfGroup", group);
            editor.apply();
        }
        this.group = group;
        RxBus.get().post(RxBusTag.UPDATE_GROUP, group);
        RxBus.get().post(RxBusTag.REFRESH_BOOK_LIST, false);
    }

    /**
     * 更新书籍类别图标
     */
    private void upGroupIcon(int old_group,int group) {
        switch (old_group) {
            case 0:
                binding.mppIvAllBooksIndicatorMain.setVisibility(View.INVISIBLE);
                break;
            case 1:
                binding.mppIvChaseBookIndicatorMain.setVisibility(View.INVISIBLE);
                break;
            case 2:
                binding.mppTvFattenBookIndicatorMain.setVisibility(View.INVISIBLE);
                break;
            case 3:
                binding.mppIvEndBookIndicatorMain.setVisibility(View.INVISIBLE);
                break;
            case 4:
                binding.mppIvLocalBookIndicatorMain.setVisibility(View.INVISIBLE);
                break;
        }
        initGroupIcon(group);
    }

    /**
     * 初始化书籍类别图标
     */
    private void initGroupIcon(int group) {
        switch (group) {
            case 0:
                binding.mppIvAllBooksIndicatorMain.setVisibility(View.VISIBLE);
                break;
            case 1:
                binding.mppIvChaseBookIndicatorMain.setVisibility(View.VISIBLE);
                break;
            case 2:
                binding.mppTvFattenBookIndicatorMain.setVisibility(View.VISIBLE);
                break;
            case 3:
                binding.mppIvEndBookIndicatorMain.setVisibility(View.VISIBLE);
                break;
            case 4:
                binding.mppIvLocalBookIndicatorMain.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * 改变书架布局按钮图标
     */
    private void initLayoutIcon() {
        if (preferences.getInt("bookshelfLayout", 1) == 0) {
            binding.mppIvSelectLayoutMain.setImageResource(R.drawable.ic_bookshelf_layout_grid);
        } else {
            binding.mppIvSelectLayoutMain.setImageResource(R.drawable.ic_bookshelf_layout_list);
        }
    }

    /**
     * 改变书架布局
     */
    private void changeBookshelfLayout() {
        if (preferences.getInt("bookshelfLayout", 1) == 0) {
            preferences.edit().putInt("bookshelfLayout", 1).apply();
        } else {
            preferences.edit().putInt("bookshelfLayout", 0).apply();
        }
        recreate();
    }

    /**
     * 检查并记录版本号
     */
    private void versionUp() {
        if (preferences.getInt("versionCode", 0) != MApplication.getVersionCode()) {
            preferences.edit()
                    .putInt("versionCode", MApplication.getVersionCode())
                    .apply();
        }
    }

    @Override
    protected void firstRequest() {
        if (!isRecreate) {
            versionUp();
        }
        if (!Objects.equals(MApplication.downloadPath, FileHelp.getFilesPath())) {
            new PermissionsCompat.Builder(this)
                    .addPermissions(Permissions.READ_EXTERNAL_STORAGE, Permissions.WRITE_EXTERNAL_STORAGE)
                    .rationale(R.string.get_storage_per)
                    .request();
        }
        handler.postDelayed(() -> {
            UpLastChapterModel.getInstance().startUpdate();
            if (BuildConfig.DEBUG) {
                ProcessTextHelp.setProcessTextEnable(false);
            }
        }, 60 * 1000);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 在2秒后再次点击退出程序
     */
    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            toast(getString(R.string.double_click_exit));
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        UpLastChapterModel.destroy();
        DbHelper.getDaoSession().getBookContentBeanDao().deleteAll();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BackupRestoreUi.INSTANCE.onActivityResult(requestCode, resultCode, data);
        /*switch (requestCode) {
            case requestQR:
                if (resultCode == RESULT_OK) {
                    String result = data.getStringExtra("result");
                    if (!StringUtils.isTrimEmpty(result)) {
                        result=result.trim();
                        // 如果只有书源,则导入书源
                        if(result.replaceAll("(\\s|\n)*","").matches("^\\{.*$")) {
                            new BookSourcePresenter().importBookSource(result);
                            break;
                        }
                        mPresenter.addBookUrl(result);
                    }
                }
                break;
        }*/
    }
}
