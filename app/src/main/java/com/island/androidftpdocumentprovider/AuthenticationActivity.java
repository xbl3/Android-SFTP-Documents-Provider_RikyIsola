package com.island.androidftpdocumentprovider;
import android.accounts.*;
import android.os.*;
import android.util.*;
public class AuthenticationActivity extends AccountAuthenticatorActivity
{
	public static final String EXTRA_ADD_ACCOUNT="addAccount";
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		Log.i(MainActivity.LOG_TAG,"Create authenticator activity");
		setContentView(R.layout.authentication_activity);
	}
}
