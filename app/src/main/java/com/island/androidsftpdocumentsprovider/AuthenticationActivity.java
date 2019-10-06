package com.island.androidsftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
public class AuthenticationActivity extends AccountAuthenticatorActivity
{
	public static final String LOG_TAG="SFTPDocumentProvider";
	public static final String ACCOUNT_TYPE="com.island.sftp.account";
	public static final String TOKEN_TYPE="login";
	public static final int TIMEOUT=20000;
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		//Set the layout
		Log.i(LOG_TAG,"Create authenticator activity");
		setContentView(R.layout.authentication_activity);
	}
	public void confirm(View view)
	{
		//Read the settings
		Log.i(LOG_TAG,"Creating account");
		String accountType=getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
		EditText ip=findViewById(R.id.ip);
		EditText port=findViewById(R.id.port);
		EditText user=findViewById(R.id.user);
		EditText pw=findViewById(R.id.password);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Read settings");
		
		//Register the account
		String username=ip.getText()+":"+port.getText()+"@"+user.getText().toString();
		String password=pw.getText().toString();
		String authToken=username+"/"+password;
		Account account=new Account(username,accountType);
		AccountManager accountManager=(AccountManager)getSystemService(ACCOUNT_SERVICE);
		if(accountManager!=null)accountManager.addAccountExplicitly(account,password,null);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added %s account %s to the account manager",accountType,username));
		
		//Stop the activity with the result account
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
