package de.cimt.talendcomp.tsmb;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.protocol.commons.EnumWithValue.EnumUtils;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.io.ByteChunkProvider;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import com.hierynomus.smbj.utils.SmbFiles;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.oro.text.GlobCompiler;

/**
 * @author dkoch
 */
public class SMBHelper {

    public enum LIST_MODE {
        FILES,
        DIRECTORIES,
        BOTH;

        public static LIST_MODE parse() {
            return BOTH;
        }

        public static LIST_MODE parse(String value) {
            try {
                return LIST_MODE.valueOf(value);
            } catch (Throwable t) {
            }
            return BOTH;
        }
    }

    private static final Logger LOG = Logger.getLogger(SMBHelper.class.getName());


    public static List<SMBFileStruct> list(DiskShare share, String path, LIST_MODE mode, String[] mask, boolean isGlob, boolean caseSensitive, boolean recursive) throws IOException {
        return list(share, path, mode, mask, isGlob, caseSensitive, recursive, true);
    }

    public static List<SMBFileStruct> list(DiskShare share, String path, LIST_MODE mode, String[] mask, boolean isGlob, boolean caseSensitive, boolean recursive, boolean hidden) throws IOException {

        if (path == null || path.equals("/") || path.equals("/.") || path.equals("\\") || path.equals("\\.") || path.equals(".")) {
            path = "";
        }
        while (path.startsWith("/") || path.startsWith("\\")) {
            path = path.substring(1);
        }
        path = path.replace('/', '\\');

        return list(share, path, "", mode, createFilter(mask, isGlob, caseSensitive), recursive, hidden);
    }

