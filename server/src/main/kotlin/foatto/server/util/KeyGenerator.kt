package foatto.server.util

import java.io.FileOutputStream
import java.security.KeyPairGenerator

class KeyGenerator {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            if (args.size == 3) {
                generateAndSave(args[0].toInt(), args[1], args[2])
            } else {
                println("Usage: KeyGenerator <bits> <private-key-file-name> <public-key-file-name>")
            }
        }

        fun generateAndSave(
            bits: Int,
            privateKeyFileName: String,
            publicKeyFileName: String,
        ) {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(bits)
            val keyPair = keyPairGenerator.generateKeyPair()
            val privateKey = keyPair.private
            val publicKey = keyPair.public

            var fos = FileOutputStream(privateKeyFileName)
            fos.write(privateKey.encoded)
            fos.close()

            fos = FileOutputStream(publicKeyFileName)
            fos.write(publicKey.encoded)
            fos.close()
        }
    }
}