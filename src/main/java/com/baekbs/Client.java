package com.baekbs;

import com.sun.xml.internal.bind.v2.model.core.ID;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Client {

    //서버 아이피 정보
    public static final String SERVER = "nuriqc.iptime.org";
    //서버 포트 정보
    public static final int SERVER_PORT = 21;
    //서버 아이디
    public static final String ID = "ftp";
    //서버 패스워드
    public static final String PW = "admin";

    public static void main(String[] args) {

        FTPClient client = new FTPClient();

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

                // 로컬 PC의 현재 디렉토리 위치 가져오기
                String dir_client = System.getProperty("user.dir");
                // 로컬 PC의 현재 디렉토리 파일 및 디렉토리 정보 가져오기
                getLocalFileList(dir_client);


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

    //로컬에서 데이터 가져오기
    private static void getLocalFileList(String root) {
        File dir_root = new File(root);
        for (File file : dir_root.listFiles()) {
            // 필요 파일 제외 모든 파일 및 디렉토리 삭제
            if (!file.getName().equals("script_downloader.bat") && !file.getName().equals("JavaSetup8u281.exe") && !file.getName().equals("Configuration.xml")) {
                if (file.isFile()) {
                    file.delete();
                }
                else {
                    file.delete();
                }
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
                    files.add(root + file.getName());
                }
                // 파일이 아닐 경우 디렉토리라고 판단 => 현재 디렉토리에서 재 검색을 통해 파일 찾기
                else {
                    if (!getFileList(client, root + file.getName() + File.separator, files, dirs)) {
                        return false;
                    }
                    // 디렉토리가 있을 경우 디렉토리 리스트에 추가
                    else {
                        dirs.add(root + file.getName() + File.separator);
                    }
                }
            }
            return client.changeWorkingDirectory(File.separator);
        }
        return false;
    }

}