    private static List<SMBFileStruct> list(final DiskShare share, final String basepath, final String path, final LIST_MODE mode, final FilenameFilter filter, final boolean recursive, final boolean hidden) throws FileNotFoundException {

        if (share.fileExists(basepath + path)) {

            if (!filter.accept(null, !path.contains("\\") ? path : path.substring(path.lastIndexOf("\\") + 1))) {
                return Collections.emptyList();
            }


            final FileAllInformation fileInformation = share.getFileInformation(basepath + path);

            // skip hidden files when hidden == falsesfileAttributes
            if (!hidden && EnumUtils.toEnumSet(fileInformation.getBasicInformation().getFileAttributes(), FileAttributes.class).contains(FileAttributes.FILE_ATTRIBUTE_HIDDEN)) {
                LOG.finer("ignore hiddden file " + basepath + path);
                return Collections.emptyList();
            }
            System.err.println("add regular file " + path);
            return Arrays.asList(new SMBFileStruct(
                    share.getSmbPath().toString(),
                    basepath,
                    path,
                    fileInformation
            ));
        }
        if (!share.folderExists(basepath + path)) {
            throw new java.io.FileNotFoundException("no such directory \"" + (basepath + path) + "\" found in share " + share);
        }

//        final String currentPath = (path.length()>0) ? (path+ "\\") : path;

        List<SMBFileStruct> files = new ArrayList<SMBFileStruct>();
        for (FileIdBothDirectoryInformation sub : share.list(basepath + path)) {
            final String filename = sub.getFileName();
            // skip . and ..
            if (filename.equals(".") || filename.equals("..")) {
                continue;
            }

            final EnumSet<FileAttributes> fileAttributes = EnumUtils.toEnumSet(sub.getFileAttributes(), FileAttributes.class);

            // skip hidden files when hidden == falses
            if (fileAttributes.contains(FileAttributes.FILE_ATTRIBUTE_HIDDEN) && !hidden) {
                continue;
            }

            if (fileAttributes.contains(FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
                // handle dierectories

                // TODO: is it a good idea to use the filenamefilter for folder selection?
                if (mode != LIST_MODE.FILES) { // && filter.accept(null, filename)){
                    System.err.println("add directory " + sub.getFileName());
                    files.add(
                            new SMBFileStruct(
                                    sub.getFileName() + "\\",
                                    share.getSmbPath().toString(),
                                    basepath,
                                    path,
                                    sub.getEndOfFile(),
                                    sub.getChangeTime().toDate()
                            )
                    );
                }
                if (recursive) {
                    System.err.println("traverse directory");
                    try {
                        files.addAll(list(share, basepath, path + sub.getFileName() + "\\", mode, filter, recursive, hidden));
                    } catch (com.hierynomus.mssmb2.SMBApiException e) {
                        if (e.getStatus() != NtStatus.STATUS_ACCESS_DENIED) {
                            throw e;
                        }

                        LOG.log(java.util.logging.Level.SEVERE, "access denied for {0} ...", sub.getFileName());
                    }
                }

            } else {
                // handle regular files
                if (mode == LIST_MODE.DIRECTORIES)
                    continue;
                if (!filter.accept(null, filename))
                    continue;


                System.err.println("add file " + sub.getFileName());
                files.add(
                        new SMBFileStruct(
                                sub.getFileName(),
                                share.getSmbPath().toString(),
                                basepath,
                                path,
                                sub.getEndOfFile(),
                                sub.getChangeTime().toDate()
                        )
                );
            }
        }

        return files;
    }

    //XK
    public static List<SMBFileStruct> list(java.io.File root, LIST_MODE mode, String[] mask, boolean isGlob, boolean caseSensitive, boolean recursive) throws IOException {
        return list(root, mode, mask, isGlob, caseSensitive, recursive, true);
    }

    public static List<SMBFileStruct> list(java.io.File root, LIST_MODE mode, String[] mask, boolean isGlob, boolean caseSensitive, boolean recursive, boolean hidden) throws IOException {
        if (root == null || !root.exists()) {
            return Collections.EMPTY_LIST;
        }

        if (root.isFile()) {
            return Arrays.asList(new SMBFileStruct(root, root.getParentFile()));
        }

        return list(root, root, mode, createFilter(mask, isGlob, caseSensitive), recursive, hidden);

    }

    private static List<SMBFileStruct> list(java.io.File root, java.io.File path, LIST_MODE mode, final FilenameFilter filter, final boolean recursive, final boolean hidden) throws IOException {
        List<SMBFileStruct> handles = new ArrayList<SMBFileStruct>();
        System.err.println("root=" + root.getAbsolutePath());
        System.err.println("path=" + path.getAbsolutePath());
        for (java.io.File f : path.listFiles(filter)) {
            if (f.isHidden() && !hidden)
                continue;

            if (f.isDirectory()) {
                if (!recursive)
                    continue;

                handles.addAll(list(root, f, mode, filter, recursive, hidden));
            } else {
                handles.add(new SMBFileStruct(f, root));
            }


        }
        return handles;
    }

    private static FilenameFilter createFilter(String[] mask, boolean isGlob, boolean caseSensitive) {
        if (mask != null && mask.length > 0) {
            final List<Pattern> patterns = new ArrayList<>();
            for (String currentMask : mask) {
                if (isGlob) {
                    currentMask = GlobCompiler.globToPerl5(currentMask.toCharArray(), GlobCompiler.DEFAULT_MASK);
                }
                patterns.add(caseSensitive ? Pattern.compile(currentMask) : Pattern.compile(currentMask, Pattern.CASE_INSENSITIVE));
            }
            return (java.io.File dir, String name) -> {
                for (final java.util.regex.Pattern pattern : patterns)
                    if (pattern.matcher(name).matches())
                        return true;

                if (dir != null && new java.io.File(dir, name).isDirectory()) {
                    return true;
                }
                return false;
            };
        } else {
            return (java.io.File dir, String name) -> true;
        }
    }

    public static SMBTransferStruct download(DiskShare diskShare, SMBFileStruct source, java.io.File destinationFolder, boolean overwrite) throws IOException {
        if (destinationFolder.exists() && !overwrite) {
            throw new IOException("file \"" + destinationFolder.getAbsolutePath() + "\" already exists");
        }

        final boolean copyToFile = destinationFolder.exists() && destinationFolder.isFile();
        if (copyToFile && source.directory) {
            throw new IOException("folder \"" + source.getAbsolutePath() + "\" can not been downloaded to file \"" + destinationFolder.getAbsolutePath() + "\",");
        }
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs();
        }

        final long ts = System.currentTimeMillis();

        File handle = null;
        FileOutputStream out = null;
        java.io.File destinationFile = new java.io.File(destinationFolder.getAbsolutePath() + "\\" + source.getRelativePath());
        if (copyToFile) {
            destinationFile = destinationFolder;
        }
        LOG.finer("download " + source.getRelativePath() + " to " + destinationFile.getAbsolutePath());
        try {

            if (source.directory) {
                destinationFile.mkdirs();

            } else {
                if (!destinationFile.getParentFile().exists())
                    destinationFile.getParentFile().mkdirs();

                handle = diskShare.openFile(source.getPath(),
                        EnumSet.of(AccessMask.GENERIC_READ),
                        EnumSet.allOf(FileAttributes.class),
                        EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                        SMB2CreateDisposition.FILE_OPEN,
                        EnumSet.of(SMB2CreateOptions.FILE_RANDOM_ACCESS));


                out = new FileOutputStream(destinationFile, overwrite);
                handle.read(out);
                out.flush();

                handle.close();
                out.close();
            }
        } finally {

            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
            if (handle != null) {
                handle.close();
            }
        }

        return new SMBTransferStruct(destinationFile, System.currentTimeMillis() - ts);
    }

