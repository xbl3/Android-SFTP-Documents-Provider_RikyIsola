package com.island.androidsftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.widget.*;
public class AuthenticationActivity extends AccountAuthenticatorActivity
{
	public static final String ACCOUNT_TYPE="com.island.sftp.account";
	public static final String TOKEN_TYPE="login";
	public static final String START_DIRECTORY="start_directory";
	public static final String HIDDEN_FOLDERS="hidden_folders";
	public static final String AUTHORITY="com.island.androidsftpdocumentsprovider";
	public static final int TIMEOUT=20000;
	public static final int MAX_LAST_MODIFIED=5;
	public static final int MAX_SEARCH_RESULT=20;
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		//Set the layout
		Log.i("Create authenticator activity");
		setContentView(R.layout.authentication_activity);
	}
	public void confirm(View view)
	{
		//Read the settings
		Log.i("Creating account");
		String accountType=getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
		EditText ip=findViewById(R.id.ip);
		EditText port=findViewById(R.id.port);
		EditText user=findViewById(R.id.user);
		EditText startDirectory=findViewById(R.id.start_directory);
		EditText pw=findViewById(R.id.password);
		Switch hiddenFolders=findViewById(R.id.hiddenfolders);
		Log.d("Read settings");
		
		//Register the account
		String username=user.getText()+"@"+ip.getText()+":"+port.getText();
		String password=pw.getText().toString();
		String authToken=username+"/"+password;
		Account account=new Account(username,accountType);
		AccountManager accountManager=(AccountManager)getSystemService(ACCOUNT_SERVICE);
		if(accountManager!=null)
		{
			Bundle userdata=new Bundle();
			userdata.putString(START_DIRECTORY,startDirectory.getText().toString());
			userdata.putBoolean(HIDDEN_FOLDERS,hiddenFolders.isActivated());
			accountManager.addAccountExplicitly(account,password,userdata);
			ContentResolver.setSyncAutomatically(account,AUTHORITY,true);
		}
		Log.d(String.format("Added %s account %s to the account manager",accountType,username));
		
		//Add the document provider to the file manager list
		getContentResolver().notifyChange(DocumentsContract.buildRootsUri(AUTHORITY),null);
		
		//Stop the activity with the result account
		Bundle data=new Bundle();
        data.putString(AccountManager.KEY_ACCOUNT_NAME,username);
        data.putString(AccountManager.KEY_ACCOUNT_TYPE,accountType);
        data.putString(AccountManager.KEY_AUTHTOKEN,authToken);
        Intent result=new Intent();
        result.putExtras(data);
        setResult(RESULT_OK,result);
        finish();
		Log.i("Created account "+username);
	}
}
