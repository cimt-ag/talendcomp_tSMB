package de.cimt.talendcomp.tsmb;

import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import java.io.File;
import java.util.Date;

/**
 *
 * @author dkoch
 */
public class SMBTransferStruct {
    public final long timespend;
    SMBFileStruct handle;
    
    public static String computeSpeedHumanReadable(long bytes, long timemillis){
        String[] suffixes=new String[]{ "B/s", "kB/s", "MB/s", "gB/S"};
        
        String suffix="kb";
        double size=1000.0 / timemillis * bytes;
        
        int idx=0;
        while(size>1024 && idx<suffixes.length){
            idx++;
            size /= 1024;
        }
               
        size=Math.round( 100*size )/100.0;
        return "" +size +" "+suffixes[idx];
    }
    
    public String getSpeed(){
        return computeSpeedHumanReadable( handle.getSize(), timespend);
    }

    public SMBTransferStruct(String sharePath, String basepath, String relpath, FileAllInformation file, long timespend) {
        handle=new SMBFileStruct(sharePath, basepath, relpath, file);
        this.timespend = timespend;
    }
    public SMBTransferStruct(SMBFileStruct handle, long timespend) {
        this.handle=handle;
        this.timespend = timespend;
    }

    public SMBTransferStruct(File file, File baseFile, long timespend) {
        handle=new SMBFileStruct(file, baseFile);
        this.timespend = timespend;
    }
    public SMBTransferStruct(File file, long timespend) {
        handle=new SMBFileStruct(file, null);
        this.timespend = timespend;
    }
    
    @Override
    public String toString() {
        return "SMBTransferStruct{" + handle.getPath() + '}';
    }

    public String getName() {
        return handle.getName();
    }

    public String getPath() {
        return handle.getPath();
    }

    public String getBasePath() {
        return handle.getBasePath();
    }

    public String getAbsolutePath() {
        return handle.getAbsolutePath();
    }

    public String getParentPath() {
        return handle.getParentPath();
    }

    public String getRelativeParentPath() {
        return handle.getRelativeParentPath();
    }

    public String getRelativePath() {
        return handle.getRelativePath();
    }

    public long getSize() {
        return handle.getSize();
    }

    public long getLastModifiedTimestamp() {
        return handle.getLastModifiedTimestamp();
    }

    public Date getLastModified() {
        return handle.getLastModified();
    }

    public String getSharePath() {
        return handle.getSharePath();
    }

    public String getBasepath() {
        return handle.getBasepath();
    }

    public String[] getSplittedNameAndSeparator() {
        return handle.getSplittedNameAndSeparator();
    }

    public long getTimespend() {
        return timespend;
    }
    
    
}
