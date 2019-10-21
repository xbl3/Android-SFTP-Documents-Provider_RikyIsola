package com.island.sftp;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
public class FileOperation
{
	/**
	 * Copy the content of a file from one place on another even if on a different file operator
	 * @param from Where to get the file
	 * @param to Where to copy the file
	 * @param source Where to get the file
	 * @param destination Where to copy the file
	 * @throw IOException If any eror happens
	 */
	public static void copy(FileOperator from,FileOperator to,File source,File destination)throws IOException
	{
		to.write(destination,from.read(source));
	}
	/**
	 * Copy the content of a file from one place on another even if on a different file operator
	 * @param from Where to get the file
	 * @param to Where to copy the file
	 * @param file The file to copy with the same name
	 * @throw IOException If any eror happens
	 */
	public static void copy(FileOperator from,FileOperator to,File file)throws IOException
	{
		copy(from,to,file,file);
	}
	/**
	 * Copy the content of a file from one place on another even if on a different file operator
	 * @param fo Where to copy the file
	 * @param source Where to get the file
	 * @param destination Where to copy the file
	 * @throw IOException If any eror happens
	 */
	public static void copy(FileOperator fo,File source,File destination)throws IOException
	{
		copy(fo,fo,source,destination);
	}
	/**
	 * Move the content of a file from one place on another even if on a different file operator
	 * @param from Where to get the file
	 * @param to Where to move the file
	 * @param source Where to get the file
	 * @param destination Where to move the file
	 * @throw IOException If any eror happens
	 */
	public static void move(FileOperator from,FileOperator to,File source,File destination)throws IOException
	{
		copy(from,to,source,destination);
		from.delete(source);
	}
	/**
	 * Move the content of a file from one place on another even if on a different file operator
	 * @param from Where to get the file
	 * @param to Where to move the file
	 * @param file The file to move
	 * @throw IOException If any eror happens
	 */
	public static void move(FileOperator from,FileOperator to,File file)throws IOException
	{
		move(from,to,file,file);
	}
	/**
	 * Move the content of a file from one place on another even if on a different file operator
	 * @param from Where to move the file
	 * @param source Where to get the file
	 * @param destination Where to move the file
	 * @throw IOException If any eror happens
	 */
	public static void move(FileOperator fo,File source,File destination)throws IOException
	{
		move(fo,fo,source,destination);
	}
	/**
	 * Get a list of recent modifed files
	 * @param fo The file operator to use
	 * @param maxResult The maximum number of results to show
	 * @throw IOException If any eror happens
	 */
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
	/**
	 * Get a list of files based on a search query
	 * @param fo The file operator to use
	 * @param query The query string to use
	 * @param maxResult The maximum number of results to show
	 * @throw IOException If any eror happens
	 */
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
}
