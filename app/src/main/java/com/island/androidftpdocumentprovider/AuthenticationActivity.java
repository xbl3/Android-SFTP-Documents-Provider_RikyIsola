package com.island.androidftpdocumentprovider;
import android.accounts.*;
import android.os.*;
import android.util.*;
public class AuthenticationActivity extends AccountAuthenticatorActivity
{
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		Log.i(MainActivity.LOG_TAG,"Create authenticator activity");
	}
}
