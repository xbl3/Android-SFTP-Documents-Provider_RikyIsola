package com.island.androidftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.CompoundButton.*;
public class AuthenticationActivity extends AccountAuthenticatorActivity implements OnCheckedChangeListener
{
	public static final String LOG_TAG="FTPDocumentProvider";
	public static final String ACCOUNT_TYPE="com.island.ftp.account";
	public static final String TOKEN_TYPE="login";
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		Log.i(LOG_TAG,"Create authenticator activity");
		setContentView(R.layout.authentication_activity);
		Switch switchView=findViewById(R.id.anonymous);
		switchView.setOnCheckedChangeListener(this);
	}
	@Override
	public void onCheckedChanged(CompoundButton view,boolean checked)
	{
		Log.i(LOG_TAG,"Checked changed:"+checked);
		if(checked)
		{
			findViewById(R.id.credentials).setVisibility(View.GONE);
		}
	}
	public void confirm(View view)
	{
		Log.i(LOG_TAG,"Creating account");
		String accountType=getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
		Switch anonymous=findViewById(R.id.anonymous);
		EditText ip=findViewById(R.id.ip);
		EditText port=findViewById(R.id.port);
		String username=ip.getText()+":"+port.getText()+"@";
		String password;
		if(anonymous.isChecked())
		{
			username+="anonymous";
			password="anonymous";
		}
		else
		{
			EditText user=findViewById(R.id.user);
			EditText pw=findViewById(R.id.password);
			username+=user.getText().toString();
			password=pw.getText().toString();
		}
		String authToken=username+"/"+password;
		Account account=new Account(username,accountType);
		AccountManager accountManager=(AccountManager)getSystemService(ACCOUNT_SERVICE);
		if(accountManager!=null)accountManager.addAccountExplicitly(account,password,null);
		Bundle data=new Bundle();
        data.putString(AccountManager.KEY_ACCOUNT_NAME,username);
        data.putString(AccountManager.KEY_ACCOUNT_TYPE,accountType);
        data.putString(AccountManager.KEY_AUTHTOKEN,authToken);
        Intent result=new Intent();
        result.putExtras(data);
        setResult(RESULT_OK,result);
        finish();
		Log.i(LOG_TAG,"Created account "+username);
	}
}
