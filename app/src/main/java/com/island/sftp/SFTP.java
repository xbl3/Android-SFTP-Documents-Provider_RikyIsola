package com.island.sftp;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
public class SFTP implements Closeable,FileOperator
{
	//Token: username@host:port?password/start_directory
	public final String ip;
	public final int port;
	public final String user;
	public final String password;
	public final File initialPath;
	public final Session session;
	public final ChannelSftp channel;
	private final HashMap<File,Long>lastModified=new HashMap<>();
	private final HashMap<File,Long>size=new HashMap<>();
	private final HashMap<File,Boolean>directory=new HashMap<>();
	private final Logger logger;
	public SFTP(String token,int timeout,Logger logger)throws IOException
	{
		user=token.substring(0,token.indexOf("@"));
		ip=token.substring(token.indexOf("@")+1,token.indexOf(":"));
		port=Integer.valueOf(token.substring(token.indexOf(":")+1,token.indexOf("?")));
		password=token.substring(token.indexOf("?")+1,token.indexOf("/"));
		if(logger==null)logger=Logger.getLogger("SFTP");
		this.logger=logger;
		JSch jsch=new JSch();
		try
		{
			session=jsch.getSession(user,ip,port);
			session.setPassword(password);
			java.util.Properties config=new java.util.Properties();
			config.put("StrictHostKeyChecking","no");
			session.setConfig(config);
			session.setTimeout(timeout);
			session.setConfig("PreferredAuthentications","password");
			logger.fine(String.format("Connecting to %s@%s:%s",user,ip,port));
			session.connect();
			logger.fine(String.format("Connected to %s@%s:%s",user,ip,port));
			channel=(ChannelSftp)session.openChannel("sftp");
			logger.fine("Opening channel");
			channel.connect();
			logger.fine("Channel opened");
			File path;
			try
			{
				path=new File(new File(channel.getHome()),token.substring(token.indexOf("/")));
			}
			catch(SftpException e)
			{
				path=new File(token.substring(token.indexOf("/")));
			}
			initialPath=path;
		}
		catch(JSchException e)
		{
			throw new IOException(e);
		}
	}
	/**
	 * Return the file to use to get the content of the cache dir
	 * @param file The file to process
	 * @return The real file to use
	 */
	private String file(File file)
	{
		return new File(initialPath,file.getPath()).getPath();
	}
	/**
	 * Return the informations about the file
	 * @param map The map to process
	 * @param file The file to find
	 * @return The value of the map
	 * @throws IOException If any network error happen when requesting the file info
	 * @throws FileNotFoundException If the file doesn't exist
	 */
	private<T>T getValue(Map<File,T>map,File file)throws IOException
	{
		if(!map.containsKey(file))listFiles(file.getParentFile());
		if(!map.containsKey(file))throw new FileNotFoundException(String.format("File %s is missing",file));
		return map.get(file);
	}
	@Override
	public long lastModified(File file)throws IOException
	{
		logger.fine(String.format("Getting last modified of %s",file(file)));
		return getValue(lastModified,file);
	}
	@Override
	public long length(File file)throws IOException
	{
		logger.fine(String.format("Getting length of %s",file(file)));
		return getValue(size,file);
	}
	@Override
	public boolean isDirectory(File file)throws IOException
	{
		logger.fine(String.format("Getting if %s is directory",file(file)));
		return getValue(directory,file);
	}
	@Override
	public boolean isFile(File file)throws IOException
	{
		logger.fine(String.format("Getting if %s is directory",file(file)));
		return !getValue(directory,file);
	}
	@Override
	public void close()
	{
		logger.fine("Closing sftp connection");
		channel.quit();
		session.disconnect();
	}
	@Override
	public File[]listFiles(File file)throws IOException
	{
		logger.fine(String.format("Listing files of %s",file(file)));
		try
		{
			Vector vector=channel.ls(file(file));
			List<File>files=new ArrayList<>(vector.size()-2);
			for(Object obj:vector)
			{
				LsEntry entry=(LsEntry)obj;
				if(entry.getFilename().equals(".")||entry.getFilename().equals(".."))continue;
				File newFile=new File(file,entry.getFilename());
				SftpATTRS attributes=entry.getAttrs();
				files.add(newFile);
				lastModified.put(newFile,attributes.getMTime()*1000L);
				size.put(newFile,attributes.getSize());
				directory.put(newFile,attributes.isDir());
			}
			return files.toArray(new File[0]);
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public void newFile(File file)throws IOException
	{
		logger.fine(String.format("Creating file %s",file(file)));
		try
		{
			channel.put(file(file)).close();
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public void delete(File file)throws IOException
	{
		logger.fine(String.format("Deleting file %s",file(file)));
		try
		{
			if(isDirectory(file))
			{
				for(File child:listFiles(file))delete(child);
				channel.rmdir(file(file));
			}
			else channel.rm(file(file));
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public InputStream read(File file)throws IOException
	{
		logger.fine(String.format("Reading file %s",file(file)));
		try
		{
			return new BufferedInputStream(channel.get(file(file)));
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public void setLastModified(File file,long lastModified)throws IOException
	{
		try
		{
			channel.setMtime(file(file),(int)lastModified);
		}
		catch (SftpException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public void mkdirs(File file)throws IOException
	{
		try
		{
			channel.mkdir(file(file));
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public boolean exists(File file)throws IOException
	{
		try
		{
			getValue(directory,file);
			return true;
		}
		catch(FileNotFoundException e)
		{
			return false;
		}
	}
	@Override
	public void renameTo(File oldPath,File newPath)throws IOException
	{
		try
		{
			channel.rename(file(oldPath),file(newPath));
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public OutputStream write(File file)throws IOException
	{
		try
		{
			return channel.put(file(file));
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
}
