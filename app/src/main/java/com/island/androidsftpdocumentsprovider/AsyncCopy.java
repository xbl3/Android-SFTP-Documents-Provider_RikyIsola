package com.island.androidsftpdocumentsprovider;
import android.os.*;
import com.island.sftp.*;
import java.io.*;
import java.util.logging.*;
public class AsyncCopy extends AsyncTask<File,Void,Void>
{
	AsyncCopy(Cache cache,String token,int timeout,Logger logger)
	{
		this.cache=cache;
		this.token=token;
		this.timeout=timeout;
		this.logger=logger;
	}
	private final Cache cache;
	private final String token;
	private final int timeout;
	private final Logger logger;
	@Override
	protected Void doInBackground(File[]files)
	{
		try
		{
			try(SFTP sftp=new SFTP(token,timeout,logger))
			{
				for(File file:files)FileOperation.copy(cache,sftp,file);
			}
		}
		catch(IOException e)
		{
			Log.e("Can't asynchronously copy",e);
		}
		return null;
	}
}
