package com.island.androidftpdocumentprovider;
import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.content.*;
public class MainActivity extends Activity
{
	public static final String CONFIG="CONFIG";
	public static final String IP="IP";
	public static final String PORT="PORT";
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	public void save(View v)
	{
		EditText ip=findViewById(R.id.ip);
		EditText port=findViewById(R.id.port);
		SharedPreferences.Editor spe=getSharedPreferences(CONFIG,0).edit();
		spe.putString(IP,ip.getText().toString());
		spe.putInt(PORT,Integer.valueOf(port.getText().toString()));
		spe.apply();
	}
}
