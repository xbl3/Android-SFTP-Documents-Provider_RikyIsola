package com.island.androidsftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.database.*;
import android.os.*;
import android.provider.*;
import android.provider.DocumentsContract.*;
import android.util.*;
import com.island.sftp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
public class SFTPProvider extends DocumentsProvider
{
	private final List<Cache>tokens=new ArrayList<>();
	private static final String[]DEFAULT_ROOT_PROJECTION=
	{Root.COLUMN_ROOT_ID,Root.COLUMN_FLAGS,Root.COLUMN_ICON,Root.COLUMN_TITLE,Root.COLUMN_DOCUMENT_ID,Root.COLUMN_SUMMARY};
	private static final String[]DEFAULT_DOCUMENT_PROJECTION=
	{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_SIZE,Document.COLUMN_DISPLAY_NAME,Document.COLUMN_LAST_MODIFIED,Document.COLUMN_MIME_TYPE,Document.COLUMN_FLAGS};
	@Override
	public boolean onCreate()
	{
		//Binds every sftp account to get their tokens
		AccountManager accountManager=(AccountManager)Objects.requireNonNull(getContext()).getSystemService(Context.ACCOUNT_SERVICE);
		Log.d("Got account manager instance");
		Account[]accounts=Objects.requireNonNull(accountManager).getAccountsByType(AuthenticationActivity.ACCOUNT_TYPE);
		Log.d(String.format("Got %s accounts",accounts.length));
		for(Account account:accounts)
		{
			tokens.add(new Cache(getContext(),account.name,Log.logger));
			Log.d(String.format("Got account %s cache",account.name));
		}
		Log.i("Sftp documents provider created");
		return true;
	}
	@Override
	public Cursor queryRoots(String[]projection)throws FileNotFoundException
	{
		try
		{
			//Create a matrix for each accounts and add its info to a row
			Log.i(String.format("Ftp query Root: Projection=%s",Arrays.toString(projection)));
			MatrixCursor result=new MatrixCursor(resolveRootProjection(projection));
			Log.d("Created result matrix");
			
			for(Cache token:tokens)
			{
				//Add the token info to the matrix
				String connection=token.name;
				Log.d(String.format("Got token root: %s",connection));
				MatrixCursor.RowBuilder row=result.newRow();
				Log.d("Created row");
				row.add(Root.COLUMN_ROOT_ID,connection);
				Log.d(String.format("Added root id: %s",connection));
				String documentId=connection+"/";
				row.add(Root.COLUMN_DOCUMENT_ID,documentId);
				Log.d(String.format("Added document id: %s",documentId));
				int icon=R.drawable.ic_launcher;
				row.add(Root.COLUMN_ICON,icon);
				Log.d(String.format("Added icon: %s",icon));
				int flags=Root.FLAG_SUPPORTS_CREATE;
				row.add(Root.COLUMN_FLAGS,flags);
				Log.d(String.format("Added flags: %s",flags));
				String title=Objects.requireNonNull(getContext()).getString(R.string.sftp);
				row.add(Root.COLUMN_TITLE,title);
				Log.d(String.format("Added title: %s",title));
				row.add(Root.COLUMN_SUMMARY,connection);
			}
			
			return result;
		}
		catch(Exception e)
		{
			String msg="Error querying roots";
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public Cursor queryDocument(String documentId,String[]projection)throws FileNotFoundException
	{
		try
		{
			//Create a matrix and add the file info to a row
			Log.i(String.format("Query Document: DocumentId=%s Projection=%s",documentId,Arrays.toString(projection)));
			MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			Log.d("Created result matrix");
			
			//Get the file and add its info
			File file=getFile(documentId);
			Log.d(String.format("Got SFTPFile: %s",file));
			putFileInfo(result.newRow(),getCache(documentId),file);
			Log.d("Added file info");
			
			return result;
		}
		catch(Exception e)
		{
			String msg=String.format("Error querying child %s",documentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public Cursor queryChildDocuments(String parentDocumentId,String[]projection,String sortOrder)throws FileNotFoundException
	{
		try
		{
			//Create a matrix and add each child's info to a row
			Log.i(String.format("Query Child Documents: ParentDocumentId=%s Projection=%s SortOrder=%s",parentDocumentId,Arrays.toString(projection),sortOrder));
			MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			Log.d("Created result matrix");
			
			//Make the path a dir
			if(parentDocumentId.charAt(parentDocumentId.length()-1)!='/')parentDocumentId+="/";
			
			//List the files and add their info to the rows
			Cache cache=getCache(parentDocumentId);
			File[]files=cache.listFiles(getFile(parentDocumentId));
			for(File file:files)
			{
				putFileInfo(result.newRow(),cache,file);
			}
			Log.d("Added files");
			
			return result;
		}
		catch(Exception e)
		{
			String msg=String.format("Error querying children of %s",parentDocumentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public ParcelFileDescriptor openDocument(String documentId,String mode,CancellationSignal signal)throws FileNotFoundException
	{
		/*try
		{
			//Open the selected document by downloading it
			Log.i(String.format("Open Document: DocumentId=%s mode=%s signal=%s",documentId,mode,signal));
			int accessMode=ParcelFileDescriptor.parseMode(mode);
			boolean isWrite=(mode.indexOf('w')!=-1);
			Log.d(String.format("Writing mode: %s",isWrite));
			final SFTPFile remoteFile=getFile(documentId);
			Log.d(String.format("Remote file is: %s",remoteFile));
			final File file=new File(Objects.requireNonNull(getContext()).getExternalCacheDir(),remoteFile.getName());
			Log.d(String.format("Source file is: %s",file));
			remoteFile.download(file);
			Log.d("Downloaded file");
			if(isWrite)
			{
				return ParcelFileDescriptor.open(file,accessMode,new Handler(getContext().getMainLooper()),new ParcelFileDescriptor.OnCloseListener()
				{
					@Override
					public void onClose(IOException exception)
					{
						//Upload the changes of the file
						Log.i(String.format("Closed file: %s",remoteFile));
						remoteFile.asyncUpload(file);
						Log.d("Uploaded file");
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
			String msg=String.format("Error opening document %s",documentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}*/
		return null;
	}
	@Override
    public String createDocument(String documentId,String mimeType,String displayName)throws FileNotFoundException
	{
		/*try
		{
			//Create a document on the selected path
			Log.i(String.format("Create document: documentId=%s displayName=%s",documentId,displayName));
			SFTPFile parent=getFile(documentId);
			Log.d(String.format("Folder: %s",parent));
			SFTPFile remoteFile=new SFTPFile(parent,displayName);
			Log.d(String.format("Remote file: %s",remoteFile));
			if(Document.MIME_TYPE_DIR.equals(mimeType))remoteFile.mkdir();
			else remoteFile.createNewFile();
			Log.d(String.format("Create %s",mimeType));
			return getDocumentId(remoteFile);
        }
		catch(Exception e)
		{
			String msg=String.format("Failed to create document with name %s and documentId %s",displayName,documentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
        }*/
		return null;
    }
    @Override
    public void deleteDocument(String documentId)throws FileNotFoundException
	{
        /*try
		{
			//Delete the selected document
			Log.i("Delete document: documentId="+documentId);
			SFTPFile file=getFile(documentId);
			Log.d(String.format("Remote file: %s",file));
			file.delete();
			Log.d("Deleted file");
		}
		catch(Exception e)
		{
			String msg=String.format("Error deleting %s",documentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}*/
    }
    @Override
    public String getDocumentType(String documentId)throws FileNotFoundException
	{
		try
		{
			//Get the mime type
			Log.i("Get document type: documentId="+documentId);
        	File file=getFile(documentId);
			Log.d(String.format("Remote file: %s",file));
			String mimeType=SyncAdapter.getMimeType(file,getCache(documentId));
			Log.d(String.format("Mime type: %s",mimeType));
        	return mimeType;
		}
		catch(Exception e)
		{
			String msg=String.format("Error getting type of %s",documentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}
    }

	@Override
	public String renameDocument(String documentId,String displayName)throws FileNotFoundException
	{
		/*try
		{
			//Rename the selected document
			Log.i(String.format("Rename document: documentId=%s displayName=%s",documentId,displayName));
			SFTPFile source=getFile(documentId);
			Log.d(String.format("Source file: %s",source));
			SFTPFile parent=source.getParentFile();
			Log.d(String.format("Parent file: %s",parent));
			SFTPFile destination=new SFTPFile(parent,displayName,source.lastModified(),source.getSize(),source.isDirectory());
			Log.d(String.format("Destination file: %s",destination));
			source.copy(destination,Objects.requireNonNull(getContext()).getExternalCacheDir(),true);
			Log.d("Renamed file");
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg=String.format("Error renaming document %s",documentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}*/
		return null;
	}
	@Override
	public String moveDocument(String sourceDocumentId,String sourceParentDocumentId,String targetParentDocumentId) throws FileNotFoundException
	{
		/*try
		{
			//Move the selected document
			Log.i(String.format("Move document: sourceDocumentId=%s sourceParentDocumentId=%s targetParentDocumentId=%s",sourceDocumentId,sourceParentDocumentId,targetParentDocumentId));
			SFTPFile source=getFile(sourceDocumentId);
			Log.d(String.format("Source file: %s",source));
			SFTPFile parent=getFile(targetParentDocumentId);
			Log.d(String.format("Parent file: %s",parent));
			SFTPFile destination=new SFTPFile(parent,source.getName(),source.lastModified(),source.getSize(),source.isDirectory());
			Log.d(String.format("Destination file: %s",destination));
			source.copy(destination,Objects.requireNonNull(getContext()).getExternalCacheDir(),true);
			Log.d("Moved file");
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg=String.format("Error moving document %s",sourceDocumentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}*/
		return null;
	}
	@Override
	public String copyDocument(String sourceDocumentId,String targetParentDocumentId) throws FileNotFoundException
	{
		/*try
		{
			//Copy the selected document
			Log.i(String.format("Move document: sourceDocumentId=%s targetParentDocumentId=%s",sourceDocumentId,targetParentDocumentId));
			SFTPFile source=getFile(sourceDocumentId);
			Log.d(String.format("Source file: %s",source));
			SFTPFile parent=getFile(targetParentDocumentId);
			Log.d(String.format("Parent file: %s",parent));
			SFTPFile destination=new SFTPFile(parent,source.getName(),source.lastModified(),source.getSize(),source.isDirectory());
			Log.d(String.format("Destination file: %s",destination));
			source.copy(destination,Objects.requireNonNull(getContext()).getExternalCacheDir(),false);
			Log.d("Copied file");
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg=String.format("Error moving document %s",sourceDocumentId);
			Log.e(msg,e);
			throw new FileNotFoundException(msg);
		}*/
		return null;
	}
	/**
	 * Get the root from the document id
	 * @param documentId The document id
	 * @return The root
	 */
	private static String getRoot(String documentId)
	{
		return documentId.substring(0,documentId.indexOf("/"));
	}
	/**
	 * Resolve the document projection
	 * @param projection The projection
	 * @return The array of the projection
	 */
	private static String[]resolveDocumentProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_DOCUMENT_PROJECTION;
		else return projection;
	}
	/**
	 * Resolve the root projection
	 * @param projection The projection
	 * @return The array of the projection
	 */
	private static String[]resolveRootProjection(String[]projection)
	{
		if(projection==null)return DEFAULT_ROOT_PROJECTION;
		else return projection;
	}
	/**
	 * Get a remote file instance
	 * @param documentId The document id
	 * @return The remote file
	 */
	private static File getFile(String documentId)
	{
		//Get the file info from the document id
		return new File(documentId.substring(documentId.indexOf("/")));
	}
	/**
	 * Get the document id from a remote file
	 * @param ftp The remote file
	 * @return The document id
	 */
	private static String getDocumentId(Cache cache,File file)
	{
		return cache.name+file.getPath();
	}
	private Cache getCache(String documentId)
	{
		String root=getRoot(documentId);
		for(Cache cache:tokens)
		{
			if(cache.name.equals(root))return cache;
		}
		throw new NoSuchElementException(String.format("No cache for %s found",documentId));
	}
	/**
	 * Add the remote file info to a row
	 * @param row The row to add the info
	 * @param file The remote file
	 */
	private static void putFileInfo(MatrixCursor.RowBuilder row,Cache cache,File file) throws IOException
	{
		//Put the files info into the row
		int flags;
		if(file.isDirectory())flags=Document.FLAG_DIR_SUPPORTS_CREATE;
		else
		{
			flags=Document.FLAG_SUPPORTS_WRITE;
			row.add(Document.COLUMN_SIZE,cache.length(file));
		}
		flags|=Document.FLAG_SUPPORTS_DELETE;
		if(Build.VERSION.SDK_INT>=24)flags|=Document.FLAG_SUPPORTS_COPY|Document.FLAG_SUPPORTS_MOVE|Document.FLAG_SUPPORTS_RENAME;
		row.add(Document.COLUMN_FLAGS,flags);
		Log.d(String.format("Added flags: %s",flags));
		String mimeType=SyncAdapter.getMimeType(file,cache);
		row.add(Document.COLUMN_MIME_TYPE,mimeType);
		Log.d(String.format("Added mime type: %s",mimeType));
		String name=file.getName();
		row.add(Document.COLUMN_DISPLAY_NAME,name);
		Log.d(String.format("Added name: %s",name));
		String documentId=getDocumentId(cache,file);
		row.add(Document.COLUMN_DOCUMENT_ID,documentId);
		Log.d(String.format("Added document id: %s",documentId));
		long lastModified=cache.lastModified(file);
		row.add(Document.COLUMN_LAST_MODIFIED,lastModified);
		Log.d(String.format("Added last modified: %s",lastModified));
	}
}
