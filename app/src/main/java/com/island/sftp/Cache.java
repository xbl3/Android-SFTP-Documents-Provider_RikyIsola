package com.island.sftp;
import android.content.*;
import java.io.*;
import java.util.logging.*;
public class Cache implements FileOperator
{
	private final File cache;
	public final String name;
	private final Logger logger;
	public Cache(Context context,String name,Logger logger)
	{
		if(logger==null)logger=Logger.getLogger("cache");
		this.logger=logger;
		cache=new File(context.getExternalCacheDir(),name);
		cache.mkdirs();
		this.name=name;
		logger.info(String.format("Opened cache directory %s",cache));
	}
	private File file(File file)
	{
		return new File(cache,file.getPath());
	}
	@Override
	public long length(File file)
	{
		logger.info(String.format("Getting length of %s",file(file)));
		return file(file).length();
	}
	@Override
	public long lastModified(File file)
	{
		logger.info(String.format("Getting last modified of %s",file(file)));
		return file(file).lastModified();
	}
	@Override
	public boolean isDirectory(File file)
	{
		logger.info(String.format("Getting if %s is directory",file(file)));
		return file(file).isDirectory();
	}
	@Override
	public File[]listFiles(File file)
	{
		logger.info(String.format("Listing files of %s",file(file)));
		String[]names=file(file).list();
		File[]files=new File[names.length];
		for(int a=0;a<files.length;a++)files[a]=new File(file,names[a]);
		return files;
	}
	@Override
	public void newFile(File file)throws IOException
	{
		logger.info(String.format("Creating file %s",file(file)));
		file=file(file);
		File parent=file.getParentFile();
		if(!parent.exists()&&parent.mkdirs())throw new IOException(String.format("Can't create folder %s",file.getParentFile()));
		if(!file.createNewFile())throw new IOException(String.format("File %s already exist",file));
	}
	@Override
	public void write(File file,InputStream input)throws IOException
	{
		logger.info(String.format("Writing file %s",file(file)));
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
		logger.info(String.format("Deleting file %s",file(file)));
		if(!file(file).delete())throw new IOException(String.format("Can't delete file %s",file(file)));
	}
	@Override
	public InputStream read(File file)throws IOException
	{
		logger.info(String.format("Reading file %s",file(file)));
		return new BufferedInputStream(new FileInputStream(file(file)));
	}
}
