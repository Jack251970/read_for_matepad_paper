package com.jack.bookshelf.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.hwangjr.rxbus.RxBus;
import com.jack.basemvplib.BitIntentDataManager;
import com.jack.bookshelf.R;
import com.jack.bookshelf.base.MBaseActivity;
import com.jack.bookshelf.base.observer.MySingleObserver;
import com.jack.bookshelf.bean.BookShelfBean;
import com.jack.bookshelf.bean.ReplaceRuleBean;
import com.jack.bookshelf.constant.RxBusTag;
import com.jack.bookshelf.databinding.ActivityRecyclerVewBinding;
import com.jack.bookshelf.help.ItemTouchCallback;
import com.jack.bookshelf.help.permission.Permissions;
import com.jack.bookshelf.help.permission.PermissionsCompat;
import com.jack.bookshelf.model.ReplaceRuleManager;
import com.jack.bookshelf.presenter.ReplaceRulePresenter;
import com.jack.bookshelf.presenter.contract.ReplaceRuleContract;
import com.jack.bookshelf.utils.ACache;
import com.jack.bookshelf.utils.RealPathUtil;
import com.jack.bookshelf.utils.StringUtils;
import com.jack.bookshelf.utils.theme.ATH;
import com.jack.bookshelf.utils.theme.ThemeStore;
import com.jack.bookshelf.view.adapter.ReplaceRuleAdapter;
import com.jack.bookshelf.widget.filepicker.picker.FilePicker;
import com.jack.bookshelf.widget.modialog.InputDialog;
import com.jack.bookshelf.widget.modialog.MoDialogHUD;
import com.jack.bookshelf.widget.modialog.ReplaceRuleDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Unit;

/**
 * 替换净化界面
 * Created by GKF on 2017/12/16.
 * Edited by Jack251970
 */

public class ReplaceRuleActivity extends MBaseActivity<ReplaceRuleContract.Presenter> implements ReplaceRuleContract.View {
    private final int IMPORT_SOURCE = 102;

    private ActivityRecyclerVewBinding binding;
    private BookShelfBean bookShelfBean;
    private MoDialogHUD moDialogHUD;
    private ReplaceRuleAdapter adapter;
    private boolean selectAll = true;

    public static void startThis(Context context, BookShelfBean shelfBean) {
        String key = String.valueOf(System.currentTimeMillis());
        Intent intent = new Intent(context, ReplaceRuleActivity.class);
        BitIntentDataManager.getInstance().putData(key, shelfBean);
        intent.putExtra("data_key", key);
        context.startActivity(intent);
    }

