package com.island.sftp;
import java.io.*;
import java.util.logging.*;
public class Cache implements FileOperator
{
	private final File cache;
	public final String name;
	private final Logger logger;
	public Cache(File cacheDir,String name,Logger logger)
	{
		if(logger==null)logger=Logger.getLogger("Cache");
		this.logger=logger;
		cache=new File(cacheDir,name);
		cache.mkdirs();
		this.name=name;
		logger.fine(String.format("Opened cache directory %s",cache));
	}
	/**
	 * Return the file to use to get the content of the cache dir
	 * @param file The file to process
	 * @return The real file to use
	 */
	public File file(File file)
	{
		return new File(cache,file.getPath());
	}
	/**
	 * Thrown an exception if the file doesn't exists
	 * @param file The file to process
	 * @throw FileNotFoundException If the file doesn't exists
	 */
	private void existsThrow(File file)throws FileNotFoundException
	{
		if(!exists(file))throw new FileNotFoundException(String.format("File %s doesn't exists",file));
	}
	/**
	 * Thrown an exception if the file exists
	 * @param file The file to process
	 * @throw FileNotFoundException If the file exists
	 */
	private void notExistsThrow(File file)throws IOException
	{
		if(exists(file))throw new IOException(String.format("File %s already exists",file));
	}
	@Override
	public long length(File file)throws IOException
	{
		//Check if the file exists and isn't a directory
		logger.fine(String.format("Getting length of %s",file(file)));
		if(isDirectory(file))throw new IOException(String.format("Can't get the size of a directory %s",file(file)));
		long length=file(file).length();
		return length;
	}
	@Override
	public long lastModified(File file)throws IOException
	{
		//Check if the file exists and is a file
		logger.fine(String.format("Getting last modified of %s",file(file)));
		existsThrow(file);
		long time=file(file).lastModified();
		return time;
	}
	@Override
	public boolean isDirectory(File file)throws IOException
	{
		//Check if the file exists and there aren't any errors
		logger.fine(String.format("Getting if %s is directory",file(file)));
		existsThrow(file);
		boolean directory=file(file).isDirectory();
		boolean isFile=file(file).isFile();
		if(directory==isFile)throw new IOException(String.format("Error getting if %s is a directory",file(file)));
		return directory;
	}
	@Override
	public boolean isFile(File file)throws IOException
	{
		//Check if the file exists and there aren't any errors
		logger.fine(String.format("Getting if %s is directory",file(file)));
		existsThrow(file);
		boolean directory=file(file).isDirectory();
		boolean isFile=file(file).isFile();
		if(directory==isFile)throw new IOException(String.format("Error getting if %s is a directory",file(file)));
		return isFile;
	}
	@Override
	public File[]listFiles(File file)throws IOException
	{
		//Check if the file is a folder, exists and there are any errors
		logger.fine(String.format("Listing files of %s",file(file)));
		isDirectory(file);
		String[]names=file(file).list();
		if(names==null)throw new IOException(String.format("Error listing files of %s",file));
		File[]files=new File[names.length];
		for(int a=0;a<files.length;a++)files[a]=new File(file,names[a]);
		return files;
	}
	@Override
	public void newFile(File file)throws IOException
	{
		//Check if the file already exists, creates its directory and check if there are any errors
		logger.fine(String.format("Creating file %s",file(file)));
		notExistsThrow(file);
		if(!exists(file.getParentFile()))mkdirs(file.getParentFile());
		if(!file(file).createNewFile())throw new IOException(String.format("Can't create %s",file));
	}
	@Override
	public void delete(File file)throws IOException
	{
		//Check if the file exist and that there aren't any errors
		logger.fine(String.format("Deleting file %s",file(file)));
		existsThrow(file);
		if(isDirectory(file))for(File child:listFiles(file))delete(child);
		if(!file(file).delete())throw new IOException(String.format("Can't delete file %s",file(file)));
	}
	@Override
	public InputStream read(File file)throws IOException
	{
		//Check if the file isn't a directory and exists
		logger.fine(String.format("Reading file %s",file(file)));
		if(isDirectory(file))throw new IOException(String.format("Can't read a folder %s"));
		return new BufferedInputStream(new FileInputStream(file(file)));
	}
	@Override
	public void setLastModified(File file,long lastModified)throws IOException
	{
		//Check if the file exists
		existsThrow(file);
		logger.fine(String.format("Changing mod time of %s",file(file)));
		if(!file(file).setLastModified(lastModified))throw new IOException(String.format("Can't change mod time of %s",file(file)));
	}
	@Override
	public void mkdirs(File file)throws IOException
	{
		//Check if the file doesn't exists and if there are any errors
		notExistsThrow(file);
		if(!file(file).mkdirs())throw new IOException(String.format("Can't create the directory %s",file(file)));
	}
	@Override
	public boolean exists(File file)
	{
		//Check if the file exists
		return file(file).exists();
	}
	@Override
	public void renameTo(File oldPath,File newPath) throws IOException
	{
		exists(oldPath);
		notExistsThrow(newPath);
		if(!file(oldPath).renameTo(file(newPath)))throw new IOException("Can't rename the file");
	}
	@Override
	public OutputStream write(File file)throws IOException
	{
		if(!exists(file))newFile(file);
		return new BufferedOutputStream(new FileOutputStream(file(file)));
	}
}
