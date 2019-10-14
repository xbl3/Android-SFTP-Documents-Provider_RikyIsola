package com.island.sftp;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Logger;
public class SFTP implements Closeable,FileOperator
{
	//Token: username@host:port?password/startdirectory
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
		initialPath=new File(token.substring(token.indexOf("/")));
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
		}
		catch(JSchException e)
		{
			throw new IOException(e);
		}
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
	public void close()
	{
		logger.fine(String.format("Closing sftp connection"));
		channel.quit();
		session.disconnect();
	}
	@Override
	public File[]listFiles(File file)throws IOException
	{
		logger.fine(String.format("Listing files of %s",file(file)));
		try
		{
			Vector vector=channel.ls(new File(initialPath,file.getPath()).getPath());
			List<File>files=new ArrayList<>(vector.size()-2);
			for(Object obj:vector)
			{
				LsEntry entry=(LsEntry)obj;
				if(entry.getFilename().equals(".")||entry.getFilename().equals(".."))continue;
				File newFile=new File(file,entry.getFilename());
				SftpATTRS attributes=entry.getAttrs();
				files.add(newFile);
				lastModified.put(newFile,(long)attributes.getMTime());
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
	public void write(File file,InputStream input)throws IOException
	{
		logger.fine(String.format("Writing file %s",file(file)));
		try
		{
			OutputStream out=new BufferedOutputStream(channel.put(file(file)));
			while(true)
			{
				int read=input.read();
				if(read==-1)break;
				out.write(read);
			}
			input.close();
			out.close();
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
			if(isDirectory(file))channel.rmdir(file(file));
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
	private String file(File file)
	{
		return new File(initialPath,file.getPath()).getPath();
	}
	private<T>T getValue(Map<File,T>map,File file)throws IOException
	{
		if(!map.containsKey(file))listFiles(file.getParentFile());
		if(!map.containsKey(file))throw new FileNotFoundException(String.format("File %s is missing",file));
		return map.get(file);
	}
}