    @Override
    protected ReplaceRuleContract.Presenter initInjector() {
        return new ReplaceRulePresenter();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onCreateActivity() {
        getWindow().getDecorView().setBackgroundColor(ThemeStore.backgroundColor(this));
        binding = ActivityRecyclerVewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void initData() {
        String dataKey = getIntent().getStringExtra("data_key");
        if (!TextUtils.isEmpty(dataKey)) {
            bookShelfBean = (BookShelfBean) BitIntentDataManager.getInstance().getData(dataKey);
        }
        this.setSupportActionBar(binding.toolbar);
        setupActionBar();
        initRecyclerView();
        moDialogHUD = new MoDialogHUD(this);
        refresh();
    }

    private void initRecyclerView() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));
        adapter = new ReplaceRuleAdapter(this);
        binding.recyclerView.setAdapter(adapter);
        ItemTouchCallback itemTouchCallback = new ItemTouchCallback();
        itemTouchCallback.setOnItemTouchCallbackListener(adapter.getItemTouchCallbackListener());
        itemTouchCallback.setDragEnable(true);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerView);
    }

    public void editReplaceRule(ReplaceRuleBean replaceRuleBean) {
        ReplaceRuleDialog.builder(this, replaceRuleBean, bookShelfBean)
                .setPositiveButton(replaceRuleBean1 ->
                        ReplaceRuleManager.saveData(replaceRuleBean1)
                                .subscribe(new MySingleObserver<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        refresh();
                                    }
                                })).show();
    }

    public void upDateSelectAll() {
        selectAll = true;
        for (ReplaceRuleBean replaceRuleBean : adapter.getData()) {
            if (replaceRuleBean.getEnable() == null || !replaceRuleBean.getEnable()) {
                selectAll = false;
                break;
            }
        }
    }

    private void selectAllDataS() {
        for (ReplaceRuleBean replaceRuleBean : adapter.getData()) {
            replaceRuleBean.setEnable(!selectAll);
        }
        adapter.notifyDataSetChanged();
        selectAll = !selectAll;
        ReplaceRuleManager.addDataS(adapter.getData());
    }

    public void delData(ReplaceRuleBean replaceRuleBean) {
        mPresenter.delData(replaceRuleBean);
    }

    public void saveDataS() {
        mPresenter.saveData(adapter.getData());
    }

    //设置ToolBar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.replace_rule_title);
        }
    }

    // 添加菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_replace_rule_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_replace_rule:
                editReplaceRule(null);
                break;
            case R.id.action_select_all:
                selectAllDataS();
                break;
            case R.id.action_import_replace_rule_local:
                selectReplaceRuleFile();
                break;
            case R.id.action_del_all_replace_rule:
                deleteAllDialog();
                break;
            case R.id.action_import_replace_rule_online:
                String cacheUrl = ACache.get(this).getAsString("replaceUrl");
                String[] cacheUrls = cacheUrl == null ? new String[]{} : cacheUrl.split(";");
                List<String> urlList = new ArrayList<>(Arrays.asList(cacheUrls));
                InputDialog.builder(this)
                        .setTitle(getString(R.string.input_replace_url))
                        .setDefaultValue(cacheUrl)
                        .setAdapterValues(urlList)
                        .setCallback(new InputDialog.Callback() {
                            @Override
                            public void setInputText(String inputText) {
                                inputText = StringUtils.trim(inputText);
                                if (!urlList.contains(inputText)) {
                                    urlList.add(0, inputText);
                                    ACache.get(ReplaceRuleActivity.this).put("replaceUrl", TextUtils.join(";", urlList));
                                }
                                mPresenter.importDataS(inputText);
                            }

                            @Override
                            public void delete(String value) {

                            }
                        }).show();
                break;
            default:
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    // 删除所有替换规则的消息框
    private void deleteAllDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.del_all_msg)
                .setPositiveButton(R.string.ok, (dialog, which) -> mPresenter.delData(adapter.getData()))
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                })
                .show();
        ATH.setAlertDialogTint(alertDialog);
    }

    private void selectReplaceRuleFile() {
        new PermissionsCompat.Builder(this)
                .addPermissions(Permissions.READ_EXTERNAL_STORAGE, Permissions.WRITE_EXTERNAL_STORAGE)
                .rationale(R.string.need_storage_permission_to_backup_book_information)
                .onGranted((requestCode) -> {
                    FilePicker filePicker = new FilePicker(ReplaceRuleActivity.this, FilePicker.FILE);
                    filePicker.setBackgroundColor(getResources().getColor(R.color.background));
                    filePicker.setTopBackgroundColor(getResources().getColor(R.color.background));
                    filePicker.setItemHeight(30);
                    filePicker.setAllowExtensions(getResources().getStringArray(R.array.text_suffix));
                    filePicker.setOnFilePickListener(s -> mPresenter.importDataSLocal(s));
                    filePicker.show();
                    filePicker.getSubmitButton().setText(R.string.sys_file_picker);
                    filePicker.getSubmitButton().setOnClickListener(view -> {
                        filePicker.dismiss();
                        selectFileSys();
                    });
                    return Unit.INSTANCE;
                })
                .request();
    }

    private void selectFileSys() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*", "application/json"});
        intent.setType("*/*");//设置类型
        startActivityForResult(intent, IMPORT_SOURCE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Boolean mo = moDialogHUD.onKeyDown(keyCode, event);
        if (mo) {
            return true;
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_SOURCE) {
            if (data != null) {
                mPresenter.importDataSLocal(RealPathUtil.getPath(this, data.getData()));
            }
        }
    }

    @Override
    public void refresh() {
        ReplaceRuleManager.getAll()
                .subscribe(new MySingleObserver<List<ReplaceRuleBean>>() {
                    @Override
                    public void onSuccess(List<ReplaceRuleBean> replaceRuleBeans) {
                        adapter.resetDataS(replaceRuleBeans);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        RxBus.get().post(RxBusTag.UPDATE_READ, false);
        super.onDestroy();
    }

    @Override
    public Snackbar getSnackBar(String msg, int length) {
        return Snackbar.make(binding.llContent, msg, length);
    }
}
