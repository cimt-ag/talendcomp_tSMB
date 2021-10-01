package de.cimt.talendcomp.tsmb;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SMBHelperTest {

    public static final String share = "tSMB"; //UUID.randomUUID().toString();
    SMBClient client = null;

    Session session = null;
    DiskShare diskShare = null;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        /*ProcessBuilder b = new ProcessBuilder("net", "share", share + "=" + new File("./"), "/GRANT:jeder,READ");
        b.redirectOutput(new File("c:/temp/out.txt"));
        try {
            b.start().exitValue();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        client = new SMBClient();
        try {
            Connection connection = client.connect("localhost");
            AuthenticationContext authenticationContext = new AuthenticationContext(
                    "xzhang",
                    "IchbinSam@91".toCharArray(),
                    "ad.cimt.de"
            );
            session = connection.authenticate(authenticationContext);
            diskShare = (DiskShare) session.connectShare(share);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        client.close();
    }

    @org.junit.jupiter.api.Test
    void share_list() throws IOException {
        // List all the files except for hidden files
        List<SMBFileStruct> fileStruct_1 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                true,
                true,
                true);
        fileStruct_1.forEach(System.out::println);
        //assertEquals(17, fileStruct_1.size());

        // List all the files including hidden files
        List<SMBFileStruct> fileStruct_2 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                true,
                true,
                false);
        fileStruct_2.forEach(System.out::println);
        //assertEquals(16, fileStruct_2.size());

        // only list the folders under today
        List<SMBFileStruct> fileStruct_3 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                true,
                false,
                false);
        fileStruct_3.forEach(System.out::println);
        //assertEquals(1, fileStruct_3.size());

        // case sensitive for mask
        List<SMBFileStruct> fileStruct_4 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                new String[]{"*.txt"},
                true,
                true,
                true,
                false);
        fileStruct_4.forEach(System.out::println);
        //assertEquals(15, fileStruct_4.size());

        // case sensitive for mask
        List<SMBFileStruct> fileStruct_5 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                new String[]{"*.txt"},
                true,
                false,
                true,
                false);
        fileStruct_5.forEach(System.out::println);
        //assertEquals(16, fileStruct_5.size());

        // list mode for DIRECTORIES
        List<SMBFileStruct> fileStruct_6 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("DIRECTORIES"),
                null,
                true,
                false,
                true,
                false);
        fileStruct_6.forEach(System.out::println);
        //assertEquals(3, fileStruct_6.size());

        // list mode for DIRECTORIES
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("BOTH"),
                null,
                true,
                false,
                true,
                false);
        fileStruct_7.forEach(System.out::println);
        //assertEquals(19, fileStruct_7.size());
    }

    @org.junit.jupiter.api.Test
    void local_list() throws IOException {
        // File path
        List<SMBFileStruct> fileStruct_1 = SMBHelper.list(
                new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\Today\\"),
                SMBHelper.LIST_MODE.parse("DIRECTORIES"),
                null,
                true,
                false,
                true,
                false
        );
        fileStruct_1.forEach(System.out::println);
        //assertEquals(19, fileStruct_1.size());

        // File name
        List<SMBFileStruct> fileStruct_2 = SMBHelper.list(
                new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\Today\\myTest_3G_ (3).txt"),
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false
        );
        fileStruct_2.forEach(System.out::println);
        assertEquals(1, fileStruct_2.size());
    }

    // test on download with true overwrite
    @org.junit.jupiter.api.Test
    void download() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("BOTH"),
                null,
                true,
                false,
                true,
                false);
        SMBTransferStruct smbTransferStruct = null;

        for (SMBFileStruct smbFileStruct : fileStruct_7) {
            smbTransferStruct = SMBHelper.download(
                    diskShare,
                    smbFileStruct,
                    new File("./target/test-classes/local/dnowload"),
                    true
            );
            System.out.println(smbTransferStruct);
        }
    }

    // test on download with false overwrite
    @org.junit.jupiter.api.Test
    void download1() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("BOTH"),
                null,
                true,
                false,
                true,
                false);
        SMBTransferStruct smbTransferStruct = null;
        System.out.println(new File(".").getAbsolutePath());
        for (SMBFileStruct smbFileStruct : fileStruct_7) {
            try {
                smbTransferStruct = SMBHelper.download(
                        diskShare,
                        smbFileStruct,
                        new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\dnowload"),
                        false
                );
                System.out.println(smbTransferStruct);
            } catch (IOException e) {
                assertEquals(e.getMessage().indexOf("already exists") > 0, true);
            }
        }
    }

    // delete on file path
    @org.junit.jupiter.api.Test
    void delete() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today1\\",
                SMBHelper.LIST_MODE.parse("DIRECTORIES"),
                null,
                true,
                false,
                false,
                false);
        fileStruct_7.forEach(System.out::println);
        for (SMBFileStruct smbFileStruct : fileStruct_7) {
            SMBHelper.delete(diskShare, smbFileStruct);
        }
    }

    // delete on file name
    @org.junit.jupiter.api.Test
    void delete1() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today1\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false);
        fileStruct_7.forEach(System.out::println);
        for (SMBFileStruct smbFileStruct : fileStruct_7) {
            SMBHelper.delete(diskShare, smbFileStruct);
        }
    }

    // delete on file both
    @org.junit.jupiter.api.Test
    void delete2() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today1\\",
                SMBHelper.LIST_MODE.parse("BOTH"),
                null,
                true,
                false,
                true,
                false);
        fileStruct_7.forEach(System.out::println);
        for (SMBFileStruct smbFileStruct : fileStruct_7) {
            SMBHelper.delete(diskShare, smbFileStruct);
        }
    }

    // move server to server without change name
    @org.junit.jupiter.api.Test
    void upload() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false);
        fileStruct_7.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_7) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History" + "\\",
                    true,
                    true
            );
        }
    }

    // move server to server with change name
    @org.junit.jupiter.api.Test
    void upload1() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false);
        fileStruct_7.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_7) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History" + "\\" + fileStruct.getName() + "_" + "30-09-2021",
                    true,
                    true,
                    true
            );
        }
    }

    // move server to server with change name
    @org.junit.jupiter.api.Test
    void upload2() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false);
        fileStruct_7.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_7) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History" + "\\" + fileStruct.getName() + "_" + "01-10-2021",
                    true,
                    true,
                    true
            );
        }
    }

    // move local to server without change name
    @org.junit.jupiter.api.Test
    void upload3() throws IOException {
        // File path
        List<SMBFileStruct> fileStruct_1 = SMBHelper.list(
                //new File("./target/test-classes/local/download/myTest_3G_ (3).txt"),
                new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\download"),
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false
        );
        fileStruct_1.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_1) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History2" + "\\",
                    true,
                    false,
                    false
            );
        }
    }

    // move local to server without change name --> with false overwrite
    @org.junit.jupiter.api.Test
    void upload4() throws IOException {
        // File path
        List<SMBFileStruct> fileStruct_1 = SMBHelper.list(
                //new File("./target/test-classes/local/download/myTest_3G_ (3).txt"),
                new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\download"),
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false
        );
        fileStruct_1.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_1) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History2" + "\\",
                    false,
                    false,
                    false
            );
        }
    }

    // move local to server with change name
    @org.junit.jupiter.api.Test
    void upload5() throws IOException {
        // File path
        List<SMBFileStruct> fileStruct_1 = SMBHelper.list(
                //new File("./target/test-classes/local/download/myTest_3G_ (3).txt"),
                new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\download"),
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false
        );
        fileStruct_1.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_1) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History2" + "\\" + fileStruct.getName() + "_01-10-2021",
                    true,
                    false,
                    true
            );
        }
    }

    // move local to server with change name
    @org.junit.jupiter.api.Test
    void upload6() throws IOException {
        // File path
        List<SMBFileStruct> fileStruct_1 = SMBHelper.list(
                //new File("./target/test-classes/local/download/myTest_3G_ (3).txt"),
                new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\download"),
                SMBHelper.LIST_MODE.parse("BOTH"),
                null,
                true,
                false,
                true,
                false
        );
        fileStruct_1.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_1) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History2" + "\\" + fileStruct.getName() + "_01-10-2021",
                    true,
                    false,
                    true
            );
        }
    }

    // move local to server with file name
    @org.junit.jupiter.api.Test
    void upload7() throws IOException {
        // File path
        List<SMBFileStruct> fileStruct_1 = SMBHelper.list(
                //new File("./target/test-classes/local/download/myTest_3G_ (3).txt"),
                new File("C:\\Users\\xzhang\\Documents\\workspace\\cimt\\smb\\src\\test\\resources\\local\\local_2.txt"),
                SMBHelper.LIST_MODE.parse("FILES"),
                null,
                true,
                false,
                true,
                false
        );
        fileStruct_1.forEach(System.out::println);

        for (SMBFileStruct fileStruct : fileStruct_1) {
            SMBHelper.upload(
                    diskShare,
                    fileStruct,
                    "History2" + "\\" + fileStruct.getName() + "_01-10-2021",
                    true,
                    false,
                    true
            );
        }
    }

    @org.junit.jupiter.api.Test
    void createComparator() throws IOException {
        List<SMBFileStruct> fileStruct_7 = SMBHelper.list(diskShare, "Today\\",
                SMBHelper.LIST_MODE.parse("BOTH"),
                null,
                true,
                false,
                true,
                false);
        //fileStruct_7.sort(SMBHelper.createComparator("E"));
        //fileStruct_7.sort(SMBHelper.createComparator("b"));
        //fileStruct_7.sort(SMBHelper.createComparator("c"));
        fileStruct_7.sort(SMBHelper.createComparator("d"));
        //info: take care in javajet if no compare is needed
        fileStruct_7.forEach(file -> System.out.println(file.getName()));
    }
}