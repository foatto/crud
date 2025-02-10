package foatto.server.util

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MinioProxy(
    endPoint: String,
    accessKey: String,
    secretKey: String,
    timeOut: Long,
    val defaultBucketName: String,
) {

    private var minioClient: MinioClient

    init {
        minioClient = MinioClient.builder()
            .endpoint(endPoint)
            .credentials(accessKey, secretKey)
//            .httpClient(
//                OkHttpClient()
//                    .newBuilder()
//                    .connectTimeout(timeOut, TimeUnit.SECONDS)
//                    .writeTimeout(timeOut, TimeUnit.SECONDS)
//                    .readTimeout(timeOut, TimeUnit.SECONDS)
//                    .protocols(listOf(Protocol.HTTP_1_1))
//                    .build()
//            )
            .build()
    }

    fun loadFileAsByteArray(
        objectName: String,
        bucketName: String = defaultBucketName,
    ): ByteArray = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).`object`(objectName).build()).readBytes()

    fun saveFile(
        objectName: String,
        objectStream: InputStream,
        objectSize: Long,
        bucketName: String = defaultBucketName,
    ) {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
        }
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .stream(objectStream, objectSize, -1)
                .contentType("application/octet-stream")
                .build()
        )
    }

    fun removeFile(
        objectName: String,
        bucketName: String = defaultBucketName,
    ) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }
}