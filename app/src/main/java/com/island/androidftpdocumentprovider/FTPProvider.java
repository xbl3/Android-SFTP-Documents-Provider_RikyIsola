package com.island.androidftpdocumentprovider;
import android.content.*;
import android.database.*;
import android.os.*;
import android.provider.*;
import android.provider.DocumentsContract.*;
import android.webkit.*;
import java.io.*;
import com.enterprisedt.net.ftp.*;
import android.util.*;
import java.util.*;
import java.text.*;
public class FTPProvider extends DocumentsProvider
{
	private static final String DEBUG_CREDENTIALS="anonymous";
	private static final int MAX_LAST_MODIFIED=5;
	private static final int MAX_SEARCH_RESULTS=20;
	private static final String[]DEFAULT_ROOT_PROJECTION=
	{Root.COLUMN_ROOT_ID,Root.COLUMN_FLAGS,Root.COLUMN_ICON,Root.COLUMN_TITLE,Root.COLUMN_DOCUMENT_ID};
	private static final String[] DEFAULT_DOCUMENT_PROJECTION=
	{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_SIZE,Document.COLUMN_DISPLAY_NAME,Document.COLUMN_LAST_MODIFIED,Document.COLUMN_MIME_TYPE,Document.COLUMN_FLAGS};
	@Override public boolean onCreate(){return true;}
	@Override
	public Cursor queryRoots(String[]projection)throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Query Root: Projection="+Arrays.toString(projection));
			MatrixCursor result=new MatrixCursor(resolveRootProjection(projection));
			for(String connection:new String[]{"127.0.0.1:8888"})
			{
				MatrixCursor.RowBuilder row=result.newRow();
				row.add(Root.COLUMN_ROOT_ID,connection);
				row.add(Root.COLUMN_DOCUMENT_ID,connection+"/");
				row.add(Root.COLUMN_ICON,R.drawable.ic_launcher);
				row.add(Root.COLUMN_FLAGS,Root.FLAG_SUPPORTS_CREATE|Root.FLAG_SUPPORTS_RECENTS|Root.FLAG_SUPPORTS_SEARCH);
				row.add(Root.COLUMN_TITLE,connection);
			}
			return result;
		}
		catch(Exception e)
		{
			String msg="Error querying roots";
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public Cursor queryDocument(String documentId,String[]projection)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Query Document: DocumentId="+documentId+" Projection="+Arrays.toString(projection));
		MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
		try
		{
			FTPFile file=getFile(documentId);
			putFileInfo(result.newRow(),file);
		}
		catch(Exception e)
		{
			String msg="Error querying child "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
		return result;
	}
	@Override
	public Cursor queryChildDocuments(String parentDocumentId,String[]projection,String sortOrder)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Query Child Documents: ParentDocumentId="+parentDocumentId+" Projection="+projection+" SortOrder="+sortOrder);
		MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
		try
		{
			if(parentDocumentId.charAt(parentDocumentId.length()-1)!='/')parentDocumentId+="/";
			FTPFile[]files=getFile(parentDocumentId).listFiles();
			for(FTPFile file:files)
			{
				putFileInfo(result.newRow(),file);
			}
		}
		catch(Exception e)
		{
			String msg="Error querying childs of "+parentDocumentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
		return result;
	}
	@Override
	public ParcelFileDescriptor openDocument(String documentId,String mode,CancellationSignal signal)throws FileNotFoundException
	{
		Log.i(MainActivity.LOG_TAG,"Open Document: DocumentId="+documentId+" mode="+mode+" signal="+signal);
		int accessMode=ParcelFileDescriptor.parseMode(mode);
		boolean isWrite=(mode.indexOf('w')!=-1);
		try
		{
			final FTPFile remoteFile=getFile(documentId);
			final File file=new File(getContext().getExternalCacheDir(),remoteFile.getName());
			remoteFile.download(file);
			if(isWrite)
			{
				return ParcelFileDescriptor.open(file,accessMode,new Handler(getContext().getMainLooper()),new ParcelFileDescriptor.OnCloseListener()
					{
						@Override
						public void onClose(IOException exception)
						{
							remoteFile.asyncUpload(file);
						}
					});
			}
			else
			{
				return ParcelFileDescriptor.open(file,accessMode);
			}
		}
		catch(Exception e)
		{
			String msg="Error opening document "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
    public Cursor queryRecentDocuments(String rootId,String[]projection)throws FileNotFoundException
	{
        try
		{
			Log.i(MainActivity.LOG_TAG,"Query recent documents: rootId="+rootId+" projection="+Arrays.toString(projection));
			MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			final FTPFile parent=getFile(rootId);
			PriorityQueue<FTPFile>lastModifiedFiles=new PriorityQueue<FTPFile>(5,new Comparator<FTPFile>()
				{
					public int compare(FTPFile i,FTPFile j)
					{
						return Long.compare(i.lastModified(),j.lastModified());
					}
				});
			// Iterate through all files and directories in the file structure under the root.  If
			// the file is more recent than the least recently modified, add it to the queue,
			// limiting the number of results.
			LinkedList<FTPFile>pending=new LinkedList<FTPFile>();
			// Start by adding the parent to the list of files to be processed
			pending.add(parent);
			// Do while we still have unexamined files
			while(!pending.isEmpty())
			{
				// Take a file from the list of unprocessed files
				final FTPFile file=pending.removeFirst();
				if(file.isDirectory())
				{
					// If it's a directory, add all its children to the unprocessed list
					Collections.addAll(pending,file.listFiles());
				}
				else
				{
					// If it's a file, add it to the ordered queue.
					lastModifiedFiles.add(file);
				}
			}
			// Add the most recent files to the cursor, not exceeding the max number of results.
			for(int i=0;i<Math.min(MAX_LAST_MODIFIED+1,lastModifiedFiles.size());i++)
			{
				FTPFile file=lastModifiedFiles.remove();
				putFileInfo(result.newRow(),file);
			}
			return result;
		}
		catch(Exception e)
		{
			String msg="Error querying recents";
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }
	@Override
    public Cursor querySearchDocuments(String rootId,String query,String[]projection)throws FileNotFoundException
	{
        try
		{
			Log.v(MainActivity.LOG_TAG,"Query search documents: rootId="+rootId+" query="+query+" projection="+Arrays.toString(projection));
			// Create a cursor with the requested projection, or the default projection.
			final MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			final FTPFile parent=getFile(rootId);
			// This example implementation searches file names for the query and doesn't rank search
			// results, so we can stop as soon as we find a sufficient number of matches.  Other
			// implementations might use other data about files, rather than the file name, to
			// produce a match; it might also require a network call to query a remote server.

			// Iterate through all files in the file structure under the root until we reach the
			// desired number of matches.
			final LinkedList<FTPFile> pending = new LinkedList<FTPFile>();

			// Start by adding the parent to the list of files to be processed
			pending.add(parent);

			// Do while we still have unexamined files, and fewer than the max search results
			while(!pending.isEmpty()&&result.getCount()<MAX_SEARCH_RESULTS)
			{
				// Take a file from the list of unprocessed files
				final FTPFile file=pending.removeFirst();
				if(file.isDirectory())
				{
					// If it's a directory, add all its children to the unprocessed list
					Collections.addAll(pending,file.listFiles());
				}
				else
				{
					// If it's a file and it matches, add it to the result cursor.
					if(file.getName().toLowerCase().contains(query))
					{
						putFileInfo(result.newRow(),file);
					}
				}
			}
			return result;
		}
		catch(Exception e)
		{
			String msg="Error searching "+query;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }
	@Override
    public String createDocument(String documentId,String mimeType,String displayName)throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Create document: documentId="+documentId+" displayName"+displayName);
			FTPFile parent=getFile(documentId);
			FTPFile remoteFile;
			while(true)
			{
				remoteFile=new FTPFile(parent,displayName);
				if(remoteFile.exist())displayName+="2";
				else break;
			}
			if(Document.MIME_TYPE_DIR.equals(mimeType))remoteFile.mkdir();
			else remoteFile.createNewFile();
			return getDocumentId(remoteFile);
        }
		catch(Exception e)
		{
			String msg="Failed to create document with name "+displayName+" and documentId "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
        }
    }
    @Override
    public void deleteDocument(String documentId)throws FileNotFoundException
	{
        try
		{
			Log.i(MainActivity.LOG_TAG,"Delete document: documentId="+documentId);
			FTPFile file=getFile(documentId);
			file.delete();
		}
		catch(Exception e)
		{
			String msg="Error deleting "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }
    @Override
    public String getDocumentType(String documentId)throws FileNotFoundException
	{
		try
		{
			Log.i(MainActivity.LOG_TAG,"Get document type: documentId="+documentId);
        	FTPFile file=getFile(documentId);
        	return file.getMimeType();
		}
		catch(Exception e)
		{
			String msg="Error getting type of "+documentId;
			Log.e(MainActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }
	private static String getRoot(String documentId)
	{
		return documentId.substring(0,documentId.indexOf("/"));
	}
	private static String getIp(String documentId)
	{
		return documentId.substring(0,documentId.indexOf(":"));
	}
	private static int getPort(String documentId)
	{
		int end=documentId.indexOf("/");
		if(end==-1)end=documentId.length();
		return Integer.valueOf(documentId.substring(documentId.indexOf(":")+1,end));
	}
	private static String getPath(String documentId)
	{
		int start=documentId.indexOf("/");
		if(start==-1)return"";
		else return documentId.substring(start+1);
	}
	private static final String[]resolveDocumentProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_DOCUMENT_PROJECTION;
		else return projection;
	}
	private static final String[]resolveRootProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_ROOT_PROJECTION;
		else return projection;
	}
	private static final FTPFile getFile(String documentID)
	{
		String ip=getIp(documentID);
		int port=getPort(documentID);
		String user=DEBUG_CREDENTIALS;
		String password=DEBUG_CREDENTIALS;
		String path=getPath(documentID);
		return new FTPFile(ip,port,user,password,path);
	}
	private static final String getDocumentId(FTPFile ftp)
	{
		return ftp.getIp()+":"+ftp.getPort()+"/"+ftp.getPath();
	}
	private void putFileInfo(MatrixCursor.RowBuilder row,FTPFile file)
	{
		int flags;
		if(file.isDirectory())flags=Document.FLAG_DIR_SUPPORTS_CREATE;
		else
		{
			flags=Document.FLAG_SUPPORTS_WRITE;
			row.add(Document.COLUMN_SIZE,file.getSize());
		}
		flags|=Document.FLAG_SUPPORTS_COPY|Document.FLAG_SUPPORTS_DELETE|Document.FLAG_SUPPORTS_MOVE|Document.FLAG_SUPPORTS_REMOVE|Document.FLAG_SUPPORTS_RENAME;
		row.add(Document.COLUMN_FLAGS,flags);
		row.add(Document.COLUMN_MIME_TYPE,file.getMimeType());
		row.add(Document.COLUMN_DISPLAY_NAME,file.getName());
		row.add(Document.COLUMN_DOCUMENT_ID,getDocumentId(file));
		row.add(Document.COLUMN_LAST_MODIFIED,file.lastModified());
	}
}
