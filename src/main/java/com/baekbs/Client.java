package com.baekbs;

import com.sun.xml.internal.bind.v2.model.core.ID;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client {

    //서버 아이피 정보
    public static final String SERVER = "";
    //서버 포트 정보
    public static final int SERVER_PORT = 21;
    //서버 아이디
    public static final String ID = "";
    //서버 패스워드
    public static final String PW = "";

    public static void main(String[] args) {

        FTPClient client = new FTPClient();
        Scanner scanner = new Scanner(System.in);

        try {
            client.setControlEncoding("UTF-8");
            //FTP 서버 접속
            client.connect(SERVER,SERVER_PORT);

            int resultCode = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(resultCode)) {
                return;
            }
            else {
                client.setSoTimeout(1000);

                if (!client.login(ID, PW)) {
                    System.out.println("Login Info incorrect");
                    return;
                }

                // 파일 리스트 저장용
                List<String> files = new ArrayList<>();
                // 디렉토리 리스트 저장용
                List<String> dirs = new ArrayList<>();

                System.out.println("프로그램 모드를 선택해주세요.");
                System.out.println("서버에 데이터 업로드 => 1 입력");
                System.out.println("서버에서 데이터 다운로드 => 2 입력");
                int mode = scanner.nextInt();

                if (mode == 1) {
                    System.out.println("서버에 데이터를 업로드 합니다");
                    if (getFileList(client, File.separator, files, dirs)) {
                        Collections.reverse(dirs);
                        Date date = new Date();
                        SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH-mm-ss");
                        String backup_dir = "backup-" + format.format(date);
                        client.changeWorkingDirectory(backup_dir);
                        int result_code = client.getReplyCode();
                        if (result_code == 550) {
                            client.makeDirectory(backup_dir);
                        }
                        ftpFileBackup(client, File.separator, files, dirs, backup_dir);
                        String dir_client = System.getProperty("user.dir");
                        files.clear();
                        dirs.clear();
                        getUploadLocalPC(dir_client, files, dirs);
                        Collections.reverse(dirs);
                        for (String dir : dirs) {
                            client.makeDirectory(dir);
                        }
                        for (String file : files) {
                            try (FileInputStream fi = new FileInputStream(file)) {
                                if (file.indexOf("autodownload.jar") == -1 && file.indexOf("script_downloader.bat") == -1 && file.indexOf("JavaSetup8u281.exe") == -1 && file.indexOf("Configuration.xml") == -1) {
                                    if (client.storeFile(file.replace(dir_client,""),fi)) {
                                        System.out.println("Upload...     " + file);
                                    }
                                }
                            }
                        }
                    }
                }
                else if (mode == 2) {
                    System.out.println("서버에서 데이터를 다운로드 합니다.");
                    files.clear();
                    dirs.clear();
                    // 로컬 PC의 현재 디렉토리 위치 가져오기
                    String dir_client = System.getProperty("user.dir");
                    // 로컬 PC의 현재 디렉토리 파일 및 디렉토리 삭제
                    getLocalFileList_delete(dir_client);

                    if (getFileList(client, File.separator, files, dirs)) {
                        // 순차적 폴더 생성을 위한 디렉토리 리스트 반전
                        Collections.reverse(dirs);
                        // 순차적 폴더 생성
                        for (String dir : dirs) {
                            File file = new File(dir_client+dir);
                            file.mkdir();
                        }
                        // 순차적 파일 다운로드
                        for (String _file : files) {
                            try (FileOutputStream fos = new FileOutputStream(dir_client + File.separator + _file)) {
                                if (client.retrieveFile(_file, fos)) {
                                    System.out.println("Download...     " + _file);
                                }
                            }
                        }
                    }
                    else {
                        System.out.println("There is no data on the server.");
                        return;
                    }
                    client.logout();
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (Throwable err) {
                err.printStackTrace();
            }
        }
    }

    //로컬 PC 파일 및 디렉토리 삭제
    private static void getLocalFileList_delete(String root) {
        File dir_root = new File(root);
        for (File file : dir_root.listFiles()) {
            // 필요 파일 제외 모든 파일 및 디렉토리 삭제
            if (!file.getName().equals("autodownload.jar") && !file.getName().equals("script_downloader.bat") && !file.getName().equals("JavaSetup8u281.exe") && !file.getName().equals("Configuration.xml")) {
                if (file.isFile()) {
                    System.out.println(file.getName() + "   삭제 중...");
                    file.delete();
                }
                else {
                    System.out.println(file.getName() + "   삭제 중...");
                    file.delete();
                }
            }
        }
    }

    //로컬 PC 파일 및 디렉토리 업로드
    private static void getUploadLocalPC(String root, List<String> files, List<String> dirs) {
        File upload = new File(root);
        for (File file : upload.listFiles()) {
            if (file.isFile()) {
                if (file.getName().indexOf("download") != 0) {
                    files.add(file.getAbsolutePath());
                }
            } else {
                if (file.getName().indexOf("download") != 0) {
                    getUploadLocalPC(file.getAbsolutePath(), files, dirs);
                    dirs.add(file.getAbsolutePath().replace(System.getProperty("user.dir"),""));
                }
            }
        }
    }

    // FTP BACKUP
    private static void ftpFileBackup(FTPClient client, String root, List<String> files, List<String> dirs, String backup_dir) throws IOException {
        if (client.changeWorkingDirectory(root)) {
            for (String dir : dirs) {
                System.out.println("Backup...     " + dir);
                client.rename(root+dir,root+backup_dir+dir);
            }
            for (String file : files) {
                System.out.println("Backup...     " + file);
                client.rename(root+file,root+backup_dir+file);
            }
        }
    }


    // FTP 서버에서 데이터 가져오기
    private static boolean getFileList(FTPClient client, String root, List<String> files, List<String> dirs) throws IOException {
        if (client.changeWorkingDirectory(root)) {
            // FTP 서버 내 파일 리스트 찾기
            for (FTPFile file : client.listFiles()) {
                // 찾은 항목이 파일일 경우 파일리스트에 추가
                if (file.isFile()) {
                    if (!file.getName().contains("backup")) {
                        files.add(root + file.getName());
                    }
                }
                // 파일이 아닐 경우 디렉토리라고 판단 => 현재 디렉토리에서 재 검색을 통해 파일 찾기
                else {
                    if (!file.getName().contains("backup")) {
                        if (!getFileList(client, root + file.getName() + File.separator, files, dirs)) {
                            return false;
                        }
                        // 디렉토리가 있을 경우 디렉토리 리스트에 추가
                        else {
                            if (!file.getName().contains("backup")) {
                                dirs.add(root + file.getName() + File.separator);
                            }
                        }
                    }
                }
            }
            return client.changeWorkingDirectory(File.separator);
        }
        return false;
    }




}
