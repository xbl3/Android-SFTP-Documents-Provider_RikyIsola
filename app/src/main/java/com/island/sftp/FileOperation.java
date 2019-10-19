package com.island.sftp;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
public class FileOperation implements Callable<Exception>
{
	private FileOperation(FileOperator from,FileOperator to,File source,File destination,boolean move)
	{
		this.from=from;
		this.to=to;
		this.source=source;
		this.destination=destination;
		this.move=move;
	}
	private final FileOperator from;
	private final FileOperator to;
	private final File source;
	private final File destination;
	private final boolean move;
	public static void copy(FileOperator from,FileOperator to,File file)throws IOException
	{
		copy(from,to,file,file);
	}
	public static void copy(FileOperator from,FileOperator to,File source,File destination)throws IOException
	{
		to.write(destination,from.read(source));
	}
	public static void copy(FileOperator fo,File source,File destination)throws IOException
	{
		copy(fo,fo,source,destination);
	}
	public static void move(FileOperator from,FileOperator to,File file)throws IOException
	{
		move(from,to,file,file);
	}
	public static void move(FileOperator from,FileOperator to,File source,File destination)throws IOException
	{
		copy(from,to,source,destination);
		from.delete(source);
	}
	public static void move(FileOperator fo,File source,File destination)throws IOException
	{
		move(fo,source,destination);
	}
	private static Future<Exception>asyncOperation(FileOperator from,FileOperator to,File source,File destination,boolean move)
	{
		ExecutorService server=Executors.newSingleThreadExecutor();
		Future<Exception>future=server.submit(new FileOperation(from,to,source,destination,move));
		server.shutdown();
		return future;
	}
	public static Future<Exception>asyncCopy(FileOperator from,FileOperator to,File source,File destination)
	{
		return asyncOperation(from,to,source,destination,false);
	}
	public static Future<Exception>asyncCopy(FileOperator from,FileOperator to,File file)
	{
		return asyncCopy(from,to,file,file);
	}
	public static Future<Exception>asyncCopy(FileOperator fo,File source,File destination)
	{
		return asyncCopy(fo,fo,source,destination);
	}
	public static Future<Exception>asyncMove(FileOperator from,FileOperator to,File source,File destination)
	{
		return asyncOperation(from,to,source,destination,true);
	}
	public static Future<Exception>asyncMove(FileOperator from,FileOperator to,File file)
	{
		return asyncMove(from,to,file,file);
	}
	public static Future<Exception>asyncMove(FileOperator fo,File source,File destination)
	{
		return asyncMove(fo,fo,source,destination);
	}
	public static Queue<File>recent(final FileOperator fo,int maxResult)throws IOException
	{
		Queue<File>lastModifiedFiles=new PriorityQueue<File>(5,new Comparator<File>()
			{
				public int compare(File i,File j)
				{
					try
					{
						return Long.compare(fo.lastModified(i),fo.lastModified(j));
					}
					catch(IOException e)
					{
						throw new RuntimeException(e);
					}
				}
			}
		);
        LinkedList<File>pending=new LinkedList<File>();
        pending.add(new File("/"));
        while(!pending.isEmpty())
		{
            File file=pending.removeFirst();
            if(fo.isDirectory(file))
			{
                Collections.addAll(pending,fo.listFiles(file));
            }
			else
			{
                lastModifiedFiles.add(file);
            }
        }
		return lastModifiedFiles;
	}
	public static List<File>search(FileOperator fo,String query,int maxResult)throws IOException
	{
        LinkedList<File>pending=new LinkedList<File>();
		List<File>result=new ArrayList<File>(maxResult);
        pending.add(new File("/"));
        while(!pending.isEmpty()&&result.size()<maxResult)
		{
            final File file=pending.removeFirst();
            if(fo.isDirectory(file))
			{
                Collections.addAll(pending,fo.listFiles(file));
            }
			else
			{
                if(file.getName().toLowerCase().contains(query))
				{
                    result.add(file);
                }
            }
        }
		return result;
	}
	@Override
	public Exception call()throws Exception
	{
		try
		{
			if(move)move(from,to,source,destination);
			else copy(from,to,source,destination);
			return null;
		}
		catch(IOException e)
		{
			return e;
		}
	}
}
