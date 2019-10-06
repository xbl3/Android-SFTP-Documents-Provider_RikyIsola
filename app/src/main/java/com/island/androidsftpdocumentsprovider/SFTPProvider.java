package com.island.androidsftpdocumentsprovider;
import android.content.*;
import android.database.*;
import android.os.*;
import android.provider.*;
import android.provider.DocumentsContract.*;
import java.io.*;
import android.util.*;
import java.util.*;
import android.accounts.*;
import java.net.*;
import com.jcraft.jsch.ChannelSftp;
public class SFTPProvider extends DocumentsProvider implements AccountManagerCallback<Bundle>
{
	private final List<String>tokens=new ArrayList<>();
    private final Map<String,ChannelSftp>channels=new HashMap<>();
	private static final String[]DEFAULT_ROOT_PROJECTION=
	{Root.COLUMN_ROOT_ID,Root.COLUMN_FLAGS,Root.COLUMN_ICON,Root.COLUMN_TITLE,Root.COLUMN_DOCUMENT_ID,Root.COLUMN_SUMMARY};
	private static final String[] DEFAULT_DOCUMENT_PROJECTION=
	{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_SIZE,Document.COLUMN_DISPLAY_NAME,Document.COLUMN_LAST_MODIFIED,Document.COLUMN_MIME_TYPE,Document.COLUMN_FLAGS};
	@Override
	public boolean onCreate()
	{
		//Binds every sftp account to get their tokens
		AccountManager accountManager=(AccountManager)Objects.requireNonNull(getContext()).getSystemService(Context.ACCOUNT_SERVICE);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Got account manager instance");
		Account[]accounts=Objects.requireNonNull(accountManager).getAccountsByType(AuthenticationActivity.ACCOUNT_TYPE);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Got %s accounts",accounts.length));
		for(Account account:accounts)
		{
			accountManager.getAuthToken(account,AuthenticationActivity.TOKEN_TYPE,null,true,this,null);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Getting account %s token",account.name));
		}
		Log.i(AuthenticationActivity.LOG_TAG,"Ftp documents provider created");
		return true;
	}
	@Override
	public void run(AccountManagerFuture<Bundle>future)
	{
		try
		{
			//Add the received token to the list
			Bundle bundle=future.getResult();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Got result bundle");
			String token=bundle.getString(AccountManager.KEY_AUTHTOKEN);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Got token from result bundle");
			tokens.add(token);
			Log.i(AuthenticationActivity.LOG_TAG,"Account token received");
		}
		catch(AuthenticatorException e)
		{
			Log.e(AuthenticationActivity.LOG_TAG,"Authentication failed",e);
		}
		catch(android.accounts.OperationCanceledException e)
		{
			Log.e(AuthenticationActivity.LOG_TAG,"Authentication canceled",e);
		}
		catch(IOException e)
		{
			Log.e(AuthenticationActivity.LOG_TAG,"Network error",e);
		}
	}
	@Override
	public Cursor queryRoots(String[]projection)throws FileNotFoundException
	{
		try
		{
			//Create a matrix for each accounts and add its info to a row
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Ftp query Root: Projection=%s",Arrays.toString(projection)));
			MatrixCursor result=new MatrixCursor(resolveRootProjection(projection));
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Created result matrix");
			
			for(String token:tokens)
			{
				//Add the token info to the matrix
				String connection=getRoot(token);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Got token root: %s",connection));
				MatrixCursor.RowBuilder row=result.newRow();
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Created row");
				row.add(Root.COLUMN_ROOT_ID,connection);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added root id: %s",connection));
				String documentId=connection+"/";
				row.add(Root.COLUMN_DOCUMENT_ID,documentId);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added document id: %s",documentId));
				int icon=R.drawable.ic_launcher;
				row.add(Root.COLUMN_ICON,icon);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added icon: %s",icon));
				int flags=Root.FLAG_SUPPORTS_CREATE;
				row.add(Root.COLUMN_FLAGS,flags);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added flags: %s",flags));
				String title=Objects.requireNonNull(getContext()).getString(R.string.sftp);
				row.add(Root.COLUMN_TITLE,title);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added title: %s",title));
				row.add(Root.COLUMN_SUMMARY,connection);
			}
			
			return result;
		}
		catch(Exception e)
		{
			String msg="Error querying roots";
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public Cursor queryDocument(String documentId,String[]projection)throws FileNotFoundException
	{
		try
		{
			//Create a matrix and add the file info to a row
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Query Document: DocumentId=%s Projection=%s",documentId,Arrays.toString(projection)));
			MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Created result matrix");
			
			//Get the file and add its info
			SFTPFile file=getFile(documentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Got SFTPFile: %s",file));
			putFileInfo(result.newRow(),file);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Added file info");
			
			return result;
		}
		catch(Exception e)
		{
			String msg=String.format("Error querying child %s",documentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public Cursor queryChildDocuments(String parentDocumentId,String[]projection,String sortOrder)throws FileNotFoundException
	{
		try
		{
			//Create a matrix and add each child's info to a row
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Query Child Documents: ParentDocumentId=%s Projection=%s SortOrder=%s",parentDocumentId,Arrays.toString(projection),sortOrder));
			MatrixCursor result=new MatrixCursor(resolveDocumentProjection(projection));
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Created result matrix");
			
			//Make the path a dir
			if(parentDocumentId.charAt(parentDocumentId.length()-1)!='/')parentDocumentId+="/";
			
			//If the document is the root check if the host online
			if(getPath(parentDocumentId).isEmpty())
			{
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"The document is root, checking if the host is online");
				Socket socket=new Socket();
				InetSocketAddress address=new InetSocketAddress(getIp(parentDocumentId),getPort(parentDocumentId));
				socket.connect(address,AuthenticationActivity.TIMEOUT);
				socket.close();
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"The host is online");
			}
			
			//List the files and add their info to the rows
			SFTPFile[]files=getFile(parentDocumentId).listFiles();
			for(SFTPFile file:files)
			{
				putFileInfo(result.newRow(),file);
			}
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Added files");
			
			return result;
		}
		catch(Exception e)
		{
			String msg=String.format("Error querying children of %s",parentDocumentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public ParcelFileDescriptor openDocument(String documentId,String mode,CancellationSignal signal)throws FileNotFoundException
	{
		try
		{
			//Open the selected document by downloading it
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Open Document: DocumentId=%s mode=%s signal=%s",documentId,mode,signal));
			int accessMode=ParcelFileDescriptor.parseMode(mode);
			boolean isWrite=(mode.indexOf('w')!=-1);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Writing mode: %s",isWrite));
			final SFTPFile remoteFile=getFile(documentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Remote file is: %s",remoteFile));
			final File file=new File(Objects.requireNonNull(getContext()).getExternalCacheDir(),remoteFile.getName());
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Source file is: %s",file));
			remoteFile.download(file);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Downloaded file");
			if(isWrite)
			{
				return ParcelFileDescriptor.open(file,accessMode,new Handler(getContext().getMainLooper()),new ParcelFileDescriptor.OnCloseListener()
				{
					@Override
					public void onClose(IOException exception)
					{
						//Upload the changes of the file
						Log.i(AuthenticationActivity.LOG_TAG,String.format("Closed file: %s",remoteFile));
						remoteFile.asyncUpload(file);
						if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Uploaded file");
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
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
    public String createDocument(String documentId,String mimeType,String displayName)throws FileNotFoundException
	{
		try
		{
			//Create a document on the selected path
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Create document: documentId=%s displayName=%s",documentId,displayName));
			SFTPFile parent=getFile(documentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Folder: %s",parent));
			SFTPFile remoteFile=new SFTPFile(parent,displayName);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Remote file: %s",remoteFile));
			if(Document.MIME_TYPE_DIR.equals(mimeType))remoteFile.mkdir();
			else remoteFile.createNewFile();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Create %s",mimeType));
			return getDocumentId(remoteFile);
        }
		catch(Exception e)
		{
			String msg=String.format("Failed to create document with name %s and documentId %s",displayName,documentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
        }
    }
    @Override
    public void deleteDocument(String documentId)throws FileNotFoundException
	{
        try
		{
			//Delete the selected document
			Log.i(AuthenticationActivity.LOG_TAG,"Delete document: documentId="+documentId);
			SFTPFile file=getFile(documentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Remote file: %s",file));
			file.delete();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Deleted file");
		}
		catch(Exception e)
		{
			String msg=String.format("Error deleting %s",documentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }
    @Override
    public String getDocumentType(String documentId)throws FileNotFoundException
	{
		try
		{
			//Get the mime type
			Log.i(AuthenticationActivity.LOG_TAG,"Get document type: documentId="+documentId);
        	SFTPFile file=getFile(documentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Remote file: %s",file));
			String mimeType=file.getMimeType();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Mime type: %s",mimeType));
        	return mimeType;
		}
		catch(Exception e)
		{
			String msg=String.format("Error getting type of %s",documentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
    }

	@Override
	public String renameDocument(String documentId,String displayName)throws FileNotFoundException
	{
		try
		{
			//Rename the selected document
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Rename document: documentId=%s displayName=%s",documentId,displayName));
			SFTPFile source=getFile(documentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Source file: %s",source));
			SFTPFile parent=source.getParentFile();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Parent file: %s",parent));
			SFTPFile destination=new SFTPFile(parent,displayName,source.lastModified(),source.getSize(),source.isDirectory());
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Destination file: %s",destination));
			source.copy(destination,Objects.requireNonNull(getContext()).getExternalCacheDir(),true);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Renamed file");
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg=String.format("Error renaming document %s",documentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public String moveDocument(String sourceDocumentId,String sourceParentDocumentId,String targetParentDocumentId) throws FileNotFoundException
	{
		try
		{
			//Move the selected document
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Move document: sourceDocumentId=%s sourceParentDocumentId=%s targetParentDocumentId=%s",sourceDocumentId,sourceParentDocumentId,targetParentDocumentId));
			SFTPFile source=getFile(sourceDocumentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Source file: %s",source));
			SFTPFile parent=getFile(targetParentDocumentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Parent file: %s",parent));
			SFTPFile destination=new SFTPFile(parent,source.getName(),source.lastModified(),source.getSize(),source.isDirectory());
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Destination file: %s",destination));
			source.copy(destination,Objects.requireNonNull(getContext()).getExternalCacheDir(),true);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Moved file");
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg=String.format("Error moving document %s",sourceDocumentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
	}
	@Override
	public String copyDocument(String sourceDocumentId,String targetParentDocumentId) throws FileNotFoundException
	{
		try
		{
			//Copy the selected document
			Log.i(AuthenticationActivity.LOG_TAG,String.format("Move document: sourceDocumentId=%s targetParentDocumentId=%s",sourceDocumentId,targetParentDocumentId));
			SFTPFile source=getFile(sourceDocumentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Source file: %s",source));
			SFTPFile parent=getFile(targetParentDocumentId);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Parent file: %s",parent));
			SFTPFile destination=new SFTPFile(parent,source.getName(),source.lastModified(),source.getSize(),source.isDirectory());
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Destination file: %s",destination));
			source.copy(destination,Objects.requireNonNull(getContext()).getExternalCacheDir(),false);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Copied file");
			return getDocumentId(destination);
		}
		catch(Exception e)
		{
			String msg=String.format("Error moving document %s",sourceDocumentId);
			Log.e(AuthenticationActivity.LOG_TAG,msg,e);
			throw new FileNotFoundException(msg);
		}
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
	 * Get the ip from the document id
	 * @param documentId The document id
	 * @return The ip
	 */
	private static String getIp(String documentId)
	{
		return documentId.substring(0,documentId.indexOf(":"));
	}
	/**
	 * Get the port from the document id
	 * @param documentId The document id
	 * @return The port
	 */
	private static int getPort(String documentId)
	{
		return Integer.valueOf(documentId.substring(documentId.indexOf(":")+1,documentId.indexOf("@")));
	}
	/**
	 * Get the user from the document id
	 * @param documentId The document id
	 * @return The user
	 */
	private static String getUser(String documentId)
	{
		int end=documentId.indexOf("/");
		if(end==-1)end=documentId.length();
		return documentId.substring(documentId.indexOf("@")+1,end);
	}
	/**
	 * Get the path from the document id
	 * @param documentId The document id
	 * @return The path
	 */
	private static String getPath(String documentId)
	{
		int start=documentId.indexOf("/");
		if(start==-1)return"/";
		else return documentId.substring(start);
	}
	/**
	 * Get the password using the document id
	 * @param documentId The document id
	 * @return The password
	 */
	private String getPassword(String documentId)
	{
		//Select the token from the token's list
		String root=getRoot(documentId);
		for(String token:tokens)
		{
			if(token.startsWith(root))
			{
				return token.substring(documentId.indexOf("/")+1);
			}
		}
		throw new NoSuchElementException(String.format("No token available for documentId %s",documentId));
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
	private SFTPFile getFile(String documentId)throws IOException
	{
		//Get the file info from the document id
		String ip=getIp(documentId);
		int port=getPort(documentId);
		String user=getUser(documentId);
		String password=getPassword(documentId);
		String path=getPath(documentId);
		
		//Get a channel to communicate with the host
        ChannelSftp channel=channels.get(getRoot(documentId));
		
		//If the channel is disconnected remove it
        if(channel!=null&&!channel.isConnected())channel=null;
		
		//If no channel is found create a new one
        if(channel==null)
        {
			Log.i(AuthenticationActivity.LOG_TAG,"Creating new sftp session");
            channel=SFTPFile.createChannel(ip,port,user,password);
            channels.put(getRoot(documentId),channel);
        }
		
		return new SFTPFile(null,ip,port,user,password,path);
	}
	/**
	 * Get the document id from a remote file
	 * @param ftp The remote file
	 * @return The document id
	 */
	private static String getDocumentId(SFTPFile ftp)
	{
		return ftp.getIp()+":"+ftp.getPort()+"@"+ftp.getUser()+ftp.getPath();
	}
	/**
	 * Add the remote file info to a row
	 * @param row The row to add the info
	 * @param file The remote file
	 */
	private static void putFileInfo(MatrixCursor.RowBuilder row,SFTPFile file)
	{
		//Put the files info into the row
		int flags;
		if(file.isDirectory())flags=Document.FLAG_DIR_SUPPORTS_CREATE;
		else
		{
			flags=Document.FLAG_SUPPORTS_WRITE;
			row.add(Document.COLUMN_SIZE,file.getSize());
		}
		flags|=Document.FLAG_SUPPORTS_DELETE;
		if(Build.VERSION.SDK_INT>=24)flags|=Document.FLAG_SUPPORTS_COPY|Document.FLAG_SUPPORTS_MOVE|Document.FLAG_SUPPORTS_RENAME;
		row.add(Document.COLUMN_FLAGS,flags);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added flags: %s",flags));
		String mimeType=file.getMimeType();
		row.add(Document.COLUMN_MIME_TYPE,mimeType);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added mime type: %s",mimeType));
		String name=file.getName();
		row.add(Document.COLUMN_DISPLAY_NAME,name);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added name: %s",name));
		String documentId=getDocumentId(file);
		row.add(Document.COLUMN_DOCUMENT_ID,documentId);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added document id: %s",documentId));
		long lastModified=file.lastModified();
		row.add(Document.COLUMN_LAST_MODIFIED,lastModified);
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Added last modified: %s",lastModified));
	}
}