    private static SMBTransferStruct performUpload(DiskShare diskShare, String path, java.io.File sourceFile, boolean overwrite) throws IOException {
        if (!sourceFile.exists()) {
            throw new IOException("no such file \"" + path + "\" found");
        }

        final long ts = System.currentTimeMillis();

        File handle = null;
        FileInputStream in = null;

        try {
            if (diskShare.fileExists(path)) {

                if (!overwrite)
                    throw new IOException("file \"" + path + "\" already exists");

                handle = diskShare.openFile(path,
                        EnumSet.of(AccessMask.GENERIC_WRITE),
                        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                        EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                        SMB2CreateDisposition.FILE_OVERWRITE_IF,
                        EnumSet.of(SMB2CreateOptions.FILE_RANDOM_ACCESS));
            } else {
                handle = diskShare.openFile(path,
                        EnumSet.of(AccessMask.GENERIC_WRITE),
                        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                        EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                        SMB2CreateDisposition.FILE_OPEN_IF,
                        EnumSet.of(SMB2CreateOptions.FILE_RANDOM_ACCESS));

            }

            in = new FileInputStream(sourceFile);
            byte[] buffer = new byte[4096];
            int size;
            long filepos = 0;
            while ((size = in.read(buffer)) > 0) {
                handle.write(buffer, filepos, 0, size);
                filepos += size;
            }

        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
            if (handle != null) {
                handle.close();
            }
        }

        return new SMBTransferStruct(diskShare.getSmbPath().toString(), path, "", diskShare.getFileInformation(path), System.currentTimeMillis() - ts);
    }

    public static synchronized void delete(DiskShare diskShare, SMBFileStruct file) throws SMBApiException {

        if (diskShare.fileExists(file.getPath())) {
            diskShare.rm(file.getPath());
        } else if (diskShare.folderExists(file.getPath())) {
            diskShare.rmdir(file.getPath(), true);
        }

    }

    // XK
    // info: this upload method can only be used for local file upload into remote server
    public static synchronized SMBTransferStruct upload(DiskShare diskShare, SMBFileStruct file, String destPath, boolean overwrite) throws SMBApiException, IOException {
        String fullpath = destPath + (!destPath.endsWith("\\") ? "\\" : "") + file.getName();

        final long ts = System.currentTimeMillis();

        if (file.directory) {
            if (!diskShare.folderExists(fullpath)) {
                diskShare.mkdir(fullpath);
            }
        } else {
            SmbFiles.copy(new java.io.File(file.getAbsolutePath()), diskShare, fullpath, overwrite);
        }

        return new SMBTransferStruct(
                new SMBFileStruct(
                        diskShare.getSmbPath().toString(),
                        destPath,
                        file.getRelativeParentPath(),
                        diskShare.getFileInformation(fullpath))
                , System.currentTimeMillis() - ts
        );
    }

    public static synchronized SMBTransferStruct upload(DiskShare diskShare, SMBFileStruct file, String destPath, boolean overwrite, boolean onserver) throws SMBApiException, IOException {
        // todo: should have to check whether the destPath is a path or a file!!!
        // info: possible to think a better way to verify the destPath
        final long ts = System.currentTimeMillis();

        if (!onserver) {
            return upload(diskShare, file, destPath, overwrite);
        } else {
            if (destPath.endsWith("\\")) {
                destPath += file.getName();
            }
        }

        // info: server to server transfer
        // warning: using the individual code in batch job b_netact_to_mediation_device if test fails!
        File input = diskShare.openFile(file.getPath(),
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null);

        File output = diskShare.openFile(destPath,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                overwrite ? SMB2CreateDisposition.FILE_OVERWRITE_IF : SMB2CreateDisposition.FILE_CREATE,
                null);
        try {
            input.remoteCopyTo(output);
        } catch (Buffer.BufferException | TransportException exception) {
            exception.printStackTrace();
        }
        return new SMBTransferStruct(
                new SMBFileStruct(
                        diskShare.getSmbPath().toString(),
                        destPath,
                        file.getRelativeParentPath(),
                        diskShare.getFileInformation(destPath)),
                System.currentTimeMillis() - ts
        );
    }
}
