package com.island.androidsftpdocumentsprovider;
import android.app.*;
import android.content.*;
import android.os.*;
import com.island.sftp.*;
import java.io.*;
import java.util.Objects;

public class UploaderService extends Service
{
	public static final String FOREGROUND_CHANNEL_ID="foreground";
	public static final int ONGOING_NOTIFICATION_ID=1;
	public static final String EXTRA_CACHE_DIR="cache_dir";
	public static final String EXTRA_TOKEN="token";
	public static final String EXTRA_NAME="name";
	public static final String EXTRA_FILE="file";
	@Override
	public int onStartCommand(Intent intent,int flags,int startId)
	{
		File file=new File(Objects.requireNonNull(intent.getStringExtra(EXTRA_FILE)));
		startForeground(ONGOING_NOTIFICATION_ID,getNotification(this,file.getName(),0));
		try
		{
			Cache cache=new Cache(new File(Objects.requireNonNull(intent.getStringExtra(EXTRA_CACHE_DIR))),intent.getStringExtra(EXTRA_NAME),Log.logger);
			AsyncCopy copy=new AsyncCopy(this,cache,intent.getStringExtra(EXTRA_TOKEN),AuthenticationActivity.TIMEOUT,Log.logger);
			copy.execute(file);
			return START_REDELIVER_INTENT;
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	static Notification getNotification(Context context,String title,int progress)
	{
		Notification.Builder builder;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
		{
			CharSequence name=context.getString(R.string.foreground);
			int importance=NotificationManager.IMPORTANCE_LOW;
			NotificationChannel channel=new NotificationChannel(FOREGROUND_CHANNEL_ID,name,importance);
			NotificationManager notificationManager=Objects.requireNonNull(context.getSystemService(NotificationManager.class));
			notificationManager.createNotificationChannel(channel);
			builder=new Notification.Builder(context,FOREGROUND_CHANNEL_ID);
		}
		else builder=new Notification.Builder(context);
		builder.setContentTitle(title);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)builder.setVisibility(Notification.VISIBILITY_PUBLIC);
		builder.setOngoing(true);
		builder.setContentText(getNotificationDescription(progress));
		builder.setSmallIcon(R.drawable.ic_stat_name);
		builder.setPriority(Notification.PRIORITY_LOW);
		builder.setProgress(100,progress,false);
		return builder.build();
	}
	private static String getNotificationDescription(long progress)
	{
		return String.format("Uploading: %s",progress)+"%";
	}
}
