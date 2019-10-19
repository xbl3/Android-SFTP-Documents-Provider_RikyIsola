package com.island.sftp;
import android.content.*;
import java.io.*;
import java.util.logging.*;
public class Cache implements FileOperator
{
	private final File cache;
	public final String name;
	private final Logger logger;
	public Cache(File cacheDir,String name,Logger logger)
	{
		if(logger==null)logger=Logger.getLogger("cache");
		this.logger=logger;
		cache=new File(cacheDir,name);
		cache.mkdirs();
		this.name=name;
		logger.fine(String.format("Opened cache directory %s",cache));
	}
	public File file(File file)
	{
		return new File(cache,file.getPath());
	}
	@Override
	public long length(File file)throws IOException
	{
		logger.fine(String.format("Getting length of %s",file(file)));
		//if(!file.exists()||isDirectory(file))throw new IOException(String.format("Can't get the size of %s",file(file)));
		long length=file(file).length();
		return length;
	}
	@Override
	public long lastModified(File file)throws IOException
	{
		logger.fine(String.format("Getting last modified of %s",file(file)));
		long time=file(file).lastModified();
		if(time==0)throw new IOException(String.format("Can't get the last modification time of %s",file(file)));
		else return time;
	}
	@Override
	public boolean isDirectory(File file)throws IOException
	{
		logger.fine(String.format("Getting if %s is directory",file(file)));
		boolean directory=file(file).isDirectory();
		boolean isFile=file(file).isFile();
		if(directory==isFile)throw new IOException(String.format("Error getting if %s is a directory",file(file)));
		return directory;
	}
	@Override
	public File[]listFiles(File file)throws IOException
	{
		logger.fine(String.format("Listing files of %s",file(file)));
		String[]names=file(file).list();
		if(names==null)throw new IOException(String.format("Error listing files of %s",file));
		File[]files=new File[names.length];
		for(int a=0;a<files.length;a++)files[a]=new File(file,names[a]);
		return files;
	}
	@Override
	public void newFile(File file)throws IOException
	{
		logger.fine(String.format("Creating file %s",file(file)));
		mkdirs(file.getParentFile());
		if(!file(file).createNewFile())throw new IOException(String.format("File %s already exist",file));
	}
	@Override
	public void write(File file,InputStream input)throws IOException
	{
		logger.fine(String.format("Writing file %s",file(file)));
		mkdirs(file.getParentFile());
		OutputStream out=new BufferedOutputStream(new FileOutputStream(file(file)));
		while(true)
		{
			int read=input.read();
			if(read==-1)break;
			out.write(read);
		}
		out.close();
		input.close();
	}
	@Override
	public void delete(File file)throws IOException
	{
		logger.fine(String.format("Deleting file %s",file(file)));
		if(isDirectory(file))for(File child:listFiles(file))delete(child);
		if(!file(file).delete())throw new IOException(String.format("Can't delete file %s",file(file)));
	}
	@Override
	public InputStream read(File file)throws IOException
	{
		logger.fine(String.format("Reading file %s",file(file)));
		return new BufferedInputStream(new FileInputStream(file(file)));
	}
	@Override
	public void setLastModified(File file,long lastModified)throws IOException
	{
		logger.fine(String.format("Changing mod time of %s",file(file)));
		if(!file(file).setLastModified(lastModified))throw new IOException(String.format("Can't change mod time of %s",file(file)));
	}
	@Override
	public void mkdirs(File file)throws IOException
	{
		if(file(file).exists()&&isDirectory(file))return;
		if(!file(file).mkdirs())throw new IOException(String.format("Can't create the directory %s",file(file)));
	}
}
