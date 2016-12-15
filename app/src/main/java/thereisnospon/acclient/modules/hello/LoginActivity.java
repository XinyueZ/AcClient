package thereisnospon.acclient.modules.hello;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.transition.AutoTransition;
import android.support.transition.Scene;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import thereisnospon.acclient.R;
import thereisnospon.acclient.databinding.ActivityHelloSceneLoginBinding;
import thereisnospon.acclient.databinding.LoginActivityBinding;
import thereisnospon.acclient.event.Arg;
import thereisnospon.acclient.modules.problem.list.HdojActivity;
import thereisnospon.acclient.utils.SpUtil;
import thereisnospon.acclient.widget.TransitionListenerAdapter;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.os.Bundle.EMPTY;
import static pub.devrel.easypermissions.AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE;
import static thereisnospon.acclient.modules.hello.ErrorConstants.NO_EMPTY_PASSWORD;
import static thereisnospon.acclient.modules.hello.ErrorConstants.NO_EMPTY_USERNAME;

public final class LoginActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks {
	private static final int LAYOUT = R.layout.activity_hello;
	private static final int DURATION = 500;
	private Scene login;

	private String id;
	private String pass;
	private String nickname;
	private volatile boolean rememberPas = false;
	private volatile boolean isShowLoginUI = false;

	private LoginActivityBinding mBinding;
	private ActivityHelloSceneLoginBinding mLoginBinding;


	public static void showInstance(Activity cxt, boolean relogin) {
		Intent intent = new Intent(cxt, LoginActivity.class);
		intent.putExtra(Arg.RE_LOGIN, relogin);
		intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivity(cxt, intent, EMPTY);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("id", id);
		outState.putString("pass", pass);
		outState.putString("nickname", nickname);
		outState.putBoolean("rememberPas", rememberPas);
		outState.putBoolean("isShowLoginUI", isShowLoginUI);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		id = savedInstanceState.getString("id");
		pass = savedInstanceState.getString("pass");
		nickname = savedInstanceState.getString("nickname");
		rememberPas = savedInstanceState.getBoolean("rememberPas");
		isShowLoginUI = savedInstanceState.getBoolean("isShowLoginUI");
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		presenter = new HelloPresenter(this);
		initScene();
		initView();
		Intent intent = getIntent();
		if (intent.hasExtra(Arg.RE_LOGIN)) {
			reLogin();
		} else {
			tryLogin();
		}

		//When permissions are not granted , this app can not be used.
		requirePermissions();
	}

