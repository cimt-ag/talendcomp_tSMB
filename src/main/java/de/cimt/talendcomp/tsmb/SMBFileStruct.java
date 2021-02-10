package de.cimt.talendcomp.tsmb;

import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import java.util.Date;

/**
 *
 * @author dkoch
 */
public final class SMBFileStruct {
    public boolean remote=true;
    public String name;
    public String sharePath;
    public String basepath;
    public String relpath;
    public long size = 0l;
    public boolean directory = false;

    public long lastModifiedTimestamp;
    public Date lastModified;

    private void checkValues(){
        
        if(name.endsWith("\\"))
            name=name.substring(0, name.length()-1);
        if(!sharePath.isEmpty()){
            if(!sharePath.endsWith("\\"))
                sharePath+= "\\";
            if(!sharePath.startsWith("\\\\"))
                sharePath="\\\\" + sharePath;
        }
        
        if(!basepath.isEmpty()){
            if(!basepath.endsWith("\\"))
                basepath+= "\\";
            if(basepath.startsWith("\\"))
                basepath=basepath.substring(1);
        }

        if(!relpath.isEmpty()){
            if(!relpath.endsWith("\\"))
                relpath+= "\\";
            if(relpath.startsWith("\\"))
                relpath=relpath.substring(1);
        }
        
    }
        
    SMBFileStruct(String share, String base, String rel, FileAllInformation file) {
        if(file==null)
            return;
        
        this.name=file.getNameInformation();
        this.sharePath=share!=null ? share : "";
        this.basepath=base!=null ? base : "";
        this.relpath=rel!=null ? rel : "";

        size = file.getStandardInformation().getEndOfFile();
        directory=file.getStandardInformation().isDirectory();
        lastModifiedTimestamp = file.getBasicInformation().getChangeTime().toEpochMillis();
        lastModified = new Date(lastModifiedTimestamp);
        
        if(this.name==null || this.name.isEmpty()){
            final boolean useBase=rel.isEmpty();
            String namepart=useBase ? base : rel;
            String newpath="";
            if(namepart.contains("\\") ){
                int pos = namepart.lastIndexOf("\\")+1;
                newpath=namepart.substring( 0, pos );
                namepart=namepart.substring( pos );
            }
            if(useBase)
                this.basepath=newpath;
            else
                this.relpath=newpath;
            this.name=namepart;
        }
        checkValues();
    }
    SMBFileStruct(java.io.File file, java.io.File baseFile) {
        if(file==null)
            return;
        
        remote=false;
        name = file.getName();
        this.sharePath="";
        this.basepath = baseFile!=null ? baseFile.getAbsolutePath() : "";
        this.relpath = file.getParent();
        
        if(basepath.length()>0 && relpath.startsWith(basepath)){
            relpath= relpath.replace(basepath, "");
        }
        size = file.length();
        lastModifiedTimestamp = file.lastModified();
        lastModified = new Date(file.lastModified());     
        directory=file.isDirectory();
        checkValues();
    }
    
//    public SMBFileStruct(String name, String path,  String relpath, long size, long timestamp) {
//        this(name, path, relpath, size, timestamp, new Date(timestamp));
//    }

    SMBFileStruct(String name, String share, String smbbasepath, String relpath, long size, Date lastModified) {
        this(name, share, smbbasepath, relpath, size, ((lastModified != null) ? lastModified.getTime() : 0l), lastModified);
    }

    SMBFileStruct(String filename, String share, String smbbasepath, String smbrelpath, long size, long timestamp, Date lastModified) {
        this.name = filename;
        this.sharePath = share;
        this.basepath = (smbbasepath==null) ? "" : smbbasepath.replace('/', '\\');
        this.relpath = (smbrelpath==null) ? "" : smbrelpath.replace('/', '\\');
        this.size = size;
        this.lastModifiedTimestamp = timestamp;
        this.lastModified = lastModified;
        directory = filename.endsWith("\\");
        checkValues();
    }

    private SMBFileStruct() {
    }

    public String toString() {
        return "SMBFileStruct{"  + (directory ? "Directory" : "File     ") + " name=" + name + ", sharePath=" + sharePath + ", basepath=" + basepath + ", relpath=" + relpath + ", size=" + size + ", directory=" + directory + ", lastModifiedTimestamp=" + lastModifiedTimestamp + ", lastModified=" + lastModified + '}';
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return basepath +  getRelativePath();
    }
    
    public String getBasePath() {
        return basepath;
    }

    public String getAbsolutePath() {
        return sharePath + getPath();
    }
    
    public String getParentPath() {
        return basepath + relpath;
    }
    
    public String getRelativeParentPath() {
        return relpath;
    }

    public String getRelativePath() {
        return relpath + getName() + (directory? "\\":"");
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getSharePath() {
        return sharePath;
    }

    public String getBasepath() {
        return basepath;
    }
    
    public String[] getSplittedNameAndSeparator(){
        final int pos = name.lastIndexOf(".");
        
        if(pos>0)
            return new String[]{
                name.substring(0,pos),
                name.substring(pos)
            };
        return new String[]{
            name, ""
        };
    }
    
    
}
