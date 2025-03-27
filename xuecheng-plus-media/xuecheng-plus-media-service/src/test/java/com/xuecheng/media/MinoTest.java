package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinoTest {
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000/")
                    .credentials("minioadmin", "minioadmin")
                    .build();
    @Test
    public void test_upload() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        ContentInfo extensionMatch= ContentInfoUtil.findExtensionMatch(".mp4");
        String mimeType= MediaType.APPLICATION_CBOR_VALUE;
        UploadObjectArgs uploadObjectArgs= UploadObjectArgs.builder()
                .bucket("testbucket")//桶
                .filename("D:\\000goods\\liuli\\9479fce48cde8aace2a2dfdb8d9e6eeb.mp4")//本地文件路径
                .object("test/ceshi.mp4")//对象名
                .contentType("video/mp4")
                .build();
        //上传文件
        minioClient.uploadObject(uploadObjectArgs);
    }
    @Test
    public void deleteTest() {
        try {
            minioClient.removeObject(RemoveObjectArgs
                    .builder()
                    .bucket("testbucket")
                    .object("test/ceshi.mp4")
                    .build());
            System.out.println("删除成功");
        } catch (Exception e) {
            System.out.println("删除失败");
        }
    }
    @Test
    public void getFileTest() {
        try {
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket("testbucket")
                    .object("test/ceshi.mp4")
                    .build());
            FileOutputStream fileOutputStream = new FileOutputStream("D:\\000goods\\liuli\\cdlsb.mp4");
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer,0,len);
            }
            inputStream.close();
            fileOutputStream.close();
            System.out.println("下载成功");
        } catch (Exception e) {
            System.out.println("下载失败");
        }
    }
    @Test
    public void getFileTest2() {
        try {
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket("testbucket")
                    .object("test/ceshi.mp4")
                    .build());
            FileOutputStream fileOutputStream = new FileOutputStream("D:\\000goods\\liuli\\cdlsb.mp4");
            IOUtils.copy(inputStream,fileOutputStream);
            System.out.println("下载成功");
        } catch (Exception e) {
            System.out.println("下载失败");
        }
    }

    //将分块文件上传至minio
    @Test
    public void uploadChunk(){
        String chunkFolderPath = "D:\\000goods\\bigfile_test\\chunk\\";
        File chunkFolder = new File(chunkFolderPath);
        //分块文件
        File[] files = chunkFolder.listFiles();
        //将分块文件上传至minio
        for (int i = 0; i < files.length; i++) {
            try {
                UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder().bucket("testbucket").object("chunk/" + i).filename(files[i].getAbsolutePath()).build();
                minioClient.uploadObject(uploadObjectArgs);
                System.out.println("上传分块成功"+i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    //合并文件，要求分块文件最小5M
    @Test
    public void test_merge() throws Exception {
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)
                .limit(5)
                .map(i -> ComposeSource.builder()
                        .bucket("testbucket")
                        .object("chunk/".concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder().bucket("testbucket").object("merge01.mp4").sources(sources).build();
        minioClient.composeObject(composeObjectArgs);

    }
    //清除分块文件
    @Test
    public void test_removeObjects(){
        //合并分块完成将分块文件清除
        List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                .limit(5)
                .map(i -> new DeleteObject("chunk/".concat(Integer.toString(i))))
                .collect(Collectors.toList());

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("testbucket").objects(deleteObjects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        results.forEach(r->{
            DeleteError deleteError = null;
            try {
                deleteError = r.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