	private void initScene() {
		Scene.getSceneForLayout(mBinding.sceneRoot, R.layout.activity_hello_scene_index, this);
		mLoginBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_hello_scene_login, mBinding.sceneRoot, false);
		mLoginBinding.setDoneListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					mLoginBinding.loginButton.performClick();
					return true;
				}
				return false;
			}
		});
		login = new Scene(mBinding.sceneRoot, mLoginBinding.getRoot());
	}


	private void initView() {
		mLoginBinding.registerButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				RegisterActivity.showInstance(LoginActivity.this);
			}
		});
		mLoginBinding.loginRemember.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mLoginBinding.loginRemember.setChecked(!mLoginBinding.loginRemember.isChecked());

				rememberPas = mLoginBinding.loginRemember.isChecked();
				if (rememberPas) {
					Snackbar.make(mBinding.sceneRoot, R.string.hello_remember_password, Snackbar.LENGTH_SHORT)
					        .setAction(R.string.cancel, new View.OnClickListener() {
						        @Override
						        public void onClick(View v) {
							        mLoginBinding.loginRemember.setChecked(false);
						        }
					        })
					        .show();
				}
			}
		});
		mLoginBinding.loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LoginActivity.this.id = mLoginBinding.loginId.getText()
				                                             .toString();
				LoginActivity.this.pass = mLoginBinding.loginPass.getText()
				                                                 .toString();
				presenter.login(id, pass);
			}
		});
	}


	private void tryLogin() {
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (checkRemembered()) {
					presenter.login(id, pass);
				} else {
					showLoginUI();
				}
			}
		}, 1000);
	}

	private void reLogin() {
		showLoginUI();
	}


	private boolean checkRemembered() {
		SpUtil sp = SpUtil.getInstance();
		id = sp.getString(SpUtil.NAME);
		pass = sp.getString(SpUtil.PASS);
		nickname = sp.getString(SpUtil.NICKNAME);
		rememberPas = !TextUtils.isEmpty(id) && !TextUtils.isEmpty(pass);
		return rememberPas;
	}


	private void showLoginUI() {
		isShowLoginUI = true;
		TransitionSet transitionSet = new TransitionSet();
		transitionSet.addTransition(new AutoTransition());
		transitionSet.setDuration(DURATION);
		transitionSet.addListener(new TransitionListenerAdapter() {
			@Override
			public void onTransitionEnd(Transition transition) {
				super.onTransitionEnd(transition);
				initView();
			}
		});
		TransitionManager.go(login, transitionSet);
	}


	@Override
	public void onLoginSuccess(String userName) {
		mLoadToast.success();
		SpUtil sp = SpUtil.getInstance();
		nickname = userName;
		if (rememberPas) {
			sp.putString(SpUtil.NAME, id);
			sp.putString(SpUtil.PASS, pass);
			sp.putString(SpUtil.NICKNAME, nickname);
		} else {
			sp.clear();
			sp.putString(SpUtil.NAME, id);
			sp.putString(SpUtil.NICKNAME, nickname);
		}
		HdojActivity.showInstance(this);
		ActivityCompat.finishAfterTransition(this);
	}

	@Override
	public void onLoginFailure(String error) {
		if (!isShowLoginUI) {
			showLoginUI();
			mLoadToast.error();
		} else {
			Snackbar.make(mBinding.sceneRoot, error, Snackbar.LENGTH_LONG)
			        .show();
			mLoadToast.setText(getApplicationContext().getString(R.string.hello_login_unsuccessfully));
			mLoadToast.error();
		}
	}


	@Override
	public void onUserInputFailure(String error, @ErrorConstants.Value int errorType) {
		switch (errorType) {
			case NO_EMPTY_USERNAME:
				mLoginBinding.loginIdContainer.setError(error);
				break;
			case NO_EMPTY_PASSWORD:
				mLoginBinding.loginPassContainer.setError(error);
				break;
			case ErrorConstants.PASSWORD_NOT_EQUAL:
			case ErrorConstants.PASSWORD_SHORT:
			case ErrorConstants.WRONG_EMAIL:
			case ErrorConstants.REGISTER_UNSUCCESSFULLY:
				break;
		}
		mLoadToast.setText(error);
		mLoadToast.error();
	}


	private void updateUIWhenLogin(boolean enabled) {
		mLoginBinding.loginId.setEnabled(enabled);
		mLoginBinding.loginIdContainer.setEnabled(enabled);
		mLoginBinding.loginPass.setEnabled(enabled);
		mLoginBinding.loginPassContainer.setEnabled(enabled);
		mLoginBinding.loginButton.setEnabled(enabled);
		mLoginBinding.registerButton.setEnabled(enabled);
		mLoginBinding.loginRemember.setEnabled(enabled);
	}

	@Override
	public void beforeLogin() {
		super.beforeLogin();
		updateUIWhenLogin(false);
	}


	@Override
	public void afterLogin() {
		super.afterLogin();
		updateUIWhenLogin(true);
	}


	private static final int RC_PERMISSIONS = 123;

	@SuppressLint("InlinedApi")
	private boolean hasPermissions() {
		return EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE);
	}


	@SuppressLint("InlinedApi")
	@AfterPermissionGranted(RC_PERMISSIONS)
	private void requirePermissions() {
		if (hasPermissions()) {
			updateUIWhenLogin(true);
		} else {
			// Ask for one permission
			EasyPermissions.requestPermissions(this,
			                                   getString(R.string.rationale_permissions),
			                                   RC_PERMISSIONS,
			                                   Manifest.permission.WRITE_EXTERNAL_STORAGE,
			                                   Manifest.permission.READ_EXTERNAL_STORAGE,
			                                   Manifest.permission.READ_PHONE_STATE);
		}
	}


	@Override
	public void onPermissionsGranted(int requestCode, List<String> perms) {
		updateUIWhenLogin(true);
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
	}


	@Override
	public void onPermissionsDenied(int requestCode, List<String> perms) {
		permissionsDeniedOpenSetting();
	}


	private void permissionsDeniedOpenSetting() {
		if (!hasPermissions()) {
			new AppSettingsDialog.Builder(this, getString(R.string.app_settings_dialog_rationale_ask_again)).setTitle(getString(R.string.app_settings_dialog_title_settings_dialog))
			                                                                                                .setPositiveButton(getString(R.string.app_settings_dialog_setting))
			                                                                                                .setNegativeButton(getString(R.string.app_settings_dialog_cancel),
			                                                                                                                   new DialogInterface.OnClickListener() {
				                                                                                                                   @Override
				                                                                                                                   public void onClick(DialogInterface dialogInterface, int i) {
					                                                                                                                   ActivityCompat.finishAffinity(LoginActivity.this);
				                                                                                                                   }
			                                                                                                                   })
			                                                                                                .build()
			                                                                                                .show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case DEFAULT_SETTINGS_REQ_CODE:
				updateUIWhenLogin(hasPermissions());
				permissionsDeniedOpenSetting();
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
