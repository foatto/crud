package foatto.sql

enum class CoreSQLDialectEnum(
    val dialect: String,
    //--- наименование целочисленного типа
    val integerFieldTypeName: String,
    //--- поддержка полей с бинарными данными
    val isBinaryDataSupported: Boolean,
    //--- наименование типа поля для хранения hex-данных (чисто бинарное или эмулирующее его текстовое)
    val hexFieldTypeName: String,
    //--- максимальный размер поля с бинарными данными
    val binaryFieldMaxSize: Int,
    //--- максимальный размер текстового поля
    val textFieldMaxSize: Int,
    //--- возможность создания кластерного индекса
    val createClusteredIndex: String
) {

    //--- jdbc:h2:tcp://localhost//$root_dir$/mms
    //--- java -cp *.jar org.h2.tools.Server
    // ? no online backup
    // - no Multi-Threaded Statement Processing
    H2(
        dialect = "H2",
        integerFieldTypeName = " INT ",                // INTEGER, INT4, MEDIUMNINT
        isBinaryDataSupported = true,                   // binary data supported
        hexFieldTypeName = " BYTEA ",              // можно ((LONG)VAR)BINARY,RAW,BYTEA
        binaryFieldMaxSize = 64000,                  // до 2 GB ); от греха подальше сделаем 64К//
        textFieldMaxSize = 64000,                  // Integer.MAX_VALUE ); от греха подальше сделаем 64К//
        createClusteredIndex = " CREATE INDEX "
    ),

    //--- jdbc:sqlserver://localhost;databaseName=mms;selectMethod=cursor;
    // + clustered index
    MSSQL(
        dialect = "MSSQL",
        integerFieldTypeName = " INT ",
        isBinaryDataSupported = true,
        hexFieldTypeName = " VARBINARY( 8000 ) ",
        binaryFieldMaxSize = 8000,
        textFieldMaxSize = 8000,
        createClusteredIndex = " CREATE CLUSTERED INDEX "
    ),

    //--- smallest max length of VARCHAR field (4000 chars)
    ORACLE(
        dialect = "Oracle",
        integerFieldTypeName = " NUMBER( 10 ) ",
        isBinaryDataSupported = false,
        hexFieldTypeName = " VARCHAR2( 4000 ) ",
        binaryFieldMaxSize = 4000,
        textFieldMaxSize = 4000,
        createClusteredIndex = " CREATE INDEX "
    ),

    //--- jdbc:postgresql://localhost/mms
    POSTGRESQL(
        dialect = "PostgreSQL",
        integerFieldTypeName = " INT ",
        isBinaryDataSupported = true,
        hexFieldTypeName = " BYTEA ",           // Integer.MAX_VALUE ); от греха подальше сделаем 64К
        binaryFieldMaxSize = 64000,            // Integer.MAX_VALUE ); от греха подальше сделаем 64К
        textFieldMaxSize = 64000,            // Integer.MAX_VALUE ); от греха подальше сделаем 64К
        createClusteredIndex = " CREATE INDEX "
    ),

    //--- jdbc:sqlite:$root_dir$/mms.sqlite
    // - no client/server mode, embedded/file mode only
    // - no online backup, file copy of closed base only
    // - native modules in jdbc drivers (bad support for ARM platforms)
    // - one writer & many reader only (in WAL mode)
    // - no ALTER TABLE (except ADD) & ALTER INDEX statements
    SQLITE(
        dialect = "SQLite",
        integerFieldTypeName = " INTEGER ",
        isBinaryDataSupported = false,
        hexFieldTypeName = " TEXT ",         // 1_000_000_000 ); от греха подальше сделаем 64К
        binaryFieldMaxSize = 64000,            // 1_000_000_000 ); от греха подальше сделаем 64К
        textFieldMaxSize = 64000,            // 1_000_000_000 ); от греха подальше сделаем 64К
        createClusteredIndex = " CREATE INDEX "
    );

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        //--- для поиска описания диалекта по строковому имени
        val hmDialect = mapOf(
            H2.dialect to H2,
            MSSQL.dialect to MSSQL,
            ORACLE.dialect to ORACLE,
            POSTGRESQL.dialect to POSTGRESQL,
            SQLITE.dialect to SQLITE
        )
    }

}

