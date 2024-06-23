package foatto.compose.external

const val SHA_1 = "SHA-1"
const val SHA_224 = "SHA-224"
const val SHA3_224 = "SHA3-224"
const val SHA_256 = "SHA-256"
const val SHA3_256 = "SHA3-256"
const val SHA_384 = "SHA-384"
const val SHA3_384 = "SHA3-384"
const val SHA_512 = "SHA-512"
const val SHA3_512 = "SHA3-512"
const val SHAKE128 = "SHAKE128"
const val SHAKE256 = "SHAKE256"

const val SHA_INPUT_HEX = "HEX"
const val SHA_INPUT_TEXT = "TEXT"
const val SHA_INPUT_B64 = "B64"
const val SHA_INPUT_BYTES = "BYTES"
const val SHA_INPUT_ARRAYBUFFER = "ARRAYBUFFER"

const val SHA_OUTPUT_HEX = "HEX"
const val SHA_OUTPUT_TEXT = "TEXT"
const val SHA_OUTPUT_B64 = "B64"
const val SHA_OUTPUT_BYTES = "BYTES"
const val SHA_OUTPUT_ARRAYBUFFER = "ARRAYBUFFER"

external class jsSHA(algorithm: String, inputType: String) {
    fun update(input: String)
    fun getHash(outputType: String): String
}