package com.island.androidsftpdocumentsprovider;
import android.content.*;
import java.io.*;
public class Cache implements FileOperator
{
	private final File cache;
	Cache(Context context)
	{
		cache=context.getExternalCacheDir();
	}
	@Override
	public long length(File file)
	{
		return new File(cache,file.getPath()).length();
	}
	@Override
	public long lastModified(File file)
	{
		
		return new File(cache,file.getPath()).lastModified();
	}
	@Override
	public boolean isDirectory(File file)
	{
		return new File(cache,file.getPath()).isDirectory();
	}
}
